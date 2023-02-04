package ru.yandex.solomon.labels.query;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;

import ru.yandex.solomon.model.protobuf.MatchType;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class SelectorTypeTest {
    @Test
    public void absentNumberDuplicate() {
        List<SelectorType> duplicateNumbers = Stream.of(SelectorType.values())
                .collect(groupingBy(SelectorType::getNumber, toList()))
                .values()
                .stream()
                .filter(same -> same.size() > 1)
                .flatMap(Collection::stream)
                .collect(toList());

        assertThat(duplicateNumbers, emptyIterable());
    }

    @Test
    public void glob() {
        SelectorType result = SelectorType.forOperator("=");
        assertThat(result, equalTo(SelectorType.GLOB));
    }

    @Test
    public void notGlob() {
        SelectorType result = SelectorType.forOperator("!=");
        assertThat(result, equalTo(SelectorType.NOT_GLOB));
    }

    @Test
    public void exact() {
        SelectorType result = SelectorType.forOperator("==");
        assertThat(result, equalTo(SelectorType.EXACT));
    }

    @Test
    public void notExact() {
        SelectorType result = SelectorType.forOperator("!==");
        assertThat(result, equalTo(SelectorType.NOT_EXACT));
    }

    @Test
    public void regexp() {
        SelectorType result = SelectorType.forOperator("=~");
        assertThat(result, equalTo(SelectorType.REGEX));
    }

    @Test
    public void noRegexp() {
        SelectorType result = SelectorType.forOperator("!~");
        assertThat(result, equalTo(SelectorType.NOT_REGEX));
    }

    @Test
    public void findByNumber() {
        for (SelectorType expect : SelectorType.values()) {
            SelectorType found = SelectorType.forNumber(expect.getNumber());
            assertThat(found, equalTo(expect));
        }
    }

    @Test
    public void createSelectorViaType() {
        for (SelectorType expect : SelectorType.values()) {
            Selector selector = expect.create("a", "b");
            assertThat(selector.getType(), equalTo(expect));
        }
    }

    @Test
    public void numbersConsistentWithProtobuf() {
        for (SelectorType expect : SelectorType.values()) {
            MatchType matchType = MatchType.forNumber(expect.getNumber());
            assertThat(expect.name(), equalTo(matchType.name()));
        }
    }
}
