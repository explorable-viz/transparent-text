package explorableviz.transparenttext.textfragment;

public class Expression extends TextFragment {

    private String expr;
    public Expression(String expr, String value) {
        this.expr = expr;
        this.setValue(value);
    }

    public String getExpr() {
        return expr;
    }

    public void setExpr(String expr) {
        this.expr = expr;
    }

    @Override
    public Expression clone() {
        return new Expression(expr, getValue());
    }
}
