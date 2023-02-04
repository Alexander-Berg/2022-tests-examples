# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import balance.mapper as mapper
import http.client as http
import pytest
from hamcrest import assert_that, equal_to, has_entries, contains

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.utils import clean_dict
from yb_snout_api.utils import get_attrib_by_name, clean_dict
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role, get_client_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_role_client, create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.resource import mock_client_resource
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.order import create_order, not_existing_order_id, create_order_w_firm, consume_order


@pytest.fixture(name='view_order_role')
def create_view_order_role():
    return create_role((
        cst.PermissionCode.VIEW_ORDERS,
        {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
    ))


@pytest.mark.smoke
class TestCaseOrder(TestCaseApiAppBase):
    BASE_API = u'/v1/order'

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'service_param',
        ['service.cc', 'service_id'],
    )
    def test_get_order(self, client, service_param, admin_role):
        security.set_passport_client(client)
        security.set_roles([admin_role])

        order = create_order(client=client)
        url_params = [('service_order_id', order.service_order_id)]
        url_params.append(
            (service_param.replace('.', '_'), get_attrib_by_name(order, service_param)),
        )

        response = self.test_client.get(self.BASE_API, dict(url_params))
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        assert_that(response.get_json()['data'], has_entries({'id': order.id}))

    def test_fail_get_order_by_service_id(self, order):
        response = self.test_client.get(self.BASE_API, {'service_id': order.service_id})
        assert_that(
            response.status_code,
            equal_to(http.INTERNAL_SERVER_ERROR),
            'required service_id and service_order_id',
        )

    def test_fail_get_order_by_service_order_id(self, order):
        order = self.test_session.query(mapper.Order).getone(order.id)

        response = self.test_client.get(self.BASE_API, {'service_order_id': order.service_order_id})
        assert_that(
            response.status_code,
            equal_to(http.INTERNAL_SERVER_ERROR),
            'required service_id and service_order_id',
        )

    def test_get_order_by_id(self, order):
        response = self.test_client.get(self.BASE_API, {'order_id': order.id})
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        assert_that(response.get_json()['data'], has_entries({'id': order.id}))

    def test_not_found(self, not_existing_id):
        response = self.test_client.get(self.BASE_API, {'order_id': not_existing_id})
        assert_that(response.status_code, equal_to(http.NOT_FOUND))

        data = response.get_json()
        assert_that(
            data,
            has_entries({
                'error': 'ORDER_NOT_FOUND',
                'description': 'Order 0-%s not found in DB' % not_existing_id,
                'tanker_context': has_entries({'object-id': not_existing_id}),
            }),
        )

    @pytest.mark.parametrize(
        'pn, ps, show_totals, boundaries',
        [
            pytest.param(1, 4, 1, (0, 4), id='first page'),
            pytest.param(1, 4, 0, (0, 4), id='first page wo totals'),
            pytest.param(2, 3, 1, (3, 6), id='second page'),
            pytest.param(2, 3, 1, (3, 6), id='last uncompleted page'),
            pytest.param(None, None, 1, (0, 11), id='all'),
            pytest.param(None, None, 0, (0, 11), id='all wo totals'),
        ],
    )
    def test_consume_pagination(self, client, pn, ps, show_totals, boundaries):
        order = create_order(client=client)
        co, = order.consumes

        consumes = [co]
        for _i in range(10):
            consumes.append(consume_order(order, 1))

        response = self.test_client.get(
            self.BASE_API,
            clean_dict({
                'order_id': order.id,
                'consumes_pn': pn,
                'consumes_ps': ps,
                'show_totals': show_totals,
            }),
        )
        assert_that(response.status_code, equal_to(http.OK))

        data = response.get_json()['data']

        from_, to_ = boundaries
        reverse_ = bool(ps)
        assert_that(
            data,
            has_entries({
                'consumes_pn': pn or 1,
                'consumes_ps': ps or 11,
                'consumes': has_entries({
                    'consumes_count': 11 if show_totals else None,
                    'current_qty': '11.000000' if show_totals else None,
                    'current_sum': '1100.00' if show_totals else None,
                    'consumes_list': contains(*[
                        has_entries({
                            'id': co.id,
                        })
                        for co in sorted(consumes, key=lambda c: (c.id, c.dt), reverse=reverse_)[from_:to_]
                    ]),
                }),
            }),
        )


@pytest.mark.permissions
@pytest.mark.smoke
class TestCaseOrderPermissions(TestCaseApiAppBase):
    BASE_API = '/v1/order'

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

    def test_nobody(self, client, admin_role):
        security.set_roles([admin_role])
        order = create_order(client=client)
        response = self.test_client.get(self.BASE_API, {'order_id': order.id})
        assert_that(response.status_code, equal_to(http.FORBIDDEN))

    @mock_client_resource('yb_snout_api.resources.v1.order.routes.order.Order')
    def test_owns_by_client(self, client, client_role):
        security.set_passport_client(client)
        security.set_roles([client_role])
        order = create_order(client=client)
        response = self.test_client.get(self.BASE_API, {'order_id': order.id}, is_admin=False)
        assert_that(response.status_code, equal_to(http.OK))

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
    def test_permission(
            self,
            admin_role,
            view_order_role,
            role_client,
            match_client,
            firm_ids,
            role_firm_ids,
            ans,
    ):
        roles = self._get_roles(view_order_role, role_firm_ids, role_client, match_client)
        roles.append(admin_role)
        security.set_roles(roles)

        order = create_order_w_firm(firm_ids=firm_ids, client=role_client.client)
        response = self.test_client.get(self.BASE_API, {'order_id': order.id})
        assert_that(response.status_code, equal_to(ans))
