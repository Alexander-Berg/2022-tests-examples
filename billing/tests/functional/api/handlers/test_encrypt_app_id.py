import pytest
from cryptography.fernet import Fernet

from hamcrest import assert_that, equal_to, has_entries, has_key


@pytest.fixture
def raw_app_id():
    return 'some_app_id'


@pytest.fixture
def raw_app_id_json(raw_app_id):
    return {
        'raw_app_id': raw_app_id
    }


@pytest.mark.asyncio
async def test_should_encrypt_id_with_fernet(
    app,
    raw_app_id,
    raw_app_id_json,
    yandex_pay_settings,
):
    fernet = Fernet(yandex_pay_settings.API_WALLET_FERNET_KEY)
    r = await app.post(
        '/api/mobile/v1/wallet/app/encrypted_id', json=raw_app_id_json
    )
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_body, has_entries({
        'code': 200,
        'data': has_key('encrypted_app_id'),
        'status': 'success',
    }))
    encrypted_app_id = json_body['data']['encrypted_app_id']
    assert_that(
        encrypted_app_id.startswith('1:')
    )
    assert_that(
        fernet.decrypt(encrypted_app_id[2:].encode()).decode(),
        equal_to(raw_app_id)
    )
