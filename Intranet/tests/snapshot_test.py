from datetime import date, timedelta

import pytest

from staff.headcounts_history.models import DepartmentHeadcountSnapshot
from staff.headcounts_history.snapshot import _get_previous_snapshots


@pytest.mark.django_db
def test_previous_snapshot_can_find_unclosed_snapshots(company):
    # given
    today = date.today()
    DepartmentHeadcountSnapshot.objects.create(department=company.yandex, date_from=today)

    # when
    result = _get_previous_snapshots([company.yandex.id], today)

    # then
    assert len(result) == 1
    assert company.yandex.id in result


@pytest.mark.django_db
def test_previous_snapshot_can_find_snapshots_closed_today(company):
    # given
    today = date.today()
    DepartmentHeadcountSnapshot.objects.create(
        department=company.yandex,
        date_from=today - timedelta(days=1),
        date_to=today,
    )

    # when
    result = _get_previous_snapshots([company.yandex.id], today)

    # then
    assert len(result) == 1
    assert company.yandex.id in result
