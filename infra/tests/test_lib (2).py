import mock
from infra.qyp.account_manager.src.lib.calendar import CalendarClient


def test_calendar():
    weekday = {'holidays': [{'date': '1970-01-11', 'type': 'weekday', 'name': 'special day'}]}
    weekend = {'holidays': [{'date': '1970-01-12', 'type': 'weekend', 'name': 'special day'}]}
    holiday = {'holidays': [{'date': '1970-01-13', 'type': 'weekend', 'name': 'special day'}]}
    empty_day = {'holidays': []}
    c = CalendarClient()
    with mock.patch('infra.qyp.account_manager.src.lib.calendar.CalendarClient.get_holidays', return_value=weekday):
        assert not c.is_holiday('1970-01-11')

    with mock.patch('infra.qyp.account_manager.src.lib.calendar.CalendarClient.get_holidays', return_value=weekend):
        assert c.is_holiday('1970-01-12')

    with mock.patch('infra.qyp.account_manager.src.lib.calendar.CalendarClient.get_holidays', return_value=holiday):
        assert c.is_holiday('1970-01-13')

    with mock.patch('infra.qyp.account_manager.src.lib.calendar.CalendarClient.get_holidays', return_value=empty_day):
        assert not c.is_holiday('1970-01-14')

    with mock.patch('infra.qyp.account_manager.src.lib.calendar.CalendarClient.get_holidays', side_effect=Exception()):
        assert not c.is_holiday('1970-01-15')
