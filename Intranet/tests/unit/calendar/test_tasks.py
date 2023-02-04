import pytz
import pytest

from datetime import datetime
from unittest.mock import patch, Mock

from intranet.femida.src.calendar import exceptions
from intranet.femida.src.calendar.tasks import _sync_calendar_events

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def _dt(year, month=1, day=1):
    return datetime(year, month, day, tzinfo=pytz.utc)


@pytest.mark.parametrize('event_start_time, start_time, expected_result', (
    (_dt(2019), _dt(2019), 0),  # дата не изменилась
    (_dt(2019), _dt(2020), 1),  # дата изменилась
))
@patch('intranet.femida.src.calendar.tasks.get_event')
def test_sync_calendar_events(mocked_get_event, event_start_time, start_time, expected_result):
    """
    Проверяет стандартный сценарий – событие существует, с ним всё ок
    """
    interview = f.InterviewFactory(event_id=1, event_start_time=event_start_time)
    event = Mock()
    event.id = interview.event_id
    event.start_time = start_time
    mocked_get_event.return_value = event

    result = _sync_calendar_events([interview])
    assert result == expected_result
    assert interview.event_id == event.id
    assert interview.event_start_time == event.start_time


@patch(
    target='intranet.femida.src.calendar.tasks.get_event',
    new=Mock(side_effect=exceptions.EventDoesNotExist),
)
def test_sync_calendar_events_no_event():
    """
    Проверяет сценарий, когда события не существует
    """
    interview = f.InterviewFactory(event_id=1, event_start_time=_dt(2019))
    count = _sync_calendar_events([interview])
    assert count == 1
    assert interview.event_id is None
    assert interview.event_start_time is None


@patch(
    target='intranet.femida.src.calendar.tasks.get_event',
    new=Mock(side_effect=exceptions.EventIsNotAvailable),
)
def test_sync_calendar_events_unavailable():
    """
    Проверяет сценарий, когда событие существует, но недоступно
    """
    interview = f.InterviewFactory(event_id=1, event_start_time=_dt(2019))
    count = _sync_calendar_events([interview])
    assert count == 1
    assert interview.event_id == 1
    assert interview.event_start_time is None


@patch('intranet.femida.src.calendar.tasks.get_event', Mock(side_effect=exceptions.CalendarError))
def test_sync_calendar_events_calendar_error():
    """
    Проверяет сценарий при неизвестной ошибке от календаря
    """
    event_id = 1
    event_start_time = _dt(2019)
    interview = f.InterviewFactory(event_id=event_id, event_start_time=event_start_time)

    count = _sync_calendar_events([interview])
    assert count == 0
    assert interview.event_id == event_id
    assert interview.event_start_time == event_start_time
