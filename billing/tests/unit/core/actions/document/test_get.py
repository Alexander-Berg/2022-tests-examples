from uuid import uuid4

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.document.get import GetDocumentAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import DocumentNotFoundError
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.document import Document


@pytest.mark.asyncio
async def test_success(document):
    returned = await GetDocumentAction(
        partner_id=document.partner_id,
        document_id=document.document_id,
    ).run()

    assert_that(
        returned,
        equal_to(document),
    )


@pytest.mark.asyncio
async def test_document_not_found(partner):
    with pytest.raises(DocumentNotFoundError):
        await GetDocumentAction(
            partner_id=partner.partner_id,
            document_id=uuid4(),
        ).run()


@pytest.mark.asyncio
async def test_document_is_alien(partner, document):
    with pytest.raises(DocumentNotFoundError):
        await GetDocumentAction(
            partner_id=uuid4(),
            document_id=document.document_id,
        ).run()


@pytest.fixture(autouse=True)
async def document(storage, partner):
    return await storage.document.create(
        Document(document_id=uuid4(), partner_id=partner.partner_id, path='/', digest='')
    )
