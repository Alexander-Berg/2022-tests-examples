# -*- coding: utf-8 -*-
__author__ = 'yuelyasheva'

import pytest
from decimal import Decimal as D
from balance import balance_steps as steps
from btestlib import utils
from btestlib.data.partner_contexts import TRAVEL_EXPEDIA_CONTEXT, TRAVEL_CONTEXT_RUB
from btestlib.matchers import contains_dicts_with_entries
from btestlib.constants import TransactionType, Export, PaymentType
from balance import balance_api as api
from datetime import datetime
import btestlib.reporter as reporter
from balance.features import Features

pytestmark = [reporter.feature(Features.TRUST)]

_, _, _, _, prev_month_start_dt, prev_month_end_dt = utils.Date.previous_three_months_start_end_dates()
current_date = utils.Date.nullify_time_of_date(datetime.today())

AMOUNT_COST = D('110.26')
AMOUNT_REWARD = D('8.64')
AMOUNT_COST_REFUND = D('15.86')
AMOUNT_REWARD_REFUND = D('2.91')


@pytest.mark.parametrize('context', [
    TRAVEL_EXPEDIA_CONTEXT,
    TRAVEL_CONTEXT_RUB,
])
def test_expedia_payments(context):
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context)

    side_payment_id_cost, side_transaction_id_cost = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, current_date, AMOUNT_COST,
                                                          PaymentType.COST, context.service.id,
                                                          transaction_type=TransactionType.PAYMENT,
                                                          currency=context.currency)

    side_payment_id_reward, side_transaction_id_reward = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, current_date, AMOUNT_REWARD,
                                                          PaymentType.REWARD, context.service.id,
                                                          transaction_type=TransactionType.PAYMENT,
                                                          currency=context.currency)

    # выставляем платежи в очередь и разбираем
    steps.ExportSteps.create_export_record_and_export(side_payment_id_cost, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)
    steps.ExportSteps.create_export_record_and_export(side_payment_id_reward, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    payment_data_cost, payment_data_reward, _, _ = get_payment_data(side_payment_id_cost, side_payment_id_reward)

    expected_payment_data_cost = steps.SimpleApi.create_expected_tpt_row(context, client_id, contract_id,
                                                                         person_id, None,
                                                                         payment_data_cost[0]['payment_id'],
                                                                         trust=False,
                                                                         **{'payment_type': PaymentType.COST,
                                                                            'amount': AMOUNT_COST})
    expected_payment_data_reward = steps.SimpleApi.create_expected_tpt_row(context, client_id, contract_id,
                                                                           person_id, None,
                                                                           payment_data_reward[0]['payment_id'],
                                                                           trust=False,
                                                                           **{'yandex_reward': AMOUNT_REWARD,
                                                                              'amount': AMOUNT_REWARD})

    utils.check_that(payment_data_cost, contains_dicts_with_entries([expected_payment_data_cost]),
                     u'Сравниваем платеж cost с шаблоном')
    utils.check_that(payment_data_reward, contains_dicts_with_entries([expected_payment_data_reward]),
                     u'Сравниваем платеж reward с шаблоном')


@pytest.mark.parametrize('context', [
    TRAVEL_EXPEDIA_CONTEXT,
    TRAVEL_CONTEXT_RUB,
])
def test_expedia_refunds(context):
    # создаем данные
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context)

    side_payment_id_cost, side_transaction_id_cost = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, current_date, AMOUNT_COST,
                                                          PaymentType.COST, context.service.id,
                                                          transaction_type=TransactionType.PAYMENT,
                                                          currency=context.currency)
    side_payment_id_reward, side_transaction_id_reward = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, current_date, AMOUNT_REWARD,
                                                          PaymentType.REWARD, context.service.id,
                                                          transaction_type=TransactionType.PAYMENT,
                                                          currency=context.currency)
    side_payment_id_cost_refund, side_transaction_id_cost_refund = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, current_date, AMOUNT_COST_REFUND,
                                                          PaymentType.COST, context.service.id,
                                                          transaction_type=TransactionType.REFUND,
                                                          currency=context.currency,
                                                          orig_transaction_id=side_transaction_id_cost)
    side_payment_id_reward_refund, side_transaction_id_reward_refund = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, current_date, AMOUNT_REWARD_REFUND,
                                                          PaymentType.REWARD,
                                                          context.service.id,
                                                          transaction_type=TransactionType.REFUND,
                                                          currency=context.currency,
                                                          orig_transaction_id=side_transaction_id_reward)

    steps.ExportSteps.create_export_record_and_export(side_payment_id_cost, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)
    steps.ExportSteps.create_export_record_and_export(side_payment_id_reward, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)
    # рефанды должны автоматически проставиться в очередь после разбора платежей
    steps.ExportSteps.create_export_record_and_export(side_payment_id_cost_refund, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT, with_export_record=False)
    steps.ExportSteps.create_export_record_and_export(side_payment_id_reward_refund, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT, with_export_record=False)

    with steps.reporter.step(u'Проставляем payout_ready_dt в платежах:'):
        api.medium().UpdatePayment({'ServiceID': context.service.id, 'TransactionID': side_transaction_id_cost},
                                   {'PayoutReady': current_date})
        api.medium().UpdatePayment({'ServiceID': context.service.id, 'TransactionID': side_transaction_id_reward},
                                   {'PayoutReady': current_date})

    # платежи должны автоматически проставиться в очередь после применения UpdatePayment
    steps.ExportSteps.create_export_record_and_export(side_payment_id_cost, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT, with_export_record=False)
    steps.ExportSteps.create_export_record_and_export(side_payment_id_reward, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT, with_export_record=False)
    # рефанды должны автоматически проставиться в очередь после разбора платежей
    steps.ExportSteps.create_export_record_and_export(side_payment_id_cost_refund, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT, with_export_record=False)
    steps.ExportSteps.create_export_record_and_export(side_payment_id_reward_refund, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT, with_export_record=False)

    payment_data_cost, payment_data_reward, refund_data_cost, refund_data_reward = \
        get_payment_data(side_payment_id_cost, side_payment_id_reward,
                         side_payment_id_cost_refund, side_payment_id_reward_refund)

    expected_payment_data_cost = steps.SimpleApi.create_expected_tpt_row(context, client_id, contract_id,
                                                                         person_id,
                                                                         None, payment_data_cost[0]['payment_id'],
                                                                         trust=False,
                                                                         **{'payout_ready_dt': current_date,
                                                                            'payment_type': PaymentType.COST,
                                                                            'amount': AMOUNT_COST})
    expected_payment_data_reward = steps.SimpleApi.create_expected_tpt_row(context, client_id, contract_id,
                                                                           person_id,
                                                                           None, payment_data_reward[0]['payment_id'],
                                                                           trust=False,
                                                                           **{'yandex_reward': AMOUNT_REWARD,
                                                                              'payout_ready_dt': current_date,
                                                                              'amount': AMOUNT_REWARD})
    expected_refund_data_cost = steps.SimpleApi.create_expected_tpt_row(context, client_id, contract_id,
                                                                        person_id,
                                                                        None, refund_data_cost[0]['payment_id'],
                                                                        trust=False,
                                                                        **{'transaction_type': TransactionType.REFUND.name,
                                                                           'payout_ready_dt': current_date,
                                                                           'payment_type': PaymentType.COST,
                                                                           'amount': AMOUNT_COST_REFUND})
    expected_refund_data_reward = steps.SimpleApi.create_expected_tpt_row(context, client_id, contract_id,
                                                                          person_id,
                                                                          None, refund_data_reward[0]['payment_id'],
                                                                          trust=False,
                                                                          **{'yandex_reward': AMOUNT_REWARD_REFUND,
                                                                             'transaction_type': TransactionType.REFUND.name,
                                                                             'payout_ready_dt': current_date,
                                                                             'amount': AMOUNT_REWARD_REFUND})

    utils.check_that(payment_data_cost, contains_dicts_with_entries([expected_payment_data_cost]),
                     u'Сравниваем платеж cost с шаблоном')
    utils.check_that(payment_data_reward, contains_dicts_with_entries([expected_payment_data_reward]),
                     u'Сравниваем платеж reward с шаблоном')
    utils.check_that(refund_data_cost, contains_dicts_with_entries([expected_refund_data_cost]),
                     u'Сравниваем рефанд cost с шаблоном')
    utils.check_that(refund_data_reward, contains_dicts_with_entries([expected_refund_data_reward]),
                     u'Сравниваем рефанд reward с шаблоном')


def get_payment_data(side_payment_id_cost, side_payment_id_reward, side_payment_id_cost_refund=None,
                     side_payment_id_reward_refund=None):
    synthetic_tpt_id_cost = steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(
        side_payment_id_cost)
    synthetic_tpt_id_reward = steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(
        side_payment_id_reward)
    synthetic_tpt_id_cost_refund = steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(
            side_payment_id_cost_refund) if side_payment_id_cost_refund else None
    synthetic_tpt_id_reward_refund = steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(
            side_payment_id_reward_refund) if side_payment_id_reward_refund else None

    payment_data_cost = steps.CommonPartnerSteps.get_thirdparty_payment_by_id(synthetic_tpt_id_cost)
    payment_data_reward = steps.CommonPartnerSteps.get_thirdparty_payment_by_id(synthetic_tpt_id_reward)
    refund_data_cost = steps.CommonPartnerSteps.get_thirdparty_payment_by_id(synthetic_tpt_id_cost_refund) \
        if side_payment_id_cost_refund else None
    refund_data_reward = steps.CommonPartnerSteps.get_thirdparty_payment_by_id(synthetic_tpt_id_reward_refund) \
        if synthetic_tpt_id_reward_refund else None
    return payment_data_cost, payment_data_reward, refund_data_cost, refund_data_reward
