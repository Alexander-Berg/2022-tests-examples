import json

from unittest import mock
import pytest

from intranet.search.core.storages import StorageUnavailable
from intranet.search.core.swarm.storage import InvalidDocument
from intranet.search.tests.helpers.indexations_helpers import (
    get_document_storage,
    create_document,
    get_base_expected_doc,
    set_saas_error
)


pytestmark = [pytest.mark.django_db(transaction=False)]


@pytest.fixture(scope="function")
def mocked_http_request():
    with mock.patch('intranet.search.core.utils.http.call_with_retry') as mocked_http_request:
        yield mocked_http_request


def assert_saas_called_with_json(mocked_http_request, body):
    mocked_http_request.assert_called_once()
    call_args = mocked_http_request.call_args[1]
    assert 'application/json' == call_args['headers']['Content-Type']
    assert body == json.loads(call_args['data'])


@pytest.mark.parametrize("realtime", [True, False])
def test_update(mocked_http_request, realtime):
    doc = create_document()
    storage = get_document_storage()
    storage.update(doc, realtime=realtime)

    expected_body = get_base_expected_doc(doc, storage.revision, realtime=realtime)
    assert_saas_called_with_json(mocked_http_request, expected_body)


def test_delete(mocked_http_request):
    doc = create_document()
    storage = get_document_storage()

    storage.delete(doc)

    expected_body = {
        'prefix': storage.revision['id'],
        'action': 'delete',
        'docs': [{
            'url': doc.url,
            'options': {'modification_timestamp': doc.updated_ts},
        }]
    }
    assert_saas_called_with_json(mocked_http_request, expected_body)


def test_flush(mocked_http_request):
    storage = get_document_storage()

    storage.flush()
    assert_saas_called_with_json(
        mocked_http_request,
        {'prefix': storage.revision['id'], 'action': 'reopen'}
    )


def test_purge(mocked_http_request):
    storage = get_document_storage()

    storage.purge()
    kps = storage.revision['id']
    expected_body = {
        'prefix': kps,
        'action': 'delete',
        'request': f'url:"*"&kps={kps}'
    }
    assert_saas_called_with_json(mocked_http_request, expected_body)


def test_delete_revision(mocked_http_request):
    storage = get_document_storage()

    storage.delete_revision()
    kps = storage.revision['id']
    expected_body = {
        'prefix': kps,
        'action': 'delete',
        'request': '$remove_kps$'
    }
    assert_saas_called_with_json(mocked_http_request, expected_body)


def test_send_obsolete_document(mocked_http_request):
    doc = create_document()
    storage = get_document_storage()
    set_saas_error(mocked_http_request, status=426, body='Incoming document is obsolete')

    try:
        storage.update(doc)
    except Exception as e:
        # не должно быть никаких исключений, просто молча пишем warning
        pytest.fail(e, pytrace=True)


def test_send_invalid_document(mocked_http_request):
    doc = create_document()
    storage = get_document_storage()
    set_saas_error(mocked_http_request, status=400)

    with pytest.raises(InvalidDocument):
        storage.update(doc)


def test_saas_unavailable(mocked_http_request):
    doc = create_document()
    storage = get_document_storage()
    set_saas_error(mocked_http_request, status=500)

    with pytest.raises(StorageUnavailable):
        storage.update(doc)
