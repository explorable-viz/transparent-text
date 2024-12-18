package agents;

import org.json.JSONObject;
import plrg.Settings;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FluidParserValidatorAgent implements Agent {

    private String response;

    private String sentence;

    private boolean valid;

    private Agent prevAgent;

    private String log;
    private String expected;
    public void setPrevAgent(Agent prevAgent) {
        this.prevAgent = prevAgent;
    }

    public static Logger logger = Logger.getLogger(FluidParserValidatorAgent.class.getName());

    /**
     * @TODO - move it in a template file. For now it remains here for debug reasons. (@alfy)
     */
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
    public FluidParserValidatorAgent(String sentence, String response) {
        this.sentence = sentence;
        this.response = response;

    }

    /**
     * Executes the validation task, generating a fluid program
     * and compiling it.
     *
     * @param a the input parameter for the task
     * @return null
     */
    @Override
    public String execute(String a) {
        logger.info("Running agents " + this.getClass().getName());
        JSONObject parsedSentence = new JSONObject(sentence);
        String data = parsedSentence.getString("data");
        String code = parsedSentence.getString("code");

        try {
            writeFluidFile(data, code);
            String file = Settings.getInstance().get(Settings.FLUID_TEMP_FILE);
            String compilerPath = Settings.getInstance().get(Settings.FLUID_COMPILER_PATH);
            Process proc = Runtime.getRuntime().exec(STR."cmd.exe /c cd \{compilerPath} & yarn fluid -f \{file}");
            InputStream in = proc.getInputStream();

            InputStream err = proc.getErrorStream();
            proc.waitFor();
            String output = new String(in.readAllBytes());
            valid = checkValidity(output, parsedSentence.getString("caption"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Checks the validity of the given output against a specific pattern within a provided string.
     *
     * @param output the output to be validated
     * @param s the string containing the pattern to match against the output
     * @return true if the output matches the pattern, false otherwise
     */
    private boolean checkValidity(String output, String s) {

        String regex = "value=\\\"(.*?)\\\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(s);
        String value;
        if (matcher.find()) {
            value = matcher.group(1);
        } else {
            return false;
        }
        String generatedExpression = output.split("\n")[2];
        generatedExpression = generatedExpression.replaceAll("^\"|\"$", "");
        if(generatedExpression.equals(value)) {
            logger.info("Validation passed");
            return true;
        }
        this.log = generatedExpression;
        this.expected = value;
        logger.info("generated=" + generatedExpression + " \t expected=" + value);
        return false;

    }

    /**
     * Retrieves the next Agent in the workflow based on validation status and previous agent type.
     *
     * @return the next Agent based on the validation status and previous agent type:
     *         <br> - If validation passed, returns a new OutputAgent with the response.
     *         <br> - If validation failed, creates a specific error message depending on the log and
     *         returns a FluidGeneratorAgent with updated values if the previous agent is an instance of FluidGeneratorAgent,
     *         otherwise returns a new OutputAgent with an error flag.
     */
    @Override
    public Agent next() {
        if(!valid) {
            logger.info("Validation not passed. Going back to " + prevAgent.getClass().getName());
            String msg = "";
            if(log.toLowerCase().contains("key") && log.toLowerCase().contains("not found")) {
                msg = STR."KeyNotFound Error. The generated expression \{response} is trying to access to a key which does not exist.Check the code and regenerate the expression which is supposed to generate the following value: \{expected}. Remember: reply only with the expression, without any other comment.";
            } else if (log.toLowerCase().contains("parseerror")) {
                msg = STR."SyntacticError. The generated expression \{response} generated the following error. \n \{log} Check the code and regenerate the expression which is supposed to generate the following value: \{expected}. Remember: reply only with the expression, without any other comment.";
            } else {
                msg = STR."ValueMismatchError. The generated expression \{response}  generated a value which is not expected. \nCheck the code and regenerate the expression which is supposed to generate the following value: \{expected}. Remember: reply only with the expression, without any other comment.";
            }
            if (prevAgent instanceof FluidGeneratorAgent agent) {
                agent.setSentence(msg);
                agent.setnExecution(agent.getnExecution()+1);
                return agent;
            } else {
                logger.info("Error: missed agent.");
                OutputAgent o = new OutputAgent(response);
                o.setError(true);
                return o;
            }
        }
        logger.info("Validation passed. Going forward to Output");
        return new OutputAgent(response);
    }

    private void writeFluidFile(String data, String code) throws FileNotFoundException {
        PrintWriter out = new PrintWriter(STR."fluid-parser/fluid/example/\{Settings.getInstance().get(Settings.FLUID_TEMP_FILE)}.fld");
        String result = template.replaceAll("##DATA##", data);
        result = result.replaceAll("##CODE##", code);
        result = result.replaceAll("##EXPRESSION##", response);
        out.println(result);
        out.flush();
        out.close();
    }
}
