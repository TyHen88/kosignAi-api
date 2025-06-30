package org.kosign.chatbotapi.components.common.api;

public enum StatusCode {

    SUCCESS(200, "Success", 200),

    // 400 Bad Request
    BAD_REQUEST(40000, "Bad Request", 400),
    CLIENT_DISABLED(40001, "Client is disabled", 400),
    IMAGE_CANNOT_BE_EMPTY(40002, "Image cannot be empty", 400),
    ISS_DMT_GREATER_THAN_NOW(40003, "IssueDateTime must be greater than now", 400),
    DUE_DTM_GREATER_THAN_ISSUE_DTM(40004, "DueDateTime must be greater than IssueDateTime", 400),
    NEW_ISS_DMT_EQUAL_OR_GREATER_OLD_ISS_DMT(40005, "IssueDateTime must be equal or greater than the old one", 400),
    NEW_DUE_DTM_EQUAL_OR_GREATER_THAN_NOW(40006, "DueDateTime must be equal or greater than now", 400),
    INVALID_REFERENCE_NUMBER(40007, "Invalid reference number", 400),
    INVALID_BILL_NO(40008, "Invalid bill number", 400),
    BILL_IN_PROGRESS(40009, "Bill is in progress", 400),
    INVALID_CURRENCY(40010, "Currency is invalid", 400),
    BILLER_CONTRACT_NOT_FOUND(40011, "Biller contract not found", 400),
    BILLER_NOT_FOUND(40012, "Biller not found", 400),
    EMPLOYEE_NOT_FOUND(40013, "Employee not found", 400),
    EMAIL_OR_PHONE_INVALID(40014, "Email or Phone is invalid", 400),
    PAYER_CANNOT_DELETE(40015, "Payer cannot deleted", 400),
    PASSWORD_MUST_BE_ENCRYPTED(40016, "Password must be encrypted", 400),
    BILL_NOT_PAID(40017, "Payer have not paid the previous bill yet", 400),
    BILL_IS_CLOSE(40018, "Bill is closed", 400),
    UN_SUPPORTED_OPERATION(40019, "Unsupported operation", 400),
    BIll_CANNOT_DELETE(40020, "Bill cannot be deleted", 400),
    BILL_FIXED_AMOUNT(40021, "Bill with a fixed amount must be paid with equal total price", 400),
    BIll_CANNOT_UPDATE(40022, "Bill cannot be updated", 400),
    BILL_CANNOT_MATCH(40023, "Bill cannot match", 400),
    BILL_CANNOT_UNMATCH(40024, "Bill cannot unmatch", 400),
    SECURITY_CODE_INCORRECT(40025, "Security code incorrect", 400),
    DISABLE_VERIFY_OTP_CODE(40026, "Verify otp code is disabled", 400),
    DISABLE_SENT_OTP_CODE_FOR_5_MINUTES(40027, "Disabled send OTP code for 5 minutes.", 400),
    DISABLE_SENT_OTP_CODE(40028, "Disabled send OTP code for 15 minutes.", 400),
    SECURITY_CODE_EXPIRED(40029, "Security code is expired.", 400),
    BILLER_ACCOUNT_NOT_FOUND(40030, "Biller account not found", 400),
    VIRTUAL_USD_NOT_ENOUGH(40031, "Virtual account USD not enough", 400),
    VIRTUAL_VND_NOT_ENOUGH(40032, "Virtual account VND not enough", 400),
    VIRTUAL_ACCOUNT_NOT_ENOUGH(40033, "Virtual account not enough", 400),
    INVALID_PHONE(40033, "Invalid phone number", 400),
    PHONENUMBER_ISEXIST(40034, "Phone number is already exist", 400),
    CUSTOMER_NAME_EXIST(40035, "This customer is already exist in this phone number", 400),
    PHONENUMBER_MORE_THEN_ONE(40036, "Phone number has been used more than one", 400),
    PHONENUMBER_ISEXIST_BLLR(40037, "Phone number is already exist in this biller", 400),
    EMAIL_IS_NULL(40038, "Email is null", 400),
    EMAIL_NOT_VALID(40039, "Email is not valid", 400),
    PHONE_NUMBER_INVALID(40040, "Phone number or Email must be encrypted", 400),
    SECURITY_CODE_MUST_BE_ENCRYPTED(40041, "Security code must be encrypted", 400),
    SECURITY_KEY_MUST_BE_ENCRYPTED(40042, "Security key must be encrypted", 400),
    BILL_NOT_FOUNT(40043, "Bill not found", 400),
    DUPLICATE_MONTH(40044, "Month cannot duplicate", 400),
    BILL_ALREADY_PAID(40045, "Bill already paid", 400),
    BILL_SCHEDULED(40046, "Bill in schedule", 400),
    BILL_OVERDUE(40047, "Bill is overdue", 400),
    WABOOKS_SEND_INVOICE_FAILED(40048, "Send invoice failed.", 400),
    TRIAL_VERSION(40049, "Trial version", 400),
    CUSTOMER_ID_EXIST(40050, "This Customer ID is already exist", 400),
    DATA_IS_DUPLICATE(40051, "Data Cannot Duplicate" , 400),
    VERIFY_OTP_CODE_REQUIRED(40024, "Verify otp code is required", 400),
    BILL_NO_EXIST(40051, "Bill no already exist [{0}] " , 400),
    CLIENT_ID_ALREAD_EXIST(40053,"Client ID already exist",400),
    ACCOUNT_LOCKED(40054,"Account Locked",400),
    BAD_CREDENTIAL(40055,"Incorrect Password",400),
    USER_ID_OR_USER_NAME_IS_DUPLICATE(40054,"User ID Or User Name is Duplicate" ,400),
    PRICE_PLAN_NOT_FOUND(40054,"Price plan not found",400),
    TAX_RAT_CANNOT_BE_NULL(40055,"Tax rate cannot be null",400),
    CODE_PRICE_PLAN_NOT_FOUND(40056,"Code price plan not found",400),
    PAYMENT_TERM_NOT_FOUND(40057,"Payment term must be 999 days or less",400),
    TEMPLATE_NOT_FOUND(40058, "Template Not Found", 400),
    PAYER__CANNOT_BE_DELETE_DUE_TO_PROCESSING_BILLS(40059, "Payer cannot be deleted due to processing bills", 400),


    LIMIT_LOGIN(40057,"Limit login",400),

    X_DEVICE_ID_HEADER_REQUIRED(40050, "Device ID header is missing", 400),
    INVALID_TOKEN(40051,"Invalid token",400),
    TOTAL_DISCOUNT_AMOUNT_MISMATCH(40052,"Total discount amount mismatch",400),
    TOTAL_TAX_AMOUNT_MISMATCH(40053,"Total tax amount mismatch",400),
    TOTAL_AMOUNT_MISMATCH(40054,"Total amount mismatch",400),
    CUSTOMER_EXIST(40058,"Customer already exist",400),
    DUPLICATE_MONTH_YEAR(40058, "Month and year cannot duplicate", 400),
    FLOOR_ROOM_TENANT_PHONE_EXIST(40059, "Floor-Room-Tenant-Period is duplicated", 400),
    BILL_CREATED_FAILED(40061,"Bill created failed",400),
    REGION_CODE_CANNOT_BE_NULL(40058,"Region Code cannot be null", 400),

    // 401 Unauthorized
    UNAUTHORIZED(40100, "Unauthorized", 401),
    BILL_REQUEST(40101, "Please wait PPCB approve your requested.", 401),
    TRANSACTION_ALREADY_MATCH(40918, "Transaction already matched", 409 ),
    FOLDER_ALREADY_EXIST(40190, "Folder already exists", 400),
    PASSWORD_MUST_MATCH(40102, "Password must match", 401),

    // 402 Payment Required
    BILL_REVIEW(40200, "Please wait PPCB approve your requested.", 402),

    // 403 Forbidden
    FORBIDDEN(40300, "You are not authorized to perform this action", 403),
    ADMIN_REJECT(40301, "Your request is being rejected.", 403),
    INVOICE_NO_EXIST(40302, "Bill No already exist", 403),
    INVOICE_NO_EXIST_NAME(40303, "Invoice no exist [{0}]", 403),
    IP_ADDRESS_NOT_ALLOWED(40302, "IP address is not allowed", 403),

    // 404 Not Found
    SERVICE_PLAN_ID_NOT_FOUND(40400, "Service Plan id is not found", 404),
    NOT_FOUND(40401, "Not Found", 404),
    VIRTUAL_ACCOUNT_NOT_FOUND(40402, "Virtual account not found", 404),
    VIRTUAL_ACCOUNT_ALREADY_EXIST(40403, "Virtual account already exist", 404),
    PARENT_ACCOUNT_NOT_FOUND(40403, "Parent account not found", 404),
    PARENT_ACCOUNT_NOT_FOUND_NAME(40404, "Parent account not found [{0}]", 404),
    BANK_ACCOUNT_NOT_FOUND(40405, "Bank account not found", 404),
    PARENT_ACCOUNT_NOT_TRUST(40406, "Parent account is not Trusted yet", 404),
    GROUP_NOT_FOUND(40407, "Group is not found [{0}]", 404),
    EMAIL_IS_NUll(40408, "Email is null", 404),
    PAYER_IS_DEACTIVATE(40409, "Payer is deactivated", 404),
    PAYER_IS_DEACTIVATE_NAME(40410, "Payer is deactivated [{0}]", 404),
    PHONE_IS_NUll(40411, "Phone is null", 404),
    TELEGRAM_ID_IS_NUll(40412, "Telegram Id is null", 404),
    ZALO_ID_IS_NUll(40413, "Zalo Id is null", 404),
    KAKAO_ID_IS_NUll(40413, "Kakao Id is null", 404),
    NOTIFICATION_TYPE_IS_FOUND(40414, "Notification Type is not found", 404),
    NOTIFICATION_NOT_FOUND(40415, "Notification is not found", 404),
    TRANSACTION_NOT_FOUND(40416, "Transaction not found", 404),
    PAYER_NOT_FOUND(40417, "Payer not found", 404),
    PAYER_ACCOUNT_NOT_FOUND(40418, "Payer account not found", 404),
    PAYER_ACCOUNT_ITEM_NOT_FOUND(40419, "Payer item not found", 404),
    BILL_NOT_FOUND(40420, "Bill is not found", 404),
    INVOICE_NOT_FOUND(40421, "Invoice is not found", 404),
    BANK_NOT_FOUND(40422, "Bank not found", 404),
    COMMON_CODE_NOT_FOUND(40423, "Common code not found [{0}]", 404),
    SERVICE_CODE_NOT_FOUNT(40424, "Service code not found", 404),
    UTILITY_RATE_NOT_FOUNT(40425, "Utility rate not found", 404),
    SECURITY_KEY_NOT_FOUND(40426, "Security key was not found", 404),
    SECURITY_CODE_NOT_FOUND(40427, "Security code was not found", 404),
    ITEM_NOT_FOUND(40428, "Item not found.", 404),
    ITEM_NOT_FOUND_NAME(40429, "Item Code not found [{0}]", 404),
    USERNAME_EXISTED(40430, "Username is already existed.", 404),
    USAGE_NAME_EXISTED(40431, "Usage name is already existed.", 404),
    BILL_RECURRING_NOT_FOUND(40432, "Bill recurring not found.", 404),
    EFILING_NOT_FOUND(40433, "E-Filing not found", 404),
    FLOOR_NOT_FOUND(40434, "Floor not found", 404),
    ROOM_NOT_FOUND(40435, "Room not found", 404),
    RESENT_LESS_MINUTE(40435, "Please resend it in a minute later", 404),
    UTILITY_USAGE_ITEM_NOT_FOUND(40436, "Utility usage item not found", 404),
    UTILITY_TYPE_NOT_FOUND(40437, "Utility type not found", 404),
    APARTMENT_NOT_FOUND(40438, "Apartment not found", 404),
    BASIC_USAGE_NOT_FOUND(40439, "Basic usage not found", 404),
    UTILITY_RATE_NOT_FOUND(40440, "Utility rate not found", 404),
    ROLE_NOT_FOUND(40441, "Role not found", 404),
    NOTIFY_METHOD_NOT_FOUND(40442, "Notify method not found.", 404),
    NO_BILL_ISSUED(40451, "No bill was issued.", 404),
    BANK_NOT_FOUND_BY_NAME(40443, "Bank not found [{0}]",404),
    CLIENT_ID_NOT_FOUND(40444, "Client id not found",404),
    ZALO_TEMPLATE_NOT_FOUND(40445, "Zalo template not found",404),
    BANK_NOT_ALLOW_SCRAP(40446, "The bank does not permit automated data scraping",404),
    TAX_CODE_NOT_FOUND(40046,"Tax code not found",404),
    FULL_NAME_IS_NULL(40444, "Full name is null", 404),
    GROUP_ID_IS_NULL(40445, "Group ID is null", 404),
    GROUP_NAME_IS_NULL(40446 , "Group Name is null", 404),
    PERSON_IN_CHARGE_NAME_IS_NULL(40447 , "Person In Charge Name is null", 404),
    PERSON_IN_CHARGE_REGION_CODE_IS_NULL(40448 , "Person In Charge Region Code is null", 404),
    ACCOUNT_NAME_IS_NULL(40449 , "Account Name is null", 404),
    NOTIFY_VALUE_NOT_FOUND(400450, "Notify value not found", 404),
    INVALID_PHONE_NUMBER_VIETNAM(400451, "Phone number must be a 10 or 11 digits starting with 0", 404),
    INVALID_PHONE_NUMBER_FOR(400451, "Phone number must be a 10 or 11 digits starting with 0 for {0} value", 404),
    INVALID_EMAIL_FORMAT(400451,"Invalid email", 404),
    NOTIFY_TYPE_NOT_FOUND(400452, "Notification Type is not found", 404),
    DUPLICATE_NOTIFY_TYPE(400453, "Duplication Notification Type", 400),

    FAQ_NOT_FOUND(40047,"FAQ content not found",404),
    NEWS_NOT_FOUND(40048,"News not found",404),
    CUSTOMER_NOT_FOUND(40059,"Customer not found",404),
    FOLDER_NOT_FOUND(40056,"Folder not found.",404),
    PRICING_ID_NOT_FOUND(404046, "Pricing id not found", 404),
    SMS_NOT_FOUND(404047, "Sms not found", 404),
    ELECTRICITY_LESS_THAN_PREVIOUS(40447,"Electricity must be greater than previous",404),
    MISSING_CUSTOMER_INFO(40448,"Missing Tenant name or phone",404),
    // 409 Conflict
    BILLER_EXIST(40900, "Biller already exist", 409),
    BILLER_ALREADY_SUBSCRIBE(40901, "Biller already subscribe", 409),
    USERNAME_EXIST(40902, "Username already exist", 409),
    GROUP_EXIST(40903, "Group is already exist", 409),
    FLOOR_EXIST(40904, "Floor is already exist", 409),
    ROOM_EXIST(40905, "Room is already exist", 409),
    ACCOUNT_NUMBER(40906, "Parent account is already exist", 409),
    PARENT_ACCOUNT_IS_ACTIVE(40907, "Parent account is being used", 409),
    UTILITY_RATE_EXIST(40908, "Utility rate is already exist", 409),
    SERVICE_TITLE_EXIST(40909, "Service title is already exist", 409),
    PAYER_EXIST(40910, "Payer is already exist", 409),
    ITEM_CODE_EXISTED(40911, "Item code is already existed", 409),
    FLOOR_CANNOT_DELETE(40912, "Floor cannot be deleted", 409),
    USER_ID_EXIST(40913, "User id already exist", 409),
    BILLER_DISABLED(40914, "Biller is disabled", 409),
    USER_ID_DISABLED(40915, "User id is disabled", 409),
    MOBILE_NUMBER_ALREADY_REGISTERED(40916, "Mobile number already registered", 409),
    USER_ID_ALREADY_IN_USE(40917, "User ID already in use", 409),
    USERNAME_OR_PHONE_EXIST(40918, "Username or phone number is already exist", 409),
    TAX_CODE_EXIST(40919, "Tax code is already exist", 409),
    TENANTNAME_OR_PHONE_EXIST(40920, "Tenant name or phone number is already exist", 409),
    // 452 Custom Client Errors
    BAD_CREDENTIALS(45200, "Password is incorrect", 452),
    SECRET_INVALID(45201, "Secret is incorrect", 452),
    CURRENT_PASSWORD(45202, "Current password is incorrect", 452),
    PASSWORD_INCORRECT(45203, "Password is incorrect", 452),
    NEW_PASSWORD(45204, "New password must be different from current password", 452),

    // 453 Custom Client Not Found
    USER_NOT_FOUND(45300, "User is not found", 453),
    USER_DISABLED(45301, "Biller account was deactivated. Please contact: 093 815 074", 453),
    CLIENT_NOT_FOUND(45302, "Client is not found", 453),

    // 333 Custom Error
    AMOUNT_NOT_ZERO(33300, "Amount must be greater than 0", 333),
    TOTAL_AMOUNT_IS_INCORRECT(33301, "Amount is incorrect", 333),

    // 502 Bad Gateway
    BAD_GATEWAY(50200, "Bad Gateway", 502),

    // 503 Service Unavailable
    SEND_OTP_FAILED(50300, "sent otp failed", 503),


    // 500 Internal Server Error
    AUTHENTICATION_FAILED(50000, "Authentication failed", 500);




    private final String message;
    private final int code;
    private final int httpCode;

    StatusCode(final int code, final String message, int httpCode) {

        this.message = message;
        this.code = code;
        this.httpCode = httpCode;
    }

    public String getMessage() {

        return this.message;

    }

    public int getCode() {

        return code;

    }

    public int getHttpCode() {

        return httpCode;

    }
}
