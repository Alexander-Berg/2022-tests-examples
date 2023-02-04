from dataclasses import replace
from datetime import datetime, timedelta, timezone

import pytest
import yenv

from sendr_utils import utcnow

from hamcrest import assert_that, contains, contains_inanyorder, equal_to, has_length, has_property

from billing.yandex_pay.yandex_pay.core.actions.card.list import GetUserCardsAction
from billing.yandex_pay.yandex_pay.core.entities.card import Card, ExpirationDate, UserCard
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import AuthMethod, CardNetwork, TSPTokenStatus, TSPType
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.interactions import TrustPaymentsClient
from billing.yandex_pay.yandex_pay.interactions.trust_payments.entities import TrustPaymentMethod
from billing.yandex_pay.yandex_pay.utils.normalize_banks import IssuerBank
from billing.yandex_pay.yandex_pay.utils.stats import unknown_trust_payment_system


@pytest.fixture
async def merchant(storage, merchant_entity):
    return await storage.merchant.create(merchant_entity)


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
def payment_method_data(owner_uid, trust_id):
    return TrustPaymentMethod(
        id=trust_id,
        card_id=trust_id,
        binding_systems=['trust'],
        orig_uid=str(owner_uid),
        payment_method='card',
        system='MasterCard',
        payment_system='MasterCard',
        expiration_month='01',
        expiration_year='2030',
        card_bank='SBERBANK OF RUSSIA',
        expired=False,
        account='123456****7890',
        last_paid_ts=utcnow(),
        binding_ts=utcnow(),
    )


@pytest.fixture(autouse=True)
def mock_get_payment_methods(mocker, payment_method_data):
    return mocker.patch.object(
        TrustPaymentsClient,
        'get_payment_methods',
        mocker.AsyncMock(return_value=[payment_method_data]),
    )


@pytest.fixture
def card_expiration_date():
    return utcnow() + timedelta(days=365)


@pytest.fixture
async def cards(storage, merchant, rands, owner_uid, card_expiration_date):
    cards = []
    for tsp_type, bank in zip([TSPType.MASTERCARD, TSPType.VISA], ['alfa-bank', 'tinkoff']):
        for merchant_id in [None, merchant.merchant_id]:
            card = await storage.card.create(Card(
                trust_card_id=rands(),
                owner_uid=owner_uid,
                tsp=tsp_type,
                expire=card_expiration_date,
                last4='1234',
                issuer_bank=bank,
            ))

            enrollment = await storage.enrollment.create(Enrollment(
                card_id=card.card_id,
                merchant_id=merchant_id,
                tsp_card_id=f'tsp-card-id-{tsp_type.value}',
                tsp_token_id=f'tsp-token-id-{tsp_type.value}-{merchant_id}',
                tsp_token_status=TSPTokenStatus.ACTIVE,
                expire=card_expiration_date,
            ))
            cards.append({'card': card, 'enrollment': enrollment})

    card_without_enrollment = await storage.card.create(Card(
        trust_card_id='card-without-enrollment',
        owner_uid=owner_uid,
        tsp=TSPType.VISA,
        expire=card_expiration_date,
        last4='1234',
    ))

    cards.append({'card': card_without_enrollment, 'enrollment': None})
    return cards


@pytest.fixture
def expected_user_cards_from_db(cards: list[dict], owner_uid):
    mc_card: Card = replace(cards[0]['card'], enrollment=cards[0]['enrollment'])
    visa_card: Card = replace(cards[2]['card'], enrollment=cards[2]['enrollment'])

    return [
        UserCard(
            card_id=str(mc_card.card_id),
            owner_uid=owner_uid,
            card_network=CardNetwork.from_tsp_type(mc_card.tsp),
            last4=mc_card.last4,
            allowed_auth_methods=[AuthMethod.CLOUD_TOKEN],
            last_paid=datetime.fromtimestamp(0, tz=timezone.utc),
            created=datetime.fromtimestamp(0, tz=timezone.utc),
            card=mc_card,
            issuer_bank=IssuerBank.ALFABANK,
            is_removed=mc_card.is_removed,
            is_expired=mc_card.is_expired(),
            expiration_date=mc_card.expiration_date,
            trust_card_id=mc_card.trust_card_id,
        ),
        UserCard(
            card_id=str(visa_card.card_id),
            owner_uid=owner_uid,
            card_network=CardNetwork.from_tsp_type(visa_card.tsp),
            last4=visa_card.last4,
            allowed_auth_methods=[AuthMethod.CLOUD_TOKEN],
            last_paid=datetime.fromtimestamp(0, tz=timezone.utc),
            created=datetime.fromtimestamp(0, tz=timezone.utc),
            card=visa_card,
            issuer_bank=IssuerBank.TINKOFF,
            is_removed=visa_card.is_removed,
            is_expired=visa_card.is_expired(),
            expiration_date=visa_card.expiration_date,
            trust_card_id=visa_card.trust_card_id,
        ),
    ]


@pytest.fixture
def expected_user_card_from_trust_payment_method(owner_uid, payment_method_data):
    return UserCard(
        card_id=payment_method_data.card_id,
        owner_uid=owner_uid,
        card_network=CardNetwork.from_trust_string(payment_method_data.payment_system),
        last4='7890',
        allowed_auth_methods=[AuthMethod.PAN_ONLY],
        issuer_bank=IssuerBank.SBERBANK,
        last_paid=payment_method_data.last_paid_ts,
        created=payment_method_data.binding_ts,
        trust_payment_method=payment_method_data,
        is_removed=False,
        is_expired=payment_method_data.expired,
        bin='123456',
        expiration_date=ExpirationDate(
            month=1,
            year=2030,
        ),
        trust_card_id=payment_method_data.card_id,
    )


@pytest.fixture
def expected_user_cards(
    expected_user_cards_from_db, expected_user_card_from_trust_payment_method
):
    return [
        expected_user_card_from_trust_payment_method,
        *expected_user_cards_from_db,
    ]


@pytest.fixture
async def returned(cards, user):
    return await GetUserCardsAction(user=user).run()


@pytest.mark.asyncio
async def test_returned_cards(returned, cards, expected_user_cards):
    assert_that(
        returned,
        contains_inanyorder(*expected_user_cards)
    )


@pytest.mark.asyncio
async def test_expired_card_ignored(
    storage, cards: list[dict], user, expected_user_card_from_trust_payment_method
):
    for each in cards:
        db_card = each['card']
        db_card.expire = utcnow() - timedelta(minutes=1)
        await storage.card.save(db_card)

    returned = await GetUserCardsAction(user=user).run()
    expected = [expected_user_card_from_trust_payment_method]
    assert_that(returned, equal_to(expected))


@pytest.mark.asyncio
async def test_expired_enrollment_ignored(
    storage, cards: list[dict], user, expected_user_card_from_trust_payment_method
):
    for each in cards:
        enrollment = each['enrollment']
        if enrollment:
            enrollment.expire = utcnow() - timedelta(minutes=1)
            await storage.enrollment.save(enrollment)

    returned = await GetUserCardsAction(user=user).run()
    expected = [expected_user_card_from_trust_payment_method]
    assert_that(returned, equal_to(expected))


class TestUnknownTrustPaymentSystemMonitoring:
    @pytest.fixture
    def payment_method_data(self, owner_uid, trust_id):
        return TrustPaymentMethod(
            id='card-x1a1234567a12abcd12345a1a',
            card_id=trust_id,
            binding_systems=['trust'],
            orig_uid=str(owner_uid),
            payment_method='card',
            system='MasterCard',
            payment_system='Some unknown payment system',
            expiration_month='01',
            expiration_year='2030',
            card_bank='SBERBANK OF RUSSIA',
            expired=False,
            account='123456****7890',
            last_paid_ts=utcnow(),
            binding_ts=utcnow(),
        )

    @pytest.mark.asyncio
    async def test_should_increment_unknown_trust_payment_system_metric(self, user):
        before = unknown_trust_payment_system.get()

        await GetUserCardsAction(user=user).run()

        after = unknown_trust_payment_system.get()
        assert after[0][1] - before[0][1] == 1

    @pytest.mark.asyncio
    async def test_should_log_unknown_trust_payment_system(
        self,
        mocked_logger,
        user,
        payment_method_data,
    ):
        GetUserCardsAction.context.logger = mocked_logger

        await GetUserCardsAction(user=user).run()

        mocked_logger.context_push.asser_called_once_with(
            card_network=payment_method_data.payment_system
        )
        mocked_logger.error.assert_called_once_with('Unknown card_network from trust')


class TestCardIsBothInTrustAndYandexPay:
    @pytest.fixture
    def trust_id(self, owner_uid, cards: list[dict]):
        return cards[0]['card'].trust_card_id

    @pytest.fixture
    def expected_intersection(self, owner_uid, cards: list[dict], payment_method_data):
        mc_card = replace(cards[0]['card'], enrollment=cards[0]['enrollment'])
        visa_card = replace(cards[2]['card'], enrollment=cards[2]['enrollment'])
        return [
            UserCard(
                card_id=str(mc_card.card_id),
                owner_uid=owner_uid,
                card_network=CardNetwork.from_tsp_type(mc_card.tsp),
                last4=mc_card.last4,
                allowed_auth_methods=[AuthMethod.CLOUD_TOKEN, AuthMethod.PAN_ONLY],
                last_paid=payment_method_data.last_paid_ts,
                created=payment_method_data.binding_ts,
                trust_payment_method=payment_method_data,
                card=mc_card,
                issuer_bank=IssuerBank.ALFABANK,  # issuer name comes from our db
                is_removed=mc_card.is_removed,
                bin='123456',
                expiration_date=ExpirationDate(
                    month=1,
                    year=2030,
                ),
                trust_card_id=mc_card.trust_card_id,
            ),
            UserCard(
                card_id=str(visa_card.card_id),
                owner_uid=owner_uid,
                card_network=CardNetwork.from_tsp_type(visa_card.tsp),
                last4=visa_card.last4,
                allowed_auth_methods=[AuthMethod.CLOUD_TOKEN],
                last_paid=datetime.fromtimestamp(0, tz=timezone.utc),
                created=datetime.fromtimestamp(0, tz=timezone.utc),
                card=visa_card,
                issuer_bank=IssuerBank.TINKOFF,
                is_removed=visa_card.is_removed,
                expiration_date=ExpirationDate(
                    month=visa_card.expiration_date.month,
                    year=visa_card.expiration_date.year,
                ),
                trust_card_id=visa_card.trust_card_id,
            ),
        ]

    @pytest.mark.asyncio
    async def test_returned_cards(self, returned, expected_intersection):
        assert_that(
            returned,
            contains_inanyorder(*expected_intersection)
        )

    @pytest.mark.asyncio
    async def test_bank_name_lookup_falls_back_to_trust_if_missing_from_db(
        self, storage, cards: list[dict], expected_intersection, user
    ):
        card = cards[0]['card']
        card.issuer_bank = None  # assume we don't know the bank name
        card = await storage.card.save(card)

        # need to call the action after the card is updated
        returned = await GetUserCardsAction(user=user).run()
        expected_intersection[0].issuer_bank = IssuerBank.SBERBANK  # issuer name comes from trust
        expected_intersection[0].card.issuer_bank = None
        expected_intersection[0].card.updated = card.updated

        assert_that(
            returned,
            contains_inanyorder(*expected_intersection)
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('bank_name_in_trust', [None, '', 'gibberish'])
    async def test_bank_name_is_bad_in_both_pay_and_trust(
        self,
        storage,
        cards,
        expected_intersection,
        user,
        payment_method_data,
        yandex_pay_settings,
        bank_name_in_trust,
    ):
        # pay does not have data
        card = cards[0]['card']
        card.issuer_bank = None
        card = await storage.card.save(card)

        # trust returns strange data
        payment_method_data.card_bank = bank_name_in_trust  # assume trust doesn't know the bank name either

        # need to call the action after the card is updated
        returned = await GetUserCardsAction(user=user).run()

        expected_intersection[0].issuer_bank = IssuerBank.UNKNOWN
        expected_intersection[0].card.issuer_bank = None
        expected_intersection[0].card.updated = card.updated

        assert_that(
            returned,
            contains_inanyorder(*expected_intersection)
        )

    @pytest.fixture
    def yenv_type_testing(self):
        _type = yenv.type
        yenv.type = 'testing'
        yield
        yenv.type = _type

    @pytest.mark.asyncio
    @pytest.mark.parametrize('bank_name_in_trust', [None, ''])
    @pytest.mark.parametrize(
        ['last_digit', 'expected_card_issuer'],
        [
            ['0', IssuerBank.UNKNOWN],
            ['1', IssuerBank.ALFABANK],
            ['2', IssuerBank.BINBANK],
            ['3', IssuerBank.OTKRITIE],
            ['4', IssuerBank.RAIFFEISEN],
            ['5', IssuerBank.RUSSTANDARD],
            ['6', IssuerBank.SBERBANK],
            ['7', IssuerBank.SOVCOMBANK],
            ['8', IssuerBank.TINKOFF],
            ['9', IssuerBank.VTB],
        ],
    )
    async def test_bank_substitutes_unknown_bank_name_for_testing(
        self,
        yenv_type_testing,
        storage,
        cards,
        expected_intersection,
        user,
        payment_method_data: TrustPaymentMethod,
        yandex_pay_settings,
        last_digit,
        expected_card_issuer,
        bank_name_in_trust,
    ):
        """
        For testing environment we have extra conditions on how to
        determine normalized card issuer depending on card number, if card_issuer is unknown
        in out database and in payment method data.
        """
        card = cards[0]['card']
        card.issuer_bank = None
        card = await storage.card.save(card)

        payment_method_data.account = payment_method_data.account[:-1] + last_digit
        payment_method_data.card_bank = bank_name_in_trust

        cards = await GetUserCardsAction(user=user).run()

        expected_intersection[0].issuer_bank = expected_card_issuer
        expected_intersection[0].card.issuer_bank = None
        expected_intersection[0].card.updated = card.updated

        # fast fail test if issuer is incorrect
        assert expected_card_issuer == cards[0].issuer_bank

        assert_that(
            cards,
            contains_inanyorder(*expected_intersection)
        )


@pytest.mark.asyncio
@pytest.mark.parametrize('owner_uid, expected_order', [
    (4, ['2', '3', '1']),
    (6, ['3', '2', '1']),
])
async def test_sort_order(mocker, owner_uid, expected_order):
    cards = [
        UserCard(
            card_id='1',
            owner_uid=2,
            card_network=CardNetwork.UNKNOWN,
            last4='1234',
            allowed_auth_methods=[AuthMethod.CLOUD_TOKEN, AuthMethod.PAN_ONLY],
            last_paid=datetime(2021, 1, 1, 0, 0, 0),
            created=datetime(2021, 1, 1, 0, 0, 0),
            issuer_bank=IssuerBank.UNKNOWN,
            expiration_date=ExpirationDate(
                month=10,
                year=2010,
            ),
            trust_card_id='trust_card_id',
        ),
        UserCard(
            card_id='2',
            owner_uid=2,
            card_network=CardNetwork.UNKNOWN,
            last4='1234',
            allowed_auth_methods=[AuthMethod.CLOUD_TOKEN, AuthMethod.PAN_ONLY],
            last_paid=datetime(2021, 1, 2, 0, 0, 0),
            created=datetime(2021, 1, 1, 0, 0, 0),
            issuer_bank=IssuerBank.UNKNOWN,
            expiration_date=ExpirationDate(
                month=10,
                year=2010,
            ),
            trust_card_id='trust_card_id',
        ),
        UserCard(
            card_id='3',
            owner_uid=2,
            card_network=CardNetwork.UNKNOWN,
            last4='1234',
            allowed_auth_methods=[AuthMethod.CLOUD_TOKEN, AuthMethod.PAN_ONLY],
            last_paid=datetime(2021, 1, 1, 0, 0, 0),
            created=datetime(2021, 1, 2, 0, 0, 0),
            issuer_bank=IssuerBank.UNKNOWN,
            expiration_date=ExpirationDate(
                month=10,
                year=2010,
            ),
            trust_card_id='trust_card_id',
        ),
    ]
    mocker.patch.object(GetUserCardsAction, '_get_user_cards', mocker.AsyncMock(return_value=cards))

    returned = await GetUserCardsAction(user=User(owner_uid)).run()

    assert_that(
        returned,
        contains(*map(lambda v: has_property('card_id', v), expected_order))
    )


@pytest.mark.asyncio
async def test_forbidden_card_networks(mocker, user):
    cards = [
        UserCard(
            card_id=str(idx),
            owner_uid=user.uid,
            card_network=card_network,
            last4='1234',
            allowed_auth_methods=[AuthMethod.PAN_ONLY],
            last_paid=utcnow(),
            created=utcnow(),
            issuer_bank=IssuerBank.UNKNOWN,
            expiration_date=ExpirationDate(
                month=10,
                year=2010,
            ),
            trust_card_id='trust_card_id',
        ) for idx, card_network in enumerate(CardNetwork)
    ]
    mocker.patch.object(GetUserCardsAction, '_get_user_cards', mocker.AsyncMock(return_value=cards))
    forbidden_card_networks = {CardNetwork.MASTERCARD, CardNetwork.VISA, CardNetwork.VISAELECTRON}

    returned = await GetUserCardsAction(user=user, forbidden_card_networks=forbidden_card_networks).run()

    assert_that(returned, has_length(len(cards)))

    for card in returned:
        assert_that(
            card.allowed_auth_methods,
            has_length(0 if card.card_network in forbidden_card_networks else 1)
        )
