# -*- coding: utf-8 -*-

import random
from decimal import Decimal

import pytest
from hamcrest import empty

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import NdsNew, TransactionType
from btestlib.data.partner_contexts import HEALTH_PAYMENTS_PH_CONTEXT
from btestlib.matchers import equal_to_casted_dict, contains_dicts_with_entries
from btestlib.data import person_defaults

# pytestmark = [reporter.feature(Features.FOOD, Features.PARTNER, Features.ACT)]

import datetime

SERVICE_AMOUNT = Decimal('42.42')

_, _, first_month_start_dt, first_month_end_dt, second_month_start_dt, second_month_end_dt = \
    utils.Date.previous_three_months_start_end_dates()


def create_person(context, client_id):
    return steps.PersonSteps.create(client_id, context.person_type.code,
                                    full=True,
                                    inn_type=person_defaults.InnType.RANDOM,
                                    name_type=person_defaults.NameType.RANDOM,
                                    params={'is-partner': '0'},
                                    )


def create_payments(context, client_id, person_id, contract_id, amount, coef, with_nds):
    product_mapping_config = steps.CommonPartnerSteps.get_product_mapping_config(context.service)
    product_label = 'default' if with_nds else 'nds_0'
    main_product_id = product_mapping_config['default_product_mapping'][
        context.payment_currency.iso_code][product_label]
    invoice_eid = steps.InvoiceSteps.get_invoice_eid(contract_id, client_id, context.currency.char_code,
                                                     1 if with_nds else 0)
    steps.SimpleApi.create_fake_tpt_row(context, client_id, person_id, contract_id,
                                        dt=first_month_start_dt.replace(day=9),
                                        yandex_reward=None, transaction_type=TransactionType.PAYMENT,
                                        amount=amount * coef,
                                        product_id=main_product_id,
                                        invoice_eid=invoice_eid)


def create_contract(context, start_dt):
    client_id = steps.ClientSteps.create()
    person_id = create_person(context, client_id)
    partner_integration_params = steps.CommonIntegrationSteps.DEFAULT_PARTNER_INTEGRATION_PARAMS_FOR_CREATE_CONTRACT
    additional_params = dict(start_dt=start_dt, **context.special_contract_params)
    return steps.ContractSteps.create_partner_contract(
        context,
        client_id=client_id, person_id=person_id,
        partner_integration_params=partner_integration_params,
        additional_params=additional_params)[:-1]


def test_health_payment_ph_acts_wo_data():
    context = HEALTH_PAYMENTS_PH_CONTEXT
    client_id, person_id, contract_id = create_contract(context, first_month_start_dt)

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, first_month_start_dt)

    act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)
    utils.check_that(act_data, empty(), 'Проверяем, что акты не сгенерились и генерация не упала')


@pytest.mark.parametrize('with_nds', [True, False], ids=['with NDS', 'without NDS'])
def test_health_payments_ph_acts(with_nds):
    context = HEALTH_PAYMENTS_PH_CONTEXT
    client_id, person_id, contract_id = create_contract(context, first_month_start_dt)

    total_month_sum = SERVICE_AMOUNT

    create_payments(context, client_id, person_id, contract_id, SERVICE_AMOUNT, coef=1, with_nds=with_nds)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, first_month_end_dt)

    create_payments(context, client_id,  person_id, contract_id, SERVICE_AMOUNT, coef=2, with_nds=with_nds)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, second_month_end_dt)

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    expected_invoice_data = [((context, contract_id, person_id,
                              3 * total_month_sum if with_nds else Decimal(0)),
                              dict(dt=first_month_start_dt)),
                             ((context, contract_id, person_id,
                               3 * total_month_sum if not with_nds else Decimal(0)),
                              dict(dt=first_month_start_dt, nds_pct=Decimal(0), nds=0,
                                   paysys_id=context.paysys_wo_nds.id))
                             ]
    expected_invoice_data = [steps.CommonData.create_expected_invoice_data_by_context(*e[0], **e[1])
                             for e in expected_invoice_data]
    utils.check_that(invoice_data, contains_dicts_with_entries(expected_invoice_data),
                     u'Сравниваем данные из счета с шаблоном')

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    context = context.new(context.name + '_rnd_test' + str(random.randint(66666, 6666666)),
                          nds=NdsNew.DEFAULT if with_nds else NdsNew.ZERO)
    expected_act_data = [
        steps.CommonData.create_expected_act_data(total_month_sum, first_month_end_dt, context=context),
        steps.CommonData.create_expected_act_data(2 * total_month_sum, second_month_end_dt, context=context)
    ]
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     u'Сравниваем данные из акта с шаблоном')


# Проверять только при разработке, пересекается и с balance.tests.payment.test_health_payments_ph.test_payment
# @pytest.mark.no_parallel('tech_contract_health_payments_ph_acts')
def _tech_contract_health_payments_ph_acts():
    context = HEALTH_PAYMENTS_PH_CONTEXT
    client_id, person_id, contract_id = steps.CommonPartnerSteps.get_active_tech_ids(context.service)

    old_amount = sum([Decimal(a['amount']) for a in steps.ActsSteps.get_act_data_by_contract(contract_id)], Decimal(0))
    create_payments(context, client_id, person_id, contract_id, SERVICE_AMOUNT, coef=1, with_nds=True)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, first_month_end_dt)
    assert old_amount + SERVICE_AMOUNT == sum(
        [Decimal(a['amount']) for a in steps.ActsSteps.get_act_data_by_contract(contract_id)], Decimal(0))

    create_payments(context, client_id, person_id, contract_id, SERVICE_AMOUNT, coef=2, with_nds=False)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, second_month_end_dt)
    assert old_amount + 3 * SERVICE_AMOUNT == sum(
        [Decimal(a['amount']) for a in steps.ActsSteps.get_act_data_by_contract(contract_id)], Decimal(0))
