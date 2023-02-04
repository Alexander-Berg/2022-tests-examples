import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.credentials.encrypt import (
    EncryptIntegrationCredentialsAction,
)
from billing.yandex_pay_admin.yandex_pay_admin.core.entities.enums import IntegrationPSPExternalID


@pytest.fixture
def url():
    return '/api/web/v1/credentials/encrypt'


@pytest.fixture
def creds():
    return {'key': 'k3y', 'password': 'passw0rd', 'gateway_merchant_id': 'the-gwid'}


@pytest.mark.asyncio
@pytest.mark.parametrize('for_testing', [True, False])
async def test_encrypt_credentials(app, url, for_testing, mock_action):
    cipher = 'fake_cipher'
    mock = mock_action(EncryptIntegrationCredentialsAction, cipher)

    r = await app.post(
        url,
        json={
            'psp_external_id': 'payture',
            'creds': 'fake_creds',
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
                'data': {'cipher': cipher},
            }
        ),
    )
    mock.assert_run_once_with(
        psp_external_id=IntegrationPSPExternalID.PAYTURE,
        creds='fake_creds',
        for_testing=for_testing,
    )


@pytest.mark.asyncio
async def test_empty_creds(app, url):
    r = await app.post(url, json={'psp_external_id': 'payture', 'creds': ''})

    assert_that(r.status, equal_to(400))
    assert_that(
        await r.json(),
        equal_to(
            {
                'code': 400,
                'status': 'fail',
                'data': {
                    'message': 'SCHEMA_VALIDATION_ERROR',
                    'params': {'creds': ['String should not be empty.']},
                },
            }
        ),
    )
