# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import hamcrest as hm
import http.client as http

from balance import constants as cst, mapper
from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import (
    create_admin_role,
    get_client_role,
    get_role,
    create_representative,
    create_passport,
)
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import (
    create_client,
    create_client_with_intercompany,
    create_agency,
)


@pytest.mark.smoke
class TestCaseClientRepresentatives(TestCaseApiAppBase):
    BASE_API = '/v1/client/representatives'

    @pytest.mark.parametrize(
        'is_admin_request',
        [True, False],
    )
    def test_passport_is_main(self, client, is_admin_request):
        self.test_session.passport.is_main = False
        self.test_session.flush()

        security.set_passport_client(client)
        res = self.test_client.get(self.BASE_API, is_admin=is_admin_request)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        hm.assert_that(res.get_json()["data"], hm.has_length(1))

    @pytest.mark.parametrize(
        'is_admin_request',
        [True, False],
    )
    def test_role_repr(self, client, is_admin_request):
        self.test_session.passport.is_main = False
        self.test_session.add(
            mapper.RoleClientPassport(
                passport=self.test_session.passport,
                role_id=cst.RoleType.REPRESENTATIVE,
                client=client
            )
        )
        self.test_session.flush()

        res = self.test_client.get(self.BASE_API, is_admin=is_admin_request)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        hm.assert_that(res.get_json()["data"], hm.has_length(1))

    @pytest.mark.parametrize(
        'is_admin_request',
        [True, False],
    )
    def test_direct_limited_role(self, client, is_admin_request):
        self.test_session.passport.is_main = True
        self.test_session.flush()

        security.set_passport_client(client)
        security.update_limited_role(client)
        res = self.test_client.get(self.BASE_API, is_admin=is_admin_request)
        hm.assert_that(res.status_code, hm.equal_to(http.FORBIDDEN if not is_admin_request else http.OK))
        if not is_admin_request:
            hm.assert_that(
                res.get_json(),
                hm.has_entries({
                    'error': 'PERMISSION_DENIED_DIRECT_LIMITED',
                    'description': (
                        'User %s has no rights to access client representatives due to DirectLimited perm.'
                        % self.test_session.passport.passport_id
                    ),
                }),
            )

    @pytest.mark.parametrize(
        'linked_client',
        [True, False],
    )
    @pytest.mark.parametrize(
        'req_client',
        [True, False],
    )
    def test_client_not_found(self, client, linked_client, req_client):
        self.test_session.passport.is_main = True
        self.test_session.flush()

        security.set_passport_client(client if linked_client else None)
        params = {}
        if req_client:
            params['client_id'] = client.id
        res = self.test_client.get(self.BASE_API, params)
        hm.assert_that(res.status_code, hm.equal_to(http.OK if (linked_client or req_client) else http.NOT_FOUND))

    def test_passport_wo_client(self):
        res = self.test_client.get(self.BASE_API, is_admin=False)
        hm.assert_that(res.status_code, hm.equal_to(http.NOT_FOUND))
        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'CLIENT_NOT_FOUND',
                'description': 'Client with ID 0 not found in DB',
            }),
        )

    def test_get_representatives(self):
        repr_passport = create_passport(
            gecos='Fiiiooooo',
            login='yndx-snout-test-test',
            email='yndx@snout.ru',
            avatar='123456'
        )
        representative = create_representative(passport=repr_passport)

        passport = self.test_session.passport
        passport.is_main = True
        self.test_session.flush()
        assert representative.passport is not passport

        security.set_passport_client(representative.client)
        res = self.test_client.get(self.BASE_API, is_admin=False)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.contains(
                hm.has_entries({
                    'email': passport.email,
                    'fio': passport.gecos,
                    'login': passport.login,
                    'accountant': False,
                    'is_main': True,
                    'avatar': passport.avatar,
                }),
                hm.has_entries({
                    'email': repr_passport.email,
                    'fio': repr_passport.gecos,
                    'login': repr_passport.login,
                    'accountant': True,
                    'is_main': False,
                    'avatar': repr_passport.avatar,
                }),
            ),
        )
