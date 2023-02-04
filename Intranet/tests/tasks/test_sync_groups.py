import mock
import pytest

from django.core.management import call_command
from django_abc_data.models import AbcService

from __tests__.utils import factories as f
from intranet.crt.constants import USE_THRESHOLD_IF_USERS_GREATER
from intranet.crt.users.models import CrtGroup

pytestmark = pytest.mark.django_db


def get_remote_group(group=None, **kwargs):
    tmp_group = group or f.CrtGroup.build(**kwargs)
    remote_group = {
        'id': tmp_group.external_id,
        'url': tmp_group.url,
        'name': tmp_group.name,
        'is_deleted': tmp_group.is_deleted,
        'role_scope': 'development',
        'parent': {'service': {'id': tmp_group.abc_service.external_id}},
    }
    return remote_group


def get_membership_response(group, users):
    response = []
    for user in users:
        membership = {
            'group': group,
            'person': {
                'login': user,
            }
        }
        response.append(membership)
    return response


mock_getiter_path = 'ids.services.staff.repositories.groupmembership.StaffGroupMembershipRepository.getiter'


@mock.patch(mock_getiter_path)
def test_adding_new_group(mocked_getiter, users, abc_services):
    service = AbcService.objects.first()
    remote_group = get_remote_group(abc_service=service)
    members = ['normal_user', 'another_user']
    mocked_getiter.return_value = get_membership_response(remote_group, members)
    assert CrtGroup.objects.count() == 0

    call_command('crt_sync_groups')

    assert CrtGroup.objects.count() == 1
    crt_group = CrtGroup.objects.first()
    assert crt_group.name == remote_group['name']
    assert crt_group.url == remote_group['url']
    assert crt_group.type == 'servicerole'
    assert crt_group.role_scope == 'development'
    assert crt_group.abc_service == service
    group_users = list(crt_group.users.values_list('username', flat=True).order_by('username'))
    assert group_users == sorted(members)


@mock.patch(mock_getiter_path)
def test_updating_existing_group(mocked_getiter, users, abc_services):
    group = f.CrtGroup(abc_service=AbcService.objects.first())
    group.users.add(users['normal_user'])

    remote_group = get_remote_group(
        external_id=group.external_id,
        name=group.name + 'new',
        url=group.url + 'new',
        is_deleted=not group.is_deleted,
        abc_service=group.abc_service,
    )
    mocked_getiter.return_value = get_membership_response(remote_group, ['normal_user'])

    call_command('crt_sync_groups')

    assert CrtGroup.objects.count() == 1
    group.refresh_from_db()
    assert group.name == remote_group['name']
    assert group.url == remote_group['url']
    assert group.is_deleted == remote_group['is_deleted']


@mock.patch(mock_getiter_path)
def test_changing_group_members(mocked_getiter, users, abc_services):
    group = f.CrtGroup(abc_service=AbcService.objects.first())
    group.users.add(users['bubblegum'])
    remote_group = get_remote_group(group)

    new_members = ['normal_user', 'another_user']
    mocked_getiter.return_value = get_membership_response(remote_group, new_members)

    call_command('crt_sync_groups')

    group_users = list(group.users.values_list('username', flat=True).order_by('username'))
    assert group_users == sorted(new_members)


@mock.patch(mock_getiter_path)
def test_deleting_old_group_members(mocked_getiter, users, abc_services):
    group = f.CrtGroup(abc_service=AbcService.objects.first())
    group.users.add(users['normal_user'])
    group.users.add(users['another_user'])
    mocked_getiter.return_value = []

    call_command('crt_sync_groups')

    assert group.users.count() == 0


@mock.patch(mock_getiter_path)
def test_threshold_works(mocked_getiter, abc_services):
    """
    Если в группе больше USE_THRESHOLD_IF_USERS_GREATER (3) членов
    и удаляется больше чем REMOVE_USERS_FROM_SCOPE_THRESHOLD (50%) пользователей,
    то синхронизация прекращается
    """
    group = f.CrtGroup(abc_service=AbcService.objects.first())
    users_count = USE_THRESHOLD_IF_USERS_GREATER + 1
    for i in range(users_count):
        group.users.add(f.User())
    mocked_getiter.return_value = []

    with pytest.raises(SystemExit):
        call_command('crt_sync_groups')
    assert group.users.count() == users_count

    call_command('crt_sync_groups', '--force')
    assert group.users.count() == 0
