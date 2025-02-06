package explorableviz.transparenttext;

import it.unisa.cluelab.lllm.llm.LLMEvaluatorAgent;
import it.unisa.cluelab.lllm.llm.prompt.PromptList;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class AuthoringAssistant {

    public final Logger logger = Logger.getLogger(AuthoringAssistant.class.getName());
    private final PromptList prompts;
    private final LLMEvaluatorAgent llm;

    public AuthoringAssistant(LearningQueryContext learningQueryContext, String agentClassName) throws Exception {
        this.prompts = learningQueryContext.generateInContextLearningJSON();
        llm = initialiseAgent(agentClassName);
    }

    public String execute(QueryContext query) throws Exception {
        AtomicReference<String> response = new AtomicReference<>();

        int limit = Settings.getInstance().getLimit();
        // Initialize the agent
        // Add the input query to the KB that will be sent to the LLM
        PromptList sessionPrompt = (PromptList) prompts.clone();
        sessionPrompt.addPrompt(PromptList.USER, query.toUserPrompt());
        for (int attempts = 0; response.get() == null && attempts <= limit; attempts++) {
            logger.info(STR."Attempt #\{attempts}");
            // Send the query to the LLM to be processed
            String candidateResponse = llm.evaluate(prompts, "");
            logger.info(STR."Received response: \{candidateResponse}");

            sessionPrompt.addPrompt(PromptList.ASSISTANT, candidateResponse);
            // Validate the response
            query.validate(query.evaluate(candidateResponse)).ifPresentOrElse(value -> {
                try {
                    sessionPrompt.addPrompt(PromptList.USER, generateLoopBackMessage(candidateResponse, value));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, () -> response.set(candidateResponse));
        }
        if (response.get() == null) {
            logger.warning(STR."Expression validation failed after \{limit} attempts");
        } else {
            query.addExpressionToParagraph(response.get());
            logger.info(query.paragraphToString());
        }

        return response.get();
    }

   private LLMEvaluatorAgent initialiseAgent(String agentClassName) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        logger.info("Initializing agent: " + agentClassName);
        LLMEvaluatorAgent llmAgent;
        Class<?> agentClass = Class.forName(agentClassName);
        llmAgent = (LLMEvaluatorAgent) agentClass
                .getDeclaredConstructor(JSONObject.class)
                .newInstance(Settings.getInstance().getSettings());

        return llmAgent;
    }

    private String generateLoopBackMessage(String response, String errorDetails) {
        String errorMessage;
        if (errorDetails.toLowerCase().contains("key") && errorDetails.toLowerCase().contains("not found")) {
            errorMessage = String.format(
                    "KeyNotFound Error. The generated expression %s is trying to access a key that does not exist. " +
                            "Check the code and regenerate the expression. Remember: reply only with the expression, without any other comment.",
                    response
            );
        } else if (errorDetails.toLowerCase().contains("parseerror")) {
            errorMessage = String.format(
                    "SyntacticError. The generated expression %s caused the following error: \n%s. " +
                            "Check the code and regenerate the expression. Remember: reply only with the expression, without any other comment.",
                    response, errorDetails
            );
        } else {
            errorMessage = String.format(
                    "ValueMismatchError. The generated expression %s produced an unexpected value. " +
                            "Check the code and regenerate the expression. Remember: reply only with the expression, without any other comment.",
                    response
            );
        }
        return errorMessage;
    }
}
