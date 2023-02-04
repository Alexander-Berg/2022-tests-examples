# coding: utf-8


import pytest

from idm.core.models import Role, Transfer
from idm.tests.utils import set_workflow, DEFAULT_WORKFLOW, add_members, remove_members, move_group
from idm.utils import reverse


pytestmark = [pytest.mark.django_db, pytest.mark.parametrize('api_name', ['frontend', 'v1'])]


def get_list_url(api_name):
    return reverse('api_dispatch_list', api_name=api_name, resource_name='transfers')


def get_transfer_url(api_name, pk):
    return reverse('api_dispatch_detail', api_name=api_name, resource_name='transfers', pk=pk)


@pytest.fixture(autouse=True)
def transfers(simple_system, arda_users, department_structure):
    frodo = arda_users.frodo
    varda = arda_users.varda
    fellowship = department_structure.fellowship
    valinor = department_structure.valinor

    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    varda_role = Role.objects.request_role(varda, varda, simple_system, '', {'role': 'admin'})
    group_role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'})

    add_members(fellowship, [varda])
    remove_members(valinor, [varda])

    assert Transfer.objects.count() == 1
    transfer = Transfer.objects.get()
    assert transfer.user_id == varda.id
    assert transfer.state == 'undecided'

    move_group(fellowship, valinor.parent)
    assert Transfer.objects.count() == fellowship.members.count() + 2


def test_transfer_filter(client, arda_users, api_name, superuser_gandalf):
    frodo = arda_users.frodo
    url = get_list_url(api_name)
    client.login('frodo')
    data = client.json.get(url).json()
    assert len(data['objects']) == 0

    # а вот с правом суперпользователя всё хорошо
    client.login('gandalf')
    data = client.json.get(url).json()
    assert len(data['objects']) == 12

    data = client.json.get(url, {'type': 'user,user_group'}).json()
    assert len(data['objects']) == 11

    data = client.json.get(url, {'type': 'user', 'user': 'varda'}).json()
    assert len(data['objects']) == 1

    data = client.json.get(url, {'user': 'varda'}).json()
    assert len(data['objects']) == 2

    data = client.json.get(url, {'state': 'rejected'}).json()
    assert len(data['objects']) == 0


def test_accept_individual(client, arda_users, api_name, superuser_gandalf):
    varda = arda_users.varda
    client.login('frodo')

    transfer = Transfer.objects.get(user=varda, type='user')

    url = get_transfer_url(api_name, transfer.pk)
    response = client.json.get(url)
    assert response.status_code == 404

    # а вот с правом суперпользователя всё хорошо
    client.login('gandalf')
    response = client.json.get(url)
    assert response.status_code == 200
    assert response.json()['state'] == 'undecided'

    # утвердим перемещение
    response = client.json.post(url, {'accept': True})
    assert response.status_code == 204
    data = client.json.get(url).json()
    assert data['state'] == 'accepted'
    assert Role.objects.filter(state='need_request').count() == 1


def test_reject_individual(client, arda_users, api_name, superuser_gandalf):
    varda = arda_users.varda
    client.login('frodo')

    transfer = Transfer.objects.get(user=varda, type='user')

    url = get_transfer_url(api_name, transfer.pk)
    response = client.json.get(url)
    assert response.status_code == 404

    client.login('gandalf')
    response = client.json.get(url)
    assert response.status_code == 200
    assert response.json()['state'] == 'undecided'

    response = client.json.post(url, {'accept': False})
    assert response.status_code == 204
    data = client.json.get(url).json()
    assert data['state'] == 'rejected'
    assert Role.objects.filter(state='need_request').count() == 0


def test_accept_en_masse(client, arda_users, api_name, superuser_gandalf):
    varda = arda_users.varda
    client.login('frodo')

    url = get_list_url(api_name)
    response = client.json.post(url, {'accept': True})
    assert response.json() == {'successes': 0, 'errors': 0}

    client.login('gandalf')
    response = client.json.post(url, {'accept': True, 'user': 'varda,frodo'})
    assert response.json() == {'successes': 2, 'errors': 1}
    assert varda.transfers.filter(state='accepted').count() == 1
    assert varda.transfers.filter(state='expired').count() == 1


def test_reject_en_masse(client, arda_users, api_name, superuser_gandalf):
    varda = arda_users.varda
    client.login('frodo')

    url = get_list_url(api_name)
    response = client.json.post(url, {'accept': False})
    assert response.json() == {'successes': 0, 'errors': 0}

    client.login('gandalf')
    response = client.json.post(url, {'accept': False, 'user': 'varda,frodo'})
    assert response.json() == {'successes': 2, 'errors': 1}
    assert varda.transfers.filter(state='rejected').count() == 1
    assert varda.transfers.filter(state='expired').count() == 1
