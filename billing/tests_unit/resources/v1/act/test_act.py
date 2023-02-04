# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
from hamcrest import assert_that, equal_to, has_entry, has_entries

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.act import create_act
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, get_client_role, create_role


@pytest.fixture(name='view_inv_role')
def create_view_inv_role():
    return create_role(
        (
            cst.PermissionCode.VIEW_INVOICES,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.mark.smoke
class TestCaseAct(TestCaseApiAppBase):
    BASE_API = '/v1/act'

    def test_get_act(self, act):
        response = self.test_client.get(self.BASE_API, {'act_id': act.id})
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        assert_that(data.get('id'), equal_to(act.id), 'got wrong act')

    def test_get_act_by_eid(self, act):
        response = self.test_client.get(self.BASE_API, {'act_eid': act.external_id})
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        assert_that(data.get('id'), equal_to(act.id), 'got wrong act')

    def test_get_act_fail(self):
        response = self.test_client.get(self.BASE_API)
        assert_that(response.status_code, equal_to(http.BAD_REQUEST), 'response code must be BAD_REQUEST')

    @pytest.mark.parametrize("id_field", ['act_id', 'act_eid'])
    def test_not_found(self, id_field, not_existing_id):
        response = self.test_client.get(self.BASE_API, {id_field: not_existing_id})
        assert_that(response.status_code, equal_to(http.NOT_FOUND), 'Response code must be 404(NOT_FOUND)')


@pytest.mark.smoke
@pytest.mark.permissions
class TestActAccess(TestCaseApiAppBase):
    BASE_API = '/v1/act'

    @pytest.mark.parametrize(
        'role_firm_id, act_firm_id, res',
        [
            (None, cst.FirmId.YANDEX_OOO, http.OK),
            (cst.FirmId.YANDEX_OOO, cst.FirmId.YANDEX_OOO, http.OK),
            (cst.FirmId.DRIVE, cst.FirmId.YANDEX_OOO, http.FORBIDDEN),
        ],
        ids=[
            'without firm_id constraint',
            'right firm_id',
            'wrong firm_id',
        ],
    )
    def test_firm_constraints(self, admin_role, view_inv_role, role_firm_id, act_firm_id, res):
        """Проверяем подходят ли права пользователя для просмотра конкретного акта"""
        roles = [
            admin_role,
            (view_inv_role, {cst.ConstraintTypes.firm_id: role_firm_id}),
        ]
        security.set_roles(roles)
        act = create_act(firm_id=act_firm_id)
        response = self.test_client.get(self.BASE_API, {'act_id': act.id})
        assert_that(response.status_code, equal_to(res))

    def test_wo_role(self, admin_role, client):
        """У пользователя нет права просматривать счета"""
        security.set_roles([admin_role])
        act = create_act(client=client)
        response = self.test_client.get(self.BASE_API, {'act_id': act.id})
        assert_that(response.status_code, equal_to(http.FORBIDDEN), 'response code must be FORBIDDEN')

    def test_owns_act(self, client, client_role):
        """Клиент владеет счетом"""
        security.set_passport_client(client)
        security.set_roles([client_role])
        act = create_act(client=client)
        response = self.test_client.get(self.BASE_API, {'act_id': act.id}, is_admin=False)
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

    def test_alien_act(self, client, client_role):
        """Клиент пытаеся запросить чужой счет"""
        security.set_roles([client_role])
        act = create_act(client=client)
        response = self.test_client.get(self.BASE_API, {'act_id': act.id}, is_admin=False)
        assert_that(response.status_code, equal_to(http.FORBIDDEN), 'response code must be FORBIDDEN')

    @pytest.mark.parametrize(
        'is_allowed',
        [True, False],
    )
    def test_ui_client_constraint(
            self,
            client,
            admin_role,
            view_inv_role,
            is_allowed,
    ):
        session = self.test_session
        firm_id = cst.FirmId.DRIVE

        client_batch_id = create_role_client(client if is_allowed else None).client_batch_id
        roles = [
            admin_role,
            (view_inv_role, {cst.ConstraintTypes.firm_id: firm_id,
                             cst.ConstraintTypes.client_batch_id: client_batch_id}),
        ]

        security.set_roles(roles)

        act = create_act(client=client, firm_id=firm_id)
        response = self.test_client.get(self.BASE_API, {'act_id': act.id})

        assert_that(response.status_code, equal_to(http.OK if is_allowed else http.FORBIDDEN))
        if is_allowed:
            res_match = has_entry('data', has_entry('id', act.id))
        else:
            res_match = has_entries({
                'error': 'PERMISSION_DENIED',
                'description': 'User {} has no permission ViewInvoices.'.format(session.passport.passport_id),
            })
        assert_that(
            response.get_json(),
            res_match,
        )
