# coding: utf-8

from dateutil.relativedelta import relativedelta
from datetime import datetime, timedelta
import hamcrest as hm
import pytest

from balance import balance_steps as steps  # , balance_db as db
from balance.features import Features
from btestlib import utils
import btestlib.reporter as reporter
from btestlib.data.partner_contexts import \
    TICKETS_118_CONTEXT, EVENTS_TICKETS2_RU_CONTEXT, EVENTS_TICKETS2_KZ_CONTEXT, \
    EVENTS_TICKETS_CONTEXT, EVENTS_TICKETS3_RU_CONTEXT
from btestlib.constants import Services, PaymentType, PaysysType, TransactionType, PaymentMethods as pm
from btestlib.constants import Export
from btestlib.matchers import contains_dicts_with_entries

pytestmark = [
    reporter.feature(Features.SIDEPAYMENT, Features.PAYMENT,
                     Features.EVENTS_TICKETS, Features.EVENTS_TICKETS_NEW,
                     Features.TICKETS,
                     ),
]

START_DT = utils.Date.first_day_of_month() - relativedelta(months=1)

NO_INVOICE_EID_SIDS = frozenset(s.id for s in (Services.TICKETS,))

all_contexts = {
    '118': TICKETS_118_CONTEXT,
    '126': EVENTS_TICKETS_CONTEXT,
    '131': EVENTS_TICKETS2_RU_CONTEXT,
    '638': EVENTS_TICKETS3_RU_CONTEXT,
    '131-kz': EVENTS_TICKETS2_KZ_CONTEXT,
}

err_trans_lst = []
parametrize_context = pytest.mark.parametrize('context_id', all_contexts)

parametrize_transaction_params = pytest.mark.parametrize('product, payment_type, amount, reward', [
    pytest.param('ticket', pm.CARD.cc, 100, 10),
    pytest.param('ticket', pm.CARD.cc, 100, 0),
    pytest.param('supplement', PaymentType.CARD, 250, 12),
    pytest.param('supplement', PaymentType.CARD, 250, 0),
    pytest.param('fee', PaymentType.CARD, 9, 0),
    pytest.param('certificate', PaymentType.CARD, 9, 0),
    pytest.param('agent_commission_nds_20', PaymentType.CARD, 0, 20),
    pytest.param('agent_commission_nds_0', PaymentType.CARD, 0, 100),
    pytest.param('yandex_account', PaymentType.YANDEX_ACCOUNT_WITHDRAW, 2000, 200.2),
    pytest.param('yandex_account', PaymentType.YANDEX_ACCOUNT_WITHDRAW, 2000, 0),
    pytest.param('new_promocode', PaymentType.NEW_PROMOCODE, 111, 0),
    pytest.param('new_promocode', 'virtual::' + PaymentType.NEW_PROMOCODE, 111, 0),
    pytest.param('marketing_promocode', PaymentType.MARKETING_PROMO, 222, 0),
    pytest.param('marketing_promocode', 'virtual::' + PaymentType.MARKETING_PROMO, 222, 0),
    pytest.param('certificate_promocode', 'certificate_promocode', 666, 0),
    pytest.param('certificate_promocode', 'virtual::certificate_promocode', 666, 0),
    pytest.param('certificate_activation', 'certificate_activation', 666, 0),
    # todo добавить PaymentType из соседней ветки
    pytest.param('compensation', PaymentType.COMPENSATION, 1030, 0),
    pytest.param('compensation_discount', PaymentType.COMPENSATION_DISCOUNT, 1030, 0),
    pytest.param('virtual::card_discount', 'virtual::card_discount', 100, 0)
])

# global used
# globals()['used'] = {
#     'client_id': set(),
#     'person_id': set(),
#     'contract_id': set(),
#     'invoice_id': set(),
#     'collateral_id': set(),
#     'transaction_id': set()
# }
# globals()['errors'] = []


def test_import_from_yt():
    # steps.CommonPartnerSteps.create_partner_completions_resource('tickets', datetime(2021, 11, 18))
    # steps.CommonPartnerSteps.create_partner_completions_resource('tickets', datetime(2021, 11, 19), {'force': 1})
    # steps.CommonPartnerSteps.process_partners_completions('tickets', datetime(2021, 11, 19))
    # steps.CommonPartnerSteps.create_partner_completions_resource('tickets', datetime(2021, 11, 20))
    # steps.CommonPartnerSteps.process_partners_completions('tickets', datetime(2021, 11, 20))
    # steps.CommonPartnerSteps.create_partner_completions_resource('tickets', datetime(2021, 11, 21))
    # steps.CommonPartnerSteps.process_partners_completions('tickets', datetime(2021, 11, 21))
    # steps.CommonPartnerSteps.create_partner_completions_resource('tickets', datetime(2021, 11, 22))
    # steps.CommonPartnerSteps.process_partners_completions('tickets', datetime(2021, 11, 22))
    # for i in range(19, 23):
    #     steps.CommonPartnerSteps.create_partner_completions_resource('tickets2', datetime(2021, 11, i))
    #     steps.CommonPartnerSteps.process_partners_completions('tickets2', datetime(2021, 11, i))

    # err_lst = []
    # tr_types_map = {'payment': TransactionType.PAYMENT, 'refund': TransactionType.REFUND}

    # all_services = [118, 126, 131, 638]
    # for i in range(2224190399, 2224190471):
    #     steps.CommonPartnerSteps.export_sidepayment(i)
    # steps.CommonPartnerSteps.export_sidepayment(2224188821)
    # steps.CommonPartnerSteps.export_sidepayment(2224188909)
    # steps.CommonPartnerSteps.export_sidepayment(2224188910)
    # steps.CommonPartnerSteps.export_sidepayment(2224190072)
    # steps.CommonPartnerSteps.export_sidepayment(2224190073)
    # steps.CommonPartnerSteps.export_sidepayment(2224189069)
    # steps.CommonPartnerSteps.export_sidepayment(2224189070)

    # for j in range(19, 23):
    #     for service in all_services:
    #         lstt = steps.CommonPartnerSteps.get_sidepayments_by_service_and_dt(service, datetime(2021, 11, j),
    #                                                                              datetime(2021, 11, j + 1))
    #         for i in lstt:
    #             try:
    #                 steps.CommonPartnerSteps.export_sidepayment(i['id'])
    #             except Exception as e:
    #                 err = e
    #                 try:
    #                     client_id, person_id, contract_id = create_contract(str(i['service_id']), i['client_id'])
    #                     steps.CommonPartnerSteps.export_sidepayment(i['id'])
    #                 except Exception as ee:
    #                     err_lst.append(dict(data=i['id'], first_err=err))
    #     err_lst = []
    #
    # err_lst = []
    # used = {
    #     'client_id': set(),
    #     'person_id': set(),
    #     'contract_id': set(),
    #     'invoice_id': set(),
    #     'collateral_id': set(),
    #     'transaction_id': set()
    # }
    # for j in range(19, 23):
    #     for service in all_services:
    #         lstt = steps.CommonPartnerSteps.get_sidepayments_by_service_and_dt(service, datetime(2021, 11, j),
    #                                                                            datetime(2021, 11, j + 1))
    #         for i in lstt:
    #             payment_data = get_thirdparty_transactions(i['id'], tr_types_map[i['transaction_type']])
    #             try:
    #                 used, errors = export_oebs(payment_data, used)
    #                 if len(errors) > 0:
    #                     for kl in errors:
    #                         err_lst.append(dict(data=payment_data, error=kl))
    #             except Exception as e:
    #                 err_lst.append(dict(data=payment_data, error=e))
    #
    # for i in err_lst:
    #     try:
    #         used, errors = export_oebs(i['data'], used)
    #     except Exception as e:
    #         pass
    pass


@parametrize_context
@parametrize_transaction_params
def test_payment(context_id, product, payment_type, amount, reward):
    context = all_contexts[context_id]
    if context.name == 'EVENTS_TICKETS2_KZ_CONTEXT' and product in ('yandex_account', 'compensation_discount'):
        return
    elif context.name in ('TICKETS_118_CONTEXT', 'EVENTS_TICKETS2_KZ_CONTEXT') and product == 'agent_commission_nds_0':
        return

    client_id, person_id, contract_id = create_contract(context_id)
    # создаем платеж
    trust_payment_id = steps.SimpleApi.generate_fake_trust_payment_id()
    side_payment_id, payment_transaction_id = create_transaction(
        context,
        TransactionType.PAYMENT,
        payment_type,
        trust_payment_id,
        amount,
        reward,
        product,
        client_id,
    )

    # обрабатываем платеж
    export(side_payment_id)
    expected_payment = create_expected_thirdparty(
        context,
        TransactionType.PAYMENT,
        payment_type,
        trust_payment_id,
        amount,
        reward,
        product,
        client_id,
        contract_id,
        person_id,
        side_payment_id,
    )

    payment_data = get_thirdparty_transactions(side_payment_id, (TransactionType.REFUND
                                                                 if product == 'agent_commission_nds_0'
                                                                 else TransactionType.PAYMENT))
    utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                     'Сравниваем платеж с шаблоном')

    # globals()['used'], globals()['errors'] = export_oebs(payment_data, globals()['used'])


@parametrize_context
@parametrize_transaction_params
def test_refund(context_id, product, payment_type, amount, reward):
    context = all_contexts[context_id]
    if context.name == 'EVENTS_TICKETS2_KZ_CONTEXT' and product in ('yandex_account', 'compensation_discount'):
        return
    elif context.name in ('TICKETS_118_CONTEXT', 'EVENTS_TICKETS2_KZ_CONTEXT') and product == 'agent_commission_nds_0':
        return

    client_id, person_id, contract_id = create_contract(context_id)
    # создаем платеж
    trust_payment_id = steps.SimpleApi.generate_fake_trust_payment_id()
    side_payment_id, payment_transaction_id = create_transaction(
        context,
        TransactionType.PAYMENT,
        payment_type,
        trust_payment_id,
        amount,
        reward,
        product,
        client_id,
    )

    side_refund_id, refund_transaction_id = create_transaction(
        context,
        TransactionType.REFUND,
        payment_type,
        trust_payment_id,
        amount,
        reward,
        product,
        client_id,
    )

    # обрабатываем платеж:
    export(side_payment_id)
    # обрабатываем возврат
    export(side_refund_id)
    expected_refund = create_expected_thirdparty(
        context,
        TransactionType.REFUND,
        payment_type,
        trust_payment_id,
        amount,
        reward,
        product,
        client_id,
        contract_id,
        person_id,
        side_refund_id,
    )

    refund_data = get_thirdparty_transactions(side_refund_id, (TransactionType.PAYMENT
                                                               if product == 'agent_commission_nds_0'
                                                               else TransactionType.REFUND))
    utils.check_that(refund_data, contains_dicts_with_entries([expected_refund]),
                     'Сравниваем возврат с шаблоном')

    # globals()['used'], globals()['errors'] = export_oebs(refund_data, globals()['used'])


@parametrize_context
@parametrize_transaction_params
def test_payout(context_id, product, payment_type, amount, reward):
    context = all_contexts[context_id]
    if context.name == 'EVENTS_TICKETS2_KZ_CONTEXT' and product in ('yandex_account', 'compensation_discount'):
        return
    elif context.name in ('TICKETS_118_CONTEXT', 'EVENTS_TICKETS2_KZ_CONTEXT') and product == 'agent_commission_nds_0':
        return

    client_id, person_id, contract_id = create_contract(context_id)
    # создаем платеж
    trust_payment_id = steps.SimpleApi.generate_fake_trust_payment_id()
    payment_dt = utils.Date.moscow_offset_dt()
    side_payment_id, payment_transaction_id = create_transaction(
        context,
        TransactionType.PAYMENT,
        payment_type,
        trust_payment_id,
        amount,
        reward,
        product,
        client_id,
        dt=payment_dt,
        orig_transaction_id_from_transaction_id=True
    )

    refund_dt = utils.Date.moscow_offset_dt()
    side_refund_id, refund_transaction_id = create_transaction(
        context,
        TransactionType.REFUND,
        payment_type,
        trust_payment_id,
        amount,
        reward,
        product,
        client_id,
        dt=refund_dt,
        orig_transaction_id_from_transaction_id=True
    )

    # обрабатываем платеж:
    export(side_payment_id)
    expected_payment = create_expected_thirdparty(
        context,
        TransactionType.PAYMENT,
        payment_type,
        trust_payment_id,
        amount,
        reward,
        product,
        client_id,
        contract_id,
        person_id,
        side_payment_id,
    )

    payment_data = get_thirdparty_transactions(side_payment_id, (TransactionType.REFUND
                                                                 if product == 'agent_commission_nds_0'
                                                                 else TransactionType.PAYMENT))
    utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                     'Сравниваем платеж с шаблоном')

    # globals()['used'], globals()['errors'] = export_oebs(payment_data, globals()['used'])

    # обрабатываем возврат
    export(side_refund_id)
    expected_refund = create_expected_thirdparty(
        context,
        TransactionType.REFUND,
        payment_type,
        trust_payment_id,
        amount,
        reward,
        product,
        client_id,
        contract_id,
        person_id,
        side_refund_id,
    )

    refund_data = get_thirdparty_transactions(side_refund_id, (TransactionType.PAYMENT
                                                               if product == 'agent_commission_nds_0'
                                                               else TransactionType.REFUND))
    utils.check_that(refund_data, contains_dicts_with_entries([expected_refund]),
                     'Сравниваем возврат с шаблоном')

    # globals()['used'], globals()['errors'] = export_oebs(refund_data, globals()['used'])

    payout_payment_dt = utils.Date.moscow_offset_dt()
    side_payment_payout_id, _ = create_transaction(
        context,
        TransactionType.PAYMENT,
        payment_type,
        trust_payment_id,
        amount,
        reward,
        product,
        client_id,
        is_payout=True,
        dt=payment_dt,
        transaction_dt=payout_payment_dt,
        orig_transaction_id=payment_data[0]['trust_payment_id']
    )

    # проводим выплату
    export(side_payment_payout_id)
    payment_data = get_thirdparty_transactions(side_payment_payout_id, (TransactionType.REFUND
                                                                        if product == 'agent_commission_nds_0'
                                                                        else TransactionType.PAYMENT))
    utils.check_that(payment_data, hm.empty(), u'Проверяем что платежа для выплаты не создано')
    export(side_payment_id, with_export_record=False)

    expected_payment.update({'payout_ready_dt': utils.Date.date_to_iso_format(payout_payment_dt)})

    payment_data = get_thirdparty_transactions(side_payment_id, (TransactionType.REFUND
                                                                 if product == 'agent_commission_nds_0'
                                                                 else TransactionType.PAYMENT))

    # проверим что payout_ready_dt проставлено
    payment_data[0]['payout_ready_dt'] = \
        utils.Date.date_to_iso_format(utils.Date.moscow_offset_dt(payment_data[0]['payout_ready_dt']))
    utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                     'Сравниваем платеж с шаблоном')

    # globals()['used'], globals()['errors'] = export_oebs(payment_data, globals()['used'])

    payout_refund_dt = utils.Date.moscow_offset_dt()
    side_refund_payout_id, _ = create_transaction(
        context,
        TransactionType.REFUND,
        payment_type,
        trust_payment_id,
        amount,
        reward,
        product,
        client_id,
        is_payout=True,
        dt=refund_dt,
        transaction_dt=payout_refund_dt,
        orig_transaction_id=refund_data[0]['trust_payment_id']
    )

    export(side_refund_payout_id)
    refund_data = get_thirdparty_transactions(side_refund_payout_id, (TransactionType.PAYMENT
                                                                      if product == 'agent_commission_nds_0'
                                                                      else TransactionType.REFUND))
    utils.check_that(refund_data, hm.empty(), u'Проверяем что возврата для выплаты не создано')
    export(side_refund_id, with_export_record=False)

    expected_refund.update({'payout_ready_dt': utils.Date.date_to_iso_format(payout_refund_dt)})

    payment_data = get_thirdparty_transactions(side_payment_id, (TransactionType.REFUND
                                                                 if product == 'agent_commission_nds_0'
                                                                 else TransactionType.PAYMENT))
    payment_data[0]['payout_ready_dt'] = \
        utils.Date.date_to_iso_format(utils.Date.moscow_offset_dt(payment_data[0]['payout_ready_dt']))

    refund_data = get_thirdparty_transactions(side_refund_id, (TransactionType.PAYMENT
                                                               if product == 'agent_commission_nds_0'
                                                               else TransactionType.REFUND))
    refund_data[0]['payout_ready_dt'] = \
        utils.Date.date_to_iso_format(utils.Date.moscow_offset_dt(refund_data[0]['payout_ready_dt']))

    # проверим что payout_ready_dt не изменился
    utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                     'Сравниваем платеж с шаблоном')
    utils.check_that(refund_data, contains_dicts_with_entries([expected_refund]),
                     'Сравниваем возврат с шаблоном')

    # globals()['used'], globals()['errors'] = export_oebs(payment_data, globals()['used'])
    # globals()['used'], globals()['errors'] = export_oebs(refund_data, globals()['used'])


@parametrize_context
@parametrize_transaction_params
def test_payout2(context_id, product, payment_type, amount, reward):
    context = all_contexts[context_id]
    if context.name == 'EVENTS_TICKETS2_KZ_CONTEXT' and product in ('yandex_account', 'compensation_discount'):
        return
    elif context.name in ('TICKETS_118_CONTEXT', 'EVENTS_TICKETS2_KZ_CONTEXT') and product == 'agent_commission_nds_0':
        return

    client_id, person_id, contract_id = create_contract(context_id)
    # создаем платеж
    trust_payment_id = steps.SimpleApi.generate_fake_trust_payment_id()
    payment_dt = utils.Date.moscow_offset_dt()
    side_payment_id, payment_transaction_id = create_transaction(
        context,
        TransactionType.PAYMENT,
        payment_type,
        trust_payment_id,
        amount,
        reward,
        product,
        client_id,
        dt=payment_dt,
        orig_transaction_id_from_transaction_id=True
    )

    # обрабатываем платеж:
    export(side_payment_id)
    expected_payment = create_expected_thirdparty(
        context,
        TransactionType.PAYMENT,
        payment_type,
        trust_payment_id,
        amount,
        reward,
        product,
        client_id,
        contract_id,
        person_id,
        side_payment_id,
    )

    payment_data = get_thirdparty_transactions(side_payment_id, (TransactionType.REFUND
                                                                 if product == 'agent_commission_nds_0'
                                                                 else TransactionType.PAYMENT))
    utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                     'Сравниваем платеж с шаблоном')

    # globals()['used'], globals()['errors'] = export_oebs(payment_data, globals()['used'])

    payout_payment_dt = utils.Date.moscow_offset_dt()
    side_payment_payout_id, _ = create_transaction(
        context,
        TransactionType.PAYMENT,
        payment_type,
        trust_payment_id,
        amount,
        reward,
        product,
        client_id,
        is_payout=True,
        dt=payment_dt,
        transaction_dt=payout_payment_dt,
        orig_transaction_id=payment_data[0]['trust_payment_id']
    )

    # проводим выплату
    export(side_payment_payout_id)
    payment_data = get_thirdparty_transactions(side_payment_payout_id, (TransactionType.REFUND
                                                                        if product == 'agent_commission_nds_0'
                                                                        else TransactionType.PAYMENT))
    utils.check_that(payment_data, hm.empty(), u'Проверяем что платежа для выплаты не создано')
    export(side_payment_id, with_export_record=False)

    expected_payment.update({'payout_ready_dt': utils.Date.date_to_iso_format(payout_payment_dt)})

    payment_data = get_thirdparty_transactions(side_payment_id, (TransactionType.REFUND
                                                                 if product == 'agent_commission_nds_0'
                                                                 else TransactionType.PAYMENT))

    # проверим что payout_ready_dt проставлено
    payment_data[0]['payout_ready_dt'] = \
        utils.Date.date_to_iso_format(utils.Date.moscow_offset_dt(payment_data[0]['payout_ready_dt']))
    utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                     'Сравниваем платеж с шаблоном')

    # globals()['used'], globals()['errors'] = export_oebs(payment_data, globals()['used'])

    refund_dt = utils.Date.moscow_offset_dt()
    side_refund_id, refund_transaction_id = create_transaction(
        context,
        TransactionType.REFUND,
        payment_type,
        trust_payment_id,
        amount,
        reward,
        product,
        client_id,
        dt=refund_dt,
        orig_transaction_id_from_transaction_id=True
    )

    # обрабатываем возврат
    export(side_refund_id)
    expected_refund = create_expected_thirdparty(
        context,
        TransactionType.REFUND,
        payment_type,
        trust_payment_id,
        amount,
        reward,
        product,
        client_id,
        contract_id,
        person_id,
        side_refund_id,
    )

    refund_data = get_thirdparty_transactions(side_refund_id, (TransactionType.PAYMENT
                                                               if product == 'agent_commission_nds_0'
                                                               else TransactionType.REFUND))
    utils.check_that(refund_data, contains_dicts_with_entries([expected_refund]),
                     'Сравниваем возврат с шаблоном')

    # globals()['used'], globals()['errors'] = export_oebs(refund_data, globals()['used'])

    payout_refund_dt = utils.Date.moscow_offset_dt()
    side_refund_payout_id, _ = create_transaction(
        context,
        TransactionType.REFUND,
        payment_type,
        trust_payment_id,
        amount,
        reward,
        product,
        client_id,
        is_payout=True,
        dt=refund_dt,
        transaction_dt=payout_refund_dt,
        orig_transaction_id=refund_data[0]['trust_payment_id']
    )

    export(side_refund_payout_id)
    refund_data = get_thirdparty_transactions(side_refund_payout_id, (TransactionType.PAYMENT
                                                                      if product == 'agent_commission_nds_0'
                                                                      else TransactionType.REFUND))
    utils.check_that(refund_data, hm.empty(), u'Проверяем что возврата для выплаты не создано')
    export(side_refund_id, with_export_record=False)

    expected_refund.update({'payout_ready_dt': utils.Date.date_to_iso_format(payout_refund_dt)})

    payment_data = get_thirdparty_transactions(side_payment_id, (TransactionType.REFUND
                                                               if product == 'agent_commission_nds_0'
                                                               else TransactionType.PAYMENT))
    payment_data[0]['payout_ready_dt'] = \
        utils.Date.date_to_iso_format(utils.Date.moscow_offset_dt(payment_data[0]['payout_ready_dt']))
    refund_data = get_thirdparty_transactions(side_refund_id, (TransactionType.PAYMENT
                                                               if product == 'agent_commission_nds_0'
                                                               else TransactionType.REFUND))
    refund_data[0]['payout_ready_dt'] = \
        utils.Date.date_to_iso_format(utils.Date.moscow_offset_dt(refund_data[0]['payout_ready_dt']))

    # проверим что payout_ready_dt не изменился
    utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                     'Сравниваем платеж с шаблоном')
    utils.check_that(refund_data, contains_dicts_with_entries([expected_refund]),
                     'Сравниваем возврат с шаблоном')

    # globals()['used'], globals()['errors'] = export_oebs(payment_data, globals()['used'])
    # globals()['used'], globals()['errors'] = export_oebs(refund_data, globals()['used'])


@parametrize_context
def test_fake_refund(context_id):
    context = all_contexts[context_id]
    if context.name == 'EVENTS_TICKETS2_KZ_CONTEXT':
        return

    product, payment_type, amount, reward = 'fake_refund', 'afisha_fake_refund', 100, 0
    client_id, person_id, contract_id = create_contract(context_id)
    # создаем платеж
    trust_payment_id = steps.SimpleApi.generate_fake_trust_payment_id()
    side_payment_id, payment_transaction_id = create_transaction(
        context,
        TransactionType.PAYMENT,
        payment_type,
        trust_payment_id,
        amount,
        reward,
        product,
        client_id,
    )

    # обрабатываем платеж
    export(side_payment_id)
    expected_payment = create_expected_thirdparty(
        context,
        TransactionType.PAYMENT,
        payment_type,
        trust_payment_id,
        amount,
        reward,
        product,
        client_id,
        contract_id,
        person_id,
        side_payment_id,
    )

    payment_data = get_thirdparty_transactions(side_payment_id, TransactionType.PAYMENT)
    utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                     'Сравниваем платеж с шаблоном')

    # globals()['used'], globals()['errors'] = export_oebs(payment_data, globals()['used'])


@parametrize_context
@pytest.mark.parametrize('product', [
    'partner_discount',
    'partner_promocode',
    'partner_loyalty_points',
    'noncompensated_discount',
    'noncompensated_promocode'
])
def test_skipped_product(context_id, product):
    context = all_contexts[context_id]
    payment_type, amount, reward = PaymentType.CARD, 100, 10
    client_id, person_id, contract_id = create_contract(context_id)
    # создаем платеж
    trust_payment_id = steps.SimpleApi.generate_fake_trust_payment_id()
    side_payment_id, payment_transaction_id = create_transaction(
        context,
        TransactionType.PAYMENT,
        payment_type,
        trust_payment_id,
        amount,
        reward,
        product,
        client_id,
    )

    # обрабатываем платеж
    export_result = export(side_payment_id)

    payment_data = get_thirdparty_transactions(side_payment_id, TransactionType.PAYMENT)
    utils.check_that(payment_data, hm.empty(), 'Проверяем, что транзакций не создано')
    utils.check_that(export_result['state'], hm.equal_to('1'), u'Проверяем, что транзакция обработана успешно, но платеж пропущен')


# Utils
@utils.memoize
def create_contract(context_id, client_id=None):
    context = all_contexts[context_id]
    with reporter.step(u'Создаем договор для клиента-партнера'):
        # создаем клиента-партнера
        new_client_id = client_id or steps.SimpleApi.create_partner(context.service)

        contract_additional_params = {'start_dt': START_DT}
        _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
            context, client_id=new_client_id, additional_params=contract_additional_params)

        return new_client_id, person_id, contract_id


def create_transaction(
        context,
        transaction_type,
        payment_type,
        trust_payment_id,
        amount,
        reward,
        product,
        client_id,
        is_payout=False,  # по умолчанию - начисление
        payload=None,
        dt=None,
        transaction_dt=None,
        orig_transaction_id=None,
        orig_transaction_id_from_transaction_id=False,
):
    if payment_type[:7] == 'virtual':
        terminal_pm = 'virtual'
    elif payment_type in ('compensation', 'compensation_discount', 'certificate_activation'):
        terminal_pm = 'card'
    elif payment_type in ('marketing_promocode', 'certificate_promocode'):
        terminal_pm = 'composite'  # there are no exactly suitable terminals in test db
    else:
        terminal_pm = payment_type

    return steps.PartnerSteps.create_sidepayment_transaction(
        client_id, dt or utils.Date.moscow_offset_dt(),
        amount,
        remove_prefix(payment_type, ['virtual::']),  # todo ждем что Сергей и Влад добавят поле
        context.service.id,
        transaction_type=transaction_type,
        currency=context.currency,
        extra_str_0=trust_payment_id,  # trust_payment_id
        extra_str_1=datetime.now() + timedelta(days=10),  # дата бизнес события - due
        extra_str_2=product,
        extra_num_0=reward,  # yandex_reward
        extra_num_1=steps.FakeTrustApi.get_terminal_id_for(context.currency, terminal_pm),  # terminal_id
        extra_num_2=int(not is_payout),  # признак: начисление или перечисление
        payload=payload,
        transaction_dt=transaction_dt,
        orig_transaction_id=orig_transaction_id,
        orig_transaction_id_from_transaction_id=orig_transaction_id_from_transaction_id
    )


def get_expected_amounts(product, amount, reward, context, payment_type):
    amount_fee = None
    if product == 'fee' and not get_expected_internal(context, payment_type, product):
        amount_fee, amount = amount, 0
    elif product == 'agent_commission_nds_0':
        amount, reward = reward, 0
    return amount, reward, amount_fee


def get_expected_internal(context, payment_type, product):
    if product == 'fee' and context.service.id in {Services.EVENTS_TICKETS3.id, Services.EVENTS_TICKETS_NEW.id}:
        return True
    if product == 'agent_commission_nds_0' and context.service.id == Services.EVENTS_TICKETS3.id:
        return True
    # Ниже фигня по идее:
    # if context.service.id == Services.EVENTS_TICKETS3.id and payment_type == PaymentType.YANDEX_ACCOUNT_WITHDRAW:
    #     return True
    return None


def create_expected_thirdparty(
        context,
        transaction_type,
        payment_type,
        trust_payment_id,
        amount,
        reward,
        product,
        client_id,
        contract_id,
        person_id,
        side_id,
):
    amount, reward, amount_fee = get_expected_amounts(product, amount, reward, context, payment_type)
    if product == 'agent_commission_nds_0':
        payment_type = PaymentType.CORRECTION_NETTING

    expected_paysys_type_cc = get_expected_paysys_type_cc(remove_prefix(payment_type, ['virtual::']))

    none_reward = (transaction_type == TransactionType.REFUND and context.service.id in (126, 131)
                   and product != 'agent_commission_nds_0')

    inv_transaction_type = TransactionType.REFUND if transaction_type == TransactionType.PAYMENT \
        else TransactionType.PAYMENT

    pt = remove_prefix(payment_type, ['virtual::', 'afisha_'])
    expected = steps.SimpleApi.create_expected_tpt_row(
        context,
        client_id,
        contract_id,
        person_id,
        trust_payment_id,
        side_id,
        trust=False,
        transaction_type=inv_transaction_type.name if product == 'agent_commission_nds_0' else transaction_type.name,
        payment_type='fake_refund_cert' if pt == 'certificate_activation' else pt,
        paysys_type_cc=('alfa' if context.currency.iso_code == 'KZT' and expected_paysys_type_cc == 'yamoney'
                        else expected_paysys_type_cc),
        amount=amount,
        amount_fee=amount_fee,
        yandex_reward=None if none_reward else reward,
        invoice_eid=get_expected_invoice_eid(context, contract_id, client_id, product),
        internal=get_expected_internal(context, payment_type, product)
    )
    return expected


def export(side_id, with_export_record=True):
    return steps.ExportSteps.create_export_record_and_export(side_id,
                                                             Export.Type.THIRDPARTY_TRANS,
                                                             Export.Classname.SIDE_PAYMENT,
                                                             with_export_record=with_export_record)


def get_thirdparty_transactions(side_id, transaction_type):
    return steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(side_id,
                                                                             transaction_type=transaction_type,
                                                                             source='sidepayment')


def get_expected_invoice_eid(context, contract_id, client_id, product):
    if context.service.id in NO_INVOICE_EID_SIDS:
        return

    if product == 'agent_commission_nds_0':
        return steps.InvoiceSteps.get_invoice_eid(contract_id, client_id, context.currency.char_code, 0)
    return steps.InvoiceSteps.get_invoice_eid(contract_id, client_id, context.currency.char_code, 1)


payment_type_paysys = {
    pm.AFISHA_CERTIFICATE.cc: PaysysType.AFISHA_CERTIFICATE,
    pm.AFISHA_FAKE_REFUND.cc: PaysysType.FAKE_REFUND,
    pm.NEW_PROMOCODE.cc: PaysysType.YANDEX,
    PaymentType.MARKETING_PROMO: PaysysType.MARKETING_PROMO,
    PaymentType.CERTIFICATE_PROMOCODE: PaysysType.CERTIFICATE_PROMO,
    'certificate_activation': 'fake_refund_cert',
    pm.COMPENSATION_DISCOUNT.cc: PaysysType.YANDEX,
    pm.COMPENSATION.cc: PaysysType.YANDEX,
    PaymentType.CORRECTION_NETTING: PaysysType.NETTING_WO_NDS,
    PaymentType.YANDEX_ACCOUNT_WITHDRAW: PaysysType.MONEY
}


def get_expected_paysys_type_cc(payment_type):
    return payment_type_paysys.get(payment_type, PaysysType.MONEY)


def remove_prefix(s, prefixes):
    for prefix in prefixes:
        if s.startswith(prefix):
            return s[len(prefix):]
    return s

#
# def export_oebs(payment_data, used):
#     errors = []
#     for payment in payment_data:
#         if payment['internal']:
#             continue
#
#         if payment['partner_id'] not in used['client_id']:
#             try:
#                 steps.ExportSteps.export_oebs(client_id=payment['partner_id'])
#             except Exception as e:
#                 errors.append(e)
#             used['client_id'].add(payment['partner_id'])
#         if payment['person_id'] not in used['person_id']:
#             try:
#                 steps.ExportSteps.export_oebs(person_id=payment['person_id'])
#             except Exception as e:
#                 errors.append(e)
#             used['person_id'].add(payment['person_id'])
#         if payment['contract_id'] not in used['contract_id']:
#             try:
#                 steps.ExportSteps.export_oebs(contract_id=payment['contract_id'])
#             except Exception as e:
#                 errors.append(e)
#             used['contract_id'].add(payment['contract_id'])
#         collateral_id = steps.ContractSteps.get_contract_collateral_ids(payment['contract_id'])[0]
#         if collateral_id not in used['collateral_id']:
#             try:
#                 steps.ExportSteps.export_oebs(collateral_id=collateral_id)
#             except Exception as e:
#                 errors.append(e)
#             used['collateral_id'].add(collateral_id)
#
#         if payment['invoice_eid'] is not None:
#             data = db.get_invoices_by_contract_id(payment['contract_id'])
#             for invoice in data:
#                 if invoice['external_id'] == payment['invoice_eid']:
#                     invoice_id = invoice['id']
#
#             if invoice_id not in used['invoice_id']:
#                 try:
#                     steps.ExportSteps.export_oebs(invoice_id=invoice_id)
#                 except Exception as e:
#                     errors.append(e)
#                 used['invoice_id'].add(invoice_id)
#         if payment['id'] not in used['transaction_id']:
#             try:
#                 steps.ExportSteps.export_oebs(transaction_id=payment['id'])
#             except Exception as e:
#                 errors.append(e)
#             used['transaction_id'].add(payment['id'])
#
#     return used, errors
    # pass
