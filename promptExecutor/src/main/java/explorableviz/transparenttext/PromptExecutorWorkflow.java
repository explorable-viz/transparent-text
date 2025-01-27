package explorableviz.transparenttext;

import it.unisa.cluelab.lllm.llm.LLMEvaluatorAgent;
import it.unisa.cluelab.lllm.llm.prompt.PromptList;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

public class PromptExecutorWorkflow {

    public final Logger logger = Logger.getLogger(PromptExecutorWorkflow.class.getName());
    private final PromptList prompts;
    private final QueryContext query;
    private final LLMEvaluatorAgent llm;

    public PromptExecutorWorkflow(LearningQueryContext learningQueryContext, QueryContext query, String agentClassName) throws Exception {
        this.query = query;
        this.prompts = learningQueryContext.generateInContextLearningJSON();
        llm = initialiseAgent(agentClassName);
    }


    /**
     * Executes a task with the provided input.
     *
     * @return the result of executing the task
     */

    public String execute() throws Exception {
        String response = null;
        /*
         * Load the maximum number of attempts for each query to process
         */
        int limit = Integer.parseInt(Settings.getInstance().get(Settings.LIMIT));
        // Initialize the agent
        // Add the input query to the KB that will be sent to the LLM
        prompts.addPrompt(PromptList.USER, query.toString());
        for (int attempts = 0; response == null && attempts <= limit; attempts++) {
            logger.info("Attempt #" + attempts);
            // Send the query to the LLM to be processed
            String candidateResponse = llm.evaluate(prompts, "");
            logger.info("Received response: " + candidateResponse);

            prompts.addPrompt(PromptList.SYSTEM, candidateResponse);
            query.setResponse(candidateResponse);
            // Validate the response
            ValidationResult validationResult = query.validate(); //validate(candidateResponse);
            if (!validationResult.isValid()) {
                //If it is not valid the wf generate a message that will be sent to the LLM
                //in order to try to correct the once previously generated.
                prompts.addPrompt(PromptList.USER, generateLoopBackMessage(candidateResponse, validationResult.getLog(), validationResult.getValue()));
            } else {
                response = candidateResponse;
            }
        }
        if (response == null) {
            logger.warning("Validation failed after " + limit + " attempts");
        }
        return response;
    }


    /**
     * This method initialise the LLMEvaluator agent dynamically
     *
     * @param agentClassName the agent to be loaded
     * @return the instance of the agent passed in input
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private LLMEvaluatorAgent initialiseAgent(String agentClassName) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        logger.info("Initializing agent: " + agentClassName);
        LLMEvaluatorAgent llmAgent;
        Class<?> agentClass = Class.forName(agentClassName);
        llmAgent = (LLMEvaluatorAgent) agentClass
                .getDeclaredConstructor(JSONObject.class)
                .newInstance(Settings.getInstance().getSettings());

        return llmAgent;
    }

    /**
     * Generate a Loopback message in order to describe to the LLM
     * the kind of error occurred and help them to regenerate the correct answer
     *
     * @param response : the previous response from the LLM
     * @return a String with a description of the error.
     */
    private String generateLoopBackMessage(String response, String log, String expected) {
        String errorMessage;
        if (log.toLowerCase().contains("key") && log.toLowerCase().contains("not found")) {
            errorMessage = String.format(
                    "KeyNotFound Error. The generated expression %s is trying to access a key that does not exist. " +
                            "Check the code and regenerate the expression for the value: %s. Remember: reply only with the expression, without any other comment.",
                    response, expected
            );
        } else if (log.toLowerCase().contains("parseerror")) {
            errorMessage = String.format(
                    "SyntacticError. The generated expression %s caused the following error: \n%s. " +
                            "Check the code and regenerate the expression for the value: %s. Remember: reply only with the expression, without any other comment.",
                    response, log, expected
            );
        } else {
            errorMessage = String.format(
                    "ValueMismatchError. The generated expression %s produced an unexpected value. " +
                            "Check the code and regenerate the expression for the value: %s. Remember: reply only with the expression, without any other comment.",
                    response, expected
            );
        }
        return errorMessage;
    }
}
