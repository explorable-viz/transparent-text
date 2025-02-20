package explorableviz.transparenttext;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Settings {

    private static Settings instance;
    private JSONObject settings;
    public static Settings getInstance() {
        if(instance == null) instance = new Settings();
        return instance;
    }

    public void loadSettings(String settingsPath) throws IOException {
        File f = new File(settingsPath);
        Path p = Paths.get(f.toURI());
        String content_settings = new String(Files.readAllBytes(p));
        this.settings = new JSONObject(content_settings);
    }

    private String get(String key) {
        return this.settings.getString(key);
    }

    public JSONObject getSettings() {
        return settings;
    }

    public int getLimit() {
        return Integer.parseInt(this.get("agent-limit"));
    }

    public String getTempWorkingPath() {
        return this.get("fluid-temp-folder");
    }

}
