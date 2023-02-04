from datetime import timedelta

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, greater_than, has_properties, not_none, starts_with

from billing.yandex_pay.yandex_pay.core.payment_token import generate_message


@pytest.mark.parametrize(
    'expires_in', [None, timedelta(seconds=30), timedelta(hours=1)]
)
def test_generate_message_id(expires_in):
    message = generate_message(777, expires_in=expires_in)

    assert_that(
        message,
        has_properties(
            message_id=starts_with('1:'),
            expires_at=has_properties(tzinfo=not_none()),
            expiration_epoch_ms=greater_than(int(utcnow().timestamp()) * 1000),
        )
    )
