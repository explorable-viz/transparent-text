package explorableviz.transparenttext;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Settings {

    private static Settings instance;
    private final JSONObject settings;
    private static Settings getInstance() {
        if(instance == null) throw new AssertionError("You have to call init first");
        return instance;
    }

    public static Settings init(String settingsPath) throws IOException {
        instance = new Settings(settingsPath);
        return instance;
    }
    private Settings(String settingsPath) throws IOException {
        this.settings = new JSONObject(new String(Files.readAllBytes(Paths.get(new File(settingsPath).toURI()))));
    }

    private String get(String key) {
        return this.settings.getString(key);
    }

    public static JSONObject getSettings() {
        return getInstance().settings;
    }

    public static int getLimit() {
        return Integer.parseInt(getInstance().get("agent-limit"));
    }

    public static String getFluidTempFolder() {
        return getInstance().get("fluid-temp-folder");
    }

    public static String getLibrariesBasePath() {
        return getInstance().get("base-path-library");
    }
    public static String getFluidCommonFolder() {
        return getInstance().get("fluid-common-folder");
    }
    public static String getTemperature() {
        return getInstance().get("temperature");
    }
    public static String getNumContextToken() {
        return getInstance().get("num_ctx");
    }
    public static String getLogFolder() {
        return getInstance().get("log-folder");
    }
    public static boolean isReasoningEnabled() {
        return getInstance().get("enable-reasoning").equals("true");
    }
    public static String getLearningCaseFolder() {
        return getInstance().get("learning-case-folder");
    }
    public static String getTestCaseFolder() {
        return getInstance().get("test-case-folder");
    }

}
