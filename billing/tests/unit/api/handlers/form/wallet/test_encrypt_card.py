import base64

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.actions.wallet.encrypt_card import ThalesEncryptCardAction
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.interactions.duckgo.entities.thales import ThalesEncryptedCardResult

FAKE_UID = 456
FAKE_CARD_ID = 'fake_card_id'
FAKE_ENCRYPTED_CARD = base64.b64encode(b'fake_encrypted_card').decode()


@pytest.fixture
def user():
    return User(FAKE_UID)


@pytest.fixture(autouse=True)
def mock_authentication(user, mocker):
    return mocker.patch('sendr_auth.BlackboxAuthenticator.get_user', mocker.AsyncMock(return_value=user))


@pytest.mark.asyncio
async def test_handler_should_return_encrypted_card(app, mock_action):
    mock_action(ThalesEncryptCardAction, ThalesEncryptedCardResult(FAKE_ENCRYPTED_CARD))

    r = await app.post(
        '/api/mobile/v1/wallet/thales/encrypted_card', json={'card_id': FAKE_CARD_ID}
    )
    assert_that(r.status, equal_to(200))

    json_body = await r.json()
    assert_that(
        json_body,
        equal_to(
            {
                'data': {'encrypted_card': FAKE_ENCRYPTED_CARD},
                'status': 'success',
                'code': 200,
            }
        )
    )
