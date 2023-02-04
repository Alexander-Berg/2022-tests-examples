import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.entities.root_public_key import RootPublicKey
from billing.yandex_pay.yandex_pay.file_storage.root_public_key import RootPublicKeyStorage


@pytest.fixture
async def response(app, mocker):
    root_public_keys = [
        RootPublicKey(key_value='abc', protocol_version='def', key_expiration='12345')
    ]
    mocker.patch.object(RootPublicKeyStorage, 'get_keys', mocker.Mock(return_value=root_public_keys))

    return await app.get('api/v1/keys/keys.json')


def test_code(app, response):
    assert_that(response.status, equal_to(200))


@pytest.mark.asyncio
async def test_body(app, response):
    json_body = await response.json()

    assert_that(
        json_body,
        equal_to({
            'keys': [
                {
                    'keyValue': 'abc',
                    'protocolVersion': 'def',
                    'keyExpiration': '12345',
                },
            ],
        })
    )


def test_headers(app, response, yandex_pay_settings):
    assert_that(response.headers['Cache-Control'], equal_to(yandex_pay_settings.API_KEYS_CACHE_CONTROL_HEADER))
