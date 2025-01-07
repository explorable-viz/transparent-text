package uk.ac.explorableviz.transparenttext.plrg;

import it.unisa.cluelab.lllm.llm.prompt.Prompt;
import it.unisa.cluelab.lllm.llm.prompt.PromptList;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FluidPromptGenerator {

    public static void main(String... args) throws Exception {


        String content = new String(Files.readAllBytes(Paths.get(new File("input/sentences.json").toURI())));
        JSONArray jsonContent = new JSONArray(content);

        PromptList list = new PromptList();
        list.addPrompt(PromptList.SYSTEM, "");
        for(int i = 0; i < jsonContent.length(); i++) {

            JSONObject row = jsonContent.getJSONObject(i);
            String input = row.getString("code") + "\n" + row.getString("caption");
            String output = row.getString("output");
            if(i % 2 == 0) {
                list.addPrompt(PromptList.USER, input);
                list.addPrompt(PromptList.ASSISTANT, output);
            }
        }

        list.exportToJson("prompt-2v.json");
    }
}
