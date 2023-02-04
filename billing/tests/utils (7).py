# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from builtins import range
from future import standard_library

standard_library.install_aliases()

import contextlib
import random
import string


def generate_random_numeric_str(n):
    """
    :param int n:
    :rtype: str
    """
    return ''.join(random.choice(string.digits) for _ in range(n))


def generate_random_alpha_numeric_str(n):
    """
    :param int n:
    :rtype: str
    """
    # pylint: disable=deprecated-string-function
    choices = string.letters + string.digits

    return ''.join(random.choice(choices) for _ in range(n))


def get_test_session():
    from .base import TestCaseAppBase
    return TestCaseAppBase.get_test_session()


def with_csrf(test_client, oper_id, yandex_uid, data):
    from muzzle.security import csrf

    test_client.set_cookie('', 'yandexuid', yandex_uid)

    result = {
        '_csrf': csrf.get_secret_key(oper_id, yandex_uid),
    }
    result.update(data)

    return result


def set_csrf_header(test_client, oper_id, yandex_uid, headers):
    from muzzle.security import csrf

    test_client.set_cookie('', 'yandexuid', yandex_uid)
    headers['X-CSRF'] = csrf.get_secret_key(oper_id, yandex_uid)

    return headers


@contextlib.contextmanager
def real_session_ctx():
    from .yb_test_app import TestApp

    TestApp._use_test_session = False

    yield

    TestApp._use_test_session = True
