# coding: utf-8
from copy import deepcopy
from decimal import Decimal as D

import pytest
from hamcrest import contains_string, equal_to

from balance import balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import constants as cnst
from btestlib import matchers
from btestlib import utils
from btestlib.data import simpleapi_defaults
from btestlib.data.defaults import Date
from cashmachines.data.constants import CMNds
from simpleapi.common import payment_methods
from useful_utils import export_all_for_client, print_all_for_client

generate_acts = steps.CommonPartnerSteps.generate_partner_acts_fair_and_export

# todo убрать лишние print, export
# todo проставить марки features, smoke

check_oebs_export = False

SRV_620 = cnst.Services.RED_MARKET_BALANCE
SRV_618 = cnst.Services.RED_MARKET_SERVICES

# сейчас платежи только в рублях (c) buxxter
PAYMENT_CURRENCY = cnst.Currencies.RUB
CONTRACT_CURRENCY = cnst.Currencies.USD

# создаем платежи не на сегодня, чтобы проверить что конвертация валюты платежа происходит на дату платежа
PAYMENT_DT = utils.Date.shift(Date.NOW(), months=-1)

month1_start_dt, month1_end_dt, month2_start_dt, month2_end_dt, month3_start_dt, month3_end_dt = \
    utils.Date.previous_three_months_start_end_dates()

PRODUCT_ID_620 = 509232


# todo убрать из paymentData все кроме commission и price. price сделать по дефолту simple.DEFAULT_PRICE
class PaymentData(object):
    def __init__(self, commission_category, fee, price, qty, fiscal_nds=CMNds.NDS_18):
        self.commission_category = commission_category
        self.fee = fee
        self.price = price
        self.qty = qty
        self.fiscal_nds = fiscal_nds


def test_create_partner_temp():
    create_partner_and_person()


def test_create_offer_temp():
    partner_id, contract_id, _ = create_contract()
    # partner_id = 83591411
    api.medium().GetClientContracts({'ClientID': partner_id})
    export_all_for_client(partner_id, check_oebs_export)


def test_create_common_contract_temp():
    partner_id, contract_id, _ = create_contract(is_offer=False)
    # partner_id = 83577147
    api.medium().GetClientContracts({'ClientID': partner_id})
    export_all_for_client(partner_id, check_oebs_export)


# проверяем что платеж не экспортируется к нам пока не установлена дата PayoutReady
def test_payment_wo_payout_ready():
    partner_id, contract_id, person_id = create_contract()

    print_all_for_client(partner_id)

    payment_data_list = [
        PaymentData(commission_category=D('1000'), fee=None, price=D('100'), qty='1'),
    ]

    # создаем платеж в трасте
    trust_payment_id, payment_id, purchase_token, _ = create_payment(partner_id, payment_data_list,
                                                                     is_compensation=False,
                                                                     currency=PAYMENT_CURRENCY)

    # проверяем, что при обработке платежа происходит ошибка
    with pytest.raises(utils.XmlRpc.XmlRpcError) as exc:
        steps.CommonPartnerSteps.export_payment(payment_id)

    utils.check_that(exc.value.response, contains_string('No payout_ready_dt'))

    # проверяем, что транзакций нет в t_thirdparty_transactions
    check_payment(payment_id, trust_payment_id, contract_id, partner_id, person_id, payment_data_list,
                  payment_not_exported=True)

    export_all_for_client(partner_id, check_oebs_export)


# проверка платежа
@pytest.mark.parametrize('is_compensation', [
    False,
    True
], ids=lambda is_compensation: 'is_compensation: {}'.format(is_compensation))
def test_payment_simple(is_compensation):
    partner_id, contract_id, person_id = create_contract()

    print_all_for_client(partner_id)

    payment_data_list = [
        PaymentData(commission_category=D('1000'), fee=None, price=D('100'), qty='1'),
    ]

    # создаем платеж в трасте
    trust_payment_id, payment_id, purchase_token, _ = create_payment(partner_id, payment_data_list,
                                                                     is_compensation=is_compensation,
                                                                     currency=PAYMENT_CURRENCY)
    steps.CommonPartnerSteps.set_payment_dt(payment_id, PAYMENT_DT)

    if not is_compensation:
        payout_ready_dt = Date.TODAY
        api.medium().UpdatePayment({'TrustPaymentID': trust_payment_id}, {'PayoutReady': payout_ready_dt})
    else:
        payout_ready_dt = None

    # экспортируем платеж
    steps.CommonPartnerSteps.export_payment(payment_id)

    if PAYMENT_CURRENCY != CONTRACT_CURRENCY:
        # конвертируем сумму платежа в валюту договора по курсу на дату платежа
        usd_rate = steps.CurrencySteps.get_currency_rate(PAYMENT_DT, CONTRACT_CURRENCY.char_code,
                                                         PAYMENT_CURRENCY.char_code, cnst.CurrencyRateSource.CBR.id)
        for pd in payment_data_list:
            pd.price = pd.price / usd_rate

    if is_compensation:
        check_compensation_payment(payment_id, trust_payment_id, contract_id, partner_id, person_id, payment_data_list)
    else:
        check_payment(payment_id, trust_payment_id, contract_id, partner_id, person_id, payment_data_list,
                      payout_ready_dt=payout_ready_dt)

    export_all_for_client(partner_id, check_oebs_export)


# проверка возврата
@pytest.mark.parametrize('is_compensation', [
    False,
    True
], ids=lambda is_compensation: 'is_compensation: {}'.format(is_compensation))
def test_refund_simple(is_compensation):
    partner_id, contract_id, person_id = create_contract()

    print_all_for_client(partner_id)

    payment_data_list = [
        PaymentData(commission_category=D('1000'), fee=None, price=simpleapi_defaults.DEFAULT_PRICE, qty='1'),
    ]

    # создаем платеж в трасте
    trust_payment_id, payment_id, purchase_token, service_order_id_list = \
        create_payment(partner_id, payment_data_list,
                       is_compensation=is_compensation,
                       currency=PAYMENT_CURRENCY)

    trust_refund_id, refund_id = steps.SimpleApi.create_multiple_refunds(SRV_620, service_order_id_list,
                                                                         trust_payment_id)

    steps.CommonPartnerSteps.set_payment_dt(payment_id, PAYMENT_DT)

    if not is_compensation:
        payout_ready_dt = Date.TODAY
        api.medium().UpdatePayment({'TrustPaymentID': trust_payment_id}, {'PayoutReady': payout_ready_dt})
    else:
        payout_ready_dt = None

    # экспортируем платеж
    steps.CommonPartnerSteps.export_payment(payment_id)
    # экспортируем рефанд
    steps.CommonPartnerSteps.export_payment(refund_id)

    if PAYMENT_CURRENCY != CONTRACT_CURRENCY:
        # конвертируем сумму платежа в валюту договора по курсу на дату платежу (не рефанда!)
        usd_rate = steps.CurrencySteps.get_currency_rate(PAYMENT_DT, CONTRACT_CURRENCY.char_code,
                                                         PAYMENT_CURRENCY.char_code, cnst.CurrencyRateSource.CBR.id)
        for pd in payment_data_list:
            pd.price = pd.price / usd_rate

    if is_compensation:
        check_compensation_refund(payment_id, trust_payment_id, trust_refund_id, contract_id, partner_id, person_id,
                                  payment_data_list)
    else:
        check_refund(payment_id, trust_payment_id, trust_refund_id, contract_id, partner_id, person_id,
                     payment_data_list,
                     payout_ready_dt)

    export_all_for_client(partner_id, check_oebs_export)


#     ---------- ЗАКРЫТИЕ

def test_close_month_wo_data():
    partner_id, contract_id, person_id = create_contract()

    print_all_for_client(partner_id)

    invoice_eid_agent_reward = get_personal_invoices_eids(contract_id)

    reward_sum = D('0')

    # вызываем генерацию актов
    month_start_dt, month_end_dt = utils.Date.current_month_first_and_last_days()

    # generate_acts(partner_id, contract_id, month_start_dt)
    generate_acts(partner_id, contract_id, Date.NOW(), manual_export=False)

    # проверяем данные счетов
    check_invoices(contract_id, person_id, reward_sum, invoice_eid_agent_reward)

    # проверяем, что в счетах консьюмы выставлены по правильным сервисам
    check_consumes(contract_id, reward_sum)

    # проверяем данные актов
    check_acts(contract_id, reward_sum, act_dt=month_end_dt, total_acts_qty_for_contract=0)

    print_all_for_client(partner_id)
    export_all_for_client(partner_id, check_oebs_export)


def test_close_one_month():
    partner_id, contract_id, person_id = create_contract()
    invoice_eid_agent_reward = get_personal_invoices_eids(contract_id)

    payment_amount_usd = 250
    commission_pct = 15
    reward = calc_reward(payment_amount_usd, commission_pct)

    create_fake_thirdparty_payment(partner_id, person_id, contract_id, payment_amount_usd, reward=reward,
                                   dt=month2_start_dt)

    # вызываем генерацию актов
    generate_acts(partner_id, contract_id, dt=month2_start_dt)

    # проверяем данные счетов
    check_invoices(contract_id, person_id, reward, invoice_eid_agent_reward)

    # проверяем, что в счетах консьюмы выставлены по правильным сервисам
    check_consumes(contract_id, reward)

    # проверяем данные актов
    check_acts(contract_id, reward, act_dt=month2_end_dt, total_acts_qty_for_contract=1)

    print_all_for_client(partner_id)
    export_all_for_client(partner_id, check_oebs_export, fake_transactions=True)


# закрытие с платежами за два месяца (нарастающий итог)
def test_close_two_months():
    partner_id, contract_id, person_id = create_contract(start_dt=month1_start_dt, via_trust=False)

    agent_reward_invoice_eid = get_personal_invoices_eids(contract_id)

    # ПЕРВЫЙ ПЛАТЕЖ
    payment_amount_usd = 250
    commission_pct = 15
    reward_1 = calc_reward(payment_amount_usd, commission_pct)

    create_fake_thirdparty_payment(partner_id, person_id, contract_id, payment_amount_usd, reward=reward_1,
                                   dt=month2_start_dt)

    # вызываем генерацию актов
    generate_acts(partner_id, contract_id, month2_start_dt)

    # ВТОРОЙ ПЛАТЕЖ В ЭТОМ ЖЕ МЕСЯЦЕ
    payment_amount_usd = 750
    commission_pct = 26
    reward_2 = calc_reward(payment_amount_usd, commission_pct)

    create_fake_thirdparty_payment(partner_id, person_id, contract_id, payment_amount_usd, reward=reward_2,
                                   dt=month2_start_dt)

    # ПЛАТЕЖ В СЛЕДУЮЩЕМ МЕСЯЦЕ
    payment_amount_usd = 1250
    commission_pct = 37
    reward_3 = calc_reward(payment_amount_usd, commission_pct)

    create_fake_thirdparty_payment(partner_id, person_id, contract_id, payment_amount_usd, reward=reward_3,
                                   dt=month3_start_dt)

    # вызываем генерацию актов
    generate_acts(partner_id, contract_id, month3_start_dt)

    reward_23 = reward_2 + reward_3
    reward_123 = reward_1 + reward_2 + reward_3

    # проверяем данные счетов
    check_invoices(contract_id, person_id, reward_123, agent_reward_invoice_eid)

    # проверяем, что в счетах консьюмы выставлены по правильным сервисам
    check_consumes(contract_id, reward_123)

    # проверяем данные актов
    check_acts(contract_id, reward_23, act_dt=month3_end_dt, total_acts_qty_for_contract=2)

    print_all_for_client(partner_id)


# отчет агента
def test_agent_report():
    partner_id, contract_id, person_id = create_contract()

    payment_amount_usd = 25
    commission_pct = 3
    reward = calc_reward(payment_amount_usd, commission_pct)

    create_fake_thirdparty_payment(partner_id, person_id, contract_id, payment_amount_usd, reward=reward,
                                   dt=month2_start_dt)

    # вызываем генерацию актов
    generate_acts(partner_id, contract_id, month2_start_dt)

    act_dt = utils.Date.nullify_time_of_date(month2_end_dt)

    check_acts(contract_id, reward, act_dt=act_dt)

    query_acts = '''
                    SELECT i.id AS invoice_id, a.id AS act_id
                    FROM T_ACT a,
                      t_invoice i
                    WHERE a.INVOICE_ID = i.ID
                    AND i.CONTRACT_ID = :contract_id
                    AND a.dt = :dt'''
    act_data = db.balance().execute(query_acts, {'contract_id': contract_id, 'dt': act_dt}, single_row=True)

    expected_agent_report_data = {
        'service_id': SRV_620.id,
        'act_id': act_data['act_id'],
        'act_qty': reward,
        'invoice_id': act_data['invoice_id'],
        'act_amount': reward,
        'dt': act_dt,
        'currency': CONTRACT_CURRENCY.char_code,
        'contract_id': contract_id
    }

    # проверяем акт в v_rep_agent_rep
    agent_report_data = steps.CommonPartnerSteps.get_data_from_agent_rep(contract_id)[0]

    utils.check_that(agent_report_data, matchers.equal_to_casted_dict(expected_agent_report_data),
                     'Проверяем, что есть данные в агент репорте')


def test_close_all_transactions():
    partner_id, contract_id, person_id = create_contract()
    invoice_eid_agent_reward = get_personal_invoices_eids(contract_id)

    # ПЛАТЕЖ

    payment_amount_usd = 250
    commission_pct = 15
    reward = calc_reward(payment_amount_usd, commission_pct)

    create_fake_thirdparty_payment(partner_id, person_id, contract_id, payment_amount_usd, reward=reward,
                                   dt=month2_start_dt)

    # ВОЗВРАТ
    payment_amount_usd = 15
    create_fake_thirdparty_refund(partner_id, person_id, contract_id, payment_amount_usd, dt=month2_start_dt)

    # КОМПЕНСАНЦИЯ
    payment_amount_usd = 30
    create_fake_thirdparty_compensation_payment(partner_id, person_id, contract_id, payment_amount_usd,
                                                dt=month2_start_dt)

    # ВОЗВРАТ КОМПЕНСАНЦИИ
    payment_amount_usd = 5
    create_fake_thirdparty_compensation_refund(partner_id, person_id, contract_id, payment_amount_usd,
                                               dt=month2_start_dt)

    # ЗАКРЫТИЕ

    # вызываем генерацию актов
    generate_acts(partner_id, contract_id, month2_start_dt)

    # проверяем данные счетов
    check_invoices(contract_id, person_id, reward, invoice_eid_agent_reward)

    # проверяем, что в счетах консьюмы выставлены по правильным сервисам
    check_consumes(contract_id, reward)

    # проверяем данные актов
    check_acts(contract_id, reward, act_dt=month2_end_dt, total_acts_qty_for_contract=1)

    print_all_for_client(partner_id)
    export_all_for_client(partner_id, check_oebs_export)


def test_close_all_transactions_real():
    partner_id, contract_id, person_id = create_contract()
    invoice_eid_agent_reward = get_personal_invoices_eids(contract_id)

    # ПЛАТЕЖ

    payment_data_list = [
        PaymentData(commission_category=D('1000'), fee=None, price=D('100'), qty='1'),
        # PaymentData(commission_category=D('2000'), fee=1, price=D('200'), qty='2'),
        # PaymentData(commission_category=D('3000'), fee=2, price=D('300'), qty='3'),
        # PaymentData(commission_category=D('400'), fee=3, price=D('400'), qty='4'),
        # PaymentData(commission_category=D('0'), fee=4, price=D('500'), qty='5'),

        # PaymentData(commission_category=D('500'), fee=None, price=D('100000'), qty='1'),
    ]

    # создаем платеж в трасте
    trust_payment_id, payment_id, purchase_token, _ = create_payment(partner_id, payment_data_list,
                                                                     is_compensation=False,
                                                                     currency=PAYMENT_CURRENCY)
    steps.CommonPartnerSteps.set_payment_dt(payment_id, PAYMENT_DT)

    payout_ready_dt = Date.TODAY
    api.medium().UpdatePayment({'TrustPaymentID': trust_payment_id}, {'PayoutReady': payout_ready_dt})

    # экспортируем платеж
    steps.CommonPartnerSteps.export_payment(payment_id)

    if PAYMENT_CURRENCY != CONTRACT_CURRENCY:
        # конвертируем сумму платежа в валюту договора по курсу на дату платежа
        usd_rate = steps.CurrencySteps.get_currency_rate(PAYMENT_DT, CONTRACT_CURRENCY.char_code,
                                                         PAYMENT_CURRENCY.char_code, cnst.CurrencyRateSource.CBR.id)
        for pd in payment_data_list:
            pd.price = pd.price / usd_rate

    reward_sum = calc_reward_sum(payment_data_list)

    check_payment(payment_id, trust_payment_id, contract_id, partner_id, person_id, payment_data_list,
                  payout_ready_dt=payout_ready_dt)

    # ВОЗВРАТ

    payment_data_list = [
        PaymentData(commission_category=D('1000'), fee=None, price=simpleapi_defaults.DEFAULT_PRICE, qty='1'),
        # PaymentData(commission_category=D('2000'), fee=1, price=simpleapi_defaults.DEFAULT_PRICE, qty='2'),
        # PaymentData(commission_category=D('3000'), fee=2, price=D('300'), qty='3'),
        # PaymentData(commission_category=D('400'), fee=3, price=D('400'), qty='4'),
        # PaymentData(commission_category=D('0'), fee=4, price=D('500'), qty='5'),

        # PaymentData(commission_category=D('500'), fee=None, price=D('100000'), qty='1'),
    ]

    # создаем платеж в трасте
    trust_payment_id, payment_id, purchase_token, service_order_id_list = \
        create_payment(partner_id, payment_data_list,
                       is_compensation=False,
                       currency=PAYMENT_CURRENCY)

    trust_refund_id, refund_id = steps.SimpleApi.create_multiple_refunds(SRV_620, service_order_id_list,
                                                                         trust_payment_id)

    steps.CommonPartnerSteps.set_payment_dt(payment_id, PAYMENT_DT)

    payout_ready_dt = Date.TODAY
    api.medium().UpdatePayment({'TrustPaymentID': trust_payment_id}, {'PayoutReady': payout_ready_dt})

    # экспортируем платеж
    steps.CommonPartnerSteps.export_payment(payment_id)
    # экспортируем рефанд
    steps.CommonPartnerSteps.export_payment(refund_id)

    if PAYMENT_CURRENCY != CONTRACT_CURRENCY:
        # конвертируем сумму платежа в валюту договора по курсу на дату платежу (не рефанда!)
        usd_rate = steps.CurrencySteps.get_currency_rate(PAYMENT_DT, CONTRACT_CURRENCY.char_code,
                                                         PAYMENT_CURRENCY.char_code, cnst.CurrencyRateSource.CBR.id)
        for pd in payment_data_list:
            pd.price = pd.price / usd_rate

    reward_sum = reward_sum + calc_reward_sum(payment_data_list)

    check_refund(payment_id, trust_payment_id, trust_refund_id, contract_id, partner_id, person_id,
                 payment_data_list,
                 payout_ready_dt)

    # КОМПЕНСАНЦИЯ

    payment_data_list = [
        PaymentData(commission_category=D('1000'), fee=None, price=D('100'), qty='1'),
        # PaymentData(commission_category=D('2000'), fee=1, price=D('200'), qty='2'),
        # PaymentData(commission_category=D('3000'), fee=2, price=D('300'), qty='3'),
        # PaymentData(commission_category=D('400'), fee=3, price=D('400'), qty='4'),
        # PaymentData(commission_category=D('0'), fee=4, price=D('500'), qty='5'),

        # PaymentData(commission_category=D('500'), fee=None, price=D('100000'), qty='1'),
    ]

    # создаем платеж в трасте
    trust_payment_id, payment_id, purchase_token, _ = create_payment(partner_id, payment_data_list,
                                                                     is_compensation=True,
                                                                     currency=PAYMENT_CURRENCY)
    steps.CommonPartnerSteps.set_payment_dt(payment_id, PAYMENT_DT)

    # экспортируем платеж
    steps.CommonPartnerSteps.export_payment(payment_id)

    if PAYMENT_CURRENCY != CONTRACT_CURRENCY:
        # конвертируем сумму платежа в валюту договора по курсу на дату платежа
        usd_rate = steps.CurrencySteps.get_currency_rate(PAYMENT_DT, CONTRACT_CURRENCY.char_code,
                                                         PAYMENT_CURRENCY.char_code, cnst.CurrencyRateSource.CBR.id)
        for pd in payment_data_list:
            pd.price = pd.price / usd_rate

    check_compensation_payment(payment_id, trust_payment_id, contract_id, partner_id, person_id, payment_data_list)

    # ВОЗВРАТ КОМПЕНСАНЦИИ

    payment_data_list = [
        PaymentData(commission_category=D('1000'), fee=None, price=simpleapi_defaults.DEFAULT_PRICE, qty='1'),
        # PaymentData(commission_category=D('2000'), fee=1, price=simpleapi_defaults.DEFAULT_PRICE, qty='2'),
        # PaymentData(commission_category=D('3000'), fee=2, price=D('300'), qty='3'),
        # PaymentData(commission_category=D('400'), fee=3, price=D('400'), qty='4'),
        # PaymentData(commission_category=D('0'), fee=4, price=D('500'), qty='5'),

        # PaymentData(commission_category=D('500'), fee=None, price=D('100000'), qty='1'),
    ]

    # создаем платеж в трасте
    trust_payment_id, payment_id, purchase_token, service_order_id_list = \
        create_payment(partner_id, payment_data_list,
                       is_compensation=True,
                       currency=PAYMENT_CURRENCY)

    trust_refund_id, refund_id = steps.SimpleApi.create_multiple_refunds(SRV_620, service_order_id_list,
                                                                         trust_payment_id)

    steps.CommonPartnerSteps.set_payment_dt(payment_id, PAYMENT_DT)

    # экспортируем платеж
    steps.CommonPartnerSteps.export_payment(payment_id)
    # экспортируем рефанд
    steps.CommonPartnerSteps.export_payment(refund_id)

    if PAYMENT_CURRENCY != CONTRACT_CURRENCY:
        # конвертируем сумму платежа в валюту договора по курсу на дату платежу (не рефанда!)
        usd_rate = steps.CurrencySteps.get_currency_rate(PAYMENT_DT, CONTRACT_CURRENCY.char_code,
                                                         PAYMENT_CURRENCY.char_code, cnst.CurrencyRateSource.CBR.id)
        for pd in payment_data_list:
            pd.price = pd.price / usd_rate

    check_compensation_refund(payment_id, trust_payment_id, trust_refund_id, contract_id, partner_id, person_id,
                              payment_data_list)

    # ЗАКРЫТИЕ

    # акты генерируются по tpt, а там везде сегодняшняя дата
    cur_month_start_dt, cur_month_end_dt = utils.Date.current_month_first_and_last_days()

    # вызываем генерацию актов
    if reward_sum != 0:
        generate_acts(partner_id, contract_id, cur_month_start_dt)
    else:
        generate_acts(partner_id, contract_id, cur_month_start_dt, manual_export=False)

    # проверяем данные счетов
    check_invoices(contract_id, person_id, reward_sum, invoice_eid_agent_reward)

    # проверяем, что в счетах консьюмы выставлены по правильным сервисам
    check_consumes(contract_id, reward_sum)

    # проверяем данные актов
    acts_qty = 0 if reward_sum == 0 else 1
    check_acts(contract_id, reward_sum, act_dt=cur_month_end_dt, total_acts_qty_for_contract=acts_qty)

    print_all_for_client(partner_id)
    export_all_for_client(partner_id, check_oebs_export)


# ----- utils


def create_partner_and_person():
    partner_id = steps.SimpleApi.create_partner(SRV_620)
    person_id = steps.PersonSteps.create(partner_id, cnst.PersonTypes.SW_YT.code)
    return partner_id, person_id


def create_contract(is_offer=True, start_dt=month1_start_dt, via_trust=True):
    partner_id = steps.SimpleApi.create_partner(SRV_620) if via_trust else steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(partner_id, cnst.PersonTypes.SW_YT.code)

    contract_params = {'client_id': partner_id,
                       'person_id': person_id,
                       'firm_id': cnst.Firms.EUROPE_AG_7.id,
                       'currency': CONTRACT_CURRENCY.char_code,
                       'manager_uid': cnst.Managers.SOME_MANAGER.uid,
                       'services': [SRV_620.id, SRV_618.id],
                       'payment_type': cnst.ContractPaymentType.POSTPAY,
                       'payment_term': 10,
                       'personal_account': 1,
                       'partner_credit': 1,
                       'netting': 1,
                       }

    if start_dt is not None:
        contract_params.update({'start_dt': start_dt})

    if is_offer:
        contract_params.update({'offer_confirmation_type': cnst.OfferConfirmationType.NO.value})
        contract_id, _ = steps.ContractSteps.create_offer(contract_params)
    else:
        contract_params.update({'signed': 1})
        contract_id, _ = steps.ContractSteps.create_common_contract(contract_params)

    return partner_id, contract_id, person_id


def create_payment(partner_id, payment_data_list, is_compensation=False, currency=PAYMENT_CURRENCY):
    commission_category_list = [pd.commission_category for pd in payment_data_list]
    payments_fees = [pd.fee for pd in payment_data_list]
    product_prices = [pd.price for pd in payment_data_list]
    qty_list = [pd.qty for pd in payment_data_list]
    # fiscal_nds_list = [pd.fiscal_nds for pd in payment_data_list]

    # создадим трастовые продукты и продукт фи
    product_list = [
        steps.SimpleApi.create_service_product(SRV_620, partner_id, service_fee=i)
        for i in payments_fees]

    payment_method = payment_methods.Compensation() if is_compensation else None

    # создадим трастовый платеж в зависимости от требуемых service_fee
    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(service=SRV_620,
                                                       service_product_id_list=product_list,
                                                       currency=currency,
                                                       prices_list=product_prices,
                                                       paymethod=payment_method,
                                                       commission_category_list=commission_category_list,
                                                       # order_dt=payment_dt
                                                       # fiscal_nds_list=fiscal_nds_list,
                                                       # user=user,
                                                       # back_url='https://user-balance.greed-tm.paysys.yandex.ru',
                                                       # qty_list=qty_list
                                                       )

    return trust_payment_id, payment_id, purchase_token, service_order_id_list


def create_fake_thirdparty_payment(partner_id, person_id, contract_id, payment_amount, reward, dt):
    steps.SimpleApi.create_fake_thirdparty_payment(simpleapi_defaults.ThirdPartyData.RED_MARKET, contract_id, person_id,
                                                   partner_id, amount=payment_amount, reward=reward,
                                                   transaction_type=cnst.TransactionType.PAYMENT,
                                                   invoice_eid=None, dt=dt, payout_ready_dt=dt)


def create_fake_thirdparty_refund(partner_id, person_id, contract_id, payment_amount, dt):
    steps.SimpleApi.create_fake_thirdparty_payment(simpleapi_defaults.ThirdPartyData.RED_MARKET, contract_id, person_id,
                                                   partner_id, amount=payment_amount, reward=None,
                                                   transaction_type=cnst.TransactionType.REFUND,
                                                   invoice_eid=None, dt=dt, payout_ready_dt=dt)


def _create_fake_thirdparty_compensation(transaction_type, partner_id, person_id, contract_id, payment_amount, dt):
    steps.SimpleApi.create_fake_thirdparty_payment(simpleapi_defaults.ThirdPartyData.RED_MARKET, contract_id, person_id,
                                                   partner_id, amount=payment_amount, reward=None,
                                                   transaction_type=transaction_type,
                                                   payment_type=cnst.PaymentType.COMPENSATION,
                                                   paysys_type=cnst.PaysysType.YANDEX,
                                                   invoice_eid=None, dt=dt, payout_ready_dt=None)


def create_fake_thirdparty_compensation_payment(partner_id, person_id, contract_id, payment_amount, dt):
    _create_fake_thirdparty_compensation(cnst.TransactionType.PAYMENT, partner_id, person_id, contract_id,
                                         payment_amount, dt)


def create_fake_thirdparty_compensation_refund(partner_id, person_id, contract_id, payment_amount, dt):
    _create_fake_thirdparty_compensation(cnst.TransactionType.REFUND, partner_id, person_id, contract_id,
                                         payment_amount, dt)


def check_payment(payment_id, trust_payment_id, contract_id, partner_id,
                  person_id, payment_data_list, payout_ready_dt=None, payment_not_exported=False):
    trust_id = trust_payment_id

    if payment_not_exported:
        expected_payment_lines = []
    else:
        expected_payment_lines = []
        common_expected_data = common_tpt_expected_data(contract_id, partner_id, payment_id, person_id,
                                                        trust_id, trust_payment_id)
        for pd in payment_data_list:
            amount = utils.dround2(pd.price)
            reward = calc_reward(amount, pd.commission_category / D('100'))

            # если тут что-то изменится, то не забыть обновить test_station_acts.create_fake_thirdparty_payment
            expected_payment_lines.append(utils.merge_dicts([deepcopy(common_expected_data), {
                'transaction_type': cnst.TransactionType.PAYMENT.name,
                'service_id': SRV_620.id,
                'paysys_type_cc': cnst.PaysysType.MONEY,
                'payment_type': cnst.PaymentType.CARD,
                'amount': amount,
                'yandex_reward': reward,
                'yandex_reward_wo_nds': reward,
                'invoice_eid': None,
                'payout_ready_dt': payout_ready_dt,
            }]))

    # получаем данные по платежу
    actual_payment_lines = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, trust_id=trust_id,
                                                                                             transaction_type=None)

    # Проверяем данные платежа
    utils.check_that(actual_payment_lines,
                     matchers.contains_dicts_with_entries(expected_payment_lines, same_length=True),
                     u'Проверяем данные платежа')


def check_compensation_payment(payment_id, trust_payment_id, contract_id, partner_id, person_id, payment_data_list):
    trust_id = trust_payment_id

    common_expected_data = common_tpt_expected_data(contract_id, partner_id, payment_id, person_id,
                                                    trust_id, trust_payment_id)

    expected_payment_lines = []
    for pd in payment_data_list:
        amount = utils.dround2(pd.price)

        expected_payment_lines.append(utils.merge_dicts([deepcopy(common_expected_data), {
            'transaction_type': cnst.TransactionType.PAYMENT.name,
            'service_id': SRV_620.id,
            'paysys_type_cc': cnst.PaysysType.YANDEX,
            'payment_type': cnst.PaymentType.COMPENSATION,
            'amount': amount,
            'yandex_reward': None,
            'yandex_reward_wo_nds': None,
            'invoice_eid': None,
            'payout_ready_dt': None,
        }]))

    # получаем данные по платежу
    actual_payment_lines = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, trust_id=trust_id,
                                                                                             transaction_type=None)

    # Проверяем данные платежа
    utils.check_that(actual_payment_lines,
                     matchers.contains_dicts_with_entries(expected_payment_lines, same_length=True),
                     u'Проверяем данные платежа')


def check_refund(payment_id, trust_payment_id, trust_refund_id, contract_id, partner_id, person_id, payment_data_list,
                 payout_ready_dt):
    trust_id = trust_refund_id

    common_expected_data = common_tpt_expected_data(contract_id, partner_id, payment_id, person_id,
                                                    trust_id, trust_payment_id)

    expected_refund_lines = []
    for pd in payment_data_list:
        amount = utils.dround2(pd.price)

        expected_refund_lines.append(utils.merge_dicts([common_expected_data, {
            'transaction_type': cnst.TransactionType.REFUND.name,
            'service_id': SRV_620.id,
            'paysys_type_cc': cnst.PaysysType.MONEY,
            'payment_type': cnst.PaymentType.CARD,
            'amount': amount,
            'yandex_reward': None,
            'yandex_reward_wo_nds': None,
            'invoice_eid': None,
            'payout_ready_dt': payout_ready_dt,
        }]))

    # получаем данные по возврату
    actual_refund_lines = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, trust_id=trust_id,
                                                                                            transaction_type=None)

    # Проверяем данные возврата
    utils.check_that(actual_refund_lines, matchers.contains_dicts_with_entries(expected_refund_lines, same_length=True),
                     u'Проверяем данные возврата')


def check_compensation_refund(payment_id, trust_payment_id, trust_refund_id, contract_id, partner_id, person_id,
                              payment_data_list):
    trust_id = trust_refund_id

    common_expected_data = common_tpt_expected_data(contract_id, partner_id, payment_id, person_id,
                                                    trust_id, trust_payment_id)
    expected_refund_lines = []
    for pd in payment_data_list:
        amount = utils.dround2(pd.price)
        expected_refund_lines.append(utils.merge_dicts([common_expected_data, {
            'transaction_type': cnst.TransactionType.REFUND.name,
            'service_id': SRV_620.id,
            'paysys_type_cc': cnst.PaysysType.YANDEX,
            'payment_type': cnst.PaymentType.COMPENSATION,
            'amount': amount,
            'yandex_reward': None,
            'yandex_reward_wo_nds': None,
            'invoice_eid': None,
            'payout_ready_dt': None,
        }]))

    # получаем данные по возврату
    actual_refund_lines = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, trust_id=trust_id,
                                                                                            transaction_type=None)

    # Проверяем данные возврата
    utils.check_that(actual_refund_lines, matchers.contains_dicts_with_entries(expected_refund_lines, same_length=True),
                     u'Проверяем данные возврата')


def calc_reward(payment_amount, partner_commission):
    amount = utils.dround2(payment_amount)
    reward = utils.dround2(utils.pct_sum(amount, partner_commission))
    return reward


def calc_reward_sum(payment_data_list):
    return sum([calc_reward(pd.price, pd.commission_category / D('100')) for pd in payment_data_list])


def get_personal_invoices_eids(contract_id):
    invoice_data = db.balance().execute("SELECT i.EXTERNAL_ID, i.id, e.VALUE_STR "
                                        "FROM T_EXTPROPS e, t_invoice i "
                                        "WHERE e.OBJECT_ID = i.id "
                                        "AND e.CLASSNAME = 'PersonalAccount' "
                                        "and e.VALUE_STR = 'AGENT_REWARD' "
                                        "AND i.CONTRACT_ID = :contract_id", {'contract_id': contract_id},
                                        single_row=True)

    return invoice_data['external_id']


def common_tpt_expected_data(contract_id, partner_id, payment_id, person_id, trust_id, trust_payment_id):
    return {
        'payment_id': payment_id,
        'trust_id': trust_id,
        'trust_payment_id': trust_payment_id,
        'contract_id': contract_id,
        'person_id': person_id,
        'partner_id': partner_id,
        'oebs_org_id': cnst.Firms.EUROPE_AG_7.oebs_org_id,

        'currency': PAYMENT_CURRENCY.char_code,
        'iso_currency': PAYMENT_CURRENCY.iso_code,
        'partner_currency': CONTRACT_CURRENCY.char_code,
        'partner_iso_currency': CONTRACT_CURRENCY.iso_code,
        'commission_currency': CONTRACT_CURRENCY.char_code,
        'commission_iso_currency': CONTRACT_CURRENCY.iso_code,

        'product_id': None,
        'service_product_id': None,
        'amount_fee': None,
        'client_id': None,
        'client_amount': None,
        'paysys_partner_id': None,

        'internal': None,
        'transaction_type': None,
        'service_id': None,
        'paysys_type_cc': None,
        'payment_type': None,
        'amount': None,
        'yandex_reward': None,
        'invoice_eid': None,

    }


def check_invoices(contract_id, person_id, reward, agent_reward_invoice_eid):
    expected_invoice_lines = [
        {
            'firm_id': cnst.Firms.EUROPE_AG_7.id,
            'type': cnst.InvoiceType.PERSONAL_ACCOUNT,
            'nds_pct': cnst.Nds.ZERO,
            'nds': 0,
            'currency': CONTRACT_CURRENCY.char_code,
            'paysys_id': cnst.Paysyses.BANK_SW_YT_USD.id,
            'contract_id': contract_id,
            'person_id': person_id,
            'receipt_sum': 0,
            'consume_sum': reward,
            'total_act_sum': reward,
            'external_id': agent_reward_invoice_eid,
        },
        {
            'firm_id': cnst.Firms.EUROPE_AG_7.id,
            'type': cnst.InvoiceType.PERSONAL_ACCOUNT,
            'nds_pct': cnst.Nds.ZERO,
            'nds': 0,
            'currency': CONTRACT_CURRENCY.char_code,
            'paysys_id': cnst.Paysyses.BANK_SW_YT_USD.id,
            'contract_id': contract_id,
            'person_id': person_id,
            'receipt_sum': 0,
            'consume_sum': D('0'),
            'total_act_sum': D('0'),
        }
    ]

    actual_invoice_lines = db.balance().execute('SELECT * FROM t_invoice WHERE CONTRACT_ID = :contract_id',
                                                {'contract_id': contract_id})

    utils.check_that(actual_invoice_lines,
                     matchers.contains_dicts_with_entries(expected_invoice_lines, same_length=True),
                     'Проверяем данные счетов')


# проверяем, что в счетах консьюмы выставлены по правильным сервисам
def check_consumes(contract_id, reward):
    expected_consume_lines = []

    if reward > 0:
        expected_consume_lines.append({
            'completion_sum': reward,
            'act_sum': reward,
            'service_id': SRV_620.id,
            'service_code': PRODUCT_ID_620,
        })

    query_consumes = '''
            SELECT c.completion_sum, c.ACT_SUM, o.SERVICE_ID, o.SERVICE_CODE
            FROM T_CONSUME c,
              t_order o,
              t_invoice i
            WHERE i.CONTRACT_ID = :contract_id
            AND c.PARENT_ORDER_ID = o.id
            AND i.id = c.INVOICE_ID'''

    actual_consume_lines = db.balance().execute(query_consumes, {'contract_id': contract_id})

    utils.check_that(actual_consume_lines,
                     matchers.contains_dicts_with_entries(expected_consume_lines, same_length=True),
                     'Проверяем данные консьюмов')


def check_acts(contract_id, reward, act_dt, total_acts_qty_for_contract=1):
    expected_act_lines = []

    if reward > 0:
        expected_act_lines.append({
            'dt': utils.Date.nullify_time_of_date(act_dt),
            'amount': reward,
            'act_sum': reward,
            'type': cnst.ActType.GENERIC,
        })

    query_acts = '''
            SELECT a.dt, a.AMOUNT, a.ACT_SUM, a.TYPE
            FROM T_ACT a,
              t_invoice i
            WHERE a.INVOICE_ID = i.ID
            AND i.CONTRACT_ID = :contract_id
            AND a.dt = :dt'''
    actual_act_lines = db.balance().execute(query_acts,
                                            {'contract_id': contract_id,
                                             'dt': utils.Date.nullify_time_of_date(act_dt)})

    query_acts_qty = '''
                SELECT count(1) AS qty
                FROM T_ACT a,
                  t_invoice i
                WHERE a.INVOICE_ID = i.ID
                AND i.CONTRACT_ID = :contract_id'''
    actual_qty = db.balance().execute(query_acts_qty, {'contract_id': contract_id}, single_row=True)['qty']

    utils.check_that(actual_act_lines, matchers.contains_dicts_with_entries(expected_act_lines, same_length=True),
                     'Проверяем данные актов')
    utils.check_that(actual_qty, equal_to(total_acts_qty_for_contract),
                     'Проверяем общее количество актов по договору')
