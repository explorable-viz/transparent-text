package explorableviz.transparenttext;
import it.unisa.cluelab.lllm.llm.prompt.PromptList;
import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import static explorableviz.transparenttext.TestQuery.loadCases;

public class LearningQuery {
    private static final String LEARNING_CASE_PATH = "learningCases";
    private final String systemPrompt;

    private final ArrayList<Query> learningCasePaths;

    public LearningQuery(String systemPrompt, ArrayList<Query> learningCasePaths) {
        this.systemPrompt = systemPrompt;
        this.learningCasePaths = learningCasePaths;
    }

    public static LearningQuery importLearningCaseFromJSON(String jsonLearningCasePath, int numCasesToGenerate) throws Exception {
        JSONObject jsonLearningCase = new JSONObject(new String(Files.readAllBytes(Path.of(jsonLearningCasePath))));
        ArrayList<Query> learningCases = loadCases(LEARNING_CASE_PATH, numCasesToGenerate);
        return new LearningQuery(jsonLearningCase.getString("system_prompt"), learningCases);
    }

    public PromptList generateInContextLearningJSON() throws Exception {
        PromptList inContextLearning = new PromptList();
        inContextLearning.addPrompt(PromptList.SYSTEM, this.systemPrompt);
        for (Query query : this.learningCasePaths) {
            inContextLearning.addPrompt(PromptList.USER, query.toUserPrompt());
            inContextLearning.addPrompt(PromptList.ASSISTANT, query.getExpected());
        }
        return inContextLearning;
    }

    public int size() {
        return this.learningCasePaths.size();
    }
}
