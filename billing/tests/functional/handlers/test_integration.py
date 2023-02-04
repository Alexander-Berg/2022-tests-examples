from uuid import uuid4

import pytest

from hamcrest import assert_that, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import IntegrationStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.integration import Integration


@pytest.fixture
def integration_id():
    return uuid4()


@pytest.fixture
def merchant_id():
    return uuid4()


@pytest.fixture
def psp_id():
    return uuid4()


@pytest.fixture
def public_key():
    return (
        'MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEwmTG26LY5/0bc7VwMfJ9kfqfLTlYr/Ue9c0G7jdFp08dWX2osZ22JR/S9PUWKblmyl1CSGiHV0'
        'bAtlbeC3QgjQ=='
    )


@pytest.fixture
def url(integration_id):
    return f'/api/internal/v1/integrations/{integration_id}'


@pytest.fixture(autouse=True)
async def merchant(app, merchant_id):
    await app.put(
        f'/api/internal/v1/merchants/{merchant_id}',
        headers={'x-ya-service-ticket': 'dummy-service-ticket'},
        json={
            'name': 'merchant name',
            'origins': [{'origin': 'https://origin.test'}],
        },
        raise_for_status=True,
    )


@pytest.fixture(autouse=True)
async def psp(app, psp_id, public_key, rands):
    await app.put(
        f'/api/internal/v1/psp/{psp_id}',
        headers={'x-ya-service-ticket': 'dummy-service-ticket'},
        json={
            'psp_external_id': rands(),
            'psp_auth_keys': [
                {
                    'key': public_key,
                    'alg': 'ES256',
                }
            ],
        },
        raise_for_status=True,
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('status', list(IntegrationStatus))
async def test_create_integration(
    storage, app, url, status, merchant_id, psp_id, integration_id
):
    data = {
        'merchant_id': str(merchant_id),
        'psp_id': str(psp_id),
        'status': status.value,
        'creds': 'secret',
    }

    await app.patch(
        url,
        json=data,
        raise_for_status=True,
    )

    integration = await storage.integration.get(integration_id)
    assert_that(integration, has_properties(status=status, enabled=True))


@pytest.mark.asyncio
async def test_disable_integration(
    storage, app, url, merchant_id, psp_id, integration_id
):
    data = {
        'merchant_id': str(merchant_id),
        'psp_id': str(psp_id),
        'status': IntegrationStatus.DEPLOYED.value,
        'creds': 'secret',
    }
    await app.patch(
        url,
        json=data,
        raise_for_status=True,
    )

    data = {
        'merchant_id': str(merchant_id),
        'psp_id': str(psp_id),
        'enabled': False,
    }
    await app.patch(
        url,
        json=data,
        raise_for_status=True,
    )

    integration = await storage.integration.get(integration_id)
    assert_that(integration, has_properties(status=IntegrationStatus.DEPLOYED, enabled=False))


@pytest.mark.asyncio
async def test_delete_integration(
    storage, app, url, merchant_id, psp_id, integration_id
):
    data = {
        'merchant_id': str(merchant_id),
        'psp_id': str(psp_id),
        'status': IntegrationStatus.DEPLOYED.value,
        'creds': 'secret',
    }
    await app.patch(
        url,
        json=data,
        raise_for_status=True,
    )
    await storage.integration.get(integration_id)  # integration exists

    await app.delete(url, raise_for_status=True)

    with pytest.raises(Integration.DoesNotExist):
        await storage.integration.get(integration_id)
