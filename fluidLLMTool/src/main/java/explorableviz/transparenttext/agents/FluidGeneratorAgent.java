package uk.ac.explorableviz.transparenttext.agents;

import it.unisa.cluelab.lllm.llm.LLMEvaluatorAgent;
import it.unisa.cluelab.lllm.llm.prompt.PromptList;
import org.json.JSONObject;
import uk.ac.explorableviz.transparenttext.plrg.Settings;

import java.util.logging.Logger;

public class FluidGeneratorAgent implements Agent{

    private Agent prevAgent;
    private PromptList prompts;
    private String originalSentence;
    private String loopBackSentence;
    private int nExecution;

    public FluidGeneratorAgent(PromptList prompts, String sentence) {
        this.prompts = prompts;
        this.originalSentence = sentence;
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

    public void setSentence(String sentence) {
        this.loopBackSentence = sentence;
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
        String agentToExecute = "it.unisa.cluelab.lllm.llm.agents." + agentClassName;
        try {
            if(loopBackSentence == null) {
                loopBackSentence = originalSentence;
            }
            Class<?> agentClass = Class.forName(agentToExecute);
            logger.info("Send request for the sentence");
            llm = (LLMEvaluatorAgent) agentClass.getDeclaredConstructor(JSONObject.class).newInstance(Settings.getInstance().getSettings());
            prompts.addPrompt(PromptList.USER, loopBackSentence);
            String s = llm.evaluate(prompts, "");
            logger.info("Received response for the sentence");
            prompts.addPrompt(PromptList.SYSTEM, s);
            return s;
        } catch (Exception e) {
            System.err.println("Class " + agentToExecute + " not found");
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
     * with the original sentence and the content of the last prompt, sets the previous agent,
     * and returns it. If the number of executions is threshold in settings or more, creates a new OutputAgent
     * with an empty string and sets an error flag, then returns it.
     */
    @Override
    public Agent next() {
        if(nExecution < Integer.parseInt(Settings.getInstance().get(Settings.LIMIT))) {
            FluidParserValidatorAgent fpa = new FluidParserValidatorAgent(originalSentence, prompts.getLast().getContent());
            fpa.setPrevAgent(this);
            return fpa;
        } else {
            OutputAgent a = new OutputAgent("");
            a.setError(true);
            return a;
        }
    }
}
