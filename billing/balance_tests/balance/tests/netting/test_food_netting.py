# coding: utf-8

from decimal import Decimal as D
from datetime import datetime

from dateutil.relativedelta import relativedelta

import pytest

from balance import balance_steps as steps
from balance.balance_steps import new_taxi_steps as taxi_steps
from balance.features import Features, AuditFeatures
from btestlib import reporter
from btestlib import utils
from btestlib.constants import (
    OEBSOperationType,
    Services,
    TransactionType,
    FoodProductType,
    PaysysType,
    PaymentType,
)
from btestlib.data.partner_contexts import FOOD_RESTAURANT_CONTEXT, FOOD_RESTAURANT_KZ_CONTEXT, \
    FOOD_RESTAURANT_BY_CONTEXT, REST_SITES_CONTEXT, \
    FOOD_RESTAURANT_BY_TAXI_BV_CONTEXT, FOOD_RESTAURANT_BY_FOODTECH_DELIVERY_BV_CONTEXT, \
    FOOD_MERCURY_CONTEXT, FOOD_FULL_MERCURY_CONTEXT, FOOD_RESTAURANT_CONTEXT_WITH_MERCURY
from btestlib.matchers import contains_dicts_equal_to, contains_dicts_with_entries

pytestmark = [
    reporter.feature(Features.NETTING, Features.FOOD),
]

# prev_month_start_dt, prev_month_end_dt = utils.Date.previous_month_first_and_last_days()
completion_sum = D('34.3')
correction_amount = D('4.3')

RESTAURANT_CONTEXTS = [
    FOOD_RESTAURANT_BY_TAXI_BV_CONTEXT,
    FOOD_RESTAURANT_BY_FOODTECH_DELIVERY_BV_CONTEXT,
    FOOD_RESTAURANT_CONTEXT,
    FOOD_RESTAURANT_KZ_CONTEXT,
    FOOD_RESTAURANT_BY_CONTEXT,
    REST_SITES_CONTEXT,
    FOOD_MERCURY_CONTEXT,
    FOOD_FULL_MERCURY_CONTEXT,
    FOOD_RESTAURANT_CONTEXT_WITH_MERCURY
]

PRODUCT_TYPE_MAPPING = {
    Services.REST_SITES_SERVICES.id: FoodProductType.TP_ORDER_PROCESSING,
}


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C02_1_Eda))
@pytest.mark.parametrize('context', RESTAURANT_CONTEXTS, ids=lambda context: context.name)
def test_netting_percent(context):
    month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
    month_migration_minus1_start_dt, month_migration_minus1_end_dt, \
    month_minus2_start_dt, month_minus2_end_dt, \
    month_minus1_start_dt, month_minus1_end_dt, \
    migration_dt = \
        get_dates_for_migration(context)

    completion_dt = month_migration_minus2_start_dt
    netting_dt = completion_dt + relativedelta(days=1)

    # Создаём клиента, партнёра, плательщика, договор
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context, is_offer=1, additional_params={'start_dt': month_migration_minus2_start_dt}
    )

    steps.PartnerSteps.create_fake_product_completion(completion_dt.replace(hour=23, minute=59, second=59),
                                                      client_id=client_id,
                                                      service_id=context.commission_service.id, service_order_id=0,
                                                      commission_sum=completion_sum,
                                                      type=PRODUCT_TYPE_MAPPING.get(context.commission_service.id,
                                                                                    FoodProductType.GOODS))

    # вторая открутка не должна учитываться, т.к. ее дата уже после даты расчета
    steps.PartnerSteps.create_fake_product_completion(netting_dt, client_id=client_id,
                                                      service_id=context.commission_service.id, service_order_id=0,
                                                      commission_sum=D('100500'),
                                                      type=PRODUCT_TYPE_MAPPING.get(context.commission_service.id,
                                                                                    FoodProductType.GOODS))

    steps.TaxiSteps.process_netting(contract_id, netting_dt, forced=True)

    # получаем данные по корректировке
    correction_data = taxi_steps.TaxiSteps.get_thirdparty_corrections_by_contract_id(contract_id)

    invoice_id, invoice_eid, _ = steps.InvoiceSteps.get_invoice_by_service_or_service_code(contract_id,
                                                                                           context.commission_service,
                                                                                           is_service_code_exist=False)
    # получаем ожидаемые данные по корректировке
    expected_correction_data = [
        taxi_steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id, invoice_eid,
                                                            netting_dt - relativedelta(seconds=1),
                                                            completion_sum,
                                                            context)]

    utils.check_that(correction_data, contains_dicts_equal_to(expected_correction_data),
                     'Сравниваем данные по корректировке с шаблоном')

    # получаем реальные и ожидаемые данные по счету
    invoice_data = steps.InvoiceSteps.get_invoice_receipt_data(invoice_id)
    expected_invoice_data = {
        'receipt_sum': completion_sum,
        'receipt_sum_1c': D('0')
    }

    utils.check_that(invoice_data, contains_dicts_equal_to([expected_invoice_data], same_length=False),
                     'Сравниваем данные в счете с шаблоном')


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C02_1_Eda))
@pytest.mark.parametrize('context', RESTAURANT_CONTEXTS, ids=lambda context: context.name)
def test_netting_calculation_including_correction(context):
    month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
    month_migration_minus1_start_dt, month_migration_minus1_end_dt, \
    month_minus2_start_dt, month_minus2_end_dt, \
    month_minus1_start_dt, month_minus1_end_dt, \
    migration_dt = \
        get_dates_for_migration(context)

    completion_dt = month_migration_minus2_start_dt
    netting_dt = completion_dt + relativedelta(days=1)

    # Создаём клиента, партнёра, плательщика, договор
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context, is_offer=1, additional_params={'start_dt': month_migration_minus2_start_dt}
    )
    # создаем открутки и запускаем обработку
    steps.PartnerSteps.create_fake_product_completion(completion_dt, client_id=client_id,
                                                      service_id=context.commission_service.id,
                                                      service_order_id=0,
                                                      commission_sum=completion_sum,
                                                      type=PRODUCT_TYPE_MAPPING.get(context.commission_service.id,
                                                                                    FoodProductType.GOODS))

    steps.TaxiSteps.process_netting(contract_id, netting_dt, forced=True)

    # получаем id счета
    invoice_id, invoice_eid, _ = steps.InvoiceSteps.get_invoice_by_service_or_service_code(contract_id,
                                                                                           context.commission_service,
                                                                                           is_service_code_exist=False)

    # начинаем подготовку ожидаемых данных
    expected_corrections_data = [
        taxi_steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id, invoice_eid,
                                                            netting_dt - relativedelta(seconds=1),
                                                            completion_sum, context)]

    # создаем открутки
    steps.PartnerSteps.create_fake_product_completion(netting_dt, client_id=client_id,
                                                      service_id=context.commission_service.id, service_order_id=0,
                                                      commission_sum=completion_sum * D('0.3'),
                                                      type=PRODUCT_TYPE_MAPPING.get(context.commission_service.id,
                                                                                    FoodProductType.GOODS))

    # добавляем информацию по взаимозачету из оебс с корректировкой
    taxi_steps.TaxiSteps.create_cash_payment_fact(invoice_eid, completion_sum - correction_amount, netting_dt,
                                                  OEBSOperationType.INSERT_NETTING,
                                                  service_id=context.service.id
                                                  if context.service == Services.FOOD_MERCURY_PAYMENTS
                                                  else None)
    taxi_steps.TaxiSteps.create_cash_payment_fact(invoice_eid, -correction_amount, netting_dt,
                                                  OEBSOperationType.CORRECTION_NETTING,
                                                  service_id=context.service.id
                                                  if context.service == Services.FOOD_MERCURY_PAYMENTS
                                                  else None)

    # запускаем обработку
    taxi_steps.TaxiSteps.process_payment(invoice_id, True)

    # дополняем ожидаемые данные корректировкой из оебс
    expected_corrections_data.append(
            taxi_steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id, invoice_eid,
                                                                netting_dt, correction_amount, context,
                                                                transaction_type=TransactionType.PAYMENT,
                                                                internal=1))

    # получаем реальные и ожидаемые данные по счету
    invoice_data = steps.InvoiceSteps.get_invoice_receipt_data(invoice_id)
    expected_invoice_data = {
        'receipt_sum': completion_sum - correction_amount,
        'receipt_sum_1c': completion_sum - correction_amount
    }

    utils.check_that(invoice_data, contains_dicts_equal_to([expected_invoice_data], same_length=False),
                     'Сравниваем данные в счете с шаблоном')

    # запускаем обработку
    steps.TaxiSteps.process_netting(contract_id, netting_dt + relativedelta(days=1), forced=True)

    # дополняем ожидаемые данные
    expected_corrections_data.append(
        taxi_steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id,
                                                            invoice_eid,
                                                            netting_dt + relativedelta(days=1) - relativedelta(
                                                                seconds=1),
                                                            completion_sum * D('0.3') + correction_amount,
                                                            context))
    # получаем данные по корректировкам
    correction_data = taxi_steps.TaxiSteps.get_thirdparty_corrections_by_contract_id(contract_id)

    utils.check_that(correction_data, contains_dicts_equal_to(expected_corrections_data),
                     'Сравниваем данные по корректировке с шаблоном')


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C02_1_Eda))
@pytest.mark.parametrize('context', RESTAURANT_CONTEXTS, ids=lambda context: context.name)
def test_netting_previous_day_data_only(context):
    month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
    month_migration_minus1_start_dt, month_migration_minus1_end_dt, \
    month_minus2_start_dt, month_minus2_end_dt, \
    month_minus1_start_dt, month_minus1_end_dt, \
    migration_dt = \
        get_dates_for_migration(context)

    completion_dt = month_migration_minus2_start_dt
    netting_dt = completion_dt + relativedelta(days=1)

    COMPLETION_AMOUNT = D('1')
    PAYMENT_AMOUNT = D('20')
    dt = completion_dt.replace(hour=23, minute=59, second=59)

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context, is_offer=1, additional_params={'start_dt': completion_dt}
    )

    # добавляем открутки за текущий и за предыдущий день
    insert_completion(context, client_id, dt, COMPLETION_AMOUNT)
    insert_completion(context, client_id, netting_dt, 2 * COMPLETION_AMOUNT)

    invoice_id, invoice_eid, _ = steps.InvoiceSteps.get_invoice_by_service_or_service_code(contract_id,
                                                                                           context.commission_service,
                                                                                           is_service_code_exist=False)

    # добавляем корректировки за текущий и за предыдущий день
    insert_correction(context, client_id, person_id, contract_id, invoice_eid, dt, TransactionType.PAYMENT, PAYMENT_AMOUNT)
    insert_correction(context, client_id, person_id, contract_id, invoice_eid, netting_dt, TransactionType.PAYMENT, 2 * PAYMENT_AMOUNT)

    steps.TaxiSteps.process_netting(contract_id, netting_dt, forced=True)
    correction_data = taxi_steps.TaxiSteps.get_thirdparty_corrections_by_contract_id(contract_id)
    correction_data = [c for c in correction_data if c['amount'] == PAYMENT_AMOUNT + COMPLETION_AMOUNT]

    expected_correction_data = taxi_steps.TaxiData.create_expected_correction_data(
        client_id, contract_id, person_id, invoice_eid, dt, PAYMENT_AMOUNT + COMPLETION_AMOUNT, context
    )

    utils.check_that(correction_data, contains_dicts_with_entries([expected_correction_data]),
                     u"Проверяем, что учтены данные только за предыдущий день, но не за текущий")


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C02_1_Eda))
@pytest.mark.parametrize('context', [FOOD_FULL_MERCURY_CONTEXT], ids=lambda context: context.name)
def test_netting_calculation_with_two_nettings(context):
    month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
    month_migration_minus1_start_dt, month_migration_minus1_end_dt, \
    month_minus2_start_dt, month_minus2_end_dt, \
    month_minus1_start_dt, month_minus1_end_dt, \
    migration_dt = \
        get_dates_for_migration(context)

    completion_dt = month_migration_minus2_start_dt
    netting_dt = completion_dt + relativedelta(days=1)

    # Создаём клиента, партнёра, плательщика, договор
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context, is_offer=1, additional_params={'start_dt': month_migration_minus2_start_dt}
    )
    # создаем открутки по 628 и 1176 сервисам и запускаем обработку
    steps.PartnerSteps.create_fake_product_completion(completion_dt, client_id=client_id,
                                                      service_id=context.commission_service.id,
                                                      service_order_id=0,
                                                      commission_sum=completion_sum,
                                                      type=PRODUCT_TYPE_MAPPING.get(context.commission_service.id,
                                                                                    FoodProductType.GOODS))

    steps.PartnerSteps.create_fake_product_completion(completion_dt, client_id=client_id,
                                                      service_id=FOOD_RESTAURANT_CONTEXT.commission_service.id,
                                                      service_order_id=0,
                                                      commission_sum=completion_sum,
                                                      type=PRODUCT_TYPE_MAPPING.get(context.commission_service.id,
                                                                                    FoodProductType.GOODS))

    steps.TaxiSteps.process_netting(contract_id, netting_dt, forced=True)

    # получаем id счета
    invoice_id, invoice_eid, _ = steps.InvoiceSteps.get_invoice_by_service_or_service_code(contract_id,
                                                                                           context.commission_service,
                                                                                           is_service_code_exist=False)

    # начинаем подготовку ожидаемых данных
    expected_corrections_data = [
        taxi_steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id, invoice_eid,
                                                            netting_dt - relativedelta(seconds=1),
                                                            completion_sum, context),
        taxi_steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id, invoice_eid,
                                                            netting_dt - relativedelta(seconds=1),
                                                            completion_sum, context,
                                                            service_id=FOOD_RESTAURANT_CONTEXT.service.id)]

    # создаем открутки
    steps.PartnerSteps.create_fake_product_completion(netting_dt, client_id=client_id,
                                                      service_id=context.commission_service.id, service_order_id=0,
                                                      commission_sum=completion_sum * D('0.3'),
                                                      type=PRODUCT_TYPE_MAPPING.get(context.commission_service.id,
                                                                                    FoodProductType.GOODS))

    steps.PartnerSteps.create_fake_product_completion(netting_dt, client_id=client_id,
                                                      service_id=FOOD_RESTAURANT_CONTEXT.commission_service.id,
                                                      service_order_id=0, commission_sum=completion_sum * D('0.3'),
                                                      type=PRODUCT_TYPE_MAPPING.get(
                                                          FOOD_RESTAURANT_CONTEXT.commission_service.id,
                                                          FoodProductType.GOODS))

    # добавляем информацию по взаимозачету из оебс с корректировкой
    taxi_steps.TaxiSteps.create_cash_payment_fact(invoice_eid, completion_sum - correction_amount, netting_dt,
                                                  OEBSOperationType.INSERT_NETTING, service_id=context.service.id)
    taxi_steps.TaxiSteps.create_cash_payment_fact(invoice_eid, -correction_amount, netting_dt,
                                                  OEBSOperationType.CORRECTION_NETTING, service_id=context.service.id)
    # кейс по 629 сервису: не передаем service_id и проверяем, что выберется он, а не 1176
    taxi_steps.TaxiSteps.create_cash_payment_fact(invoice_eid, completion_sum - correction_amount, netting_dt,
                                                  OEBSOperationType.INSERT_NETTING)
    taxi_steps.TaxiSteps.create_cash_payment_fact(invoice_eid, -correction_amount, netting_dt,
                                                  OEBSOperationType.CORRECTION_NETTING)

    # запускаем обработку
    taxi_steps.TaxiSteps.process_payment(invoice_id, True)

    # дополняем ожидаемые данные корректировкой из оебс
    expected_corrections_data.append(
            taxi_steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id, invoice_eid,
                                                                netting_dt, correction_amount, context,
                                                                transaction_type=TransactionType.PAYMENT,
                                                                internal=1))
    expected_corrections_data.append(
        taxi_steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id, invoice_eid,
                                                            netting_dt, correction_amount, context,
                                                            transaction_type=TransactionType.PAYMENT,
                                                            service_id=FOOD_RESTAURANT_CONTEXT.service.id,
                                                            internal=1))

    # получаем реальные и ожидаемые данные по счету
    invoice_data = steps.InvoiceSteps.get_invoice_receipt_data(invoice_id)

    expected_invoice_data = {
        'receipt_sum': (completion_sum - correction_amount) * 2,
        'receipt_sum_1c': (completion_sum - correction_amount) * 2,
    }

    utils.check_that(invoice_data, contains_dicts_equal_to([expected_invoice_data], same_length=False),
                     'Сравниваем данные в счете 1 с шаблоном')

    # запускаем обработку
    steps.TaxiSteps.process_netting(contract_id, netting_dt + relativedelta(days=1), forced=True)

    # дополняем ожидаемые данные
    expected_corrections_data.append(
        taxi_steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id,
                                                            invoice_eid,
                                                            netting_dt + relativedelta(days=1) - relativedelta(
                                                                seconds=1),
                                                            completion_sum * D('0.3') + correction_amount,
                                                            context))
    expected_corrections_data.append(
        taxi_steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id,
                                                            invoice_eid,
                                                            netting_dt + relativedelta(days=1) - relativedelta(
                                                                seconds=1),
                                                            completion_sum * D('0.3') + correction_amount,
                                                            context, service_id=FOOD_RESTAURANT_CONTEXT.service.id))
    # получаем данные по корректировкам
    correction_data = taxi_steps.TaxiSteps.get_thirdparty_corrections_by_contract_id(contract_id)

    utils.check_that(correction_data, contains_dicts_equal_to(expected_corrections_data),
                     'Сравниваем данные по корректировке с шаблоном')


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C02_1_Eda))
@pytest.mark.parametrize('context', [FOOD_FULL_MERCURY_CONTEXT], ids=lambda context: context.name)
def test_stop_netting_after_date(context):
    netting_config = steps.CommonPartnerSteps.get_netting_config()
    service_netting_conf, = filter(lambda conf: conf['service_id'] == context.service.id, netting_config)
    stop_netting_after_date = service_netting_conf['stop_netting_after_date']
    completion_dt = stop_netting_after_date - relativedelta(days=2)

    # Создаём клиента, партнёра, плательщика, договор
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context, is_offer=1, additional_params={'start_dt': completion_dt}
    )
    # создаем открутки по 628 и 1176 сервисам и запускаем обработку
    steps.PartnerSteps.create_fake_product_completion(completion_dt, client_id=client_id,
                                                      service_id=context.commission_service.id,
                                                      service_order_id=0,
                                                      commission_sum=completion_sum,
                                                      type=PRODUCT_TYPE_MAPPING.get(context.commission_service.id,
                                                                                    FoodProductType.GOODS))

    steps.PartnerSteps.create_fake_product_completion(completion_dt, client_id=client_id,
                                                      service_id=FOOD_RESTAURANT_CONTEXT.commission_service.id,
                                                      service_order_id=0,
                                                      commission_sum=completion_sum,
                                                      type=PRODUCT_TYPE_MAPPING.get(context.commission_service.id,
                                                                                    FoodProductType.GOODS))
    # В примерах: дата остановки неттинга ставится на 2 число.
    # Неттинг запускается 1го числа, чтобы посчитаться по откруткам до 1го числа (не включительно).
    netting_dt = stop_netting_after_date - relativedelta(days=1)
    steps.TaxiSteps.process_netting(contract_id, netting_dt, forced=True)

    # получаем id счета
    invoice_id, invoice_eid, _ = steps.InvoiceSteps.get_invoice_by_service_or_service_code(contract_id,
                                                                                           context.commission_service,
                                                                                           is_service_code_exist=False)

    # начинаем подготовку ожидаемых данных
    expected_corrections_data = [
        taxi_steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id, invoice_eid,
                                                            netting_dt - relativedelta(seconds=1),
                                                            completion_sum, context),
        taxi_steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id, invoice_eid,
                                                            netting_dt - relativedelta(seconds=1),
                                                            completion_sum, context,
                                                            service_id=FOOD_RESTAURANT_CONTEXT.service.id)]

    # создаем открутки, которые уже не попадут в неттинг, т.к. он не запустится
    completion_dt = stop_netting_after_date - relativedelta(days=1)
    steps.PartnerSteps.create_fake_product_completion(completion_dt, client_id=client_id,
                                                      service_id=context.commission_service.id, service_order_id=0,
                                                      commission_sum=completion_sum * D('0.3'),
                                                      type=PRODUCT_TYPE_MAPPING.get(context.commission_service.id,
                                                                                    FoodProductType.GOODS))

    steps.PartnerSteps.create_fake_product_completion(completion_dt, client_id=client_id,
                                                      service_id=FOOD_RESTAURANT_CONTEXT.commission_service.id,
                                                      service_order_id=0, commission_sum=completion_sum * D('0.3'),
                                                      type=PRODUCT_TYPE_MAPPING.get(
                                                          FOOD_RESTAURANT_CONTEXT.commission_service.id,
                                                          FoodProductType.GOODS))

    # добавляем информацию по взаимозачету из оебс с корректировкой
    taxi_steps.TaxiSteps.create_cash_payment_fact(invoice_eid, completion_sum - correction_amount, completion_dt,
                                                  OEBSOperationType.INSERT_NETTING, service_id=context.service.id)
    taxi_steps.TaxiSteps.create_cash_payment_fact(invoice_eid, -correction_amount, completion_dt,
                                                  OEBSOperationType.CORRECTION_NETTING, service_id=context.service.id)
    # кейс по 629 сервису: не передаем service_id и проверяем, что выберется он, а не 1176
    taxi_steps.TaxiSteps.create_cash_payment_fact(invoice_eid, completion_sum - correction_amount, completion_dt,
                                                  OEBSOperationType.INSERT_NETTING)
    taxi_steps.TaxiSteps.create_cash_payment_fact(invoice_eid, -correction_amount, completion_dt,
                                                  OEBSOperationType.CORRECTION_NETTING)

    # запускаем обработку
    taxi_steps.TaxiSteps.process_payment(invoice_id, True)

    # дополняем ожидаемые данные корректировкой из оебс
    expected_corrections_data.append(
            taxi_steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id, invoice_eid,
                                                                completion_dt, correction_amount, context,
                                                                transaction_type=TransactionType.PAYMENT,
                                                                internal=1))
    expected_corrections_data.append(
        taxi_steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id, invoice_eid,
                                                            completion_dt, correction_amount, context,
                                                            transaction_type=TransactionType.PAYMENT,
                                                            service_id=FOOD_RESTAURANT_CONTEXT.service.id,
                                                            internal=1))

    # получаем реальные и ожидаемые данные по счету
    invoice_data = steps.InvoiceSteps.get_invoice_receipt_data(invoice_id)

    expected_invoice_data = {
        'receipt_sum': (completion_sum - correction_amount) * 2,
        'receipt_sum_1c': (completion_sum - correction_amount) * 2,
    }

    utils.check_that(invoice_data, contains_dicts_equal_to([expected_invoice_data], same_length=False),
                     'Сравниваем данные в счете 1 с шаблоном')

    netting_dt = stop_netting_after_date
    # запускаем обработку
    steps.TaxiSteps.process_netting(contract_id, netting_dt, forced=True)

    # неттинг на новые открутки не сформируется
    correction_data = taxi_steps.TaxiSteps.get_thirdparty_corrections_by_contract_id(contract_id)

    utils.check_that(correction_data, contains_dicts_equal_to(expected_corrections_data),
                     'Сравниваем данные по корректировке с шаблоном')


def insert_correction(context, client_id, person_id, contract_id, invoice_eid, dt, type, amount):
    steps.SimpleApi.create_fake_tpt_row(context, client_id, person_id, contract_id,
                                        dt=dt,
                                        is_correction=True,
                                        transaction_type=type,
                                        amount=amount,
                                        invoice_eid=invoice_eid,
                                        auto=1,
                                        payment_type=PaymentType.CORRECTION_NETTING,
                                        paysys_type_cc=PaysysType.YANDEX)


def insert_completion(context, client_id, dt, amount):
    steps.PartnerSteps.create_fake_product_completion(dt, client_id=client_id,
                                                      service_id=context.commission_service.id,
                                                      service_order_id=0,
                                                      commission_sum=amount,
                                                      amount=3 * amount,
                                                      type=PRODUCT_TYPE_MAPPING.get(context.commission_service.id, FoodProductType.GOODS))


def get_dates_for_migration(context):
    if getattr(context, 'migration_alias', None):
        migration_params = steps.CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
    else:
        migration_params = {}
    migration_dt = migration_params and migration_params.get('migration_date') or None
    # 2 месяца до даты миграции
    month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
    month_migration_minus1_start_dt, month_migration_minus1_end_dt = \
        utils.Date.previous_two_months_dates(migration_dt)
    # 2 предыдуших месяца от текущего, если они больше даты миграции, либо 2 месяца вперед от даты миграции
    posible_oebs_compls_start_dt, _, _, _ = utils.Date.previous_two_months_dates()
    oebs_compls_start_dt = max(posible_oebs_compls_start_dt, migration_dt) if migration_dt else posible_oebs_compls_start_dt
    month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = \
        utils.Date.previous_two_months_dates(oebs_compls_start_dt + relativedelta(months=2))
    return month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
           month_migration_minus1_start_dt, month_migration_minus1_end_dt, \
           month_minus2_start_dt, month_minus2_end_dt, \
           month_minus1_start_dt, month_minus1_end_dt, migration_dt
