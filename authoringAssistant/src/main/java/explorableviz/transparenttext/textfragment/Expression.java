package explorableviz.transparenttext.textfragment;

public class Expression extends TextFragment {

    private String expr;
    public Expression(String expr) {
        this.expr = expr;
    }

    public String getExpr() {
        return expr;
    }

    public void setExpr(String expr) {
        this.expr = expr;
    }
}
