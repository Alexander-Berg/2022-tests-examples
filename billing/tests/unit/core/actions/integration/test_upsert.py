import logging
from dataclasses import fields, replace
from uuid import uuid4

import pytest

from hamcrest import assert_that, equal_to, has_entries, has_item, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.integration.upsert import UpsertIntegrationAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import DuplicateMerchantPSPPairIntegrationError
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import IntegrationStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.integration import Integration
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
async def creds(rands):
    return rands()


@pytest.mark.asyncio
@pytest.mark.parametrize('status', list(IntegrationStatus))
async def test_create_integration(merchant, psp, status, creds, dummy_logs):
    integration_id = uuid4()
    integration = await UpsertIntegrationAction(
        integration_id=integration_id,
        merchant_id=merchant.merchant_id,
        psp_id=psp.psp_id,
        creds=creds,
        status=status,
    ).run()

    assert_that(
        integration,
        has_properties(
            integration_id=integration_id,
            merchant_id=merchant.merchant_id,
            psp_id=psp.psp_id,
            status=status,
            creds=creds,
            enabled=True,
        )
    )
    [log] = dummy_logs()
    assert_that(
        log,
        has_properties(
            message='MERCHANT_INTEGRATION_CREATED',
            levelno=logging.INFO,
            _context=has_entries(
                integration_id=integration_id,
                integration=integration,
            ),
        )
    )


_immutable_fields = {'integration_id', 'merchant_id', 'psp_id', 'created', 'updated'}
_updatable_fields = {'status': IntegrationStatus.DEPLOYED, 'creds': 'new_creds', 'enabled': False}


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'kwargs',
    [
        {field.name: _updatable_fields[field.name]} for field in fields(Integration)
        if field.name not in _immutable_fields
    ]
)
async def test_update_integration(merchant, psp, creds, kwargs, dummy_logs):
    integration_id = uuid4()

    created = await UpsertIntegrationAction(
        integration_id=integration_id,
        merchant_id=merchant.merchant_id,
        psp_id=psp.psp_id,
        creds=creds,
    ).run()

    updated = await UpsertIntegrationAction(
        integration_id=integration_id,
        merchant_id=merchant.merchant_id,
        psp_id=psp.psp_id,
        **kwargs,
    ).run()

    assert_that(updated, equal_to(replace(created, updated=updated.updated, **kwargs)))

    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                message='MERCHANT_INTEGRATION_UPDATED',
                levelno=logging.INFO,
                _context=has_entries(
                    integration_id=integration_id,
                    integration_before_update=created,
                    integration=updated,
                ),
            )
        )
    )


@pytest.mark.asyncio
async def test_duplicate_merchant_psp_pair(storage, merchant, psp, creds):
    await UpsertIntegrationAction(
        integration_id=uuid4(),
        merchant_id=merchant.merchant_id,
        psp_id=psp.psp_id,
        creds=creds,
    ).run()

    with pytest.raises(DuplicateMerchantPSPPairIntegrationError):
        await UpsertIntegrationAction(
            integration_id=uuid4(),
            merchant_id=merchant.merchant_id,
            psp_id=psp.psp_id,
            creds=creds,
        ).run()


@pytest.mark.asyncio
async def test_duplicate_deployed_integration__on_create(
    storage, merchant, psp, creds, rands
):
    new_psp = await storage.psp.create(
        PSP(psp_id=uuid4(), psp_external_id=rands())
    )
    integration1 = await UpsertIntegrationAction(
        integration_id=uuid4(),
        merchant_id=merchant.merchant_id,
        psp_id=psp.psp_id,
        creds=creds,
        status=IntegrationStatus.DEPLOYED,
    ).run()
    assert_that(integration1, has_properties(status=IntegrationStatus.DEPLOYED))

    integration2 = await UpsertIntegrationAction(
        integration_id=uuid4(),
        merchant_id=merchant.merchant_id,
        psp_id=new_psp.psp_id,
        creds=rands(),
        status=IntegrationStatus.DEPLOYED,
    ).run()
    integration1 = await storage.integration.get(integration1.integration_id)
    assert_that(integration1, has_properties(status=IntegrationStatus.READY))
    assert_that(integration2, has_properties(status=IntegrationStatus.DEPLOYED))


@pytest.mark.asyncio
async def test_duplicate_deployed_integration__on_update(
    storage, merchant, psp, creds, rands
):
    new_psp = await storage.psp.create(
        PSP(psp_id=uuid4(), psp_external_id=rands())
    )
    integration1 = await UpsertIntegrationAction(
        integration_id=uuid4(),
        merchant_id=merchant.merchant_id,
        psp_id=psp.psp_id,
        creds=creds,
        status=IntegrationStatus.DEPLOYED,
    ).run()
    integration2 = await UpsertIntegrationAction(
        integration_id=uuid4(),
        merchant_id=merchant.merchant_id,
        psp_id=new_psp.psp_id,
        creds=rands(),
    ).run()

    assert_that(integration1, has_properties(status=IntegrationStatus.DEPLOYED))
    assert_that(integration2, has_properties(status=IntegrationStatus.READY))

    integration2 = await UpsertIntegrationAction(
        integration_id=integration2.integration_id,
        merchant_id=merchant.merchant_id,
        psp_id=new_psp.psp_id,
        status=IntegrationStatus.DEPLOYED,
    ).run()

    integration1 = await storage.integration.get(integration1.integration_id)
    assert_that(integration1, has_properties(status=IntegrationStatus.READY))
    assert_that(integration2, has_properties(status=IntegrationStatus.DEPLOYED))
