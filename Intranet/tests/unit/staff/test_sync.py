import pytest

from unittest.mock import patch

from ok.staff.sync import sync_group_memberships

from tests import factories as f


pytestmark = pytest.mark.django_db


def test_sync_group_memberships():
    synced, changed, new = f.GroupFactory.create_batch(3)

    synced.memberships.create(login='existing-1')
    synced.memberships.create(login='existing-2')
    changed.memberships.create(login='existing-1')
    changed.memberships.create(login='existing-2')
    changed.memberships.create(login='existing-3')

    mocked_staff_data = [
        {'group_url': synced.url, 'login': 'existing-1'},
        {'group_url': synced.url, 'login': 'existing-2'},
        {'group_url': changed.url, 'login': 'existing-1'},
        {'group_url': changed.url, 'login': 'new-1'},
        {'group_url': new.url, 'login': 'new-1'},
        {'group_url': new.url, 'login': 'new-2'},
    ]

    with patch('ok.staff.sync.get_staff_group_memberships', return_value=mocked_staff_data):
        sync_group_memberships()

    assert set(synced.memberships.values_list('login', flat=True)) == {'existing-1', 'existing-2'}
    assert set(changed.memberships.values_list('login', flat=True)) == {'existing-1', 'new-1'}
    assert set(new.memberships.values_list('login', flat=True)) == {'new-1', 'new-2'}
