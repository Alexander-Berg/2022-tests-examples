from datetime import date
import pytest

from staff.lib.testing import StaffFactory

from staff.dismissal.notifications import DismissalNotification, DismissalNotificationContext
from staff.dismissal.tests.factories import DismissalFactory


@pytest.mark.django_db
def test_get_subj_id():
    person = StaffFactory(login='tester', quit_at=date(2020, 1, 1))
    assert DismissalNotification(
        context=DismissalNotificationContext(staff=person),
        target='DISMISSAL_BROADCAST',
        department=person.department,
        office=person.office
    ).get_subj_id() == 'dismissal_tester_2020-01-01'

    person = StaffFactory(login='tester', quit_at=None)
    assert DismissalNotification(
        context=DismissalNotificationContext(staff=person),
        target='DISMISSAL_BROADCAST',
        department=person.department,
        office=person.office
    ).get_subj_id() == 'dismissal_tester'

    person = StaffFactory(login='tester', quit_at=None)
    assert DismissalNotification(
        context=DismissalNotificationContext(staff=person, dismissal=DismissalFactory(id=123, staff=person)),
        target='DISMISSAL_BROADCAST',
        department=person.department,
        office=person.office
    ).get_subj_id() == 'dismissal_123'
