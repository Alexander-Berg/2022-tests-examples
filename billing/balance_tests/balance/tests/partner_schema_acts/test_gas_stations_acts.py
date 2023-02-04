# -*- coding: utf-8 -*-

__author__ = 'a-vasin'

from decimal import Decimal

import pytest
from hamcrest import empty

from balance import balance_steps as steps
from btestlib import utils
from btestlib.data.partner_contexts import GAS_STATION_RU_CONTEXT
from btestlib.matchers import contains_dicts_equal_to, contains_dicts_with_entries

PAYMENT_AMOUNT = Decimal('42.77')
REFUND_AMOUNT = Decimal('31.42')
TOTAL_AMOUNT = PAYMENT_AMOUNT - REFUND_AMOUNT

_, _, first_month_start_dt, first_month_end_dt, second_month_start_dt, second_month_end_dt = \
    utils.Date.previous_three_months_start_end_dates()


def test_act_wo_data():
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        GAS_STATION_RU_CONTEXT,
        additional_params={'start_dt': first_month_start_dt})

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(
        client_id, contract_id, second_month_start_dt,
        manual_export=False)

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    expected_invoice_data = [steps.CommonData.create_expected_invoice_data_by_context(
        GAS_STATION_RU_CONTEXT,
        contract_id, person_id,
        Decimal('0'),
        dt=first_month_start_dt)]
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data),
                     u'Сравниваем данные из счета с шаблоном')

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    utils.check_that(act_data, empty(), u'Сравниваем данные из акта с шаблоном')


@pytest.mark.smoke
@pytest.mark.parametrize("service_fee", [None, 'default', 2, 3],
                         ids=lambda cc: 'fee_' + str(cc))
def test_act_second_month(service_fee):
    service_fee_config = steps.CommonPartnerSteps.get_product_mapping_config(GAS_STATION_RU_CONTEXT.service)
    service_fee_config = service_fee_config['service_fee_product_mapping'][
        GAS_STATION_RU_CONTEXT.payment_currency.iso_code]
    additional_tpt_params = None if service_fee is None else dict(product_id=service_fee_config[str(service_fee)])

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        GAS_STATION_RU_CONTEXT,
        additional_params={'start_dt': first_month_start_dt})

    first_month_sum, second_month_sum = steps.SimpleApi.create_tipical_tpt_data_for_act(
        GAS_STATION_RU_CONTEXT,
        client_id,
        person_id, contract_id,
        first_month_start_dt,
        second_month_start_dt,
        sum_key='yandex_reward',
        additional_tpt_params=additional_tpt_params)

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)

    expected_invoice_data = [steps.CommonData.create_expected_invoice_data_by_context(GAS_STATION_RU_CONTEXT,
                                                                                      contract_id, person_id,
                                                                                      first_month_sum + second_month_sum,
                                                                                      dt=first_month_start_dt)]
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data),
                     u'Сравниваем данные из счета с шаблоном')

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    expected_act_data = [
        steps.CommonData.create_expected_act_data(first_month_sum, first_month_end_dt),
        steps.CommonData.create_expected_act_data(second_month_sum, second_month_end_dt)]

    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     u'Сравниваем данные из акта с шаблоном')
