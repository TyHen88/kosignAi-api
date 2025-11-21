package org.kosign.chatbotapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for account verification and blocked account handling
 */
@Service
public class AccountVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(AccountVerificationService.class);

    public boolean containsAccountBlockedKeywords(String text) {
        String lowerText = text.toLowerCase();
        String[] keywords = {
                "blocked", "locked", "account", "block", "unlock", "password",
                "check", "status", "account", "how to unblock", "how to unlock",
                "cannot access", "can't access", "can't login", "cannot login",
                "how do i unblock", "how do i unlock", "help unlock", "help unblock",
                "how to check account", "check account block", "account blocked",
                "unlock account", "unblock account", "account unlock", "account unblock"
        };

        for (String keyword : keywords) {
            if (lowerText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAccountVerificationData(String text) {
        String lowerText = text.toLowerCase();

        boolean hasFullName = lowerText.contains("full name:") || lowerText.contains("name:");
        boolean hasDateOfBirth = lowerText.contains("date of birth:") || lowerText.contains("dob:");
        boolean hasNationalId = lowerText.contains("national id:") || lowerText.contains("id:");
        boolean hasAccountNumber = lowerText.contains("account number:") || lowerText.contains("account:");
        boolean hasMobile = lowerText.contains("mobile:") || lowerText.contains("phone:");

        int fieldCount = 0;
        if (hasFullName)
            fieldCount++;
        if (hasDateOfBirth)
            fieldCount++;
        if (hasNationalId)
            fieldCount++;
        if (hasAccountNumber)
            fieldCount++;
        if (hasMobile)
            fieldCount++;

        return fieldCount >= 3;
    }

    public AccountVerificationResult validateAccountVerificationData(String text) {
        String fullName = extractAccountValue(text, "full name", "name");
        String dateOfBirth = extractAccountValue(text, "date of birth", "dob");
        String nationalId = extractAccountValue(text, "national id", "id");
        String accountNumber = extractAccountValue(text, "account number", "account");
        String mobile = extractAccountValue(text, "mobile", "phone");

        List<String> missingFields = new ArrayList<>();
        List<String> invalidFields = new ArrayList<>();
        Map<String, String> invalidReasons = new HashMap<>();

        if (fullName.isEmpty()) {
            missingFields.add("Full Name");
        } else if (!isValidFullName(fullName)) {
            invalidFields.add("Full Name");
            invalidReasons.put("Full Name", getFullNameValidationError(fullName));
        }

        if (dateOfBirth.isEmpty()) {
            missingFields.add("Date of Birth");
        } else if (!isValidDateOfBirth(dateOfBirth)) {
            invalidFields.add("Date of Birth");
            invalidReasons.put("Date of Birth", getDateOfBirthValidationError(dateOfBirth));
        }

        if (nationalId.isEmpty()) {
            missingFields.add("National ID");
        } else if (!isValidNationalId(nationalId)) {
            invalidFields.add("National ID");
            invalidReasons.put("National ID", getNationalIdValidationError(nationalId));
        }

        if (accountNumber.isEmpty()) {
            missingFields.add("Account Number");
        } else if (!isValidAccountNumber(accountNumber)) {
            invalidFields.add("Account Number");
            invalidReasons.put("Account Number", getAccountNumberValidationError(accountNumber));
        }

        if (mobile.isEmpty()) {
            missingFields.add("Mobile");
        } else if (!isValidMobile(mobile)) {
            invalidFields.add("Mobile");
            invalidReasons.put("Mobile", getMobileValidationError(mobile));
        }

        boolean isValid = missingFields.isEmpty() && invalidFields.isEmpty();

        return new AccountVerificationResult(isValid, missingFields, invalidFields, invalidReasons,
                fullName, dateOfBirth, nationalId, accountNumber, mobile);
    }

    public String generateValidationErrorPrompt(AccountVerificationResult validationResult) {
        StringBuilder response = new StringBuilder();

        if (!validationResult.missingFields.isEmpty()) {
            response.append("❌ **Missing Required Information**\n\n");
            response.append("I cannot find the following required information:\n\n");

            for (String field : validationResult.missingFields) {
                response.append("• ").append(field).append("\n");
            }
            response.append("\n");
        }

        if (!validationResult.invalidFields.isEmpty()) {
            response.append("⚠️ **Invalid Information Found**\n\n");
            response.append("The following information appears to be invalid:\n\n");

            for (String field : validationResult.invalidFields) {
                String reason = validationResult.invalidReasons.get(field);
                response.append("• **").append(field).append("**: ").append(reason).append("\n");
            }
            response.append("\n");
        }

        response.append("💬 **Please provide the correct information in this format:**\n");
        response.append("```\n");
        response.append("Full Name: ").append(validationResult.fullName).append("\n");
        response.append("Date of Birth: ").append(validationResult.dateOfBirth).append("\n");
        response.append("National ID: ").append(validationResult.nationalId).append("\n");
        response.append("Account Number: ").append(validationResult.accountNumber).append("\n");
        response.append("Mobile: ").append(validationResult.mobile).append("\n");
        response.append("```\n\n");
        response.append("📝 **Example:**\n");
        response.append("```\n");
        response.append("Full Name: John Doe\n");
        response.append("Date of Birth: 10/11/2001\n");
        response.append("National ID: 160524688\n");
        response.append("Account Number: 1-120000127284-0\n");
        response.append("Mobile: 010297859\n");
        response.append("```\n\n");
        response.append("Once you provide all the correct information, I'll be able to verify your account.");

        return response.toString();
    }

    private String extractAccountValue(String text, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String pattern1 = fieldName + ":\\s*(.+?)(?:\\n|$|,|\\s{2,})";
            String pattern2 = fieldName + "\\s*:\\s*(.+?)(?:\\n|$|,|\\s{2,})";
            String pattern3 = fieldName + ":\\s*(.+?)\\s*-\\s*(?:[A-Za-z]|$)";
            String pattern4 = fieldName + "\\s*:\\s*(.+?)\\s*-\\s*(?:[A-Za-z]|$)";

            Pattern p1 = Pattern.compile(pattern1, Pattern.CASE_INSENSITIVE);
            Pattern p2 = Pattern.compile(pattern2, Pattern.CASE_INSENSITIVE);
            Pattern p3 = Pattern.compile(pattern3, Pattern.CASE_INSENSITIVE);
            Pattern p4 = Pattern.compile(pattern4, Pattern.CASE_INSENSITIVE);

            Matcher m3 = p3.matcher(text);
            if (m3.find()) {
                return m3.group(1).trim();
            }

            Matcher m4 = p4.matcher(text);
            if (m4.find()) {
                return m4.group(1).trim();
            }

            Matcher m1 = p1.matcher(text);
            if (m1.find()) {
                return m1.group(1).trim();
            }

            Matcher m2 = p2.matcher(text);
            if (m2.find()) {
                return m2.group(1).trim();
            }
        }
        return "";
    }

    private boolean isValidFullName(String fullName) {
        if (fullName.trim().length() < 2 || fullName.trim().length() > 100) {
            return false;
        }
        return fullName.matches("^[\\p{L}\\s\\-'\\.]+$");
    }

    private String getFullNameValidationError(String fullName) {
        if (fullName.trim().length() < 2) {
            return "Full name is too short (minimum 2 characters)";
        }
        if (fullName.trim().length() > 100) {
            return "Full name is too long (maximum 100 characters)";
        }
        if (!fullName.matches("^[\\p{L}\\s\\-'\\.]+$")) {
            return "Full name contains invalid characters (only letters, spaces, hyphens, apostrophes, and dots are allowed)";
        }
        return "Invalid full name format";
    }

    private boolean isValidDateOfBirth(String dateOfBirth) {
        String cleaned = dateOfBirth.replaceAll("[/-]", "");

        if (!cleaned.matches("\\d{8}")) {
            return false;
        }

        try {
            int day = Integer.parseInt(cleaned.substring(0, 2));
            int month = Integer.parseInt(cleaned.substring(2, 4));
            int year = Integer.parseInt(cleaned.substring(4, 8));

            if (day < 1 || day > 31)
                return false;
            if (month < 1 || month > 12)
                return false;
            if (year < 1900 || year > 2010)
                return false;

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String getDateOfBirthValidationError(String dateOfBirth) {
        String cleaned = dateOfBirth.replaceAll("[/-]", "");

        if (!cleaned.matches("\\d{8}")) {
            return "Date format invalid. Use DD/MM/YYYY, DD-MM-YYYY, or DDMMYYYY (e.g., 10/11/2001 or 10112001)";
        }

        try {
            int day = Integer.parseInt(cleaned.substring(0, 2));
            int month = Integer.parseInt(cleaned.substring(2, 4));
            int year = Integer.parseInt(cleaned.substring(4, 8));

            if (day < 1 || day > 31) {
                return "Invalid day: " + day + " (must be between 1-31)";
            }
            if (month < 1 || month > 12) {
                return "Invalid month: " + month + " (must be between 1-12)";
            }
            if (year < 1900 || year > 2010) {
                return "Invalid year: " + year + " (must be between 1900-2010)";
            }
        } catch (NumberFormatException e) {
            return "Date contains non-numeric characters";
        }

        return "Invalid date of birth format";
    }

    private boolean isValidNationalId(String nationalId) {
        String cleaned = nationalId.replaceAll("[\\s-]", "");
        return cleaned.matches("\\d{9,12}");
    }

    private String getNationalIdValidationError(String nationalId) {
        String cleaned = nationalId.replaceAll("[\\s-]", "");

        if (!cleaned.matches("\\d+")) {
            return "National ID must contain only numbers (spaces and hyphens are allowed for formatting)";
        }
        if (cleaned.length() < 9) {
            return "National ID is too short (minimum 9 digits)";
        }
        if (cleaned.length() > 12) {
            return "National ID is too long (maximum 12 digits)";
        }
        return "Invalid National ID format";
    }

    private boolean isValidAccountNumber(String accountNumber) {
        String cleaned = accountNumber.replaceAll("[\\s-]", "");
        return cleaned.matches("\\d{10,20}");
    }

    private String getAccountNumberValidationError(String accountNumber) {
        String cleaned = accountNumber.replaceAll("[\\s-]", "");

        if (!cleaned.matches("\\d+")) {
            return "Account number must contain only numbers (spaces and hyphens are allowed for formatting)";
        }
        if (cleaned.length() < 10) {
            return "Account number is too short (minimum 10 digits)";
        }
        if (cleaned.length() > 20) {
            return "Account number is too long (maximum 20 digits)";
        }
        return "Invalid account number format";
    }

    private boolean isValidMobile(String mobile) {
        String cleaned = mobile.replaceAll("[\\s\\-\\+]", "");
        return cleaned.matches("\\d{8,15}");
    }

    private String getMobileValidationError(String mobile) {
        String cleaned = mobile.replaceAll("[\\s\\-\\+]", "");

        if (!cleaned.matches("\\d+")) {
            return "Mobile number must contain only numbers (spaces, hyphens, and + are allowed for formatting)";
        }
        if (cleaned.length() < 8) {
            return "Mobile number is too short (minimum 8 digits)";
        }
        if (cleaned.length() > 15) {
            return "Mobile number is too long (maximum 15 digits)";
        }
        return "Invalid mobile number format";
    }

    /**
     * Helper class to hold account verification validation results
     */
    public static class AccountVerificationResult {
        public final boolean isValid;
        public final List<String> missingFields;
        public final List<String> invalidFields;
        public final Map<String, String> invalidReasons;
        public final String fullName;
        public final String dateOfBirth;
        public final String nationalId;
        public final String accountNumber;
        public final String mobile;

        public AccountVerificationResult(boolean isValid, List<String> missingFields, List<String> invalidFields,
                Map<String, String> invalidReasons, String fullName, String dateOfBirth,
                String nationalId, String accountNumber, String mobile) {
            this.isValid = isValid;
            this.missingFields = missingFields;
            this.invalidFields = invalidFields;
            this.invalidReasons = invalidReasons;
            this.fullName = fullName;
            this.dateOfBirth = dateOfBirth;
            this.nationalId = nationalId;
            this.accountNumber = accountNumber;
            this.mobile = mobile;
        }
    }
}
