# -*- coding: utf-8 -*-


import os

import pytest

import btestlib.reporter as reporter
from balance import balance_db as db
from btestlib import utils
from balance import balance_steps as steps
from balance.features import Features
from btestlib.data.defaults import NatVer
from export_commons_acts import ACT_ATTRS, ACT_ROW_ATTRS, PrepayContext, create_act_on_prepay_invoice, \
    check_attrs
from btestlib.data.partner_contexts import AVIA_RU_CONTEXT, AVIA_RU_YT_CONTEXT, AVIA_SW_CONTEXT, AVIA_SW_YT_CONTEXT
import btestlib.config as balance_config

pytestmark = [reporter.feature(Features.OEBS, Features.ACT),
              pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/oebs')]

try:
    import balance_contracts
    from balance_contracts.oebs.act_by_rows import replace_mask
    from balance_contracts.contract_utils import utils as contract_utils
    from balance_contracts.contract_utils import deep_equals
    json_contracts_repo_path = os.path.dirname(os.path.abspath(balance_contracts.__file__))
except ImportError as err:
    json_contracts_repo_path = ''

JSON_OEBS_PATH = '/oebs/act_by_rows/'

'''
    Здесь проверяем случаи когда строки акта выгружаются в оебс без группировки:
     - у клиента установлен признак подробные документы
     - в строках акта разные цены
     - еще условия (их пока не проверяем):
     https://wiki.yandex-team.ru/balance/docs/process/oebs/#gruppirovkastrokaktovprivygruzke

    Посмотреть в коде какие атрибуты, как и куда выгружаются можно тут balance/processors/oebs/__init__.py
'''

def check_json_contract(act_id, json_file):
    # try:
    #     db.balance().execute(
    #         """update t_person_firm set oebs_export_dt = sysdate where person_id = :person_id""",
    #         {'person_id': person_id})
    # except Exception:
    #     pass

    steps.ExportSteps.init_oebs_api_export('Act', act_id)
    actual_json_data = steps.ExportSteps.get_json_data('Act', act_id)

    steps.ExportSteps.log_json_contract_actions(json_contracts_repo_path,
                                                JSON_OEBS_PATH,
                                                json_file,
                                                balance_config.FIX_CURRENT_JSON_CONTRACT)

    contract_utils.process_json_contract(json_contracts_repo_path,
                                         JSON_OEBS_PATH,
                                         json_file,
                                         actual_json_data,
                                         replace_mask,
                                         balance_config.FIX_CURRENT_JSON_CONTRACT)


# Устанавливаем клиенту подробные документы и проверяем, что каждая строка акта выгружается отдельно
@pytest.mark.parametrize('context, act_attrs, json_file', [
    # firm 1
    (PrepayContext.BANK_UR_RUB, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_RU_WITH_NDS, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
                                 ACT_ATTRS.SHIP_TO_CUSTOMER, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'act_detailed_bank_ur_rub.json'),
    (PrepayContext.BANK_YT_RUB, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_RU_YT_RUB, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
                                 ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'act_detailed_bank_yt_rub.json'),
    (PrepayContext.BANK_YTUR_KZT, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_RU_YT, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
                                   ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'act_detailed_bank_ytur_kzt.json'),
    # firm 2
    # pytest.mark.skip(reason='UKRAINE WAS TURNED OFF')
    # (PrepayContext.BANK_UA_UR_UAH, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_UA, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
    #                                 ACT_ATTRS.SHIP_TO_CUSTOMER, ACT_ATTRS.ACT_PAYMENT_TERM_DT]),
    # firm 4
    (PrepayContext.BANK_US_UR_USD, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_INVOICE, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
                                    ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'act_detailed_bank_us_ur_usd.json'),
    # firm 7
    (PrepayContext.BANK_SW_UR_EUR, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_INVOICE, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
                                    ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'act_detailed_bank_sw_ur_eur.json'),
    (PrepayContext.BANK_SW_YT_EUR, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_INVOICE, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
                                    ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'act_detailed_bank_sw_yt_eur.json'),
    (PrepayContext.CC_BY_YTPH_RUB, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_INVOICE, ACT_ATTRS.BILL_TO_CONTACT_PH,
                                    ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'act_detailed_cc_by_ytph_rub.json'),
    # firm 8 BALANCE-35586
    # (PrepayContext.BANK_TR_UR_TRY, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_INVOICE, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
    #                                 ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'act_detailed_bank_tr_ur_try.json'),
], ids=lambda context, act_attrs, json_file: "{context_name}-{paysys_id}".format(context_name=PrepayContext.name(context),
                                                                      paysys_id=context.paysys.id))
def test_export_act_detailed(context, act_attrs, json_file):
    with reporter.step(u'Создаем клиента с подробными печатными документами'):
        client_id = steps.ClientSteps.create(prevent_oebs_export=True)
        db.balance().execute('UPDATE t_client SET is_docs_detailed = 1 WHERE id = :client_id',
                             {'client_id': client_id})

    client_id, person_id, invoice_id, act_id = create_act_on_prepay_invoice(context, client_id=client_id)

    if balance_config.JSON_CONTRACT_OEBS and json_file:
        check_json_contract(act_id, json_file)

    else:
        steps.ExportSteps.export_oebs(client_id=client_id,
                                      person_id=person_id,
                                      invoice_id=invoice_id,
                                      act_id=act_id)

        act_rows_attrs = [ACT_ROW_ATTRS.ACT_ROW, ACT_ROW_ATTRS.PRINT_DOCS_DETAILED]
        check_attrs(act_id, act_attrs, act_rows_attrs)


# Проверяем, что если в строках акта разная цена, то в ОЕБС они выгружаются отдельно
# (выставляемся на adfox, т.к. у них цена зависит от количества)
@pytest.mark.parametrize('context, act_attrs, json_file', [
    (PrepayContext.BANK_YT_RUB_WITH_NDS, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_RU_WITH_NDS,
                                          ACT_ATTRS.BILL_TO_CONTACT_NON_PH, ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY,
                                          ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'act_different_price_in_rows_yt_rub_with_nds.json'),
], ids=lambda context, act_attrs, json_file: "{context_name}-{paysys_id}".format(context_name=PrepayContext.name(context),
                                                                      paysys_id=context.paysys.id))
def test_export_act_different_price_in_rows(context, act_attrs, json_file):
    qty_list = [2000000, 3000000]
    client_id, person_id, invoice_id, act_id = create_act_on_prepay_invoice(context, qty_list=qty_list)

    if balance_config.JSON_CONTRACT_OEBS and json_file:
        check_json_contract(act_id, json_file)

    else:
        steps.ExportSteps.export_oebs(client_id=client_id,
                                      person_id=person_id,
                                      invoice_id=invoice_id,
                                      act_id=act_id)

        act_rows_attrs = [ACT_ROW_ATTRS.ACT_ROW, ACT_ROW_ATTRS.PRINT_DOCS_COMMON]
        check_attrs(act_id, act_attrs, act_rows_attrs)


# для BANK_SW_UR_EUR_SAG выгрузка падает с ошибкой BALANCE-27745
@pytest.mark.parametrize('context, act_attrs, json_file', [
    # firm 1
    (AVIA_RU_CONTEXT, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_RU_WITH_NDS_S, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
                                 ACT_ATTRS.SHIP_TO_CUSTOMER, ACT_ATTRS.ACT_POSTPAY_PAYMENT_TERM_DT], 'act_avia_ru_context.json'),
    (AVIA_RU_YT_CONTEXT, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_RU_YT_RUB_S, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
                                 ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_POSTPAY_PAYMENT_TERM_DT], 'act_avia_ru_yt_context.json'),
    # # firm 16
    (AVIA_SW_CONTEXT,
     [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_INVOICE_EUR_S, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
      ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_POSTPAY_PAYMENT_TERM_DT], 'act_avia_sw_context.json'),
    (AVIA_SW_YT_CONTEXT,
     [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_INVOICE_EUR_S, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
      ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_POSTPAY_PAYMENT_TERM_DT], 'act_avia_sw_yt_context.json'),
], ids=lambda context: context.name)
def test_export_act_avia(context, act_attrs, json_file):
    contract_start_dt, prev_month_last_day = utils.Date.previous_month_first_and_last_days()
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context,
                                                                                       additional_params={
                                                                                           'start_dt': contract_start_dt})

    # создаем открутки по всем нац.версиям, чтобы проверить учет налогов во всех продуктах
    nat_ver_to_amount = steps.PartnerSteps.create_avia_completions(client_id, contract_id, context.currency, NatVer.values(),
                                                                   dt=contract_start_dt)

    # вызываем генерацию актов (при этом создается заказ и ЛС)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, prev_month_last_day)

    invoice_id = db.balance().execute("SELECT id FROM t_invoice WHERE CLIENT_ID = :client_id",
                                      {'client_id': client_id}, single_row=True)['id']

    act_id = db.balance().execute("SELECT id FROM t_act WHERE CLIENT_ID = :client_id",
                                  {'client_id': client_id}, single_row=True)['id']

    if balance_config.JSON_CONTRACT_OEBS and json_file:
        check_json_contract(act_id, json_file)

    else:
        steps.ExportSteps.export_oebs(client_id=client_id,
                                      person_id=person_id,
                                      contract_id=contract_id,
                                      invoice_id=invoice_id,
                                      act_id=act_id)

        act_rows_attrs = [ACT_ROW_ATTRS.ACT_ROW, ACT_ROW_ATTRS.PRINT_DOCS_COMMON]
        check_attrs(act_id, act_attrs, act_rows_attrs, merge_balance_act_rows=False)
