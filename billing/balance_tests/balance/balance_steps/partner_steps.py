# coding=utf-8
__author__ = 'igogor'

import collections
import datetime
import json
import random
from dateutil import relativedelta
from decimal import Decimal

import balance.balance_api as api
import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
import btestlib.config as balance_config
import client_steps
import person_steps
from balance.distribution.distribution_types import DistributionType
from balance.utils import get_config_item
from btestlib.constants import Currencies, Services, Products, PlaceType, \
    TransactionType, Export, OrderIDLowerBounds, CURRENCY_NUM_CODE_ISO_MAP
from btestlib.data import defaults
from btestlib.data.defaults import Distribution, Partner, AVIA_PRODUCT_IDS
from common_steps import CommonSteps
from export_steps import ExportSteps


log_align = 30

to_iso = utils.Date.date_to_iso_format
NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
A_DAY_AFTER_TOMORROW = NOW + datetime.timedelta(days=2)


Contract = collections.namedtuple('Contract', ['client_id', 'person_id', 'contract_id'])


class BalanceJSONEncoder(json.JSONEncoder):
    balance_type_map = {
        Decimal: lambda x: '{}'.format(
            Decimal(0) + x.normalize()),
        datetime.datetime: lambda x: x.isoformat()
    }

    def default(self, obj):
        return self.balance_type_map.get(
            type(obj),
            lambda x: json.JSONEncoder.default(self, x))(obj)


class PartnerSteps(object):
    @staticmethod
    def create_partner_client_person():
        client_id = client_steps.ClientSteps.create({'IS_AGENCY': 0, 'NAME': u'Test AG RSYA'})
        person_id = person_steps.PersonSteps.create(client_id, 'ur', {'is-partner': '1'})
        return client_id, person_id

    @staticmethod
    def create_or_update_partner(passport_uid, params):
        return api.medium().CreateOrUpdatePartner(passport_uid, params)

    @staticmethod
    def create_partner_place(client_id, internal_type=Distribution.PLACE_INTERNAL_TYPE, place_type=PlaceType.RSYA,
                             tag_id=None, product_list=None, search_id=None, url='pytest.com'):
        place_id = db.balance().execute("SELECT s_test_place_id.nextval place FROM dual")[0]['place']
        api.medium().CreateOrUpdatePlace(defaults.PASSPORT_UID, utils.remove_empty({
            'ID': place_id,
            'ClientID': client_id,
            'Type': place_type,
            'URL': url,
            'InternalType': internal_type,
            'ProductList': product_list,
            'SearchID': search_id,
            'TagID': tag_id
        }))

        return place_id

    @staticmethod
    def create_direct_partner_completion(place_id, completion_dt, bucks=Partner.BUCKS,
                                         page_id=DistributionType.DIRECT.page_id, clicks=42):
        with reporter.step(u"Добавляем открутки для директа"):
            query = "INSERT INTO t_partner_completion_buffer (PLACE_ID,PAGE_ID,DT,TYPE,SHOWS,CLICKS,BUCKS,MBUCKS,COMPLETION_TYPE,HITS,SOURCE_ID,TEXT,OBJECT_TYPE,UNITS,MONEY) " \
                    "VALUES (:place_id,:page_id,:completion_dt,'0','12955',:clicks,:bucks,:mbucks,'1','5702','1',NULL,NULL,NULL,NULL)"
            query_params = {'place_id': place_id,
                            'page_id': page_id, 'completion_dt': completion_dt,
                            'bucks': bucks, 'mbucks': bucks * DistributionType.DIRECT.units_type_rate,
                            'clicks': clicks}
            db.balance().execute(query, query_params)

    @staticmethod
    def create_dsp_partner_completions(completion_dt, place_id=Partner.PLACE_ID,
                                       partner_reward=Partner.PARTNER_REWARD_DSP,
                                       dsp_charge=Partner.DSP_CHARGE, dsp_id=Partner.DSP_ID, update_params=None):
        with reporter.step(u"Добавляем статистику по DSP"):
            query = "INSERT INTO t_partner_dsp_stat (DT,PLACE_ID,BLOCK_ID,DSP_ID,HITS,DSP_CHARGE,PARTNER_REWARD,SHOWS,TOTAL_RESPONSE_COUNT,TOTAL_BID_SUM, DEAL_ID, YANDEX_PRICE, FAKE_PRICE, PARTNER_STAT_ID) " \
                    "VALUES (:completion_dt,:place_id,:block_id,:dsp_id,:hits,:dsp_charge,:partner_reward,:shows,:response_count,:bid_sum,:deal_id,:yandex_price,:fake_price,:partner_stat_id)"

            query_params = {'place_id': place_id,
                            'dsp_charge': dsp_charge, 'completion_dt': completion_dt,
                            'partner_reward': partner_reward, 'block_id': Partner.DSP_BLOCK_ID,
                            'dsp_id': dsp_id, 'hits': Partner.DSP_HITS,
                            'shows': Partner.DSP_SHOWS,
                            'response_count': Partner.DSP_TOTAL_RESPONSE_COUNT,
                            'bid_sum': Partner.DSP_TOTAL_BID_SUM,
                            'deal_id': Partner.DSP_DEAL_ID,
                            'yandex_price': Partner.DSP_YANDEX_PRICE,
                            'fake_price': Partner.DSP_FAKE_PRICE,
                            'partner_stat_id': Partner.PARTNER_STAT_ID}

            if update_params:
                query_params.update(update_params)

            db.balance().execute(query, query_params)

    @staticmethod
    def create_multiship_completion(contract_id, amount, dt=datetime.datetime.today(),
                                    is_correction=0, product_id=defaults.multiship()['DEFAULT_PRODUCT']):
        sql = "INSERT INTO T_PARTNER_MULTISHIP_STAT (ID,ENTITY_ID,CONTRACT_ID,DT,PRODUCT_ID,AMOUNT,IS_CORRECTION) " \
              "VALUES ('1','1',:contract_id,:dt,:product_id,:amount,:is_correction)"
        params = {'contract_id': contract_id, 'dt': dt, 'amount': amount,
                  'is_correction': is_correction, 'product_id': product_id}
        db.balance().execute(sql, params, descr='Вставляем открутки по договору для доставки',
                             fail_empty=True)

    @staticmethod
    def create_adfox_completion(contract_id, dt, product_id=defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                requests=defaults.ADFox.DEFAULT_REQUESTS,
                                shows=defaults.ADFox.DEFAULT_SHOWS, units=0, bill=1, price_dt=None):
        sql = "INSERT INTO t_partner_adfox_stat (DT,CONTRACT_ID,PRODUCT_ID,REQUESTS,SHOWS,UNITS,BILL, PRICE_DT) " \
              "VALUES (:dt,:contract_id,:product_id,:requests,:shows,:units,:bill, :price_dt)"
        params = {'dt': dt, 'contract_id': contract_id, 'product_id': product_id, 'requests': requests, 'shows': shows,
                  'units': units, 'bill': bill, 'price_dt': price_dt}
        db.balance().execute(sql, params, descr='Вставляем открутки по ADFox',
                             fail_empty=True)

    @staticmethod
    def create_autobus_completion(client_id, dt, price):
        transaction_id = str(datetime.datetime.today().strftime("%d%m%Y%H%M%S")) + str(random.randint(0, 100000000))

        sql = "INSERT INTO T_PARTNER_BUSES_STAT (DT,TRANSACTION_ID,CLIENT_ID,PRICE) " \
              "VALUES (:dt,:transaction_id,:client_id,:price)"
        params = {'dt': dt, 'client_id': client_id, 'price': price, 'transaction_id': transaction_id}
        db.balance().execute(sql, params, descr='Вставляем открутки по Автобусам',
                             fail_empty=True)

    @staticmethod
    def create_avia_completions(client_id, contract_id, contract_currency, nat_versions_list, lines=1,
                                dt=datetime.datetime.now(), initial_amount=Decimal('1')):
        amount = initial_amount
        nat_ver_to_amount = {}
        for nat_ver in sorted(nat_versions_list):
            for _ in range(lines):
                PartnerSteps.create_fake_product_completion(dt, product_id=AVIA_PRODUCT_IDS[nat_ver][contract_currency],
                                                            contract_id=contract_id, client_id=client_id, amount=amount,
                                                            service_id=Services.KUPIBILET.id, source_id=2)
            nat_ver_to_amount.update({nat_ver: amount * lines})
            amount += Decimal('1')
        return nat_ver_to_amount

    @staticmethod
    def get_dsp_stat(start_dt, end_dt, include_deals=None, include_partner_stat_id=None, place_id=None):
        with reporter.step(u"Получаем статистику по DSP за определенный период"):
            csv_data = api.medium().GetDspStat(start_dt, end_dt, include_deals, include_partner_stat_id, 0,
                                               utils.remove_empty({'place_id': place_id}))
            dsp_stats = utils.csv_data_to_dict_list(csv_data, '\t')

            # a-vasin: округляем дробные значения
            for row in dsp_stats:
                utils.round_dict_string_fields(row, ['DSP', 'DSPWITHOUTNDS', 'TOTAL_BID_SUM',
                                                     'PARTNER', 'PARTNERWITHOUTNDS'])

            return dsp_stats

    @staticmethod
    def get_dsp_stat_by_page_id(page_id, completion_dt, include_deals=None, include_partner_stat_id=None):
        with reporter.step(u"Получаем статистику по DSP за определенный период для page_id: {}".format(page_id)):
            dsp_stat = PartnerSteps.get_dsp_stat(completion_dt, completion_dt, include_deals, include_partner_stat_id,
                                                 page_id)[0]

            reporter.attach(u"Статистика по DSP", utils.Presenter.pretty(dsp_stat))

            return dsp_stat

    @staticmethod
    def get_new_appointment_id():
        with reporter.step(u'Ищем первый не занятый appointment_id'):
            query = "SELECT MAX(APPOINTMENT_ID) id FROM t_partner_health_stat"

            appointment_id = (db.balance().execute(query)[0]['id'] or 0) + 1
            reporter.attach(u'Appointment ID', utils.Presenter.pretty(appointment_id))

            return appointment_id

    @staticmethod
    def create_medicine_completion(client_id, dt, price):
        with reporter.step(u'Добавляем открутку Яндекс.Здоровья для клиента: {} за дату: {} с ценой: {}'
                                   .format(client_id, dt, price)):
            appointment_id = PartnerSteps.get_new_appointment_id()

            query = "INSERT INTO t_partner_health_stat(CLIENT_ID, APPOINTMENT_ID, PRICE, DT, SERVICE_TYPE)" \
                    "VALUES (:client_id,:appointment_id,:price, :dt, NULL)"
            params = {'client_id': client_id, 'appointment_id': appointment_id, 'price': price, 'dt': dt}

            db.balance().execute(query, params)

    @staticmethod
    def create_connect_completion(client_id, product_id, dt, qty):
        with reporter.step(u'Добавляем открутку Яндекс.Коннекта для клиента: {}'.format(client_id)):
            query = "INSERT INTO T_PARTNER_CONNECT_STAT(DT, PRODUCT_ID, CLIENT_ID, QTY)" \
                    "VALUES (:dt,:product_id,:client_id,:qty)"
            params = {'client_id': client_id, 'product_id': product_id, 'qty': qty, 'dt': dt}

            db.balance().execute(query, params)

    @staticmethod
    def create_entity_completions_row(product_id, entity_id, src_id, completion_sum, dt):
        # TODO добавить проверку, что в таблице нет записи для клиента на выбранную дату, если есть - менять значение
        # (чтоб было по бизнес-логике)
        with reporter.step(u'Добавляем открутку в T_ENTITY_COMPLETION для клиента с entity_id: {}'.format(entity_id)):
            query = "INSERT INTO T_ENTITY_COMPLETION (DT, PRODUCT_ID, ENTITY_ID, SRC_ID, VAL_NUM_1) " \
                    "VALUES (:dt, :product_id, :entity_id, :src_id, :completion_sum)"
            params = {'dt': dt,
                      'product_id': product_id,
                      'entity_id': entity_id,
                      'src_id': src_id,
                      'completion_sum': completion_sum}
            db.balance().execute(query, params)

    @staticmethod
    def check_client_tarification(client_id, page_id=None, key_num_2=None, key_num_3=None):
        with reporter.step(u'Проверяем, есть ли клиент {} в S_TARIFICATION_ENTITY'.format(client_id)):
            query = 'select id from T_TARIFICATION_ENTITY where key_num_1=:client_id'
            params = {'client_id': client_id}
            if page_id is not None:
                query += ' and product_id=:product_id'
                params.update({'product_id': page_id})
            if key_num_2 is not None:
                query += ' and key_num_2=:key_num_2'
                params.update({'key_num_2': key_num_2})
            if key_num_3 is not None:
                query += ' and key_num_3=:key_num_3'
                params.update({'key_num_3': key_num_3})
            entity_id = db.balance().execute(query, params)
            return entity_id

    @staticmethod
    def create_tarification_entity_row(client_id, page_id=11101, key_num_2=None, key_num_3=None):
        with reporter.step(u'Добавляем клиента {} в S_TARIFICATION_ENTITY'.format(client_id)):
            entity_id = db.balance().execute('SELECT S_TARIFICATION_ENTITY.nextval id FROM dual')[0]['id']
            sql_insert_completion = "INSERT INTO T_TARIFICATION_ENTITY (ID, PRODUCT_ID, KEY_NUM_1, KEY_NUM_2, KEY_NUM_3, " \
                                    "KEY_NUM_4, KEY_NUM_5, KEY_NUM_6) " \
                                    "VALUES (:entity_id, :page_id, :client_id, :key_num_2, :key_num_3, -1, -1, -1)"
            params = {'entity_id': entity_id,
                      'page_id': page_id,
                      'client_id': client_id,
                      'key_num_2': key_num_2 or -1,
                      'key_num_3': key_num_3 or -1}
            db.balance().execute(sql_insert_completion, params)
            return entity_id

    @staticmethod
    def get_spendable_contract_type_by_service_id(service_id):
        print service_id
        return defaults.SERVICE_ID_TO_CONTRACT_TYPE_SPENDABLE_MAP.get(int(service_id[0]))

    @staticmethod
    def get_partner_balance(service, contract_ids, balance_type=None):
        with reporter.step(u'Вызываем GetPartnerBalance для сервиса: {}, договоров: {}'
                                   .format(service.name, contract_ids)):
            if balance_type:
                return api.medium().GetPartnerBalance(service.id, contract_ids, balance_type)
            else:
                return api.medium().GetPartnerBalance(service.id, contract_ids)

    # id вида '4bf6f157-4957-495d-9deb-27daed510004', но только из цифр
    @staticmethod
    def create_cloud_project_uuid():
        with reporter.step(u"Создаем id для проекта cloud"):
            query = "SELECT S_CLOUD_PROJECT_TEST.nextval id FROM dual"
            project_id = db.balance().execute(query)[0]['id']
            project_id_str = str(project_id).zfill(32)
            return '{}-{}-{}-{}-{}'.format(project_id_str[:8], project_id_str[8:12], project_id_str[12:16],
                                           project_id_str[16:20], project_id_str[20:])
            # return project_id_str

    @staticmethod
    def create_cloud_completion(contract_id, dt, amount, product=Products.CLOUD):
        with reporter.step(u"Создаем открутку Cloud для договора: {}".format(contract_id)):
            query = "INSERT INTO T_PARTNER_CLOUD_STAT(DT, CONTRACT_ID, PRODUCT_ID, AMOUNT, PROJECT_UUID) " \
                    "VALUES (:dt, :contract_id, :product_id, :amount, :project_uuid)"
            params = {
                'contract_id': contract_id,
                'dt': dt,
                'product_id': product.id,
                'amount': amount,
                'project_uuid': None  # igogor: теперь всегда пустое BALANCE-29164
            }
            db.balance().execute(query, params)

    @staticmethod
    def create_fake_product_completion(dt, product_id=None, client_id=None, service_order_id=None,
                                       service_id=None, source_id=None, contract_id=None, qty=None, amount=None,
                                       currency=None, commission_sum=None, promocode_sum=None, type=None,
                                       transaction_dt=None, payment_type=None, transaction_type=None):
        mes = u"Добавляем открутки"
        if client_id:
            mes += u" для клиента {}".format(client_id)
        if contract_id:
            mes += u" по договору {}".format(contract_id)
        if service_id:
            mes += u" для сервиса {}".format(service_id)
        mes += u":"

        with reporter.step(mes):
            query = 'INSERT INTO T_PARTNER_PRODUCT_COMPLETION ' \
                    '(DT, PRODUCT_ID, CONTRACT_ID, CLIENT_ID, SERVICE_ORDER_ID, SERVICE_ID, SOURCE_ID, QTY, AMOUNT, CURRENCY_CHR,' \
                    'COMMISSION_SUM, PROMOCODE_SUM, TYPE, TRANSACTION_DT, PAYMENT_TYPE, TRANSACTION_TYPE)' \
                    'VALUES (:dt, :product_id, :contract_id, :client_id, :service_order_id, ' \
                    ':service_id, :source_id, :qty, :amount, :currency, :commission_sum, :promocode_sum, :type,' \
                    ' :transaction_dt, :payment_type, :transaction_type)'

            query_params = {'dt': dt,
                            'product_id': product_id,
                            'contract_id': contract_id,
                            'client_id': client_id,
                            'service_order_id': service_order_id,
                            'service_id': service_id,
                            'source_id': source_id,
                            'qty': qty,
                            'amount': amount,
                            'currency': currency,
                            'commission_sum': commission_sum,
                            'promocode_sum': promocode_sum,
                            'type': type,
                            'transaction_dt': transaction_dt,
                            'payment_type': payment_type,
                            'transaction_type': transaction_type}

            db.balance().execute(query, query_params, descr='Создадим фэйковые открутки')

    @staticmethod
    def create_fake_partner_stat_aggr_tlog_completion(dt, client_id, service_id, amount, type_, last_transaction_id,
                                                      nds=1, currency=Currencies.RUB.iso_code,
                                                      completion_src='fake_src', src_dt=datetime.datetime.now()):
        mes = u"Добавляем открутки"
        if client_id:
            mes += u" для клиента {}".format(client_id)
        if service_id:
            mes += u" для сервиса {}".format(service_id)
        mes += u":"

        with reporter.step(mes):
            query = 'INSERT INTO t_partner_stat_aggr_tlog ' \
                    '(completion_src, src_dt, dt, client_id, currency, service_id, amount, type, last_transaction_id, nds)' \
                    'VALUES (:completion_src, :src_dt, :dt, :client_id, :currency, :service_id, :amount, :type_, :last_transaction_id, :nds)'

            query_params = {
                'completion_src': completion_src,
                'src_dt': src_dt,
                'dt': dt,
                'client_id': client_id,
                'currency': currency,
                'service_id': service_id,
                'amount': amount,
                'type_': type_,
                'last_transaction_id': last_transaction_id,
                'nds': nds,
            }

            db.balance().execute(query, query_params, descr='Создадим фэйковые открутки')

    @staticmethod
    def create_sidepayment_transaction(client_id, dt, amount, payment_type, service_id,
                                       transaction_type=TransactionType.PAYMENT,
                                       payload=None, currency=Currencies.RUB, orig_transaction_id=None,
                                       extra_dt_0=None, paysys_type_cc=None, extra_str_0=None, extra_str_1=None,
                                       extra_str_2=None, transaction_id=None,
                                       extra_num_0=None, extra_num_1=None, extra_num_2=None, transaction_dt=None,
                                       orig_transaction_id_from_transaction_id=False):
        with reporter.step(u"Создаем SidePayment открутку для клиента: {}, за дату: {}, на сумму: {}, валюта: {}"
                                   .format(client_id, dt, amount, currency)):

            transaction_id = (transaction_id or
                              int(db.balance().execute('SELECT S_ZEN_TRANSACTIONS_TEST.nextval AS val FROM dual')[0]['val']))

            side_payment_id = int(db.balance().execute('SELECT s_partner_payment_stat_id.nextval as val FROM dual')[0]['val'])

            query = "INSERT INTO t_partner_payment_stat(PRICE, DT, TRANSACTION_DT, CLIENT_ID, TRANSACTION_ID, CURRENCY, PAYMENT_TYPE," \
                    "SERVICE_ID, TRANSACTION_TYPE, PAYLOAD, ID, ORIG_TRANSACTION_ID, EXTRA_DT_0, PAYSYS_TYPE_CC, EXTRA_STR_0, " \
                    "EXTRA_STR_1, EXTRA_STR_2, EXTRA_NUM_0, EXTRA_NUM_1, EXTRA_NUM_2) " \
                    "VALUES(:price, :dt, :transaction_dt, :client_id, :transaction_id, :currency, :payment_type, :service_id, " \
                    ":transaction_type, :payload, :id, :orig_transaction_id, :extra_dt_0, :paysys_type_cc, :extra_str_0, " \
                    ":extra_str_1, :extra_str_2, :extra_num_0, :extra_num_1, :extra_num_2)"
            params = {
                'price': amount,
                'dt': dt,
                'transaction_dt': transaction_dt or dt,
                'client_id': client_id,
                'transaction_id': transaction_id,
                'currency': currency.iso_code,
                'payment_type': payment_type,
                'service_id': service_id,
                'transaction_type': transaction_type.name,
                'payload': payload,
                'id': side_payment_id,
                'orig_transaction_id':
                    transaction_id if orig_transaction_id_from_transaction_id else orig_transaction_id,
                'extra_dt_0': extra_dt_0,
                'paysys_type_cc': paysys_type_cc,
                'extra_str_0': extra_str_0,
                'extra_str_1': extra_str_1,
                'extra_str_2': extra_str_2,
                'extra_num_0': extra_num_0,
                'extra_num_1': extra_num_1,
                'extra_num_2': extra_num_2,
            }
            db.balance().execute(query, params)

            return int(side_payment_id), int(transaction_id)

    @staticmethod
    def generate_subvention_transaction_log_ids(n=1):
        assert n >= 1
        return [int(row['transaction_id']) for row in db.balance().execute("""
            select bo.s_test_partner_payment_stat_tlog_subventions.nextval transaction_id
            from dual
            connect by level <= :n""", {'n': n})]

    @staticmethod
    def get_partner_payment_stat_with_export(service_ids, transaction_ids):
        assert transaction_ids and service_ids
        transaction_ids = ', '.join(map(str, transaction_ids))
        if isinstance(service_ids, int):
            service_ids = {service_ids, }
        service_ids = ', '.join(map(str, service_ids))
        res = db.balance().execute("""
            select pps.id, pps.price, pps.dt, pps.client_id, pps.transaction_id, pps.currency, pps.payment_type,
            pps.service_id, pps.transaction_type, pps.payload, pps.extra_str_0, pps.extra_dt_0, pps.orig_transaction_id,
            e.state export_state, pps.extra_str_1, pps.transaction_dt, pps.PAYSYS_TYPE_CC, pps.extra_num_0
            from bo.t_partner_payment_stat pps
                left join bo.t_export e on e.OBJECT_ID = pps.id
                    and e.classname = 'SidePayment'
                    and e.type = 'THIRDPARTY_TRANS'
            where pps.TRANSACTION_ID in ({transaction_ids})
                    and pps.service_id in ({service_ids})
        """.format(service_ids=service_ids, transaction_ids=transaction_ids))
        for row in res:
            row['price'] = Decimal(row['price'])
        return res


class CommonPartnerSteps(object):
    @staticmethod
    def acts_enqueue(month, contract_ids):
        api.test_balance().PartnerActsEnqueue({'contract_ids': contract_ids, 'month': month})

    @staticmethod
    def get_partner_act_data_by_contract_id(contract_id):
        with reporter.step(u"Получаем инфомацию по партнерским актам по договору: {}".format(contract_id)):
            query = '''
                SELECT
                    partner_contract_id, page_id, owner_id, tag_id, description, partner_reward,
                    act_reward, place_id, clicks, shows, bucks, hits, dt, nds, type_id,
                    round(turnover, 5) AS turnover, product_price,
                    round(partner_reward_wo_nds,5) AS partner_reward_wo_nds,
                    round(act_reward_wo_nds,5) AS act_reward_wo_nds, end_dt,
                    currency, place_type, iso_currency, product_id,
                    round(ref_partner_reward_wo_nds,5) AS ref_partner_reward_wo_nds, reference_currency
                FROM
                  bo.t_partner_act_data
                WHERE
                  PARTNER_CONTRACT_ID = :contract_id
                ORDER BY
                  end_dt, page_id
            '''

            params = {'contract_id': contract_id}
            data = db.balance().execute(query, params,
                                        descr='Ищем данные в t_partner_act_data по договору',
                                        fail_empty=False)
            return data

    @staticmethod
    def generate_partner_acts(month, contract_id):
        CommonPartnerSteps.acts_enqueue(month, [contract_id])
        CommonSteps.restart_pycron_task('generate-partner-acts')
        sql = "SELECT state AS val FROM t_export WHERE type = 'PARTNER_ACTS' AND object_id = :contract_id"
        sql_params = {'contract_id': contract_id}
        CommonSteps.wait_for(sql, sql_params, value=1)

    @staticmethod
    def generate_partner_acts_fair(contract_id, dt):
        with reporter.step(u"Генерируем партнерские акты для договора: {} за дату: {}".format(contract_id, dt)):
            date_executed = datetime.datetime.now()
            api.test_balance().GeneratePartnerAct(contract_id, dt)

        if balance_config.TRUST_ME_I_AM_OEBS_QA:
            invoices = db.balance().execute("select id, person_id from t_invoice where contract_id = {} "
                                            "and type not in ('charge_note', 'fictive')".format(contract_id))
            for invoice in invoices:
                ExportSteps.extended_oebs_invoice_export(invoice['id'], invoice['person_id'])
        return date_executed

    @staticmethod
    def generate_plus_acts_fair(dt):
        with reporter.step(u"Генерируем акты Плюс 2.0 за дату: {}".format(dt)):
            date_executed = datetime.datetime.now()
            api.test_balance().GeneratePlusActs(dt)
        return date_executed

    @staticmethod
    def generate_interim_partner_act(contract_id, service_id, date=None,
                                     service_orders_ids=None, passport_uid=defaults.PASSPORT_UID):
        with reporter.step(u'Запускаем промежуточную генерацию партнерских актов по контракту {0} по сервису {1}'
                           u' на дату {2} по сервисным заказам {3}'.format(contract_id, service_id, date or u'сегодня',
                                                                           service_orders_ids)):
            date = date or datetime.datetime.now()
            params = {
                'ContractID': contract_id,
                'ServiceID': service_id,
                'ActDT': date,
            }
            if service_orders_ids:
                params['ServiceOrdersIDS'] = service_orders_ids

            acts = api.medium().GenerateInterimPartnerAct(passport_uid, params)

        return acts

    @staticmethod
    def _oebs_export(contract_id):
        if balance_config.TRUST_ME_I_AM_OEBS_QA:
            with reporter.step(u'Пробуем экспортировать акты по договору id: {contract_id} в OEBS'.format(
                    contract_id=contract_id)):
                from btestlib.utils import TestsError
                acts_sql = "SELECT t_act.id, t_act.external_id, t_act.dt, t_act.type " \
                           "FROM t_act, t_invoice " \
                           "WHERE t_invoice.contract_id = :contract_id AND t_act.invoice_id = t_invoice.id"
                params = {'contract_id': contract_id}
                acts_data = db.balance().execute(acts_sql, params)
                reporter.step(u'По договору найдено актов: {}'.format(len(acts_data)))
                for act in sorted(acts_data, key=lambda ad: ad['dt']):
                    try:
                        ExportSteps.export_oebs(act_id=act['id'])
                    except TestsError as te:
                        if te.message == 'Error for export check: Ошибка создания акта : ' \
                                         'ORA-20000: ORA-20000: Ошибка: Попытка создания акта не в открытом периоде!':
                            reporter.step(u'Акт {external_id} (id: {id}), дата: {dt} в закрытом периоде, '
                                          u'экспорт не прошел.'.format(**act))
                            query = "update bo.t_export " \
                                    "set state=1 " \
                                    "where classname='Act' " \
                                    "and type in ('OEBS', 'OEBS_API') " \
                                    "and object_id=:object_id"
                            params = {"object_id": act['id']}
                            db.balance().execute(query, params)
                        else:
                            raise

    @staticmethod
    def _read_export(queue, classname, object_id):
        query = """
            SELECT update_dt, export_dt, enqueue_dt
            FROM t_export
            WHERE classname = :classname
            AND object_id = :object_id
            AND type = :type
        """
        params = {
            'classname': Export.Classname.CLIENT,
            'type': queue,
            'object_id': object_id,
        }
        with reporter.step(
                u'Читаем состояние очереди экспорта {queue} для classname={classname} object_id={object_id}'.format(
                    queue=queue,
                    classname=classname,
                    object_id=object_id,
                ),
        ):
            return list(db.balance().execute(query, params))

    @staticmethod
    def generate_partner_acts_fair_and_export(
            partner_id,
            contract_id,
            dt,
            manual_export=True,
    ):
        expecting_export_object = manual_export
        expected_queue = Export.Type.MONTH_PROC
        with reporter.step(
                u'Генерируем акт и проверяем, что Клиент {id} {adverb} проставился в экспорт в {queue}'.format(
                    id=partner_id,
                    queue=expected_queue,
                    adverb=u'' if expecting_export_object else u'не'
                ),
        ):

            # Так как в очереди уже может существовать строка экспорта для клиента,
            # то вместо проверки наличия/отсутствия клиента в очереди
            # будем проверять изменения очереди (добавление или изменение строки)

            before = CommonPartnerSteps._read_export(queue=expected_queue,
                                                     classname=Export.Classname.CLIENT,
                                                     object_id=partner_id)

            CommonPartnerSteps.generate_partner_acts_fair(contract_id, dt)

            after = CommonPartnerSteps._read_export(queue=expected_queue,
                                                    classname=Export.Classname.CLIENT,
                                                    object_id=partner_id)

            is_exported = before != after

            assert is_exported == expecting_export_object, \
                "Expected is_exported == {expected}. Found {result}".format(
                    queue=expected_queue,
                    expected=expecting_export_object,
                    result=is_exported,
                )

        # [d-manakovskiy] Код ниже опирается на то, что будет создан хоть какой-то объект экспорта
        # Раньше создавали фейковые экспорты, поэтому код выполнялся.
        # По BALANCE-30144 мы больше не создаем никаких фейковы объектов экспорта,
        # поэтому здесь проверяем, что мы на самом деле ничего не хотим в бд -- ничего не хотим делать
        if expecting_export_object:
            # зовем сами, чтобы не зависеть от статуса задачи разбора очереди (включена/выключена)
            CommonSteps.export(Export.Type.MONTH_PROC, Export.Classname.CLIENT, partner_id)
            CommonPartnerSteps._oebs_export(contract_id)

    @staticmethod
    def get_extprops_by_object_id(classname, object_id):
        query = "SELECT * FROM t_extprops WHERE classname = :classname AND object_id = :object_id"
        query_params = {'classname': classname, 'object_id': object_id}
        return db.balance().execute(query, query_params)

    @staticmethod
    def get_thirdparty_transaction_by_payment_id(payment_id, transaction_type=TransactionType.PAYMENT, source='payment',
                                                 **filters):
        filters['payment_id'] = payment_id
        if transaction_type:
            filters['transaction_type'] = transaction_type.name
        filters = {k: v for k, v in filters.items() if v is not None}

        query = "SELECT id, payment_id, trust_id, trust_payment_id, contract_id, person_id, " \
                "transaction_type, partner_id, service_id, currency, paysys_type_cc, payment_type, " \
                "round(amount, 6) AS amount, " \
                "round(yandex_reward, 5) AS yandex_reward, round(yandex_reward_wo_nds, 5) AS yandex_reward_wo_nds," \
                "partner_currency, " \
                "client_id, round(client_amount, 6) AS client_amount, round(amount_fee, 6) AS amount_fee, " \
                "internal, commission_currency, oebs_org_id, iso_currency, " \
                "partner_iso_currency, commission_iso_currency, invoice_eid, invoice_commission_sum, " \
                "row_paysys_commission_sum, paysys_partner_id, product_id, service_product_id, payout_ready_dt, " \
                "service_order_id_str, immutable " \
                "FROM t_thirdparty_transactions WHERE {filter_template}"\
            .format(filter_template=' and '.join('{key} = :{key}'.format(key=key) for key in filters.keys()))

        payment_data = db.balance().execute(query, filters)
        # В thirdparty проводятся как t_payment, так и sidepayments (t_partner_payment_stat и синонимы)
        # в поле payment_id таблицы thirdparty они записывают айдишник своей исходной таблицы,
        # поэтому payment_id в thirdparty - не уникальное поле.
        # Различаются эти строчки в thirdparty сдвигом id, см. balance.thirdparty_transaction.GetId в коде баланса.
        # !!!!ВНИМАЕНИЕ!!!! На тесте сиквенс bo.s_request_order_id должен быть такой же, как на проде:
        # оканчиваться на 9, инкремент 10
        if source == 'payment':
            payment_data = [row for row in payment_data if (int(row['id']) <= 1324836609 or int(row['id']) % 10 == 9)]
        elif source == 'sidepayment':
            payment_data = [row for row in payment_data if (int(row['id']) > 1324836609 and int(row['id']) % 10 == 0)]

        return payment_data

    @staticmethod
    def get_all_thirdparty_data_by_payment_id(payment_id, transaction_type=TransactionType.PAYMENT, trust_id=None):
        query = "SELECT * " \
                "FROM t_thirdparty_transactions WHERE payment_id = :payment_id"

        params = {'payment_id': payment_id}

        payment_data = db.balance().execute(query, params)

        if transaction_type:
            payment_data = [row for row in payment_data if row['transaction_type'] == transaction_type.name]

        if trust_id:
            payment_data = [row for row in payment_data if row['trust_id'] == trust_id]

        return payment_data

    @staticmethod
    def get_thirdparty_payment_by_id(id):
        # pelmeshka: не можем дописать id в параметр, так как он может превышать XML-RPC limits, дописываем его в запрос
        with reporter.step(u"Получаем данные по платежу из t_thirdparty_transactions"):
            query = "SELECT payment_id, trust_id, trust_payment_id, contract_id, person_id, " \
                "transaction_type, partner_id, service_id, currency, paysys_type_cc, payment_type, " \
                "round(amount, 6) AS amount, " \
                "round(yandex_reward, 5) AS yandex_reward, round(yandex_reward_wo_nds, 5) AS yandex_reward_wo_nds," \
                "partner_currency, " \
                "client_id, round(client_amount, 6) AS client_amount, round(amount_fee, 6) AS amount_fee, " \
                "internal, commission_currency, oebs_org_id, iso_currency, " \
                "partner_iso_currency, commission_iso_currency, invoice_eid, invoice_commission_sum, " \
                "row_paysys_commission_sum, paysys_partner_id, product_id, service_product_id, payout_ready_dt, " \
                "dt, transaction_dt, service_order_id_str " \
                "FROM t_thirdparty_transactions WHERE id = " + str(id)
            payment_data = db.balance().execute(query)
        return payment_data

    @staticmethod
    def get_synthetic_thirdparty_transaction_id_by_payment_id(payment_id):
        # pelmeshka: Разным сервисам соответствуют разные смещения:
        # 1 - SVO
        shift = 1
        with reporter.step(u"Получаем thirdparty_transaction.id по side_payment_id = {}".format(payment_id)):
            tpt_id = OrderIDLowerBounds.LOWER_BOUND_S_REQUEST_ORDER_ID + int(payment_id) * 10 + shift
            reporter.log(u"Полученный id = {}".format(tpt_id))
        return tpt_id

    @staticmethod
    def get_thirdparty_payment_by_sidepayment_id(side_payment_id):
        thirdparty_transaction_id = CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(side_payment_id)
        return CommonPartnerSteps.get_thirdparty_payment_by_id(thirdparty_transaction_id)

    @staticmethod
    def get_delivered_date(payment_id, table='t_payment', id='id'):
        query = 'SELECT payout_ready_dt FROM {} WHERE {}=:payment_id'.format(table, id)
        params = {'payment_id': payment_id}
        res = db.balance().execute(query, params)[0]['payout_ready_dt']
        return res

    @staticmethod
    def update_t_config_ya_partner(service, client_id):
        query = "UPDATE t_thirdparty_service SET force_partner_id = :client_id WHERE id = :service_id"
        params = {'client_id': client_id, 'service_id': service.id}
        db.balance().execute(query, params)

    @staticmethod
    def get_tech_ids(service, row_num=0):
        with reporter.step(u'Получаем тех данные для сервиса: {}'.format(service.id)):
            query = "SELECT CLIENT_ID, PERSON_ID, ID FROM T_CONTRACT2 WHERE CLIENT_ID = " \
                    "(SELECT FORCE_PARTNER_ID FROM T_THIRDPARTY_SERVICE WHERE id=:service_id)"
            params = {'service_id': service.id}
            result = db.balance().execute(query, params)[row_num]
            return result['client_id'], result['person_id'], result['id']

    @staticmethod
    def get_plus_client_id(service, plus_part_key):
        with reporter.step(u'Получаем клиента Плюса для сервиса: {}'.format(service.id)):
            from balance.balance_steps.other_steps import ConfigSteps
            config = ConfigSteps.get_plus_part_config(plus_part_key)
            client_id = config['service_clients'].get(str(service.id))
            if not client_id:
                raise ValueError('There is no client for service {} in PLUS_2_0_CONFIGURATION'.format(service.id))
            return client_id

    @staticmethod
    def get_active_plus_ids_by_service(service, plus_part_key, contract_type='GENERAL'):
        with reporter.step(u'Получаем тех данные для сервиса: {}'.format(service.id)):
            from balance.balance_steps.other_steps import ConfigSteps
            client_id = CommonPartnerSteps.get_plus_client_id(service, plus_part_key)
            config = ConfigSteps.get_plus_part_config(plus_part_key)
            possible_contract_ids = None
            if client_id == config['operator']['client']:
                possible_contract_ids = {config['operator']['general'], config['operator']['spendable']}
            return CommonPartnerSteps.get_active_plus_ids_by_client(client_id, contract_type,
                                                                    possible_contract_ids=possible_contract_ids)

    @staticmethod
    def get_active_plus_ids_by_client(client_id, contract_type='GENERAL', possible_contract_ids=None):
        query = "SELECT c.CLIENT_ID, c.PERSON_ID, c.ID " \
                "FROM T_CONTRACT2 c " \
                "JOIN T_CONTRACT_COLLATERAL cc ON c.ID=cc.CONTRACT2_ID " \
                "JOIN T_CONTRACT_ATTRIBUTES cs ON cc.ATTRIBUTE_BATCH_ID=cs.ATTRIBUTE_BATCH_ID AND cs.CODE='SERVICES' " \
                "WHERE c.TYPE = :contract_type " \
                "AND cc.IS_CANCELLED IS NULL " \
                "AND c.CLIENT_ID = :client_id " \
                "AND cs.KEY_NUM = :service_id AND cs.VALUE_NUM = 1"
        service_id = Services.PLUS_2_0_INCOME.id if contract_type == 'GENERAL' else Services.PLUS_2_0_EXPENDITURE.id
        result = db.balance().execute(query, {
            'client_id': client_id,
            'contract_type': contract_type,
            'service_id': service_id
        })

        if not result:
            raise ValueError('There is no contract for plus client {} with service {}'.format(client_id,
                                                                                              service_id))

        if possible_contract_ids:
            result = filter(lambda c: c['id'] in possible_contract_ids, result)
        result = result[0]
        return Contract(result['client_id'], result['person_id'], result['id'])

    @staticmethod
    def get_active_tech_ids(service, contract_type=None, currency=None, contract_currency=None):
        with reporter.step(u'Получаем тех данные для сервиса: {}'.format(service.id)):
            # query = "SELECT CLIENT_ID, PERSON_ID, ID FROM T_CONTRACT2 WHERE CLIENT_ID = " \
            #         "(SELECT FORCE_PARTNER_ID FROM T_THIRDPARTY_SERVICE WHERE id=:service_id)"

            force_partner_id = None
            if currency:
                currency_iso_code = CURRENCY_NUM_CODE_ISO_MAP[currency]
                force_partner_map = db.balance().execute(
                    "select force_partner_map from t_thirdparty_service where id = :service_id",
                    {'service_id': service.id})[0]['force_partner_map']
                if force_partner_map:
                    force_partner_map = json.loads(force_partner_map)
                    for client_id, filter in force_partner_map.items():
                        if filter['currency_iso_code'] == currency_iso_code:
                            force_partner_id = int(client_id)
                            break

            if contract_type is None and currency is None:

                query = "SELECT c.CLIENT_ID, c.PERSON_ID, c.ID, cs.KEY_NUM as service_id " \
                        "FROM T_CONTRACT2 c " \
                        "LEFT JOIN T_CONTRACT_COLLATERAL cc ON c.ID=cc.CONTRACT2_ID " \
                        "LEFT JOIN T_CONTRACT_ATTRIBUTES cs ON cc.attribute_batch_id=cs.attribute_batch_id AND cs.CODE='SERVICES' " \
                        "WHERE c.type = 'GENERAL' " \
                        "AND cc.is_cancelled IS NULL " \
                        "AND c.CLIENT_ID = " \
                        "NVL(:force_partner_id, (SELECT FORCE_PARTNER_ID FROM T_THIRDPARTY_SERVICE WHERE id=:service_id)) " \
                        "and cc.dt < sysdate " \
                        "and (cc.is_signed is not null or cc.is_faxed is not null)"

            else:
                contract_currency = contract_currency or currency
                query = "SELECT c.CLIENT_ID, c.PERSON_ID, c.ID, cs.KEY_NUM as service_id FROM T_CONTRACT2 c " \
                        "JOIN T_CONTRACT_COLLATERAL cc ON c.ID=cc.CONTRACT2_ID " \
                        "JOIN T_CONTRACT_ATTRIBUTES ca ON cc.attribute_batch_id=ca.attribute_batch_id AND ca.CODE='COMMISSION' " \
                        "JOIN T_CONTRACT_ATTRIBUTES ca2 ON cc.attribute_batch_id=ca2.attribute_batch_id AND ca2.CODE='CURRENCY' " \
                        "LEFT JOIN T_CONTRACT_ATTRIBUTES cs ON cc.attribute_batch_id=cs.attribute_batch_id AND cs.CODE='SERVICES' " \
                        "WHERE 1=1 " \
                        "{} " \
                        "{} " \
                        "AND c.type = 'GENERAL' " \
                        "AND cc.is_cancelled IS NULL " \
                        "AND c.CLIENT_ID = " \
                        "NVL(:force_partner_id, (SELECT FORCE_PARTNER_ID FROM T_THIRDPARTY_SERVICE WHERE id=:service_id)) " \
                        "and cc.dt < sysdate " \
                        "and (cc.is_signed is not null or cc.is_faxed is not null)"

                contract_type_filter = "AND ca.VALUE_NUM=:contract_type" if contract_type is not None else "AND 2=2"
                currency_filter = "AND ca2.VALUE_NUM=:currency" if contract_currency else "AND 3=3"
                query = query.format(contract_type_filter, currency_filter)

            params = {'service_id': service.id,
                      'contract_type': contract_type,
                      'currency': contract_currency,
                      'force_partner_id': force_partner_id,
                      }
            result = db.balance().execute(query, params)

            active_id = None

            if service.id in [c['service_id'] for c in result]:
                # если установлен service_id - дофильтруем по нему
                result = [c for c in result if c['service_id'] == service.id]

            for contract in result:
                query = "select * from mv_contract_signed_attr where contract_id =:contract_id and code='FINISH_DT'"
                finish_attr = db.balance().execute(query, {'contract_id': contract['id']})

                if finish_attr:
                    finish_attr = finish_attr[0]
                    finish_dt = finish_attr['value_dt']
                    if finish_dt is None or finish_dt > datetime.datetime.now():
                        active_id = contract['id']
                        client_id = contract['client_id']
                        person_id = contract['person_id']
                else:
                    active_id = contract['id']
                    client_id = contract['client_id']
                    person_id = contract['person_id']

            assert active_id is not None, 'No active tech contract available for technical client'

            return client_id, person_id, active_id

    @staticmethod
    def get_partner_reward(contract_id):
        query_get_partner_reward = "SELECT place_id, round(partner_reward_wo_nds,5) reward FROM t_partner_act_data WHERE partner_contract_id = :contract_id"
        params_invoice = {'contract_id': contract_id}
        return db.balance().execute(query_get_partner_reward, params_invoice)

    @staticmethod
    def get_data_from_agent_rep(contract_id):
        query = "SELECT service_id, act_id, act_qty, invoice_id, act_amount, dt, currency, contract_id " \
                "FROM v_rep_agent_rep WHERE CONTRACT_ID = :contract_id"
        params = {'contract_id': contract_id}
        return db.balance().execute(query, params, descr="Выбираем данные из v_rep_agent_rep")

    @staticmethod
    def export_payment(payment_id):
        with reporter.step(u'Запускаем обработку платежа с номером: {}'.format(payment_id)):
            export_res = CommonSteps.export('THIRDPARTY_TRANS', 'Payment', payment_id)

        # Для коллег из OEBS: публикуем ID объекта в отчёте, выгружаем объект в OEBS и публикуем лог выгрузки
        if balance_config.TRUST_ME_I_AM_OEBS_QA:
            ExportSteps.extended_oebs_payment_export(payment_id)
        return export_res

    @staticmethod
    def get_sidepayment_id(service_id, transaction_id):
        return db.balance().execute("""
            select id
            from bo.t_partner_payment_stat pps
            where TRANSACTION_ID = :transaction_id and SERVICE_ID = :service_id
        """, {'transaction_id': transaction_id, 'service_id': service_id})[0]['id']

    @staticmethod
    def get_sidepayments_by_service_and_dt(service_id, l_dt, r_dt):
        return db.balance().execute("""
                select id, transaction_type, client_id, service_id
                from bo.t_partner_payment_stat pps
                where SERVICE_ID = :service_id and TRANSACTION_DT >= :l_dt and TRANSACTION_DT < :r_dt
            """, {'service_id': service_id, 'l_dt': l_dt, 'r_dt': r_dt})

    @classmethod
    def export_sidepayment(cls, sidepayment_id=None, service_id=None, transaction_id=None):
        assert sidepayment_id is not None or (service_id is not None and transaction_id is not None)
        if not sidepayment_id:
            sidepayment_id = cls.get_sidepayment_id(service_id, transaction_id)
        with reporter.step(u'Запускаем обработку sidepayment с номером: {}'.format(sidepayment_id)):
            return CommonSteps.export('THIRDPARTY_TRANS', 'SidePayment', sidepayment_id)

    @staticmethod
    def set_payment_dt(payment_id, dt):
        with reporter.step(u"Устанавливаем для платежа: {} дату: {}".format(payment_id, dt)):
            query = "UPDATE T_PAYMENT SET DT=:dt, PAYMENT_DT=:dt, POSTAUTH_DT=:dt, RESP_DT=:dt WHERE ID=:payment_id"
            params = {
                'payment_id': payment_id,
                'dt': dt
            }
            db.balance().execute(query, params)

    @staticmethod
    def get_prefix_for_spendable_contract(service_id, firm_id, is_offer):
        query = "SELECT prefix FROM t_contract_prefix WHERE service_id = :service_id " \
                "AND is_offer = :is_offer AND firm_id = :firm_id AND contract_type = 'SPENDABLE'"
        params = {'service_id': service_id, 'is_offer': is_offer, 'firm_id': firm_id}
        prefix_data = db.balance().execute(query, params, descr='Находим префикс для расходного договора')
        if prefix_data:
            prefix = prefix_data[0]['prefix']
        elif not prefix_data and is_offer:
            prefix = 'ОФ'
        else:
            prefix = 'РАС'
        return prefix

    @staticmethod
    def get_contract_firm(contract_id):
        with reporter.step(u"Получаем OEBS_ORG_ID для договора: {}".format(contract_id)):
            query = """SELECT t_contract_attributes.VALUE_NUM firm_id
FROM t_contract2
         JOIN t_contract_collateral ON t_contract2.id = t_contract_collateral.contract2_id
         JOIN t_contract_attributes
              ON t_contract_attributes.attribute_batch_id = t_contract_collateral.attribute_batch_id AND
                 t_contract_attributes.code = 'FIRM'
WHERE t_contract2.id=:contract_id"""
            params = {'contract_id': contract_id}
            return db.balance().execute(query, params)[0]['firm_id']

    @staticmethod
    def get_service_product_id(product):
        with reporter.step(u"Получаем ID сервисного продукта по номенкалатурному с ID: {}".format(product.id)):
            query = "SELECT * FROM T_PARTNER_PRODUCT WHERE PRODUCT_ID=:product_id"
            params = {'product_id': product.id}
            return db.balance().execute(query, params)[0]['service_product_id']

    @staticmethod
    def set_product_mapping_config(service, config):
        with reporter.step(u"Обновляем конфиг для сервиса: {} значением: {}".format(service.id, config)):
            query = "UPDATE T_THIRDPARTY_SERVICE SET PRODUCT_MAPPING_CONFIG=:config WHERE ID=:service_id"
            params = {
                'service_id': service.id,
                'config': json.dumps(config) if config is not None else None
            }
            db.balance().execute(query, params)

    @staticmethod
    def get_product_mapping_config(service):
        with reporter.step(u"Получаем конфиг для сервиса: {}".format(service.id)):
            query = "SELECT PRODUCT_MAPPING_CONFIG FROM T_THIRDPARTY_SERVICE WHERE ID=:service_id"
            params = {
                'service_id': service.id
            }
            return json.loads(db.balance().execute(query, params)[0]['product_mapping_config'])

    @staticmethod
    def get_service_product(product):
        with reporter.step(u"Получаем ID сервисного продукта для продукта: {}".format(product.id)):
            query = "SELECT sp.EXTERNAL_ID FROM T_PARTNER_PRODUCT pp " \
                    "JOIN T_SERVICE_PRODUCT sp ON pp.SERVICE_PRODUCT_ID=sp.ID " \
                    "WHERE pp.PRODUCT_ID=:product_id"
            params = {'product_id': product.id}
            return db.balance().execute(query, params)[0]['external_id']

    @staticmethod
    def get_trust_payments(trust_payment_id):
        with reporter.step(u"Получаем платежи по trust_payment_id: {}".format(trust_payment_id)):
            query = "SELECT p.ID PAYMENT_ID, p.TRUST_PAYMENT_ID, t.PAYMENT_METHOD_ID, cp.PAYMENT_METHOD " \
                    "FROM T_PAYMENT p " \
                    "JOIN T_CCARD_BOUND_PAYMENT CP ON CP.ID = P.ID " \
                    "JOIN T_TERMINAL t on (t.ID = p.TERMINAL_ID) " \
                    "WHERE p.TRUST_PAYMENT_ID=:trust_payment_id"
            params = {'trust_payment_id': trust_payment_id}
            return db.balance().execute(query, params)

    @staticmethod
    def get_trust_refunds(trust_refund_id):
        with reporter.step(u"Получаем возвраты по trust_refund_id: {}".format(trust_refund_id)):
            query = "SELECT id, trust_refund_id FROM t_refund WHERE trust_refund_id = :trust_refund_id"
            params = {'trust_refund_id': trust_refund_id}
            return db.balance().execute(query, params)

    @staticmethod
    def get_children_trust_group_payments(parent_trust_payment_id):
        with reporter.step(u"Получаем дочерние платежи для группового платежа: {}".format(parent_trust_payment_id)):
            query = "SELECT p.ID PAYMENT_ID, p.TRUST_PAYMENT_ID, t.PAYMENT_METHOD_ID, cp.PAYMENT_METHOD " \
                    "FROM T_PAYMENT p " \
                    "JOIN T_CCARD_BOUND_PAYMENT CP ON CP.ID = P.ID " \
                    "JOIN T_TERMINAL t on (t.ID = p.TERMINAL_ID) " \
                    "WHERE p.TRUST_GROUP_ID=:parent_trust_payment_id"
            params = {'parent_trust_payment_id': parent_trust_payment_id}
            return db.balance().execute(query, params)

    @staticmethod
    def get_children_cashback_payments(group_payment_id):
        with reporter.step(u"Получаем дочерние кешбечные платежи для группового платежа: {}".format(group_payment_id)):
            query = "SELECT p.ID PAYMENT_ID, p.TRUST_PAYMENT_ID, p.PAYMENT_METHOD_ID FROM T_PAYMENT p " \
                    "WHERE p.cashback_parent_id=:group_payment_id"
            params = {'group_payment_id': group_payment_id}
            return db.balance().execute(query, params)

    @staticmethod
    def get_refunds_by_orig_payment_id(orig_payment_id):
        with reporter.step(u"Получаем рефанды для платежа id: {}".format(orig_payment_id)):
            query = "SELECT r.id, r.trust_refund_id " \
                    "FROM t_refund r " \
                    "JOIN bo.t_payment p on p.id = r.id and p.paysys_code != 'REFUND_GROUP' " \
                    "WHERE r.orig_payment_id=:orig_payment_id"
            params = {'orig_payment_id': orig_payment_id}
            return db.balance().execute(query, params)

    @staticmethod
    def get_refund_group_by_orig_payment_id(orig_payment_id):
        with reporter.step(u"Получаем рефанды для платежа id: {}".format(orig_payment_id)):
            query = "SELECT r.id, r.trust_refund_id " \
                    "FROM t_refund r " \
                    "JOIN bo.t_payment p on p.id = r.id and p.paysys_code = 'REFUND_GROUP' " \
                    "WHERE r.orig_payment_id=:orig_payment_id"
            params = {'orig_payment_id': orig_payment_id}
            return db.balance().execute(query, params)

    @staticmethod
    def create_partner_completions_resource(source_name, resource_dt, additional_params=None):

        additional_params = json.dumps(additional_params, cls=BalanceJSONEncoder)

        with reporter.step(u'Устанавливаем partner_completions_resource типа {0} за {1}'
                             .format(source_name, datetime.datetime.strftime(resource_dt, "%Y-%m-%d"))):
            query = """merge into bo.t_partner_completions_resource pcr
                        using (
                            select (
                                select id from bo.t_partner_completions_resource
                                where 1=1
                                    and source_name = :source_name
                                    and dt = :resource_dt
                                ) id from dual
                            ) req
                                on (req.id = pcr.id)
                        when matched then update
                            set pcr.additional_params = :additional_params
                        when not matched then insert (pcr.id, pcr.source_name, pcr.dt, pcr.additional_params)
                            values(bo.s_partner_comp_resource_id.nextval, :source_name, :resource_dt, :additional_params)"""
            params = {'source_name': source_name, 'resource_dt': resource_dt, 'additional_params': additional_params}

            db.balance().execute(query, params)

    @staticmethod
    def get_partner_completions_resource(source_name, resource_dt):
        with reporter.step(u'Получаем partner_completions_resource типа {0} за {1}'
                             .format(source_name, datetime.datetime.strftime(resource_dt, "%Y-%m-%d"))):
            query = """select id, source_name, dt, ADDITIONAL_PARAMS
                       from bo.t_partner_completions_resource
                       where source_name = :source_name
                        and dt = :resource_dt
            """
            params = {'source_name': source_name, 'resource_dt': resource_dt}
            res = db.balance().execute(query, params)[0]
            if res['additional_params']:
                res['additional_params'] = json.loads(res['additional_params'])
        return res

    @staticmethod
    def process_partners_completions(source_name, resource_dt):
        # from balance import balance_api as api
        api.test_balance().GetPartnerCompletions({'start_dt': resource_dt, 'end_dt': resource_dt,
                                                  'completion_source': source_name})

    @staticmethod
    def process_entity_completions(source_name, resource_dt):
        # from balance import balance_api as api
        api.test_balance().GetEntityCompletions({'start_dt': resource_dt, 'end_dt': resource_dt,
                                                 'completion_source': source_name})

    @staticmethod
    def get_order_service_order_id_str_by_transaction_id(transaction_id):
        with reporter.step(u"Получаем service_order_id_str для транзакции {}".format(transaction_id)):
            query = """
            select o.service_order_id_str
            from bo.t_order o
                join bo.t_thirdparty_transactions tt on o.id = tt.order_id
            where tt.id = :transaction_id
            """
            params = {'transaction_id': transaction_id}
            return db.balance().execute(query, params)[0]['service_order_id_str']

    @staticmethod
    def get_fake_trust_payment_id():
        return db.balance().execute("select S_TEST_TRUST_PAYMENT_ID.nextval as id from dual")[0]['id']

    @staticmethod
    def get_fake_food_transaction_id():
        return db.balance().execute("select S_TEST_FOOD_TRANSACTION_ID.nextval as id from dual")[0]['id']

    @staticmethod
    def get_partner_oebs_compls_migration_params(migration_alias):
        query = u"select value_json from bo.t_config where item='OEBS_PARTNER_COMPLETIONS_PARAMS'"
        res = db.balance().execute(query)#[0]['id']
        if res:
            migration_params = json.loads(res[0][u'value_json'])
            for _migration_alias, _params in migration_params.iteritems():
                if _params.get(u'migration_date'):
                    _params[u'migration_date'] = datetime.datetime.strptime(_params[u'migration_date'], '%Y-%m-%d')
            return migration_params.get(migration_alias)

    @staticmethod
    def get_taxi_payments_oebs_migration_dt():
        query = u"select value_dt from bo.t_config where item='TAXI_PAYMENTS_OEBS_COMPLETIONS_MIGRATION_DT'"
        res = db.balance().execute(query)
        return res[0]['value_dt'] if res else None

    @staticmethod
    def get_netting_config():
        query = u"select value_json from bo.t_config where item='NETTING_CONFIG'"
        res = db.balance().execute(query)
        conf = json.loads(res[0]['value_json']) if res else {}
        for _conf in conf:
            if _conf.get(u'stop_netting_after_date'):
                _conf[u'stop_netting_after_date'] = datetime.datetime.strptime(_conf[u'stop_netting_after_date'], '%Y-%m-%d')
        return conf

    @staticmethod
    def generate_dates_for_taxi_payments_oebs_migration():
        migration_dt, \
        month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
        month_migration_minus1_start_dt, month_migration_minus1_end_dt, \
        month_minus2_start_dt, month_minus2_end_dt, \
        month_minus1_start_dt, month_minus1_end_dt = [None] * 9
        migration_dt = CommonPartnerSteps.get_taxi_payments_oebs_migration_dt()
        if migration_dt:
            # 2 месяца до даты миграции
            month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
            month_migration_minus1_start_dt, month_migration_minus1_end_dt = \
                utils.Date.previous_two_months_dates(migration_dt)
            # 2 предыдуших месяца от текущего, если они больше даты миграции, либо 2 месяца вперед от даты миграции
            posible_oebs_compls_start_dt, _, _, _ = utils.Date.previous_two_months_dates()
            oebs_compls_start_dt = max(posible_oebs_compls_start_dt, migration_dt)
            month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = \
                utils.Date.previous_two_months_dates(oebs_compls_start_dt + relativedelta.relativedelta(months=2))
        else:
            month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
            month_migration_minus1_start_dt, month_migration_minus1_end_dt = \
                utils.Date.previous_two_months_dates()
        return migration_dt, \
            month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
            month_migration_minus1_start_dt, month_migration_minus1_end_dt, \
            month_minus2_start_dt, month_minus2_end_dt, \
            month_minus1_start_dt, month_minus1_end_dt

    @staticmethod
    def create_partner_oebs_compl(contract_id, client_id, **kwargs):
        # if value in default_kwargs is type, then param is necessary and method will check type of it
        # if value in default_kwargs is object, then param is not necessary and will be filled by value object if not passed
        default_kwargs = {
            'service_id': int,
            'last_transaction_id': None,
            'product_id': int,
            'amount_nds': None,
            'amount_wo_nds': None,
            'dt': datetime.datetime,
            'currency': basestring,
            'amount': Decimal,
            'transaction_dt': datetime.datetime,
            'accounting_period': None,
            'source_tabname': None,
        }
        compl_data = {'client_id': client_id,
                      'contract_id': contract_id}
        for key, value in default_kwargs.items():
            if type(value) is type:
                compl_data[key] = kwargs[key]
                assert isinstance(compl_data[key], value)
            else:
                compl_data[key] = kwargs.get(key, default_kwargs[key])

        with reporter.step(u'Создаем заказ partner_oebs_completions'):
            query = "INSERT INTO t_partner_oebs_completions\n" \
                    "(client_id, service_id, last_transaction_id, product_id, amount_nds, amount_wo_nds, " \
                    "contract_id, dt, currency, amount, transaction_dt, accounting_period, source_tabname)\n" \
                    "VALUES\n" \
                    "(:client_id, :service_id, :last_transaction_id, :product_id, :amount_nds, :amount_wo_nds, " \
                    ":contract_id, :dt, :currency, :amount, :transaction_dt, :accounting_period, :source_tabname)"

            query_params = compl_data
            db.balance().execute(query, query_params, descr='Добавляем открутки в t_partner_oebs_completions')

    @staticmethod
    def create_partner_oebs_completions(contract_id, client_id, compls_data):
        with reporter.step(u'Добавляем открутки partner_oebs_completions для договора: {}'.format(contract_id)):
            for compl_dict in compls_data:
                CommonPartnerSteps.create_partner_oebs_compl(contract_id, client_id, **compl_dict)

    @staticmethod
    def export_partner_fast_balance(contract_id):
        with reporter.step(u'Запускаем экспорт договора: {} из очереди: {}'.format(contract_id,
                                                                                   Export.Classname.CONTRACT)):
            CommonSteps.export(Export.Type.PARTNER_FAST_BALANCE, Export.Classname.CONTRACT, contract_id)

    @staticmethod
    def migrate_client(namespace, filter_, object_id, dt):
        db.balance().execute(
            "insert into bo.t_migration_new_billing values (:namespace, :filter, :object_id, :dt, 0)",
            {
                'namespace': namespace,
                'filter': filter_,
                'object_id': object_id,
                'dt': dt,
            }
        )

    @staticmethod
    def cancel_migrate_client(namespace, filter_, object_id):
        db.balance().execute(
            "delete from bo.t_migration_new_billing "
            "where namespace = :namespace and filter = :filter and object_id = :object_id",
            {
                'namespace': namespace,
                'filter': filter_,
                'object_id': object_id,
            }
        )

    @staticmethod
    def client_orders(client_id, service_id=None):
        with reporter.step(u'Получаем айдишники заказов у клиента {} с service_id = {}'
                                   .format(client_id, service_id if service_id else 'любой')):
            query = 'select id from T_ORDER where client_id=:client_id'
            params = {'client_id': client_id}
            if service_id is not None:
                query += ' and service_id=:service_id'
                params.update({'service_id': service_id})

            order_ids = db.balance().execute(query, params)
            return order_ids

    @staticmethod
    def get_invoice_cash_receipts(params):
        invoice_id = params.get('InvoiceID') or params.get('InvoiceEID')

        with reporter.step(u'Вызываем GetInvoiceCashReceipts для счета {}'.format(invoice_id)):
            return api.medium().GetInvoiceCashReceipts(params)
