from typing import Any
from uuid import UUID

from aiohttp.pytest_plugin import TestClient
from aioresponses import aioresponses

from billing.yandex_pay_plus.yandex_pay_plus.tests.lib.functional.mocks.merchant_server import MerchantServerMocker
from billing.yandex_pay_plus.yandex_pay_plus.tests.lib.functional.mocks.passport_addresses import (
    PassportAddressesMocker,
)
from billing.yandex_pay_plus.yandex_pay_plus.tests.lib.functional.mocks.yandex_delivery import YandexDeliveryMocker


class CheckoutOrderDeliveryStepper:
    def __init__(
        self,
        merchant_id: UUID,
        merchant_callback_url: str,
        public_client: TestClient,
        aioresponses_mocker: aioresponses,
        yandex_pay_plus_settings,
    ):
        self.merchant_id = merchant_id
        self.merchant_callback_url = merchant_callback_url
        self.public_client = public_client
        self.aioresponses_mocker = aioresponses_mocker
        self.yandex_pay_plus_settings = yandex_pay_plus_settings

    async def create_delivery(
        self,
        order_id: str,
    ) -> dict[str, Any]:
        self._yandex_delivery.mock_create_claim('the-claim-id')
        return (
            await (
                await self.public_client.post(
                    f'/api/merchant/v1/orders/{order_id}/delivery/create',
                    raise_for_status=True,
                )
            ).json()
        )

    @property
    def _merchant_server(self):
        return MerchantServerMocker(self.aioresponses_mocker, base_url=self.merchant_callback_url)

    @property
    def _yandex_delivery(self):
        return YandexDeliveryMocker(
            self.aioresponses_mocker, self.yandex_pay_plus_settings.YANDEX_DELIVERY_API_URL
        )

    @property
    def _passport_addresses(self):
        return PassportAddressesMocker(
            self.aioresponses_mocker, self.yandex_pay_plus_settings.PASSPORT_ADDRESSES_URL
        )
