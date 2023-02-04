# coding=utf-8
__author__ = 'igogor'

import datetime
from decimal import Decimal

import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
import client_steps
import contract_steps
import partner_steps
import person_steps
from btestlib.constants import Currencies, Services, Firms, NdsNew as Nds, Regions, Managers, ContractPaymentType, \
    OfferConfirmationType, TaxiOrderType
from btestlib.data.defaults import Taxi
from common_steps import CommonSteps

log_align = 30
# log = reporter.logger()

# еще дефолтные даты есть в btestlib.data.defaults.Date
to_iso = utils.Date.date_to_iso_format
NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
A_DAY_AFTER_TOMORROW = NOW + datetime.timedelta(days=2)


# TODO a-vasin: тут начал происходить какой-то пиздец, это всё надо рефакторить, в том числе TaxiData
class TaxiSteps(object):
    @staticmethod
    def create_order(client_id, order_dt, payment_type, promocode_sum=0, commission_sum=200.12, subsidy_sum=0,
                     currency='RUB', order_type=TaxiOrderType.commission):
        with reporter.step(u'Создаем заказ в такси для клиента: {}, дата: {}'.format(client_id, order_dt)):
            query = "INSERT INTO T_PARTNER_TAXI_STAT_AGGR " \
                    "(DT, CLIENT_ID, COMMISSION_CURRENCY, PAYMENT_TYPE, COMMISSION_SUM, PROMOCODE_SUM, SUBSIDY_SUM, TYPE) " \
                    "VALUES " \
                    "(:order_dt, :client_id, :currency, :payment_type, :commission_sum, :promocode_sum, :subsidy_sum, :order_type)"

            query_params = {'client_id': client_id,
                            'promocode_sum': promocode_sum,
                            'subsidy_sum': subsidy_sum,
                            'order_dt': order_dt,
                            'payment_type': payment_type,
                            'commission_sum': commission_sum,
                            'currency': currency,
                            'order_type': order_type}

            db.balance().execute(query, query_params, descr='Добавляем заказ в t_partner_taxi_stat_aggr')

    @staticmethod
    def _export_to_queue(contract_id, queue, on_dt=None, code=None, forced=False):
        if on_dt is None:
            on_dt = utils.Date.nullify_time_of_date(datetime.datetime.now())

        input_ = {'on_dt': on_dt}
        if code:
            input_['code'] = code
        if forced:
            input_['forced'] = forced

        with reporter.step(u'Запускаем выгрузку в очередь {} (code={}) договора: {}, на дату: {}'
                                   .format(queue, code, contract_id, on_dt)):
            CommonSteps.export(queue, 'Contract', contract_id, input_=input_)

    @classmethod
    def process_offer_activation(cls, contract_id, on_dt=None):
        cls._export_to_queue(contract_id=contract_id, queue='OFFER_ACTIVATION', on_dt=on_dt, code='offer_activation')

    @classmethod
    def process_netting(cls, contract_id, on_dt=None, forced=False):
        cls._export_to_queue(contract_id=contract_id, queue='NETTING', on_dt=on_dt, code='netting', forced=forced)

    @staticmethod
    def create_cash_payment_fact(invoice_eid, amount, dt, type, orig_id=None, source_type=None):
        with reporter.step(u'Вставляем запись об оплате в T_OEBS_CASH_PAYMENT_FACT для счета: {}'.format(invoice_eid)):
            source_id = db.balance().execute("SELECT S_OEBS_CPF_SOURCE_ID_TEST.nextval val FROM dual")[0]['val']
            cash_fact_id = db.balance().execute("SELECT S_OEBS_CASH_PAYMENT_FACT_TEST.nextval val FROM dual")[0]['val']

            query = "INSERT INTO T_OEBS_CASH_PAYMENT_FACT" \
                    "(XXAR_CASH_FACT_ID, AMOUNT, RECEIPT_NUMBER, RECEIPT_DATE, OPERATION_TYPE, LAST_UPDATED_BY, " \
                    "LAST_UPDATE_DATE, CREATED_BY, CREATION_DATE, SOURCE_ID, ORIG_ID, SOURCE_TYPE) VALUES " \
                    "(:cash_fact_id, :amount, :invoice_eid, :dt, :type, " \
                    "-1, :dt, -1, :dt, :source_id, :orig_id, :source_type)"
            params = {
                'invoice_eid': invoice_eid,
                'amount': amount,
                'dt': dt,
                'type': type,
                'source_id': source_id,
                'cash_fact_id': cash_fact_id,
                'orig_id': orig_id,
                'source_type': source_type
            }
            db.balance().execute(query, params)

            return cash_fact_id, source_id

    @staticmethod
    def get_taxi_stat_aggr_tlog(date):
        # assert date
        return db.balance().execute("""
            select 
                transaction_dt, dt, client_id, commission_currency, service_id, amount, type, last_transaction_id,
                 tlog_version
            from bo.t_partner_taxi_stat_aggr_tlog
            where transaction_dt = :date
            order by last_transaction_id
        """, {'date': date})