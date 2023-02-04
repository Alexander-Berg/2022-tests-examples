from uuid import uuid4

import psycopg2
import pytest

from hamcrest import assert_that, equal_to

from pay.bill_payments.bill_payments.storage import Storage
from pay.bill_payments.bill_payments.storage.entities.enums import OrderStatus
from pay.bill_payments.bill_payments.storage.entities.order import Order
from pay.bill_payments.bill_payments.storage.mappers.order import OrderMapper
from pay.bill_payments.bill_payments.tests.unit.storage.mappers.base import BaseMapperTests


class TestOrderMapper(BaseMapperTests):
    @pytest.fixture
    def entity(self, user) -> Order:
        return Order(uid=user.uid, status=OrderStatus.NEW)

    @pytest.fixture
    def mapper(self, storage: Storage) -> OrderMapper:
        return storage.order

    @pytest.mark.asyncio
    async def test_get(self, mapper, entity):
        created = await mapper.create(entity)

        assert_that(
            await mapper.get(created.order_id),
            equal_to(created),
        )

    @pytest.mark.asyncio
    async def test_get_not_found(self, mapper):
        with pytest.raises(Order.DoesNotExist):
            await mapper.get(uuid4())

    @pytest.mark.asyncio
    async def test_get_by_order_id_and_uid(self, mapper, entity):
        created = await mapper.create(entity)

        assert_that(
            await mapper.get_by_order_id_and_uid(uid=entity.uid, order_id=created.order_id),
            equal_to(created),
        )

    @pytest.mark.asyncio
    async def test_get_by_order_id_and_uid__when_not_found(self, mapper):
        with pytest.raises(Order.DoesNotExist):
            await mapper.get_by_order_id_and_uid(uid=123, order_id=uuid4())

    @pytest.mark.asyncio
    async def test_has_foreign_key_on_user(self, mapper, entity, randn, user):
        entity.uid = user.uid + randn()

        with pytest.raises(psycopg2.errors.ForeignKeyViolation):
            await mapper.create(entity)

    @pytest.mark.asyncio
    async def test_can_update(self, mapper, entity: Order):
        created: Order = await mapper.create(entity)

        created.status = OrderStatus.PAID

        updated = await mapper.save(created)
        created.updated = updated.updated

        assert_that(
            updated,
            equal_to(created),
        )

    @pytest.mark.asyncio
    async def test_delete(self, mapper, entity):
        created = await mapper.create(entity)

        await mapper.delete(created)

        with pytest.raises(Order.DoesNotExist):
            await mapper.get(created.order_id)
