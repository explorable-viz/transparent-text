package explorableviz.transparenttext.paragraph;

import explorableviz.transparenttext.LiteralParts;
import explorableviz.transparenttext.variable.Variables;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Paragraph extends ArrayList<TextFragment> {

    public Paragraph(JSONArray json_paragraph, Variables variables, Map<String, String> expectedValue) {
        this.addAll(IntStream.range(0, json_paragraph.length())
                .mapToObj(i -> {
                    JSONObject paragraphElement = json_paragraph.getJSONObject(i);
                    String type = paragraphElement.getString("type");
                    String value = expectedValue.entrySet().stream()
                            .reduce(paragraphElement.getString("value"),
                                    (acc, entry) -> acc.replaceAll(STR."\\[ADD_VAL id=\"\{entry.getKey()}\"]", entry.getValue()),
                                    (a, _) -> a
                            );
                    return (switch (type) {
                        case "literal" -> new Literal(value);
                        case "expression" ->
                                new Expression(paragraphElement.getString("expression"), value);
                        default -> throw new RuntimeException(STR."\{type} type is invalid");
                    }).replace(variables);
                })
                .toList());
    }

    public String toString() {
        return STR."Paragraph([\{stream().map(e -> {
            if (e instanceof Literal) return STR."\"\{e.getValue()}\"";
            if (e instanceof Expression) return ((Expression) e).getExpr();
            throw new RuntimeException("Error, it is possible to have only String or Expression element");
        }).collect(Collectors.joining(","))}])";
    }

    public void spliceExpression(String expression) {
        List<TextFragment> paragraph = this;
        ListIterator<TextFragment> iterator = paragraph.listIterator();

        while (iterator.hasNext()) {
            TextFragment textFragment = iterator.next();
            if (textFragment instanceof Literal) {
                splitLiteral(textFragment).ifPresentOrElse(expectedValue -> {
                    iterator.remove();
                    iterator.add(expectedValue.beforeTag());
                    iterator.add(new Expression(expression, expectedValue.tag().getValue()));
                    iterator.add(expectedValue.afterTag());
                }, () -> {
                    throw new RuntimeException("REPLACE tag not found");
                });
            }
        }
    }

    public static Optional<LiteralParts> splitLiteral(TextFragment literal) {
        Matcher valueReplaceMatcher = Pattern.compile("(.*)\\[REPLACE id=\".*?\" value=\"(.*?)\"](.*)").matcher(literal.getValue());
        if (!valueReplaceMatcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new LiteralParts(new Literal(valueReplaceMatcher.group(1)), new Literal(valueReplaceMatcher.group(2)), new Literal(valueReplaceMatcher.group(3))));
    }
}
