package ru.yandex.solomon.labels.protobuf;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ru.yandex.solomon.labels.query.Selector;
import ru.yandex.solomon.labels.query.Selectors;
import ru.yandex.solomon.model.protobuf.MatchType;

import static org.junit.Assert.assertEquals;

/**
 * @author Oleg Baryshnikov
 */
public class LabelSelectorConverterTest {

    @Test
    public void protoToSelectors() {
        var protoSelectors = Arrays.asList(
            protoSelector("host", "-", MatchType.ABSENT),
            protoSelector("host", "*", MatchType.ANY),
            protoSelector("host", "exact", MatchType.EXACT),
            protoSelector("host", "not exact", MatchType.NOT_EXACT),
            protoSelector("host", "glob", MatchType.GLOB),
            protoSelector("host", "not glob", MatchType.NOT_GLOB),
            protoSelector("host", "regex", MatchType.REGEX),
            protoSelector("host", "not regex", MatchType.NOT_REGEX)
        );

        Selectors expectedSelectors = Selectors.of(
            Selector.absent("host"),
            Selector.any("host"),
            Selector.exact("host", "exact"),
            Selector.notExact("host", "not exact"),
            Selector.glob("host", "glob"),
            Selector.notGlob("host", "not glob"),
            Selector.regex("host", "regex"),
            Selector.notRegex("host", "not regex")
        );

        Selectors actualSelectors = LabelSelectorConverter.protoToSelectors(protoSelectors);
        assertEquals(expectedSelectors, actualSelectors);
    }

    @Test
    public void selectorsToProto() {
        Selectors selectors = Selectors.of(
            Selector.absent("host"),
            Selector.any("host"),
            Selector.exact("host", "exact"),
            Selector.notExact("host", "not exact"),
            Selector.glob("host", "glob"),
            Selector.notGlob("host", "not glob"),
            Selector.regex("host", "regex"),
            Selector.notRegex("host", "not regex")
        );

        List<ru.yandex.solomon.model.protobuf.Selector> expectedProtoSelectors = Arrays.asList(
            protoSelector("host", "-", MatchType.ABSENT),
            protoSelector("host", "*", MatchType.ANY),
            protoSelector("host", "exact", MatchType.EXACT),
            protoSelector("host", "not exact", MatchType.NOT_EXACT),
            protoSelector("host", "glob", MatchType.GLOB),
            protoSelector("host", "not glob", MatchType.NOT_GLOB),
            protoSelector("host", "regex", MatchType.REGEX),
            protoSelector("host", "not regex", MatchType.NOT_REGEX)
        );

        var actualProtoSelectors = LabelSelectorConverter.selectorsToProto(selectors);
        assertEquals(expectedProtoSelectors, actualProtoSelectors);
    }

    private static ru.yandex.solomon.model.protobuf.Selector protoSelector(String key, String pattern, MatchType matchType) {
        return ru.yandex.solomon.model.protobuf.Selector.newBuilder()
                .setKey(key)
                .setPattern(pattern)
                .setMatchType(matchType)
                .build();
    }
}
