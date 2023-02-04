import uuid
from datetime import timedelta

import pytest

from sendr_pytest.matchers import convert_then_match
from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, match_equality

from billing.yandex_pay.yandex_pay.core.actions.card.combine import CombinePayCardsAndTrustPaymentMethodsAction
from billing.yandex_pay.yandex_pay.core.actions.card.get import (
    GetUserCardByCardIdAction, GetUserCardByPayCardIdAction, GetUserCardByTrustCardIdAction
)
from billing.yandex_pay.yandex_pay.core.actions.trust.gateway import TrustGatewayGetPaymentMethodAction
from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enums import TSPType
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.core.exceptions import CoreCardNotFoundError


@pytest.fixture
def owner_uid():
    return 5555


@pytest.fixture
def user(owner_uid):
    return User(owner_uid)


@pytest.fixture
def trust_id():
    return 'card-x1a1234567a12abcd12345a1a'


@pytest.fixture
def card_expiration_date():
    return utcnow() + timedelta(days=365)


@pytest.fixture
async def card(storage, owner_uid, trust_id, card_expiration_date):
    return await storage.card.create(
        Card(
            trust_card_id=trust_id,
            owner_uid=owner_uid,
            tsp=TSPType.MASTERCARD,
            expire=card_expiration_date,
            last4='0000',
        )
    )


class TestGetUserCardByTrustCardId:
    @pytest.fixture
    def combined_cards(self, mocker):
        return [mocker.Mock(), mocker.Mock()]

    @pytest.fixture
    def trust_card(self, mocker):
        return mocker.Mock()

    @pytest.fixture(autouse=True)
    def combiner(self, mocker, combined_cards):
        return mocker.AsyncMock(return_value=combined_cards)

    @pytest.fixture(autouse=True)
    def trust_resolver(self, mocker, trust_card):
        return mocker.AsyncMock(return_value=trust_card)

    @pytest.fixture
    def run_action(self, combiner, trust_resolver, user, card):
        async def _run_action(**kwargs):
            kwargs.setdefault('user', user)
            kwargs.setdefault('combine_cards', combiner)
            kwargs.setdefault('resolve_trust_card', trust_resolver)
            kwargs.setdefault('trust_card_id', card.trust_card_id)
            return await GetUserCardByTrustCardIdAction(**kwargs).run()
        return _run_action

    @pytest.mark.asyncio
    async def test_returned(self, run_action, combined_cards):
        card = await run_action(require_pay_card=True)

        assert_that(card, equal_to(combined_cards[0]))

    @pytest.mark.asyncio
    async def test_calls_combine(self, user, card, combiner, trust_card, run_action):
        await run_action(require_pay_card=True)

        combiner.assert_awaited_once_with(user=user, cards=[card], trust_cards=[trust_card])

    @pytest.mark.asyncio
    async def test_card_with_unknown_trust_card_id__should_raise_not_found(self, run_action, mocker):
        trust_resolver = mocker.AsyncMock(side_effect=CoreCardNotFoundError)
        with pytest.raises(CoreCardNotFoundError):
            await run_action(resolve_trust_card=trust_resolver)

    @pytest.mark.asyncio
    async def test_pay_card_not_found_and_required__raises(self, run_action):
        with pytest.raises(CoreCardNotFoundError):
            await run_action(trust_card_id='card-x9999', require_pay_card=True)

    @pytest.mark.asyncio
    async def test_pay_card_not_found_and_not_required__calls_combiner_without_pay_card(
        self, run_action, user, combiner, trust_card,
    ):
        await run_action(trust_card_id='card-x9999', require_pay_card=False)

        combiner.assert_awaited_once_with(user=user, cards=[], trust_cards=[trust_card])

    @pytest.mark.asyncio
    async def test_defaults(self, user, mocker, mock_action):
        trust_id = 'card-x9999'
        mock_combiner = mock_action(CombinePayCardsAndTrustPaymentMethodsAction, mocker.MagicMock())
        mock_trust_resolver = mock_action(TrustGatewayGetPaymentMethodAction, mocker.MagicMock())

        await GetUserCardByTrustCardIdAction(user=user, trust_card_id=trust_id).run()

        mock_trust_resolver.assert_called_once()
        mock_combiner.assert_called_once()


class TestGetUserCardByCardIdAction:
    @pytest.fixture
    def combiner(self, mocker):
        return mocker.AsyncMock()

    @pytest.fixture
    def trust_resolver(self, mocker):
        return mocker.AsyncMock()

    @pytest.fixture
    def run_action(self, combiner, trust_resolver, user):
        async def _run_action(**kwargs):
            kwargs.setdefault('user', user)
            kwargs.setdefault('combine_cards', combiner)
            kwargs.setdefault('resolve_trust_card', trust_resolver)
            return await GetUserCardByCardIdAction(**kwargs).run()
        return _run_action

    @pytest.mark.asyncio
    async def test_when_trust_id__returns_combined_card(self, user, run_action, trust_id, trust_resolver, combiner):
        returned = await run_action(card_id=trust_id)

        assert_that(
            returned,
            equal_to(
                (await combiner())[0]
            )
        )

    @pytest.mark.asyncio
    async def test_when_trust_id__calls_resolver(self, user, run_action, trust_id, trust_resolver):
        await run_action(card_id=trust_id)

        trust_resolver.assert_awaited_once_with(user, trust_id)

    @pytest.mark.asyncio
    async def test_when_trust_id__calls_combiner(self, user, run_action, trust_id, trust_resolver, combiner):
        await run_action(card_id=trust_id)

        combiner.assert_awaited_once_with(
            user=user,
            trust_cards=[await trust_resolver()],
            cards=[],
        )

    @pytest.mark.asyncio
    async def test_when_trust_id__defaults(self, user, mocker, mock_action, trust_id):
        mock_combiner = mock_action(CombinePayCardsAndTrustPaymentMethodsAction, mocker.MagicMock())
        mock_trust_resolver = mock_action(TrustGatewayGetPaymentMethodAction, mocker.MagicMock())

        await GetUserCardByCardIdAction(user=user, card_id=trust_id).run()

        mock_trust_resolver.assert_called_once()
        mock_combiner.assert_called_once()

    @pytest.mark.asyncio
    async def test_when_pay_id__returned(
        self, mocker, mock_action, user, run_action, trust_resolver, combiner
    ):
        mocked_subaction_card = mocker.Mock()
        mock_action(GetUserCardByPayCardIdAction, mocked_subaction_card)

        returned = await run_action(card_id=str(uuid.uuid4()))

        assert_that(
            returned,
            equal_to(mocked_subaction_card)
        )

    @pytest.mark.asyncio
    async def test_when_pay_id__calls_get_by_pay_id(
        self, mocker, mock_action, user, run_action, card, trust_resolver, combiner
    ):
        raise_on_inactive = mocker.NonCallableMock()
        raw_pay_card_id = str(uuid.uuid4())
        mock = mock_action(GetUserCardByPayCardIdAction)

        await run_action(card_id=raw_pay_card_id, raise_on_inactive=raise_on_inactive)

        mock.assert_called_once_with(
            user=user,
            card_id=raw_pay_card_id,
            resolve_trust_card=trust_resolver,
            combine_cards=combiner,
            raise_on_inactive=raise_on_inactive,
            skip_trust_if_possible=True,
        )

    @pytest.mark.asyncio
    async def test_when_pay_id__defaults(
        self, mocker, mock_action, user, run_action, card, trust_resolver, combiner
    ):
        raw_pay_card_id = str(uuid.uuid4())
        mock = mock_action(GetUserCardByPayCardIdAction)

        await GetUserCardByCardIdAction(user=user, card_id=raw_pay_card_id).run()

        mock.assert_called_once_with(
            user=user,
            card_id=raw_pay_card_id,
            resolve_trust_card=match_equality(
                convert_then_match(
                    lambda run_action: run_action.action_class,
                    equal_to(TrustGatewayGetPaymentMethodAction),
                )
            ),
            combine_cards=match_equality(
                convert_then_match(
                    lambda run_action: run_action.action_class,
                    equal_to(CombinePayCardsAndTrustPaymentMethodsAction),
                )
            ),
            raise_on_inactive=True,
            skip_trust_if_possible=True,
        )


class TestGetUserCardByPayCardIdAction:
    @pytest.fixture
    def trust_card(self, mocker):
        return mocker.Mock()

    @pytest.fixture
    def combiner(self, mocker, card):
        return mocker.AsyncMock(return_value=[mocker.Mock(card=card)])

    @pytest.fixture
    def trust_resolver(self, mocker, trust_card):
        return mocker.AsyncMock(return_value=trust_card)

    @pytest.fixture
    def user_card_with_inactive_pay_card(self, mocker):
        user_card = mocker.Mock()
        user_card.card = mocker.Mock()
        user_card.card.is_inactive = mocker.Mock(return_value=True)
        return user_card

    @pytest.fixture
    def run_action(self, combiner, trust_resolver, user, card):
        async def _run_action(**kwargs):
            kwargs.setdefault('user', user)
            kwargs.setdefault('combine_cards', combiner)
            kwargs.setdefault('resolve_trust_card', trust_resolver)
            kwargs.setdefault('card_id', card.card_id)
            return await GetUserCardByPayCardIdAction(**kwargs).run()
        return _run_action

    @pytest.mark.asyncio
    async def test_returned(self, run_action, combiner):
        returned = await run_action()

        assert_that(
            returned,
            equal_to((await combiner())[0])
        )

    @pytest.mark.asyncio
    async def test_calls_resolver(self, run_action, user, card, trust_resolver):
        await run_action()

        trust_resolver.assert_awaited_once_with(user, card.trust_card_id)

    @pytest.mark.asyncio
    async def test_calls_combiner(self, run_action, user, card, combiner, trust_card):
        await run_action()

        combiner.assert_awaited_once_with(user=user, cards=[card], trust_cards=[trust_card])

    @pytest.mark.asyncio
    async def test_when_card_id_is_not_uuid__raises_not_found(self, run_action):
        with pytest.raises(CoreCardNotFoundError):
            await run_action(card_id='not-an-uuid')

    @pytest.mark.asyncio
    async def test_when_card_does_not_exist__raises_not_found(self, run_action):
        with pytest.raises(CoreCardNotFoundError):
            await run_action(card_id=uuid.uuid4())

    @pytest.mark.asyncio
    async def test_when_trust_card_does_not_exist__passes_empty_trust_card_to_combiner(
        self, mocker, run_action, combiner, card, user,
    ):
        trust_resolver = mocker.AsyncMock(side_effect=CoreCardNotFoundError)
        await run_action(resolve_trust_card=trust_resolver)

        combiner.assert_awaited_once_with(user=user, cards=[card], trust_cards=[])

    @pytest.mark.asyncio
    async def test_when_pay_card_is_inactive__raises_not_found_error(
        self, mocker, run_action, combiner, user_card_with_inactive_pay_card
    ):
        combiner = mocker.AsyncMock(return_value=[user_card_with_inactive_pay_card])

        with pytest.raises(CoreCardNotFoundError):
            await run_action(combine_cards=combiner, raise_on_inactive=True)

    @pytest.mark.asyncio
    async def test_when_pay_card_is_inactive_and_raise_is_false__returns_user_card(
        self, mocker, run_action, combiner, user_card_with_inactive_pay_card
    ):
        combiner = mocker.AsyncMock(return_value=[user_card_with_inactive_pay_card])

        returned = await run_action(combine_cards=combiner, raise_on_inactive=False)

        assert_that(returned, equal_to(user_card_with_inactive_pay_card))

    @pytest.mark.asyncio
    async def test_when_pay_card_and_not_trust__do_not_resolve_trust(
        self, trust_resolver, run_action, user, card, combiner, trust_card
    ):
        await run_action(skip_trust_if_possible=False)

        combiner.assert_awaited_once_with(user=user, cards=[card], trust_cards=[])
        trust_resolver.assert_not_awaited()

    @pytest.mark.asyncio
    async def test_when_pay_card_and_not_trust__returned_card(
        self, trust_resolver, run_action, user, card, combiner, trust_card
    ):
        returned = await run_action(skip_trust_if_possible=False)

        assert_that(
            returned,
            equal_to((await combiner())[0])
        )
