package explorableviz.transparenttext;

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

    public static void main(String... args)  {

        if (args.length < 4) {
            System.err.println("missing arguments, 2 expected but " + args.length + " given");
            System.err.println("java -jar prompt-executorCLI.jar [AgentClass] [prompts] [settings] [queries] [expected] [maxQueries]");
            System.exit(0);
        }

        final String agent = args[0];
        final String promptPath = args[1];
        final String settingsPath = args[2];
        final String queryPath = args[3];
        final String fluitTemplatePath = args[6];
        final ArrayList<QueryContext> queries;
        try {
            queries = loadQueries(queryPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final int numQueries = (args.length == 7) ? Integer.parseInt(args[5]) : queries.size();
        final ArrayList<String> results = new ArrayList<>();

        /*
         * Workflow execution
         */
        try {
            Settings.getInstance().loadSettings(settingsPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < numQueries; i++) {
            QueryContext query = queries.get(i);
            logger.info(STR."Analysing query id=\{i}");
            PromptExecutorWorkflow promptExecutorWorkflow = null;
            try {
                promptExecutorWorkflow = new PromptExecutorWorkflow(promptPath, query, agent, fluitTemplatePath);
                results.add(promptExecutorWorkflow.execute());
            }  catch (Exception e) {
                System.err.println(e);
                e.printStackTrace();
                System.exit(1);
                results.add("ERROR " + e.getMessage());
            }
        }

        /*
         * Accuracy measure
         */
        if (args.length >= 5) {
            String[] expectedResult = null;
            int correct = 0;
            try {
                expectedResult = loadExpectedResults(args[4]);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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

    public static ArrayList<QueryContext> loadQueries(String queryPath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(new File(queryPath).toURI())));
        JSONArray queries = new JSONArray(content);
        ArrayList<QueryContext> outputQueries = new ArrayList<>();
        for (int i = 0; i < queries.length(); i++) {
            JSONObject o = queries.getJSONObject(i);
            QueryContext queryContext = QueryContext.importFromJson(o);
            outputQueries.add(queryContext);
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