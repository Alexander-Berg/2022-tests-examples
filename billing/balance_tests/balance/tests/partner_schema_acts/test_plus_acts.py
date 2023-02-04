# -*- coding: utf-8 -*-

__author__ = 'roman-nagaev'

import datetime
import hamcrest as hm
import pytest

import btestlib.reporter as reporter
import balance.balance_db as db
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.data.partner_contexts import PLUS_2_0_INCOME_CONTEXT
from btestlib.matchers import contains_dicts_with_entries


PARTNER_INTEGRATION_PARAMS = {
    'link_integration_to_client': 1,
    'link_integration_to_client_args': {
        'integration_cc': 'plus_2_0_services',
        'configuration_cc': 'plus_2_0_ur_rub_services_config',
    },
    'set_integration_to_contract': 1,
    'set_integration_to_contract_params': {
        'integration_cc': 'plus_2_0_services',
    },
}
PRODUCT_MDH_ID = 'd64f6922-2ca5-44fe-a2e8-e31efc9a08b8'


pytestmark = [
    reporter.feature(Features.ACT, Features.PLUS),
    pytest.mark.plus_2_0,
    pytest.mark.no_parallel('plus_acts')
]

_, _, month2_start_dt, month2_end_dt, month3_start_dt, month3_end_dt = \
    utils.Date.previous_three_months_start_end_dates(dt=datetime.datetime.today())
contract_start_dt = month2_start_dt
migrate_dt = datetime.datetime(2020, 1, 1)


def test_plus_act_wo_data():
    client_id, person_id, contract_id = create_contract(PLUS_2_0_INCOME_CONTEXT)
    try:
        # смигрировано по namespace в тесте
        # steps.CommonPartnerSteps.migrate_client('plus', 'Client', client_id, migrate_dt)
        steps.CommonPartnerSteps.generate_plus_acts_fair(month2_end_dt)
        invoice_data, act_data = get_actual_data(client_id)
        expected_invoice = create_expected_invoice(PLUS_2_0_INCOME_CONTEXT, contract_id, person_id, 0, contract_start_dt)

        utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice]),
                         'Сравниваем данные из счета с шаблоном')
        utils.check_that(act_data, hm.empty(),
                         'Проверяем что актов нет')
    finally:
        clear(contract_id)


def test_plus_act_second_month():
    client_id, person_id, contract_id = create_contract(PLUS_2_0_INCOME_CONTEXT)
    try:
        # смигрировано по namespace в тесте
        # steps.CommonPartnerSteps.migrate_client('plus', 'Client', client_id, migrate_dt)

        fst_amount = 125
        steps.SimpleApi.create_actotron_act_row(
            client_id, contract_id, PLUS_2_0_INCOME_CONTEXT.service.id, fst_amount, month2_end_dt, PRODUCT_MDH_ID
        )
        steps.CommonPartnerSteps.generate_plus_acts_fair(month2_end_dt)
        fst_act = steps.CommonData.create_expected_act_data(fst_amount, month2_end_dt)

        sd_amount = 25
        steps.SimpleApi.create_actotron_act_row(
            client_id, contract_id, PLUS_2_0_INCOME_CONTEXT.service.id, sd_amount, month3_end_dt, PRODUCT_MDH_ID
        )
        steps.CommonPartnerSteps.generate_plus_acts_fair(month3_end_dt)
        sd_act = steps.CommonData.create_expected_act_data(sd_amount, month3_end_dt)

        invoice_data, act_data = get_actual_data(client_id)
        expected_invoice = create_expected_invoice(
            PLUS_2_0_INCOME_CONTEXT, contract_id, person_id, fst_amount + sd_amount, contract_start_dt
        )

        utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice]),
                         'Сравниваем данные из счета с шаблоном')
        utils.check_that(act_data, contains_dicts_with_entries([fst_act, sd_act]),
                         'Сравниваем данные из актов с шаблоном')
    finally:
        clear(contract_id)


# Utils
def create_contract(context):
    client_id = steps.client_steps.ClientSteps.create()
    person_id = steps.person_steps.PersonSteps.create(client_id, context.person_type.code)

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context, client_id=client_id, person_id=person_id, additional_params={'start_dt': contract_start_dt},
        partner_integration_params=PARTNER_INTEGRATION_PARAMS
    )
    return client_id, person_id, contract_id


def clear(contract_id):
    db.balance().execute(
        "update bo.t_contract_attributes set key_num = 124 where id in "
        "(select distinct s.id from bo.t_contract_collateral cl "
        "join bo.t_contract_attributes s on s.collateral_id = cl.id and s.code = 'SERVICES'"
        "where cl.contract2_id = :contract_id)", {'contract_id': contract_id})


def get_actual_data(client_id):
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    return invoice_data, act_data


def create_expected_invoice(context, contract_id, person_id, amount, dt, **kw):
    params = {'paysys_id': context.paysys.id, 'dt': dt}
    params.update(kw)
    return steps.CommonData.create_expected_invoice_data_by_context(context, contract_id, person_id, amount, **params)
