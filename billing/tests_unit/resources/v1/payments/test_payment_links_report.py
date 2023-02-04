# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import http.client as http
import hamcrest as hm
from decimal import Decimal as D

from balance import constants as cst
from tests import object_builder as ob

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.client import create_client
from yb_snout_api.tests_unit.fixtures.invoice import create_custom_invoice
from yb_snout_api.tests_unit.fixtures.payments import create_trust_payment
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role


@pytest.fixture(name='links_report_role')
def create_links_report_role():
    return create_role(
        (
            cst.PermissionCode.PAYMENT_LINKS_REPORT,
            {},
        ),
    )


class TestPaymentLinksReport(TestCaseApiAppBase):
    BASE_API = '/v1/payments/links-report'

    def _create_payment(self):
        client = create_client()
        order = ob.OrderBuilder.construct(self.test_session, client=client)
        invoice = create_custom_invoice({order: D('99.666')}, client=client)
        payment = create_trust_payment(invoice=invoice)
        return order, payment

    @pytest.mark.smoke
    def test_get_links_report(self):
        order, _payment = self._create_payment()

        res = self.test_client.get(
            self.BASE_API,
            {
                'service_order_id_str': order.service_order_id_str,
                'show_totals': True,
                'iso_currency': 'RUB',
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'total_row_count': 1,
                'gtotals': hm.has_entries({'row_count': 1, 'amount': '9966.60'}),
                'totals': hm.has_entries({'row_count': 1, 'amount': '9966.60'}),
                'items': hm.contains(
                    hm.has_entries({
                        'trust_payment_id': None,
                        'link_dt': hm.not_none(),
                        'payment_status': '0',
                        'link_id': None,
                        'amount': '9966.60',
                        'status_dt': None,
                        'service_order_id_str': order.service_order_id_str,
                        'service_id': cst.ServiceId.DIRECT,
                        'iso_currency': 'RUB',
                        'external_id': None,
                    }),
                ),
            }),
        )

    def test_wo_currency(self):
        """Без валюты не можем посчитать total
        """
        order, payment = self._create_payment()
        res = self.test_client.get(
            self.BASE_API,
            {
                'service_order_id_str': order.service_order_id_str,
                'show_totals': True,
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'total_row_count': 1,
                'gtotals': None,
                'totals': None,
            }),
        )

    @pytest.mark.parametrize(
        'w_role',
        [True, False],
    )
    def test_wo_role(self, admin_role, links_report_role, w_role):
        """Заменяется service_id, так что без него не сможем найти
        """
        roles = [admin_role]
        if w_role:
            roles.append(links_report_role)
        security.set_roles(roles)

        order, payment = self._create_payment()
        res = self.test_client.get(
            self.BASE_API,
            {'service_order_id_str': order.service_order_id_str},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'total_row_count': 1 if w_role else 0,
            }),
        )
