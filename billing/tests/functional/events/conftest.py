import pytest


@pytest.fixture(params=['aiohttp', 'faas'])
def mode(request):
    return request.param


ENDPOINT_MODULES = {
    'cashless': 'billing.hot.calculators.taxi.calculator.core.faas.cashless_adaptor.adaptor',
    'fuel': 'billing.hot.calculators.taxi.calculator.core.faas.fuel_adaptor.adaptor',
    'payout': 'billing.hot.calculators.taxi.calculator.core.faas.payout_adaptor.adaptor',
    'payout-interim': 'billing.hot.calculators.taxi.calculator.core.faas.payout_interim_adaptor.adaptor',
    'revenue': 'billing.hot.calculators.taxi.calculator.core.faas.revenue_adaptor.adaptor',
    'subvention': 'billing.hot.calculators.taxi.calculator.core.faas.subvention_adaptor.adaptor',
    'transfer-cancel': 'billing.hot.calculators.taxi.calculator.core.faas.transfer_cancel_adaptor.adaptor',
    'transfer-init': 'billing.hot.calculators.taxi.calculator.core.faas.transfer_init_adaptor.adaptor',
}


@pytest.fixture
def make_request(mode, aiohttp_client, app, faas_settings, get_faas_app):
    async def _inner(endpoint, data):
        if mode == 'aiohttp':
            response = await app.post(f'/v1/{endpoint}', json=data)
            return await response.json()
        elif mode == 'faas':
            module = ENDPOINT_MODULES.get(endpoint)
            if not module:
                raise RuntimeError(f'unknown module for endpoint {endpoint}')

            faas_settings.FUNCTION = module
            faas_app = await get_faas_app()

            response = await faas_app.post('/call/json', json=data)
            return await response.json()
        else:
            raise RuntimeError('unknown mode')

    return _inner
