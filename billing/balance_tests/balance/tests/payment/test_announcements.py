# coding: utf-8
import pytest
from datetime import datetime
from decimal import Decimal as D

from dateutil.relativedelta import relativedelta

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import TransactionType
from btestlib.data.partner_contexts import ANNOUNCEMENT_CONTEXT
from btestlib.matchers import contains_dicts_with_entries

AMOUNT = D('111.11')
PAYMENT_DT = utils.Date.first_day_of_month()
CONTRACT_START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=2))


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


# noinspection DuplicatedCode
def test_payment_with_tech_ids():
    context = ANNOUNCEMENT_CONTEXT

    # получаем контракт и клиента
    client_id, person_id, contract_id = steps.CommonPartnerSteps.get_active_tech_ids(context.service)
    service_product_id = steps.SimpleApi.create_service_product(context.service)

    # создаем платеж
    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(context.service,
                                                       [service_product_id],
                                                       prices_list=[AMOUNT],
                                                       currency=context.currency,
                                                       )
    steps.CommonPartnerSteps.export_payment(payment_id)

    # проверяем tpt
    payment_tpt = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)
    invoice_eid = steps.InvoiceSteps.get_invoice_eid(contract_id=contract_id,
                                                     client_id=client_id,
                                                     currency=context.currency.char_code)
    product_mapping_config = steps.CommonPartnerSteps.get_product_mapping_config(context.service)
    main_product_id = product_mapping_config['default_product_mapping'][context.payment_currency.iso_code]['default']
    expected_payment_tpt = [steps.SimpleApi.create_expected_tpt_row(context, client_id, contract_id,
                                                                    person_id, trust_payment_id, payment_id,
                                                                    amount=AMOUNT,
                                                                    payment_type=context.tpt_payment_type,
                                                                    paysys_type_cc=context.tpt_paysys_type_cc,
                                                                    internal=1,
                                                                    product_id=main_product_id,
                                                                    invoice_eid=invoice_eid,
                                                                    ),
                            ]

    utils.check_that(payment_tpt, contains_dicts_with_entries(expected_payment_tpt), u'Сравниваем платеж с шаблоном')


# noinspection DuplicatedCode
def test_refund_with_tech_ids():
    context = ANNOUNCEMENT_CONTEXT

    # получаем контракт и клиента
    client_id, person_id, contract_id = steps.CommonPartnerSteps.get_active_tech_ids(context.service)
    service_product_id = steps.SimpleApi.create_service_product(context.service)

    # создаем платеж
    service_order_id, trust_payment_id, _purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(context.service, service_product_id, price=AMOUNT)

    # делаем возврат
    trust_refund_id, refund_id = \
        steps.SimpleApi.create_refund(
            service=context.service,
            service_order_id=service_order_id,
            trust_payment_id=trust_payment_id,
            delta_amount=AMOUNT)

    # обрабатываем платеж и возврат
    steps.CommonPartnerSteps.export_payment(payment_id)
    steps.CommonPartnerSteps.export_payment(refund_id)

    # проверяем tpt
    payment_tpt = \
        steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, TransactionType.REFUND)

    invoice_eid = steps.InvoiceSteps.get_invoice_eid(contract_id=contract_id,
                                                     client_id=client_id,
                                                     currency=context.currency.char_code)
    expected_payment_tpt = \
        steps.SimpleApi.create_expected_tpt_row(
            context, client_id, contract_id,
            person_id, trust_payment_id, payment_id,
            trust_refund_id=trust_refund_id,
            internal=1, invoice_eid=invoice_eid,
            amount=AMOUNT
        )

    utils.check_that(payment_tpt, contains_dicts_with_entries([expected_payment_tpt]),
                     'Сравниваем платеж с шаблоном')
