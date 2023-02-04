# coding: utf-8
from decimal import Decimal as D

import datetime as dt
import json
import pytest
import uuid
import xmlrpclib

import btestlib.utils as utils
from balance import balance_steps as steps
from btestlib.matchers import contains_dicts_with_entries, equal_to
from btestlib import reporter
from btestlib.constants import PaymentType, YTSourceName, YTDefaultPath, TransactionType, TaxiOrderType, Services
from btestlib.data.partner_contexts import TAXI_RU_CONTEXT_SPENDABLE, CORP_TAXI_RU_CONTEXT_SPENDABLE, \
                                           TAXI_REQUEST_CONTEXT_SPENDABLE

SOURCE_NAME = YTSourceName.TAXI_SUBVENTIONS_SOURCE_NAME
ROOT_PATH = YTDefaultPath.TAXI_SUBVENTIONS_YT_ROOT_PATH
DEFAULT_CLIENT = 31337
DEFAULT_AMOUNT = D('432.95')
BASE_DATE = utils.Date.nullify_time_of_date(dt.datetime.now())


def test_default_processing_chain():
    yt_client = steps.YTSteps.create_yt_client()

    next_date = BASE_DATE
    cur_date = next_date - dt.timedelta(days=1)
    prev_date = cur_date - dt.timedelta(days=1)

    transaction_ids = steps.PartnerSteps.generate_subvention_transaction_log_ids(n=7)
    compls = [
          prepare_row(transaction_ids[0], uuid.uuid1().hex, payload={'ProcessThroughTrust': 1, 'kotiki': 'shaurma',
                                                                     'alias_id': 'bcds123'},
                      export_state=1),
          prepare_row(transaction_ids[1], uuid.uuid1().hex, payload={'sobachki': 'shashlyk', 'alias_id': 'bcds123'},
                      export_state=1, amount=DEFAULT_AMOUNT*D('1.5'), product=PaymentType.COUPON),
          prepare_row(transaction_ids[2], uuid.uuid1().hex, amount=DEFAULT_AMOUNT * D('0.5'),
                      orig_transaction_id=transaction_ids[0], transaction_type=TransactionType.REFUND.name),
          prepare_row(transaction_ids[3], uuid.uuid1().hex, client_id=23456, amount=DEFAULT_AMOUNT * D('0.7'),
                      service_id=Services.TAXI_REQUEST.id, product=PaymentType.CORP_TAXI_PARTNER_TRIP_PAYMENT,
                      currency=TAXI_REQUEST_CONTEXT_SPENDABLE.currency.iso_code),
          prepare_row(transaction_ids[4], uuid.uuid1().hex, client_id=23456, amount=DEFAULT_AMOUNT * D('0.4'),
                      service_id=Services.TAXI_REQUEST.id, product=PaymentType.CORP_TAXI_PARTNER_TRIP_PAYMENT,
                      currency=TAXI_REQUEST_CONTEXT_SPENDABLE.currency.iso_code,
                      transaction_type=TransactionType.REFUND.name, orig_transaction_id=transaction_ids[3]),
          prepare_row(transaction_ids[5], uuid.uuid1().hex, client_id=23456, amount=DEFAULT_AMOUNT * D('0.7'),
                      service_id=Services.TAXI_REQUEST.id, product=PaymentType.CASH,
                      currency=TAXI_REQUEST_CONTEXT_SPENDABLE.currency.iso_code),
          prepare_row(transaction_ids[6], uuid.uuid1().hex, client_id=23456, amount=DEFAULT_AMOUNT * D('0.4'),
                      service_id=Services.TAXI_REQUEST.id, product=PaymentType.CASH,
                      currency=TAXI_REQUEST_CONTEXT_SPENDABLE.currency.iso_code,
                      transaction_type=TransactionType.REFUND.name, orig_transaction_id=transaction_ids[5]),
    ]

    steps.YTSteps.remove_tables(yt_client, [cur_date, next_date], ROOT_PATH)
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

        prev_pcr = steps.CommonPartnerSteps.get_partner_completions_resource(SOURCE_NAME, prev_date)
        utils.check_that(prev_pcr['additional_params'], equal_to({'last_transaction_id': transaction_ids[0]}))

        expected_data = prepare_comparison_rows(compls[:1], get_table_name(prev_date))
        inserted_payments = \
            steps.PartnerSteps.get_partner_payment_stat_with_export(TAXI_REQUEST_CONTEXT_SPENDABLE.service.id,
                                                                          transaction_ids[:1])
        utils.check_that(inserted_payments, contains_dicts_with_entries(expected_data),
                         step=u'Сравним полученные платежи с ожидаемыми')

    data_for_yt = [prepare_yt_row(**c) for c in compls[1:]]
    steps.YTSteps.fill_table(yt_client, cur_date, data_for_yt, ROOT_PATH)

    with reporter.step(u"Процессим ресурс prev_date, чтобы он закрылся"):
        steps.CommonPartnerSteps.process_partners_completions(SOURCE_NAME, prev_date)
        prev_pcr = steps.CommonPartnerSteps.get_partner_completions_resource(SOURCE_NAME, prev_date)
        utils.check_that(prev_pcr['additional_params'], equal_to({'last_transaction_id': transaction_ids[0],
                                                                  'finished': Anything()}))

    with reporter.step(u"Процессим ресурс cur_date"):
        steps.CommonPartnerSteps.create_partner_completions_resource(SOURCE_NAME, cur_date)
        steps.CommonPartnerSteps.process_partners_completions(SOURCE_NAME, cur_date)

        cur_pcr = steps.CommonPartnerSteps.get_partner_completions_resource(SOURCE_NAME, cur_date)
        utils.check_that(cur_pcr['additional_params'], equal_to({'last_transaction_id': transaction_ids[6]}))

        expected_data = prepare_comparison_rows(compls[1:], get_table_name(cur_date))
        inserted_payments = \
            steps.PartnerSteps.get_partner_payment_stat_with_export(
                TAXI_REQUEST_CONTEXT_SPENDABLE.service.id,
                transaction_ids[1:])
        utils.check_that(inserted_payments, contains_dicts_with_entries(expected_data),
                         step=u'Сравним полученные платежи с ожидаемыми')


def test_flags():
    yt_client = steps.YTSteps.create_yt_client()

    next_date = BASE_DATE - dt.timedelta(days=7)
    cur_date = next_date - dt.timedelta(days=1)
    prev_date = cur_date - dt.timedelta(days=1)

    transaction_ids = steps.PartnerSteps.generate_subvention_transaction_log_ids(n=3)
    compls = [
        prepare_row(transaction_ids[0], uuid.uuid1().hex, payload={'ProcessThroughTrust': 1, 'kotiki': 'shaurma'},
                    service_id=Services.TAXI_REQUEST.id, export_state=1),
        prepare_row(transaction_ids[1], uuid.uuid1().hex, payload={'sobachki': 'shashlyk'},
                    service_id=Services.TAXI_REQUEST.id, export_state=1, amount=DEFAULT_AMOUNT * D('1.5'),
                    product=PaymentType.COUPON),
        prepare_row(transaction_ids[2], uuid.uuid1().hex, amount=DEFAULT_AMOUNT * D('0.5'),
                    service_id=Services.TAXI_REQUEST.id, orig_transaction_id=transaction_ids[0],
                    transaction_type=TransactionType.REFUND.name),
    ]

    steps.YTSteps.remove_tables(yt_client, [cur_date, next_date], ROOT_PATH)
    data_for_yt = [prepare_yt_row(**c) for c in compls[:1]]
    steps.YTSteps.fill_table(yt_client, prev_date, data_for_yt, ROOT_PATH)

    with reporter.step(u"Процессим prev_date c force=1"):
        steps.CommonPartnerSteps.create_partner_completions_resource(SOURCE_NAME, prev_date, {'force': 1})
        steps.CommonPartnerSteps.create_partner_completions_resource(SOURCE_NAME, prev_date - dt.timedelta(days=1))
        steps.CommonPartnerSteps.process_partners_completions(SOURCE_NAME, prev_date)

        prev_pcr = steps.CommonPartnerSteps.get_partner_completions_resource(SOURCE_NAME, prev_date)
        utils.check_that(prev_pcr['additional_params'], equal_to({'last_transaction_id': transaction_ids[0], 'force': 0}))

        expected_data = prepare_comparison_rows(compls[:1], get_table_name(prev_date))
        inserted_payments = \
            steps.PartnerSteps.get_partner_payment_stat_with_export(Services.TAXI_REQUEST.id,
                                                                          transaction_ids[:1])
        utils.check_that(inserted_payments, contains_dicts_with_entries(expected_data),
                         step=u'Сравним полученные платежи с ожидаемыми')

    data_for_yt = [prepare_yt_row(**c) for c in compls[1:2]]
    steps.YTSteps.fill_table(yt_client, cur_date, data_for_yt, ROOT_PATH)

    with reporter.step(u"Процессим ресурс prev_date, чтобы он закрылся"):
        steps.CommonPartnerSteps.create_partner_completions_resource(SOURCE_NAME, prev_date,
                                                                     {'force': 1, 'skip_earlier_inserted_transactions': 1})
        steps.CommonPartnerSteps.process_partners_completions(SOURCE_NAME, prev_date)
        prev_pcr = steps.CommonPartnerSteps.get_partner_completions_resource(SOURCE_NAME, prev_date)
        utils.check_that(prev_pcr['additional_params'], equal_to({'last_transaction_id': transaction_ids[0],
                                                                  'finished': Anything(),
                                                                  'skip_earlier_inserted_transactions': 1,
                                                                  'force': 0}))

    with reporter.step(u"Процессим ресурс cur_date"):
        steps.CommonPartnerSteps.create_partner_completions_resource(SOURCE_NAME, cur_date)
        steps.CommonPartnerSteps.process_partners_completions(SOURCE_NAME, cur_date)

        cur_pcr = steps.CommonPartnerSteps.get_partner_completions_resource(SOURCE_NAME, cur_date)
        utils.check_that(cur_pcr['additional_params'], equal_to({'last_transaction_id': transaction_ids[1]}))

        expected_data = prepare_comparison_rows(compls[1:2], get_table_name(cur_date))
        inserted_payments = \
            steps.PartnerSteps.get_partner_payment_stat_with_export(Services.TAXI_REQUEST.id,
                                                                    transaction_ids[1:2])
        utils.check_that(inserted_payments, contains_dicts_with_entries(expected_data),
                         step=u'Сравним полученные платежи с ожидаемыми')

    data_for_yt = [prepare_yt_row(**c) for c in compls[1:]]
    steps.YTSteps.fill_table(yt_client, cur_date, data_for_yt, ROOT_PATH)

    with reporter.step(u"Процессим ресурс cur_date безначальной транзакции но с проверкой "
                  u"и пропуском ранее забранных транзакций"):
        steps.CommonPartnerSteps.create_partner_completions_resource(SOURCE_NAME, cur_date,
                                                    {'skip_earlier_inserted_transactions': 1})
        steps.CommonPartnerSteps.process_partners_completions(SOURCE_NAME, cur_date)

        cur_pcr = steps.CommonPartnerSteps.get_partner_completions_resource(SOURCE_NAME, cur_date)
        utils.check_that(cur_pcr['additional_params'], equal_to(
            {'last_transaction_id': transaction_ids[2], 'skip_earlier_inserted_transactions': 1}))

        expected_data = prepare_comparison_rows(compls[1:], get_table_name(cur_date))
        inserted_payments = \
            steps.PartnerSteps.get_partner_payment_stat_with_export(Services.TAXI_REQUEST.id,
                                                                    transaction_ids[1:])
        utils.check_that(inserted_payments, contains_dicts_with_entries(expected_data),
                         step=u'Сравним полученные платежи с ожидаемыми')


# UTILS
class Anything(object):
    def __eq__(self, other):
        return True


def prepare_row(transaction_id, service_transaction_id=None, client_id=DEFAULT_CLIENT, amount=DEFAULT_AMOUNT,
                service_id=TAXI_REQUEST_CONTEXT_SPENDABLE.service.id, payload=None, event_time=BASE_DATE,
                transaction_time=BASE_DATE, product=PaymentType.SUBSIDY, transaction_type=TransactionType.PAYMENT.name,
                currency=TAXI_REQUEST_CONTEXT_SPENDABLE.currency.iso_code, export_state=0, orig_transaction_id=None,
                payment_type='unknown', ignore_in_balance=False, detailed_product='XXX666'):
    return {'orig_transaction_id': orig_transaction_id,
            'product': product,
            'event_time': event_time,
            'transaction_time': transaction_time,
            'service_transaction_id': service_transaction_id,
            'transaction_type': transaction_type,
            'currency': currency,
            'amount': amount,
            'payment_type': payment_type,
            'client_id': client_id,
            'service_id': service_id,
            'payload': payload,
            'transaction_id': transaction_id,
            'export_state': export_state,
            'ignore_in_balance': ignore_in_balance,
            'detailed_product': detailed_product,
            }


def filter_payload(payload):
    if not payload:
        return None
    if 'ProcessThroughTrust' in payload:
        return json.dumps({'ProcessThroughTrust': payload['ProcessThroughTrust']}, cls=steps.BalanceJSONEncoder)
    return None


def get_alias_id(payload):
    if payload:
        return payload.get('alias_id')


def prepare_yt_row(**kwargs):
    return {
        'transaction_id': int(kwargs['transaction_id']),
        'event_time': steps.CommonSteps.format_dt_msk2utc(kwargs.get('event_time')),
        'transaction_time': steps.CommonSteps.format_dt_msk2utc(kwargs.get('transaction_time')),
        'transaction_type': kwargs.get('transaction_type'),
        'orig_transaction_id': int(kwargs.get('orig_transaction_id')) if kwargs.get('orig_transaction_id') is not None else None,
        'service_transaction_id': kwargs.get('service_transaction_id'),
        'service_id': kwargs.get('service_id') and int(kwargs.get('service_id')),
        'client_id': kwargs['client_id'] and str(kwargs['client_id']),
        'product': kwargs.get('product'),
        'payment_type': kwargs.get('payment_type', 'unknown'),  # поле не используется
        'amount': str(kwargs['amount']),
        'currency': kwargs.get('currency'),
        'payload': kwargs.get('payload', {}),
        'ignore_in_balance': kwargs.get('ignore_in_balance'),
        'detailed_product': kwargs.get('detailed_product'),
    }


def prepare_comparison_row(**kwargs):
    return {
        'orig_transaction_id': kwargs.get('orig_transaction_id'),
        'payment_type': kwargs.get('product'),
        'dt': kwargs.get('event_time') and kwargs.get('event_time').replace(microsecond=0),
        'extra_dt_0': kwargs.get('transaction_time') and kwargs.get('transaction_time').replace(microsecond=0),
        'price': kwargs['amount'],
        'transaction_type': kwargs.get('transaction_type'),
        'currency': kwargs.get('currency'),
        'extra_str_0': get_alias_id(kwargs.get('payload')),
        'extra_str_1': kwargs.get('table_name'),
        'client_id': kwargs.get('client_id'),
        'service_id': kwargs.get('service_id'),
        'transaction_id': kwargs.get('transaction_id'),
        'payload': filter_payload(kwargs.get('payload')),
    }


def prepare_comparison_rows(compls, table_name):
    return [prepare_comparison_row(table_name=table_name, **c) for c in compls if not c['ignore_in_balance']]


def remove_tables(yt_client, dt_list):
    for date in dt_list:
        with reporter.step(u"Удаляем таблицу в yt за дату {}".format(date)):
            filename = get_table_name(date)
            steps.YTSteps.remove_table_in_yt(ROOT_PATH + filename, yt_client)


def fill_table(yt_client, date, compls):
    with reporter.step(u"Заполняем в yt таблицу на дату {}".format(date)):
        filename = get_table_name(date)
        filepath = ROOT_PATH + filename
        data_for_yt = [prepare_yt_row(**c) for c in compls]
        steps.YTSteps.create_data_in_yt(yt_client, filepath, data_for_yt)


def get_table_name(date):
    return dt.datetime.strftime(date, "%Y-%m-%d")
