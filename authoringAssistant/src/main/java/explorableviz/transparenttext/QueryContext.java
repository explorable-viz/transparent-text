package explorableviz.transparenttext;

import explorableviz.transparenttext.textfragment.Expression;
import explorableviz.transparenttext.textfragment.TextFragment;
import explorableviz.transparenttext.textfragment.Literal;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class QueryContext {

    public final Logger logger = Logger.getLogger(this.getClass().getName());
    private HashMap<String, String> dataset;
    private final HashMap<String, String> _loadedDatasets;

    private ArrayList<String> imports;
    private final ArrayList<String> _loadedImports;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    private String code;
    private final ArrayList<TextFragment> paragraph;

    private String expected;

    public QueryContext(HashMap<String, String> dataset, ArrayList<String> imports, String code, ArrayList<TextFragment> paragraph) throws IOException {
        this.dataset = dataset;
        this.imports = imports;
        this.paragraph = paragraph;
        this.code = code;
        this._loadedImports = new ArrayList<>();
        this._loadedDatasets = new HashMap<>();
        loadFiles();
    }

    public QueryContext(HashMap<String, String> dataset, ArrayList<String> imports, String code, ArrayList<TextFragment> paragraph, String expected) throws IOException {
        this(dataset, imports, code, paragraph);
        this.expected = expected;
    }

    public HashMap<String, String> getDataset() {
        return dataset;
    }

    public void setDataset(HashMap<String, String> dataset) {
        this.dataset = dataset;
    }

    public ArrayList<String> getImports() {
        return imports;
    }

    public void setImports(ArrayList<String> imports) {
        this.imports = imports;
    }

    public ArrayList<TextFragment> getParagraph() {
        return paragraph;
    }

    public void loadFiles() throws IOException {
        for (Map.Entry<String, String> dataset : this.dataset.entrySet()) {
            String path = STR."fluid/\{dataset.getValue()}";
            this._loadedDatasets.put(dataset.getKey(), new String(Files.readAllBytes(Paths.get(new File(path + ".fld").toURI()))));
        }
        for (String path : imports) {
            path = STR."fluid/\{path}";
            this._loadedImports.add(new String(Files.readAllBytes(Paths.get(new File(path + ".fld").toURI()))));
        }
    }

    public String toUserPrompt() {
        JSONObject object = new JSONObject();
        object.put("datasets", this._loadedDatasets);
        object.put("imports", this._loadedImports);
        object.put("code", this.code);
        object.put("paragraph", paragraphToString());
        return object.toString();
    }

    public String paragraphToString() {
        return STR."Paragraph([\{paragraph.stream().map(e -> {
            if (e instanceof Literal) return STR."\"\{e.getValue()}\"";
            if (e instanceof Expression) return ((Expression) e).getExpr();
            throw new RuntimeException("Error, it is possible to have only String or Expression element");
        }).collect(Collectors.joining(","))}])";
    }

    public void addExpressionToParagraph(String expression) throws Exception {
        for (int i = 0; i < paragraph.size(); i++) {
            TextFragment textFragment = paragraph.get(i);
            if (textFragment instanceof Literal && textFragment.getValue().contains("[REPLACE")) {
                LiteralParts expectedValue = splitLiteral((Literal) textFragment);
                paragraph.remove(textFragment);
                paragraph.add(i, expectedValue.beforeTag());
                paragraph.add(i + 1, new Expression(expression, expectedValue.tag().getValue()));
                paragraph.add(i + 2, expectedValue.afterTag());
            }
        }
    }

    public String getExpected() {
        return expected;
    }

    public void setExpected(String expected) {
        this.expected = expected;
    }

    public String evaluate(String response) {
        try {
            //Generate the fluid program that will be processed and evaluated by the compiler
            String tempFile = Settings.getInstance().getFluidTempFile();
            writeFluidFile(response, tempFile);
            String os = System.getProperty("os.name").toLowerCase();
            String bashPrefix = os.contains("win") ? "cmd.exe /c " : "";

            //Command construction
            StringBuilder command = new StringBuilder(STR."\{bashPrefix}yarn fluid evaluate -f \{tempFile}");
            this.getDataset().forEach((key, path) -> {
                command.append(" -d \"(").append(key).append(", ").append(path).append(")\"");
            });
            this.getImports().forEach(path -> {
                command.append(" -i ").append(path);
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
            if(!errorOutput.isEmpty()) {
                logger.info(STR."Error output: \{errorOutput}");
            }
            return output;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error during the execution of the fluid evaluate command", e);
        }
    }

    public Optional<String> validate(String output) throws Exception {
        logger.info(STR."Validating output: \{output}");
        //Extract value from input query.text
        Optional<TextFragment> textFragment = paragraph.stream().filter(t -> t.getValue().contains("[REPLACE")).findFirst();
        if(textFragment.isEmpty()) {
            throw new RuntimeException("REPLACE tag missing");
        }
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

    private void writeFluidFile(String response, String path) throws FileNotFoundException {
        PrintWriter out = new PrintWriter(STR."fluid/\{path}.fld");
        out.println(this.getCode());
        out.println(STR."in \{response}");
        out.flush();
        out.close();
    }
}
