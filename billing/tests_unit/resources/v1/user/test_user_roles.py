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

from balance import constants as cst, mapper
from tests import object_builder as ob
from tests.tutils import has_exact_entries

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.resource import mock_client_resource
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import (
    create_client,
    create_role_client_group,
    create_role_client,
    create_manager,
)
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.user import create_passport
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.correction_template_group import create_correction_template_group


@pytest.fixture(name='view_roles_role')
def create_view_roles_role():
    return create_role(cst.PermissionCode.VIEW_ROLES)


@pytest.fixture(name='user_role')
def create_user_role():
    return create_role(
        (
            'UserPermission',
            {
                cst.ConstraintTypes.firm_id: None,
                cst.ConstraintTypes.client_batch_id: None,
                cst.ConstraintTypes.template_group_id: None,
                cst.ConstraintTypes.manager: True,
            },
        ),
        name='user_role',
    )


@pytest.fixture(name='group_role')
def create_group_role():
    return create_role(
        (
            'GroupPermission',
            {
                cst.ConstraintTypes.firm_id: None,
                cst.ConstraintTypes.client_batch_id: None,
                cst.ConstraintTypes.template_group_id: None,
            },
        ),
        name='group_role',
    )


class TestCaseUserRoles(TestCaseApiAppBase):
    BASE_API = '/v1/user/roles'

    def test_base(
        self,
        user_role,
        group_role,
        correction_template_group,
    ):
        session = self.test_session
        domain_passport = create_passport(passport_id=ob.get_domain_uid_value())
        passport = create_passport()
        passport.master = domain_passport

        client = create_client()
        repr_client = create_client()
        passport.client = client
        ob.set_repr_client(self.test_session, passport, repr_client)

        client_1 = create_client()
        clients = [create_client() for _i in range(3)]

        session.add(mapper.PassportGroup(passport=passport, group_id=666))
        session.add(mapper.RoleGroup(session, group_id=666, role_id=group_role.id))
        session.add(
            mapper.RoleGroup(
                session,
                group_id=666,
                role_id=group_role.id,
                firm_id=1,
                template_group_id=correction_template_group.id,
            ),
        )
        session.add(
            mapper.RoleGroup(
                session,
                group_id=666,
                role_id=group_role.id,
                firm_id=111,
                client_batch_id=create_role_client_group(clients).client_batch_id,
            ),
        )
        session.flush()

        roles = [
            (user_role, {}),
            (user_role, {cst.ConstraintTypes.firm_id: 1}),
            (user_role, {cst.ConstraintTypes.firm_id: 111}),
            (
                user_role,
                {
                    cst.ConstraintTypes.firm_id: 111,
                    cst.ConstraintTypes.client_batch_id: create_role_client(client=client_1).client_batch_id,
                    cst.ConstraintTypes.template_group_id: correction_template_group.id,
                },
            ),
        ]
        with mock.patch('butils.passport.passport_admsubscribe'):  # не ходим в апи паспорта
            passport.set_roles(roles)

        res = self.test_client.get(
            self.BASE_API,
            {'passport_id': passport.passport_id},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            has_exact_entries({
                'passport': has_exact_entries({
                    'login': passport.login,
                    'passport_id': passport.passport_id,
                    'master': has_exact_entries({
                        'login': domain_passport.login,
                        'passport_id': domain_passport.passport_id,
                    }),
                    'name': passport.gecos,
                    'email': passport.email,
                    'is_main': False,
                    'dead': False,
                    'client': has_exact_entries({'id': client.id, 'name': client.name}),
                    'repr_client': has_exact_entries({'id': repr_client.id, 'name': repr_client.name}),
                }),
                'roles': hm.contains_inanyorder(
                    has_exact_entries({
                        'id': group_role.id,
                        'name': 'group_role',
                        'permissions': hm.contains(
                            has_exact_entries({
                                'id': hm.not_none(),
                                'code': 'GroupPermission',
                                'name': None,
                                'iface_place': None,
                                'iface_item': None,
                            }),
                        ),
                    }),
                    has_exact_entries({
                        'id': user_role.id,
                        'name': 'user_role',
                        'permissions': hm.contains(
                            has_exact_entries({
                                'id': hm.not_none(),
                                'code': 'UserPermission',
                                'name': None,
                                'iface_place': None,
                                'iface_item': None,
                            }),
                        ),
                    }),
                ),
                'passport_roles': hm.contains_inanyorder(
                    has_exact_entries({
                        'group_id': None,
                        'role_id': user_role.id,
                        'create_dt': hm.not_none(),
                        'firm_id': None,
                        'template_group_id': None,
                        'client_ids': hm.empty(),
                    }),
                    has_exact_entries({
                        'group_id': None,
                        'role_id': user_role.id,
                        'create_dt': hm.not_none(),
                        'firm_id': 1,
                        'template_group_id': None,
                        'client_ids': hm.empty(),
                    }),
                    has_exact_entries({
                        'group_id': None,
                        'role_id': user_role.id,
                        'create_dt': hm.not_none(),
                        'firm_id': 111,
                        'template_group_id': None,
                        'client_ids': hm.empty(),
                    }),
                    has_exact_entries({
                        'group_id': None,
                        'role_id': user_role.id,
                        'create_dt': hm.not_none(),
                        'firm_id': 111,
                        'template_group_id': correction_template_group.id,
                        'client_ids': hm.contains(client_1.id),
                    }),
                    has_exact_entries({
                        'group_id': 666,
                        'role_id': group_role.id,
                        'create_dt': hm.not_none(),
                        'firm_id': None,
                        'template_group_id': None,
                        'client_ids': hm.empty(),
                    }),
                    has_exact_entries({
                        'group_id': 666,
                        'role_id': group_role.id,
                        'create_dt': hm.not_none(),
                        'firm_id': 1,
                        'template_group_id': correction_template_group.id,
                        'client_ids': hm.empty(),
                    }),
                    has_exact_entries({
                        'group_id': 666,
                        'role_id': group_role.id,
                        'create_dt': hm.not_none(),
                        'firm_id': 111,
                        'template_group_id': None,
                        'client_ids': hm.contains_inanyorder(*list(map(lambda x: x.id, clients))),
                    }),
                ),
            }),
        )

    @pytest.mark.permissions
    @pytest.mark.parametrize('w_role', [True, False], ids=['w_role', 'wo_role'])
    @pytest.mark.parametrize('is_admin', [True, False], ids=['admin_ui', 'client_ui'])
    @mock_client_resource('yb_snout_api.resources.v1.user.routes.roles.UserRoles')
    def test_permission(
        self,
        w_role,
        is_admin,
        admin_role,
        view_roles_role,
        passport,
    ):
        roles = [admin_role]
        if w_role:
            roles.append(view_roles_role)
        security.set_roles(roles)

        res = self.test_client.get(
            self.BASE_API,
            {'passport_id': passport.passport_id},
            is_admin=is_admin,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK if w_role else http.FORBIDDEN))
        if w_role:
            hm.assert_that(
                res.get_json()['data'].get('passport', {}),
                hm.has_entries({'passport_id': passport.passport_id}),
            )

    @pytest.mark.permissions
    @pytest.mark.parametrize('w_role', [True, False], ids=['w_role', 'wo_role'])
    def test_own_passport(self, w_role, admin_role, view_roles_role):
        roles = [admin_role]
        if w_role:
            roles.append(view_roles_role)
        security.set_roles(roles)

        res = self.test_client.get(self.BASE_API)
        hm.assert_that(res.status_code, hm.equal_to(http.OK if w_role else http.FORBIDDEN))
        if w_role:
            hm.assert_that(
                res.get_json()['data'].get('passport', {}),
                hm.has_entries({'passport_id': self.test_session.passport.passport_id}),
            )
