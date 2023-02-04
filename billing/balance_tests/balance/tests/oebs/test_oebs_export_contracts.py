# -*- coding: utf-8 -*-

import xmlrpclib
import datetime
import os

import pytest
from enum import Enum
from hamcrest import contains_string

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils as utils
from btestlib.constants import ContractCommissionType, ContractPaymentType, \
    ContractSubtype, ContractCreditType, Firms, Services
from btestlib.data.defaults import ContractDefaults
from btestlib.data.person_defaults import InnType
from btestlib.matchers import has_entries, equal_to_casted_dict, contains_dicts_equal_to
from export_commons import Locators, get_oebs_client_party_id, \
    get_oebs_person_cust_account_id, get_oebs_manager_person_id, read_attr_values, read_attr_values_list, \
    get_balance_firm_oebs_org_id, get_balance_currency_iso_code, create_contract, create_contract_rsya
from temp.igogor.balance_objects import Contexts
import balance.balance_api as api
from btestlib.data.partner_contexts import TAXI_RU_CONTEXT_SPENDABLE, TAXI_BV_GEO_USD_CONTEXT_SPENDABLE, \
    TAXI_BV_GEO_USD_CONTEXT, CORP_TAXI_RU_CONTEXT_SPENDABLE_MIGRATED, TELEMEDICINE_SPENDABLE_CONTEXT, BLUE_MARKET_SUBSIDY, \
    ZEN_SPENDABLE_CONTEXT, ZEN_SPENDABLE_SERVICES_AG_CONTEXT, TELEMEDICINE_CONTEXT, TAXI_RU_CONTEXT_CLONE
import btestlib.config as balance_config


pytestmark = [reporter.feature(Features.OEBS, Features.CONTRACT),
              pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/oebs')]

try:
    import balance_contracts
    from balance_contracts.oebs.contract import replace_mask
    from balance_contracts.contract_utils import utils as contract_utils
    from balance_contracts.contract_utils import deep_equals
    json_contracts_repo_path = os.path.dirname(os.path.abspath(balance_contracts.__file__))
except ImportError as err:
    json_contracts_repo_path = ''

JSON_OEBS_PATH = '/oebs/contract/'

'''
Посмотреть в коде какие атрибуты, как и куда выгружаются можно тут balance/processors/oebs/__init__.py
'''

TODAY = utils.Date.nullify_time_of_date(datetime.datetime.now())
TODAY_ISO = utils.Date.date_to_iso_format(TODAY)
WEEK_AFTER_ISO = utils.Date.date_to_iso_format(TODAY + datetime.timedelta(days=7))
TWO_WEEKS_AFTER_ISO = utils.Date.date_to_iso_format(TODAY + datetime.timedelta(days=14))


# атрибуты для проверки выгрузки договора по старой схеме
class ContractAttrs(Enum):
    dt = Locators(
        balance=lambda b: b['t_contract_collateral.dt'],
        oebs=lambda o: o['okc_k_headers_b.start_date'])

    client_party_id = Locators(
        balance=lambda b: get_oebs_client_party_id(b['t_contract_attributes.firm.value_num'],
                                                   b['t_contract2.client_id']),
        oebs=lambda o: o['okc_k_party_roles_v.customer.object1_id1'])

    person_cust_account_id = Locators(
        balance=lambda b: get_oebs_person_cust_account_id(b['t_contract_attributes.firm.value_num'],
                                                          b['t_contract2.person_id']),
        oebs=lambda o: o['okc_k_party_roles_v.paid_by.object1_id1'])

    manager_person_id = Locators(
        balance=lambda b: get_oebs_manager_person_id(b['t_contract_attributes.firm.value_num'],
                                                     b['t_contract_attributes.manager_code.value_num']),
        oebs=lambda o: o['pa_project_parties.1000.resource_source_id'])

    currency = Locators(
        balance=lambda b: get_balance_currency_iso_code(num_code=b['t_contract_attributes.currency.value_num']),
        oebs=lambda o: o['okc_k_headers_b.currency_code'])

    # последний день действия коммерческого договора = дата окончания со страницы договора - 1 день
    finish_dt = Locators(
        balance=lambda b: utils.Date.shift_date(b['t_contract_attributes.finish_dt.value_dt'], days=-1),
        oebs=lambda o: o['okc_k_headers_b.end_date'])

    manager_bo_person_id = Locators(
        balance=lambda b: get_oebs_manager_person_id(b['t_contract_attributes.firm.value_num'],
                                                     b['t_contract_attributes.manager_bo_code.value_num']),
        oebs=lambda o: o['pa_project_parties.2000.resource_source_id'])


class PartnerContractAttrs(Enum):
    dt = ContractAttrs.dt.value

    client_party_id = ContractAttrs.client_party_id.value

    person_cust_account_id = ContractAttrs.person_cust_account_id.value

    manager_person_id = ContractAttrs.manager_person_id.value

    currency = Locators(
        balance=lambda b: get_balance_currency_iso_code(
            iso_num_code=b['t_contract_attributes.currency.value_num']),
        oebs=lambda o: o['okc_k_headers_b.currency_code'])

    # последний день действия партнерского договора = дата окончания со страницы договора
    end_dt = Locators(
        balance=lambda b: b['t_contract_attributes.end_dt.value_dt'],
        oebs=lambda o: o['okc_k_headers_b.end_date'])


class SpendableContractAttrs(Enum):
    dt = ContractAttrs.dt.value

    client_party_id = ContractAttrs.client_party_id.value

    person_cust_account_id = ContractAttrs.person_cust_account_id.value

    manager_person_id = ContractAttrs.manager_person_id.value

    currency = Locators(
        balance=lambda b: get_balance_currency_iso_code(
            iso_num_code=b['t_contract_attributes.currency.value_num']),
        oebs=lambda o: o['okc_k_headers_b.currency_code'])


# атрибуты для проверки выгрузки доп.соглашений, в том числе нулевого
# (проверка выгрузки нулевого дс = проверке выгрузки договора по новой схеме)
class CollateralAttrs(Enum):
    num = Locators(
        balance=lambda b: b['t_contract_collateral.num'] or 0,
        oebs=lambda o: o['xxoke_contract_find_v.major_version'] or 0)

    # тип договора
    commission = Locators(
        # todo-blubimov подумать как лучше. так или вынести константы в параметризацию
        # кмк это можно было бы вынести в параметризацию, если бы было ограниченное количество типов
        # (для россии - 1, для украины - 2 итп), но так как здесь придется для каждого типа договора
        # задавать новый параметр с константой - это как-то не очень
        # Еще как вариант: можно оставить метод как сейчас, но убрать оттуда сложную логику
        # и оставить только маппинг того что сейчас проверяется
        balance=lambda b: oebs_commission_type(b),
        oebs=lambda o: o['xxoke_dop_terms.xxoke_comission.term_value'])

    memo = Locators(
        balance=lambda b: b['t_contract_attributes.memo.value_clob'],
        oebs=lambda o: o['xxoke_dop_terms.xxoke_description.term_value_string'])

    # последний день действия коммерческого договора = дата окончания со страницы договора - 1 день
    finish_dt = Locators(
        balance=lambda b: utils.Date.shift_date(b['t_contract_attributes.finish_dt.value_dt'], days=-1),
        oebs=lambda o: o['xxoke_dop_terms.xxoke_end_date.term_value_date'])

    payment_type = Locators(
        balance=lambda b: b['t_contract_attributes.payment_type.value_num'],
        oebs=lambda o: o['xxoke_dop_terms.xxoke_payment_type.term_value'])

    services = Locators(
        balance=lambda b: b['t_contract_attributes.services.key_num'],
        oebs=lambda o: o['xxoke_dop_terms.xxoke_services.term_value'])

    # односторонний / двусторонний
    unilateral = Locators(
        balance=lambda b: b['t_contract_attributes.unilateral.value_num'] or 2,
        oebs=lambda o: o['xxoke_dop_terms.xxoke_sides.term_value'])

    credit_type = Locators(
        balance=lambda b: b['t_contract_attributes.credit_type.value_num'],
        oebs=lambda o: o['xxoke_dop_terms.xxoke_credit.term_value'])

    # срок кредита
    payment_term = Locators(
        balance=lambda b: b['t_contract_attributes.payment_term.value_num'],
        oebs=lambda o: o['xxoke_dop_terms.xxoke_credit_term.term_value_number'])

    credit_limit_single = Locators(
        balance=lambda b: b['t_contract_attributes.credit_limit_single.value_num'],
        oebs=lambda o: o['xxoke_dop_terms.xxoke_credit_amount.term_value_number'])


class PartnerCollateralAttrs(Enum):
    num = CollateralAttrs.num.value

    contract_type = CollateralAttrs.commission.value

    memo = CollateralAttrs.memo.value

    # последний день действия партнерского договора = дата окончания со страницы договора
    end_dt = Locators(
        balance=lambda b: b['t_contract_attributes.end_dt.value_dt'],
        oebs=lambda o: o['xxoke_dop_terms.xxoke_end_date.term_value_date'])

    open_date = Locators(
        balance=lambda b: b['t_contract_attributes.open_date.value_num'],
        oebs=lambda o: o['xxoke_dop_terms.xxoke_reserved_flag.term_value'])

    # односторонний / двусторонний
    unilateral = Locators(
        balance=lambda b: b['t_contract_attributes.unilateral_acts.value_num'] or 2,
        oebs=lambda o: o['xxoke_dop_terms.xxoke_sides.term_value'])

    # период актов
    payment_type = Locators(
        balance=lambda b: b['t_contract_attributes.payment_type.value_num'],
        oebs=lambda o: o['xxoke_dop_terms.xxoke_period_report.term_value'])

    # поисковые формы
    search_forms = Locators(
        balance=lambda b: b['t_contract_attributes.search_forms.value_num'],
        oebs=lambda o: {'-1': '0'}.get(o['xxoke_dop_terms.xxoke_search.term_value'],
                                       o['xxoke_dop_terms.xxoke_search.term_value']))

    # тип выплат
    reward_type = Locators(
        balance=lambda b: b['t_contract_attributes.reward_type.value_num'],
        oebs=lambda o: o['xxoke_dop_terms.xxoke_pay_receive_type.term_value'])

    # % партнера
    partner_pct = Locators(
        balance=lambda b: b['t_contract_attributes.partner_pct.value_num'],
        oebs=lambda o: o['xxoke_dop_terms.xxoke_partner_percent.term_value_number'])

    # ставка НДС (название атрибута в оебс: Вознаграждение не облагается НДС)
    without_nds = Locators(
        balance=lambda b: 1 if b['t_contract_attributes.nds.value_num'] == 0 else 0,
        oebs=lambda o: o['xxoke_dop_terms.xxoke_nds.term_value'])


class SpendableCollateralAttrs(Enum):
    num = CollateralAttrs.num.value

    contract_type = Locators(
        balance=lambda b: steps.PartnerSteps.get_spendable_contract_type_by_service_id(b['t_contract_attributes.services.key_num']),
        oebs=lambda o: o['xxoke_dop_terms.xxoke_comission.term_value'])

    memo = CollateralAttrs.memo.value

    # последний день действия партнерского договора = дата окончания со страницы договора
    end_dt = Locators(
        balance=lambda b: b['t_contract_attributes.end_dt.value_dt'],
        oebs=lambda o: o['xxoke_dop_terms.xxoke_end_date.term_value_date'])

    # период актов
    payment_type = Locators(
        balance=lambda b: b['t_contract_attributes.payment_type.value_num'],
        oebs=lambda o: o['xxoke_dop_terms.xxoke_period_report.term_value'])

    # ставка НДС (название атрибута в оебс: Вознаграждение не облагается НДС)
    without_nds = Locators(
        balance=lambda b: 1 if b['t_contract_attributes.nds.value_num'] == 0 else 0,
        oebs=lambda o: o['xxoke_dop_terms.xxoke_nds.term_value'])

    # регион
    region = Locators(
        balance=lambda b: b['t_contract_attributes.region.value_str'],
        oebs=lambda o: o['xxoke_dop_terms.xxoke_region.term_value_string'])

    # страна
    country = Locators(
        balance=lambda b: get_country_iso_code(b['t_contract_attributes.country.value_num']),
        oebs=lambda o: o['xxoke_dop_terms.xxoke_country_iso_code.term_value_string'])


class ContractAttrsByType(object):
    COMMERCIAL = {ContractAttrs}
    PARTNER = {PartnerContractAttrs}
    SPENDABLE = {SpendableContractAttrs}


class CollateralAttrsByType(object):
    # просто группы для объединения атрибутов
    _PREPAY = {
        CollateralAttrs.num,
        CollateralAttrs.commission,
        CollateralAttrs.memo,
        CollateralAttrs.finish_dt,
        CollateralAttrs.payment_type,
        CollateralAttrs.services,
        CollateralAttrs.unilateral,
    }
    _POSTPAY = {
        CollateralAttrs.credit_type,
        CollateralAttrs.payment_term,
    }

    # атрибуты по типам допсоглашений

    ZERO_PREPAY = _PREPAY
    ZERO_POSTPAY_BY_TERM = set.union(_PREPAY, _POSTPAY)
    ZERO_POSTPAY_BY_TERM_AND_SUM = set.union(_PREPAY, _POSTPAY, {CollateralAttrs.credit_limit_single})

    PROLONGATION = {
        CollateralAttrs.num,
        CollateralAttrs.memo,
        CollateralAttrs.finish_dt,
    }

    ZERO_PARTNER = {
        PartnerCollateralAttrs.num,
        PartnerCollateralAttrs.contract_type,
        PartnerCollateralAttrs.memo,
        PartnerCollateralAttrs.end_dt,
        PartnerCollateralAttrs.open_date,
        PartnerCollateralAttrs.unilateral,
        PartnerCollateralAttrs.payment_type,
        PartnerCollateralAttrs.search_forms,
        PartnerCollateralAttrs.reward_type,
        PartnerCollateralAttrs.partner_pct,
        PartnerCollateralAttrs.without_nds,
    }

    ZERO_SPENDABLE = {
        SpendableCollateralAttrs.num,
        SpendableCollateralAttrs.contract_type,
        SpendableCollateralAttrs.memo,
        SpendableCollateralAttrs.payment_type,
        SpendableCollateralAttrs.without_nds,
    }

    SPENDABLE_TAXI_LLC_DONATE = set.union(ZERO_SPENDABLE,
                                          {SpendableCollateralAttrs.country, SpendableCollateralAttrs.region})
    SPENDABLE_TAXI_BV_DONATE = set.union(ZERO_SPENDABLE, {SpendableCollateralAttrs.country})


def check_json_contract(contract_id, person_id, json_file, linked_contract_id=None):

    if person_id:
        try:
            db.balance().execute(
                """update t_person_firm set oebs_export_dt = sysdate where person_id = :person_id""",
                {'person_id': person_id})
        except Exception:
            pass

    if linked_contract_id:
        steps.ExportSteps.set_fictive_export_status('Contract', linked_contract_id)

    steps.ExportSteps.init_oebs_api_export('Contract', contract_id)
    actual_json_data = steps.ExportSteps.get_json_data('Contract', contract_id)

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


@pytest.mark.parametrize('context, collateral_attrs, json_file', [
    # firm 1
    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(name='OPT_CLIENT_PREPAY',
                                          contract_type=ContractCommissionType.OPT_CLIENT,
                                          contract_params={
                                              'PAYMENT_TYPE': ContractPaymentType.PREPAY,
                                          }),
     CollateralAttrsByType.ZERO_PREPAY,
     'opt_client_prepay.json'),

    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(name='OPT_CLIENT_POSTPAY_BY_TERM',
                                          contract_type=ContractCommissionType.OPT_CLIENT,
                                          contract_params={
                                              'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                                              'CREDIT_TYPE': ContractCreditType.BY_TERM,
                                          }),
     CollateralAttrsByType.ZERO_POSTPAY_BY_TERM,
     'opt_client_postpay_by_term.json'),

    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(name='OPT_CLIENT_POSTPAY_BY_TERM_AND_SUM',
                                          contract_type=ContractCommissionType.OPT_CLIENT,
                                          contract_params={
                                              'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                                              'CREDIT_TYPE': ContractCreditType.BY_TERM_AND_SUM,
                                          }),
     CollateralAttrsByType.ZERO_POSTPAY_BY_TERM_AND_SUM,
     'opt_client_postpay_by_term_and_sum.json'),

    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(name='NO_AGENT_POSTPAY',
                                          contract_type=ContractCommissionType.NO_AGENCY,
                                          contract_params={
                                              'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                                              'CREDIT_TYPE': ContractCreditType.BY_TERM_AND_SUM,
                                          }),
     CollateralAttrsByType.ZERO_POSTPAY_BY_TERM_AND_SUM,
     'no_agent_postpay.json'),

    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(name='COMMISS_POSTPAY',
                                          contract_type=ContractCommissionType.COMMISS,
                                          client_params={'IS_AGENCY': 1},
                                          contract_params={
                                              'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                                              'CREDIT_TYPE': ContractCreditType.BY_TERM_AND_SUM,
                                          }),
     CollateralAttrsByType.ZERO_POSTPAY_BY_TERM_AND_SUM,
     'commiss_postpay.json'),

    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(name='DIRECT_AGENT_POSTPAY',
                                          contract_type=ContractCommissionType.PR_AGENCY,
                                          client_params={'IS_AGENCY': 1},
                                          contract_params={
                                              'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                                              'CREDIT_TYPE': ContractCreditType.BY_TERM_AND_SUM,
                                          }),
     CollateralAttrsByType.ZERO_POSTPAY_BY_TERM_AND_SUM,
     'direct_agent_postpay.json'),

    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(name='OPT_AGENT_POSTPAY',
                                          contract_type=ContractCommissionType.OPT_AGENCY,
                                          client_params={'IS_AGENCY': 1},
                                          contract_params={
                                              'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                                              'CREDIT_TYPE': ContractCreditType.BY_TERM_AND_SUM,
                                          }),
     CollateralAttrsByType.ZERO_POSTPAY_BY_TERM_AND_SUM,
     'opt_agent_postpay.json'),

    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(name='PARTNER_POSTPAY',
                                          contract_type=ContractCommissionType.PARTNER,
                                          client_params={'IS_AGENCY': 1},
                                          contract_params={
                                              'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                                              'CREDIT_TYPE': ContractCreditType.BY_TERM_AND_SUM,
                                          }),
     CollateralAttrsByType.ZERO_POSTPAY_BY_TERM_AND_SUM,
     'partner_postpay.json'),

    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(name='POSTPAY_UNSIGNED',
                                          contract_type=ContractCommissionType.PR_AGENCY,
                                          client_params={'IS_AGENCY': 1},
                                          contract_params={
                                              'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                                              'CREDIT_TYPE': ContractCreditType.BY_TERM_AND_SUM,
                                              'IS_FAXED': None,
                                              'IS_SIGNED': None,
                                          }),
     CollateralAttrsByType.ZERO_POSTPAY_BY_TERM_AND_SUM,
     'postpay_unsigned.json'),

    # pytest.mark.skip(reason='UKRAINE WAS TURNED OFF')

    # firm 4
    (Contexts.DIRECT_FISH_USD_CONTEXT.new(name='USA_OPT_CLIENT_PERSONAL',
                                          contract_type=ContractCommissionType.USA_OPT_CLIENT,
                                          contract_params={
                                              'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                                              'CREDIT_TYPE': ContractCreditType.BY_TERM_AND_SUM,
                                          }),
     CollateralAttrsByType.ZERO_POSTPAY_BY_TERM_AND_SUM,
     'usa_opt_client_personal.json'),

    # firm 7
    (Contexts.DIRECT_FISH_SW_EUR_CONTEXT.new(name='SW_OPT_AGENT_POSTPAY',
                                             contract_type=ContractCommissionType.SW_OPT_AGENCY,
                                             client_params={'IS_AGENCY': 1},
                                             contract_params={
                                                 'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                                                 'CREDIT_TYPE': ContractCreditType.BY_TERM_AND_SUM,
                                                 'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 0
                                             }),
     CollateralAttrsByType.ZERO_POSTPAY_BY_TERM_AND_SUM,
     'sw_opt_agent_postpay.json'),

    # firm 8
    # (Contexts.DIRECT_FISH_TRY_CONTEXT.new(name='TR_OPT_AGENT_POSTPAY',
    #                                       contract_type=ContractCommissionType.TR_OPT_AGENCY,
    #                                       client_params={'IS_AGENCY': 1},
    #                                       contract_params={
    #                                           'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
    #                                           'CREDIT_TYPE': ContractCreditType.BY_TERM_AND_SUM,
    #                                       }),
    #  CollateralAttrsByType.ZERO_POSTPAY_BY_TERM_AND_SUM,
    #  'tr_opt_agent_postpay.json'),

    # firm 12
    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(name='AUTO_OPT_AGENT_POSTPAY',
                                          contract_type=ContractCommissionType.AUTO_OPT_AGENCY_PREM,
                                          client_params={'IS_AGENCY': 1},
                                          contract_params={
                                              'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                                              'CREDIT_TYPE': ContractCreditType.BY_TERM_AND_SUM,
                                          }),
     CollateralAttrsByType.ZERO_POSTPAY_BY_TERM_AND_SUM,
     'auto_opt_agent_postpay.json'),

    # firm 25
    (Contexts.DIRECT_FISH_KZ_CONTEXT.new(name='KZ_OPT_AGENT_POSTPAY',
                                         contract_type=ContractCommissionType.KZ_OPT_AGENCY,
                                         client_params={'IS_AGENCY': 0},
                                         contract_params={
                                             'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                                             'CREDIT_TYPE': ContractCreditType.BY_TERM_AND_SUM,
                                         }),
     CollateralAttrsByType.ZERO_POSTPAY_BY_TERM_AND_SUM,
     'kz_opt_agent_postpay.json'),

    (Contexts.DIRECT_BYN_BYU_CONTEXT.new(name='BEL_PR_AGENCY',
                                         contract_type=ContractCommissionType.BEL_PR_AGENCY,
                                         client_params={'IS_AGENCY': 1},
                                         contract_params={
                                             'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                                             'CREDIT_TYPE': ContractCreditType.BY_TERM_AND_SUM,
                                         }),
     CollateralAttrsByType.ZERO_POSTPAY_BY_TERM_AND_SUM,
     'bel_pr_agency.json'),

    (Contexts.DIRECT_BYN_BYU_CONTEXT.new(name='BEL_OPT_AGENCY',
                                         contract_type=ContractCommissionType.BEL_OPT_AGENCY,
                                         client_params={'IS_AGENCY': 1},
                                         contract_params={
                                             'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                                             'CREDIT_TYPE': ContractCreditType.BY_TERM_AND_SUM,

                                         }),
     CollateralAttrsByType.ZERO_POSTPAY_BY_TERM_AND_SUM,
     'bel_opt_agency.json')
], ids=lambda context, collateral_attrs, json_file: context.name)
def test_export_contract(context, collateral_attrs, json_file):
    client_id, person_id, contract_id = create_base_contract(context)

    if balance_config.JSON_CONTRACT_OEBS and json_file:
        check_json_contract(contract_id, person_id, json_file)

    else:
        steps.ExportSteps.export_oebs(client_id=client_id,
                                      person_id=person_id,
                                      contract_id=contract_id)

        contract_attrs = ContractAttrsByType.COMMERCIAL
        check_contract_attrs(contract_id, contract_attrs, collateral_attrs)


@pytest.mark.parametrize('context, collateral_attrs, json_file', [
    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(name='RSYA_UNIVERSAL',
                                          contract_template='rsya_universal'),
     CollateralAttrsByType.ZERO_PARTNER, 'rsya_universal_zero_partner.json'),
], ids=lambda context, collateral_attrs, json_file: context.name)
def test_export_contract_rsya(context, collateral_attrs, json_file):
    client_id, person_id, contract_id = create_rsya_contract(context)

    if balance_config.JSON_CONTRACT_OEBS and json_file:
        check_json_contract(contract_id, person_id, json_file)

    else:
        steps.ExportSteps.export_oebs(client_id=client_id,
                                      person_id=person_id,
                                      contract_id=contract_id)

        contract_attrs = ContractAttrsByType.PARTNER
        check_contract_attrs(contract_id, contract_attrs, collateral_attrs)


@pytest.mark.parametrize('context, collateral_attrs, json_file', [
    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(name='PROLONGATION',
                                          contract_type=ContractCommissionType.OPT_CLIENT,
                                          contract_params={
                                              'PAYMENT_TYPE': ContractPaymentType.PREPAY,
                                              'FINISH_DT': WEEK_AFTER_ISO
                                          },
                                          collateral_type_id=80,
                                          collateral_params={
                                              'FINISH_DT': TWO_WEEKS_AFTER_ISO
                                          }),
     CollateralAttrsByType.PROLONGATION,
     'collateral_prolongation.json'),
], ids=lambda context, collateral_attrs, json_file: context.name)
def test_export_collateral(context, collateral_attrs, json_file):
    client_id, person_id, contract_id, collateral_id = create_exported_collateral(context)

    if balance_config.JSON_CONTRACT_OEBS and json_file:
        check_json_contract(contract_id, person_id, json_file)

    else:
        check_collateral_attrs(contract_id, collateral_id, collateral_attrs)


@pytest.mark.parametrize('context_spendable, context_for_link, collateral_attrs, additional_service, json_file',
                         [
                             (ZEN_SPENDABLE_CONTEXT, None, CollateralAttrsByType.ZERO_SPENDABLE, None, 'zen_spendable.json'),
                             (ZEN_SPENDABLE_SERVICES_AG_CONTEXT, None, CollateralAttrsByType.ZERO_SPENDABLE, None, 'zen_spendable_services_ag_none_for_link.json'),
                             (ZEN_SPENDABLE_SERVICES_AG_CONTEXT, ZEN_SPENDABLE_CONTEXT, CollateralAttrsByType.ZERO_SPENDABLE, None, 'zen_spendable_services_ag_zen_for_link.json'),
                             (TAXI_RU_CONTEXT_SPENDABLE, TAXI_RU_CONTEXT_CLONE, CollateralAttrsByType.SPENDABLE_TAXI_LLC_DONATE, None, 'taxi_ru_spandable_llc_donate.json'),
                             (TAXI_RU_CONTEXT_SPENDABLE, TAXI_RU_CONTEXT_CLONE, CollateralAttrsByType.SPENDABLE_TAXI_LLC_DONATE, Services.SCOUTS.id, 'taxi_ru_spandable_llc_donate_with_scouts.json'),
                             (TAXI_BV_GEO_USD_CONTEXT_SPENDABLE, TAXI_BV_GEO_USD_CONTEXT, CollateralAttrsByType.SPENDABLE_TAXI_BV_DONATE, None, 'taxi_bv_geo_usd_spendable_bv_donate.json'),
                             (CORP_TAXI_RU_CONTEXT_SPENDABLE_MIGRATED, TAXI_RU_CONTEXT_CLONE, CollateralAttrsByType.SPENDABLE_TAXI_LLC_DONATE, None, 'corp_taxi_ru_spandable_migrated_llc_donate.json'),
                             (TELEMEDICINE_SPENDABLE_CONTEXT, TELEMEDICINE_CONTEXT, CollateralAttrsByType.ZERO_SPENDABLE, None, 'telemedicine_spendable.json'),
                             (BLUE_MARKET_SUBSIDY, None, CollateralAttrsByType.ZERO_SPENDABLE, None, 'blue_market_subsidy_none_for_link.json')
                         ],
                         ids=[
                             "ZEN_134_ZEN_PLATFORM",
                             "ZEN_134_SERVICES_AG",
                             "ZEN_134_SERVICES_AG with link to ZEN_134_PLATFORM",
                             "SPENDABLE_137_TAXILLC",
                             "SPENDABLE_137_619_TAXILLC",
                             "SPENDABLE_137_TAXIBV",
                             "SPENDABLE_135_TAXILLC",
                             "SPENDABLE_204_TELEMEDICINE",
                             "SPENDABLE_609_BLUE_MARKET"
                         ])
def test_export_contract_spendable(context_spendable, context_for_link, collateral_attrs, additional_service, json_file):
    client_id, person_id, contract_id, linked_contract_id = create_spendable_contract(context_spendable, context_for_link, additional_service=additional_service)

    if balance_config.JSON_CONTRACT_OEBS and json_file:
        check_json_contract(contract_id, person_id, json_file, linked_contract_id)

    else:
        steps.ExportSteps.export_oebs(client_id=client_id,
                                      person_id=person_id,
                                      contract_id=contract_id)

        contract_attrs = ContractAttrsByType.SPENDABLE
        check_contract_attrs(contract_id, contract_attrs, collateral_attrs)


@pytest.mark.tickets('BALANCE-30209')
def test_brand_contract_oebs_export():
    client_id_1 = steps.ClientSteps.create()
    client_id_2 = steps.ClientSteps.create()

    with reporter.step(u'Заключаем договор о техсвязке'):
        brand_contract_id, contract_external_id = steps.ContractSteps.create_brand_contract(
            client_id_1, client_id_2, dt=datetime.datetime.now()
        )

    with pytest.raises(xmlrpclib.Fault) as err_info:
        api.test_balance().ExportObject(
            'OEBS_API', 'Contract', brand_contract_id, 0, None, None, True
        )

    utils.check_that(
        err_info.value.faultString,
        contains_string('Object not found'),
        u'Contract about brand must not automatically be enqueued to OEBS_API queue.'
    )

    utils.check_that(
        api.test_balance().ExportObject('OEBS_API', 'Contract', brand_contract_id),
        has_entries({'state': '1', 'output': 'Contract is not exportable'}),
        u'There must be no errors if we try to export a contract manually.'
    )

# ---------------------------------------------- Utils ----------------------------------------------

def get_balance_data(contract_id):
    balance_contract_data = {}

    # t_contract2
    query = "SELECT * FROM t_contract2 WHERE id = :contract_id"
    result = db.balance().execute(query, {'contract_id': contract_id}, single_row=True)
    balance_contract_data.update(utils.add_key_prefix(result, 't_contract2.'))

    # данные всех доп.соглашений договора
    # balance_collaterals_data - {<collateral_num>: {<attr>: <attr_value>}}
    query = "SELECT attribute_batch_id, num FROM t_contract_collateral WHERE contract2_id = :contract_id"
    result = db.balance().execute(query, {'contract_id': contract_id})
    balance_collaterals_data = {collateral['num'] or 0: get_balance_collateral_data(collateral['attribute_batch_id'])
                                for collateral in result}

    # дополняем данные каждого ДС данными договора из t_contract2
    for collateral in balance_collaterals_data.values():
        collateral.update(balance_contract_data)

    # теперь в нулевом доп.соглашении у нас лежат полные данные по договору
    balance_contract_data = balance_collaterals_data[0]

    return balance_contract_data, balance_collaterals_data


def get_balance_collateral_data(attribute_batch_id):
    balance_collateral_data = {}

    # t_contract_collateral
    query = "SELECT * FROM t_contract_collateral WHERE attribute_batch_id = :attribute_batch_id"
    result = db.balance().execute(query, {'attribute_batch_id': attribute_batch_id}, single_row=True)
    balance_collateral_data.update(utils.add_key_prefix(result, 't_contract_collateral.'))

    # t_contract_attributes
    query = "SELECT * FROM t_contract_attributes WHERE attribute_batch_id = :attribute_batch_id"
    result = db.balance().execute(query, {'attribute_batch_id': attribute_batch_id})
    merge_services_data(result, field_with_attr_name='code', field_with_attr_value='key_num', attr_name='SERVICES')
    for attr in result:
        balance_collateral_data.update(utils.add_key_prefix(attr,
                                                            't_contract_attributes.{}.'.format(attr['code'].lower())))

    return balance_collateral_data


def get_oebs_data(balance_contract_data, balance_collaterals_data):
    balance_contract_id = balance_contract_data['t_contract2.id']
    balance_contract_eid = balance_contract_data['t_contract2.external_id']
    firm_id = balance_collaterals_data[0]['t_contract_attributes.firm.value_num']

    oebs_contract_data = {}

    # получаем внутренний id договора в оебс
    query = u"select k_header_id from apps.oke_k_headers_full_v " \
            "where k_alias = '{balance_contract_eid}' and authoring_org_id = '{org_id}'".format(
        balance_contract_eid=balance_contract_eid, org_id=get_balance_firm_oebs_org_id(firm_id))
    oebs_contract_id = db.oebs().execute_oebs(firm_id, query, single_row=True)['k_header_id']

    # okc_k_headers_b
    query = "SELECT * FROM apps.okc_k_headers_b WHERE id = :oebs_contract_id"
    result = db.oebs().execute_oebs(firm_id, query, {'oebs_contract_id': oebs_contract_id}, single_row=True)
    oebs_contract_data.update(utils.add_key_prefix(result, 'okc_k_headers_b.'))

    # okc_k_party_roles_v
    role_codes = ['CUSTOMER', 'PAID_BY']
    for role_code in role_codes:
        query = "SELECT * FROM apps.okc_k_party_roles_v WHERE chr_id = :oebs_contract_id AND rle_code = :role_code"
        result = db.oebs().execute_oebs(firm_id, query, {'oebs_contract_id': oebs_contract_id,
                                                         'role_code': role_code},
                                        single_row=True)
        oebs_contract_data.update(utils.add_key_prefix(result, 'okc_k_party_roles_v.{}.'.format(role_code.lower())))

    # pa_project_parties
    project_role_ids = [1000, 2000]
    for project_role_id in project_role_ids:
        query = "SELECT * FROM apps.pa_project_parties WHERE object_id = :oebs_contract_id " \
                "AND project_role_id = :project_role_id"
        result = db.oebs().execute_oebs(firm_id, query,
                                        {'oebs_contract_id': oebs_contract_id, 'project_role_id': project_role_id},
                                        single_row=True)
        oebs_contract_data.update(utils.add_key_prefix(result, 'pa_project_parties.{}.'.format(project_role_id)))

    # данные всех доп.соглашений договора
    # oebs_collaterals_data - {<collateral_num>: {<attr>: <attr_value>}}
    query = "SELECT d.reference_id as reference_id, d.major_version as major_version " \
            "FROM okc_k_headers_b h, xxoke_contract_dop d " \
            "WHERE h.id = d.k_header_id " \
            "AND contract_number = :balance_contract_id"
    result = db.oebs().execute_oebs(firm_id, query, {'balance_contract_id': str(balance_contract_id)})
    oebs_collaterals_data = {collateral['major_version'] or 0: get_oebs_collateral_data(balance_contract_eid,
                                                                                        collateral['reference_id'],
                                                                                        firm_id)
                             for collateral in result}

    return oebs_contract_data, oebs_collaterals_data


def get_oebs_collateral_data(balance_contract_eid, balance_collateral_id, firm_id):
    oebs_collateral_data = {}

    # xxoke_contract_find_v
    query = "SELECT d.* " \
            "FROM okc_k_headers_b h, xxoke_contract_dop d " \
            "WHERE h.id = d.k_header_id " \
            "AND reference_id  = :balance_collateral_id"
    result = db.oebs().execute_oebs(firm_id, query, {'balance_collateral_id': balance_collateral_id}, single_row=True)
    oebs_collateral_data.update(utils.add_key_prefix(result, 'xxoke_contract_find_v.'))

    # xxoke_dop_terms
    # hf.k_alias - balance_contract_eid
    # cf.reference_id - balance_collateral_id
    # cf.major_version - balance_collateral_num
    query = u"SELECT hf.k_alias, cda.reference_id, cda.major_version, term.term_name, dt.term_code, dt.term_value,  " \
            "dt.term_value_number, dt.term_value_string, dt.term_value_date, v.term_value AS value_descr  " \
            "FROM apps.xxoke_contract_dop_all cda, " \
            "apps.xxoke_dop_terms dt, " \
            "apps.oke_terms_v term, " \
            "apps.oke_term_values_v v, " \
            "apps.oke_k_headers_full_v hf " \
            "WHERE dt.k_line_id = cda.k_line_id " \
            "AND hf.k_header_id = cda.k_header_id " \
            "AND dt.term_code = term.term_code " \
            "AND dt.term_code = v.term_code " \
            "AND dt.term_value = v.term_value_pk1 " \
            "AND cda.k_line_id = dt.k_line_id " \
            "AND hf.k_alias = :balance_contract_eid " \
            "AND cda.reference_id = :balance_collateral_id " \
            "AND hf.authoring_org_id = :org_id"

    result = db.oebs().execute_oebs(firm_id, query, {'balance_contract_eid': balance_contract_eid,
                                                     'balance_collateral_id': balance_collateral_id,
                                                     'org_id': get_balance_firm_oebs_org_id(firm_id)})
    merge_services_data(result, field_with_attr_name='term_code',
                        field_with_attr_value='term_value',
                        attr_name='XXOKE_SERVICES')
    for attr in result:
        oebs_collateral_data.update(utils.add_key_prefix(attr,
                                                         'xxoke_dop_terms.{}.'.format(attr['term_code'].lower())))
    return oebs_collateral_data


# список строк с сервисами договора заменяем на одну строку со списком сервисов
def merge_services_data(attributes, field_with_attr_name, field_with_attr_value, attr_name):
    services_data = [attr for attr in attributes if attr[field_with_attr_name] == attr_name]
    if services_data:
        merged_services_data = services_data[0]
        merged_services_data[field_with_attr_value] = \
            sorted([str(service[field_with_attr_value]) for service in services_data])
        attributes.append(merged_services_data)
        for service in services_data:
            attributes.remove(service)


# копия balance.processors.oebs.dao.contract.oebs_commission_type
def oebs_commission_type(balance_params):
    commission_type_map = {
        ContractSubtype.GENERAL.name: {
            9: 99, 18: 118, 19: 119, 22: 218, 23: 219,
            40: 1040, 41: 1041, 42: 1042, 43: 1043,
            50: 1050, 27: 61, 70: 61, 14: 90
        },
        ContractSubtype.PARTNERS.name: {1: 8, 2: 10, 3: 12, 4: 14, 6: 30, 7: 40, 8: 1060},
        ContractSubtype.DISTRIBUTION.name: {1: 18, 2: 19},
        ContractSubtype.AFISHA.name: {None: 27},
        ContractSubtype.SPENDABLE.name: {81: 81, 85: 85, 87: 87, 89: 89},
    }

    type = balance_params['t_contract2.type']
    commission = balance_params['t_contract_attributes.commission.value_num'] if type == ContractSubtype.GENERAL.name \
        else balance_params.get('t_contract_attributes.contract_type.value_num', None)
    firm = balance_params['t_contract_attributes.firm.value_num']

    if type == ContractSubtype.GENERAL.name:
        return commission_type_map[type].get(commission, commission)

    if type == ContractSubtype.GEOCONTEXT.name:
        return 20

    if type == ContractSubtype.PARTNERS.name and commission == 5:
        return {Firms.YANDEX_1.id: 25,
                Firms.YANDEX_UA_2.id: 125,
                Firms.EUROPE_AG_7.id: 225}.get(firm)

    if type == ContractSubtype.PARTNERS.name and commission in (6, 7, 8):  # PARTNERS-2014 и Новая оферта РСЯ
        return commission_type_map[type][commission]

    if type == ContractSubtype.DISTRIBUTION.name and commission in (3, 4, 5):
        return {Firms.YANDEX_1.id: {3: 26, 4: 29, 5: 26},
                Firms.EUROPE_AG_7.id: {3: 226, 4: 229, 5: 226},
                Firms.SERVICES_AG_16.id: {3: 226, 4: 229, 5: 226},
                Firms.MARKET_111.id: {3: 26, 4: 29, 5: 26}}.get(firm) \
            .get(commission)

    commission_type = \
        commission_type_map[type][balance_params['t_contract_attributes.doc_set.value_num']
        if type == ContractSubtype.PARTNERS.name else commission]

    # for old agregator contracts
    if type == ContractSubtype.PARTNERS.name and commission == 2:
        commission_type += 1

    return commission_type


# blubimov методы такого типа лучше хранить в export_commons с декоратором @cacheable
def get_country_iso_code(country):
    query = "SELECT iso_code FROM t_country WHERE region_id = :country"
    country_iso_code = str(db.balance().execute(query, {'country': country})[0]['iso_code'])
    return country_iso_code.rjust(3, '0')


# ====================================================================================================================

# проверка выгрузки договора, нулевого ДС и всех остальных ДС по переданным спискам атрибутов
def check_contract_attrs(contract_id, contract_attrs_list, collaterals_attrs_list):
    with reporter.step(u'Считываем данные из баланса'):
        balance_contract_data, balance_collaterals_data = get_balance_data(contract_id)
    with reporter.step(u'Считываем данные из ОЕБС'):
        oebs_contract_data, oebs_collaterals_data = get_oebs_data(balance_contract_data, balance_collaterals_data)

    balance_contract_values, oebs_contract_values = \
        read_attr_values(contract_attrs_list, balance_contract_data, oebs_contract_data)

    utils.check_that(oebs_contract_values, equal_to_casted_dict(balance_contract_values),
                     step=u'Проверяем корректность данных договора в ОЕБС')

    balance_collaterals_values_list, oebs_collaterals_values_list = \
        read_attr_values_list(collaterals_attrs_list,
                              balance_collaterals_data.values(), oebs_collaterals_data.values())

    utils.check_that(oebs_collaterals_values_list, contains_dicts_equal_to(balance_collaterals_values_list),
                     step=u'Проверяем корректность данных доп.соглашений в ОЕБС')


# проверка выгрузки ДС
def check_collateral_attrs(contract_id, collateral_id, collateral_attrs_list):
    with reporter.step(u'Считываем данные из баланса'):
        balance_contract_data, balance_collaterals_data = get_balance_data(contract_id)
    with reporter.step(u'Считываем данные из ОЕБС'):
        oebs_contract_data, oebs_collaterals_data = get_oebs_data(balance_contract_data, balance_collaterals_data)

    collateral_num = db.balance().execute('SELECT num FROM t_contract_collateral WHERE id = :collateral_id',
                                          {'collateral_id': collateral_id}, single_row=True, fail_empty=True)['num']

    balance_collateral_values, oebs_collateral_values = \
        read_attr_values(collateral_attrs_list,
                         balance_collaterals_data[collateral_num], oebs_collaterals_data[collateral_num])

    utils.check_that(oebs_collateral_values, equal_to_casted_dict(balance_collateral_values),
                     step=u'Проверяем корректность данных доп.соглашения {} в ОЕБС'.format(
                         collateral_repr(contract_id, collateral_id)))


def collateral_repr(contract_id, collateral_id):
    query = "SELECT cc.id, cc.num, cct.CAPTION " \
            "FROM T_CONTRACT_COLLATERAL_TYPES cct, " \
            "T_CONTRACT2 c, " \
            "t_contract_collateral cc " \
            "WHERE c.id = :contract_id " \
            "AND cc.id = :collateral_id " \
            "AND cc.COLLATERAL_TYPE_ID = cct.id " \
            "AND cc.CONTRACT2_ID = c.ID " \
            "AND cct.CONTRACT_TYPE = c.TYPE"
    result = db.balance().execute(query, {'contract_id': contract_id, 'collateral_id': collateral_id},
                                  single_row=True)
    return u"'{caption}' (num = {num}, id = {id})".format(caption=result['caption'],
                                                          num=result['num'],
                                                          id=result['id'])


# ====================================================================================================================

def create_base_contract(context):
    with reporter.step(u'Выставляем договор'):
        client_params = context.client_params if hasattr(context, 'client_params') else None
        client_id = steps.ClientSteps.create(client_params, prevent_oebs_export=True)
        person_id = steps.PersonSteps.create(client_id, context.person_type.code, inn_type=InnType.RANDOM)

        contract_params = {
            'CURRENCY': context.currency.num_code,
            'MANAGER_CODE': ContractDefaults.MANAGER_CODE,
            'MANAGER_BO_CODE': ContractDefaults.MANAGER_BO_CODE,
            'SERVICES': [context.service.id],
            'DT': TODAY_ISO,
            'FINISH_DT': WEEK_AFTER_ISO,
            'IS_SIGNED': TODAY_ISO,
            'DEAL_PASSPORT': TODAY_ISO,
        }
        contract_params.update(context.contract_params)

        contract_id, _ = create_contract(client_id, person_id, context.contract_type, contract_params)

    return client_id, person_id, contract_id


def create_exported_collateral(context):
    client_id, person_id, contract_id = create_base_contract(context)

    collateral_params = {
        'CONTRACT2_ID': contract_id,
        'DT': TODAY_ISO,
        'IS_SIGNED': TODAY_ISO,
    }
    collateral_params.update(context.collateral_params)

    collateral_id = steps.ContractSteps.create_collateral(context.collateral_type_id,
                                                          collateral_params,
                                                          prevent_oebs_export=True)
    # steps.ExportSteps.export_oebs(contract_id=contract_id)
    return client_id, person_id, contract_id, collateral_id


def create_rsya_contract(context):
    with reporter.step(u'Выставляем договор РСЯ'):
        client_id = steps.ClientSteps.create(prevent_oebs_export=True)
        person_id = steps.PersonSteps.create_partner(client_id, context.person_type.code, inn_type=InnType.RANDOM)

        contract_id, _ = create_contract_rsya(client_id, person_id, context.contract_template)

    return client_id, person_id, contract_id


def create_spendable_contract(spendable_context, context_for_link=None, additional_service=None):
    with reporter.step(u'Создаем расходный договор'):
        client_id = steps.ClientSteps.create(prevent_oebs_export=True)
        additional_params = None
        general_contract_id = None
        if context_for_link:
            _, person_id, general_contract_id, _ = steps.ContractSteps.create_partner_contract(context_for_link, client_id=client_id)
            additional_params = {'link_contract_id': general_contract_id}
            # steps.ExportSteps.export_oebs(client_id=client_id,
            #                               person_id=person_id,
            #                               contract_id=general_contract_id)

        if additional_service:
            services = spendable_context.contract_services
            services.append(additional_service)
            additional_params.update({'services': services})
        _, person_id, partner_contract_id, _ = \
            steps.ContractSteps.create_partner_contract(spendable_context, client_id=client_id,
                                                        additional_params=additional_params)

    return client_id, person_id, partner_contract_id, general_contract_id
