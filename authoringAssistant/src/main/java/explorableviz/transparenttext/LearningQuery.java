package explorableviz.transparenttext;
import it.unisa.cluelab.lllm.llm.prompt.PromptList;
import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import static explorableviz.transparenttext.TestQuery.loadCases;

public class LearningQuery {
    private final String systemPrompt;

    private final ArrayList<Query> cases;

    public LearningQuery(String systemPrompt, ArrayList<Query> cases) {
        this.systemPrompt = systemPrompt;
        this.cases = cases;
    }

    public static LearningQuery importLearningCaseFromJSON(String jsonLearningCasePath, int numCasesToGenerate) throws Exception {
        JSONObject jsonLearningCase = new JSONObject(new String(Files.readAllBytes(Path.of(jsonLearningCasePath))));
        ArrayList<Query> learningCases = loadCases(Settings.getLearningCaseFolder(), numCasesToGenerate);
        return new LearningQuery(jsonLearningCase.getString("system_prompt"), learningCases);
    }

    public PromptList generateInContextLearningJSON() throws Exception {
        PromptList inContextLearning = new PromptList();
        inContextLearning.addPrompt(PromptList.SYSTEM, this.systemPrompt);
        for (Query query : this.cases) {
            inContextLearning.addPairPrompt(query.toUserPrompt(), query.getExpected());
        }
        return inContextLearning;
    }

    public int size() {
        return this.cases.size();
    }
}
