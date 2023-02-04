from dataclasses import replace

import pytest

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.psp import PSP


@pytest.fixture
async def stored_merchant(storage, entity_merchant):
    return await storage.merchant.create(replace(entity_merchant))


@pytest.fixture
async def stored_checkout_order(storage, entity_checkout_order, stored_merchant):
    return await storage.checkout_order.create(
        replace(
            entity_checkout_order,
            merchant_id=stored_merchant.merchant_id,
        )
    )


@pytest.fixture
async def stored_unittest_psp(storage, entity_unittest_psp):
    try:
        return await storage.psp.get_by_external_id(entity_unittest_psp.psp_external_id)
    except PSP.DoesNotExist:
        return await storage.psp.create(entity_unittest_psp)


@pytest.fixture
async def stored_psp(storage, entity_psp):
    return await storage.psp.create(entity_psp)


@pytest.fixture
async def stored_integration(storage, entity_integration, stored_psp, stored_merchant):
    return await storage.integration.create(
        replace(
            entity_integration,
            psp_id=stored_psp.psp_id,
            merchant_id=stored_merchant.merchant_id,
        )
    )


@pytest.fixture
async def stored_transaction(storage, entity_transaction, stored_integration, stored_checkout_order):
    return await storage.transaction.create(
        replace(
            entity_transaction,
            checkout_order_id=stored_checkout_order.checkout_order_id,
            integration_id=stored_integration.integration_id,
        )
    )


@pytest.fixture
async def stored_operation(storage, entity_operation, stored_merchant, stored_checkout_order):
    return await storage.order_operation.create(
        replace(
            entity_operation,
            checkout_order_id=stored_checkout_order.checkout_order_id,
            merchant_id=stored_merchant.merchant_id,
        )
    )
