from datetime import datetime
import mock
import pytest

from freezegun import freeze_time

from django.conf import settings
from django.core.management import call_command


pytestmark = pytest.mark.django_db


@freeze_time('2020-01-01')
def test_sync_holidays():
    with mock.patch('plan.holidays.models.Holiday.sync') as sync_method:
        call_command('sync_holidays')
        today = datetime.now().date()
        date_from = today - settings.HOLIDAY_SYNC_RADIUS
        date_to = today + settings.HOLIDAY_SYNC_RADIUS
        sync_method.assert_called_with(date_from, date_to)
