package explorableviz.transparenttext;
import it.unisa.cluelab.lllm.llm.prompt.PromptList;
import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import static explorableviz.transparenttext.TestQueryContext.loadCases;

public class LearningQueryContext {
    private static final String LEARNING_CASE_PATH = "learningCases";
    private final String systemPrompt;

    private final ArrayList<QueryContext> learningCasePaths;

    public LearningQueryContext(String systemPrompt, ArrayList<QueryContext> learningCasePaths) {
        this.systemPrompt = systemPrompt;
        this.learningCasePaths = learningCasePaths;
    }

    public static LearningQueryContext importLearningCaseFromJSON(String jsonLearningCasePath, int numCasesToGenerate) throws Exception {
        JSONObject jsonLearningCase = new JSONObject(new String(Files.readAllBytes(Path.of(jsonLearningCasePath))));
        ArrayList<QueryContext> learningCases = loadCases(LEARNING_CASE_PATH, numCasesToGenerate);
        return new LearningQueryContext(jsonLearningCase.getString("system_prompt"), learningCases);
    }

    public PromptList generateInContextLearningJSON() throws Exception {
        PromptList inContextLearning = new PromptList();
        inContextLearning.addPrompt(PromptList.SYSTEM, this.systemPrompt);
        for (QueryContext queryContext : this.learningCasePaths) {
            inContextLearning.addPrompt(PromptList.USER, queryContext.toUserPrompt());
            inContextLearning.addPrompt(PromptList.ASSISTANT, queryContext.getExpected());
        }
        return inContextLearning;
    }

    public int size() {
        return this.learningCasePaths.size();
    }
}
