package org.kosign.chatbotapi.utilAI;

/**
 * Utility class for building AI prompts with structured formatting
 */
public class PromptBuilder {
    private StringBuilder prompt;
    private final String userQuery;

    public PromptBuilder(String userQuery) {
        this.userQuery = userQuery;
        this.prompt = new StringBuilder();
        prompt.append(String.format("You are a helpful AI assistant for PPC Bank Cambodia.\n\n"));
    }

    public PromptBuilder addSection(String title, String content) {
        prompt.append(String.format("""
                📚 **%s**:
                %s
                """, title, content));
        return this;
    }

    public PromptBuilder addLine(String line) {
        prompt.append(line);
        return this;
    }

    public PromptBuilder addConditionalTable(boolean condition, String type) {
        if (condition) {
            if ("Requirements".equals(type)) {
                prompt.append("""
                        📋 **Requirements**:
                        | Document | Validity | Purpose |
                        |----------|----------|---------|
                        | [Item] | [Period] | [Reason] |

                        """);
            } else if ("Amounts & Fees".equals(type)) {
                prompt.append("""
                        💰 **Amounts & Fees**:
                        | Type | Amount | Fee |
                        |------|--------|-----|
                        | [Type] | [Amount] | [Fee] |

                        """);
            } else if ("Interest Rates Fixed Deposit".equals(type)) {
                prompt.append("""
                        💰 **Interest Rates**:
                        | Terms | Standard Rate | Digital Channel | Interest KHR |
                        |--------|--------------|----------------|--------------|
                        | [Term] | [Rate] | [Rate USD] | [Rate KHR] |

                        """);
            } else if ("Interest Rates Loan".equals(type)) {
                prompt.append("""
                        💰 **Interest Rates**:
                        | Terms | Standard Rate | Digital Channel | Interest KHR |
                        |--------|--------------|----------------|--------------|
                        | [Term] | [Rate] | [Rate USD] | [Rate KHR] |
                        """);
            } else if ("Interest Rates Piggy Bank".equals(type)) {
                prompt.append("""
                        💰 **Interest Rates**:
                        | Terms | Interest USD | Interest KHR |
                        |--------|--------------|--------------|
                        | [Term] | [Rate] | [Rate] |
                        """);
            }
        }
        return this;
    }

    public PromptBuilder workflowMessage(Object data) {
        prompt.append(String.format(
            """
            You are an intelligent assistant. Analyze the following workflow data and provide a clear, structured summary:
    
            %s
    
            Please ensure your summary includes key actions, outcomes, and any notable issues or insights.
            """,
            data));
        return this;
    }
    

    public PromptBuilder addSmartJsonPrompt(String jsonData, String userQuery, String queryIntent) {
        // Check if we have valid data
        if (jsonData == null || jsonData.trim().isEmpty() || "null".equals(jsonData.trim()) || "[]".equals(jsonData.trim()) || "{}".equals(jsonData.trim())) {
            // No data available - provide helpful response without external search
            prompt.append(String.format(
                """
                        You are a helpful AI assistant for PPC Bank Cambodia.

                        A customer asked the following question:
                        "%s"

                        🔍 **Response Guidelines:**
                        I don't have specific information about this topic in my current database. Here's how I can help:

                        📞 **Direct Assistance:**
                        For the most accurate and up-to-date information about your inquiry, please:
                        - Visit your nearest PPC Bank branch
                        - Call our customer service hotline
                        - Use the PPC Bank mobile app
                        - Visit our official website

                        ✅ **What I Can Do:**
                        - Provide general banking guidance
                        - Help with common questions about PPC Bank services
                        - Direct you to the right resources for specific information

                        💡 **Important Note:**
                        I can only provide information based on what's available in my current database. For specific account details, current rates, or personalized assistance, our customer service team will be better equipped to help you.

                        Please feel free to ask me about general banking topics or let me know how else I can assist you!
                        """,
                userQuery));
        } else {
            // We have data - proceed with normal processing
            prompt.append(String.format(
                    """
                            You are a helpful AI assistant for PPC Bank Cambodia.

                            A customer asked the following question:
                            "%s"

                            Below is relevant information in JSON format from PPC Bank:
                            ---
                            %s
                            ---

                            🧠 **Intent of the question**: %s

                            Your job is to:
                            - Understand the user intent
                            - Extract ALL relevant information from the JSON data provided
                            - Write a helpful, professional, clear response
                            - Only use information that is available in the provided data
                            - Be comprehensive and include all important details
                            - Use plain formatting (e.g., **bold**, bullet points, short paragraphs)
                            - When presenting tables, include ALL rows of data, not just samples

                            📝 **Respond using this format**:

                            🎯 **Answer**
                            [Direct, descriptive answer for the user's question based on the available data]

                            📋 **Details**
                            - [List ALL key information from the provided data]
                            - [Extract ALL facts, rates, conditions, features available]
                            - [If there are tables, present ALL rows, not just examples]
                            - [Include complete information when available]

                            💡 **Tips**
                            - [1-2 useful tips related to the question based on available data]

                            🔢 **Steps** (if the question is procedural and data supports it)
                            1. [First step]
                            2. [Second step]
                            3. [Third step]

                            ❓ **Related Questions**
                            - What else would you like to know about this service?
                            - Do you need help with any other PPC Bank services?

                            ✅ **IMPORTANT INSTRUCTIONS:**
                            - Only use information from the provided data - do not make assumptions
                            - If specific details are missing from the data, acknowledge this limitation
                            - For missing information, advise the user to contact PPC Bank directly
                            - When showing rates or tables, include ALL available data points
                            - Do not truncate or summarize table data - show complete information
                            - Use bullet points and clear formatting for readability
                            - Do not search for or reference external sources
                            """,
                    userQuery, jsonData, queryIntent));
        }
        return this;
    }

    /**
     * Enhanced prompt for hybrid search results (internal + external data)
     * This method will be used when external search is implemented
     */
    public PromptBuilder addHybridSearchPrompt(String internalData, String externalData, String userQuery, String queryIntent) {
        prompt.append(String.format(
                """
                        You are a helpful AI assistant for PPC Bank Cambodia.

                        A customer asked the following question:
                        "%s"

                        🔍 **Search Mode**: Hybrid Search (Internal + External Sources)

                        📊 **Internal PPC Bank Data**:
                        ---
                        %s
                        ---

                        🌐 **External Reference Data** (for context only):
                        ---
                        %s
                        ---

                        🧠 **Intent of the question**: %s

                        Your job is to:
                        - **PRIORITIZE internal PPC Bank data** - this is the primary source
                        - Use external data only for additional context when internal data is incomplete
                        - Clearly indicate when information comes from external sources
                        - Write a helpful, professional, clear response
                        - Be comprehensive but prioritize PPC Bank specific information

                        📝 **Respond using this format**:

                        🎯 **Answer**
                        [Direct answer based primarily on PPC Bank data]

                        📋 **PPC Bank Details**
                        - [List ALL key information from internal PPC Bank data]
                        - [Include complete PPC Bank specific information]

                        🌐 **Additional Context** (if external data provides useful supplementary information)
                        - [Only include if it adds value and clearly mark as external reference]

                        💡 **Tips**
                        - [Focus on PPC Bank specific advice]

                        ✅ **IMPORTANT INSTRUCTIONS:**
                        - **Always prioritize PPC Bank internal data over external sources**
                        - Clearly label any external information as "reference" or "general banking"
                        - If internal data is sufficient, don't include external data
                        - For specific PPC Bank services, rates, or procedures, only use internal data
                        - Direct customers to PPC Bank for official information
                        """,
                userQuery, 
                internalData != null ? internalData : "No internal data available", 
                externalData != null ? externalData : "No external data available", 
                queryIntent));
        return this;
    }

    /**
     * Enhanced workflow prompt that intelligently parses database workflow data
     * and creates user-friendly transaction checking summaries
     */
    public PromptBuilder workflowPromptWithMetadata(Object workflowData, String userQuery, String queryIntent) {
        System.err.println("data: " + workflowData);
        try {
            // Parse the workflow data
            if (workflowData instanceof java.util.List<?> workflowList && !workflowList.isEmpty()) {
                Object firstWorkflow = workflowList.get(0);

                if (firstWorkflow instanceof org.kosign.chatbotapi.domains.Workflow workflow) {
                    String title = workflow.getTitle() != null ? workflow.getTitle() : "Service Guide";
                    String goalStatement = workflow.getGoalStatement() != null ? workflow.getGoalStatement() : "Let me help you with your banking needs.";

                    // Parse metadata steps
                    StringBuilder stepsBuilder = new StringBuilder();
                    if (workflow.getMetadata() != null) {
                        java.util.Map<String, Object> metadata = workflow.getMetadata();

                        // Sort step keys (step1, step2, etc.)
                        java.util.List<String> stepKeys = metadata.keySet().stream()
                            .filter(key -> key.toLowerCase().startsWith("step"))
                            .sorted()
                            .collect(java.util.stream.Collectors.toList());

                        for (String stepKey : stepKeys) {
                            Object stepData = metadata.get(stepKey);
                            if (stepData instanceof java.util.Map<?, ?> stepMap) {
                                String stepTitle = (String) stepMap.get("title");
                                String stepContent = (String) stepMap.get("content");

                                if (stepTitle != null || stepContent != null) {
                                    stepsBuilder.append("**").append(stepTitle != null ? stepTitle : "Step").append("**\n");
                                    if (stepContent != null) {
                                        stepsBuilder.append(stepContent).append("\n\n");
                                    }
                                }
                            }
                        }
                    }

                    // Build the final prompt with available data
                    prompt.append(String.format(
                        """
                                You are a helpful AI assistant for PPC Bank Cambodia.
                                
                                🧾 A customer asked: 
                                "%s"
                                
                                ➡️ Based on the available workflow data, generate a direct and informative response that meets their needs.
                                
                                🔍 **Workflow Title** 
                                **%s**
                                
                                🎯 **Goal** 
                                %s
                                
                                📥 **Steps to Help the Customer** 
                                %s
                                
                                💬 **Your Task** 
                                - Use only the information provided in the workflow data
                                - Transform the workflow information into a natural, easy-to-follow answer 
                                - Be friendly, professional, and clear 
                                - Use emojis, bullet points, and concise formatting where helpful 
                                - If the question relates to transactions, provide clear guidance through each step 
                                - Include any important next actions the customer should take based on available data
                                - If information is incomplete, direct them to contact PPC Bank for additional details
                                
                                ✅ Generate a helpful, user-friendly reply based only on the provided workflow information.
                                
                        """,
                        userQuery,
                        title,
                        goalStatement,
                        stepsBuilder.toString().trim().isEmpty() ? "I'm here to help you step by step with the available information!" : stepsBuilder.toString().trim()
                    ));
                }
            } else {
                // Fallback if no workflow data
                prompt.append(String.format(
                    """
                    You are a helpful AI assistant for PPC Bank Cambodia.
                    
                    A customer asked: "%s"
                    
                    🔍 **Current Status:**
                    I don't have specific workflow data available for this inquiry.
                    
                    📞 **How to Get Assistance:**
                    Please contact PPC Bank directly for detailed guidance:
                    - Visit your nearest branch
                    - Call our customer service hotline
                    - Use the PPC Bank mobile app
                    
                    💡 **General Support:**
                    I can help with general banking questions or direct you to the right resources.
                    """,
                    userQuery
                ));
            }
        } catch (Exception e) {
            // Error handling - provide fallback response without external search
            prompt.append(String.format(
                """
                You are a helpful AI assistant for PPC Bank Cambodia.
                
                A customer asked: "%s"
                
                🔍 **Status:**
                I'm experiencing difficulty accessing the specific information you need.
                
                📞 **Immediate Assistance:**
                For the best support with your inquiry, please contact PPC Bank directly:
                - Visit your nearest PPC Bank branch
                - Call our customer service hotline
                - Use the PPC Bank mobile app
                
                💡 **Alternative Help:**
                I can assist with general banking questions or help direct you to the right resources.
                
                Is there anything else I can help you with?
                """,
                userQuery
            ));
        }
        return this;
    }

    //test account blocked prompt verify
    public PromptBuilder testAccountBlockedPromptVerify(String userQuery) {
        // Extract actual values from user input
        String fullName = extractValue(userQuery, "full name", "name");
        String dateOfBirth = extractValue(userQuery, "date of birth", "dob");
        String nationalId = extractValue(userQuery, "national id", "id");
        String accountNumber = extractValue(userQuery, "account number", "account");
        String mobile = extractValue(userQuery, "mobile", "phone");
        
        prompt.append(String.format(
                """
                        ✅ Account Verification Successful!
                        
                        Thank you for providing your information. I've verified:

                        • Full Name: %s
                        • Date of Birth: %s  
                        • National ID: %s
                        • Account Number: %s
                        • Mobile: %s

                        🔒 **Next Steps:**
                        Your account verification is complete based on the provided information. 
                        For any additional account services or if you need further assistance, 
                        please contact PPC Bank customer service.

                        💡 **Need More Help?**
                        I'm here to assist with other banking questions or services.

                        User asked: "%s"
                        Generate only the response above, nothing more.
                        """,
                fullName.isEmpty() ? "[Not provided]" : fullName,
                dateOfBirth.isEmpty() ? "[Not provided]" : dateOfBirth,
                nationalId.isEmpty() ? "[Not provided]" : nationalId,
                accountNumber.isEmpty() ? "[Not provided]" : accountNumber,
                mobile.isEmpty() ? "[Not provided]" : mobile,
                userQuery));
        return this;
    }
    
    /**
     * Helper method to extract values from user input using multiple possible field names
     */
    private String extractValue(String text, String... fieldNames) {
        for (String fieldName : fieldNames) {
            // Pattern 1: Standard format "Field Name: Value" or "Field: Value" 
            String pattern1 = fieldName + ":\\s*(.+?)(?:\\n|$|,|\\s{2,})";
            String pattern2 = fieldName + "\\s*:\\s*(.+?)(?:\\n|$|,|\\s{2,})";
            
            // Pattern 3: Dash-separated format "Field Name: Value -" or "Field: Value -"
            String pattern3 = fieldName + ":\\s*(.+?)\\s*-\\s*(?:[A-Za-z]|$)";
            String pattern4 = fieldName + "\\s*:\\s*(.+?)\\s*-\\s*(?:[A-Za-z]|$)";
            
            java.util.regex.Pattern p1 = java.util.regex.Pattern.compile(pattern1, java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Pattern p2 = java.util.regex.Pattern.compile(pattern2, java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Pattern p3 = java.util.regex.Pattern.compile(pattern3, java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Pattern p4 = java.util.regex.Pattern.compile(pattern4, java.util.regex.Pattern.CASE_INSENSITIVE);
            
            // Try dash-separated patterns first (more specific)
            java.util.regex.Matcher m3 = p3.matcher(text);
            if (m3.find()) {
                return m3.group(1).trim();
            }
            
            java.util.regex.Matcher m4 = p4.matcher(text);
            if (m4.find()) {
                return m4.group(1).trim();
            }
            
            // Then try standard colon patterns
            java.util.regex.Matcher m1 = p1.matcher(text);
            if (m1.find()) {
                return m1.group(1).trim();
            }
            
            java.util.regex.Matcher m2 = p2.matcher(text);
            if (m2.find()) {
                return m2.group(1).trim();
            }
        }
        return "";
    }

    //test account blocked prompt check your account has been blocked
    public PromptBuilder testAccountHaveBlockedPromptCheck(String userQuery) {
        prompt.append(String.format(
                """
                        🔒 Account Status Alert
                        
                        Your account appears to be restricted.

                        📞 **Immediate Action Required:**
                        Please contact PPC Bank customer service immediately for assistance:
                        - Visit your nearest PPC Bank branch
                        - Call our customer service hotline
                        - Bring valid identification documents

                        ⚠️ **Important:**
                        Our customer service team will help you resolve this issue and restore normal account access.

                        User asked: "%s"
                        Generate only the response above, nothing more.
                        """,
                userQuery));
        return this;
    }

    public String build() {
        return prompt.toString();
    }
}