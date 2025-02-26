package explorableviz.transparenttext;

import explorableviz.transparenttext.textfragment.Expression;
import explorableviz.transparenttext.textfragment.Literal;
import explorableviz.transparenttext.textfragment.TextFragment;
import explorableviz.transparenttext.variable.ValueOptions;
import explorableviz.transparenttext.variable.Variables;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static explorableviz.transparenttext.variable.Variables.computeVariables;

public class QueryContext {

    public final Logger logger = Logger.getLogger(this.getClass().getName());
    private final java.util.Map<String, String> datasets;
    private final List<String> imports;
    private final ArrayList<String> _loadedImports;
    private final String fluidFileName = "llmTest";
    private final String code;
    private final List<TextFragment> paragraph;
    private final String expected;
    private final HashMap<String, String> _loadedDatasets;
    private final Variables variables;

    public QueryContext(java.util.Map<String, String> datasets, List<String> imports, String code, List<TextFragment> paragraph, Variables variables, String expected, String testCaseFileName) throws IOException {
        Variables computedVariables = computeVariables(variables, new Random(0));
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
                .collect(Collectors.toList());
        //Validation of the created object
        Optional<String> result = this.validate(this.evaluate(this.getExpected()));
        if (result.isPresent()) {
            throw new RuntimeException(STR."[testCaseFile=\{testCaseFileName}] Invalid test exception\{result}");
        }
    }

    public HashMap<String, String> loadDatasets() throws IOException {
        HashMap<String, String> loadedDatasets = new HashMap<>();
        for (java.util.Map.Entry<String, String> dataset : this.datasets.entrySet()) {
            loadedDatasets.put(dataset.getKey(), new String(Files.readAllBytes(Paths.get(new File(STR."\{Settings.getInstance().getFluidCommonFolder()}/\{dataset.getValue()}.fld").toURI()))));
        }
        return loadedDatasets;
    }

    private ArrayList<String> loadImports() throws IOException {
        ArrayList<String> loadedImports = new ArrayList<>();
        for (String path : this.getImports()) {
            File importLib = new File(STR."\{Settings.getInstance().getFluidCommonFolder()}/\{path}.fld");
            if (importLib.exists()) {
                loadedImports.add(new String(Files.readAllBytes(importLib.toPath())));
            } else {
                loadedImports.add(new String(Files.readAllBytes(Paths.get(STR."\{Settings.getInstance().getLibrariesBasePath()}/\{path}.fld"))));
            }
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
            String tempWorkingPath = Settings.getInstance().getFluidTempFolder();
            writeFluidFiles(response, tempWorkingPath);
            String os = System.getProperty("os.name").toLowerCase();
            String bashPrefix = os.contains("win") ? "cmd.exe /c " : "";

            //Command construction
            StringBuilder command = new StringBuilder(STR."\{bashPrefix}yarn fluid evaluate -l -p '\{tempWorkingPath}/' -f \{this.getFluidFileName()}");
            this.getDatasets().forEach((key, path) -> {
                command.append(STR." -d \"(\{key}, ./\{path})\"");
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
            //FileUtils.deleteDirectory(new File(tempWorkingPath));
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
        if (value.equals(expectedValue) || roundedEquals(value, expectedValue)) {
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
        for (int i = 0; i < this._loadedImports.size(); i++) {
            Files.createDirectories(Paths.get(STR."\{tempWorkingPath}/\{this.imports.get(i)}.fld").getParent());
            try (PrintWriter outData = new PrintWriter(STR."\{tempWorkingPath}/\{this.imports.get(i)}.fld")) {
                outData.println(this.get_loadedImports().get(i));
            }
        }
        for (java.util.Map.Entry<String, String> dataset : this.getDatasets().entrySet()) {
            Files.createDirectories(Paths.get(STR."\{tempWorkingPath}/\{dataset.getValue()}.fld").getParent());
            try (PrintWriter outData = new PrintWriter(STR."\{tempWorkingPath}/\{dataset.getValue()}.fld")) {
                outData.println(this.get_loadedDatasets().get(dataset.getKey()));
            }
        }
    }

    public static String replaceVariables(String textToReplace, Variables variables) {
        for (java.util.Map.Entry<String, ValueOptions> var : variables.entrySet()) {
            String variablePlaceholder = STR."$\{var.getKey()}$";
            textToReplace = textToReplace.replace(variablePlaceholder, String.valueOf(var.getValue().get()));
        }
        return textToReplace;
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

    public java.util.Map<String, String> getDatasets() {
        return datasets;
    }

    public List<String> getImports() {
        return imports;
    }

    public List<TextFragment> getParagraph() {
        return paragraph;
    }

    public Variables getVariables() {
        return variables;
    }

    public String getExpected() {
        return expected;
    }

    private String getFluidFileName() {
        return fluidFileName;
    }
}
