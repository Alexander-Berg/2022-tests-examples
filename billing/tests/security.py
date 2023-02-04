# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import contextlib
import copy

import mock

from . import utils


def get_roles():
    session = utils.get_test_session()
    return list(session.passport.real_roles)


def set_roles(roles):
    session = utils.get_test_session()
    passport = session.passport

    with mock.patch('butils.passport.passport_admsubscribe'):  # не ходим в апи паспорта
        passport.set_roles(roles)

    # Clean up permissions cache
    def delattr_safe(o, name):
        if hasattr(o, name):
            delattr(o, name)

    delattr_safe(passport, '_perms_cache')
    delattr_safe(session, 'oper_perms')


def set_passport_client(client):
    session = utils.get_test_session()
    passport = session.passport

    passport.client = client
    session.flush()


def update_limited_role(client):
    """
    Add limited role to passport permissions
    """
    session = utils.get_test_session()
    passport = session.passport

    passport.update_limited([client.id])
    session.flush()


def with_csrf(test_client, oper_id, yandex_uid, data):
    from muzzle.security import csrf

    test_client.set_cookie('', 'yandexuid', yandex_uid)

    result = {
        '_csrf': csrf.get_secret_key(oper_id, yandex_uid),
    }
    result.update(data)

    return result


def set_auth_methods(app, new_auth_methods):
    app.auth.methods = new_auth_methods


@contextlib.contextmanager
def switch_auth_methods(app, new_auth_methods):
    old_auth_methods = copy.copy(app.auth.methods)

    set_auth_methods(app, new_auth_methods)

    yield

    set_auth_methods(app, old_auth_methods)
