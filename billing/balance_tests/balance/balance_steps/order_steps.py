# coding=utf-8
__author__ = 'igogor'

from functools import wraps

from contextlib import contextmanager

import datetime
import json

import balance.balance_api as api
import balance.balance_db as db
import btestlib.environments as env
import btestlib.reporter as reporter
import btestlib.utils as utils

from btestlib.data import defaults
from btestlib.data.defaults import Order
import invoice_steps
import request_steps
from other_steps import ConfigSteps

try:
    from typing import Dict, List
except ImportError:
    pass


class OrderSteps(object):
    @staticmethod
    def next_id(service_id):
        with reporter.step("Получаем service_order_id для сервиса {0}".format(service_id)):
            seq_name = api.test_balance().GetTestSequenceNameForService(service_id)
            service_order_id = db.balance().sequence_nextval(seq_name)
            # reporter.log(("{0:<" + str(log_align) + "} | {1}").format('Service_order_id: {0}'.format(service_order_id),
            #                                                           "'ServiceID': {0}".format(service_id)))
            reporter.attach("service_order_id", service_order_id)
            return service_order_id

    @staticmethod
    def create(client_id, service_order_id=None, product_id=Order.PRODUCT.id, service_id=Order.PRODUCT.service_id,
               params=None, passport_uid=defaults.PASSPORT_UID, contract_id=None):  # TODO параметры
        if not service_order_id:
            service_order_id = OrderSteps.next_id(service_id=service_id)
        # TODO можно задать каждому сервису продукт по-умолчанию и получать его здесь по сервису
        main_params = {
            'ClientID': client_id,
            'ProductID': product_id,
            'ServiceID': service_id,
            'ServiceOrderID': service_order_id
        }
        if contract_id:
            main_params['ContractID'] = contract_id

        request_params = utils.remove_empty(Order.default_params())
        request_params.update({'Text': '{} {}-{}'.format(Order.default_params().get('Text'), service_id, product_id)})
        if params is not None: request_params.update(params)
        request_params.update(main_params)

        with reporter.step(u"Создаём заказ c параметрами: {0}".format(
                ', '.join('{0}: {1}'.format(k, v) for k, v in main_params.items()))):
            answer = api.medium().CreateOrUpdateOrdersBatch(passport_uid, [request_params])

            # TODO: refactor it!
            if answer[0][0] == 0:
                order_id = db.balance().get_order_id(service_id, service_order_id)
                order_url = '{base_url}/order.xml?order_id={order_id}'.format(base_url=env.balance_env().balance_ai,
                                                                              order_id=order_id)
            else:
                raise utils.ServiceError(answer[0][1])

            reporter.attach(u'order_id', order_id)
            reporter.report_url(u'Ссылка на заказ', order_url)
            reporter.attach(u'Кастомные параметры', main_params)
            return order_id

    @staticmethod
    def put_on_order(order_id, qty):
        request_id = request_steps.RequestSteps.create(order_id, qty)
        invoice_id = invoice_steps.InvoiceSteps.create(request_id)
        invoice_steps.InvoiceSteps.pay(invoice_id)

    # TODO: refactor it! GroupWithoutTransfer, Print
    @staticmethod
    def merge(parent_order, sub_orders_ids, group_without_transfer=1, passport_uid=defaults.PASSPORT_UID):
        value_to_exclude_from_group = [None, 0, -1]
        with reporter.step(
                u'Объединяем в группу Единого счёта. Родительский заказ: {0}, дочерний(е) заказ(ы): {1} {2}.'.format(
                    parent_order,
                    sub_orders_ids,
                    u'без проставления в очередь' if group_without_transfer == 1 else u'с проставлением родительского заказа в очередь разбора')):
            if not parent_order in value_to_exclude_from_group:
                parent_service_order_id = \
                    db.balance().execute('SELECT service_order_id FROM t_order WHERE id =:id', {'id': parent_order})[0][
                        'service_order_id']
            else:
                parent_service_order_id = parent_order
            request_params = []
            for sub_order in sub_orders_ids:
                sub_service_order_id = db.balance().execute(
                    'SELECT service_id, service_order_id, service_code, client_id, agency_id FROM t_order WHERE id =:id',
                    {'id': sub_order})[0]
                sub_service_order_id['GroupServiceOrderID'] = parent_service_order_id
                sub_service_order_id['GroupWithoutTransfer'] = group_without_transfer
                sub_service_order_id['ProductID'] = sub_service_order_id.pop('service_code')
                sub_service_order_id['AgencyID'] = sub_service_order_id.pop('agency_id')
                sub_service_order_id['ServiceOrderID'] = sub_service_order_id.pop('service_order_id')
                sub_service_order_id['ClientID'] = sub_service_order_id.pop('client_id')
                sub_service_order_id['ServiceID'] = sub_service_order_id.pop('service_id')
                request_params.append(sub_service_order_id)
            result = api.medium().CreateOrUpdateOrdersBatch(passport_uid, request_params)
        # reporter.log('GroupOrderID:{0}, sub_orders: {1} {2} '.format(
        #     parent_order, sub_orders_ids,
        #     '(not transferred)' if group_without_transfer == 1 else '(transferred, search by order_id)'))
        return result

    # def exclude_from_union_account([order_ids]):

    @staticmethod
    def ua_enqueue(client_ids, for_dt=None):
        with reporter.step(u'Ставим в очередь UA_TRANSFER на обработку: {0}'.format(client_ids)):
            api.test_balance().UATransferQueue(client_ids, for_dt)
            # reporter.log('UA_ENQUEUE: {0} enqueued'.format(client_ids))

    @staticmethod
    def make_optimized(order_id):
        with reporter.step(u'Делаем Единый Счёт неотключаемым: {0}'.format(order_id)):
            order = db.get_order_by_id(order_id)[0]
            client_id = order['client_id']
            product_id = order['service_code']
            service_id = order['service_id']
            service_order_id = order['service_order_id']
            main_params = {
                'ClientID': client_id,
                'ProductID': product_id,
                'ServiceID': service_id,
                'ServiceOrderID': service_order_id,
                'is_ua_optimize': '1'
            }
            answer = api.medium().CreateOrUpdateOrdersBatch(defaults.PASSPORT_UID, [main_params])
            return answer

    @staticmethod
    def make_optimized_force(order_id):
        with reporter.step(u'Делаем Единый Счёт неотключаемым в базе: {0}'.format(order_id)):
            db.BalanceBO().execute('UPDATE t_order SET is_ua_optimize=1 WHERE id = :order_id', {'order_id': order_id})

    @staticmethod
    @ConfigSteps.temporary_changer('PRIORITY_FOR_UA_TRANSFER', 'value_json')
    def increase_priority(value_json, client_id):
        json_acceptable_string = value_json.replace("'", "\"")
        config = json.loads(json_acceptable_string)
        config['Client_ids'].append(client_id)
        return json.dumps(config)


    @staticmethod
    def transfer(from_orders_list, to_orders_list, output=1, operation_id=None):
        # from_orders_list and to_orders_list is a list of dicts
        # structure is [{'order_id', 'qty_old', 'qty_new', 'all_qty'}]/[{'order_id', 'qty_delta'}]
        with reporter.step(u'Переносим средства с заказа(ов): {0} на заказ(ы): {1}'.format(', '.join(
                u'{0}, средств до переноса: {1} {2}'.format(order['order_id'], order['qty_old'],
                                                            u'все неоткрученное' if order[
                                                                'all_qty'] == 1 else u', средств после переноса: ' + str(
                                                                order['qty_new'])) for order in from_orders_list),
                ', '.join(
                    u'{0}, часть средств на этом заказе:  {1}'.format(
                        order[
                            'order_id'],
                        order[
                            'qty_delta'])
                    for order in
                    to_orders_list))):
            dict = defaults.api_params()
            for order_dict in from_orders_list + to_orders_list:
                order_dict.update(db.balance().execute('SELECT service_id, service_order_id FROM t_order WHERE id =:id',
                                                       {'id': order_dict['order_id']})[0])
                order_dict.pop('order_id')
                for key, value in order_dict.items():
                    order_dict.update({dict[key]: value})
                    del order_dict[key]
            # reporter.log((from_orders_list, to_orders_list))
            api.medium().CreateTransferMultiple(defaults.PASSPORT_UID,
                                                from_orders_list,
                                                to_orders_list, output, operation_id)

    @staticmethod
    def get_order_data_by_client(client_id):
        sql = "SELECT service_id, service_code, consume_sum, consume_qty, completion_qty, contract_id " \
              "FROM t_order WHERE client_id = :client_id"
        params = {'client_id': client_id}
        data = db.balance().execute(sql, params, descr='Ищем данные в t_order по клиенту',
                                    fail_empty=False)
        return data

    @staticmethod
    def get_order_data_and_ids_by_client(client_id):
        sql = "SELECT id, service_id, service_code, consume_sum, consume_qty, completion_qty, contract_id " \
              "FROM t_order WHERE client_id = :client_id"
        params = {'client_id': client_id}
        data = db.balance().execute(sql, params, descr='Ищем данные в t_order по клиенту',
                                    fail_empty=False)
        return data

    @staticmethod
    def make_unmoderated(order_id):
        sql = 'UPDATE t_order SET unmoderated = 1 WHERE id = :order_id'
        params = {'order_id': order_id}
        db.balance().execute(sql, params)

    @staticmethod
    def get_order_id_by_contract(contract_id, service_id, main_order=None):
        query = "select service_order_id from t_order where service_id = :service_id and contract_id = :contract_id"
        params = {'contract_id': contract_id, 'service_id': service_id}
        if main_order is not None:
            query += ' and main_order = :main_order'
            params['main_order'] = main_order

        service_order_id = db.balance().execute(query, params)[0]['service_order_id']

        return service_order_id

    @staticmethod
    def move_order_dt(service_order_id, service_id, delta):
        query = "update t_order t set t.dt = sysdate + :delta " \
                "where t.service_id = :service_id and t.service_order_id = :service_order_id"
        params = {'service_id': service_id, 'service_order_id': service_order_id, 'delta': delta}
        db.balance().execute(query, params)
