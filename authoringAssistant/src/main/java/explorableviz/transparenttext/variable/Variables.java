package explorableviz.transparenttext.variable;

import java.util.HashMap;
import java.util.Random;

public class Variables extends HashMap<String, Variable> {

    public void addVariable(String varName, Variable value) {
        put(varName, value);
    }

    public static Variables computeVariables(Variables variables, Random random) {
        return variables.entrySet().stream()
                .map(entry -> expandVariableEntry(random, entry))
                .reduce(new Variables(), (acc, map) -> {
                    acc.putAll(map);
                    return acc;
                }, (m1, m2) -> {
                    m1.putAll(m2);
                    return m1;
                });
    }
    private static Variables expandVariableEntry(Random random, java.util.Map.Entry<String, Variable> variable) {
        Variables vars = new Variables();
        if (variable.getValue() instanceof Variable.StringVariable value) {
            vars.addVariable(variable.getKey(), switch (value.get()) {
                case "RANDOM_INT" -> new Variable.Number(random.nextInt(10));
                case "RANDOM_FLOAT" -> new Variable.Number(random.nextFloat() * 10);
                case "RANDOM_STRING" -> new Variable.StringVariable(getRandomString(8, random).toLowerCase());
                default -> value;
            });
        } else if (variable.getValue() instanceof Variable.List values) {
            Variable value = values.get().get(random.nextInt(values.get().size()));
            if(value instanceof Variable.Map mapValue) {
                mapValue.keySet().forEach(k -> vars.addVariable(STR."\{variable.getKey()}.\{k}", new Variable.StringVariable(mapValue.getValue(k))));
            } else if(value instanceof Variable.StringVariable || value instanceof Variable.Number) {
                vars.addVariable(variable.getKey(), value);
            }
        }
        return vars;
    }
    public static String replaceVariables(String textToReplace, Variables variables) {
        for (java.util.Map.Entry<String, Variable> var : variables.entrySet()) {
            String variablePlaceholder = STR."$\{var.getKey()}$";
            textToReplace = textToReplace.replace(variablePlaceholder, ((Variable.StringVariable) var.getValue()).get());
        }
        return textToReplace;
    }
    private static String getRandomString(int length, Random generator) {
        StringBuilder sb = new StringBuilder(length);
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        for (int i = 0; i < length; i++) {
            int randomIndex = generator.nextInt(characters.length());
            sb.append(characters.charAt(randomIndex));
        }
        return sb.toString();
    }
}
