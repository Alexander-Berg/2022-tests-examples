from uuid import uuid4

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, has_entries, match_equality

from pay.bill_payments.bill_payments.interactions.kazna.entities import DocumentCode
from pay.bill_payments.bill_payments.storage.entities.bill import Bill
from pay.bill_payments.bill_payments.storage.entities.document import Document
from pay.bill_payments.bill_payments.storage.entities.enums import BillStatus


@pytest.mark.usefixtures('mock_app_authentication')
@pytest.mark.asyncio
async def test_post_orders(app, user, storage, rands):
    document = await storage.document.create(
        Document(document_id=uuid4(), uid=user.uid, value=rands(), code=DocumentCode.DRIVER_LICENSE)
    )
    bill = await storage.bill.create(
        Bill(
            document_id=document.document_id,
            uid=user.uid,
            supplier_bill_id=rands(),
            status=BillStatus.NEW,
            amount=100,
            amount_to_pay=100,
            bill_date=utcnow(),
        )
    )

    resp = await app.post('/api/v1/orders', json={'bill_ids': [str(bill.bill_id)]})
    json_body = await resp.json()

    assert_that(resp.status, equal_to(201))
    assert_that(
        json_body,
        equal_to(
            {
                'status': 'success',
                'code': 201,
                'data': {
                    'order': match_equality(
                        has_entries(
                            {
                                'status': 'NEW',
                            }
                        )
                    ),
                },
            }
        ),
    )
