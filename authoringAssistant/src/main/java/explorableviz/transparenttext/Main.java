package explorableviz.transparenttext;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {
    public static Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String... args) {
        Map<String, String> arguments = parseArguments(args);
        logger.info("Arguments passed from command line");
        logger.info(arguments.toString().replace(",", "\n"));
        final ArrayList<QueryContext> queryContexts;
        final LearningQueryContext learningQueryContext;
        final Optional<Integer> numQueryToExecute = arguments.containsKey("numQueryToExecute") ? Optional.of(Integer.parseInt(arguments.get("numQueryToExecute"))) : Optional.empty();
        try {
            Settings.getInstance().loadSettings(arguments.get("settings"));
            learningQueryContext = LearningQueryContext.importLearningCaseFromJSON(arguments.get("inContextLearningPath"), Integer.parseInt(arguments.get("numLearningCaseToGenerate")));
            queryContexts = TestQueryContext.loadCases(arguments.get("testPath"), Integer.parseInt(arguments.get("numTestToGenerate")));
            final int queryLimit = numQueryToExecute.orElseGet(queryContexts::size);
            final ArrayList<AuthoringAssistantResult> results = execute(learningQueryContext, arguments.get("agent"), queryLimit, queryContexts);
            float accuracy = computeAccuracy(results, queryContexts, queryLimit);
            writeLog(results, arguments.get("agent"), learningQueryContext.size());
            if (accuracy >= Float.parseFloat(arguments.get("threshold"))) {
                System.out.println(STR."Accuracy OK =\{accuracy}");
                System.exit(0);
            } else {
                System.out.println(STR."Accuracy KO =\{accuracy}");
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    private static void writeLog(ArrayList<AuthoringAssistantResult> results, String agent, int learningContextSize) throws IOException {
        Files.createDirectories(Paths.get(Settings.getInstance().getLogFolder()));
        PrintWriter out = new PrintWriter(new FileOutputStream(STR."\{Settings.getInstance().getLogFolder()}/log_\{System.currentTimeMillis()}.csv"));
        out.append("test-case;llm-agent;temperature;num-token;in-context-learning-size;attempts;result;generated-expression;duration(ms)\n");

        results.forEach(result -> {
            out.append(STR."\{result.query().getTestCaseFileName()};");
            out.append(STR."\{agent};");
            out.append(STR."\{Settings.getInstance().getTemperature()};");
            out.append(STR."\{Settings.getInstance().getNumContextToken()};");
            out.append(STR."\{learningContextSize};");
            out.append(STR."\{result.attempt()};");
            out.append(STR."\{result.response() != null ? "OK" : "KO"};");
            out.append(STR."\{result.response()};");
            out.append(STR."\{result.duration()};");
            out.append("\n");
        });

        out.flush();
        out.close();
    }

    private static float computeAccuracy(List<AuthoringAssistantResult> results, List<QueryContext> queryContexts, int queryLimit) {
        logger.info("Computing accuracy");
        long count = IntStream.range(0, results.size()).filter(i -> {
            logger.info(STR."I=\{i}exp=\{queryContexts.get(i).getExpected()} obtained=\{results.get(i).response()}");
            return queryContexts.get(i).getExpected().equals(results.get(i).response());
        }).count();
        return (float) count / queryLimit;
    }

    private static ArrayList<AuthoringAssistantResult> execute(LearningQueryContext learningQueryContext, String agent, int queryLimit, ArrayList<QueryContext> queryContexts) throws Exception {
        final ArrayList<AuthoringAssistantResult> results = new ArrayList<>();
        AuthoringAssistant workflow = new AuthoringAssistant(learningQueryContext, agent);
        for (int i = 0; i < queryLimit; i++) {
            QueryContext queryContext = queryContexts.get(i);
            logger.info(STR."Analysing query id=\{i}");
            results.add(workflow.execute(queryContext));
        }
        logger.info("Printing generated expression");
        for (AuthoringAssistantResult result : results) {
            logger.info(result.response());
        }
        return results;
    }

    public static Map<String, String> parseArguments(String[] args) {
        return Arrays.stream(args)
                .filter(arg -> arg.contains("="))
                .map(arg -> arg.split("=", 2))
                .filter(keyValue -> keyValue.length == 2)
                .collect(Collectors.toMap(
                        keyValue -> keyValue[0],
                        keyValue -> keyValue[1],
                        (_, replacement) -> replacement
                ));
    }
}
