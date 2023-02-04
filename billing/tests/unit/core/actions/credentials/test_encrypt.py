import json

import pytest

from hamcrest import assert_that, equal_to, has_entries

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.credentials.encrypt import (
    EncryptIntegrationCredentialsAction,
)
from billing.yandex_pay_admin.yandex_pay_admin.core.entities.enums import IntegrationPSPExternalID
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import (
    InvalidIntegrationCredentialValueError,
    UnsupportedPSPForIntegrationError,
)

CREDS_JSON = {'key': 'k3y', 'password': 'passw0rd', 'gateway_merchant_id': 'the-gwid'}
CREDS = json.dumps(CREDS_JSON)


@pytest.mark.asyncio
@pytest.mark.parametrize('for_testing', [True, False])
async def test_success(for_testing):
    encrypted = await EncryptIntegrationCredentialsAction(
        psp_external_id=IntegrationPSPExternalID.PAYTURE,
        creds=CREDS,
        for_testing=for_testing,
    ).run()

    decrypted_cryptogram = json.loads(EncryptIntegrationCredentialsAction.get_crypter(for_testing).decrypt(encrypted))
    assert_that(
        decrypted_cryptogram,
        equal_to(CREDS_JSON),
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('key', sorted(CREDS_JSON))
async def test_invalid_creds(key):
    creds = {k: v for k, v in CREDS_JSON.items() if k != key}

    with pytest.raises(InvalidIntegrationCredentialValueError) as exc_info:
        await EncryptIntegrationCredentialsAction(
            psp_external_id=IntegrationPSPExternalID.PAYTURE,
            creds=json.dumps(creds),
        ).run()

    assert_that(exc_info.value.params, has_entries(validation_errors={key: ['Missing data for required field.']}))


@pytest.mark.parametrize('psp_external_id', list(IntegrationPSPExternalID))
class TestDeserializationForEachPSP:
    @pytest.mark.asyncio
    async def test_deserialization_for_each_psp(self, psp_external_id, creds):
        encrypted = await EncryptIntegrationCredentialsAction(
            psp_external_id=psp_external_id,
            creds=json.dumps(creds),
            for_testing=True,
        ).run()

        decrypted_cryptogram = json.loads(
            EncryptIntegrationCredentialsAction.get_crypter(for_testing=True).decrypt(encrypted)
        )
        if psp_external_id == IntegrationPSPExternalID.UNITELLER:
            creds['send_receipt'] = False

        assert_that(
            decrypted_cryptogram,
            equal_to(creds),
        )

    @pytest.fixture
    def creds(self, psp_external_id):
        return {
            IntegrationPSPExternalID.PAYTURE: {
                'key': 'the-key',
                'password': 'the-password',
                'gateway_merchant_id': 'gwid',
            },
            IntegrationPSPExternalID.UNITELLER: {
                'login': 'the-login',
                'password': 'the-password',
                'gateway_merchant_id': 'gwid',
            },
            IntegrationPSPExternalID.ALFA_BANK: {
                'username': 'the-username',
                'password': 'the-password',
                'gateway_merchant_id': 'gwid',
            },
            IntegrationPSPExternalID.RBS: {
                'username': 'the-username',
                'password': 'the-password',
                'gateway_merchant_id': 'gwid',
                'acquirer': 'MTS',
            },
            IntegrationPSPExternalID.MOCK: {},
        }[psp_external_id]


@pytest.mark.asyncio
async def test_invalid_psp(mocker):
    mocker.patch.object(IntegrationPSPExternalID, '__eq__', return_value=False)

    with pytest.raises(UnsupportedPSPForIntegrationError):
        await EncryptIntegrationCredentialsAction(
            psp_external_id=IntegrationPSPExternalID.PAYTURE,
            creds=CREDS,
        ).run()
