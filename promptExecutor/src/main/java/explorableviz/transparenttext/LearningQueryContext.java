package explorableviz.transparenttext;

import it.unisa.cluelab.lllm.llm.prompt.PromptList;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Random;

public class LearningQueryContext  {
    private static final String LEARNING_CASE_PATH = "learningCases";
    private String systemPrompt;

    private ArrayList<QueryContext> learningCasePaths;

    public LearningQueryContext(String systemPrompt, ArrayList<QueryContext> learningCasePaths) {
        this.systemPrompt = systemPrompt;
        this.learningCasePaths = learningCasePaths;
    }

    public static LearningQueryContext importLearningCaseFromJSON(String jsonLearningCasePath, int numCasesToGenerate) throws IOException {
        JSONObject jsonLearningCase = new JSONObject(new String(Files.readAllBytes(Path.of(jsonLearningCasePath))));
        String systemPrompt = jsonLearningCase.getString("system_prompt");
        ArrayList<QueryContext> learningCases = new ArrayList<>();
        Random random = new Random(0);

        DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Path.of(LEARNING_CASE_PATH), Files::isRegularFile);
        for (Path filePath : directoryStream) {
            String content = new String(Files.readAllBytes(filePath));
            JSONObject jsonTestCase = new JSONObject(content);
            TestQueryContext testQueryContext = TestQueryContext.importFromJson(jsonTestCase, random);
            learningCases.addAll(testQueryContext.instantiate(numCasesToGenerate));
        }

        return new LearningQueryContext(systemPrompt, learningCases);
    }



    public PromptList generateInContextLearningJSON() throws Exception {
        PromptList inContextLearning = new PromptList();
        inContextLearning.addPrompt(PromptList.SYSTEM, this.systemPrompt);
        for(QueryContext queryContext : this.learningCasePaths) {
            inContextLearning.addPrompt(PromptList.USER, queryContext.toString());
            inContextLearning.addPrompt(PromptList.ASSISTANT, queryContext.toString());
        }
        return inContextLearning;
    }

}
