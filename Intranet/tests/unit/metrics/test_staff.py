import freezegun
import pytest

from django.utils import timezone

from plan.metrics.staff import get_found_in_staff_times
from common import factories

pytestmark = pytest.mark.django_db


def test_get_found_in_staff_times():
    now = timezone.now()
    created_at = now - timezone.timedelta(hours=24)
    with freezegun.freeze_time(created_at):
        for found_at in (
            None,
            created_at + timezone.timedelta(minutes=1),
            created_at + timezone.timedelta(hours=6),
            created_at + timezone.timedelta(hours=12),
        ):
            factories.ServiceMemberFactory(found_in_staff_at=found_at)

    def get_times(timedelta):
        return sorted(dt.total_seconds() for dt in get_found_in_staff_times(timedelta))

    with freezegun.freeze_time(now):
        assert [24*3600] == get_times(timezone.timedelta(hours=1))
        assert [12*3600, 24*3600] == get_times(timezone.timedelta(hours=13))
        assert [6*3600, 12*3600, 24*3600] == get_times(timezone.timedelta(hours=19))
        assert [60, 6*3600, 12*3600, 24*3600] == get_times(timezone.timedelta(hours=25))
