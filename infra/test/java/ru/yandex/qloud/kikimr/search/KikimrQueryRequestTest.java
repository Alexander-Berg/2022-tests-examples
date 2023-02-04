package ru.yandex.qloud.kikimr.search;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qloud.kikimr.lucene.ESQueryConverter;
import ru.yandex.qloud.kikimr.lucene.TimeRangeFilter;
import ru.yandex.qloud.kikimr.transport.KikimrScheme;
import ru.yandex.qloud.kikimr.transport.YQL;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author violin
 */
public class KikimrQueryRequestTest {

    @Mock
    private YQL yql = mock(YQL.class);
    @Mock
    private KikimrScheme kikimrScheme = mock(KikimrScheme.class);
    @Mock
    private ESQueryConverter esQueryConverter = mock(ESQueryConverter.class);

    @Autowired
    @InjectMocks
    private KikimrQueryRequestFactory kikimrQueryRequestFactory;

    @Before
    public void init() {
        initMocks(this);
        when(esQueryConverter.convertToYqlWhereCondition(anyString(), anyBoolean())).thenReturn("test_converted_query");
    }

    @Test
    public void searchTablesOfTimeRangeWithinSameDate() {
        TimeRangeFilter timeRangeFilter = new TimeRangeFilter(
                ZonedDateTime.of(2017, 11, 14, 15, 50, 30, 0, ZoneId.of("Z")).toInstant().toEpochMilli(),
                ZonedDateTime.of(2017, 11, 14, 16, 30, 30, 0, ZoneId.of("Z")).toInstant().toEpochMilli()
        );
        List<String> tables = createKikimrRequest(timeRangeFilter, false).getTables();
        assertEquals(Collections.singletonList("fake/path/2017-11-14"), tables);

        List<String> accessTables = createKikimrRequest(timeRangeFilter, true).getTables();
        assertEquals(Collections.singletonList("fake/path/access-2017-11-14"), accessTables);
    }

    @Test
    public void searchTablesOfTimeDifferentDaysTimeEarlier() {
        TimeRangeFilter timeRangeFilter = new TimeRangeFilter(
                ZonedDateTime.of(2017, 11, 14, 15, 50, 30, 0, ZoneId.of("Z")).toInstant().toEpochMilli(),
                ZonedDateTime.of(2017, 11, 15, 11, 30, 30, 0, ZoneId.of("Z")).toInstant().toEpochMilli()
        );
        List<String> tables = createKikimrRequest(timeRangeFilter, false).getTables();
        assertEquals(Lists.newArrayList("fake/path/2017-11-15", "fake/path/2017-11-14"), tables);

        List<String> accessTables = createKikimrRequest(timeRangeFilter, true).getTables();
        assertEquals(Lists.newArrayList("fake/path/access-2017-11-15", "fake/path/access-2017-11-14"), accessTables);
    }

    @Test
    public void searchTablesOfTimeDifferentDaysTimeLater() {
        TimeRangeFilter timeRangeFilter = new TimeRangeFilter(
                ZonedDateTime.of(2017, 11, 14, 11, 50, 30, 0, ZoneId.of("Z")).toInstant().toEpochMilli(),
                ZonedDateTime.of(2017, 11, 15, 15, 30, 30, 0, ZoneId.of("Z")).toInstant().toEpochMilli()
        );
        List<String> tables = createKikimrRequest(timeRangeFilter, false).getTables();
        assertEquals(Lists.newArrayList("fake/path/2017-11-15", "fake/path/2017-11-14"), tables);

        List<String> accessTables = createKikimrRequest(timeRangeFilter, true).getTables();
        assertEquals(Lists.newArrayList("fake/path/access-2017-11-15", "fake/path/access-2017-11-14"), accessTables);
    }

    @Test
    public void searchTablesOfTimeDifferentDaysTimeSame() {
        TimeRangeFilter timeRangeFilter = new TimeRangeFilter(
                ZonedDateTime.of(2017, 11, 14, 11, 50, 30, 0, ZoneId.of("Z")).toInstant().toEpochMilli(),
                ZonedDateTime.of(2017, 11, 15, 11, 50, 30, 0, ZoneId.of("Z")).toInstant().toEpochMilli()
        );
        List<String> tables = createKikimrRequest(timeRangeFilter, false).getTables();
        assertEquals(Lists.newArrayList("fake/path/2017-11-15", "fake/path/2017-11-14"), tables);

        List<String> accessTables = createKikimrRequest(timeRangeFilter, true).getTables();
        assertEquals(Lists.newArrayList("fake/path/access-2017-11-15", "fake/path/access-2017-11-14"), accessTables);
    }

    private KikimrQueryRequest createKikimrRequest(TimeRangeFilter timeRangeFilter, boolean isAccessLog) {
        return kikimrQueryRequestFactory.createKikimrQueryRequest(
                new QueryWhereCondition("fake_es_query", "fake_yql_query"), "fake/path", null, timeRangeFilter, null, isAccessLog
        );
    }
}
