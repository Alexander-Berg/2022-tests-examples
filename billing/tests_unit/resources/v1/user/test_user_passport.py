# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import mock
import http.client as http
import hamcrest as hm

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.user import create_passport


class TestCaseUserPassport(TestCaseApiAppBase):
    BASE_URL = '/v1/user/passport'

    def test_forbidden(self, admin_role):
        security.set_roles([admin_role])
        res = self.test_client.get(self.BASE_URL)
        hm.assert_that(res.status_code, hm.equal_to(http.FORBIDDEN))

    def test_wo_params(self):
        res = self.test_client.get(self.BASE_URL, {})
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'FORM_VALIDATION_ERROR',
                'description': 'Form validation error.',
                'form_errors': hm.has_entries({
                    '?': hm.contains(hm.has_entries({
                        'error': 'PASSPORT_REQUIRED_FIELD_VALIDATION_ERROR',
                        'description': 'Required passport_id or login.',
                    })),
                }),
            }),
        )

    @pytest.mark.parametrize(
        'w_login',
        [
            pytest.param(True, id='by login'),
            pytest.param(False, id='by uid'),
        ],
    )
    @mock.patch('butils.passport_cache.PassportCacheBase._actually_refresh')
    def test_get_existing(self, _mock_bb, w_login):
        passport = create_passport(
            login='Covid-19',
            gecos='Coronavirus 2019',
            email='mivseumrem@covid.com',
        )
        self.test_session.flush()

        if w_login:
            params = {'login': passport.login}
        else:
            params = {'passport_id': passport.passport_id}

        res = self.test_client.get(self.BASE_URL, params)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json().get('data')
        hm.assert_that(
            data,
            hm.contains(hm.has_entries({
                'login': 'Covid-19',
                'fio': 'Coronavirus 2019',
                'uid': passport.passport_id,
                'email': 'mivseumrem@covid.com',
                'repr_client': None,
            })),
        )

    def test_not_found(self):
        bb_resp = {
            'status': 200,
            'uid': None,
            'fields': {
                'error': 'NotFound',  # бесправные не узнают о дебажной ручке
                'description': 'The requested URL was not found on the server.'
                               '  If you entered the URL manually please check your spelling and try again.',
            },
        }
        with mock.patch('butils.passport.PassportBlackbox._call_api_once', return_value=bb_resp):
            res = self.test_client.get(self.BASE_URL, {'passport_id': 1})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        hm.assert_that(res.get_json().get('data'), hm.empty())

    def test_new_passport(self):
        from yb_snout_api.resources.v1.user.enums import PassportIntranet

        login = 'covid'
        bb_resp = {
            'status': 200,
            'uid': 666,
            'fields': {
                'login': login,
                'email': 'mivseumrem@covid.com',
                'fio': 'Pupkin Vasya',
            },
        }

        with mock.patch('butils.passport.PassportBlackbox._call_api_once', return_value=bb_resp):
            res = self.test_client.get(
                self.BASE_URL,
                {'login': login, 'is_intranet': PassportIntranet.EXTERNAL.name},
            )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json().get('data')
        hm.assert_that(
            data,
            hm.contains(hm.has_entries({
                'login': login,
                'fio': 'Pupkin Vasya',
                'uid': 666,
                'email': 'mivseumrem@covid.com',
                'repr_client': None,
            })),
        )

    @pytest.mark.parametrize(
        'match_login',
        [True, False],
    )
    @mock.patch('butils.passport_cache.PassportCacheBase._actually_refresh')
    def test_uid_plus_login(self, _mock_refresh, match_login):
        passport = create_passport(login='new test login 12345')
        self.test_session.flush()

        res = self.test_client.get(
            self.BASE_URL,
            {
                'passport_id': passport.passport_id,
                'login': 'new test login 12345' + ('' if match_login else '1'),
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        res_match = hm.contains(hm.has_entries(uid=passport.passport_id)) if match_login else hm.empty()
        hm.assert_that(res.get_json().get('data'), res_match)
