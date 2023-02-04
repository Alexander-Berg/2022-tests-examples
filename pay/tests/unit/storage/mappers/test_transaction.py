from uuid import uuid4

import psycopg2
import pytest

from hamcrest import assert_that, equal_to, has_length

from pay.bill_payments.bill_payments.storage import Storage
from pay.bill_payments.bill_payments.storage.entities.enums import OrderStatus, PaymentMethodType, TransactionStatus
from pay.bill_payments.bill_payments.storage.entities.order import Order
from pay.bill_payments.bill_payments.storage.entities.transaction import PayerData, Transaction
from pay.bill_payments.bill_payments.storage.mappers.transaction import TransactionMapper
from pay.bill_payments.bill_payments.tests.unit.storage.mappers.base import BaseMapperTests


class TestTransactionMapper(BaseMapperTests):
    @pytest.fixture
    def entity(self, order: Order) -> Transaction:
        return Transaction(
            order_id=order.order_id,
            status=TransactionStatus.NEW,
            amount=1000,
            external_payment_id='kazna-example-id',
            payer_data=PayerData(payer_full_name='John Doe'),
            payment_method=PaymentMethodType.YANDEX_PAY,
        )

    @pytest.fixture
    def mapper(self, storage: Storage) -> TransactionMapper:
        return storage.transaction

    @pytest.fixture
    async def order(self, storage, user):
        return await storage.order.create(Order(uid=user.uid, status=OrderStatus.NEW))

    @pytest.mark.asyncio
    async def test_get(self, mapper, entity):
        created = await mapper.create(entity)

        assert_that(
            await mapper.get(created.transaction_id),
            equal_to(created),
        )

    @pytest.mark.asyncio
    async def test_get_not_found(self, mapper):
        with pytest.raises(Transaction.DoesNotExist):
            await mapper.get(uuid4())

    @pytest.mark.asyncio
    async def test_find_by_order_id(self, mapper, entity, order):
        created = await mapper.create(entity)

        found = await mapper.find_by_order_id(order_id=order.order_id)
        assert_that(found, equal_to([created]))

    @pytest.mark.asyncio
    async def test_find_by_order_id__when_not_found(self, mapper, entity, order):
        await mapper.create(entity)

        found = await mapper.find_by_order_id(order_id=uuid4())
        assert_that(found, has_length(0))

    @pytest.mark.asyncio
    async def test_can_update(self, mapper, entity: Transaction):
        created: Transaction = await mapper.create(entity)

        created.status = TransactionStatus.PAID
        created.amount = 2000
        created.external_payment_id = 'other-external-id'
        created.payer_data.payer_full_name = 'Jane Doe'

        updated = await mapper.save(created)
        created.updated = updated.updated

        assert_that(
            updated,
            equal_to(created),
        )

    @pytest.mark.asyncio
    async def test_has_foreign_key_on_order(self, mapper, entity):
        entity.order_id = uuid4()

        with pytest.raises(psycopg2.errors.ForeignKeyViolation):
            await mapper.create(entity)

    @pytest.mark.asyncio
    async def test_delete(self, mapper, entity):
        created = await mapper.create(entity)

        await mapper.delete(created)

        with pytest.raises(Transaction.DoesNotExist):
            await mapper.get(created.transaction_id)
