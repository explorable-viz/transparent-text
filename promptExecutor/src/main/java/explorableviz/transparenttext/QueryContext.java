package explorableviz.transparenttext;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class QueryContext {

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
    private String file;

    private String expected;

    public QueryContext(HashMap<String, String> dataset, ArrayList<String> imports, String code, String file) throws IOException {
        this.dataset = dataset;
        this.imports = imports;
        this.file = file;
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

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
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
        object.put("text", this.file);
        return object.toString();
    }

    public String getExpected() {
        return expected;
    }

    public void setExpected(String expected) {
        this.expected = expected;
    }
}
