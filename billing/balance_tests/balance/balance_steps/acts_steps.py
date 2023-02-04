# coding=utf-8
__author__ = 'igogor'

import datetime

from common_steps import CommonSteps
import balance.balance_api as api
import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
from btestlib.constants import Export
from btestlib.data import defaults
from btestlib import config as balance_config
import client_steps
import invoice_steps
import export_steps

NO_INVOICE_ID = 0


class ActsSteps(object):
    # TODO: refactor it!
    @staticmethod
    def create_acts(client_id=None, acts_info_list=[defaults.act()]):
        '''
        Если не задан, создать клиента
        Если задан счет, проверить есть ли счет у элементов списка
        '''
        if not client_id:
            client_id = client_steps.ClientSteps.create()['client_id']
        #
        for act_info in acts_info_list:
            act_info.setdefault('invoice_id', NO_INVOICE_ID)

        acts_by_invoice = {}
        for act_info in acts_info_list:
            acts_by_invoice.setdefault(act_info.get('invoice_id'), []).append(act_info)

        for invoice_id, act_info in acts_by_invoice:
            if not invoice_id:
                sum = reduce(lambda x, y: x['sum'] + y['sum'], act_info)
                invoice_id = invoice_steps.InvoiceSteps.create(client_id=client_id)  # TODO проверить, что работает как ожидается

    @staticmethod
    def create(invoice_id, act_dt=None):
        with reporter.step(u'Выставляем акт по счёту {0} на {1}'.format(invoice_id, act_dt or u'сегодня')):
            act_id = api.test_balance().OldAct(invoice_id, act_dt)
            # reporter.log(('{0:<' + str(log_align) + '} | {1}, {2}').format('create_act: done',
            #                                                                "'InvoiceID': {0}".format(invoice_id),
            #                                                                "'ActDT': {0}".format(act_dt)))
            return act_id

    @staticmethod
    def generate(client_id, force=1, date=None, with_coverage=False, prevent_oebs_export=False):
        with reporter.step(u'Запускаем {0} генерацию актов по клиенту {1} на {2}'.format(
                u'ежемесячную' if force else u'ежедневную'
                , client_id
                , date or u'сегодня')):
            date = date or datetime.datetime.now()
            act_list = api.test_balance().ActAccounter(client_id, force, date, with_coverage)

        if prevent_oebs_export:
            for act_id in act_list:
                export_steps.ExportSteps.prevent_auto_export(act_id, Export.Classname.ACT)

        # Для коллег из OEBS: публикуем ID объекта в отчёте, выгружаем объект в OEBS и публикуем лог выгрузки
        if balance_config.TRUST_ME_I_AM_OEBS_QA:
            for act_id in act_list:
                export_steps.ExportSteps.export_oebs(act_id=act_id)

        return act_list

    @staticmethod
    def enqueue(clients_list, force, date):
        with reporter.step(u'Ставим клиентов {0} в очередь MONTH_PROC на {1} генерацию на {2}'.format(clients_list,
                                                                                                      u'ежемесячную' if force else u'ежедневную',
                                                                                                      date or u'сегодня')):
            api.test_balance().ActEnqueuer(clients_list, date, force)
            # reporter.log('Clients {0} enqueued on {1} with force {2}'.format(clients_list, date, force))

    @staticmethod
    def hide(act_id):
        with reporter.step(u'Удаляем акт {0}'.format(act_id)):
            api.test_balance().HideAct(act_id)
            # reporter.log('Act {0} hidden'.format(act_id))

    @staticmethod
    def hide_force(act_id):
        with reporter.step(u'Удаляем акт {0}'.format(act_id)):
            db.balance().execute('UPDATE t_act_internal SET hidden = 4 WHERE id = :act_id', {'act_id': act_id})
            # reporter.log('Act {0} hidden'.format(act_id))

    @staticmethod
    def unhide(act_id):
        with reporter.step(u'Возвращаем удалённый акт {0}'.format(act_id)):
            api.test_balance().UnhideAct(act_id)
            # reporter.log('Act {0} unhidden'.format(act_id))

    @staticmethod
    def set_endbuyer_budget(passport_id, type, object_id, contract_id, period_dt, endbuyer_id, sum, priority, hidden=0,
                            agency_id=None):
        with reporter.step(u'Устанавливаем лимиты для конечных покупателей:'):
            budget_id = CommonSteps.next_sequence_id('s_endbuyer_budget_id')
            query = 'INSERT INTO t_endbuyer_budget (ID,PASSPORT_ID,UPDATE_DT,ENDBUYER_ID,PERIOD_DT,SUM,ACT_SUM,HIDDEN,contract_id,allow_negative_row) \
                     VALUES (:budget_id,:passport_id,sysdate,:endbuyer_id,:period_dt,:sum,0,:hidden, :contract_id, 1)'
            query_params = {'budget_id': budget_id,
                            'passport_id': passport_id,
                            'endbuyer_id': endbuyer_id,
                            'period_dt': period_dt,
                            'sum': sum,
                            'hidden': hidden,
                            'contract_id': contract_id}
            db.balance().execute(query, query_params)
            if type.upper() == 'ORDER':
                order_budget_id = CommonSteps.next_sequence_id('s_endbuyer_client_id')
                query = "INSERT INTO t_endbuyer_order (id, passport_id, update_dt, order_id, budget_id, priority, hidden) \
                         VALUES (:order_budget_id, :passport_id, sysdate, :order_id, :budget_id, :priority, :hidden)"
                query_params = {'order_budget_id': order_budget_id,
                                'passport_id': passport_id,
                                'order_id': object_id,
                                'budget_id': budget_id,
                                'priority': priority,
                                'hidden': hidden}
                db.balance().execute(query, query_params)
                reporter.attach('Created ORDER limit: {0} with budget {1}'.format(order_budget_id, budget_id))
            elif type.upper() == 'SUBCLIENT':
                subclient_budget_id = CommonSteps.next_sequence_id('s_endbuyer_client_id')
                query = "INSERT INTO t_endbuyer_subclient (id, passport_id, update_dt, agency_id, client_id, budget_id, priority, hidden) \
                         VALUES (:subclient_budget_id, :passport_id, sysdate, :agency_id, :client_id, :budget_id, :priority, :hidden)"
                query_params = {'subclient_budget_id': subclient_budget_id,
                                'passport_id': passport_id,
                                'client_id': object_id,
                                'agency_id': agency_id,
                                'budget_id': budget_id,
                                'priority': priority,
                                'hidden': hidden}
                db.balance().execute(query, query_params)
                reporter.attach('Created SUBCLIENT limit: {0} with budget {1}'.format(subclient_budget_id, budget_id))
            else:
                raise Exception(u"Unknown budget type. Should be one of the ['Order', 'Subclient']")

    @staticmethod
    def get_act_data_by_client(client_id, internal=False):
        table_name = "t_act_internal" if internal else "t_act"
        sql = "SELECT dt, amount, amount_nds, act_sum, type FROM " + table_name + " WHERE client_id = :client_id ORDER BY dt"
        params = {'client_id': client_id}
        return db.balance().execute(sql, params, descr='Ищем данные в t_act по клиенту', fail_empty=False)

    @staticmethod
    def get_act_data_by_client_with_invoice(client_id, dt):
        sql = "SELECT dt, invoice_id, amount, act_sum, type FROM t_act WHERE client_id = :client_id AND dt = :dt"
        params = {'client_id': client_id, 'dt': dt}
        return db.balance().execute(sql, params, descr='Ищем данные в t_act по клиенту',
                                    fail_empty=False)

    @staticmethod
    def get_act_external_id(act_id):
        with reporter.step(u"Получаем внешний ID акта по внутреннему"):
            query = "SELECT external_id FROM T_ACT WHERE id =:act_id"
            params = {'act_id': act_id}

            result = db.balance().execute(query, params, single_row=True)
            external_id = result['external_id']
            reporter.attach(u"Внешний ID акта", utils.Presenter.pretty(external_id))

            return external_id

    @staticmethod
    def set_payment_term_dt(act_id, dt):
        db.balance().execute('UPDATE t_act_internal SET payment_term_dt = :dt WHERE id = :act_id',
                             {'act_id': act_id, 'dt': dt})

    @staticmethod
    def get_act_data_with_contract_by_client(client_id):
        sql = "SELECT t_act.dt, t_act.amount, t_act.act_sum, t_act.type, T_INVOICE.CONTRACT_ID FROM t_act, T_INVOICE " \
              "WHERE t_act.client_id = :client_id AND t_act.INVOICE_ID = T_INVOICE.ID"
        params = {'client_id': client_id}

        return db.balance().execute(sql, params)

    @staticmethod
    def get_act_data_by_contract(contract_id, include_internal=False):
        act_table = 't_act_internal' if include_internal else 't_act'
        sql = "SELECT {act_table}.dt, {act_table}.act_sum, {act_table}.amount, {act_table}.type, t_invoice.contract_id, t_invoice.currency " \
              "FROM {act_table}, t_invoice " \
              "WHERE t_invoice.contract_id = :contract_id AND {act_table}.invoice_id = t_invoice.id".format(act_table=act_table)
        params = {'contract_id': contract_id}

        return db.balance().execute(sql, params)

    @staticmethod
    def get_act_retrodiscount(act_id):
        sql = "select amount as retroDsc from t_act_refundment where act_id=:act_id"
        params = {'act_id': act_id}
        act_amt = db.balance().execute(sql, params)
        return None if len(act_amt) == 0 else act_amt[0]

    @staticmethod
    def get_act_amount_by_act_id(act_id):
        sql = "select amount from t_act where id=:act_id"
        params = {'act_id': act_id}
        return db.balance().execute(sql, params)

    @staticmethod
    def get_act_data_by_id(act_id):
        sql = "select * from t_act where id=:act_id"
        params = {'act_id': act_id}
        return db.balance().execute(sql, params)[0]

    # a-vasin: что-то многовато методов, надо бы почистить
    @staticmethod
    def get_all_act_data(client_id, dt=None):
        sql = "SELECT * FROM t_act WHERE client_id = :client_id"
        params = {
            'client_id': client_id
        }

        if dt:
            sql += " AND dt = :dt"
            params['dt'] = dt

        return db.balance().execute(sql, params)
