import json
import logging
from uuid import UUID

import pytest

from sendr_pytest.helpers import ensure_all_fields

from hamcrest import all_of, assert_that, equal_to, has_entries, has_items, has_properties, has_value, not_

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.credentials.encrypt import (
    EncryptIntegrationCredentialsAction,
)
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.integration.create import CreateIntegrationAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.partner.register_in_balance import (
    RegisterPartnerInBalanceAction,
)
from billing.yandex_pay_admin.yandex_pay_admin.core.entities.enums import IntegrationPSPExternalID
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import (
    IntegrationAlreadyExistsError,
    InvalidIntegrationStatusError,
    UnsupportedPSPForIntegrationError,
)
from billing.yandex_pay_admin.yandex_pay_admin.interactions import (
    YandexPayPlusBackendClient,
    YandexPayPlusSandboxClient,
)
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import IntegrationStatus
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.integration import Integration
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.merchant import Merchant
from billing.yandex_pay_admin.yandex_pay_admin.tests.utils import is_datetime_with_tz, is_uuid

CREDS_JSON = {'key': 'k3y', 'password': 'passw0rd', 'gateway_merchant_id': 'the-gwid'}
CREDS = json.dumps(CREDS_JSON)


@pytest.fixture
async def merchant(storage, partner, role):
    return await storage.merchant.create(
        Merchant(
            name='merchant name',
            partner_id=partner.partner_id,
        )
    )


@pytest.fixture
def for_testing():
    return True


@pytest.fixture
def mock_pay_client(mocker, for_testing):
    mock = mocker.AsyncMock()
    client_cls = YandexPayPlusSandboxClient if for_testing else YandexPayPlusBackendClient
    return mocker.patch.object(client_cls, 'upsert_integration', mock)


@pytest.mark.asyncio
async def test_create_integration__no_creds(user, merchant, for_testing, mock_pay_client):
    action_kwargs = {
        'user': user,
        'merchant_id': merchant.merchant_id,
        'psp_external_id': IntegrationPSPExternalID.PAYTURE,
        'for_testing': for_testing,
    }
    integration_kwargs = {
        'integration_id': is_uuid(),
        'merchant_id': merchant.merchant_id,
        'psp_id': UUID('678de147-6ff0-4171-8bf8-5796c7b717c3'),
        'psp_external_id': 'payture',
        'status': IntegrationStatus.AWAITING_CREDENTIALS,
        'revision': 1,
        'for_testing': for_testing,
        'created': is_datetime_with_tz(),
        'updated': is_datetime_with_tz(),
    }
    expected_integration = ensure_all_fields(Integration, **integration_kwargs)

    integration = await CreateIntegrationAction(**action_kwargs).run()

    assert_that(integration, equal_to(expected_integration))
    mock_pay_client.assert_not_awaited()


@pytest.mark.asyncio
@pytest.mark.parametrize('for_testing', [False, True])
@pytest.mark.parametrize('encrypted', [False, True])
@pytest.mark.parametrize(
    'status,integration_status',
    [
        (None, IntegrationStatus.READY),
        (IntegrationStatus.READY, IntegrationStatus.READY),
        (IntegrationStatus.DEPLOYED, IntegrationStatus.DEPLOYED),
    ],
)
async def test_create_integration__with_creds(
    user, status, integration_status, merchant, for_testing, encrypted, mock_pay_client, mock_action
):
    encrypt_mock = mock_action(EncryptIntegrationCredentialsAction, CREDS)
    action_kwargs = {
        'user': user,
        'merchant_id': merchant.merchant_id,
        'psp_external_id': IntegrationPSPExternalID.PAYTURE,
        'for_testing': for_testing,
        'creds': CREDS,
        'encrypted': encrypted,
        'status': status,
    }
    integration_kwargs = {
        'integration_id': is_uuid(),
        'merchant_id': merchant.merchant_id,
        'psp_id': UUID('678de147-6ff0-4171-8bf8-5796c7b717c3'),
        'psp_external_id': 'payture',
        'status': integration_status,
        'revision': 1,
        'for_testing': for_testing,
        'created': is_datetime_with_tz(),
        'updated': is_datetime_with_tz(),
    }
    expected_integration = ensure_all_fields(Integration, **integration_kwargs)

    integration = await CreateIntegrationAction(**action_kwargs).run()

    assert_that(integration, equal_to(expected_integration))
    mock_pay_client.assert_awaited_once_with(
        integration=integration,
        creds=CREDS,
    )
    if encrypted:
        encrypt_mock.assert_not_run()
    else:
        encrypt_mock.assert_run_once_with(
            psp_external_id=IntegrationPSPExternalID.PAYTURE,
            creds=CREDS,
            for_testing=for_testing,
        )


@pytest.mark.asyncio
@pytest.mark.usefixtures('mock_pay_client')
async def test_call_logged(user, merchant, dummy_logs, for_testing):
    action_kwargs = {
        'user': user,
        'merchant_id': merchant.merchant_id,
        'psp_external_id': IntegrationPSPExternalID.PAYTURE,
        'creds': 'secret',
        'status': IntegrationStatus.READY,
        'encrypted': True,
        'for_testing': for_testing,
    }

    integration = await CreateIntegrationAction(**action_kwargs).run()

    logs = dummy_logs()
    assert_that(
        logs,
        has_items(
            has_properties(
                message='CREATE_INTEGRATION_REQUEST_RECEIVED',
                levelno=logging.INFO,
                _context=all_of(
                    has_entries(
                        uid=user.uid,
                        merchant_id=merchant.merchant_id,
                        status=IntegrationStatus.READY,
                    ),
                    not_(has_value('secret')),
                ),
            ),
            has_properties(
                message='INTEGRATION_CREATED',
                levelno=logging.INFO,
                _context=all_of(
                    has_entries(
                        uid=user.uid,
                        merchant_id=merchant.merchant_id,
                        status=IntegrationStatus.READY,
                        integration_id=integration.integration_id,
                        integration=integration,
                    ),
                    not_(has_value('secret')),
                ),
            ),
        ),
    )


@pytest.mark.asyncio
async def test_integration_already_exists(user, merchant):
    action_kwargs = {
        'user': user,
        'merchant_id': merchant.merchant_id,
        'psp_external_id': IntegrationPSPExternalID.PAYTURE,
    }
    action = CreateIntegrationAction(**action_kwargs)

    await action.run()

    with pytest.raises(IntegrationAlreadyExistsError):
        await action.run()


@pytest.mark.asyncio
async def test_unsupported_psp(user, merchant, mocker):
    mock = mocker.Mock()
    mock.get_psp_id.side_effect = UnsupportedPSPForIntegrationError
    action_kwargs = {
        'user': user,
        'merchant_id': merchant.merchant_id,
        'psp_external_id': mock,
    }

    with pytest.raises(UnsupportedPSPForIntegrationError):
        await CreateIntegrationAction(**action_kwargs).run()


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'creds,status',
    [
        (None, IntegrationStatus.READY),
        (None, IntegrationStatus.DEPLOYED),
        ('secret', IntegrationStatus.AWAITING_CREDENTIALS),
    ],
)
async def test_invalid_status(user, merchant, creds, status):
    action_kwargs = {
        'user': user,
        'merchant_id': merchant.merchant_id,
        'psp_external_id': IntegrationPSPExternalID.PAYTURE,
        'creds': creds,
        'status': status,
    }

    with pytest.raises(InvalidIntegrationStatusError):
        await CreateIntegrationAction(**action_kwargs).run()


@pytest.mark.asyncio
@pytest.mark.parametrize('for_testing', [False])
async def test_register_in_balance(user, storage, merchant, partner, for_testing, mock_pay_client, mocker):

    action_kwargs = {
        'user': user,
        'merchant_id': merchant.merchant_id,
        'psp_external_id': IntegrationPSPExternalID.PAYTURE,
        'for_testing': for_testing,
        'creds': CREDS,
        'encrypted': True,
        'status': IntegrationStatus.DEPLOYED,
    }
    mock = mocker.patch.object(RegisterPartnerInBalanceAction, 'run_async')
    await CreateIntegrationAction(**action_kwargs).run()
    mock.assert_called_once()


@pytest.mark.asyncio
async def test_create_mock(user, storage, merchant, for_testing, mock_pay_client):
    action_kwargs = {
        'user': user,
        'merchant_id': merchant.merchant_id,
        'psp_external_id': IntegrationPSPExternalID.MOCK,
        'for_testing': for_testing,
        'creds': '',
        'encrypted': True,
        'status': IntegrationStatus.DEPLOYED,
    }
    integration_kwargs = {
        'integration_id': is_uuid(),
        'merchant_id': merchant.merchant_id,
        'psp_id': UUID('895cf90a-4530-4972-961e-4498e107f993'),
        'psp_external_id': 'mock',
        'status': IntegrationStatus.DEPLOYED,
        'revision': 1,
        'for_testing': for_testing,
        'created': is_datetime_with_tz(),
        'updated': is_datetime_with_tz(),
    }
    expected_integration = ensure_all_fields(Integration, **integration_kwargs)

    integration = await CreateIntegrationAction(**action_kwargs).run()

    assert_that(integration, equal_to(expected_integration))
    mock_pay_client.assert_awaited_once_with(
        integration=integration,
        creds='',
    )
