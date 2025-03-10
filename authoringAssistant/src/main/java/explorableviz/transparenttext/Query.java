package explorableviz.transparenttext;

import explorableviz.transparenttext.paragraph.Expression;
import explorableviz.transparenttext.paragraph.Literal;
import explorableviz.transparenttext.paragraph.Paragraph;
import explorableviz.transparenttext.variable.ValueOptions;
import explorableviz.transparenttext.variable.Variables;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static explorableviz.transparenttext.variable.Variables.Flat.computeVariables;

public class Query {

    public final Logger logger = Logger.getLogger(this.getClass().getName());
    private final java.util.Map<String, String> datasets;
    private final List<String> imports;
    private final ArrayList<String> _loadedImports;
    private final String code;
    private final Paragraph paragraph;
    private final String expected;
    private final HashMap<String, String> _loadedDatasets;
    private final Variables variables;
    private final String testCaseFileName;
    private final String fluidFileName = "llmTest";

    public Query(java.util.Map<String, String> datasets, List<String> imports, String code, Paragraph paragraph, Variables variables, String expected, String testCaseFileName) throws IOException {
        Variables.Flat computedVariables = computeVariables(variables, new Random(0));
        this.variables = variables;
        this.datasets = datasets;
        this.imports = imports;
        this._loadedDatasets = new HashMap<>(this.loadDatasets()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        java.util.Map.Entry::getKey,
                        entry -> replaceVariables(entry.getValue(), computedVariables)
                )));
        this._loadedImports = this.loadImports();
        this.code = replaceVariables(code, computedVariables);
        this.expected = replaceVariables(expected, computedVariables);
        this.paragraph = paragraph.stream()
                .map(t -> t.replace(computedVariables))
                .collect(Collectors.toCollection(Paragraph::new));
        this.testCaseFileName = testCaseFileName;
        //Validation of the created object
//        writeFluidFiles(this.getExpected());
//        Optional<String> result = this.validate(new FluidCLI(this.getDatasets(), this.getImports()).evaluate(fluidFileName));
//        if (result.isPresent()) {
//            throw new RuntimeException(STR."[testCaseFile=\{testCaseFileName}] Invalid test exception\{result}");
//        }

    }

    public HashMap<String, String> loadDatasets() throws IOException {
        HashMap<String, String> loadedDatasets = new HashMap<>();
        for (java.util.Map.Entry<String, String> dataset : this.datasets.entrySet()) {
            loadedDatasets.put(dataset.getKey(), new String(Files.readAllBytes(Paths.get(new File(STR."\{Settings.getFluidCommonFolder()}/\{dataset.getValue()}.fld").toURI()))));
        }
        return loadedDatasets;
    }

    private ArrayList<String> loadImports() throws IOException {
        ArrayList<String> loadedImports = new ArrayList<>();
        for (String path : this.getImports()) {
            File importLib = new File(STR."\{Settings.getFluidCommonFolder()}/\{path}.fld");
            if (importLib.exists()) {
                loadedImports.add(new String(Files.readAllBytes(importLib.toPath())));
            } else {
                loadedImports.add(new String(Files.readAllBytes(Paths.get(STR."\{Settings.getLibrariesBasePath()}/\{path}.fld"))));
            }
        }
        return loadedImports;
    }

    public String toUserPrompt() {
        JSONObject object = new JSONObject();
        object.put("datasets", this.get_loadedDatasets());
        object.put("imports", this.get_loadedImports());
        object.put("code", this.getCode());
        object.put("paragraph", this.paragraph.toString());
        return object.toString();
    }

    public Optional<String> validate(String output) {
        logger.info(STR."Validating output: \{output}");
        Optional<LiteralParts> parts = this.getParagraph().stream().map(Paragraph::splitLiteral).flatMap(Optional::stream).findFirst();
        if (parts.isEmpty()) {
            throw new RuntimeException("No REPLACE tag found");
        }
        String expectedValue = parts.get().tag().getValue();
        // Extract and clean the generated expression
        String[] outputLines = output.split("\n");
        if (outputLines.length < 2) {
            throw new RuntimeException("Output format is invalid");
        }
        String value = outputLines[1].replaceAll("^\"|\"$", "");

        //interpreter errors detection -
        if(output.contains("Error: ")) {
            logger.info(STR."Validation failed because interpreter error");
            return Optional.of(value);
        }
        if (value.equals(expectedValue) || roundedEquals(value, expectedValue) || expectedValue.equals("?")) {
            logger.info("Validation passed");
            return Optional.empty();
        } else {
            logger.info(STR."Validation failed: generated=\{value}, expected=\{expectedValue}");
            return Optional.of(value);
        }
    }

    private boolean roundedEquals(String generated, String expected) {
        try {
            BigDecimal bdGen = new BigDecimal(generated);
            BigDecimal bdExp = new BigDecimal(expected);
            bdGen = bdGen.setScale(bdExp.scale(), RoundingMode.HALF_UP);
            return bdGen.compareTo(bdExp) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String replaceVariables(String textToReplace, Variables variables) {
        for (java.util.Map.Entry<String, ValueOptions> var : variables.entrySet()) {
            String variablePlaceholder = STR."$\{var.getKey()}$";
            textToReplace = textToReplace.replace(variablePlaceholder, String.valueOf(var.getValue().get()));
        }
        return textToReplace;
    }

    public static ArrayList<Query> loadQuery(String casesFolder, int numInstances) throws IOException {
        ArrayList<Query> queries = new ArrayList<>();
        Set<String> casePaths = Files.list(Paths.get(casesFolder))
                .filter(Files::isRegularFile) // Only process files, not directories
                .map(path -> path.toAbsolutePath().toString()) // Get file name
                .map(name -> name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name)
                .collect(Collectors.toSet());
        for (String casePath : casePaths) {
            JSONObject testCase = new JSONObject(new String(Files.readAllBytes(Path.of(STR."\{casePath}.json"))));
            for (int i = 0; i < numInstances; i++) {
                queries.add(importFromJson(testCase, casePath));
            }
        }
        return queries;
    }

    public static Query importFromJson(JSONObject testCase, String filePath) throws IOException {
        JSONArray json_datasets = testCase.getJSONArray("datasets");
        JSONObject json_variables = testCase.getJSONObject("variables");
        JSONArray json_imports = testCase.getJSONArray("imports");
        JSONArray json_paragraph = testCase.getJSONArray("paragraph");

        Paragraph paragraph = IntStream.range(0, json_paragraph.length())
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
                .collect(Collectors.toCollection(Paragraph::new));

        Variables var = Variables.fromJSON(json_variables);
        Map<String, String> datasets = IntStream.range(0, json_datasets.length())
                .boxed()
                .collect(Collectors.toMap(
                        i -> json_datasets.getJSONObject(i).getString("var"),
                        i -> json_datasets.getJSONObject(i).getString("file")
                ));

        List<String> imports = IntStream.range(0, json_imports.length())
                .mapToObj(json_imports::getString)
                .collect(Collectors.toList());

        return new Query(datasets, imports, new String(Files.readAllBytes(Path.of(STR."\{filePath}.fld"))), paragraph, var, testCase.getString("expected"), filePath);
    }

    public void writeFluidFiles(String response) throws IOException {
        Files.createDirectories(Paths.get(Settings.getFluidTempFolder()));
        //Write temp fluid file
        try (PrintWriter out = new PrintWriter(STR."\{Settings.getFluidTempFolder()}/\{fluidFileName}.fld")) {
            out.println(code);
            out.println(response);
        }
        for (int i = 0; i < get_loadedImports().size(); i++) {
            Files.createDirectories(Paths.get(STR."\{Settings.getFluidTempFolder()}/\{imports.get(i)}.fld").getParent());
            try (PrintWriter outData = new PrintWriter(STR."\{Settings.getFluidTempFolder()}/\{imports.get(i)}.fld")) {
                outData.println(get_loadedImports().get(i));
            }
        }
        for (java.util.Map.Entry<String, String> dataset : datasets.entrySet()) {
            Files.createDirectories(Paths.get(STR."\{Settings.getFluidTempFolder()}/\{dataset.getValue()}.fld").getParent());
            try (PrintWriter outData = new PrintWriter(STR."\{Settings.getFluidTempFolder()}/\{dataset.getValue()}.fld")) {
                outData.println(get_loadedDatasets().get(dataset.getKey()));
            }
        }
    }

    public ArrayList<String> get_loadedImports() {
        return _loadedImports;
    }

    public HashMap<String, String> get_loadedDatasets() {
        return _loadedDatasets;
    }

    public String getCode() {
        return code;
    }

    public java.util.Map<String, String> getDatasets() {
        return datasets;
    }

    public List<String> getImports() {
        return imports;
    }

    public Paragraph getParagraph() {
        return paragraph;
    }

    public Variables getVariables() {
        return variables;
    }

    public String getExpected() {
        return expected;
    }

    public String getTestCaseFileName() {
        return testCaseFileName;
    }

    public String getFluidFileName() {
        return fluidFileName;
    }
}
