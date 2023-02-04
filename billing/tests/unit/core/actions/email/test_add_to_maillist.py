import logging

import pytest
from pay.lib.interactions.sender.client import SenderResult
from pay.lib.interactions.sender.enums import SenderResultStatus

from hamcrest import assert_that, has_entries, has_properties, is_, starts_with

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.email.add_to_maillist import AddToMaillistAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import InvalidSubscriptionTokenError
from billing.yandex_pay_plus.yandex_pay_plus.interactions import SenderClient


@pytest.fixture
def uid(randn):
    return randn()


@pytest.fixture
def email():
    return 'test@example.tld'


class TestDecrypt:
    def test_token_decrypt_success(self, uid, email):
        token = AddToMaillistAction.generate_token(uid, email)
        assert_that(token, starts_with('1:'))

        decrypt_result = AddToMaillistAction(token=token).decrypt_token()
        assert_that(decrypt_result, has_properties(email=email, uid=uid))

    def test_invalid_token_version(self, uid, email):
        token = AddToMaillistAction.generate_token(uid, email)
        token = f'2:{token[2:]}'

        with pytest.raises(InvalidSubscriptionTokenError):
            AddToMaillistAction(token=token).decrypt_token()

    def test_invalid_uid(self, uid, email):
        token = AddToMaillistAction.generate_token('not-a-number', email)

        with pytest.raises(InvalidSubscriptionTokenError):
            AddToMaillistAction(token=token).decrypt_token()

    def test_invalid_token(self):
        with pytest.raises(InvalidSubscriptionTokenError):
            AddToMaillistAction(token='1:fake').decrypt_token()


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'status,expected_result',
    [
        (SenderResultStatus.OK, True),
        (SenderResultStatus.ERROR, False),
    ]
)
async def test_subscription_result(
    uid, email, status, expected_result, yandex_pay_plus_settings, mocker
):
    mock = mocker.AsyncMock(return_value=SenderResult(status=status))
    mocker.patch.object(SenderClient, 'add_to_maillist', mock)
    token = AddToMaillistAction.generate_token(uid, email)

    result = await AddToMaillistAction(token=token).run()

    assert_that(result, is_(expected_result))
    mock.assert_awaited_once_with(
        maillist_slug=yandex_pay_plus_settings.SENDER_MAILLIST_SLUG,
        email=email,
    )


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'status,expected_level,expected_message',
    [
        (SenderResultStatus.OK, logging.INFO, 'Successfully added email to the maillist'),
        (SenderResultStatus.ERROR, logging.WARNING, 'Failed to add email to the maillist'),
    ]
)
async def test_subscription_call_logged(
    uid,
    email,
    status,
    expected_level,
    expected_message,
    yandex_pay_plus_settings,
    dummy_logs,
    mocker,
):
    result = SenderResult(status=status)
    mocker.patch.object(SenderClient, 'add_to_maillist', mocker.AsyncMock(return_value=result))
    token = AddToMaillistAction.generate_token(uid, email)

    await AddToMaillistAction(token=token).run()

    [log] = dummy_logs()
    assert_that(
        log,
        has_properties(
            message=expected_message,
            levelno=expected_level,
            _context=has_entries(
                token=token,
                uid=uid,
                maillist_slug=yandex_pay_plus_settings.SENDER_MAILLIST_SLUG,
                result=result,
            )
        )
    )
