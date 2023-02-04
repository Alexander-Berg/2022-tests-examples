from dataclasses import replace
from decimal import Decimal
from uuid import uuid4

import pytest
from pay.lib.entities.operation import OperationStatus, OperationType

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.checkout_order import StorageShippingMethod
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order_operation import Operation
from billing.yandex_pay_plus.yandex_pay_plus.storage.exceptions import OperationDuplicateExternalOrderIdError


@pytest.fixture
def merchant_id(stored_checkout_order):
    return stored_checkout_order.merchant_id


@pytest.fixture
async def checkout_order(stored_checkout_order):
    return stored_checkout_order


@pytest.fixture
def make_order_operation(checkout_order, entity_shipping_method):
    def _inner(**kwargs):
        kwargs = {
            'operation_id': uuid4(),
            'merchant_id': checkout_order.merchant_id,
            'checkout_order_id': checkout_order.checkout_order_id,
            'order_id': checkout_order.order_id,
            'amount': Decimal('10.00'),
            'operation_type': OperationType.AUTHORIZE,
            'cart': checkout_order.cart,
            'shipping_method': StorageShippingMethod.from_shipping_method(entity_shipping_method),
        } | kwargs
        return Operation(**kwargs)
    return _inner


@pytest.mark.asyncio
@pytest.mark.parametrize('operation_type', list(OperationType))
@pytest.mark.parametrize('status', list(OperationStatus))
async def test_create(storage, make_order_operation, operation_type, status):
    order_operation = make_order_operation(operation_type=operation_type, status=status)

    created = await storage.order_operation.create(order_operation)

    order_operation.created = created.created
    order_operation.updated = created.updated
    assert_that(
        created,
        equal_to(order_operation),
    )


@pytest.mark.asyncio
async def test_get(storage, make_order_operation):
    order_operation = make_order_operation()

    created = await storage.order_operation.create(order_operation)

    got = await storage.order_operation.get(order_operation.operation_id)

    assert_that(
        got,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(Operation.DoesNotExist):
        await storage.order_operation.get(uuid4())


@pytest.mark.asyncio
async def test_save(storage, make_order_operation):
    created = await storage.order_operation.create(make_order_operation())
    created.status = OperationStatus.SUCCESS
    created.reason = 'reason'
    created.external_operation_id = 'external_operation_id'
    created.amount += 1

    saved = await storage.order_operation.save(created)

    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_duplicated_external_operation_id__on_create(storage, make_order_operation):
    order_operation = make_order_operation(external_operation_id='id')
    await storage.order_operation.create(order_operation)

    order_operation.operation_id = uuid4()
    with pytest.raises(OperationDuplicateExternalOrderIdError):
        await storage.order_operation.create(order_operation)


@pytest.mark.asyncio
async def test_duplicated_external_operation_id__on_save(storage, make_order_operation):
    order_operation1 = make_order_operation(external_operation_id='id')
    await storage.order_operation.create(order_operation1)

    order_operation2 = make_order_operation(operation_id=uuid4())
    await storage.order_operation.create(order_operation2)

    order_operation2.external_operation_id = 'id'
    with pytest.raises(OperationDuplicateExternalOrderIdError):
        await storage.order_operation.save(order_operation2)


@pytest.mark.asyncio
async def test_get_by_merchant_and_external_operation_id(
    storage, merchant_id, make_order_operation
):
    created = await storage.order_operation.create(
        make_order_operation(external_operation_id='id')
    )

    loaded = await storage.order_operation.get_by_merchant_and_external_operation_id(
        merchant_id, 'id'
    )
    assert_that(loaded, equal_to(created))


@pytest.mark.asyncio
async def test_get_by_merchant_and_external_operation_id__not_found(
    storage, merchant_id, make_order_operation
):
    await storage.order_operation.create(make_order_operation())

    with pytest.raises(Operation.DoesNotExist):
        await storage.order_operation.get_by_merchant_and_external_operation_id(
            merchant_id, 'id2'
        )


class TestFindByCheckoutOrder:
    @pytest.mark.asyncio
    async def test_filters_by_checkout_order(self, storage, merchant_id, checkout_order, make_order_operation):
        operation = await storage.order_operation.create(make_order_operation())
        other_checkout_order = await storage.checkout_order.create(
            replace(checkout_order, checkout_order_id=uuid4(), order_id='1234')
        )
        await storage.order_operation.create(
            make_order_operation(checkout_order_id=other_checkout_order.checkout_order_id)
        )

        found_operation = await storage.order_operation.find_by_checkout_order(
            checkout_order_id=checkout_order.checkout_order_id,
        )
        assert_that(found_operation, equal_to([operation]))

    @pytest.mark.asyncio
    async def test_filters_by_type(self, storage, checkout_order, make_order_operation):
        await storage.order_operation.create(make_order_operation(operation_type=OperationType.AUTHORIZE))
        operation = await storage.order_operation.create(make_order_operation(operation_type=OperationType.CAPTURE))

        found_operation = await storage.order_operation.find_by_checkout_order(
            checkout_order_id=checkout_order.checkout_order_id,
            operation_type=OperationType.CAPTURE,
        )
        assert_that(found_operation, equal_to([operation]))

    @pytest.mark.asyncio
    async def test_filters_by_status(self, storage, checkout_order, make_order_operation):
        await storage.order_operation.create(make_order_operation(status=OperationStatus.PENDING))
        operation = await storage.order_operation.create(make_order_operation(status=OperationStatus.SUCCESS))

        found_operation = await storage.order_operation.find_by_checkout_order(
            checkout_order_id=checkout_order.checkout_order_id,
            status=OperationStatus.SUCCESS,
        )
        assert_that(found_operation, equal_to([operation]))
