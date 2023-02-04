import uuid

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.agent.authenticate import AuthenticateAgentAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.agent.register_merchant import RegisterMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.agent.update_merchant import UpdateAgentMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.merchant import Merchant


@pytest.fixture
def utc_now():
    return utcnow()


@pytest.fixture
def merchant_entity(partner, role, utc_now):
    merchant_id = uuid.uuid4()
    return Merchant(
        merchant_id=merchant_id,
        partner_id=partner.partner_id,
        name='merchant_name',
        created=utc_now,
        updated=utc_now,
    )


@pytest.fixture
def url(merchant):
    return f'/api/agent/v1/merchants/{merchant.merchant_id}'


class TestUpdateMerchant:
    @pytest.mark.asyncio
    async def test_put(self, url, app, mock_action, agent, merchant, partner):
        mock_action(AuthenticateAgentAction, agent.agent_id)
        mock_action(
            UpdateAgentMerchantAction,
            {
                'merchant_id': str(merchant.merchant_id),
                'agent_merchant_id': partner.agent_partner_id,
                'name': partner.name,
                'blocked': partner.blocked,
                'inn': partner.registration_data.tax_ref_number,
                'origins': ['https://example.com:443'],
            },
        )

        r = await app.put(url, json={'origins': ['https://example.com']})

        data = await r.json()

        assert_that(r.status, equal_to(200))
        assert_that(
            data,
            equal_to(
                {
                    'status': 'success',
                    'code': 200,
                    'data': {
                        'merchant_id': str(merchant.merchant_id),
                        'agent_merchant_id': partner.agent_partner_id,
                        'name': partner.name,
                        'blocked': partner.blocked,
                        'inn': partner.registration_data.tax_ref_number,
                        'origins': ['https://example.com:443'],
                    },
                }
            ),
        )


class TestCreateMerchant:
    @pytest.mark.asyncio
    async def test_post(self, app, mock_action, agent):
        merchant_id = '10d0bc22-527d-41cf-9e5e-5735eea0e12d'
        mock_action(AuthenticateAgentAction, agent.agent_id)
        mock_action(RegisterMerchantAction, uuid.UUID(merchant_id))
        r = await app.post(
            '/api/agent/v1/merchants',
            json={
                'inn': '12345678901',
                'agent_merchant_id': '123456',
                'name': 'merchant',
                'origins': ['https://example.com:443'],
                'url': 'example.com',
            },
        )

        assert_that(r.status, equal_to(200))

        data = await r.json()

        assert_that(
            data,
            equal_to(
                {
                    'status': 'success',
                    'code': 200,
                    'data': {'merchant_id': merchant_id},
                }
            ),
        )


class TestGetMerchant:
    @pytest.mark.asyncio
    async def test_get(self, url, app, mock_action, merchant, partner, agent):
        mock_action(AuthenticateAgentAction, agent.agent_id)

        r = await app.get(url)

        data = await r.json()

        assert_that(r.status, equal_to(200))
        assert_that(
            data,
            equal_to(
                {
                    'status': 'success',
                    'code': 200,
                    'data': {
                        'inn': partner.registration_data.tax_ref_number,
                        'merchant_id': str(merchant.merchant_id),
                        'agent_merchant_id': partner.agent_partner_id,
                        'blocked': partner.blocked,
                        'name': partner.name,
                        'origins': [],
                    },
                }
            ),
        )
