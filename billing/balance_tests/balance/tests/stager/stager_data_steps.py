# coding: utf-8

__author__ = 'a-vasin'

from functools import partial, wraps
from datetime import datetime
from decimal import Decimal

from dateutil.relativedelta import relativedelta
import json

from balance import balance_steps as steps
import balance.balance_db as db
from btestlib import utils
from btestlib.constants import Currencies, NdsNew, Services, TaxiOrderType, CurrencyRateSource, BlueMarketOrderType, \
    BlueMarketingServicesOrderType
from btestlib.data.defaults import AVIA_PRODUCT_IDS, NatVer
from btestlib.data.partner_contexts import CLOUD_RU_CONTEXT, AVIA_RU_CONTEXT, HOSTING_SERVICE

STR_DT = '2018-06-24'
DT = datetime.strptime(STR_DT, '%Y-%m-%d')

WRONG_STR_DT = '2002-09-13'


def make_default_context(dt):
    return {
        'dt': dt,
        'str_dt': dt.strftime('%Y-%m-%d')
    }

# ------------------------------------------------------------------------------------------
# EDA

def make_eda_input(t_id, client_id, service_id, amount, currency, t_type, dt):
    return {
        'client_id': client_id,
        'commission_sum': '{:.2f}'.format(amount),
        'transaction_currency': currency.iso_code,
        'type': t_type,
        'service_id': service_id,

        # do not affect logic
        'total_sum': '{:.2f}'.format(amount),
        'dt': dt,
        'orig_transaction_id': t_id,
        'transaction_id': t_id,
        'promocode_sum': '0.00',
        'service_order_id': str(t_id),
        'utc_start_load_dttm': '2019-07-15 08:00:04'
    }


def make_eda_output(client_id, service_id, amount, currency, t_type, dt):
    return {
        u'client_id': client_id,
        u'commission_sum': amount,
        u'service_id': service_id,
        u'transaction_currency': currency.iso_code,
        u'type': t_type,
        u'dt': dt,
    }


# ------------------------------------------------------------------------------------------
# DRIVE

def make_drive_input(order_type, amount, currency, promo_amount):
    return {
        'type': order_type,
        'total_sum': '{:.2f}'.format(amount),
        'promocode_sum': '{:.2f}'.format(promo_amount),
        'transaction_currency': currency.iso_code,

        # do not affect logic
        'client_id': '592328561',
        'payment_type': 'card',
        'orig_transaction_id': 'b669c166-d0547e3c-7b22aa1-5c06ce3a',
        'service_order_id': 'b669c166-d0547e3c-7b22aa1-5c06ce3a',
        'commission_sum': 0,
        'use_discount': 0,
        'dt': '2019-07-31T00:25:40.000000Z',
        'transaction_id': 'b669c166-d0547e3c-7b22aa1-5c06ce3a'
    }


def make_drive_output(order_type, amount, currency, product):
    return {
        u'amount': amount,
        u'product_id': product.id,
        u'transaction_currency': currency.iso_code,
        u'type': order_type
    }


# ------------------------------------------------------------------------------------------
# CLOUD

def make_cloud_context(dt):
    project_uuid = steps.PartnerSteps.create_cloud_project_uuid()
    params = {
        'projects': [project_uuid],
        'start_dt': dt
    }
    client_id, _, contract_id, _ = steps.ContractSteps.create_partner_contract(CLOUD_RU_CONTEXT,
                                                                               additional_params=params)

    return {
        'project_id': project_uuid,
        'client_id': client_id,
        'contract_id': contract_id
    }


def make_cloud_input_event(product, project_id, amount, str_dt):
    return {
        'amount': str(amount),  # '45.797485324',
        'product_id': str(product.id),
        'project_id': project_id,  # 'dn200sdtluta9ocnv4ls'
        'date': str_dt,
    }


def make_cloud_input_marketplace(client_id, date, amount):
    return {
        'publisher_balance_client_id': str(client_id),
        'date': date,
        'total': '{:.2f}'.format(amount)
    }


def make_cloud_output_completion(product, contract_id, project_id, amount):
    return {
        u'amount': amount,  # u'63.686374',
        u'contract_id': contract_id,
        u'product_id': product.id,
        u'project_id': project_id
    }


def make_cloud_output_marketplace(client_id, amount):
    return {
        u'client_id': client_id,
        u'total': amount
    }


def make_cloud_output_error(project_id, date):
    return {
        u'error': u'Unable to locate project "{}" in timeline: start_dt="{}", finish_dt="{}"'.format(
            project_id, date, date)
    }


# ------------------------------------------------------------------------------------------
# HOSTING_SERVICE

def make_hosting_service_context(dt):
    project_uuid = steps.PartnerSteps.create_cloud_project_uuid()
    params = {
        'projects': [project_uuid],
        'start_dt': dt
    }
    client_id, _, contract_id, _ = steps.ContractSteps.create_partner_contract(HOSTING_SERVICE,
                                                                               additional_params=params)

    return {
        'project_id': project_uuid,
        'client_id': client_id,
        'contract_id': contract_id
    }


def make_hosting_service_input_event(product, project_id, amount, str_dt):
    return {
        'amount': str(amount),  # '45.797485324',
        'product_id': str(product.id),
        'project_id': project_id,  # 'dn200sdtluta9ocnv4ls'
        'date': str_dt,
    }


def make_hosting_service_output_completion(product, contract_id, project_id, amount):
    return {
        u'amount': amount,  # u'63.686374',
        u'contract_id': contract_id,
        u'product_id': product.id,
        u'project_id': project_id
    }


def make_hosting_service_output_error(project_id, date):
    return {
        u'error': u'Unable to locate project "{}" in timeline: start_dt="{}", finish_dt="{}"'.format(
            project_id, date, date)
    }


# ------------------------------------------------------------------------------------------
# AVIA

def create_contract(dt):
    params = {
        'start_dt': dt,
        'finish_dt': dt + relativedelta(months=6)
    }

    client_id, _, contract_id, _ = steps.ContractSteps.create_partner_contract(AVIA_RU_CONTEXT,
                                                                               additional_params=params)

    return client_id, contract_id


def make_avia_context(dt):
    ids = [create_contract(dt) for _ in range(2)]
    client_id_wo_contract = steps.ClientSteps.create()
    eur_rate = steps.CurrencySteps.get_currency_rate(dt, Currencies.EUR.char_code,
                                                     Currencies.RUB.char_code, CurrencyRateSource.ECB.id)
    return {
        'client_ids': [id_pair[0] for id_pair in ids],
        'contract_ids': [id_pair[1] for id_pair in ids],
        'client_id_wo_contract': client_id_wo_contract,
        'eur_rate': eur_rate
    }


def make_avia_input_redir_log(client_id, row_id, price, clid='', stid='', national_version=NatVer.RU, ip='127.0.0.1'):
    return {
        'billing_client_id': client_id,
        'clid': clid,
        'stid': stid,
        'national_version': national_version,
        'userip': ip,  # '87.250.239.148'
        'price': price,

        'show_id': '3592189.1564606800.{}'.format(row_id),
        'iso_eventtime': '2019-08-01 00:00:00',

        # do not affect logic
        '_logfeller_timestamp': 1564606800,
        'timestamp': 1564606800,
        'puid': None,
        'puid_ses_type': None,
        'spuid': None,
        'spuid_ses_type': None,
        'p2uid': None,
        'p2uid_ses_type': None,
        'adult_seats': 1,
        'billing_order_id': 1026,
        'children_seats': 0,
        'infant_seats': 0,
        'eventtime': '20190731210000',
        'experimentsTestIds': None,
        'fromId': 'c213',
        'host': 'ticket',
        'klass': 'economy',
        'lang': 'ru',
        'marker': 'YA01EP9K67',
        'national': 'ru',
        'offer_currency': 'RUR',
        'offer_price': 17322.0,
        'original_currency': 'RUR',
        'original_price': 17322.0,
        'passportuid': None,
        'pp': 508,
        'price_cpa': 1610,
        'price_cpc': 0,
        'price_ecpc': 8.3499999999999996447,
        'qid': '190731-220715-075.m_avia.plane.c213_c1095_2019-09-24_None_economy_1_0_0_ru.ru',
        'queryKey': 'c213_c1095_2019-09-24_None_economy_1_0_0_ru',
        'query_source': 'm_avia',
        'return_date': '',
        'service': 'm_avia',
        'tduid': None,
        'toId': 'c1095',
        'url': 'https://www.aeroflot.ru/sb/app/ru-ru?yaclid=IjIwMTktMDctMzFUMjE6MDA6MDAi.AG7lIX8RrJ63gV7xD8fHUYNpZ4c&_openstat=ticket.yandex.ru%3Baeroflot%3B%D0%9A%D1%83%D0%BF%D0%B8%D1%82%D1%8C%20%D0%90%D0%B2%D0%B8%D0%B0%D0%B1%D0%B8%D0%BB%D0%B5%D1%82%3Bavia.yandex.ru#/passengers?adults=1&children=0&infants=0&segments=SVO20190924ABA.SU1478.T.TNOR.N.NB&referrer=YA01EP9K67',
        'unixtime': 1564606800,
        'user_from_geo_id': 21621,
        'user_from_key': 'c21621',
        'utm_campaign': 'city',
        'utm_content': 'offer',
        'utm_medium': 'pp',
        'utm_source': 'wizard_ru',
        'utm_term': None,
        'uuid': 'cf90a210-be43-4a0b-b125-0ca2ff2b8706',
        'when': '2019-09-24',
        'wizardRedirKey': '16b45b5a-b3d6-11e9-8808-1fa200ca9302|1',
        'wizardReqId': '1564606769575692-1628941378219154052500035-vla1-0244-TCH',
        'wizard_flags': '{\'new_saas\':1,\'with_filters\':1}',
        'yandexuid': '5643393891564606755',
        'variantId': 'aeroflot015646068001174810623476337559',
        'partnerId': 111,
        'clid24h': None,
        'stid24h': None,
        'tduid24h': None,
        'utms24h': {
            'utm_campaign': 'city',
            'utm_term': None,
            'utm_source': 'wizard_ru',
            'utm_medium': 'pp',
            'utm_content': 'offer'
        },
        'serp_uuid': None,
        'eppid': None,
        'icookie': 'zd9HV+ISvxSQBw4e2QQgWsl8iWWXoLD9gVmd9R0RNhoirdamg0M/+AmO9IypRHxrL7C+qRyM/LYHKAc7DMNJELATxPc=',
        'test_buckets': '34317,0,71;154216,0,85',
        '_rest': {
            'settlement_from_id': 213,
            'wizardReqId24': True,
            'settlement_to_id': 1095
        },
        'source_uri': 'prt://rasp-front@sas1-ccb50cc16d44.qloud-c.yandex.net/ephemeral/var/log/yandex/avia-frontend/yt/json_redir.log',
        '_stbx': 'rt3.man--rasp-front--avia-json-redir-log:0@@5363172@@base64:-ZQUUItxGsoRBmhvp_JwLg@@1564606800124@@1564606800@@avia-json-redir-log@@49305575@@1564606800136',
        '_logfeller_index_bucket': '//home/logfeller/index/rasp-front/avia-json-redir-log/900-1800/1564606500/1564606800'
    }


def make_avia_input_show_log(client_id, row_id, clid='', stid='', national_version=NatVer.RU, ip='127.0.0.1'):
    return {
        'billing_client_id': client_id,
        'clid': clid,
        'stid': stid,
        'national_version': national_version,
        'userip': ip,

        'show_id': '8941553.1564606805.{}'.format(row_id),
        'iso_eventtime': '2019-08-01 00:00:05',

        # do not affect logic
        'source_uri': 'prt://rasp-front@myt2-b7a7fdb54f3c.qloud-c.yandex.net/ephemeral/var/log/yandex/avia-frontend/yt/show.log',
        'subkey': '',
        'tskv_format': 'avia-show-log',
        'unixtime': '1564606805',
        'service': 'ticket',
        'datasource_id': '428',
        'billing_order_id': '429',
        'qid': '190731-235811-915.wizard.plane.c10945_c1107_2019-08-05_None_economy_1_1_0_ru.ru',
        'fromId': 'c10945',
        'toId': 'c1107',
        'when': '2019-08-05',
        'return_date': '',
        'klass': 'economy',
        'adult_seats': '1',
        'children_seats': '1',
        'infant_seats': '0',
        'lang': 'ru',
        'queryKey': 'c10945_c1107_2019-08-05_None_economy_1_1_0_ru',
        'utm_source': 'wizard_ru',
        'utm_medium': 'common',
        'utm_campaign': 'city',
        'utm_content': 'offer',
        'utm_term': 'after_preloader',
        'tduid': '',
        'wizard_flags': '{\'new_saas\':1,\'with_filters\':1}',
        'wizardReqId': '1564606642373828-1451080907433731929400035-man1-3527',
        'wizardReqId24': 'true',
        'serp_uuid': '',
        'eppid': '',
        'utms24h': '',
        'clid24h': '',
        'stid24h': '',
        'tduid24h': '',
        'price': '77',
        'host': 'ticket',
        'national': 'ru',
        'pp': '502',
        'query_source': 'wizard',
        'baggage_included_count': '0',
        'baggage_included_source': 'partner',
        'baggage_pc_count': '0',
        'baggage_pc_source': 'partner',
        'baggage_wt_count': '0',
        'baggage_wt_source': 'partner',
        'variantId': '',
        'eventtime': '20190731210005',
        'yandexuid': '4133941731563712168',
        'passportuid': '',
        'icookie': 'bRfn9vwXW66xJZvDFyf/UHqvUuB8xN7hgAzNE7mpJLaAij5s/HKKrg38ZIb7fyxtJbhfKzEJS57t9ANMszaSkeci2fk=',
        '_stbx': 'þþþþþþþþþþþþþþþþþþþþþþþþþþþþþþþþþþ:0:64540023:î7ÕIl\u0001\u0000\u0000:q4ÕIl\u0001\u0000\u0000:\nrasp-front'
    }


def make_avia_output_error_no_contract(client_id, clicks, amount, national_version=NatVer.RU):
    return {
        u'client_id': client_id,
        u'clicks': clicks,
        u'national_version': national_version,
        u'price': amount,

        u'shows': 0,
        u'error': u'Unable to locate contract data for this entry',
        u'clid': None,
        u'stid': None,
    }


def make_avia_output_completion(client_id, contract_id, clicks, amount, national_version=NatVer.RU,
                                currency=Currencies.RUB):
    return {
        u'client_id': client_id,
        u'contract_id': contract_id,
        u'clicks': clicks,
        u'product_id': AVIA_PRODUCT_IDS[national_version][currency],
        u'price': amount,

        u'currency': currency.iso_code,
        u'national_version': national_version,
        u'shows': 0,
    }


def make_avia_output_distribution(client_id, clicks, amount, national_version=NatVer.RU):
    return {
        u'national_version': national_version,
        u'bucks': amount,
        u'client_id': client_id,
        u'clicks': clicks,

        u'shows': 0,
        u'clid': None,
        u'stid': None,
    }


def make_avia_output_fraud(client_id, filter_name):
    return {
        u"billing_client_id": client_id,
        u"filter_name": filter_name,
    }


def calc_avia_bucks(amount, dt, nds=NdsNew.DEFAULT):
    return utils.dround(Decimal(amount) / nds.koef_on_dt(dt) / 30, 6)


# ------------------------------------------------------------------------------------------
# TAXI

def make_taxi_input(transaction_id, client_id, clid, currency, transaction_type, amount, str_dt,
                    service=Services.TAXI_128, product=TaxiOrderType.commission, ignore_in_balance=False,
                    aggregation_sign=1):
    return {
        "amount": '{:.4f}'.format(amount),
        "currency": currency.iso_code,
        "clid": clid,
        "service_id": service.id,
        "client_id": str(client_id),
        "product": product,
        "event_time": "{}T20:59:58.000000+00:00".format(str_dt),
        "transaction_id": transaction_id,
        "transaction_type": transaction_type.name,
        "ignore_in_balance": ignore_in_balance,
        "aggregation_sign": aggregation_sign,

        # do not affect logic
        "transaction_time": "2019-07-31T21:00:00.042769+00:00",
        "due": "2019-07-31T20:50:00.000000+00:00",
        "orig_transaction_id": 556733459,
        "service_transaction_id": "868463560090",
        "payload": {
            "event_id": "5d42014ea3730f16fdf6bd6c",
            "alias_id": "63767a2394a12eeda936380f155051b4"
        },
    }


def make_taxi_output_distr(clid, count, amount):
    return {
        "count": count,
        "clid": clid,
        "amount": amount
    }

TLOG_VERSION = 2


def make_taxi_output_revenue(client_id, last_transaction_id, currency, amount, str_dt,
                             service=Services.TAXI_128, product=TaxiOrderType.commission):
    return {
        "product": product,
        "currency": currency.iso_code,
        "amount": amount,
        "last_transaction": last_transaction_id,
        "service_id": service.id,
        "client_id": client_id,
        "event_time": "{}T00:00:00+03:00".format(str_dt),
        "ignore_in_balance": False,
        "tlog_version": TLOG_VERSION
    }


def make_taxi_tl_closed_attributes():
    return {
        'fetcher_config':
            {
                'yandex-balance-end-of-day-marker': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
            }
    }


# ----------------------------------------------------------------------------------------------------------------------
# BLUE MARKET

def make_blue_market_context(dt):
    result = db.balance().execute("select value_json from bo.t_config where item='TLOG_BLUE_MARKET_CONFIG'")
    tlog_start_dt_str = json.loads(result[0]['value_json'])['migration-date']
    tlog_start_dt = datetime.strptime(tlog_start_dt_str, '%Y-%m-%d')
    filter_border = tlog_start_dt.replace(day=1) + relativedelta(months=1)
    return {
        'tlog_start_dt': tlog_start_dt,
        'filter_border': filter_border,
    }


def make_blue_market_input(transaction_id, event_time_tz, transaction_time_tz, client_id, amount,
                           currency=Currencies.RUB, product=BlueMarketOrderType.fee, service=Services.BLUE_MARKET,
                           aggregation_sign=1, ignore_in_balance=False, nds=1):
    # 2020-04-08T16:59:44.000000+03:00
    dt_template = '{}+{:02d}:00'
    event_time = dt_template.format(event_time_tz[0].strftime('%Y-%m-%dT%H:%M:%S.%f'), event_time_tz[1])
    transaction_time = dt_template.format(transaction_time_tz[0].strftime('%Y-%m-%dT%H:%M:%S.%f'), transaction_time_tz[1])
    return {
        "transaction_id": transaction_id,
        "event_time": event_time,
        "transaction_time": transaction_time,
        "client_id": str(client_id),
        "product": product,
        "amount": str(amount),
        "currency": currency.iso_code,
        "service_id": service.id,
        "aggregation_sign": aggregation_sign,
        "ignore_in_balance": ignore_in_balance,
        "nds": nds,

        # do not affect logic
        "previous_transaction_id": "null",
        "key": {
            "event_id": "5d42014ea3730f16fdf6bd6c",
            "alias_id": "63767a2394a12eeda936380f155051b4"
        },
    }


def make_blue_market_output_revenue(event_time_msk, client_id, amount, last_transaction_id,
                                    product=BlueMarketOrderType.fee, nds=1, currency=Currencies.RUB,
                                    service=Services.BLUE_MARKET):
    # 2020-03-30T00:00:00+03:00
    dt_template = '{}+03:00'
    event_time = dt_template.format(event_time_msk.strftime('%Y-%m-%dT%H:%M:%S'))
    return {
        "amount": str(amount),
        "client_id": int(client_id),
        "currency": currency.iso_code,
        "event_time": event_time,
        "last_transaction_id": int(last_transaction_id),
        "nds": int(nds),
        "product": product,
        "service_id": service.id,
    }


# ----------------------------------------------------------------------------------------------------------------------
# BLUE MARKET


def tran_time_is_event_time_plus_delta(**delta_kwargs):
    def decorator(func):
        def wrapper(*args, **kwargs):
            kwargs['transaction_time_tz'] = kwargs['event_time_tz'][:]
            kwargs['transaction_time_tz'][0] += relativedelta(**delta_kwargs)
            return func(*args, **kwargs)
        return wrapper
    return decorator


def blue_marketing_partial_for(base):
    def decorator(dummy_func):
        return partial(
            base,
            product=BlueMarketingServicesOrderType.marketing_promo_tv,
            service=Services.BILLING_MARKETING_SERVICES
        )
    return decorator


@tran_time_is_event_time_plus_delta(days=1)
@blue_marketing_partial_for(make_blue_market_input)
def make_blue_marketing_services_input():
    pass


@blue_marketing_partial_for(make_blue_market_output_revenue)
def make_blue_marketing_services_output_revenue():
    pass

# мне показалось, что так выходит наиболее читабельно
