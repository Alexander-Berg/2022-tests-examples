from copy import deepcopy  # noqa

import pytest
from billing.hot.faas.python.faas.api.app import FaasApplication

from billing.hot.calculators.taxi.calculator.tests.json_encoder import json_dumps


@pytest.fixture(autouse=True)
def faas_settings():
    from billing.hot.faas.python.faas.conf import settings
    data = deepcopy(settings._settings)

    yield settings

    settings._settings = data


@pytest.fixture
def get_faas_app(aiohttp_client):
    async def _inner():
        return await aiohttp_client(FaasApplication(), json_serialize=json_dumps)

    return _inner


@pytest.fixture(scope='session')
async def faas_app(get_faas_app):
    return await get_faas_app()
