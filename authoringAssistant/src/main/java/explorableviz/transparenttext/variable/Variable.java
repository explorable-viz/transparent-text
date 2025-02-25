package explorableviz.transparenttext.variable;

import java.util.HashMap;
import java.util.Set;

public abstract class Variable {
    public abstract Object get();

    public static class StringVariable extends Variable {
        private final String value;

        public StringVariable(String value) {
            this.value = value;
        }

        @Override
        public String get() {
            return value;
        }
    }

    public static class Int extends Variable {
        private final int value;

        public Int(int value) {
            this.value = value;
        }

        @Override
        public Integer get() {
            return value;
        }
    }

    public static class Map extends Variable {
        private final HashMap<String, String> value;

        public Map(HashMap<String, String> value) {
            this.value = value;
        }

        public void add(String key, String value) {
            this.value.put(key, value);
        }
        @Override
        public HashMap<String, String> get() {
            return value;
        }
        public Set<String> keySet() {
            return this.value.keySet();
        }
    }

    public static class List extends Variable {
        private final java.util.List<Variable> value;

        public List(java.util.List<Variable> value) {
            this.value = value;
        }

        @Override
        public java.util.List<Variable> get() {
            return value;
        }
    }
}


