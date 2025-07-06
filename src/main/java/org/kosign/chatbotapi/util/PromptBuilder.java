package org.kosign.chatbotapi.util;

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

    public PromptBuilder addSmartJsonPrompt(String jsonData, String userQuery, String queryIntent) {

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
                        - Extract ALL relevant information from the JSON data
                        - Write a helpful, professional, clear response
                        - Do not invent or assume anything not in the data
                        - Be comprehensive and include all important details
                        - Use plain formatting (e.g., **bold**, bullet points, short paragraphs)
                        - When presenting tables, include ALL rows of data, not just samples

                        📝 **Respond using this format**:

                        🎯 **Answer**
                        [Direct, descriptive answer for the user's question based on the data and generate interesting response based on the query intent]

                        📋 **Details**
                        - [List ALL key information from the data]
                        - [Extract ALL facts, rates, conditions, features]
                        - [If there are tables, present ALL rows, not just examples]
                        - [Include complete interest rate schedules when available]

                        💡 **Tips**
                        - [1-2 useful tips related to the question]

                        🔢 **Steps** (if the question is procedural)
                        1. [First step]
                        2. [Second step]
                        3. [Third step]

                        ❓ **Related Questions**
                        - What are the requirements for ...?
                        - How long does it take to ...?

                        ✅ **IMPORTANT INSTRUCTIONS:**
                        - Be honest — if data is missing, say so and advise the user to contact PPC Bank directly
                        - When showing interest rates or tables, include ALL available data points
                        - Do not truncate or summarize table data - show complete information
                        - Use bullet points and clear formatting for readability
                        """,
                userQuery, jsonData, queryIntent));
        return this;
    }

    public  PromptBuilder workflowPrompt(String contentData, String userQuery, String queryIntent) {
        prompt.append(String.format(
                """
                        You are a PPC Bank customer service assistant specializing in problem resolution.
                        The user has encountered a specific issue that requires a solution.
                        
                        **USER QUERY:** %s
                        
                        **QUERY INTENT:** %s
                        
                        **AVAILABLE WORKFLOW SOLUTIONS:**
                        %s
                        
                        **INSTRUCTIONS:**
                        1. Analyze the user's problem carefully
                        2. Identify the most relevant workflow solution from the available options
                        3. Provide clear, step-by-step guidance based on the workflow
                        4. Use a helpful, empathetic tone
                        5. Include contact information for further assistance if needed
                        
                        **RESPONSE FORMAT:**
                        - Acknowledge the user's issue
                        - Provide the solution steps clearly
                        - Offer additional support options
                        
                        Please provide a comprehensive solution to help resolve the user's issue.
                """,
                userQuery, queryIntent, contentData));
        return this;
    }
    

    /**
     * Adds a workflow-specific prompt for problem-solving queries
     */
    public PromptBuilder addWorkflowSolutionPrompt(String workflowData, String userQuery, String category) {
        prompt.append(String.format(
                """
                        
                        **WORKFLOW SOLUTION GUIDANCE**
                        
                        You are helping a PPC Bank customer resolve a specific issue in the **%s** category.
                        
                        **Customer Problem:** %s
                        
                        **Pre-configured Solutions Available:**
                        %s
                        
                        **Your Task:**
                        1. 🔍 **Analyze** the customer's specific problem
                        2. 🎯 **Match** it to the most relevant workflow solution
                        3. 📋 **Provide** clear, actionable steps
                        4. 🤝 **Offer** additional support options
                        
                        **Response Guidelines:**
                        - Start with empathy and understanding
                        - Break down solutions into clear steps
                        - Use bullet points for easy reading
                        - Include timeframes where applicable
                        - Provide escalation paths for complex issues
                        
                        **Contact Information to Include:**
                        - Phone: +855 23 726 999 (24/7 support)
                        - Website: www.ppcbank.com.kh
                        - Branch locations for in-person assistance
                        
                """,
                category, userQuery, workflowData));
        return this;
    }

    /**
     * Adds general information prompt for tb_ppcb_ban queries
     */
    public PromptBuilder addGeneralInfoPrompt(String bankData, String userQuery, String queryIntent) {
        prompt.append(String.format(
                """
                        
                        **GENERAL BANK INFORMATION GUIDANCE**
                        
                        You are providing general information about PPC Bank services and products.
                        
                        **Customer Inquiry:** %s
                        **Query Type:** %s
                        
                        **Available Bank Information:**
                        %s
                        
                        **Your Task:**
                        1. 📚 **Extract** relevant information from the bank data
                        2. 🎯 **Focus** on what the customer specifically asked about
                        3. 📝 **Present** information in a clear, organized manner
                        4. 💡 **Suggest** related services that might interest them
                        
                        **Response Guidelines:**
                        - Use clear, professional language
                        - Organize information with headers and bullet points
                        - Include specific details like rates, fees, and requirements
                        - Mention any special offers or promotions
                        - Provide next steps for interested customers
                        
                """,
                userQuery, queryIntent, bankData));
        return this;
    }

    /**
     * Adds a payment transaction workflow prompt for payment-specific queries
     */
    public PromptBuilder addPaymentWorkflowPrompt(String workflowData, String userQuery, String category) {
        prompt.append(String.format(
                """
                        
                        **PAYMENT TRANSACTION WORKFLOW GUIDANCE**
                        
                        You are helping a PPC Bank customer resolve a payment transaction issue in the **%s** category.
                        
                        **Customer Payment Issue:** %s
                        
                        **Available Payment Solutions:**
                        %s
                        
                        **Your Task:**
                        1. 🔍 **Analyze** the customer's specific payment transaction problem
                        2. 🎯 **Match** it to the most relevant workflow solution from the bank's database
                        3. 📋 **Provide** clear, step-by-step payment resolution guidance
                        4. 💳 **Include** transaction status checking options if applicable
                        5. 🤝 **Offer** multiple support channels for urgent issues
                        
                        **Payment Response Guidelines:**
                        - Start with empathy about their payment issue
                        - Break down payment solutions into actionable steps
                        - Use bullet points and numbered lists for clarity
                        - Include specific timeframes for payment processing
                        - Mention transaction tracking options
                        - Provide escalation paths for failed payments
                        - Include both digital and physical support options
                        
                        **Payment Support Information:**
                        - 24/7 Payment Hotline: +855 23 726 999
                        - Online Banking: www.ppcbank.com.kh
                        - Mobile App: PPC Bank Mobile
                        - Branch locations for in-person payment assistance
                        - Transaction status checking via hash/reference number
                        
                        **If Transaction Details Are Needed:**
                        Guide the customer to provide: Transaction Hash, Amount, and Currency (USD/KHR)
                        for direct transaction status verification.
                        
                """,
                category, userQuery, workflowData));
        return this;
    }

    public String build() {
        return prompt.toString();
    }
}