import time

import pytest

from intranet.library.fastapi_csrf.src import generate_csrf_token, check_csrf_token


SECRET_KEY = 'test-secret'
TOKEN_LIFETIME = 24 * 60 * 60


@pytest.mark.parametrize(
    'generate_uid, generate_ya_uid, validate_uid, validate_ya_uid, is_valid', (
        (1, 1, 1, 1, True),
        (1, 1, 1, 2, False),
        (1, 1, 2, 1, False),
        (1, 1, 2, 2, False),
    )
)
def test_csrf_validation(generate_uid, generate_ya_uid, validate_uid, validate_ya_uid, is_valid):
    token = generate_csrf_token(
        uid=generate_uid,
        yandex_uid=generate_ya_uid,
        secret_key=SECRET_KEY,
    )
    assert check_csrf_token(
        uid=validate_uid,
        yandex_uid=validate_ya_uid,
        secret_key=SECRET_KEY,
        token_lifetime=TOKEN_LIFETIME,
        csrf_token=token,
    ) == is_valid


def test_csrf_with_broken_timestamp():
    token = generate_csrf_token(
        uid=1,
        yandex_uid=1,
        secret_key=SECRET_KEY,
        timestamp=int(time.time()) - TOKEN_LIFETIME - 1,
    )
    assert check_csrf_token(
        uid=1,
        yandex_uid=1,
        secret_key=SECRET_KEY,
        token_lifetime=TOKEN_LIFETIME,
        csrf_token=token,
    ) is False
