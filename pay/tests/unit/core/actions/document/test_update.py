from dataclasses import fields
from uuid import uuid4

import pytest

from sendr_auth import User as AuthUser

from hamcrest import assert_that, equal_to, match_equality, not_none

from pay.bill_payments.bill_payments.core.actions.bill.search import SearchFinesAction
from pay.bill_payments.bill_payments.core.actions.document.create import CreateDocumentAction
from pay.bill_payments.bill_payments.core.actions.document.update import UpdateDocumentAction
from pay.bill_payments.bill_payments.core.exceptions import DocumentNotFoundError
from pay.bill_payments.bill_payments.interactions.kazna.entities import DocumentCode
from pay.bill_payments.bill_payments.storage.entities.document import Document
from pay.bill_payments.bill_payments.tests.utils import remove_keys


@pytest.mark.asyncio
async def test_returns_updated(params, update_payload, existing_document):
    returned = await UpdateDocumentAction(**params).run()

    expected_update_document_result = Document(
        document_id=existing_document.document_id,
        uid=777000,
        created=match_equality(not_none()),
        updated=match_equality(not_none()),
        **update_payload,
    )
    assert_that(returned, equal_to(expected_update_document_result))


@pytest.mark.asyncio
async def test_returns_updated__without_optionals(params, update_payload, existing_document):
    del params['title']
    returned = await UpdateDocumentAction(**params).run()

    expected_update_document_result = Document(
        document_id=existing_document.document_id,
        uid=777000,
        created=match_equality(not_none()),
        updated=match_equality(not_none()),
        title=None,
        **remove_keys(update_payload, 'title'),
    )
    assert_that(returned, equal_to(expected_update_document_result))


@pytest.mark.asyncio
async def test_updates_in_db(storage, params, existing_document):
    returned = await UpdateDocumentAction(**params).run()

    updated_document = await storage.document.get(existing_document.document_id)
    assert_that(returned, equal_to(updated_document))


@pytest.mark.asyncio
async def test_document_not_found(params):
    params['document_id'] = uuid4()

    with pytest.raises(DocumentNotFoundError):
        await UpdateDocumentAction(**params).run()


@pytest.mark.asyncio
async def test_document_does_not_belong_to_user(params, existing_document):
    params['user'] = AuthUser(uid=888000)
    params['document_id'] = existing_document.document_id

    with pytest.raises(DocumentNotFoundError):
        await UpdateDocumentAction(**params).run()


@pytest.mark.asyncio
async def test_schedules_search(storage, params, mocker):
    search_fines_mock = mocker.patch.object(SearchFinesAction, 'schedule', mocker.AsyncMock(return_value=2))

    await UpdateDocumentAction(**params).run()

    search_fines_mock.assert_called_once()


@pytest.fixture
def update_payload():
    update = {
        'code': DocumentCode.ELS,
        'value': 'thisiselsnumber',
        'title': 'MyUpdatedTitle',
    }
    assert all(
        field.name in update
        for field in fields(Document)
        if field.name not in ('created', 'updated', 'document_id', 'uid')
    )
    return update


@pytest.fixture
async def existing_document():
    return await CreateDocumentAction(
        title='MyTitle',
        value='license1',
        user=AuthUser(uid=777000),
        code=DocumentCode.DRIVER_LICENSE,
    ).run()


@pytest.fixture
def params(update_payload, existing_document):
    base_params = dict(
        user=AuthUser(uid=777000),
    )

    return base_params | update_payload | {'document_id': existing_document.document_id}
