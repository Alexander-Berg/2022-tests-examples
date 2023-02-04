import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.actions.trust.paysys import TrustPaysysGetBindingAction
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.core.exceptions import CoreCardNotFoundError
from billing.yandex_pay.yandex_pay.interactions.trust_paysys import TrustPaysysClient


class TestTrustPaysysGetBindingAction:
    USER = User(uid=55555)

    @pytest.mark.asyncio
    async def test_returned(self, mocker):
        card_mock = mocker.Mock(card_id='trust-card-id')
        other_card_mock = mocker.Mock(card_id='other-trust-card-id')
        mocker.patch.object(
            TrustPaysysClient, 'get_user_cards', mocker.AsyncMock(return_value=[other_card_mock, card_mock])
        )

        returned = await TrustPaysysGetBindingAction(
            user=self.USER,
            trust_card_id='trust-card-id',
            show_hidden=True,
        ).run()
        assert_that(
            returned,
            equal_to(card_mock)
        )

    @pytest.mark.asyncio
    async def test_raises_when_card_not_found(self, mocker):
        mocker.patch.object(
            TrustPaysysClient, 'get_user_cards', mocker.AsyncMock(return_value=[mocker.Mock(card_id='other-card-id')])
        )

        with pytest.raises(CoreCardNotFoundError):
            await TrustPaysysGetBindingAction(user=self.USER, trust_card_id='trust-card-id', show_hidden=True).run()

    @pytest.mark.asyncio
    async def test_calls_paysys_client(self, mocker):
        card_mock = mocker.Mock(card_id='trust-card-id')
        show_hidden = object()
        paysys_mock = mocker.patch.object(
            TrustPaysysClient, 'get_user_cards', mocker.AsyncMock(return_value=[card_mock])
        )

        await TrustPaysysGetBindingAction(user=self.USER, trust_card_id='trust-card-id', show_hidden=show_hidden).run()

        paysys_mock.assert_awaited_once_with(uid=self.USER.uid, show_hidden=show_hidden)
