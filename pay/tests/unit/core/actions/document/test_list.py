from uuid import uuid4

import pytest

from sendr_auth import User

from hamcrest import assert_that, equal_to

from pay.bill_payments.bill_payments.core.actions.document.list import ListDocumentsOfUserAction
from pay.bill_payments.bill_payments.interactions.kazna.entities import DocumentCode
from pay.bill_payments.bill_payments.storage.entities.document import Document


@pytest.mark.asyncio
async def test_returns_list(storage, user):
    document = await storage.document.create(
        Document(document_id=uuid4(), uid=user.uid, code=DocumentCode.DRIVER_LICENSE, value='value')
    )

    result = await ListDocumentsOfUserAction(user=User(uid=user.uid)).run()

    assert_that(result, equal_to([document]))
