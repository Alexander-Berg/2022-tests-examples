from uuid import uuid4

import pytest

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.agent.get_merchant import GetAgentMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import MerchantNotFoundError


@pytest.mark.asyncio
async def test_get_merchant_success(partner, merchant, agent):
    data = await GetAgentMerchantAction(merchant_id=merchant.merchant_id, agent_id=agent.agent_id).run()

    assert data == {
        'inn': partner.registration_data.tax_ref_number,
        'agent_merchant_id': partner.agent_partner_id,
        'merchant_id': merchant.merchant_id,
        'name': partner.name,
        'origins': [],
        'blocked': partner.blocked,
    }


@pytest.mark.asyncio
async def test_get_merchant_different_agent(merchant):
    with pytest.raises(MerchantNotFoundError):
        await GetAgentMerchantAction(merchant_id=merchant.merchant_id, agent_id=uuid4()).run()


@pytest.mark.asyncio
async def test_get_merchant_merchant_is_absent(merchant, agent):
    with pytest.raises(MerchantNotFoundError):
        await GetAgentMerchantAction(merchant_id=uuid4(), agent_id=agent.agent_id).run()
