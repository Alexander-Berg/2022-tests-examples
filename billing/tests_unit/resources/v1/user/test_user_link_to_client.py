# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import mock
import pytest
import http.client as http
import hamcrest as hm

from balance import constants as cst, mapper

from tests import object_builder as ob
from tests.tutils import mock_transactions

from brest.core.tests import security
from yb_snout_api.resources.v1.user import enums
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import (
    create_admin_role,
    create_view_client_role,
    create_support_role,
    get_client_role,
)
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
from yb_snout_api.tests_unit.fixtures.person import create_person
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.user import create_passport


@pytest.fixture(name="rcp")
def role_client_passport(passport, client, role_id=cst.RoleType.REPRESENTATIVE):
    return ob.RoleClientPassportBuilder.construct(
        passport.session,
        passport=passport,
        client=client,
        role_id=role_id
    )


@pytest.fixture(name="domain_passport")
def create_domain_passport():
    return create_passport(passport_id=ob.get_domain_uid_value())


def bb_response(passport):
    return {
        'uid': passport.passport_id,
        'phones': [],
        'fields': {
            'login': passport.login,
            'fio': passport.gecos,
            'email': passport.email,
            'default_avatar_id': passport.avatar,
        }
    }


bb_not_found = dict(uid=None, login=None, fio=None, email=None)


@pytest.mark.smoke
class UserLinkToClientBase(TestCaseApiAppBase):
    def test_forbidden(self, admin_role, client, passport):
        security.set_roles([admin_role])
        with mock_transactions():
            res = self.test_client.secure_post(
                self.BASE_URL,
                {'passport_id': passport.passport_id, 'client_id': client.id},
            )
        hm.assert_that(res.status_code, hm.equal_to(http.FORBIDDEN))


class TestCaseUserLinkToClient(UserLinkToClientBase):
    BASE_URL = '/v1/user/link-to-client'

    @pytest.mark.parametrize(
        "as_admin, has_perm_or_owns, status",
        [
            [True, True, http.OK],
            [True, False, http.FORBIDDEN],
            [False, True, http.FORBIDDEN],
            [False, False, http.FORBIDDEN]
        ]
    )
    def test_permissions_for_repr(
        self, passport, client, as_admin, has_perm_or_owns, status,
        client_role, admin_role, support_role, view_client_role
    ):
        if as_admin:
            security.set_roles([admin_role] + ([support_role, view_client_role] if has_perm_or_owns else []))
        else:
            security.set_passport_client(client if has_perm_or_owns else create_client())
            security.set_roles([client_role])

        res = self.test_client.secure_post(
            self.BASE_URL,
            {
                'passport_id': passport.passport_id,
                'client_id': client.id,
                'link_type': enums.LinkType.REPRESENTATIVE.value
            },
            is_admin=as_admin,
        )
        hm.assert_that(res.status_code, hm.equal_to(status))

    @pytest.mark.parametrize(
        "who, exp_res",
        [
            ['admin_with_role', http.OK],
            ['admin', http.FORBIDDEN],
            ['main_representative', http.OK],
            ['not_main_representative', http.OK],
            ['role_representative', http.OK],
            ['someone_else', http.FORBIDDEN]
        ]
    )
    def test_permissions_for_role_repr(
        self, passport, client, who, exp_res, client_role, admin_role, support_role, view_client_role
    ):
        if 'admin' in who:
            security.set_roles([admin_role] + ([support_role, view_client_role] if 'with_role' in who else []))
        else:
            if who == 'main_representative':
                security.set_passport_client(client)
            elif who == 'not_main_representative':
                security.set_passport_client(client)
                self.test_session.passport.is_main = False
            elif who == 'someone_else':
                security.set_passport_client(create_client())
            elif who == 'role_representative':
                self.test_session.add(
                    mapper.RoleClientPassport(
                        passport=self.test_session.passport,
                        role_id=cst.RoleType.REPRESENTATIVE,
                        client=client
                    )
                )
                self.test_session.flush()
            security.set_roles([client_role])
        create_person(client=client, type='ur')

        with mock.patch('butils.passport.PassportBlackbox._call_api_once', return_value=bb_response(passport)):
            res = self.test_client.secure_post(
                self.BASE_URL,
                {
                    'passport_id': passport.passport_id,
                    'client_id': client.id,
                    'link_type': enums.LinkType.ROLE_REPRESENTATIVE.value
                },
                is_admin=('admin' in who)
            )
        hm.assert_that(res.status_code, hm.equal_to(exp_res))

    @pytest.mark.parametrize("with_link_type", [True, False])
    def test_link_to_client(self, passport, client, with_link_type):
        assert passport.client is None
        params = {'passport_id': passport.passport_id, 'client_id': client.id}
        if with_link_type:
            params['link_type'] = enums.LinkType.REPRESENTATIVE.value
        res = self.test_client.secure_post(
            self.BASE_URL,
            params,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        assert passport.client == client

    @pytest.mark.parametrize("extra_reprs", [True, False])
    @pytest.mark.parametrize("by_login", [True, False])
    def test_link_as_repr_to_client(self, passport, client, extra_reprs, by_login):
        if extra_reprs:
            role_client_passport(create_passport(), client, cst.RoleType.REPRESENTATIVE)
        params = {
            "client_id": client.id,
            "link_type": enums.LinkType.ROLE_REPRESENTATIVE.value
        }
        if by_login:
            params["login"] = passport.login
        else:
            params["passport_id"] = passport.passport_id

        create_person(client=client, type='ur')

        with mock.patch('butils.passport.PassportBlackbox._call_api_once', return_value=bb_response(passport)):
            res = self.test_client.secure_post(
                self.BASE_URL,
                params,
            )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        self.test_session.refresh(passport)
        hm.assert_that(
            passport.client_roles,
            hm.has_entries({
                client.id: hm.has_item(
                    hm.has_properties(
                        id=cst.RoleType.REPRESENTATIVE
                    )
                )
            })
        )


    @pytest.mark.parametrize("by_login", [True, False])
    @pytest.mark.parametrize(
        "link_type, target_client",
        [
            ('repr',      'this'),
            ('repr',      'other'),
            ('role repr', 'this'),
            ('role repr', 'other'),
        ]
    )
    def test_link_as_repr_resctricted_already_linked(self, passport, client, by_login, link_type, target_client):
        if link_type == 'repr':
            passport.client = client if target_client == 'this' else create_client()
        else:
            role_client_passport(passport, client if target_client == 'this' else create_client())
        self.test_session.flush()

        params = {
            "client_id": client.id,
            "link_type": enums.LinkType.ROLE_REPRESENTATIVE.value
        }
        if by_login:
            params["login"] = passport.login
        else:
            params["passport_id"] = passport.passport_id

        with mock.patch('butils.passport.PassportBlackbox._call_api_once', return_value=bb_response(passport)):
            res = self.test_client.secure_post(
                self.BASE_URL,
                params,
            )
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

    @pytest.mark.parametrize("by_login", [True, False])
    def test_intranet_passport_errors(self, domain_passport, client, by_login):
        params = {
            "client_id": client.id,
            "link_type": enums.LinkType.ROLE_REPRESENTATIVE.value
        }
        if by_login:
            params["login"] = domain_passport.login
        else:
            params["passport_id"] = domain_passport.passport_id

        create_person(client=client, type='ur')

        with mock.patch('butils.passport.PassportBlackbox._call_api_once', return_value=bb_not_found):
            res = self.test_client.secure_post(
                self.BASE_URL,
                params,
            )
        hm.assert_that(res.status_code, hm.equal_to(http.NOT_FOUND if by_login else http.BAD_REQUEST))
        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'PASSPORT_NOT_FOUND_BY_LOGIN' if by_login else 'PASSPORT_IS_INTERNAL',
                'description': 'Passport%s' % (
                    "(login=%s) was not found." % domain_passport.login
                    if by_login else
                    "(%s) is internal." % domain_passport.passport_id
                ),
            }),
        )

    @pytest.mark.parametrize('same_client', [True, False])
    def test_already_linked_to_repr_client(self, passport, client, same_client):
        repr_client = client if same_client else create_client()
        role_client_passport(passport, repr_client, cst.RoleType.REPRESENTATIVE)

        res = self.test_client.secure_post(
            self.BASE_URL,
            {'passport_id': passport.passport_id, 'client_id': client.id},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

        if same_client:
            hm.assert_that(
                res.get_json(),
                hm.has_entries({
                    'error': 'PASSPORT_ALREADY_LINKED_AS_REPRESENTATIVE',
                    'description': ('Passport %s (%s) is already linked as representative of client %s'
                                    % (passport.login, passport.passport_id, repr_client.id)),
                }),
            )
        else:
            hm.assert_that(
                res.get_json(),
                hm.has_entries({
                    'error': 'PASSPORT_ALREADY_LINKED_TO_OTHER_CLIENT',
                    'description': ('Passport %s (%s) is already linked to OTHER client %s'
                                    % (passport.login, passport.passport_id, repr_client.id)),
                }),
            )

    def test_already_linked_to_other_client(self, passport):
        client1, client2 = create_client(), create_client()

        passport.client = client1
        self.test_session.flush()

        res = self.test_client.secure_post(
            self.BASE_URL,
            {'passport_id': passport.passport_id, 'client_id': client2.id},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))
        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'PASSPORT_ALREADY_LINKED_TO_OTHER_CLIENT',
                'description': ('Passport %s (%s) is already linked to OTHER client %s'
                                % (passport.login, passport.passport_id, client1.id)),
            }),
        )

    @pytest.mark.parametrize(
        'w_ur_person',
        [True, False],
    )
    def test_accountant_link(self, client, w_ur_person):
        create_person(client=client, type='ph')
        if w_ur_person:
            create_person(client=client, type='ur')

        res = self.test_client.secure_post(
            self.BASE_URL,
            {
                'passport_id': self.test_session.passport.passport_id,
                'client_id': client.id,
                'link_type': 'ROLE_REPRESENTATIVE',
            },
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK if w_ur_person else http.BAD_REQUEST))

        if not w_ur_person:
            hm.assert_that(
                res.get_json(),
                hm.has_entries({
                    'error': 'CANNOT_LINK_ACCOUNT_PASSPORT',
                    'description': 'Client %s does not have ur persons.' % client.id,
                }),
            )


class TestCaseUserUnlinkFromClient(UserLinkToClientBase):
    BASE_URL = '/v1/user/unlink-from-client'

    @pytest.mark.parametrize(
        "as_admin, has_perm_or_owns, status",
        [
            [True, True, http.OK],
            [True, False, http.FORBIDDEN],
            [False, True, http.FORBIDDEN],
            [False, False, http.FORBIDDEN]
        ]
    )
    def test_permissions_for_repr(
        self, passport, client, as_admin, has_perm_or_owns, status,
        client_role, admin_role, support_role, view_client_role
    ):
        passport.client = client
        if as_admin:
            security.set_roles([admin_role] + ([support_role, view_client_role] if has_perm_or_owns else []))
        else:
            security.set_passport_client(client if has_perm_or_owns else create_client())
            security.set_roles([client_role])

        res = self.test_client.secure_post(
            self.BASE_URL,
            {
                'passport_id': passport.passport_id,
                'client_id': client.id,
                'link_type': enums.LinkType.REPRESENTATIVE.value
            },
            is_admin = as_admin,
        )
        hm.assert_that(res.status_code, hm.equal_to(status))

    @pytest.mark.parametrize(
        "who, exp_res",
        [
            ['admin_with_role',     http.OK],
            ['admin',               http.FORBIDDEN],
            ['main_representative', http.OK],
            ['not_main_representative', http.OK],
            ['role_representative', http.OK],
            ['someone_else',        http.FORBIDDEN]
        ]
    )
    def test_permissions_for_role_repr(
        self, passport, client, rcp, who, exp_res, client_role, admin_role, support_role, view_client_role
    ):
        if 'admin' in who:
            security.set_roles([admin_role] + ([support_role, view_client_role] if 'with_role' in who else []))
        else:
            if who == 'main_representative':
                security.set_passport_client(client)
            elif who == 'not_main_representative':
                security.set_passport_client(client)
                self.test_session.passport.is_main = False
            elif who == 'someone_else':
                security.set_passport_client(create_client())
            elif who == 'role_representative':
                self.test_session.add(
                    mapper.RoleClientPassport(
                        passport=self.test_session.passport,
                        role_id=cst.RoleType.REPRESENTATIVE,
                        client=client
                    )
                )
                self.test_session.flush()
            security.set_roles([client_role])

        with mock.patch('butils.passport.PassportBlackbox._call_api_once', return_value=bb_response(passport)):
            res = self.test_client.secure_post(
                self.BASE_URL,
                {
                    'passport_id': passport.passport_id,
                    'client_id': client.id,
                    'link_type': enums.LinkType.ROLE_REPRESENTATIVE.value
                },
                is_admin=('admin' in who)
            )
        hm.assert_that(res.status_code, hm.equal_to(exp_res))

    @pytest.mark.parametrize("with_link_type", [True, False])
    def test_unlink_client(self, passport, client, with_link_type):
        passport.client = client

        params = {'passport_id': passport.passport_id, 'client_id': client.id}
        if with_link_type:
            params['link_type'] = enums.LinkType.REPRESENTATIVE.value
        res = self.test_client.secure_post(
            self.BASE_URL,
            params,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        assert passport.client is None

    def test_passport_not_linked_to_client(self, passport, client):
        res = self.test_client.secure_post(
            self.BASE_URL,
            {'passport_id': passport.passport_id, 'client_id': client.id},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))
        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'PASSPORT_NOT_LINKED_TO_CLIENT',
                'description': ('Passport %s (%s) is NOT linked to client %s'
                                % (passport.login, passport.passport_id, client.id)),
            }),
        )

    @pytest.mark.parametrize("extra_reprs", [True, False])
    @pytest.mark.parametrize("by_login", [True, False])
    def test_unlink_client_linked_as_repr(self, passport, client, rcp, extra_reprs, by_login):
        if extra_reprs:
            role_client_passport(create_passport(), client, cst.RoleType.REPRESENTATIVE)

        params = {
            "client_id": client.id,
            "link_type": enums.LinkType.ROLE_REPRESENTATIVE.value
        }
        if by_login:
            params["login"] = passport.login
        else:
            params["passport_id"] = passport.passport_id

        with mock.patch('butils.passport.PassportBlackbox._call_api_once', return_value=bb_response(passport)):
            res = self.test_client.secure_post(
                self.BASE_URL,
                params,
            )

        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        self.test_session.refresh(passport)
        hm.assert_that(
            passport.client_roles,
            hm.not_(
                hm.has_entries({
                    client.id: hm.has_item(
                        hm.has_properties(
                            id=cst.RoleType.REPRESENTATIVE
                        )
                    )
                })
            )
        )

    @pytest.mark.parametrize("extra_rcps", [True, False])
    def test_passport_not_linked_to_client_as_repr(self, passport, client, extra_rcps):
        if extra_rcps:
            another_client, another_passport = create_client(), create_passport()
            role_client_passport(passport, client, cst.RoleType.DIRECT_LIMITED)
            role_client_passport(passport, another_client, cst.RoleType.DIRECT_LIMITED)
            role_client_passport(another_passport, client, cst.RoleType.REPRESENTATIVE)
            create_client(passport=passport)

        with mock.patch('butils.passport.PassportBlackbox._call_api_once', return_value=bb_response(passport)):
            res = self.test_client.secure_post(
                self.BASE_URL,
                {
                    'passport_id': passport.passport_id,
                    'client_id': client.id,
                    'link_type': enums.LinkType.ROLE_REPRESENTATIVE.value
                },
            )
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))
        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'PASSPORT_NOT_LINKED_TO_CLIENT',
                'description': ('Passport %s (%s) is NOT linked to client %s'
                                % (passport.login, passport.passport_id, client.id)),
            }),
        )

    @pytest.mark.parametrize("by_login", [True, False])
    def test_intranet_passport_errors(self, domain_passport, client, by_login):
        # there are some intranet passports linked as representatives to clients (created by hand?)
        # we shouldn't have an opportunity to change them
        rcp = role_client_passport(domain_passport, client)
        params = {
            "client_id": client.id,
            "link_type": enums.LinkType.ROLE_REPRESENTATIVE.value
        }
        if by_login:
            params["login"] = domain_passport.login
        else:
            params["passport_id"] = domain_passport.passport_id

        create_person(client=client, type='ur')

        with mock.patch('butils.passport.PassportBlackbox._call_api_once', return_value=bb_not_found):
            res = self.test_client.secure_post(
                self.BASE_URL,
                params,
            )
        hm.assert_that(res.status_code, hm.equal_to(http.NOT_FOUND if by_login else http.BAD_REQUEST))
        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'PASSPORT_NOT_FOUND_BY_LOGIN' if by_login else 'PASSPORT_IS_INTERNAL',
                'description': 'Passport%s' % (
                    "(login=%s) was not found." % domain_passport.login
                    if by_login else
                    "(%s) is internal." % domain_passport.passport_id
                ),
            }),
        )
