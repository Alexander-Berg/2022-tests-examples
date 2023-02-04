# coding=utf-8
__author__ = 'igogor'

import copy

import datetime
import json
import time
import urlparse
from dateutil.relativedelta import relativedelta

import dateutil.parser
from enum import Enum
import common_steps
import client_steps
import export_steps
import integration_steps
import person_steps
import balance.balance_api as api
import balance.balance_db as db
import btestlib.environments as env
import btestlib.reporter as reporter
import btestlib.utils as utils
import btestlib.config as balance_config
from btestlib.constants import Nds, Export, ContractCommissionType, Managers, BrandType, ContractSubtype, OfferConfirmationType
from btestlib.data import person_defaults, defaults, contract_defaults

log_align = 30
# log = reporter.logger()

# еще дефолтные даты есть в btestlib.data.defaults.Date
to_iso = utils.Date.date_to_iso_format
NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
A_DAY_AFTER_TOMORROW = NOW + datetime.timedelta(days=2)


class ContractConversion(object):
    MONTHS_NUMBERS_TO_STRING = {
        1: u'янв', 2: u'фев', 3: u'мар', 4: u'апр', 5: u'май', 6: u'июн', 7: u'июл', 8: u'авг',
        9: u'сен', 10: u'окт', 11: u'ноя', 12: u'дек'
    }

    @staticmethod
    def add_checkbox_set(contract_params, contract_key, values):
        for key in contract_params.keys():
            if key.startswith(contract_key):
                contract_params.pop(key)

        contract_params[contract_key] = u'1'

        for value in values:
            contract_params['{}-{}'.format(contract_key, value)] = value

    @staticmethod
    def add_checkbox_date(contract_params, contract_key, string_date):
        keys = contract_key.split(',')

        if string_date is None:
            for key in keys:
                contract_params.pop(key, None)
            return

        # setting flags
        contract_params[keys[0]] = ''
        contract_params[keys[0] + '-checkpassed'] = u'1'

        # ...-date - date for display in format: 'DD MON YYYY г.'
        datetime_value = dateutil.parser.parse(string_date)
        contract_params[keys[1]] = u'{0} {1} {2} г.'.format(
            datetime_value.day,
            ContractConversion.MONTHS_NUMBERS_TO_STRING[datetime_value.month],
            datetime_value.year
        )

        contract_params[keys[2]] = string_date  # real date

    @staticmethod
    def add_checkbox(contract_params, contract_key, value):
        contract_params.pop(contract_key, None)

        if value == 1:
            contract_params[contract_key] = ''
            contract_params[contract_key + '-checkpassed'] = 1

    @staticmethod
    def add_advisor_price(contract_params, contract_key, price):
        contract_params[contract_key] = price

    @staticmethod
    def set_price(contract_params, contract_key, prices):
        prices_list = json.loads(contract_params[contract_key])
        not_found_products = [product_id for product_id, _ in prices]

        for product_id, price in prices:
            for price_dict in prices_list:
                if price_dict['id'] == product_id:
                    price_dict['price'] = price
                    not_found_products.remove(product_id)

        contract_params[contract_key] = json.dumps(prices_list)

        if not_found_products:
            raise KeyError('Product with IDs {} were not found'.format(not_found_products))


class ContractMode(Enum):
    CONTRACT = (
        contract_defaults.get_contract_template_by_name,
        lambda type_: contract_defaults.get_contract_database_to_contract_keys_dict(),
        [
            (ContractConversion.add_checkbox_set, ['SERVICES', 'SUPPLEMENTS']),

            (ContractConversion.add_checkbox_date, ['IS_SIGNED', 'IS_FAXED', 'IS_CANCELLED',
                                                    'IS_SUSPENDED', 'SENT_DT', 'DEAL_PASSPORT']),

            (ContractConversion.add_checkbox, ['NON_RESIDENT_CLIENTS', 'REPAYMENT_ON_CONSUME',
                                               'PERSONAL_ACCOUNT', 'LIFT_CREDIT_ON_PAYMENT', 'PERSONAL_ACCOUNT_FICTIVE',
                                               'CREDIT_LIMIT_IN_CONTRACT_CURRENCY', 'IS_BOOKED', 'TEST_MODE',
                                               'DMP_SEGMENTS', 'NO_ACTS', 'ATYPICAL_CONDITIONS']),

            (ContractConversion.add_advisor_price, ['ADVISOR_PRICE']),

            (ContractConversion.set_price, ['PRODUCTS_DOWNLOAD', 'PRODUCTS_REVSHARE'])
        ]
    )

    COLLATERAL = (
        contract_defaults.get_collateral_template_by_name,
        contract_defaults.get_collateral_database_to_contract_keys_dict,
        [
            (ContractConversion.add_checkbox_set, ['SERVICES']),

            (ContractConversion.add_checkbox_date, ['IS_SIGNED', 'IS_FAXED', 'IS_BOOKED']),

            (ContractConversion.add_checkbox, ['DMP_SEGMENTS']),
        ]
    )

    def get_template_params_dict(self, template_name):
        # Convert to list of the tuples ('param', 'value')
        source_tuples = urlparse.parse_qsl(self.get_template_by_name(template_name), True)
        # Convert to dict
        return {key: value.decode('utf-8') for (key, value) in source_tuples}

    def get_template_params_dict_new(self, commission_type):
        # Convert to list of the tuples ('param', 'value')
        source_tuples = urlparse.parse_qsl((
            contract_defaults.contract_left
            + contract_defaults.contract_right
            + contract_defaults.contract_signed
            + contract_defaults.contract_postpay
            + contract_defaults.adfox
        ), True)
        # Convert to dict
        parsed_dict = {key: value.decode('utf-8') for (key, value) in source_tuples}
        parsed_dict['commission'] = commission_type.id
        return parsed_dict

    def __init__(self, get_template_by_name, database_to_contract_keys_dict, special_keys):
        self.get_template_by_name = get_template_by_name
        self.get_database_to_contract_keys_dict = database_to_contract_keys_dict
        self.special_keys = special_keys


class ContractSteps(object):
    @staticmethod
    def refresh_contracts_cache(*contract_ids):  # type: (*int) -> None
        """
        Тк тесты часто изменяют T_CONTRACT_ATTRIBUTE прямыми sql запросами,
        то нужно вызывать этот метод, чтобы обновить T_CONTRACT_LAST_ATTTR и T_CONTRACT_SIGNED_ATTR.
        Так же надо вызвать, если меняется dt / is_signed
        """
        api.test_balance().UpdateContractCache(contract_ids)

    # a-vasin: юзать только внутри степов, а то развалится
    @staticmethod
    def report_url(contract_id):
        contract_url = '{base_url}/contract-edit.xml?contract_id={contract_id}'.format(
            base_url=env.balance_env().balance_ai,
            contract_id=contract_id)
        reporter.report_url(u'Ссылка на договор', contract_url)

    @staticmethod
    def create(template_name, params, mode, passport_uid, remove_params):
        with reporter.step(u"Создаём {} c типом '{}'".format(
                u'договор' if mode == ContractMode.CONTRACT else u'допсоглашение', template_name)):
            contract_params = mode.get_template_params_dict(template_name)
            database_to_contract_keys_dict = mode.get_database_to_contract_keys_dict(template_name)

            special_keys = mode.special_keys
            special_keys_list = [database_key for entry in special_keys for database_key in entry[1]]

            ignored_keys = []

            for database_key in params:
                if database_key not in database_to_contract_keys_dict:
                    ignored_keys.append(database_key)
                    continue

                contract_key = database_to_contract_keys_dict[database_key]
                value = params[database_key]

                if database_key not in special_keys_list:
                    contract_params[contract_key] = value if value is not None else u''
                    continue

                for adder, key_list in special_keys:
                    if database_key in key_list:
                        adder(contract_params, contract_key, value)

            if remove_params:
                for database_key in remove_params:
                    contract_keys = database_to_contract_keys_dict[database_key] \
                        if database_key in database_to_contract_keys_dict else database_key
                    for key in contract_keys.split(','):
                        contract_params.pop(key, None)

            if mode == ContractMode.COLLATERAL:
                contract_collaterals_before = ContractSteps.get_contract_collateral_ids(contract_params['id'])

            # reporter.log('Next params were ignored: {0}'.format(ignored_keys))
            # reporter.log((passport_uid, contract_params))
            contract = api.medium().CreateContract(passport_uid, contract_params)

            contract_id = contract['ID']
            contract_external_id = contract['EXTERNAL_ID']
            contract_result_info = {'Contract_ID': contract_id, 'External_ID': contract_external_id}

            if mode == ContractMode.COLLATERAL:
                contract_collaterals_after = ContractSteps.get_contract_collateral_ids(contract_id)
                collateral_id = set(contract_collaterals_after).difference(contract_collaterals_before).pop()
                contract_result_info['Collateral_ID'] = collateral_id

            reporter.attach(u'Параметры {}'.format(u'договора' if mode == ContractMode.CONTRACT else u'допсоглашения'),
                            u'Заданные параметры: \n{}\nИгнорированные параметры: \n{}\n'
                            u'Результат: {}'.format(
                                utils.Presenter.pretty(params), utils.Presenter.pretty(ignored_keys),
                                utils.Presenter.pretty(contract_result_info)))

            ContractSteps.report_url(contract_id)

        # Для коллег из OEBS: публикуем ID объекта в отчёте, выгружаем объект в OEBS и публикуем лог выгрузки
        if balance_config.TRUST_ME_I_AM_OEBS_QA:
            export_steps.ExportSteps.extended_oebs_contract_export(contract_id,
                                                                   params.get('PERSON_ID', None))

        if mode == ContractMode.COLLATERAL:
            return collateral_id
        else:
            return contract_id, contract_external_id

    @staticmethod
    def create_new(commission_type_or_name, params, mode, passport_uid, strict_params=False):
        """
        :param commission_type_or_name: константа из constants.ContractCommissionType или ее имя строкой
        """

        contract_commission_type = ContractCommissionType.get_by_name(commission_type_or_name)

        with reporter.step(u"Создаём договор c типом '{0}'".format(contract_commission_type.display_name)):
            if not strict_params:
                contract_params = mode.get_template_params_dict_new(contract_commission_type)
            else:
                contract_params = {}
            database_to_contract_keys_dict = mode.get_database_to_contract_keys_dict(
                contract_commission_type.name.lower())
            special_keys = mode.special_keys
            special_keys_list = [database_key for entry in special_keys for database_key in entry[1]]

            ignored_keys = []

            for database_key in params:
                if database_key not in database_to_contract_keys_dict:
                    ignored_keys.append(database_key)
                    continue

                contract_key = database_to_contract_keys_dict[database_key]
                value = params[database_key]

                if database_key not in special_keys_list:
                    if value == 0:
                        contract_params[contract_key] = value
                    else:
                        contract_params[contract_key] = value or ''  # empty string for params with None value
                        continue

                for adder, key_list in special_keys:
                    if database_key in key_list:
                        adder(contract_params, contract_key, value)

            reporter.attach(u'Игнорированные параметры', utils.Presenter.pretty(ignored_keys))

            reporter.attach(u'Параметры договора', utils.Presenter.pretty(params))
            # reporter.log((passport_uid, contract_params))

            contract = api.medium().CreateContract(passport_uid, contract_params)

            contract_url = '{base_url}/contract-edit.xml?contract_id={contract_id}'.format(
                base_url=env.balance_env().balance_ai, contract_id=contract['ID'])
            # reporter.log(u'mode = {0} | Contract_id: {1} (external_id: {2}) url: {3}'.format(mode, contract['ID'],
            #                                                                                  contract['EXTERNAL_ID'],
            #                                                                                  contract_url))
            reporter.report_url(u'Ссылка на договор', contract_url)

        # Для коллег из OEBS: публикуем ID объекта в отчёте, выгружаем объект в OEBS и публикуем лог выгрузки
        if balance_config.TRUST_ME_I_AM_OEBS_QA:
            export_steps.ExportSteps.extended_oebs_contract_export(contract['ID'],
                                                                   params.get('PERSON_ID', None))

        return contract['ID'], contract['EXTERNAL_ID']

    @staticmethod
    def create_contract(type_, params, passport_uid=defaults.PASSPORT_UID, remove_params=None,
                        prevent_oebs_export=False):
        contract_id, contract_external_id = \
            ContractSteps.create(type_, params, ContractMode.CONTRACT, passport_uid, remove_params)

        if prevent_oebs_export:
            export_steps.ExportSteps.prevent_auto_export(contract_id, Export.Classname.CONTRACT)
            export_steps.ExportSteps.prevent_auto_export(ContractSteps.get_collateral_id(contract_id),
                                                         Export.Classname.CONTRACT_COLLATERAL)

        return contract_id, contract_external_id

    @staticmethod
    def create_contract_new(type_, params, passport_uid=None, prevent_oebs_export=False,
                            strict_params=False):
        if passport_uid is None:
            passport_uid = defaults.PASSPORT_UID

        contract_id, contract_external_id = ContractSteps.create_new(type_, params, ContractMode.CONTRACT, passport_uid,
                                                                     strict_params)

        if prevent_oebs_export:
            export_steps.ExportSteps.prevent_auto_export(contract_id, Export.Classname.CONTRACT)
            export_steps.ExportSteps.prevent_auto_export(ContractSteps.get_collateral_id(contract_id),
                                                         Export.Classname.CONTRACT_COLLATERAL)

        return contract_id, contract_external_id

    @staticmethod
    def create_contract_3(type_, client_id, person_id, params, add_params=None, format_params=True, passport_uid=None,
                          strict_params=False):
        params = params.copy()
        params.update({
            'CLIENT_ID': client_id,
            'PERSON_ID': person_id,
        })

        if add_params:
            params.update(add_params)

        if format_params:
            params = ContractSteps.format_params_for_contract(params)

        return ContractSteps.create_contract_new(type_, params, passport_uid, strict_params=strict_params)


    @staticmethod
    def create_general_contract_by_context(context, client_id=None, person_id=None, postpay=False, services=[],
                                           contract_template='general_default', is_signed=True, contract_type=None,
                                           start_dt=datetime.datetime.now(), finish_dt=None, fictive_scheme=False,
                                           signed_dt=datetime.datetime.now(), additional_params=None, remove_params=None,
                                           old_pa=False):
        client_id = client_id or client_steps.ClientSteps.create()
        person_id = person_id or person_steps.PersonSteps.create(client_id, context.person_type.code,
                                                                 inn_type=person_defaults.InnType.RANDOM)

        params = {
            'CLIENT_ID': client_id,
            'PERSON_ID': person_id,
            'MANAGER_CODE': Managers.SOME_MANAGER.code,
            'FIRM': context.firm.id,
            'CURRENCY': context.currency.num_code,
            'COMMISSION': contract_type or context.contract_type.id,
            'SERVICES': services or [context.service.id],
            'DT': utils.Date.nullify_time_of_date(start_dt)
        }

        if postpay:
            params.update({'PAYMENT_TERM': 10, 'PAYMENT_TYPE': 3, 'CREDIT_TYPE': 2, 'PERSONAL_ACCOUNT': 1,
                           'CREDIT_LIMIT_SINGLE': 10000000, 'PERSONAL_ACCOUNT_FICTIVE': 1})
            if old_pa:
                params.pop('PERSONAL_ACCOUNT_FICTIVE')
        else:
            params.update({'PAYMENT_TYPE': 2})

        to_iso = utils.Date.date_to_iso_format
        if is_signed:
            signed_dt = to_iso(utils.Date.nullify_time_of_date(signed_dt))
            params.update({'IS_SIGNED': signed_dt})
        if finish_dt:
            finish_dt = to_iso(utils.Date.nullify_time_of_date(finish_dt))
            params.update({'FINISH_DT': finish_dt})

        if remove_params:
            for key in remove_params:
                params.pop(key, None)

        if additional_params:
            params.update(additional_params)

        contract_id, external_contract_id = ContractSteps.create_contract(contract_template, params=params)

        if fictive_scheme:
            ContractSteps.force_convert_to_fictive_credit_scheme(contract_id)

        return client_id, person_id, contract_id, external_contract_id


    @staticmethod
    def create_collateral(type_, params, passport_uid=None, remove_params=None, prevent_oebs_export=False):
        if passport_uid is None:
            passport_uid = defaults.PASSPORT_UID

        collateral_id = ContractSteps.create(type_, params, ContractMode.COLLATERAL, passport_uid, remove_params)

        if prevent_oebs_export:
            export_steps.ExportSteps.prevent_auto_export(collateral_id, Export.Classname.CONTRACT_COLLATERAL)

        return collateral_id

    @staticmethod
    def create_collateral_real(contract_id, collateral_type_id, params, passport_uid=None):
        if passport_uid is None:
            passport_uid = defaults.PASSPORT_UID

        with reporter.step(
                u"Создаём допсоглашение явным методом CreateCollateral c типом '{0}'".format(collateral_type_id)):
            result = api.medium().CreateCollateral(passport_uid, contract_id, collateral_type_id, params)

        return result

    @staticmethod
    def create_collateral_3(type_, contract_id, params, add_params=None, format_params=True, passport_uid=None,
                            remove_params=None):
        params = params.copy()
        params.update({
            'CONTRACT2_ID': contract_id,
        })

        if add_params:
            params.update(add_params)

        if format_params:
            params = ContractSteps.format_params_for_contract(params)

        return ContractSteps.create_collateral(type_, params, passport_uid, remove_params)

    @staticmethod
    def format_params_for_contract(params):
        if 'FIRM' in params:
            params['FIRM'] = params['FIRM'].id

        if 'CURRENCY' in params:
            params['CURRENCY'] = params['CURRENCY'].num_code

        if 'SERVICES' in params:
            params['SERVICES'] = [s.id for s in params['SERVICES']]

        date_params = ['DT', 'FINISH_DT', 'END_DT', 'SENT_DT', 'IS_SIGNED', 'IS_FAXED', 'IS_CANCELLED', 'IS_SUSPENDED',
                       'IS_BOOKED', 'DEAL_PASSPORT']
        for dp in date_params:
            if dp in params:
                params[dp] = utils.Date.to_iso(params[dp])

        return params

    @staticmethod
    def update_contract_params_in_db(contract_id, action, dt=None):
        # action = 1 установить is_signed (дату подписания)
        # action = 2 обнулить is_signed
        # action = 3 проставить end_dt
        # action = 4 проставить start_dt
        if dt:
            dt = utils.Date.nullify_time_of_date(dt)

        if action == 1:
            query = "UPDATE t_contract_collateral SET is_signed = :dt WHERE contract2_id = :contract_id"
            params = {'dt': utils.Date.nullify_time_of_date(datetime.datetime.today()), 'contract_id': contract_id}
            db.balance().execute(query, params)
        if action == 2:
            query = "UPDATE t_contract_collateral SET is_signed = NULL WHERE contract2_id = :contract_id"
            params = {'contract_id': contract_id}
            db.balance().execute(query, params)
        if action == 3:
            query = "UPDATE t_contract_attributes SET value_dt = :dt " \
                    "WHERE attribute_batch_id = (SELECT attribute_batch_id FROM t_contract_collateral" \
                    " WHERE contract2_id = :contract_id) AND code = 'FINISH_DT'"
            params = {'contract_id': contract_id, 'dt': dt}
            db.balance().execute(query, params)
        if action == 4:
            query = "UPDATE t_contract_collateral SET dt = :dt WHERE contract2_id = :contract_id"
            params = {'contract_id': contract_id, 'dt': dt}
            db.balance().execute(query, params)
        ContractSteps.refresh_contracts_cache(contract_id)

    @staticmethod
    def set_contract_is_signed(contract_id):
        with reporter.step(u"Устанавливаем дату подписания договора на сегодня"):
            ContractSteps.update_contract_is_signed(contract_id, datetime.datetime.today())
            ContractSteps.refresh_contracts_cache(contract_id)

    @staticmethod
    def clear_contract_is_signed(contract_id):
        with reporter.step(u"Очищаем дату подписания договора"):
            ContractSteps.update_contract_is_signed(contract_id, None)
            ContractSteps.refresh_contracts_cache(contract_id)

    @staticmethod
    def get_contract_collateral_ids(contract_id):
        result = db.balance().execute(
            'SELECT id FROM t_contract_collateral WHERE contract2_id = :contract_id',
            {'contract_id': contract_id})
        return [col['id'] for col in result]

    @staticmethod
    def get_collateral_id(contract_id, collateral_num=0):
        if collateral_num == 0:
            query = 'SELECT id FROM t_contract_collateral ' \
                    'WHERE contract2_id = {contract_id} AND num IS NULL'.format(contract_id=contract_id)
        else:
            query = 'SELECT id FROM t_contract_collateral ' \
                    'WHERE contract2_id = {contract_id} AND num = {col_num}'.format(contract_id=contract_id,
                                                                                    col_num=collateral_num)
        return db.balance().execute(query, single_row=True)['id']

    @staticmethod
    def get_attribute_batch_id(contract_id):
        query = 'select attribute_batch_id from t_contract_collateral ' \
                    'WHERE contract2_id = {contract_id}'.format(contract_id=contract_id)
        return db.balance().execute(query, single_row=True)['attribute_batch_id']

    @staticmethod
    def link(main_contract, linked_contract):
        attribute_batch_id = db.get_collaterals_by_contract(main_contract)[0]['attribute_batch_id']
        db.balance().execute('''INSERT INTO T_CONTRACT_ATTRIBUTES (id, dt, code, KEY_NUM, VALUE_STR, VALUE_NUM, VALUE_DT, UPDATE_DT, PASSPORT_ID, VALUE_CLOB, ATTRIBUTE_BATCH_ID, RELATED_OBJECT_TABLE)
    VALUES
      (S_CONTRACT_ATTRIBUTES_ID.nextval, NULL, 'LINK_CONTRACT_ID', NULL, NULL, {1}, NULL, sysdate, NULL, NULL, {0}, 'T_CONTRACT_COLLATERAL')'''.format(
            attribute_batch_id, linked_contract))
        ContractSteps.refresh_contracts_cache(main_contract)


    @staticmethod
    def insert_attribute(contract_id, attribute_name, key_num=None, value_str=None,
                         value_num=None, value_dt=None, ):
        attribute_batch_id = db.get_collaterals_by_contract(contract_id)[0]['attribute_batch_id']
        collateral_id = db.balance().execute("select id from t_contract_collateral where "
                                             "contract2_id = :contract_id and num is null", {'contract_id': contract_id})[0]['id']
        query = '''INSERT INTO T_CONTRACT_ATTRIBUTES (id, collateral_id, dt, code, KEY_NUM, VALUE_STR, VALUE_NUM, VALUE_DT,
                  UPDATE_DT, PASSPORT_ID, VALUE_CLOB, ATTRIBUTE_BATCH_ID, RELATED_OBJECT_TABLE)
                  VALUES
                  (S_CONTRACT_ATTRIBUTES_ID.nextval, :collateral_id, NULL, :code, :key_num, :value_str, :value_num, :value_dt,
                  sysdate, NULL, NULL, :attribute_batch_id, 'T_CONTRACT_COLLATERAL')'''
        params = {
            'attribute_batch_id': attribute_batch_id,
            'code': attribute_name,
            'key_num': key_num,
            'value_str': value_str,
            'value_num': value_num,
            'value_dt': value_dt,
            'collateral_id': collateral_id
        }
        db.balance().execute(query, params)
        ContractSteps.refresh_contracts_cache(contract_id)


    @staticmethod
    def update_contract_end_dt(contract_id, end_date):
        with reporter.step(u"Устанавливаем дату для параметра end_dt для договора"):
            reporter.attach(u"ID договора", utils.Presenter.pretty(contract_id))

            end_date = utils.Date.nullify_time_of_date(end_date)
            reporter.attach(u"Дата окончания договора", utils.Presenter.pretty(end_date))

            # todo-blubimov нужно использовать get_collateral_id(), т.к. сейчас все упадет при наличии ДС у договора
            query = "UPDATE t_contract_attributes SET value_dt = :dt " \
                    "WHERE attribute_batch_id = (SELECT attribute_batch_id FROM t_contract_collateral" \
                    " WHERE contract2_id = :contract_id) AND code = 'FINISH_DT'"
            params = {'contract_id': contract_id, 'dt': end_date}
            db.balance().execute(query, params)
            ContractSteps.refresh_contracts_cache(contract_id)

    @staticmethod
    def update_contract_start_dt(contract_id, start_date):
        with reporter.step(u"Устанавливаем дату для параметра start_dt для договора"):
            reporter.attach(u"ID договора", utils.Presenter.pretty(contract_id))

            start_date = utils.Date.nullify_time_of_date(start_date)
            reporter.attach(u"Дата начала договора", utils.Presenter.pretty(start_date))

            # todo-blubimov нужно использовать get_collateral_id()
            query = "UPDATE t_contract_collateral SET dt = :dt WHERE contract2_id = :contract_id"
            params = {'contract_id': contract_id, 'dt': start_date}
            db.balance().execute(query, params)
            ContractSteps.refresh_contracts_cache(contract_id)

    @staticmethod
    def update_contract_is_signed(contract_id, contract_date):
        with reporter.step(u"Устанавливаем дату для параметра is_signed для договора"):
            reporter.attach(u"ID договора", utils.Presenter.pretty(contract_id))

            contract_date = utils.Date.nullify_time_of_date(contract_date)
            reporter.attach(u"Дата подписания", utils.Presenter.pretty(contract_date))

            # todo-blubimov нужно использовать get_collateral_id()
            query = "UPDATE t_contract_collateral SET is_signed = :dt WHERE contract2_id = :contract_id"
            params = {'dt': contract_date, 'contract_id': contract_id}
            db.balance().execute(query, params)
            ContractSteps.refresh_contracts_cache(contract_id)

    @staticmethod
    def contract_notify(contracts):
        for contract_id in contracts.keys():
            common_steps.CommonSteps.export('CONTRACT_NOTIFY', 'Contract', contract_id)
        time.sleep(2)
        for contract_id in contracts.keys():
            common_steps.CommonSteps.wait_for(
                'SELECT state AS val FROM t_export WHERE object_id = :contract_id AND type  = \'CONTRACT_NOTIFY\' AND classname = \'Contract\'',
                {'contract_id': contract_id}, 1, interval=1, timeout=420)

    @staticmethod
    def contract_notify_fair(contract_id):
        db.balance().execute('UPDATE t_job SET next_dt=sysdate-1 WHERE id=\'contract_notify_enqueue\'')
        db.balance().execute(
            'UPDATE (SELECT * FROM T_PYCRON_State  WHERE id = (SELECT state_id FROM V_PYCRON WHERE name = \'balance-contract-notify-enqueue\')) SET started = NULL')
        common_steps.CommonSteps.wait_for(
            'SELECT state AS val FROM t_export WHERE object_id = :contract_id AND type  = \'CONTRACT_NOTIFY\' AND classname = \'Contract\'',
            {'contract_id': contract_id}, 0, interval=5, timeout=300)
        db.balance().execute(
            'UPDATE (SELECT * FROM t_export WHERE OBJECT_ID = :contract_id AND type = \'CONTRACT_NOTIFY\' AND classname = \'Contract\') SET priority=-1',
            {'contract_id': contract_id})

        common_steps.CommonSteps.wait_for_export('CONTRACT_NOTIFY', contract_id)

    @staticmethod
    def contract_notify_flipping_dates(contract_id, checkbox, _type, is_cancelling_col=False):
        dates = {}
        dates[1] = utils.Date.shift_date(datetime.datetime.now(), days=-31)  # 1 month (2*15)
        dates[2] = utils.Date.shift_date(dates[1], days=-15)  # 1.5 months (3*15)
        dates[3] = utils.Date.shift_date(dates[2], days=-15)  # 2 months (4*15)
        dates[4] = utils.Date.shift_date(dates[3], days=-15)  # 2.5 months (5*15)

        if is_cancelling_col:
            dates[5] = utils.Date.shift_date(dates[4], days=-15)  # (6*15)
        else:
            dates[5] = utils.Date.shift_date(datetime.datetime.now(), months=-3, days=-1)  # 3 months

        # todo-blubimov можно заиспользовать get_collateral_id()
        # reporter.log(dates)
        mapper_contract = {
            'is_faxed': "UPDATE (SELECT * FROM T_CONTRACT_COLLATERAL WHERE CONTRACT2_ID  = :contract_id AND num IS NULL) SET is_faxed =:update_dt",
            'not_signed': "UPDATE (SELECT * FROM T_CONTRACT_COLLATERAL WHERE CONTRACT2_ID  = :contract_id AND num IS NULL) SET dt =:update_dt",
            'is_booked': "UPDATE (SELECT * FROM T_CONTRACT_COLLATERAL c JOIN t_contract_attributes a ON c.attribute_batch_id = a.attribute_batch_id WHERE c.contract2_id =:contract_id AND code = 'IS_BOOKED_DT' AND num IS NULL) SET value_dt  =  :update_dt"
        }
        mapper_collateral = {
            'is_faxed': "UPDATE (SELECT * FROM T_CONTRACT_COLLATERAL WHERE CONTRACT2_ID   = :contract_id AND num ='01') SET is_faxed =:update_dt",
            'not_signed': "UPDATE (SELECT * FROM T_CONTRACT_COLLATERAL WHERE CONTRACT2_ID   = :contract_id AND num='01') SET dt =:update_dt",
            'is_booked': "UPDATE (SELECT * FROM T_CONTRACT_COLLATERAL c JOIN t_contract_attributes a ON c.attribute_batch_id = a.attribute_batch_id WHERE c.contract2_id   = :contract_id  AND code = 'IS_BOOKED_DT' AND num='01') SET value_dt  = :update_dt"
        }
        for update_dt in dates.values():
            # reporter.log(update_dt)
            mapping = mapper_collateral if _type else mapper_contract

            db.balance().execute(mapping[checkbox],
                                 {'contract_id': contract_id, 'update_dt': update_dt},
                                 descr='сдвигаем дату {0} договора на {1}'.format(checkbox,
                                                                                  update_dt))
            # номер 15ти дневного интервала попадает в input ответа
            # ContractSteps.contract_notify_fair(contract_id)  ##честная выгрузка через очередь (долго)
            common_steps.CommonSteps.export('CONTRACT_NOTIFY', 'Contract', contract_id)
        ContractSteps.refresh_contracts_cache(contract_id)

    @staticmethod
    def contract_notify_check(contract_id):
        dt = db.balance().execute(
            """SELECT atr.VALUE_DT AS dt
            FROM T_CONTRACT_COLLATERAL col
            JOIN T_CONTRACT_ATTRIBUTES atr ON col.attribute_batch_id = atr.attribute_batch_id
            WHERE col.CONTRACT2_ID = :contract_id AND atr.code = 'IS_SUSPENDED'""", {'contract_id': contract_id},
            descr='запрашиваем дату приостановления договора')
        cnt = db.balance().execute(
            """SELECT count(id) AS cnt
            FROM T_MESSAGE
            WHERE OBJECT_ID = :contract_id AND dt>sysdate-0.05""", {'contract_id': contract_id},
            descr='запрашиваем количество писем по договору')
        return dt, cnt

    @staticmethod
    def create_offer(params, passport_uid=defaults.PASSPORT_UID):
        with reporter.step(u'Создаем договор через CreateOffer'):
            result = api.medium().CreateOffer(passport_uid, params)
            contract_id, contract_eid = result['ID'], result['EXTERNAL_ID']

            ContractSteps.report_url(contract_id)

        # Для коллег из OEBS: публикуем ID объекта в отчёте, выгружаем объект в OEBS и публикуем лог выгрузки
        if balance_config.TRUST_ME_I_AM_OEBS_QA:
            export_steps.ExportSteps.extended_oebs_contract_export(contract_id,
                                                                   params.get('person_id', None))

        return contract_id, contract_eid


    @staticmethod
    def create_partner_contract(context, client_id=None, person_id=None, is_postpay=1,
                                remove_params=None, additional_params=None, is_offer=False,
                                unsigned=False, contract_template=None, full_person_params=False,
                                omit_currency=False, partner_integration_params=None):
        partner_integration_params = partner_integration_params or context.partner_integration_params
        if partner_integration_params:
            partner_integration_params = copy.deepcopy(partner_integration_params)

        is_offer = is_offer if context.is_offer is None else context.is_offer

        client_id = client_id or client_steps.ClientSteps.create()

        if context.client_intercompany:
            common_steps.CommonSteps.set_extprops(
                'Client', client_id, 'intercompany', {'value_str': context.client_intercompany}
            )

        ################# Магия интеграций #################################################
        # по дефолту надо передавать только флаги create_integration, create_configuration,
        # link_integration_to_client, set_integration_to_contract. Все объекты сгенерируются автоматически,
        # привяжутся к клиенту и проставятся в договор. Сама схема интеграции при этом должна быть записана
        # в context.partner_integration['scheme'] (см. SUPERCHECK_CONTEXT)
        # Полный дикт, если создается все руками, выглядит так:
        # {
        #     'create_integration': 1,
        #     'create_integration_args': {
        #         'params': {
        #             'cc': 'INTEGRATION_NAME',
        #             'display_name': 'INTEGRATION_DISPLAY_NAME'
        #         },
        #         'passport_uid': 666,
        #     },
        #     'create_configuration': 1,
        #     'create_configuration_args': {
        #         'integration_cc': 'INTEGRATION_NAME',
        #         'params': {
        #             'cc': 'CONFIGURATION_NAME',
        #             'display_name': 'CONFIGURATION_DISPLAY_NAME',
        #             'scheme': 'JSON С КОНФИГУРАЦИЕЙ',
        #         },
        #     },
        #     'link_integration_to_client': 1,
        #     'link_integration_to_client_args': {
        #         'integration_cc': 'INTEGRATION_NAME',
        #         'configuration_cc': 'CONFIGURATION_NAME',
        #         'client_id': 666,
        #     },
        #     'set_integration_to_contract': 1,
        #     'set_integration_to_contract_params': {
        #         'integration_cc': 'INTEGRATION_NAME',
        #     },
        #  }

        _integration_cc_for_contract = None
        if partner_integration_params:
            _req_create_integration = {}
            if partner_integration_params.get('create_integration'):
                create_integration_args = partner_integration_params.get('create_integration_args', {})
                _req_create_integration, _code, _status = \
                    integration_steps.CommonIntegrationSteps.create_integration(**create_integration_args)
                assert _code == 0, (_code, _status)

            _req_create_configuration = {}
            if partner_integration_params.get('create_configuration'):
                create_configuration_args = partner_integration_params.get('create_configuration_args', {})
                if 'integration_cc' not in create_configuration_args:
                    _integration_cc = _req_create_integration.get('cc')
                    assert _integration_cc
                    create_configuration_args['integration_cc'] = _integration_cc
                create_configuration_args_PARAM_ARG = create_configuration_args.setdefault('params', {})
                create_configuration_args_PARAM_ARG.setdefault('scheme', context.partner_integration['scheme'])
                _req_create_configuration, _code, _status = \
                    integration_steps.CommonIntegrationSteps.create_integrations_configuration(**create_configuration_args)
                assert _code == 0, (_code, _status)

            _req_link_integration_to_client = {}
            if partner_integration_params.get('link_integration_to_client'):
                link_integration_to_client_args = \
                    partner_integration_params.get('link_integration_to_client_args', {})

                link_integration_to_client_args.setdefault('client_id', client_id)
                if 'integration_cc' not in link_integration_to_client_args:
                    integration_cc = _req_create_integration.get('cc')
                    assert integration_cc
                    link_integration_to_client_args['integration_cc'] = integration_cc
                if 'configuration_cc' not in link_integration_to_client_args:
                    configuration_cc = _req_create_configuration.get('cc')
                    assert configuration_cc
                    link_integration_to_client_args['configuration_cc'] = configuration_cc
                _req_link_integration_to_client, _code, _status = \
                    integration_steps.CommonIntegrationSteps.link_integration_configuration_to_client(**link_integration_to_client_args)
                assert _code == 0, (_code, _status)

            if partner_integration_params.get('set_integration_to_contract'):
                _integration_cc_for_contract = partner_integration_params.get('set_integration_to_contract_params', {})\
                    .get('integration_cc')
                if not _integration_cc_for_contract:
                    _integration_cc_for_contract = _req_create_integration.get('cc')
                    assert _integration_cc_for_contract


        is_partner = context.contract_type in (ContractSubtype.SPENDABLE, ContractSubtype.PARTNERS)
        person_id = person_id or person_steps.PersonSteps.create(client_id, context.person_type.code,
                                                                 {'is-partner': str(1 if is_partner else 0)},
                                                                 inn_type=person_defaults.InnType.RANDOM,
                                                                 full=full_person_params)
        services = context.contract_services or [context.service.id]

        params = {
            'client_id': client_id,
            'person_id': person_id,
            'manager_uid': Managers.SOME_MANAGER.uid,
            'firm_id': context.firm.id,
        }

        if context.contract_type in (ContractSubtype.GENERAL, ContractSubtype.SPENDABLE):
            params['services'] = services

        if not omit_currency:
            params['currency'] = context.currency.char_code

        if context.contract_type == ContractSubtype.GENERAL:
            if is_postpay:
                params.update({'payment_term': 10, 'payment_type': 3})
            else:
                params.update({'payment_type': 2})

        if context.contract_type in (ContractSubtype.SPENDABLE, ContractSubtype.PARTNERS):
            params.update({'nds': context.nds.nds_id})

        if context.special_contract_params:
            params.update(context.special_contract_params)

        if remove_params:
            for key in remove_params:
                params.pop(key, None)

        if is_offer and context.contract_type == ContractSubtype.GENERAL:
            params.update({'offer_confirmation_type': OfferConfirmationType.NO.value})

        if additional_params:
            params.update(additional_params)

        # не все параметры поддерживаются в CreateCommonContract,
        # поэтому для некоторых севрисов используем старый добрый CreateContract с шаблонами :(
        if context.use_create_contract:
            if not contract_template and context.contract_type == ContractSubtype.GENERAL:
                contract_template = 'general_default'
                params.update({'PARTNER_CREDIT': 1})
                if is_postpay:
                    params.update({'CREDIT_TYPE': 2})
            elif not contract_template and context.contract_type == ContractSubtype.SPENDABLE:
                contract_template = 'spendable_default'
            elif not contract_template and context.contract_type == ContractSubtype.PARTNERS:
                contract_template = 'rsya_ssp'
            if _integration_cc_for_contract:
                # TODO Не тестировалось создание договоров с интеграцией через темплейт!
                params.update({'INTEGRATION': _integration_cc_for_contract})
            contract_id, contract_eid = ContractSteps.create_partner_contract_by_template(context, params,
                                                                                          contract_template,
                                                                                          unsigned=unsigned)
        else:
            if _integration_cc_for_contract:
                params.update({'integration': _integration_cc_for_contract})
            if not unsigned:
                params.update({'signed': 1})

            if is_offer:
                contract_id, contract_eid = ContractSteps.create_offer(params)
            else:
                contract_id, contract_eid = ContractSteps.create_common_contract(params)

        if balance_config.TRUST_ME_I_AM_OEBS_QA:
            invoices = db.balance().execute("select id, person_id from t_invoice where contract_id = {} "
                                            "and type not in ('charge_note', 'fictive')".format(contract_id))
            for invoice in invoices:
                export_steps.ExportSteps.extended_oebs_invoice_export(invoice['id'], invoice['person_id'])

        return client_id, person_id, contract_id, contract_eid


    @staticmethod
    # используется только в create_partner_contract, не надо его использовать напрямую!!!!!!!
    # используй create_partner_contract
    def create_partner_contract_by_template(context, params, contract_template, unsigned=False):
        params = {k.upper(): v for k, v in params.items()}
        params['FIRM'] = params.pop('FIRM_ID')
        params['DT'] = params.pop('START_DT')

        if params.has_key('MANAGER_UID'):
            manager_code = db.balance().execute("select manager_code from t_manager where passport_id = :manager_uid or "
                       "domain_passport_id = :manager_uid and rownum = 1",
                       {'manager_uid': params['MANAGER_UID']})[0]['manager_code']
            params.update({'MANAGER_CODE': manager_code})
            params.pop('MANAGER_UID')

        if context.contract_type in (ContractSubtype.SPENDABLE, ContractSubtype.PARTNERS):
            params['CURRENCY'] = context.currency.iso_num_code
        else:
            params['CURRENCY'] = context.currency.num_code
        if unsigned:
            params.update({'IS_SIGNED': None})
        contract_id, external_contract_id = ContractSteps.create_contract(contract_template, params=params)
        return contract_id, external_contract_id


    @staticmethod
    def create_common_contract(params, passport_uid=defaults.PASSPORT_UID):
        with reporter.step(u'Создаем договор через CreateCommonContract'):
            result = api.medium().CreateCommonContract(passport_uid, params)
            contract_id, contract_eid = result['ID'], result['EXTERNAL_ID']

            ContractSteps.report_url(contract_id)

        # Для коллег из OEBS: публикуем ID объекта в отчёте, выгружаем объект в OEBS и публикуем лог выгрузки
        if balance_config.TRUST_ME_I_AM_OEBS_QA:
            export_steps.ExportSteps.extended_oebs_contract_export(contract_id,
                                                                   params.get('person_id', None))

        return contract_id, contract_eid

    @staticmethod
    def update_offer_projects(params, passport_uid=defaults.PASSPORT_UID):
        return api.medium().UpdateProjects(passport_uid, params)

    @staticmethod
    def get_contract_credits_detailed(contract_id, params=None, extra_params={}):
        params_dict = {'ContractID': contract_id}
        return api.medium().GetContractCreditsDetailed(params_dict, extra_params)

    @staticmethod
    def accept_taxi_offer(person_id, contract_id, nds=Nds.DEFAULT, passport_uid=defaults.PASSPORT_UID):
        with reporter.step(u'Создаем расходный договор через AcceptTaxiOffer'):
            params = {
                'contract_id': contract_id,
                'person_id': person_id,
                'nds': nds
            }

            result = api.medium().AcceptTaxiOffer(passport_uid, params)
            contract_id, contract_eid = result['ID'], result['EXTERNAL_ID']

            ContractSteps.report_url(contract_id)

        # Для коллег из OEBS: публикуем ID объекта в отчёте, выгружаем объект в OEBS и публикуем лог выгрузки
        if balance_config.TRUST_ME_I_AM_OEBS_QA:
            export_steps.ExportSteps.extended_oebs_contract_export(contract_id,
                                                                   person_id)

        return contract_id, contract_eid

    @staticmethod
    def get_attribute(contract_id, atr_type, atr_name, only_first=True):
        with reporter.step(u'Получаем {} параметр с типом {} договора: {}'.format(atr_name, atr_type, contract_id)):
            query = "SELECT {atr_type} FROM T_CONTRACT2 c " \
                    "JOIN T_CONTRACT_COLLATERAL cc ON c.ID=cc.CONTRACT2_ID " \
                    "JOIN T_CONTRACT_ATTRIBUTES ca ON ca.attribute_batch_id=cc.attribute_batch_id " \
                    "WHERE c.ID=:contract_id AND ca.CODE=:atr_name".format(atr_type=atr_type)
            params = {
                'contract_id': contract_id,
                'atr_name': atr_name
            }

            result = db.balance().execute(query, params)
            return result[0][atr_type] if only_first else [row[atr_type] for row in result]

    @staticmethod
    def get_attribute_collateral(collateral_id, atr_type, atr_name, return_value=True):
        with reporter.step(u'Получаем {} параметр с типом {} допника: {}'.format(atr_name, atr_type, collateral_id)):
            query = "SELECT {atr_type} FROM T_CONTRACT_COLLATERAL cc " \
                    "JOIN T_CONTRACT_ATTRIBUTES ca ON ca.attribute_batch_id=cc.attribute_batch_id " \
                    "WHERE cc.ID=:collateral_id AND ca.CODE=:atr_name".format(atr_type=atr_type)
            params = {
                'collateral_id': collateral_id,
                'atr_name': atr_name
            }

            result = db.balance().execute(query, params)
            return result[0][atr_type] if return_value else result

    @staticmethod
    def update_collateral_dt(contract_id, dt, collateral_type):
        query = "UPDATE t_contract_collateral SET dt = :dt WHERE contract2_id = :contract_id AND collateral_type_id = :collateral_type"
        params = {'contract_id': contract_id, 'dt': dt, 'collateral_type': collateral_type}
        db.balance().execute(query, params)
        ContractSteps.refresh_contracts_cache(contract_id)


    @staticmethod
    def count_contracts(client_id):
        with reporter.step(u"Подсчитываем договора для клиента: {}".format(client_id)):
            query = "SELECT COUNT(*) cnt FROM T_CONTRACT2 WHERE CLIENT_ID=:client_id"
            params = {'client_id': client_id}
            return db.balance().execute(query, params)[0]['cnt']

    @staticmethod
    def create_brand_contract(brand_client_id, client_id, brand_type=BrandType.DIRECT_TECH, dt=NOW,
                              finish_dt=defaults.Date.HALF_YEAR_AFTER_TODAY, is_signed_dt=NOW, force_dt=True):

        to_iso = lambda x: utils.Date.to_iso(utils.Date.nullify_time_of_date(x))

        with reporter.step(u'Создаем договор о рекламном бренде'):
            contract_params = {'CLIENT_ID': brand_client_id,
                               'DT': to_iso(NOW + relativedelta(days=2) if force_dt else dt),
                               'IS_SIGNED': to_iso(NOW if force_dt else is_signed_dt),
                               'FINISH_DT': to_iso(finish_dt + datetime.timedelta(days=3) if force_dt else finish_dt),
                               'BRAND_TYPE': brand_type,
                               'BRAND_CLIENTS': json.dumps(
                                   [{"id": "1", "num": brand_client_id, "client": brand_client_id},
                                    {"id": "2", "num": client_id, "client": client_id}])}
            brand_contract_id, contract_external_id = ContractSteps.create_contract_new(ContractCommissionType.BRAND,
                                                                                        contract_params)

        # Сейчас дата начала договора в будущем (иначе договор не создается), апдейтом меняем ее на нужную
        dt = utils.Date.nullify_time_of_date(dt)
        if force_dt:
            report_msg = u'Меняем дату начала договора на {date}, дату конца - на {finish_dt}'\
                .format(date=dt, finish_dt=finish_dt)
            with reporter.step(report_msg):
                db.balance().execute("UPDATE t_contract_collateral SET dt = :dt WHERE contract2_id = :contract_id",
                                     {'dt': dt, 'contract_id': brand_contract_id})

                batch_id = db.get_collaterals_by_contract(brand_contract_id)[0]['attribute_batch_id']
                db.balance().execute(
                    "update T_CONTRACT_ATTRIBUTES set value_dt= :finish_dt where ATTRIBUTE_BATCH_ID = :batch_id and code = 'FINISH_DT'",
                    {'batch_id': batch_id, 'finish_dt': finish_dt})

                db.balance().execute(
                    """UPDATE bo.t_client_link_history
                    SET from_dt=:dt, till_dt=:finish_dt
                    WHERE group_id=:id AND group_type=:brand_type""",
                    {
                        'id': brand_contract_id,
                        'brand_type': brand_type,
                        'dt': dt,
                        'finish_dt': finish_dt,
                    }
                )
                ContractSteps.refresh_contracts_cache(brand_contract_id)

        reporter.attach(u'[ID] contract', utils.Presenter.pretty(brand_contract_id))

        return brand_contract_id, contract_external_id

    @staticmethod
    def force_convert_to_fictive_credit_scheme(contract_id):
        with reporter.step(u'Форсированно меняем схему договора на старую кредитную схему с фиктивными счетами'):
            query = 'update T_CONTRACT_ATTRIBUTES ' \
                    'set VALUE_NUM = 0 ' \
                    'where attribute_batch_id = ' \
                    '(select attribute_batch_id from t_contract_collateral where contract2_id = :item) ' \
                    'AND CODE IN (\'LIFT_CREDIT_ON_PAYMENT\', \'PERSONAL_ACCOUNT\', \'PERSONAL_ACCOUNT_FICTIVE\')'
            db.balance().execute(query, {'item': contract_id})
            ContractSteps.refresh_contracts_cache(contract_id)
