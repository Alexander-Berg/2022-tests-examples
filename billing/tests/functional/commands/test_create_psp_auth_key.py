import uuid

import pytest
from click.testing import CliRunner

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.commands.create_psp_auth_key import cli
from billing.yandex_pay.yandex_pay.core.actions.psp.create_entity import create_psp_entity
from billing.yandex_pay.yandex_pay.core.entities.psp import PSP
from billing.yandex_pay.yandex_pay.storage import Storage


@pytest.fixture
def psp_external_id(rands):
    return rands()


@pytest.fixture
def key():
    return (
        'MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEwmTG26LY5/0bc7VwMfJ9kfqfLTlYr/Ue9c0G7jdFp08dWX2osZ22JR/S9PUWKblmyl1CSGiHV0'
        'bAtlbeC3QgjQ=='
    )


def test_create_psp_auth_key(
    loop, raw_db_engine, aioresponses_mocker, yandex_pay_settings, psp_external_id, key
):
    async def create_psp():
        async with raw_db_engine.acquire() as conn:
            storage = Storage(conn)
            psp = await create_psp_entity(
                storage,
                PSP(psp_id=uuid.uuid4(), psp_external_id=psp_external_id, public_key='', public_key_signature=''),
            )
            return psp

    aioresponses_mocker.post(
        f'{yandex_pay_settings.DUCKGO_API_URL}/v1/payment_token/verify_recipient_key',
        payload={'code': 200, 'status': 'success', 'data': {}},
    )

    psp = loop.run_until_complete(create_psp())
    runner = CliRunner()

    r = runner.invoke(cli, ['--psp-id', str(psp.psp_id), '--key', key])
    assert not r.exit_code, r

    async def check_psp_key():
        async with raw_db_engine.acquire() as conn:
            storage = Storage(conn)
            psp_key = await storage.psp_key.get(psp.psp_id, 1)
            assert_that(psp_key.key, equal_to(key))

    loop.run_until_complete(check_psp_key())
