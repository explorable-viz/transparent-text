package explorableviz.transparenttext.textfragment;

import explorableviz.transparenttext.QueryContext;

import java.util.Map;

public class Expression extends TextFragment {

    private final String expr;
    public Expression(String expr, String value) {
        super(value);
        this.expr = expr;
    }

    public String getExpr() {
        return expr;
    }

    @Override
    public Expression clone() {
        return new Expression(expr, getValue());
    }

    public TextFragment replace(Map<String, String> computedVariables) {
        return new Expression(QueryContext.replaceVariables(expr, computedVariables), getValue());
    }
}
