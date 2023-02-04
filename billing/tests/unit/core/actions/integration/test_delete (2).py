from uuid import uuid4

import pytest

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.integration.delete import DeleteIntegrationAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.integration.upsert import UpsertIntegrationAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import IntegrationNotFoundError
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import Merchant
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.psp import PSP


@pytest.fixture
async def merchant(storage, rands):
    return await storage.merchant.create(Merchant(name=rands()))


@pytest.fixture
async def psp(storage, rands):
    return await storage.psp.create(
        PSP(psp_id=uuid4(), psp_external_id=rands())
    )


@pytest.fixture
async def integration(merchant, psp, rands):
    return await UpsertIntegrationAction(
        integration_id=uuid4(),
        merchant_id=merchant.merchant_id,
        psp_id=psp.psp_id,
        creds=rands(),
    ).run()


@pytest.mark.asyncio
async def test_delete(integration):
    await DeleteIntegrationAction(integration_id=integration.integration_id).run()


@pytest.mark.asyncio
async def test_delete__integration_not_found():
    with pytest.raises(IntegrationNotFoundError):
        await DeleteIntegrationAction(integration_id=uuid4()).run()
