from datetime import date

import pytest

from tasha.core.models import User
from tasha.lib.staff.staff_person import StaffPerson
from tasha.lib.staff.utils import has_changes

pytestmark = [pytest.mark.asyncio]


@pytest.fixture()
def create_users():
    user = User(
        id=0,
        username='test_user',
        is_active=True,
        quit_at="2000-01-01",
        email="test_user@",
        join_at="2000-01-01",
        organization_id=1,
    )
    person = StaffPerson(
        username='test_user',
        is_active=True,
        quit_at=date.fromisoformat("2000-01-01"),
        email="test_user@",
        join_at=date.fromisoformat("2000-01-01"),
        leave_at=None,
        organization_id=1,
        accounts=set()
    )
    return user, person


async def test_has_changes_equal(create_users):
    user, person = create_users
    assert not has_changes(user, person)


async def test_has_changes_diff_one(create_users):
    user, person = create_users
    person.username = 'Scaly'
    assert has_changes(user, person)


async def test_has_changes_diff_not_sensitive(create_users):
    user, person = create_users
    person.accounts = {1}
    assert not has_changes(user, person)
