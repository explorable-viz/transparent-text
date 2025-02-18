package explorableviz.transparenttext.textfragment;

public class Expression extends TextFragment {

    private final String expr;
    public Expression(String expr, String value) {
        this.expr = expr;
        this.setValue(value);
    }

    public String getExpr() {
        return expr;
    }

    @Override
    public Expression clone() {
        return new Expression(expr, getValue());
    }
}
