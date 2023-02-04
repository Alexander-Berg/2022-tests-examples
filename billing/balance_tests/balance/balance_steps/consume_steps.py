# coding=utf-8
__author__ = 'igogor'

import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils


class ConsumeSteps(object):
    @staticmethod
    def get_consumes_by_client_id(client_id):
        with reporter.step(
                u'Получаем данные консьюмов через джоин T_ORDER, T_INVOICE, T_COSUME для клиента: {}'
                        .format(client_id)):
            query = "SELECT T_CONSUME.ACT_QTY, T_CONSUME.ACT_SUM, T_CONSUME.COMPLETION_QTY, " \
                    "T_CONSUME.COMPLETION_SUM, T_CONSUME.CURRENT_QTY, T_ORDER.SERVICE_CODE, T_INVOICE.TYPE " \
                    "FROM T_CONSUME JOIN T_ORDER ON T_ORDER.ID=T_CONSUME.PARENT_ORDER_ID " \
                    "JOIN T_INVOICE ON T_INVOICE.ID=T_CONSUME.INVOICE_ID " \
                    "WHERE T_ORDER.CLIENT_ID = :client_id AND T_INVOICE.CLIENT_ID = :client_id"

            params = {'client_id': client_id}
            consumes = db.balance().execute(query, params)

            reporter.attach(u'Данные косьюмов', utils.Presenter.pretty(consumes))
            return consumes

    @staticmethod
    def get_consumes_sum_by_client_id(client_id):
        with reporter.step(
                u'Получаем данные суммы консьюмов через джоин T_ORDER, T_INVOICE, T_CONSUME для клиента: {}'
                        .format(client_id)):
            query = "SELECT SUM(T_CONSUME.ACT_QTY) act_qty, SUM(T_CONSUME.ACT_SUM) act_sum, " \
                    "SUM(T_CONSUME.COMPLETION_QTY) completion_qty, SUM(T_CONSUME.COMPLETION_SUM) completion_sum, " \
                    "SUM(T_CONSUME.CURRENT_QTY) current_qty, T_ORDER.SERVICE_CODE, T_INVOICE.TYPE " \
                    "FROM T_CONSUME JOIN T_ORDER ON T_ORDER.ID=T_CONSUME.PARENT_ORDER_ID " \
                    "JOIN T_INVOICE ON T_INVOICE.ID=T_CONSUME.INVOICE_ID " \
                    "WHERE T_ORDER.CLIENT_ID = :client_id AND T_INVOICE.CLIENT_ID = :client_id " \
                    "GROUP BY T_ORDER.SERVICE_CODE, T_INVOICE.TYPE"

            params = {'client_id': client_id}
            consumes = db.balance().execute(query, params)

            reporter.attach(u'Данные суммы косьюмов', utils.Presenter.pretty(consumes))
            return consumes

    @staticmethod
    def get_consumes_sum_by_invoice_contract_id(contract_id):
        with reporter.step(
                u'Получаем данные суммы конзюмов через джоин T_ORDER, T_INVOICE, T_COSUME для договора: {}'
                        .format(contract_id)):
            query = "SELECT SUM(T_CONSUME.ACT_QTY) act_qty, SUM(T_CONSUME.ACT_SUM) act_sum, " \
                    "SUM(T_CONSUME.COMPLETION_QTY) completion_qty, SUM(T_CONSUME.COMPLETION_SUM) completion_sum, " \
                    "SUM(T_CONSUME.CURRENT_QTY) current_qty, T_ORDER.SERVICE_CODE, T_INVOICE.TYPE " \
                    "FROM T_CONSUME JOIN T_ORDER ON T_ORDER.ID=T_CONSUME.PARENT_ORDER_ID " \
                    "JOIN T_INVOICE ON T_INVOICE.ID=T_CONSUME.INVOICE_ID " \
                    "WHERE T_INVOICE.CONTRACT_ID = :contract_id " \
                    "GROUP BY T_ORDER.SERVICE_CODE, T_INVOICE.TYPE"

            params = {'contract_id': contract_id}
            consumes = db.balance().execute(query, params)

            reporter.attach(u'Данные суммы косьюмов', utils.Presenter.pretty(consumes))
            return consumes


    @staticmethod
    def get_consumes_by_client_id_sorted_by_sum(client_id):
        with reporter.step(
                u'Получаем данные консьюмов через джоин T_ORDER, T_INVOICE, T_COSUME для клиента: {}'
                        .format(client_id)):
            query = "SELECT T_CONSUME.ACT_QTY, T_CONSUME.ACT_SUM, T_CONSUME.COMPLETION_QTY, " \
                    "T_CONSUME.COMPLETION_SUM, T_CONSUME.CURRENT_QTY, T_ORDER.SERVICE_CODE, T_INVOICE.TYPE, " \
                    "T_CONSUME.ID " \
                    "FROM T_CONSUME JOIN T_ORDER ON T_ORDER.ID=T_CONSUME.PARENT_ORDER_ID " \
                    "JOIN T_INVOICE ON T_INVOICE.ID=T_CONSUME.INVOICE_ID " \
                    "WHERE T_ORDER.CLIENT_ID = :client_id AND T_INVOICE.CLIENT_ID = :client_id " \
                    "ORDER BY T_CONSUME.COMPLETION_SUM"

            params = {'client_id': client_id}
            consumes = db.balance().execute(query, params)

            reporter.attach(u'Данные косьюмов', utils.Presenter.pretty(consumes))
            return consumes

    @staticmethod
    def get_consumes_by_invoice_id(invoice_id):
        with reporter.step(
                u'Получаем данные консьюмов T_CONSUME для счета: {}'.format(invoice_id)):
            query = "SELECT ID, DT, CONSUME_SUM, INVOICE_ID FROM T_CONSUME WHERE INVOICE_ID = :invoice_id"
            params = {'invoice_id': invoice_id}
            consumes = db.balance().execute(query, params)

            reporter.attach(u'Данные консьюмов', utils.Presenter.pretty(consumes))
            return consumes
