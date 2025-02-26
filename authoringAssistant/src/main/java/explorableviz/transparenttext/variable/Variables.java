package explorableviz.transparenttext.variable;

import com.google.errorprone.annotations.Var;

import java.util.HashMap;
import java.util.Random;

public class Variables extends HashMap<String, ValueOptions> {

    public static Variables.Flat computeVariables(Variables variables, Random random) {
        return variables.entrySet().stream()
                .map(entry -> entry.getValue().expandVariable(random, entry.getKey()))
                .reduce(new Variables.Flat(), (acc, map) -> {
                    acc.putAll(map);
                    return acc;
                }, (m1, m2) -> {
                    m1.putAll(m2);
                    return m1;
                });
    }

    public static class Flat extends Variables {}
}
