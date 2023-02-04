from datetime import datetime, timedelta, timezone
from uuid import UUID

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, contains_inanyorder, equal_to, has_properties, has_property

from billing.yandex_pay.yandex_pay.core.actions.card.combine import (
    BaseCombineUserCardsAction, CombinePayCardsAndTrustBindingsAction, CombinePayCardsAndTrustPaymentMethodsAction
)
from billing.yandex_pay.yandex_pay.core.entities.card import Card, ExpirationDate, UserCard
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import AuthMethod, CardNetwork, TSPTokenStatus, TSPType
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.interactions.trust_payments.entities import PartnerInfo, TrustPaymentMethod
from billing.yandex_pay.yandex_pay.interactions.trust_paysys.entities import TrustPaysysBinding
from billing.yandex_pay.yandex_pay.utils.normalize_banks import IssuerBank

USER = User(uid=55555)


def set_with(obj, path, value):
    """
    Присваивание по составному пути key1.key2.key3.
    Как в lodash/pydash. Но у нас слишком старый pydash.
    """
    path_items = path.split('.')
    for key in path_items[:-1]:
        obj = getattr(obj, key)
    setattr(obj, path_items[-1], value)


class TestBaseCombine:
    @pytest.fixture
    def pay_card(self):
        return Card(
            card_id=UUID('1360c44b-07c6-4fed-a16a-89d15e4c8870'),
            trust_card_id='trust-card-id',
            owner_uid=55555,
            tsp=TSPType.MASTERCARD,
            expire=utcnow() + timedelta(days=1),
            last4='4444',
            is_removed=False,
            issuer_bank='SBERBANK',
            enrollment=Enrollment(
                card_id=UUID('1360c44b-07c6-4fed-a16a-89d15e4c8870'),
                merchant_id=None,
                tsp_card_id='abc',
                tsp_token_id='def',
                tsp_token_status=TSPTokenStatus.ACTIVE,
                card_last4='1114',
            )
        )

    @pytest.fixture
    def trust_user_card(self, mocker):
        return UserCard(
            card_id='trust-card-id',
            owner_uid=55555,
            last4='4444',
            card_network=CardNetwork.JCB,
            bin='111000',
            allowed_auth_methods=[AuthMethod.PAN_ONLY],
            last_paid=utcnow(),
            created=utcnow(),
            trust_payment_method=mocker.Mock(),
            trust_binding=mocker.Mock(),
            issuer_bank=IssuerBank.TINKOFF,
            is_removed=False,
            is_expired=True,
            expiration_date=ExpirationDate(
                month=10,
                year=2010,
            ),
            trust_card_id='trust-card-id',
        )

    @pytest.fixture
    def combine_cls(self, trust_user_card):
        class Combine(BaseCombineUserCardsAction[None]):
            def _get_user_card_from_trust_card(self, trust_card: None) -> UserCard:
                return trust_user_card

        return Combine

    @pytest.mark.asyncio
    async def test_converts_pay_card_to_user_card(self, combine_cls, pay_card):
        [returned_card] = await combine_cls(
            user=USER, cards=[pay_card], trust_cards=[],
        ).run()

        assert_that(
            returned_card,
            equal_to(
                UserCard(
                    card_id='1360c44b-07c6-4fed-a16a-89d15e4c8870',
                    owner_uid=55555,
                    card_network=CardNetwork.MASTERCARD,
                    last4='4444',
                    bin=None,
                    issuer_bank=IssuerBank.SBERBANK,
                    created=datetime.fromtimestamp(0, tz=timezone.utc),
                    last_paid=datetime.fromtimestamp(0, tz=timezone.utc),
                    allowed_auth_methods=[AuthMethod.CLOUD_TOKEN],
                    is_removed=False,
                    is_expired=False,
                    card=pay_card,
                    trust_payment_method=None,
                    trust_binding=None,
                    expiration_date=ExpirationDate(
                        month=pay_card.expire.month,
                        year=pay_card.expire.year,
                    ),
                    trust_card_id='trust-card-id',
                )
            )
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('card_field, card_value', (
        pytest.param('tsp', TSPType.UNKNOWN, id='tsp-type-not-supported'),
        pytest.param('enrollment', None, id='no enrollment for given card'),
        pytest.param('enrollment.tsp_token_status', TSPTokenStatus.DELETED, id='token is not active'),
        pytest.param('enrollment.expire', utcnow() - timedelta(days=365), id='token expired'),
    ))
    async def test_when_method_should_not_be_allowed(self, combine_cls, pay_card, card_field, card_value):
        set_with(pay_card, card_field, card_value)

        [returned_card] = await combine_cls(
            user=USER, cards=[pay_card], trust_cards=[],
        ).run()

        assert_that(
            returned_card,
            has_property('allowed_auth_methods', [])
        )

    @pytest.mark.asyncio
    async def test_merge(self, mocker, combine_cls, pay_card, trust_user_card):
        [returned_card] = await combine_cls(
            user=USER, cards=[pay_card], trust_cards=[mocker.Mock(card_id='trust-card-id')],
        ).run()

        assert_that(
            returned_card,
            equal_to(
                UserCard(
                    card_id='1360c44b-07c6-4fed-a16a-89d15e4c8870',
                    owner_uid=55555,
                    last4=trust_user_card.last4,
                    bin=trust_user_card.bin,
                    card_network=CardNetwork.JCB,
                    issuer_bank=IssuerBank.SBERBANK,
                    created=trust_user_card.created,
                    last_paid=trust_user_card.last_paid,
                    allowed_auth_methods=[AuthMethod.CLOUD_TOKEN, AuthMethod.PAN_ONLY],
                    card=pay_card,
                    trust_payment_method=trust_user_card.trust_payment_method,
                    trust_binding=trust_user_card.trust_binding,
                    is_removed=False,
                    is_expired=False,
                    expiration_date=trust_user_card.expiration_date,
                    trust_card_id='trust-card-id',
                )
            )
        )

    @pytest.mark.asyncio
    async def test_merge_different_trust_card_id(self, mocker, combine_cls, pay_card, trust_user_card):
        trust_user_card.card_id = 'other-trust-card-id'

        returned = await combine_cls(
            user=USER, cards=[pay_card], trust_cards=[mocker.Mock(card_id='other-trust-card-id')],
        ).run()

        assert_that(
            returned,
            contains_inanyorder(
                has_property('card_id', '1360c44b-07c6-4fed-a16a-89d15e4c8870'),
                has_property('card_id', 'other-trust-card-id'),
            ),
        )

    @pytest.mark.asyncio
    async def test_merge_unknown_bank(self, mocker, combine_cls, pay_card, trust_user_card):
        pay_card.issuer_bank = 'unknown:)'

        [returned_card] = await combine_cls(
            user=USER, cards=[pay_card], trust_cards=[mocker.Mock(card_id='trust-card-id')],
        ).run()

        assert_that(
            returned_card,
            has_property('issuer_bank', IssuerBank.TINKOFF)
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('pay_card_removed, trust_user_card_removed, expected_merged_removed', (
        (False, False, False),
        (True, False, True),
        (False, True, True),
    ))
    async def test_merge_is_removed(
        self,
        mocker,
        combine_cls,
        pay_card,
        trust_user_card,
        pay_card_removed,
        trust_user_card_removed,
        expected_merged_removed,
    ):
        pay_card.is_removed = pay_card_removed
        trust_user_card.is_removed = trust_user_card_removed

        [returned_card] = await combine_cls(
            user=USER, cards=[pay_card], trust_cards=[mocker.Mock(card_id='trust-card-id')],
        ).run()

        assert_that(
            returned_card,
            has_property('is_removed', expected_merged_removed)
        )


class TestCombinePayCardsAndTrustPaymentMethods:
    @pytest.fixture
    def trust_payment_method(self):
        return TrustPaymentMethod(
            id='trust-card-id',
            card_id='trust-card-id',
            binding_systems=['trust'],
            orig_uid='55555',
            payment_method='card',
            system='mastercard',
            payment_system='mastercard',
            expiration_month='12',
            expiration_year='2000',
            card_bank='SBERBANK',
            expired=False,
            account='123456****7890',
            last_paid_ts=utcnow(),
            binding_ts=utcnow(),
        )

    @pytest.mark.asyncio
    async def test_converts_trust_payment_method_to_user_card(self, trust_payment_method):
        [returned_card] = await CombinePayCardsAndTrustPaymentMethodsAction(
            user=USER, trust_cards=[trust_payment_method], cards=[],
        ).run()

        assert_that(
            returned_card,
            equal_to(
                UserCard(
                    card_id='trust-card-id',
                    owner_uid=55555,
                    card_network=CardNetwork.MASTERCARD,
                    last4='7890',
                    bin='123456',
                    allowed_auth_methods=[AuthMethod.PAN_ONLY],
                    issuer_bank=IssuerBank.SBERBANK,
                    last_paid=trust_payment_method.last_paid_ts,
                    created=trust_payment_method.binding_ts,
                    is_removed=False,
                    is_expired=False,
                    trust_payment_method=trust_payment_method,
                    trust_binding=None,
                    card=None,
                    expiration_date=ExpirationDate(
                        month=12,
                        year=2000,
                    ),
                    trust_card_id='trust-card-id',
                )
            )
        )

    @pytest.mark.asyncio
    async def test_when_trust_method_is_expired(self, trust_payment_method):
        trust_payment_method.expired = True

        [returned_card] = await CombinePayCardsAndTrustPaymentMethodsAction(
            user=USER, trust_cards=[trust_payment_method], cards=[],
        ).run()

        assert_that(
            returned_card,
            has_properties(
                allowed_auth_methods=[],
                is_expired=True,
            )
        )

    @pytest.mark.asyncio
    async def test_yabank_card(self, trust_payment_method):
        trust_payment_method.partner_info = PartnerInfo(is_yabank_card=True)

        [returned_card] = await CombinePayCardsAndTrustPaymentMethodsAction(
            user=USER, trust_cards=[trust_payment_method], cards=[],
        ).run()

        assert_that(
            returned_card,
            has_properties(
                allowed_auth_methods=[],
            )
        )

    @pytest.mark.asyncio
    async def test_when_bank_is_unknown__calls_fallback(self, mocker, trust_payment_method):
        mock = mocker.patch('billing.yandex_pay.yandex_pay.core.actions.card.combine.fallback_card_issuer')
        trust_payment_method.card_bank = 'unknown'

        await CombinePayCardsAndTrustPaymentMethodsAction(
            user=USER, trust_cards=[trust_payment_method], cards=[],
        ).run()

        mock.assert_called_once_with(last4='7890')

    @pytest.mark.asyncio
    async def test_when_bank_is_unknown__returns_fallback(self, mocker, trust_payment_method):
        mocker.patch(
            'billing.yandex_pay.yandex_pay.core.actions.card.combine.fallback_card_issuer',
            mocker.Mock(return_value=IssuerBank.TINKOFF)
        )
        trust_payment_method.card_bank = 'unknown'

        [returned_card] = await CombinePayCardsAndTrustPaymentMethodsAction(
            user=USER, trust_cards=[trust_payment_method], cards=[],
        ).run()

        assert_that(returned_card.issuer_bank, equal_to(IssuerBank.TINKOFF))


class TestCombinePayCardsAndTrustBindingsMethods:
    @pytest.fixture
    def now(self):
        return utcnow()

    @pytest.fixture
    def trust_binding(self, now):
        return TrustPaysysBinding(
            id='trust-card-id',
            holder='Card Holder',
            expiration_month='12',
            expiration_year='2000',
            system='mastercard',
            bank='SBERBANK',
            masked_number='123456****7890',
            binding_ts=now.timestamp(),
            remove_ts=now.timestamp(),
            is_expired=False,
            is_removed=False,
            is_verified=True,
        )

    @pytest.mark.asyncio
    async def test_converts_trust_binding_to_user_card(self, trust_binding, now):
        [returned_card] = await CombinePayCardsAndTrustBindingsAction(
            user=USER, trust_cards=[trust_binding], cards=[],
        ).run()

        assert_that(
            returned_card,
            equal_to(
                UserCard(
                    card_id='trust-card-id',
                    owner_uid=55555,
                    card_network=CardNetwork.MASTERCARD,
                    last4='7890',
                    bin='123456',
                    allowed_auth_methods=[AuthMethod.PAN_ONLY],
                    issuer_bank=IssuerBank.SBERBANK,
                    last_paid=datetime.fromtimestamp(0, tz=timezone.utc),
                    created=now,
                    is_removed=False,
                    is_expired=False,
                    trust_payment_method=None,
                    trust_binding=trust_binding,
                    card=None,
                    expiration_date=ExpirationDate(
                        month=12,
                        year=2000,
                    ),
                    trust_card_id='trust-card-id',
                )
            )
        )

    @pytest.mark.asyncio
    async def test_when_trust_binding_is_expired(self, trust_binding):
        trust_binding.is_expired = True

        [returned_card] = await CombinePayCardsAndTrustBindingsAction(
            user=USER, trust_cards=[trust_binding], cards=[],
        ).run()

        assert_that(
            returned_card,
            has_properties(
                allowed_auth_methods=[],
                is_expired=True,
            )
        )

    @pytest.mark.asyncio
    async def test_when_bank_is_unknown__calls_fallback(self, mocker, trust_binding):
        mock = mocker.patch('billing.yandex_pay.yandex_pay.core.actions.card.combine.fallback_card_issuer')
        trust_binding.bank = 'unknown'

        await CombinePayCardsAndTrustBindingsAction(
            user=USER, trust_cards=[trust_binding], cards=[],
        ).run()

        mock.assert_called_once_with(last4='7890')

    @pytest.mark.asyncio
    async def test_when_bank_is_unknown__returns_fallback(self, mocker, trust_binding):
        mocker.patch(
            'billing.yandex_pay.yandex_pay.core.actions.card.combine.fallback_card_issuer',
            mocker.Mock(return_value=IssuerBank.TINKOFF)
        )
        trust_binding.bank = 'unknown'

        [returned_card] = await CombinePayCardsAndTrustBindingsAction(
            user=USER, trust_cards=[trust_binding], cards=[],
        ).run()

        assert_that(returned_card.issuer_bank, equal_to(IssuerBank.TINKOFF))
