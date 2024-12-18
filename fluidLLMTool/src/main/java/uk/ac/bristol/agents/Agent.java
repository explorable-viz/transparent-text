package uk.ac.bristol.agents;

public interface Agent {

    /**
     * Executes a task with the provided input.
     *
     * @param a the input parameter for the task
     * @return the result of executing the task
     */
    String execute(String a);

    /**
     * Retrieves the next Agent in the workflow.
     *
     * @return the next Agent in the workflow
     */
    Agent next();
}
