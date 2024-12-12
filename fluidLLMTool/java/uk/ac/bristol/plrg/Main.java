package uk.ac.bristol.plrg;

import fun.mike.dmp.Diff;
import fun.mike.dmp.DiffMatchPatch;
import fun.mike.dmp.Operation;
import it.unisa.cluelab.lllm.llm.LLMEvaluatorAgent;
import it.unisa.cluelab.lllm.llm.agents.ClaudeSonnetEvaluatorAgent;
import it.unisa.cluelab.lllm.llm.agents.Gpt4oEvaluationAgent;
import it.unisa.cluelab.lllm.llm.agents.Llama3EvaluatorAgent;
import it.unisa.cluelab.lllm.llm.prompt.PromptList;
import org.codehaus.plexus.util.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Main {
    public static Logger logger = Logger.getLogger(Main.class.getName());
    static final String regex = "\\b(?:virtually certain|very likely|likely|more likely than not|about as likely as not|unlikely|very unlikely|exceptionally unlikely)\\b";

    public static void main(String... args) throws Exception {
        if (args.length < 4) {
            System.err.println("missinig arguments, 2 expected but " + args.length + " given");
            System.err.println("java -jar prompt-executorCLI.jar [AgentClass] [prompts.json] [settings.json] [sentences.txt] [expected.txt]");
            System.exit(0);
        }

        String agents = args[0];
        String promptPath = args[1];
        String sentencePath = args[3];
        String settingsPath = args[2];

        String[] expectedResult = null;
        if (args.length == 5) {
            expectedResult = loadExpectedResults(args[4]);
        }
        logger.info("Reading prompt configuration from " + promptPath);
        String content_settings = new String(Files.readAllBytes(Paths.get(new File(settingsPath).toURI())));
        String content = new String(Files.readAllBytes(Paths.get(new File(promptPath).toURI())));
        JSONObject jsonContent = new JSONObject(content);
        JSONObject settings = new JSONObject(content_settings);

        PromptList prompts = new PromptList();

        logger.info("Reading sentences from " + sentencePath);
        String[] sentences = loadSentences(sentencePath);
        ArrayList<String> results = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);


        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i];
            prompts.clear();
            prompts.parseJSONContent((JSONArray) jsonContent.get("prompts"));
            //sentence = sentence.replaceAll(regex, "[STAT_REPLACE]");
            String valid = "-";
            String response = "";
            String sent = sentence;
            int k = 0;
            while (!valid.isEmpty() && k++ < 5) {
                response = evaluateLLM(prompts, sent, settings, agents);
                valid = validateResponse(response, sentence);
                logger.info("Validation check " + (!valid.isEmpty() ? "not " : "") + "passed");
                if (!valid.isEmpty()) {
                    String diff = StringUtils.difference("foobar", "foo");
                    sent = "Previous generation was incorrect. " +
                            "Log: \n" + valid + " \n" +
                            "Remember to replace only the information between the tag [STAT_REPLACE], [SCEN_REPLACE], [NUM_REPLACE]. " +
                            "Do not change other parts of the text. Don't add any comment.";
                }
            }
            if (response != null) results.add(response.trim());
            else results.add("ERROR");
        }

        int correct = 0;
        if (expectedResult != null) {
            for (int i = 0; i < results.size(); i++) {
                if (expectedResult[i].equals(results.get(i))) {
                    correct++;
                }
            }
            float rate = (float) correct / sentences.length;
            System.out.println("Accuracy: " + rate);
        }
        if (settings.has("log-path")) {
            writeResults(results, settings.getString("log-path"));
        } else {
            results.forEach(System.out::println);
        }
    }

    private static String validateResponse(String response, String sentence) {
        String fluidRegex = "\\\"\\,\\s*([a-zA-Z0-9\\s\\.*\\)*\\(*\\,]*)\\s*\\,\\s\\\"";
        String tagRegex = "\\[\\w*\\svalue=\\\"(.*?)\\\"\\]";

        String r = response.replaceAll(fluidRegex, "").replaceAll("[\"\s]", "");
        JSONObject parsed_sentence = new JSONObject(sentence);
        String s = parsed_sentence.getString("caption").replaceAll(tagRegex, "").replaceAll("[\"\s]", "");
        List<Diff> diffs = new DiffMatchPatch().diff_main(s, r);
        String differences = "";
        for (Diff diff : diffs) {
            if (diff.operation == Operation.INSERT) {
                differences += "Unespected add: " + diff.text + " \n"; // Return only single diff, can also find multiple based on use case
            } else if (diff.operation == Operation.DELETE) {
                differences += "Unespected remove: " + diff.text + "\n";
            }
        }
        return r.equals(s) ? "" : differences;
    }

    public static String evaluateLLM(PromptList prompts, String sentence, JSONObject settings, String agent) throws Exception {
        LLMEvaluatorAgent llm;
        String agentToExecute = "it.unisa.cluelab.lllm.llm.agents." + agent;
        try {
            Class<?> agentClass = Class.forName(agentToExecute);
            logger.info("Send request for the sentence");
            llm = (LLMEvaluatorAgent) agentClass.getDeclaredConstructor(JSONObject.class).newInstance(settings);
            prompts.addPrompt(PromptList.USER, sentence);
            String s = llm.evaluate(prompts, "");
            logger.info("Received response for the sentence");
            prompts.addPrompt(PromptList.SYSTEM, s);
            return s;
        } catch (Exception e) {
            System.err.println("Class " + agentToExecute + " not found");
            e.printStackTrace();
            System.exit(0);
            return null;
        }
    }

    public static String[] loadSentences(String sentencePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(new File(sentencePath).toURI())));
        JSONArray sentences = new JSONArray(content);
        String[] outputSentences = new String[sentences.length()];
        for (int i = 0; i < sentences.length(); i++) {
            JSONObject o = sentences.getJSONObject(i);
            outputSentences[i] = o.toString();
        }
        return outputSentences;
    }

    public static String[] loadExpectedResults(String sentencePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(new File(sentencePath).toURI())));
        return content.split("\n");
    }

    public static void writeResults(ArrayList<String> result, String logPath) throws FileNotFoundException {
        PrintWriter out = new PrintWriter(logPath + "log_" + System.currentTimeMillis() + ".log");
        result.forEach(out::println);
        out.flush();
        out.close();
    }
}