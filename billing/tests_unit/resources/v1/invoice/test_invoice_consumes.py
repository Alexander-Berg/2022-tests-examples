# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library
from future.builtins import str as text

standard_library.install_aliases()

import http.client as http
import pytest
import hamcrest as hm

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.resources.enums import SortOrderType
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role


@pytest.fixture(name='view_invoices_role')
def create_view_invoices_role():
    return create_role((cst.PermissionCode.VIEW_INVOICES, {cst.ConstraintTypes.firm_id: None}))


@pytest.mark.smoke
@pytest.mark.permissions
class TestInvoiceConsumes(TestCaseApiAppBase):
    BASE_API = u'/v1/invoice/consumes'

    @pytest.mark.parametrize(
        'pagination_pn, pagination_ps, reverse_sort',
        [
            pytest.param(1, 6, 0, id='all'),
            pytest.param(1, 3, 0, id='start'),
            pytest.param(2, 3, 0, id='end'),
            pytest.param(2, 2, 0, id='middle'),
            pytest.param(1, 6, 1, id='reversed'),
        ],
    )
    def test_admin_get(
            self,
            admin_role,
            view_invoices_role,
            pagination_pn,
            pagination_ps,
            reverse_sort,
    ):
        from yb_snout_api.resources.v1.invoice import enums

        security.set_roles([admin_role, view_invoices_role])
        invoice = create_invoice(order_count=6)
        invoice.turn_on_rows()
        res = self.test_client.get(
            self.BASE_API,
            params={
                'invoice_id': invoice.id,
                'consumes_pn': pagination_pn,
                'consumes_ps': pagination_ps,
                'sort_key': enums.ConsumesSortKeyType.ORDER_ID.name,
                'sort_order': SortOrderType.DESC.name if reverse_sort else SortOrderType.ASC.name,
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json().get('data', {})

        offset = (pagination_pn - 1) * pagination_ps
        limit = offset + pagination_ps
        consumes = sorted(
            invoice.consumes[offset:limit],
            key=lambda co_: co_.order.id,
            reverse=reverse_sort,
        )

        hm.assert_that(
            data,
            hm.has_entries(
                consumes_list=hm.contains(*[
                    hm.has_entries(
                        id=text(co.id),
                        order=hm.has_entries(id=text(co.order.id)),
                    )
                    for co in consumes
                ]),
                invoice_currency_rate=text(invoice.currency_rate),
                invoice_internal_rate=text(invoice.currency_rate) + 'RUB/FISH',
                pagination_pn=pagination_pn,
                pagination_ps=pagination_ps,
                pagination_sz=6,
            ),
        )

    def test_forbidden(self, admin_role, view_invoices_role):
        roles = [
            admin_role,
            (view_invoices_role, {cst.ConstraintTypes.firm_id: cst.FirmId.MARKET}),
        ]
        security.set_roles(roles)
        invoice = create_invoice(firm_id=cst.FirmId.YANDEX_OOO)
        invoice.turn_on_rows()
        res = self.test_client.get(self.BASE_API, params={'invoice_id': invoice.id})
        hm.assert_that(res.status_code, hm.equal_to(http.FORBIDDEN))
