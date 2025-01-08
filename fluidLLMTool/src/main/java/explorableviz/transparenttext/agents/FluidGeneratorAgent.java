package explorableviz.transparenttext.agents;

import it.unisa.cluelab.lllm.llm.LLMEvaluatorAgent;
import it.unisa.cluelab.lllm.llm.prompt.PromptList;
import org.json.JSONObject;
import explorableviz.transparenttext.Settings;

import java.util.logging.Logger;

public class FluidGeneratorAgent implements Agent{

    private Agent prevAgent;
    private PromptList prompts;
    private String originalQuery;
    private String loopBackQuery;
    private int nExecution;

    public FluidGeneratorAgent(PromptList prompts, String query) {
        this.prompts = prompts;
        this.originalQuery = query;
        nExecution = 0;
    }

    public int getnExecution() {
        return nExecution;
    }

    public void setnExecution(int nExecution) {
        this.nExecution = nExecution;
    }

    public void setPrevAgent(Agent prevAgent) {
        this.prevAgent = prevAgent;
    }

    public void setLoopbackSetence(String query) {
        this.loopBackQuery = query;
    }

    public static Logger logger = Logger.getLogger(FluidGeneratorAgent.class.getName());

    /**
     * Executes a task with the provided input.
     *
     * @param agentClassName the input parameter for the task
     * @return the result of executing the task
     */
    @Override
    public String execute(String agentClassName)  {
        logger.info(STR."[EX no=\{nExecution}]Running \{this.getClass().getName()}");
        LLMEvaluatorAgent llm;

        try {
            Class<?> agentClass = Class.forName(agentClassName);
            logger.info("Send request for the query");
            llm = (LLMEvaluatorAgent) agentClass.getDeclaredConstructor(JSONObject.class).newInstance(Settings.getInstance().getSettings());
            if(loopBackQuery == null) {
                prompts.addPrompt(PromptList.USER, originalQuery);
            } else {
                prompts.addPrompt(PromptList.USER, loopBackQuery);
            }
            String s = llm.evaluate(prompts, "");
            logger.info("Received response for the query");
            prompts.addPrompt(PromptList.SYSTEM, s);
            return s;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
            return null;
        }
    }

    /**
     * Retrieves the next Agent in the workflow.
     *
     * @return the next Agent in the workflow based on the number of executions.
     * If the number of executions is less than a threshold in settings, creates a new FluidParserValidatorAgent
     * with the original query and the content of the last prompt, sets the previous agent,
     * and returns it. If the number of executions is threshold in settings or more, creates a new OutputAgent
     * with an empty string and sets an error flag, then returns it.
     */
    @Override
    public Agent next() {
        if(nExecution < Integer.parseInt(Settings.getInstance().get(Settings.LIMIT))) {
            FluidParserValidatorAgent fpa = new FluidParserValidatorAgent(originalQuery, prompts.getLast().getContent());
            fpa.setPrevAgent(this);
            return fpa;
        } else {
            OutputAgent a = new OutputAgent("");
            a.setError(true);
            return a;
        }
    }
}
