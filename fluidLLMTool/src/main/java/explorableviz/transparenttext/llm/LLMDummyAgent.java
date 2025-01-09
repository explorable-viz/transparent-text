package explorableviz.transparenttext.llm;

import it.unisa.cluelab.lllm.llm.LLMEvaluatorAgent;
import it.unisa.cluelab.lllm.llm.prompt.Prompt;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class LLMDummyAgent extends LLMEvaluatorAgent {
    public LLMDummyAgent(JSONObject settings) {
        super(settings);
    }

    @Override
    public String evaluate(List<Prompt> list, String s) throws IOException {
        return "__dummy response__";
    }
}
