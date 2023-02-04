# coding=utf-8
__author__ = 'igogor'

import uuid
import datetime
from decimal import Decimal

from hamcrest import has_item, not_none, any_of, has_length, greater_than_or_equal_to

import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
import partner_steps
import simpleapi.steps.simple_steps as simpleapi_steps
from btestlib.constants import Currencies, Services, TransactionType, PaymentType, PaysysType, Export
from btestlib.data import simpleapi_defaults
from btestlib.data.defaults import EventsTickets
import btestlib.config as balance_config
from cashmachines.data.constants import CMNds
from common_steps import CommonSteps
from export_steps import ExportSteps
from simpleapi.common.payment_methods import TrustWebPage, Via, Compensation, CompensationDiscount, LinkedCard, \
    UberRoamingCard
from simpleapi.data import cards_pool
from simpleapi.data import defaults as simpleapi_simpleapi_defaults
from simpleapi.data import uids_pool as uids
from simpleapi.steps import payments_api_steps
from simpleapi.steps import balance_steps

log_align = 30
# log = reporter.logger()

# еще дефолтные даты есть в btestlib.data.defaults.Date
to_iso = utils.Date.date_to_iso_format
NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
A_DAY_AFTER_TOMORROW = NOW + datetime.timedelta(days=2)


class SimpleApi(object):
    @staticmethod
    def create_service_product(service, partner_id=None, service_fee=None, fiscal_nds=CMNds.NDS_NONE,
                               prices=simpleapi_simpleapi_defaults.product_prices, service_product_id=None):
        with reporter.step(u'Вызываем CreateServiceProduct для сервиса: {}, партнера: {}, service_fee: {}'
                                   .format(service, partner_id, service_fee)):
            service_product_id = service_product_id or utils.generate_string_id()
            reporter.attach(u'ID продукта сервиса', utils.Presenter.pretty(service_product_id))

            simpleapi_steps.create_service_product(service=service, service_product_id=service_product_id,
                                                   partner_id=partner_id, service_fee=service_fee,
                                                   fiscal_nds=str(fiscal_nds.name),
                                                   fiscal_title='test_fiscal_title',
                                                   prices=prices)

            return service_product_id

    @staticmethod
    def create_thenumberofthebeast_service_product(service, partner_id, service_fee=None,
                                                   fiscal_nds=None, fiscal_title=None):
        '''При обработке ThirdPartyTransaction функция thirdparty_transaction.get_service_products ищет сервисные продукты
         и если не находит, то создает через Траст.
         Поэтому, для того, чтобы работал только фейковый Траст, дополнительные сервисные продукты нужно создавать заранее.
         Смотри в коде Баланса thirdparty_transaction.ServiceProductJsonRowCopier
         (name = 'transaction_row_new_order_by_service_product_json_copier')'''

        def get_thenumberofthebeast_product_id(service_id, partner_id, service_fee):
            external_product_id = '%s000%s' % (service_id, partner_id)
            if service_fee is not None:
                external_product_id += '-%s' % service_fee
            return external_product_id

        with reporter.step(u'Создаем сервисный продукт, необходимый при создании ThirdPartyTransaction для копирования строк: {}, партнера: {}, service_fee: {}'
                                   .format(service, partner_id, service_fee)):
            external_product_id = get_thenumberofthebeast_product_id(service.id, partner_id, service_fee)
            params = {
                'service_fee': service_fee,
                'partner_id': partner_id,
                'name': 'partner_service_product',
                'service_product_id': external_product_id,
                'type_': 'app'
            }
            if fiscal_nds:
                fiscal_nds = str(fiscal_nds.name)
                params.update({
                    'fiscal_nds': fiscal_nds,
                    'fiscal_title': fiscal_title,
                    'service_product_id': '{}-{}'.format(external_product_id, fiscal_nds),
                })
            return simpleapi_steps.create_service_product(service, **params)

    @staticmethod
    def create_partner(service):
        partner_id = simpleapi_steps.create_partner(service)[1]

        reporter.attach(u'[ID] partner', utils.Presenter.pretty(partner_id))

        # Для коллег из OEBS: публикуем ID объекта в отчёте, выгружаем объект в OEBS и публикуем лог выгрузки
        if balance_config.TRUST_ME_I_AM_OEBS_QA:
            ExportSteps.export_oebs(client_id=partner_id)
        return partner_id

    @staticmethod
    def create_partner_and_product(service, prices=None, service_fee=None):
        with reporter.step(u'Создаем партнера и продукт: {}'.format(service.name)):
            partner_id = SimpleApi.create_partner(service)

            service_product_id = SimpleApi.create_service_product(service, partner_id, prices=prices,
                                                                  service_fee=service_fee)

            return partner_id, service_product_id

    @staticmethod
    def get_reward_refund_for_service(service):
        with reporter.step(u'Получаем reward refund для сервиса: {}'.format(service.name)):
            query = "SELECT reward_refund FROM t_thirdparty_service WHERE id = :service_id"
            params = {'service_id': service.id}

            reward_refund = db.balance().execute(query, params)[0]['reward_refund']
            reporter.attach(u'Reward refund', utils.Presenter.pretty(reward_refund))

            return reward_refund

    @staticmethod
    def get_yandex_reward(amount, percent, service, nds, is_refund=0, is_compensation=0,
                          min_commission=Decimal('0.01'), precision=2):
        if is_compensation:
            reward = None
        elif is_refund and not is_compensation:
            is_reward_needed = SimpleApi.get_reward_refund_for_service(service)
            if is_reward_needed:
                reward = max(round(amount / Decimal('100') * percent, precision), min_commission)
            else:
                reward = None
        else:
            reward = max(round(amount / Decimal('100') * percent, precision), min_commission)
        return reward

    @staticmethod
    def get_payment_by_trust_payment_id(trust_payment_id):
        with reporter.step(u'Получаем payment_id по trust_payment_id: {}'.format(trust_payment_id)):
            payment_query = "SELECT * FROM t_payment WHERE id = (SELECT id FROM t_ccard_bound_payment WHERE trust_payment_id = :trust_payment_id)"
            payment_params = {'trust_payment_id': trust_payment_id}

            payment = db.balance().execute(payment_query, payment_params)[0]
            reporter.attach(u'Payment ID', utils.Presenter.pretty(payment))

            return payment

    @staticmethod
    def get_payment_amount(trust_payment_id):
        with reporter.step(u'Считаем сумму платежа по всем строчкам'):
            query = "SELECT sum(AMOUNT) sum FROM V_PAYMENT_TRUST " \
                    "WHERE TRUST_PAYMENT_ID=:trust_payment_id GROUP BY TRUST_PAYMENT_ID"
            params = {'trust_payment_id': trust_payment_id}
            return db.balance().execute(query, params)[0]['sum']

    @staticmethod
    def create_trust_payment(service, service_product_id,
                             service_order_id=None,
                             currency=Currencies.RUB,
                             region_id=simpleapi_defaults.DEFAULT_REGION_ID,
                             commission_category=simpleapi_defaults.DEFAULT_COMMISSION_CATEGORY,
                             price=simpleapi_defaults.DEFAULT_PRICE,
                             user=None,
                             paymethod=None,
                             user_ip=simpleapi_defaults.DEFAULT_USER_IP,
                             order_dt=None,
                             need_postauthorize=True,
                             developer_payload=None,
                             fiscal_nds=None,
                             developer_payload_basket=None,
                             pass_params=None,
                             pass_cvn=False,
                             back_url=None,
                             qty=None,
                             wait_for_export_from_bs=True,
                             export_payment=False):
        service_order_id_list, trust_payment_id, purchase_token, payment_id = \
            SimpleApi.create_multiple_trust_payments(service, [service_product_id], [service_order_id], currency,
                                                     region_id, [commission_category], [price], user, paymethod,
                                                     user_ip, order_dt, need_postauthorize, [developer_payload],
                                                     [fiscal_nds], developer_payload_basket, pass_params, pass_cvn,
                                                     back_url, [qty],
                                                     wait_for_export_from_bs=wait_for_export_from_bs)
        if export_payment:
            partner_steps.CommonPartnerSteps.export_payment(payment_id)

        return service_order_id_list[0], trust_payment_id, purchase_token, payment_id

    # a-vasin: не очень красиво с UBER_ROAMING, но передавать руками каждый раз в паре с сервисом хуже;
    # если будут еще подобные случаи, то как-нибудь обобщим
    # back_url должен резолвиться, иначе в bo не передается
    @staticmethod
    def create_multiple_trust_payments(service, service_product_id_list,
                                       service_order_id_list=None,
                                       currency=Currencies.RUB,
                                       region_id=simpleapi_defaults.DEFAULT_REGION_ID,
                                       commission_category_list=None,
                                       prices_list=None,
                                       user=None,
                                       paymethod=None,
                                       user_ip=simpleapi_defaults.DEFAULT_USER_IP,
                                       order_dt=None,
                                       need_postauthorize=True,
                                       developer_payload_list=None,
                                       fiscal_nds_list=None,
                                       developer_payload_basket=None,
                                       pass_params=None,
                                       pass_cvn=False,
                                       back_url=None,
                                       qty_list=None,
                                       wait_for_export_from_bs=True,
                                       paymethod_markup=None,
                                       spasibo_order_map=None):
        with reporter.step(u'Создаем платеж в трасте для сервиса: {} и продуктов: {}'
                                   .format(service.name, service_product_id_list)):
            if user is None:
                user = uids.get_random_of_type(uids.Types.uber) if service == Services.UBER_ROAMING \
                    else simpleapi_defaults.DEFAULT_USER

            if prices_list is None:
                prices_list = [simpleapi_defaults.DEFAULT_PRICE] * len(service_product_id_list)

            if commission_category_list is None:
                commission_category_list = \
                    [simpleapi_defaults.DEFAULT_COMMISSION_CATEGORY] * len(service_product_id_list)

            if service_order_id_list is None or service_order_id_list == [None]:
                service_order_id_list = [None] * len(service_product_id_list)

            if developer_payload_list is None or len(developer_payload_list) != len(service_product_id_list):
                val = developer_payload_list[-1] if developer_payload_list else None
                developer_payload_list = [val] * len(service_product_id_list)

            if paymethod_markup and not spasibo_order_map:
                # если создается композитный платеж со "спасибо" сбера, то должен быть передан меппинг
                # spasibo_order_map, в котором наши service_order_id меппятся на сберовские.
                # В случае теста на данный момент сберовские service_order_id могут быть произвольными,
                # но меппинг обязан присутствовать
                spasibo_order_map = {}
                for service_order_id, pay_type_amount_dict in paymethod_markup.iteritems():
                    if 'spasibo' in pay_type_amount_dict.keys():
                        spasibo_order_map[service_order_id] = service_order_id + '_'

            # создаем заказ
            service_order_id_list = [
                SimpleApi.create_order_or_subscription(service, service_product_id, user, region_id,
                                                       service_order_id=service_order_id,
                                                       commission_category=commission_category,
                                                       user_ip=user_ip,
                                                       order_dt=order_dt, developer_payload=developer_payload)
                for service_product_id, service_order_id, commission_category, developer_payload
                in
                zip(service_product_id_list, service_order_id_list, commission_category_list, developer_payload_list)]

            orders = simpleapi_defaults.create_default_orders(service_order_id_list, prices_list, fiscal_nds_list,
                                                              qty_list)

            if paymethod is None:
                if service == Services.UBER_ROAMING:
                    paymethod = UberRoamingCard()
                elif currency == Currencies.KZT:
                    paymethod = LinkedCard(card=cards_pool.RBS.Success.Without3DS.card_mastercard)
                else:
                    paymethod = LinkedCard(card=simpleapi_defaults.STATIC_EMULATOR_CARD)

            basket = simpleapi_steps.process_payment(service, user, orders=orders, paymethod=paymethod,
                                                     currency=currency.iso_code, need_postauthorize=need_postauthorize,
                                                     developer_payload=developer_payload_basket, back_url=back_url,
                                                     paymethod_markup=paymethod_markup, pass_params=pass_params,
                                                     pass_cvn=pass_cvn,
                                                     spasibo_order_map=spasibo_order_map)

            purchase_token = basket['purchase_token']
            trust_payment_id = basket['trust_payment_id']

            if wait_for_export_from_bs:
                payment_id = SimpleApi.wait_for_payment(trust_payment_id)
            else:
                payment_id = None

            return service_order_id_list, trust_payment_id, purchase_token, payment_id

    @staticmethod
    def get_bs_payment_id(trust_payment_id, field='id', search_field='trust_payment_id'):
        query = "SELECT {} FROM t_payment WHERE {}=:trust_payment_id".format(field, search_field)
        params = {'trust_payment_id': trust_payment_id}
        return db.balance_bs().execute(
                query, params,
                descr=u'Получаем поля {} из t_payment на базе траста'.format(field))[0][field]

    @staticmethod
    @utils.measure_time()
    def wait_for_export_from_bs(trust_payment_id, field, search_field, wait_rate=None):
        with reporter.step(u'Ожидаем экспорта из bs, trust_payment_id: {}'.format(trust_payment_id)):
            if SimpleApi.is_logbroker_export(trust_payment_id, field, search_field):
                try:
                    SimpleApi.wait_for_single_export_from_logbroker(trust_payment_id, field, search_field, wait_rate)
                except ValueError:  # временный костыль-фоллбек на случай если траст переключает на старый экспорт.
                    with reporter.step(u'Не найдена запись для экспорта через LOGBROKER, fallback в BALANCE'):
                        SimpleApi.wait_for_single_export_from_bs(trust_payment_id, field, search_field, wait_rate)
            elif SimpleApi.is_batch_export(trust_payment_id, field, search_field):
                SimpleApi.wait_for_batch_export_from_bs(trust_payment_id, field, search_field, wait_rate)
            else:
                SimpleApi.wait_for_single_export_from_bs(trust_payment_id, field, search_field, wait_rate)

    @staticmethod
    def get_service_id_for_bs_payment(payment_id):
        return db.balance_bs().execute(
            query="select service_id from t_payment where id = '{}'".format(payment_id),
            single_row=True, fail_empty=True,
            descr=u'Получаем сервис платежа'
        )['service_id']

    @staticmethod
    def wait_for_payment(trust_payment_id):
        wait = utils.wait_until2(partner_steps.CommonPartnerSteps.get_trust_payments,
                                 has_length(greater_than_or_equal_to(1)))
        payments = wait(trust_payment_id)
        return payments[0]['payment_id']

    @staticmethod
    def wait_for_refund(trust_refund_id):
        wait = utils.wait_until2(partner_steps.CommonPartnerSteps.get_trust_refunds,
                                 has_length(greater_than_or_equal_to(1)))
        payments = wait(trust_refund_id)
        return payments[0]['id']

    @staticmethod
    def wait_for_payment_by_purchase_token(purchase_token):
        with reporter.step(u'Ожидаем появления платежа в t_payment'):
            def get_entity_id():
                try:
                    return SimpleApi.get_payment_ids_by_purchase_token(purchase_token)
                except ValueError:
                    return None

            return utils.wait_until(get_entity_id, not_none(), timeout=360, sleep_time=5)

    @staticmethod
    def create_refund(service, service_order_id, trust_payment_id, service_order_id_fee=None,
                      delta_amount=simpleapi_defaults.DEFAULT_PRICE, export_payment=False,
                      paymethod_markup=None, spasibo_order_map=None):
        service_order_id_list = [service_order_id]
        delta_amount_list = [delta_amount]

        if service_order_id_fee:
            service_order_id_list.append(service_order_id_fee)
            delta_amount_list.append(simpleapi_defaults.DEFAULT_FEE)

        return SimpleApi.create_multiple_refunds(service, service_order_id_list, trust_payment_id,
                                                 delta_amount_list, export_payment=export_payment,
                                                 paymethod_markup=paymethod_markup, spasibo_order_map=spasibo_order_map)

    @staticmethod
    def create_multiple_refunds(service, service_order_id_list, trust_payment_id, delta_amount_list=None,
                                export_payment=False, paymethod_markup=None, spasibo_order_map=None):
        with reporter.step(u'Создаем рефанд для сервиса: {} и заказов: {} с платежом: {}'
                                   .format(service.name, service_order_id_list, trust_payment_id)):
            if delta_amount_list is None:
                delta_amount_list = [simpleapi_defaults.DEFAULT_PRICE] * len(service_order_id_list)

            orders = simpleapi_defaults.create_default_refund_orders(service_order_id_list, delta_amount_list)

            if paymethod_markup and not spasibo_order_map:
                # если создается композитный платеж со "спасибо" сбера, то должен быть передан меппинг
                # spasibo_order_map, в котором наши service_order_id меппятся на сберовские.
                # В случае теста на данный момент сберовские service_order_id могут быть произвольными,
                # но меппинг обязан присутствовать
                spasibo_order_map = {}
                for service_order_id, pay_type_amount_dict in paymethod_markup.iteritems():
                    if 'spasibo' in pay_type_amount_dict.keys():
                        spasibo_order_map[service_order_id] = service_order_id + '_'

            refund = simpleapi_steps.create_refund(service, simpleapi_defaults.DEFAULT_USER_IP,
                                                   simpleapi_defaults.REFUND_REASON, orders, trust_payment_id,
                                                   user=simpleapi_defaults.USER_ANONYMOUS,
                                                   paymethod_markup=paymethod_markup,
                                                   spasibo_order_map=spasibo_order_map)
            trust_refund_id = refund['trust_refund_id']

            simpleapi_steps.wait_until_refund_done(service, user=simpleapi_defaults.USER_ANONYMOUS,
                                                   trust_refund_id=trust_refund_id)
            SimpleApi.wait_for_payment(trust_payment_id)

            with reporter.step(u'Получаем id рефанда по trust_refund_id: {}'.format(trust_refund_id)):
                refund_query = "SELECT id AS value FROM t_refund WHERE trust_refund_id = :trust_refund_id"
                refund_params = {'trust_refund_id': trust_refund_id}

                refund_id = CommonSteps.wait_for_value(refund_query, refund_params, interval=3, timeout=300)
                reporter.attach(u'ID рефанда', utils.Presenter.pretty(refund_id))

            if export_payment:
                partner_steps.CommonPartnerSteps.export_payment(refund_id)

            return trust_refund_id, refund_id

    @staticmethod
    def create_fake_thirdparty_payment(third_party_data, contract_id, person_id, partner_id,
                                       amount, reward=None, transaction_type=TransactionType.PAYMENT, payment_type=None,
                                       dt=datetime.datetime.today(), invoice_eid=None, paysys_partner_id=None,
                                       client_id=None, immutable=None, client_amount=None, amount_fee=None,
                                       internal=None, contract_currency=None, paysys_type=None, payout_ready_dt=None,
                                       service=None, payment_currency=None, product=None, is_correction=False):
        with reporter.step(u'Вставляем запись о платеже в таблицу T_THIRDPARTY_TRANSACTIONS для: {}, договор: {}'
                                   .format(third_party_data.name, contract_id)):
            if service is None:
                service = third_party_data.service

            if not payment_type:
                payment_type = third_party_data.payment_type

            if not contract_currency:
                contract_currency = third_party_data.contract_currency

            if not payment_currency:
                payment_currency = third_party_data.currency

            if not paysys_type:
                paysys_type = third_party_data.paysys_type

            id = db.balance().sequence_nextval('s_request_order_id')

            if is_correction:
                sql_insert_transaction = "INSERT INTO T_THIRDPARTY_CORRECTIONS (ID,CONTRACT_ID,PERSON_ID," \
                                         "TRANSACTION_TYPE,TRUST_PAYMENT_ID,PARTNER_ID,DT,SERVICE_ID,COMMISSION_CURRENCY,CURRENCY," \
                                         "PAYSYS_TYPE_CC,PAYMENT_TYPE,PAYSYS_PARTNER_ID,TRANSACTION_DT," \
                                         "AMOUNT,AMOUNT_FEE,YANDEX_REWARD," \
                                         "PARTNER_CURRENCY,CLIENT_ID,CLIENT_AMOUNT,INTERNAL," \
                                         "OEBS_ORG_ID,COMMISSION_ISO_CURRENCY,ISO_CURRENCY,PARTNER_ISO_CURRENCY," \
                                         "INVOICE_EID, PAYOUT_READY_DT, STARTRACK_ID) " \
                                         "VALUES (:id,:contract_id,:person_id,:transaction_type," \
                                         ":trust_payment_id, :partner_id,:dt,:service_id,:commission_currency,:currency,:paysys_type_cc," \
                                         ":payment_type,:paysys_partner_id,:dt,:amount,0," \
                                         "0,:partner_currency,:client_id," \
                                         ":client_amount,:internal,:oebs_org_id,:commission_iso_currency,:iso_currency," \
                                         ":partner_iso_currency,:invoice_eid, :payout_ready_dt, :startrack_id)"
            else:
                sql_insert_transaction = "INSERT INTO T_THIRDPARTY_TRANSACTIONS (ID,CONTRACT_ID,PERSON_ID," \
                                         "TRANSACTION_TYPE,PARTNER_ID,DT,SERVICE_ID,COMMISSION_CURRENCY,CURRENCY," \
                                         "PAYSYS_TYPE_CC,PAYMENT_TYPE,PAYSYS_PARTNER_ID,TRANSACTION_DT," \
                                         "AMOUNT,AMOUNT_FEE,YANDEX_REWARD," \
                                         "PARTNER_CURRENCY,CLIENT_ID,IMMUTABLE,CLIENT_AMOUNT,INTERNAL," \
                                         "OEBS_ORG_ID,COMMISSION_ISO_CURRENCY,ISO_CURRENCY,PARTNER_ISO_CURRENCY," \
                                         "INVOICE_EID, PAYOUT_READY_DT, PRODUCT_ID) " \
                                         "VALUES (:id,:contract_id,:person_id,:transaction_type," \
                                         ":partner_id,:dt,:service_id,:commission_currency,:currency,:paysys_type_cc," \
                                         ":payment_type,:paysys_partner_id,:dt,:amount,:amount_fee," \
                                         ":yandex_reward,:partner_currency,:client_id,:immutable," \
                                         ":client_amount,:internal,:oebs_org_id,:commission_iso_currency,:iso_currency," \
                                         ":partner_iso_currency,:invoice_eid, :payout_ready_dt, :product_id)"
            params = {
                'id': id,
                'contract_id': contract_id,
                'person_id': person_id,
                'dt': dt,
                'partner_id': partner_id,
                'transaction_dt': dt,
                'transaction_type': transaction_type.name,
                'service_id': service.id,
                'commission_currency': contract_currency.char_code,
                'currency': payment_currency.char_code,
                'paysys_type_cc': paysys_type,
                'payment_type': payment_type,
                'paysys_partner_id': paysys_partner_id,
                'amount': amount,
                'amount_fee': amount_fee,
                'yandex_reward': reward,
                'partner_currency': contract_currency.char_code,
                'client_id': client_id,
                'immutable': immutable,
                'client_amount': client_amount,
                'internal': internal,
                'oebs_org_id': third_party_data.firm.oebs_org_id,
                'commission_iso_currency': contract_currency.iso_code,
                'iso_currency': payment_currency.iso_code,
                'partner_iso_currency': contract_currency.iso_code,
                'invoice_eid': invoice_eid,
                'payout_ready_dt': payout_ready_dt,
                'product_id': product.id if product else None,
                'trust_payment_id': 'fake_trust1010101',
                'startrack_id': 'TESTCORRECTION'
            }
            db.balance().execute(sql_insert_transaction, params)

    @staticmethod
    def generate_fake_trust_payment_id():
        query = "select lower(rawtohex(dbms_obfuscation_toolkit.md5(input_string => to_char(SYSTIMESTAMP, 'SSSSS.FF') || S_TEST_TRUST_PAYMENT_ID.nextval))) as trust_payment_id from dual"
        return db.balance().execute(query)[0]['trust_payment_id']

    @staticmethod
    def create_actotron_act_row(client_id,
                                contract_id,
                                service_id,
                                amount,
                                finish_dt,
                                mdh_product_id,
                                act_nds_pct=20,
                                currency='RUB',
                                payload=None,
                                act_row_id=None,
                                ):
        act_row_id = act_row_id or str(uuid.uuid4())
        query = 'INSERT INTO BO.T_ACTOTRON_ACT_ROWS (' \
                'ACT_ROW_ID, MDH_PRODUCT_ID, ACT_SUM, ACT_EFFECTIVE_NDS_PCT, TARIFFER_SERVICE_ID, ' \
                'ACT_START_DT, ACT_FINISH_DT, CLIENT_ID, CONTRACT_ID, CURRENCY, PAYLOAD) VALUES (' \
                ':act_row_id, :mdh_product_id, :amount, :act_nds_pct, :service_id, ' \
                ':start_dt, :finish_dt, :client_id, :contract_id, :currency, :payload)'
        db.balance().execute(query, {
            'act_row_id': act_row_id,
            'mdh_product_id': mdh_product_id,
            'amount': amount,
            'act_nds_pct': act_nds_pct,
            'service_id': service_id,
            'start_dt': datetime.datetime.fromordinal(finish_dt.toordinal()).replace(day=1),
            'finish_dt': finish_dt,
            'client_id': client_id,
            'contract_id': contract_id,
            'currency': currency,
            'payload': payload
        })

    @staticmethod
    def create_fake_tpt_row(context, partner_id, person_id, contract_id, dt=datetime.datetime.today(),
                            # transaction_type=TransactionType.PAYMENT,
                            is_correction=False, client_id=None, **additional_params):

        if is_correction:
            table_name = 'T_THIRDPARTY_CORRECTIONS'
            additional_columns = ', STARTRACK_ID, AUTO'
            additional_values = ', :startrack_id, :auto'
        else:
            table_name = 'T_THIRDPARTY_TRANSACTIONS'
            additional_columns = ''
            additional_values = ''
        with reporter.step(u'Вставляем запись о платеже или корректировке в таблицу ' + str(
                table_name) + u' для: {}, договор: {}'
                .format(context.name, contract_id)):

            payment_currency = context.payment_currency or context.currency

            id = db.balance().sequence_nextval('s_request_order_id')
            trust_payment_id = SimpleApi.generate_fake_trust_payment_id()

            sql_insert_transaction = "INSERT INTO " + str(table_name) + " (ID,CONTRACT_ID,PERSON_ID,TRUST_PAYMENT_ID,TRUST_ID," \
                                                                        "TRANSACTION_TYPE,PARTNER_ID,DT,SERVICE_ID,COMMISSION_CURRENCY,CURRENCY," \
                                                                        "PAYSYS_TYPE_CC,PAYMENT_TYPE,PAYSYS_PARTNER_ID,TRANSACTION_DT," \
                                                                        "AMOUNT,AMOUNT_FEE,YANDEX_REWARD," \
                                                                        "PARTNER_CURRENCY,CLIENT_ID,CLIENT_AMOUNT,INTERNAL," \
                                                                        "OEBS_ORG_ID,COMMISSION_ISO_CURRENCY,ISO_CURRENCY,PARTNER_ISO_CURRENCY," \
                                                                        "INVOICE_EID, PAYOUT_READY_DT, PRODUCT_ID" + str(
                    additional_columns) + ")" \
                                          "VALUES (:id,:contract_id,:person_id,:trust_payment_id,:trust_id,:transaction_type," \
                                          ":partner_id,:dt,:service_id,:commission_currency,:currency,:paysys_type_cc," \
                                          ":payment_type,:paysys_partner_id,:dt,:amount,:amount_fee," \
                                          ":yandex_reward,:partner_currency,:client_id," \
                                          ":client_amount,:internal,:oebs_org_id,:commission_iso_currency,:iso_currency," \
                                          ":partner_iso_currency,:invoice_eid, :payout_ready_dt, :product_id" + str(
                    additional_values) + ")"

            params = {
                'id': id,
                'contract_id': contract_id,
                'person_id': person_id,
                'trust_payment_id': trust_payment_id,
                'trust_id': trust_payment_id,
                'dt': dt,
                'partner_id': partner_id,
                'transaction_dt': dt,
                # 'transaction_type': TransactionType.PAYMENT.name,
                'service_id': context.service.id,
                'commission_currency': payment_currency.char_code,
                'currency': payment_currency.char_code,
                'paysys_type_cc': context.tpt_paysys_type_cc,
                'payment_type': context.tpt_payment_type,
                'paysys_partner_id': None,
                'amount': simpleapi_defaults.DEFAULT_PRICE,
                'amount_fee': 0 if is_correction else None,  # для корректировки не может быть null
                'yandex_reward': 0 if is_correction else None,  # для корректировки не может быть null
                'partner_currency': context.currency.char_code,
                'client_id': client_id,
                'immutable': None,
                'client_amount': None,
                'internal': None,
                'oebs_org_id': context.firm.oebs_org_id,
                'commission_iso_currency': payment_currency.iso_code,
                'iso_currency': payment_currency.iso_code,
                'partner_iso_currency': context.currency.iso_code,
                'invoice_eid': None,
                'payout_ready_dt': None,
                'product_id': None,
                'startrack_id': 'TESTCORRECTION'
            }

            params.update(additional_params)
            params['transaction_type'] = params['transaction_type'].name
            db.balance().execute(sql_insert_transaction, params)

        # Для коллег из OEBS: публикуем ID объекта в отчёте, выгружаем объект в OEBS и публикуем лог выгрузки
        if balance_config.TRUST_ME_I_AM_OEBS_QA:
            classname = Export.Classname.CORRECTION if is_correction else Export.Classname.TRANSACTION
            ExportSteps.create_export_record(id, classname, type=Export.Type.OEBS)
            if classname == Export.Classname.CORRECTION:
                ExportSteps.export_oebs(correction_id=id)
            if classname == Export.Classname.TRANSACTION:
                ExportSteps.export_oebs(transaction_id=id)

    @staticmethod
    def create_fake_tpt_data(context, partner_id, person_id, contract_id, dt, fake_data, sum_key='amount'):
        final_sum = 0
        for row in fake_data:
            SimpleApi.create_fake_tpt_row(context, partner_id, person_id, contract_id, dt, **row)
            final_sum += (row.get(sum_key) or 0) * row['transaction_type'].sign
        return final_sum

    @staticmethod
    def create_tipical_tpt_data_for_act(context, client_id, person_id, contract_id, dt1, dt2, sum_key='amount',
                                        additional_tpt_params=None, is_general=True):
        def gen_row_dict(sum_, transaction_type):
            res = {sum_key: sum_, 'transaction_type': transaction_type}
            if additional_tpt_params is not None:
                res.update(additional_tpt_params)
            return res

        final_sum_first_month = SimpleApi.create_fake_tpt_data(context, client_id, person_id, contract_id,
                                                               dt1,
                                                               [gen_row_dict(Decimal('3000.4'), TransactionType.PAYMENT),
                                                                gen_row_dict(Decimal('100.05'), TransactionType.REFUND)],
                                                               sum_key=sum_key)

        partner_steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, dt1)
        if is_general:
            CommonSteps.export('MONTH_PROC', 'Client', client_id)

        second_month_part_1 = SimpleApi.create_fake_tpt_data(context, client_id, person_id, contract_id,
                                                             dt1,
                                                             [gen_row_dict(Decimal('30.01'), TransactionType.PAYMENT),
                                                              gen_row_dict(Decimal('4.8'), TransactionType.REFUND)],
                                                             sum_key=sum_key)
        second_month_part_2 = SimpleApi.create_fake_tpt_data(context, client_id, person_id, contract_id,
                                                             dt2,
                                                             [gen_row_dict(Decimal('987.33'), TransactionType.PAYMENT),
                                                              gen_row_dict(Decimal('5.12'), TransactionType.REFUND)],
                                                             sum_key=sum_key)

        partner_steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, dt2)
        if is_general:
            CommonSteps.export('MONTH_PROC', 'Client', client_id)

        return final_sum_first_month, second_month_part_1 + second_month_part_2

    @staticmethod
    def create_expected_tpt_row(context, partner_id, contract_id, person_id, trust_payment_id,
                                payment_id, trust_refund_id=None, trust=True, **additional_params):
        expected_template = {
            'service_id': context.service.id,
            'oebs_org_id': context.firm.oebs_org_id,
            'amount_fee': None,
            'internal': None,
            'client_id': None,
            'client_amount': None,
            'currency': context.payment_currency.char_code,
            'partner_currency': context.currency.char_code,
            'commission_currency': context.currency.char_code,
            'partner_iso_currency': context.currency.iso_code,
            'iso_currency': context.payment_currency.iso_code,
            'commission_iso_currency': context.currency.iso_code,
            'invoice_commission_sum': None,
            'row_paysys_commission_sum': None,
            'invoice_eid': None,
            'yandex_reward': None,
            'paysys_type_cc': context.tpt_paysys_type_cc,
            'payment_type': context.tpt_payment_type,
            'amount': simpleapi_defaults.DEFAULT_PRICE}

        expected_template.update({
            'partner_id': partner_id,
            'contract_id': contract_id,
            'person_id': person_id,
            'payment_id': payment_id,
            'transaction_type': TransactionType.REFUND.name if trust_refund_id else TransactionType.PAYMENT.name
        })

        if trust:
            expected_template.update({
                'trust_payment_id': trust_payment_id,
                'trust_id': trust_refund_id or trust_payment_id,
            })

        expected_template.update(additional_params)

        return expected_template

    @staticmethod
    def create_expected_tpt_row_compensation(context, partner_id, contract_id, person_id, trust_payment_id, payment_id,
                                             trust_refund_id=None, **additional_params):
        return SimpleApi.create_expected_tpt_row(context, partner_id, contract_id, person_id, trust_payment_id,
                                                 payment_id,
                                                 trust_refund_id=trust_refund_id,
                                                 payment_type=PaymentType.COMPENSATION,
                                                 paysys_type_cc=PaysysType.YANDEX,
                                                 **additional_params)

    @staticmethod
    def create_expected_tpt_data(context, partner_id, contract_id, person_id, trust_payment_id, payment_id,
                                 additional_data, trust_refund_id=None):
        expected_data = []
        for row in additional_data:
            expected_data.append(SimpleApi.create_expected_tpt_row(context, partner_id, contract_id, person_id, trust_payment_id, payment_id,
                                              trust_refund_id=trust_refund_id, **row))
        return expected_data

    @staticmethod
    def create_partner_product_and_fee(service, service_fee=1):
        with reporter.step(u'Создаем партнера, продукт и сбор для сервиса: {}'.format(service.name)):
            partner_id = SimpleApi.create_partner(service)

            service_product_id = SimpleApi.create_service_product(service, partner_id)

            service_product_fee_id = SimpleApi.create_service_product(service, partner_id, service_fee=service_fee)

            return partner_id, service_product_id, service_product_fee_id

    @staticmethod
    def get_payment_id(trust_payment_id):
        with reporter.step(u'Получаем ID платежа по ID платежа из траста: {}'.format(trust_payment_id)):
            query = "SELECT id FROM v_payment_trust WHERE TRUST_PAYMENT_ID = :trust_payment_id"
            params = {'trust_payment_id': trust_payment_id}

            rows = db.balance().execute(query, params)
            if not rows:
                raise ValueError('Not found payments for trust_payment_id: "{}"'.format(trust_payment_id))
            payment_id = rows[0]['id']
            reporter.attach(u'ID платежа', utils.Presenter.pretty(payment_id))

            return payment_id

    @staticmethod
    def get_payment_ids_by_purchase_token(purchase_token):
        with reporter.step(u'Получаем ID платежа по purchase_token платежа из траста: {}'.format(purchase_token)):
            query = "SELECT id, trust_payment_id FROM v_payment_trust WHERE purchase_token = :purchase_token"
            params = {'purchase_token': purchase_token}

            rows = db.balance().execute(query, params)
            if not rows:
                raise ValueError('Not found payments for purchase_token: "{}"'.format(purchase_token))
            payment_id = rows[0]['id']
            trust_payment_id = rows[0]['trust_payment_id']
            reporter.attach(u'ID платежа', utils.Presenter.pretty(payment_id))
            reporter.attach(u'TRUST_PAYMENT_ID платежа', utils.Presenter.pretty(trust_payment_id))

            return payment_id, trust_payment_id

    @staticmethod
    def get_trust_payment_id(purchase_token):
        with reporter.step(u'Получаем trust_payment_id по purchase_token = {}'.format(purchase_token)):
            query = "SELECT trust_payment_id FROM bs.t_payment WHERE purchase_token=:purchase_token"
            params = {'purchase_token': purchase_token}
            trust_payment_id = db.balance_bs().execute(query, params)[0]['trust_payment_id']
            reporter.attach(u'trust_payment_id', utils.Presenter.pretty(trust_payment_id))
            return trust_payment_id

    @staticmethod
    def get_trust_refund_id(purchase_token):
        with reporter.step(u'Получаем trust_refund_id по purchase_token = {}'.format(purchase_token)):
            query = "SELECT trust_refund_id FROM t_payment WHERE orig_payment_id = " \
                    "(SELECT id FROM t_payment WHERE purchase_token = :purchase_token)"
            params = {'purchase_token': purchase_token}
            trust_refund_id = db.balance_bs().execute(query, params)[0]['trust_refund_id']
            reporter.attach(u'trust_refund_id', utils.Presenter.pretty(trust_refund_id))
            return trust_refund_id

    @staticmethod
    def get_refund_id_by_trust_refund_id(trust_refund_id):
        with reporter.step(u'Получаем id по trust_refund_id = {}'.format(trust_refund_id)):
            query = "SELECT id FROM t_refund WHERE trust_refund_id = :trust_refund_id"
            params = {'trust_refund_id': trust_refund_id}
            rows = db.balance().execute(query, params)
            if not rows:
                raise ValueError('Not found refunds for trust_refund_id: "{}"'.format(trust_refund_id))
            refund_id = rows[0]['id']
            reporter.attach(u'ID возврата', utils.Presenter.pretty(refund_id))

            return refund_id

    @staticmethod
    def create_fake_service_product(service, partner_id=None, service_fee=None, fiscal_nds=None,
                                    prices=None, service_code=None):
        query = 'select id from bo.t_product where engine_id = :service_id'
        params = {'service_id': service.id}
        if service_code is None:
            query = query + ' and service_code is null'
        else:
            query = query + ' and service_code = :service_code'
            params['service_code'] = service_code

        product_id = db.balance().execute(query, params, descr='Ищем продукт')[0]['id']
        external_id = utils.generate_string_id()
        with reporter.step(u'Создаем фейковый сервисный продукт'):
            db.balance().execute('''
                insert into bo.t_service_product (id, service_id, product_id, name, partner_id, external_id, service_fee)
                values (s_service_product_id.nextval, :service_id, :product_id, 'Service Product', :partner_id,
                        :external_id, :service_fee)
            ''', {
                'service_id': service.id,
                'product_id': product_id,
                'partner_id': partner_id,
                'external_id': external_id,
                'service_fee': service_fee,
                'fiscal_nds': fiscal_nds,
            })
        return external_id

    @staticmethod
    def create_compensation(service, service_product_id, service_order_id=None, user=simpleapi_defaults.DEFAULT_USER,
                            currency=simpleapi_defaults.DEFAULT_CURRENCY, is_discount=False, commission_category=None,
                            region_id=simpleapi_defaults.DEFAULT_REGION_ID, order_dt=None, export_payment=False):
        with reporter.step(
                u'Создаем компенсацию для сервиса: {}, продукта: {}'.format(service.name, service_product_id)):
            paymethod = CompensationDiscount() if is_discount else Compensation()

            return SimpleApi.create_trust_payment(service, service_product_id, service_order_id, currency, region_id,
                                                  commission_category, user=user, paymethod=paymethod,
                                                  order_dt=order_dt, export_payment=export_payment)

    @staticmethod
    def create_order_or_subscription(service, service_product_id, user=simpleapi_defaults.DEFAULT_USER,
                                     region_id=simpleapi_defaults.DEFAULT_REGION_ID, commission_category=None,
                                     service_order_id=None, user_ip=simpleapi_defaults.DEFAULT_USER_IP,
                                     order_dt=None, developer_payload=None):
        order = simpleapi_steps.create_order_or_subscription(service, user, user_ip,
                                                             service_product_id, region_id,
                                                             service_order_id=service_order_id,
                                                             commission_category=commission_category,
                                                             start_ts=order_dt, developer_payload=developer_payload)
        return order['service_order_id']

    @staticmethod
    def create_tickets_payment_with_id(service, product, user=simpleapi_defaults.DEFAULT_USER, product_fee=None,
                                       discounts=None, promocode_id=None, paymethod=None,
                                       region_id=simpleapi_defaults.DEFAULT_REGION_ID,
                                       currency=simpleapi_defaults.DEFAULT_CURRENCY):
        with reporter.step(u'Создаем платеж и регистр для продукта: {}, сбора: {}'.format(product, product_fee)):
            reporter.attach(u'Скидки', utils.Presenter.pretty(discounts))
            reporter.attach(u'Промокод', utils.Presenter.pretty(promocode_id))

            purchase_token, service_order_id_fee, service_order_id_product, trust_payment_id = \
                SimpleApi.create_tickets_payment(service, product, user, product_fee, discounts, promocode_id,
                                                 paymethod, region_id=region_id, currency=currency)

            payment_id = SimpleApi.get_payment_by_trust_payment_id(trust_payment_id)['id']

            return payment_id, trust_payment_id, service_order_id_product, service_order_id_fee, purchase_token

    @staticmethod
    def create_multiple_tickets_payment(service, products, user=simpleapi_defaults.DEFAULT_USER,
                                        product_fees=None, discounts=None, promocode_id=None, paymethod=None,
                                        region_id=simpleapi_defaults.DEFAULT_REGION_ID,
                                        currency=simpleapi_defaults.DEFAULT_CURRENCY):
        with reporter.step(u'Создаем платеж для продуктов: {}, сборов: {}'.format(products, product_fees)):
            reporter.attach(u'Скидки', utils.Presenter.pretty(discounts))
            reporter.attach(u'Промокод', utils.Presenter.pretty(promocode_id))

            service_order_id_products = [SimpleApi.create_order_or_subscription(
                service, product, user=user,
                region_id=region_id,
                commission_category=simpleapi_defaults.DEFAULT_COMMISSION_CATEGORY) for product in products]

            orders = simpleapi_defaults.create_default_orders(service_order_id_products)

            service_order_id_fees = None
            if product_fees is not None:
                service_order_id_fees = [SimpleApi.create_order_or_subscription(service, product_fee,
                                                                                region_id=region_id,
                                                                                user=user)
                                         for product_fee in product_fees]

                orders += simpleapi_defaults.create_default_orders(service_order_id_fees,
                                                                   [simpleapi_defaults.DEFAULT_FEE] * len(
                                                                           product_fees))

            if paymethod is None:
                paymethod = TrustWebPage(Via.card(simpleapi_defaults.STATIC_EMULATOR_CARD, unbind_before=False))

            basket = simpleapi_steps.process_payment(service, user, orders=orders, paymethod=paymethod,
                                                     discounts=discounts, promocode_id=promocode_id,
                                                     need_postauthorize=True,
                                                     region_id=region_id,
                                                     currency=currency.iso_code)

            trust_payment_id = basket['trust_payment_id']
            purchase_token = basket['purchase_token']

            SimpleApi.wait_for_payment(trust_payment_id)

            return purchase_token, service_order_id_fees, service_order_id_products, trust_payment_id

    @staticmethod
    def create_tickets_payment(service, product, user=simpleapi_defaults.DEFAULT_USER,
                               product_fee=None, discounts=None, promocode_id=None, paymethod=None,
                               region_id=simpleapi_defaults.DEFAULT_REGION_ID,
                               currency=simpleapi_defaults.DEFAULT_CURRENCY):
        purchase_token, service_order_id_fees, service_order_id_products, trust_payment_id = \
            SimpleApi.create_multiple_tickets_payment(service, [product], user, [product_fee] if product_fee else None,
                                                      discounts, promocode_id, paymethod,
                                                      region_id=region_id, currency=currency)

        return purchase_token, service_order_id_fees[0] if service_order_id_fees else None, \
               service_order_id_products[0], trust_payment_id

    @staticmethod
    def get_multiple_promocode_payment_ids_by_composite_tag(composite_tag):
        with reporter.step(u'Получаем ids промокодных платежей из траста по composite_tag: {}'.format(composite_tag)):
            query = "SELECT TRUST_PAYMENT_ID FROM T_PAYMENT " \
                    "WHERE COMPOSITE_PAYMENT_ID = :composite_tag AND PAYMENT_METHOD = 'new_promocode'"
            params = {'composite_tag': composite_tag}

            result = db.balance_bs().execute(query, params)
            promocode_trust_payment_ids = [row['trust_payment_id'] for row in result]

            for trust_payment_id in promocode_trust_payment_ids:
                SimpleApi.wait_for_payment(trust_payment_id)

            promocode_payment_ids = [SimpleApi.get_payment_by_trust_payment_id(trust_payment_id)['id']
                                     for trust_payment_id in promocode_trust_payment_ids]

            reporter.attach(u'IDS промокодных платежей', utils.Presenter.pretty(promocode_payment_ids))
            reporter.attach(u'IDS промокодных платежей в трасте', utils.Presenter.pretty(promocode_trust_payment_ids))

            return promocode_payment_ids, promocode_trust_payment_ids

    @staticmethod
    def get_promocode_payment_ids_by_composite_tag(composite_tag):
        promocode_payment_ids, promocode_trust_payment_ids = \
            SimpleApi.get_multiple_promocode_payment_ids_by_composite_tag(composite_tag)

        return promocode_payment_ids[0], promocode_trust_payment_ids[0]

    @staticmethod
    def get_composite_tag_and_payment_method(trust_payment_id):
        with reporter.step(u'Получаем способ оплаты и composite_tag платежа из траста: {}'.format(trust_payment_id)):
            query = 'SELECT PAYMENT_METHOD, COMPOSITE_TAG FROM T_CCARD_BOUND_PAYMENT WHERE TRUST_PAYMENT_ID = :trust_payment_id'
            params = {'trust_payment_id': trust_payment_id}

            result = db.balance().execute(query, params)[0]
            composite_tag = result['composite_tag']
            payment_method = result['payment_method']

            reporter.attach(u'Composite tag', utils.Presenter.pretty(composite_tag))
            reporter.attach(u'Способ оплаты', utils.Presenter.pretty(payment_method))

            return composite_tag, payment_method

    @staticmethod
    @utils.CheckMode.result_matches(not_none())
    def wait_for_composite_tag(trust_payment_id):
        with reporter.step(u'Получаем composite_tag платежа из траста: {}'.format(trust_payment_id)):
            composite_tag, _ = utils.wait_until(
                    lambda: SimpleApi.get_composite_tag_and_payment_method(trust_payment_id),
                    has_item(any_of(EventsTickets.PAYMENT_TYPE_CARD_WEB, EventsTickets.PAYMENT_TYPE)))

            reporter.attach(u'Composite tag', utils.Presenter.pretty(composite_tag))

            return composite_tag

    @staticmethod
    def get_promocode_payment_ids(trust_payment_id):
        with reporter.step(u'Получаем промокодные платежи по основному платежу из траста: {}'.format(trust_payment_id)):
            composite_tag = SimpleApi.wait_for_composite_tag(trust_payment_id)
            return SimpleApi.get_promocode_payment_ids_by_composite_tag(composite_tag)

    @staticmethod
    def get_service_order_id_by_trust_payment_id(trust_payment_id):
        with reporter.step(u"Получаем service_order_id для trust_payment_id: {}".format(trust_payment_id)):
            query = "SELECT T_ORDER.SERVICE_ORDER_ID " \
                    "FROM T_ORDER JOIN T_PAYMENT_ORDER ON T_ORDER.ID=T_PAYMENT_ORDER.ORDER_ID " \
                    "JOIN T_PAYMENT ON T_PAYMENT.ID=T_PAYMENT_ORDER.PAYMENT_ID " \
                    "WHERE T_PAYMENT.TRUST_PAYMENT_ID=:trust_payment_id"
            params = {'trust_payment_id': trust_payment_id}

            service_order_id = db.balance_bs().execute(query, params)[0]['service_order_id']
            reporter.attach(u"service_order_id", utils.Presenter.pretty(service_order_id))

            return service_order_id

    @staticmethod
    def postauthorize(service, trust_payment_id, service_order_id_list, user=simpleapi_defaults.DEFAULT_USER,
                      amounts=None, actions=None, paymethod_markup=None):
        if amounts or paymethod_markup:
            orders = simpleapi_defaults.create_update_orders(service_order_id_list, amount_list=amounts)
            simpleapi_steps.update_basket(service, orders, trust_payment_id, user=user,
                                          paymethod_markup=paymethod_markup)

        orders = simpleapi_defaults.create_update_orders(service_order_id_list, action_list=actions)
        basket = simpleapi_steps.process_postauthorize(service, user, trust_payment_id, orders_for_update=orders)

        SimpleApi.wait_for_payment(trust_payment_id)
        balance_steps.wait_until_postauthorize(trust_payment_id=trust_payment_id)
        return basket

    @staticmethod
    def get_balance_service_product_id(trust_service_product_id, service_id=None):
        with reporter.step(u"Получаем ID сервисного продукта в balance: {}".format(trust_service_product_id)):
            query = "SELECT ID FROM T_SERVICE_PRODUCT WHERE EXTERNAL_ID=:service_product_id"
            if service_id:
                query += " AND SERVICE_ID=:service_id"
            params = {'service_product_id': trust_service_product_id, 'service_id': service_id}
            return int(db.balance().execute(query, params)[0]['id'])

    @staticmethod
    def get_trust_service_product_id(balance_service_product_id):
        with reporter.step(u"Получаем ID сервисного продукта в trust: {}".format(balance_service_product_id)):
            query = "SELECT EXTERNAL_ID FROM T_SERVICE_PRODUCT WHERE ID=:service_product_id"
            params = {'service_product_id': balance_service_product_id}
            return db.balance().execute(query, params)[0]['external_id']

    @staticmethod
    def find_refunds_by_orig_payment_id(orig_payment_id):
        query = "SELECT r.id, r.trust_refund_id FROM t_refund r " \
                "JOIN bo.t_payment p on p.id = r.id and p.paysys_code != 'REFUND_GROUP' " \
                "WHERE r.orig_payment_id=:orig_payment_id"
        params = {'orig_payment_id': orig_payment_id}
        return db.balance().execute(query, params)

    @staticmethod
    def find_refund_by_orig_payment_id(orig_payment_id, strict=True):
        with reporter.step(u"Получаем payment_id рефанда для платежа: {}".format(orig_payment_id)):
            res = SimpleApi.find_refunds_by_orig_payment_id(orig_payment_id)
            if not res:
                if not strict:
                    return None
                raise ValueError('Not found refunds for orig_payment_id: "{}"'.format(orig_payment_id))
            return res[0]['id'], res[0]['trust_refund_id']

    @staticmethod
    def find_reversal_by_orig_payment_id(orig_payment_id):
        with reporter.step(u"Получаем payment_id рефанда для платежа: {}".format(orig_payment_id)):
            query = "SELECT r.id, r.trust_refund_id FROM t_refund r" \
                    " JOIN bo.t_payment p on p.id = r.id AND p.paysys_code != 'REFUND_GROUP'" \
                        " WHERE r.orig_payment_id=:orig_payment_id AND is_reversal IS NOT NULL"
            params = {'orig_payment_id': orig_payment_id}
            return db.balance().execute(query, params)


class SimpleNewApi(object):
    @staticmethod
    def create_partner(service):
        return payments_api_steps.Partners.create(service, user=simpleapi_defaults.USER_ANONYMOUS)['partner_id']

    @staticmethod
    def create_product(service, partner_id, user=simpleapi_defaults.USER_NEW_API, fiscal_nds=CMNds.NDS_NONE,
                       fiscal_title='test_fiscal_title', service_fee=None):
        product_id = simpleapi_steps.get_service_product_id(service)
        fiscal_nds_str = str(fiscal_nds.name) if fiscal_nds else None
        payments_api_steps.Products.create(service, user, product_id=product_id,
                                           partner_id=partner_id, fiscal_nds=fiscal_nds_str, fiscal_title=fiscal_title,
                                           service_fee=service_fee)
        return product_id

    @staticmethod
    def create_partner_with_product(service, user=simpleapi_defaults.USER_NEW_API, fiscal_nds=CMNds.NDS_NONE):
        partner_id = SimpleNewApi.create_partner(service)
        product_id = SimpleNewApi.create_product(service, partner_id, user, fiscal_nds)
        return partner_id, product_id

    @staticmethod
    def create_orders_for_payment(service, product_id=None, user=None,
                                  orders_structure=simpleapi_simpleapi_defaults.Order.structure_rub_one_order,
                                  commission_category=None, amount=simpleapi_defaults.DEFAULT_PRICE,
                                  fiscal_nds=simpleapi_simpleapi_defaults.Fiscal.NDS.nds_none):
        orders = SimpleNewApi.create_multiple_orders_for_payment(service, [product_id], user, orders_structure,
                                                                 [commission_category], [amount], fiscal_nds=fiscal_nds)
        return orders

    @staticmethod
    def create_multiple_orders_for_payment(service, product_id_list=None, user=None,
                                           orders_structure=simpleapi_simpleapi_defaults.Order.structure_rub_two_orders,
                                           commission_category_list=None, amount_list=None,
                                           fiscal_nds=simpleapi_simpleapi_defaults.Fiscal.NDS.nds_none):

        if not user:
            user = simpleapi_defaults.USER_NEW_API

        if product_id_list is not None:
            for order_structure, product_id in zip(orders_structure, product_id_list):
                if product_id is not None:
                    order_structure.update({'product_id': product_id})

        if amount_list is None:
            amount_list = [simpleapi_defaults.DEFAULT_PRICE] * len(orders_structure)

        for order_structure, amount in zip(orders_structure, amount_list):
            if amount is not None:
                order_structure.update({'price': str(amount)})

        orders = payments_api_steps.Form.orders_for_payment(service=service, user=user,
                                                            orders_structure=orders_structure,
                                                            commission_category_list=commission_category_list,
                                                            fiscal_nds=fiscal_nds
                                                            )
        return orders

    # если передаем orders, то:
    #  - передавать product_id в payments не нужно (он уже есть в orders)
    #  - amount передаваемый в payments игнорируется и вместо него используется price из orders
    @staticmethod
    def create_payment(service, product_id=None, amount=None,
                       paymethod=None, paymethod_markup=None, user=None, orders=None,
                       wait_for_export_from_bs=True, need_clearing=True,
                       pass_params=None, currency=Currencies.RUB.iso_code, discounts=None,
                       fiscal_nds=None, fiscal_title=None, developer_payload=None,
                       ignore_missing_trust_payment_id=False):
        # выбираем способ оплаты
        if not paymethod:
            paymethod = LinkedCard(card=simpleapi_defaults.STATIC_EMULATOR_CARD,
                                   list_payment_methods_callback=payments_api_steps.PaymentMethods.get)

        if not user:
            user = simpleapi_defaults.USER_NEW_API

        if amount is None:
            amount = simpleapi_defaults.DEFAULT_PRICE

        fiscal_nds_str = str(fiscal_nds.name) if fiscal_nds else None
        basket = payments_api_steps.Payments.process(service, paymethod, user=user, product_id=product_id,
                                                     amount=str(amount), need_clearing=need_clearing,
                                                     paymethod_markup=paymethod_markup,
                                                     orders=orders, pass_params=pass_params, currency=currency,
                                                     discounts=discounts,
                                                     fiscal_nds=fiscal_nds_str, fiscal_title=fiscal_title,
                                                     developer_payload=developer_payload)

        # получаем trust_payment_id
        try:
            # для такси и синего маркета либо не хватает грантов, используется отдельная бд
            # игнорируем ошибку тут и получаем trust_payment_id отдельно
            trust_payment_id = SimpleApi.get_trust_payment_id(basket['purchase_token'])
        except IndexError:
            if not ignore_missing_trust_payment_id:
                raise
            trust_payment_id = None

        if wait_for_export_from_bs and trust_payment_id:
            # ждем обработки платежа
            payment_id = SimpleApi.wait_for_payment(trust_payment_id)
        else:
            payment_id = None

        return trust_payment_id, payment_id, basket['purchase_token']

    @staticmethod
    def create_topup_payment(
            service, product_id=None, amount=simpleapi_defaults.DEFAULT_PRICE,
            paymethod=None, user=None,
            wait_for_export_from_bs=True,
            pass_params=None, currency=Currencies.RUB.iso_code,
            fiscal_nds=None, fiscal_title=None, developer_payload=None):
        user = user or simpleapi_defaults.USER_NEW_API

        fiscal_nds_str = str(fiscal_nds.name) if fiscal_nds else None
        basket = payments_api_steps.AccountTopupPayment.process(
            service, paymethod, user=user, product_id=product_id,
            amount=str(amount) if amount is not None else None,
            pass_params=pass_params, currency=currency,
            fiscal_nds=fiscal_nds_str, fiscal_title=fiscal_title,
            developer_payload=developer_payload)

        if wait_for_export_from_bs:
            # ждем обработки платежа
            payment_id, trust_payment_id = SimpleApi.wait_for_payment_by_purchase_token(basket['purchase_token'])
        else:
            payment_id, trust_payment_id = None, None

        return trust_payment_id, payment_id, basket['purchase_token']

    @staticmethod
    def clear_payment(service, purchase_token, user=None):
        if not user:
            user = simpleapi_defaults.USER_NEW_API

        payments_api_steps.Payments.process_clearing(service, user, purchase_token)

    @staticmethod
    def create_compensation(service, product_id=None, amount=None,
                            is_discount=False, paymethod_markup=None, user=None, orders=None,
                            wait_for_export_from_bs=True, need_clearing=True,
                            pass_params=None, currency=Currencies.RUB.iso_code, discounts=None):
        paymethod = CompensationDiscount() if is_discount else Compensation()
        return SimpleNewApi.create_payment(
            service, product_id, amount, paymethod, paymethod_markup,
            user, orders, wait_for_export_from_bs, need_clearing,
            pass_params, currency, discounts)

    @staticmethod
    def create_refund(service, purchase_token, user=simpleapi_defaults.USER_NEW_API,
                      orders=None, paymethod_markup=None):
        payments_api_steps.Refunds.process(service, user, purchase_token,
                                           orders_for_refund=orders,
                                           paymethod_markup=paymethod_markup)

        trust_refund_id = SimpleApi.get_trust_refund_id(purchase_token)
        refund_id = SimpleApi.wait_for_refund(trust_refund_id)
        return trust_refund_id, refund_id

    @staticmethod
    def create_account_refund(service, purchase_token, user=simpleapi_defaults.USER_NEW_API):
        payments_api_steps.AccountRefund.process(service, user, purchase_token)

        trust_refund_id = SimpleApi.get_trust_refund_id(purchase_token)
        refund_id = SimpleApi.wait_for_refund(trust_refund_id)
        return trust_refund_id, refund_id

    @staticmethod
    def resize(service, purchase_token, orders, amount, user=None, paymethod_markup=None):
        SimpleNewApi.resize_multiple_orders(service, purchase_token, orders, [amount], user,
                                            paymethod_markup=paymethod_markup)

    @staticmethod
    def resize_multiple_orders(service, purchase_token, orders, amount_list, user=None, paymethod_markup=None,
                               qty_list=None):
        if not user:
            user = simpleapi_defaults.USER_NEW_API

        if qty_list is None:
            qty_list = [1 for _ in amount_list]

        for order, amount, qty in zip(orders, amount_list, qty_list):
            # при ресайзе передаем из общего paymethod_markup - разметку только для текущего заказа
            order_paymethod_markup = paymethod_markup.get(order['order_id']) if paymethod_markup else None
            payments_api_steps.Payments.Order.resize(
                service, user, purchase_token, order['order_id'], amount,
                qty=qty, paymethod_markup={order['order_id']: order_paymethod_markup})

    @staticmethod
    def unhold_payment(service, purchase_token, user=None):
        if not user:
            user = simpleapi_defaults.USER_NEW_API

        payments_api_steps.Payments.unhold(service, user, purchase_token)
        payments_api_steps.Wait.until_payment_cancelled(service, user, purchase_token)
