package explorableviz.transparenttext.textfragment;

import java.util.HashMap;
import java.util.Map;

public abstract class TextFragment {
    private final String value;

    protected TextFragment(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public abstract Object clone();

    public abstract TextFragment replace(Map<String, String> computedVariables);
}
