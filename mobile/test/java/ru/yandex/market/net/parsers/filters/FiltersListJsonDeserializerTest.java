package ru.yandex.market.net.parsers.filters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.annimon.stream.Stream;

import org.apache.tools.ant.filters.StringInputStream;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

import ru.yandex.market.BaseTest;
import ru.yandex.market.ResourceHelper;
import ru.yandex.market.data.filters.filter.ListValuesFilter;
import ru.yandex.market.data.filters.filter.NumericFilter;
import ru.yandex.market.data.filters.filter.filterValue.NumericFilterValue;
import ru.yandex.market.net.category.FiltersResponse;
import ru.yandex.market.net.parsers.SimpleApiV2JsonParser;
import ru.yandex.market.utils.CollectionUtils;
import ru.yandex.market.util.FilterUtils;
import timber.log.Timber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FiltersListJsonDeserializerTest extends BaseTest {

    @NonNull
    private SimpleApiV2JsonParser<FiltersResponse> getParser() {
        return new SimpleApiV2JsonParser<>(FiltersResponse.class);
    }

    @Test
    public void testCorrectResponse() {
        String responseStr = ResourceHelper.getResponse("/responses/categories/filters/categories_filters_1.json");
        final FiltersResponse response = getParser().parse(new StringInputStream(responseStr));
        assertNotNull(response);
        assertNotNull(response.getFilters());
        assertEquals(response.countFilters(), 6);
    }

    @Test
    public void testFullInCorrectResponse() {
        Timber.plant(new Timber.DebugTree());
        String responseStr = ResourceHelper.getResponse("/responses/categories/filters/categories_filters_2.json");
        final FiltersResponse response = getParser().parse(new StringInputStream(responseStr));
        assertNotNull(response);
        assertNotNull(response.getFilters());
        assertEquals(2, response.countFilters()); // just only numeric filters
    }

    @Test
    public void testPartiallyInCorrectResponse() {
        Timber.plant(new Timber.DebugTree());
        String responseStr = ResourceHelper.getResponse("/responses/categories/filters/categories_filters_3.json");
        final FiltersResponse response = getParser().parse(new StringInputStream(responseStr));
        assertNotNull(response);
        assertNotNull(response.getFilters());
        assertEquals(response.countFilters(), 4);
        // each filter has only one valid value
        Stream.ofNullable(response.getFilters())
                .select(ListValuesFilter.class)
                .forEach(filter -> assertEquals(1, CollectionUtils.size(filter.getValues())));
    }

    @Test
    public void testRepairNumericFilter() {
        //1
        NumericFilter initialFilter = getFilter(null, null, null);
        NumericFilter actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        NumericFilter expectedFilter = getFilter(0, 30, null);
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(10, 20, null);
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(10, 20, null);
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter("aaa", "aaa", null);
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(0, 30, null);
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter("aaa", "10", null);
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(0, 10, null);
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter("10", "aaa", null);
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(10, 30, null);
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(-10, 10, null);
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(0, 10, null);
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(10, 40, null);
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(10, 30, null);
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(-10, 100, null);
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(0, 30, null);
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(10, 10, null);
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(10, 30, null);
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(-10, 0, null);
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(0, 30, null);
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(20, 10, null);
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(10, 20, null);
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(-100, -50, null);
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(0, 30, null);
        Assert.assertEquals(expectedFilter, actualFilter);


        initialFilter = getFilter(200, 1000, null);
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(0, 30, null);
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(30, 1000, null);
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(0, 30, null);
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(-10, 30, null);
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(0, 30, null);
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(0, 0, null);
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(0, 30, null);
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(30, 30, null);
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(0, 30, null);
        Assert.assertEquals(expectedFilter, actualFilter);

        //2
        initialFilter = getFilter(null, null, getValue(null, null));
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(0, 30, getValue(0, 30));
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(null, null, getValue(10, 20));
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(0, 30, getValue(10, 20));
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(null, null, getValue(-10, 20));
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(0, 30, getValue(0, 20));
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(null, null, getValue(10, 40));
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(0, 30, getValue(10, 30));
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(null, null, getValue(-10, 40));
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(0, 30, getValue(0, 30));
        Assert.assertEquals(expectedFilter, actualFilter);

        //3
        initialFilter = getFilter(10, 20, getValue(null, null));
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(10, 20, getValue(10, 20));
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(10, 20, getValue(10, 20));
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(10, 20, getValue(10, 20));
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(10, 20, getValue(-10, 20));
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(10, 20, getValue(10, 20));
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(10, 20, getValue(10, 40));
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(10, 20, getValue(10, 20));
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(10, 20, getValue(-10, 40));
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(10, 20, getValue(10, 20));
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(10, 20, getValue(12, 14));
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(10, 20, getValue(12, 14));
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(10, 20, getValue(14, 12));
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(10, 20, getValue(12, 14));
        Assert.assertEquals(expectedFilter, actualFilter);

        //4
        initialFilter = getFilter(-10, 20, getValue(null, null));
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(0, 20, getValue(0, 20));
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(-10, 20, getValue(10, 20));
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(0, 20, getValue(10, 20));
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(-10, 20, getValue(-10, 20));
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(0, 20, getValue(0, 20));
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(-10, 20, getValue(10, 40));
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(0, 20, getValue(10, 20));
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(-10, 20, getValue(-10, 40));
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(0, 20, getValue(0, 20));
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(-10, 20, getValue(12, 14));
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(0, 20, getValue(12, 14));
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(-10, 20, getValue(14, 12));
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(0, 20, getValue(12, 14));
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(-10, 20, getValue(-100, -50));
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(0, 20, getValue(0, 20));
        Assert.assertEquals(expectedFilter, actualFilter);

        initialFilter = getFilter(-10, 20, getValue(200, 300));
        actualFilter = FiltersListJsonDeserializer.repairNumericFilter(initialFilter,
                new BigDecimal(0), new BigDecimal(30));
        expectedFilter = getFilter(0, 20, getValue(0, 20));
        Assert.assertEquals(expectedFilter, actualFilter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRepairNumericFilterIllegalArguments() {
        FiltersListJsonDeserializer.repairNumericFilter(new NumericFilter(), BigDecimal.ONE,
                BigDecimal.ZERO);
    }

    @NonNull
    private NumericFilter getFilter(
            @Nullable final String minValue,
            @Nullable final String maxValue,
            @Nullable final NumericFilterValue checkedValue) {

        NumericFilter numericFilter = new NumericFilter();
        numericFilter.setMin(minValue);
        numericFilter.setMax(maxValue);
        numericFilter.setCheckedValue(checkedValue);
        return numericFilter;
    }

    @NonNull
    private NumericFilter getFilter(
            final long minValue,
            final long maxValue,
            @Nullable final NumericFilterValue checkedValue) {

        return getFilter(
                format(new BigDecimal(minValue)),
                format(new BigDecimal(maxValue)),
                checkedValue
        );
    }

    @NonNull
    private NumericFilterValue getValue(
            @Nullable final String minValue,
            @Nullable final String maxValue) {

        NumericFilterValue value = new NumericFilterValue();
        value.setMin(minValue);
        value.setMax(maxValue);
        return value;
    }

    @NonNull
    private NumericFilterValue getValue(final long minValue, final long maxValue) {
        return getValue(format(new BigDecimal(minValue)), format(new BigDecimal(maxValue)));
    }

    @Nullable
    private String format(@Nullable final BigDecimal value) {
        return value != null ? FilterUtils.formatForBackend(value) : null;
    }
}