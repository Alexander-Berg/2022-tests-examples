# -*- coding: utf-8 -*-
import pytest
import datetime
import os
import json

from balance import balance_steps as steps, balance_db as db
from btestlib import utils as ut
from btestlib.data import person_defaults
from btestlib import reporter
import btestlib.config as balance_config

NOW = ut.Date.nullify_time_of_date(datetime.datetime.now())
to_iso = ut.Date.date_to_iso_format
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))

try:
    import balance_contracts
    from balance_contracts.oebs.contract import replace_mask
    from balance_contracts.contract_utils import utils as contract_utils
    from balance_contracts.contract_utils import deep_equals
    json_contracts_repo_path = os.path.dirname(os.path.abspath(balance_contracts.__file__))
except ImportError as err:
    json_contracts_repo_path = ''

JSON_OEBS_PATH = '/oebs/contract/'

def create_contract(contract_type='no_agency'):
    client_id = steps.ClientSteps.create(prevent_oebs_export=True)
    person_id = steps.PersonSteps.create(client_id, 'ur', full=False,
                                         inn_type=person_defaults.InnType.UNIQUE)
    contract_params_dict = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'DT': HALF_YEAR_BEFORE_NOW_ISO,
        'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
        'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
        'SERVICES': [135, 651],
        'COUNTRY': 225,
        'REGION': '23000001000',
        'FIRM': 1
    }
    contract_id, _ = steps.ContractSteps.create_contract(contract_type, contract_params_dict)
    return client_id, person_id, contract_id


def delete_all_attributes(contract_id, attribute_batch_id):
    db.balance().execute("""delete from t_contract_attributes where attribute_batch_id=:attribute_batch_id
    and code != 'PRINT_TPL_BARCODE'""",
                         {'attribute_batch_id': attribute_batch_id})
    steps.ContractSteps.refresh_contracts_cache(contract_id)


SPENDABLE_SPECIFIC = {
    'CODE', 'IS_BOOKED', 'SERVICES', 'PRINT_TPL_EMAIL', 'PRINT_TPL_EMAIL_FROM', 'PAY_TO', 'PARTNER_COMMISSION_PCT',
    'PRINT_FORM_DT', 'INDIVIDUAL_DOCS', 'MANAGER_CODE', 'FIRM', 'IS_ARCHIVED', 'PRINT_TPL_EMAIL_MANAGER',
    'PAYMENT_TYPE', 'SENT_DT', 'IS_SUSPENDED', 'IS_OFFER', 'PRINT_TEMPLATE', 'PRINT_TPL_BARCODE', 'PRINT_TPL_MDS_KEY',
    'MEMO', 'LINK_CONTRACT_ID', 'CURRENCY_RATE_DT', 'GEOBASE_REGION', 'NDS', 'ATYPICAL_CONDITIONS',
    'PRINT_TPL_EMAIL_BODY', 'CONTRACT_TYPE', 'COUNTRY', 'REGION', 'SERVICE_START_DT', 'PRINT_TPL_EMAIL_SUBJECT',
    'PAYMENT_SUM', 'IS_BOOKED_DT', 'PRINT_TPL_EMAIL_TO'}

FULL_CONTRACT_ATTRS = {
    'ACCOUNT_TYPE': {'value_num': 0},
    'AFISHA_INFO': {'value_num': 500},
    'AFISHA_RIGHTS': {'value_num': 600},
    'AGREGATOR_PCT': {'value_num': 27},
    'BANK_DETAILS_ID': {'value_num': 601},
    'BUDGET_DISCOUNT_PCT': {'value_num': 20},
    'CALC_TERMINATION': {'value_dt': NOW},
    'COMMISSION': {'value_num': 22},
    'COMMISSION_CHARGE_TYPE': {'value_num': 1},
    'COMMISSION_PAYBACK_TYPE': {'value_num': 2},
    'COMMISSION_TYPE': {'value_num': 47},
    # 'CONTRACT_TYPE': {'value_num': 3},
    'COUNTRY': {'value_num': 169},
    'CREDIT_LIMIT': {'key_num': 17, 'value_num': 150000},
    'CREDIT_LIMIT_SINGLE': {'value_num': 100000},
    'CREDIT_TYPE': {'value_num': 2},
    'CURRENCY': {'value_str': 'RUB', 'value_num': 810},
    # флаг для general_договоров
    'DISCARD_NDS': {'value_num': 1},
    'DISCOUNT_PCT': {'value_num': 10},
    'DISCOUNT_POLICY_TYPE': {'value_num': 8},
    'DISTRIBUTION_PLACES': {'key_num': 4, 'value_num': 1},
    'DOC_SET': {'value_num': 4},
    'DOMAINS': {'value_str': 'mastercity.ru'},
    'DOWNLOAD_DOMAINS': {'value_str': 'tlauncher.org'},
    'END_DT': {'value_dt': NOW},
    'END_REASON': {'value_num': 1},
    # потестить!
    # 'FAKE_ID': {'value_num': 1},
    'FINISH_DT': {'value_dt': NOW + datetime.timedelta(days=1)},
    'FIRM': {'value_num': 13},
    'FIXED_DISCOUNT_PCT': {'value_num': 20},
    'INDIVIDUAL_DOCS': {'value_num': 1},
    'INSTALL_PRICE': {'value_num': 25},
    'INSTALL_SOFT': {'value_str': u'Софт: Я.Браузер+Алиса.'},
    'IS_OFFER': {'value_num': 1},
    'LINKED_CONTRACTS': {'key_num': 16755358, 'value_num': 1},
    # нарушает какой-то констрейнт
    # 'LINK_CONTRACT_ID': {'value_num': 16755358},
    'LOYAL_CLIENTS': {'key_num': 7484290, 'value_str': '{"todate": "2015-08-31", "turnover": "1005755.85"}'},
    'MANAGER_BO_CODE': {'value_num': 28812},
    'MANAGER_CODE': {'value_num': 27703},
    'MEMO': {'value_clob': u'Договор создан автоматически'},
    'MKB_PRICE': {'key_num': 28011, 'value_num': 200},
    # значение для партнерских
    'NDS': {'value_num': 18},
    'NEW_COMMISSIONER_REPORT': {'value_num': 1},
    'OPEN_DATE': {'value_num': 0},
    'PARENT_CONTRACT_ID': {'value_num': 1169968},
    'PARTNER_PCT': {'value_num': 43},
    'PAYMENT_TERM': {'value_num': 20},
    'PAYMENT_TERM_MAX': {'value_num': 45},
    'PAYMENT_TYPE': {'value_num': 3},
    'PAY_TO': {'value_num': 1},
    'PP_1137': {'value_num': 0},
    'PRINT_FORM_DT': {'value_dt': NOW},
    'PRINT_TEMPLATE': {'value_str': '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-Korporativnoe-Taksi/'
                                    'Partnerskaja-sxema/1/'},

    'PRODUCT_OPTIONS': {'value_str': 'Firefox desktop and mobile browser, firefox operating system'},
    'PRODUCT_SEARCH': {'value_str': u' - Сторонний софт'},
    'PRODUCT_SEARCHF': {'value_str': 'Firefox Browser'},
    'REGION': {'value_str': '23000001000'},
    'RIT_DISCOUNT': {'value_num': 100},
    'SEARCH_FORMS': {'value_num': 1},
    'SEARCH_PRICE': {'value_num': 100},
    'SENT_DT': {'value_dt': NOW},
    'SERVICES': {'key_num': 135, 'value_num': 1},
    'SERVICE_START_DT': {'value_dt': NOW},
    'SUPERCOMMISSION': {'value_num': 21},
    'SUPERCOMMISSION_BONUS': {'key_num': 150, 'value_num': 1},
    'SUPPLEMENTS': {'key_num': 1, 'value_num': 1},
    'TAIL_TIME': {'value_num': 6},
    # не грузим договор в тестовом режиме
    # 'TEST_MODE': {'value_num': 1},
    'UNILATERAL': {'value_num': 1},
    'UNILATERAL_ACTS': {'value_num': 1},
    'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': {'value_num': 23}
}


class Attribute(object):
    def __init__(self, code, value_num=None, value_str=None, value_dt=None, key_num=None, value_clob=None):
        self.code = code
        self.value_str = value_str
        self.value_num = value_num
        self.value_dt = value_dt
        self.key_num = key_num
        self.value_clob = value_clob

    def set(self, attribute_batch_id):
        db.balance().execute('''INSERT INTO BO.T_CONTRACT_ATTRIBUTES (ID, RELATED_OBJECT_TABLE, 
        ATTRIBUTE_BATCH_ID, CODE, VALUE_STR, VALUE_NUM, VALUE_DT, VALUE_CLOB, KEY_NUM) VALUES 
        (S_CONTRACT_ATTRIBUTES_ID.nextval, 'T_CONTRACT_COLLATERAL', :batch_id, :code,
         :value_str, :value_num, :value_dt,
         :value_clob, :key_num)''', {'value_num': self.value_num,
                                     'value_str': self.value_str,
                                     'batch_id': attribute_batch_id,
                                     'code': self.code,
                                     "value_dt": self.value_dt,
                                     "value_clob": self.value_clob,
                                     "key_num": self.key_num
                                     })

def check_json_contract(contract_id, person_id, json_file):
    try:
        db.balance().execute(
            """update t_person_firm set oebs_export_dt = sysdate where person_id = :person_id""",
            {'person_id': person_id})
    except Exception:
        pass

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

@pytest.mark.parametrize('contract_type, json_file', [
    # ('rsya_universal', ''),
    # ('no_agency', ''),
    ('spendable_default', 'spendable_default_full_contract.json')
],
ids=lambda contract_type, json_file: "{0}".format(contract_type))
def test_export_full_contract(contract_type, json_file):
    W_API = 0
    # json_value = '{{"Person": 1, "Contract": {}}}'.format(W_API)
    # db.balance().execute("""UPDATE (
    #                           SELECT *
    #                           FROM t_Config
    #                           WHERE ITEM = 'CLASSNAMES_EXPORTED_WITH_OEBS_API')
    #                         SET VALUE_JSON = '{}'""".format(json_value))
    client_id, person_id, contract_id = create_contract(contract_type)
    attribute_batch_id = db.get_collaterals_by_contract(contract_id)[0]['attribute_batch_id']
    delete_all_attributes(contract_id, attribute_batch_id)
    if contract_type == 'spendable_default':
        attr = Attribute('CONTRACT_TYPE',  **{'value_num': 3})
        attr.set(attribute_batch_id)
    for attrname, description in FULL_CONTRACT_ATTRS.iteritems():
        if contract_type == 'spendable_default':
            if attrname not in SPENDABLE_SPECIFIC:
                continue
        attr = Attribute(attrname, **description)
        attr.set(attribute_batch_id)

    if balance_config.JSON_CONTRACT_OEBS and json_file:
        check_json_contract(contract_id, person_id, json_file)

    else:
        steps.ExportSteps.export_oebs(client_id=client_id, person_id=person_id, contract_id=contract_id)


@pytest.mark.parametrize('contract_params', [{
    'CONTRACT_TYPE': {'value_num': 2},
    'DOC_SET': {'value_num': 4},
    'FIRM': {'value_num': 1},

    # 'AGREGATOR_PCT': {'value_num': None},
    'DOMAINS': {'value_str': None},
    'END_DT': {'value_dt': None},
    'END_REASON': {'value_num': 1},
    'INDIVIDUAL_DOCS': {'value_num': 0},
    'MEMO': {'value_clob': None},

    'NDS': {'value_num': 0},
    'OPEN_DATE': {'value_num': 1},
    'PAYMENT_TYPE': {'value_num': 1},
    'PAY_TO': {'value_num': 2},
    'SEARCH_FORMS': {'value_num': 0},
    'UNILATERAL_ACTS': {'value_num': 0},
}])
@pytest.mark.parametrize('contract_type, json_file', [
    ('rsya_universal', 'rsya_universal_alternative_partner.json'),
    # ('no_agency', '')
],
ids=lambda contract_type, json_file: "{0}".format(contract_type))
def test_export_alternative_partner_contract(contract_type, contract_params, json_file):
    # W_API = 1
    # json_value = '{{"Person": 1, "Contract": {}}}'.format(W_API)
    # db.balance().execute("""UPDATE (
    #                           SELECT *
    #                           FROM t_Config
    #                           WHERE ITEM = 'CLASSNAMES_EXPORTED_WITH_OEBS_API')
    #                         SET VALUE_JSON = '{}'""".format(json_value))
    client_id, person_id, contract_id = create_contract(contract_type)
    attribute_batch_id = db.get_collaterals_by_contract(contract_id)[0]['attribute_batch_id']
    delete_all_attributes(contract_id, attribute_batch_id)
    for attrname, description in contract_params.iteritems():
        attr = Attribute(attrname, **description)
        attr.set(attribute_batch_id)

    if balance_config.JSON_CONTRACT_OEBS and json_file:
        check_json_contract(contract_id, person_id, json_file)

    else:
        steps.ExportSteps.export_oebs(client_id=client_id, person_id=person_id, contract_id=contract_id)
