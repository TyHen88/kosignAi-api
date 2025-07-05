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

    public PromptBuilder addDirectAnswerSection() {
        prompt.append("🎯 **Answer**: [Provide a direct, concise answer to the user's question]\n\n");
        return this;
    }

    public PromptBuilder addDetailsSection() {
        prompt.append("📋 **Details**: [Provide deeper information, context, and comprehensive explanation]\n\n");
        return this;
    }

    public PromptBuilder addTipsSection() {
        prompt.append("💡 **Tips**: [Provide helpful notes, best practices, and important considerations]\n\n");
        return this;
    }

    public PromptBuilder addSummarySection() {
        prompt.append("📝 **Summary**: [Provide a concise overview of the key points]\n\n");
        return this;
    }

    public PromptBuilder addStepsSection() {
        prompt.append("🔢 **Steps**: [Provide step-by-step instructions if applicable]\n\n");
        return this;
    }

    public PromptBuilder addRelatedQuestionsSection() {
        prompt.append("❓ **Related Questions**: [Suggest 2-3 additional questions the user might ask]\n\n");
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

    public PromptBuilder addImportantNotes() {
        prompt.append("💡 **Important**: [Critical considerations]\n");
        return this;
    }

    public PromptBuilder addNextSteps() {
        prompt.append("🔗 **Next Steps**: [Clear actions to take]\n");
        return this;
    }

    public PromptBuilder addFollowUpQuestions() {
        prompt.append("❓ **Related Questions**: [2-3 relevant follow-ups]\n\n");
        return this;
    }

    public PromptBuilder addGeneralBankingInstructions() {
        prompt.append("""
                INSTRUCTIONS:
                - Provide helpful general banking guidance
                - Include: "Contact PPC Bank for specific requirements"
                - Use professional, warm tone
                """);
        return this;
    }

    public PromptBuilder addContextualInstructions(String context) {
        prompt.append(String.format(
                """
                        📋 **PPC BANK INFORMATION:**
                        %s

                        🎯 **RESPONSE INSTRUCTIONS:**
                        - Use the provided PPC Bank information as your PRIMARY and AUTHORITATIVE source
                        - Be specific, accurate, and detailed based on this verified data
                        - If the information seems incomplete, clearly state what might be missing
                        - Structure your response with clear sections using the emojis provided
                        - Include specific requirements, amounts, or procedures when available
                        - Use tables for document requirements, fees, or comparison data
                        - Maintain a professional yet friendly and helpful tone
                        - If asked about services not covered in the data, acknowledge the limitation and suggest contacting PPC Bank directly

                        🔍 **DOMAIN CONTEXT:**
                        - Focus on the specific banking domain detected in the user's query
                        - Provide comprehensive information within that domain
                        - Cross-reference related services when relevant
                        - Include important disclaimers and conditions
                        """,
                context));
        return this;
    }

    public PromptBuilder addJsonContextInstructions(String jsonContext) {
        prompt.append(String.format("""
                📊 **STRUCTURED DATA FROM PPC BANK:**
                %s

                🎯 **AI ANALYSIS INSTRUCTIONS:**
                - Analyze the provided JSON data structure carefully
                - Extract relevant information based on the user's specific question
                - Transform the JSON data into natural, human-readable language
                - Focus on the most relevant parts of the data for this query
                - If the JSON contains tables, present them in a clear format
                - If the JSON contains lists, organize them logically
                - Maintain accuracy - don't add information not present in the JSON
                - If data seems incomplete, acknowledge what might be missing

                📋 **RESPONSE STRUCTURE REQUIRED:**
                Use the following format for your response:

                🎯 **Answer**: [Direct answer based on the JSON data]

                📋 **Details**: [Comprehensive information extracted from the JSON]

                💡 **Tips**: [Helpful notes and considerations from the data]

                📝 **Summary**: [Concise overview of key points]

                🔢 **Steps**: [Step-by-step instructions if the data contains procedures]

                ❓ **Related Questions**: [2-3 questions users might ask based on this topic]

                🔍 **QUALITY GUIDELINES:**
                - Be conversational and helpful while maintaining professionalism
                - Use bullet points and clear formatting for readability
                - Include specific details from the JSON when available
                - Suggest contacting PPC Bank for information not covered in the data
                """, jsonContext));
        return this;
    }

    public PromptBuilder addExternalSearchInstructions() {
        prompt.append(
                """
                        INSTRUCTIONS:
                        - The internal search for PPC Bank information did not return a specific answer.
                        - Your task is to now act as a general, helpful AI assistant.
                        - Use your broad knowledge and search capabilities to find the best possible answer to the user's question.
                        - **Do NOT invent information about PPC Bank.**
                        - If the user's question was about a general topic (e.g., "what is a loan?"), answer it comprehensively.
                        - If the user's question was specifically about PPC Bank (e.g., "what are PPC Bank's car loan rates?"), you must state that you could not find specific information on the PPC Bank website, but you can provide general information on the topic. Then, provide that general information.
                        """);
        return this;
    }

    // prompt for tell ai to make step by step response

    public PromptBuilder addStepByStepInstructions() {
        prompt.append("""
                📝 **STEP-BY-STEP RESPONSE FORMAT:**
                When providing instructions or procedures, use this format:

                **Step 1:** [Clear action with specific details]
                **Step 2:** [Next action with requirements]
                **Step 3:** [Final action with expected outcome]

                For complex procedures, include:
                - Required documents at each step
                - Estimated timeframes
                - Potential issues to watch for
                - Alternative options when available

                """);
        return this;
    }

    /**
     * Builds a comprehensive prompt for AI-powered JSON analysis
     */
    public PromptBuilder addComprehensiveJsonAnalysisInstructions(String jsonData, String queryIntent) {
        prompt.append(String.format("""
                🤖 **PPC BANK DATA ANALYSIS**

                📊 **BANK INFORMATION:**
                %s

                🎯 **USER QUERY INTENT:** %s

                📋 **INSTRUCTIONS:**
                - Extract ONLY the most relevant information from the provided bank data
                - Answer the user's question directly and clearly
                - Use ONLY the information provided - do not add external information
                - If specific details are missing, state "Please contact PPC Bank for specific details"
                - Organize your response using the exact format below

                🎨 **REQUIRED RESPONSE FORMAT:**

                🎯 **Answer**
                [Write a direct, clear answer to the user's question using the bank data]

                📋 **Details**
                [List specific details from the bank data such as:
                • Loan amounts, interest rates, terms
                • Requirements or eligibility criteria
                • Fees or charges
                • Important conditions]

                💡 **Tips**
                [Provide 2-3 helpful tips based on the bank data, such as:
                • What to prepare before applying
                • How to improve approval chances
                • Important things to consider]

                📝 **Summary**
                [Write 1-2 sentences summarizing the key points]

                🔢 **Steps** (only if the query involves a process)
                [If applicable, list step-by-step instructions:
                1. [First step]
                2. [Second step]
                3. [Third step]]

                ❓ **Related Questions**
                [Suggest 2-3 related questions users might ask]

                🔍 **QUALITY RULES:**
                - Be concise and avoid repetition
                - Use bullet points for clarity
                - Include specific numbers/amounts when available
                - Maintain professional but friendly tone
                - End with "Contact PPC Bank for personalized assistance"
                """, jsonData, queryIntent));
        return this;
    }

    /**
     * Adds instructions for handling multiple match results
     */
    public PromptBuilder addMultipleMatchInstructions(int resultCount) {
        prompt.append(String.format("""
                �� **MULTIPLE RESULTS AVAILABLE:**
                You have %d matching results from PPC Bank's database.

                **PROCESSING RULES:**
                - Use the primary result as your main source
                - Supplement with additional information from other results if relevant
                - Combine information logically, not by match order
                - If there are conflicting details, use the highest confidence match
                - Focus on providing one clear, comprehensive answer
                """, resultCount));
        return this;
    }

    public PromptBuilder addSmartJsonPrompt(String jsonData, String userQuery, String queryIntent) {
        System.err.println("🔍 jsonData: " + jsonData);
        System.err.println("🔍 userQuery: " + userQuery);
        System.err.println("🔍 queryIntent: " + queryIntent);
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

    public PromptBuilder addExternalFallbackInstructions() {
        prompt.append("""
                🕵️ **No Bank Info Found**

                Your task:
                - The internal search did not find any matching PPC Bank data
                - Provide general helpful information on the topic based on your own knowledge
                - If user asked for something specific to PPC Bank, clearly say that and give general info instead
                - Always encourage the user to contact PPC Bank for official answers
                """);
        return this;
    }

    public PromptBuilder addContextFollowupInstructions(String userQuery, String previousContextJson) {
        prompt.append(String.format(
                """
                        🤖 You are PPC Bank's intelligent assistant. You have access to previously retrieved context data from the user's earlier queries.

                        📋 **USER’S QUESTION:**
                        "%s"

                        📂 **PREVIOUS CONTEXT DATA:**
                        %s

                        🎯 **YOUR TASK:**
                        - Refer to the previous context data to answer the user’s follow-up question.
                        - If the user's question asks for more detail, clarification, or a different angle, reuse relevant parts of the context and highlight what’s new.
                        - If the user is continuing a topic, maintain continuity by summarizing only relevant information from the prior context.

                        🔍 **RESPONSE REQUIREMENTS:**
                        - Avoid re-listing all previous information unless specifically asked.
                        - Clearly explain how this new question relates to the previous one.
                        - Focus on the most relevant information from the stored context for this specific follow-up.
                        - Maintain structured response sections with clarity.

                        🎯 **Answer**
                        [Give a direct answer using the prior context as your base.]

                        📋 **Details**
                        [Highlight or repeat only relevant facts from the previous result.]

                        💡 **Context Summary**
                        [Briefly summarize what the previous context was about and how it connects to the new question.]

                        🔁 **Clarification (if applicable)**
                        [If the user’s follow-up is unclear, state what information is missing or needed.]

                        ❓ **Related Follow-Ups**
                        [Suggest next possible questions the user might ask based on both the previous and current conversation.]

                        👔 **Tone**
                        - Use a professional, friendly tone.
                        - Avoid repeating full previous responses.
                        - Clarify only what’s necessary and useful.
                        """,
                userQuery, previousContextJson));
        return this;
    }

    public String build() {
        return prompt.toString();
    }
}