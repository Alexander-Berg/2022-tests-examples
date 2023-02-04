import os

import aiohttp.pytest_plugin
import pytest
import ujson

from billing.hot.calculators.taxi.calculator.api.app import CalculatorApplication
from billing.hot.calculators.taxi.calculator.tests.json_encoder import json_dumps


def pytest_configure(config):
    os.environ['QLOUD_TVM_CONFIG'] = ujson.dumps({
        'BbEnvType': 1,
        'clients': {
            'calculator': {
                'secret': 'tvm_secret',
                'self_tvm_id': 0,
                'dsts': {},
            },
        },
    })


pytest_plugins = ['aiohttp.pytest_plugin']
del aiohttp.pytest_plugin.loop


@pytest.fixture
def loop(event_loop):
    return event_loop


@pytest.fixture
async def app(aiohttp_client):
    return await aiohttp_client(CalculatorApplication(), json_serialize=json_dumps)


@pytest.fixture
def mock_action(mocker):
    def _inner(action_cls, action_result=None):
        async def run(self):
            return action_result

        mocker.patch.object(action_cls, 'run', run)
        return mocker.patch.object(action_cls, '__init__', mocker.Mock(return_value=None))
    return _inner
