# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import mock
import pytest

import app.utils.otrs
import core.models.user

from core.utils.blackbox import BlackboxUser
from core.utils.blackbox import BlackboxUserError


@pytest.fixture
def patch_external_userinfo_by_uid(monkeypatch):
    def userinfo_mock(uid):
        kwargs = {'uid': uid,
                  'fields': {'aliases': [('1', 'user')]},
                  'emails': [{'address': 'user@mailserver'}]}
        return BlackboxUser(**kwargs)
    monkeypatch.setattr(
        core.models.user, 'external_userinfo_by_uid', userinfo_mock)


@pytest.fixture
def patch_external_userinfo_by_login(monkeypatch):
    import app.forms.blackbox
    def userinfo_mock(login):
        try:
            user_id = {'user': 1, 'nouser': 2}[login]
        except KeyError:
            raise BlackboxUserError
        return BlackboxUser(fields={'aliases': [('1', 'login')]}, uid=user_id)
    monkeypatch.setattr(
        app.forms.blackbox, 'external_userinfo_by_login', userinfo_mock)


@pytest.fixture
def patch_otrs_client(monkeypatch):
    class OTRSClientMock(object):

        def __init__(self, *args):
            pass

        def get_ticket_number(self, ticket_id):
            return 1

    monkeypatch.setattr(
        app.utils.otrs, 'OTRSClient', OTRSClientMock)


@pytest.fixture
def patch_tanker(monkeypatch):
    import app.views.mail_template
    monkeypatch.setattr(app.views.mail_template, 'Tanker', mock.Mock())


@pytest.fixture
def patch_tanker_adapter(monkeypatch):
    import app.views.page
    monkeypatch.setattr(app.views.page.tanker, 'TankerAdapter', mock.Mock())
