from uuid import uuid4

import psycopg2
import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, has_length

from pay.bill_payments.bill_payments.storage import Storage
from pay.bill_payments.bill_payments.storage.entities.bill import Bill
from pay.bill_payments.bill_payments.storage.entities.bill_order import BillOrder
from pay.bill_payments.bill_payments.storage.entities.enums import BillStatus, OrderStatus
from pay.bill_payments.bill_payments.storage.entities.order import Order
from pay.bill_payments.bill_payments.storage.mappers.bill_order import BillOrderMapper
from pay.bill_payments.bill_payments.tests.unit.storage.mappers.base import BaseMapperTests


class TestBillOrderMapper(BaseMapperTests):
    @pytest.fixture
    def entity(self, order: Order, bill: Bill) -> BillOrder:
        return BillOrder(
            order_id=order.order_id,
            bill_id=bill.bill_id,
        )

    @pytest.fixture
    def mapper(self, storage: Storage) -> BillOrderMapper:
        return storage.bill_order

    @pytest.fixture
    async def bill(self, storage, user, document):
        return await storage.bill.create(
            Bill(
                uid=user.uid,
                supplier_bill_id='MapperBillID',
                status=BillStatus.NEW,
                document_id=document.document_id,
                amount=10000,
                amount_to_pay=9900,
                bill_date=utcnow(),
            )
        )

    @pytest.fixture
    async def order(self, storage, user):
        return await storage.order.create(Order(uid=user.uid, status=OrderStatus.NEW))

    @pytest.mark.asyncio
    async def test_get(self, mapper, entity):
        created = await mapper.create(entity)

        assert_that(
            await mapper.get(created.bill_id, created.order_id),
            equal_to(created),
        )

    @pytest.mark.asyncio
    async def test_get_not_found(self, mapper):
        with pytest.raises(BillOrder.DoesNotExist):
            await mapper.get(uuid4(), uuid4())

    @pytest.mark.asyncio
    async def test_find_by_order_id(self, mapper, entity, order):
        created = await mapper.create(entity)

        found = await mapper.find_by_order_id(order_id=order.order_id)
        assert_that(found, equal_to([created]))

    @pytest.mark.asyncio
    async def test_include_bills(self, mapper, entity, order, bill):
        created = await mapper.create(entity)

        found = await mapper.find_by_order_id(order_id=order.order_id, include_bill=True)

        created.bill = bill
        assert_that(found, equal_to([created]))

    @pytest.mark.asyncio
    async def test_find_by_order_id__when_not_found(self, mapper, entity, order):
        await mapper.create(entity)

        found = await mapper.find_by_order_id(order_id=uuid4())
        assert_that(found, has_length(0))

    @pytest.mark.asyncio
    async def test_has_foreign_key_on_order(self, mapper, entity):
        entity.order_id = uuid4()

        with pytest.raises(psycopg2.errors.ForeignKeyViolation):
            await mapper.create(entity)

    @pytest.mark.asyncio
    async def test_has_foreign_key_on_bill(self, mapper, entity):
        entity.bill_id = uuid4()

        with pytest.raises(psycopg2.errors.ForeignKeyViolation):
            await mapper.create(entity)

    @pytest.mark.asyncio
    async def test_delete(self, mapper, entity):
        created = await mapper.create(entity)

        await mapper.delete(created)

        with pytest.raises(BillOrder.DoesNotExist):
            await mapper.get(created.bill_id, created.order_id)
