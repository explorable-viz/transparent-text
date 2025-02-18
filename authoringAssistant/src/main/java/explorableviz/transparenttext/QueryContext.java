package explorableviz.transparenttext;

import explorableviz.transparenttext.textfragment.Expression;
import explorableviz.transparenttext.textfragment.Literal;
import explorableviz.transparenttext.textfragment.TextFragment;
import org.codehaus.plexus.util.FileUtils;
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
    private final HashMap<String, String> datasets;
    private final HashMap<String, String> _loadedDatasets;
    private final ArrayList<String> imports;
    private final ArrayList<String> _loadedImports;
    private final String fluidFileName = "llmTest";
    private String code;
    private final ArrayList<TextFragment> paragraph;
    private String expected;

    public String getCode() {
        return code;
    }

    public QueryContext(HashMap<String, String> datasets, ArrayList<String> imports, String code, ArrayList<TextFragment> paragraph) throws IOException {
        this.datasets = datasets;
        this.imports = imports;
        this.paragraph = new ArrayList<>();
        paragraph.forEach(t -> {
            this.paragraph.add((TextFragment) t.clone());
        });
        this.code = code;
        this._loadedImports = new ArrayList<>();
        this._loadedDatasets = new HashMap<>();
        this.loadFiles();
    }

    public QueryContext(TestQueryContext queryContext, Random random) throws IOException {
        this(queryContext.getDatasets(), queryContext.getImports(), queryContext.getCode(), queryContext.getParagraph());
        this.expected = queryContext.getExpected();
        this.replaceVariables(queryContext.getVariables(), random);
        Optional<String> result = this.validate(this.evaluate(this.getExpected()));
        if (result.isPresent()) {
            throw new RuntimeException(STR."[testCaseFile=\{queryContext.getTestCaseFileName()}] Invalid test exception\{result}");
        }
    }

    public HashMap<String, String> getDatasets() {
        return datasets;
    }

    public ArrayList<String> getImports() {
        return imports;
    }

    public ArrayList<TextFragment> getParagraph() {
        return paragraph;
    }

    public void loadFiles() throws IOException {
        for (Map.Entry<String, String> dataset : this.datasets.entrySet()) {
            String path = STR."\{dataset.getValue()}";
            this.get_loadedDatasets().put(dataset.getKey(), new String(Files.readAllBytes(Paths.get(new File(path + ".fld").toURI()))));
        }
        for (String path : this.getImports()) {
            path = STR."\{path}";
            this.get_loadedImports().add(new String(Files.readAllBytes(Paths.get(new File(path + ".fld").toURI()))));
        }
    }

    private ArrayList<String> get_loadedImports() {
        return _loadedImports;
    }

    public HashMap<String, String> get_loadedDatasets() {
        return _loadedDatasets;
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
        for (int i = 0; i < this.getParagraph().size(); i++) {
            TextFragment textFragment = this.getParagraph().get(i);
            if (textFragment instanceof Literal && textFragment.getValue().contains("[REPLACE")) {
                LiteralParts expectedValue = splitLiteral((Literal) textFragment);
                this.getParagraph().remove(textFragment);
                this.getParagraph().add(i, expectedValue.beforeTag());
                this.getParagraph().add(i + 1, new Expression(expression, expectedValue.tag().getValue()));
                this.getParagraph().add(i + 2, expectedValue.afterTag());
            }
        }
    }

    public String getExpected() {
        return expected;
    }

    private String getFluidFileName() {
        return fluidFileName;
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
        //Extract value from input query.text
        Optional<TextFragment> textFragment = this.getParagraph().stream().filter(t -> t.getValue().contains("[REPLACE")).findFirst();

        String expectedValue = splitLiteral((Literal) textFragment.get()).tag().getValue();
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

    private LiteralParts splitLiteral(Literal literal) {
        final String replaceRegex = "(.*)\\[REPLACE value=\"(.*?)\"](.*)";
        Pattern valueReplacePattern = Pattern.compile(replaceRegex);
        Matcher valueReplaceMatcher = valueReplacePattern.matcher(literal.getValue());
        if (!valueReplaceMatcher.find()) {
            throw new RuntimeException("No matching value found in text");
        }
        return new LiteralParts(new Literal(valueReplaceMatcher.group(1)), new Literal(valueReplaceMatcher.group(2)), new Literal(valueReplaceMatcher.group(3)));
    }

    private void writeFluidFiles(String response, String tempWorkingPath) throws IOException {
        Files.createDirectories(Paths.get(tempWorkingPath));
        //Write temp fluid file
        try (PrintWriter out = new PrintWriter(STR."\{tempWorkingPath}/\{this.fluidFileName}.fld")) {
            out.println(this.getCode());
            out.println(STR."in \{response}");
        }
        //Write temp datasets
        this.getDatasets().forEach((v, p) -> {
            try {
                Files.createDirectories(Paths.get(STR."\{tempWorkingPath}/\{p}.fld").getParent());
                try (PrintWriter outData = new PrintWriter(STR."temp/\{p}.fld")) {
                    outData.println(this.get_loadedDatasets().get(v));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void replaceVariables(Map<String, String> variables, Random random) {
        String code = this.getCode();
        String expected = this.getExpected();

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String variablePlaceholder = STR."$\{key}$";
            String replacement = switch (value) {
                case "RANDOM_INT" -> String.valueOf(random.nextInt(10));
                case "RANDOM_FLOAT" -> String.format("%.6f", random.nextDouble() * 10);
                case "RANDOM_STRING" -> getRandomString(8, random).toLowerCase();
                default -> value;
            };
            code = code.replace(variablePlaceholder, replacement);
            expected = expected.replace(variablePlaceholder, replacement);
            this.getParagraph().forEach(t -> {
                t.setValue(t.getValue().replace(variablePlaceholder, replacement));
            });
            this.get_loadedDatasets().replaceAll((k, v) -> v.replace(variablePlaceholder, replacement));
        }
        this.code = code;
        this.expected = expected;
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

    protected void setExpected(String expected) {
        this.expected = expected;
    }
}
