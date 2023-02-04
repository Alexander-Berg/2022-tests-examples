import tempfile

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.file_storage.root_public_key import RootPublicKeyStorage


@pytest.fixture
def keys_file():
    with tempfile.NamedTemporaryFile() as fp:
        fp.write('{"keys": [{"keyValue": "123", "protocolVersion": "def", "keyExpiration": "5555"}]}'.encode('utf-8'))
        fp.flush()
        yield fp


@pytest.mark.asyncio
async def test_response(mocker, app, keys_file, yandex_pay_settings):
    mocker.patch.object(RootPublicKeyStorage, 'FILE_PATH', keys_file.name)
    app.server.app.file_storage.load()

    r = await app.get('api/v1/keys/keys.json')
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(
        json_body,
        equal_to({
            'keys': [
                {
                    'keyValue': '123',
                    'protocolVersion': 'def',
                    'keyExpiration': '5555',
                },
            ],
        })
    )
