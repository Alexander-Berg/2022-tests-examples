import pytest

from datetime import datetime, timedelta

import plan.holidays.models
from plan.holidays.models import Holiday

pytestmark = pytest.mark.django_db


def patch_get_remote_holidays(monkeypatch, return_dates):
    monkeypatch.setattr(
        plan.holidays.models,
        'get_remote_holidays',
        lambda *a, **kw: return_dates,
    )


def test_sync_holidays(monkeypatch):
    holidays = [datetime.today().date() + timedelta(days=n) for n in range(-12, 14, 2)]
    holidays_dict = {date: False for date in holidays}
    old_holidays = set(Holiday.objects.all())

    # одной из дат присвоем знаечние праздничного дня == True
    holiday = Holiday.objects.last()
    holiday_date = Holiday.objects.last().date
    assert holiday.is_holiday is False
    holidays_dict[holiday_date] = True

    patch_get_remote_holidays(monkeypatch, holidays_dict)
    date_range = (holidays[0], holiday_date)
    Holiday.sync(date_range[0], date_range[1])

    synced_dates = Holiday.objects.filter(date__range=date_range).values_list('date', flat=True)

    assert set(str(d) for d in synced_dates) == set(str(d) for d in holidays_dict.keys())
    assert set(old_holidays) != set(Holiday.objects.all())
    unaffected_old_holidays = set(
        day for day in old_holidays
        if (day.date < date_range[0]) or (day.date > date_range[1])
    )

    assert Holiday.objects.get(date=holiday_date).is_holiday is True
    assert unaffected_old_holidays == set(
        Holiday.objects.exclude(date__range=(holidays[0], holiday_date))
    )
