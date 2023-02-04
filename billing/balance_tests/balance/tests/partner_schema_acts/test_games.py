# -*- coding: utf-8 -*-
import datetime

import pytest
from decimal import Decimal

from hamcrest import empty

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import TransactionType
from btestlib.data.partner_contexts import GAMES_CONTEXT_USD_TRY, GAMES_CONTEXT
from btestlib.matchers import contains_dicts_with_entries


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


AMOUNT = Decimal('111.11')

_, _, first_month_start_dt, first_month_end_dt, second_month_start_dt, second_month_end_dt = \
    utils.Date.previous_three_months_start_end_dates()


@pytest.mark.parametrize('context', [
    pytest.param(GAMES_CONTEXT, id='GAMES_CONTEXT'),
    pytest.param(GAMES_CONTEXT_USD_TRY, id='GAMES_CONTEXT_USD_TRY'),
])
def test_acts_wo_data(context):
    client_id, person_id, contract_id = create_contract(context, first_month_start_dt)
    act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)
    utils.check_that(act_data, empty(), 'Проверяем, что акты не сгенерились и генерация не упала')


@pytest.mark.parametrize('context', [
    pytest.param(GAMES_CONTEXT, id='GAMES_CONTEXT'),
    pytest.param(GAMES_CONTEXT_USD_TRY, id='GAMES_CONTEXT_USD_TRY'),
])
def test_payments_and_acts(context):
    # создаем контракт
    client_id, person_id, contract_id = create_contract(context, first_month_start_dt)

    # создаем платежи и генерируем акты
    create_payments(context, client_id, person_id, contract_id, AMOUNT, coef=2, dt=first_month_start_dt.replace(day=9))
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, first_month_end_dt)

    create_payments(context, client_id, person_id, contract_id, AMOUNT, coef=3, dt=first_month_start_dt.replace(day=9))
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, second_month_end_dt)

    # проверяем счет
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(
        context=context,
        contract_id=contract_id,
        person_id=person_id,
        amount=5 * AMOUNT,
        dt=first_month_start_dt,
    )
    utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data]),
                     u'Сравниваем данные из счета с шаблоном')

    # выгружаем и проверяем акты клиента
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    expected_act_data = [
        steps.CommonData.create_expected_act_data(2 * AMOUNT, first_month_end_dt, context=context),
        steps.CommonData.create_expected_act_data(3 * AMOUNT, second_month_end_dt, context=context)
    ]
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     u'Сравниваем данные из акта с шаблоном')


def create_contract(context, start_dt):
    client_id = steps.SimpleApi.create_partner(service=context.service)
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


def create_payments(context, client_id, person_id, contract_id, amount, coef, dt):
    product_mapping_config = steps.CommonPartnerSteps.get_product_mapping_config(context.service)
    main_product_id = product_mapping_config['default_product_mapping'][context.payment_currency.iso_code]['default']
    steps.SimpleApi.create_fake_tpt_row(context, client_id, person_id, contract_id,
                                        dt=dt,
                                        transaction_type=TransactionType.PAYMENT,
                                        amount=coef * amount,
                                        product_id=main_product_id)
