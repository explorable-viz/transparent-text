package uk.ac.bristol.agents;

import java.util.logging.Logger;

public class OutputAgent implements Agent {
    public static Logger logger = Logger.getLogger(OutputAgent.class.getName());
    private String response;
    private boolean error;

    public OutputAgent(String response) {
        this.response = response;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    @Override
    public String execute(String a) {
        logger.info("Running " + this.getClass().getName());
        if(error) return "ERROR";
        return response;
    }

    @Override
    public Agent next() {
        return null;
    }
}
