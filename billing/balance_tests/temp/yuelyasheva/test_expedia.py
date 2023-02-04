# -*- coding: utf-8 -*-
__author__ = 'yuelyasheva'

import datetime
from decimal import Decimal as D

import pytest

from balance import balance_steps as steps
from balance import balance_db
from btestlib import utils
from btestlib.constants import NdsNew, Services
from btestlib.data.partner_contexts import TRAVEL_EXPEDIA_CONTEXT
from dateutil.relativedelta import relativedelta
from btestlib.matchers import contains_dicts_with_entries
from btestlib.constants import TransactionType, Export
from balance import balance_api as api
from hamcrest import empty
from btestlib.matchers import contains_dicts_equal_to

context = TRAVEL_EXPEDIA_CONTEXT

_, _, month_before_prev_start_dt, month_before_prev_end_dt, prev_month_start_dt, prev_month_end_dt = \
    utils.Date.previous_three_months_start_end_dates()
current_month_start_dt, current_month_end_dt = utils.Date.current_month_first_and_last_days()
prev_quarter_start_dt, prev_quarter_end_dt = utils.Date.get_previous_quarter(datetime.datetime.today())


def test_create_side_payments():
    # создаем данные
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params={'start_dt': prev_month_start_dt})

    # steps.ExportSteps.export_oebs(client_id=client_id)
    # steps.ExportSteps.export_oebs(person_id=person_id)
    # steps.ExportSteps.export_oebs(contract_id=contract_id)

    payment_dt = prev_month_start_dt + relativedelta(days=1)
    amount = D('123')
    side_payment_id_cost, _ = steps.PartnerSteps.create_sidepayment_transaction(client_id, payment_dt, D('110'),
                                                                               'cost', TRAVEL_EXPEDIA_CONTEXT.service.id,
                                                                                transaction_type=TransactionType.PAYMENT,
                                                                                currency=TRAVEL_EXPEDIA_CONTEXT.currency)
    side_payment_id_reward, _ = steps.PartnerSteps.create_sidepayment_transaction(client_id, payment_dt, D('10'),
                                                                               'reward', TRAVEL_EXPEDIA_CONTEXT.service.id,
                                                                                  transaction_type=TransactionType.PAYMENT,
                                                                                  currency=TRAVEL_EXPEDIA_CONTEXT.currency)

    steps.ExportSteps.create_export_record(side_payment_id_cost, classname=Export.Classname.SIDE_PAYMENT)
    steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, side_payment_id_cost)
    steps.ExportSteps.create_export_record(side_payment_id_reward, classname=Export.Classname.SIDE_PAYMENT)
    steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, side_payment_id_reward)

    thirdparty_transaction_id_cost = steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(
        side_payment_id_cost)
    thirdparty_transaction_id_reward = steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(
        side_payment_id_reward)

    payment_data_cost = steps.CommonPartnerSteps.get_thirdparty_payment_by_id(thirdparty_transaction_id_cost)
    payment_data_reward = steps.CommonPartnerSteps.get_thirdparty_payment_by_id(thirdparty_transaction_id_reward)


def test_create_trust_payments():
    # создаем данные
    client_id, service_product_id = steps.SimpleApi.create_partner_and_product(Services.HOTELS)
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(Services.HOTELS, service_product_id,
                                             commission_category=D('100'))

    steps.CommonPartnerSteps.export_payment(payment_id)

def test_update_payments():
    # создаем данные
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params={'start_dt': prev_month_start_dt})

    # steps.ExportSteps.export_oebs(client_id=client_id)
    # steps.ExportSteps.export_oebs(person_id=person_id)
    # steps.ExportSteps.export_oebs(contract_id=contract_id)

    payment_dt = prev_month_start_dt + relativedelta(days=1)
    side_payment_id_cost, side_transaction_id_cost = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, payment_dt, D('110'),
                                                          'cost', TRAVEL_EXPEDIA_CONTEXT.service.id,
                                                          transaction_type=TransactionType.PAYMENT,
                                                          currency=TRAVEL_EXPEDIA_CONTEXT.currency)

    side_payment_id_reward, side_transaction_id_reward = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, payment_dt, D('10'),
                                                          'reward', TRAVEL_EXPEDIA_CONTEXT.service.id,
                                                          transaction_type=TransactionType.PAYMENT,
                                                          currency=TRAVEL_EXPEDIA_CONTEXT.currency)

    side_payment_id_cost_refund, side_transaction_id_cost_refund = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, payment_dt, D('110'),
                                                          'cost', TRAVEL_EXPEDIA_CONTEXT.service.id,
                                                          transaction_type=TransactionType.REFUND,
                                                          currency=TRAVEL_EXPEDIA_CONTEXT.currency,
                                                          orig_transaction_id=side_transaction_id_cost)

    side_payment_id_reward_refund, side_transaction_id_reward_refund = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, payment_dt, D('10'),
                                                          'reward',
                                                          TRAVEL_EXPEDIA_CONTEXT.service.id,
                                                          transaction_type=TransactionType.REFUND,
                                                          currency=TRAVEL_EXPEDIA_CONTEXT.currency,
                                                          orig_transaction_id=side_transaction_id_reward)

    payout_ready_dt = prev_month_start_dt + relativedelta(days=1)
    # api.medium().UpdatePayment({'ServiceID': EXPEDIA_CONTEXT.service.id, 'TransactionID': side_transaction_id_cost},
    #                            {'PayoutReady': payout_ready_dt})
    # api.medium().UpdatePayment({'ServiceID': EXPEDIA_CONTEXT.service.id, 'TransactionID': side_transaction_id_reward},
    #                            {'PayoutReady': payout_ready_dt})
    # api.medium().UpdatePayment(
    #     {'ServiceID': EXPEDIA_CONTEXT.service.id, 'TransactionID': side_transaction_id_cost_refund},
    #     {'PayoutReady': payout_ready_dt})
    # api.medium().UpdatePayment(
    #     {'ServiceID': EXPEDIA_CONTEXT.service.id, 'TransactionID': side_transaction_id_reward_refund},
    #     {'PayoutReady': payout_ready_dt})

    steps.ExportSteps.create_export_record(side_payment_id_cost, classname=Export.Classname.SIDE_PAYMENT)
    steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, side_payment_id_cost)
    steps.ExportSteps.create_export_record(side_payment_id_reward, classname=Export.Classname.SIDE_PAYMENT)
    steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, side_payment_id_reward)
    # steps.ExportSteps.create_export_record(side_payment_id_cost_refund, classname=Export.Classname.SIDE_PAYMENT)
    steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, side_payment_id_cost_refund)
    # steps.ExportSteps.create_export_record(side_payment_id_reward_refund, classname=Export.Classname.SIDE_PAYMENT)
    steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, side_payment_id_reward_refund)

    synthetic_thirdparty_transaction_id_cost =  steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(
        side_payment_id_cost)
    synthetic_thirdparty_transaction_id_reward = steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(
        side_payment_id_reward)
    synthetic_thirdparty_transaction_id_cost_refund = steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(
        side_payment_id_cost_refund)
    synthetic_thirdparty_transaction_id_reward_refund =  steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(
        side_payment_id_reward_refund)

    api.medium().UpdatePayment({'ServiceID': TRAVEL_EXPEDIA_CONTEXT.service.id, 'TransactionID': side_transaction_id_cost},
                               {'PayoutReady': payout_ready_dt})
    api.medium().UpdatePayment({'ServiceID': TRAVEL_EXPEDIA_CONTEXT.service.id, 'TransactionID': side_transaction_id_reward},
                               {'PayoutReady': payout_ready_dt})
    # api.medium().UpdatePayment(
    #     {'ServiceID': EXPEDIA_CONTEXT.service.id, 'TransactionID': side_transaction_id_cost_refund},
    #     {'PayoutReady': payout_ready_dt})
    # api.medium().UpdatePayment(
    #     {'ServiceID': EXPEDIA_CONTEXT.service.id, 'TransactionID': side_transaction_id_reward_refund},
    #     {'PayoutReady': payout_ready_dt})


def test_negative_cases_updatepayment():
    api.medium().UpdatePayment(
        {'ServiceID': TRAVEL_EXPEDIA_CONTEXT.service.id, 'TransactionID': 10008101},
        {'PayoutReady': prev_month_start_dt + relativedelta(days=2)})
    api.medium().UpdatePayment(
        {'ServiceID': TRAVEL_EXPEDIA_CONTEXT.service.id, 'TransactionID': None},
        {'PayoutReady': None})

def test_woop():
    # api.medium().UpdatePayment(
    #     {'ServiceID': EXPEDIA_CONTEXT.service.id, 'TransactionID': str(10010543)},
    #     {'PayoutReady': datetime.datetime.today()})
    # api.medium().UpdatePayment(
    #     {'ServiceID': EXPEDIA_CONTEXT.service.id, 'TransactionID': str(10010564)},
    #     {'PayoutReady':  datetime.datetime.today()})
    # api.medium().UpdatePayment(
    #     {'ServiceID': EXPEDIA_CONTEXT.service.id, 'TransactionID': str(10010565)},
    #     {'PayoutReady':  datetime.datetime.today()})
    # api.medium().UpdatePayment(
    #     {'ServiceID': EXPEDIA_CONTEXT.service.id, 'TransactionID': str(10010566)},
    #     {'PayoutReady':  datetime.datetime.today()})
    # steps.ExportSteps.create_export_record(str(1000332083), classname=Export.Classname.SIDE_PAYMENT)
    # steps.ExportSteps.create_export_record(str(1000332104), classname=Export.Classname.SIDE_PAYMENT)
    # steps.ExportSteps.create_export_record(str(1000332105), classname=Export.Classname.SIDE_PAYMENT)
    # steps.ExportSteps.create_export_record(str(1000332106), classname=Export.Classname.SIDE_PAYMENT)
    # steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, str(1000332083))
    # steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, str(1000332104))
    # steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, str(1000332105))
    # steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, str(1000332106))

    # steps.ExportSteps.export_oebs(transaction_id=str(11328157441))
    # steps.ExportSteps.export_oebs(transaction_id=str(11328157651))
    # steps.ExportSteps.export_oebs(transaction_id=str(11328157661))
    # steps.ExportSteps.export_oebs(transaction_id=str(11328157671))

    steps.ExportSteps.export_oebs(person_id=10083429)
    steps.ExportSteps.export_oebs(contract_id=1664422)
