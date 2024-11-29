package utils;

public class GatewayConstants {
    // Status Constants
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String STATUS_MAINTENANCE = "MAINTENANCE";

    // Transaction Status Constants
    public static final String TRANSACTION_PENDING = "PENDING";
    public static final String TRANSACTION_COMPLETED = "COMPLETED";
    public static final String TRANSACTION_FAILED = "FAILED";
    public static final String TRANSACTION_REFUNDED = "REFUNDED";

    // Event Types
    public static final String EVENT_STATUS_CHANGE = "STATUS_CHANGE";
    public static final String EVENT_TRANSACTION = "TRANSACTION";
    public static final String EVENT_ERROR = "ERROR";
    public static final String EVENT_MAINTENANCE = "MAINTENANCE";

    // Configuration Constants
    public static final int GATEWAY_CHECK_INTERVAL = 30000; // 30 seconds
    public static final int LOG_RETENTION_DAYS = 30;
    public static final int MAX_RETRY_ATTEMPTS = 3;
}