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

from balance import (
    constants as cst,
    cookie2,
    mapper,
)
from tests import object_builder as ob

from brest.core.tests import security
from brest.utils.config import get_config
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.resource import mock_client_resource
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_role, create_admin_role
from yb_snout_api.tests_unit.fixtures.resource import URL, build_custom_resource_cxt


@pytest.fixture(name='view_dev_data_role')
def create_view_dev_data_role():
    return create_role(cst.PermissionCode.VIEW_DEV_DATA)


@pytest.fixture(name='substitute_role')
def create_substitute_role():
    return create_role(cst.PermissionCode.SUBSTITUTE_LOGIN)


class SignKeyMixin(object):
    @property
    def _sign_key(self):
        return get_config('Properties/Property', {'name': 'cookie_sign_key'}).get('value', None)


class TestCasePassportSession(TestCaseApiAppBase, SignKeyMixin):
    BASE_API = '/v1/debug/dump'

    def test_existing_passport_oper_id(self, admin_role, view_dev_data_role, client):
        passport = ob.create_passport(self.test_session, admin_role, view_dev_data_role)
        passport.link_to_client(client)
        self.test_session.flush()

        assert self.test_session.passport != passport
        assert self.test_session.oper_id != passport.passport_id

        with mock.patch('brest.utils.security.check_auth', return_value={'passport_id': passport.passport_id}):
            response = self.test_client.get(self.BASE_API)
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        assert self.test_session.passport is passport
        hm.assert_that(
            response.get_json().get('data', {}),
            hm.has_entries({
                'resource': hm.has_entries({
                    'oper_id': passport.passport_id,
                    'subst_oper_id': hm.is_(None),
                    'raw_subst_oper_id': hm.is_(None),
                    'session': hm.has_entry('oper_id', passport.passport_id),
                }),
                'user': hm.has_entry('client_id', client.id),
            }),
        )

    @pytest.mark.parametrize(
        'role_funcs, replaced',
        [
            ((create_view_dev_data_role,), False),
            ((create_view_dev_data_role, create_substitute_role), True),
        ],
    )
    def test_existing_passport_subst_oper_id(self, admin_role, view_dev_data_role, client, role_funcs, replaced):
        passport = ob.create_passport(self.test_session, admin_role, view_dev_data_role)
        passport.link_to_client(client)
        self.test_session.flush()

        assert self.test_session.passport is not passport
        assert self.test_session.oper_id != passport.passport_id

        old_passport_id = self.test_session.oper_id

        cookie = cookie2.encode_map(
            {'SUBST_ID': str(passport.passport_id)},
            sign_key=self._sign_key,
        )
        self.test_client.set_cookie('', 'balance_cookie', cookie)

        roles = [admin_role]
        roles.extend([r() for r in role_funcs])
        security.set_roles(roles)
        response = self.test_client.get(self.BASE_API, is_admin=False)
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        hm.assert_that(
            response.get_json().get('data', {}),
            hm.has_entries({
                'resource': hm.has_entries({
                    'oper_id': old_passport_id,
                    'subst_oper_id': passport.passport_id if replaced else hm.is_(None),
                    'raw_subst_oper_id': passport.passport_id,
                    'session': hm.has_entry('oper_id', passport.passport_id if replaced else old_passport_id),
                }),
                'user': hm.has_entry('client_id', client.id if replaced else None),
            }),
        )


class TestCasePassportSession2(TestCaseApiAppBase, SignKeyMixin):
    BASE_API = URL

    @pytest.mark.parametrize(
        'passport_fields, exists, json_res',
        [
            pytest.param(
                {
                    'login': 'new test user',
                    'email': 'Test@email.com',
                    'fio': 'Pupkin Vasya',
                },
                True,
                {
                    'error': 'NotFound',  # бесправные не узнают о дебажной ручке
                    'description': 'The requested URL was not found on the server.'
                                   '  If you entered the URL manually please check your spelling and try again.',
                },
                id='passport exists',
            ),
            pytest.param(
                {},
                False,
                {
                    'error': 'PASSPORT_NOT_FOUND',
                    'description': 'Passport with ID -100500 not found in DB',
                },
                id='passport doesn\'t exist',
            ),
        ],
    )
    @pytest.mark.parametrize(
        'id_type',
        ['oper_id', 'subst_id'],
    )
    def test_new_passport(self, passport_fields, exists, json_res, id_type):
        new_passport_id = -100500
        old_oper_id = self.test_session.oper_id
        assert not self.test_session.query(mapper.Passport).filter_by(passport_id=new_passport_id).exists()

        is_oper_id = id_type == 'oper_id'
        new_oper_id = new_passport_id if is_oper_id else old_oper_id

        bb_resp = {
            'status': 200,
            'uid': new_passport_id if exists else None,
            'fields': passport_fields,
        }

        with mock.patch('butils.passport.PassportBlackbox._call_api_once', return_value=bb_resp) as mock_call, \
                mock.patch('brest.utils.security.check_auth', return_value={'passport_id': new_oper_id}), \
                build_custom_resource_cxt():

            if not is_oper_id:
                cookie = cookie2.encode_map(
                    {'SUBST_ID': str(new_passport_id)},
                    sign_key=self._sign_key,
                )
                self.test_client.set_cookie('', 'balance_cookie', cookie)

            response = self.test_client.get(self.BASE_API, is_admin=False)
            hm.assert_that(response.status_code, hm.equal_to(http.NOT_FOUND))
            hm.assert_that(
                response.get_json(),
                hm.has_entries(json_res),
            )

            mock_call.assert_called_once()
            hm.assert_that(
                mock_call.call_args_list[0][0],
                hm.has_item(
                    hm.has_entries(
                        uid=new_passport_id,
                        method='userinfo',
                        passport_name='mimino',
                    ),
                ),
            )

        if not exists:
            # если паспорта нет в Паспорте, то такого пользователя мы не пускаем
            return

        assert self.test_session.oper_id == new_oper_id

        new_passport = self.test_session.query(mapper.Passport).getone(passport_id=new_passport_id)
        hm.assert_that(
            new_passport,
            hm.has_properties({
                'login': passport_fields['login'],
                'email': passport_fields['email'],
                'gecos': passport_fields['fio'],
            }),
        )
