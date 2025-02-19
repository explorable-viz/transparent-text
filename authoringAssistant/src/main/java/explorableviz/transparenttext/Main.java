package explorableviz.transparenttext;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
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
            final ArrayList<String> results = execute(learningQueryContext, arguments.get("agent"), queryLimit, queryContexts);
            if (computeAccuracy(results, queryContexts, queryLimit, Integer.parseInt(arguments.get("threshold")))) {
                System.exit(0);
            } else {
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static boolean computeAccuracy(List<String> results, List<QueryContext> queryContexts, int queryLimit, float threshold) {
        logger.info("Computing accuracy");
        float rate = (float) IntStream.range(0, results.size()).filter(i -> queryContexts.get(i).getExpected().equals(results.get(i))).count() / queryLimit;
        System.out.println(STR."Accuracy: \{rate}");
        if (rate < threshold) {
            System.out.println("FAILED: Accuracy too low");
            return false;
        }
        System.out.println("PASS: Accuracy ok");
        return true;
    }

    private static ArrayList<String> execute(LearningQueryContext learningQueryContext, String agent, int queryLimit, ArrayList<QueryContext> queryContexts) throws Exception {
        final ArrayList<String> results = new ArrayList<>();
        AuthoringAssistant workflow = new AuthoringAssistant(learningQueryContext, agent);
        for (int i = 0; i < queryLimit; i++) {
            QueryContext queryContext = queryContexts.get(i);
            logger.info(STR."Analysing query id=\{i}");
            results.add(workflow.execute(queryContext));
        }
        logger.info("Printing generated expression");
        for (String result : results) {
            logger.info(result);
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
