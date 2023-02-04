from mock import patch
import pretend
import pytest

from django.conf import settings
from django.utils import timezone

from plan.duty.schedulers.ordered_staff import OrderedStaff
from common import factories


@pytest.fixture
def data():
    ordered_staff = OrderedStaff()
    fred = factories.StaffFactory()
    george = factories.StaffFactory()
    ron = factories.StaffFactory()
    ordered_staff.add_staff_with_date(fred, timezone.datetime(2019, 1, 1, tzinfo=settings.DEFAULT_TIMEZONE))
    ordered_staff.add_staff_with_date(george, timezone.datetime(2019, 1, 2, tzinfo=settings.DEFAULT_TIMEZONE))
    ordered_staff.add_staff_with_date(george, timezone.datetime(2018, 11, 12, tzinfo=settings.DEFAULT_TIMEZONE))
    ordered_staff.add_staff_with_fake_date([ron])
    return pretend.stub(
        ordered_staff=ordered_staff,
        fred=fred,
        george=george,
        ron=ron,
    )


def test_iteration(data):
    values = [val for val in data.ordered_staff]
    assert values == [data.ron, data.fred, data.george]


@pytest.mark.parametrize('in_order', [True, False])
def test_rebuild_queue(data, in_order):
    with patch(
        'plan.duty.schedulers.ordered_staff.OrderedStaff._rebuild_queue'
    ) as rebuild, patch(
        'plan.duty.schedulers.ordered_staff.OrderedStaff._step_queue'
    ) as step:
        date = timezone.datetime(2019, 2, 2, tzinfo=settings.DEFAULT_TIMEZONE)
        data.ordered_staff.add_staff_with_date(data.ron if in_order else data.fred, date)
        if in_order:
            assert rebuild.call_count == 0
            assert step.call_count == 1
        else:
            assert rebuild.call_count == 1
            assert step.call_count == 0
