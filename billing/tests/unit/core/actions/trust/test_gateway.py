import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.actions.trust.gateway import TrustGatewayGetPaymentMethodAction
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.core.exceptions import CoreCardNotFoundError
from billing.yandex_pay.yandex_pay.interactions.trust_payments import TrustPaymentsClient


class TestTrustGatewayGetPaymentMethodAction:
    USER = User(uid=55555)

    @pytest.mark.asyncio
    async def test_returned(self, mocker):
        card_mock = mocker.Mock()
        mocker.patch.object(TrustPaymentsClient, 'get_payment_method', mocker.AsyncMock(return_value=card_mock))

        returned = await TrustGatewayGetPaymentMethodAction(user=self.USER, trust_card_id='trust-card-id').run()
        assert_that(
            returned,
            equal_to(card_mock)
        )

    @pytest.mark.asyncio
    async def test_raises_when_card_not_found(self, mocker):
        mocker.patch.object(TrustPaymentsClient, 'get_payment_method', mocker.AsyncMock(return_value=None))

        with pytest.raises(CoreCardNotFoundError):
            await TrustGatewayGetPaymentMethodAction(user=self.USER, trust_card_id='trust-card-id').run()

    @pytest.mark.asyncio
    async def test_calls_gateway_client(self, mocker):
        gateway_mock = mocker.patch.object(TrustPaymentsClient, 'get_payment_method', mocker.AsyncMock())

        await TrustGatewayGetPaymentMethodAction(user=self.USER, trust_card_id='trust-card-id').run()

        gateway_mock.assert_awaited_once_with(uid=self.USER.uid, trust_api_card_id='trust-card-id')
