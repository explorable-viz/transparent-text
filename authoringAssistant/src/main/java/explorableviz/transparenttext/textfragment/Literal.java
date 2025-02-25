package explorableviz.transparenttext.textfragment;

import explorableviz.transparenttext.QueryContext;
import explorableviz.transparenttext.variable.Variables;

public class Literal extends TextFragment {

    public Literal(String value) {
        super(value);
    }

    @Override
    public Object clone() {
        return new Literal(getValue());
    }

    @Override
    public TextFragment replace(Variables computedVariables) {
        return new Literal(Variables.replaceVariables(getValue(), computedVariables));
    }
}
