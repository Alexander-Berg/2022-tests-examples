# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import datetime
from decimal import Decimal as D

import pytest
from dateutil.relativedelta import relativedelta

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import TransactionType, ServiceCode, Collateral, PaymentType, PaysysType, ContractType, Products
from btestlib.data import simpleapi_defaults
from btestlib.data.partner_contexts import TELEMEDICINE_CONTEXT
from btestlib.matchers import contains_dicts_with_entries
from cashmachines.data.constants import CMNds

SERVICE = TELEMEDICINE_CONTEXT.service
AGENT_REWARD_PCT = D('24.87')
COMMISSION_PCT = D('25.39')
AGENT_REWARD_PCT_NEW = D('50')
COMMISSION_PCT_NEW = D('20')
PRICE = simpleapi_defaults.DEFAULT_PRICE


contract_start_dt = datetime.datetime.fromordinal(datetime.datetime.today().toordinal()).replace(
        day=1) - datetime.timedelta(days=200)


@utils.memoize
def create_client_person_contract(agent_reward_pct, commission_pct):
    partner_id, product_id = steps.SimpleNewApi.create_partner_with_product(TELEMEDICINE_CONTEXT.service,
                                                                            fiscal_nds=CMNds.NDS_20_120)

    _ = steps.SimpleApi.create_thenumberofthebeast_service_product(TELEMEDICINE_CONTEXT.service,
                                                                   partner_id, service_fee=666,
                                                                   fiscal_nds=CMNds.NDS_20_120,
                                                                   fiscal_title='test_fiscal_title')

    params = {'medicine_pay_commission': agent_reward_pct, 'medicine_pay_commission2': commission_pct,
              'start_dt': datetime.datetime.now() - relativedelta(days=5)}

    _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(TELEMEDICINE_CONTEXT,
                                                                               client_id=partner_id,
                                                                               additional_params=params)

    return partner_id, person_id, product_id, contract_id


def get_expected_and_actual_data(client_id, person_id, contract_id, price, agent_reward_pct, commission_pct,
                                 payment_id, trust_payment_id, trust_refund_id=None, is_refund=False,
                                 nds=CMNds.NDS_20_120, with_product_id=False):
    # получаем external_id счета
    invoice_external_id, _ = steps.InvoiceSteps.get_personal_account_external_id_with_service_code(contract_id,
                                                                                                ServiceCode.YANDEX_SERVICE)

    # вычисляем вознаграждение для основного платежа
    yandex_reward = max(D('0.01'), round(price / D('100') * agent_reward_pct, 2)) if agent_reward_pct > 0 else 0

    # вычисляем сумму корректировки
    amount_correction = max(D('0.01'), round(price / D('100') * commission_pct, 2)) if commission_pct > 0 else 0

    expected_template = [steps.SimpleApi.create_expected_tpt_row(TELEMEDICINE_CONTEXT, client_id, contract_id,
                                                                 person_id, trust_payment_id,
                                                                 payment_id, trust_refund_id,
                                                                 amount=price,
                                                                 yandex_reward=None if is_refund else yandex_reward,
                                                                 product_id=None)]

    if not is_refund:

        product_id = None
        if with_product_id:
            product_id = Products.TELEMEDICINE2_WO_NDS.id \
                if nds in [CMNds.NDS_0, CMNds.NDS_NONE] \
                else Products.TELEMEDICINE2.id

        # у платежа создается две строки, одна из которых рефанд корректировка,
        # поэтому добавляем еще один фильтр в запрос данных, чтобы рефанд корректировка тоже был в выдаче
        additional_params = {
            'yandex_reward': None,
            'transaction_type': TransactionType.REFUND.name,
            'paysys_type_cc': PaysysType.YANDEX,
            'payment_type': PaymentType.CORRECTION_COMMISSION,
            'amount': amount_correction,
            'invoice_eid': invoice_external_id,
            'product_id': product_id
        }
        expected_template.append(steps.SimpleApi.create_expected_tpt_row(TELEMEDICINE_CONTEXT, client_id, contract_id,
                                                                         person_id, trust_payment_id,
                                                                         payment_id,
                                                                         **additional_params))
    expected_template.sort(key=lambda k: k['payment_type'])

    # т.к. в платеже создается рефанд корректировка, а по умолчанию ищем по payment_id и типу refund,
    # то добавляем еще один фильтр, чтобы рефанд корректировка в выдачу не попадал
    if is_refund:
        filter_trust_id = trust_refund_id
        transaction_type = TransactionType.REFUND
    else:
        filter_trust_id = None
        transaction_type = None

    # проверяем платеж или рефанд
    actual_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, transaction_type,
                                                                                    trust_id=filter_trust_id)

    # сортируем данные
    actual_data.sort(key=lambda k: k['payment_type'])

    return expected_template, actual_data


# метод для создания дс
def create_collateral_change_pct(contract_id, start_dt, agent_reward_pct, commission_pct):
    params = {'CONTRACT2_ID': contract_id, 'DT': start_dt, 'IS_SIGNED': start_dt.isoformat(),
              'MEDICINE_PAY_COMMISSION': agent_reward_pct, 'MEDICINE_PAY_COMMISSION2': commission_pct}
    steps.ContractSteps.create_collateral(Collateral.CHANGE_TELEMED_PCT, params)


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


@reporter.feature(Features.TRUST, Features.PAYMENT, Features.MEDICINE, Features.REFUND, Features.COMPENSATION)
@pytest.mark.tickets('BALANCE-25181')
@pytest.mark.parametrize(
    'is_refund, agent_reward_pct, commission_pct, price',
    [
        pytest.param(False, AGENT_REWARD_PCT, COMMISSION_PCT, PRICE, marks=pytest.mark.smoke,
                     id='Payment'),
        pytest.param(True, AGENT_REWARD_PCT, COMMISSION_PCT, PRICE,
                     id='Refund'),
        pytest.param(False, D('0'), D('100'), PRICE,
                     id='Payment with reward_pct = 0'),
        pytest.param(True, D('0'), D('100'), PRICE,
                     id='Refund with reward_pct = 0'),
        pytest.param(False, AGENT_REWARD_PCT, COMMISSION_PCT, D('0.01'),
                     id='Payment min commission check'),
        pytest.param(True, AGENT_REWARD_PCT, COMMISSION_PCT, D('0.01'),
                     id='Refund min commission check'),
        pytest.param(False, D('0'), D('0'), PRICE,
                     id='Payment with commission_pct = 0'),
    ]
)
def test_create_payment(is_refund, agent_reward_pct, commission_pct, price, switch_to_pg):
    # создаем данные
    client_id, person_id, product_id, \
    contract_id = create_client_person_contract(agent_reward_pct, commission_pct)

    # создаем платеж
    trust_payment_id, payment_id, purchase_token = steps.SimpleNewApi.create_payment(TELEMEDICINE_CONTEXT.service,
                                                                                     product_id, amount=price)

    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(payment_id)

    trust_refund_id = None
    if is_refund:
        # создаем рефанд и обрабатываем его
        trust_refund_id, refund_id = steps.SimpleNewApi.create_refund(TELEMEDICINE_CONTEXT.service, purchase_token)
        steps.CommonPartnerSteps.export_payment(refund_id)

    expected_template, actual_data = get_expected_and_actual_data(client_id, person_id, contract_id, price,
                                                                  agent_reward_pct, commission_pct,
                                                                  payment_id, trust_payment_id,
                                                                  trust_refund_id,
                                                                  is_refund=is_refund)

    # сравниваем платеж с шаблоном
    utils.check_that(actual_data, contains_dicts_with_entries(expected_template, in_order=True),
                     'Сравниваем платеж с шаблоном')


# тест на проверку того, что проценты из дс подтягиваются при обработке платежа
@reporter.feature(Features.TRUST, Features.PAYMENT, Features.MEDICINE)
@pytest.mark.tickets('BALANCE-26358')
@pytest.mark.parametrize(
    'new_agent_reward_pct, new_commission_pct',
    [
        pytest.param(AGENT_REWARD_PCT_NEW, COMMISSION_PCT_NEW, id='Change pct'),
        pytest.param(D('0'), D('0'), id='Change pct to 0'),
    ])
def test_create_payment_with_new_pct_in_ds(new_agent_reward_pct, new_commission_pct, switch_to_pg):
    # создаем данные
    client_id, person_id, product_id, \
    contract_id = create_client_person_contract(AGENT_REWARD_PCT, COMMISSION_PCT)

    # создаем дс
    create_collateral_change_pct(contract_id, utils.Date.nullify_time_of_date(datetime.datetime.now()),
                                 new_agent_reward_pct, new_commission_pct)

    # создаем платеж
    trust_payment_id, payment_id, purchase_token = steps.SimpleNewApi.create_payment(TELEMEDICINE_CONTEXT.service,
                                                                                     product_id)

    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(payment_id)

    expected_template, actual_data = get_expected_and_actual_data(client_id, person_id, contract_id, PRICE,
                                                                  new_agent_reward_pct, new_commission_pct,
                                                                  payment_id, trust_payment_id)

    # сравниваем платеж с шаблоном
    utils.check_that(actual_data, contains_dicts_with_entries(expected_template, in_order=True),
                     'Сравниваем платеж с шаблоном')

@reporter.feature(Features.TRUST, Features.PAYMENT, Features.MEDICINE)
@pytest.mark.parametrize("contract_type, product_fiscal_nds, payment_fiscal_nds", [
    pytest.param(ContractType.LICENSE, None, CMNds.NDS_0, id='PAYMENT_FISCAL_NDS=0'),
    pytest.param(ContractType.NOT_AGENCY, None, CMNds.NDS_20, id='PAYMENT_FISCAL_NDS=20'),
    pytest.param(ContractType.NOT_AGENCY, CMNds.NDS_20, None, id='PRODUCT_FISCAL_NDS=20'),
    pytest.param(ContractType.NOT_AGENCY, CMNds.NDS_0, CMNds.NDS_20, id='PAYMENT_NDS_OVER_PRODUCT_NDS'),
])
def test_tech_contract_payment(contract_type, product_fiscal_nds, payment_fiscal_nds, switch_to_pg):
    agent_reward_pct = D('0')
    commission_pct = D('100')
    fiscal_nds = payment_fiscal_nds or product_fiscal_nds

    def get_fiscal_title(fiscal_nds):
        return 'test' if fiscal_nds else None

    client_id, person_id, contract_id = steps.CommonPartnerSteps.get_active_tech_ids(SERVICE, contract_type)

    product_id = steps.SimpleNewApi.create_product(SERVICE, partner_id=client_id,
                                                   fiscal_nds=product_fiscal_nds,
                                                   fiscal_title=get_fiscal_title(product_fiscal_nds))

    trust_payment_id, payment_id, purchase_token = \
        steps.SimpleNewApi.create_payment(SERVICE, product_id,
                                          fiscal_nds=payment_fiscal_nds,
                                          fiscal_title=get_fiscal_title(payment_fiscal_nds))

    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(payment_id)

    expected_template, actual_data = get_expected_and_actual_data(client_id, person_id, contract_id, PRICE,
                                                                  agent_reward_pct, commission_pct,
                                                                  payment_id, trust_payment_id, nds=fiscal_nds,
                                                                  with_product_id=True)

    # сравниваем платеж с шаблоном
    utils.check_that(actual_data, contains_dicts_with_entries(expected_template, in_order=True),
                     'Сравниваем платеж с шаблоном')