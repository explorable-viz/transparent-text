package uk.ac.bristol.agents;

import it.unisa.cluelab.lllm.llm.prompt.PromptList;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class InputAgent implements Agent {

    private PromptList prompts;

    private String sentence;

    public InputAgent(String promptPath, String sentence) throws IOException {
        this.prompts = new PromptList();
        loadPrompts(promptPath);
        this.sentence = sentence;
    }
    @Override
    public String execute(String a) {
        return null;
    }

    @Override
    public Agent next() {
        FluidGeneratorAgent fga =  new FluidGeneratorAgent(prompts, sentence);
        fga.setPrevAgent(this);
        return fga;
    }

    /**
     * Loads prompts from the specified file path.
     *
     * @param promptPath the path to the file containing prompts in a JSON format
     * @throws IOException if an I/O error occurs while reading the file or parsing the JSON content
     */
    private void loadPrompts(String promptPath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(new File(promptPath).toURI())));
        JSONObject jsonContent = new JSONObject(content);
        prompts.parseJSONContent((JSONArray) jsonContent.get("prompts"));
    }

}
