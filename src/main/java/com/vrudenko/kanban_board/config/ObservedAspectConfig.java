package com.vrudenko.kanban_board.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Replace Spring Boot's ObservedAspect with one that runs outside the transaction advisor.
 *
 * Boot's own bean has no order, so it ties with the transaction advisor at the lowest precedence
 * and lands inside it. An observed service method that owns its transaction would then stop its
 * timer before the commit, and the commit is part of what a caller waits for.
 *
 * The bean keeps the name and the condition of Boot's, so Boot's backs off and the same property,
 * management.observations.annotations.enabled, still turns it on.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty("management.observations.annotations.enabled")
public class ObservedAspectConfig {

    // The declared return type is the subclass on purpose: Spring reads the aspect's order from the
    // bean type it knows before instantiation, and a return type of ObservedAspect hides Ordered.
    @Bean
    OutsideTransactionObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new OutsideTransactionObservedAspect(observationRegistry);
    }

    static class OutsideTransactionObservedAspect extends ObservedAspect implements Ordered {

        OutsideTransactionObservedAspect(ObservationRegistry observationRegistry) {
            super(observationRegistry);
        }

        @Override
        public int getOrder() {
            // One step outside the transaction advisor, which sits at LOWEST_PRECEDENCE.
            return Ordered.LOWEST_PRECEDENCE - 1;
        }
    }
}
