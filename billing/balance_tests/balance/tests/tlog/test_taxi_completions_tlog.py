# coding: utf-8
from decimal import Decimal as D

import datetime as dt
import pytest
import xmlrpclib

import btestlib.utils as utils
from balance import balance_steps as steps
from btestlib.matchers import contains_dicts_equal_to, equal_to
from btestlib import reporter
from btestlib.constants import YTSourceName, YTDefaultPath, TaxiOrderType, Currencies, Services, CorpTaxiOrderType
from btestlib.data.partner_contexts import TAXI_RU_CONTEXT

SOURCE_NAME = YTSourceName.TAXI_COMPLETIONS_SOURCE_NAME
BASE_DATE = utils.Date.nullify_time_of_date(dt.datetime.now())
ROOT_PATH = YTDefaultPath.TAXI_COMPLETIONS_YT_ROOT_PATH
DEFAULT_CLIENT = 666
DEFAULT_AMOUNT = D('123.12')

TLOG_VERSION = 2


def test_default_processing_chain():
    yt_client = steps.YTSteps.create_yt_client()

    next_date = BASE_DATE
    cur_date = next_date - dt.timedelta(days=1)
    prev_date = cur_date - dt.timedelta(days=1)

    compls = [
        prepare_row(888),
        prepare_row(999, client_id=31337, currency=Currencies.USD.iso_code, service_id=Services.TAXI_128.id),
        prepare_row(1111, amount=D('-999.123'), currency=Currencies.USD.iso_code, product=TaxiOrderType.promocode_tlog),
        prepare_row(2222, client_id=98765, service_id=Services.TAXI_CORP_CLIENTS.id,
                    amount=D('356.67'), product=CorpTaxiOrderType.commission),
    ]

    steps.YTSteps.remove_tables(yt_client, [prev_date, cur_date, next_date], ROOT_PATH)
    data_for_yt = [prepare_yt_row(**c) for c in compls[:1]]
    steps.YTSteps.fill_table(yt_client, prev_date, data_for_yt, ROOT_PATH)

    with reporter.step(u"Пытаемся процессить ресурс prev_date, когда предыдущий (prev_date - 1 день) еще не закрыт"):
        steps.CommonPartnerSteps.create_partner_completions_resource(SOURCE_NAME, prev_date)
        steps.CommonPartnerSteps.create_partner_completions_resource(SOURCE_NAME, prev_date - dt.timedelta(days=1))
        with pytest.raises(xmlrpclib.Fault) as exc_info:
            steps.CommonPartnerSteps.process_partners_completions(SOURCE_NAME, prev_date)
        utils.check_that('Resource delayed: Waiting for processing of the resource for the previous date'
                         in exc_info.value.faultString, equal_to(True),
                         step=u'Проверим, что ресурс Delayed')

    with reporter.step(u"Закрываем предыдущий ресурс (prev_date - 1 день) и процессим текущий prev_date"):
        steps.CommonPartnerSteps.create_partner_completions_resource(SOURCE_NAME, prev_date - dt.timedelta(days=1),
                                                                     {'finished': dt.datetime.now()})
        steps.CommonPartnerSteps.process_partners_completions(SOURCE_NAME, prev_date)

        expected_data = prepare_comparison_rows(prev_date, compls[:1])
        inserted_compls = steps.TaxiSteps.get_taxi_stat_aggr_tlog(prev_date)
        utils.check_that(inserted_compls, contains_dicts_equal_to(expected_data),
                         step=u'Сравним полученные платежи с ожидаемыми')

    data_for_yt = [prepare_yt_row(**c) for c in compls[1:]]
    steps.YTSteps.fill_table(yt_client, prev_date, data_for_yt, ROOT_PATH,
                             attributes={"fetcher_config": {"yandex-balance-end-of-day-marker": "gl hf"}})

    with reporter.step(u"Процессим ресурс prev_date, чтобы он закрылся. Так же проверяем, "
                  u"что данные при перезаборе заменилиcь на новые"):
        steps.CommonPartnerSteps.process_partners_completions(SOURCE_NAME, prev_date)
        prev_pcr = steps.CommonPartnerSteps.get_partner_completions_resource(SOURCE_NAME, prev_date)
        utils.check_that(prev_pcr['additional_params'], equal_to({'finished': Anything()}))

        expected_data = prepare_comparison_rows(prev_date, compls[1:])
        inserted_compls = steps.TaxiSteps.get_taxi_stat_aggr_tlog(prev_date)
        utils.check_that(inserted_compls, contains_dicts_equal_to(expected_data),
                         step=u'Сравним полученные платежи с ожидаемыми')

    data_for_yt = [prepare_yt_row(**c) for c in compls]
    steps.YTSteps.fill_table(yt_client, cur_date, data_for_yt, ROOT_PATH)

    with reporter.step(u"Процессим ресурс cur_date"):
        steps.CommonPartnerSteps.create_partner_completions_resource(SOURCE_NAME, cur_date)
        steps.CommonPartnerSteps.process_partners_completions(SOURCE_NAME, cur_date)

        expected_data = prepare_comparison_rows(cur_date, compls)
        inserted_compls = steps.TaxiSteps.get_taxi_stat_aggr_tlog(cur_date)
        utils.check_that(inserted_compls, contains_dicts_equal_to(expected_data),
                         step=u'Сравним полученные платежи с ожидаемыми')


def test_flags():
    yt_client = steps.YTSteps.create_yt_client()

    cur_date = BASE_DATE - dt.timedelta(days=6)
    prev_date = BASE_DATE - dt.timedelta(days=7)

    compls = [
        prepare_row(888),
        prepare_row(999, client_id=31337, currency=Currencies.USD.iso_code, service_id=Services.TAXI_128.id),
        prepare_row(1111, amount=D('-999.123'), currency=Currencies.USD.iso_code, product=TaxiOrderType.promocode_tlog),
        prepare_row(2222, client_id=98765, service_id=Services.TAXI_CORP_CLIENTS.id,
                    amount=D('356.67'), product=CorpTaxiOrderType.commission),
    ]

    steps.YTSteps.remove_tables(yt_client, [prev_date, cur_date], ROOT_PATH)
    data_for_yt = [prepare_yt_row(**c) for c in compls]
    steps.YTSteps.fill_table(yt_client, prev_date, data_for_yt, ROOT_PATH)

    with reporter.step(u"Процессим prev_date c force=1"):
        steps.CommonPartnerSteps.create_partner_completions_resource(SOURCE_NAME, prev_date, {'force': 1})
        steps.CommonPartnerSteps.create_partner_completions_resource(SOURCE_NAME, prev_date - dt.timedelta(days=1))
        steps.CommonPartnerSteps.process_partners_completions(SOURCE_NAME, prev_date)

        prev_pcr = steps.CommonPartnerSteps.get_partner_completions_resource(SOURCE_NAME, prev_date)
        utils.check_that(prev_pcr['additional_params'], equal_to({'force': 0}))

        expected_data = [prepare_comparison_row(transaction_dt=prev_date, **c) for c in compls]
        inserted_compls = steps.TaxiSteps.get_taxi_stat_aggr_tlog(prev_date)
        utils.check_that(inserted_compls, contains_dicts_equal_to(expected_data),
                         step=u'Сравним полученные платежи с ожидаемыми')


# UTILS

class Anything(object):
    def __eq__(self, other):
        return True


def prepare_row(last_transaction_id, client_id=DEFAULT_CLIENT, amount=DEFAULT_AMOUNT,
                service_id=TAXI_RU_CONTEXT.service.id,event_time=BASE_DATE,
                product=TaxiOrderType.commission, currency=TAXI_RU_CONTEXT.currency.iso_code,
                ignore_in_balance=False, aggregation_sign=1, tlog_version=TLOG_VERSION,
                detailed_product='XXX666'):
    return {'amount': amount,
            'client_id': client_id,
            'currency': currency,
            'event_time': event_time,
            'last_transaction': last_transaction_id,
            'product': product,
            'service_id': service_id,
            'ignore_in_balance': ignore_in_balance,
            'aggregation_sign': aggregation_sign,
            'tlog_version': tlog_version,
            'detailed_product': detailed_product,
            }


def prepare_yt_row(**kwargs):
    return {
        'amount': str(kwargs['amount']),
        'client_id': kwargs['client_id'] and str(kwargs['client_id']),
        'currency': kwargs.get('currency'),
        'event_time': steps.CommonSteps.format_dt_msk2utc(kwargs.get('event_time')),
        'last_transaction': int(kwargs['last_transaction']),
        'product': kwargs.get('product'),
        'service_id': kwargs.get('service_id') and int(kwargs.get('service_id')),
        'ignore_in_balance': kwargs.get('ignore_in_balance'),
        'aggregation_sign': kwargs.get('aggregation_sign'),
        'tlog_version': kwargs.get('tlog_version'),
        'detailed_product': kwargs.get('detailed_product'),
    }


def prepare_comparison_row(**kwargs):
    return {
        'transaction_dt': kwargs.get('transaction_dt'),
        'dt': kwargs.get('event_time') and kwargs.get('event_time').replace(microsecond=0),
        'client_id': kwargs.get('client_id'),
        'commission_currency': kwargs.get('currency'),
        'service_id': kwargs.get('service_id'),
        'amount': str(kwargs['amount'] * kwargs.get('aggregation_sign')),
        'type': kwargs.get('product'),
        'last_transaction_id': kwargs.get('last_transaction'),
        'tlog_version': kwargs.get('tlog_version'),
    }


def prepare_comparison_rows(on_dt, compls):
    return [prepare_comparison_row(transaction_dt=on_dt, **c) for c in compls if not c['ignore_in_balance']]


def remove_tables(yt_client, dt_list):
    for date in dt_list:
        with reporter.step(u"Удаляем таблицу в yt за дату {}".format(date)):
            filename = dt.datetime.strftime(date, "%Y-%m-%d")
            steps.YTSteps.remove_table_in_yt(ROOT_PATH + filename, yt_client)


def fill_table(yt_client, date, compls, attributes=None):
    with reporter.step(u"Заполняем в yt таблицу на дату {}".format(date)):
        filename = dt.datetime.strftime(date, "%Y-%m-%d")
        filepath = ROOT_PATH + filename
        data_for_yt = [prepare_yt_row(**c) for c in compls]
        steps.YTSteps.create_data_in_yt(yt_client, filepath, data_for_yt, attributes_dict=attributes)
