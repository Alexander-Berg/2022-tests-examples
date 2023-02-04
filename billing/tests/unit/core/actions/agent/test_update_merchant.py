from uuid import uuid4

import pytest

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.agent.update_merchant import UpdateAgentMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.pay_backend.put_merchant import PayBackendPutMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import MerchantNotFoundError


@pytest.mark.asyncio
async def test_update_merchant(mock_action, agent, partner, merchant):
    mock_pay_backend = mock_action(PayBackendPutMerchantAction, partner)
    name = 'merchant name'
    origins = ['https://127.0.0.1']
    url = 'example.com'
    updated = await UpdateAgentMerchantAction(
        agent_id=agent.agent_id,
        merchant_id=merchant.merchant_id,
        name=name,
        origins=origins,
        url=url,
        blocked=True,
    ).run()

    assert updated == {
        'agent_merchant_id': partner.agent_partner_id,
        'merchant_id': merchant.merchant_id,
        'blocked': True,
        'name': name,
        'inn': partner.registration_data.tax_ref_number,
        'origins': origins,
    }

    mock_pay_backend.assert_called_once_with(merchant=merchant, force_add_origin=True)


@pytest.mark.asyncio
async def test_update_merchant_not_found(mock_action, agent, partner, merchant):
    mock_action(PayBackendPutMerchantAction, partner)
    name = 'merchant name'
    origins = ['https://127.0.0.1']
    url = 'example.com'

    with pytest.raises(MerchantNotFoundError):
        await UpdateAgentMerchantAction(
            agent_id=uuid4(),
            merchant_id=merchant.merchant_id,
            name=name,
            origins=origins,
            url=url,
            blocked=True,
        ).run()

    with pytest.raises(MerchantNotFoundError):
        await UpdateAgentMerchantAction(
            agent_id=agent.agent_id,
            merchant_id=uuid4(),
            name=name,
            origins=origins,
            url=url,
            blocked=True,
        ).run()
