package explorableviz.transparenttext;

import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class FluidCLI {

    public final Logger logger = Logger.getLogger(this.getClass().getName());
    private final Map<String, String> datasets;
    private final List<String> imports;

    public FluidCLI(Map<String, String> datasets, List<String> imports) {
        this.datasets = datasets;
        this.imports = imports;
    }

    private String buildCommand(String fluidFileName) {
        String os = System.getProperty("os.name").toLowerCase();
        String bashPrefix = os.contains("win") ? "cmd.exe /c " : "";
        StringBuilder command = new StringBuilder(STR."\{bashPrefix}yarn fluid evaluate -l -p '\{Settings.getFluidTempFolder()}/' -f \{fluidFileName}");
        datasets.forEach((key, path) -> command.append(STR." -d \"(\{key}, ./\{path})\""));
        imports.forEach(path -> command.append(STR." -i \{path}"));
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
    public String evaluate(String fluidFileName) {
        try {
            return executeCommand(buildCommand(fluidFileName));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error during the execution of the fluid evaluate command", e);
        }
    }
}
