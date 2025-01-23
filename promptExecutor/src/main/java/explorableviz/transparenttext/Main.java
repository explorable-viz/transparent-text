package explorableviz.transparenttext;

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
import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;

public class Main {
    public static Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String... args) {

        HashMap<String, String> arguments = parseArguments(args);
        logger.info("Arguments passed from command line");
        logger.info(arguments.toString().replace(",", "\n"));
        final String agent = arguments.get("agent");
        final String inContextLearningPath = arguments.get("inContextLearningPath");
        final String settingsPath = arguments.get("settingsPath");
        final String testPath = arguments.get("testPath");
        final int numTestToGenerate = Integer.parseInt(arguments.get("numTestToGenerate"));
        final ArrayList<QueryContext> queryContexts;
        final LearningQueryContext learningQueryContext;
        final Optional<Integer> numQueryToExecute = Optional.of(Integer.parseInt(arguments.get("numQueryToExecute")));

        try {
            learningQueryContext = LearningQueryContext.importLearningCaseFromJSON(inContextLearningPath, 10);
            queryContexts = loadTestCases(testPath, numTestToGenerate);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final int queryLimit = numQueryToExecute.orElseGet(queryContexts::size);

        final ArrayList<String> results = new ArrayList<>();

        /*
         * Workflow execution
         */

        try {
            Settings.getInstance().loadSettings(settingsPath);
            for (int i = 0; i < queryLimit; i++) {
                QueryContext queryContext = queryContexts.get(i);
                logger.info(STR."Analysing query id=\{i}");

                PromptExecutorWorkflow workflow = new PromptExecutorWorkflow(learningQueryContext, queryContext, agent);
                results.add(workflow.execute());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        logger.info("Printing generated expression");
        for(String result : results) {
            logger.info(result);
        }

        logger.info("Computing accuracy");
        int correct = 0;
        for (int i = 0; i < results.size(); i++) {
            if (queryContexts.get(i).getExpected().equals(results.get(i))) {
                correct++;
            }
        }
        float rate = (float) correct / queryLimit;
        System.out.println("Accuracy: " + rate);

        //writeResults(results, Settings.getInstance().get(Settings.LOG_PATH));
    }

    public static ArrayList<QueryContext> loadTestCases(String testCasesFolder, int numInstances) throws IOException {
        Path folder = Paths.get(testCasesFolder);

        // Check if the folder exists and is a directory
        if (!Files.exists(folder) || !Files.isDirectory(folder)) {
            throw new RuntimeException("Invalid folder path: " + testCasesFolder);
        }
        DirectoryStream<Path> directoryStream = Files.newDirectoryStream(folder, Files::isRegularFile);
        ArrayList<QueryContext> queryContexts = new ArrayList<>();
        Random random = new Random(0);
        for (Path filePath : directoryStream) {
            String content = new String(Files.readAllBytes(filePath));
            JSONObject jsonTestCase = new JSONObject(content);
            TestQueryContext testQueryContext = TestQueryContext.importFromJson(jsonTestCase, random);
            queryContexts.addAll(testQueryContext.instantiate(numInstances));
        }
        return queryContexts;
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