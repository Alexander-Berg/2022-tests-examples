import pytest

from datetime import date, datetime, timedelta

from django.conf import settings

from staff.person.models import AFFILIATION

from staff.lib.testing import RouteFactory, StaffFactory
from staff.dismissal.models import Dismissal, DISMISSAL_STATUS

from .factories import DismissalFactory


@pytest.fixture
def notification_route():
    RouteFactory(
        target='DISMISSAL_HR',
        department=None,
        office=None,
        staff=None,
        transport_id='email',
        params='{}',
    )


@pytest.fixture
def robot_staff():
    StaffFactory(login=settings.ROBOT_STAFF_LOGIN)


@pytest.mark.django_db
def test_complete_dismissals(notification_route, robot_staff):
    from staff.dismissal.objects import DismissalCtl

    d1 = DismissalFactory(
        deadline=date.today() - timedelta(days=1),
        status=DISMISSAL_STATUS.IN_PROGRESS,
        move_to_ext=False,
    )
    d2 = DismissalFactory(
        deadline=date.today(),
        status=DISMISSAL_STATUS.IN_PROGRESS,
        move_to_ext=False,
    )
    d3 = DismissalFactory(
        deadline=date.today() + timedelta(days=1),
        status=DISMISSAL_STATUS.IN_PROGRESS,
        move_to_ext=False,
    )
    d4 = DismissalFactory(
        deadline=date.today() - timedelta(days=1),
        status=DISMISSAL_STATUS.IN_PROGRESS,
        intranet_status=0,
        move_to_ext=False,
    )

    d5 = DismissalFactory(
        deadline=date.today() - timedelta(days=1),
        status=DISMISSAL_STATUS.IN_PROGRESS,
        move_to_ext=True,
        staff=StaffFactory(affiliation=AFFILIATION.YANDEX),
    )
    d6 = DismissalFactory(
        deadline=date.today() - timedelta(days=DismissalCtl.MAX_MOVE_TO_EXT_DAYS),
        status=DISMISSAL_STATUS.IN_PROGRESS,
        move_to_ext=True,
        staff=StaffFactory(affiliation=AFFILIATION.YANDEX),
    )
    d7 = DismissalFactory(
        deadline=date.today(),
        status=DISMISSAL_STATUS.IN_PROGRESS,
        move_to_ext=True,
        staff=StaffFactory(affiliation=AFFILIATION.YANDEX),
    )
    d8 = DismissalFactory(
        deadline=date.today() + timedelta(days=1),
        status=DISMISSAL_STATUS.IN_PROGRESS,
        move_to_ext=True,
    )
    d9 = DismissalFactory(
        deadline=date.today() - timedelta(days=1),
        status=DISMISSAL_STATUS.IN_PROGRESS,
        intranet_status=0,
        move_to_ext=True,
        staff=StaffFactory(affiliation=AFFILIATION.YANDEX),
    )

    DismissalCtl.complete_passed()

    res = list(Dismissal.objects.order_by('id').values_list('id', 'status'))
    exp = [
        (d1.id, DISMISSAL_STATUS.DONE),
        (d2.id, DISMISSAL_STATUS.DONE),
        (d3.id, DISMISSAL_STATUS.IN_PROGRESS),
        (d4.id, DISMISSAL_STATUS.IN_PROGRESS),
        (d5.id, DISMISSAL_STATUS.DATE_PASSED),
        (d6.id, DISMISSAL_STATUS.DONE),
        (d7.id, DISMISSAL_STATUS.DATE_PASSED),
        (d8.id, DISMISSAL_STATUS.IN_PROGRESS),
        (d9.id, DISMISSAL_STATUS.IN_PROGRESS),
    ]
    assert res == exp


@pytest.mark.django_db
def test_clear_inactive_dismissals(notification_route):
    from staff.dismissal.tasks import ClearInactiveDismissalsSignalTask

    d1 = DismissalFactory(intranet_status=0,
                          status=DISMISSAL_STATUS.IN_PROGRESS)
    d2 = DismissalFactory(intranet_status=0,
                          status=DISMISSAL_STATUS.IN_PROGRESS)
    d3 = DismissalFactory(intranet_status=1,
                          status=DISMISSAL_STATUS.IN_PROGRESS)
    d4 = DismissalFactory(intranet_status=0,
                          status=DISMISSAL_STATUS.CANCELLED)

    # update, т.к. created_at auto_now по-другому не выставить
    Dismissal.objects.filter(pk=d1.pk).update(
        created_at=datetime.now() - timedelta(days=50))
    Dismissal.objects.filter(pk=d2.pk).update(
        created_at=datetime.now() - timedelta(days=20))
    Dismissal.objects.filter(pk=d3.pk).update(
        created_at=datetime.now() - timedelta(days=50))
    Dismissal.objects.filter(pk=d4.pk).update(
        created_at=datetime.now() - timedelta(days=50))

    ClearInactiveDismissalsSignalTask(days_ago=30)

    dismissals = Dismissal.objects.all().values_list('pk', flat=True)
    assert list(dismissals) == [d2.pk, d3.pk, d4.pk]
