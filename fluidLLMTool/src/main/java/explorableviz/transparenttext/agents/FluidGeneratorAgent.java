package explorableviz.transparenttext.agents;

import it.unisa.cluelab.lllm.llm.LLMEvaluatorAgent;
import it.unisa.cluelab.lllm.llm.prompt.PromptList;
import org.json.JSONArray;
import org.json.JSONObject;
import explorableviz.transparenttext.Settings;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FluidGeneratorAgent {

    private PromptList prompts;
    private String originalQuery;
    private String loopBackQuery;
    private int nExecution;

    private final String template = "let d = ##DATA##;\n" +
            "let modelProbs = [\n" +
            "     { model: \"SSP1-1.9\", probability: 0.92 }\n" +
            "];\n" +
            "let newDataTable offset = map (fun s -> { scenario: s.scenario, bE2140: s.bestEst2140 + offset, low2140: s.low2140, high2140: s.high2140, bE4160: s.bestEst4160, low4160: s.low4160, high4160: s.high4160, bE81100: s.bestEst81100, low81100: s.low81100, high81100: s.high81100}) d;\n" +
            "let newModel offset = map (fun s -> { model: s.model, probability: s.probability + offset}) modelProbs;\n" +
            "\n" +
            "let getByScenario scenario data =\n" +
            "   let helper [] = [];\n" +
            "      helper (x: xs) =\n" +
            "            if x.scenario == scenario\n" +
            "            then x\n" +
            "            else helper xs\n" +
            "   in helper data;\n" +
            "let likelihoods = [\n" +
            "    { prob: 0.99, msg: \"virtually certain\" },\n" +
            "    { prob: 0.9, msg: \"very likely\"},\n" +
            "    { prob: 0.66, msg: \"likely\"},\n" +
            "    { prob: 0.33, msg: \"about as likely as not\"},\n" +
            "    { prob: 0.1, msg: \"unlikely\"},\n" +
            "    { prob: 0.01, msg: \"very unlikely\"},\n" +
            "    { prob: 0.0, msg: \"exceptionally unlikely\"}\n" +
            "];" +
            "let likelihoodMap prob =\n" +
            "    let go (x:xs) =\n" +
            "        if x.prob < prob\n" +
            "        then x.msg\n" +
            "        else go xs;\n" +
            "        go [] = \"not found\"\n" +
            "    in go likelihoods;\n" +
            "\n" +
            "let findLikelihood (min, max) target =\n" +
            "    if target > max\n" +
            "    then 0.0\n" +
            "    else if target < min\n" +
            "    then 1.0\n" +
            "    else (target - min)/(max - min);" +
            "##CODE##\n" +
            "in ##EXPRESSION##\n";

    public FluidGeneratorAgent(String promptPath, String query) throws IOException {
        this.originalQuery = query;
        initialise(promptPath);
        nExecution = 0;
    }

    public int getnExecution() {
        return nExecution;
    }

    public void setnExecution(int nExecution) {
        this.nExecution = nExecution;
    }

    public void setLoopbackSetence(String query) {
        this.loopBackQuery = query;
    }

    public static Logger logger = Logger.getLogger(FluidGeneratorAgent.class.getName());

    public void initialise(String promptPath) throws IOException {
        this.prompts = new PromptList();
        String content = new String(Files.readAllBytes(Paths.get(new File(promptPath).toURI())));
        JSONObject jsonContent = new JSONObject(content);
        prompts.parseJSONContent((JSONArray) jsonContent.get("prompts"));
    }

    public void validate(String response) {
        logger.info("Running agents " + this.getClass().getName());
        JSONObject parsedQuery = new JSONObject(originalQuery);
        String data = parsedQuery.getString("data");
        String code = parsedQuery.getString("code");

        try {
            writeFluidFile(data, code, response);
            String file = Settings.getInstance().get(Settings.FLUID_TEMP_FILE);
            String os = System.getProperty("os.name").toLowerCase();

            String bashType = "";
            /*
             * Check which bash need to be executed
             * based on OS.
             */
            if (os.contains("win")) {
                bashType = "cmd.exe /c";
            } else {
                bashType = "";
            }
            String command = STR."\{bashType} yarn fluid -f \{file}";
            logger.info("Running command");
            logger.info(command);
            Process proc = Runtime.getRuntime().exec(command);
            InputStream in = proc.getInputStream();
            InputStream err = proc.getErrorStream();
            proc.waitFor();
            String output = new String(in.readAllBytes());
            String err_output = new String(err.readAllBytes());
            logger.info("Output to be validated" + output);
            logger.info("seems error " + err_output);

            boolean valid = checkValidity(output, parsedQuery.getString("text"));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }



    /**
     * Checks the validity of the given output against a specific pattern within a provided string.
     *
     * @param output the output to be validated
     * @param s      the string containing the pattern to match against the output
     * @return true if the output matches the pattern, false otherwise
     */
    private boolean checkValidity(String output, String s) {
        logger.info(STR."Checking validity of the following output \{output}");
        String regex = "value=\\\"(.*?)\\\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(s);
        String value;
        if (matcher.find()) {
            value = matcher.group(1);
        } else {
            return false;
        }
        String generatedExpression = output.split("\n")[1];
        generatedExpression = generatedExpression.replaceAll("^\"|\"$", "");
        if (generatedExpression.equals(value)) {
            logger.info("Validation passed");
            return true;
        }
        String log = generatedExpression;
        String expected = value;
        logger.info("generated=" + generatedExpression + " \t expected=" + value);
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
    /**
     * Executes a task with the provided input.
     *
     * @param agentClassName the input parameter for the task
     * @return the result of executing the task
     */

    public String execute(String agentClassName)  {
        logger.info(STR."[EX no=\{nExecution}]Running \{this.getClass().getName()}");
        LLMEvaluatorAgent llm;

        try {
            Class<?> agentClass = Class.forName(agentClassName);
            logger.info("Send request for the query");
            llm = (LLMEvaluatorAgent) agentClass.getDeclaredConstructor(JSONObject.class).newInstance(Settings.getInstance().getSettings());
            if(loopBackQuery == null) {
                prompts.addPrompt(PromptList.USER, originalQuery);
            } else {
                prompts.addPrompt(PromptList.USER, loopBackQuery);
            }
            String s = llm.evaluate(prompts, "");
            logger.info("Received response for the query");
            prompts.addPrompt(PromptList.SYSTEM, s);
            validate(s);
            return s;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
            return null;
        }
    }

}
