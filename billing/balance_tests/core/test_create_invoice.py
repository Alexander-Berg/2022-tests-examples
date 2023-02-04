# -*- coding: utf-8 -*-

import pytest

from balance import muzzle_util as ut
from balance import mapper, exc
from balance.actions import single_account

from tests.base import BalanceTest
from tests import object_builder as ob

BANK_PAYSYS_ID = 1001
CREDIT_CARD_PAYSYS_ID = 1002
SERVICE_ID = 7
FIRM_ID = 1


class TestCreateInvoice(BalanceTest):
    def test_ok(self):
        request = ob.RequestBuilder(
            basket=ob.BasketBuilder(
                rows=[ob.BasketItemBuilder(quantity=6, order=ob.OrderBuilder())]
            )
        ).build(self.session).obj
        person = ob.PersonBuilder(client=request.client).build(self.session).obj

        invoice, = self.coreobj.create_invoice(
            request_id=request.id,
            paysys_id=CREDIT_CARD_PAYSYS_ID,
            person_id=person.id
        )
        assert invoice.effective_sum == 600

    def test_limit_fail(self):
        request = ob.RequestBuilder(
            basket=ob.BasketBuilder(
                rows=[ob.BasketItemBuilder(quantity=500, order=ob.OrderBuilder())]
            )
        ).build(self.session).obj
        person = ob.PersonBuilder(client=request.client).build(self.session).obj

        self.assertRaises(ut.PAYSYS_LIMIT_EXCEEDED,
                          self.coreobj.create_invoice,
                          request.id, CREDIT_CARD_PAYSYS_ID, person.id)

    def test_turnon_service35(self):
        # If all orders are on service 35, invoice should be automatically
        # turned on
        b_request = ob.RequestBuilder(firm_id=1)
        for row in b_request.basket.rows:
            row.b.order.b.service = ob.Getter(mapper.Service, 35)
            row.b.order.b.manager = None
        request = b_request.build(self.session).obj
        person = ob.PersonBuilder(
            client=request.client,
            type='ph'
        ).build(self.session).obj
        inv_id = self.coreobj.create_invoice(
            request_id=request.id,
            paysys_id=BANK_PAYSYS_ID,
            person_id=person.id,
            turnon_service35=True)[0].id
        invoice = self.session.query(mapper.Invoice).get(inv_id)
        assert invoice.consumes  # Invoice has been turned on

    def test_overdraft(self):
        request = ob.RequestBuilder(
            basket=ob.BasketBuilder(
                rows=[ob.BasketItemBuilder(quantity=6, order=ob.OrderBuilder())]
            )
        ).build(self.session).obj
        person = ob.PersonBuilder(client=request.client).build(self.session).obj
        person.client.set_overdraft_limit(SERVICE_ID, FIRM_ID, 333, None)

        invoice, = self.coreobj.create_invoice(
            request_id=request.id,
            paysys_id=CREDIT_CARD_PAYSYS_ID,
            person_id=person.id,
            overdraft=True,
            skip_verification=True
        )
        assert invoice.overdraft == 1

    def test_no_overdraft_invoice_with_contract(self):
        request = ob.RequestBuilder(
            basket=ob.BasketBuilder(
                rows=[ob.BasketItemBuilder(quantity=6, order=ob.OrderBuilder())]
            )
        ).build(self.session).obj
        person = ob.PersonBuilder(client=request.client).build(self.session).obj
        contract = ob.ContractBuilder(
            client=person.client,
            person=person,
            commission=0,
            firm=FIRM_ID,
            services={SERVICE_ID},
            is_signed=self.session.now(),
        ).build(self.session).obj

        person.client.set_overdraft_limit(SERVICE_ID, FIRM_ID, 333, None)

        with pytest.raises(exc.INCOMPATIBLE_INVOICE_PARAMS):
            self.coreobj.create_invoice(
                request_id=request.id,
                paysys_id=CREDIT_CARD_PAYSYS_ID,
                person_id=person.id,
                contract_id=contract.id,
                overdraft=True,
                skip_verification=True
            )

    def test_single_account_overdraft(self):
        self.session.config.__dict__['SINGLE_ACCOUNT_PAYSTEP_OVERDRAFT_ENABLED'] = 0

        client = ob.ClientBuilder(with_single_account=True).build(self.session).obj
        person = ob.PersonBuilder(client=client, type='ph').build(self.session).obj
        single_account.prepare.process_client(client)
        (personal_account, _), = client.get_single_account_subaccounts()

        person.client.set_overdraft_limit(SERVICE_ID, FIRM_ID, 333, None)

        request = ob.RequestBuilder(
            basket=ob.BasketBuilder(
                rows=[
                    ob.BasketItemBuilder(
                        quantity=100,
                        order=ob.OrderBuilder(
                            client=client,
                            product=ob.ProductBuilder(
                                price=1,
                                engine_id=SERVICE_ID
                            ),
                            service_id=SERVICE_ID
                        )
                    )
                ]
            )
        ).build(self.session).obj

        with pytest.raises(exc.INCOMPATIBLE_INVOICE_PARAMS):
            self.coreobj.create_invoice(
                request_id=request.id,
                paysys_id=BANK_PAYSYS_ID,
                person_id=person.id,
                overdraft=True,
                skip_verification=True
            )
