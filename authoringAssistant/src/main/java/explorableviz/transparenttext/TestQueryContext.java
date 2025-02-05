package explorableviz.transparenttext;

import explorableviz.transparenttext.textfragment.Expression;
import explorableviz.transparenttext.textfragment.TextFragment;
import explorableviz.transparenttext.textfragment.Literal;
import kotlin.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TestQueryContext extends QueryContext {
    private final HashMap<String, String> variables;
    private final Random random;

    public TestQueryContext(HashMap<String, String> dataset, ArrayList<String> imports, HashMap<String, String> variables, String code, ArrayList<TextFragment> file, Random random, String expected) throws IOException {
        super(dataset, imports, code, file);
        this.random = random;
        this.variables = variables;
        this.setExpected(expected);
    }

    public ArrayList<QueryContext> instantiate(int number) throws IOException {
        ArrayList<QueryContext> queryContexts = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            Pair<String, String> replacedVariables = replaceVariables(this.getCode(), this.getExpected());
            QueryContext queryContext = new QueryContext(this.getDataset(), this.getImports(), replacedVariables.getFirst(), getParagraph(), replacedVariables.getSecond());
            if (queryContext.validate(queryContext.getExpected()).isEmpty()) {
                queryContexts.add(queryContext);
            } else {
                throw new RuntimeException("Invalid test exception");
            }
        }
        return queryContexts;
    }

    public static TestQueryContext importFromJson(JSONObject testCase, Random random) throws IOException {
        JSONArray json_datasets = testCase.getJSONArray("datasets");
        JSONObject json_variables = testCase.getJSONObject("variables");
        JSONArray json_imports = testCase.getJSONArray("imports");
        String code = testCase.getString("code");
        JSONArray json_paragraph = testCase.getJSONArray("paragraph");
        ArrayList<TextFragment> paragraph = new ArrayList<>();

        for (int i = 0; i < json_paragraph.length(); i++) {
            JSONObject paragraph_element = json_paragraph.getJSONObject(i);
            String type = paragraph_element.getString("type");
            switch (type) {
                case "literal":
                    paragraph.add(new Literal(paragraph_element.getString("value")));
                    break;
                case "expression":
                    paragraph.add(new Expression(json_paragraph.getJSONObject(i).getString("expression"), json_paragraph.getJSONObject(i).getString("value")));
                    break;
                default:
                    throw new RuntimeException((STR."\{paragraph_element.getString("type")} type is invalid"));
            }
        }
        String expected = testCase.getString("expected");

        HashMap<String, String> variables = new HashMap<>();
        HashMap<String, String> datasets = new HashMap<>();
        ArrayList<String> imports = new ArrayList<>();

        for (String key : json_variables.keySet()) {
            variables.put(key, json_variables.getString(key));
        }
        for (int i = 0; i < json_datasets.length(); i++) {
            datasets.put(json_datasets.getJSONObject(i).getString("var"), json_datasets.getJSONObject(i).getString("file"));
        }
        for (int i = 0; i < json_imports.length(); i++) {
            imports.add(json_imports.getString(i));
        }

        return new TestQueryContext(datasets, imports, variables, code, paragraph, random, expected);
    }

    private static String getRandomString(int length, Random generator) {
        StringBuilder sb = new StringBuilder(length);
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        for (int i = 0; i < length; i++) {
            int randomIndex = generator.nextInt(characters.length());
            sb.append(characters.charAt(randomIndex));
        }
        return sb.toString();
    }

    private Pair<String, String> replaceVariables(String code, String expected) {

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String variablePlaceholder = "$" + key + "$";
            String replacement = switch (value) {
                case "RANDOM_INT" -> String.valueOf(random.nextInt(10));
                case "RANDOM_FLOAT" -> String.format("%.6f", random.nextDouble() * 10);
                case "RANDOM_STRING" -> getRandomString(8, random).toLowerCase();
                default -> value;
            };

            code = code.replace(variablePlaceholder, replacement);
            expected = expected.replace(variablePlaceholder, replacement);

        }
        return new Pair<>(code, expected);
    }
}
