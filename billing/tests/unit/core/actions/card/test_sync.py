import pytest

from sendr_pytest.matchers import convert_then_match

from hamcrest import assert_that, equal_to, has_property, match_equality

from billing.yandex_pay.yandex_pay.core.actions.card.combine import CombinePayCardsAndTrustBindingsAction
from billing.yandex_pay.yandex_pay.core.actions.card.get import GetUserCardByCardIdAction
from billing.yandex_pay.yandex_pay.core.actions.card.sync import SyncUserCardAction
from billing.yandex_pay.yandex_pay.core.actions.sync_user_cards_from_trust import SyncUserCardsFromTrustAction
from billing.yandex_pay.yandex_pay.core.actions.trust.paysys import TrustPaysysGetBindingAction
from billing.yandex_pay.yandex_pay.core.entities.user import User

USER = User(uid=55555)


@pytest.mark.asyncio
async def test_returned(mock_get_user_card_by_card_id):
    returned = await SyncUserCardAction(user=USER, card_id='card-id').run()
    assert_that(
        returned,
        equal_to(mock_get_user_card_by_card_id())
    )


@pytest.mark.asyncio
async def test_calls_sync_user_cards_from_trust(mock_sync_cards_from_trust):
    await SyncUserCardAction(user=USER, card_id='card-id').run()

    mock_sync_cards_from_trust.assert_called_once_with(user=USER)


@pytest.mark.asyncio
async def test_calls_get_user_card_by_card_id(mock_get_user_card_by_card_id):
    await SyncUserCardAction(user=USER, card_id='card-id').run()

    mock_get_user_card_by_card_id.assert_called_once_with(
        user=USER,
        card_id='card-id',
        resolve_trust_card=match_equality(
            convert_then_match(
                lambda partial_func: partial_func.func,
                has_property('action_class', equal_to(TrustPaysysGetBindingAction))
            ),
        ),
        combine_cards=match_equality(has_property('action_class', equal_to(CombinePayCardsAndTrustBindingsAction))),
        raise_on_inactive=False,
        require_pay_card=True,
    )


@pytest.fixture(autouse=True)
def mock_sync_cards_from_trust(mock_action):
    return mock_action(SyncUserCardsFromTrustAction)


@pytest.fixture(autouse=True)
def mock_get_user_card_by_card_id(mock_action):
    return mock_action(GetUserCardByCardIdAction)
