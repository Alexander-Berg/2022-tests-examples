# coding=utf-8
__author__ = 'igogor'

import datetime
import json
import math
import time
import common_steps
import overdraft_steps
import export_steps
import person_steps
import campaigh_steps
import contract_steps
import acts_steps
from invoice_steps import InvoiceSteps
import balance.balance_api as api
import balance.balance_db as db
import btestlib.environments as env
import btestlib.reporter as reporter
import btestlib.utils as utils
from btestlib.constants import Export
from btestlib.data import defaults
from btestlib.data.defaults import Client
import btestlib.config as balance_config

log_align = 30
# log = reporter.logger()

# еще дефолтные даты есть в btestlib.data.defaults.Date
to_iso = utils.Date.date_to_iso_format
NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
A_DAY_AFTER_TOMORROW = NOW + datetime.timedelta(days=2)


class ClientSteps(object):
    @staticmethod
    def create(params=None, passport_uid=defaults.PASSPORT_UID, prevent_oebs_export=False,
               enable_single_account=None, single_account_activated=None):
        request_params = Client.default_params()
        if params:
            request_params.update(params)
        is_agency = request_params.get('IS_AGENCY', 0) != 0
        is_edit_mode = request_params.get('CLIENT_ID', 0) != 0
        with reporter.step(
                u"{0} {1}{2}, {3}".format(u'Создаем' if not is_edit_mode else u'Редактируем',
                                          u"агентство" if is_agency else u'клиента',
                                          u' {}'.format(request_params.get('CLIENT_ID')) if is_edit_mode else u'',
                                          u"Регион: {}".format(request_params.get('REGION_ID', 'None')))):
            code, status, client_id = api.medium().CreateClient(passport_uid, request_params)

            client_page_url = '{base_url}/passports.xml?tcl_id={client_id}'.format(
                base_url=env.balance_env().balance_ai, client_id=client_id)

            reporter.attach(u'agency_id' if is_agency else u'client_id', client_id)
            reporter.report_url(u'Ссылка на клиента', client_page_url)

        if prevent_oebs_export:
            export_steps.ExportSteps.prevent_auto_export(client_id, Export.Classname.CLIENT)

        def enable_els():
            with reporter.step(u"Переносим дату создания клиента в будущее"):
                query = "update bo.t_client set creation_dt = date'2030-01-01' where id = :id"
                db.balance().execute(query, {'id': client_id})

        def activate_els():
            with reporter.step(u"Включаем клиенту ЕЛС"):
                single_account_number = api.test_balance().SingleAccountProcessClient(client_id)
                reporter.attach(u'single_account_number', single_account_number)

        if enable_single_account is False:
            with reporter.step(u"Переносим дату создания клиента в прошлое"):
                query = "update bo.t_client set creation_dt = date'2018-01-01' where id = :id"
                db.balance().execute(query, {'id': client_id})
        elif enable_single_account is True:
            enable_els()
            if single_account_activated is True:
                activate_els()
        elif balance_config.ENABLE_SINGLE_ACCOUNT:
            enable_els()
            if balance_config.SINGLE_ACCOUNT_ACTIVATED:
                activate_els()

        # Для коллег из OEBS: публикуем ID объекта в отчёте, выгружаем объект в OEBS и публикуем лог выгрузки
        if balance_config.TRUST_ME_I_AM_OEBS_QA:
            export_steps.ExportSteps.export_oebs(client_id=client_id)

        return client_id

    @staticmethod
    def create_single_account(client_id):
        query = "update bo.t_client set creation_dt = date'2021-01-01' where id = :id"
        db.balance().execute(query, {'id': client_id})
        single_account_number = api.test_balance().SingleAccountProcessClient(client_id)
        return single_account_number

    @staticmethod
    def create_sub_client_non_resident(non_resident_currency, params=None, passport_uid=defaults.PASSPORT_UID):
        client_params = Client.default_params()
        client_params.update({'NON_RESIDENT_CURRENCY': non_resident_currency})
        if params:
            client_params.update(params)
        return ClientSteps.create(client_params, passport_uid=passport_uid)

    @staticmethod
    def get_client_name(client_id):
        return db.balance().execute('SELECT name FROM t_client WHERE id = :client_id', {'client_id': client_id},
                                    single_row=True)['name']

    @staticmethod
    def find_client(params):
        return api.medium().FindClient(params)

    @staticmethod
    def create_multicurrency(params=None, currency_convert_type='MODIFY', dt=None, service_id=7, region_id=225,
                             currency='RUB', passport_uid=defaults.PASSPORT_UID):
        client_id = ClientSteps.create(params=None, passport_uid=defaults.PASSPORT_UID)
        ClientSteps.migrate_to_currency(client_id, currency_convert_type, dt, service_id, region_id, currency)
        return client_id

    @staticmethod
    def create_agency():
        return ClientSteps.create({'IS_AGENCY': 1})

    # todo: сейчас связь создается через БД и если логина нет в t_account - то будет ошибка. Лучше переделать через xmlrpc
    @staticmethod
    def link(client_id, login):
        with reporter.step(u"Привязываем клиента (агенство) (id: {}) к логину '{}'".format(client_id, login)):
            query = 'UPDATE (SELECT * FROM T_ACCOUNT WHERE LOGIN = :login ) SET client_id = :client_id'
            params = {'login': login, 'client_id': client_id}
            db.balance().execute(query, params)
            # reporter.log('Link: {0} -> {1}'.format(login, client_id))

    @staticmethod
    def fair_link(client_id, uid, limited_list=None):
        with reporter.step(
                u"Честно привязываем клиента (агенство) (id: {}) к логину '{}', ограниченный доступ клиентам: {}".format(
                    client_id, uid, limited_list)):
            if limited_list is not None:
                api.medium().CreateUserClientAssociation(defaults.PASSPORT_UID, client_id, uid, limited_list)
            else:
                api.medium().CreateUserClientAssociation(defaults.PASSPORT_UID, client_id, uid)

    @staticmethod
    def unlink_from_login(uid):
        with reporter.step(u"Отвязываем всех клиентов от представителя {}".format(uid)):
            db.balance().execute(
                '''UPDATE (SELECT * FROM t_passport WHERE PASSPORT_ID = :passport_id) SET CLIENT_ID = NULL''',
                {'passport_id': uid})
            db.balance().execute(
                '''DELETE FROM T_ROLE_CLIENT_USER WHERE PASSPORT_ID = :passport_id''',
                {'passport_id': uid})
            db.balance().execute(
                '''DELETE FROM T_SERVICE_CLIENT WHERE PASSPORT_ID = :passport_id''',
                {'passport_id': uid})

    @staticmethod
    def fair_unlink_from_login(client_id, uid):
        with reporter.step(u"Отвязываем клиента {} от представителя честно {}".format(client_id, uid)):
            api.medium().RemoveUserClientAssociation(defaults.PASSPORT_UID, client_id, uid)

    @staticmethod
    def add_accountant_role(user, client_id):
        with reporter.step(u'Добавляем роль бухгалтера для пользователя: {}'.format(user.uid)):
            query = 'INSERT INTO t_role_client_user(ID, PASSPORT_ID, CLIENT_ID, ROLE_ID, CREATE_DT, UPDATE_DT) ' \
                    'VALUES (BO.S_ROLE_CLIENT_USER_ID.nextval,  :passport_id, :client_id, 100, SYSDATE, SYSDATE)'
            params = {'passport_id': user.uid, 'client_id': client_id}
            db.balance().execute(query, params)

    @staticmethod
    def delete_accountant_role(user, client_id):
        with reporter.step(u'Удаляем роль бухгалтера для пользователя: {}'.format(user.uid)):
            query = 'delete from t_role_client_user where passport_id=:passport_id and client_id=:client_id'
            params = {'passport_id': user.uid, 'client_id': client_id}
            db.balance().execute(query, params)

    @staticmethod
    def add_accountant_role_by_login(login, client_id):
        passport_id = db.get_passport_by_login(login)[0]['passport_id']
        with reporter.step(u'Добавляем роль бухгалтера для пользователя: {}'.format(passport_id)):
            query = 'INSERT INTO t_role_client_user(ID, PASSPORT_ID, CLIENT_ID, ROLE_ID, CREATE_DT, UPDATE_DT) ' \
                    'VALUES (BO.S_ROLE_CLIENT_USER_ID.nextval,  :passport_id, :client_id, 100, SYSDATE, SYSDATE)'
            params = {'passport_id': passport_id, 'client_id': client_id}
            db.balance().execute(query, params)

    @staticmethod
    def delete_accountant_role_by_login(login, client_id):
        passport_id = db.get_passport_by_login(login)[0]['passport_id']
        with reporter.step(u'Удаляем роль бухгалтера для пользователя: {}'.format(passport_id)):
            query = 'delete from t_role_client_user where passport_id=:passport_id and client_id=:client_id'
            params = {'passport_id': passport_id, 'client_id': client_id}
            db.balance().execute(query, params)
    @staticmethod
    def delete_every_accountant_role_by_login(login):
        passport_id = db.get_passport_by_login(login)[0]['passport_id']
        with reporter.step(u'Удаляем все роль бухгалтера для пользователя: {}'.format(passport_id)):
            query = 'delete from t_role_client_user where passport_id=:passport_id'
            params = {'passport_id': passport_id}
            db.balance().execute(query, params)

    @staticmethod
    def merge(master, slave, passport_uid=defaults.PASSPORT_UID):
        '''
        Merge 2 clients to make them equal
        '''
        # reporter.log('Clients {0} <- {1} merged'.format(master, slave))
        with reporter.step(u'Объединяем клиентов: главный {0} <= дочерний {1}'.format(master, slave)):
            query = 'UPDATE t_client SET class_id = :master WHERE id = :slave'
            db.balance().execute(query, {'master': master, 'slave': slave})
            # return api.test_balance().MergeClients(passport_uid, master, slave)
            return 'Merged: {0} and {1}'.format(master, slave)

    @staticmethod
    def set_force_overdraft(client_id, service_id, limit, firm_id=1, start_dt=datetime.datetime.now(), currency=None):
        overdraft_steps.OverdraftSteps.set_force_overdraft(client_id, service_id, limit, firm_id, start_dt, currency)

    # TODO: Why +10\-10
    @staticmethod
    # я сделаю лучше
    def set_overdraft(client_id, service_id, limit, firm_id=1, start_dt=datetime.datetime.now(), currency=None,
                      invoice_currency=None):

        def some_days_ago(date, number):
            delta = datetime.timedelta(days=number)
            dt = date - delta
            return (dt)

        dates = {}
        # if datetime.datetime.now().month - 7 <= 0:
        #     BEFORE_6_MONTHS_VALUE = datetime.datetime.now().replace(year=datetime.datetime.now().year - 1,
        #                                                             month=datetime.datetime.now().month + 12 - 7)
        # else:
        #     BEFORE_6_MONTHS_VALUE = datetime.datetime.now().replace(
        #         month=datetime.datetime.now().month - 7)  ## 3 months

        BEFORE_6_MONTHS_VALUE = utils.add_months_to_date(datetime.datetime.now(), -7)

        dates[2] = some_days_ago(BEFORE_6_MONTHS_VALUE, -31)  ## 1.5 months
        dates[3] = some_days_ago(dates[2], -31)  ## 2 months
        dates[4] = some_days_ago(dates[3], -31)  ## 2.5 months
        dates[5] = some_days_ago(dates[4], -31)  ## 2.5 months

        with reporter.step(u'Выдаём честный овердрафт:'):

            FIRM_PARAMS = {1: {'region_id': 225, 'paysys_id': 1003, 'person_type': 'ur', 'currency_product': 503162},
                           2: {'region_id': 187, 'paysys_id': 1017, 'person_type': 'ua', 'currency_product': 503165},
                           111: {'region_id': 225, 'paysys_id': 11101003, 'person_type': 'ur',
                                 'currency_product': 503162},
                           25: {'region_id': 159, 'paysys_id': 2501020, 'person_type': 'kzu',
                                'currency_product': 503166},
                           27: {'region_id': 149, 'paysys_id': 2701101, 'person_type': 'byu',
                                'currency_product': 507529}}

            SERVICE_ID_DIRECT = 7
            PRODUCT_ID_DIRECT = 1475
            PRODUCT_ID_MARKET = 2136

            # If 'currency' param provided we will create invoice and acts with currency product
            if invoice_currency:
                product_id = FIRM_PARAMS[firm_id]['currency_product']
            else:
                product_id = PRODUCT_ID_DIRECT if service_id == SERVICE_ID_DIRECT else PRODUCT_ID_MARKET

            # To get fair overdraft we need to generate 5 acts:
            # 1 full 6 month ago
            # 1 acts per any 4 months of previous 6

            # Calculate parts for 4 acts to reach limit
            qty = (limit * 12) + 10

            # Divide qty to 5 parts: 1 act older 6 months and 4 acts in half-year period
            if currency:
                ClientSteps.create(
                    {'CLIENT_ID': client_id, 'REGION_ID': FIRM_PARAMS[firm_id]['region_id'], 'CURRENCY': currency,
                     'MIGRATE_TO_CURRENCY': start_dt + datetime.timedelta(seconds=5),
                     'SERVICE_ID': service_id, 'CURRENCY_CONVERT_TYPE': 'COPY'})
                common_steps.CommonSteps.export('MIGRATE_TO_CURRENCY', 'Client', client_id)
                money_new = 10
                money = qty / 4
                bucks = bucks_new = 0
            else:
                money = money_new = 0
                bucks_new = 10
                bucks = qty / 4

            # Create invoice and generate act BEFORE 6 months
            person_id = person_steps.PersonSteps.create(client_id, FIRM_PARAMS[firm_id]['person_type'])
            campaigns_list = [
                {'service_id': service_id, 'product_id': product_id, 'qty': qty,
                 'begin_dt': BEFORE_6_MONTHS_VALUE}
            ]

            invoice_id, external_id, total_sum, orders_list = InvoiceSteps.create_force_invoice(client_id, person_id,
                                                                                                campaigns_list,
                                                                                                FIRM_PARAMS[firm_id][
                                                                                                    'paysys_id'],
                                                                                                # utils.add_months_to_date(start_dt, BEFORE_6_MONTHS_VALUE)
                                                                                                BEFORE_6_MONTHS_VALUE
                                                                                                )
            InvoiceSteps.pay(invoice_id, None, None)
            # reporter.attach(u'Дата 6 месяцев назад', BEFORE_6_MONTHS_VALUE)
            campaigh_steps.CampaignsSteps.do_campaigns(service_id, orders_list[0]['ServiceOrderID'],
                                                       {'Money': money_new, 'Bucks': bucks_new},
                                                       campaigns_dt=BEFORE_6_MONTHS_VALUE)
            acts_steps.ActsSteps.generate(client_id, force=1, date=BEFORE_6_MONTHS_VALUE)

            # Generate acts in 4 previous months
            for i in dates:
                money_new += money
                bucks_new += bucks
                # reporter.log(money_new)
                # reporter.log(bucks_new)
                # reporter.log(dates[i])
                campaigh_steps.CampaignsSteps.do_campaigns(service_id, orders_list[0]['ServiceOrderID'],
                                                           {'Money': money_new, 'Bucks': bucks_new},
                                                           campaigns_dt=dates[i])
                acts_steps.ActsSteps.generate(client_id, force=1, date=dates[i])

            # Calculate overdraft
            # api.test_balance().CalculateOverdraft([client_id])
            api.test_balance().Enqueue('Client', client_id, 'OVERDRAFT')
            common_steps.CommonSteps.export('OVERDRAFT', 'Client', client_id)

        reporter.attach('Fair оverdraft given to',
                        '{0} (service: {1}, limit: {2}, firm: {3}, dt: {4}, multicurrency: {5})'.format(
                            client_id, service_id, limit, firm_id, start_dt, currency))

    # TODO: refactor it! Magic numbers, issues with currency is None => null
    @staticmethod
    def set_direct_discount(client_id, dt, pct=None, budget=None, currency='null'):
        with reporter.step(u'Выдаём клиенту скидку:'):
            if pct:
                # sql = "select x+1 as x from bo.t_scale_points where scale_code = 'direct25' and end_dt and hidden = 0 and y = :y and nvl(currency, 'null') = :currency"
                # sql_params = {'y': pct, 'currency': currency or 'null'}
                query = "SELECT x+1 AS x FROM bo.t_scale_points WHERE scale_code = 'direct25' AND end_dt IS NULL AND hidden = 0 AND y = :y AND currency IS NULL"
                query_params = {'y': pct}
                if budget:
                    reporter.attach("Both params specified. 'pct' value will override 'budget'")
                budgets_list = db.balance().execute(query, query_params)
                if len(budgets_list) != 1:
                    raise Exception(u"MTestlib exception: 'Empty or multiple budget values'")
                else:
                    budget = budgets_list[0]['x']
                budget = int(math.ceil((budget / 25.42)))
            # sql = "Insert into bo.t_client_direct_budget (ID,CLIENT_ID,END_DT,CLASSNAME,BUDGET,CURRENCY,UPDATE_DT) values (s_client_direct_budget_id.nextval,:client_id,:end_dt,'DirectDiscountCalculator',:budget,:currency,sysdate)"
            # sql_params = {'client_id': client_id, 'end_dt': dt or datetime.datetime.today().date().replace(day=1), 'budget': budget, 'currency': currency}
            query = "INSERT INTO bo.t_client_direct_budget (ID,CLIENT_ID,END_DT,CLASSNAME,BUDGET,CURRENCY,UPDATE_DT) VALUES (s_client_direct_budget_id.nextval,:client_id,:end_dt,'DirectDiscountCalculator',:budget,NULL,sysdate)"
            query_params = {'client_id': client_id, 'end_dt': dt or datetime.datetime.today().date().replace(day=1),
                            'budget': budget}
            db.balance().execute(query, query_params)
        print('SUCCESS')

    @staticmethod
    def migrate_to_currency(client_id, currency_convert_type, dt=None, service_id=7, region_id=225, currency='RUB'):
        migrate_to_currency_dt = datetime.datetime.now() + datetime.timedelta(seconds=5)
        ClientSteps.create({
            'CLIENT_ID': client_id,
            'REGION_ID': region_id,
            'CURRENCY': currency,
            'MIGRATE_TO_CURRENCY': migrate_to_currency_dt,
            'SERVICE_ID': service_id,
            'CURRENCY_CONVERT_TYPE': currency_convert_type
        })

        with reporter.step(
                u'Переводим клиента {0} на мультивалютность {1} на {2} для сервиса {3}, в регионе {4}, валюта {5}'.format(
                    client_id,
                    u'копированием' if currency_convert_type == 'COPY' else u'конвертацией',
                    u'текущую дату' if dt == None else dt,
                    service_id,
                    region_id,
                    currency)):

            while True:
                if datetime.datetime.now() > migrate_to_currency_dt:
                    break
                else:
                    time.sleep(1)
                    continue

            common_steps.CommonSteps.export('MIGRATE_TO_CURRENCY', 'Client', client_id)
            # query = "SELECT state AS val FROM T_EXPORT WHERE type = 'MIGRATE_TO_CURRENCY' AND object_id = :client_id"
            # sql_params = {'client_id': client_id}
            # CommonSteps.wait_for(query, sql_params)
            if dt:
                db.balance().execute(
                    "UPDATE t_client_service_data SET migrate_to_currency = :dt WHERE class_id = :client_id AND service_id = :service_id AND convert_type  = :currency_convert_type",
                    {'dt': dt, 'client_id': client_id, 'service_id': service_id,
                     'currency_convert_type': currency_convert_type})
                reporter.attach('Client {0} was migrated successfully at {1} by {2}'.format(client_id, dt,
                                                                                            currency_convert_type))
            else:
                reporter.attach('Client {0} was migrated successfully now by {1}'.format(client_id,
                                                                                         currency_convert_type))

    @staticmethod
    def negative_reverse_allow(client_id, turn_on=True):
        with reporter.step(u'Включаем отрицательные реверсы для клиента: {0}'.format(client_id)):
            query = "SELECT value_json FROM t_config WHERE item = 'CONSUMPTION_NEGATIVE_REVERSE_ALLOWED_PARTIAL'"
            result = db.balance().execute(query, {})[0]['value_json']
            json_acceptable_string = result.replace("'", "\"")
            json_string = json.loads(json_acceptable_string)
            if turn_on:
                json_string['Client_ids'].append(client_id)
            else:
                json_string['Client_ids'] = [x for x in json_string['Client_ids'] if x != client_id]
            changed_json_string = json.dumps(json_string)
            query = "UPDATE t_config SET value_json = :value_json WHERE item = 'CONSUMPTION_NEGATIVE_REVERSE_ALLOWED_PARTIAL'"
            query_params = {'value_json': changed_json_string}
            db.balance().execute(query, query_params)
            reporter.attach('Negative reverse for client {0}'.format(client_id))

    @staticmethod
    def set_partner_type(client_id, partner_type):
        with reporter.step(u"Устанавливаем тип партнера для клиента"):
            db.balance().execute("UPDATE t_client SET PARTNER_TYPE=:partner_type WHERE ID=:client_id",
                                 {'client_id': client_id, 'partner_type': partner_type})

    # a-vasin: на самом деле параметр 'Signed' метода означает возвращать ли только активные или все подряд договоры
    # и не имеет никакого отношения к тому подписан ли договор или нет,
    # но назвать параметр 'OnlyActive' было бы слишком просто =)
    @staticmethod
    def get_client_contracts(client_id, contract_subtype, dt=None, signed=1):
        with reporter.step(u"Получаем договоры для клиента с id: {}".format(client_id)):
            if not dt:
                dt = utils.Date.nullify_time_of_date(datetime.datetime.now())

            contracts_info = api.medium().GetClientContracts({'ClientID': client_id,
                                                              'ContractType': contract_subtype.name,
                                                              'Dt': dt, 'Signed': signed})
            reporter.attach(u"Информация о договорах", utils.Presenter.pretty(contracts_info))
            return contracts_info

    @staticmethod
    def get_client_id_by_passport_id(passport_id):
        with reporter.step(u"Получаем ID клиента по ID паспорта"):
            query = 'SELECT * FROM T_PASSPORT WHERE PASSPORT_ID=:passport_id'
            params = {'passport_id': passport_id}

            result = db.balance().execute(query, params)
            client_id = result[0]['client_id']

            if not client_id:
                query = 'SELECT * FROM T_ROLE_CLIENT_USER WHERE PASSPORT_ID=:passport_id'

                result = db.balance().execute(query, params)
                client_id = result[0]['client_id']

            reporter.attach(u"ID клиента", utils.Presenter.pretty(client_id))

            return client_id

    @staticmethod
    def deny_cc(client_id):
        db.balance().execute("UPDATE t_client SET deny_cc=1 WHERE id = :client_id", {'client_id': client_id})

    @staticmethod
    def create_client_brand_contract(client_id, brand_client_params=None):
        brand_client_id = ClientSteps.create(brand_client_params)
        contract_steps.ContractSteps.create_brand_contract(
            client_id,
            brand_client_id,
            dt=NOW - datetime.timedelta(days=180)
        )
        return brand_client_id

    @staticmethod
    def create_equal_client(client_id):
        equal_client_id = ClientSteps.create()
        ClientSteps.merge(client_id, equal_client_id)
        return equal_client_id

    @staticmethod
    def set_client_partner_type(client_id, partner_type=2):
        query = "UPDATE t_client SET partner_type = :partner_type WHERE id = :client_id"
        params = {'client_id': client_id, 'partner_type': partner_type}
        db.balance().execute(query, params)

    @staticmethod
    def get_client_persons(client_id):
        return api.medium().GetClientPersons(client_id)

    @staticmethod
    def get_tech_client(service):
        with reporter.step(u"Получаем техклиента для сервиса: {}".format(service.name)):
            query = "SELECT FORCE_PARTNER_ID FROM T_THIRDPARTY_SERVICE WHERE ID=:service_id"
            params = {'service_id': service.id}
            return db.balance().execute(query, params)[0]['force_partner_id']

    @staticmethod
    def insert_client_into_batch(client_id, batch_id=9999999):
        with reporter.step(u"Вставляем клиента {} в батч: {}".format(client_id, batch_id)):
            query = "insert into t_role_client (client_batch_id, client_id) values (:batch_id, :client_id)"
            params = {'batch_id': batch_id, 'client_id': client_id}
            db.balance().execute(query, params)
