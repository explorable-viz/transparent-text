package explorableviz.transparenttext;

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
    private String paragraph;

    private String expected;

    private String response;

    public QueryContext(HashMap<String, String> dataset, ArrayList<String> imports, String code, String paragraph) throws IOException {
        this.dataset = dataset;
        this.imports = imports;
        this.paragraph = paragraph;
        this.code = code;
        this._loadedImports = new ArrayList<>();
        this._loadedDatasets = new HashMap<>();
        loadFiles();
    }

    public QueryContext(HashMap<String, String> dataset, ArrayList<String> imports, String code, String file, String expected) throws IOException {
        this(dataset,imports,code,file);
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

    public ArrayList<String> get_loadedImports() {
        return _loadedImports;
    }

    public void setImports(ArrayList<String> imports) {
        this.imports = imports;
    }

    public String getParagraph() {
        return paragraph;
    }

    public void loadFiles() throws IOException {
        for(Map.Entry<String, String> dataset : this.dataset.entrySet()) {
            String path = "fluid/" + dataset.getValue();
            this._loadedDatasets.put(dataset.getKey(), new String(Files.readAllBytes(Paths.get(new File(path + ".fld").toURI()))));
        }
        for(String path : imports)  {
            path = "fluid/" + path;
            this._loadedImports.add(new String(Files.readAllBytes(Paths.get(new File(path + ".fld").toURI()))));
        }
    }
    @Override
    public String toString() {
        JSONObject object = new JSONObject();
        object.put("loadedDatasets", this._loadedDatasets);
        object.put("loadedImports", this._loadedImports);
        object.put("code", this.code);
        object.put("paragraph", this.paragraph);
        return object.toString();
    }

    public String getExpected() {
        return expected;
    }

    public void setExpected(String expected) {
        this.expected = expected;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    /**
     * Executes the validation task, generating a fluid program
     * and compiling it.
     * @return null
     */
    public Optional<String> validate() {

        try {
            writeFluidFile(this.response);

            //Generate the fluid program that will be processed and evaluated by the compiler
            String tempFile = Settings.getInstance().get(Settings.FLUID_TEMP_FILE);
            String os = System.getProperty("os.name").toLowerCase();
            String bashPrefix = os.contains("win") ? "cmd.exe /c " : "";

            //Command construction
            StringBuilder command = new StringBuilder(bashPrefix + "yarn fluid evaluate -f " + tempFile);
            this.getDataset().forEach((key, path) -> {
                command.append(" -d \"(").append(key).append(", ").append(path).append(")\"");
            });
            this.getImports().forEach(path -> {
                command.append(" -i ").append(path);
            });
            logger.info("Running command: " + command);
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

            logger.info("Command output: " + output);
            logger.info("Error output (if any): " + errorOutput);
            //Output validation
            return validateOutput(output, this.getParagraph());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error during validation", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks the validity of the given output against a specific pattern within a provided string.
     *
     * @param output the output to be validated
     * @param text   the string containing the pattern to match against the output
     * @return true if the output matches the pattern, false otherwise
     */

    private Optional<String> validateOutput(String output, String text) throws Exception {
        logger.info("Validating output: " + output);

        //Extract value from input query.text
        //The scenario [REPLACE value="SSP5-8.5"] framework foresees a considerable escalation in temperatures
        //Return: SSP5-8.5
        String regex = "value=\\\"(.*?)\\\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        if (!matcher.find()) {
            throw new Exception("No matching value found in text");
        }

        //expectedValue: SSP5-8.5
        String expectedValue = matcher.group(1);

        // Extract and clean the generated expression
        String[] outputLines = output.split("\n");
        if (outputLines.length < 2) {
            throw new Exception("Output format is invalid");
        }

        String value = outputLines[1].replaceAll("^\"|\"$", "");

        if (value.equals(expectedValue)) {
            logger.info("Validation passed");
            return Optional.empty();
        } else {
            logger.info("Validation failed: generated=" + value + ", expected=" + expectedValue);
            return Optional.of(value);
        }
    }

    /**
     * This function write the generated fluid code in a file
     *
     * @param response: the expression generated by the LLM
     * @throws FileNotFoundException
     */
    private void writeFluidFile(String response) throws FileNotFoundException {
        PrintWriter out = new PrintWriter(STR."fluid/\{Settings.getInstance().get(Settings.FLUID_TEMP_FILE)}.fld");
        out.println(this.getCode());
        out.println("in " + response);
        out.flush();
        out.close();
    }
}
