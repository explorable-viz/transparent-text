package explorableviz.transparenttext;

import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class FluidCLI {

    public final Logger logger = Logger.getLogger(this.getClass().getName());
    private final Query query;
    private final String fluidFileName = "llmTest";
    public FluidCLI(Query query) {
        this.query = query;
    }
    private String buildCommand() {
        String os = System.getProperty("os.name").toLowerCase();
        String bashPrefix = os.contains("win") ? "cmd.exe /c " : "";
        StringBuilder command = new StringBuilder(STR."\{bashPrefix}yarn fluid evaluate -l -p '\{Settings.getFluidTempFolder()}/' -f \{fluidFileName}");
        query.getDatasets().forEach((key, path) -> command.append(STR." -d \"(\{key}, ./\{path})\""));
        query.getImports().forEach(path -> command.append(STR." -i \{path}"));
        return command.toString();
    }
    private String executeCommand(String command) throws IOException, InterruptedException {
        String os = System.getProperty("os.name").toLowerCase();
        Process process;
        if (os.contains("win")) {
            process = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", command});
        } else {
            process = Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", command});
        }
        process.waitFor();

        String output = new String(process.getInputStream().readAllBytes());
        String errorOutput = new String(process.getErrorStream().readAllBytes());

        logger.info(STR."Command output: \{output}");
        if (!errorOutput.isEmpty()) {
            logger.info(STR."Error output: \{errorOutput}");
        }
        FileUtils.deleteDirectory(new File(Settings.getFluidTempFolder()));
        return output;
    }
    public String evaluate(String response) {
        try {
            writeFluidFiles(response);
            return executeCommand(buildCommand());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error during the execution of the fluid evaluate command", e);
        }
    }
    private void writeFluidFiles(String response) throws IOException {
        Files.createDirectories(Paths.get(Settings.getFluidTempFolder()));
        //Write temp fluid file
        try (PrintWriter out = new PrintWriter(STR."\{Settings.getFluidTempFolder()}/\{fluidFileName}.fld")) {
            out.println(query.getCode());
            out.println(response);
        }
        for (int i = 0; i < query.get_loadedImports().size(); i++) {
            Files.createDirectories(Paths.get(STR."\{Settings.getFluidTempFolder()}/\{query.getImports().get(i)}.fld").getParent());
            try (PrintWriter outData = new PrintWriter(STR."\{Settings.getFluidTempFolder()}/\{query.getImports().get(i)}.fld")) {
                outData.println(query.get_loadedImports().get(i));
            }
        }
        for (java.util.Map.Entry<String, String> dataset : query.getDatasets().entrySet()) {
            Files.createDirectories(Paths.get(STR."\{Settings.getFluidTempFolder()}/\{dataset.getValue()}.fld").getParent());
            try (PrintWriter outData = new PrintWriter(STR."\{Settings.getFluidTempFolder()}/\{dataset.getValue()}.fld")) {
                outData.println(query.get_loadedDatasets().get(dataset.getKey()));
            }
        }
    }
}

