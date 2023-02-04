from unittest import mock
import pytest

from django.urls import reverse

from intranet.search.core import models
from intranet.search.core.swarm import Indexer

from intranet.search.tests.helpers import models_helpers as mh


pytestmark = [
    pytest.mark.django_db(transaction=False),
]


def _get_url(revision, search=None, index=''):
    args = list()
    args.append(search or revision.search)
    index = index or revision.index
    if index:
        args.append(index)
    return reverse('indexer-index-resource', args=args)


def _assert_response_error(response, reason='', status_code=400):
    assert response.status_code == status_code, response.content
    data = response.json()
    assert data['status'] == 'error'
    if reason:
        assert data['reason'] == reason


def _assert_logged(revision, type='create'):
    assert models.PushRecord.objects.filter(
        search=revision.search,
        index=revision.index,
        type=type,
    ).exists()


def _assert_response_ok(response):
    assert response.status_code == 200, response.content
    data = response.json()
    assert data['status'] == 'ok'
    assert 'push_id' in data


def test_get_not_allowed(api_client):
    """
    Метод GET не поддерживается
    """
    revision = mh.create_revision()
    url = _get_url(revision)
    r = api_client.get(url)
    assert r.status_code == 405


@pytest.mark.parametrize('data', [
    {
        'search': 'unknown_search',
    },
    {
        'index': 'unknown_index',
    },
])
def test_unknown_search_or_index(api_client, data):
    """
    400 при попытке переиндексировать источник, для которого нет обработчика
    """
    revision = mh.create_revision(**data)
    url = _get_url(revision)
    r = api_client.post(url, json={})
    _assert_response_error(r)


def test_bad_resource_data(api_client):
    """
    400, если передали невалидные данные
    """
    revision = mh.create_revision()
    url = _get_url(revision)
    data = {
        'data': 'badjsondata',
    }
    r = api_client.post(url, data=data)
    _assert_response_error(r)


@mock.patch('intranet.search.core.swarm.indexer.Indexer.next')
def test_post_create_resource(mocked_next, api_client):
    """
    При post запросе переиндексируем ресурс
    """
    revision = mh.create_revision()
    url = _get_url(revision)
    data = {
        'resource': 'some_resource_data',
    }
    r = api_client.post(url, json=data)

    _assert_response_ok(r)
    mocked_next.assert_called_once_with('push', data=data)
    _assert_logged(revision)


@mock.patch('intranet.search.core.swarm.indexer.Indexer.next')
def test_put_create_resource(mocked_next, api_client):
    """
    При put запросе переиндексируем ресурс
    """
    revision = mh.create_revision()
    url = _get_url(revision)
    data = {
        'resource': 'some_resource_data',
    }
    r = api_client.put(url, json=data)

    _assert_response_ok(r)
    mocked_next.assert_called_once_with('push', data=data)
    _assert_logged(revision)


@mock.patch('intranet.search.core.swarm.indexer.Indexer.next')
def test_delete_resource(mocked_next, api_client):
    """
    При delete запросе удаляем ресурс из индекса
    """
    revision = mh.create_revision()
    url = _get_url(revision)
    data = {
        'resource': 'some_resource_data',
    }
    r = api_client.delete(url, json=data)

    _assert_response_ok(r)
    mocked_next.assert_called_once_with('push', data=data, delete=True)
    _assert_logged(revision, type='delete')


@mock.patch('intranet.search.core.swarm.indexer.Indexer.next')
def test_unknown_error(mocked_next, api_client):
    """
    500, если случилась проблема при постановке задачи
    """
    revision = mh.create_revision()
    msg = 'stub error'
    mocked_next.side_effect = Exception(msg)

    url = _get_url(revision)
    r = api_client.post(url, json={})
    _assert_response_error(r, msg, status_code=500)


@mock.patch('intranet.search.core.swarm.indexer.Indexer.next', autospec=True)
def test_search_priority(mocked_next, api_client):
    """
    Выставляем приоритет в высший при приеме пушей
    """
    revision = mh.create_revision()
    url = _get_url(revision)
    r = api_client.post(url, json={})

    _assert_response_ok(r)

    obj = mocked_next.call_args[0][0]
    assert obj.options['priority'] == Indexer.PRIORITY_HIGH


@mock.patch('intranet.search.core.swarm.indexer.Indexer.next', autospec=True)
def test_many_revisions(mocked_next, api_client):
    """
    При запросе пуши отправляются во все активные ревизии
    """
    revision = mh.create_revision()
    active_revisions = [revision]

    for status, _ in models.Revision.STATUSES:
        rev = mh.Revision(
            search=revision.search,
            index=revision.index,
            status=status,
            service='intrasearch',
        )
        if status in ('active', 'new', 'ready'):
            active_revisions.append(rev)

    url = _get_url(revision)
    r = api_client.post(url, json={})

    _assert_response_ok(r)
    assert mocked_next.call_count == len(active_revisions)

    pushed = [call[0][0].options['revision']['id'] for call in mocked_next.call_args_list]
    expected = [r.pk for r in active_revisions]
    assert pushed == expected


@mock.patch('intranet.search.core.swarm.indexer.Indexer.next')
@pytest.mark.parametrize('http_method', ['post', 'put', 'delete'])
def test_csrf_exempt(mocked_next, csrf_api_client, http_method):
    """
    У апишки отключена csrf защита
    """
    revision = mh.create_revision()
    url = _get_url(revision)
    r = getattr(csrf_api_client, http_method)(url, json={})
    _assert_response_ok(r)


@mock.patch('intranet.search.core.swarm.tasks.reindex_one.delay')
def test_with_filters(mocked_delay, api_client):
    """
    Пуши на переиндексацию с фильтрами вызывают reindex_one с правильным списком ключей
    и указанным таймстампом
    """
    revision = mh.create_revision()
    filters = {
        'filter': 'some_filter',
        'other_filter': 'some_other_filter',
        'ts': 1536318158,
    }

    url = _get_url(revision)
    data = {
        'resource': 'some_resource_data',
        'filters': filters,
    }
    r = api_client.post(url, json=data)
    _assert_response_ok(r)
    mocked_delay.assert_called_once()
    assert mocked_delay.call_args[1].get('keys') == ['filter:some_filter', 'other_filter:some_other_filter']
    assert mocked_delay.call_args[1].get('ts') == filters['ts']
