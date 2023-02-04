import re

import pytest

from hamcrest import assert_that, equal_to


@pytest.fixture
def dev_uid(yandex_pay_settings):
    return yandex_pay_settings.NFC_BETA_UID_WHITELIST[0]


@pytest.fixture
def authentication(yandex_pay_settings, dev_uid, aioresponses_mocker):
    aioresponses_mocker.get(
        re.compile(f'^{yandex_pay_settings.BLACKBOX_API_URL}.*method=oauth.*'),
        status=200,
        payload={
            'status': {'value': 'VALID'},
            'oauth': {'uid': dev_uid, 'client_id': 'client_id'},
            'login_id': 'login_id',
        }
    )
    return {
        'headers': {
            'Authorization': 'OAuth 123',
        }
    }


@pytest.mark.asyncio
async def test_allowed_checking_should_work(
    app,
    authentication,
):
    r = await app.get(
        '/api/mobile/v1/wallet/app/beta/allowed',
        **authentication,
    )
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_body, equal_to({
        'code': 200,
        'data': {'allowed': True},
        'status': 'success',
    }))
