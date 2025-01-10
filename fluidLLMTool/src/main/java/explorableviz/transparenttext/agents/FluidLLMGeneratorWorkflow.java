package explorableviz.transparenttext.agents;

import it.unisa.cluelab.lllm.llm.LLMEvaluatorAgent;
import it.unisa.cluelab.lllm.llm.prompt.PromptList;
import org.json.JSONArray;
import org.json.JSONObject;
import explorableviz.transparenttext.Settings;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FluidGeneratorAgent {

    private PromptList prompts;
    private String originalQuery;
    private final String template = Settings.getInstance().get(Settings.FLUID_TEMPLATE);
    private String log;
    private String expected;

    public FluidGeneratorAgent(String promptPath, String query) throws IOException {
        this.originalQuery = query;
        initialise(promptPath);
    }
    public static Logger logger = Logger.getLogger(FluidGeneratorAgent.class.getName());

    public void initialise(String promptPath) throws IOException {
        this.prompts = new PromptList();
        String content = new String(Files.readAllBytes(Paths.get(new File(promptPath).toURI())));
        JSONObject jsonContent = new JSONObject(content);
        prompts.parseJSONContent((JSONArray) jsonContent.get("prompts"));
    }

    /**
     * Executes a task with the provided input.
     *
     * @param agentClassName the input parameter for the task
     * @return the result of executing the task
     */

    private LLMEvaluatorAgent initialiseAgent(String agentClassName) {
        logger.info("Initializing agent: " + agentClassName);
        LLMEvaluatorAgent llmAgent;
        try {
            Class<?> agentClass = Class.forName(agentClassName);
            llmAgent = (LLMEvaluatorAgent) agentClass
                    .getDeclaredConstructor(JSONObject.class)
                    .newInstance(Settings.getInstance().getSettings());
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Error: class not found", e);
            return null;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during execution", e);
            return null;
        }
        return llmAgent;
    }

    public String execute(String agentClassName) {
        LLMEvaluatorAgent llm;
        String response = "";

        try {
            // Initialize the agent
            llm = initialiseAgent(agentClassName);

            // Prepare prompts
            prompts.addPrompt(PromptList.USER, originalQuery);

            int limit = Integer.parseInt(Settings.getInstance().get(Settings.LIMIT));
            boolean isValid = false;

            for (int attempts = 1; attempts <= limit; attempts++) {
                logger.info("Attempt #" + attempts);
                // Evaluate the response
                assert llm != null;
                response = llm.evaluate(prompts, "");
                logger.info("Received response: " + response);

                prompts.addPrompt(PromptList.SYSTEM, response);

                // Validate the response
                isValid = validate(response);
                if (isValid) {
                    logger.info("Validation succeeded on attempt #" + attempts);
                    break;
                }

                // Construct and add error-specific prompts
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
                prompts.addPrompt(PromptList.USER, errorMessage);
            }

            if (!isValid) {
                logger.warning("Validation failed after " + limit + " attempts");
            }

            return response;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during execution", e);
            return null;
        }
    }



    public boolean validate(String response) {
        logger.info("Running agents: " + this.getClass().getName());

        JSONObject parsedQuery = new JSONObject(originalQuery);
        String data = parsedQuery.getString("data");
        String code = parsedQuery.getString("code");
        String text = parsedQuery.getString("text");

        try {
            writeFluidFile(data, code, response);

            String tempFile = Settings.getInstance().get(Settings.FLUID_TEMP_FILE);
            String os = System.getProperty("os.name").toLowerCase();
            String bashPrefix = os.contains("win") ? "cmd.exe /c " : "";

            String command = bashPrefix + "yarn fluid -f " + tempFile;
            logger.info("Running command: " + command);

            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();

            String output = new String(process.getInputStream().readAllBytes());
            String errorOutput = new String(process.getErrorStream().readAllBytes());

            logger.info("Command output: " + output);
            logger.info("Error output (if any): " + errorOutput);

            return validateOutput(output, text);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error during validation", e);
        }
    }

    private boolean validateOutput(String output, String text) {
        logger.info("Validating output: " + output);

        // Extract value from input text
        String regex = "value=\\\"(.*?)\\\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        if (!matcher.find()) {
            logger.warning("No matching value found in text");
            return false;
        }

        String expectedValue = matcher.group(1);

        // Extract and clean the generated expression
        String[] outputLines = output.split("\n");
        if (outputLines.length < 2) {
            logger.warning("Output format is invalid");
            return false;
        }

        String generatedExpression = outputLines[1].replaceAll("^\"|\"$", "");

        this.log = generatedExpression;
        this.expected = expectedValue;

        if (generatedExpression.equals(expectedValue)) {
            logger.info("Validation passed");
            return true;
        }

        logger.info("Validation failed: generated=" + generatedExpression + ", expected=" + expectedValue);
        return false;
    }

    private void writeFluidFile(String data, String code, String response) throws FileNotFoundException {
        PrintWriter out = new PrintWriter(STR."fluid/example/\{Settings.getInstance().get(Settings.FLUID_TEMP_FILE)}.fld");
        String result = template.replaceAll("##DATA##", data);
        result = result.replaceAll("##CODE##", code);
        result = result.replaceAll("##EXPRESSION##", response);
        out.println(result);
        out.flush();
        out.close();
    }
}
