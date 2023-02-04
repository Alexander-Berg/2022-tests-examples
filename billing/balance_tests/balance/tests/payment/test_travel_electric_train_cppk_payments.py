# coding: utf-8
from datetime import datetime

import pytest
from hamcrest import has_length

from btestlib import reporter, utils
from btestlib.constants import PaymentType, PaysysType, TransactionType, Export
from btestlib.data.partner_contexts import TRAVEL_ELECTRIC_TRAINS_RU_CONTEXT
from btestlib.data.simpleapi_defaults import DEFAULT_PRICE
from balance import balance_steps as steps
from btestlib.matchers import contains_dicts_with_entries

START_DT = utils.Date.nullify_time_of_date(datetime.now())
TRUST_PAYMENT_ID = "some payment id"
TRUST_REFUND_ID = "some refund id"
context = TRAVEL_ELECTRIC_TRAINS_RU_CONTEXT


def create_ids_for_payment(context, start_dt):
    with reporter.step(u'Создаем договор для клиента-партнера'):
        client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, additional_params={
            'start_dt': start_dt})

        return client_id, person_id, contract_id


def export_and_check_side_payment(side_payment_id, expected_data, context, transaction_type):
    # запускаем обработку платежа
    if transaction_type == TransactionType.PAYMENT:
        steps.ExportSteps.create_export_record_and_export(side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                          Export.Classname.SIDE_PAYMENT, service_id=context.service.id)
    else:
        steps.ExportSteps.create_export_record_and_export(side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                          Export.Classname.SIDE_PAYMENT, service_id=context.service.id,
                                                          with_export_record=False)

    # получаем данные по платежу
    payment_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_sidepayment_id(side_payment_id)

    # сравниваем платеж с шаблоном
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')

    utils.check_that(payment_data, has_length(len(expected_data)), u"Проверяем, что отсутствуют дополнительные записи")


@pytest.mark.parametrize('payment_type, paysys_type_cc, amount, amount_fee, yandex_reward',
                         [pytest.mark.smoke((PaymentType.COST, PaysysType.SBERBANK, DEFAULT_PRICE, None, None)),
                          (PaymentType.REWARD, PaysysType.SBERBANK, 0, None, DEFAULT_PRICE)],
                         ids=['Cost', 'Reward'])
def test_travel_electric_sidepayment(payment_type, paysys_type_cc, amount, amount_fee, yandex_reward):
    client_id, person_id, contract_id = create_ids_for_payment(context, START_DT)

    # создаем сайдпеймент
    side_payment_id, side_transaction_id = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, START_DT, DEFAULT_PRICE,
                                                          payment_type, context.service.id,
                                                          transaction_type=TransactionType.PAYMENT,
                                                          currency=context.currency,
                                                          paysys_type_cc=paysys_type_cc,
                                                          extra_str_1=TRUST_PAYMENT_ID,
                                                          payload="[]")

    side_refund_id, _ = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, START_DT, DEFAULT_PRICE,
                                                          payment_type, context.service.id,
                                                          transaction_type=TransactionType.REFUND,
                                                          orig_transaction_id=side_transaction_id,
                                                          currency=context.currency,
                                                          paysys_type_cc=paysys_type_cc,
                                                          extra_str_1=TRUST_REFUND_ID,
                                                          payload="[]")

    # формируем шаблон для сравнения
    payment_params = dict(payment_type=payment_type,
                          paysys_type_cc=paysys_type_cc,
                          amount=amount,
                          amount_fee=amount_fee,
                          yandex_reward=yandex_reward)

    refund_params = dict({'transaction_type': TransactionType.REFUND.name,
                          'trust_refund_id': TRUST_REFUND_ID}, **payment_params)

    expected_payment = steps.SimpleApi.create_expected_tpt_row(context, client_id, contract_id, person_id,
                                                               TRUST_PAYMENT_ID, side_payment_id, **payment_params)
    expected_refund = steps.SimpleApi.create_expected_tpt_row(context, client_id, contract_id, person_id,
                                                              None, side_refund_id,
                                                              **refund_params)

    export_and_check_side_payment(side_payment_id, [expected_payment], context, TransactionType.PAYMENT)
    export_and_check_side_payment(side_refund_id, [expected_refund], context, TransactionType.REFUND)
