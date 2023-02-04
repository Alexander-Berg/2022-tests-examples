# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import datetime
import http.client as http
import pytest
import hamcrest as hm

from balance import constants as cst

from brest.core.tests import security

from yb_snout_api.utils import get_attrib_by_name
from yb_snout_api.resources import enums
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.order import create_order
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.request import create_request
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client, create_role_client_group
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role


@pytest.fixture(name='view_order_role')
def create_view_order_role():
    return create_role((
        cst.PermissionCode.VIEW_ORDERS,
        {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
    ))


@pytest.fixture(name='view_invoice_role')
def create_view_invoice_role():
    return create_role((
        cst.PermissionCode.VIEW_INVOICES,
        {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
    ))


@pytest.mark.smoke
class TestCaseOrderUntouchedRequests(TestCaseApiAppBase):
    BASE_API = u'/v1/order/untouched-requests'

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'parameters',
        [
            [('order_id', 'id')],
            [('service_id', 'service_id'), ('service_order_id', 'service_order_id')],
            [('service_cc', 'service.cc'), ('service_order_id', 'service_order_id')],
        ],
        ids=['order_id', 'service_id & service_order_id', 'service_cc & service_order_id'],
    )
    def test_get_untouched_request(self, client, parameters, admin_role, view_invoice_role, view_order_role):
        firm_id = cst.FirmId.DRIVE
        client_batch_id = create_role_client(client).client_batch_id
        roles = [
            admin_role,
            view_order_role,
            (view_invoice_role,
             {cst.ConstraintTypes.firm_id: firm_id,
              cst.ConstraintTypes.client_batch_id: client_batch_id}),
        ]
        security.set_roles(roles)

        order = create_order(client=client)
        request = create_request(client=client, order=order, firm_id=firm_id)
        response = self.test_client.get(
            self.BASE_API,
            {key: get_attrib_by_name(order, attr_name) for key, attr_name in parameters},
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')
        data = response.get_json()['data']
        hm.assert_that(data['total_row_count'], hm.equal_to(1))
        hm.assert_that(
            data.get('entry', []),
            hm.contains(hm.has_entries({
                'order_id': order.id,
                'request_id': request.id,
            })),
        )

    @pytest.mark.parametrize(
        'reverse',
        [True, False],
    )
    def test_sorting(self, reverse):
        from yb_snout_api.resources.v1.order.enums import UntouchedRequestsSortKey

        order = create_order()
        now = self.test_session.now()
        requests = []
        for i in range(3):
            request = create_request(client=order.client, order=order)
            request.dt = now - datetime.timedelta(days=i)
            requests.append(request)
        self.test_session.flush()

        response = self.test_client.get(
            self.BASE_API,
            {
                'order_id': order.id,
                'sort_order': enums.SortOrderType.DESC.name if reverse else enums.SortOrderType.ASC.name,
                'sort_key': UntouchedRequestsSortKey.DT.name,
            },
        )

        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        if not reverse:
            requests = requests[::-1]
        data = response.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'total_row_count': 3,
                'entry': hm.contains(*[
                    hm.has_entries({'order_id': order.id, 'request_id': r.id})
                    for r in requests
                ]),
            }),
        )


@pytest.mark.permissions
class TestCaseOrderUntouchedRequestsPermissions(TestCaseApiAppBase):
    BASE_API = u'/v1/order/untouched-requests'

    def test_wo_role(self, admin_role, client, view_order_role):
        security.set_roles([admin_role, view_order_role])

        order = create_order(client=client)
        create_request(client=client, order=order, firm_id=None)
        response = self.test_client.get(
            self.BASE_API,
            {'order_id': order.id},
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data', [])
        hm.assert_that(data['total_row_count'], hm.equal_to(0))

    def test_filtered_by_firm(self, admin_role, client, view_invoice_role, view_order_role):
        roles = [
            admin_role,
            view_order_role,
            (view_invoice_role, {cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO}),
            (view_invoice_role, {cst.ConstraintTypes.firm_id: cst.FirmId.CLOUD}),
        ]
        security.set_roles(roles)

        order = create_order(client=client)
        required_requests = [
            create_request(client=client, order=order, firm_id=None),
            create_request(client=client, order=order, firm_id=cst.FirmId.YANDEX_OOO),
            create_request(client=client, order=order, firm_id=cst.FirmId.CLOUD),
        ]
        create_request(client=client, order=order, firm_id=cst.FirmId.BUS)
        response = self.test_client.get(
            self.BASE_API,
            {'order_id': order.id},
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data', [])
        hm.assert_that(data['total_row_count'], hm.equal_to(len(required_requests)))
        hm.assert_that(
            data['entry'],
            hm.contains_inanyorder(*[
                hm.has_entry('request_id', request.id)
                for request in required_requests
            ]),
        )

    @pytest.mark.parametrize(
        'match_client, firm_id, ans',
        [
            pytest.param(True, None, True, id='request wo firm_id'),
            pytest.param(True, cst.FirmId.YANDEX_OOO, True, id='client and firm are allowed'),
            pytest.param(True, cst.FirmId.CLOUD, False, id='invalid firm_id'),
            pytest.param(False, cst.FirmId.YANDEX_OOO, False, id='invalid client_id'),
        ],
    )
    def test_filtered_by_client_and_firm(
            self,
            admin_role,
            client,
            view_invoice_role,
            view_order_role,
            match_client,
            firm_id,
            ans,
    ):
        role_client = create_role_client(client=client if match_client else None)
        roles = [
            admin_role,
            view_order_role,
            (
                view_invoice_role,
                {
                    cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO,
                    cst.ConstraintTypes.client_batch_id: role_client.client_batch_id,
                },
            ),
        ]
        security.set_roles(roles)

        order = create_order(client=client)
        request = create_request(client=client, order=order, firm_id=firm_id)
        response = self.test_client.get(
            self.BASE_API,
            {'order_id': order.id},
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        data = response.get_json().get('data', [])
        hm.assert_that(data['total_row_count'], hm.equal_to(ans))

        if ans:
            hm.assert_that(
                data['entry'],
                hm.contains(
                    hm.has_entries({
                        'request_id': request.id,
                        'client_id': request.client_id,
                    }),
                ),
            )
