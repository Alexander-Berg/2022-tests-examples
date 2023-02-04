# coding: utf-8
from pprint import pprint
from apikeys import apikeys_api
from apikeys.apikeys_steps_new import deposit_money, create_contract, move_invoice_event_for_contract_in_past, \
    check_last_shipments, clean_up, tarifficator_state_add_consume_for_product, tarifficator_state_move_in_past, \
    tarifficator_state_is_active

from apikeys.apikeys_steps import get_invoice_id_with_effective_sum
import datetime
import allure
from hamcrest import greater_than, equal_to, has_property, is_, not_none, is_not, empty, instance_of
from apikeys.apikeys_defaults import POSTPAYMENT_CONSUMER_UNITS, PREPAYMENT_COUNSUMER_UNITS, ADMIN, APIKEYS_SERVICE_ID, \
    WAITER_PARAMS as W
from balance import balance_api as api, balance_steps, balance_db
from btestlib import matchers as mtch, utils
import btestlib.reporter as reporter
from btestlib.utils import wait_until2 as wait, aDict

TODAY = datetime.datetime.utcnow()
shift = datetime.timedelta
BASE_DT = datetime.datetime.utcnow().replace(hour=5)
START_PREVIOUS_MONTH, END_PREVIOUS_MONTH = utils.Date.previous_month_first_and_last_days(BASE_DT)
START_CURRENT_MONTH, END_CURRENT_MONTH = utils.Date.current_month_first_and_last_days(BASE_DT)


class Task(object):
    def __init__(self, link=None, contractor=None, limit_checker=None):
        pass


class Service(object):
    def __init__(self, id=None, cc=None):

        if not (id or cc):
            raise Exception('Pass cc or id')

        service = apikeys_api.TEST.mongo_find('service',
                                              {'_id' if id else 'cc': id if id else cc})[0]
        if service is None:
            raise Exception('Service not found')
        self.id = int(service['_id'])
        self.cc = service['cc']
        self.token = service.get('token')
        self.attachable_in_ui = service.get('attachable_in_ui', False)
        self.questionnaire_id = service.get('questionnaire_id', None)


class User(object):
    project_id = None

    def __init__(self, uid=None, login=None):

        if not (login or uid):
            raise Exception('Pass login or uid')

        try:
            self.login = login if login else apikeys_api.TEST.mongo_find('user', {'_id': uid})[0]['login']
        except IndexError:
            raise Exception("Do not have user with this uid")

        try:
            self.uid = uid if uid else apikeys_api.TEST.mongo_find('user', {'login': login})[0]['_id']
        except IndexError:
            raise Exception("Do not have user with this login")

    def get_balance_client(self, new=True):
        """Create new client or get old client from balance

        :arg
            :param new(bool): Create new client flag, default - True
        :returns
            :return client_id
        """
        if new:
            self.client_id = balance_steps.ClientSteps.create()
            balance_steps.ClientSteps.link(self.client_id, self.login)
            # Сделано для того, чтоб не конфликтовать с PullClientFromBalance.
            apikeys_api.TEST().mongo_update('user', {"$set": {"balance_client_id": self.client_id}},
                                            {"_id": self.uid})
            # apikeys_api.BO.get_client_from_balance(ADMIN, self.uid)
        else:
            self.client_id = apikeys_api.BO.get_client_from_balance(ADMIN, self.uid).json()['result']['CLIENT_ID']

    def create_user_project(self):
        if self.project_id is None:
            self.project_id = apikeys_api.UI2().project_create(oper_uid=self.uid).json()['data']['id']
        else:
            raise Exception('project already created')

    def get_user_projects(self):
        self.project_list = apikeys_api.TEST.mongo_find('project', {'user_uid': self.uid})
        return 'OK'

    def set_user_projects(self, number=1):
        apikeys_api.TEST.mongo_update('user', {"$set": {"n_project_slots": number}},{"_id": self.uid})

    def clean_up(self, db):
        clean_up(self.uid, db)
        pass


class Key(object):

    def __init__(self, user_uid, service_id, db, key_id=None):
        """
        :param user:
        :param service_id:
        """

        self.db = db
        self.service_id = service_id
        if not key_id:
            self.id = apikeys_api.BO.create_key(ADMIN, user_uid).json()['result']['key']
            apikeys_api.BO.update_service_link(ADMIN, self.id, service_id, method="PATCH")
        else:
            self.id = key_id
        self.service_token = db['service'].find_one({'_id': service_id})['token']
        self.counter_list = dict([[
            db['unit'].find_one({'_id': item['unit_id']})['cc'],
            {'unit_id': item['unit_id'], 'counter_id': 0}
        ] for item in db['key_service_counter'].find({'key': self.id})])

    def update_counter_honest(self, counter_name, stat):
        apikeys_api.API.update_counters(self.service_token, self.id, {counter_name: stat})

    def update_counters(self, counter_name, date=None, stat=0):
        with allure.step(u'Insert stat counter: {} value{}'.format(counter_name, stat)):
            if not self.counter_list.get(counter_name, None):
                raise Exception("Error, Counter not found")

            if self.counter_list.get(counter_name)['counter_id'] == 0:
                with reporter.step(u"Инициализируем счетчик: {}".format(counter_name)):
                    apikeys_api.API.update_counters(self.service_token, self.id, {counter_name: stat})
                self.counter_list[counter_name]['counter_id'] = self.db['key_service_counter'].find_one \
                    ({"key": self.id,
                      "service_id": self.service_id,
                      "unit_id": self.counter_list[counter_name]['unit_id'], })['counter_id']

            if self.counter_list.get(counter_name)['counter_id'] > 0:
                with reporter.step(u'Вставляем статистику количеством: {}\n на дату {}\n'.format(stat, date)):
                    self.db['hourly_stat'].insert(
                        {"counter_id": self.counter_list[counter_name]['counter_id'], "dt": date, "value": stat})
                    return "OK"


class Link(object):

    def __init__(self, user, service, db):
        with reporter.step(u'Создаем связку для пользователя \n'
                           u'User_uid:  {user_uid} User_login:  {user_login}\n'.format(user_uid=user.uid,
                                                                                       user_login=user.login)
                           ):
            self.db = db
            self.user = user
            self.service = service
            self.keys = []

            # Если пользователь может подключить сервис, поключаем через анкету, иначе через админку. Если анкеты нет, подключаем через ручку создания связки
            def create_link():
                if service.attachable_in_ui:
                    if service.questionnaire_id:
                        apikeys_api.Questionary.attach_project(service_cc=self.service.cc,
                                                               project_id=self.user.project_id)
                    else:
                        apikeys_api.UI2().project_service_link_create(self.user.uid, self.user.project_id,
                                                                      self.service.id)
                else:
                    self.keys.append(Key(user_uid=user.uid, service_id=service.id, db=self.db))

                link = db['project_service_link'].find_one({'project_id': self.user.project_id,
                                                            'service_id': self.service.id})
                if link:
                    return link['_id']
                else:
                    return None

            # Проверяем действительно ли создалась линка

            self.link_id = wait(create_link, is_not(None), timeout=W.time, sleep_time=W.s_time)()

            reporter.attach(
                u'[DEBUG]------Параметры связки: link_id: {link_id}, service_cc: {service_cc}'.format(
                    link_id=self.link_id,
                    service_cc=self.service.cc))

    def add_key(self, key_id=None):
        self.keys.append(Key(user_uid=self.user.uid, service_id=self.service.id, db=self.db, key_id=key_id))

    def create_and_change_person(self, person_type='ur', change_flag=True):
        # BALANCE_UI Создаем Плательщика
        self.person_type = person_type
        self.person_id = balance_steps.PersonSteps.create(self.user.client_id, type_=person_type)  # PAYSYS_ID = 1003
        self.paysys_invoice = 1001 if person_type == 'ph' else 1003
        if change_flag:
            apikeys_api.UI2().project_service_link_update(self.user.uid, self.user.project_id, self.service.id,
                                                          {'balance_person': {
                                                              'id': str(self.person_id)}})

    def change_and_activate_tariff(self, tariff_cc=None, sign_flag='sign', agent_flag=False):
        """
        :arg
            :param tariff_cc(string): New tariff for link
        :returns
            :return client_id

        """

        with reporter.step(u'Change and activate tariff'):
            self.tariff_cc = tariff_cc
            with reporter.step(u" Получаем даннные по тарифу: {tariff_cc}".format(tariff_cc=tariff_cc)):
                self.tariff_data = self.db['tariff'].find_one({'cc': tariff_cc})
                reporter.attach(u'Параметры тарифа: {}'.format(utils.Presenter.pretty(self.tariff_data)))

            # Тариф договорной если поле contractless=False или оно отсутствует
            if self.tariff_data.get('contractless', False) or self.tariff_data.get('flow_type',
                                                                                   None) == 'contractless_flow':

                if self.tariff_data.get('client_access', False):
                    for unit in self.tariff_data['tarifficator_config']:
                        if unit['unit'] == 'PrepayPeriodicallyUnit' or unit['unit'] == 'DailyPrepaySeveralDaysUnit' or \
                                unit['unit'] == 'PrepayPeriodicallyDiscountedUnit':
                            with reporter.step(u'Offer tariff changing'):
                                # Меняем тариф через ручку и оплачиваем годовую подписку, после этого связка должна быть активной
                                # В балансе должны появиться открутки на сумму подписки
                                # apikeys_api.UI.schedule_tariff_changing(self.user.uid, self.user.project_id,
                                #                                         self.service.id,
                                #                                         tariff_cc)
                                apikeys_api.UI2().project_service_link_update(self.user.uid, self.user.project_id,
                                                                              self.service.id,
                                                                              {'scheduled_tariff_cc': self.tariff_cc})
                                # self.db['task'].delete_many({'link': self.link_id})
                                # apikeys_api.TEST.run_tarifficator(self.link_id, TODAY + shift(days=1))

                                reporter.log('Ожидаем смену тарифа на {}'.format(tariff_cc))
                                wait(lambda: self.db['project_service_link'].find_one({'_id': self.link_id})[
                                    'config'].get('tariff', {}),
                                     matcher=mtch.equal_to(tariff_cc), timeout=W.time, sleep_time=W.s_time)()

                                deposit_money(self.user.uid, self.user.project_id, self.service.id,
                                              self.person_id, self.user.client_id, int(unit['params']['product_value']),
                                              self.paysys_invoice)
                                # apikeys_api.TEST.run_tarifficator(self.link_id)
                                check_last_shipments(client_id=self.user.client_id,
                                                     expected_money=int(unit['params']['product_value']))

                if not self.tariff_data.get('client_access', False) == True:
                    with reporter.step(u'Change tariiff from ADMIN'):
                        apikeys_api.BO.schedule_tariff_changing(ADMIN, self.keys[0], self.service.id, tariff_cc)
                        apikeys_api.TEST.run_tarifficator(self.link_id)

            if not self.tariff_data.get('contractless', False) and not self.tariff_data.get('flow_type',
                                                                                            None) == 'contractless_flow':
                reporter.step(u'Contract tariff changing')
                for unit in self.tariff_data['tarifficator_config']:
                    if unit['unit'] == 'CreditedActivatorUnit':
                        with reporter.step(u'Processing credited_activator_unit'):
                            # Если у тарифа есть credited_activator_unit то это предоплатный, контрактный тариф и договор для него
                            # нужно создавать в будущем. При этом связка сразу активированна не будет, а только после оплаты счета
                            self.contract_id = create_contract('apikeys_' + self.tariff_cc, self.service.cc,
                                                               self.user.client_id, self.person_id,
                                                               contract_in='future')
                            apikeys_api.TEST.run_user_contractor(user_uid=self.user.uid)

                            prepayment_invoice_id = wait(get_invoice_id_with_effective_sum, is_not(None),
                                                         timeout=W.time,
                                                         sleep_time=W.s_time)(self.contract_id, self.user.uid)
                            balance_steps.InvoiceSteps.pay(prepayment_invoice_id)
                            balance_steps.ContractSteps.update_contract_params_in_db(contract_id=self.contract_id,
                                                                                     action=4,
                                                                                     dt=START_PREVIOUS_MONTH)
                            move_invoice_event_for_contract_in_past(self.contract_id, self.db)
                            orders = balance_db.get_order_by_client(self.user.client_id)
                            # apikeys_api.TEST.run_user_contractor(user_uid=self.user.uid)
                            self.db['task'].delete_many({'link': self.link_id})
                            balance_steps.CommonSteps.wait_and_get_notification(1, orders[0]['id'], 1)

                            apikeys_api.TEST.run_tarifficator(self.link_id)
                            # apikeys_api.TEST.run_tarifficator(self.link_id)
                            # проверяем активировалась ли связка
                            tarifficator_state_is_active(self.link_id, self.db)
                            return 'OK'

                    if unit['unit'] == 'FirstPeriodScaleByDayFixerUnit':
                        with reporter.step(u'Processing FirstPeriodScaleByDayFixerUnit'):
                            # Если у тарифа есть PeriodicalConsumerUnit то это постоплатный, контрактный тариф и договор для него
                            # нужно создавать в в прошлом. Связка будет активна сразу, но после активации тарифа в балансе должна быть открученна
                            # сумма подписки за оставшуюся часть месяца

                            self.contract_id = create_contract('apikeys_' + self.tariff_cc, self.service.cc,
                                                               self.user.client_id, self.person_id, contract_in='past',
                                                               sign_flag=sign_flag)
                            apikeys_api.TEST.run_user_contractor(self.user.uid)
                            self.db['task'].delete_many({'link': self.link_id})
                            apikeys_api.TEST.run_tarifficator(self.link_id)
                            apikeys_api.TEST.run_tarifficator(self.link_id)
                            pprint(self.db['project_service_link'].find_one({'_id': self.link_id}))
                            # Проверяем открутки в балансе

                            check_last_shipments(client_id=self.user.client_id,
                                                 expected_money=round(
                                                     (float(unit['params'][
                                                                'product_value']) / END_CURRENT_MONTH.day) * (
                                                             END_CURRENT_MONTH.day - TODAY.day + 1), 2
                                                 )
                                                 )
                            return 'OK'

                    if unit['unit'] == 'PeriodicalConsumerUnit':
                        with reporter.step(u'Processing PeriodicalConsumerUnit'):
                            # Если у тарифа есть PeriodicalConsumerUnit то это постоплатный, контрактный тариф и договор для него
                            # нужно создавать в в прошлом. Связка будет активна сразу, но после активации тарифа в балансе должна быть открученна сумма подписки
                            self.contract_id = create_contract('apikeys_' + self.tariff_cc, self.service.cc,
                                                               self.user.client_id, self.person_id, contract_in='past')
                            apikeys_api.TEST.run_user_contractor(self.user.uid)
                            self.db['task'].delete_many({'link': self.link_id})
                            apikeys_api.TEST.run_tarifficator(self.link_id)
                            apikeys_api.TEST.run_tarifficator(self.link_id)
                            pprint(self.db['project_service_link'].find_one({'_id': self.link_id}))
                            # Проверяем открутки в балансе
                            check_last_shipments(client_id=self.user.client_id,
                                                 expected_money=int(unit['params']['product_value']))
                            return 'OK'


                else:
                    with reporter.step(u'No contract activate Unit'):
                        # если у договорного  тарифа нет юнитов связанных с оплатой лицензии, то договор заводим в прошлое.
                        # связка должна быть активна сразу
                        self.contract_id = create_contract('apikeys_' + self.tariff_cc, self.service.cc,
                                                           self.user.client_id, self.person_id, contract_in='past',
                                                           sign_flag=sign_flag, agent=agent_flag)
                        apikeys_api.TEST.run_user_contractor(self.user.uid)
                        # self.db['task'].delete_many({'link': self.link_id})
                        # apikeys_api.TEST.run_tarifficator(self.link_id)
                        # apikeys_api.TEST.run_tarifficator(self.link_id)
                        if agent_flag:
                            apikeys_api.UI2().project_service_link_update(self.user.uid, self.user.project_id,
                                                                          self.service.id,
                                                                          {'scheduled_tariff_cc': self.tariff_cc})
                        return 'OK'
                        pass

    def key_stat_exploit(self):
        with reporter.step(u'Start exploit block'):
            for unit in self.tariff_data['tarifficator_config']:
                if unit['unit'] == 'DailyStatisticRangeConsumerUnit':
                    with reporter.step(u'Processing DailyStatisticRangeConsumerUnit'):
                        # устанавливаюе дату активации связки в прошлое, для того чтоб тарификатор обработал статистику
                        tarifficator_state_move_in_past(self.link_id, self.db)

                        self.keys[0].update_counters('total', START_PREVIOUS_MONTH + shift(days=1),
                                                     int(unit['params']['range_from']) + 1)

                        apikeys_api.TEST.run_tarifficator(self.link_id,
                                                          START_PREVIOUS_MONTH + shift(days=1) + shift(minutes=4))
                        apikeys_api.TEST.run_tarifficator(self.link_id,
                                                          START_PREVIOUS_MONTH + shift(days=2) + shift(minutes=4))

                        # Проверяем что в балансе появлась открутки по превышениям
                        check_last_shipments(client_id=self.user.client_id,
                                             expected_money=int(unit['params']['product_value']))
                        return 'OK'

                if unit['unit'] == (u'TodayPrepayStatisticRangeConsumerUnit'):
                    with reporter.step(u'Processing TodayPrepayStatisticRangeConsumerUnit'):
                        tarifficator_state_move_in_past(self.link_id, self.db)
                        # Так как юнит предоплатный сначала вносим средства на лицевой счет
                        deposit_money(self.user.uid, self.user.project_id, self.service.id,
                                      self.person_id, self.user.client_id, int(unit['params']['product_value']),
                                      self.paysys_invoice)
                        self.keys[0].update_counters(unit['params']['statistic_aggregator'],
                                                     START_PREVIOUS_MONTH + shift(days=1),
                                                     int(unit['params']['range_from']) + 1)
                        apikeys_api.TEST.run_tarifficator(self.link_id,
                                                          START_PREVIOUS_MONTH + shift(days=1) + shift(minutes=4))
                        apikeys_api.TEST.run_tarifficator(self.link_id,
                                                          START_PREVIOUS_MONTH + shift(days=2) + shift(minutes=4))

                        # Проверяем что в балансе появлась открутки по превышениям
                        check_last_shipments(client_id=self.user.client_id,
                                             expected_money=int(unit['params']['product_value']))
                        return 'OK'

                if unit['unit'] == 'PostpaySubscribePeriodicallyRangeConsumerUnit':
                    with reporter.step(u'Processing PostpaySubscribePeriodicallyRangeConsumerUnit'):
                        # устанавливаюе дату активации связки в прошлое, для того чтоб тарификатор обработал статистику
                        tarifficator_state_move_in_past(self.link_id, self.db)

                        self.keys[0].update_counters('total', START_PREVIOUS_MONTH + shift(days=1),
                                                     int(unit['params']['range_from']) + 1)

                        apikeys_api.TEST.run_tarifficator(self.link_id,
                                                          START_PREVIOUS_MONTH + shift(days=1) + shift(minutes=4))
                        apikeys_api.TEST.run_tarifficator(self.link_id,
                                                          START_PREVIOUS_MONTH + shift(days=2) + shift(minutes=4))

                        # Проверяем что в балансе появлась открутки по превышениям
                        check_last_shipments(client_id=self.user.client_id,
                                             expected_money=int(unit['params']['product_value']))
                        return 'OK'

                if unit['unit'] == 'MonthlyStatisticRangePerDayConsumerUnit':
                    with reporter.step(u'Processing MonthlyStatisticRangePerDayConsumerUnit'):
                        # устанавливаюе дату активации связки в прошлое, для того чтоб тарификатор обработал статистику
                        tarifficator_state_move_in_past(self.link_id, self.db)

                        apikeys_api.TEST.run_tarifficator(self.link_id,
                                                          START_PREVIOUS_MONTH + shift(days=1) + shift(minutes=4))
                        apikeys_api.TEST.run_tarifficator(self.link_id,
                                                          START_PREVIOUS_MONTH + shift(days=2) + shift(minutes=4))

                        # Проверяем что в балансе появлась открутки по превышениям
                        check_last_shipments(client_id=self.user.client_id,
                                             expected_money=int(unit['params']['product_value']) * 2)
                        return 'OK'

            else:
                raise Exception('No consumer UNITS')

    def without_key_stat_exploit(self):
        """основная идея данного блока в том, что мы устанваливаем в state под продуктом
        определенную сумму откруток и смотрим что они пришли в баланс. Как таковой обработки ститистики здесь
        не происходит"""
        with reporter.step(u'Start exploit block\n'):
            for unit in self.tariff_data['tarifficator_config']:
                if unit['unit'] in POSTPAYMENT_CONSUMER_UNITS:
                    with reporter.step(u'Processing {}\n'.format(unit['unit'])):
                        # устанавливаюе дату активации связки в прошлое, для того чтоб тарификатор обработал статистику
                        tarifficator_state_add_consume_for_product(self.link_id, self.db, unit['params']['product_id'],
                                                                   unit['params']['product_value'])
                        apikeys_api.TEST.run_tarifficator(self.link_id)
                        check_last_shipments(client_id=self.user.client_id,
                                             expected_money=unit['params']['product_value'])
                        return 'OK'

                if unit['unit'] in PREPAYMENT_COUNSUMER_UNITS:
                    with reporter.step(u'Processing {}\n'.format(unit['unit'])):
                        # Так как юнит предоплатный сначала вносим средства на лицевой счет
                        deposit_money(self.user.uid, self.user.project_id, self.service.id,
                                      self.person_id, self.user.client_id, int(unit['params']['product_value']),
                                      self.paysys_invoice)

                        # Проверяем что в балансе появлась открутки по превышениям
                        tarifficator_state_add_consume_for_product(self.link_id, self.db, unit['params']['product_id'],
                                                                   unit['params']['product_value'])
                        apikeys_api.TEST.run_tarifficator(self.link_id)
                        check_last_shipments(client_id=self.user.client_id,
                                             expected_money=unit['params']['product_value'],
                                             product_id=unit['params']['product_id'])
                        return 'OK'

            else:
                raise Exception('No consumer UNITS')

    def end_contract(self):
        with reporter.step('End contract'):
            balance_steps.ContractSteps.update_contract_end_dt(self.contract_id, TODAY - shift(days=1))
            apikeys_api.TEST.run_user_contractor(self.user.uid)
            apikeys_api.TEST.run_tarifficator(self.link_id)
            utils.check_that(
                lambda: self.db['project_service_link'].find_one({'_id': self.link_id})['config']['tariff'],
                not equal_to(self.tariff_cc))

    def create_balance_contract(self, tariff_cc):
        with reporter.step(u'Создаем контракт через ручку Create_Balance_Contract в Apikeys'):
            apikeys_api.UI2().create_balance_contract(oper_uid=self.user.uid, project_id=self.user.project_id,
                                                      service_id=self.service.id,
                                                      person=str(self.person_id),
                                                      tariff=tariff_cc,
                                                      comment='I love Cake',
                                                      allow_not_200=True)

    def get_balance_contract(self):
        with reporter.step(u'Получаем список контрактов через ручку Get_Balance_Contract в Apikeys'):
            apikeys_api.UI2().get_balance_contract(oper_uid=self.user.uid, project_id=self.user.project_id,
                                                   service_id=self.service.id)

    def time_exploit(self):
        pass

    def end_exploit(self):
        pass
