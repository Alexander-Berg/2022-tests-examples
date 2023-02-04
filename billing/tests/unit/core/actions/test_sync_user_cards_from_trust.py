import datetime
import uuid
from copy import copy
from unittest.mock import AsyncMock, Mock

import pytest

from sendr_utils import alist, utcnow

import hamcrest as h

from billing.yandex_pay.yandex_pay.core.actions.sync_user_cards_from_trust import (
    SyncUserCardsFromTrustAction, UpdateCardsFromBindingsAction
)
from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enums import BindingHandleStatus, TSPType
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.interactions import TrustPaysysClient
from billing.yandex_pay.yandex_pay.interactions.trust_paysys import TrustPaysysBinding
from billing.yandex_pay.yandex_pay.storage import CardMapper


class TestUpdateCardsFromBindingsActionCardNotUpdated:
    @pytest.fixture(name='card_being_updated')
    async def fixture_card_being_updated(self, storage):
        card = Card(
            trust_card_id=f'card-x{uuid.uuid4().hex}',
            payment_system='MasterCard',
            owner_uid=5555,
            issuer_bank='TCS BANK',
            tsp=TSPType.MASTERCARD,
            expire=datetime.datetime(year=2030, month=1, day=31, tzinfo=datetime.timezone.utc),
            last4='1234',
        )
        return await storage.card.create(card)

    @pytest.fixture
    def binding(self, card_being_updated: Card):
        return TrustPaysysBinding(
            id=card_being_updated.trust_card_id,
            holder='Jill Valentine',
            expiration_year=str(card_being_updated.expire.year),
            expiration_month=str(card_being_updated.expire.month),
            system=card_being_updated.payment_system,
            bank='TCS BANK',
            masked_number='123456****' + card_being_updated.last4,
            binding_ts=datetime.datetime.now(tz=datetime.timezone.utc).timestamp(),
            remove_ts=0,
            is_expired=False,
            is_verified=True,
            is_removed=False,
        )

    @pytest.mark.asyncio
    async def test_does_not_update_card(
        self,
        card_being_updated: Card,
        binding,
        storage,
    ):
        card_before_update: Card = await storage.card.get(card_being_updated.card_id)
        user = User(card_being_updated.owner_uid)

        binging_statuses = await UpdateCardsFromBindingsAction(
            user=user,
            cards=[card_before_update],
            bindings=[binding],
        ).run()

        card_after_update: Card = await storage.card.get(card_being_updated.card_id)

        assert binging_statuses[binding.id] == BindingHandleStatus.CARD_NOT_UPDATED
        assert card_before_update == card_after_update


class TestUpdateCardsFromBindingsActionCardUpdated:
    @pytest.fixture(name='card_being_updated')
    async def fixture_card_being_updated(self, storage):
        card = Card(
            trust_card_id=f'card-x{uuid.uuid4().hex}',
            payment_system='MasterCard',
            owner_uid=5555,
            issuer_bank='TCS BANK',
            tsp=TSPType.MASTERCARD,
            expire=datetime.datetime(year=2030, month=1, day=31, tzinfo=datetime.timezone.utc),
            last4='1234',
        )
        return await storage.card.create(card)

    @pytest.fixture(name='binding')
    def fixture_binding(self, card_being_updated: Card):
        return TrustPaysysBinding(
            id=card_being_updated.trust_card_id,
            holder='Jill Valentine',
            expiration_year=str(card_being_updated.expire.year + 1),
            expiration_month=str(card_being_updated.expire.month),
            system=card_being_updated.payment_system,
            bank='TCS BANK',
            masked_number='123456****' + card_being_updated.last4,
            binding_ts=datetime.datetime.now(tz=datetime.timezone.utc).timestamp(),
            remove_ts=0,
            is_expired=False,
            is_verified=True,
            is_removed=False,
        )

    @pytest.mark.asyncio
    async def test_updates_card(self, card_being_updated: Card, binding, storage):
        card_before_update: Card = await storage.card.get(card_being_updated.card_id)
        user = User(card_being_updated.owner_uid)

        binging_statuses: dict[str, BindingHandleStatus] = await UpdateCardsFromBindingsAction(
            user=user,
            cards=[await storage.card.get(card_being_updated.card_id)],
            bindings=[binding],
        ).run()

        card_after_update: Card = await storage.card.get(card_being_updated.card_id)

        assert binging_statuses[binding.id] == BindingHandleStatus.CARD_UPDATED

        assert card_before_update != card_after_update
        assert card_after_update.revision == card_before_update.revision + 1

        assert card_before_update.expire != card_after_update.expire

        assert card_after_update.expire == datetime.datetime(
            year=int(binding.expiration_year),
            month=int(binding.expiration_month),
            day=31,
            tzinfo=datetime.timezone.utc,
        )


class TestUpdateCardsFromBindingsActionCardCreated:
    @pytest.fixture(name='owner_uid')
    def fixture_owner_uid(self):
        return 2718281828

    @pytest.fixture
    def user(self, owner_uid):
        return User(owner_uid)

    @pytest.fixture(name='expected_card_fields')
    async def fixture_expected_card_fields(self, owner_uid):
        card_fields = dict(
            owner_uid=owner_uid,
            payment_system='MasterCard',
            trust_card_id=f'card-x{uuid.uuid4().hex}',
            issuer_bank='TCS BANK',
            is_removed=False,
            tsp=TSPType.MASTERCARD,
            expire=datetime.datetime(2030, 1, 31, 0, 0, 0, tzinfo=datetime.timezone.utc),
            last4='1234',
        )
        return card_fields

    @pytest.fixture(name='binding')
    def fixture_binding(self, expected_card_fields: dict):
        return TrustPaysysBinding(
            id=expected_card_fields['trust_card_id'],
            expiration_year=str(expected_card_fields['expire'].year),
            expiration_month=str(expected_card_fields['expire'].month),
            is_removed=expected_card_fields['is_removed'],
            system=expected_card_fields['payment_system'],
            bank=expected_card_fields['issuer_bank'],
            masked_number='123456****' + expected_card_fields['last4'],
            holder='Jill Valentine',
            binding_ts=datetime.datetime.now(tz=datetime.timezone.utc).timestamp(),
            remove_ts=0,
            is_expired=False,
            is_verified=True,
        )

    @pytest.mark.asyncio
    async def test_creates_card(self, owner_uid, user, expected_card_fields: dict, binding, storage):
        with pytest.raises(Card.DoesNotExist):
            await storage.card.get_by_trust_card_id_and_uid(
                owner_uid=owner_uid,
                trust_card_id=expected_card_fields['trust_card_id'],
            )

        assert binding.id == expected_card_fields['trust_card_id'], 'Sanity check'

        binging_statuses: dict[str, BindingHandleStatus] = await UpdateCardsFromBindingsAction(
            user=user,
            cards=[],
            bindings=[binding],
        ).run()

        card_created: Card = await storage.card.get_by_trust_card_id_and_uid(
            owner_uid=owner_uid,
            trust_card_id=expected_card_fields['trust_card_id'],
        )

        assert binging_statuses[binding.id] == BindingHandleStatus.CARD_CREATED

        h.assert_that(
            card_created,
            h.has_properties(**expected_card_fields),
        )

    @pytest.mark.asyncio
    async def test_do_not_create_removed_card(self, user, owner_uid, expected_card_fields: dict, binding, storage):
        binding.is_removed = True

        binging_statuses: dict[str, BindingHandleStatus] = await UpdateCardsFromBindingsAction(
            user=user, cards=[], bindings=[binding]
        ).run()

        assert binging_statuses[binding.id] == BindingHandleStatus.SKIPPED

        with pytest.raises(Card.DoesNotExist):
            await storage.card.get_by_trust_card_id_and_uid(
                owner_uid=owner_uid,
                trust_card_id=expected_card_fields['trust_card_id'],
            )

    @pytest.mark.asyncio
    async def test_do_not_create_expired_card(self, user, owner_uid, expected_card_fields, binding, storage):
        binding.is_expired = True

        binging_statuses: dict[str, BindingHandleStatus] = await UpdateCardsFromBindingsAction(
            user=user,
            cards=[],
            bindings=[binding],
        ).run()

        assert binging_statuses[binding.id] == BindingHandleStatus.SKIPPED

        with pytest.raises(Card.DoesNotExist):
            await storage.card.get_by_trust_card_id_and_uid(
                owner_uid=owner_uid,
                trust_card_id=expected_card_fields['trust_card_id'],
            )

    @pytest.mark.asyncio
    async def test_should_avoid_exceptions_on_duplicated_bindings(
        self,
        user,
        owner_uid,
        expected_card_fields,
        binding,
        storage,
    ):
        second_binding = copy(binding)
        second_binding.masked_number = '123456****0110'
        binging_statuses: dict[str, BindingHandleStatus] = await UpdateCardsFromBindingsAction(
            user=user,
            cards=[],
            bindings=[binding, second_binding],
        ).run()

        assert binging_statuses[binding.id] == BindingHandleStatus.CARD_CREATED
        cards = await alist(storage.card.find(
            owner_uid=owner_uid,
        ))
        assert len(cards) == 1
        assert cards[0].last4 == binding.last4


class TestSyncUserCardsFromTrustActionCalls:
    @pytest.fixture(name='card')
    def fixture_card(self):
        return Card(
            trust_card_id='1',
            owner_uid=1234,
            tsp=TSPType.MASTERCARD,
            expire=utcnow(),
            last4='6666',
            card_id=uuid.uuid4(),
        )

    @pytest.fixture
    def user(self, card):
        return User(card.owner_uid)

    @pytest.fixture(name='binding')
    def fixture_binding(self):
        return TrustPaysysBinding(
            id='1',
            holder='Anna-Varney Cantodea',
            expiration_year='2030',
            expiration_month='01',
            system='MasterCard',
            bank='Tiny Corpses Smurfing Bank',
            masked_number='666666****6666',
            binding_ts=1,
            is_expired=False,
            is_removed=False,
            remove_ts=0,
            is_verified=True,
        )

    @pytest.fixture(name='get_user_cards_mock')
    def fixture_get_user_cards_mock(self, mocker, binding) -> AsyncMock:
        return mocker.patch.object(
            TrustPaysysClient,
            'get_user_cards',
            mocker.AsyncMock(return_value=[binding]),
        )

    @pytest.fixture(name='find_cards_mock')
    def fixture_find_cards_mock(self, mocker, card) -> Mock:
        async def find_cards_result(*args, **kwargs):
            yield card

        return mocker.patch.object(
            CardMapper,
            'find',
            mocker.Mock(side_effect=find_cards_result),
        )

    @pytest.mark.asyncio
    async def test_sync_action_calls(
        self,
        get_user_cards_mock: AsyncMock,
        find_cards_mock: Mock,
        card: Card,
        binding: TrustPaysysBinding,
        user: User,
        mock_action,
    ):
        update_action_mock: Mock = mock_action(
            UpdateCardsFromBindingsAction,
            {binding.id: BindingHandleStatus.CARD_UPDATED},
        )

        sync_cards_action = SyncUserCardsFromTrustAction(user=user)

        result = await sync_cards_action.run()

        get_user_cards_mock.assert_called_once_with(uid=user.uid, show_hidden=True)
        find_cards_mock.assert_called_once_with(owner_uid=card.owner_uid)

        assert result == {binding.id: BindingHandleStatus.CARD_UPDATED}
        assert update_action_mock.called_once_with(user=user, bindings=[binding], cards=[card])
