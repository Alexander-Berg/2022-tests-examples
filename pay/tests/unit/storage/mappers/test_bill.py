from datetime import timedelta
from uuid import uuid4

import psycopg2
import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, contains_inanyorder, equal_to

from pay.bill_payments.bill_payments.interactions.kazna.entities import DepartmentType
from pay.bill_payments.bill_payments.storage import Storage
from pay.bill_payments.bill_payments.storage.entities.bill import Bill
from pay.bill_payments.bill_payments.storage.entities.document import Document
from pay.bill_payments.bill_payments.storage.entities.enums import BillStatus
from pay.bill_payments.bill_payments.storage.entities.user import User
from pay.bill_payments.bill_payments.storage.mappers.bill import BillMapper
from pay.bill_payments.bill_payments.tests.unit.storage.mappers.base import BaseMapperTests


class TestBillMapper(BaseMapperTests):
    @pytest.fixture
    def entity(self, user: User, document: Document) -> Bill:
        return Bill(
            uid=user.uid,
            supplier_bill_id='supplier_bill_id',
            document_id=document.document_id,
            status=BillStatus.NEW,
            amount=100,
            amount_to_pay=20,
            bill_date=utcnow(),
        )

    @pytest.fixture
    def autofill_with_uuid(self):
        return ['bill_id']

    @pytest.fixture
    def mapper(self, storage: Storage) -> BillMapper:
        return storage.bill

    @pytest.mark.asyncio
    async def test_can_find_by_uid(self, mapper, entity):
        bill = await mapper.create(entity)
        entity.supplier_bill_id = 'another_supplier_bill_id'
        entity.bill_id = uuid4()
        another_bill = await mapper.create(entity)

        found = await mapper.find_by_uid(entity.uid)

        assert_that(
            found,
            contains_inanyorder(
                bill,
                another_bill,
            ),
        )

    @pytest.mark.asyncio
    async def test_should_desc_order_on_find_unpaid(self, mapper, entity):
        first_entity = await mapper.create(entity)
        entity.supplier_bill_id = 'another_one'
        entity.bill_date += timedelta(minutes=10)
        second_entity = await mapper.create(entity)
        entity.supplier_bill_id = 'yet_another_one'
        entity.bill_date -= timedelta(minutes=5)
        third_entity = await mapper.create(entity)

        found_result = await mapper.find_latest_unpaid_by_uid(entity.uid)

        assert_that(found_result, equal_to([second_entity, third_entity, first_entity]))

    @pytest.mark.asyncio
    async def test_should_filter_status_on_find_unpaid(self, mapper, entity):
        first_entity = await mapper.create(entity)
        entity.status = BillStatus.PAID
        entity.supplier_bill_id = 'another_one'
        await mapper.create(entity)
        entity.status = BillStatus.GONE
        entity.supplier_bill_id = 'yet_another_one'
        await mapper.create(entity)

        found_result = await mapper.find_latest_unpaid_by_uid(entity.uid)

        assert_that(found_result, equal_to([first_entity]))

    @pytest.mark.asyncio
    async def test_should_support_limit_on_find_unpaid(self, mapper, entity):
        await mapper.create(entity)
        entity.supplier_bill_id = 'another_one'
        await mapper.create(entity)

        found_result = await mapper.find_latest_unpaid_by_uid(uid=entity.uid, limit=1)

        assert len(found_result) == 1

    @pytest.mark.asyncio
    async def test_get(self, mapper, entity):
        created = await mapper.create(entity)

        assert_that(
            await mapper.get(created.bill_id),
            equal_to(created),
        )

    @pytest.mark.asyncio
    async def test_get_not_found(self, mapper):
        with pytest.raises(Bill.DoesNotExist):
            await mapper.get(uuid4())

    @pytest.mark.asyncio
    async def test_can_update(self, mapper, entity: Bill):
        created = await mapper.create(entity)

        created.purpose = 'purpose'
        created.kbk = 'kbk'
        created.oktmo = 'oktmo'
        created.inn = 'inn'
        created.kpp = 'kpp'
        created.bik = 'bik'
        created.account_number = 'account_number'
        created.payee_name = 'payee_name'
        created.payer_name = 'payer_name'
        created.div_id = 1234
        created.treasure_branch = 'treasure_branch'
        created.dep_type = DepartmentType.FNS
        created.discount_size = 'discount_size'
        created.discount_date = utcnow().date()
        created.offense_name = 'speed'
        created.offense_place = 'center'
        created.offense_date = utcnow()
        created.paid_amount = 300
        created.paid_date = utcnow()
        created.status = BillStatus.PAID

        updated = await mapper.save(created)
        created.updated = updated.updated

        assert_that(
            updated,
            equal_to(created),
        )

    @pytest.mark.asyncio
    async def test_unique_for_supplier_bill_id_with_uid(self, mapper, entity):
        await mapper.create(entity)

        entity.supplier_bill_id = 'another_bill_id'
        await mapper.create(entity)

        with pytest.raises(Bill.AlreadyExists):
            await mapper.create(entity)

    @pytest.mark.asyncio
    async def test_has_foreign_key_on_user(self, mapper, entity):
        entity.uid = 11111

        with pytest.raises(psycopg2.errors.ForeignKeyViolation):
            await mapper.create(entity)

    @pytest.mark.asyncio
    async def test_delete(self, mapper, entity):
        created = await mapper.create(entity)

        await mapper.delete(created)

        found = await mapper.find_by_uid(entity.uid)

        assert not found
