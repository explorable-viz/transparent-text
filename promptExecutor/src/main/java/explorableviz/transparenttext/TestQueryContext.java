package explorableviz.transparenttext;

import kotlin.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TestQueryContext extends QueryContext {
    private HashMap<String, String> variables;
    private int seed = 0;
    public TestQueryContext(HashMap<String, String> dataset, ArrayList<String> imports, HashMap<String, String> variables, String code, String file) {
        super(dataset, imports, code, file);
        this.variables = variables;
    }

    public ArrayList<QueryContext> generate(int number) {
        ArrayList<QueryContext> queryContexts = new ArrayList<>();
        for(int i = 0; i < number; i++) {
            Pair<String, String> code_replaced = replaceVariable(this.getCode(), this.getFile());
            queryContexts.add(new QueryContext(this.getDataset(), this.getImports(), code_replaced.getFirst(), code_replaced.getSecond()));
        }
        return queryContexts;
    }

    public static TestQueryContext importFromJson(JSONObject object) {
        JSONArray json_datasets = object.getJSONArray("datasets");
        //Load Datasets
        HashMap<String, String> dataset = new HashMap<>();
        HashMap<String, String> variables = new HashMap<>();
        for(int i = 0; i < json_datasets.length(); i++) {
            dataset.put(json_datasets.getJSONObject(i).getString("var"), json_datasets.getJSONObject(i).getString("file"));
        }
        if(object.has("variables")) {
            JSONObject json_variables = object.getJSONObject("variables");
            for (String key : json_variables.keySet()) {
                variables.put(key, json_variables.getString(key));
            }
        }
        //Load Imports
        JSONArray json_imports = object.getJSONArray("imports");
        ArrayList<String> imports = new ArrayList<>();
        for(int i = 0; i < json_imports.length(); i++) {
            imports.add(json_imports.getString(i));
        }
        String code = object.getString("code");
        String file = object.getString("text");
        return new TestQueryContext(dataset, imports, variables, code, file);
    }

    private static String getRandomString(int length, Random generator) {
        StringBuilder sb = new StringBuilder(length);
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        for (int i = 0; i < length; i++) {
            int randomIndex = generator.nextInt(characters.length());
            sb.append(characters.charAt(randomIndex));
        }
        return sb.toString();
    }

    private Pair<String, String> replaceVariable(String code, String file) {
        Random generator = new Random(seed++);

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String variablePlaceholder = "$" + key + "$";
            String replacement;

            // Gestione dei tipi di variabili casuali
            switch (value) {
                case "RANDOM_INT":
                    replacement = String.valueOf(generator.nextInt(10)); // Numeri interi casuali [0, 10)
                    break;
                case "RANDOM_FLOAT":
                    replacement = String.format("%.6f", generator.nextDouble() * 10); // Float casuali [0, 10) con 6 decimali
                    break;
                case "RANDOM_STRING":
                    replacement = getRandomString(8, generator); // Stringa casuale di 8 caratteri
                    break;
                default:
                    replacement = value; // Per altri casi usiamo il valore fornito
            }

            // Sostituiamo il segnaposto con il valore calcolato
            code = code.replace(variablePlaceholder, replacement);
            file = file.replace(variablePlaceholder, replacement);

        }

        return new Pair<>(code, file);
    }
}
