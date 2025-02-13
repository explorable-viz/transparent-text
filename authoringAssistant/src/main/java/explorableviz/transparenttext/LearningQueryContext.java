package explorableviz.transparenttext;

import it.unisa.cluelab.lllm.llm.prompt.PromptList;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

public class LearningQueryContext  {
    private static final String LEARNING_CASE_PATH = "learningCases";
    private final String systemPrompt;

    private final ArrayList<QueryContext> learningCasePaths;

    public LearningQueryContext(String systemPrompt, ArrayList<QueryContext> learningCasePaths) {
        this.systemPrompt = systemPrompt;
        this.learningCasePaths = learningCasePaths;
    }

    public static LearningQueryContext importLearningCaseFromJSON(String jsonLearningCasePath, int numCasesToGenerate) throws Exception {
        JSONObject jsonLearningCase = new JSONObject(new String(Files.readAllBytes(Path.of(jsonLearningCasePath))));
        String systemPrompt = jsonLearningCase.getString("system_prompt");
        ArrayList<QueryContext> learningCases = new ArrayList<>();
        Random random = new Random(0);
        Files.list(Paths.get(LEARNING_CASE_PATH))
                .filter(Files::isRegularFile)
                .map(path -> path.toAbsolutePath().toString())
                .map(name -> name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name)
                .forEach(filePath -> {
            try {
                TestQueryContext testQueryContext = TestQueryContext.importFromJson(filePath, random);
                learningCases.addAll(testQueryContext.instantiate(numCasesToGenerate));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return new LearningQueryContext(systemPrompt, learningCases);
    }



    public PromptList generateInContextLearningJSON() throws Exception {
        PromptList inContextLearning = new PromptList();
        inContextLearning.addPrompt(PromptList.SYSTEM, this.systemPrompt);
        for(QueryContext queryContext : this.learningCasePaths) {
            inContextLearning.addPrompt(PromptList.USER, queryContext.toUserPrompt());
            inContextLearning.addPrompt(PromptList.ASSISTANT, queryContext.getExpected());
        }
        return inContextLearning;
    }

}
