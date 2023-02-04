# -*- coding: utf-8 -*-

__author__ = 'alshkit'

from datetime import datetime
from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import empty

import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_steps as steps
from balance.features import Features
from btestlib import shared, utils
from btestlib.constants import Services, TransactionType, PaymentType, Currencies, Firms, Paysyses, InvoiceType, \
    Managers, ContractPaymentType, PersonTypes, OfferConfirmationType, Products, Nds
from btestlib.data.simpleapi_defaults import ThirdPartyData
from btestlib.matchers import has_entries_casted, equal_to_casted_dict, contains_dicts_with_entries, equal_to
from temp.igogor.balance_objects import Contexts
from btestlib.data.defaults import GeneralPartnerContractDefaults as GenParams
from btestlib.data.partner_contexts import CORP_TAXI_RU_CONTEXT_GENERAL, CORP_TAXI_KZ_CONTEXT_GENERAL

# эту сумму зачисляем на счёт
PERSONAL_ACC_SUM = Decimal('41.05')
# суммы для откруток
PAYMENT_SUM = Decimal('21.63')
REFUND_SUM = Decimal('1.89')
PAYMENT_SUM_INT = Decimal('5.3')
REFUND_SUM_INT = Decimal('2.7')

CONTRACT_START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=2))
PREVIUS_MONTH_START_DT, PREVIUS_MONTH_END_DT = utils.Date.previous_month_first_and_last_days(datetime.today())
CURRENT_MONTH_START_DT, CURRENT_MONTH_END_DT = utils.Date.current_month_first_and_last_days()

fake_tpt_contract_id = 123
fake_tpt_client_id = 123
fake_tpt_person_id = 123

EXPECTED_BALANCE_DATA = {
    'Balance': None,
    'BonusLeft': '0',
    'ClientID': None,
    'ContractID': None,
    'CurrMonthBonus': '0',
    'CurrMonthCharge': '0',
    'SubscriptionBalance': 0,
    'SubscriptionRate': 0}

COMMON_CONTRACT_PARAMS = {
    'manager_uid': Managers.SOME_MANAGER.uid,
    'personal_account': 1,
    'currency': Currencies.RUB.char_code,
    'firm_id': Firms.TAXI_13.id,
    'services': [Services.TAXI_CORP.id],
    'payment_term': None,
    'payment_type': ContractPaymentType.PREPAY,
    'ctype': 'GENERAL',
    'start_dt': CONTRACT_START_DT}


def create_client_persons_contracts_prepay(context, additional_params, is_postpay=1, is_offer=True):
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        is_postpay=is_postpay,
        is_offer=is_offer,
        additional_params=additional_params)
    return client_id, contract_id, person_id


def create_act(dt, client_id, contract_id,
               personal_acc_payment=0.0, payment_sum=0.0,
               refund_sum=0.0, context=CORP_TAXI_RU_CONTEXT_GENERAL):
    first_month_day, last_month_day = utils.Date.current_month_first_and_last_days(dt)
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)
    steps.InvoiceSteps.pay(invoice_id, payment_sum=personal_acc_payment, payment_dt=first_month_day)

    if context == CORP_TAXI_KZ_CONTEXT_GENERAL:
        if payment_sum:
            steps.SimpleApi.create_fake_tpt_data(context, client_id, 666,
                                                 contract_id, first_month_day + relativedelta(days=1),
                                                 [{'client_amount': PAYMENT_SUM_INT,
                                                   'client_id': client_id,
                                                   'amount': 666,
                                                   'transaction_type': TransactionType.PAYMENT,
                                                   'internal': 1}])
        if refund_sum:
            steps.SimpleApi.create_fake_tpt_data(context, client_id, 666,
                                                 contract_id, first_month_day + relativedelta(days=2),
                                                 [{'client_amount': REFUND_SUM_INT,
                                                   'client_id': client_id,
                                                   'amount': 666,
                                                   'transaction_type': TransactionType.REFUND,
                                                   'internal': 1}])

    steps.SimpleApi.create_fake_tpt_data(context, client_id, 666,
                                         contract_id, first_month_day + relativedelta(days=1),
                                         [{'client_amount': payment_sum,
                                           'client_id': client_id,
                                           'amount': 666,
                                           'transaction_type': TransactionType.PAYMENT},
                                          {'client_amount': refund_sum,
                                           'client_id': client_id,
                                           'amount': 666,
                                           'transaction_type': TransactionType.REFUND}])

    steps.TaxiSteps.process_taxi(contract_id, first_month_day + relativedelta(days=2, minutes=5))
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, last_month_day)
    steps.CommonSteps.export('MONTH_PROC', 'Client', client_id)


# проверим, что баланс после откруток и возвратов считается верно
@reporter.feature(Features.PAYMENT, Features.TAXI)
@pytest.mark.parametrize('personal_acc_payment, payment_sum, refund_sum, context',
                         [
                             (0, 0, 0, CORP_TAXI_RU_CONTEXT_GENERAL),
                             (PERSONAL_ACC_SUM, 0, 0, CORP_TAXI_RU_CONTEXT_GENERAL),
                             (PERSONAL_ACC_SUM, PAYMENT_SUM, REFUND_SUM, CORP_TAXI_RU_CONTEXT_GENERAL),
                             (1, PAYMENT_SUM, 0, CORP_TAXI_RU_CONTEXT_GENERAL),
                             (0, 0, 0, CORP_TAXI_KZ_CONTEXT_GENERAL),
                             (PERSONAL_ACC_SUM, 0, 0, CORP_TAXI_KZ_CONTEXT_GENERAL),
                             (PERSONAL_ACC_SUM, PAYMENT_SUM, REFUND_SUM, CORP_TAXI_KZ_CONTEXT_GENERAL),
                             (1, PAYMENT_SUM, 0, CORP_TAXI_KZ_CONTEXT_GENERAL),
                         ],
                         ids=[
                             "No payments RUS",
                             "Personal_acc payment RUS",
                             "Personal_acc payments, payments and refunds RUS",
                             "Only payments RUS",
                             "No payments KZT",
                             "Personal_acc payment KZT",
                             "Personal_acc payments, payments and refunds KZT",
                             "Only payments KZT"
                         ])
def test_taxi_balances(personal_acc_payment, payment_sum, refund_sum, context):
    params = {'start_dt': CONTRACT_START_DT}
    client_id, contract_id, person_id = create_client_persons_contracts_prepay(context, params, is_postpay=0)

    # положим денег на лицевой счёт
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)
    steps.InvoiceSteps.pay(invoice_id, payment_sum=personal_acc_payment, payment_dt=CURRENT_MONTH_START_DT)
    sum_first_month = steps.SimpleApi.create_fake_tpt_data(context, fake_tpt_client_id, fake_tpt_person_id,
                                                           fake_tpt_contract_id,
                                                           CURRENT_MONTH_START_DT,
                                                           [{'client_amount': payment_sum,
                                                             'amount': 0,
                                                             'client_id': client_id,
                                                             'transaction_type': TransactionType.PAYMENT},
                                                            {'client_amount': refund_sum,
                                                             'amount': 0,
                                                             'client_id': client_id,
                                                             'transaction_type': TransactionType.REFUND}],
                                                           sum_key='client_amount')
    if context == CORP_TAXI_KZ_CONTEXT_GENERAL:
        if payment_sum:
            payment_sum_first_month_int = steps.SimpleApi.create_fake_tpt_data(context, fake_tpt_client_id,
                                                                               fake_tpt_person_id,
                                                                               fake_tpt_contract_id,
                                                                               CURRENT_MONTH_START_DT,
                                                                               [{'client_amount': PAYMENT_SUM_INT,
                                                                                 'amount': 0,
                                                                                 'client_id': client_id,
                                                                                 'transaction_type': TransactionType.PAYMENT,
                                                                                 'internal': 1}],
                                                                               sum_key='client_amount')
        if refund_sum:
            refund_sum_first_month_int = steps.SimpleApi.create_fake_tpt_data(context, fake_tpt_client_id,
                                                                              fake_tpt_person_id,
                                                                              fake_tpt_contract_id,
                                                                              CURRENT_MONTH_START_DT,
                                                                              [{'client_amount': REFUND_SUM_INT,
                                                                                'amount': 0,
                                                                                'client_id': client_id,
                                                                                'transaction_type': TransactionType.REFUND,
                                                                                'internal': 1}],
                                                                              sum_key='client_amount')

    steps.TaxiSteps.process_taxi(contract_id, CURRENT_MONTH_START_DT + relativedelta(hours=2))
    taxi_balance_data = steps.PartnerSteps.get_partner_balance(Services.TAXI_CORP, [contract_id])[0]

    curr_month_charge = sum_first_month
    balance = personal_acc_payment - curr_month_charge

    if context == CORP_TAXI_KZ_CONTEXT_GENERAL:
        refund_sum_internal = refund_sum_first_month_int if refund_sum != 0 else 0
        payment_sum_internal = payment_sum_first_month_int if payment_sum != 0 else 0
        balance = balance - refund_sum_internal - payment_sum_internal
        curr_month_charge = curr_month_charge + payment_sum_internal + refund_sum_internal

    expected_data = utils.copy_and_update_dict(EXPECTED_BALANCE_DATA,
                                               {'Balance': balance,
                                                'ClientID': client_id,
                                                'ContractID': contract_id,
                                                'CurrMonthCharge': curr_month_charge,
                                                'PersonalAccountExternalID': external_invoice_id,
                                                'ReceiptSum': personal_acc_payment,
                                                'Currency': context.currency.iso_code
                                                })

    steps.CommonData.create_expected_invoice_data(contract_id, person_id, payment_sum - refund_sum,
                                                  InvoiceType.PERSONAL_ACCOUNT,
                                                  context.tpt_paysys_type,
                                                  context.firm, payment_sum - refund_sum)
    utils.check_that(taxi_balance_data, has_entries_casted(expected_data),
                     'Проверим, что после оплаты и возврата баланс верный')


# тест для GetTaxiBalance после закрытия первого месяца
@reporter.feature(Features.TRUST, Features.PAYMENT, Features.TAXI, Features.COMPENSATION)
@pytest.mark.parametrize(('context'),
                         [
                             (CORP_TAXI_RU_CONTEXT_GENERAL),
                             (CORP_TAXI_KZ_CONTEXT_GENERAL)
                         ],
                         ids=[
                             "RU",
                             "KZ"
                         ])
def test_get_balance_after_act(context):
    params = {'start_dt': CONTRACT_START_DT}
    client_id, contract_id, person_id = create_client_persons_contracts_prepay(context, params, is_postpay=0)

    # создаем акт. Создается за тот месяц, день из которого передаем в качестве даты.
    create_act(PREVIUS_MONTH_START_DT, client_id, contract_id, PERSONAL_ACC_SUM,
               PAYMENT_SUM, context=context)
    taxi_balance_data = steps.PartnerSteps.get_partner_balance(Services.TAXI_CORP, [contract_id])[0]

    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)

    if context == CORP_TAXI_KZ_CONTEXT_GENERAL:
        balance = PERSONAL_ACC_SUM - PAYMENT_SUM - PAYMENT_SUM_INT
    else:
        balance = PERSONAL_ACC_SUM - PAYMENT_SUM

    expected_balance_data = utils.copy_and_update_dict(EXPECTED_BALANCE_DATA,
                                                       {'Balance': balance,
                                                        'ClientID': client_id,
                                                        'ContractID': contract_id,
                                                        'PersonalAccountExternalID': external_invoice_id,
                                                        'ReceiptSum': PERSONAL_ACC_SUM,
                                                        'Currency': context.currency.iso_code})

    utils.check_that(taxi_balance_data, has_entries_casted(expected_balance_data),
                     'Проверим, что после оплаты и возврата баланс верный')


# тест на кэш баланса
@reporter.feature(Features.PAYMENT, Features.TAXI)
@pytest.mark.parametrize(('context'),
                         [
                             (CORP_TAXI_RU_CONTEXT_GENERAL),
                             (CORP_TAXI_KZ_CONTEXT_GENERAL)
                         ],
                         ids=[
                             "RU",
                             "KZ"
                         ])
def test_cache_balance(context):
    params = {'start_dt': CONTRACT_START_DT}
    client_id, contract_id, person_id = create_client_persons_contracts_prepay(context, params, is_postpay=0)

    # положим денег на лицевой счёт
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)
    steps.InvoiceSteps.pay(invoice_id, payment_sum=PERSONAL_ACC_SUM, payment_dt=PREVIUS_MONTH_START_DT)
    # создадим кэш баланса
    api.test_balance().CacheBalance(contract_id)
    # ожидаем, что получим данные отсюда
    expected_data = steps.PartnerSteps.get_partner_balance(Services.TAXI_CORP, [contract_id])[0]
    # создадим открутки
    if context == CORP_TAXI_KZ_CONTEXT_GENERAL:
        steps.SimpleApi.create_fake_tpt_data(context, client_id, person_id,
                                             contract_id, PREVIUS_MONTH_START_DT + relativedelta(days=1),
                                             [{'client_amount': PAYMENT_SUM_INT,
                                               'amount': 0,
                                               'transaction_type': TransactionType.PAYMENT,
                                               'internal': 1}])
    steps.SimpleApi.create_fake_tpt_data(context, client_id, person_id,
                                         contract_id, PREVIUS_MONTH_START_DT + relativedelta(days=1),
                                         [{'client_amount': PAYMENT_SUM,
                                           'amount': 0,
                                           'transaction_type': TransactionType.PAYMENT}])
    steps.TaxiSteps.process_taxi(contract_id, PREVIUS_MONTH_START_DT + relativedelta(days=2))
    # узнаем баланс. Проверим, что он совпадает с ожидаемым.
    taxi_balance_data = steps.PartnerSteps.get_partner_balance(Services.TAXI_CORP, [contract_id])[0]
    utils.check_that(taxi_balance_data, has_entries_casted(expected_data),
                     'Проверим, что после оплаты и возврата баланс верный')


# проверим генерацию актов без данных
@reporter.feature(Features.PAYMENT, Features.TAXI)
@pytest.mark.parametrize(('context'),
                         [
                             (CORP_TAXI_RU_CONTEXT_GENERAL),
                             (CORP_TAXI_KZ_CONTEXT_GENERAL)
                         ],
                         ids=[
                             "RU",
                             "KZ"
                         ])
def test_act_generation_wo_data(context):
    params = {'start_dt': CONTRACT_START_DT}
    client_id, contract_id, person_id = create_client_persons_contracts_prepay(context, params, is_postpay=0)
    _, last_month_day = utils.Date.previous_month_first_and_last_days(CONTRACT_START_DT)
    # Сгенерируем пустой акт без платежей и откруток
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, last_month_day)
    steps.TaxiSteps.process_taxi(contract_id, last_month_day)

    # Возьмём данные по счету
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    # Возьмём данные по акту
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    print context.paysys
    # готовим ожидаемые данные для счёта
    expected_invoice_data = steps.CommonData.create_expected_invoice_data(contract_id, person_id,
                                                                          amount=0,
                                                                          invoice_type=InvoiceType.PERSONAL_ACCOUNT,
                                                                          paysys_id=context.paysys.id,
                                                                          firm=context.firm, total_act_sum=0,
                                                                          currency=context.currency,
                                                                          nds=context.nds)

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, empty(),
                     'Сравниваем данные из акта с шаблоном')


# акты за один месяц
@reporter.feature(Features.PAYMENT, Features.TAXI)
@pytest.mark.parametrize('month_payment_sum, refund_sum, context',
                         [
                             (PAYMENT_SUM, REFUND_SUM, CORP_TAXI_RU_CONTEXT_GENERAL),
                             (3 * PAYMENT_SUM, REFUND_SUM, CORP_TAXI_RU_CONTEXT_GENERAL),
                             (PAYMENT_SUM, REFUND_SUM, CORP_TAXI_KZ_CONTEXT_GENERAL),
                             (3 * PAYMENT_SUM, REFUND_SUM, CORP_TAXI_KZ_CONTEXT_GENERAL)
                         ], ids=['Payments<Invoice_sum RU',
                                 'Payments>Invoice_sum RU',
                                 'Payments<Invoice_sum KZ',
                                 'Payments>Invoice_sum KZ'
                                 ])
def act_generation_first_month(month_payment_sum, refund_sum, context):
    params = {'start_dt': CONTRACT_START_DT}
    client_id, contract_id, person_id = create_client_persons_contracts_prepay(context, params, is_postpay=0)

    # создадим платежи и сгенерируем акт за 1 месяц
    create_act(PREVIUS_MONTH_START_DT, client_id, contract_id, PERSONAL_ACC_SUM, month_payment_sum, refund_sum,
               context=context)

    if context == CORP_TAXI_RU_CONTEXT_GENERAL:
        consume_sum = PERSONAL_ACC_SUM if PERSONAL_ACC_SUM >= month_payment_sum - refund_sum else \
            month_payment_sum - refund_sum
        total_act_sum = month_payment_sum - refund_sum
    else:
        consume_sum = PERSONAL_ACC_SUM if PERSONAL_ACC_SUM >= month_payment_sum - refund_sum else \
            month_payment_sum - refund_sum - REFUND_SUM_INT + PAYMENT_SUM_INT
        total_act_sum = month_payment_sum - refund_sum - REFUND_SUM_INT + PAYMENT_SUM_INT

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)[0]
    # готовим ожидаемые данные для счёта

    expected_invoice_data = steps.CommonData.create_expected_invoice_data(contract_id, person_id,
                                                                          amount=consume_sum,
                                                                          invoice_type=InvoiceType.PERSONAL_ACCOUNT,
                                                                          paysys_id=context.paysys.id,
                                                                          firm=context.firm,
                                                                          total_act_sum=total_act_sum,
                                                                          currency=context.currency,
                                                                          nds=context.nds)

    expected_act_data = {'act_sum': total_act_sum,
                         'amount': total_act_sum,
                         'dt': PREVIUS_MONTH_END_DT,
                         'type': 'generic'
                         }

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, equal_to_casted_dict(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')


# акты за два месяца и накопительный итог
@reporter.feature(Features.PAYMENT, Features.TAXI)
@pytest.mark.parametrize(('context'),
                         [
                             (CORP_TAXI_RU_CONTEXT_GENERAL),
                             (CORP_TAXI_KZ_CONTEXT_GENERAL),
                         ],
                         ids=[
                             "RU",
                             "KZ"
                         ])
def test_act_generation_two_month(context):
    params = {'start_dt': CONTRACT_START_DT}
    client_id, contract_id, person_id = create_client_persons_contracts_prepay(context, params, is_postpay=0)

    # сгенерируем акт за предпредыдущий месяц
    create_act(PREVIUS_MONTH_START_DT - relativedelta(months=1),
               client_id, contract_id, PERSONAL_ACC_SUM, PAYMENT_SUM, context=context)
    # создадим фэйковый платеж в предпредыдущем во имя накопительного итога
    if context == CORP_TAXI_KZ_CONTEXT_GENERAL:
        steps.SimpleApi.create_fake_tpt_data(context, fake_tpt_client_id, fake_tpt_person_id,
                                             fake_tpt_contract_id,
                                             PREVIUS_MONTH_START_DT - relativedelta(days=1),
                                             [{'client_amount': PAYMENT_SUM_INT,
                                               'amount': 0,
                                               'client_id': client_id,
                                               'transaction_type': TransactionType.PAYMENT,
                                               'internal': 1}],
                                             sum_key='client_amount')
    steps.SimpleApi.create_fake_tpt_data(context, fake_tpt_client_id, fake_tpt_person_id,
                                         fake_tpt_contract_id,
                                         PREVIUS_MONTH_START_DT - relativedelta(days=1),
                                         [{'client_amount': PAYMENT_SUM,
                                           'amount': 0,
                                           'client_id': client_id,
                                           'transaction_type': TransactionType.PAYMENT}],
                                         sum_key='client_amount')

    if context == CORP_TAXI_KZ_CONTEXT_GENERAL:
        steps.SimpleApi.create_fake_tpt_data(context, fake_tpt_client_id, fake_tpt_person_id,
                                             fake_tpt_contract_id,
                                             [{'client_amount': PAYMENT_SUM_INT,
                                               'amount': 0,
                                               'client_id': client_id,
                                               'transaction_type': TransactionType.PAYMENT,
                                               'internal': 1}],
                                             sum_key='client_amount')
    steps.SimpleApi.create_fake_tpt_data(context, fake_tpt_client_id, fake_tpt_person_id,
                                         fake_tpt_contract_id,
                                         PREVIUS_MONTH_END_DT,
                                         [{'client_amount': PAYMENT_SUM,
                                           'amount': 0,
                                           'client_id': client_id,
                                           'transaction_type': TransactionType.PAYMENT}],
                                         sum_key='client_amount')

    # закроем предыдущий месяц
    steps.TaxiSteps.process_taxi(contract_id, PREVIUS_MONTH_END_DT)
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, PREVIUS_MONTH_END_DT)
    steps.CommonSteps.export('MONTH_PROC', 'Client', client_id)

    # получим данные для проверки. Проверяем только второй месяц
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    invoice_amount = PAYMENT_SUM * 3
    if context == CORP_TAXI_KZ_CONTEXT_GENERAL:
        invoice_amount += 3 * PAYMENT_SUM_INT

    # Сформируем ожидаемые данные для счета.
    expected_invoice_data = steps.CommonData.create_expected_invoice_data(contract_id, person_id,
                                                                          amount=invoice_amount,
                                                                          invoice_type=InvoiceType.PERSONAL_ACCOUNT,
                                                                          paysys_id=context.paysys.id,
                                                                          firm=context.firm,
                                                                          total_act_sum=invoice_amount,
                                                                          currency=context.currency,
                                                                          nds=context.nds)

    act_amount_first_month = PAYMENT_SUM
    if context == CORP_TAXI_KZ_CONTEXT_GENERAL:
        act_amount_first_month += PAYMENT_SUM_INT

    act_amount_second_month = 2 * PAYMENT_SUM
    if context == CORP_TAXI_KZ_CONTEXT_GENERAL:
        act_amount_second_month += 2 * PAYMENT_SUM_INT

    # сформируем ожидаемые данные для акта.
    expected_act_data_first_month = steps.CommonData.create_expected_act_data(amount=act_amount_first_month,
                                                                              type='generic',
                                                                              act_date=utils.Date.last_day_of_month(
                                                                                  PREVIUS_MONTH_END_DT -
                                                                                  relativedelta(months=1)))
    expected_act_data_second_month = steps.CommonData.create_expected_act_data(amount=act_amount_second_month,
                                                                               type='generic',
                                                                               act_date=PREVIUS_MONTH_END_DT)

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries([expected_act_data_first_month,
                                                            expected_act_data_second_month]),
                     'Сравниваем данные из акт с шаблоном')
