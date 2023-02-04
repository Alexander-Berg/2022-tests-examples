# coding: utf-8


import pytest

from idm.core.models import Role, Approve
from idm.tests.utils import (set_workflow)
from idm.utils import reverse

pytestmark = pytest.mark.django_db


@pytest.fixture
def all_approvers_url():
    return reverse('api_dispatch_list', api_name='frontend', resource_name='all_approvers')


def test_get_all_approvers_by_approve_id(client, simple_system, arda_users, settings, all_approvers_url):
    """
    GET /api/frontend/all_approvers/?approve_id - возвращает список апруверов по approve_id
    """

    set_workflow(simple_system, 'approvers = [approver("legolas") | approver("saruman") | approver("manve"),'
                                'approver("varda") | approver("legolas"),'
                                'approver("sam") | approver("bilbo") | approver("peregrin"),'
                                'approver("galadriel")]')

    frodo = arda_users.frodo
    Role.objects.request_role(frodo, frodo, simple_system, None, {'role': 'superuser'}, None)

    approves = list(Approve.objects.all())
    client.login('nazgul')

    # получить первую OR-группу
    data = client.json.get(all_approvers_url, {'approve_id': approves[0].id}).json()

    assert data['meta']['total_count'] == 3

    for index, name in enumerate(['legolas', 'saruman', 'manve']):
        assert set(data['objects'][index].keys()) == {
            'approved',
            'decision',
            'approver',
        }
        assert set(data['objects'][index]['approver'].keys()) == {
            'is_active',
            'username',
            'full_name',
        }
        assert data['objects'][index]['approver']['username'] == name

    # получить последнюю OR-группу
    data = client.json.get(all_approvers_url, {'approve_id': approves[-1].id}).json()

    assert data['meta']['total_count'] == 1
    assert data['objects'][0]['approver']['username'] == 'galadriel'

    # отрицательный approve_id
    data = client.json.get(all_approvers_url, {'approve_id': -1}).json()
    assert data['error_code'] == 'BAD_REQUEST' and data['message'] == 'Invalid data sent'

    # несуществующий approve_id
    data = client.json.get(all_approvers_url, {'approve_id': 500}).json()
    assert data['error_code'] == 'BAD_REQUEST' and data['message'] == 'Invalid data sent'
