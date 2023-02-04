# -*- coding: utf-8 -*-

from decimal import Decimal as D

from balance import constants as const
from balance.constants import *
from balance.processors.partner_offer_activation import OfferActivationLogicBase
from tests import object_builder as ob
from tests.balance_tests.rev_partners.common import (
    TAXI_COMMISSION_SERVICE_CODE,
    gen_contract,
    get_pa,
)


def test_suspense(session):
    u"""Контракт приостанавливается, если не внести мин. платёж, и возобновляется, если внести."""

    def con_func(c):
        c.col0.commission = 9  # оферта
        c.col0.services = {const.ServiceId.TAXI_CASH}
        c.col0.offer_activation_payment_amount = D(500)  # мин. платёж - 500 денег
        c.col0.offer_confirmation_type = "min-payment"
        c.col0.offer_activation_due_term = 10

    with session.begin():
        contract = gen_contract(session, con_func=con_func, personal_account=True)

        current_signed = contract.current_signed()
        personal_account = get_pa(
            session, contract, service_code=TAXI_COMMISSION_SERVICE_CODE
        )
        # делаем платежей на сумму 450 < 500 денег
        ob.OebsCashPaymentFactBuilder(
            amount=100,
            operation_type=OebsOperationType.INSERT,
            invoice=personal_account,
        ).build(session)
        ob.OebsCashPaymentFactBuilder(
            amount=200,
            operation_type=OebsOperationType.INSERT,
            invoice=personal_account,
        ).build(session)
        ob.OebsCashPaymentFactBuilder(
            amount=300,
            operation_type=OebsOperationType.ONLINE,
            invoice=personal_account,
        ).build(session)
        ob.OebsCashPaymentFactBuilder(
            amount=-150,
            operation_type=OebsOperationType.ONLINE,
            invoice=personal_account,
        ).build(session)
        ob.OebsCashPaymentFactBuilder(
            amount=1000000,
            operation_type=OebsOperationType.SF_AVANS,
            # операция такого типа не будет учитываться
            invoice=personal_account,
        ).build(session)

        # после обработки договор должен стать приостановленным
        OfferActivationLogicBase(contract, current_signed, None).do_process()
        assert (
            contract.current_signed().is_suspended is not None
        ), "contract must be suspended"

        # добавляем платежей до суммы 700 > 500 денег
        ob.OebsCashPaymentFactBuilder(
            amount=250,
            operation_type=OebsOperationType.INSERT,
            invoice=personal_account,
        ).build(session)

        # после обработки договор должен стать возобновлённым
        OfferActivationLogicBase(contract, current_signed, None).do_process()
        assert (
            contract.current_signed().is_suspended is None
        ), "contract must be unsuspended"
