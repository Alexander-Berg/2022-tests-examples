import subprocess

import json
import pytest

import yatest
import yql.api.v1.client as yql


class SPopen:
    def __init__(self, cmd: str):
        self.process = subprocess.Popen(
            cmd,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
        )

    def __call__(self, data) -> str:
        ddata = json.dumps(data)
        self.process.stdin.write(ddata.encode())
        self.process.stdin.write(b'\n')
        self.process.stdin.flush()
        output = self.process.stdout.readline().decode().strip()
        try:
            json.loads(output)
        except json.decoder.JSONDecodeError:
            raise ValueError((output, ddata))
        return output


@pytest.fixture()
def lbexport_formater():
    """
    Requeries to add DEPENDS(billing/hot/accounts/tests/utils/lbexport_formater) to test`s ya.make
    """
    cmd = yatest.common.binary_path('billing/hot/accounts/tests/utils/lbexport_formater/lbexport_formater')
    yield SPopen(cmd)


@pytest.fixture()
def actentity_formater():
    """
    Requeries to add DEPENDS(billing/hot/accrualer/internal/test_utils/actentity_formater) to test`s ya.make
    """
    cmd = yatest.common.binary_path('billing/hot/accrualer/internal/test_utils/actentity_formater/actentity_formater')
    yield SPopen(cmd)


@pytest.fixture()
def yt_client(yt):
    yield yt.yt_client


@pytest.fixture()
def yt_root(yt_client):
    path = yt_client.find_free_subpath('//')
    yt_client.create('map_node', path)
    yield path
    yt_client.remove(path, recursive=True, force=True)


@pytest.fixture()
def yql_client(yql_api):
    yield yql.YqlClient(
        server='localhost',
        port=yql_api.port,
        db='plato'
    )


@pytest.fixture()
def ns_config():
    yield {
        'detalization_sign_config': {
            'commissions_with_vat': -1,
            'commissions_with_vat_refunds': +1,
            'fuel_hold': -1,
            'cashless': +1,
            'cashless_refunds': -1,
            'compensations': +1,
            'compensations_refunds': -1,

            'agent_rewards': '+1',
            'agent_reward_refunds': '-1',
            'cashless_payable': '+1',
            'cashless_refunds_payable': '-1',
            'agent_rewards_payable': '+1',
            'agent_reward_refunds_payable': '-1'
        },
        'location_attrs_config': ['client_id', 'contract_id', 'currency'],
        'extract_fields_config': {
            'contract_external_id': 'event_batch.info.tariffer_payload.contract_external_id',
            'client_id': 'event.loc.client_id',
            'contract_id': 'event.loc.contract_id',
            'currency': 'event.loc.currency',
        },
        'account_types_config': {
            'commissions': 'ACTIVE',
            'commissions_refunds': 'PASSIVE',
            'commissions_with_vat': 'ACTIVE',
            'commissions_with_vat_refunds': 'PASSIVE',
            'fuel_hold': 'PASSIVE',
            'fuel_fact': 'ACTIVE',
            'cashless': 'PASSIVE',
            'cashless_refunds': 'ACTIVE',
            'compensations': 'PASSIVE',
            'compensations_refunds': 'ACTIVE',
            'payout': 'PASSIVE',
            'payout_sent': 'PASSIVE',

            'agent_rewards': 'PASSIVE',
            'agent_reward_refunds': 'ACTIVE',
            'cashless_payable': 'PASSIVE',
            'cashless_refunds_payable': 'ACTIVE',
            'agent_rewards_payable': 'PASSIVE',
            'agent_reward_refunds_payable': 'ACTIVE',
        },
    }
