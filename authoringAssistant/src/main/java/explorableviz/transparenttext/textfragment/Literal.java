package explorableviz.transparenttext.textfragment;

public class Literal extends TextFragment {

    public Literal(String value) {
        this.setValue(value);
    }

    @Override
    public Object clone() {
        return new Literal(getValue());
    }
}
