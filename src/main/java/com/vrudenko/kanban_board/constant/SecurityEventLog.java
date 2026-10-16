package com.vrudenko.kanban_board.constant;

/**
 * The field names and fixed values of the WARN line written for an authentication failure or an
 * ownership denial.
 *
 * Log queries and alert rules read these names as JSON keys, so a rename breaks them silently. The
 * fields are deliberately few and fixed: the submitted email, password, exception message and
 * session id are never fields, because a log line outlives the request and is readable by everyone
 * who can query the log store.
 */
public final class SecurityEventLog {
    public static final String EVENT = "security_event";
    public static final String EVENT_AUTH_FAILURE = "auth_failure";
    public static final String EVENT_ACCESS_DENIED = "access_denied";

    public static final String REASON = "reason";
    public static final String REASON_NOT_OWNER = "not_owner";

    public static final String HTTP_METHOD = "http_method";
    public static final String PATH = "path";
    public static final String USER_ID = "user_id";

    public static final String MESSAGE = "Security event";
}
