# coding: utf-8

import datetime

from balance import balance_db as db
from btestlib import reporter
from balance import balance_api as api


class PaymentSteps(object):

    @staticmethod
    def create(invoice_id, payment_method_id, paysys_code, receipt_sum=0, receipt_sum_1c=0, user_account=None,
               transaction_id=None):
        payment_id = db.balance().execute('''select S_PAYMENT_ID.nextval as payment_id from dual''')[0]['payment_id']
        db.balance().execute(
            '''INSERT INTO T_PAYMENT (ID, dt,  INVOICE_ID, PAYSYS_CODE, AMOUNT, PAYMENT_METHOD_ID, USER_ACCOUNT,
            TRANSACTION_ID, RECEIPT_SUM, RECEIPT_SUM_1C, CURRENCY)
             VALUES (:id, :dt, :invoice_id, :paysys_code, 0, :payment_method_id, :user_account,
             :transaction_id, :receipt_sum, :receipt_sum_1c, :currency)''',
            {'invoice_id': invoice_id, 'dt': datetime.datetime.now(), 'id': payment_id, 'paysys_code': paysys_code,
             'payment_method_id': payment_method_id, 'user_account': user_account, 'transaction_id': transaction_id,
             'receipt_sum': receipt_sum, 'receipt_sum_1c': receipt_sum_1c, 'currency': 'RUR'})
        return payment_id

    @staticmethod
    def create_paycash(payment_id, ym_invoice_id, wallet):
        db.balance().execute('''INSERT INTO t_paycash_payment_v3 (ID, YM_INVOICE_ID, WALLET, UNMODERATED) VALUES
         (:id, :ym_invoice_id, :wallet, :unmoderated )''',
                             {'id': payment_id, 'ym_invoice_id': ym_invoice_id, 'wallet': wallet,
                              'unmoderated': 0})

    @staticmethod
    def get_payment_by_invoice_id(invoice_id):
        with reporter.step(u'Получаем платеж для счета: {}'.format(invoice_id)):
            query = "SELECT * FROM t_payment WHERE invoice_id=:invoice_id"
            params = {
                'invoice_id': invoice_id
            }
            return db.balance().execute(query, params)[0]

    @staticmethod
    def get_payment(payment_id):
        with reporter.step(u'Получаем платеж по id: {}'.format(payment_id)):
            query = "SELECT * FROM t_payment WHERE id=:payment_id"
            params = {
                'payment_id': payment_id
            }
            return db.balance().execute(query, params)[0]

    @staticmethod
    def update_payment(payment_id, payout_ready_dt):
        with reporter.step(u'Обновляем дату выплаты платежа {} на {}'.format(payment_id, payout_ready_dt)):
            api.medium().UpdatePayment(
                {
                    'TrustPaymentID': payment_id
                }, {
                    'PayoutReady': payout_ready_dt
                }
            )
