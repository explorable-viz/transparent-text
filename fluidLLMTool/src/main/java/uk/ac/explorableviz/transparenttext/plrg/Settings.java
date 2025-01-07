package uk.ac.explorableviz.transparenttext.plrg;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Settings {

    public static final String LIMIT = "agent-limit";
    private static Settings instance;
    public static final String LOG_PATH = "log-path";
    public static final String FLUID_COMPILER_PATH = "fluid-compiler-path";
    public static final String FLUID_TEMP_FILE = "fluid-temp-file";

    private JSONObject settings;
    public static Settings getInstance() {
        if(instance == null) instance = new Settings();
        return instance;
    }

    public void loadSettings(String settingsPath) throws IOException {
        String content_settings = new String(Files.readAllBytes(Paths.get(new File(settingsPath).toURI())));
        this.settings = new JSONObject(content_settings);
    }

    public String get(String key) {
        return this.settings.getString(key);
    }

    public JSONObject getSettings() {
        return settings;
    }
}
