package explorableviz.transparenttext;

import it.unisa.cluelab.lllm.llm.LLMEvaluatorAgent;
import it.unisa.cluelab.lllm.llm.prompt.PromptList;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
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
        String response = null;
        int limit = Settings.getInstance().getLimit();
        // Add the input query to the KB that will be sent to the LLM
        PromptList sessionPrompt = (PromptList) prompts.clone();
        sessionPrompt.addPrompt(PromptList.USER, query.toUserPrompt());
        for (int attempts = 0; response == null && attempts <= limit; attempts++) {
            logger.info(STR."Attempt #\{attempts}");
            // Send the query to the LLM to be processed
            String candidateExpr = llm.evaluate(prompts, "");
            logger.info(STR."Received response: \{candidateExpr}");

            Optional<String> result = query.validate(query.evaluate(candidateExpr));
            if(result.isPresent()) {
                //Add the prev. expression to the SessionPrompt to say to the LLM that the response is wrong.
                sessionPrompt.addPrompt(PromptList.ASSISTANT, candidateExpr);
                sessionPrompt.addPrompt(PromptList.USER, generateLoopBackMessage(candidateExpr, result.get()));
            } else {
                response = (candidateExpr);
            }
        }
        if (response == null) {
            logger.warning(STR."Expression validation failed after \{limit} attempts");
        } else {
            query.addExpressionToParagraph(response);
            logger.info(query.paragraphToString());
        }

        return response;
    }

   private LLMEvaluatorAgent initialiseAgent(String agentClassName) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        logger.info(STR."Initializing agent: \{agentClassName}");
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
