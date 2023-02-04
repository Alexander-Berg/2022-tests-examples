# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import mock
import hamcrest as hm
import http.client as http

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.utils import context_managers as ctx_util, clean_dict
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_role_client, create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.order import create_order, create_order_w_firm


@pytest.fixture(name='notify_role')
def create_notify_role():
    return create_role((
        cst.PermissionCode.NOTIFY_ORDER,
        {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
    ))


def get_notification_info(session, order_id):
    sql = """select * from t_object_notification where opcode=:opcode and object_id=:object_id"""
    return session.execute(sql, {'opcode': cst.NOTIFY_ORDER_OPCODE, 'object_id': order_id}).fetchone()


class TestCaseNotifyOrder(TestCaseApiAppBase):
    BASE_API = u'/v1/order/notify'

    @staticmethod
    def _get_roles(role, role_firm_ids, role_client, match_client):
        if role_firm_ids is cst.SENTINEL:
            return []

        if match_client is None:
            client_batch_id = None
        else:
            client_batch_id = role_client.client_batch_id if match_client else create_role_client().client_batch_id
        return [
            (
                role,
                clean_dict({cst.ConstraintTypes.firm_id: firm_id, cst.ConstraintTypes.client_batch_id: client_batch_id}),
            )
            for firm_id in role_firm_ids
        ]

    def test_notify_order(self, client, admin_role, notify_role):
        security.set_roles([admin_role, notify_role])
        order = create_order(client=client)
        response = self.test_client.secure_post(
            self.BASE_API,
            data={'order_id': order.id},
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))
        notification = get_notification_info(self.test_session, order.id)
        hm.assert_that(
            notification,
            hm.has_properties({
                'opcode': cst.NOTIFY_ORDER_OPCODE,
                'object_id': order.id,
                'last_scn': 0,
            }),
        )

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'match_client, firm_ids, role_firm_ids, ans',
        [
            pytest.param(False, [None], cst.SENTINEL,
                         http.FORBIDDEN, id='wo perm'),
            pytest.param(None, [None], [None],
                         http.OK, id='wo constraints'),
            pytest.param(True, [cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD], [cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD],
                         http.OK, id='w right client w right firm'),
            pytest.param(True, [cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD], [None],
                         http.OK, id='w right client wo role_firm'),
            pytest.param(True, [None], [cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD],
                         http.OK, id='w right client wo order_firm'),
            pytest.param(True, [cst.FirmId.YANDEX_OOO], [cst.FirmId.DRIVE],
                         http.FORBIDDEN, id='w right client w wrong role_firm'),
            pytest.param(True, [cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD], [cst.FirmId.YANDEX_OOO, cst.FirmId.DRIVE],
                         http.FORBIDDEN, id='w right client w wrong role_firm 2'),
            pytest.param(True, [cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD], [cst.FirmId.YANDEX_OOO],
                         http.FORBIDDEN, id='w right client w 1 role_firm'),
            pytest.param(None, [cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD], [cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD],
                         http.OK, id='wo client w right firm'),
            pytest.param(False, [cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD], [cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD],
                         http.FORBIDDEN, id='w wrong client w right firm'),
        ],
    )
    @mock.patch('yb_snout_api.utils.context_managers._new_transactional_session', ctx_util.new_rollback_session)
    def test_permission(
            self,
            admin_role,
            notify_role,
            role_client,
            match_client,
            firm_ids,
            role_firm_ids,
            ans,
    ):
        roles = self._get_roles(notify_role, role_firm_ids, role_client, match_client)
        roles.append(admin_role)
        security.set_roles(roles)

        order = create_order_w_firm(firm_ids=firm_ids, client=role_client.client)
        response = self.test_client.secure_post(
            self.BASE_API,
            data={'order_id': order.id},
        )
        hm.assert_that(response.status_code, hm.equal_to(ans))
