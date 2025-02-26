package explorableviz.transparenttext;

import explorableviz.transparenttext.textfragment.Expression;
import explorableviz.transparenttext.textfragment.TextFragment;
import explorableviz.transparenttext.textfragment.Literal;
import explorableviz.transparenttext.variable.Variables;
import explorableviz.transparenttext.variable.ValueOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestQueryContext {

    private final Variables variables;
    private final Map<String, String> datasets;
    private final List<String> imports;
    private final String testCaseFileName;
    private final String code;
    private final String expected;
    private final List<TextFragment> paragraph;

    public TestQueryContext(Map<String, String> datasets, List<String> imports, String code, List<TextFragment> paragraph, Variables variables, String expected, String testCaseFileName) throws IOException {
        this.datasets = datasets;
        this.imports = imports;
        this.testCaseFileName = testCaseFileName;
        this.code = code;
        this.expected = expected;
        this.variables = variables;
        this.paragraph = paragraph;
    }

    public Variables getVariables() {
        return variables;
    }

    public static ArrayList<QueryContext> loadCases(String casesFolder, int numInstances) throws IOException {
        ArrayList<QueryContext> queries = new ArrayList<>();
        Set<String> casePaths = Files.list(Paths.get(casesFolder))
                .filter(Files::isRegularFile) // Only process files, not directories
                .map(path -> path.toAbsolutePath().toString()) // Get file name
                .map(name -> name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name)
                .collect(Collectors.toSet());
        for (String casePath : casePaths) {
            TestQueryContext testQuery = TestQueryContext.importFromJson(casePath);
            for (int i = 0; i < numInstances; i++) {
                queries.add(new QueryContext(testQuery.getDatasets(), testQuery.getImports(), testQuery.getCode(), testQuery.getParagraph(), testQuery.getVariables(), testQuery.getExpected(), testQuery.getTestCaseFileName()));
            }
        }
        return queries;
    }

    public static TestQueryContext importFromJson(String filePath) throws IOException {
        JSONObject testCase = new JSONObject(new String(Files.readAllBytes(Path.of(STR."\{filePath}.json"))));
        JSONArray json_datasets = testCase.getJSONArray("datasets");
        JSONObject json_variables = testCase.getJSONObject("variables");
        JSONArray json_imports = testCase.getJSONArray("imports");
        JSONArray json_paragraph = testCase.getJSONArray("paragraph");

        List<TextFragment> paragraph = IntStream.range(0, json_paragraph.length())
                .mapToObj(i -> {
                    JSONObject paragraphElement = json_paragraph.getJSONObject(i);
                    String type = paragraphElement.getString("type");

                    return switch (type) {
                        case "literal" -> new Literal(paragraphElement.getString("value"));
                        case "expression" ->
                                new Expression(paragraphElement.getString("expression"), paragraphElement.getString("value"));
                        default -> throw new RuntimeException(STR."\{type} type is invalid");
                    };
                })
                .collect(Collectors.toList());

        Variables var = new Variables();
        json_variables.keySet().forEach(k -> var.put(k, switch (json_variables.get(k)) {
            case String s -> new ValueOptions.StringValue(s);
            case Integer i -> new ValueOptions.Number(i);
            case BigDecimal f -> new ValueOptions.Number(f.floatValue());
            case JSONArray array -> processJsonArray(array, k);
            default -> null;
        }));

        Map<String, String> datasets = IntStream.range(0, json_datasets.length())
                .boxed()
                .collect(Collectors.toMap(
                        i -> json_datasets.getJSONObject(i).getString("var"),
                        i -> json_datasets.getJSONObject(i).getString("file")
                ));

        List<String> imports = IntStream.range(0, json_imports.length())
                .mapToObj(json_imports::getString)
                .collect(Collectors.toList());

        return new TestQueryContext(datasets, imports, new String(Files.readAllBytes(Path.of(STR."\{filePath}.fld"))), paragraph, var, testCase.getString("expected"), filePath);
    }

    private static ValueOptions.List processJsonArray(JSONArray array, String varName) {
        return new ValueOptions.List((IntStream.range(0, array.length())
                .mapToObj(array::get)
                .map(e -> {
                    if (!e.getClass().equals(array.get(0).getClass())) {
                        throw new RuntimeException(STR."Different types in a list are not allowed. Check the type of '\{varName}'.");
                    }
                    return ValueOptions.of(e);
                }).collect(Collectors.toList())));
    }

    public Map<String, String> getDatasets() {
        return datasets;
    }

    public List<String> getImports() {
        return imports;
    }

    public String getTestCaseFileName() {
        return testCaseFileName;
    }

    public String getCode() {
        return code;
    }

    public String getExpected() {
        return expected;
    }

    public List<TextFragment> getParagraph() {
        return paragraph;
    }

}
