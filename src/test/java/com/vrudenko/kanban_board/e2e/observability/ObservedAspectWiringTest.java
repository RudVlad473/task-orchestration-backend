package com.vrudenko.kanban_board.e2e.observability;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import com.vrudenko.kanban_board.constant.MetricNames;
import com.vrudenko.kanban_board.dto.task_dto.MoveTaskRequestDTO;
import com.vrudenko.kanban_board.event.TaskCreatedEvent;
import com.vrudenko.kanban_board.event.TaskMovedEvent;
import com.vrudenko.kanban_board.service.TaskService;
import com.vrudenko.kanban_board.support.fixtures.AbstractAppTest;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.annotation.Observed;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AbstractAspectJAdvice;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor;

/**
 * Proves how the Observed annotation behaves on the real service beans: where the observation
 * sits relative to the transaction, and which calls it does not see.
 *
 * The context carries two probe beans (see Probes). SlowCommitListener sleeps just before a
 * commit when armed, which makes the time spent committing measurable. SelfInvokingProbe has an
 * annotated method and a caller of it on the same object.
 *
 * Decisions:
 *
 * The annotation is applied by a Spring AOP proxy, so it sees a call only when the call crosses
 * the proxy. A call on this inside the class bypasses it, which the self-invocation cases pin.
 *
 * A service method that owns its transaction commits after its body returns. Whether that commit
 * falls inside the observed time depends on the aspect order against the transaction advisor,
 * which the order cases pin.
 */
@SpringBootTest
@Import(ObservedAspectWiringTest.Probes.class)
public class ObservedAspectWiringTest extends AbstractAppTest {

    private static final String PROBE_OBSERVATION = "kanban.test.probe";
    private static final long COMMIT_DELAY_MILLIS = 300;

    @Autowired private MeterRegistry meterRegistry;

    @Autowired private TaskService taskService;

    @Autowired private SlowCommitListener slowCommitListener;

    @Autowired private SelfInvokingProbe selfInvokingProbe;

    @TestConfiguration
    static class Probes {
        @Bean
        SlowCommitListener slowCommitListener() {
            return new SlowCommitListener();
        }

        @Bean
        SelfInvokingProbe selfInvokingProbe() {
            return new SelfInvokingProbe();
        }
    }

    static class SlowCommitListener {
        private volatile long delayMillis;

        void arm(long millis) {
            delayMillis = millis;
        }

        @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
        public void onTaskMoved(TaskMovedEvent event) throws InterruptedException {
            Thread.sleep(delayMillis);
        }

        @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
        public void onTaskCreated(TaskCreatedEvent event) throws InterruptedException {
            Thread.sleep(delayMillis);
        }
    }

    static class SelfInvokingProbe {
        @Observed(name = PROBE_OBSERVATION)
        public void observed() {}

        public void callsObservedOnThis() {
            observed();
        }
    }

    @AfterEach
    void disarm() {
        slowCommitListener.arm(0);
    }

    private long timerCount(String name) {
        return meterRegistry.find(name).timers().stream().mapToLong(Timer::count).sum();
    }

    private long timerMillis(String name) {
        return (long)
                meterRegistry.find(name).timers().stream()
                        .mapToDouble(timer -> timer.totalTime(TimeUnit.MILLISECONDS))
                        .sum();
    }

    @Nested
    class AspectOrderTest {
        private int indexOf(Advisor[] advisors, Predicate<Advisor> match) {
            for (var index = 0; index < advisors.length; index++) {
                if (match.test(advisors[index])) {
                    return index;
                }
            }
            return -1;
        }

        @Test
        void shouldPlaceObservationAdvisorBeforeTransactionAdvisor_onTaskService() {
            // arrange
            var advisors = ((Advised) taskService).getAdvisors();

            // act: an earlier advisor is the outer one in the interceptor chain
            var observation =
                    indexOf(
                            advisors,
                            advisor ->
                                    advisor.getAdvice() instanceof AbstractAspectJAdvice aspect
                                            && aspect.getAspectName().equals("observedAspect"));
            var transaction =
                    indexOf(
                            advisors,
                            advisor ->
                                    advisor
                                            instanceof
                                            BeanFactoryTransactionAttributeSourceAdvisor);

            // assert
            Assertions.assertThat(observation).as("observation advisor index").isNotNegative();
            Assertions.assertThat(transaction).as("transaction advisor index").isNotNegative();
            Assertions.assertThat(observation).isLessThan(transaction);
        }
    }

    @Nested
    class CommitCoverageTest {
        @Test
        void shouldIncludeCommitInMoveObservation_whenMoveOwnsItsTransaction() {
            // arrange
            slowCommitListener.arm(COMMIT_DELAY_MILLIS);
            var before = timerMillis(MetricNames.OBSERVATION_TASK_MOVE);

            // act
            taskService.moveToColumn(
                    getOwningUser().getId(),
                    mockPopulatedTask.getId(),
                    MoveTaskRequestDTO.builder()
                            .targetColumnId(mockColumns.get(0).getId())
                            .version(mockPopulatedTask.getVersion())
                            .build());

            // assert: the sleep happens in the commit, after the method body has returned
            Assertions.assertThat(timerMillis(MetricNames.OBSERVATION_TASK_MOVE) - before)
                    .isGreaterThanOrEqualTo(COMMIT_DELAY_MILLIS);
        }

        @Test
        void shouldNotCoverCommit_whenCallerOwnsTheTransactionOfTaskCreate() {
            // arrange: ColumnService.addTaskByColumnId owns the transaction, TaskService.save joins
            // it
            slowCommitListener.arm(COMMIT_DELAY_MILLIS);
            var before = timerMillis(MetricNames.OBSERVATION_TASK_CREATE);

            // act
            createTask();

            // assert: documents a limit, not a goal. The commit runs when the outer method returns,
            // after save's observation has already stopped.
            Assertions.assertThat(timerMillis(MetricNames.OBSERVATION_TASK_CREATE) - before)
                    .isLessThan(COMMIT_DELAY_MILLIS);
        }
    }

    @Nested
    class SelfInvocationTest {
        @Test
        void shouldObserve_whenAnnotatedMethodIsCalledThroughTheProxy() {
            // arrange
            var before = timerCount(PROBE_OBSERVATION);

            // act
            selfInvokingProbe.observed();

            // assert
            Assertions.assertThat(timerCount(PROBE_OBSERVATION)).isEqualTo(before + 1);
        }

        @Test
        void shouldNotObserve_whenAnnotatedMethodIsCalledOnThisFromInsideItsClass() {
            // arrange
            var before = timerCount(PROBE_OBSERVATION);

            // act
            selfInvokingProbe.callsObservedOnThis();

            // assert: the inner call never crosses the proxy
            Assertions.assertThat(timerCount(PROBE_OBSERVATION)).isEqualTo(before);
        }
    }
}
