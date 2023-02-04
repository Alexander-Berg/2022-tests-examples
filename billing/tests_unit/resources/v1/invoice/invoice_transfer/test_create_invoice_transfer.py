# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import datetime
from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
import mock
from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
    has_entry,
    contains,
)

from balance.constants import (
    InvoiceTransferStatus,
    OebsOperationType,
    PermissionCode,
)

from brest.core.tests import security

from yb_snout_api.utils import context_managers as ctx_util
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.person import create_person
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_cash_payment_fact, create_invoice, create_person
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_do_inv_trans_role


@pytest.mark.smoke
class TestCreateInvoiceTransfer(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/create-invoice-transfer'

    @pytest.mark.parametrize(
        'all_money, expected_amount, expected_new_available_sum',
        [
            (True, '100.00', '0.00'),
            (False, '50.00', '50.00')
        ]
    )
    def test_create(self, all_money, expected_amount, expected_new_available_sum):
        src_invoice = create_invoice()
        create_cash_payment_fact(src_invoice, 100, OebsOperationType.INSERT)
        src_invoice.manual_turn_on(100)
        dst_invoice = create_invoice(src_invoice.client, person=src_invoice.person)

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'src_invoice_external_id': src_invoice.external_id,
                'dst_invoice_external_id': dst_invoice.external_id,
                'amount': '50',
                'all_money': all_money
            },
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        invoice_transfer = src_invoice.dst_invoice_transfers[0]
        invoice_transfer_dst = dst_invoice.src_invoice_transfers[0]

        assert invoice_transfer == invoice_transfer_dst

        assert_that(
            response.get_json()['data'],
            has_entries(
                {
                    "invoice_transfer":
                        has_entries(
                            {
                                "id": invoice_transfer.id,
                                "src_invoice": has_entries({
                                    "id": src_invoice.id,
                                    "external_id": src_invoice.external_id
                                }),
                                "dst_invoice": has_entries({
                                    "id": dst_invoice.id,
                                    "external_id": dst_invoice.external_id
                                }),
                                "amount": expected_amount,
                                "status": InvoiceTransferStatus.not_exported,
                                "unlock_allowed": False
                            }
                        ),
                    "available_invoice_transfer_sum": expected_new_available_sum
                }
            ),
        )

    def test_fail_person(self, person):
        src_invoice = create_invoice(person=person)
        create_cash_payment_fact(src_invoice, 100, OebsOperationType.INSERT)
        src_invoice.manual_turn_on(100)
        dst_person = create_person(name='Dst Pupkin Snout Vasya')
        dst_invoice = create_invoice(person=dst_person)

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'src_invoice_external_id': src_invoice.external_id,
                'dst_invoice_external_id': dst_invoice.external_id,
                'amount': '100',
            },
        )
        assert_that(response.status_code, equal_to(http.BAD_REQUEST), 'response code must be 400')

        assert_that(
            response.get_json(),
            has_entries({
                'error': 'INVOICE_TRANSFER_FORBIDDEN_DIFFERENT_PERSONS',
                'tanker_context': has_entries({
                    'src-person-id': person.id,
                    'src-person-name': 'Pupkin Snout Vasya',
                    'dst-person-id': dst_person.id,
                    'dst-person-name': 'Dst Pupkin Snout Vasya',
                }),
            }),
        )

    @pytest.mark.parametrize('from_ai', (True, False))
    @pytest.mark.parametrize('person_type', ('ur', 'byu', 'kzu'))
    def test_legal_persons_different_clients(self, person_type, from_ai):
        client1, client2 = create_client(), create_client()
        person1, person2 = create_person(client1, type=person_type), create_person(client2, type=person_type)
        src_invoice = create_invoice(client1, person=person1)
        create_cash_payment_fact(src_invoice, 100, OebsOperationType.INSERT)
        src_invoice.manual_turn_on(100)
        dst_invoice = create_invoice(client2, person=person2)
        client1.session.flush()

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'src_invoice_external_id': src_invoice.external_id,
                'dst_invoice_external_id': dst_invoice.external_id,
                'amount': '100',
            },
            is_admin=from_ai,
        )
        if not from_ai:
            assert_that(response.status_code, equal_to(http.BAD_REQUEST), 'response code must be 400')

            assert_that(
                response.get_json(),
                has_entries({
                    'error': 'INVOICE_TRANSFER_FORBIDDEN_DIFFERENT_CLIENTS',
                    'tanker_context': has_entries({
                        'src-client-id': client1.id,
                        'dst-client-id': client2.id,
                    }),
                }),
            )
        else:
            assert_that(response.status_code, equal_to(http.OK))

    def test_fail_nds_pct(self):
        src_invoice = create_invoice()
        create_cash_payment_fact(src_invoice, 100, OebsOperationType.INSERT)
        src_invoice.manual_turn_on(100)
        dst_invoice = create_invoice(src_invoice.client, person=src_invoice.person)
        dst_invoice.nds_pct = 12
        self.test_session.flush()

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'src_invoice_external_id': src_invoice.external_id,
                'dst_invoice_external_id': dst_invoice.external_id,
                'amount': '100',
            },
        )
        assert_that(response.status_code, equal_to(http.BAD_REQUEST), 'response code must be 400')

        assert_that(
            response.get_json(),
            has_entries({
                'error': 'INVOICE_TRANSFER_FORBIDDEN_DIFFERENT_NDS',
                'tanker_context': has_entries({
                    'src-nds': '20%',
                    'dst-nds': '12%',
                }),
            }),
        )

    def test_fail_amount_fields(self):
        src_invoice = create_invoice()
        src_invoice.manual_turn_on(100)
        dst_invoice = create_invoice()

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'src_invoice_external_id': src_invoice.external_id,
                'dst_invoice_external_id': dst_invoice.external_id,
            },
        )
        assert_that(response.status_code, equal_to(http.BAD_REQUEST), 'response code must be 400')

        has_entries({
            'error': 'FORM_VALIDATION_ERROR',
            'description': 'Form validation error.',
            'form_errors': has_entries({
                '?': contains(has_entries({
                    'error': 'FIELD_VALIDATION_ERROR',
                    'description': 'Use either `amount` parameter or `all_money` parameter.',
                    'tanker_context': has_entries({u'input-val': None, u'field-name': u'?'}),
                })),
            }),
        })

    @pytest.mark.parametrize(
        'who_is, res',
        [
            ['owner', http.OK],
            ['stranger', http.FORBIDDEN],
            ['admin', http.FORBIDDEN],
            ['admin_do_inv_trans', http.OK],
        ]
    )
    def test_access(self, admin_role, do_inv_trans_role, client, who_is, res):
        security.set_passport_client(client if who_is == 'owner' else create_client())
        roles = []
        is_admin = ('admin' in who_is)
        if is_admin:
            roles.append(admin_role)
        if 'do_inv_trans' in who_is:
            roles.append(do_inv_trans_role)
        security.set_roles(roles)

        src_invoice = create_invoice(client)
        create_cash_payment_fact(src_invoice, 100, OebsOperationType.INSERT)
        src_invoice.manual_turn_on(100)
        dst_invoice = create_invoice(client, person=src_invoice.person)

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'src_invoice_external_id': src_invoice.external_id,
                'dst_invoice_external_id': dst_invoice.external_id,
                'amount': '50',
            },
            is_admin=is_admin,
        )

        assert_that(response.status_code, equal_to(res))
        if res == http.FORBIDDEN:
            assert_that(
                response.get_json(),
                has_entries({
                    'description': 'User %s has no permission %s.' % (
                        self.test_session.oper_id,
                        PermissionCode.DO_INVOICE_TRANSFER,
                    ),
                    'error': 'PERMISSION_DENIED',
                }),
            )
