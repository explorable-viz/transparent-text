package explorableviz.transparenttext;

import explorableviz.transparenttext.textfragment.Expression;
import explorableviz.transparenttext.textfragment.Literal;
import explorableviz.transparenttext.textfragment.TextFragment;
import org.codehaus.plexus.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class QueryContext {

    public final Logger logger = Logger.getLogger(this.getClass().getName());
    private final Map<String, String> datasets;
    private final List<String> imports;
    private final ArrayList<String> _loadedImports;
    private final String fluidFileName = "llmTest";
    private final String code;
    private final List<TextFragment> paragraph;
    private final String expected;
    private final String testCaseFileName;
    private final HashMap<String, String> _loadedDatasets;
    private final Map<String, Object> variables;

    public QueryContext(Map<String, String> datasets, List<String> imports, String code, List<TextFragment> paragraph, Map<String, Object> variables, String expected, String testCaseFileName) throws IOException {
        Map<String, String> computedVariables = computeVariables(variables, new Random(0));
        this.variables = variables;
        this.datasets = datasets;
        this.imports = imports;
        this.testCaseFileName = testCaseFileName;
        this._loadedDatasets = new HashMap<>(this.loadDatasets()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> replaceVariables(entry.getValue(), computedVariables)
                )));
        this._loadedImports = this.loadImports();
        this.code = replaceVariables(code, computedVariables);
        this.expected = replaceVariables(expected, computedVariables);
        this.paragraph = paragraph.stream()
                .map(t -> t.replace(computedVariables))
                .collect(Collectors.toList());
        //Validation of the created object
        Optional<String> result = this.validate(this.evaluate(this.getExpected()));
        if (result.isPresent()) {
            throw new RuntimeException(STR."[testCaseFile=\{testCaseFileName}] Invalid test exception\{result}");
        }
    }

    public HashMap<String, String> loadDatasets() throws IOException {
        HashMap<String, String> loadedDatasets = new HashMap<>();
        for (Map.Entry<String, String> dataset : this.datasets.entrySet()) {
            String path = STR."\{dataset.getValue()}";
            loadedDatasets.put(dataset.getKey(), new String(Files.readAllBytes(Paths.get(new File(path + ".fld").toURI()))));
        }
        return loadedDatasets;
    }

    private ArrayList<String> loadImports() throws IOException {
        ArrayList<String> loadedImports = new ArrayList<>();
        for (String path : this.getImports()) {
            path = STR."\{Settings.getInstance().getLibrariesBasePath()}/\{path}";
            loadedImports.add(new String(Files.readAllBytes(Paths.get(new File(STR."\{path}.fld").toURI()))));
        }
        return loadedImports;
    }

    public String toUserPrompt() {
        JSONObject object = new JSONObject();
        object.put("datasets", this.get_loadedDatasets());
        object.put("imports", this.get_loadedImports());
        object.put("code", this.getCode());
        object.put("paragraph", this.paragraphToString());
        return object.toString();
    }

    public String paragraphToString() {
        return STR."Paragraph([\{this.getParagraph().stream().map(e -> {
            if (e instanceof Literal) return STR."\"\{e.getValue()}\"";
            if (e instanceof Expression) return ((Expression) e).getExpr();
            throw new RuntimeException("Error, it is possible to have only String or Expression element");
        }).collect(Collectors.joining(","))}])";
    }

    public void addExpressionToParagraph(String expression) {
        List<TextFragment> paragraph = this.getParagraph();
        ListIterator<TextFragment> iterator = paragraph.listIterator();

        while (iterator.hasNext()) {
            TextFragment textFragment = iterator.next();
            if (textFragment instanceof Literal && textFragment.getValue().contains("[REPLACE")) {
                splitLiteral(textFragment).ifPresentOrElse(expectedValue -> {
                    iterator.remove();
                    iterator.add(expectedValue.beforeTag());
                    iterator.add(new Expression(expression, expectedValue.tag().getValue()));
                    iterator.add(expectedValue.afterTag());
                }, () -> {
                    throw new RuntimeException("REPLACE tag not found");
                });
            }
        }
    }

    public String evaluate(String response) {
        try {
            //Generate the fluid program that will be processed and evaluated by the compiler
            String tempWorkingPath = Settings.getInstance().getTempWorkingPath();
            writeFluidFiles(response, tempWorkingPath);
            String os = System.getProperty("os.name").toLowerCase();
            String bashPrefix = os.contains("win") ? "cmd.exe /c " : "";

            //Command construction
            StringBuilder command = new StringBuilder(STR."\{bashPrefix}yarn fluid evaluate -l -p './' -f \{tempWorkingPath}/\{this.getFluidFileName()}");
            this.getDatasets().forEach((key, path) -> {
                command.append(STR." -d \"(\{key}, \{tempWorkingPath}/\{path})\"");
            });
            this.getImports().forEach(path -> {
                command.append(STR." -i \{path}");
            });
            logger.info(STR."Running command: \{command}");
            Process process;
            if (os.contains("win")) {
                process = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", command.toString()});
            } else {
                process = Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", command.toString()});
            }
            process.waitFor();

            //Reading command output
            String output = new String(process.getInputStream().readAllBytes());
            String errorOutput = new String(process.getErrorStream().readAllBytes());

            logger.info(STR."Command output: \{output}");
            if (!errorOutput.isEmpty()) {
                logger.info(STR."Error output: \{errorOutput}");
            }
            FileUtils.deleteDirectory(new File(tempWorkingPath));
            return output;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error during the execution of the fluid evaluate command", e);
        }
    }

    public Optional<String> validate(String output) {
        logger.info(STR."Validating output: \{output}");

        Optional<LiteralParts> parts = this.getParagraph().stream().map(this::splitLiteral).flatMap(Optional::stream).findFirst();
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
        if (value.equals(expectedValue)) {
            logger.info("Validation passed");
            return Optional.empty();
        } else {
            logger.info(STR."Validation failed: generated=\{value}, expected=\{expectedValue}");
            return Optional.of(value);
        }
    }

    private Optional<LiteralParts> splitLiteral(TextFragment literal) {
        Matcher valueReplaceMatcher = Pattern.compile("(.*)\\[REPLACE value=\"(.*?)\"](.*)").matcher(literal.getValue());
        if (!valueReplaceMatcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new LiteralParts(new Literal(valueReplaceMatcher.group(1)), new Literal(valueReplaceMatcher.group(2)), new Literal(valueReplaceMatcher.group(3))));
    }

    private void writeFluidFiles(String response, String tempWorkingPath) throws IOException {
        Files.createDirectories(Paths.get(tempWorkingPath));
        //Write temp fluid file
        try (PrintWriter out = new PrintWriter(STR."\{tempWorkingPath}/\{this.fluidFileName}.fld")) {
            out.println(this.getCode());
            out.println(STR."in \{response}");
        }
        for (Map.Entry<String, String> dataset : this.getDatasets().entrySet()) {
            Files.createDirectories(Paths.get(STR."\{tempWorkingPath}/\{dataset.getValue()}.fld").getParent());
            try (PrintWriter outData = new PrintWriter(STR."temp/\{dataset.getValue()}.fld")) {
                outData.println(this.get_loadedDatasets().get(dataset.getKey()));
            }
        }
    }

    public static String replaceVariables(String textToReplace, Map<String, String> variables) {
        for (Map.Entry<String, String> var : variables.entrySet()) {
            String variablePlaceholder = STR."$\{var.getKey()}$";
            textToReplace = textToReplace.replace(variablePlaceholder, var.getValue());
        }
        return textToReplace;
    }

    private HashMap<String, String> computeVariables(Map<String, Object> variables, Random random) {
        return variables.entrySet().stream()
                .map(entry -> expandVariableEntry(random, entry))
                .reduce(new HashMap<>(), (acc, map) -> {
                    acc.putAll(map);
                    return acc;
                }, (m1, m2) -> {
                    m1.putAll(m2);
                    return m1;
                });
    }

    private static Map<String, String> expandVariableEntry(Random random, Map.Entry<String, Object> entry) {
        HashMap<String, String> vars = new HashMap<>();
        if (entry.getValue() instanceof String value) {
            vars.put(entry.getKey(), switch (value) {
                case "RANDOM_INT" -> String.valueOf(random.nextInt(10));
                case "RANDOM_FLOAT" -> String.format("%.6f", random.nextDouble() * 10);
                case "RANDOM_STRING" -> getRandomString(8, random).toLowerCase();
                default -> value;
            });
        } else if (entry.getValue() instanceof JSONArray values && !values.isEmpty()) {
            JSONObject value = values.getJSONObject(random.nextInt(values.length()));
            value.keySet().forEach(k -> {
                vars.put(STR."\{entry.getKey()}.\{k}", value.getString(k));
            });
        }
        return vars;
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

    private ArrayList<String> get_loadedImports() {
        return _loadedImports;
    }

    public HashMap<String, String> get_loadedDatasets() {
        return _loadedDatasets;
    }

    public String getCode() {
        return code;
    }

    public Map<String, String> getDatasets() {
        return datasets;
    }

    public List<String> getImports() {
        return imports;
    }

    public List<TextFragment> getParagraph() {
        return paragraph;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public String getTestCaseFileName() {
        return testCaseFileName;
    }

    public String getExpected() {
        return expected;
    }

    private String getFluidFileName() {
        return fluidFileName;
    }
}
