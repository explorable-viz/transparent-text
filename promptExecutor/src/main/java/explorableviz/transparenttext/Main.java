package explorableviz.transparenttext;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

public class Main {
    public static Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String... args)  {

        HashMap<String, String> arguments = parseArguments(args);
        if (args.length < 4) {
            System.err.println("missing arguments, 2 expected but " + args.length + " given");
            System.err.println("java -jar prompt-executorCLI.jar [AgentClass] [prompts] [settings] [queries] [expected] [maxQueries]");
            System.exit(0);
        }

        final String agent = arguments.get("agent");
        final String promptPath = arguments.get("inContextLearningPath");
        final String settingsPath = arguments.get("settingsPath");
        final String testPath = arguments.get("testPath");
        final String fluitTemplatePath = arguments.get("fluidTemplatePath");
        final ArrayList<QueryContext> queries;
        try {
            queries = loadTestCases(testPath, Integer.parseInt(arguments.get("numTestToGenerate")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final int numQueries = (arguments.containsKey("numTestToExecute")) ? Integer.parseInt(arguments.get("numTestToExecute")) : queries.size();
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
        */
        System.out.println("Accuracy: 0.0");
        //writeResults(results, Settings.getInstance().get(Settings.LOG_PATH));
    }

    public static ArrayList<QueryContext> loadTestCases(String testPath, int nTest) throws IOException {
        Path folder = Paths.get(testPath);

        // Check if the folder exists and is a directory
        if (!Files.exists(folder) || !Files.isDirectory(folder)) {
            throw new RuntimeException("Invalid folder path: " + testPath);
        }
        DirectoryStream<Path> directoryStream = Files.newDirectoryStream(folder);
        ArrayList<QueryContext> outputQueries = new ArrayList<>();
        for (Path filePath : directoryStream) {
            if(filePath.getFileName().toString().startsWith("expression")) continue;
            if (Files.isRegularFile(filePath)) { // Ensure it's a file
                String content = new String(Files.readAllBytes(filePath));
                JSONObject o = new JSONObject(content);
                TestQueryContext queryContext = TestQueryContext.importFromJson(o);
                outputQueries.addAll(queryContext.generate(nTest));
            }
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

    public static HashMap<String, String> parseArguments(String[] args) {
        HashMap<String, String> arguments = new HashMap<>();

        for (String arg : args) {
            if (arg.contains("=")) {
                String[] keyValue = arg.split("=", 2); // Split into key and value
                if (keyValue.length == 2) {
                    arguments.put(keyValue[0], keyValue[1]);
                } else {
                    System.err.println("Invalid argument format: " + arg);
                }
            } else {
                System.err.println("Skipping argument without '=': " + arg);
            }
        }

        return arguments;
    }


}