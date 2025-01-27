package explorableviz.transparenttext;

public class ValidationResult {

    private final String log, value;
    private final boolean valid;
    ValidationResult(boolean valid, String log, String value) {
        this.valid = valid;
        this.log = log;
        this.value = value;

    }

    public String getLog() {
        return log;
    }

    public String getValue() {
        return value;
    }

    public boolean isValid() {
        return valid;
    }
}
