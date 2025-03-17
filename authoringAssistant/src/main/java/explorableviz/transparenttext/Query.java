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
    private final String expectedValue;
    private final HashMap<String, String> _loadedDatasets;
    private final String testCaseFileName;
    private final String fluidFileName = "llmTest";

    public Query(JSONObject testCase, String testCaseFileName, Random random) throws IOException {
        JSONArray json_datasets = testCase.getJSONArray("datasets");
        JSONArray json_imports = testCase.getJSONArray("imports");
        JSONArray json_paragraph = testCase.getJSONArray("paragraph");

        Variables.Flat computedVariables = computeVariables(Variables.fromJSON(testCase.getJSONObject("variables")), random);
        this.datasets = IntStream.range(0, json_datasets.length())
                .boxed()
                .collect(Collectors.toMap(
                        i -> json_datasets.getJSONObject(i).getString("var"),
                        i -> json_datasets.getJSONObject(i).getString("file")
                ));
        this.imports = IntStream.range(0, json_imports.length())
                .mapToObj(json_imports::getString)
                .collect(Collectors.toList());
        this._loadedDatasets = new HashMap<>(this.loadDatasets()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        java.util.Map.Entry::getKey,
                        entry -> replaceVariables(entry.getValue(), computedVariables)
                )));
        this._loadedImports = this.loadImports();
        this.code = replaceVariables(new String(Files.readAllBytes(Path.of(STR."\{testCaseFileName}.fld"))), computedVariables);
        this.expected = replaceVariables(testCase.getString("expected"), computedVariables);
        this.paragraph = new Paragraph(json_paragraph, computedVariables);

        this.testCaseFileName = testCaseFileName;
        //Validation of the created object
        writeFluidFiles(this.getExpected());
        this.expectedValue = new FluidCLI(this.getDatasets(), this.getImports()).evaluate(fluidFileName);
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

    public Optional<String> validate(String commandLineResponse) {
        logger.info(STR."Validating command line output: \{commandLineResponse}");

        String[] outputLines = commandLineResponse.split("\n");
        if (outputLines.length < 2) {
            throw new RuntimeException("Output format is invalid");
        }
        String value = outputLines[1].replaceAll("^\"|\"$", "");
        //interpreter errors detection -
        if (commandLineResponse.contains("Error: ")) {
            logger.info(STR."Validation failed because interpreter error");
            return Optional.of(value);
        }
        if (value.equals(this.expectedValue) || roundedEquals(value, this.expectedValue)) {
            logger.info("Validation passed");
            return Optional.empty();
        } else {
            logger.info(STR."Validation failed: generated=\{value}, expected=\{this.expectedValue}");
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

    // TODO: maybe loadQueries?
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
                // TODO: if we want to fix the seed here, shouldn't we just use a single one? Why one per query?
                queries.add(new Query(testCase, casePath, new Random(i)));
            }
        }
        return queries;
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
