import json

import pytest

from sendr_pytest.matchers import convert_then_match

from hamcrest import assert_that, equal_to, match_equality

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.credentials.encrypt import (
    EncryptIntegrationCredentialsAction,
)


@pytest.fixture
def url():
    return '/api/web/v1/credentials/encrypt'


@pytest.fixture
def creds():
    return {'key': 'k3y', 'password': 'passw0rd', 'gateway_merchant_id': 'the-gwid'}


@pytest.mark.asyncio
@pytest.mark.parametrize('for_testing', [True, False])
async def test_success(app, url, creds, for_testing):
    r = await app.post(
        url,
        json={
            'psp_external_id': 'payture',
            'creds': json.dumps(creds),
            'for_testing': for_testing,
        },
    )

    assert_that(r.status, equal_to(200))
    assert_that(
        await r.json(),
        equal_to(
            {
                'code': 200,
                'status': 'success',
                'data': {
                    'cipher': match_equality(
                        convert_then_match(
                            lambda cipher: json.loads(
                                EncryptIntegrationCredentialsAction.get_crypter(for_testing).decrypt(cipher),
                            ),
                            creds,
                        )
                    )
                },
            }
        ),
    )
