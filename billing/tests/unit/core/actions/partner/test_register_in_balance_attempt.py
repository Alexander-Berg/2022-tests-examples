import uuid

import pytest

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.balance.init_client import InitClientAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.partner.register_in_balance import (
    RegisterPartnerInBalanceAction,
)
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.partner.register_in_balance_attempt import (
    AttemptRegisterPartnerInBalanceAction,
)
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import IntegrationStatus
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.integration import Integration


@pytest.fixture(autouse=True)
def mock_init_client(mock_action):
    return mock_action(InitClientAction, 'balance-client-id')


@pytest.fixture
async def integration(storage, merchant, status, for_testing):
    return await storage.integration.create(
        Integration(
            status=status,
            for_testing=for_testing,
            merchant_id=merchant.merchant_id,
            psp_id=uuid.uuid4(),
            psp_external_id="payture",
        )
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('for_testing', (True, False))
@pytest.mark.parametrize('status', [s for s in IntegrationStatus])
async def test_register_in_balance(
    user, storage, partner, merchant, integration, mock_init_client, for_testing, status, mocker
):
    mock = mocker.patch.object(RegisterPartnerInBalanceAction, 'run_async')
    await AttemptRegisterPartnerInBalanceAction(uid=user.uid, partner_id=partner.partner_id).run()

    if not for_testing and status == IntegrationStatus.DEPLOYED:
        mock.assert_called_once()
