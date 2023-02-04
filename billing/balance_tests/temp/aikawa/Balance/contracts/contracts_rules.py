# -*- coding: utf-8 -*-
import datetime
import itertools

import attr
import hamcrest

import btestlib.utils as utils
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib.constants import PersonTypes, ContractCommissionType, ContractPaymentType
from btestlib.data.defaults import ContractDefaults

to_iso = utils.Date.date_to_iso_format
from_iso = utils.Date.from_iso_to_date_format


def diff(first, second):
    second = set(x.type for x in second)
    return [item for item in first if item.type not in second]


@attr.s()
class ContractContext(object):
    type = attr.ib()
    person_type = attr.ib()
    client_contract = attr.ib()
    name = attr.ib()
    commission_type = attr.ib()
    payment_type = attr.ib()

    def new(self, **kwargs):
        attrs_from_self_instance = attr.asdict(self, recurse=False)
        attrs_all = utils.merge_dicts([attrs_from_self_instance, kwargs])
        attrs_names_from_context_class = [f.name for f in attr.fields(ContractContext)]
        attrs_names_to_extend_context = list(set(attrs_all.keys()) - set(attrs_names_from_context_class))
        ContextNew = attr.make_class('ContextNew', attrs_names_to_extend_context, bases=(ContractContext,))
        return ContextNew(**attrs_all)


class ContractException(object):
    IS_SUSPENDED_EXCEPTION = u'Rule violation: \'Дата приостановки договора больше текущей даты\''
    FINISH_DT_NEEDED_EXCEPTION = u'Rule violation: \'Не задана дата окончания действия договора\''
    COMMISSION_NEEDED_EXCEPTION = u'Rule violation: \'Выберите комиссию\''
    WRONG_COMMISSION_EXCEPTION = u'Rule violation: \'Неккоректный тип комиссии\''
    WRONG_PERSON_CATEGORY_FOR_FIRM = u'Rule violation: \'Тип плательщика не соответствует фирме\''
    WRONG_PERSON_CATEGORY_FOR_CONTRACT_EXCEPTION = u'Rule violation: \'Неправильный плательщик для выбранного типа' \
                                                   u' договора\''
    PERSON_NEEDED_EXCEPTION = u'Rule violation: \'Не выбран плательщик\''
    MANAGER_NEEDED_EXCEPTION = u'Rule violation: \'Не выбран менеджер\''
    PAYMENT_TYPE_NEEDED_EXCEPTION = u'Rule violation: \'Не выбран тип оплаты\''
    SERVICE_NEEDED_EXCEPTION = u'Rule violation: \'Выберите один из сервисов\''
    BRAND_START_DT_EXCEPTION = u'Rule violation: \'До даты начала действия бренда должно быть не менее суток\''
    START_DT_EXCEPTION = u'Rule violation: \'Не задана дата начала действия договора\''
    CLIENT_NEEDED_EXCEPTION = u'Rule violation: \'Не выбран клиент\''
    AGENCY_NEEDED_EXCEPTION = u'Rule violation: \'Не выбрано агентство\''
    DT_NEEDED_EXCEPTION = u'Rule violation: \'Не задана дата начала действия договора\''


PARAM_NEEDED_EXCEPTION = {
    'CLIENT_ID': [ContractException.CLIENT_NEEDED_EXCEPTION,
                  ContractException.AGENCY_NEEDED_EXCEPTION],
    'DT': ContractException.DT_NEEDED_EXCEPTION,
    'MANAGER_CODE': ContractException.MANAGER_NEEDED_EXCEPTION,
    'PAYMENT_TYPE': ContractException.PAYMENT_TYPE_NEEDED_EXCEPTION,
    'PERSON_ID': ContractException.PERSON_NEEDED_EXCEPTION,
    'SERVICES': ContractException.SERVICE_NEEDED_EXCEPTION,
    'FINISH_DT': ContractException.FINISH_DT_NEEDED_EXCEPTION,
    'COMMISSION_TYPE': ContractException.COMMISSION_NEEDED_EXCEPTION
}

CommonContract = ContractContext(type=ContractCommissionType.NO_AGENCY,
                                 person_type=PersonTypes.UR,
                                 client_contract=True,
                                 name='Contract',
                                 commission_type=33,
                                 payment_type=ContractPaymentType.PREPAY)

# all_contracts = [NO_AGENCY, COMMISS, PARTNER, PR_AGENCY, OPT_AGENCY, OPT_CLIENT, OFFER, OFD_WO_COUNT, USA_OPT_CLIENT,
#                  USA_OPT_AGENCY, SW_OPT_CLIENT, SW_OPT_AGENCY, TR_OPT_AGENCY, TR_OPT_CLIENT, BRAND, OPT_AGENCY_PREM,
#                  BEL_OPT_AGENCY_PREM, BEL_NO_AGENCY, BEL_PR_AGENCY, BEL_OPT_AGENCY, AUTO_OPT_AGENCY_PREM,
#                  AUTO_NO_AGENCY, GARANT_RU, GARANT_BEL, GARANT_KZT, LICENSE, KZ_NO_AGENCY, KZ_COMMISSION,
#                  KZ_OPT_AGENCY]
'''не используется'''
# OPT_AGENCY_RB = CommonContract.new(type=ContractCommissionType.OPT_AGENCY_RB)
# UA_OPT_CLIENT = CommonContract.new(type=ContractCommissionType.UA_OPT_CLIENT, person_type=PersonTypes.UA)
# KZ = CommonContract.new(person_type=PersonTypes.KZU, type=ContractCommissionType.KZ)
# WO_COUNT = ContractCommissionType.WO_COUN
# UA_COMMISS = ContractCommissionType.UA_COMMISS
# UA_PR_AGENCY = ContractCommissionType.UA_PR_AGENCY
# UA_OPT_AGENCY_PREM = ContractCommissionType.UA_OPT_AGENCY_PREM
# GARANT_UA = CommonContract.new(type=ContractCommissionType.GARANT_UA, name='GARANT_UA')

'''не работает, разобраться позже'''
ATTORNEY = CommonContract.new(type=ContractCommissionType.ATTORNEY, name='ATTORNEY')

NOW = datetime.datetime.now()
NOW_NULLIFIED = utils.Date.nullify_time_of_date(NOW)
NOW_NULLIFIED_ISO = utils.Date.date_to_iso_format(NOW_NULLIFIED)
TOMORROW = NOW + datetime.timedelta(days=1)
TOMORROW_NULLIFIED = utils.Date.nullify_time_of_date(TOMORROW)
TOMORROW_NULLIFIED_ISO = utils.Date.date_to_iso_format(TOMORROW_NULLIFIED)


def group_by_contract_state(data):
    data = sorted(data, key=lambda d: d[0])
    for k, g in itertools.groupby(data, lambda d: d[0]):
        dict_ = {}
        for dict_param in [c[1] for c in list(g)]:
            dict_.update(dict_param)
        yield k, dict_


def multiply_by_params_value(data):
    result = []
    for contract_state, params in data:
        param_values = params.values()[0]
        param_key = params.keys()[0]
        if isinstance(param_values, list):
            for param_value in param_values:
                result.append([contract_state, {param_key: param_value}])
    return result


def fill_attrs(context, adds=None, deletions=None):
    if context.client_contract:
        client_params = {}
    else:
        client_params = {'IS_AGENCY': 1}
    client_id = hasattr(context, 'adds') and context.adds.get('CLIENT_ID', None) or steps.ClientSteps.create(
        client_params)
    person_id = hasattr(context, 'adds') and context.adds.get('PERSON_ID', None) or steps.PersonSteps.create(client_id,
                                                                                                             context.person_type.code)

    common_dict = {'CLIENT_ID': client_id,
                   'DT': NOW_NULLIFIED_ISO,
                   'FINISH_DT': TOMORROW_NULLIFIED_ISO,
                   'COMMISSION': context.type.id,
                   'PERSON_ID': person_id,
                   'PAYMENT_TYPE': context.payment_type,
                   'PAYMENT_TERM': 15,
                   'SERVICES': [7],
                   'MANAGER_CODE': ContractDefaults.MANAGER_BO_CODE,
                   'COMMISSION_TYPE': context.commission_type,
                   'DECLARED_SUM': 1
                   }

    result_dict = {your_key: common_dict[your_key] for your_key in context.minimal_attrs}
    if hasattr(context, 'adds'):
        result_dict.update(context.adds)
    if hasattr(context, 'deletions'):
        for key in context.deletions:
            result_dict.pop(key, None)
    return result_dict


def get_collateral_attribute_by_code(collateral_id, code, type):
    collateral_attrs = db.get_attributes_by_collateral_id(collateral_id)
    value = [attr for attr in collateral_attrs if attr['code'] == code]
    if value:
        return value[0][type]
    else:
        return None


DB_PARAM_TYPE = {'value_dt': ['IS_SUSPENDED', 'FINISH_DT', 'IS_FAXED'],
                 'value_num': ['FIRM', 'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE', 'AUTORU_Q_PLAN', 'COMMISSION_TYPE',
                               'PAYMENT_TYPE', 'MANAGER_CODE', 'UNILATERAL', 'SERVICES', 'BRAND_TYPE', 'SENT_DT',
                               'DISCARD_MEDIA_DISCOUNT', 'COMMISSION_CHARGE_TYPE', 'DISCARD_NDS', 'DISCOUNT_FIXED',
                               'COMMISSION', 'MEMO', 'BANK_DETAILS_ID', 'CURRENCY', 'FAKE_ID', 'DISCOUNT_POLICY_TYPE',
                               'NDS_FOR_RECEIPT', 'MANAGER_BO_CODE']}


def get_collateral_header_attribute_by_code(contract_id, param_name):
    return db.get_collaterals_by_contract(contract_id)[0][param_name]


def get_contract_attribute_by_code(contract_id, code):
    for key, attrs_list in DB_PARAM_TYPE.iteritems():
        if code in attrs_list:
            db_type = key
    collateral_id = db.get_collaterals_by_contract(contract_id)[0]['id']
    return get_collateral_attribute_by_code(collateral_id, code, db_type)


def prepare_contract_params(context, param_name=False, param_in_contract=False):
    op_context = context.new()
    if not param_in_contract:
        op_context.deletions = [param_name]
    contract_params = fill_attrs(op_context)
    if not param_in_contract:
        utils.check_that(contract_params.get(param_name, None), hamcrest.equal_to(None))
    else:
        utils.check_that(contract_params.get(param_name, None), hamcrest.not_none())

    return context, contract_params


def create_contract_with_exception(context, contract_params, exception_text):
    try:
        steps.ContractSteps.create_contract_new(context.type.name, contract_params,
                                                strict_params=True)
    except Exception as exc:
        if isinstance(exception_text, list):
            utils.check_that(steps.CommonSteps.get_exception_code(exc, tag_name='msg'),
                             hamcrest.is_in(exception_text))
        else:
            utils.check_that(exception_text,
                             hamcrest.equal_to(steps.CommonSteps.get_exception_code(exc, tag_name='msg')))
    else:
        raise utils.TestsError(u'Договор создался без обязательного параметра или без исключения')


CONTRACT_HEADER_PARAMS = ['PERSON_ID', 'CLIENT_ID']
COLLATERAL_HEADER_PARAMS = ['IS_FAXED', 'IS_SIGNED', 'DT']


def get_db_type(code):
    for key, attrs_list in DB_PARAM_TYPE.iteritems():
        if code in attrs_list:
            db_type = key
    return db_type


def collect_contract_params(contract_id):
    attribute_dict = {}
    contract_header = db.get_contract_by_id(contract_id)[0]
    attribute_dict.update({k.upper(): v for k, v in contract_header.iteritems()})
    collateral_header = db.get_collaterals_by_contract(contract_id)[0]
    attribute_dict.update({k.upper(): v for k, v in collateral_header.iteritems()})
    collateral_id = collateral_header['id']
    collateral_attributes = db.get_attributes_by_collateral_id(collateral_id)
    attribute_dict.update(
        {attribute['code']: attribute[get_db_type(attribute['code'])] for attribute in collateral_attributes})
    return attribute_dict


def check_contract_param_db(contract_id, param_name, expected_value=None):
    if param_name in CONTRACT_HEADER_PARAMS:
        value_db = db.get_contract_by_id(contract_id)[0][param_name.lower()]
    elif param_name in COLLATERAL_HEADER_PARAMS:
        value_db = get_collateral_header_attribute_by_code(contract_id, param_name.lower())
    else:
        value_db = get_contract_attribute_by_code(contract_id, param_name)
    if isinstance(value_db, datetime.datetime):
        expected_value = from_iso(expected_value)
    utils.check_that(value_db, hamcrest.equal_to(expected_value))


def check_param(context, param_name=None, optional=False, strictly_needed=False, with_exception=False, hidden=False,
                with_default=False, changeable=False, without_check=False):
    '''
    optional - проверяет создание с атрибутом и без него
    changeable - проверяет создание с атрибутом
    strictly_needed - проверяет создание без атрибута с исключением
    with_exception - проверяет создание с атрибутом с исключением
    hidden - проверяет, что при создании с атрибутом значение не сохраняется в бд
    with_default - проверяет, что при создании без атрибута значение по умолчанию сохраняется в бд
    '''
    orig_context = context
    contract_id, contract_params = None, None
    if without_check:
        op_context, contract_params = prepare_contract_params(orig_context, param_name, param_in_contract=False)
        contract_id, _ = steps.ContractSteps.create_contract_new(op_context.type.name, contract_params,
                                                                 strict_params=True)
    if optional or with_default:
        op_context, contract_params = prepare_contract_params(orig_context, param_name, param_in_contract=False)
        contract_id, _ = steps.ContractSteps.create_contract_new(op_context.type.name, contract_params,
                                                                 strict_params=True)
        check_contract_param_db(contract_id, param_name, expected_value=None if optional else with_default)
    if strictly_needed:
        sn_context, contract_params = prepare_contract_params(orig_context, param_name, param_in_contract=False)
        create_contract_with_exception(sn_context, contract_params, strictly_needed)
    if changeable:
        av_context, contract_params = prepare_contract_params(orig_context, param_name, param_in_contract=True)
        contract_id, _ = steps.ContractSteps.create_contract_new(av_context.type.name, contract_params,
                                                                 strict_params=True)
        check_contract_param_db(contract_id, param_name, expected_value=contract_params[param_name])
    if with_exception:
        ex_context = orig_context.new()
        contract_params = fill_attrs(ex_context)
        create_contract_with_exception(ex_context, contract_params, with_exception)
    if hidden:
        hid_context, contract_params = prepare_contract_params(orig_context, param_name, param_in_contract=True)
        contract_id, _ = steps.ContractSteps.create_contract_new(hid_context.type.name, contract_params,
                                                                 strict_params=True)
        check_contract_param_db(contract_id, param_name, expected_value=None)
    return contract_id, contract_params
