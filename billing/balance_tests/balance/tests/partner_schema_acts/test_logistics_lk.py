# -*- coding: utf-8 -*-

import random
from decimal import Decimal as D

import pytest
from hamcrest import empty

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import NdsNew, TransactionType
from btestlib.data.partner_contexts import LOGISTICS_LK_CONTEXT
from btestlib.matchers import equal_to_casted_dict, contains_dicts_with_entries
from btestlib.data import person_defaults

# pytestmark = [reporter.feature(Features.FOOD, Features.PARTNER, Features.ACT)]

import datetime

SERVICE_AMOUNT = D('42.42')

_, _, first_month_start_dt, first_month_end_dt, second_month_start_dt, second_month_end_dt = \
    utils.Date.previous_three_months_start_end_dates()


def test_logistics_lk_wo_data():
    context = LOGISTICS_LK_CONTEXT
    client_id, person_id, contract_id = create_contract(context, first_month_start_dt)

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, first_month_start_dt)

    act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)
    utils.check_that(act_data, empty(), 'Проверяем, что акты не сгенерились и генерация не упала')


def test_logistics_lk_acts():
    context = LOGISTICS_LK_CONTEXT
    client_id, person_id, contract_id = create_contract(context, first_month_start_dt)

    total_month_sum = SERVICE_AMOUNT

    create_oebs_completions(context, contract_id, client_id, first_month_start_dt, total_month_sum)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, first_month_end_dt)

    create_oebs_completions(context, contract_id, client_id, second_month_start_dt, 2 * total_month_sum)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, second_month_end_dt)

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    expected_invoice_data = [((context, contract_id, person_id, 3 * total_month_sum),
                              dict(dt=first_month_start_dt, nds_pct=context.nds.pct_on_dt(first_month_end_dt),
                                   nds=int(bool(context.nds)),
                                   paysys_id=context.paysys.id))
                             ]
    expected_invoice_data = [steps.CommonData.create_expected_invoice_data_by_context(*e[0], **e[1])
                             for e in expected_invoice_data]
    utils.check_that(invoice_data, contains_dicts_with_entries(expected_invoice_data),
                     u'Сравниваем данные из счета с шаблоном')

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    expected_act_data = [
        steps.CommonData.create_expected_act_data(total_month_sum, first_month_end_dt, context=context),
        steps.CommonData.create_expected_act_data(2 * total_month_sum, second_month_end_dt, context=context)
    ]
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     u'Сравниваем данные из акта с шаблоном')


# utils
def create_oebs_completions(context, contract_id, client_id, dt, payment_sum=D('0'), refund_sum=D('0')):
    completions = [
        {
            'service_id': context.service.id,
            'amount': (payment_sum - refund_sum),
            'product_id': context.product.id,
            'dt': dt,
            'transaction_dt': dt,
            'currency': context.currency.iso_code,
            'accounting_period': dt
        },
    ]
    steps.CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, completions)


def create_person(context, client_id):
    return steps.PersonSteps.create(client_id, context.person_type.code,
                                    full=True,
                                    inn_type=person_defaults.InnType.RANDOM,
                                    name_type=person_defaults.NameType.RANDOM,
                                    params={'is-partner': '0'},
                                    )


def create_contract(context, start_dt):
    client_id = steps.ClientSteps.create()
    person_id = create_person(context, client_id)
    partner_integration_params = steps.CommonIntegrationSteps.DEFAULT_PARTNER_INTEGRATION_PARAMS_FOR_CREATE_CONTRACT
    additional_params = dict(start_dt=start_dt)
    return steps.ContractSteps.create_partner_contract(
        context,
        client_id=client_id, person_id=person_id,
        partner_integration_params=partner_integration_params,
        additional_params=additional_params)[:-1]
