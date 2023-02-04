# coding=utf-8
__author__ = 'igogor'

import datetime
import xmlrpclib
from decimal import Decimal

from common_steps import CommonSteps
import balance.balance_api as api
import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
from btestlib.constants import Firms, Products, Export
import client_steps
import person_steps
import invoice_steps

log_align = 30
# log = reporter.logger()

# еще дефолтные даты есть в btestlib.data.defaults.Date
to_iso = utils.Date.date_to_iso_format
NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
A_DAY_AFTER_TOMORROW = NOW + datetime.timedelta(days=2)


class OverdraftSteps(object):
    @staticmethod
    def calculate_overdraft(client_id):
        with reporter.step(u'Запускаем пересчёт овердрафта:'):
            api.test_balance().calculate_overdraft([client_id])

    @staticmethod
    def overdraft_job(client_id):
        with reporter.step(u'Запускаем overdraft_job:'):
            api.test_balance().ResetOverdraftInvoices([client_id])
            api.test_balance().RefundOrders([client_id])

    @staticmethod
    def reset_overdraft_invoices(client_id):
        with reporter.step(u'Запускаем ResetOverdraftInvoices:'):
            api.test_balance().ResetOverdraftInvoices([client_id])

    @staticmethod
    def refund_overdraft_invoices(client_id):
        with reporter.step(u'Запускаем RefundOrders:'):
            api.test_balance().RefundOrders([client_id])

    @staticmethod
    def run_manual_suspect(client_id):
        with reporter.step(u'Запускаем manual_suspect:'):
            api.test_balance().ManualSuspect([client_id])

    @staticmethod
    def run_overdraft_ban(client_id):
        with reporter.step(u'Запускаем overdraft_ban:'):
            api.test_balance().BanOverdraft([client_id])

    @staticmethod
    def make_client_manual_suspect(client_id):
        db.balance().execute('UPDATE t_client SET manual_suspect =1 WHERE id = :client_id', {'client_id': client_id})

    @staticmethod
    def get_limit_by_client(client_id):
        with reporter.step(u'Считываем лимит овердрафта:'):
            query = "SELECT * FROM t_client_overdraft WHERE client_id = :client_id"
            result = db.balance().execute(query, {'client_id': client_id})
        return result

    @staticmethod
    def get_limit(client_id, service_id):
        with reporter.step(u'Считываем лимит овердрафта:'):
            query = "SELECT * FROM t_client_overdraft " \
                    "WHERE client_id = :client_id AND service_id = :service_id"
            result = db.balance().execute(query, {'client_id': client_id, 'service_id': service_id})
        return result

    @staticmethod
    def set_force_overdraft(client_id, service_id, limit, firm_id=1, start_dt=datetime.datetime.now(), currency=None,
                            limit_wo_tax=None):
        with reporter.step(u'Выдаём "быстрый" овердрафт:'):
            iso_currency = 'RUB' if currency == 'RUR' else currency
            insert_query = """INSERT INTO t_client_overdraft
                              (client_id, service_id, overdraft_limit, firm_id, start_dt, update_dt, currency, 
                              iso_currency, overdraft_limit_wo_tax)
                              VALUES
                              (:client_id, :service_id, :limit, :firm_id, :start_dt, sysdate, :currency, :iso_currency, 
                              :limit_wo_tax)"""
            update_query = """UPDATE t_client_overdraft
                              SET OVERDRAFT_LIMIT = :limit, start_dt = :start_dt, currency = :currency, 
                              iso_currency = :iso_currency, overdraft_limit_wo_tax = :limit_wo_tax
                              WHERE CLIENT_ID = :client_id AND service_id = :service_id AND firm_id = :firm_id"""
            query_params = {'client_id': client_id, 'service_id': service_id, 'limit': limit, 'firm_id': firm_id,
                            'start_dt': start_dt, 'currency': currency, 'iso_currency': iso_currency,
                            'limit_wo_tax': limit_wo_tax}
            try:
                db.balance().execute(insert_query, query_params)
            except xmlrpclib.Fault as e:
                if 'unique constraint (BO.T_CLIENT_OVERDRAFT_PK)' in CommonSteps.get_exception_code(e):
                    db.balance().execute(update_query, query_params)
                else:
                    raise e

        reporter.attach(
            'Force overdraft given to client {0} '
            '(service: {1}, limit: {2}, firm: {3}, dt: {4}, multicurrency: {5})'.format(
                client_id, service_id, limit, firm_id, start_dt, currency))

    @staticmethod
    def set_overdraft_fair(client_id, context, limit, currency=None):
        assert limit % 10 == 0

        MINIMAL_QTY = 1

        CURRENCIES = {Firms.YANDEX_1.id: {'cc': 'RUB',
                                          'fish_price': Decimal('30'),
                                          'nds_pct': Decimal('1.18'),
                                          'currency_rate': lambda act_dt, currency: Decimal('1')},
                      Firms.MARKET_111.id: {'cc': 'RUB',
                                            'fish_price': Decimal('30'),
                                            'nds_pct': Decimal('1.18'),
                                            'currency_rate': lambda act_dt, currency: Decimal('1')},
                      Firms.YANDEX_UA_2.id: {'cc': 'UAH',
                                             'fish_price': Decimal('12'),
                                             'nds_pct': Decimal('1.2'),
                                             'currency_rate': lambda act_dt, currency: Decimal(
                                                 db.get_currency_rate(act_dt, currency, 1000)[0]['rate'])},
                      Firms.REKLAMA_BEL_27.id: {'cc': 'BYN',
                                                'fish_price': Decimal('12'),
                                                'nds_pct': Decimal('1.2'),
                                                'currency_rate': lambda act_dt, currency: Decimal(
                                                    db.get_currency_rate(act_dt, currency.upper(), 1000)[0]['rate'])},
                      Firms.KZ_25.id: {'cc': 'KZU',
                                       'fish_price': Decimal('12'),
                                       'nds_pct': Decimal('1.12'),
                                       'currency_rate': lambda act_dt, currency: Decimal('1')},

                      Firms.VERTICAL_12.id: {'cc': 'RUB',
                                             'fish_price': Decimal('30'),
                                             'nds_pct': Decimal('1.18'),
                                             'currency_rate': lambda act_dt, currency: Decimal('1')},
                      }

        # вычисляет сумму акта в деньгах для последнего акта или в фишках для 1475
        def calculate_act_sum_for_last_act(act_list, firm_id, service_id, act_dt, currency, product_id,
                                           limit, is_currency=True):
            acted_qty, acted_amount, acted_amount_nds, fake_fish, money_sum = 0, 0, 0, 0, 0
            nds_pct = CURRENCIES[firm_id]['nds_pct']
            fish_price_ru_nds_included = CURRENCIES[Firms.YANDEX_1.id]['fish_price']
            nds_pct_ru = CURRENCIES[Firms.YANDEX_1.id]['nds_pct']
            for act in act_list:
                currency_rate = Decimal(db.get_act_by_id(act)[0]['currency_rate'])
                act_trans_lines_ids = [act_trans_line['id'] for act_trans_line in db.get_act_trans_by_act(act)]
                for act_trans_line_id in act_trans_lines_ids:
                    act_trans_line = db.get_act_trans_by_id(act_trans_line_id)[0]
                    # расчет количества в фишках ранее выставленных актов
                    acted_qty += Decimal(act_trans_line['act_qty'])
                    # расчет суммы в деньгах включая НДС
                    money_sum += Decimal(act_trans_line['amount'])
                    # расчет суммы в деньгах не включая НДС
                    acted_amount_without_nds = Decimal(act_trans_line['amount']) - Decimal(act_trans_line['amount_nds'])
                    # расчет лимита в псевдофишках
                    fake_fish += (acted_amount_without_nds * currency_rate) / fish_price_ru_nds_included / nds_pct_ru
            if not is_currency:
                # для фишечных
                if product_id == Products.DIRECT_FISH.id:
                    # для директа последний акт выставляем на количество
                    # (лимит - количество в фишках ранее выставленных актов)
                    return limit - acted_qty
                else:
                    # для остальных фишечных последний акт выставляем на количество (лимит - количество в псевдофишках)
                    currency_rate_last_act = CURRENCIES[firm_id]['currency_rate'](act_dt, currency.lower())
                    remain = (
                                 limit - fake_fish) * fish_price_ru_nds_included / nds_pct_ru * nds_pct / currency_rate_last_act
                    return remain
            else:
                # для валютных
                remain = limit - money_sum
                # если НДС не включен в цену, отнимаем НДС
                if currency not in ('RUB', 'KZT'):
                    remain = remain / nds_pct
                return remain

        def calculate_act_qty_for_last_act(act_list, firm_id, service_id, act_dt, currency, product_id, limit):
            fish_price_in_currency = CURRENCIES[firm_id]['fish_price']
            sum = calculate_act_sum_for_last_act(act_list, firm_id, service_id, act_dt, currency, product_id,
                                                 is_currency=False, limit=limit)
            if product_id != Products.DIRECT_FISH.id:
                return sum / fish_price_in_currency
            else:
                return sum

        act_dt_delta = utils.Date.shift_date
        ACT_DATES = [act_dt_delta(NOW, months=-7),
                     act_dt_delta(NOW, months=-5),
                     act_dt_delta(NOW, months=-4),
                     act_dt_delta(NOW, months=-3),
                     act_dt_delta(NOW, months=-2)]

        act_list = []
        person_id = person_steps.PersonSteps.create(client_id, context.person_type.code)
        if currency:
            if currency:
                client_steps.ClientSteps.create(
                    {'CLIENT_ID': client_id, 'REGION_ID': context.region.id, 'CURRENCY': context.currency.iso_code,
                     'MIGRATE_TO_CURRENCY': NOW + datetime.timedelta(seconds=5),
                     'SERVICE_ID': context.service.id, 'CURRENCY_CONVERT_TYPE': 'COPY'})
                CommonSteps.export('MIGRATE_TO_CURRENCY', 'Client', client_id)
        # создаем акт 6 месяцев назад и еще 3 акта (5, 4, 3 месяца назад) на одну фишку или валюту
        for act_date in ACT_DATES[0:-1]:
            invoice_id, act_id = invoice_steps.InvoiceSteps.create_invoice_with_act(context, client_id, person_id,
                                                                                    act_date,
                                                                                    MINIMAL_QTY)
            act_list.append(act_id)
        turnover = limit * 12
        if currency:
            act_qty = calculate_act_sum_for_last_act(act_list[1:], context.firm.id, context.service.id,
                                                     utils.Date.last_day_of_month(ACT_DATES[-1]),
                                                     currency=CURRENCIES[context.firm.id]['cc'],
                                                     product_id=context.product.id, limit=turnover)
        else:
            act_qty = calculate_act_qty_for_last_act(act_list[1:], context.firm.id, context.service.id,
                                                     utils.Date.last_day_of_month(ACT_DATES[-1]),
                                                     currency=CURRENCIES[context.firm.id]['cc'],
                                                     product_id=context.product.id, limit=turnover)
        invoice_steps.InvoiceSteps.create_invoice_with_act(context, client_id, person_id,
                                                           ACT_DATES[-1], utils.dround(act_qty, decimal_places=6))
        api.test_balance().Enqueue('Client', client_id, 'OVERDRAFT')
        OverdraftSteps.export_client(client_id, with_enqueue=True)

    @staticmethod
    def expire_overdraft_invoice(invoice_id, delta=1):
        with reporter.step(u'Делаем просроченным овердрафтный счет'):
            dt = datetime.datetime.now()
            PAYMENT_TERM_DT = dt - datetime.timedelta(days=delta)
            db.BalanceBO().execute('UPDATE t_invoice SET payment_term_dt = :PAYMENT_TERM_DT WHERE id=:invoice_id',
                                   {'invoice_id': invoice_id, 'PAYMENT_TERM_DT': PAYMENT_TERM_DT})

    @staticmethod
    def set_payment_term_dt(invoice_id, dt):
        db.BalanceBO().execute('UPDATE t_invoice SET payment_term_dt = :PAYMENT_TERM_DT WHERE id=:invoice_id',
                               {'invoice_id': invoice_id, 'PAYMENT_TERM_DT': dt})

    @staticmethod
    def enqueue_client(client_id):
        api.test_balance().Enqueue('Client', client_id, 'OVERDRAFT')

    @staticmethod
    def export_client(client_id, with_enqueue=False, input_=None):
        CommonSteps.export(Export.Type.OVERDRAFT, Export.Classname.CLIENT, client_id, with_enqueue=with_enqueue,
                           input_=input_)

    @staticmethod
    def set_overdraft_params(person_id, client_limit, service_id=7, payment_method='bank', iso_currency='RUB'):
        return api.medium().SetOverdraftParams({'PersonID': person_id, 'ServiceID': service_id,
                                                'PaymentMethodCC': payment_method, 'ClientLimit': client_limit,
                                                'Currency': iso_currency})

    @staticmethod
    def get_overdraft_params(client_id, service_id=7):
        return api.medium().GetOverdraftParams(service_id, client_id)
