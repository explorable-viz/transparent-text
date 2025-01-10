package explorableviz.transparenttext;

import explorableviz.transparenttext.agents.FluidGeneratorAgent;
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
            System.err.println("missing arguments, 2 expected but " + args.length + " given");
            System.err.println("java -jar prompt-executorCLI.jar [AgentClass] [prompts] [settings] [queries] [expected] [maxQueries]");
            System.exit(0);
        }

        final String agent = args[0];
        final String promptPath = args[1];
        final String queryPath = args[3];
        final String settingsPath = args[2];

        final String[] queries = loadQueries(queryPath);

        final int numQueries = (args.length == 6) ? Integer.parseInt(args[5]) : queries.length;

        final ArrayList<String> results = new ArrayList<>();
        /*
         * Workflow execution
         */
        Settings.getInstance().loadSettings(settingsPath);
        for (int i = 0; i < numQueries; i++) {
            String query = queries[i];
            logger.info("Analysing query id=" + i);

            FluidGeneratorAgent fluidGeneratorAgent = new FluidGeneratorAgent(promptPath, query);
            results.add(fluidGeneratorAgent.execute(agent));
//            InputAgent inputAgent = new InputAgent(promptPath, query);
//            Agent nextAgent = inputAgent.next();
//            /* @todo move to for instead of do. work on 'execute' method to return the next agent and the result */
//            do {
//                nextAgent.execute(agent);
//                logger.info("NextAgent is" + nextAgent.getClass());
//                nextAgent = nextAgent.next();
//            } while (!(nextAgent instanceof OutputAgent));

//            results.add(nextAgent.execute(""));
        }


        /*
         * Accuracy measure
         */
        if (args.length >= 5) {
            String[] expectedResult = null;
            int correct = 0;
            expectedResult = loadExpectedResults(args[4]);
            for (int i = 0; i < results.size(); i++) {
                if (expectedResult[i].equals(results.get(i))) {
                    correct++;
                }
            }
            float rate = (float) correct / numQueries;
            System.out.println("Accuracy: " + rate);
        } else {
            System.out.println("Accuracy: 0.0");
        }

        //writeResults(results, Settings.getInstance().get(Settings.LOG_PATH));
    }

    public static String[] loadQueries(String queryPath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(new File(queryPath).toURI())));
        JSONArray queries = new JSONArray(content);
        String[] outputQueries = new String[queries.length()];
        for (int i = 0; i < queries.length(); i++) {
            JSONObject o = queries.getJSONObject(i);
            outputQueries[i] = o.toString();
        }
        return outputQueries;
    }

    public static String[] loadExpectedResults(String queriesPath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(new File(queriesPath).toURI())));
        return content.split("\n");
    }

    public static void writeResults(ArrayList<String> result, String logPath) throws FileNotFoundException {
        PrintWriter out = new PrintWriter(logPath + "log_" + System.currentTimeMillis() + ".log");
        result.forEach(out::println);
        out.flush();
        out.close();
    }

}