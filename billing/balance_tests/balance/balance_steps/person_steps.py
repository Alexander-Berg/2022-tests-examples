# coding=utf-8
__author__ = 'igogor'

import random
import datetime

import balance.balance_api as api
import balance.balance_db as db
import btestlib.environments as env
import btestlib.reporter as reporter
import btestlib.utils as utils
from btestlib.data import person_defaults, defaults

log_align = 30
# log = reporter.logger()

# еще дефолтные даты есть в btestlib.data.defaults.Date
to_iso = utils.Date.date_to_iso_format
NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
A_DAY_AFTER_TOMORROW = NOW + datetime.timedelta(days=2)


class PersonSteps(object):
    # TODO параметры
    @staticmethod
    def create(client_id,
               type_,
               params=None,
               passport_uid=defaults.PASSPORT_UID,
               inn_type=None,  # type: person_defaults.InnType
               full=False,
               name_type=None,  # type: person_defaults.NameType
               pfr_type=None,  # type: person_defaults.PfrType
               account_type=None,  # type: person_defaults.AccountType
               birthday_type=None,  # type: person_defaults.BirthdayType
               strict_params=False,  # type: bool
               custom_request_params=None,  # type: dict
               ):

        main_params = {
            'client_id': client_id,
            'type': type_
        }
        create_params = {
            'person_id': 0,
        }

        is_partner = params and params.get('is-partner', '0') == '1'
        default_details = person_defaults.get_details(type_, is_partner=is_partner, full=full)

        request_params = {}
        request_params.update(default_details)
        request_params.update(create_params)

        inn_params = person_defaults.get_inn_params(type_, is_partner, inn_type)
        request_params.update(inn_params)

        name_params = person_defaults.get_uniq_name(request_params.get('name'), request_params.get('fname'),
                                                    request_params.get('mname'), request_params.get('lname'),
                                                    name_type)
        request_params.update(name_params)

        if 'pfr' in request_params:
            request_params['pfr'] = person_defaults.get_pfr_number(request_params.get('pfr'), pfr_type)
        if 'account' in request_params:
            request_params['account'] = person_defaults.get_account(type_, request_params.get('account'), account_type,
                                                                    request_params.get('bik'))
        if 'birthday' in request_params:
            request_params['birthday'] = person_defaults.get_birthday(request_params.get('birthday'), birthday_type)

        if params is not None:
            request_params.update(params)

        if strict_params:
            request_params = params

        request_params.update(main_params)
        request_params.update(custom_request_params or {})

        print request_params
        with reporter.step(u"Создаем плательщика с типом '{0}' для клиента с client_id: {1}".format(type_, client_id)):
            person_id = api.medium().CreatePerson(passport_uid, request_params)
            reporter.attach(u'Кастомные параметры',
                            utils.dicts_diff(request_params, utils.merge_dicts([default_details, create_params])))
            reporter.attach(u"person_id", person_id)
            persons_page_url = '{base_url}/subpersons.xml?tcl_id={client_id}'.format(
                base_url=env.balance_env().balance_ai, client_id=client_id)
            reporter.report_url(u'Ссылка на плательщика', persons_page_url)
            return person_id

    @staticmethod
    def create_partner(client_id, type_, params=None, passport_uid=defaults.PASSPORT_UID, inn_type=None,
                       name_type=None, full=False):
        params = utils.merge_dicts([params, {'is-partner': '1'}])
        return PersonSteps.create(client_id, type_, params, passport_uid, inn_type, name_type=name_type, full=full)

    @staticmethod
    def hide_person(person_id):
        with reporter.step(u'Архивируем плательщика {0}'.format(person_id)):
            db.balance().execute('UPDATE t_person SET hidden = 1 WHERE id = :id', {'id': person_id})

    @staticmethod
    def unhide_person(person_id):
        with reporter.step(u'Разархивируем плательщика {0}'.format(person_id)):
            db.balance().execute('UPDATE t_person SET hidden = 0 WHERE id = :id', {'id': person_id})

    @staticmethod
    def accept_edo(person_id, firm_id, from_dt, edo_type_id=1, offer_type=1, can_use_new_edo=True, to_dt=None, status='FRIENDS'):
        person = db.get_person_by_id(person_id)
        person_inn = person[0]['inn']
        person_kpp = person[0]['kpp']
        query = """
            INSERT INTO t_edo_offer (
                person_inn, person_kpp, firm_id, edo_type_id, status, blocked, default_flag, enabled_flag,
                active_start_date, active_end_date, org_orarowid, inv_orarowid
            )
            VALUES (
                :person_inn, :person_kpp, :firm_id, :edo_type_id, :status, :blocked, :default_flag, :enabled_flag,
                :active_start_date, :active_end_date, :org_orarowid, :inv_orarowid
            )"""
        query_params = {
            'org_orarowid': str(random.randint(1, 1000000)), 'inv_orarowid': str(random.randint(1, 1000000)),
            'person_inn': person_inn, 'person_kpp': person_kpp, 'firm_id': firm_id, 'edo_type_id': edo_type_id,
            'status': status, 'blocked': 0, 'default_flag': 1, 'enabled_flag': 1,
            'active_start_date': from_dt, 'active_end_date': to_dt
        }

        db.balance().execute(query, query_params)

    @staticmethod
    def get_edo_by_firm(person_id, firm_id):
        firm_inn_kpp = db.balance().execute('''SELECT max(rec.inn) AS inn, max(rec.kpp) AS kpp, f.id FROM MV_YANDEX_RECS rec
                                FULL JOIN T_FIRM_EXPORT fe ON fe.OEBS_ORG_ID=rec.org_id
                                INNER JOIN t_firm f ON f.id = fe.firm_id WHERE f.id = :firm_id GROUP BY f.id''',
                                            {'firm_id': firm_id})
        if firm_inn_kpp:
            firm_inn = firm_inn_kpp[0]['inn']
            firm_kpp = firm_inn_kpp[0]['kpp']
        else:
            return
        person = db.get_person_by_id(person_id)
        person_inn = person[0]['inn']
        person_kpp = person[0]['kpp']

        query = 'SELECT * FROM t_edo_offer_cal where firm_kpp = :firm_kpp and  firm_inn = :firm_inn and ' \
                'person_inn=:person_inn and person_kpp =:person_kpp'
        query_params = {'person_inn': person_inn, 'person_kpp': person_kpp, 'firm_inn': firm_inn,
                        'firm_kpp': firm_kpp}

        return db.balance().execute(query, query_params)

    @staticmethod
    def refuse_edo(person_id, from_dt, firm_id):
        person = db.get_person_by_id(person_id)
        person_inn = person[0]['inn']
        person_kpp = person[0]['kpp']
        firm_inn_kpp = db.balance().execute('''SELECT max(rec.inn) AS inn, max(rec.kpp) AS kpp, f.id FROM MV_YANDEX_RECS rec
                                FULL JOIN T_FIRM_EXPORT fe ON fe.OEBS_ORG_ID=rec.org_id
                                INNER JOIN t_firm f ON f.id = fe.firm_id WHERE f.id = :firm_id GROUP BY f.id''',
                                            {'firm_id': firm_id})

        if firm_inn_kpp:
            firm_inn = firm_inn_kpp[0]['inn']
            firm_kpp = firm_inn_kpp[0]['kpp']
        query = "UPDATE t_edo_offer_cal SET edo_type_id = NULL, from_dt=:from_dt \
                WHERE person_inn = :person_inn AND person_kpp = :person_kpp AND firm_inn=:firm_inn AND firm_kpp=:firm_kpp"
        query_params = {'person_inn': person_inn, 'person_kpp': person_kpp, 'from_dt': from_dt, 'firm_inn': firm_inn,
                        'firm_kpp': firm_kpp}

        db.balance().execute(query, query_params)

    @staticmethod
    def clean_up_edo(person_id):
        person = db.get_person_by_id(person_id)
        person_inn = person[0]['inn']
        person_kpp = person[0]['kpp']
        query = "DELETE FROM t_edo_offer_cal \
                WHERE person_inn = :person_inn AND person_kpp = :person_kpp"
        query_params = {'person_inn': person_inn, 'person_kpp': person_kpp}

        db.balance().execute(query, query_params)
