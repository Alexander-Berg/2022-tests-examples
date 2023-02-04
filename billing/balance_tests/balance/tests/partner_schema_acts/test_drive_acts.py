# coding: utf-8
import datetime
from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta

import balance.balance_db as db
import balance.balance_steps as steps
import btestlib.reporter as reporter
import btestlib.utils as utils
from btestlib.constants import Services, Products, Paysyses, NdsNew, ContractType
from btestlib.data.partner_contexts import DRIVE_CONTEXT
from btestlib.matchers import contains_dicts_with_entries

__author__ = 'quark'

#https://st.yandex-team.ru/BALANCE-29496

START_DT = utils.Date.first_day_of_month(datetime.datetime.now() - relativedelta(months=2))
START_DT_SECOND_MONTH = START_DT + relativedelta(months=1)
CONTRACT_FINISH_DT = utils.Date.first_day_of_month(datetime.datetime.now() + relativedelta(months=6))
AMOUNT = Decimal('190.01')
AMOUNT_WO_NDS = Decimal('66.6')
FIRST_MONTH_ADDITIONAL_AMOUNT = Decimal('1')


# продукта без ндс в проде нет, пока не работает, тест отключен
# Insert into T_PARTNER_PRODUCT (ID,PRODUCT_ID,SERVICE_ID,CURRENCY_ISO_CODE,ORDER_TYPE,UNIFIED_ACCOUNT_ROOT,
# NETTING_PAYMENT_TYPE,SERVICE_PRODUCT_ID,END_DT,PROMO_SUBT_ORDER)
# values ('307','50917701','604','RUB','fine','0',null,null,null,null);
# эта строчка выпилена в тесте тоже
# no_parallel пока выключен у тестов, т.к. тест один
# @pytest.mark.no_parallel('drive', write=False)
def acts_two_contracts_different_nds():
    client_id, person_id, contract_id_with_nds, _ = steps.ContractSteps.create_partner_contract(DRIVE_CONTEXT,
                                                                                                additional_params={
                                                                                                    'start_dt': START_DT})
    _, _, contract_id_wo_nds, _ = steps.ContractSteps.create_partner_contract(DRIVE_CONTEXT,
                                                                                              client_id=client_id,
                                                                                              person_id=person_id,
                                                                                              additional_params={
                                                                                                  'start_dt': START_DT,
                                                                                                  'commission': ContractType.LICENSE})

    delete_drive_completions(START_DT)
    create_drive_completion(Products.CARSHARING_WITH_NDS_1.id, START_DT, AMOUNT)
    create_drive_completion(Products.CARSHARING_WO_NDS.id, START_DT, AMOUNT_WO_NDS)

    # запускаем генерацию актов с ндс
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id_with_nds, START_DT)

    # запускаем генерацию актов без ндс
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id_wo_nds, START_DT)

    # проверяем данные в счетах
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    expected_invoice_data = [
        steps.CommonData.create_expected_invoice_data_by_context(DRIVE_CONTEXT, contract_id_with_nds, person_id,
                                                                 amount=AMOUNT, dt=START_DT),
        steps.CommonData.create_expected_invoice_data_by_context(DRIVE_CONTEXT, contract_id_wo_nds, person_id,
                                                                 amount=AMOUNT_WO_NDS, dt=START_DT,
                                                                 paysys_id=Paysyses.BANK_PH_WO_NDS_RUB_CARSHARING.id,
                                                                 nds_pct=NdsNew.ZERO.pct_on_dt(START_DT),
                                                                 nds=0)]
    # nds=0 только для того, чтобы тест работал,
    # пока не починят https://st.yandex-team.ru/BALANCE-30398
    utils.check_that(invoice_data, contains_dicts_with_entries(expected_invoice_data),
                     u'Проверяем, что созданы счета')

    # проверяем данные в актах
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    expected_act_data = [steps.CommonData.create_expected_act_data(AMOUNT, utils.Date.last_day_of_month(START_DT)),
                         steps.CommonData.create_expected_act_data(AMOUNT_WO_NDS,
                                                                   utils.Date.last_day_of_month(START_DT))]

    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     u'Проверяем, что сгенерирован ожидаемый акт')


@pytest.mark.smoke
# @pytest.mark.no_parallel('drive', write=False)
def test_acts_with_nds_two_month():
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(DRIVE_CONTEXT,
                                                                                       additional_params={
                                                                                           'start_dt': START_DT})

    delete_drive_completions(START_DT)
    delete_drive_completions(START_DT_SECOND_MONTH)
    create_drive_completion(Products.CARSHARING_WITH_NDS_1.id, START_DT, AMOUNT)

    # запускаем генерацию актов
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, START_DT)

    # Второй месяц
    create_drive_completion(Products.CARSHARING_WITH_NDS_1.id, START_DT, FIRST_MONTH_ADDITIONAL_AMOUNT)
    create_drive_completion(Products.CARSHARING_WITH_NDS_1.id, START_DT_SECOND_MONTH, AMOUNT)

    # запускаем генерацию актов
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, START_DT_SECOND_MONTH)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    total_amount = 2 * AMOUNT + FIRST_MONTH_ADDITIONAL_AMOUNT
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(DRIVE_CONTEXT, contract_id,
                                                                                     person_id,
                                                                                     amount=total_amount, dt=START_DT)
    utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data]),
                     u'Проверяем, что созданы ожидаемые счета')

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    expected_act_data = [steps.CommonData.create_expected_act_data(AMOUNT, utils.Date.last_day_of_month(START_DT)),
                         steps.CommonData.create_expected_act_data(AMOUNT + FIRST_MONTH_ADDITIONAL_AMOUNT,
                                                                   utils.Date.last_day_of_month(START_DT_SECOND_MONTH)),
                         ]

    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     u'Проверяем, что сгенерирован ожидаемый акт')


# ----------------------------------------------------------------
# Utils
def delete_drive_completions(start_dt):
    """
    Поскольку открутки драйва не привязаны к конкретному клиенту или договору
    их нужно очищать перед каждым запуском тестов
    Побочный эффект - эти тесты нельзя запускать паралллельно
    """
    end_dt = start_dt + relativedelta(months=1)
    with reporter.step(u'Удаление откруток Яндекс.Драйва'):
        query = "delete from T_PARTNER_PRODUCT_COMPLETION " \
                "where dt >= :start_dt and dt < :end_dt and service_id = :service_id"
        params = {'start_dt': start_dt, 'end_dt': end_dt, 'service_id': Services.DRIVE.id}

        db.balance().execute(query, params)


def create_drive_completion(product_id, dt, amount):
    with reporter.step(u'Добавляем открутку Яндекс.Драйва'):
        query = "INSERT INTO T_PARTNER_PRODUCT_COMPLETION (DT, SERVICE_ID, PRODUCT_ID, AMOUNT) " \
                "VALUES (:dt, :service_id, :product_id, :amount)"
        params = {'product_id': product_id, 'amount': amount, 'dt': dt, 'service_id': Services.DRIVE.id}

        db.balance().execute(query, params)
