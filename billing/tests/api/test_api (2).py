import pytest

from ift.api.utils import put_infra_event, transform_data, InfraException
from ift.core.models import EventLink
from ift.core.utils import environ

INFRA_AUTH = environ.get('INFRA_TOKEN', '') or 'OAuth <somethignhere>'


def test_put_event_exception(get_track_req, response_mock):

    data = {
        'infra': {
            'serviceId': None,
            'environmentId': None,
            'title': 'Выкладка: 1.20.8052044',
            'startTime': '',
            'finishTime': ''
        },
        'tracker': {
            'key': 'TESTIFT-2',
            'type': 'Выкладка',
            'priority': 'Средний',
            'status': 'Подтверждён'
        },
        'ift': {
            'statusFinish': ['решен', 'закрыт', 'выложен в прод']
        }
    }
    req_data = transform_data(event='', data=data, service_id='2440', environment_id='3890')
    assert req_data['environmentId'] == 3890
    assert req_data['serviceId'] == 2440

    def put():
        put_infra_event(
            event=None,
            data=req_data,
            auth=INFRA_AUTH,
            test=True,
        )

    with response_mock(
        'POST https://infra-api-test.yandex-team.ru/v1/events -> 400:'
        '{"message":"Incorrect combination of serviceId and environmentId"}',
    ):
        with pytest.raises(InfraException) as e:
            put()
        assert f'{e.value}' == 'Incorrect combination of serviceId and environmentId'

    with response_mock('POST https://infra-api-test.yandex-team.ru/v1/events -> 400:'):
        with pytest.raises(InfraException) as e:
            put()
        assert '400 Client Error' in f'{e.value}'


def test_put_infra_event(get_track_req, response_mock):

    use_mock = True

    data = get_track_req(service=2440, env=3890)
    req_data = transform_data(event='', data=data, service_id=1, environment_id=2)

    def put_event():
        return put_infra_event(
            event=event_id,
            data=req_data,
            auth=INFRA_AUTH,
            test=True,
        )

    with response_mock(
        'POST https://infra-api-test.yandex-team.ru/v1/events -> 200:'
        '{"id":12345,"title":"sometitle"}',
        bypass=not use_mock
    ):
        event_id = None
        event_id = put_event()
        assert isinstance(event_id, int)

        if use_mock:
            assert event_id == 12345

    with response_mock(
        f'PUT https://infra-api-test.yandex-team.ru/v1/events/{event_id} -> 200:'
        '{"id":12345,"title":"sometitle"}',
        bypass=not use_mock
    ):
        event_id = put_event()
        assert isinstance(event_id, int)


def test_handling(client, response_mock, get_track_req):

    response = client.post('/api/send/', data={}, content_type='application/json')

    assert response.status_code == 400
    data = response.json()
    assert data['status'] == 'error'
    assert data['errors'] == ['ValueError: Please provide Authorization header.']

    assert EventLink.objects.count() == 0

    def send(*, created: bool = True):
        response = client.post(
            '/api/send/',
            data=get_track_req(),
            HTTP_AUTHORIZATION='OAuth token',
            HTTP_INFRA_TEST='1',
            content_type='application/json'
        )
        data = response.json()
        assert data['status'] == 'success'
        assert data['data'] == {
            'events': {
                '12345': {'issue': 'ISSUE-1', 'service_id': 1234, 'environment_id': 5678, 'created': created}
            }}
        return data

    with response_mock(
        'POST https://infra-api-test.yandex-team.ru/v1/events -> 200:'
        '{"id":12345,"title":"sometitle"}'
    ):
        send()

    link = EventLink.objects.get(event=12345)
    assert link.issue == 'ISSUE-1'

    with response_mock(
        'PUT https://infra-api-test.yandex-team.ru/v1/events/12345 -> 200:'
        '{"id":12345,"title":"sometitle"}'
    ):
        send(created=False)

    assert EventLink.objects.count() == 1


def test_handling_batch(client, response_mock, get_track_req):

    data = get_track_req()
    data['ift']['batch'] = {
        1234: [333],
        1: [6, 8],
    }

    with response_mock([
        'POST https://infra-api-test.yandex-team.ru/v1/events -> 200:{"id":10,"title":"a"}',
        'POST https://infra-api-test.yandex-team.ru/v1/events -> 200:{"id":20,"title":"b"}',
        'POST https://infra-api-test.yandex-team.ru/v1/events -> 200:{"id":30,"title":"c"}',
        'POST https://infra-api-test.yandex-team.ru/v1/events -> 200:{"id":40,"title":"d"}',
    ]):
        response = client.post(
            '/api/send/',
            data=data,
            HTTP_AUTHORIZATION='OAuth token',
            HTTP_INFRA_TEST='1',
            content_type='application/json'
        )

    data = response.json()
    assert data['status'] == 'success'
    assert data['data']['events'] == {
        '10': {'issue': 'ISSUE-1', 'service_id': 1234, 'environment_id': 333, 'created': True},
        '20': {'issue': 'ISSUE-1', 'service_id': 1, 'environment_id': 6, 'created': True},
        '30': {'issue': 'ISSUE-1', 'service_id': 1, 'environment_id': 8, 'created': True},
        '40': {'issue': 'ISSUE-1', 'service_id': 1234, 'environment_id': 5678, 'created': True}
    }


def test_handling_batch_404(client, response_mock, get_track_req):

    data = get_track_req()

    def send(request):
        with response_mock(request):
            response = client.post(
                '/api/send/',
                data=data,
                HTTP_AUTHORIZATION='OAuth token',
                HTTP_INFRA_TEST='1',
                content_type='application/json'
            )
        return response.json(), response

    # попытка создать событие и ответный 404
    response_data, response = send('POST https://infra-api-test.yandex-team.ru/v1/events -> 404:Not found')

    assert response_data == {
        'status': 'error',
        'data': {'events': {}},
        'errors': [
            'InfraException: 404 Client Error: Not Found for url: '
            'https://infra-api-test.yandex-team.ru/v1/events']
    }
    assert response.status_code == 400

    # успешное создание
    _, response = send('POST https://infra-api-test.yandex-team.ru/v1/events -> 200:{"id":10,"title":"d"}')
    assert response.status_code == 200

    # попытка обновления и ответный 200 даже при 404
    response_data, response = send('PUT https://infra-api-test.yandex-team.ru/v1/events/10 -> 404:Not found')
    assert response.status_code == 200
