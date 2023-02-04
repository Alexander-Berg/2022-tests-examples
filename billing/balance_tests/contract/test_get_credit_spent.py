# -*- coding: utf-8 -*-

import datetime

import pytest

from billing.contract_iface import ContractTypeId
from balance.constants import (
    POSTPAY_PAYMENT_TYPE,
    FirmId,
    CreditType,
    NUM_CODE_RUR,
    DIRECT_PRODUCT_RUB_ID,
)
from balance import core
from balance import mapper
from balance.actions import invoice_turnon
from tests import object_builder as ob

BANK_UR_PAYSYS_ID = 1003
CARD_UR_PAYSYS_ID = 1033


def create_contract(session, **params):
    client = ob.ClientBuilder()
    person = ob.PersonBuilder(client=client, type='ur')
    on_dt = datetime.datetime.now() - datetime.timedelta(30)

    base_params = dict(
        client=client,
        person=person,
        dt=on_dt,
        is_signed=on_dt,
        commission=ContractTypeId.WHOLESALE_AGENCY_AWARD,
        firm=FirmId.YANDEX_OOO,
        services={7},
        payment_type=POSTPAY_PAYMENT_TYPE,
        credit_type=CreditType.PO_SROKU_I_SUMME,
        currency=NUM_CODE_RUR,
        personal_account=1,
        personal_account_fictive=1,
        credit_limit_single=666666,
        payment_term=30,
        lift_credit_on_payment=1,
    )
    base_params.update(params)
    return ob.ContractBuilder.construct(session, **base_params)


def pay_on_credit(contract, product, quantity, paysys_id=BANK_UR_PAYSYS_ID, sublcient=None):
    session = contract.session

    if sublcient:
        agency = contract.client
        client = sublcient
    else:
        agency = None
        client = contract.client

    order = ob.OrderBuilder.construct(session, agency=agency, client=client, product=product)
    request = ob.RequestBuilder(
        basket=ob.BasketBuilder(
            client=contract.client,
            rows=[ob.BasketItemBuilder(order=order, quantity=quantity)]
        )
    ).build(session).obj
    return core.Core(session).pay_on_credit(request.id, paysys_id, contract.person_id, contract.id)


class TestFictives(object):
    @pytest.mark.parametrize(
        'quantities',
        [
            (0, 0),
            (10, 20),
            (6666, 7777),
        ],
        ids=lambda x: str(x)
    )
    def test_spent(self, session, quantities):
        products = [ob.ProductBuilder.construct(session, price=1) for _ in range(2)]
        limits = [6666, 7777]

        contract = create_contract(
            session,
            personal_account=0,
            personal_account_fictive=0,
            credit_limit_single=None,
            credit_limit={
                p.activity_type_id: sum_
                for p, sum_ in zip(products, limits)
            }
        )

        for product, quantity in zip(products, quantities):
            if quantity:
                pay_on_credit(contract, product, quantity)

        assert contract.current.get_credit_spent() == {
            p.activity_type: (lim, sum_)
            for p, lim, sum_ in zip(products, limits, quantities)
        }

    def test_paid(self, session):
        product1 = ob.ProductBuilder.construct(session, price=1)
        product2 = ob.ProductBuilder.construct(session, price=1)

        contract = create_contract(
            session,
            personal_account=0,
            personal_account_fictive=0,
            credit_limit_single=None,
            credit_limit={
                product1.activity_type_id: 6667,
                product2.activity_type_id: 7777,
            }
        )

        pay_on_credit(contract, product1, 100)
        fictive_invoice, = pay_on_credit(contract, product2, 50)
        pay_on_credit(contract, product2, 60)

        repayment_invoice = core.Core(session).issue_repayment_invoice(
            session.oper_id,
            [fictive_invoice.deferpay.id],
            datetime.datetime.now()
        )
        core.Core(session).preliminary_invoice_action(session.oper_id, repayment_invoice.id, 'confirm')
        invoice_turnon.InvoiceTurnOn(repayment_invoice, manual=True).do()
        session.flush()

        assert contract.current.get_credit_spent() == {
            product1.activity_type: (6667, 100),
            product2.activity_type: (7777, 60),
        }


class TestPersonalAccount(object):
    @pytest.fixture
    def product(self, session):
        return ob.Getter(mapper.Product, DIRECT_PRODUCT_RUB_ID).build(session).obj

    @pytest.mark.parametrize('spent', [0, 666])
    def test_spent(self, session, product, spent):
        contract = create_contract(session, credit_limit_single=6666)
        if spent:
            pay_on_credit(contract, product, spent)

        assert contract.current.get_credit_spent() == {0: [6666, spent]}

    @pytest.mark.parametrize('paid', [0, 100, 666])
    def test_paid(self, session, product, paid):
        contract = create_contract(session, credit_limit_single=6666)
        pa, = pay_on_credit(contract, product, 666)

        pa.close_invoice(datetime.datetime.now())
        if paid:
            pa.repayments[0].create_receipt(paid)

        assert contract.current.get_credit_spent() == {0: [6666, 666 - paid]}

    @pytest.mark.parametrize('extra_spent', [0, 777])
    def test_spent_no_lift_credit_on_payment(self, session, product, extra_spent):
        contract = create_contract(session, credit_limit_single=6666, lift_credit_on_payment=False)

        pa, = pay_on_credit(contract, product, 666)
        pa.close_invoice(datetime.datetime.now())
        if extra_spent:
            pay_on_credit(contract, product, extra_spent)

        assert contract.current.get_credit_spent() == {0: [6666, extra_spent]}

    def test_paid_no_lift_credit_on_payment(self, session, product):
        contract = create_contract(session, credit_limit_single=6666, lift_credit_on_payment=False)

        pa, = pay_on_credit(contract, product, 666)
        pa.close_invoice(datetime.datetime.now())
        pa.repayments[0].create_receipt(123)
        pay_on_credit(contract, product, 777)

        assert contract.current.get_credit_spent() == {0: [6666, 777]}

    def test_subclients(self, session, product):
        agency = ob.ClientBuilder.construct(session, is_agency=1)
        person = ob.PersonBuilder.construct(session, client=agency, type='ur')

        subclient1 = ob.ClientBuilder.construct(session, agency=agency)
        subclient2 = ob.ClientBuilder.construct(session, agency=agency)
        subclient3 = ob.ClientBuilder.construct(session, agency=agency)

        contract = create_contract(
            session,
            client=agency,
            person=person,
            credit_limit_single=200,
            client_limits={
                subclient1.id: {'currency': 'RUR', 'client_limit': 6666},
                subclient2.id: {'currency': 'RUR', 'client_limit': 7777},
            }
        )

        pa1, = pay_on_credit(contract, product, 6, sublcient=subclient1)

        pa2, = pay_on_credit(contract, product, 66, sublcient=subclient2)
        pa2.close_invoice(datetime.datetime.now())
        pa2.repayments[0].create_receipt(12)

        pa, = pay_on_credit(contract, product, 6, sublcient=subclient3)
        pa.close_invoice(datetime.datetime.now())
        pa.repayments[0].create_receipt(2)

        assert contract.current.get_credit_spent() == {0: [200, 4]}
        assert contract.current.get_credit_spent(subclients=[subclient1]) == {0: [6666, 6]}
        assert contract.current.get_credit_spent(subclients=[subclient2]) == {0: [7777, 54]}
        assert contract.current.get_credit_spent(subclients=[subclient3]) == {0: [200, 4]}

    def test_old_pa_paid(self, session, product):
        contract = create_contract(session, credit_limit_single=6666, personal_account_fictive=0)
        pa, = pay_on_credit(contract, product, 666)
        pa.create_receipt(111)

        assert contract.current.get_credit_spent() == {0: [6666, 555]}

    def test_multiple_pas_fictive(self, session, product):
        contract = create_contract(session, credit_limit_single=6666)

        pay_on_credit(contract, product, 123, paysys_id=BANK_UR_PAYSYS_ID)
        pay_on_credit(contract, product, 321, paysys_id=CARD_UR_PAYSYS_ID)

        assert contract.current.get_credit_spent() == {0: [6666, 444]}
