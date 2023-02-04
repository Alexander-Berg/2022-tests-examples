import pytest
from django.core.management import call_command
from django.utils import timezone

from idm.core.constants.role import ROLE_STATE
from idm.tests.utils import set_workflow, raw_make_role

pytestmark = [pytest.mark.django_db]


@pytest.mark.parametrize('subject', ('user', 'group'))
def test_regular_roles_review(arda_users, fellowship, simple_system, subject):
    """
    TestpalmID: 3456788-220
    TestpalmID: 3456788-219
    """
    simple_system.has_review = True
    simple_system.save(update_fields=['has_review'])
    workflow = '''approvers = ['frodo', 'sam']'''
    set_workflow(simple_system, workflow, workflow)

    if subject == 'user':
        subject = arda_users.frodo
    elif subject == 'group':
        subject = fellowship
    else:
        assert False, 'Wrong subject'

    old_date = timezone.now() - timezone.timedelta(days=365)
    role = raw_make_role(subject, simple_system, {'role': 'admin'}, state=ROLE_STATE.GRANTED)
    old_role = raw_make_role(subject, simple_system, {'role': 'manager'}, state=ROLE_STATE.GRANTED, granted_at=old_date)

    call_command('idm_review_roles')

    role.refresh_from_db()
    assert role.state == ROLE_STATE.GRANTED
    old_role.refresh_from_db()
    assert old_role.state == ROLE_STATE.REVIEW_REQUEST
