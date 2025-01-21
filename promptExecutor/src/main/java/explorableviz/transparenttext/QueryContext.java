package explorableviz.transparenttext;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class QueryContext {

    private HashMap<String, String> dataset;
    private HashMap<String, String> _loadedDataset;

    private ArrayList<String> imports;
    private ArrayList<String> _loadedImports;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    private String code;
    private String file;


    public QueryContext(HashMap<String, String> dataset, ArrayList<String> imports, String code, String file) {
        this.dataset = dataset;
        this.imports = imports;
        this.file = file;
        this.code = code;
        this._loadedImports = new ArrayList<>();
        this._loadedDataset = new HashMap<>();
        loadFiles();
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

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public void loadFiles() {
        this.dataset.forEach((key,path) -> {
            path = "fluid/" + path;
            try {
                this._loadedDataset.put(key, new String(Files.readAllBytes(Paths.get(new File(path + ".fld").toURI()))));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        this.imports.forEach(path -> {
            path = "fluid/" + path;
            try {
                this._loadedImports.add(new String(Files.readAllBytes(Paths.get(new File(path + ".fld").toURI()))));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
    public void writeFiles() {
        //@todo generate the needed files for the fluid evaluation
    }

    @Override
    public String toString() {
        JSONObject object = new JSONObject();
        object.put("dataset", this._loadedDataset);
        object.put("code", this.code);
        object.put("text", this.file);
        return object.toString();
    }
}
