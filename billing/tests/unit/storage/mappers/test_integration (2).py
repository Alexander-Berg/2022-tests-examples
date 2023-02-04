from uuid import uuid4

import psycopg2.errors
import pytest

from hamcrest import assert_that, contains_string, equal_to

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
def make_integration(storage, merchant, psp, rands):
    def _inner(**kwargs):
        kwargs = {
            'merchant_id': merchant.merchant_id,
            'psp_id': psp.psp_id,
            'status': IntegrationStatus.READY,
            'creds': rands(),
        } | kwargs
        return Integration(**kwargs)
    return _inner


@pytest.mark.asyncio
async def test_create(storage, make_integration):
    integration = make_integration()

    created = await storage.integration.create(integration)

    integration.created = created.created
    integration.updated = created.updated
    assert_that(
        created,
        equal_to(integration),
    )


@pytest.mark.asyncio
async def test_merchant_id_and_psp_id_pair_is_unique(storage, make_integration):
    integration1 = make_integration()
    await storage.integration.create(integration1)

    integration2 = make_integration()
    with pytest.raises(Integration.DuplicateMerchantPSPPair):
        await storage.integration.create(integration2)


@pytest.mark.asyncio
async def test_get(storage, make_integration):
    integration = make_integration()

    created = await storage.integration.create(integration)

    got = await storage.integration.get(integration.integration_id)

    assert_that(
        got,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(Integration.DoesNotExist):
        await storage.integration.get(uuid4())


@pytest.mark.asyncio
async def test_save(storage, make_integration):
    created = await storage.integration.create(make_integration())
    created.status = IntegrationStatus.DEPLOYED
    created.creds = ''
    created.enabled = False

    saved = await storage.integration.save(created)

    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_by_merchant_id_and_psp_id(storage, make_integration):
    created = await storage.integration.create(make_integration())

    found = await storage.integration.get_by_merchant_id_and_psp_id(
        merchant_id=created.merchant_id, psp_id=created.psp_id
    )

    assert_that(found, equal_to(created))


@pytest.mark.asyncio
async def test_merchant_id_foreign_key(storage, make_integration):
    with pytest.raises(psycopg2.errors.ForeignKeyViolation) as exc_info:
        await storage.integration.create(make_integration(merchant_id=uuid4()))

    assert_that(str(exc_info.value), contains_string('integrations_merchant_id_fkey'))


@pytest.mark.asyncio
async def test_psp_id_foreign_key(storage, make_integration):
    with pytest.raises(psycopg2.errors.ForeignKeyViolation) as exc_info:
        await storage.integration.create(make_integration(psp_id=uuid4()))

    assert_that(str(exc_info.value), contains_string('integrations_psp_id_fkey'))


@pytest.mark.asyncio
async def test_multiple_integrations_per_merchant_allowed(storage, make_integration, rands):
    await storage.integration.create(
        make_integration(status=IntegrationStatus.DEPLOYED)
    )
    new_psp = await storage.psp.create(
        PSP(psp_id=uuid4(), psp_external_id=rands())
    )

    await storage.integration.create(make_integration(psp_id=new_psp.psp_id))


@pytest.mark.asyncio
async def test_multiple_deployed_integrations_forbidden__on_create(storage, make_integration, rands):
    await storage.integration.create(
        make_integration(status=IntegrationStatus.DEPLOYED)
    )
    new_psp = await storage.psp.create(
        PSP(psp_id=uuid4(), psp_external_id=rands())
    )

    with pytest.raises(Integration.DuplicateDeployedIntegrationError):
        await storage.integration.create(
            make_integration(status=IntegrationStatus.DEPLOYED, psp_id=new_psp.psp_id)
        )


@pytest.mark.asyncio
async def test_multiple_deployed_integrations_forbidden__on_update(storage, make_integration, rands):
    await storage.integration.create(
        make_integration(status=IntegrationStatus.DEPLOYED)
    )
    new_psp = await storage.psp.create(
        PSP(psp_id=uuid4(), psp_external_id=rands())
    )
    integration2 = await storage.integration.create(make_integration(psp_id=new_psp.psp_id))

    integration2.status = IntegrationStatus.DEPLOYED
    with pytest.raises(Integration.DuplicateDeployedIntegrationError):
        await storage.integration.save(integration2)


@pytest.mark.asyncio
async def test_get_deployed_by_merchant_id(storage, make_integration, merchant, rands):
    target_integration = await storage.integration.create(
        make_integration(status=IntegrationStatus.DEPLOYED)
    )
    other_psp = await storage.psp.create(
        PSP(psp_id=uuid4(), psp_external_id=rands())
    )
    await storage.integration.create(
        make_integration(status=IntegrationStatus.READY, psp_id=other_psp.psp_id)
    )
    other_merchant = await storage.merchant.create(Merchant(name=rands()))
    await storage.integration.create(
        make_integration(merchant_id=other_merchant.merchant_id, status=IntegrationStatus.DEPLOYED)
    )

    result = await storage.integration.get_deployed_by_merchant_id(merchant.merchant_id)

    assert_that(result, equal_to(target_integration))
