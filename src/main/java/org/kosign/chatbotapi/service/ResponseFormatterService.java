package org.kosign.chatbotapi.service;

import org.kosign.chatbotapi.payload.bakong.BakongTransactionResponse;
import org.kosign.chatbotapi.service.TransactionDataExtractionService.TransactionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for formatting responses and error messages
 */
@Service
public class ResponseFormatterService {

    private static final Logger logger = LoggerFactory.getLogger(ResponseFormatterService.class);

    public String formatTransactionResponse(BakongTransactionResponse response,
            TransactionAIService transactionAIService) {
        if (response == null) {
            return "Transaction not found. Please verify your details and try again.";
        }

        StringBuilder sb = new StringBuilder();

        if (response.getResponseMessage() != null) {
            sb.append("Status: ").append(response.getResponseMessage()).append("\n");
        }

        if (response.getFromAccountId() != null) {
            sb.append("Sender: ").append(response.getFromAccountId()).append("\n");
        }
        if (response.getToAccountId() != null) {
            sb.append("Recipient: ").append(response.getToAccountId()).append("\n");
        }
        if (response.getAmount() != null) {
            sb.append("Amount: ").append(response.getAmount());
            if (response.getCurrency() != null) {
                sb.append(" ").append(response.getCurrency());
            }
            sb.append("\n");
        }
        if (response.getDescription() != null) {
            sb.append("Description: ").append(response.getDescription()).append("\n");
        }
        if (response.getReceiverBank() != null) {
            sb.append("Bank: ").append(response.getReceiverBank()).append("\n");
        }

        String customMessage = transactionAIService.getCustomMessage();
        if (customMessage != null && !customMessage.isEmpty()) {
            sb.append("\n").append(customMessage);
        }

        return sb.toString();
    }

    public String generateErrorResponse(Exception e, String userQuery) {
        if (userQuery != null && !userQuery.isEmpty()) {
            if (userQuery.contains("loan") || userQuery.contains("credit")) {
                return "🏦 **Service Temporarily Unavailable**\n\n" +
                        "I'm experiencing technical difficulties while accessing loan and credit information. " +
                        "For immediate assistance with loans, please:\n\n" +
                        "📞 Call PPC Bank: +855 23 726 999\n" +
                        "🌐 Visit: www.ppcbank.com.kh\n" +
                        "🏢 Visit any PPC Bank branch\n\n" +
                        "I apologize for the inconvenience. Please try again in a few minutes.";
            } else if (userQuery.contains("account") || userQuery.contains("savings")) {
                return "🏦 **Service Temporarily Unavailable**\n\n" +
                        "I'm having trouble accessing account information right now. " +
                        "For immediate help with your account, please:\n\n" +
                        "📞 Call PPC Bank: +855 23 726 999\n" +
                        "🌐 Visit: www.ppcbank.com.kh\n" +
                        "🏢 Visit any PPC Bank branch\n\n" +
                        "Please try again in a few minutes.";
            }
        }

        return "❌ **I'm Sorry, Something Went Wrong**\n\n" +
                "I encountered a technical issue while processing your request. " +
                "This is temporary and should resolve shortly.\n\n" +
                "**For immediate assistance:**\n" +
                "📞 Call PPC Bank: +855 23 726 999\n" +
                "🌐 Visit: www.ppcbank.com.kh\n" +
                "🏢 Visit any PPC Bank branch\n\n" +
                "Please try rephrasing your question or contact PPC Bank support directly. " +
                "I apologize for the inconvenience.";
    }

    public String generateTimeoutResponse(String userQuery) {
        String lowerQuery = userQuery.toLowerCase();

        if (lowerQuery.contains("loan") || lowerQuery.contains("credit")) {
            return "⏰ **Response Timeout - Loan Information**\n\n" +
                    "I'm experiencing high processing load while accessing loan information. " +
                    "For immediate loan assistance:\n\n" +
                    "📞 **Call PPC Bank Loan Department**: +855 23 726 999\n" +
                    "🌐 **Visit**: www.ppcbank.com.kh/loans\n" +
                    "🏢 **Visit any PPC Bank branch**\n\n" +
                    "💡 **Tip**: Try asking a more specific question like 'What are the car loan rates?' " +
                    "or 'How do I apply for a home loan?'";
        } else if (lowerQuery.contains("account") || lowerQuery.contains("savings")) {
            return "⏰ **Response Timeout - Account Information**\n\n" +
                    "I'm experiencing high processing load while accessing account information. " +
                    "For immediate account assistance:\n\n" +
                    "📞 **Call PPC Bank Customer Service**: +855 23 726 999\n" +
                    "🌐 **Visit**: www.ppcbank.com.kh/accounts\n" +
                    "🏢 **Visit any PPC Bank branch**\n\n" +
                    "💡 **Tip**: Try asking a more specific question like 'What are savings account rates?' " +
                    "or 'How do I open an account?'";
        } else {
            return "⏰ **Response Timeout**\n\n" +
                    "I'm experiencing high processing load right now. Please try again in a moment " +
                    "or contact PPC Bank directly for immediate assistance:\n\n" +
                    "📞 **Call**: +855 23 726 999\n" +
                    "🌐 **Visit**: www.ppcbank.com.kh\n" +
                    "🏢 **Visit any PPC Bank branch**\n\n" +
                    "💡 **Tip**: Try asking a shorter, more specific question for faster response.";
        }
    }

    public String generatePartialDataResponse(TransactionData data, String extractedText) {
        StringBuilder response = new StringBuilder();
        response.append("🔍 **Transaction Receipt Detected**\n\n");
        response.append("I found some transaction details in your image:\n\n");

        if (data.hash != null) {
            response.append("• **Hash**: ").append(data.hash).append("\n");
        }
        if (data.amount != null) {
            response.append("• **Amount**: ").append(data.amount).append("\n");
        }
        if (data.currency != null) {
            response.append("• **Currency**: ").append(data.currency).append("\n");
        }

        response.append("\n**📝 Missing Information:**\n");
        if (data.hash == null) {
            response.append("• Transaction Hash/ID\n");
        }
        if (data.amount == null) {
            response.append("• Transaction Amount\n");
        }
        if (data.currency == null) {
            response.append("• Currency (USD or KHR)\n");
        }

        response.append("\n💬 **Want me to check this transaction?**\n");
        response.append("Please provide the missing details or upload a clearer image of your receipt.\n\n");

        response.append("**📄 Extracted Text:**\n");
        response.append("```\n").append(extractedText).append("\n```");

        return response.toString();
    }

    public String generateTransactionGuidanceResponse(String extractedText) {
        return """
                🧾 **Transaction Receipt Upload**

                I can see this appears to be a transaction-related image, but I couldn't extract the specific details needed for verification.

                **For transaction verification, I need:**
                • **Hash**: Transaction ID (like: c250339a)
                • **Amount**: Transaction amount (like: 50)
                • **Currency**: USD or KHR

                💡 **Tips for better results:**
                • Ensure the image is clear and well-lit
                • Make sure transaction details are fully visible
                • Try uploading a higher resolution image

                💬 **Alternative:** You can also provide the details manually:
                ```
                Hash: [your-transaction-hash]
                Amount: [amount]
                Currency: [USD/KHR]
                ```

                **📄 Extracted Text:**
                ```
                """
                + extractedText + """
                        ```

                        Would you like to try uploading another image or provide the details manually?
                        """;
    }

    public String generateGeneralImageResponse(String extractedText) {
        return """
                📄 **Image Text Extracted**

                Here's the text I found in your image:

                ```
                """ + extractedText + """
                ```

                💬 If this contains transaction details and you'd like me to verify a transaction, please let me know!

                I can help check transaction status if you provide:
                • Transaction Hash
                • Amount
                • Currency (USD or KHR)

                How can I assist you with this information?
                """;
    }
}
