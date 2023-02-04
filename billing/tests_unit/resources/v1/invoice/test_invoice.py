# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future.builtins import str as text
from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
    has_entry,
    contains_inanyorder,
    is_,
)

from balance import constants as cst
from balance import mapper
from balance import muzzle_util as ut
from tests import object_builder as ob

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_agency, create_client, create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, get_client_role, get_agency_role


@pytest.mark.smoke
class TestCaseInvoice(TestCaseApiAppBase):
    BASE_API = '/v1/invoice'

    def test_invoice_get_own(self, invoice):
        self.test_session.expire_all()  # expire consumes

        security.set_passport_client(invoice.client)
        response = self.test_client.get(self.BASE_API, {'invoice_id': invoice.id})

        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        assert_that(invoice.consumes_loaded, is_(False), 'Consumes were loaded!')

    def test_not_found(self, not_existing_id):
        response = self.test_client.get(self.BASE_API, {'invoice_id': not_existing_id})
        assert_that(response.status_code, equal_to(http.NOT_FOUND), 'Response code must be 404(NOT_FOUND)')

    @pytest.mark.charge_note_register
    def test_register_rows(self):
        main_invoice = create_invoice()
        invoices = [create_invoice(main_invoice.client) for _ in range(2)]

        main_invoice.register_rows = [
            mapper.InvoiceRegisterRow(
                ref_invoice=i,
                amount=i.amount,
                amount_nds=i.amount_nds,
                amount_nsp=i.amount_nsp,
            )
            for i in invoices
        ]
        self.test_session.flush()

        response = self.test_client.get(self.BASE_API, {'invoice_id': main_invoice.id})
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        assert_that(
            response.get_json()['data'],
            has_entries({
                'register-rows': contains_inanyorder(*[
                    has_entries({
                        'ref-invoice': has_entry('id', str(i.id)),
                        'amount': str(ut.round00(i.amount.as_decimal())),
                    })
                    for i in invoices
                ]),
            }),
        )


@pytest.mark.permissions
@pytest.mark.smoke
class TestCaseInvoiceAccess(TestCaseApiAppBase):
    BASE_API = '/v1/invoice'

    def test_client_owns_invoice(self, invoice, client_role):
        security.set_passport_client(invoice.client)
        security.set_roles([client_role])
        response = self.test_client.get(self.BASE_API, {'invoice_id': invoice.id}, is_admin=False)
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        assert_that(response.get_json()['data'], has_entry('id', text(invoice.id)))

    def test_alien_invoice(self, client_role):
        """Чужой счёт не показываем"""
        security.set_roles([client_role])
        invoice = create_invoice()
        response = self.test_client.get(self.BASE_API, {'invoice_id': invoice.id}, is_admin=False)
        assert_that(response.status_code, equal_to(http.FORBIDDEN), 'response code must be FORBIDDEN')
        assert_that(
            response.get_json(),
            has_entries({
                'error': 'PERMISSION_DENIED',
                'description': 'User %s has no permission ViewInvoices.' % self.test_session.oper_id,
            }),
        )

    @pytest.mark.parametrize(
        'firm_id, resp_code',
        [
            (cst.FirmId.MARKET, http.FORBIDDEN),
            (cst.FirmId.YANDEX_OOO, http.OK),
        ],
        ids=['FORBIDDEN', 'OK'],
    )
    def test_ui_firm_constraint(self, admin_role, firm_id, resp_code):
        session = self.test_session
        invoice_firm_id = cst.FirmId.YANDEX_OOO

        role = ob.create_role(session, (cst.PermissionCode.VIEW_INVOICES, {cst.ConstraintTypes.firm_id: None}))
        security.set_roles([admin_role, (role, {cst.ConstraintTypes.firm_id: firm_id})])

        invoice = create_invoice(firm_id=invoice_firm_id)
        response = self.test_client.get(self.BASE_API, {'invoice_id': invoice.id})

        assert_that(response.status_code, equal_to(resp_code))
        if resp_code == http.FORBIDDEN:
            assert_that(
                response.get_json(),
                has_entries({
                    'error': 'PERMISSION_DENIED',
                    'description': 'User {} has no permission ViewInvoices.'.format(session.passport.passport_id),
                }),
            )

    @pytest.mark.parametrize(
        'is_allowed',
        [True, False],
    )
    def test_ui_client_constraint(
            self,
            client,
            admin_role,
            is_allowed,
    ):
        session = self.test_session
        firm_id = cst.FirmId.DRIVE

        role = ob.create_role(
            session,
            (
                cst.PermissionCode.VIEW_INVOICES,
                {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
            ),
        )
        client_batch_id = create_role_client(client if is_allowed else None).client_batch_id
        roles = [
            admin_role,
            (role, {cst.ConstraintTypes.firm_id: firm_id,
                    cst.ConstraintTypes.client_batch_id: client_batch_id}),
        ]

        security.set_roles(roles)

        invoice = create_invoice(client=client, firm_id=firm_id)
        response = self.test_client.get(self.BASE_API, {'invoice_id': invoice.id})

        assert_that(response.status_code, equal_to(http.OK if is_allowed else http.FORBIDDEN))
        if not is_allowed:
            assert_that(
                response.get_json(),
                has_entries({
                    'error': 'PERMISSION_DENIED',
                    'description': 'User {} has no permission ViewInvoices.'.format(session.passport.passport_id),
                }),
            )
