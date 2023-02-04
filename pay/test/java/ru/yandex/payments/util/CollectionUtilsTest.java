package ru.yandex.payments.util;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptySet;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.payments.util.CollectionUtils.findMissing;
import static ru.yandex.payments.util.CollectionUtils.join;
import static ru.yandex.payments.util.CollectionUtils.map;
import static ru.yandex.payments.util.CollectionUtils.mapToList;
import static ru.yandex.payments.util.CollectionUtils.mapToMap;
import static ru.yandex.payments.util.CollectionUtils.mapToSet;
import static ru.yandex.payments.util.CollectionUtils.merge;
import static ru.yandex.payments.util.CollectionUtils.namesOf;
import static ru.yandex.payments.util.CollectionUtils.namesSetOf;
import static ru.yandex.payments.util.CollectionUtils.zip;

public class CollectionUtilsTest {
    @Test
    @DisplayName("Verify that CollectionsUtils.map returns expected collection")
    void testMap() {
        assertThat(map(List.of(1, 2, 42, 0), num -> num + 1, Collectors.toUnmodifiableSet()))
            .isInstanceOf(Set.class)
            .containsExactlyInAnyOrder(2, 3, 43, 1);

        assertThat(map(Set.of(1, 2, 42, 0), num -> num - 1, Collectors.toUnmodifiableList()))
            .isInstanceOf(List.class)
            .containsExactlyInAnyOrder(0, 1, 41, -1);
    }

    @Test
    @DisplayName("Verify that CollectionsUtils.mapToList returns expected collection")
    void testMapToList() {
        assertThat(mapToList(Set.of(1, 2, 0, -1), Object::toString))
            .isInstanceOf(List.class)
            .containsExactlyInAnyOrder("1", "2", "0", "-1");
    }

    @Test
    @DisplayName("Verify that CollectionsUtils.mapToSet returns expected collection")
    void testMapToSet() {
        assertThat(mapToSet(List.of(1, 2, 0, -1), Object::toString))
            .isInstanceOf(Set.class)
            .containsExactlyInAnyOrder("1", "2", "0", "-1");
    }

    @Test
    @DisplayName("Verify that CollectionsUtils.mapToMap returns expected collection")
    void testMapToMap() {
        assertThat(mapToMap(List.of("1", "a", "-"), identity(), value -> value + '!'))
            .isInstanceOf(Map.class)
            .containsExactlyInAnyOrderEntriesOf(
                Map.of(
                    "1", "1!",
                    "a", "a!",
                    "-", "-!"
                )
            );

        assertThat(mapToMap(List.of("1", "a", "-"), value -> value + '!'))
            .isInstanceOf(Map.class)
            .containsExactlyInAnyOrderEntriesOf(
                Map.of(
                    "1!", "1",
                    "a!", "a",
                    "-!", "-"
                )
            );
    }

    @Test
    @DisplayName("Verify that CollectionsUtils.zip returns expected stream")
    void testZip() {
        assertThat(zip(List.of(1, 2, 3), List.of(-1, -2, -3), Integer::sum).toImmutableList())
            .containsExactly(0, 0, 0);
    }

    private enum TestEnum {
        A,
        B
    }

    @Test
    @DisplayName("Verify that CollectionsUtils.namesOf returns stream containing enum members names")
    void testNamesOf() {
        assertThat(namesOf(TestEnum.class).toImmutableList())
            .containsExactly("A", "B");
    }

    @Test
    @DisplayName("Verify that CollectionsUtils.namesSetOf returns set containing enum members names")
    void testNamesSetOf() {
        assertThat(namesSetOf(TestEnum.class))
            .isInstanceOf(Set.class)
            .containsExactlyInAnyOrder("A", "B");
    }

    @Test
    @DisplayName("Verify that CollectionsUtils.findMissing returns stream containing elements missing in a search set")
    void testFindMissing() {
        assertThat(findMissing(List.of(42, 100500, 0), Set.of(100500, 12, -55)).toImmutableList())
            .containsExactlyInAnyOrder(42, 0);
    }

    @Test
    @DisplayName("Verify that CollectionsUtils.findMissing returns stream containing all needles elements while "
        + "haystack is empty")
    void testFindMissingInEmptyHaystack() {
        assertThat(findMissing(List.of(42, 100500, 0), emptySet()).toImmutableList())
            .containsExactlyInAnyOrder(42, 100500, 0);
    }

    @Test
    @DisplayName("Verify that CollectionsUtils.findMissing returns empty stream while needles is empty")
    void testFindMissingInEmptyNeedles() {
        assertThat(findMissing(emptySet(), List.of(42, 100500, 0)).toImmutableList())
            .isEmpty();
    }

    @Test
    @DisplayName("Verify that `join` method concatenates collection elements")
    void testJoin() {
        assertThat(join(List.of(1, 2, 3), ",", "[", "]", "(", ")"))
                .isEqualTo("[(1),(2),(3)]");
        assertThat(join(List.of(1, 2, 3), ",", "[", "]"))
                .isEqualTo("[1,2,3]");
        assertThat(join(List.of("a", "b", "c"), ","))
                .isEqualTo("a,b,c");
    }

    @Test
    @DisplayName("Verify that `merge` method correctly merge two sets")
    void testMerge() {
        assertThat(merge(Set.of(1, 2), Set.of(2, 3)))
                .containsExactlyInAnyOrder(1, 2, 3);
        assertThat(merge(emptySet(), emptySet()))
                .isEmpty();
    }
}
