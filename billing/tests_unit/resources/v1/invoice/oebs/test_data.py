# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import decimal

import http.client as http
import pytest
import hamcrest

from balance import constants as cst, muzzle_util as ut

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import (
    create_invoice,
    create_cash_payment_fact,
    create_correction_payment,
)
from yb_snout_api.tests_unit.resources.v1.invoice.oebs.common import (
    create_refundable_cpf,
    create_refundable_payment_cpf,
    create_refundable_trust_cpf,
    create_refund,
)

D = decimal.Decimal


@pytest.fixture(name='view_invoices_roles')
def create_view_invoices_roles(admin_role):
    view_role = create_role((cst.PermissionCode.VIEW_INVOICES, {cst.ConstraintTypes.firm_id: None}))
    refunds_role = create_role((cst.PermissionCode.DO_INVOICE_REFUNDS, {cst.ConstraintTypes.firm_id: None}))
    refunds_trust_role = create_role((cst.PermissionCode.DO_INVOICE_REFUNDS_TRUST, {cst.ConstraintTypes.firm_id: None}))
    return [
        admin_role,
        view_role,
        refunds_role,
        refunds_trust_role,
    ]


class TestOebsData(TestCaseApiAppBase):
    BASE_API = u'/v1/invoice/oebs-data'

    @pytest.mark.smoke
    @pytest.mark.invoice_refunds
    def test_base_list(self, client, view_invoices_roles):
        security.set_roles(view_invoices_roles)
        invoice = create_invoice(client=client)
        cpfs = [
            create_cash_payment_fact(invoice, 10),
            create_cash_payment_fact(invoice, 20),
            create_cash_payment_fact(invoice, 30),
        ]
        response = self.test_client.get(self.BASE_API, {'invoice_id': invoice.id})
        hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        hamcrest.assert_that(
            data,
            hamcrest.has_entries({
                'oebs_nn': {'total_row_count': 0, 'items': []},
                'oebs_factura': {'total_row_count': 0, 'items': []},
                'payments1c': hamcrest.has_entries({
                    'total_row_count': 3,
                    'items': hamcrest.contains_inanyorder(*[
                        hamcrest.has_entries({
                            'cpf_id': cpf.id,
                            'invoice_eid': invoice.external_id,
                            'doc_sum': str(ut.round00(cpf.amount)),
                            'inn': None,
                            'customer_name': None,
                            'bik': None,
                            'account_name': None,
                            'refundable_amount': '0.00',
                            'refunds_num': 0,
                            'editable_refund_requisites': None,
                            'refund_requisites': None,
                        })
                        for cpf in cpfs
                    ]),
                }),
            }),
        )

    @pytest.mark.slow
    @pytest.mark.invoice_refunds
    def test_refundable_bank_instant(self, client, view_invoices_roles):
        security.set_roles(view_invoices_roles)
        invoice = create_invoice(client=client)
        cpf = create_refundable_cpf(invoice, 666)
        ym_cpf, ym_payment = create_refundable_payment_cpf(invoice)

        invoice.create_receipt(D('6.66'))
        response = self.test_client.get(self.BASE_API, {'invoice_id': invoice.id})
        hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        hamcrest.assert_that(
            data,
            hamcrest.has_entries({
                'oebs_nn': {'total_row_count': 0, 'items': []},
                'oebs_factura': {'total_row_count': 0, 'items': []},
                'payments1c': hamcrest.has_entries({
                    'total_row_count': 2,
                    'items': hamcrest.contains_inanyorder(*[
                        hamcrest.has_entries({
                            'cpf_id': ym_cpf.id,
                            'invoice_eid': invoice.external_id,
                            'doc_sum': str(ut.round00(ym_cpf.amount)),
                            'inn': None,
                            'customer_name': None,
                            'bik': None,
                            'account_name': None,
                            'refundable_amount': '6.66',
                            'refunds_num': 0,
                            'editable_refund_requisites': [],
                            'refund_requisites': {
                                'transaction_num': ym_payment.transaction_id,
                                'wallet_num': ym_payment.user_account,
                            },
                        }),
                        hamcrest.has_entries({
                            'cpf_id': cpf.id,
                            'invoice_eid': invoice.external_id,
                            'doc_sum': str(ut.round00(cpf.amount)),
                            'inn': cpf.inn,
                            'customer_name': cpf.customer_name,
                            'bik': cpf.bik,
                            'account_name': cpf.account_name,
                            'refundable_amount': '6.66',
                            'refunds_num': 0,
                            'editable_refund_requisites': [],
                            'refund_requisites': {
                                'inn': cpf.inn,
                                'customer_name': cpf.customer_name,
                                'bik': cpf.bik,
                                'account': cpf.account_name,
                            },
                        }),
                    ]),
                }),
            }),
        )

    @pytest.mark.slow
    @pytest.mark.invoice_refunds
    def test_refundable_trust(self, client, view_invoices_roles):
        security.set_roles(view_invoices_roles)
        invoice = create_invoice(client=client)
        cpf, payment = create_refundable_trust_cpf(invoice)
        invoice.create_receipt(D('6.66'))
        response = self.test_client.get(self.BASE_API, {'invoice_id': invoice.id})
        hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        hamcrest.assert_that(
            data,
            hamcrest.has_entries({
                'oebs_nn': {'total_row_count': 0, 'items': []},
                'oebs_factura': {'total_row_count': 0, 'items': []},
                'payments1c': hamcrest.has_entries({
                    'total_row_count': 1,
                    'items': hamcrest.contains_inanyorder(*[
                        hamcrest.has_entries({
                            'cpf_id': cpf.id,
                            'invoice_eid': invoice.external_id,
                            'doc_sum': str(ut.round00(cpf.amount)),
                            'inn': None,
                            'customer_name': None,
                            'bik': None,
                            'account_name': None,
                            'refundable_amount': '6.66',
                            'refunds_num': 0,
                            'editable_refund_requisites': [],
                            'refund_requisites': {
                                'purchase_token': payment.transaction_id,
                            },
                        }),
                    ]),
                }),
            }),
        )

    @pytest.mark.slow
    @pytest.mark.parametrize('requisite_name', ['user_account', 'transaction_id'])
    def test_refundable_editable_reqs(self, client, requisite_name, view_invoices_roles):
        security.set_roles(view_invoices_roles)
        invoice = create_invoice(client=client)
        p_cpf, payment = create_refundable_payment_cpf(invoice)
        setattr(payment, requisite_name, None)

        invoice.create_receipt(D('6.66'))
        response = self.test_client.get(self.BASE_API, {'invoice_id': invoice.id})
        hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.OK), 'response code must be OK')

        if requisite_name == 'transaction_id':
            editable_reqs = ['transaction_num']
            reqs = {'wallet_num': payment.user_account}
        else:
            editable_reqs = ['wallet_num']
            reqs = {'transaction_num': payment.transaction_id}

        data = response.get_json().get('data')
        hamcrest.assert_that(
            data,
            hamcrest.has_entries({
                'oebs_nn': {'total_row_count': 0, 'items': []},
                'oebs_factura': {'total_row_count': 0, 'items': []},
                'payments1c': hamcrest.has_entries({
                    'total_row_count': 1,
                    'items': hamcrest.contains_inanyorder(*[
                        hamcrest.has_entries({
                            'cpf_id': p_cpf.id,
                            'invoice_eid': invoice.external_id,
                            'doc_sum': str(ut.round00(p_cpf.amount)),
                            'inn': None,
                            'customer_name': None,
                            'bik': None,
                            'account_name': None,
                            'refundable_amount': '6.66',
                            'refunds_num': 0,
                            'editable_refund_requisites': editable_reqs,
                            'refund_requisites': reqs,
                        }),
                    ]),
                }),
            }),
        )

    @pytest.mark.slow
    @pytest.mark.invoice_refunds
    def test_existing_refund(self, client, view_invoices_roles):
        security.set_roles(view_invoices_roles)
        invoice = create_invoice(client=client)
        cpf = create_refundable_cpf(invoice, 100)
        invoice.create_receipt(100)
        create_refund(cpf, 10)
        create_refund(cpf, 20)
        create_refund(cpf, 30)
        response = self.test_client.get(self.BASE_API, {'invoice_id': invoice.id})
        hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        hamcrest.assert_that(
            data,
            hamcrest.has_entries({
                'oebs_nn': {'total_row_count': 0, 'items': []},
                'oebs_factura': {'total_row_count': 0, 'items': []},
                'payments1c': hamcrest.has_entries({
                    'total_row_count': 1,
                    'items': hamcrest.contains(
                        hamcrest.has_entries({
                            'cpf_id': cpf.id,
                            'invoice_eid': invoice.external_id,
                            'doc_sum': '100.00',
                            'refundable_amount': '40.00',
                            'refunds_num': 3,
                        }),
                    ),
                }),
            }),
        )

    @pytest.mark.slow
    def test_two_payments(self, client, view_invoices_roles):
        '''
        https://st.yandex-team.ru/BALANCE-28744
        Проверяем, что distinct не применяется к xreport по платежам
        '''
        security.set_roles(view_invoices_roles)
        invoice = create_invoice(client=client)
        invoice.create_receipt(100)
        create_correction_payment(invoice)
        create_correction_payment(invoice)
        response = self.test_client.get(self.BASE_API, {'invoice_id': invoice.id})
        hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        hamcrest.assert_that(
            data,
            hamcrest.has_entries({
                'oebs_nn': {'total_row_count': 0, 'items': []},
                'oebs_factura': {'total_row_count': 0, 'items': []},
                'payments1c': hamcrest.has_entries({
                    'total_row_count': 2,
                    'items': hamcrest.contains(
                        hamcrest.has_entries({
                            'cpf_id': None,
                            'invoice_eid': invoice.external_id,
                            'doc_sum': '3000.00',
                        }),
                        hamcrest.has_entries({
                            'cpf_id': None,
                            'invoice_eid': invoice.external_id,
                            'doc_sum': '3000.00',
                        }),
                    ),
                }),
            }),
        )
