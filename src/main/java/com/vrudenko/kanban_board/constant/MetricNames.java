package com.vrudenko.kanban_board.constant;

/**
 * The Micrometer meter names, tag keys, bounded tag values and observation names this application
 * publishes.
 *
 * Every name is a published contract: dashboards and alert rules query the Prometheus form of it
 * (dots become underscores, a counter gains a _total suffix, a timer gains _seconds), so renaming
 * one silently blanks every panel and alert built on it.
 *
 * Tag values come from the fixed sets below and never from request data. A raw path, a user id, a
 * board id or an exception message as a tag value would mint a new time series per distinct value.
 */
public final class MetricNames {
    public static final String TASKS_CREATED = "kanban.tasks.created";
    public static final String TASKS_MOVED = "kanban.tasks.moved";
    public static final String OPTIMISTIC_LOCK_CONFLICTS = "kanban.optimistic.lock.conflicts";
    public static final String OWNERSHIP_DENIED = "kanban.ownership.denied";
    public static final String AUTH_FAILURES = "kanban.auth.failures";

    public static final String TAG_MOVE = "move";
    public static final String MOVE_SAME_COLUMN = "same_column";
    public static final String MOVE_CROSS_COLUMN = "cross_column";

    public static final String TAG_REASON = "reason";
    public static final String REASON_BAD_CREDENTIALS = "bad_credentials";
    public static final String REASON_UNAUTHENTICATED = "unauthenticated";

    // Used as @Observed(name = ...) on service methods; an annotation value must be a compile-time
    // constant, so these stay plain static final strings.
    public static final String OBSERVATION_TASK_MOVE = "kanban.task.move";
    public static final String OBSERVATION_TASK_CREATE = "kanban.task.create";
    public static final String OBSERVATION_BOARD_FULL_READ = "kanban.board.full.read";
}
