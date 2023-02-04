# -*- coding: utf-8 -*-

import os

import pytest

import balance.balance_steps as steps
import btestlib.reporter as reporter
from balance.features import Features
from export_commons_acts import ACT_ATTRS, ACT_ROW_ATTRS, PrepayContext, PostpayContext, \
    create_act_on_prepay_invoice, create_act_on_overdraft_invoice, \
    create_act_on_prepay_speechkit_invoice, create_act_on_postpay_invoice, \
    create_act_on_personal_invoice, check_attrs
import btestlib.config as balance_config

pytestmark = [reporter.feature(Features.OEBS, Features.ACT),
              pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/oebs')]

try:
    import balance_contracts
    from balance_contracts.oebs.act import replace_mask
    from balance_contracts.contract_utils import utils as contract_utils
    from balance_contracts.contract_utils import deep_equals
    json_contracts_repo_path = os.path.dirname(os.path.abspath(balance_contracts.__file__))
except ImportError as err:
    json_contracts_repo_path = ''

JSON_OEBS_PATH = '/oebs/act/'

'''
    Здесь проверяем случаи когда строки акта выгружаются в оебс сгруппированными в одну строку

    Условия группировки: https://wiki.yandex-team.ru/balance/docs/process/oebs/#gruppirovkastrokaktovprivygruzke

    Посмотреть в коде какие атрибуты, как и куда выгружаются можно тут balance/processors/oebs/__init__.py

    Задачи по которым расширялись тесты: BALANCE-25193
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


@pytest.mark.parametrize('context, act_attrs, json_file', [
    # firm 1
    (PrepayContext.BANK_UR_RUB, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_RU_WITH_NDS, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
                                 ACT_ATTRS.SHIP_TO_CUSTOMER, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'bank_ur_rub.json'),
    (PrepayContext.BANK_PH_RUB, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_RU_WITH_NDS, ACT_ATTRS.BILL_TO_CONTACT_PH,
                                 ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'bank_ph_rub.json'),
    (PrepayContext.BANK_YT_RUB, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_RU_YT_RUB, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
                                 ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'bank_yt_rub.json'),
    (PrepayContext.BANK_YT_USD, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_RU_YT_USD, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
                                 ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'bank_yt_usd.json'),
    (PrepayContext.BANK_YT_EUR, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_RU_YT, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
                                 ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'bank_yt_eur.json'),
    (PrepayContext.BANK_YTUR_KZT, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_RU_YT, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
                                   ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'bank_ytur_kzt.json'),
    (PrepayContext.BANK_YTPH_KZT, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_RU_YT, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
                                   ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'bank_ytph_kzt.json'),
    # firm 2
    # pytest.mark.skip(reason='UKRAINE WAS TURNED OFF')
    # (PrepayContext.BANK_UA_UR_UAH, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_UA, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
    #                                 ACT_ATTRS.SHIP_TO_CUSTOMER, ACT_ATTRS.ACT_PAYMENT_TERM_DT]),
    # (PrepayContext.BANK_UA_PH_UAH, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_UA, ACT_ATTRS.BILL_TO_CONTACT_PH,
    #                                 ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_PAYMENT_TERM_DT]),
    # firm 4
    (PrepayContext.BANK_US_UR_USD, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_INVOICE, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
                                    ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'bank_us_ur_usd.json'),
    (PrepayContext.BANK_US_PH_USD, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_INVOICE, ACT_ATTRS.BILL_TO_CONTACT_PH,
                                    ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'bank_us_ph_usd.json'),
    # firm 7
    (PrepayContext.BANK_SW_UR_EUR, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_INVOICE, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
                                    ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'bank_sw_ur_eur.json'),
    (PrepayContext.BANK_SW_PH_EUR, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_INVOICE, ACT_ATTRS.BILL_TO_CONTACT_PH,
                                    ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'bank_sw_ph_eur.json'),
    (PrepayContext.BANK_SW_YT_EUR, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_INVOICE, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
                                    ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'bank_sw_yt_eur.json'),
    (PrepayContext.BANK_SW_YTPH_EUR, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_INVOICE, ACT_ATTRS.BILL_TO_CONTACT_PH,
                                      ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'bank_sw_ytph_eur.json'),
    (PrepayContext.CC_BY_YTPH_RUB, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_INVOICE, ACT_ATTRS.BILL_TO_CONTACT_PH,
                                    ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'cc_by_ytph_rub.json'),
    # firm 8 BALANCE-35586
    # (PrepayContext.BANK_TR_UR_TRY, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_INVOICE, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
    #                                 ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'bank_tr_ur_try.json'),
    # (PrepayContext.BANK_TR_PH_TRY, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_INVOICE, ACT_ATTRS.BILL_TO_CONTACT_PH,
    #                                 ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'bank_tr_ph_try.json'),
    # firm 12
    (PrepayContext.BANK_PH_RUB_VERTICAL, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_RU_WITH_NDS, ACT_ATTRS.BILL_TO_CONTACT_PH,
                                          ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'bank_ph_rub_vertical.json'),
    # firm 25
    (PrepayContext.KZ_UR, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_INVOICE, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
                           ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'bank_kz_ur.json'),
], ids=lambda context, act_attrs, json_file: "{context_name}-{paysys_id}".format(context_name=PrepayContext.name(context),
                                                                      paysys_id=context.paysys.id))
def test_export_act_prepay(context, act_attrs, json_file):
    client_id, person_id, invoice_id, act_id = create_act_on_prepay_invoice(context)

    if balance_config.JSON_CONTRACT_OEBS and json_file:
        check_json_contract(act_id, json_file)

    else:
        steps.ExportSteps.export_oebs(client_id=client_id,
                                      person_id=person_id,
                                      invoice_id=invoice_id,
                                      act_id=act_id)

        act_rows_attrs = [ACT_ROW_ATTRS.ACT_ROW, ACT_ROW_ATTRS.PRINT_DOCS_COMMON]
        check_attrs(act_id, act_attrs, act_rows_attrs, merge_balance_act_rows=True)


# Проверяем выгрузку типа акта для нерезидента с НДС (BALANCE-25193)
@pytest.mark.parametrize('context, act_attrs, json_file', [
    (PrepayContext.BANK_YT_RUB_WITH_NDS, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_RU_WITH_NDS,
                                          ACT_ATTRS.BILL_TO_CONTACT_NON_PH, ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY,
                                          ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'act_bank_yt_rub_with_nds.json'),
], ids=lambda context, act_attrs, json_file: "{context_name}-{paysys_id}".format(context_name=PrepayContext.name(context),
                                                                      paysys_id=context.paysys.id))
def test_export_act_yt_rub_with_nds(context, act_attrs, json_file):
    # выставляемся на adfox, а у них цена зависит от количества,
    # поэтому чтобы строки акта сгруппировались выставляемся на одинаковое количество
    qty_list = [3000000, 3000000]
    client_id, person_id, invoice_id, act_id = create_act_on_prepay_invoice(context, qty_list=qty_list)

    if balance_config.JSON_CONTRACT_OEBS and json_file:
        check_json_contract(act_id, json_file)

    else:
        steps.ExportSteps.export_oebs(client_id=client_id,
                                      person_id=person_id,
                                      invoice_id=invoice_id,
                                      act_id=act_id)

        act_rows_attrs = [ACT_ROW_ATTRS.ACT_ROW, ACT_ROW_ATTRS.PRINT_DOCS_COMMON]
        check_attrs(act_id, act_attrs, act_rows_attrs, merge_balance_act_rows=True)


# Проверяем выгрузку типа акта для резидента без НДС (BALANCE-25193)
@pytest.mark.parametrize('context, act_attrs, json_file', [
    (PrepayContext.BANK_UR_RUB_WO_NDS, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_RU_WO_NDS, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
                                        ACT_ATTRS.SHIP_TO_CUSTOMER, ACT_ATTRS.ACT_PAYMENT_TERM_DT], 'act_resident_wo_nds_bank_ur_rub.json'),
], ids=lambda context, act_attrs, json_file: "{context_name}-{paysys_id}".format(context_name=PrepayContext.name(context),
                                                                      paysys_id=context.paysys.id))
def test_export_act_resident_wo_nds(context, act_attrs, json_file):
    client_id, person_id, invoice_id, act_id = create_act_on_prepay_speechkit_invoice(context)

    if balance_config.JSON_CONTRACT_OEBS and json_file:
        check_json_contract(act_id, json_file)

    else:
        steps.ExportSteps.export_oebs(client_id=client_id,
                                      person_id=person_id,
                                      invoice_id=invoice_id,
                                      act_id=act_id)

        act_rows_attrs = [ACT_ROW_ATTRS.ACT_ROW, ACT_ROW_ATTRS.PRINT_DOCS_COMMON]
        check_attrs(act_id, act_attrs, act_rows_attrs, merge_balance_act_rows=True)


@pytest.mark.parametrize('context, act_attrs, json_file', [
    # firm 1
    (PrepayContext.BANK_UR_RUB, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_RU_WITH_NDS, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
                                 ACT_ATTRS.SHIP_TO_CUSTOMER, ACT_ATTRS.ACT_POSTPAY_PAYMENT_TERM_DT], 'act_overdraft_bank_ur_rub.json'),
], ids=lambda context, act_attrs, json_file: "{context_name}-{paysys_id}".format(context_name=PrepayContext.name(context),
                                                                      paysys_id=context.paysys.id))
def test_export_act_overdraft(context, act_attrs, json_file):
    client_id, person_id, invoice_id, act_id = create_act_on_overdraft_invoice(context)

    if balance_config.JSON_CONTRACT_OEBS and json_file:
        check_json_contract(act_id, json_file)

    else:
        steps.ExportSteps.export_oebs(client_id=client_id,
                                      person_id=person_id,
                                      invoice_id=invoice_id,
                                      act_id=act_id)

        act_rows_attrs = [ACT_ROW_ATTRS.ACT_ROW, ACT_ROW_ATTRS.PRINT_DOCS_COMMON]
        check_attrs(act_id, act_attrs, act_rows_attrs, merge_balance_act_rows=True)


@pytest.mark.parametrize('context, act_attrs, json_file', [
    # firm 1
    pytest.mark.smoke((PostpayContext.BANK_UR_RUB, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_RU_WITH_NDS,
                                                    ACT_ATTRS.BILL_TO_CONTACT_NON_PH, ACT_ATTRS.SHIP_TO_CUSTOMER,
                                                    ACT_ATTRS.ACT_POSTPAY_PAYMENT_TERM_DT], 'act_postpay_bank_ur_rub.json')),
], ids=lambda context, act_attrs, json_file: "{context_name}-{paysys_id}".format(context_name=PostpayContext.name(context),
                                                                      paysys_id=context.paysys.id))
def test_export_act_postpay(context, act_attrs, json_file):
    client_id, person_id, contract_id, repayment_invoice_id, act_id = create_act_on_postpay_invoice(context)

    if balance_config.JSON_CONTRACT_OEBS and json_file:
        check_json_contract(act_id, json_file)

    else:
        steps.ExportSteps.export_oebs(client_id=client_id,
                                      person_id=person_id,
                                      contract_id=contract_id,
                                      invoice_id=repayment_invoice_id,
                                      act_id=act_id)

        act_rows_attrs = [ACT_ROW_ATTRS.ACT_ROW, ACT_ROW_ATTRS.PRINT_DOCS_COMMON]
        check_attrs(act_id, act_attrs, act_rows_attrs, merge_balance_act_rows=True)


@pytest.mark.parametrize('context, act_attrs, json_file', [
    # firm 4
    (PostpayContext.BANK_US_UR_USD, [ACT_ATTRS.ACT, ACT_ATTRS.ACT_TYPE_INVOICE_S, ACT_ATTRS.BILL_TO_CONTACT_NON_PH,
                                     ACT_ATTRS.SHIP_TO_CUSTOMER_EMPTY, ACT_ATTRS.ACT_POSTPAY_PAYMENT_TERM_DT],
     'act_personal_bank_us_ur_usd.json'),
], ids=lambda context, act_attrs, json_file: "{context_name}-{paysys_id}".format(context_name=PostpayContext.name(context),
                                                                      paysys_id=context.paysys.id))
def test_export_act_personal(context, act_attrs, json_file):
    client_id, person_id, contract_id, invoice_id, act_id = create_act_on_personal_invoice(context)

    if balance_config.JSON_CONTRACT_OEBS and json_file:
        check_json_contract(act_id, json_file)

    else:
        steps.ExportSteps.export_oebs(client_id=client_id,
                                      person_id=person_id,
                                      contract_id=contract_id,
                                      invoice_id=invoice_id,
                                      act_id=act_id)

        act_rows_attrs = [ACT_ROW_ATTRS.ACT_ROW, ACT_ROW_ATTRS.PRINT_DOCS_COMMON]
        check_attrs(act_id, act_attrs, act_rows_attrs, merge_balance_act_rows=True)
