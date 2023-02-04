# coding=utf-8
__author__ = 'borograam'

import inspect
import uuid
import xmlrpclib
from decimal import Decimal as D

import datetime as dt
import itertools
import pytest
from bottle import cached_property
from hamcrest import equal_to
from operator import mul

import btestlib.utils as utils
from balance import balance_steps as steps
from btestlib import reporter
from btestlib.constants import TransactionType, YTDefaultPath, YTSourceName
from btestlib.matchers import contains_dicts_with_entries

DEFAULT_CLIENT = 31337
DEFAULT_AMOUNT = D('432.95')
BASE_DATE = utils.Date.nullify_time_of_date(dt.datetime.now())


class DataRow(dict):  # сменить дефолт для сервиса
    def __init__(self, transaction_id=None, event_time=BASE_DATE, transaction_time=BASE_DATE, record_type='payment',
                 client_id=DEFAULT_CLIENT, service_id=610, paysys_type_cc='paysys',
                 service_transaction_id='1234-item-5678',
                 amount=DEFAULT_AMOUNT, ignore_in_balance=False, transaction_type=TransactionType.PAYMENT.name,
                 product='partner_product',
                 ignore_in_oebs=False, currency='RUB'):
        locals_ = locals()
        super(DataRow, self).__init__({k: locals_[k]
                                       for k in inspect.getargspec(self.__init__).args
                                       if k != 'self'})

    @staticmethod
    def as_bool(obj):
        if isinstance(obj, (str, unicode)):
            obj = obj.strip().lower()
            if obj in ('true', 'yes', 'on', 'y', 't', '1'):
                return True
            elif obj in ('false', 'no', 'off', 'n', 'f', '0'):
                return False
            raise ValueError('string is not true/false: {}'.format(obj))
        return bool(obj)

    @cached_property
    def yt_row(self):
        return {
            'transaction_id': self['transaction_id'],
            'event_time': steps.CommonSteps.format_dt_msk2utc(self['event_time']),
            'transaction_time': steps.CommonSteps.format_dt_msk2utc(self['transaction_time']),
            'record_type': self['record_type'],
            'factoring': 'bank',
            'client_id': self['client_id'],
            'partner_id': None,
            'entity_id': None,
            'entity_type': None,
            'service_id': self['service_id'],
            'contract_id': 666,
            'product': self['product'],
            'transaction_type': self['transaction_type'],
            'paysys_type_cc': self['paysys_type_cc'],
            'paysys_partner_id': None,
            'secured_payment': False,
            'service_transaction_id': self['service_transaction_id'],
            'currency': self['currency'],  # 'RUB', 'ILS'
            'amount': self['amount'],
            'ignore_in_balance': self['ignore_in_balance'],
            'ignore_in_oebs': self['ignore_in_oebs'],
            'payload': '{}',
            'previous_transaction_id': None,
            'order_id': None,
            'checkouter_id': None,
            'org_id': None,
            'terminal_id': None,
            'terminal_contract_id': None,
        }

    @cached_property
    def pps_row(self):
        return {
            'service_id': self['service_id'],
            'transaction_id': self['transaction_id'],
            'orig_transaction_id': None,
            'transaction_dt': self['transaction_time'],
            'dt': self['event_time'],
            'payment_type': self['product'],
            'transaction_type': self['transaction_type'],
            'client_id': self['client_id'],
            'currency': self['currency'],
            'price': self['amount'],
            'paysys_type_cc': self['paysys_type_cc'],
            'payload': '{}',
            'extra_str_0': self['service_transaction_id'],
            'extra_str_1': 'bank',
            'extra_num_0': 1 if self.as_bool(self['ignore_in_oebs']) else None
        }


PARAMETRIZE = pytest.mark.parametrize("params", (
    pytest.param(
        {
            'service': 610,
            'products': ['partner_payment'],
            'currencies': ['RUB', 'ILS'],
            'paysys_types': ['partner_payment', 'acc_bnpl', 'acc_return_item', 'acc_return_extra', 'acc_return_refund',
                             'acc_ya_compensation', 'acc_ds', 'acc_sberbank', 'acc_tinkoff_credit', 'acc_apple_pay',
                             'acc_google_pay'],
            'compl_source': YTSourceName.BLUE_MARKET_PAYMENT,
            'yt_path': YTDefaultPath.BLUE_MARKET_PAYMENT_YT_ROOT_PATH,
            'additional_datarow_kwargs': [
                dict(ignore_in_balance=True),
                # его нельзя последним, так как ожидаем, что он не заберётся и не выставится last_transaction_id
                dict(transaction_type=TransactionType.REFUND.name),
                # в yt строке нет orig_transaction_id - ожидаем, что выставится в null и потом не упадёт в обработке
                dict(ignore_in_oebs=True),  # ожидаем internal=1 (точнее extra_num_0)
                dict(ignore_in_oebs='no'),  # ожадиаем internal=None (extra_num_0)
            ]
        },
    ),
    pytest.param(
        {
            'service': 609,
            'products': ['subsidy', 'yandex_account_withdraw', 'ya_delivery_subsidy'],
            'currencies': ['RUB', 'ILS'],
            'paysys_types': ['yamarketplus', 'yamarket'],
            'compl_source': YTSourceName.BLUE_MARKET_SUBSIDY,
            'yt_path': YTDefaultPath.BLUE_MARKET_SUBSIDY_YT_ROOT_PATH,
            'additional_datarow_kwargs': [
                dict(ignore_in_balance=True),
                # его нельзя последним, так как ожидаем, что он не заберётся и не выставится last_transaction_id
                dict(transaction_type=TransactionType.REFUND.name),
                # в yt строке нет orig_transaction_id - ожидаем, что выставится в null и потом не упадёт в обработке
                dict(ignore_in_oebs='on'),  # ожидаем internal=1 (точнее extra_num_0)
                dict(ignore_in_oebs=False)  # ожадиаем internal=None (extra_num_0)
            ]
        },
    ),
    pytest.param(
        {
            'service': 1060,
            'products': ['sorting_return_reward', 'sorting_reward', 'storing_return_reward', 'storing_reward'],
            'currencies': ['RUB'],
            'paysys_types': ['sort_center'],
            'compl_source': YTSourceName.MARKET_COURIER_EXPENSES_SOURCE_NAME,
            'yt_path': YTDefaultPath.MARKET_COURIER_EXPENSES_YT_ROOT_PATH,
            'additional_datarow_kwargs': [
                dict(ignore_in_balance=True),
                # его нельзя последним, так как ожидаем, что он не заберётся и не выставится last_transaction_id
                dict(transaction_type=TransactionType.REFUND.name),
                # в yt строке нет orig_transaction_id - ожидаем, что выставится в null и потом не упадёт в обработке
                dict(ignore_in_oebs='on'),  # ожидаем internal=1 (точнее extra_num_0)
                dict(ignore_in_oebs=False)  # ожадиаем internal=None (extra_num_0)
            ]
        },
    ),
    pytest.param(
        {
            'service': 1100,
            'products': ['car_delivery', 'load_unload', 'truck_delivery'],
            'currencies': ['RUB'],
            'paysys_types': ['market-delivery'],
            'compl_source': YTSourceName.MARKET_COURIER_EXPENSES_SOURCE_NAME,
            'yt_path': YTDefaultPath.MARKET_COURIER_EXPENSES_YT_ROOT_PATH,
            'additional_datarow_kwargs': [
                dict(ignore_in_balance=True),
                # его нельзя последним, так как ожидаем, что он не заберётся и не выставится last_transaction_id
                dict(transaction_type=TransactionType.REFUND.name),
                # в yt строке нет orig_transaction_id - ожидаем, что выставится в null и потом не упадёт в обработке
                dict(ignore_in_oebs='on'),  # ожидаем internal=1 (точнее extra_num_0)
                dict(ignore_in_oebs=False)  # ожадиаем internal=None (extra_num_0)
            ]
        },
    ),
), ids=lambda p: '{}: {}'.format(p['service'], p['compl_source']))


@PARAMETRIZE
def test_default_processing_chain(params):
    yt_client = steps.YTSteps.create_yt_client()

    next_date = BASE_DATE
    cur_date = next_date - dt.timedelta(days=1)
    prev_date = cur_date - dt.timedelta(days=1)

    def mul_lens(*fields):
        return reduce(mul, (len(params[f]) for f in fields))

    appended_row_number = len(params.get('additional_datarow_kwargs', []))
    transaction_ids = steps.PartnerSteps.generate_subvention_transaction_log_ids(
        n=mul_lens('products', 'paysys_types', 'currencies') + appended_row_number)

    data_rows = [
        DataRow(transaction_id,
                service_transaction_id='123-item-{}{}'.format(product, paysys_type_cc),
                service_id=params['service'],
                amount=DEFAULT_AMOUNT * (1 + (D(i) / 10)),
                paysys_type_cc=paysys_type_cc,
                product=product,
                currency=currency)
        for transaction_id, i, (product, paysys_type_cc, currency) in zip(transaction_ids,
                                                                          itertools.count(0),
                                                                          itertools.product(params['products'],
                                                                                            params['paysys_types'],
                                                                                            params['currencies']))
    ]
    tr_id_iter = iter(transaction_ids[-appended_row_number:])
    data_rows.extend((
        DataRow(
            next(tr_id_iter),
            service_id=params['service'],
            **extra_row_kwargs
        )
        for extra_row_kwargs in params.get('additional_datarow_kwargs', [])
    ))

    steps.YTSteps.remove_tables(yt_client, [cur_date, next_date], params['yt_path'])
    data_for_yt = (data_row.yt_row for data_row in data_rows[:1])
    steps.YTSteps.fill_table(yt_client, prev_date, data_for_yt, params['yt_path'])

    with reporter.step(u"Пытаемся процессить ресурс prev_date, когда предыдущий (prev_date - 1 день) еще не закрыт"):
        steps.CommonPartnerSteps.create_partner_completions_resource(params['compl_source'], prev_date)
        steps.CommonPartnerSteps.create_partner_completions_resource(params['compl_source'],
                                                                     prev_date - dt.timedelta(days=1))
        with pytest.raises(xmlrpclib.Fault) as exc_info:
            steps.CommonPartnerSteps.process_partners_completions(params['compl_source'], prev_date)
        utils.check_that('Resource delayed: Waiting for processing of the resource for the previous date'
                         in exc_info.value.faultString, equal_to(True),
                         step=u'Проверим, что ресурс Delayed')

    with reporter.step(u"Закрываем предыдущий ресурс (prev_date - 1 день) и процессим текущий prev_date"):
        steps.CommonPartnerSteps.create_partner_completions_resource(params['compl_source'],
                                                                     prev_date - dt.timedelta(days=1),
                                                                     {'finished': dt.datetime.now()})
        steps.CommonPartnerSteps.process_partners_completions(params['compl_source'], prev_date)

        prev_pcr = steps.CommonPartnerSteps.get_partner_completions_resource(params['compl_source'], prev_date)
        utils.check_that(prev_pcr['additional_params'], equal_to({'last_transaction_id': transaction_ids[0]}))

        expected_data = (data_row.pps_row for data_row in data_rows[:1])
        inserted_payments = steps.PartnerSteps.get_partner_payment_stat_with_export(
            params['service'],
            transaction_ids[:1])
        utils.check_that(inserted_payments, contains_dicts_with_entries(expected_data),
                         step=u'Сравним полученные платежи с ожидаемыми')

    data_for_yt = (data_row.yt_row for data_row in data_rows[1:])
    steps.YTSteps.fill_table(yt_client, cur_date, data_for_yt, params['yt_path'])

    with reporter.step(u"Процессим ресурс prev_date, чтобы он закрылся"):
        steps.CommonPartnerSteps.process_partners_completions(params['compl_source'], prev_date)
        prev_pcr = steps.CommonPartnerSteps.get_partner_completions_resource(params['compl_source'], prev_date)
        utils.check_that(prev_pcr['additional_params'], equal_to({'last_transaction_id': transaction_ids[0],
                                                                  'finished': Anything()}))

    with reporter.step(u"Процессим ресурс cur_date"):
        steps.CommonPartnerSteps.create_partner_completions_resource(params['compl_source'], cur_date)
        steps.CommonPartnerSteps.process_partners_completions(params['compl_source'], cur_date)

        cur_pcr = steps.CommonPartnerSteps.get_partner_completions_resource(params['compl_source'], cur_date)
        utils.check_that(cur_pcr['additional_params'], equal_to({'last_transaction_id': transaction_ids[-1]}))

        expected_data = (data_row.pps_row for data_row in data_rows[1:] if not data_row['ignore_in_balance'])
        inserted_payments = steps.PartnerSteps.get_partner_payment_stat_with_export(
            params['service'],
            transaction_ids[1:])
        utils.check_that(inserted_payments, contains_dicts_with_entries(expected_data),
                         step=u'Сравним полученные платежи с ожидаемыми')


@PARAMETRIZE
def test_flags(params):
    yt_client = steps.YTSteps.create_yt_client()

    next_date = BASE_DATE - dt.timedelta(days=7)
    cur_date = next_date - dt.timedelta(days=1)
    prev_date = cur_date - dt.timedelta(days=1)

    transaction_ids = steps.PartnerSteps.generate_subvention_transaction_log_ids(n=3)
    tr_id_iter = iter(transaction_ids)
    # data_rows = [DataRow(next(tr_id_iter), service_transaction_id=uuid.uuid1().hex, amount=DEFAULT_AMOUNT * D('1.5'), product=params['products'][0], service_id=params['service'], paysys_type_cc=params['paysys_types'][0])]
    # data_rows.append(data_rows[0])
    # data
    data_rows = [
        DataRow(next(tr_id_iter), service_transaction_id=uuid.uuid1().hex, product=params['products'][0],
                service_id=params['service'], paysys_type_cc=params['paysys_types'][0]),
        DataRow(next(tr_id_iter), service_transaction_id=uuid.uuid1().hex, amount=DEFAULT_AMOUNT * D('1.5'),
                product=params['products'][0], service_id=params['service'], paysys_type_cc=params['paysys_types'][0]),
        DataRow(next(tr_id_iter), service_transaction_id=uuid.uuid1().hex, amount=DEFAULT_AMOUNT * D('0.5'),
                product=params['products'][0], service_id=params['service'], paysys_type_cc=params['paysys_types'][0],
                transaction_type=TransactionType.REFUND.name),
    ]

    steps.YTSteps.remove_tables(yt_client, [cur_date, next_date], params['yt_path'])
    # data_for_yt = (data_row.yt_row for data_row in data_rows[:1])
    data_for_yt = [data_rows[0].yt_row]
    steps.YTSteps.fill_table(yt_client, prev_date, data_for_yt, params['yt_path'])

    with reporter.step(u"Процессим prev_date c force=1"):
        steps.CommonPartnerSteps.create_partner_completions_resource(params['compl_source'], prev_date, {'force': 1})
        steps.CommonPartnerSteps.create_partner_completions_resource(params['compl_source'],
                                                                     prev_date - dt.timedelta(days=1))
        steps.CommonPartnerSteps.process_partners_completions(params['compl_source'], prev_date)

        prev_pcr = steps.CommonPartnerSteps.get_partner_completions_resource(params['compl_source'], prev_date)
        utils.check_that(prev_pcr['additional_params'],
                         equal_to({'last_transaction_id': transaction_ids[0], 'force': 0}))

        # expected_data = prepare_comparison_rows(compls[:1], get_table_name(prev_date))
        expected_data = [data_rows[0].pps_row]
        inserted_payments = steps.PartnerSteps.get_partner_payment_stat_with_export(
            params['service'],
            transaction_ids[:1])
        utils.check_that(inserted_payments, contains_dicts_with_entries(expected_data),
                         step=u'Сравним полученные платежи с ожидаемыми')

    # data_for_yt = [prepare_yt_row(**c) for c in compls[1:2]]
    data_for_yt = [data_rows[1].yt_row]
    steps.YTSteps.fill_table(yt_client, cur_date, data_for_yt, params['yt_path'])

    with reporter.step(u"Процессим ресурс prev_date, чтобы он закрылся"):
        steps.CommonPartnerSteps.create_partner_completions_resource(params['compl_source'], prev_date,
                                                                     {'force': 1,
                                                                      'skip_earlier_inserted_transactions': 1})
        steps.CommonPartnerSteps.process_partners_completions(params['compl_source'], prev_date)
        prev_pcr = steps.CommonPartnerSteps.get_partner_completions_resource(params['compl_source'], prev_date)
        utils.check_that(prev_pcr['additional_params'], equal_to({'last_transaction_id': transaction_ids[0],
                                                                  'finished': Anything(),
                                                                  'skip_earlier_inserted_transactions': 1,
                                                                  'force': 0}))

    with reporter.step(u"Процессим ресурс cur_date"):
        steps.CommonPartnerSteps.create_partner_completions_resource(params['compl_source'], cur_date)
        steps.CommonPartnerSteps.process_partners_completions(params['compl_source'], cur_date)

        cur_pcr = steps.CommonPartnerSteps.get_partner_completions_resource(params['compl_source'], cur_date)
        utils.check_that(cur_pcr['additional_params'], equal_to({'last_transaction_id': transaction_ids[1]}))

        # expected_data = prepare_comparison_rows(compls[1:2], get_table_name(cur_date))
        expected_data = [data_rows[1].pps_row]
        inserted_payments = steps.PartnerSteps.get_partner_payment_stat_with_export(
            params['service'],
            [transaction_ids[1]])
        utils.check_that(inserted_payments, contains_dicts_with_entries(expected_data),
                         step=u'Сравним полученные платежи с ожидаемыми')

    # data_for_yt = [prepare_yt_row(**c) for c in compls[1:]]
    data_for_yt = (data_row.yt_row for data_row in data_rows[1:])
    steps.YTSteps.fill_table(yt_client, cur_date, data_for_yt, params['yt_path'])

    with reporter.step(u"Процессим ресурс cur_date безначальной транзакции но с проверкой "
                       u"и пропуском ранее забранных транзакций"):
        steps.CommonPartnerSteps.create_partner_completions_resource(params['compl_source'], cur_date,
                                                                     {'skip_earlier_inserted_transactions': 1})
        steps.CommonPartnerSteps.process_partners_completions(params['compl_source'], cur_date)

        cur_pcr = steps.CommonPartnerSteps.get_partner_completions_resource(params['compl_source'], cur_date)
        utils.check_that(cur_pcr['additional_params'], equal_to(
            {'last_transaction_id': transaction_ids[2], 'skip_earlier_inserted_transactions': 1}))

        # expected_data = prepare_comparison_rows(compls[1:], get_table_name(cur_date))
        expected_data = (data_row.pps_row for data_row in data_rows[1:])

        inserted_payments = steps.PartnerSteps.get_partner_payment_stat_with_export(
            params['service'],
            transaction_ids[1:])
        utils.check_that(inserted_payments, contains_dicts_with_entries(expected_data),
                         step=u'Сравним полученные платежи с ожидаемыми')


def get_table_name(date):
    return dt.datetime.strftime(date, "%Y-%m-%d")


class Anything(object):
    def __eq__(self, other):
        return True
