# -*- coding: utf-8 -*-

import datetime
from collections import defaultdict
from decimal import Decimal
from random import randint

import pytest
from hamcrest import anything

import acts_steps
import balance.balance_api as api
import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
import client_steps
import common_data_steps
import contract_steps
import invoice_steps
import order_steps
import partner_steps
import export_steps
from btestlib.constants import CorpTaxiOrderType
from btestlib.constants import Currencies
from btestlib.constants import Export
from btestlib.constants import Firms
from btestlib.constants import NdsNew
from btestlib.constants import PaymentType
from btestlib.constants import Products
from btestlib.constants import Regions
from btestlib.constants import Services
from btestlib.constants import TaxiOrderType
from btestlib.constants import TransactionType
from btestlib.data.defaults import TaxiNetting
from btestlib.data.defaults import TaxiNewPromo as Taxi
from btestlib.data.defaults import TaxiPayment
import btestlib.config as balance_config
import btestlib.data.partner_contexts as pc
from btestlib.matchers import close_to
from btestlib.matchers import contains_dicts_equal_to
from btestlib.matchers import contains_dicts_with_entries
from common_steps import CommonSteps


# будет использоваться по дефолту в параметризации теста (context, is_offer)
DEFAULT_PARAMETRIZATION = "context, is_offer"
DEFAULT_TAXI_CONTEXTS = [
    (pc.TAXI_RU_CONTEXT, 1),
    (pc.TAXI_BV_GEO_USD_CONTEXT, 0),
    (pc.TAXI_BV_LAT_EUR_CONTEXT, 1),
    (pc.TAXI_UBER_BV_AZN_USD_CONTEXT, 0),
    (pc.TAXI_UBER_BV_BYN_AZN_USD_CONTEXT, 0),
    (pc.TAXI_UBER_BV_BY_BYN_CONTEXT, 1),
    (pc.TAXI_UBER_BV_BYN_BY_BYN_CONTEXT, 1),
    (pc.TAXI_ISRAEL_CONTEXT, 0),
    (pc.TAXI_YANGO_ISRAEL_CONTEXT, 0),
    (pc.TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT, 1),
    (pc.TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT, 0),
    (pc.TAXI_GHANA_USD_CONTEXT, 0),
    (pc.TAXI_BOLIVIA_USD_CONTEXT, 0),
    (pc.TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT, 1),
    (pc.TAXI_ZA_USD_CONTEXT, 0),
    (pc.TAXI_YANDEX_GO_SRL_CONTEXT, 1),
    (pc.TAXI_BV_NOR_NOK_CONTEXT, 1),
    (pc.TAXI_BV_COD_EUR_CONTEXT, 1),
    (pc.TAXI_MLU_EUROPE_SWE_SEK_CONTEXT, 1),
    (pc.TAXI_ARM_AZ_USD_CONTEXT, 0),
    (pc.TAXI_ARM_GEO_USD_CONTEXT, 0),
    (pc.TAXI_ARM_KGZ_USD_CONTEXT, 0),
    (pc.TAXI_ARM_GHA_USD_CONTEXT, 0),
    (pc.TAXI_ARM_ZAM_USD_CONTEXT, 0),
    (pc.TAXI_ARM_UZB_USD_CONTEXT, 0),
    (pc.TAXI_ARM_CMR_EUR_CONTEXT, 0),
    (pc.TAXI_ARM_SEN_EUR_CONTEXT, 0),
    (pc.TAXI_ARM_CIV_EUR_CONTEXT, 0),
    (pc.TAXI_ARM_ANG_EUR_CONTEXT, 0),
    (pc.TAXI_ARM_MD_EUR_CONTEXT, 0),
    (pc.TAXI_ARM_RS_EUR_CONTEXT, 0),
    (pc.TAXI_ARM_LT_EUR_CONTEXT, 0),
    (pc.TAXI_ARM_FIN_EUR_CONTEXT, 0),
    # (pc.TAXI_ARM_BY_BYN_CONTEXT, 0),
    # (pc.TAXI_ARM_NOR_NOK_CONTEXT, 0),
    # (pc.TAXI_ARM_ARM_AMD_CONTEXT_RESIDENT, 0)
]

# TODO: разметить тесты для аудита
DEFAULT_TAXI_CONTEXTS_WITH_MARKS = [
    pytest.mark.smoke((pc.TAXI_RU_CONTEXT, 1)),
    (pc.TAXI_BV_GEO_USD_CONTEXT, 0),
    (pc.TAXI_BV_LAT_EUR_CONTEXT, 1),
    (pc.TAXI_UBER_BV_AZN_USD_CONTEXT, 0),
    (pc.TAXI_UBER_BV_BYN_AZN_USD_CONTEXT, 0),
    (pc.TAXI_UBER_BV_BY_BYN_CONTEXT, 1),
    (pc.TAXI_UBER_BV_BYN_BY_BYN_CONTEXT, 1),
    (pc.TAXI_ISRAEL_CONTEXT, 0),
    (pc.TAXI_YANGO_ISRAEL_CONTEXT, 0),
    (pc.TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT, 1),
    (pc.TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT, 0),
    (pc.TAXI_GHANA_USD_CONTEXT, 0),
    (pc.TAXI_BOLIVIA_USD_CONTEXT, 0),
    (pc.TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT, 1),
    (pc.TAXI_ZA_USD_CONTEXT, 0),
    (pc.TAXI_YANDEX_GO_SRL_CONTEXT, 1),
    (pc.TAXI_BV_NOR_NOK_CONTEXT, 1),
    (pc.TAXI_BV_COD_EUR_CONTEXT, 1),
    (pc.TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT, 0),
    (pc.TAXI_MLU_EUROPE_CONGO_EUR_CONTEXT, 0),
    (pc.TAXI_MLU_EUROPE_ALGERIA_EUR_CONTEXT, 0),
    (pc.TAXI_ARM_AZ_USD_CONTEXT, 0),
    (pc.TAXI_ARM_GEO_USD_CONTEXT, 0),
    (pc.TAXI_ARM_KGZ_USD_CONTEXT, 0),
    (pc.TAXI_ARM_GHA_USD_CONTEXT, 0),
    (pc.TAXI_ARM_ZAM_USD_CONTEXT, 0),
    (pc.TAXI_ARM_UZB_USD_CONTEXT, 0),
    (pc.TAXI_ARM_CMR_EUR_CONTEXT, 0),
    (pc.TAXI_ARM_SEN_EUR_CONTEXT, 0),
    (pc.TAXI_ARM_CIV_EUR_CONTEXT, 0),
    (pc.TAXI_ARM_ANG_EUR_CONTEXT, 0),
    (pc.TAXI_ARM_MD_EUR_CONTEXT, 0),
    (pc.TAXI_ARM_RS_EUR_CONTEXT, 0),
    (pc.TAXI_ARM_LT_EUR_CONTEXT, 0),
    (pc.TAXI_ARM_FIN_EUR_CONTEXT, 0),
    # (pc.TAXI_ARM_BY_BYN_CONTEXT, 0),
    # (pc.TAXI_ARM_NOR_NOK_CONTEXT, 0),
    # (pc.TAXI_ARM_ARM_AMD_CONTEXT_RESIDENT, 0)
]

CORP_TAXI_CURRENCY_TO_ORDER_TYPES_PRODUCTS_MAP = {
    Currencies.RUB: [
        (CorpTaxiOrderType.commission, Products.CORP_TAXI_CLIENTS_RUB.id),
        (CorpTaxiOrderType.cargo_commission, Products.CORP_TAXI_CARGO_RUB.id),
        (CorpTaxiOrderType.delivery_commission, Products.CORP_TAXI_DELIVERY_RUB.id),
    ],
    Currencies.KZT: [
        (CorpTaxiOrderType.commission, Products.CORP_TAXI_CLIENTS_KZT.id),
        (CorpTaxiOrderType.delivery_commission, Products.CORP_TAXI_DELIVERY_KZT.id),
    ],
    Currencies.ILS: [
        (CorpTaxiOrderType.commission,  Products.CORP_TAXI_CLIENTS_ILS.id),
        (CorpTaxiOrderType.delivery_commission, Products.CORP_TAXI_DELIVERY_ILS.id),
    ],
    Currencies.BYN: [
        (CorpTaxiOrderType.commission, Products.CORP_TAXI_CLIENTS_BYN.id),
        (CorpTaxiOrderType.cargo_commission, Products.CORP_TAXI_CARGO_BYN.id),
        (CorpTaxiOrderType.delivery_commission, Products.CORP_TAXI_DELIVERY_BYN.id),
    ],
    Currencies.KGS: [
        (CorpTaxiOrderType.commission, Products.CORP_TAXI_CLIENTS_KGS.id),
    ],
}


class OrdersDataList(list):
    @property
    def max_last_transaction_ids(self):
        result = defaultdict(int)
        for order_dict in self:
            result[order_dict['service_id']] = \
                max(order_dict['last_transaction_id'], result[order_dict['service_id']])
            result['all_services'] = \
                max(order_dict['last_transaction_id'], result['all_services'])
        return result


class TaxiSteps(object):
    @staticmethod
    def get_completions_taxi_invoice(contract_id):
        with reporter.step(u'Находим id ЛС такси для договора: {}'.format(contract_id)):
            query = "select inv.id, inv.external_id " \
                    "from t_invoice inv left join t_extprops prop on " \
                    "inv.id = prop.object_id and " \
                    "prop.classname='PersonalAccount' and prop.attrname='service_code' " \
                    "where inv.type='personal_account' and inv.contract_id=:contract_id " \
                    "and prop.value_str = 'YANDEX_SERVICE' "

            params = {'contract_id': contract_id}
            invoice = db.balance().execute(query, params, single_row=True)
            return invoice['id'], invoice['external_id']

    @staticmethod
    def pay_to_personal_account(amount, contract_id):
        with reporter.step(u'Зачисляем средства: {} на лицевой счет договора: {}'.format(amount, contract_id)):
            invoice_id, _ = TaxiSteps.get_completions_taxi_invoice(contract_id)

            invoice_steps.InvoiceSteps.pay(invoice_id, amount)

    @staticmethod
    def _export_to_queue(contract_id, queue, on_dt=None, code=None, force_netting=True):
        if on_dt is None:
            on_dt = utils.Date.nullify_time_of_date(datetime.datetime.now())

        input_ = {'on_dt': on_dt}
        if code:
            input_['code'] = code
        if force_netting:
            input_['force_netting'] = 1

        with reporter.step(u'Запускаем выгрузку в очередь {} (code={}) договора: {}, на дату: {}'
                                   .format(queue, code, contract_id, on_dt)):
            CommonSteps.export(queue, 'Contract', contract_id, input_=input_)

    @classmethod
    def process_offer_activation(cls, contract_id, on_dt=None):
        cls._export_to_queue(contract_id=contract_id, queue='OFFER_ACTIVATION', on_dt=on_dt, code='offer_activation')

    @classmethod
    def process_netting(cls, contract_id, on_dt=None):
        cls._export_to_queue(contract_id=contract_id, queue='NETTING', on_dt=on_dt, code='netting')

    @staticmethod
    def create_order(client_id, **kwargs):
        # if value in default_kwargs is type, then param is necessary and method will check type of it
        # if value in default_kwargs is object, then param is not necessary and will be filled by value object if not passed
        default_kwargs = {
            'dt': datetime.datetime,
            'currency': basestring,
            'payment_type': basestring,
            'order_type': basestring,
            'commission_sum': 0,
            'subsidy_sum': 0,
            'promocode_sum': 0
        }
        order_data = {'client_id': client_id}
        for key, value in default_kwargs.items():
            if type(value) is type:
                order_data[key] = kwargs[key]
                assert isinstance(order_data[key], value)
            else:
                order_data[key] = kwargs.get(key, default_kwargs[key])

        with reporter.step(u'Создаем заказ в такси для клиента: {}, дата: {}'.format(client_id, order_data['dt'])):
            query = "INSERT INTO T_PARTNER_TAXI_STAT_AGGR " \
                    "(DT, CLIENT_ID, COMMISSION_CURRENCY, PAYMENT_TYPE, COMMISSION_SUM, PROMOCODE_SUM, SUBSIDY_SUM, TYPE) " \
                    "VALUES " \
                    "(:dt, :client_id, :currency, :payment_type, :commission_sum, :promocode_sum, :subsidy_sum, :order_type)"

            query_params = order_data
            db.balance().execute(query, query_params, descr='Добавляем заказ в t_partner_taxi_stat_aggr')

    @staticmethod
    def create_order_tlog(client_id, **kwargs):
        # if value in default_kwargs is type, then param is necessary and method will check type of it
        # if value in default_kwargs is object, then param is not necessary and will be filled by value object if not passed
        default_kwargs = {
            'dt': datetime.datetime,
            'transaction_dt': datetime,
            'currency': basestring,
            'service_id': int,
            'type': basestring,
            'amount': Decimal,
            'last_transaction_id': None,
            'tlog_version': None
        }
        order_data = {'client_id': client_id}
        for key, value in default_kwargs.items():
            if type(value) is type:
                order_data[key] = kwargs[key]
                assert isinstance(order_data[key], value)
            else:
                order_data[key] = kwargs.get(key, default_kwargs[key])

        with reporter.step(u'Создаем заказ tlog в такси для клиента: {}, дата: {}'.format(client_id, order_data['dt'])):
            query = "INSERT INTO T_PARTNER_TAXI_STAT_AGGR_TLOG " \
                    "(DT, TRANSACTION_DT, CLIENT_ID, COMMISSION_CURRENCY, SERVICE_ID, AMOUNT, TYPE, LAST_TRANSACTION_ID, TLOG_VERSION) " \
                    "VALUES " \
                    "(:dt, :transaction_dt, :client_id, :currency, :service_id, :amount, :type, :last_transaction_id, :tlog_version)"

            query_params = order_data
            db.balance().execute(query, query_params, descr='Добавляем заказ в t_partner_taxi_stat_aggr_tlog')

    @staticmethod
    def create_orders(client_id, orders_data):
        with reporter.step(u'Добавляем открутки по такси для клиента: {}'.format(client_id)):
            for order_dict in orders_data:
                TaxiSteps.create_order(client_id, **order_dict)

    @staticmethod
    def create_orders_tlog(client_id, orders_data):
        with reporter.step(u'Добавляем открутки tlog по такси для клиента: {}'.format(client_id)):
            for order_dict in orders_data:
                TaxiSteps.create_order_tlog(client_id, **order_dict)

    @staticmethod
    def generate_acts(client_id, contract_id, completion_dt, force_month_proc=True):
        with reporter.step(u'Запускаем генерацию актов и экспорт для клиента: {}, договора: {}'
                                   .format(client_id, contract_id)):
            partner_steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, completion_dt)
            if force_month_proc:
                CommonSteps.export('MONTH_PROC', 'Client', client_id)

    @staticmethod
    def get_completions_from_view(contract_id, start_dt=None, end_dt=None):
        # "Тупой" метод, работает корректно только в случае, когда сумма комиссии больше суммы промокодов и субсидий.
        # В большинстве случаев так и есть, и это ок. В противном случае в коде баланса идет перераспределение
        # полученных минусов по другим продуктам, в тестах эти кейсы должны быть учтены руками.
        query = "SELECT round(sum(ptc.commission_sum),2) " \
                "- round(sum(ptc.subsidy_sum),2) " \
                "- round(sum(ptc.promocode_sum),2) AS amount, pp.product_id " \
                "FROM v_partner_taxi_completion ptc " \
                "JOIN t_partner_product pp " \
                "ON pp.service_id = ptc.service_id " \
                "AND pp.currency_iso_code = ptc.iso_currency " \
                "AND decode(ptc.ORDER_TYPE, 'subvention', 'order', ptc.ORDER_TYPE) = pp.ORDER_TYPE " \
                "AND ptc.contract_id = :contract_id "
        if end_dt:
            query = query + "AND ptc.dt < :end_dt "
        if start_dt:
            query = query + "AND ptc.dt >= :start_dt "
        query = query + "GROUP BY pp.product_id"
        params = {'contract_id': contract_id, 'end_dt': end_dt, 'start_dt': start_dt}
        amount_list = db.balance().execute(query, params, descr='Получаем сумму откруток из v_partner_taxi_completion')
        total_amount = Decimal(0)
        for item in amount_list:
            total_amount += Decimal(item['amount'])
            item['amount'] = Decimal(item['amount']) if item['amount'] else Decimal('0')
        return amount_list, total_amount

    @staticmethod
    def get_completions_from_view_tlog(contract_id, start_dt=None, end_dt=None,
                                       transaction_start_dt=None, transaction_end_dt=None):
        # "Тупой" метод, работает корректно только в случае, когда сумма комиссии больше суммы промокодов и субсидий.
        # В большинстве случаев так и есть, и это ок. В противном случае в коде баланса идет перераспределение
        # полученных минусов по другим продуктам, в тестах эти кейсы должны быть учтены руками.
        query = "SELECT " \
                "  round(sum(ptc.amount * decode(ptc.ORDER_TYPE, 'subvention', -1, 'coupon', -1, 1)), 2) AS amount, " \
                "  pp.product_id " \
                "FROM v_partner_taxi_completion_tlog ptc " \
                "JOIN t_partner_product pp " \
                "  ON pp.service_id = ptc.service_id " \
                "  AND pp.currency_iso_code = ptc.iso_currency " \
                "  AND decode(ptc.ORDER_TYPE, 'subvention', 'order', 'coupon', 'order', ptc.ORDER_TYPE) = pp.ORDER_TYPE " \
                "WHERE 1=1 " \
                "  AND ptc.contract_id = :contract_id "
        if end_dt:
            query = query + "  AND ptc.dt < :end_dt "
        if start_dt:
            query = query + "  AND ptc.dt >= :start_dt "
        if transaction_end_dt:
            query = query + "  AND ptc.transaction_dt < :transaction_end_dt "
        if transaction_start_dt:
            query = query + "  AND ptc.transaction_dt >= :transaction_start_dt "
        query = query + "GROUP BY pp.product_id"
        params = {'contract_id': contract_id, 'end_dt': end_dt, 'start_dt': start_dt,
                  'transaction_start_dt': transaction_start_dt, 'transaction_end_dt': transaction_end_dt}
        amount_list = db.balance().execute(query, params, descr='Получаем сумму откруток из v_partner_taxi_completion_tlog')
        total_amount = Decimal(0)
        for item in amount_list:
            total_amount += Decimal(item['amount'])
            item['amount'] = Decimal(item['amount']) if item['amount'] else Decimal('0')
        return amount_list, total_amount

    @staticmethod
    def get_completions_from_partner_oebs_completions(contract_id, start_dt=None, end_dt=None,
                                                      from_accounting_period=None, to_accounting_period=None,
                                                      transaction_start_dt=None, transaction_end_dt=None,
                                                      oebs_compls_service_id=None):
        query = "SELECT " \
                "  sum(oc.amount) amount, " \
                "  oc.product_id " \
                "FROM t_partner_oebs_completions oc " \
                "WHERE 1=1 " \
                "  AND oc.contract_id = :contract_id "
        if end_dt:
            query = query + "  AND oc.dt < :end_dt "
        if start_dt:
            query = query + "  AND oc.dt >= :start_dt "
        if to_accounting_period:
            query = query + "  AND oc.accounting_period < :to_accounting_period "
        if from_accounting_period:
            query = query + "  AND oc.accounting_period >= :from_accounting_period "
        if oebs_compls_service_id:
            query = query + "  AND oc.service_id = :service_id "
        query = query + "GROUP BY oc.product_id"
        params = {'contract_id': contract_id, 'end_dt': end_dt, 'start_dt': start_dt,
                  'from_accounting_period': from_accounting_period, 'to_accounting_period': to_accounting_period,
                  'service_id': oebs_compls_service_id}
        amount_list = db.balance().execute(query, params,
                                           descr='Получаем сумму откруток из t_partner_oebs_completions')
        total_amount = Decimal(0)
        for item in amount_list:
            total_amount += Decimal(item['amount'])
            item['amount'] = Decimal(item['amount']) if item['amount'] else Decimal('0')
        return amount_list, total_amount

    @staticmethod
    def get_completions_from_both_views(contract_id, start_dt=None, end_dt=None, completion_absence_check=True,
                                        migration_dt=None, oebs_compls_service_id=None):
        amount_list, total_amount = TaxiSteps.get_completions_from_view(contract_id, start_dt=start_dt, end_dt=end_dt)
        amount_list_tlog, total_amount_tlog = TaxiSteps.get_completions_from_view_tlog(contract_id, start_dt=start_dt, end_dt=end_dt)

        for item_tlog in amount_list_tlog:
            for item in amount_list:
                if item_tlog['product_id'] == item['product_id']:
                    item['amount'] += item_tlog['amount']
                    break
            else:
                amount_list.append(item_tlog)

        total_amount += total_amount_tlog

        if migration_dt:
            amount_list_oebs, total_amount_oebs = TaxiSteps.get_completions_from_partner_oebs_completions(
                contract_id, from_accounting_period=start_dt, to_accounting_period=end_dt,
                oebs_compls_service_id=oebs_compls_service_id)
            total_amount += total_amount_oebs
            for item_oebs in amount_list_oebs:
                for item in amount_list:
                    if item_oebs['product_id'] == item['product_id']:
                        item['amount'] += item_oebs['amount']
                        break
                else:
                    amount_list.append(item_oebs)

        if total_amount == Decimal('0') and completion_absence_check:
            raise Exception(u'Нулевые данные во вью. Возможно, не обновилась матвью!')
        return amount_list, total_amount

    @staticmethod
    def check_taxi_invoice_data(client_id, contract_id, person_id, context, payment_amount=Decimal('0'), amount=None,
                                total_act_sum=None, dt=datetime.datetime.now(), migration_dt=None,
                                oebs_compls_service_id=None):
        with reporter.step(u'Проверяем данные в счете для клиента: {}'.format(client_id)):
            invoice_data = invoice_steps.InvoiceSteps.get_invoice_data_by_client(client_id)

            expected_invoice_data = TaxiData.create_expected_taxi_invoice_data(contract_id, person_id,
                                                                               firm=context.firm.id,
                                                                               currency=context.currency,
                                                                               nds=context.nds,
                                                                               payment_amount=payment_amount,
                                                                               amount=amount,
                                                                               total_act_sum=total_act_sum,
                                                                               dt=dt,
                                                                               migration_dt=migration_dt,
                                                                               oebs_compls_service_id=oebs_compls_service_id)

            utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, same_length=False),
                             u'Сравниваем данные из счета с шаблоном')

    @staticmethod
    def check_taxi_act_data(client_id, contract_id, act_dt, total_commission=None, subt_previous_preiods_sums=False,
                            migration_dt=None, oebs_compls_service_id=None):
        with reporter.step(u'Проверяем данные в акте для клиента: {}'.format(client_id)):
            act_data = acts_steps.ActsSteps.get_act_data_by_contract(contract_id)
            if not total_commission:
                _, total_commission = TaxiSteps.get_completions_from_view(contract_id, end_dt=act_dt + datetime.timedelta(days=1))
                _, total_commission_tlog = TaxiSteps.get_completions_from_view_tlog(contract_id, end_dt=act_dt + datetime.timedelta(days=1))
                total_commission += total_commission_tlog
                if migration_dt:
                    _, total_commission_oebs = TaxiSteps.get_completions_from_partner_oebs_completions(
                        contract_id, to_accounting_period=act_dt + datetime.timedelta(days=1),
                        oebs_compls_service_id=oebs_compls_service_id
                    )
                    total_commission += total_commission_oebs
            amount = total_commission
            if subt_previous_preiods_sums:
                previous_acts = [ad for ad in act_data if ad['dt'] < act_dt]
                if previous_acts:
                    amount -= sum([Decimal(ad['amount']) for ad in previous_acts])
            amount = close_to(amount, Decimal('0.04'))
            expected_act_data = TaxiData.create_expected_act_data(amount, act_dt)
            utils.check_that(act_data, contains_dicts_with_entries(expected_act_data, same_length=False),
                             u'Сравниваем данные из акта с шаблоном')

    @staticmethod
    def check_taxi_order_data(context, client_id,
                              contract_id, payment_amount=Decimal('0'),
                              currency=Currencies.RUB,
                              contract_monetization_services=None, end_dt=None,
                              completion_absence_check=True,
                              migration_dt=None,
                              oebs_compls_service_id=None):
        with reporter.step(u'Проверяем данные в заказах для клиента: {}'.format(client_id)):
            order_data = order_steps.OrderSteps.get_order_data_by_client(client_id)
            for item in order_data:
                item['completion_qty'] = Decimal(item['completion_qty'])
                item['consume_qty'] = Decimal(item['consume_qty'])
                item['consume_sum'] = Decimal(item['consume_sum'])
            order_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))

            services = contract_monetization_services or context.monetization_services

            expected_orders = TaxiData.create_expected_taxi_order_data(contract_id, currency, services,
                                                                       payment_amount=payment_amount,
                                                                       end_dt=end_dt,
                                                                       completion_absence_check=completion_absence_check,
                                                                       migration_dt=migration_dt,
                                                                       oebs_compls_service_id=oebs_compls_service_id)
            utils.check_that(order_data, contains_dicts_with_entries(expected_orders, same_length=False),
                             u'Сравниваем данные из заказов с шаблоном')

    @staticmethod
    def get_taxi_products_by_currency(currency, services):
        query = "SELECT product_id, service_id FROM t_partner_product WHERE currency_iso_code = :currency " \
                "AND service_id IN (:service_id_cash, :service_id_card) GROUP BY product_id, service_id"

        query_params = {'service_id_cash': Services.TAXI_111.id,
                        'service_id_card': Services.TAXI_128.id,
                        'currency': currency.iso_code}
        res = db.balance().execute(query, query_params,
                                   descr='Выбираем продукты из t_partner_product по валюте и сервису')

        res = filter(lambda row: row['service_id'] in services, res)
        return res

    @staticmethod
    def get_main_taxi_product(currency, services=[Services.TAXI_111.id, Services.TAXI_128.id]):
        if Services.TAXI_111.id in services:
            service_id = Services.TAXI_111.id
        else:
            service_id = Services.TAXI_128.id
        query = "SELECT product_id FROM t_partner_product WHERE currency_iso_code = :currency " \
                "AND service_id =:service_id AND unified_account_root = 1"

        query_params = {'service_id': service_id, 'currency': currency.iso_code}

        return db.balance().execute(query, query_params,
                                    descr='Выбираем главный продукт из t_partner_product по валюте и сервису')[0]['product_id']

    @staticmethod
    def set_no_acts_attribute(contract_id):
        query = "UPDATE t_contract_attributes SET value_num = 1 WHERE code = 'NO_ACTS' AND attribute_batch_id = (SELECT attribute_batch_id FROM t_contract_collateral WHERE contract2_id = :contract_id AND num IS NULL)"
        params = {'contract_id': contract_id}
        db.balance().execute(query, params, descr='Устанавливаем атрибут NO_ACTS = 1')
        contract_steps.ContractSteps.refresh_contracts_cache(contract_id)

    # метод для получения корректировок по договору
    @staticmethod
    def get_thirdparty_corrections_by_contract_id(contract_id):
        query = "SELECT contract_id, person_id, transaction_type, partner_id, service_id, payment_type," \
                "commission_currency, currency, paysys_type_cc, amount, amount_fee, " \
                "yandex_reward_wo_nds, yandex_reward, partner_currency, internal, oebs_org_id, " \
                "commission_iso_currency, iso_currency, partner_iso_currency, auto, invoice_eid, dt, transaction_dt, id " \
                "FROM T_THIRDPARTY_CORRECTIONS WHERE contract_id = :contract_id ORDER BY dt, id"
        params = {'contract_id': contract_id}
        corrections_data = db.balance().execute(query, params,
                                                descr='Получаем thirdparty_corrections для договора {contract_id}'.format(contract_id=contract_id))

        for row in corrections_data:
            row['amount'] = Decimal(row['amount'])

        if balance_config.TRUST_ME_I_AM_OEBS_QA:
            corrections = db.balance().execute(
                "select id from T_THIRDPARTY_CORRECTIONS where contract_id = {} and internal <> 1".format(contract_id))
            for correction in corrections:
                export_steps.ExportSteps.export_oebs(correction_id=correction['id'])

        return corrections_data

    @staticmethod
    def get_tlog_timeline_notch(object_id=None, classname=None, contract_id=None):
        assert contract_id or (object_id and classname)
        query = """
            select dt, object_id, classname, contract_id, operation_type, last_transaction_id,
            pad_place_id, pad_page_id, pad_dt, pad_contract_id
            from bo.t_tlog_timeline
            where 1=1
        """
        if contract_id:
            query += " and contract_id=:contract_id"
        if object_id:
            query += " and object_id=:object_id"
        if classname:
            query += " and classname=:classname"
        query += """
           order by id desc
        """
        params = {'contract_id': contract_id,
                  'object_id': object_id,
                  'classname': classname}
        with reporter.step(u'Получаем зарубку транзакционного лога'):
            return db.balance().execute(query, params)

    @staticmethod
    def get_commission_personal_account_by_client_id(contract_id, service=Services.TAXI_111):
        with reporter.step(u'Находим id ЛС такси для договора: {}'.format(contract_id)):
            invoice_id, invoice_external_id, _ = invoice_steps.InvoiceSteps.\
                get_invoice_by_service_or_service_code(contract_id, Services.TAXI_111)
            return invoice_id, invoice_external_id


    @staticmethod
    def get_receipt_sums_for_invoice(invoice_id):
        invoice_data_query = "SELECT inv.RECEIPT_SUM, inv.RECEIPT_SUM_1C " \
                             "FROM T_INVOICE inv " \
                             "WHERE inv.id=:invoice_id"

        params = {'invoice_id': invoice_id}
        return db.balance().execute(invoice_data_query, params)

    @staticmethod
    def check_personal_account_data_for_netting(invoice_id, netting_amount, receipt_sum_1c=Decimal('0')):
        invoice_data = TaxiSteps.get_receipt_sums_for_invoice(invoice_id)

        expected_invoice_data = {
            'receipt_sum': close_to(netting_amount, Decimal('0.04')),
            'receipt_sum_1c': receipt_sum_1c,
        }

        # сравниваем данные
        utils.check_that(invoice_data, contains_dicts_equal_to([expected_invoice_data], same_length=False),
                         'Сравниваем данные в счете с шаблоном')

    @staticmethod
    def create_taxi_netting_collateral(contract_id, dt, netting_pct):
        params = {'CONTRACT2_ID': contract_id,
                  'DT': dt,
                  'IS_SIGNED': dt.isoformat()}
        if netting_pct is not None:
            params.update({'NETTING_PCT': netting_pct, 'NETTING': 1})
        contract_steps.ContractSteps.create_collateral(1046, params)

    @staticmethod
    def create_cash_payment_fact(invoice_eid, amount, dt, type, service_id=None):
        with reporter.step(u'Вставляем запись об оплате в T_OEBS_CASH_PAYMENT_FACT для счета: {}'.format(invoice_eid)):
            source_id = db.balance().execute("SELECT S_OEBS_CPF_SOURCE_ID_TEST.nextval val FROM dual")[0]['val']
            cash_fact_id = db.balance().execute("SELECT S_OEBS_CASH_PAYMENT_FACT_TEST.nextval val FROM dual")[0]['val']

            query = "INSERT INTO T_OEBS_CASH_PAYMENT_FACT" \
                    "(XXAR_CASH_FACT_ID, AMOUNT, RECEIPT_NUMBER, RECEIPT_DATE, OPERATION_TYPE, " \
                    "LAST_UPDATED_BY, LAST_UPDATE_DATE, CREATED_BY, CREATION_DATE, SOURCE_ID, SERVICE_ID) VALUES " \
                    "(:cash_fact_id, :amount, :invoice_eid, :dt, :type, " \
                    "-1, :dt, -1, :dt, :source_id, :service_id)"
            params = {
                'invoice_eid': invoice_eid,
                'amount': amount,
                'dt': dt,
                'type': type,
                'source_id': source_id,
                'cash_fact_id': cash_fact_id,
                'service_id': service_id
            }
            db.balance().execute(query, params)

            return cash_fact_id, source_id

    @staticmethod
    def export_correction_netting(cash_fact_id, handle_with_process_payment=False):
        if not handle_with_process_payment:
            with reporter.step(u'Экспортируем {} очереди {}, id: {}'
                               .format(Export.Classname.OEBS_CPF, Export.Type.THIRDPARTY_TRANS, cash_fact_id)):
                CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.OEBS_CPF, cash_fact_id)

    @staticmethod
    def set_wait_for_correction_netting(timeout):
        with reporter.step(
                u"Устанавливаем значение параметра WAIT_FOR_CORRECTION_NETTING_BEFORE_PROCESS_PAYMENT в конфиге "
                u"на значение: {}".format(timeout)):
            query = "UPDATE T_CONFIG SET VALUE_NUM=:timeout WHERE ITEM='WAIT_FOR_CORRECTION_NETTING_BEFORE_PROCESS_PAYMENT'"
            params = {'timeout': timeout}
            db.balance().execute(query, params)

    @classmethod
    def process_payment(cls, invoice_id, handle_with_process_payment=False):
        with reporter.step(u'Экспортируем {} очереди {}, id: {}'
                           .format(Export.Classname.INVOICE, Export.Type.PROCESS_PAYMENTS, invoice_id)):
            if handle_with_process_payment:
                cls.set_wait_for_correction_netting(timeout=5)
            CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, invoice_id)

    @classmethod
    def calculate_expected_netting_amount_by_orders_data(cls, orders_data, nds, netting_pct):
        amount_by_products = TaxiData.create_expected_completions_data_by_orders_data(orders_data, nds)
        expected_netting_amount = sum([amount for _, amount in amount_by_products.items()]) * netting_pct / Decimal('100')
        return expected_netting_amount

    @classmethod
    def calculate_expected_netting_amount_by_orders_data_tlog(cls, orders_data_tlog, nds, netting_pct):
        amount_by_products = TaxiData.create_expected_completions_data_by_orders_data_tlog(orders_data_tlog, nds)
        expected_netting_amount = sum([amount for _, amount in amount_by_products.items()]) * netting_pct / Decimal('100')
        return expected_netting_amount

    @staticmethod
    def check_postpay_taxi_balance(client_id, contract_id, context, commission=None, orders_data=None,
                                   orders_data_tlog=None, compls_data_oebs=None):
        assert commission is not None or orders_data is not None or orders_data_tlog is not None or compls_data_oebs is not None
        commission_by_orders_data = None

        if orders_data:
            amount_by_products = TaxiData.create_expected_completions_data_by_orders_data(orders_data, context.nds)
            commission_by_orders_data = sum([amount for _, amount in amount_by_products.items()])

        if orders_data_tlog:
            amount_by_products = TaxiData.create_expected_completions_data_by_orders_data_tlog(orders_data_tlog, context.nds)
            if commission_by_orders_data is not None:
                commission_by_orders_data += sum([amount for _, amount in amount_by_products.items()])
            else:
                commission_by_orders_data = sum([amount for _, amount in amount_by_products.items()])

        if compls_data_oebs:
            if commission_by_orders_data is not None:
                commission_by_orders_data += sum([compl_dict['amount'] for compl_dict in compls_data_oebs])
            else:
                commission_by_orders_data = sum([compl_dict['amount'] for compl_dict in compls_data_oebs])

        if commission_by_orders_data is not None:
            commission = commission_by_orders_data

        commission = close_to(commission, Decimal('0.02'))
        taxi_balance = TaxiSteps.get_taxi_balance([contract_id])
        expected_taxi_balance = TaxiData.create_expected_postpay_taxi_balance(client_id, contract_id, commission,
                                                                              currency=context.currency)

        utils.check_that(taxi_balance, contains_dicts_with_entries(expected_taxi_balance),
                         u'Проверяем, что метод отдал ожидаемые значения')

    @staticmethod
    def check_prepay_taxi_balance(client_id, contract_id, balance, charge=Decimal('0'),
                                  promocode_amount=Decimal('0'), promocode_left=Decimal('0'),
                                  currency=Currencies.RUB, close_to_delta=Decimal('0.02')):
        taxi_balance = TaxiSteps.get_taxi_balance([contract_id])
        balance = close_to(balance, close_to_delta)
        charge = close_to(charge, close_to_delta)
        expected_taxi_balance = TaxiData.create_expected_prepay_taxi_balance(client_id, contract_id,
                                                                             balance, charge,
                                                                             promocode_amount, promocode_left,
                                                                             currency)

        utils.check_that(taxi_balance, contains_dicts_with_entries(expected_taxi_balance),
                         u'Проверяем, что метод отдал ожидаемые значения')

    @staticmethod
    def get_taxi_balance(contract_ids):
        with reporter.step(u'Вызываем метод GetTaxiBalance для договоров: {}'.format(contract_ids)):
            return api.medium().GetTaxiBalance(contract_ids)

    @staticmethod
    def export_process_completion(client_id):
        with reporter.step(u"Запускаем экспорт откруток для заказов клиента: {}".format(client_id)):
            query = "SELECT ID FROM T_ORDER WHERE CLIENT_ID=:client_id"
            params = {'client_id': client_id}

            orders_ids = [row['id'] for row in db.balance().execute(query, params)]
            reporter.attach(u"IDs заказов", utils.Presenter.pretty(orders_ids))

            for order_id in orders_ids:
                CommonSteps.export("PROCESS_COMPLETION", "Order", order_id)

    @staticmethod
    @utils.cached
    def get_product_mapping():
        with reporter.step(u"Получаем маппинг из service_id + currency + order_type -> product_id"):
            query = """SELECT service_id as service_id,
       currency_iso_code as currency_iso_code,
       order_type as order_type,
       product_mdh_id,
       nvl(product_id,
           (select min(id) from bo.t_product where mdh_id = PRODUCT_MDH_ID)
           ) as product_id
       FROM bo.t_partner_product pp"""
            rows = db.balance().execute(query)

            mapping = dict()
            for row in rows:
                product_id = row['product_id']
                if not product_id:
                    subquery = "SELECT * FROM bo.t_product where mdh_id = '%s'" % row['product_mdh_id']
                    products = db.balance().execute(subquery)
                    if len(products) == 1:
                        product_id = products[0]['id']
                mapping[(row['service_id'], row['currency_iso_code'], row['order_type'])] = product_id

            return mapping


class TaxiData(object):

    @classmethod
    def get_default_contract_parametrization(cls, parametrization_string):
        params = [pytest.param(*cp.get_parametrization(parametrization_string), id=cp.parametrization_id, marks=cp.pytest_marks)
                  for cp in cls.default_contract_parametrization]
        return params

    @classmethod
    def get_contract_parametrization(cls, parametrization_string, parameters):
        params = [pytest.param(*cp.get_parametrization(parametrization_string), id=cp.parametrization_id,
                               marks=cp.pytest_marks)
                  for cp in parameters]
        return params

    # orders_types_2_products можно получить запросом:
    # select currency_iso_code, decode(SERVICE_ID, 111, 'cash', 128, 'card', null) payment_type, ORDER_TYPE, PRODUCT_ID
    # from bo.T_PARTNER_PRODUCT
    # where SERVICE_ID in (111, 128)
    # order by currency_iso_code, pt, ORDER_TYPE;

    orders_types_2_products = {
        ('EUR', 'card', 'commission_correction'): 507863,
        ('EUR', 'card', 'order'): 507863,
        ('EUR', 'card', 'delivery_order'): 511268,
        ('EUR', 'card', 'delivery_driver_workshift'): 511270,
        ('EUR', 'card', 'delivery_hiring_with_car'): 511272,
        ('EUR', 'cash', 'commission_correction'): 507862,
        ('EUR', 'cash', 'order'): 507862,
        ('EUR', 'cash', 'delivery_order'): 511266,
        ('EUR', 'cash', 'delivery_driver_workshift'): 511269,
        ('EUR', 'cash', 'delivery_hiring_with_car'): 511271,

        ('RON', 'card', 'commission_correction'): 510787,
        ('RON', 'card', 'order'): 510787,
        ('RON', 'cash', 'commission_correction'): 510770,
        ('RON', 'cash', 'order'): 510770,
        ('RON', 'cash', 'driver_workshift'): 510788,

        ('RUB', 'card', 'childchair'): 508867,
        ('RUB', 'card', 'commission_correction'): 505142,
        ('RUB', 'card', 'driver_workshift'): 505142,
        ('RUB', 'card', 'hiring_with_car'): 509005,
        ('RUB', 'card', 'order'): 505142,
        ('RUB', 'card', 'cargo_order'): 510063,
        ('RUB', 'card', 'cargo_driver_workshift'): 510063,
        ('RUB', 'card', 'cargo_hiring_with_car'): 510838,
        ('RUB', 'card', 'delivery_order'): 510790,
        ('RUB', 'card', 'delivery_driver_workshift'): 510790,
        ('RUB', 'card', 'delivery_hiring_with_car'): 510878,

        ('RUB', 'cash', 'childchair'): 508625,
        ('RUB', 'cash', 'commission_correction'): 503352,
        ('RUB', 'cash', 'driver_workshift'): 503352,
        ('RUB', 'cash', 'hiring_with_car'): 509001,
        ('RUB', 'cash', 'order'): 503352,
        ('RUB', 'cash', 'marketplace_advert_call'): 509287,
        ('RUB', 'cash', 'cargo_order'): 510064,
        ('RUB', 'cash', 'cargo_driver_workshift'): 510064,
        ('RUB', 'cash', 'cargo_hiring_with_car'): 510837,
        ('RUB', 'cash', 'delivery_order'): 510791,
        ('RUB', 'cash', 'delivery_driver_workshift'): 510791,
        ('RUB', 'cash', 'delivery_hiring_with_car'): 510877,

        ('UAH', 'card', 'order'): 507999,
        ('UAH', 'cash', 'order'): 507907,

        ('USD', 'card', 'commission_correction'): 507860,
        ('USD', 'card', 'order'): 507860,
        ('USD', 'card', 'delivery_order'): 510986,
        ('USD', 'card', 'delivery_driver_workshift'): 510988,
        ('USD', 'card', 'delivery_hiring_with_car'): 510990,

        ('USD', 'cash', 'commission_correction'): 507859,
        ('USD', 'cash', 'driver_workshift'): 509318,
        ('USD', 'cash', 'order'): 507859,
        ('USD', 'cash', 'delivery_order'): 510985,
        ('USD', 'cash', 'delivery_driver_workshift'): 510987,
        ('USD', 'cash', 'delivery_hiring_with_car'): 510989,

        ('BYN', 'card', 'commission_correction'): 509673,
        ('BYN', 'card', 'order'): 509673,
        ('BYN', 'cash', 'commission_correction'): 509674,
        ('BYN', 'cash', 'order'): 509674,

        ('ILS', 'cash', 'order'): 509825,
        ('ILS', 'cash', 'commission_correction'): 509825,
        ('ILS', 'cash', 'driver_workshift'): 509825, # пока основной, но, возможно, будет отдельный
        ('ILS', 'cash', 'childchair'): 509830,
        ('ILS', 'cash', 'hiring_with_car'): 509831,
        ('ILS', 'cash', 'marketplace_advert_call'): 509832,
        ('ILS', 'cash', 'delivery_order'): 511197,
        ('ILS', 'cash', 'delivery_driver_workshift'): 511199,
        ('ILS', 'cash', 'delivery_hiring_with_car'): 511201,

        ('ILS', 'card', 'order'): 509826,
        ('ILS', 'card', 'commission_correction'): 509826,
        ('ILS', 'card', 'driver_workshift'): 509826, # пока основной, но, возможно, будет отдельный
        ('ILS', 'card', 'childchair'): 509835,
        ('ILS', 'card', 'hiring_with_car'): 509833,
        ('ILS', 'card', 'delivery_order'): 511198,
        ('ILS', 'card', 'delivery_driver_workshift'): 511200,
        ('ILS', 'card', 'delivery_hiring_with_car'): 511202,

        ('KZT', 'card', 'commission_correction'): 510018,
        ('KZT', 'card', 'order'): 510018,
        ('KZT', 'cash', 'commission_correction'): 510017,
        ('KZT', 'cash', 'driver_workshift'): 510019,
        ('KZT', 'cash', 'order'): 510017,

        ('NOK', 'card', 'commission_correction'): 512304,
        ('NOK', 'card', 'order'): 512304,
        ('NOK', 'card', 'delivery_order'): 512302,
        ('NOK', 'card', 'delivery_driver_workshift'): 512300,
        ('NOK', 'card', 'delivery_hiring_with_car'): 512301,
        ('NOK', 'cash', 'commission_correction'): 512298,
        ('NOK', 'cash', 'order'): 512298,
        ('NOK', 'cash', 'delivery_order'): 512296,
        ('NOK', 'cash', 'delivery_driver_workshift'): 512294,
        ('NOK', 'cash', 'delivery_hiring_with_car'): 512295,

        ('SEK', 'card', 'commission_correction'): 513455,
        ('SEK', 'card', 'order'): 513455,
        ('SEK', 'card', 'delivery_order'): 513453,
        ('SEK', 'card', 'delivery_driver_workshift'): 513451,
        ('SEK', 'card', 'delivery_hiring_with_car'): 513452,
        ('SEK', 'cash', 'commission_correction'): 513449,
        ('SEK', 'cash', 'order'): 513449,
        ('SEK', 'cash', 'delivery_order'): 513447,
        ('SEK', 'cash', 'delivery_driver_workshift'): 513445,
        ('SEK', 'cash', 'delivery_hiring_with_car'): 513446,
    }
    payment_type_mapping = {
        'corporate': 'cash',
        'prepaid': 'cash',
    }
    order_type_mapping = {
        TaxiOrderType.subsidy: TaxiOrderType.commission
    }
    order_type_mapping_tlog = {
        TaxiOrderType.subsidy_tlog: TaxiOrderType.commission,
        TaxiOrderType.promocode_tlog: TaxiOrderType.commission
    }
    service_payment_type_mapping_tlog = {
        Services.TAXI_111.id: 'cash',
        Services.TAXI_128.id: 'card',
    }
    no_nds_order_types = (TaxiOrderType.subsidy_tlog, TaxiOrderType.promocode_tlog)

    @classmethod
    def map_order_dict_to_product(cls, order_dict):
        currency = order_dict['currency']
        payment_type = cls.payment_type_mapping.get(order_dict['payment_type'], order_dict['payment_type'])
        order_type = cls.order_type_mapping.get(order_dict['order_type'], order_dict['order_type'])
        return cls.orders_types_2_products.get((currency, payment_type, order_type), None)

    @classmethod
    def _filter_func(cls, order_dict):
        product_exists = cls.map_order_dict_to_product(order_dict)
        if product_exists:
            return True

    @classmethod
    def map_order_dict_to_product_tlog(cls, order_dict):
        currency = order_dict['currency']
        order_type = cls.order_type_mapping_tlog.get(order_dict['type'], order_dict['type'])
        payment_type = cls.service_payment_type_mapping_tlog[order_dict['service_id']]
        return cls.orders_types_2_products.get((currency, payment_type, order_type), None)

    @classmethod
    def filter_orders_data_by_acceptable_orders_type_for_currency(cls, orders_data):
        return filter(cls._filter_func, orders_data)

    @classmethod
    def _filter_func_tlog(cls, order_dict):
        product_exists = cls.map_order_dict_to_product_tlog(order_dict)
        if product_exists:
            return True

    @classmethod
    def filter_orders_data_by_acceptable_orders_type_for_currency_tlog(cls, orders_data):
        return filter(cls._filter_func_tlog, orders_data)

    default_orders_data = [
            # открутки с типом commission (order)
            {'payment_type': PaymentType.CASH,
             'commission_sum': Taxi.order_commission_cash,
             'order_type': TaxiOrderType.commission,
             'promocode_sum': Taxi.promocode_sum,
             'subsidy_sum': 0,
             },
            {'payment_type': PaymentType.CARD,
             'commission_sum': Taxi.order_commission_card,
             'order_type': TaxiOrderType.commission,
             'promocode_sum': Taxi.promocode_sum,
             'subsidy_sum': 0,
             },
            {'payment_type': PaymentType.CORPORATE,
             'commission_sum': Taxi.order_commission_corp,
             'order_type': TaxiOrderType.commission,
             'promocode_sum': Taxi.promocode_sum,
             'subsidy_sum': 0,
             },
            {'payment_type': PaymentType.PREPAID,
             'commission_sum': Taxi.order_commission_cash,
             'order_type': TaxiOrderType.commission,
             'promocode_sum': Taxi.promocode_sum,
             'subsidy_sum': 0,
             },
            # открутки с типом commission_correction
            {'payment_type': PaymentType.CASH,
             'commission_sum': Taxi.commission_correction_cash,
             'order_type': TaxiOrderType.commission_correction,
             'promocode_sum': 0,
             'subsidy_sum': 0,
             },
            {'payment_type': PaymentType.CARD,
             'commission_sum': Taxi.commission_correction_card,
             'order_type': TaxiOrderType.commission_correction,
             'promocode_sum': 0,
             'subsidy_sum': 0,
             },
            # открутки с типом subsidy
            {'payment_type': PaymentType.CASH,
             'commission_sum': 0,
             'order_type': TaxiOrderType.subsidy,
             'promocode_sum': 0,
             'subsidy_sum': Taxi.subsidy_sum,
             },
            {'payment_type': PaymentType.CASH,
             'commission_sum': 0,
             'order_type': TaxiOrderType.subsidy,
             'promocode_sum': 0,
             # отрицательные субсидии на данный момент не учитываются
             'subsidy_sum': -Taxi.subsidy_sum,
             },
            # открутки с типом childchair
            {'payment_type': PaymentType.CASH,
             'commission_sum': Taxi.childchair_cash,
             'order_type': TaxiOrderType.childchair,
             'promocode_sum': Taxi.promocode_sum,
             'subsidy_sum': 0,
             },
            {'payment_type': PaymentType.CARD,
             'commission_sum': Taxi.childchair_card,
             'order_type': TaxiOrderType.childchair,
             'promocode_sum': Taxi.promocode_sum,
             'subsidy_sum': 0,
             },
            {'payment_type': PaymentType.CORPORATE,
             'commission_sum': Taxi.childchair_corp,
             'order_type': TaxiOrderType.childchair,
             'promocode_sum': Taxi.promocode_sum,
             'subsidy_sum': 0,
             },
            # открутки с типом driver_workshift cash
            {'payment_type': PaymentType.CASH,
             'commission_sum': Taxi.driver_workshift_cash,
             'order_type': TaxiOrderType.driver_workshift,
             'promocode_sum': 0,
             'subsidy_sum': 0,
             },
            # открутки с типом driver_workshift card
            {'payment_type': PaymentType.CARD,
             'commission_sum': Taxi.driver_workshift_card,
             'order_type': TaxiOrderType.driver_workshift,
             'promocode_sum': 0,
             'subsidy_sum': 0,
             },
            # открутки с типом hiring_with_car
            {'payment_type': PaymentType.CASH,
             'commission_sum': Taxi.hiring_with_car_cash,
             'order_type': TaxiOrderType.hiring_with_car,
             'promocode_sum': Taxi.promocode_sum,
             'subsidy_sum': 0,
             },
            {'payment_type': PaymentType.CARD,
             'commission_sum': Taxi.hiring_with_car_card,
             'order_type': TaxiOrderType.hiring_with_car,
             'promocode_sum': Taxi.promocode_sum,
             'subsidy_sum': 0,
             },
            # открутки с типом marketplace_advert_call
            {'payment_type': PaymentType.CASH,
             'commission_sum': Taxi.marketplace_advert_call_cash,
             'order_type': TaxiOrderType.marketplace_advert_call,
             'promocode_sum': 0,
             'subsidy_sum': 0,
            },
        ]

    default_orders_data_tlog = [
            # открутки с типом commission (order)
            {'service_id': Services.TAXI_111.id,
             'amount': Taxi.order_commission_cash,
             'type': TaxiOrderType.commission,
             },
            {'service_id': Services.TAXI_111.id,
             'amount': Taxi.order_commission_cash / Decimal('-2'),
             'type': TaxiOrderType.commission,
             },
            {'service_id': Services.TAXI_128.id,
             'amount': Taxi.order_commission_card,
             'type': TaxiOrderType.commission,
             },
            {'service_id': Services.TAXI_128.id,
             'amount': Taxi.order_commission_card / Decimal('-2'),
             'type': TaxiOrderType.commission,
             },
            # открутки с типом commission_correction
            {'service_id': Services.TAXI_111.id,
             'amount': Taxi.commission_correction_cash,
             'type': TaxiOrderType.commission_correction,
             },
            {'service_id': Services.TAXI_128.id,
             'amount': Taxi.commission_correction_card,
             'type': TaxiOrderType.commission_correction,
             },
            {'service_id': Services.TAXI_111.id,
             'amount': Taxi.commission_correction_cash / Decimal('-2'),
             'type': TaxiOrderType.commission_correction,
             },
            {'service_id': Services.TAXI_128.id,
             'amount': Taxi.commission_correction_card / Decimal('-2'),
             'type': TaxiOrderType.commission_correction,
             },
            # открутки с типом subsidy
            {'service_id': Services.TAXI_111.id,
             'amount': Taxi.subsidy_sum,
             'type': TaxiOrderType.subsidy_tlog,
             },
            {'service_id': Services.TAXI_128.id,
             'amount': Taxi.subsidy_sum,
             'type': TaxiOrderType.subsidy_tlog,
             },
            {'service_id': Services.TAXI_111.id,
             'amount': Taxi.subsidy_sum / Decimal('-2'),
             'type': TaxiOrderType.subsidy_tlog,
             },
            {'service_id': Services.TAXI_128.id,
             'amount': Taxi.subsidy_sum / Decimal('-2'),
             'type': TaxiOrderType.subsidy_tlog,
             },
            # открутки с типом promocode
            {'service_id': Services.TAXI_111.id,
             'amount': Taxi.promocode_sum,
             'type': TaxiOrderType.promocode_tlog,
             },
            {'service_id': Services.TAXI_128.id,
             'amount': Taxi.promocode_sum,
             'type': TaxiOrderType.promocode_tlog,
             },
            {'service_id': Services.TAXI_111.id,
             'amount': Taxi.promocode_sum / Decimal('-2'),
             'type': TaxiOrderType.promocode_tlog,
             },
            {'service_id': Services.TAXI_128.id,
             'amount': Taxi.promocode_sum / Decimal('-2'),
             'type': TaxiOrderType.promocode_tlog,
             },
            # открутки с типом childchair
            {'service_id': Services.TAXI_111.id,
             'amount': Taxi.childchair_cash,
             'type': TaxiOrderType.childchair,
             },
            {'service_id': Services.TAXI_128.id,
             'amount': Taxi.childchair_card,
             'type': TaxiOrderType.childchair,
             },
            {'service_id': Services.TAXI_111.id,
             'amount': Taxi.childchair_cash / Decimal('-2'),
             'type': TaxiOrderType.childchair,
             },
            {'service_id': Services.TAXI_128.id,
             'amount': Taxi.childchair_card / Decimal('-2'),
             'type': TaxiOrderType.childchair,
             },
            # открутки с типом driver_workshift
            {'service_id': Services.TAXI_111.id,
             'amount': Taxi.driver_workshift_cash,
             'type': TaxiOrderType.driver_workshift,
             },
            {'service_id': Services.TAXI_111.id,
             'amount': Taxi.driver_workshift_cash / Decimal('-2'),
             'type': TaxiOrderType.driver_workshift,
             },
            {'service_id': Services.TAXI_128.id,
             'amount': Taxi.driver_workshift_card,
             'type': TaxiOrderType.driver_workshift,
             },
            {'service_id': Services.TAXI_128.id,
             'amount': Taxi.driver_workshift_card / Decimal('-2'),
             'type': TaxiOrderType.driver_workshift,
             },
            # открутки с типом hiring_with_car
            {'service_id': Services.TAXI_111.id,
             'amount': Taxi.hiring_with_car_cash,
             'type': TaxiOrderType.hiring_with_car,
             },
            {'service_id': Services.TAXI_128.id,
             'amount': Taxi.hiring_with_car_card,
             'type': TaxiOrderType.hiring_with_car,
             },
            {'service_id': Services.TAXI_111.id,
             'amount': Taxi.hiring_with_car_cash / Decimal('-2'),
             'type': TaxiOrderType.hiring_with_car,
             },
            {'service_id': Services.TAXI_128.id,
             'amount': Taxi.hiring_with_car_card / Decimal('-2'),
             'type': TaxiOrderType.hiring_with_car,
             },
            # открутки с типом marketplace_advert_call
            {'service_id': Services.TAXI_111.id,
             'amount': Taxi.marketplace_advert_call_cash,
             'type': TaxiOrderType.marketplace_advert_call,
             },
            {'service_id': Services.TAXI_111.id,
             'amount': Taxi.marketplace_advert_call_cash / Decimal('-2'),
             'type': TaxiOrderType.marketplace_advert_call,
             },
            # открутки с типом cargo
            {
                'service_id': Services.TAXI_111.id,
                'amount': Taxi.cargo_cash,
                'type': TaxiOrderType.cargo_order,
            },
            {
                'service_id': Services.TAXI_128.id,
                'amount': Taxi.cargo_card,
                'type': TaxiOrderType.cargo_order,
            },

            {
                'service_id': Services.TAXI_111.id,
                'amount': Taxi.cargo_driver_workshift_cash,
                'type': TaxiOrderType.cargo_driver_workshift,
            },
            {
                'service_id': Services.TAXI_128.id,
                'amount': Taxi.cargo_driver_workshift_card,
                'type': TaxiOrderType.cargo_driver_workshift,
            },

            {
                'service_id': Services.TAXI_111.id,
                'amount': Taxi.cargo_hiring_with_car_cash,
                'type': TaxiOrderType.cargo_hiring_with_car,
            },
            {
                'service_id': Services.TAXI_128.id,
                'amount': Taxi.cargo_hiring_with_car_card,
                'type': TaxiOrderType.cargo_hiring_with_car,
            },
            # открутки с типом delivery
            {
                'service_id': Services.TAXI_111.id,
                'amount': Taxi.delivery_cash,
                'type': TaxiOrderType.delivery_order,
            },
            {
                'service_id': Services.TAXI_128.id,
                'amount': Taxi.delivery_card,
                'type': TaxiOrderType.delivery_order,
            },
            {
                'service_id': Services.TAXI_111.id,
                'amount': Taxi.delivery_driver_workshift_cash,
                'type': TaxiOrderType.delivery_driver_workshift,
            },
            {
                'service_id': Services.TAXI_128.id,
                'amount': Taxi.delivery_driver_workshift_card,
                'type': TaxiOrderType.delivery_driver_workshift,
            },
            {
                'service_id': Services.TAXI_111.id,
                'amount': Taxi.delivery_hiring_with_car_cash,
                'type': TaxiOrderType.delivery_hiring_with_car,
            },
            {
                'service_id': Services.TAXI_128.id,
                'amount': Taxi.delivery_hiring_with_car_card,
                'type': TaxiOrderType.delivery_hiring_with_car,
            },
        ]

    @classmethod
    def generate_default_orders_data(cls, orders_dt, currency_iso_code,
                                     filter_orders_data_by_acceptable_orders_type_for_currency=True):
        orders_data = []
        for order_dict in cls.default_orders_data:
            order_dict = order_dict.copy()
            order_dict.update({'dt': orders_dt, 'currency': currency_iso_code})
            orders_data.append(order_dict)
        if filter_orders_data_by_acceptable_orders_type_for_currency:
            orders_data = cls.filter_orders_data_by_acceptable_orders_type_for_currency(orders_data)
        return orders_data

    @classmethod
    def generate_default_orders_data_tlog(cls, orders_dt, currency_iso_code, transaction_dt=None,
                                          filter_orders_data_by_acceptable_orders_type_for_currency=True,
                                          transaction_ids_range=None):
        orders_data = []
        if transaction_ids_range:
            lower, upper = transaction_ids_range
        else:
            lower, upper = 10000, 100000
        if not transaction_dt:
            transaction_dt = orders_dt
        for order_dict in cls.default_orders_data_tlog:
            last_transaction_id = randint(lower, upper)
            order_dict = order_dict.copy()
            order_dict.update({'dt': orders_dt, 'transaction_dt': transaction_dt, 'currency': currency_iso_code,
                               'last_transaction_id': last_transaction_id})
            orders_data.append(order_dict)
        if filter_orders_data_by_acceptable_orders_type_for_currency:
            orders_data = cls.filter_orders_data_by_acceptable_orders_type_for_currency_tlog(orders_data)
        orders_data = OrdersDataList(orders_data)
        return orders_data

    @classmethod
    def generate_default_oebs_compls_data(cls, compl_dt, currency_iso_code, accounting_period=None):
        # переписать после полной миграции на ОЕБС как источник и избавления от расчета откруток по старым табдицам
        compls_data = []
        compls_accumulator = defaultdict(lambda: Decimal(0))
        for order_dict in cls.default_orders_data_tlog:
            order_dict = order_dict.copy()
            order_dict.update({'dt': compl_dt, 'currency': currency_iso_code})
            product_id = cls.map_order_dict_to_product_tlog(order_dict)
            # создадим открутки, для которых есть product_id
            if product_id:
                key = (order_dict['service_id'], product_id, order_dict['dt'], order_dict['currency'])
                compls_accumulator[key] += order_dict['amount']

        for (service_id, product_id, dt, currency), amount in compls_accumulator.iteritems():
            compls_data.append(
                {
                    'service_id': service_id,
                    'last_transaction_id': 99,
                    'amount': amount,
                    'product_id': product_id,
                    'dt': dt,
                    'transaction_dt': dt,
                    'currency': currency,
                    'accounting_period': accounting_period
                }
            )
        return compls_data

    @staticmethod
    def create_expected_taxi_invoice_data_row(contract_id, person_id, amount, invoice_type, paysys_id,
                                              total_act_sum=None,
                                              nds=NdsNew.DEFAULT, firm=Firms.TAXI_13.id, currency=Currencies.RUB,
                                              dt=datetime.datetime.now()):
        return common_data_steps.CommonData.create_expected_taxi_invoice_data(contract_id, person_id, amount,
                                                                              invoice_type,
                                                                              paysys_id,
                                                                              firm, total_act_sum, currency, nds,
                                                                              dt=dt)

    @staticmethod
    def create_expected_taxi_invoice_data(contract_id, person_id, nds=NdsNew.DEFAULT,
                                          firm=Firms.TAXI_13.id, currency=Currencies.RUB, payment_amount=Decimal('0'),
                                          amount=None, total_act_sum=None,
                                          dt=datetime.datetime.now(),
                                          migration_dt=None,
                                          oebs_compls_service_id=None):

        _, total_commission = TaxiSteps.get_completions_from_view(contract_id, end_dt=dt)
        _, total_commission_tlog = TaxiSteps.get_completions_from_view_tlog(contract_id, end_dt=dt)
        total_commission += total_commission_tlog

        if migration_dt:
            _, total_commission_oebs = TaxiSteps.get_completions_from_partner_oebs_completions(
                contract_id, to_accounting_period=dt, oebs_compls_service_id=oebs_compls_service_id)
            total_commission += total_commission_oebs

        if not amount:
            amount = close_to(total_commission, Decimal('0.04'))
        if not total_act_sum:
            # _, total_commission_on_dt = TaxiSteps.get_completions_from_view(contract_id, end_dt=dt)
            total_act_sum = close_to(total_commission, Decimal('0.04'))

        paysyses = Taxi.FIRM_CURRENCY_TO_PAYSYS[firm][currency]
        payment_paysys = paysyses['payment']

        invoice_data = [TaxiData.create_expected_taxi_invoice_data_row(contract_id, person_id, amount,
                                                                       TaxiPayment.INVOICE_TYPE_ACCOUNT,
                                                                       payment_paysys,
                                                                       total_act_sum, nds, firm, currency,
                                                                       dt=dt)]

        return invoice_data

    @staticmethod
    def create_expected_act_data(amount, act_date):
        return [
            common_data_steps.CommonData.create_expected_act_data(amount, act_date,
                                                                  TaxiPayment.ACT_TYPE)]

    @staticmethod
    def create_expected_order_data_row(service_id, product_id, contract_id, amount, completion_qty=None,
                                       consume_addition=Decimal('0'), amount_close_to=None):
        if completion_qty is None:
            completion_qty = amount

        consume_amount = amount + consume_addition
        if amount_close_to:
            consume_amount = close_to(consume_amount, amount_close_to)
            completion_qty = close_to(completion_qty, amount_close_to)

        return common_data_steps.CommonData.create_expected_order_data(service_id, product_id, contract_id,
                                                                       consume_sum=consume_amount,
                                                                       consume_qty=consume_amount,
                                                                       completion_qty=completion_qty)

    @staticmethod
    def create_expected_taxi_order_data(contract_id, currency, services, payment_amount=Decimal('0'),
                                        end_dt=None, completion_absence_check=True, migration_dt=None,
                                        oebs_compls_service_id=None):
        amount_list, total_amount = TaxiSteps.get_completions_from_both_views(contract_id, end_dt=end_dt,
                                                                              completion_absence_check=completion_absence_check,
                                                                              migration_dt=migration_dt,
                                                                              oebs_compls_service_id=oebs_compls_service_id)

        product_list = TaxiSteps.get_taxi_products_by_currency(currency, services)
        expected_orders = []
        for product in product_list:
            order_amount = 0
            for amount in amount_list:
                if amount['product_id'] == product['product_id']:
                    order_amount = Decimal(amount['amount'])
            expected_orders.append(TaxiData.create_expected_order_data_row(product['service_id'],
                                                                           product['product_id'],
                                                                           contract_id,
                                                                           order_amount,
                                                                           amount_close_to=Decimal('0.02')))

        expected_orders.sort(key=lambda k: (k['completion_qty'], k['service_code']))
        return expected_orders

    @classmethod
    def create_expected_completions_data_by_orders_data(cls, orders_data, nds, round_totals=True):
        sums_by_product = defaultdict(lambda: Decimal('0'))
        for order_dict in orders_data:
            product_id = cls.map_order_dict_to_product(order_dict)
            assert product_id, order_dict
            amount = nds.koef_on_dt(order_dict['dt']) * order_dict['commission_sum'] \
                - max(order_dict['subsidy_sum'], Decimal('0')) - order_dict['promocode_sum']
            sums_by_product[product_id] += amount

        if round_totals:
            for product_id in sums_by_product:
                sums_by_product[product_id] = utils.dround2(sums_by_product[product_id])

        return sums_by_product

    @classmethod
    def create_expected_completions_data_by_orders_data_tlog(cls, orders_data, nds, round_totals=True):
        sums_by_product = defaultdict(lambda: Decimal('0'))
        order_type_signs = {TaxiOrderType.subsidy_tlog: -1, TaxiOrderType.promocode_tlog: -1}
        for order_dict in orders_data:
            product_id = cls.map_order_dict_to_product_tlog(order_dict)
            assert product_id, order_dict
            tlog_version = order_dict.get('tlog_version', None)
            tlog_version = 1 if tlog_version is None else tlog_version
            sign = 1 if tlog_version >= 2 else order_type_signs.get(order_dict['type'], 1)
            if order_dict['type'] in cls.no_nds_order_types:
                amount = sign * order_dict['amount']
            else:
                amount = sign * nds.koef_on_dt(order_dict['dt']) * order_dict['amount']
            sums_by_product[product_id] += amount

        if round_totals:
            for product_id in sums_by_product:
                sums_by_product[product_id] = utils.dround2(sums_by_product[product_id])

        return sums_by_product

    # метод для подготовки ожидаемых данных по корректировке
    @staticmethod
    def create_expected_correction_data(partner_id, contract_id, person_id, invoice_eid, dt, amount,
                                        context, internal=None, transaction_type=TransactionType.REFUND,
                                        payment_type=PaymentType.CORRECTION_COMMISSION, service_id=None,
                                        paysys_type_cc=TaxiNetting.PAYSYS_TYPE_CC):
        if amount == Decimal('0'):
            return {}
        return {
            'commission_currency': context.currency.char_code,
            'partner_id': partner_id,
            'paysys_type_cc': paysys_type_cc,
            'contract_id': contract_id,
            'auto': TaxiNetting.AUTO,
            'commission_iso_currency': context.currency.iso_code,
            'amount_fee': TaxiNetting.AMOUNT_FEE,
            'amount': close_to(amount, Decimal('0.04')),
            'transaction_type': transaction_type.name,
            'currency': context.currency.char_code,
            'yandex_reward': TaxiNetting.YANDEX_REWARD,
            'internal': internal,
            'oebs_org_id': context.firm.oebs_org_id,
            'partner_currency': context.currency.char_code,
            'person_id': person_id,
            'service_id': service_id or context.service.id,
            'partner_iso_currency': context.currency.iso_code,
            'iso_currency': context.currency.iso_code,
            'yandex_reward_wo_nds': TaxiNetting.YANDEX_REWARD_WO_NDS,
            'invoice_eid': invoice_eid,
            'dt': dt,
            'transaction_dt': dt,
            'payment_type': payment_type,
            'id': anything(),
        }


    @staticmethod
    def create_expected_postpay_taxi_balance(client_id, contract_id, commission, promocode_amount=Decimal('0'),
                                             promocode_left=Decimal('0'), currency=Currencies.RUB, com_close_to=None):
        return [{'BonusLeft': promocode_left,
                 'CurrMonthBonus': promocode_amount,
                 'CommissionToPay': commission if not com_close_to else close_to(commission, com_close_to),
                 'ClientID': client_id,
                 'Currency': currency.iso_code,
                 'ContractID': contract_id,
                 'DT': anything()
                 }]

    @staticmethod
    def create_expected_prepay_taxi_balance(client_id, contract_id, balance, charge=Decimal('0'),
                                            promocode_amount=Decimal('0'), promocode_left=Decimal('0'),
                                            currency=Currencies.RUB, balance_close_to=None):
        _, external_invoice_id = TaxiSteps.get_completions_taxi_invoice(contract_id)
        return [{'BonusLeft': promocode_left,
                 'CurrMonthBonus': promocode_amount,
                 'SubscriptionBalance': Decimal('0'),
                 'SubscriptionRate': Decimal('0'),
                 'ClientID': client_id,
                 'Currency': currency.iso_code,
                 'CurrMonthCharge': charge if not balance_close_to else close_to(charge, balance_close_to),
                 'Balance': balance if not balance_close_to else close_to(balance, balance_close_to),
                 'ContractID': contract_id,
                 'PersonalAccountExternalID': external_invoice_id,
                 'DT': anything()
                 }]


class SubventionParams(object):
    class NoSubsidy(object):
        def process_orders_data(self, orders_data):
            for order_dict in orders_data:
                order_dict['subsidy_sum'] = 0
            return orders_data

        def process_orders_data_tlog(self, orders_data_tlog):
            return filter(lambda order_dict: order_dict['type'] != TaxiOrderType.subsidy_tlog, orders_data_tlog)

    class DefaultSubsidy(object):
        def process_orders_data(self, orders_data):
            return orders_data

        def process_orders_data_tlog(self, orders_data_tlog):
            return orders_data_tlog

    class CertainSubsidy(object):
        def __init__(self, subsidy_sum):
            self.subsidy_sum = subsidy_sum

        def process_orders_data(self, orders_data):
            for order_dict in orders_data:
                if order_dict['subsidy_sum']:
                    order_dict['subsidy_sum'] = self.subsidy_sum
            return orders_data

        def process_orders_data_tlog(self, orders_data_tlog):
            orders_data_tlog = filter(lambda _order_dict: _order_dict['type'] != TaxiOrderType.subsidy_tlog or
                                                          (_order_dict['type'] == TaxiOrderType.subsidy_tlog and _order_dict['amount'] > 0),
                                      orders_data_tlog)
            for order_dict in orders_data_tlog:
                if order_dict['type'] == TaxiOrderType.subsidy_tlog:
                    order_dict['amount'] = self.subsidy_sum
            return orders_data_tlog

    class NoPromo(object):
        def process_orders_data(self, orders_data):
            for order_dict in orders_data:
                order_dict['promocode_sum'] = 0
            return orders_data

        def process_orders_data_tlog(self, orders_data_tlog):
            return filter(lambda order_dict: order_dict['type'] != TaxiOrderType.promocode_tlog, orders_data_tlog)

    class DefaultPromo(object):
        def process_orders_data(self, orders_data):
            return orders_data

        def process_orders_data_tlog(self, orders_data_tlog):
            return orders_data_tlog

    class CertainPromo(object):
        def __init__(self, promocode_sum):
            self.promocode_sum = promocode_sum

        def process_orders_data(self, orders_data):
            for order_dict in orders_data:
                if order_dict['promocode_sum']:
                    order_dict['promocode_sum'] = self.promocode_sum
            return orders_data

        def process_orders_data_tlog(self, orders_data_tlog):
            orders_data_tlog = filter(lambda _order_dict: _order_dict['type'] != TaxiOrderType.promocode_tlog or
                                                          (_order_dict['type'] == TaxiOrderType.promocode_tlog and _order_dict['amount'] > 0),
                                      orders_data_tlog)
            for order_dict in orders_data_tlog:
                if order_dict['type'] == TaxiOrderType.promocode_tlog:
                    order_dict['amount'] = self.promocode_sum
            return orders_data_tlog
