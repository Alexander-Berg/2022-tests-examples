# -*- coding: utf-8 -*-

from decimal import Decimal

from hamcrest import empty

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import InvoiceType
from btestlib.data.partner_contexts import DISK_B2B_CONTEXT
from btestlib.matchers import contains_dicts_with_entries
from btestlib.data import person_defaults

pytestmark = [reporter.feature(Features.DISK_B2B, Features.PARTNER, Features.ACT)]

_, _, first_month_start_dt, first_month_end_dt, second_month_start_dt, second_month_end_dt = \
    utils.Date.previous_three_months_start_end_dates()


def create_person(context, client_id):
    return steps.PersonSteps.create(
        client_id, context.person_type.code,
        full=True,
        inn_type=person_defaults.InnType.RANDOM,
        name_type=person_defaults.NameType.RANDOM,
        params={'is-partner': '0'},
    )


def create_completions(context, client_id, dt, amount, coef):
    steps.PartnerSteps.create_fake_product_completion(
        dt,
        client_id=client_id,
        service_id=context.service.id,
        service_order_id=0,
        amount=coef * amount,
        type='disk_b2b'
    )


@pytest.mark.parametrize('is_postpay', [1, 0], ids=lambda s: 'is_postpay=' + str(s))
def test_disk_b2b_acts_wo_data(is_postpay):
    client_id = steps.ClientSteps.create()
    person_id = create_person(DISK_B2B_CONTEXT, client_id)
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        DISK_B2B_CONTEXT,
        client_id=client_id, person_id=person_id,
        is_postpay=is_postpay, additional_params={'start_dt': first_month_start_dt,
                                         'offer_confirmation_type': 'no',
                                         'personal_account': '1'
                                         })

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, first_month_start_dt)

    act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)
    utils.check_that(act_data, empty(), 'Проверяем, что акты не сгенерились и генерация не упала')


SERVICE_AMOUNT = Decimal('42.42')


@pytest.mark.parametrize('is_postpay', [1, 0], ids=lambda s: 'is_postpay=' + str(s))
def test_disk_b2b_acts(is_postpay):
    context = DISK_B2B_CONTEXT
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context,
                                                                                       is_postpay=is_postpay,
                                                                                       additional_params={
                                                                                           'start_dt': first_month_start_dt
                                                                                       })

    create_completions(context, client_id, first_month_start_dt, SERVICE_AMOUNT, coef=1)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, first_month_end_dt)

    create_completions(context, client_id, second_month_start_dt, SERVICE_AMOUNT, coef=2)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, second_month_end_dt)

    total_month_sum = SERVICE_AMOUNT

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context, contract_id,
                                                                                     person_id,
                                                                                     3 * total_month_sum,
                                                                                     dt=first_month_start_dt)
    utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data]),
                     u'Сравниваем данные из счета с шаблоном')

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    expected_act_data = [
        steps.CommonData.create_expected_act_data(total_month_sum, first_month_end_dt),
        steps.CommonData.create_expected_act_data(2 * total_month_sum, second_month_end_dt)
    ]
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     u'Сравниваем данные из акта с шаблоном')

    consume_data = steps.ConsumeSteps.get_consumes_by_client_id(client_id)
    expected_consume_data = [
        steps.CommonData.create_expected_consume_data(
            context.product.id,
            3 * SERVICE_AMOUNT,
            InvoiceType.PERSONAL_ACCOUNT
        )
    ]
    utils.check_that(consume_data, contains_dicts_with_entries(expected_consume_data), u'Проверяем данные консьюмов')

    balance = steps.PartnerSteps.get_partner_balance(context.service, [contract_id])
    utils.check_that(balance, contains_dicts_with_entries([{'LastActDT': second_month_end_dt.isoformat()}]))
    print(balance)
