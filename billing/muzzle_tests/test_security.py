# -*- coding: utf-8 -*-
import time
import random
import pytest

from muzzle.security import (csrf, sauth)

PASSPORT_ID = '666'
YANDEX_UID = '999'

SESSION_ID = '12345'
SESSION_ID2 = '54321'


@pytest.mark.parametrize(
    'w_passport_id',
    [True, False],
)
def test_check_flag(w_passport_id):
    secret_key = csrf.get_secret_key(PASSPORT_ID if w_passport_id else None, YANDEX_UID)
    assert secret_key.startswith('u' if w_passport_id else 'y')


@pytest.mark.parametrize(
    'delta, ans',
    [
        (-2, False),
        (-1, True),
        (0, True),
        (1, True),
        (2, False),
    ],
)
def test_days(delta, ans):
    days = (int(time.time()) / csrf.ONE_DAY_SEC) + delta
    secret_key = csrf._get_secret_key(PASSPORT_ID, YANDEX_UID, days)

    assert csrf.check_secret_key(PASSPORT_ID, YANDEX_UID, secret_key) is ans


def test_sauth_key():
    hmac_key = sauth.get_secret_key(PASSPORT_ID, SESSION_ID, SESSION_ID2, YANDEX_UID)
    assert sauth.check_secret_key(PASSPORT_ID, SESSION_ID, SESSION_ID2, YANDEX_UID, hmac_key) is True


@pytest.mark.parametrize(
    'count_',
    [2, 3, 4],
)
def test_sauth_key_parts(count_):
    key = sauth.get_secret_key(PASSPORT_ID, SESSION_ID, SESSION_ID2, YANDEX_UID)
    key = '.'.join((key.split('.') + ['666'])[:count_])
    assert sauth.check_secret_key(PASSPORT_ID, SESSION_ID, SESSION_ID2, YANDEX_UID, key) is (count_ == 3)
