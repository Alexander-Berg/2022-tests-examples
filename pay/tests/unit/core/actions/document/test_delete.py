from uuid import uuid4

import pytest

from sendr_auth import User

from pay.bill_payments.bill_payments.core.actions.bill.search import SearchFinesAction
from pay.bill_payments.bill_payments.core.actions.document.delete import DeleteDocumentAction
from pay.bill_payments.bill_payments.interactions.kazna.entities import DocumentCode
from pay.bill_payments.bill_payments.storage.entities.document import Document


@pytest.mark.asyncio
async def test_deletes(storage, user, mocker):
    search_fines_mock = mocker.patch.object(SearchFinesAction, 'schedule', mocker.AsyncMock(return_value=2))
    document = await storage.document.create(
        Document(document_id=uuid4(), uid=user.uid, code=DocumentCode.DRIVER_LICENSE, value='value')
    )

    await DeleteDocumentAction(user=User(uid=user.uid), document_id=document.document_id).run()

    search_fines_mock.assert_called_once()
    with pytest.raises(Document.DoesNotExist):
        await storage.document.get(document.document_id)


@pytest.mark.asyncio
async def test_when_does_not_exists__should_not_raise(storage, user):
    await DeleteDocumentAction(user=User(uid=user.uid), document_id=uuid4()).run()
