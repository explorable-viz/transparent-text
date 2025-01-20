package explorableviz.transparenttext;

import org.json.JSONObject;

public class QueryContext {

    private String dataset;

    private String imports;

    private String file;


    public QueryContext(String dataset, String imports, String file) {
        this.dataset = dataset;
        this.imports = imports;
        this.file = file;
    }

    public static QueryContext importFromJson(JSONObject object) {
        String dataset = object.getString("data");
        String imports = object.getString("code");
        String file = object.getString("text");
        return new QueryContext(dataset, imports, file);
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getImports() {
        return imports;
    }

    public void setImports(String imports) {
        this.imports = imports;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }


    public void writeFiles() {


    }

    @Override
    public String toString() {
        JSONObject object = new JSONObject();
        object.put("dataset", this.dataset);
        object.put("code", this.imports);
        object.put("text", this.file);
        return object.toString();
    }
}
