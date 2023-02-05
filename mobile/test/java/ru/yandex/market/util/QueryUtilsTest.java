package ru.yandex.market.util;


import android.os.Build;

import com.google.gson.Gson;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import kotlin.Pair;
import ru.yandex.market.BaseTest;
import ru.yandex.market.data.filters.filter.EnumFilter;
import ru.yandex.market.data.filters.filter.NumericFilter;
import ru.yandex.market.data.filters.filter.TextFilter;
import ru.yandex.market.data.filters.filter.filterValue.FilterValue;
import ru.yandex.market.data.filters.filter.filterValue.NumericFilterValue;
import ru.yandex.market.data.filters.sort.FilterSort;
import ru.yandex.market.data.filters.sort.SortsViewModel;
import ru.yandex.market.data.navigation.SimplifiedFilterValue;
import ru.yandex.market.data.navigation.TextSimplifiedFilterValue;
import ru.yandex.market.filters.FiltersDictionary;
import ru.yandex.market.gson.GsonFactory;
import ru.yandex.market.util.query.QueryUtils;
import ru.yandex.market.util.query.Queryable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.P})
public class QueryUtilsTest extends BaseTest {

    @Test
    public void testParsePairEmpty() {
        Assert.assertNull(QueryUtils.parsePair(null));
        Assert.assertNull(QueryUtils.parsePair(""));
        Assert.assertNull(QueryUtils.parsePair(" "));
        Assert.assertNull(QueryUtils.parsePair("    "));
    }

    @Test
    public void testParsePairSimple() {
        final String name = "name";
        final String value = "value";
        String query = name + Queryable.EQUALS + value;
        Pair<String, String[]> pair = QueryUtils.parsePair(query);
        String message = "Check " + query;
        Assert.assertNotNull(message, pair);
        Assert.assertEquals(message, name, pair.getFirst());
        Assert.assertNotNull(message, pair.getSecond());
        Assert.assertEquals(message, 1, pair.getSecond().length);
        Assert.assertEquals(message, value, pair.getSecond()[0]);
    }

    @Test
    public void testParsePairArray() {
        final String name = "name";
        final String[] values = {"value1", "value2", "значение3"};
        String value = QueryUtils.getEncodedString(values[0]) + Queryable.PARAM_DELIMITER +
                QueryUtils.getEncodedString(values[1]) +
                Queryable.PARAM_DELIMITER + QueryUtils.getEncodedString(values[2]);
        String[] queries = {
                name + Queryable.EQUALS + value,
                name + Queryable.EQUALS + value
        };
        for (String query : queries) {
            String message = "Check " + query;
            Pair<String, String[]> pair = QueryUtils.parsePair(query);
            Assert.assertNotNull(message, pair);
            Assert.assertEquals(message, name, pair.getFirst());
            Assert.assertNotNull(message, pair.getSecond());
            Assert.assertEquals(message, values.length, pair.getSecond().length);
            for (int i = 0; i < values.length; i++) {
                Assert.assertEquals(message, values[i], pair.getSecond()[i]);
            }
        }
    }

    @Test
    public void testTextFilterValueToQuery() {
        List<Queryable> queryables = QueryUtils.convertToQueryableList(
                null,
                Arrays.asList(new SimplifiedFilterValue("12", "23"),
                        new TextSimplifiedFilterValue(
                                FiltersDictionary.ID_TEXT,
                                "текст",
                                "rs1",
                                "shown [Text]"
                        )
                )
        );

        List<String> reqStrings = Arrays.asList(
                "",
                "12=23",
                "-8=" + QueryUtils.getEncodedString("текст") + "&rs=rs1");
        List<String> strings = Arrays.asList(
                "",
                "12=23",
                "-8=" + QueryUtils.getEncodedString("текст") + ",rs1," +
                        QueryUtils.getEncodedString("shown [Text]")
        );
        assertEquals(queryables.size(), reqStrings.size());
        assertEquals(queryables.size(), strings.size());
        for (int i = 0; i < queryables.size(); i++) {
            Queryable queryable = queryables.get(i);
            if (queryable == null) {
                assertNull(strings.get(i));
            } else {
                assertEquals("same req", reqStrings.get(i), queryable.toQuery(true));
                assertEquals("same str", strings.get(i), queryable.toQuery(false));
            }
        }
        queryables = QueryUtils.convertToQueryableList(
                null,
                Arrays.asList(
                        new SimplifiedFilterValue("12", "23"),
                        new TextSimplifiedFilterValue(
                                FiltersDictionary.ID_TEXT,
                                "текст",
                                null,
                                "shown [Text]"
                        )
                )
        );

        reqStrings = Arrays.asList("", "12=23",
                "-8=" + QueryUtils.getEncodedString("текст"));
        strings = Arrays.asList(
                "",
                "12=23",
                "-8=" + QueryUtils.getEncodedString("текст") + ",," +
                        QueryUtils.getEncodedString("shown [Text]")
        );
        assertEquals(queryables.size(), reqStrings.size());
        assertEquals(queryables.size(), strings.size());
        for (int i = 0; i < queryables.size(); i++) {
            Queryable queryable = queryables.get(i);
            if (queryable == null) {
                assertNull(strings.get(i));
            } else {
                assertEquals("same req", reqStrings.get(i), queryable.toQuery(true));
                assertEquals("same str", strings.get(i), queryable.toQuery(false));
            }
        }

    }

    @Test
    public void testValueToQueryEmpty() {
        final String emptyQuery = "";
        Assert.assertEquals(emptyQuery, QueryUtils
                .valueToQuery(null, Queryable.QUERY_DELIMITER, true));
        final Queryable emptyQueryable = (final boolean toRequest) -> null;
        Assert.assertEquals(emptyQuery, QueryUtils.valueToQuery(emptyQuery
                , Queryable.QUERY_DELIMITER, true));
        Assert.assertEquals(emptyQuery, QueryUtils.valueToQuery(new ArrayList<>()
                , Queryable.QUERY_DELIMITER, true));
        Assert.assertEquals(emptyQuery, QueryUtils.valueToQuery(
                new ArrayList<Queryable>() {{
                    add(emptyQueryable);
                }}
                , Queryable.QUERY_DELIMITER, true));
        Assert.assertEquals(emptyQuery, QueryUtils.valueToQuery(new String[0]
                , Queryable.QUERY_DELIMITER, true));
        Assert.assertEquals(emptyQuery, QueryUtils.valueToQuery(new Queryable[0]
                , Queryable.QUERY_DELIMITER, true));
        Assert.assertEquals(emptyQuery, QueryUtils.valueToQuery(new Queryable[]{emptyQueryable}
                , Queryable.QUERY_DELIMITER, true));
    }

    @Test
    public void testValueToQuerySimple() {
        final String name = "name";
        final String value = "value";
        String query = name + Queryable.EQUALS + value;
        final Queryable simpleQueryable = (final boolean toRequest) -> query;
        Assert.assertEquals(query, QueryUtils.valueToQuery(simpleQueryable
                , Queryable.QUERY_DELIMITER, true));
        Assert.assertEquals(query, QueryUtils.valueToQuery(
                new ArrayList<Queryable>() {{
                    add(simpleQueryable);
                }}
                , Queryable.QUERY_DELIMITER, true));
        Assert.assertEquals(query, QueryUtils.valueToQuery(new Queryable[]{simpleQueryable}
                , Queryable.QUERY_DELIMITER, true));

    }

    @Test
    public void testValueToQueryComplicated() {
        SortsViewModel sortsViewModel = SortsViewModel.empty();
        FilterSort filterSort = new FilterSort();
        FilterSort.FieldType sortValue = FilterSort.FieldType.DATE;
        filterSort.setField(sortValue);
        FilterSort.FilterSortOrder howValue = FilterSort.FilterSortOrder.ASC;
        filterSort.setOptions(Collections.singletonList(new FilterSort.FilterSortOption(null
                , howValue)));
        sortsViewModel.setCheckedValue(filterSort);
        String query = sortsViewModel.toQuery(true);
        Assert.assertEquals(
                FilterSort.QUERY_PART_SORT + Queryable.EQUALS + sortValue.name()
                        + Queryable.QUERY_DELIMITER
                        + FilterSort.QUERY_PART_HOW + Queryable.EQUALS + howValue.name()
                , query);
        Assert.assertEquals(query, QueryUtils.valueToQuery(sortsViewModel
                , Queryable.QUERY_DELIMITER, true));
        Assert.assertEquals(query, QueryUtils.valueToQuery(
                new ArrayList<Queryable>() {{
                    add(sortsViewModel);
                }}
                , Queryable.QUERY_DELIMITER, true));
        Assert.assertEquals(query, QueryUtils.valueToQuery(new Queryable[]{sortsViewModel}
                , Queryable.QUERY_DELIMITER, true));
    }

    @Test
    public void testQueryToValuesEmpty() {
        Assert.assertNotNull(QueryUtils.queryToValues(null));
        Assert.assertEquals(QueryUtils.queryToValues(null).size(), 0);
        Assert.assertNotNull(QueryUtils.queryToValues(""));
        Assert.assertEquals(QueryUtils.queryToValues("").size(), 0);
        Assert.assertNotNull(QueryUtils.queryToValues(" "));
        Assert.assertEquals(QueryUtils.queryToValues(" ").size(), 0);
        Assert.assertNotNull(QueryUtils.queryToValues("   "));
        Assert.assertEquals(QueryUtils.queryToValues("   ").size(), 0);
    }

    @Test
    public void testQueryToValuesSimple() {
        final String name = "name";
        final String value = "value";
        String query = name + Queryable.EQUALS + value;
        List<Pair<String, String[]>> pairs = QueryUtils.queryToValues(query);
        Assert.assertNotNull(pairs);
        Assert.assertEquals(pairs.size(), 1);
        Assert.assertEquals(pairs.get(0).getFirst(), name);
        Assert.assertEquals(pairs.get(0).getSecond().length, 1);
        Assert.assertEquals(pairs.get(0).getSecond()[0], value);
    }

    @Test
    public void testQueryToValuesNumeric() {
        final String name = "666";
        final String maxValueChecked = "5,5";
        final String minValueChecked = "1,5";
        List<Queryable> queryables = new ArrayList<>();
        NumericFilter filter = new NumericFilter();
        filter.setId(name);
        filter.setMin("0");
        filter.setMax("100");
        filter.setMaxChecked(maxValueChecked);
        filter.setMinChecked(minValueChecked);
        queryables.add(filter);
        String query = QueryUtils.valueToQuery(queryables, Queryable.QUERY_DELIMITER, true);
        //666=1%2C5%7E5%2C5
        Assert.assertEquals(name + Queryable.EQUALS + QueryUtils.getEncodedString(minValueChecked +
                NumericFilterValue.DELIMITER + maxValueChecked), query);
        List<Pair<String, String[]>> pairs = QueryUtils.queryToValues(query);
        Assert.assertNotNull(pairs);
        Assert.assertEquals(pairs.size(), 1);
        Assert.assertEquals(pairs.get(0).getFirst(), name);
        Assert.assertEquals(pairs.get(0).getSecond().length, 1);
        Assert.assertEquals(pairs.get(0).getSecond()[0], "1,5~5,5");
    }

    @Test
    public void testQueryToValuesComplicated() {
        final String nameSimple = "nameSimple";
        final String valueSimple = "valueSimple";
        final String nameEnum = FiltersDictionary.ID_VENDOR;
        final String[] valuesEnum = {"123", "124", "значение"};
        final String valueEnum = QueryUtils.getEncodedString(valuesEnum[0])
                + Queryable.PARAM_DELIMITER + QueryUtils.getEncodedString(valuesEnum[1])
                + Queryable.PARAM_DELIMITER + QueryUtils.getEncodedString(valuesEnum[2]);

        TextSimplifiedFilterValue textValue = new TextSimplifiedFilterValue(
                FiltersDictionary.ID_TEXT,
                "текст",
                "rs1",
                "[shown] text"
        );

        final SortsViewModel sortsViewModel = SortsViewModel.empty();
        final FilterSort filterSort = new FilterSort();
        final FilterSort.FieldType sortValue = FilterSort.FieldType.DATE;
        filterSort.setField(sortValue);
        final FilterSort.FilterSortOrder howValue = FilterSort.FilterSortOrder.ASC;
        filterSort.setOptions(Collections.singletonList(new FilterSort.FilterSortOption(null
                , howValue)));
        sortsViewModel.setCheckedValue(filterSort);
        final EnumFilter enumFilter = new EnumFilter();
        enumFilter.setId(nameEnum);
        enumFilter.setCheckedValue(new ArrayList<FilterValue>() {{
            for (String id : valuesEnum) {
                add(new FilterValue(id, ""));
            }
        }});
        List<Queryable> queryables = new ArrayList<>();
        queryables.add(filterSort);
        queryables.add((final boolean toRequest) -> nameSimple + Queryable.EQUALS + valueSimple);
        queryables.add(enumFilter);
        queryables.add(QueryUtils.convertToQueryableList(null, Arrays.asList(textValue)).get(1));
        String query = QueryUtils.valueToQuery(queryables, Queryable.QUERY_DELIMITER, true);
        Assert.assertEquals(
                FilterSort.QUERY_PART_SORT + Queryable.EQUALS + sortValue.name()
                        + Queryable.QUERY_DELIMITER
                        + FilterSort.QUERY_PART_HOW + Queryable.EQUALS + howValue.name()
                        + Queryable.QUERY_DELIMITER
                        + nameSimple + Queryable.EQUALS + valueSimple
                        + Queryable.QUERY_DELIMITER
                        + nameEnum + Queryable.EQUALS + valueEnum
                        + Queryable.QUERY_DELIMITER
                        + textValue.getId() + Queryable.EQUALS +
                        QueryUtils.getEncodedString(textValue.getValue()) +
                        Queryable.QUERY_DELIMITER +
                        TextFilter.RS + Queryable.EQUALS + textValue.getReportState()
                , query);
        List<Pair<String, String[]>> pairs = QueryUtils.queryToValues(query);

        assertEqualsGson(pairs, QueryUtils.queryToValues("http://yandex.ru?" + query));
        assertEqualsGson(pairs, QueryUtils.queryToValues("https://yandex.ru?" + query));
        Assert.assertNotNull(pairs);
        Assert.assertEquals(6, pairs.size());
        Assert.assertEquals(FilterSort.QUERY_PART_SORT, pairs.get(0).getFirst());
        Assert.assertEquals(1, pairs.get(0).getSecond().length);
        Assert.assertEquals(sortValue.name(), pairs.get(0).getSecond()[0]);
        Assert.assertEquals(FilterSort.QUERY_PART_HOW, pairs.get(1).getFirst());
        Assert.assertEquals(1, pairs.get(1).getSecond().length);
        Assert.assertEquals(howValue.name(), pairs.get(1).getSecond()[0]);
        Assert.assertEquals(nameSimple, pairs.get(2).getFirst());
        Assert.assertEquals(1, pairs.get(2).getSecond().length);
        Assert.assertEquals(valueSimple, pairs.get(2).getSecond()[0]);
        Assert.assertEquals(nameEnum, pairs.get(3).getFirst());
        Assert.assertEquals(valuesEnum.length, pairs.get(3).getSecond().length);
        for (int i = 0; i < valuesEnum.length; i++) {
            Assert.assertEquals(valuesEnum[i], pairs.get(3).getSecond()[i]);
        }
        Assert.assertEquals(textValue.getId(), pairs.get(4).getFirst());
        Assert.assertEquals(1, pairs.get(4).getSecond().length);
        Assert.assertEquals(textValue.getValue(), pairs.get(4).getSecond()[0]);

        Assert.assertEquals(TextFilter.RS, pairs.get(5).getFirst());
        Assert.assertEquals(1, pairs.get(5).getSecond().length);
        Assert.assertEquals(textValue.getReportState(), pairs.get(5).getSecond()[0]);


        testNoRequestSerialization(nameSimple, valueSimple, nameEnum, valueEnum, textValue,
                sortValue, howValue, queryables);

        textValue = new TextSimplifiedFilterValue(FiltersDictionary.ID_TEXT, "текст", null,
                "[shown] text");

        testNoRsTextComplicated(nameSimple, valueSimple, nameEnum, valueEnum, textValue, filterSort,
                sortValue, howValue, enumFilter);

    }

    private void testNoRequestSerialization(final String nameSimple, final String valueSimple,
            final String nameEnum, final String valueEnum,
            final TextSimplifiedFilterValue textValue, final FilterSort.FieldType sortValue,
            final FilterSort.FilterSortOrder howValue, final List<Queryable> queryables) {
        final String query;
        final List<Pair<String, String[]>> pairs;
        query = QueryUtils.valueToQuery(queryables, Queryable.QUERY_DELIMITER, false);
        Assert.assertEquals(
                FilterSort.QUERY_PART_SORT + Queryable.EQUALS + sortValue.name()
                        + Queryable.QUERY_DELIMITER
                        + FilterSort.QUERY_PART_HOW + Queryable.EQUALS + howValue.name()
                        + Queryable.QUERY_DELIMITER
                        + nameSimple + Queryable.EQUALS + valueSimple
                        + Queryable.QUERY_DELIMITER
                        + nameEnum + Queryable.EQUALS + valueEnum
                        + Queryable.QUERY_DELIMITER
                        + textValue.getId() + Queryable.EQUALS +
                        QueryUtils.getEncodedString(textValue.getValue()) +
                        Queryable.PARAM_DELIMITER + textValue.getReportState() +
                        Queryable.PARAM_DELIMITER +
                        QueryUtils.getEncodedString(textValue.getShownText())
                , query
        );
        pairs = QueryUtils.queryToValues(query);
        Assert.assertNotNull(pairs);
        Assert.assertEquals(5, pairs.size());

        Assert.assertEquals(textValue.getId(), pairs.get(4).getFirst());
        Assert.assertEquals(3, pairs.get(4).getSecond().length);
        Assert.assertEquals(textValue.getValue(), pairs.get(4).getSecond()[0]);
        Assert.assertEquals(textValue.getReportState(), pairs.get(4).getSecond()[1]);
        Assert.assertEquals(textValue.getShownText(), pairs.get(4).getSecond()[2]);
    }

    private void testNoRsTextComplicated(final String nameSimple, final String valueSimple,
            final String nameEnum, final String valueEnum,
            final TextSimplifiedFilterValue textValue, final FilterSort filterSort,
            final FilterSort.FieldType sortValue, final FilterSort.FilterSortOrder howValue,
            final EnumFilter enumFilter) {
        final List<Queryable> queryables;
        final String query;
        final List<Pair<String, String[]>> pairs;
        queryables = new ArrayList<>();
        queryables.add(filterSort);
        queryables.add((final boolean toRequest) -> nameSimple + Queryable.EQUALS + valueSimple);
        queryables.add(enumFilter);
        queryables.add(QueryUtils.convertToQueryableList(null, Arrays.asList(textValue)).get(1));
        query = QueryUtils.valueToQuery(queryables, Queryable.QUERY_DELIMITER, true);
        Assert.assertEquals(
                FilterSort.QUERY_PART_SORT + Queryable.EQUALS + sortValue.name()
                        + Queryable.QUERY_DELIMITER
                        + FilterSort.QUERY_PART_HOW + Queryable.EQUALS + howValue.name()
                        + Queryable.QUERY_DELIMITER
                        + nameSimple + Queryable.EQUALS + valueSimple
                        + Queryable.QUERY_DELIMITER
                        + nameEnum + Queryable.EQUALS + valueEnum
                        + Queryable.QUERY_DELIMITER
                        + textValue.getId() + Queryable.EQUALS +
                        QueryUtils.getEncodedString(textValue.getValue())
                , query);
        pairs = QueryUtils.queryToValues(query);

        assertEqualsGson(pairs, QueryUtils.queryToValues("http://yandex.ru?" + query));
        assertEqualsGson(pairs, QueryUtils.queryToValues("https://yandex.ru?" + query));
        Assert.assertNotNull(pairs);
        Assert.assertEquals(5, pairs.size());

        Assert.assertEquals(textValue.getId(), pairs.get(4).getFirst());
        Assert.assertEquals(1, pairs.get(4).getSecond().length);
        Assert.assertEquals(textValue.getValue(), pairs.get(4).getSecond()[0]);
    }

    private void assertEqualsGson(Object expected, Object actual) {
        final Gson gson = GsonFactory.get();
        Assert.assertEquals(gson.toJson(expected), gson.toJson(actual));
    }
}
