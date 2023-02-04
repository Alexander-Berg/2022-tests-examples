from decimal import Decimal
from typing import Any, Optional
from uuid import UUID, uuid4

from aiohttp.pytest_plugin import TestClient
from aioresponses import aioresponses

from billing.yandex_pay_plus.yandex_pay_plus.tests.lib.functional.builders.form.order_create import (
    OrderCreateRequestBuilder,
)
from billing.yandex_pay_plus.yandex_pay_plus.tests.lib.functional.builders.form.order_render import (
    OrderRenderRequestBuilder,
)
from billing.yandex_pay_plus.yandex_pay_plus.tests.lib.functional.builders.merchant_server.order_render import (
    OrderRenderResponseBuilder,
)
from billing.yandex_pay_plus.yandex_pay_plus.tests.lib.functional.mocks.merchant_server import MerchantServerMocker
from billing.yandex_pay_plus.yandex_pay_plus.tests.lib.functional.mocks.passport_addresses import (
    PassportAddressesMocker,
)
from billing.yandex_pay_plus.yandex_pay_plus.tests.lib.functional.mocks.yandex_delivery import YandexDeliveryMocker


class CheckoutOrderStepper:
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

    async def render_order(
        self,
        order_render_request: Optional[dict[str, Any]] = None,
        merchant_render_response: Optional[dict[str, Any]] = None
    ) -> dict[str, Any]:
        if order_render_request is None:
            order_render_request = OrderRenderRequestBuilder(self.merchant_id).build()
        if merchant_render_response is None:
            merchant_render_response = OrderRenderResponseBuilder().build()

        self._merchant_server.mock_order_render(merchant_render_response)
        return (
            await (
                await self.public_client.post(
                    '/api/public/v1/orders/render',
                    headers={'x-pay-session-id': 'sessid-123'},
                    json=order_render_request,
                    raise_for_status=True,
                )
            ).json()
        )['data']['order']

    async def create_order(
        self,
        order_create_request: dict[str, Any],
        merchant_create_order_id: str = '',
    ) -> dict[str, Any]:
        merchant_create_order_id = merchant_create_order_id or uuid4()

        self._merchant_server.mock_order_create(merchant_create_order_id)
        return (
            await (
                await self.public_client.post(
                    '/api/public/v1/orders/create',
                    headers={'x-pay-session-id': 'sessid-123'},
                    json=order_create_request,
                    raise_for_status=True,
                )
            ).json()
        )['data']['order']

    async def make_order_with_delivery(self) -> str:
        merchant_render_response = OrderRenderResponseBuilder().with_yandex_delivery()
        order_render_request = OrderRenderRequestBuilder(self.merchant_id).with_delivery('ship-a-id')
        self._yandex_delivery.mock_delivery_methods(express=True)
        self._yandex_delivery.mock_check_price(Decimal('10'))
        self._passport_addresses.mock_get_address('ship-a-id')
        render_order_response = await self.render_order(
            merchant_render_response=merchant_render_response.build(), order_render_request=order_render_request.build()
        )

        order_create_request = OrderCreateRequestBuilder(
            merchant_id=self.merchant_id, render_response=render_order_response
        ).with_yandex_delivery(
            shipping_address_id='ship-a-id',
            chosen_option=render_order_response['shipping']['yandex_delivery']['options'][0],
        ).build()
        self._passport_addresses.mock_get_address('ship-a-id')
        self._passport_addresses.mock_get_contact('ship-c-id')
        await self.create_order(order_create_request=order_create_request, merchant_create_order_id='merchant-order-id')

        return 'merchant-order-id'

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
