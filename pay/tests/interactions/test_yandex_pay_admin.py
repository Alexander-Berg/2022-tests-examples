from datetime import datetime
from uuid import uuid4

import pytest
from aiohttp import TCPConnector

from hamcrest import assert_that, equal_to

from pay.lib.interactions.yandex_pay_admin.client import AbstractYandexPayAdminClient

YANDEX_PAY_ADMIN_API_URL = 'https://yandex-pay-admin.test'


class YandexPayAdminTestClient(AbstractYandexPayAdminClient):
    BASE_URL = YANDEX_PAY_ADMIN_API_URL
    DEBUG = False
    REQUEST_RETRY_TIMEOUTS = ()
    CONNECTOR = TCPConnector()


class TestTriggerEvents:
    @pytest.mark.asyncio
    async def test_first_transaction_success(self, yandex_pay_admin_client, mock_trigger):
        merchant_id = uuid4()
        partner_id = uuid4()
        event_time = datetime.now()

        await yandex_pay_admin_client.trigger_first_transaction(
            merchant_id=merchant_id,
            partner_id=partner_id,
            event_time=event_time,
        )

        assert_that(
            mock_trigger.call_args.kwargs['json'],
            equal_to(
                {
                    'event_time': event_time.isoformat(),
                    'data': {
                        'event_type': 'FIRST_TRANSACTION',
                        'merchant_id': str(merchant_id),
                        'partner_id': str(partner_id),
                    },
                }
            ),
        )

    @pytest.fixture
    def mock_trigger(self, aioresponses_mocker):
        return aioresponses_mocker.post(
            f'{YANDEX_PAY_ADMIN_API_URL}/internal/v1/events',
            payload={
                'status': 'success',
                'code': 200,
                'data': {},
            },
        )


@pytest.fixture
async def yandex_pay_admin_client(create_interaction_client) -> YandexPayAdminTestClient:
    client = create_interaction_client(YandexPayAdminTestClient)
    yield client
    await client.close()
