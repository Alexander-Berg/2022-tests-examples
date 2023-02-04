# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import datetime
import pytest
import http.client as http
from hamcrest import (
    assert_that,
    equal_to,
)

from balance import constants as cst, mapper
from tests import object_builder as ob
from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
from yb_snout_api.tests_unit.fixtures.contract import create_general_contract
from yb_snout_api.tests_unit.fixtures.permissions import get_client_role


class TestCreateInvoice(TestCaseApiAppBase):
    BASE_API = u'/assessor/invoice/create'
    UR_BANK_PAYSYS_ID = ob.Getter(mapper.Paysys, 1003)._id
    PRODUCT = cst.DIRECT_PRODUCT_ID
    SERVICE = cst.DIRECT_SERVICE_ID
    PERSON_TYPE = 'ur'
    QTY = 10

    def create_invoice(self, session, client, is_credit):
        order = ob.OrderBuilder.construct(session, service_id=self.SERVICE, product_id=self.PRODUCT, client=client)
        person = ob.PersonBuilder.construct(session, client=order.client, type=self.PERSON_TYPE)
        contract = create_general_contract(
            client=client,
            credit_limit_single=100000,
            firm_id=cst.FirmId.YANDEX_OOO,
            is_signed=session.now() - datetime.timedelta(30),
            payment_type=cst.POSTPAY_PAYMENT_TYPE,
            person=person,
            personal_account=1,
            personal_account_fictive=1,
            services={self.SERVICE},
        ).id if is_credit else None
        request = ob.RequestBuilder.construct(
            session,
            basket=ob.BasketBuilder(
                rows=[ob.BasketItemBuilder(order=order, quantity=self.QTY)]),
        ).id
        security.set_passport_client(client)
        security.set_roles(get_client_role())
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'request_id': request,
                'paysys_id': self.UR_BANK_PAYSYS_ID,
                'person_id': person.id,
                'credit': is_credit,
                'contract_id': contract,
                'overdraft': False,
            },
            is_admin=False,
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        invoice = self.test_session.query(mapper.Invoice).getone(response.get_json()['data']['invoice_id'])
        return invoice

    @pytest.mark.parametrize("is_credit, amount, consume_sum, inv_type",
                             [
                                 (0, QTY * 30, 0, 'prepayment'),
                                 (1, None, QTY * 30, 'fictive_personal_account'),
                             ])
    def test_create_invoice(self, client, is_credit, amount, consume_sum, inv_type):
        session = self.test_session
        invoice = self.create_invoice(session, client, is_credit)
        assert_that(invoice.amount, equal_to(amount))
        assert_that(invoice.consume_sum, equal_to(consume_sum))
        assert_that(invoice.type, equal_to(inv_type))
