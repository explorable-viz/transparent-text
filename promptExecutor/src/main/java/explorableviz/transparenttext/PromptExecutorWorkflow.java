package explorableviz.transparenttext;

import it.unisa.cluelab.lllm.llm.LLMEvaluatorAgent;
import it.unisa.cluelab.lllm.llm.prompt.PromptList;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PromptExecutorWorkflow {

    public final Logger logger = Logger.getLogger(PromptExecutorWorkflow.class.getName());
    private final PromptList prompts;
    private final QueryContext query;
    private final String template;
    private final LLMEvaluatorAgent llm;

    public PromptExecutorWorkflow(String promptPath, QueryContext query, String agentClassName, String fluidTemplatePath) throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        this.query = query;
        this.prompts = new PromptList();
        String content = new String(Files.readAllBytes(Paths.get(new File(promptPath).toURI())));
        JSONArray jsonContent = new JSONArray(content);
        prompts.parseJSONContent(jsonContent);
        llm = initialiseAgent(agentClassName);
        this.template = new String(Files.readAllBytes(Paths.get(new File(fluidTemplatePath).toURI())));;
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
            //@todo separate context to the rest of the query
            prompts.addPrompt(PromptList.SYSTEM, candidateResponse);

            // Validate the response
            ValidationResult validationResult = validate(candidateResponse);
            if (!validationResult.result) {
                //If it is not valid the wf generate a message that will be sent to the LLM
                //in order to try to correct the once previously generated.
                prompts.addPrompt(PromptList.USER, generateLoopBackMessage(candidateResponse, validationResult.log, validationResult.value));
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
     * Executes the validation task, generating a fluid program
     * and compiling it.
     *
     * @param response the input parameter for the task
     * @return null
     */
    public ValidationResult validate(String response) {

        String data = "";
        String code = query.getCode();
        String text = query.getFile();

        try {
            writeFluidFile(data, code, response);

            //Generate the fluid program that will be processed and evaluated by the compiler
            String tempFile = Settings.getInstance().get(Settings.FLUID_TEMP_FILE);
            String os = System.getProperty("os.name").toLowerCase();
            String bashPrefix = os.contains("win") ? "cmd.exe /c " : "";

            //Command construction
            StringBuilder command = new StringBuilder(bashPrefix + "yarn fluid evaluate -f " + tempFile);
            query.getDataset().forEach((key, path) -> {
                command.append(" -d \"(").append(key).append(", ").append(path).append(")\"");
            });
            query.getImports().forEach(path -> {
                command.append(" -i ").append(path);
            });
            logger.info("Running command: " + command);
            Process process = Runtime.getRuntime().exec(command.toString());
            process.waitFor();

            //Reading command output
            String output = new String(process.getInputStream().readAllBytes());
            String errorOutput = new String(process.getErrorStream().readAllBytes());

            logger.info("Command output: " + output);
            logger.info("Error output (if any): " + errorOutput);
            //Output validation
            return validateOutput(output, text);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error during validation", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    /**
     * Checks the validity of the given output against a specific pattern within a provided string.
     *
     * @param output the output to be validated
     * @param text   the string containing the pattern to match against the output
     * @return true if the output matches the pattern, false otherwise
     */
    //@todo return value is a triple
    private ValidationResult validateOutput(String output, String text) throws Exception {
        logger.info("Validating output: " + output);

        //Extract value from input query.text
        //The scenario [REPLACE value="SSP5-8.5"] framework foresees a considerable escalation in temperatures
        //Return: SSP5-8.5
        String regex = "value=\\\"(.*?)\\\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        if (!matcher.find()) {
            throw new Exception("No matching value found in text");
        }

        //expectedValue: SSP5-8.5
        String expectedValue = matcher.group(1);

        // Extract and clean the generated expression
        String[] outputLines = output.split("\n");
        if (outputLines.length < 2) {
            throw new Exception("Output format is invalid");
        }

        String value = outputLines[2].replaceAll("^\"|\"$", "");

        if (value.equals(expectedValue)) {
            logger.info("Validation passed");
            return new ValidationResult(true, value, expectedValue);
        }

        logger.info("Validation failed: generated=" + value + ", expected=" + expectedValue);
        return new ValidationResult(false, value, expectedValue);
    }

    /**
     * This function write the generated fluid code in a file
     *
     * @param data:     the data part of the fluid program
     * @param code:     the code part of the fluid program
     * @param response: the expression generated by the LLM
     * @throws FileNotFoundException
     */
    private void writeFluidFile(String data, String code, String response) throws FileNotFoundException {
        PrintWriter out = new PrintWriter(STR."fluid/example/\{Settings.getInstance().get(Settings.FLUID_TEMP_FILE)}.fld");
        String result = template.replaceAll("##CODE##", query.getCode());
        result = result.replaceAll("##EXPRESSION##", response);
        out.println(result);
        out.flush();
        out.close();
    }

    static class ValidationResult {
        private final String log, value;
        private final boolean result;
        ValidationResult(boolean result, String log, String value) {
            this.result = result;
            this.log = log;
            this.value = value;

        }


    }
}
