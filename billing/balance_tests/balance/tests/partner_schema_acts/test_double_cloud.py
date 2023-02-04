# -*- coding: utf-8 -*-

import pytest
from decimal import Decimal as D
from collections import defaultdict
import datetime

from hamcrest import empty

from balance import balance_steps as steps
from btestlib import utils
from btestlib.data.partner_contexts import (
    DOUBLE_CLOUD_USA_RESIDENT_UR,
    DOUBLE_CLOUD_USA_RESIDENT_PH,
    DOUBLE_CLOUD_USA_NONRESIDENT_PH,
    DOUBLE_CLOUD_USA_NONRESIDENT_UR,
    DOUBLE_CLOUD_DE_RESIDENT_UR,
    DOUBLE_CLOUD_DE_RESIDENT_PH,
    DOUBLE_CLOUD_DE_NONRESIDENT_UR,
    DOUBLE_CLOUD_DE_NONRESIDENT_PH,
)
from btestlib.constants import Paysyses, Products, InvoiceType
from btestlib.matchers import contains_dicts_with_entries

AMOUNT = D('112.37')


month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = \
    utils.Date.previous_two_months_dates()
# # налоги заведены с 01.06.2022
# if month_minus2_start_dt < datetime.datetime(2022, 6, 1):
#     month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = \
#         datetime.datetime(2022, 6, 1), datetime.datetime(2022, 6, 14), \
#         datetime.datetime(2022, 6, 15), datetime.datetime(2022, 6, 30)


# ключи в словарях - это процент НДС
CONTEXTS_PARAMS = {
    DOUBLE_CLOUD_USA_RESIDENT_UR.name: {
        'personal_account_data': {
            0: {'service_code': None, 'nds_flag': 1, 'paysys_id': Paysyses.BANK_USU_USD.id},
        },
        'products_data': {
            0: [Products.DOUBLE_CLOUD_US_NDS_0, ]
        }
    },
    DOUBLE_CLOUD_USA_RESIDENT_PH.name: {
        'personal_account_data': {
            0: {'service_code': None, 'nds_flag': 1, 'paysys_id': Paysyses.BANK_USP_USD.id},
        },
        'products_data': {
            0: [Products.DOUBLE_CLOUD_US_NDS_0, ]
        }
    },
    DOUBLE_CLOUD_USA_NONRESIDENT_UR.name: {
        'personal_account_data': {
            0: {'service_code': None, 'nds_flag': 0, 'paysys_id': Paysyses.BANK_USYT_USD.id},
        },
        'products_data': {
            0: [Products.DOUBLE_CLOUD_US_NDS_0, ]
        }
    },
    DOUBLE_CLOUD_USA_NONRESIDENT_PH.name: {
        'personal_account_data': {
            0: {'service_code': None, 'nds_flag': 0, 'paysys_id': Paysyses.BANK_USYTPH_USD.id},
        },
        'products_data': {
            0: [Products.DOUBLE_CLOUD_US_NDS_0, ]
        }
    },
    DOUBLE_CLOUD_DE_RESIDENT_UR.name: {
        'personal_account_data': {
            19: {'service_code': None, 'nds_flag': 1, 'paysys_id': Paysyses.BANK_DEUR_EUR.id},
        },
        'products_data': {
            19: [Products.DOUBLE_CLOUD_DE_NDS_20, ]
        }
    },
    DOUBLE_CLOUD_DE_RESIDENT_PH.name: {
        'personal_account_data': {
            19: {'service_code': None, 'nds_flag': 1, 'paysys_id': Paysyses.BANK_DEPH_EUR.id},
        },
        'products_data': {
            19: [Products.DOUBLE_CLOUD_DE_NDS_20, ]
        }
    },
    DOUBLE_CLOUD_DE_NONRESIDENT_UR.name: {
        'personal_account_data': {
            0: {'service_code': None, 'nds_flag': 0, 'paysys_id': Paysyses.BANK_DEYT_EUR.id},
        },
        'products_data': {
            0: [Products.DOUBLE_CLOUD_DE_NDS_0, ]
        }
    },
    DOUBLE_CLOUD_DE_NONRESIDENT_PH.name: {
        'personal_account_data': {
            0: {'service_code': None, 'nds_flag': 0, 'paysys_id': Paysyses.BANK_DEYTPH_EUR.id},
        },
        'products_data': {
            0: [Products.DOUBLE_CLOUD_DE_NDS_0, ]
        }
    },
}

class Summator(object):
    def __init__(self):
        self.sums_by_products = defaultdict(lambda: D('0'))
        self.sums_by_nds_pct = defaultdict(lambda: D('0'))
        self.products_by_nds_pct = defaultdict(lambda: set())

        self.current_sums_by_products = defaultdict(lambda: D('0'))
        self.current_sums_by_nds_pct = defaultdict(lambda: D('0'))
        self.current_products_by_nds_pct = defaultdict(lambda: set())

    def reset_current_sums(self):
        self.current_sums_by_products = defaultdict(lambda: D('0'))
        self.current_sums_by_nds_pct = defaultdict(lambda: D('0'))
        self.current_products_by_nds_pct = defaultdict(lambda: set())

    def add_amount(self, amount, nds_pct, product):
        self.sums_by_nds_pct[nds_pct] += amount
        self.current_sums_by_nds_pct[nds_pct] += amount

        self.sums_by_products[product] += amount
        self.current_sums_by_products[product] += amount

        self.products_by_nds_pct[nds_pct].add(product)
        self.current_products_by_nds_pct[nds_pct].add(product)

        for _nds_pct in self.products_by_nds_pct:
            if _nds_pct != nds_pct and product in self.products_by_nds_pct[_nds_pct]:
                raise Exception('Several nds_pct for product!')
        for _nds_pct in self.current_products_by_nds_pct:
            if _nds_pct != nds_pct and product in self.current_products_by_nds_pct[_nds_pct]:
                raise Exception('Several nds_pct for product!')


@pytest.mark.parametrize('context', [
    pytest.param(DOUBLE_CLOUD_USA_RESIDENT_UR, id=DOUBLE_CLOUD_USA_RESIDENT_UR.name),
    pytest.param(DOUBLE_CLOUD_USA_RESIDENT_PH, id=DOUBLE_CLOUD_USA_RESIDENT_PH.name),
    pytest.param(DOUBLE_CLOUD_USA_NONRESIDENT_UR, id=DOUBLE_CLOUD_USA_NONRESIDENT_UR.name),
    pytest.param(DOUBLE_CLOUD_USA_NONRESIDENT_PH, id=DOUBLE_CLOUD_USA_NONRESIDENT_PH.name),
    pytest.param(DOUBLE_CLOUD_DE_RESIDENT_UR, id=DOUBLE_CLOUD_DE_RESIDENT_UR.name),
    pytest.param(DOUBLE_CLOUD_DE_RESIDENT_PH, id=DOUBLE_CLOUD_DE_RESIDENT_PH.name),
    pytest.param(DOUBLE_CLOUD_DE_NONRESIDENT_UR, id=DOUBLE_CLOUD_DE_NONRESIDENT_UR.name),
    pytest.param(DOUBLE_CLOUD_DE_NONRESIDENT_PH, id=DOUBLE_CLOUD_DE_NONRESIDENT_PH.name),
])
def test_double_cloud_acts_wo_data(context):
    client_id, person_id, contract_id = create_contract(context, month_minus2_start_dt)

    expected_invoice_data = [
        create_expected_invoice_data(context, person_id, contract_id, month_minus2_start_dt, D('0'),
                                     pa_params['service_code'], nds_pct, pa_params['nds_flag'], pa_params['paysys_id'])
        for nds_pct, pa_params in CONTEXTS_PARAMS[context.name]['personal_account_data'].items()
    ]

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)

    utils.check_that(invoice_data, contains_dicts_with_entries(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_minus2_end_dt,
                                                                   manual_export=False)
    consume_data = steps.ConsumeSteps.get_consumes_sum_by_client_id(client_id)

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    utils.check_that(act_data, empty(), 'Сравниваем данные из акта с шаблоном')
    utils.check_that(consume_data, empty(), 'Проверяем, что конзюмов нет')


@pytest.mark.parametrize('context', [
    pytest.param(DOUBLE_CLOUD_USA_RESIDENT_UR, id=DOUBLE_CLOUD_USA_RESIDENT_UR.name),
    pytest.param(DOUBLE_CLOUD_USA_RESIDENT_PH, id=DOUBLE_CLOUD_USA_RESIDENT_PH.name),
    pytest.param(DOUBLE_CLOUD_USA_NONRESIDENT_UR, id=DOUBLE_CLOUD_USA_NONRESIDENT_UR.name),
    pytest.param(DOUBLE_CLOUD_USA_NONRESIDENT_PH, id=DOUBLE_CLOUD_USA_NONRESIDENT_PH.name),
    pytest.param(DOUBLE_CLOUD_DE_RESIDENT_UR, id=DOUBLE_CLOUD_DE_RESIDENT_UR.name),
    pytest.param(DOUBLE_CLOUD_DE_RESIDENT_PH, id=DOUBLE_CLOUD_DE_RESIDENT_PH.name),
    pytest.param(DOUBLE_CLOUD_DE_NONRESIDENT_UR, id=DOUBLE_CLOUD_DE_NONRESIDENT_UR.name),
    pytest.param(DOUBLE_CLOUD_DE_NONRESIDENT_PH, id=DOUBLE_CLOUD_DE_NONRESIDENT_PH.name),
])
def test_act_sender_2_months(context):
    client_id, person_id, contract_id = create_contract(context, month_minus2_start_dt)

    sums = Summator()
    for nds_pct, products_data in CONTEXTS_PARAMS[context.name]['products_data'].items():
        cur_amount = AMOUNT * (nds_pct + 1)
        for product in products_data:
            sums.add_amount(cur_amount, nds_pct, product)
            steps.PartnerSteps.create_fake_product_completion(month_minus2_start_dt, contract_id=contract_id,
                                                              service_id=context.service.id, type=product.mdh_id,
                                                              amount=cur_amount, commission_sum=nds_pct,
                                                              )
    # запускаем конец месяца
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_minus2_end_dt)
    consume_data_1 = steps.ConsumeSteps.get_consumes_sum_by_client_id(client_id)
    # проверяем данные в счете
    invoice_data_1 = steps.InvoiceSteps.get_invoice_data_by_client(client_id)

    expected_invoice_data_1 = [
        create_expected_invoice_data(context, person_id, contract_id, month_minus2_start_dt, sums.sums_by_nds_pct[nds_pct],
                                     pa_params['service_code'], nds_pct, pa_params['nds_flag'], pa_params['paysys_id'])
        for nds_pct, pa_params in CONTEXTS_PARAMS[context.name]['personal_account_data'].items()
    ]

    # проверяем данные в акте
    act_data_1 = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_consumes_1 = []
    for product in sums.sums_by_products:
        expected_consumes_1.append(
            steps.CommonData.create_expected_consume_data(product.id, sums.sums_by_products[product], InvoiceType.PERSONAL_ACCOUNT)
        )

    # создаем шаблон для сравнения
    expected_act_data_1 = []
    for nds_pct, amount in sums.current_sums_by_nds_pct.items():
        if amount > 0:
            addittional_params = dict(amount_nds=calc_amount_nds(amount, nds_pct))
            expected_act_data_1.append(steps.CommonData.create_expected_act_data(amount, month_minus2_end_dt,
                                                                                 addittional_params=addittional_params))
    utils.check_that(consume_data_1, contains_dicts_with_entries(expected_consumes_1),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data_1, contains_dicts_with_entries(expected_invoice_data_1),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_1, contains_dicts_with_entries(expected_act_data_1),
                     'Сравниваем данные из акта с шаблоном')

    sums.reset_current_sums()

    for nds_pct, products_data in CONTEXTS_PARAMS[context.name]['products_data'].items():
        cur_amount = AMOUNT * (nds_pct + 1)
        for product in products_data:
            sums.add_amount(cur_amount, nds_pct, product)
            steps.PartnerSteps.create_fake_product_completion(month_minus1_start_dt, contract_id=contract_id,
                                                              service_id=context.service.id, type=product.mdh_id,
                                                              amount=cur_amount, commission_sum=nds_pct,
                                                              )
    # запускаем конец месяца
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_minus1_end_dt)
    consume_data_2 = steps.ConsumeSteps.get_consumes_sum_by_client_id(client_id)
    # проверяем данные в счете
    invoice_data_2 = steps.InvoiceSteps.get_invoice_data_by_client(client_id)

    expected_invoice_data_2 = [
        create_expected_invoice_data(context, person_id, contract_id, month_minus2_start_dt, sums.sums_by_nds_pct[nds_pct],
                                     pa_params['service_code'], nds_pct, pa_params['nds_flag'], pa_params['paysys_id'])
        for nds_pct, pa_params in CONTEXTS_PARAMS[context.name]['personal_account_data'].items()
    ]

    # проверяем данные в акте
    act_data_2 = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_consumes_2 = []
    for product in sums.sums_by_products:
        expected_consumes_2.append(
            steps.CommonData.create_expected_consume_data(product.id, sums.sums_by_products[product], InvoiceType.PERSONAL_ACCOUNT)
        )

    # создаем шаблон для сравнения
    expected_act_data_2 = expected_act_data_1
    for nds_pct, amount in sums.current_sums_by_nds_pct.items():
        if amount > 0:
            addittional_params = dict(amount_nds=calc_amount_nds(amount, nds_pct))
            expected_act_data_2.append(steps.CommonData.create_expected_act_data(amount, month_minus1_end_dt,
                                                                                 addittional_params=addittional_params))
    utils.check_that(consume_data_2, contains_dicts_with_entries(expected_consumes_2),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data_2, contains_dicts_with_entries(expected_invoice_data_2),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_2, contains_dicts_with_entries(expected_act_data_2),
                     'Сравниваем данные из акта с шаблоном')




def create_contract(context, start_dt):
    client_id = steps.ClientSteps.create()
    _, person_id, contract_id, _ = steps.ContractSteps. \
        create_partner_contract(context,
                                client_id=client_id,
                                additional_params={
                                    'start_dt': start_dt,
                                },
                                partner_integration_params={
                                    'link_integration_to_client': 1,
                                    'link_integration_to_client_args': {
                                        'integration_cc': context.partner_integration_cc,
                                        'configuration_cc': context.partner_configuration_cc,
                                    },
                                    'set_integration_to_contract': 1,
                                    'set_integration_to_contract_params': {
                                        'integration_cc': context.partner_integration_cc,
                                    },
                                })
    return client_id, person_id, contract_id


def create_expected_invoice_data(context, person_id, contract_id, dt, amount, service_code, nds_pct, nds_flag, paysys_id):
    return steps.CommonData.create_expected_invoice_data_by_context(
        context, contract_id, person_id, amount,
        nds_pct=nds_pct, nds=nds_flag, dt=dt, paysys_id=paysys_id)


def calc_amount_nds(amount_w_nds, nds_pct):
    return utils.dround(amount_w_nds - (amount_w_nds / (D('1') + (D(nds_pct) / D('100')))), 2)
