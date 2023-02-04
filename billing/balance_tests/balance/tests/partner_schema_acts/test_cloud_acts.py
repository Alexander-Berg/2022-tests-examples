# coding: utf-8

from balance.features import Features
from btestlib import reporter

__author__ = 'a-vasin'

import datetime as dt
from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import empty

import balance.balance_steps as steps
import btestlib.utils as utils
from btestlib.constants import PersonTypes, Paysyses, Services
from btestlib.matchers import contains_dicts_equal_to, contains_dicts_with_entries
from btestlib.data.partner_contexts import CLOUD_RU_CONTEXT, CLOUD_KZ_CONTEXT, HOSTING_SERVICE

CLOUD_RU_PH_CONTEXT = CLOUD_RU_CONTEXT.new(
        name='CLOUD_RU_PH_CONTEXT',
        person_type=PersonTypes.PH,
        paysys=Paysyses.BANK_PH_RUB_CLOUD,
        is_offer=1
)

CLOUD_KZ_PH_CONTEXT = CLOUD_KZ_CONTEXT.new(
        name='CLOUD_KZ_PH_CONTEXT',
        person_type=PersonTypes.KZP,
        paysys=Paysyses.BANK_PH_KZT_CLOUD,
        is_offer=1
)

pytestmark = [
    reporter.feature(Features.CLOUD, Features.ACT)
]

START_DT = utils.Date.first_day_of_month(dt.datetime.now())
ACT_DT = utils.Date.last_day_of_month(START_DT)
AMOUNT = Decimal('99.38543')


# AMOUNT = Decimal('99.94444')
# AMOUNT = Decimal('99.50')


@pytest.mark.smoke
@pytest.mark.parametrize("context", [
    CLOUD_RU_CONTEXT, CLOUD_RU_PH_CONTEXT,
    CLOUD_KZ_CONTEXT, CLOUD_KZ_PH_CONTEXT,
    HOSTING_SERVICE], ids=lambda p: p.name)
def test_cloud_acts_first_month(context):
    client_id, contract_id, person_id, _ = create_act_first_month(context)

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    amount = utils.dround(AMOUNT, 2) if context.service != Services.HOSTING_SERVICE \
        else utils.dround(AMOUNT * context.nds.koef_on_dt(START_DT), 2)
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context, contract_id, person_id,
                                                                                     amount, dt=START_DT)
    utils.check_that(invoice_data, contains_dicts_equal_to([expected_invoice_data]),
                     u'Проверяем, что созданы ожидаемые счета')

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    expected_act_data = steps.CommonData.create_expected_act_data(amount, ACT_DT)
    utils.check_that(act_data, contains_dicts_with_entries([expected_act_data]), u'Проверяем, что акт корректен')


@pytest.mark.parametrize("context", [CLOUD_RU_CONTEXT, CLOUD_KZ_CONTEXT, HOSTING_SERVICE], ids=lambda p: p.name)
def test_cloud_acts_wo_data(context):
    project_uuid = steps.PartnerSteps.create_cloud_project_uuid()
    client_id, person_id, contract_id = create_contract(context, [project_uuid], is_offer=0)

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, START_DT)

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context, contract_id,
                                                                                     person_id,
                                                                                     Decimal(0), dt=START_DT)
    utils.check_that(invoice_data, contains_dicts_equal_to([expected_invoice_data]),
                     u'Проверяем, что созданы ожидаемые счета')

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    utils.check_that(act_data, empty(), u'Проверяем, что акты не сгенерированы')


@pytest.mark.parametrize("context", [CLOUD_RU_CONTEXT, HOSTING_SERVICE], ids=lambda p: p.name)
def test_cloud_acts_increasing_total(context):
    client_id, contract_id, person_id, project_uuid = create_act_first_month(context)

    steps.PartnerSteps.create_cloud_completion(contract_id, START_DT, AMOUNT, product=context.product)
    steps.PartnerSteps.create_cloud_completion(contract_id, START_DT + relativedelta(months=1), AMOUNT,
                                               product=context.product)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id,
                                                                   START_DT + relativedelta(months=1))

    amount = utils.dround(3 * AMOUNT, 2) if context.service != Services.HOSTING_SERVICE \
        else utils.dround(AMOUNT * context.nds.koef_on_dt(START_DT) +
                          2 * AMOUNT * context.nds.koef_on_dt(START_DT + relativedelta(months=1)), 2)

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context, contract_id,
                                                                                     person_id,
                                                                                     amount,
                                                                                     dt=START_DT)
    utils.check_that(invoice_data, contains_dicts_equal_to([expected_invoice_data]),
                     u'Проверяем, что созданы ожидаемые счета')

    amount = [utils.dround(AMOUNT, 2) if context.service != Services.HOSTING_SERVICE
              else utils.dround(AMOUNT * context.nds.koef_on_dt(START_DT), 2),
              utils.dround(2 * AMOUNT, 2) if context.service != Services.HOSTING_SERVICE
              else utils.dround(2 * AMOUNT * context.nds.koef_on_dt(START_DT + relativedelta(months=1)), 2)]

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    expected_act_data = [steps.CommonData.create_expected_act_data(amount[0], ACT_DT),
                         steps.CommonData.create_expected_act_data(amount[1],
                                                                   utils.Date.last_day_of_month(
                                                                       START_DT + relativedelta(months=1)))]
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data), u'Проверяем, что акт корректен')


# -------------------------
# Utils


def create_act_first_month(context):
    project_uuid = steps.PartnerSteps.create_cloud_project_uuid()
    client_id, person_id, contract_id = create_contract(context, [project_uuid], is_offer=1 if context.is_offer else 0)

    steps.PartnerSteps.create_cloud_completion(contract_id, START_DT, AMOUNT, product=context.product)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, START_DT)

    return client_id, contract_id, person_id, project_uuid


def create_contract(context, projects, is_offer):
    params = {'projects': projects,
              'start_dt': START_DT}
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, is_offer=is_offer,
                                                                                       additional_params=params)
    return client_id, person_id, contract_id
