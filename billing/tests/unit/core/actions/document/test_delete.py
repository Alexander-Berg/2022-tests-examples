from uuid import uuid4

import pytest

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.document.delete import DeleteDocumentAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.document.delete_from_storage import (
    DeleteDocumentFromFileStorageAction,
)
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.document import Document


@pytest.mark.asyncio
async def test_deletes_document(storage, document, mock_delete_from_storage):
    await DeleteDocumentAction(document.document_id).run()

    with pytest.raises(Document.DoesNotExist):
        await storage.document.get(document.document_id)


@pytest.mark.asyncio
async def test_calls_delete_from_storage(storage, document, mock_delete_from_storage):
    await DeleteDocumentAction(document.document_id).run()

    mock_delete_from_storage.assert_run_once_with(path='/file-s3-path')


@pytest.mark.asyncio
async def test_not_raises_when_document_not_found(storage):
    await DeleteDocumentAction(uuid4()).run()


@pytest.fixture
async def document(storage, partner):
    return await storage.document.create(
        Document(
            document_id=uuid4(),
            partner_id=partner.partner_id,
            path='/file-s3-path',
            digest='',
            name='file.png',
        )
    )


@pytest.fixture(autouse=True)
def mock_delete_from_storage(mock_action):
    return mock_action(DeleteDocumentFromFileStorageAction)
