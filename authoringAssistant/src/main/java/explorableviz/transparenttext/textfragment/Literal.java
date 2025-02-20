package explorableviz.transparenttext.textfragment;

import explorableviz.transparenttext.QueryContext;

import java.util.Map;

public class Literal extends TextFragment {

    public Literal(String value) {
        super(value);
    }

    @Override
    public Object clone() {
        return new Literal(getValue());
    }

    @Override
    public TextFragment replace(Map<String, String> computedVariables) {
        return new Literal(QueryContext.replaceVariables(getValue(), computedVariables));
    }
}
