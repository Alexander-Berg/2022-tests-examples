package ru.yandex.realty.model.serialization;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;
import org.junit.Test;
import ru.yandex.realty.model.message.ExtDataSchema;
import ru.yandex.realty.model.timetable.DatePattern;
import ru.yandex.realty.model.timetable.TimeInterval;
import ru.yandex.realty.model.timetable.TimePattern;
import ru.yandex.realty.model.timetable.WeekTimetable;

import java.util.List;

import static ru.yandex.common.util.collections.CollectionFactory.newLinkedList;

public class WeekTimetableProtoConverterTest {
    @Test
    public void testCorrectWork() throws Exception {
        WeekTimetable wt = WeekTimetable.getDefault();
        ExtDataSchema.WeekTimetableMessage msg = WeekTimetableProtoConverter.toMessage(wt);
        WeekTimetable clone = WeekTimetableProtoConverter.fromMessage(msg);
        MockUtils.assertEquals(wt, clone);
    }


    @Test
    public void testChangeTimeZoneFullWeek() throws Exception {
        List<DatePattern> dps = newLinkedList();
        dps.add(new DatePattern(1,7, new TimePattern(new TimeInterval(LocalTime.parse("09:00"), LocalTime.parse("20:00")))));
        WeekTimetable wt = new WeekTimetable(dps, DateTimeZone.forOffsetHours(3));

        WeekTimetable actualWt = wt.changeTimeZone(DateTimeZone.forOffsetHours(11));

        List<DatePattern> expDps = newLinkedList();
        expDps.add(
                new DatePattern(1, 7,
                        new TimePattern(
                                new TimeInterval(LocalTime.MIDNIGHT, LocalTime.parse("04:00")),
                                new TimeInterval(LocalTime.parse("17:00"), LocalTime.MIDNIGHT.minusMillis(1)))));
        WeekTimetable expWt = new WeekTimetable(expDps, DateTimeZone.forOffsetHours(11));

        MockUtils.assertEquals(expWt, actualWt);
    }

    @Test
    public void testChangeTimeZoneWeekWithGaps() throws Exception {
        List<DatePattern> dps = newLinkedList();
        dps.add(new DatePattern(1,3, new TimePattern(new TimeInterval(LocalTime.parse("09:00"), LocalTime.parse("20:00")))));
        dps.add(new DatePattern(5,5, new TimePattern(new TimeInterval(LocalTime.parse("09:00"), LocalTime.parse("18:00")))));
        dps.add(new DatePattern(6,7, new TimePattern(new TimeInterval(LocalTime.parse("10:00"), LocalTime.parse("17:00")))));
        WeekTimetable wt = new WeekTimetable(dps, DateTimeZone.forOffsetHours(3));

        WeekTimetable actualWt = wt.changeTimeZone(DateTimeZone.forOffsetHours(11));

        List<DatePattern> expDps = newLinkedList();
        expDps.add(
                new DatePattern(1, 1,
                        new TimePattern(
                                new TimeInterval(LocalTime.MIDNIGHT, LocalTime.parse("01:00")),
                                new TimeInterval(LocalTime.parse("17:00"), LocalTime.MIDNIGHT.minusMillis(1)))));
        expDps.add(
                new DatePattern(2, 3,
                        new TimePattern(
                                new TimeInterval(LocalTime.MIDNIGHT, LocalTime.parse("04:00")),
                                new TimeInterval(LocalTime.parse("17:00"), LocalTime.MIDNIGHT.minusMillis(1)))));
        expDps.add(
                new DatePattern(4, 4,
                        new TimePattern(
                                new TimeInterval(LocalTime.MIDNIGHT, LocalTime.parse("04:00")))));
        expDps.add(
                new DatePattern(5, 5,
                        new TimePattern(
                                new TimeInterval(LocalTime.parse("17:00"), LocalTime.MIDNIGHT.minusMillis(1)))));
        expDps.add(
                new DatePattern(6, 6,
                        new TimePattern(
                                new TimeInterval(LocalTime.MIDNIGHT, LocalTime.parse("02:00")),
                                new TimeInterval(LocalTime.parse("18:00"), LocalTime.MIDNIGHT.minusMillis(1)))));
        expDps.add(
                new DatePattern(7, 7,
                        new TimePattern(
                                new TimeInterval(LocalTime.MIDNIGHT, LocalTime.parse("01:00")),
                                new TimeInterval(LocalTime.parse("18:00"), LocalTime.MIDNIGHT.minusMillis(1)))));
        WeekTimetable expWt = new WeekTimetable(expDps, DateTimeZone.forOffsetHours(11));

        MockUtils.assertEquals(expWt, actualWt);
    }

    @Test
    public void testChangeTimeZoneSameTimeZone() throws Exception {
        List<DatePattern> dps = newLinkedList();
        dps.add(new DatePattern(1,3, new TimePattern(new TimeInterval(LocalTime.parse("09:00"), LocalTime.parse("20:00")))));
        dps.add(new DatePattern(5,5, new TimePattern(new TimeInterval(LocalTime.parse("09:00"), LocalTime.parse("18:00")))));
        dps.add(new DatePattern(6,7, new TimePattern(new TimeInterval(LocalTime.parse("10:00"), LocalTime.parse("17:00")))));
        WeekTimetable wt = new WeekTimetable(dps, DateTimeZone.forOffsetHours(3));

        WeekTimetable actualWt = wt.changeTimeZone(DateTimeZone.forOffsetHours(3));

        MockUtils.assertEquals(wt, actualWt);
    }

}
