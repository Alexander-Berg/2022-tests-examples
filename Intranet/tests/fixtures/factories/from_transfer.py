import datetime
import factory
import pytest

from typing import Generator

from watcher.db import Holiday, UserPermission
from watcher.logic.timezone import today
from watcher.config import settings


@pytest.fixture(scope='function')
def holiday_factory(meta_base):
    class HolidayFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = Holiday

    return HolidayFactory


def get_weekends_day(dtstart: datetime.date, dtend: datetime.date) -> Generator[datetime.date, None, None]:
    for date in (
            dtstart + datetime.timedelta(days=n)
            for n in range((dtend - dtstart).days + 1)
    ):
        if date.weekday() in (5, 6):
            yield date


@pytest.fixture
def weekends(holiday_factory):
    start = today() - datetime.timedelta(days=30)
    end = today() + datetime.timedelta(days=60)

    return [holiday_factory(date=date) for date in get_weekends_day(start, end)]


@pytest.fixture
def creating_unexpected_holidays(weekends, holiday_factory):
    # формируем внезапный выходной
    day = today()
    while day in [h.date for h in weekends]:
        day += datetime.timedelta(days=1)

    return holiday_factory(date=day)


@pytest.fixture(scope='function')
def permission_factory(meta_base):
    class UserPermissionFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = UserPermission

        permission_id = settings.FULL_ACCESS_ID

    return UserPermissionFactory
