import pytest

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.agent.register_merchant import RegisterMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.merchant.create import CreateMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.partner.create import CreatePartnerByAgentAction
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin import OriginBackbone
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.partner import RegistrationData


@pytest.mark.asyncio
async def test_register_merchant(mock_action, agent, partner, merchant, mocker):
    mock_create_partner = mock_action(CreatePartnerByAgentAction, partner)
    mock_create_merchant = mock_action(CreateMerchantAction, merchant)
    agent_merchant_id = 'some_agent_merchant_id'
    inn = '123456'
    name = 'merchant name'
    origins = ['https://127.0.0.1']
    url = 'example.com'
    merchant_id = await RegisterMerchantAction(
        agent_id=agent.agent_id,
        agent_merchant_id=agent_merchant_id,
        inn=inn,
        name=name,
        origins=origins,
        url=url,
    ).run()

    mock_create_partner.assert_called_once_with(
        agent_id=agent.agent_id,
        agent_partner_id=agent_merchant_id,
        name=name,
        registration_data=RegistrationData(
            tax_ref_number=inn,
        ),
    )

    mock_create_merchant.assert_called_once_with(
        partner_id=partner.partner_id,
        name=name,
        origins=[OriginBackbone(origin=origin) for origin in origins],
        propagate=True,
        url=url,
        post_moderation=False,
        force_add_origin=True,
    )

    assert merchant_id == merchant.merchant_id
