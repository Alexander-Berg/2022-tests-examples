package ru.yandex.solomon.yasm.expression;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.solomon.yasm.expression.grammar.YasmExpression;
import ru.yandex.solomon.yasm.expression.grammar.YasmSelRenderer;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class RenderTestSupport {

    private static final String DEFAULT_PROJECT_PREFIX = "yasm_";

    public static String renderTags(Map<String, String> tags) {
        return renderMultivaluedTags(tags.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> List.of(e.getValue())))
        );
    }

    public static String renderMultivaluedTags(Map<String, List<String>> tags) {
        return YasmSelRenderer.renderTags(tags, DEFAULT_PROJECT_PREFIX);
    }

    public static YasmSelRenderer.RenderResult renderExpression(String expression) {
        return YasmSelRenderer.render(YasmExpression.parse(expression), DEFAULT_PROJECT_PREFIX);
    }
}
