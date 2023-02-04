import yatest
import datetime
from maps.analyzer.libs.light_calendar.py.calendar import PyCalendarStorage

PATH_TO_CALENDAR_SOURCE = "maps/analyzer/libs/light_calendar/py/data/light_calendar.fb"


def test_is_holiday():
    calendar_storage = PyCalendarStorage(yatest.common.source_path(PATH_TO_CALENDAR_SOURCE), True, True)
    assert calendar_storage.is_holiday(18025, 225), "2019-05-09 is a holiday"
    assert calendar_storage.is_holiday(datetime.date(2019, 5, 9), 225), "2019-05-09 is a holiday"
    assert not calendar_storage.is_holiday(18029, 225), "2019-05-13 is a working day"
    assert not calendar_storage.is_holiday(datetime.date(2019, 5, 13), 225), "2019-05-13 is a working day"
