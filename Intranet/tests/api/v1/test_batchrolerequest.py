import pytest

from functools import partial

from idm.core.constants.role import ROLE_STATE
from idm.core.models import BatchRequest
from idm.tests.utils import set_workflow, run_commit_hooks, raw_make_role
from idm.users.models import User
from idm.utils import reverse


pytestmark = pytest.mark.django_db


def test_batchrolerequest(client, simple_system, users_for_test):
    set_workflow(simple_system, 'approvers = []')

    batch_request_url = partial(reverse, api_name='v1', resource_name='batchrolerequest')
    create_batch_request_url = batch_request_url('api_dispatch_list')

    request = {'path': '/manager/', 'system': 'simple'}

    response = client.json.post(create_batch_request_url, {'requests': [request]})
    assert response.status_code == 401

    client.login('fantom')

    response = client.json.post(create_batch_request_url, {'requests': [request]})
    assert response.status_code == 400
    assert {'error_code', 'message'}.issubset(response.json().keys())

    response = client.json.post(create_batch_request_url, {'requests': [
        {**request, 'label': 'not_unique_label'},
        {**request, 'label': 'not_unique_label'},
    ]})
    assert response.status_code == 400
    assert {'error_code', 'message'}.issubset(response.json().keys())

    response = client.json.post(create_batch_request_url, {'requests': [
        {**request, 'label': 'unique_label1'},
        {**request, 'label': 'unique_label2', 'user': 'fantom'},
    ]})
    assert response.status_code == 201

    batch_request = BatchRequest.objects.get(id=response.json()['id'])
    assert batch_request.requester['impersonated'] == 'fantom'

    role_requests = list(batch_request.requests.order_by('label'))

    assert len(role_requests) == 2

    assert role_requests[0].label == 'unique_label1'
    assert role_requests[0].is_done is True
    assert role_requests[0].error == 'Вы должны указать либо сотрудника, либо группу'

    assert role_requests[1].label == 'unique_label2'
    assert role_requests[1].is_done is False
    assert role_requests[1].data == {
        'label': 'unique_label2',
        'path': '/manager/',
        'system': 'simple',
        'user': 'fantom',
    }


def test_delayed_role_request(client, simple_system, users_for_test):
    set_workflow(simple_system, 'approvers = []')

    batch_request_url = partial(reverse, api_name='v1', resource_name='batchrolerequest')
    create_batch_request_url = batch_request_url('api_dispatch_list')
    get_batch_request_url = lambda id: batch_request_url('api_dispatch_detail', pk=id)

    client.login('fantom')

    with run_commit_hooks():
        response = client.json.post(create_batch_request_url, {'requests': [
            {'label': 'unique_label1', 'user': 'fantom', 'path': '/manager/', 'system': 'simple'},
            {'label': 'unique_label2', 'user': 'terran', 'path': '/manager/', 'system': 'simple'},
        ]})
    assert response.status_code == 201

    batch_id = response.json()['id']
    response = client.json.get(get_batch_request_url(batch_id))
    assert response.status_code == 200

    batch_response = response.json()
    assert batch_response['id'] == batch_id
    assert batch_response['is_done'] is True

    request1 = sorted(batch_response['requests'], key=lambda x: x['label'])[0]
    assert request1['label'] == 'unique_label1'
    assert request1['is_done'] is True
    assert request1['role']['is_active'] is True
    assert request1['role']['state'] == 'granted'
    assert request1['error'] is None

    request2 = sorted(batch_response['requests'], key=lambda x: x['label'])[1]
    assert request2['label'] == 'unique_label2'
    assert request2['is_done'] is True
    assert request2['role'] is None
    assert request2['error'] is not None


@pytest.mark.parametrize('resource_name', ('batchrolerequest', 'batchroledeprive'))
def test_non_existent_request(client, users_for_test, resource_name):
    batch_request_url = partial(reverse, api_name='v1', resource_name=resource_name)
    get_batch_request_url = lambda id: batch_request_url('api_dispatch_detail', pk=id)

    client.login('fantom')

    response = client.json.get(get_batch_request_url(123))
    assert response.status_code == 404


def test_delayed_request_in_broken_system(client, simple_system, users_for_test):
    batch_request_url = partial(reverse, api_name='v1', resource_name='batchrolerequest')
    create_batch_request_url = batch_request_url('api_dispatch_list')
    get_batch_request_url = lambda id: batch_request_url('api_dispatch_detail', pk=id)

    simple_system.is_broken = True
    simple_system.save()

    client.login('fantom')

    with run_commit_hooks():
        response = client.json.post(create_batch_request_url, {'requests': [
            {'label': 'label', 'user': 'fantom', 'path': '/manager/', 'system': 'simple'},
        ]})
    assert response.status_code == 201

    batch_id = response.json()['id']
    response = client.json.get(get_batch_request_url(batch_id))
    batch_response = response.json()
    assert batch_response['requests'][0]['error'] is not None


def test_batchroledeprive(client, simple_system, users_for_test):
    user = User.objects.get(username='fantom')
    client.login(user.username)
    batch_request_url = partial(reverse, api_name='v1', resource_name='batchroledeprive')
    create_batch_request_url = batch_request_url('api_dispatch_list')
    get_batch_request_url = lambda id: batch_request_url('api_dispatch_detail', pk=id)

    deprived_role = raw_make_role(
        subject=user,
        system=simple_system,
        data={'role': 'manager'},
        state=ROLE_STATE.DEPRIVED,
    )
    granted_role = raw_make_role(
        subject=user,
        system=simple_system,
        data={'role': 'admin'},
        state=ROLE_STATE.GRANTED,
    )

    requests = [
        {'id': deprived_role.id, 'comment': 'Невалидный отзыв'},
        {'id': granted_role.id, 'comment': 'Валидный отзыв'},
    ]

    with run_commit_hooks():
        response = client.json.post(create_batch_request_url, {'requests': requests})

    assert response.status_code == 201
    batch_request = BatchRequest.objects.get(id=response.json()['id'])
    delayed_requests = sorted(batch_request.requests.all(), key=lambda x: requests.index(x.data))

    assert all(i.is_done for i in delayed_requests)
    assert delayed_requests[0].error == (
        'Роль находится в состоянии Отозвана, из которого отзыв невозможен'
    )
    assert not delayed_requests[1].error
    assert delayed_requests[1].role == granted_role
    assert delayed_requests[1].role.state == ROLE_STATE.DEPRIVED

    response = client.json.get(get_batch_request_url(batch_request.id))
    assert response.status_code == 200
    response_data = response.json()
    assert response_data['id'] == str(batch_request.id)
    assert response_data['is_done']


@pytest.mark.parametrize('requests, message', (
    ([], '`requests` property must be non-empty list of role requests'),
    ([{'comment': 'test'}], 'every request must have `id` property with id of existing role'),
    ([{'id': 222111}], 'every request must have `id` property with id of existing role'),
))
def test_batchroledeprive_wrong_requests(client, users_for_test, requests, message):
    client.login('fantom')
    url = reverse('api_dispatch_list', api_name='v1', resource_name='batchroledeprive')
    response = client.json.post(url, {'requests': requests})
    assert response.status_code == 400
    assert response.json()['message'] == message


def test_batchroledeprive_duplicates(client, simple_system, users_for_test):
    user = User.objects.get(username='fantom')
    client.login(user.username)
    url = reverse('api_dispatch_list', api_name='v1', resource_name='batchroledeprive')

    role = raw_make_role(
        subject=user,
        system=simple_system,
        data={'role': 'admin'},
        state=ROLE_STATE.GRANTED,
    )

    response = client.json.post(url, {'requests': [{'id': role.id}, {'id': role.id}]})
    assert response.status_code == 400
    assert response.json()['message'] == 'every request must have unique `id` property'
