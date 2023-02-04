from yatest.common import build_path
import pytest
import os

from yt.wrapper import ypath_join

from maps.analyzer.toolkit.lib.paths import Calendars
from maps.pylibs.yt.lib import YtContext
import maps.analyzer.pylibs.envkit.config as config


@pytest.fixture(scope='session')
def calendar(request):
    config.DEFAULT_CALENDAR_PATH = build_path('maps/data/test/calendar/calendar.fb')
    yield config.DEFAULT_CALENDAR_PATH


@pytest.fixture(scope='session')
def light_calendar(request):
    config.DEFAULT_LIGHT_CALENDAR_PATH = build_path('maps/data/test/calendar/light_calendar.fb')
    yield config.DEFAULT_LIGHT_CALENDAR_PATH


def init_upload_calendar(ytc: YtContext):
    Calendars.DATA_ROOT.path = '//data/calendar'
    Calendars.LATEST_PATH.path = ypath_join(Calendars.DATA_ROOT.value, 'latest')
    Calendars.CALENDAR.path = ypath_join(Calendars.LATEST_PATH.value, 'calendar.fb')
    Calendars.LIGHT_CALENDAR.path = ypath_join(Calendars.LATEST_PATH.value, 'light_calendar.fb')

    calendar_file = build_path('maps/data/test/calendar/calendar.fb')
    light_calendar_file = build_path('maps/data/test/calendar/light_calendar.fb')
    assert os.path.exists(calendar_file) and os.path.exists(light_calendar_file),\
        'No local calendars found, did you forget to add `maps/data/test/calendar` to DEPENDS()?'

    ytc.smart_upload_file(
        filename=calendar_file,
        destination=Calendars.CALENDAR.value,
        placement_strategy='ignore',
    )
    ytc.smart_upload_file(
        filename=light_calendar_file,
        destination=Calendars.LIGHT_CALENDAR.value,
        placement_strategy='ignore',
    )
