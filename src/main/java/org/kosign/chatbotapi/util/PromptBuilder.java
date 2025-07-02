package org.kosign.chatbotapi.util;

/**
 * Utility class for building AI prompts with structured formatting
 */
public class PromptBuilder {
    private StringBuilder prompt;
    private String userQuery;

    public PromptBuilder(String userQuery) {
        this.userQuery = userQuery;
        this.prompt = new StringBuilder();
        prompt.append(String.format("You are PPC Bank's AI assistant. User asks: \"%s\"\n\n", userQuery));
    }

    public PromptBuilder addDirectAnswerSection() {
        prompt.append("🎯 **Direct Answer**: [Answer the question immediately]\n\n");
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
        prompt.append(String.format("""
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
                """, context));
        return this;
    }
    
    public PromptBuilder addExternalSearchInstructions() {
        prompt.append("""
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

    //prompt for tell ai to make step by step response
    
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

    public String build() {
        return prompt.toString();
    }
} 