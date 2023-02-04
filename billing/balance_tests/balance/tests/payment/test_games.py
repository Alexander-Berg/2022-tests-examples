# coding: utf-8
import pytest
from datetime import datetime
from decimal import Decimal as D

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import TransactionType
from btestlib.data.partner_contexts import GAMES_CONTEXT_USD_TRY, GAMES_CONTEXT
from btestlib.matchers import contains_dicts_with_entries

AMOUNT = D('111.11')
TURKEY_CB_ID = 1002  # 'Central Bank of the Republic of Turkey'


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


# noinspection DuplicatedCode
@pytest.mark.parametrize('context', [
    pytest.param(GAMES_CONTEXT, id='GAMES_CONTEXT'),
    pytest.param(GAMES_CONTEXT_USD_TRY, id='GAMES_CONTEXT_USD_TRY'),
])
def test_payment_with_tech_ids(context):
    tech_client_id, tech_person_id, tech_contract_id = \
        steps.CommonPartnerSteps.get_active_tech_ids(context.service,
                                                     currency=context.payment_currency.num_code,
                                                     contract_currency=context.currency.num_code)

    # создаем платеж
    partner_id, service_product_id = steps.SimpleApi.create_partner_and_product(service=context.service)
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(context.service,
                                             service_product_id=service_product_id,
                                             price=AMOUNT,
                                             currency=context.payment_currency)
    steps.CommonPartnerSteps.export_payment(payment_id)

    # проверяем tpt
    payment_tpt = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)
    product_mapping_config = steps.CommonPartnerSteps.get_product_mapping_config(context.service)
    main_product_id = product_mapping_config['default_product_mapping'][context.payment_currency.iso_code]['default']
    currency_rate = steps.CurrencySteps.get_currency_rate(dt=datetime.now(), currency=context.payment_currency.iso_code,
                                                          base_cc=context.currency.iso_code, rate_src_id=TURKEY_CB_ID,
                                                          iso_base=True)
    expected_payments_tpt = [
        steps.SimpleApi.create_expected_tpt_row(context=context,
                                                partner_id=tech_client_id,
                                                contract_id=tech_contract_id,
                                                person_id=tech_person_id,
                                                trust_payment_id=trust_payment_id,
                                                payment_id=payment_id,
                                                amount=round(AMOUNT * currency_rate, 2),
                                                payment_type=context.tpt_payment_type,
                                                paysys_type_cc=context.tpt_paysys_type_cc,
                                                internal=1,
                                                product_id=main_product_id,
                                                oebs_org_id=payment_tpt[0]['oebs_org_id']),
    ]

    utils.check_that(payment_tpt, contains_dicts_with_entries(expected_payments_tpt), u'Сравниваем платеж с шаблоном')

    # делаем возврат
    trust_refund_id, refund_id = \
        steps.SimpleApi.create_refund(
            service=context.service,
            service_order_id=service_order_id,
            trust_payment_id=trust_payment_id,
            delta_amount=AMOUNT)

    # обрабатываем платеж и возврат
    steps.CommonPartnerSteps.export_payment(refund_id)

    # проверяем tpt
    payment_tpt = \
        steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, TransactionType.REFUND)

    expected_payment_tpt = \
        steps.SimpleApi.create_expected_tpt_row(
            context=context,
            partner_id=tech_client_id,
            contract_id=tech_contract_id,
            person_id=tech_person_id,
            trust_payment_id=trust_payment_id,
            payment_id=payment_id,
            trust_refund_id=trust_refund_id,
            amount=round(AMOUNT * currency_rate, 2),
            internal=1,
            oebs_org_id=payment_tpt[0]['oebs_org_id'],
        )

    utils.check_that(payment_tpt, contains_dicts_with_entries([expected_payment_tpt]),
                     'Сравниваем платеж с шаблоном')
