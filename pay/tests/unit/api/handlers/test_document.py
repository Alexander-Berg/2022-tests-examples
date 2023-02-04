from datetime import datetime, timezone
from uuid import UUID

import pytest

from hamcrest import assert_that, equal_to

from pay.bill_payments.bill_payments.core.actions.document.delete import DeleteDocumentAction
from pay.bill_payments.bill_payments.core.actions.document.update import UpdateDocumentAction
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

EXAMPLE_PUT_DATA = {
    'type': 'DRIVER_LICENSE',
    'value': '1692555666',
    'title': 'the-title',
}

EXAMPLE_UPDATE_CALL_PARAMS = {
    'document_id': UUID('16853b8e-2cef-4ecf-a36a-89138752db8f'),
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


@pytest.mark.asyncio
async def test_document_delete(app, mock_action, uri):
    mock_action(DeleteDocumentAction)

    resp = await app.delete(f'{uri}/018a6476-49a8-4f49-9147-6c6274c898e1')

    assert_that(resp.status, equal_to(200))
    assert_that(
        await resp.json(),
        equal_to(
            {
                'status': 'success',
                'code': 200,
                'data': {},
            }
        ),
    )


@pytest.mark.asyncio
async def test_document_delete_calls_delete_action(app, mock_action, auth_user, uri):
    mock = mock_action(DeleteDocumentAction)

    await app.delete(f'{uri}/018a6476-49a8-4f49-9147-6c6274c898e1')

    mock.assert_called_once_with(user=auth_user, document_id=UUID('018a6476-49a8-4f49-9147-6c6274c898e1'))


@pytest.mark.asyncio
async def test_document_delete_validates_document_id(app, mock_action, auth_user, uri):
    mock_action(DeleteDocumentAction)

    resp = await app.delete(f'{uri}/hello-world')

    assert_that(resp.status, equal_to(400))
    assert_that(
        await resp.json(),
        equal_to(
            {
                'status': 'fail',
                'code': 400,
                'data': {
                    'message': 'SCHEMA_VALIDATION_ERROR',
                    'params': {
                        'document_id': ['Not a valid UUID.'],
                    },
                },
            }
        ),
    )


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
async def test_put_document_response(app, mock_action, document, expected_response_data, uri):
    mock_action(UpdateDocumentAction, Document(**document))

    resp = await app.put(f'{uri}/16853b8e-2cef-4ecf-a36a-89138752db8f', json=EXAMPLE_PUT_DATA)

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


@pytest.mark.parametrize(
    'data, expected_error',
    (
        pytest.param(
            EXAMPLE_PUT_DATA | {'type': 'UNKNOWN_TYPE_123'},
            {'type': ['Invalid enum member UNKNOWN_TYPE_123']},
            id='unknown-type',
        ),
        pytest.param(
            EXAMPLE_PUT_DATA | {'value': 1632123456},
            {'value': ['Not a valid string.']},
            id='invalid-value-type',
        ),
        pytest.param(
            EXAMPLE_PUT_DATA | {'title': 111},
            {'title': ['Not a valid string.']},
            id='invalid-title-type',
        ),
        pytest.param(
            remove_keys(EXAMPLE_PUT_DATA, 'type'),
            {'type': ['Missing data for required field.']},
            id='type-is-required',
        ),
        pytest.param(
            remove_keys(EXAMPLE_PUT_DATA, 'value'),
            {'value': ['Missing data for required field.']},
            id='value-is-required',
        ),
    ),
)
@pytest.mark.asyncio
async def test_put_document_validates(app, data, expected_error, uri):
    resp = await app.put(f'{uri}/16853b8e-2cef-4ecf-a36a-89138752db8f', json=data)

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


@pytest.mark.asyncio
async def test_put_documents_validates_document_id(app, mock_action, auth_user, uri):
    mock_action(DeleteDocumentAction)

    resp = await app.put(f'{uri}/hello-world', json=EXAMPLE_PUT_DATA)

    assert_that(resp.status, equal_to(400))
    assert_that(
        await resp.json(),
        equal_to(
            {
                'status': 'fail',
                'code': 400,
                'data': {
                    'message': 'SCHEMA_VALIDATION_ERROR',
                    'params': {
                        'document_id': ['Not a valid UUID.'],
                    },
                },
            }
        ),
    )


@pytest.mark.parametrize(
    'data, expected_call_params',
    (
        pytest.param(
            EXAMPLE_PUT_DATA,
            EXAMPLE_UPDATE_CALL_PARAMS,
            id='full-call',
        ),
        pytest.param(
            remove_keys(EXAMPLE_PUT_DATA, 'title'),
            remove_keys(EXAMPLE_UPDATE_CALL_PARAMS, 'title'),
            id='without-optionals',
        ),
    ),
)
@pytest.mark.asyncio
async def test_put_document_calls_action(app, mock_action, data, expected_call_params, auth_user, uri):
    expected_call_params['user'] = auth_user
    mock = mock_action(UpdateDocumentAction, Document(**EXAMPLE_DOCUMENT))

    await app.put(f'{uri}/16853b8e-2cef-4ecf-a36a-89138752db8f', json=data)

    mock.assert_called_once_with(**expected_call_params)
