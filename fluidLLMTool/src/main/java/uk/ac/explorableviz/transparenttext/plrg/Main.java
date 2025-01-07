package uk.ac.explorableviz.transparenttext.plrg;

import uk.ac.explorableviz.transparenttext.agents.Agent;
import uk.ac.explorableviz.transparenttext.agents.InputAgent;
import uk.ac.explorableviz.transparenttext.agents.OutputAgent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Logger;

public class Main {
    public static Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String... args) throws Exception {
        if (args.length < 4) {
            System.err.println("missinig arguments, 2 expected but " + args.length + " given");
            System.err.println("java -jar prompt-executorCLI.jar [AgentClass] [prompts.json] [settings.json] [sentences.txt] [expected.txt]");
            System.exit(0);
        }

        String agent = args[0];
        String promptPath = args[1];
        String sentencePath = args[3];
        String settingsPath = args[2];

        String[] sentences = loadSentences(sentencePath);

        ArrayList<String> results = new ArrayList<>();
        /**
         * Workflow execution
         */
        for (int i = 0; i < sentences.length; i++) {
            String s = sentences[i];
            logger.info("Analysing sentence id=" + i);
            Settings.getInstance().loadSettings(settingsPath);
            InputAgent inputAgent = new InputAgent(promptPath, s);
            Agent nextAgent = inputAgent.next();
            do {
                nextAgent.execute(agent);
                logger.info("NextAgent is" + nextAgent.getClass());
                nextAgent = nextAgent.next();
            } while (!(nextAgent instanceof OutputAgent));

            results.add(nextAgent.execute(""));
        }


        /**
         * Accuracy measure
         */
        if (args.length == 5) {
            String[] expectedResult = null;
            int correct = 0;
            expectedResult = loadExpectedResults(args[4]);
            for (int i = 0; i < results.size(); i++) {
                if (expectedResult[i].equals(results.get(i))) {
                    correct++;
                }
            }
            float rate = (float) correct / sentences.length;
            System.out.println("Accuracy: " + rate);
        }

        writeResults(results, Settings.getInstance().get(Settings.LOG_PATH));
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