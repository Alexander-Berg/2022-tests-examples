# -*- coding: utf-8 -*-
"""
Module for base database actions
"""

import json

from hamcrest import is_in, equal_to

from balance.balance_db import balance, balance_bs_ora, balance_bs_ora_dev, \
    balance_bo_ora_dev, balance_bs_pg, pcidss_mysql
from btestlib import environments
from btestlib import matchers
from btestlib import reporter
from btestlib import utils as butils
from btestlib.constants import Services
from simpleapi.common.utils import current_scheme_is
from simpleapi.steps import balance_test_steps as balance_test

__author__ = 'fellow'


class Source(object):
    ORACLE = 'oracle'
    MYSQL = 'mysql'
    POSTGRE = 'postgre'


class Scheme(object):
    def __init__(self, name, source):
        self.source = source
        self.name = name


def get_schema_by_env():
    if current_scheme_is('BO'):
        return Schemas.BO
    else:
        return Schemas.BS


def get_bs_source():
    if environments.simpleapi_env().db_driver == Source.ORACLE:
        if current_scheme_is('DEV_BS'):
            return balance_bs_ora_dev()
        else:
            return balance_bs_ora()
    elif environments.simpleapi_env().db_driver == Source.POSTGRE:
        return balance_bs_pg()
    return balance_bs_ora()


def get_bo_source():
    if current_scheme_is('DEV_BS'):
        return balance_bo_ora_dev()
    else:
        return balance()


def get_ng_source():
    if current_scheme_is('DEV_BS'):
        return None  # TODO: add dev_bs to tools
    else:
        return balance_bs_pg()


class Schemas(object):
    BO = Scheme(name='BO', source=get_bo_source())
    BS = Scheme(name='BS', source=get_bs_source())
    NG = Scheme(name='NG', source=get_ng_source())
    MYSQL_PCIDSS = Scheme(name='PCIDSS', source=pcidss_mysql())


class DB(object):
    def __init__(self, scheme):
        self.scheme = scheme

    def get_partner_by_id(self, partner_id):
        with reporter.step(u"Get partner info from Oracle DB in scheme {} by partner_id={}"
                                   .format(self.scheme.name, partner_id)):
            query = "SELECT * from T_PARTNER where ID={}".format(partner_id)
            return self.scheme.source.execute(query, {}, single_row=True)

    def get_product_by_external_id(self, service_product_id, service):
        with reporter.step(u"Get product info from Oracle DB in scheme {} by product_id={}".
                                   format(self.scheme.name, service_product_id)):
            query = "SELECT ID, SERVICE_ID, EXTERNAL_ID, NAME, PARTNER_ID " \
                    "from T_SERVICE_PRODUCT " \
                    "where EXTERNAL_ID='{}' and SERVICE_ID={}".format(service_product_id,
                                                                      service.id)
            return self.scheme.source.execute(query, {}, single_row=True)

    def get_terminal_by(self, service_product_id, partner_id):
        with reporter.step(u"Get terminal info from Oracle DB in scheme {} by product_id={}"
                                   .format(self.scheme.name, service_product_id)):
            query = "SELECT * from T_TERMINAL " \
                    "where SERVICE_PRODUCT_ID={} and PARTNER_ID={}".format(service_product_id,
                                                                           partner_id)
            return self.scheme.source.execute(query, {})

    def _execute_query_internal(self, query, single_row=False):
        return self.scheme.source.execute(query, {}, single_row=single_row)

    def execute_query(self, query):
        return self._execute_query_internal(query)[0]

    def execute_query_all(self, query):
        return self._execute_query_internal(query)

    def execute_query_scalar(self, query):
        return self._execute_query_internal(query, single_row=True)

    def execute_update(self, query):
        self._execute_query_internal(query)

    def get_next_service_order_id(self, service):
        query = 'SELECT {}.nextval AS next_id FROM dual'.format(balance_test.get_test_sequence_name(service))
        return self.execute_query(query)['next_id']

    def get_export_info(self, object_id=None, trust_payment_id=None, classname=None, exp_type=None):
        query = 'SELECT exp.state, exp.export_dt FROM t_export exp'
        if object_id:
            query += " WHERE exp.object_id={}".format(object_id)
            if classname:
                query += " AND classname='{}'".format(classname)
        elif trust_payment_id:
            # query = "SELECT exp.state, exp.export_dt FROM t_export exp JOIN t_payment p ON exp.object_id = p.id " \
            # "WHERE p.trust_payment_id='{}' AND exp.type='BALANCE'".format(trust_payment_id)
            query += " JOIN t_payment p ON exp.object_id = p.id" \
                     " WHERE p.trust_payment_id='{}'".format(trust_payment_id)
            if classname:
                query += " AND classname='{}'".format(classname)
            if exp_type:
                query += " AND exp.type='{}'".format(exp_type)

        return self.execute_query(query)

    # todo здесь ли это должно быть?
    def wait_export_done(self, object_id=None, trust_payment_id=None,
                         classname='TrustPayment', export_time=None):
        def is_export_correct(export_info):
            return export_info['state'] in [3, 5, ]

        def is_export_done(export_info):
            if export_time is not None:
                return export_info['state'] == 2 and export_info['export_dt'] > export_time
            else:
                return export_info['state'] == 2

        # терминалы по маркету выгружаются раз в минуту но ждем подольше
        with reporter.step(u'Ждем пока закончится экспорт в bo...'):
            butils.wait_until(lambda: self.get_export_info(object_id, trust_payment_id, classname),
                              success_condition=matchers.matcher_for(is_export_done, descr='Export is done'),
                              failure_condition=matchers.matcher_for(is_export_correct, descr='Export is correct'),
                              timeout=6 * 60)

    def wait_postauth_export_done(self, trust_payment_id=None):
        # терминалы по маркету выгружаются раз в минуту но ждем подольше
        with reporter.step(u'Ждем пока закончится экспорт после поставторизации в bo...'):
            butils.wait_until(lambda: self.get_export_info(trust_payment_id=trust_payment_id,
                                                           exp_type='BALANCE')['state'],
                              success_condition=equal_to(2),
                              # fellow: раньше проверяли здесь еще статус=3
                              # но бывает что получаем статус 3, но через некоторое время он становится 2
                              failure_condition=is_in([5]),
                              timeout=6 * 60)
            butils.wait_until(lambda: self.get_export_info(trust_payment_id=trust_payment_id,
                                                           exp_type='POSTAUTH')['state'],
                              success_condition=equal_to(2),
                              failure_condition=is_in([3, 5]),
                              timeout=6 * 60)

    def get_latest_music_transaction(self, original_transaction_id):
        query = "SELECT app.transaction_id FROM t_apple_inapp_payment app " \
                "WHERE app.parent_transaction_id = '{}' ORDER BY app.purchase_date DESC" \
            .format(original_transaction_id)
        return self.execute_query(query)

    def _chunk_payments(self, payments, base_query):
        # to avoid ORA-01795
        chunk_size = 1000
        if len(payments) == 1:
            query = base_query + "WHERE id={}".format(payments[0])
        elif len(payments) < chunk_size:
            query = base_query + "WHERE id IN {}".format(payments)
        else:
            def chunks(l, n):
                """Yield successive n-sized chunks from l."""
                for i in xrange(0, len(l), n):
                    yield l[i:i + n]

            query = base_query + "WHERE id IN "
            i = 0
            for payments_chunk in chunks(payments, chunk_size):
                if i == 0:
                    query += '{}'.format(payments_chunk)
                else:
                    query += ' OR id IN {}'.format(payments_chunk)
                i += 1

        return query

    def delete_register_ids_from_payments(self, payments):
        with reporter.step(u'Удаляем register_id и register_line_id для всех платежей реестра'):
            base_query = "UPDATE t_payment SET register_id=NULL, register_line_id=NULL "
            query = self._chunk_payments(tuple(payments), base_query)
            self.execute_update(query)

    def get_registers_for_tests(self, host='greed-ts1h'):
        query = "SELECT id, msg_path, src FROM t_incoming_mail WHERE msg_path LIKE '%{}' " \
                "AND dt BETWEEN TIMESTAMP '2017-08-28 00:00:00' AND TIMESTAMP '2017-08-29 00:00:00'".format(host)
        return self.execute_query_all(query)

    def get_register_payments(self, payments):
        base_query = "SELECT id, register_id, register_line_id FROM t_payment "
        query = self._chunk_payments(payments, base_query)
        return self.execute_query_all(query)

    def get_all_payments_of_register(self, incoming_mail_id):
        query = "select p.id from bo.t_payment p \
                    join bo.t_payment_register pr on p.register_id = pr.id \
                    where pr.incoming_mail_id = {}".format(incoming_mail_id)
        return self.execute_query_all(query)

    def get_register_status(self, incoming_mail_id):
        query = "SELECT status, status_desc FROM t_incoming_mail WHERE id='{}'".format(incoming_mail_id)
        return self.execute_query(query)

    def set_incoming_mail_status(self, register_id, status):
        with reporter.step(u'Проставляем t_incoming_mail.status=0'):
            query = "UPDATE t_incoming_mail SET status={} WHERE id='{}'".format(status, register_id)
            self.execute_update(query)

    def update_payment_dt(self, new_dt, purchase_token):
        with reporter.step(u'Обновляем дату создания платежа purchase_token = {}'.format(purchase_token)):
            query = """UPDATE T_PAYMENT
                       SET DT = TO_DATE('{}', 'yyyy-MM-dd HH24:mi:ss')
                       where PURCHASE_TOKEN = '{}'""".format(new_dt, purchase_token)
            self.execute_update(query)

    def wait_registers_export_done(self, incoming_mail_id):
        with reporter.step(u'Ждем пока обработается реестр (t_incoming_mail.status==1)'):
            def is_status_correct(row):
                return row['status'] == 1

            def is_status_incorrect(row):
                return row['status'] not in (0, 1, 2)

            butils.wait_until(lambda: self.get_register_status(incoming_mail_id),
                              success_condition=matchers.matcher_for(is_status_correct, 'status == 1'),
                              failure_condition=matchers.matcher_for(is_status_incorrect, 'status not in (0, 1, 2)'),
                              timeout=5 * 60)

    def delete_from_music_by_passport(self, passport_id):
        query = "DELETE FROM t_service_client WHERE service_id = {} AND passport_id = {}".format(Services.MUSIC.id,
                                                                                                 passport_id)
        self.execute_update(query)

    def delete_passport_id_from_t_order(self, passport_id):
        with reporter.step(u'Удаляем passport_id = {} из t_order'.format(passport_id)):
            query = "UPDATE t_order SET passport_id = NULL WHERE passport_id = {}".format(passport_id)
            self.execute_update(query)

    def delete_passport_id_from_t_operation(self, passport_id):
        with reporter.step(u'Удаляем passport_id = {} из t_operation'.format(passport_id)):
            query = "UPDATE t_operation SET passport_id = NULL WHERE passport_id = {}".format(passport_id)
            self.execute_update(query)

    def delete_passport_id_from_t_subscription_info(self, passport_id):
        with reporter.step(u'Удаляем записи из таблицы t_subscription_info для passport_id = {}'.format(passport_id)):
            query = "DELETE from t_subscription_info  WHERE passport_id = {}".format(passport_id)
            self.execute_update(query)

    def delete_passport_id_from_t_passport(self, passport_id):
        with reporter.step(u'Удаляем записи из таблицы t_passport для passport_id = {}'.format(passport_id)):
            query = "DELETE FROM t_passport WHERE passport_id = {}".format(passport_id)
            self.execute_update(query)

    def get_service_client_relation(self, client_id, passport_id):
        query = "SELECT * FROM t_service_client WHERE service_id=23 AND " \
                "client_id = {} AND passport_id = {}".format(client_id, passport_id)
        return self.execute_query_scalar(query)

    def get_all_refunds_of_payment(self, trust_payment_id):
        with reporter.step(u'Получаем список возвратов по платежу {}'.format(trust_payment_id)):
            query = "SELECT r.trust_refund_id " \
                    "FROM t_payment r JOIN t_payment p ON r.orig_payment_id = p.id " \
                    "WHERE p.trust_payment_id='%s'" % trust_payment_id

            return self.execute_query_all(query)

    def get_info_for_group_refund(self, trust_payment_id):
        with reporter.step(u'Получаем сумму и тип для группового рефанда по платежу {}'.format(trust_payment_id)):
            query = "SELECT amount, type FROM T_PAYMENT WHERE ORIG_PAYMENT_ID=" \
                    "(SELECT id FROM T_PAYMENT WHERE TRUST_PAYMENT_ID='{}')".format(trust_payment_id)

            return self.execute_query_all(query)

    def get_composite_payment(self, purchase_token):
        with reporter.step(u'Получаем id композитного платежа по {}'.format(purchase_token)):
            query = "SELECT composite_payment_id FROM T_PAYMENT WHERE purchase_token='{}'".format(purchase_token)
            return self.execute_query(query)['composite_payment_id']

    def get_amounts_from_payment(self, purchase_token, composite_payment_id=None):
        with reporter.step(u'Получаем данные по ценам в платеже {}'.format(purchase_token)):
            base_query = "SELECT amount, orig_amount FROM T_PAYMENT WHERE "
            if composite_payment_id:
                added_query = "composite_payment_id='{}' and purchase_token!='{}'" \
                    .format(composite_payment_id, purchase_token)
            else:
                added_query = "purchase_token='{}'".format(purchase_token)

            resp = self.execute_query(base_query + added_query)
            return resp['amount'], resp['orig_amount']

    def get_services_by_schema_param(self, param, value):
        with reporter.step(u'Получаем сервисы по {}={}'.format(param, value)):
            query = "SELECT ID FROM T_SERVICE WHERE SCHEMA_NAME IN (SELECT NAME FROM T_SERVICE_SCHEMA WHERE {}={})". \
                format(param, value)
        return self.execute_query_all(query)

    def get_processing_from_payment(self, purchase_token):
        with reporter.step(u'Узнаём процессинг платежа по {}'.format(purchase_token)):
            query = "SELECT processing_id FROM T_PAYMENT WHERE purchase_token='{}'".format(purchase_token)
            return self.execute_query(query)['processing_id']

    def get_terminal_from_payment(self, trust_payment_id):
        with reporter.step(u'Узнаём терминал платежа по {}'.format(trust_payment_id)):
            query = "SELECT terminal_id FROM T_PAYMENT WHERE trust_payment_id='{}'".format(trust_payment_id)
            return self.execute_query(query)['terminal_id']

    def update_service_receipt_info(self, service, email=None, url=None):
        with reporter.step(u'Меняем email={} и url={} у сервиса {}'.format(email, url, service)):
            query = "UPDATE T_SERVICE SET URL='{}', EMAIL='{}' WHERE ID={}".format(url, email, service.id)
            return self.execute_update(query)

    def get_service_receipt_info(self, service):
        with reporter.step(u'Получаем email и url сервиса {}'.format(service)):
            query = "SELECT URL, EMAIL FROM T_SERVICE WHERE ID={}".format(service.id)
            return self.execute_query(query)

    def get_trust_us_processing_users(self):
        with reporter.step(u'Получаем список пользователей которые роутятся на пейстепе в траст как процессинг'):
            query = "SELECT VALUE_JSON FROM T_CONFIG WHERE ITEM='PASSPORT2TERMINAL_RULES'"
            return self.execute_query_scalar(query)['value_json']

    def write_trust_us_processing_users(self, users):
        with reporter.step(u'Меняем список пользователей которые роутятся на пейстепе в траст как процессинг'):
            query = "UPDATE T_CONFIG SET VALUE_JSON='{}' WHERE ITEM='PASSPORT2TERMINAL_RULES'".format(users)
            return self.execute_update(query)

    def add_user_to_trust_as_processing(self, user, terminal=96001010):
        with reporter.step(u'Добавляем пользователя {} в список пользователей '
                           u'которые на пейстепе роутятся в траст как процессинг'.format(user)):
            users = json.loads(self.get_trust_us_processing_users())

            if user.uid not in users:
                users.update({user.uid: terminal})
                return self.write_trust_us_processing_users(json.dumps(users))

    def delete_user_from_trust_as_processing(self, user):
        with reporter.step(u'Удаляем пользователя {} из списка пользователей '
                           u'которые на пейстепе роутятся в траст как процессинг'.format(user)):
            users = json.loads(self.get_trust_us_processing_users())

            if user.uid in users:
                users.pop(user.uid, None)
                return self.write_trust_us_processing_users(json.dumps(users))

    def get_receipt_email_info(self, purchase_token):
        query = """SELECT e.TYPE, e.STATE, e.ERROR
                    FROM bs.T_PAYMENT p
                    JOIN bs.T_FISCAL_RECEIPT_HEAD fr ON p.ID = fr.PAYMENT_ID
                    JOIN bs.T_EXPORT e ON fr.ID = e.OBJECT_ID
                   WHERE p.PURCHASE_TOKEN = '{}'""".format(purchase_token)
        return self.execute_query(query)

    def get_darkspirit_retrieve_uri(self, purchase_token):
        query = """SELECT fr.RETRIEVE_URI
                            FROM bs.T_PAYMENT p
                            JOIN bs.T_FISCAL_RECEIPT_HEAD fr ON p.ID = fr.PAYMENT_ID
                           WHERE p.PURCHASE_TOKEN = '{}'""".format(purchase_token)
        return self.execute_query(query)

    def wait_receipt_mail_is_send(self, purchase_token):
        with reporter.step('Ждем пока отправится письмо по чеку'):
            def is_mail_send(row):
                return row == {'state': 2, 'type': 'FISCAL_EMAIL', 'error': None}

            def is_mail_send_error(row):
                return row['error'] is not None

            butils.wait_until(lambda: self.get_receipt_email_info(purchase_token),
                              success_condition=matchers.matcher_for(is_mail_send),
                              failure_condition=matchers.matcher_for(is_mail_send_error),
                              timeout=60)

    def get_afs_stat(self, trust_payment_id):
        with reporter.step(u'Получаем сохраненную в трасте статистику из АФС по платежу {}'.format(trust_payment_id)):
            query = """SELECT AFS_STATUS, AFS_ACTION, AFS_RESP_DESC, AFS_TAGS
                    FROM bs.T_PAYMENT_FRAUD_STATUS WHERE TRUST_PAYMENT_ID = '{}'""".format(trust_payment_id)
            return self.execute_query(query)

    def get_payment_terminal(self, trust_payment_id):
        with reporter.step(u'Получаем информацию о терминале по платежу {}'.format(trust_payment_id)):
            query = """select * from T_TERMINAL where id in
                               (select terminal_id from T_PAYMENT
                                where TRUST_PAYMENT_ID='{}')""".format(trust_payment_id)
            return self.execute_query(query)

    def get_tpi_for_uber_refund(self, uids):
        with reporter.step(u'Получаем trust_payment_id для совершения возврата по платежам Uber`а'):
            query = """SELECT * FROM T_PAYMENT
                        where SERVICE_ID = {} and
                              PAYMENT_DT <= sysdate - 1 and
                              TYPE = 'TRUST_PAYMENT' and
                              CANCEL_DT is Null and
                              PAYMENT_METHOD = 'card-e:uber:valid' and
                              PASSPORT_ID IN {}
                        FETCH FIRST 1 ROWS ONLY
                    """.format(Services.UBER_ROAMING.id, tuple(uids))
            return self.execute_query(query)

    def delete_user_info(self, passport_id):
        with reporter.step(u'Удаляем passport_id = {} и связанные с ним записи'.format(passport_id)):
            self.delete_passport_id_from_t_order(passport_id)
            self.delete_passport_id_from_t_operation(passport_id)
            self.delete_passport_id_from_t_subscription_info(passport_id)
            self.delete_passport_id_from_t_passport(passport_id)


def bo():
    return DB(scheme=Schemas.BO)


def bs():
    return DB(scheme=Schemas.BS)


def ng():
    return DB(scheme=Schemas.NG)


def bs_or_ng_by_service(service):
    if service in [Services.TICKETS, Services.EVENTS_TICKETS,
                   Services.EVENTS_TICKETS_NEW, Services.BUSES]:
        return ng()
    else:
        return bs()


def pcidss():
    return DB(scheme=Schemas.MYSQL_PCIDSS)
