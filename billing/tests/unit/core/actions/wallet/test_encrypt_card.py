import logging
import re
from urllib.parse import urljoin

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, has_entries, has_item, has_properties

from billing.yandex_pay.yandex_pay.core.actions.card.get import GetUserCardByCardIdAction
from billing.yandex_pay.yandex_pay.core.actions.wallet.encrypt_card import ThalesEncryptCardAction
from billing.yandex_pay.yandex_pay.core.entities.card import ExpirationDate, UserCard
from billing.yandex_pay.yandex_pay.core.entities.enums import AuthMethod, CardNetwork
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.core.exceptions import CoreCardExpiredError, CoreCardNotFoundError
from billing.yandex_pay.yandex_pay.interactions import CardProxyClient
from billing.yandex_pay.yandex_pay.interactions.duckgo.entities.thales import ThalesEncryptedCardResult
from billing.yandex_pay.yandex_pay.utils.normalize_banks import IssuerBank

FAKE_UID = 123
FAKE_CARD_ID = 'fake_card_id'
FAKE_ENCRYPTED_CARD = 'fake_encrypted_card'


@pytest.fixture(params=[CardNetwork.VISA, CardNetwork.MASTERCARD])
def card_network(request):
    return request.param


@pytest.fixture
def user():
    return User(FAKE_UID)


@pytest.fixture
def user_card(card_network):
    return UserCard(
        card_id='fake_card_id',
        owner_uid=FAKE_UID,
        card_network=card_network,
        last4='0123',
        allowed_auth_methods=[AuthMethod.PAN_ONLY, AuthMethod.CLOUD_TOKEN],
        created=utcnow(),
        last_paid=utcnow(),
        issuer_bank=IssuerBank.UNKNOWN,
        expiration_date=ExpirationDate(
            month=10,
            year=2010,
        ),
        trust_card_id='trust_card_id',
    )


@pytest.fixture(autouse=True)
def mock_get_user_card(mock_action, user_card):
    return mock_action(GetUserCardByCardIdAction, user_card)


@pytest.fixture
def expiration_year():
    return utcnow().year + 1


@pytest.fixture
def paysys_card_payload(expiration_year):
    return {
        'card_id': FAKE_CARD_ID,
        'card_token': 'fake_card_token',
        'holder': 'CARD HOLDER',
        'expiration_year': expiration_year % 100,  # last 2 digits of the year
        'expiration_month': 1,
    }


@pytest.fixture
def mock_get_card_paysys(aioresponses_mocker, yandex_pay_settings, paysys_card_payload):
    return aioresponses_mocker.get(
        re.compile(f'{yandex_pay_settings.TRUST_PAYSYS_API_URL}/yapay/v1/cards/.*'),
        payload=paysys_card_payload,
    )


@pytest.fixture(autouse=True)
def mock_cardproxy(aioresponses_mocker, yandex_pay_settings):
    return aioresponses_mocker.post(
        urljoin(yandex_pay_settings.CARD_PROXY_API_URL, CardProxyClient.Paths.forward_request),
        payload={'data': {'encrypted_card': FAKE_ENCRYPTED_CARD}},
    )


@pytest.mark.asyncio
@pytest.mark.usefixtures('mock_get_card_paysys')
async def test_encrypt_card_succeeds(user):
    encrypted_card = await ThalesEncryptCardAction(user, FAKE_CARD_ID, 'cvn_token').run()

    assert_that(
        encrypted_card, equal_to(ThalesEncryptedCardResult(FAKE_ENCRYPTED_CARD))
    )


@pytest.mark.asyncio
@pytest.mark.usefixtures('mock_get_card_paysys')
async def test_encrypt_card_logged(user, caplog, dummy_logger):
    caplog.set_level(logging.INFO, logger=dummy_logger.logger.name)

    await ThalesEncryptCardAction(user, FAKE_CARD_ID, 'cvn_token').run()

    logs = [r for r in caplog.records if r.name == dummy_logger.logger.name]
    assert_that(
        logs,
        has_item(
            has_properties(
                message='Card encrypted',
                _context=has_entries(
                    uid=FAKE_UID,
                    card_id=FAKE_CARD_ID,
                )
            )
        )
    )


@pytest.mark.asyncio
async def test_encrypt_card_not_found_in_paysys(user, aioresponses_mocker, yandex_pay_settings):
    aioresponses_mocker.get(
        re.compile(f'{yandex_pay_settings.TRUST_PAYSYS_API_URL}/yapay/v1/cards/.*'),
        payload={'status': 'card_not_found'},
        status=404,
    )

    with pytest.raises(CoreCardNotFoundError):
        await ThalesEncryptCardAction(user, FAKE_CARD_ID).run()


@pytest.mark.asyncio
async def test_encrypt_expired_card(user, aioresponses_mocker, yandex_pay_settings, paysys_card_payload):
    paysys_card_payload['expiration_year'] = utcnow().year - 1
    aioresponses_mocker.get(
        re.compile(f'{yandex_pay_settings.TRUST_PAYSYS_API_URL}/yapay/v1/cards/.*'),
        payload=paysys_card_payload,
    )

    with pytest.raises(CoreCardExpiredError):
        await ThalesEncryptCardAction(user, FAKE_CARD_ID).run()
