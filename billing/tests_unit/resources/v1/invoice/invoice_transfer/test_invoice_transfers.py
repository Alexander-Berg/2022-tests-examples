# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
from hamcrest import (
    assert_that,
    contains,
    equal_to,
    has_entries,
    has_length,
)

from balance.constants import InvoiceTransferStatus
from brest.core.tests import security

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice, create_invoice_transfer
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_view_inv_role


@pytest.mark.smoke
class TestInvoiceTransfers(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/invoice-transfers'

    def create_dst_invoice_and_transfer(self, src_invoice, amount):
        dst_invoice = create_invoice(client=src_invoice.client, person=src_invoice.person)
        self.test_session.flush()

        invoice_transfer = create_invoice_transfer(
            src_invoice, dst_invoice, amount
        )

        return dst_invoice, invoice_transfer

    def test_get_invoice_transfers(self):
        src_invoice = create_invoice()
        src_invoice.manual_turn_on(100)
        dst_invoice = create_invoice()

        dst_invoice.person = src_invoice.person
        self.test_session.flush()

        invoice_transfer = create_invoice_transfer(
            src_invoice, dst_invoice, 100, status=InvoiceTransferStatus.export_failed
        )

        params = {
            'invoice_id': src_invoice.id,
            'pagination_pn': 1,
            'pagination_ps': 5,
        }

        response = self.test_client.get(self.BASE_API, params)
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        assert_that(
            response.get_json()['data'],
            has_entries(
                {
                    "total_count": 1,
                    "items": contains(
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
                                "amount": "100.00",
                                "status": InvoiceTransferStatus.export_failed,
                                "unlock_allowed": True
                            }
                        ),
                    ),
                    "request_params": has_entries(
                        {
                            "pagination_pn": 1,
                            "pagination_ps": 5,
                            "sort_order": "desc",
                            "sort_key": "dt"
                        }
                    )
                }
            )
        )

    def test_pages(self):
        src_invoice = create_invoice()
        src_invoice.manual_turn_on(100)

        dst_invoice_1, invoice_transfer_1 = self.create_dst_invoice_and_transfer(src_invoice, 10)
        dst_invoice_2, invoice_transfer_2 = self.create_dst_invoice_and_transfer(src_invoice, 10)
        dst_invoice_3, invoice_transfer_3 = self.create_dst_invoice_and_transfer(src_invoice, 10)

        params = {
            'invoice_id': src_invoice.id,
            'pagination_pn': 1,
            'pagination_ps': 2,
        }

        response = self.test_client.get(self.BASE_API, params)
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        assert_that(
            response.get_json()['data'],
            has_entries(
                {
                    "total_count": 3,
                    "items": contains(
                        has_entries(
                            {
                                "id": invoice_transfer_3.id,
                                "src_invoice": has_entries({
                                    "id": src_invoice.id,
                                    "external_id": src_invoice.external_id
                                }),
                                "dst_invoice": has_entries({
                                    "id": dst_invoice_3.id,
                                    "external_id": dst_invoice_3.external_id
                                }),
                                "amount": "10.00",
                                "status": InvoiceTransferStatus.exported,
                                "unlock_allowed": False
                            }
                        ),
                        has_entries(
                            {
                                "id": invoice_transfer_2.id,
                                "src_invoice": has_entries({
                                    "id": src_invoice.id,
                                    "external_id": src_invoice.external_id
                                }),
                                "dst_invoice": has_entries({
                                    "id": dst_invoice_2.id,
                                    "external_id": dst_invoice_2.external_id
                                }),
                                "amount": "10.00",
                                "status": InvoiceTransferStatus.exported,
                                "unlock_allowed": False
                            }
                        )
                    ),
                    "request_params": has_entries(
                        {
                            "pagination_pn": 1,
                            "pagination_ps": 2,
                            "sort_order": "desc",
                            "sort_key": "dt"
                        }
                    )
                }
            )
        )

        params = {
            'invoice_id': src_invoice.id,
            'pagination_pn': 2,
            'pagination_ps': 2,
        }

        response = self.test_client.get(self.BASE_API, params)
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        assert_that(
            response.get_json()['data'],
            has_entries(
                {
                    "total_count": 3,
                    "items": contains(
                        has_entries(
                            {
                                "id": invoice_transfer_1.id,
                                "src_invoice": has_entries({
                                    "id": src_invoice.id,
                                    "external_id": src_invoice.external_id
                                }),
                                "dst_invoice": has_entries({
                                    "id": dst_invoice_1.id,
                                    "external_id": dst_invoice_1.external_id
                                }),
                                "amount": "10.00",
                                "status": InvoiceTransferStatus.exported,
                                "unlock_allowed": False
                            }
                        ),
                    ),
                    "request_params": has_entries(
                        {
                            "pagination_pn": 2,
                            "pagination_ps": 2,
                            "sort_order": "desc",
                            "sort_key": "dt"
                        }
                    )
                }
            )
        )

    @pytest.mark.parametrize(
        'who_is, res',
        [
            ['owner', 'success'],
            ['stranger', 'filtered'],
            ['admin', 'filtered'],
            ['admin_view_inv', 'success'],
        ]
    )
    def test_access(self, admin_role, view_inv_role, client, who_is, res):
        security.set_passport_client(client if who_is == 'owner' else create_client())
        roles = []
        is_admin = ('admin' in who_is)
        if is_admin:
            roles.append(admin_role)
        if 'view_inv' in who_is:
            roles.append(view_inv_role)
        security.set_roles(roles)

        src_invoice = create_invoice(client)
        src_invoice.manual_turn_on(100)
        dst_invoice = create_invoice(client)

        dst_invoice.person = src_invoice.person
        self.test_session.flush()

        invoice_transfer = create_invoice_transfer(
            src_invoice, dst_invoice, 100, status=InvoiceTransferStatus.export_failed
        )

        params = {
            'invoice_id': src_invoice.id,
            'pagination_pn': 1,
            'pagination_ps': 5,
        }
        response = self.test_client.get(self.BASE_API, params, is_admin=is_admin)
        assert_that(response.status_code, equal_to(http.OK), 'response must be OK, we just filter transfers')
        assert_that(response.get_json()['data']['items'], has_length(1 if res == 'success' else 0))
