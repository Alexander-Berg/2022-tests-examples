from datetime import datetime, timezone
from uuid import UUID

import pytest

from hamcrest import assert_that, equal_to

from pay.bill_payments.bill_payments.core.actions.document.create import CreateDocumentAction
from pay.bill_payments.bill_payments.core.actions.document.list import ListDocumentsOfUserAction
from pay.bill_payments.bill_payments.interactions.kazna.entities import DocumentCode
from pay.bill_payments.bill_payments.storage.entities.document import Document
from pay.bill_payments.bill_payments.tests.utils import remove_keys

EXAMPLE_DOCUMENT = dict(
    document_id=UUID('4046a7b8-6467-4801-a8ee-a89e6d78dbcd'),
    uid=777000,
    code=DocumentCode.DRIVER_LICENSE,
    value='1692555555',
    title='the-title',
    created=datetime(2020, 11, 25, 13, 30, 59, tzinfo=timezone.utc),
    updated=datetime(2020, 11, 25, 13, 30, 59, tzinfo=timezone.utc),
)

EXAMPLE_DOCUMENT_RESPONSE = {
    'document_id': '4046a7b8-6467-4801-a8ee-a89e6d78dbcd',
    'type': 'DRIVER_LICENSE',
    'value': '1692555555',
    'title': 'the-title',
    'created': '2020-11-25T13:30:59+00:00',
    'updated': '2020-11-25T13:30:59+00:00',
}

EXAMPLE_POST_DATA = {
    'type': 'DRIVER_LICENSE',
    'value': '1692555666',
    'title': 'the-title',
}

EXAMPLE_CREATE_CALL_PARAMS = {
    'code': DocumentCode.DRIVER_LICENSE,
    'value': '1692555666',
    'title': 'the-title',
}


@pytest.fixture(autouse=True)
def mock_authentication(mocker, auth_user):
    return mocker.patch('sendr_auth.BlackboxAuthenticator.get_user', mocker.AsyncMock(return_value=auth_user))


@pytest.fixture
def uri():
    return '/api/v1/documents'


@pytest.mark.parametrize(
    'documents, expected_response_data',
    (
        pytest.param([EXAMPLE_DOCUMENT], {'documents': [EXAMPLE_DOCUMENT_RESPONSE]}, id='all-fields'),
        pytest.param(
            [EXAMPLE_DOCUMENT | {'title': None}],
            {'documents': [EXAMPLE_DOCUMENT_RESPONSE | {'title': None}]},
            id='with-nulls',
        ),
    ),
)
@pytest.mark.asyncio
async def test_get_documents_response(app, mock_action, documents, expected_response_data, uri):
    mock_action(ListDocumentsOfUserAction, [Document(**params) for params in documents])

    resp = await app.get(uri)

    assert_that(resp.status, equal_to(200))
    assert_that(
        await resp.json(),
        equal_to(
            {
                'status': 'success',
                'code': 200,
                'data': expected_response_data,
            }
        ),
    )


@pytest.mark.asyncio
async def test_get_documents_calls_list_action(app, mock_action, auth_user, uri):
    mock = mock_action(ListDocumentsOfUserAction, [Document(**EXAMPLE_DOCUMENT)])

    await app.get(uri)

    mock.assert_called_once_with(user=auth_user)


@pytest.mark.parametrize(
    'document, expected_response_data',
    (
        pytest.param(
            EXAMPLE_DOCUMENT,
            {'document': EXAMPLE_DOCUMENT_RESPONSE},
            id='all-fields',
        ),
        pytest.param(
            EXAMPLE_DOCUMENT | {'title': None},
            {'document': EXAMPLE_DOCUMENT_RESPONSE | {'title': None}},
            id='with-nulls',
        ),
    ),
)
@pytest.mark.asyncio
async def test_post_documents_response(app, mock_action, document, expected_response_data, uri):
    mock_action(CreateDocumentAction, Document(**document))

    resp = await app.post(uri, json=EXAMPLE_POST_DATA)

    assert_that(resp.status, equal_to(201))
    assert_that(
        await resp.json(),
        equal_to(
            {
                'status': 'success',
                'code': 201,
                'data': expected_response_data,
            }
        ),
    )


@pytest.mark.parametrize(
    'data, expected_error',
    (
        pytest.param(
            EXAMPLE_POST_DATA | {'type': 'UNKNOWN_TYPE_123'},
            {'type': ['Invalid enum member UNKNOWN_TYPE_123']},
            id='unknown-type',
        ),
        pytest.param(
            EXAMPLE_POST_DATA | {'value': 1632123456},
            {'value': ['Not a valid string.']},
            id='invalid-value-type',
        ),
        pytest.param(
            EXAMPLE_POST_DATA | {'value': '0011223344'},
            {'value': ['String does not match expected pattern.']},
            id='leading-zeroes-are-invalid',
        ),
        pytest.param(
            EXAMPLE_POST_DATA | {'title': 111},
            {'title': ['Not a valid string.']},
            id='invalid-title-type',
        ),
        pytest.param(
            remove_keys(EXAMPLE_POST_DATA, 'type'),
            {'type': ['Missing data for required field.']},
            id='type-is-required',
        ),
        pytest.param(
            remove_keys(EXAMPLE_POST_DATA, 'value'),
            {'value': ['Missing data for required field.']},
            id='value-is-required',
        ),
    ),
)
@pytest.mark.asyncio
async def test_post_documents_validates(app, data, expected_error, uri):
    resp = await app.post(uri, json=data)

    assert_that(resp.status, equal_to(400))
    assert_that(
        await resp.json(),
        equal_to(
            {
                'status': 'fail',
                'code': 400,
                'data': {
                    'message': 'SCHEMA_VALIDATION_ERROR',
                    'params': expected_error,
                },
            }
        ),
    )


@pytest.mark.parametrize(
    'data, expected_call_params',
    (
        pytest.param(
            EXAMPLE_POST_DATA,
            EXAMPLE_CREATE_CALL_PARAMS,
            id='full-call',
        ),
        pytest.param(
            remove_keys(EXAMPLE_POST_DATA, 'title'),
            remove_keys(EXAMPLE_CREATE_CALL_PARAMS, 'title'),
            id='without-optionals',
        ),
    ),
)
@pytest.mark.asyncio
async def test_post_documents_calls_action(app, mock_action, data, expected_call_params, auth_user, uri):
    expected_call_params['user'] = auth_user
    mock = mock_action(CreateDocumentAction, Document(**EXAMPLE_DOCUMENT))

    await app.post(uri, json=data)

    mock.assert_called_once_with(**expected_call_params)
