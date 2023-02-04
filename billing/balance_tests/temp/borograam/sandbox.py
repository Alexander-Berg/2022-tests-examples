#!/usr/bin/env python
# coding=utf-8
# from StringIO import StringIO
import logging
import os
from collections import defaultdict
from contextlib import contextmanager
from decimal import Decimal as D
from functools import wraps, partial
from io import StringIO

import itertools
import requests
from datetime import datetime, timedelta
from dateutil.relativedelta import relativedelta
from enum import Enum
from typing import Iterator, Dict, Set, List, Tuple, Callable, Iterable, Any

import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.distribution.distribution_types import DistributionType
from balance.tests.payment.test_market_sidepayments import Context
from btestlib import utils
from btestlib.constants import YTSourceName, Export, ServiceCode, BlueMarketOrderType, Services, TransactionType, \
    NdsNew, Currencies
from btestlib.data import simpleapi_defaults
from btestlib.data.partner_contexts import BLUE_MARKET_SUBSIDY, PVZ_RU_CONTEXT_SPENDABLE, \
    DELIVERY_SERVICES_CONTEXT_SPENDABLE, BLACK_MARKET_1173_CONTEXT
from btestlib.matchers import contains_dicts_with_entries
from btestlib.utils import XmlRpc, TestsError
from simpleapi.common.payment_methods import Subsidy

TODAY = utils.Date.nullify_time_of_date(datetime.now())
YESTERDAY = TODAY - timedelta(days=1)
START_LAST_MONTH, END_LAST_MONTH = utils.Date.previous_month_first_and_last_days()
START_CUR_MONTH, END_CUR_MONTH = utils.Date.current_month_first_and_last_days()
# START_LAST_MONTH = END_LAST_MONTH + timedelta(days=1)

# CLIENT_ID = # 1351538678
# 1351538680

COLUMNS = ('client', 'person', 'contract', 'invoice', 'act')  # order matters

# context_609 = BLUE_MARKET_SUBSIDY
# context_610 = BLUE_MARKET_PAYMENTS
context_725 = PVZ_RU_CONTEXT_SPENDABLE
context_1100 = DELIVERY_SERVICES_CONTEXT_SPENDABLE

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)


# handlers = dict()  # type: # Dict[StringIO, logging.StreamHandler]


class TemporaryLoggerInterceptor(object):
    def __init__(self,
                 level=logging.INFO,
                 fmt='%(asctime)s - %(levelname)s - %(message)s',
                 datefmt='%Y-%m-%d %H:%M:%S'):
        self.stream = StringIO()
        self.handler = logging.StreamHandler(self.stream)
        self.handler.setLevel(level)
        formatter = logging.Formatter(fmt=fmt, datefmt=datefmt)
        self.handler.setFormatter(formatter)

    def link(self):
        logger.addHandler(self.handler)

    def unlink(self):
        logger.removeHandler(self.handler)

    @property
    def text(self):
        return self.stream.getvalue()

    def __enter__(self):
        # todo: patch print function here to call log entry?
        self.link()

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.unlink()
        print '\n'
        print self.text

    def __call__(self, func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            with self:
                func(*args, **kwargs)

        return wrapper


TLI = TemporaryLoggerInterceptor
full_logger_stream = TLI(level=logging.DEBUG)
full_logger_stream.link()


def get_ids_from_client(client_id):  # type: (int) -> Dict[str, Set[int]]
    sets = defaultdict(set)

    query = '''
    select cl.id as client, p.id as person, c.id as contract, a.id as act, i.id as invoice
    from t_client cl
    left join t_person p on p.CLIENT_ID=cl.id
    left join T_CONTRACT2 c on c.CLIENT_ID=cl.id
    left join t_act a on a.CLIENT_ID=cl.id
    left join t_invoice i on i.CLIENT_ID=cl.id
    where cl.id=:client_id'''
    result = db.balance().execute(  # type: List[Dict[str, int]]
        query,
        {'client_id': client_id})
    for row in result:
        for col in COLUMNS:
            val = row.get(col)
            if val:
                sets[col].add(val)

    sets['product'] = set(get_products_id_from_client_acts(client_id))
    return sets


def get_products_id_from_client_acts(client_id):  # type: (int) -> Iterator[int]

    query = """select distinct o.SERVICE_CODE
from t_act a
join t_act_trans at on at.ACT_ID=a.id
join t_consume c on c.id=at.CONSUME_ID
join t_order o on o.id=c.PARENT_ORDER_ID
where a.CLIENT_ID=:client_id"""
    return (e['service_code'] for e in db.balance().execute(query, dict(client_id=client_id)))


@TLI()
def export_client_and_linked(client_id):  # type: (int) -> None
    ids = get_ids_from_client(client_id)

    for col in COLUMNS:
        if col in ids:
            kwarg_field = '{}_id'.format(col)
            for obj in ids[col]:
                steps.ExportSteps.export_oebs(**{kwarg_field: obj})

    for k, v in ids.items():
        # print '{}: {}'.format(k, ', '.join(str(x) for x in v))
        logger.info(u'exported to oebs - %s: %s', k, ', '.join(str(x) for x in v))


def get_client_person_from_contract(contract_id):  # type: (int) -> Tuple[int, int]
    query = '''
    select client_id, person_id
    from BO.T_CONTRACT2
    where id=:contract_id
    '''
    with reporter.step(u"Получаем клиента и плательщика для договора {}".format(contract_id)):
        res = db.balance().execute(query, {'contract_id': contract_id})[0]
        cl, per = res['client_id'], res['person_id']
        reporter.attach(u'client: {}, person: {}'.format(cl, per))
        return cl, per


# def get_distribution_tag_id_from_contract(contract_id):
#     return db.balance().execute("""
#         select value_num as tag_id
#         from T_CONTRACT_ATTRIBUTES ca
#         join T_CONTRACT_COLLATERAL cc on cc.ATTRIBUTE_BATCH_ID=ca.ATTRIBUTE_BATCH_ID
#         where cc.CONTRACT2_ID=:contract_id and ca.code='DISTRIBUTION_TAG'""",
#                                 {'contract_id': contract_id})[0]['tag_id']


def distribution_act(contract_id):  # 4211257

    client_id, person_id = get_client_person_from_contract(contract_id)
    tag_id = steps.ContractSteps.get_attribute(contract_id, 'value_num', 'DISTRIBUTION_TAG')
    # tag_id = get_distribution_tag_id_from_contract(contract_id)
    parent_contract_id = steps.ContractSteps.get_attribute(contract_id, 'value_num', 'PARENT_CONTRACT_ID')

    # places_ids = steps.DistributionSteps.create_fixed_and_revshare_places(
    #     client_id,
    #     tag_id,
    #     DistributionType.VIDEO_HOSTING)
    places_ids, _ = steps.DistributionSteps.create_places(client_id, tag_id, [DistributionType.MARKET_CPC])

    steps.DistributionSteps.create_entity_completions(places_ids, YESTERDAY)
    steps.DistributionSteps.run_calculator_for_contract(contract_id)

    # api.test_balance().GeneratePartnerAct(group_contract_id, SECOND_START_DT)
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, YESTERDAY)
    # steps.CommonPartnerSteps.generate_partner_acts_fair(parent_contract_id, TODAY)

    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(parent_contract_id or contract_id)
    print partner_act_data


def generate_trust_payment(client_id, context, payment_type=None, paysys_type=None):
    service_product_id = steps.SimpleApi.create_service_product(context.service, client_id)
    service_product_fee_id = steps.SimpleApi.create_service_product(context.service, client_id, service_fee=1)

    service_order_id_list, trust_payment_id, _, payment_id = steps.SimpleApi.create_multiple_trust_payments(
        context.service,
        [service_product_id, service_product_fee_id],
        commission_category_list=[D(0), D(0)],
        prices_list=[D(1000), D(500)],
        paymethod=None
    )

    steps.CommonPartnerSteps.export_payment(payment_id)
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
        payment_id,
        TransactionType.PAYMENT
    )
    for p in payment_data:
        if p['internal'] != 1:
            steps.ExportSteps.export_oebs(transaction_id=p['id'])


def generate_context_side_payment(client_id, context, transaction_type=TransactionType.PAYMENT, payment_type=None,
                                  paysys_type=None, dt=None):
    if not payment_type:
        payment_type = context.tpt_payment_type
    if not paysys_type:
        paysys_type = context.tpt_paysys_type_cc
    pps_kwargs = context.pps_kwargs or {}
    return process_and_export_sidepayments([
        steps.PartnerSteps.create_sidepayment_transaction(
            client_id, utils.Date.moscow_offset_dt(dt), simpleapi_defaults.DEFAULT_PRICE, payment_type,
            context.service.id, paysys_type_cc=paysys_type, extra_str_0='fake-sidepayment',
            currency=context.currency, transaction_type=transaction_type, **pps_kwargs
        )
    ], transaction_type=transaction_type)


def generate_610_side_payments(client_id):
    temp_data = [
        {'paysys_type_cc': 'acc_sberbank', 'amount': D('1.0'), 'extra_str_0': 'payment-12'},
        {'paysys_type_cc': 'partner_payment', 'amount': D('0.9'), 'extra_str_0': 'payment-12'},
        {'paysys_type_cc': 'acc_bnpl', 'amount': D('1.0'), 'extra_str_0': 'payment-14_bnpl'},
        {'paysys_type_cc': 'acc_google_pay', 'amount': D('3.0'), 'extra_str_0': 'payment-16'},
    ]

    process_and_export_sidepayments(
        steps.PartnerSteps.create_sidepayment_transaction(
            client_id, utils.Date.moscow_offset_dt(), payment_type='partner_payment',
            service_id=610, payload='{}', extra_str_1='bank', **kw)
        for kw in temp_data
    )


def process_and_export_sidepayments(ids,
                                    transaction_type=TransactionType.PAYMENT):  # type: (Iterable[Tuple[str, str]]) -> None
    # exported_tpt = []
    for sp_id, trans_id in ids:
        steps.ExportSteps.create_export_record_and_export(
            sp_id, Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT)

        tpt_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
            sp_id, source='sidepayment', transaction_type=transaction_type)[0]
        tpt_id = tpt_data['id']

        steps.ExportSteps.export_oebs(transaction_id=tpt_id)
        logger.info(u'exported row `%s` (%s)', tpt_id, 'internal' if tpt_data['internal'] else 'oebs')
        # exported_tpt.append((tpt_id, tpt_data['internal']))

    # result = '\n'.join('row `{}`{}'.format(tpt_id, ': internal' if internal else '')
    #                    for tpt_id, internal in exported_tpt)
    # print result
    # return result


def check_new_612_product(context):
    client_id, _, contract_id, _ = create_client_person_contract(context)
    make_fake_612_completions_all_types(client_id, currency=context.currency)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(
        client_id, contract_id, TODAY)
    export_client_and_linked(client_id)


def make_fake_612_completions_all_types(client_id, amount_iter=None, dt=TODAY, currency=Currencies.RUB):
    amount_iter = amount_iter or (D(1) * 2 ** i for i in itertools.count())
    for k, v in BlueMarketOrderType.__dict__.items():
        if not k.startswith('__'):
            make_fake_612_completion(client_id, next(amount_iter), dt, v, currency=currency)


def make_fake_612_completion(client_id, amount=D(100), dt=TODAY, type_=BlueMarketOrderType.fee,
                             currency=Currencies.RUB):
    logger.debug(u'create 612 completion client=%s, dt=%s, type=%s, amount=%s, currency=%s',
                 client_id, dt, type_, amount, currency.iso_code)
    steps.PartnerSteps.create_fake_partner_stat_aggr_tlog_completion(dt,
                                                                     type_=type_,
                                                                     service_id=Services.BLUE_MARKET.id,
                                                                     client_id=client_id, amount=amount,
                                                                     last_transaction_id=0,
                                                                     currency=currency.iso_code)


def generate_blue_netting_oebs(contract_id, dt=END_LAST_MONTH, compl_amount=D(100), gen_compl_act=False, gen_act=False):
    # invoice_id, invoice_eid, _ = steps.InvoiceSteps.get_invoice_by_service_or_service_code(
    #     contract_id, service_code=ServiceCode.YANDEX_SERVICE
    # )
    client_id, person_id = get_client_person_from_contract(contract_id)

    # dt = END_LAST_MONTH
    if gen_compl_act:
        make_fake_612_completion(client_id, dt=dt, amount=compl_amount)
    if gen_act or gen_compl_act:
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, dt)

    logger.debug(u'start to calculate netting contract=%s', contract_id)
    steps.CommonSteps.export('PARTNER_PROCESSING', 'Contract', contract_id,
                             input_={'code': 'blue_market_netting', 'forced': True})
    rows = db.balance().execute(
        'select id from t_thirdparty_corrections where contract_id=:contract_id',
        dict(contract_id=contract_id)
    )
    for row in rows:
        steps.ExportSteps.export_oebs(correction_id=row['id'])
        logger.info(u'netting exported to oebs: %s', row['id'])


@contextmanager
def update_completion_source_url(source, url):
    select = 'select {} from {} where {}'
    update = 'update {} set {} where {}'
    where = "code=:source and queue='PARTNER_COMPL'"
    table = 't_completion_source'
    field = 'url'

    def upd_url(**kwargs):
        return db.balance().execute(update.format(table, '{}=:url'.format(field), where), kwargs)

    old_url = db.balance().execute(select.format(field, table, where), {'source': source})[0]['url']
    try:
        # print u'подменяем адрес в заборщике {}: {}'.format(source, url)
        logger.info(u'подменяем адрес в заборщике %s: %s', source, url)
        yield upd_url(source=source, url=url)
    finally:
        # print u'возращаем адрес в заборщике {}: {}'.format(source, old_url)
        logger.info(u'возращаем адрес в заборщике %s: %s', source, old_url)
        upd_url(source=source, url=old_url)


def chunkify(size, by_iterable_kwarg):  # type: (int, str) -> Callable
    def decorator(f):
        @wraps(f)
        def wrapper(*args, **kwargs):
            assert by_iterable_kwarg in kwargs, u'call {} with {} kwarg!'.format(f.__name__, by_iterable_kwarg)
            target = list(kwargs[by_iterable_kwarg])  # type: List[Any]
            new_kwargs = kwargs.copy()
            new_kwargs.pop(by_iterable_kwarg)
            part = partial(f, *args, **new_kwargs)

            return itertools.chain.from_iterable(
                part(**{
                    by_iterable_kwarg: target[i: i + size]
                })
                for i in range(0, len(target), size))

        return wrapper

    return decorator


@chunkify(1000, 'transaction_ids')
def get_pps_from_transaction_ids(service_ids, transaction_ids):  # type: (Iterable[int], Iterable[int]) -> Iterable[Any]
    return steps.PartnerSteps.get_partner_payment_stat_with_export(service_ids, transaction_ids)


@TLI()
def side_payments_from_tlog_to_oebs(url, source=YTSourceName.BLUE_MARKET_PAYMENT):
    config_path = url.split('//')[-1]

    yt_client = steps.YTSteps.create_yt_client()
    yt_data = steps.YTSteps.read_table(yt_client, '//' + config_path)
    transactions = [line['transaction_id'] for line in yt_data]
    services = set(line['service_id'] for line in yt_data)

    _split = config_path.split('/')
    dt = datetime.strptime(_split[-1], '%Y-%m-%d')
    config_path = '/'.join(_split[:-1] + ['%(start_dt)s'])

    steps.CommonPartnerSteps.create_partner_completions_resource(source, dt - timedelta(days=1),
                                                                 {'finished': datetime.now()})
    steps.CommonPartnerSteps.create_partner_completions_resource(source, dt)
    with update_completion_source_url(source, config_path):
        steps.CommonPartnerSteps.process_partners_completions(source, dt)
    # print u'забрал, вот partner_completion_resource:'
    # print steps.CommonPartnerSteps.get_partner_completions_resource(source, dt)
    logger.debug(u'забрали платежи, вот partner_completion_resource: %s',
                 steps.CommonPartnerSteps.get_partner_completions_resource(source, dt))

    # cl_per_con_export = dict()
    # cl_per_con_tpts_export = defaultdict(dict)
    # # side_payment_errors = dict()
    # cl_per_con_set = set()  # type: Set[Tuple[int, int, int]]

    export_called = defaultdict(dict)  # type: Dict[str, Dict[int, bool]]
    export_kwarg_in_tpt_map = {
        'client_id': 'partner_id',
        'person_id': 'person_id',
        'contract_id': 'contract_id',
        'transaction_id': 'id',
    }

    # забираем side payments
    # side_payments = steps.PartnerSteps.get_partner_payment_stat_with_export(
    #     services, transactions)
    side_payments = get_pps_from_transaction_ids(services, transaction_ids=transactions)

    def join_kwargs(kwargs):
        return u','.join(u'{}={}'.format(k, v) for k, v in kwargs.items())

    def make_kwargs(tpt):
        return dict(kwargs_field_generator(tpt))

    def kwargs_field_generator(tpt):
        for exp_key, tpt_key in export_kwarg_in_tpt_map.items():
            id_ = tpt[tpt_key]
            if id_ in export_called[exp_key]:
                if not export_called[exp_key][id_]:
                    raise TestsError('связанный объект {}={} ранее не смог выгрузиться'.format(exp_key, id_))
                continue
            yield exp_key, id_

    for side_payment in side_payments:
        # раскладываем в thirdparty_transactions
        try:
            logger.debug(u'пытаемся разложить side_payment id=%s на транзакцию', side_payment['id'])
            steps.ExportSteps.create_export_record_and_export(
                side_payment['id'], Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT,
                service_id=side_payment['service_id'],
                with_export_record=False
            )
        except XmlRpc.XmlRpcError as e:
            # side_payment_errors[side_payment['id']] = e.response
            logger.error(u'SidePayment %s THIRDPARTY_TRANS error: %s', side_payment['id'], e.response)
            continue

        # получаем tpt
        tpt_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
            side_payment['id'],
            transaction_type=None,
            source='sidepayment'
        )
        assert len(tpt_data) == 1
        tpt_data, = tpt_data
        tpt_id = tpt_data['id']
        logger.debug(u'SidePayment %s -> ThirdPartyTransaction %s', side_payment['id'], tpt_id)

        # запоминаем кто прописан в транзакции
        # client_id, person_id, contract_id, tpt_id = (
        #     tpt_data[key] for key in ('partner_id', 'person_id', 'contract_id', 'id'))
        # cl_per_con_set.add((client_id, person_id, contract_id))
        exp_kwargs, exported = {}, False
        try:
            logger.debug(u'пытаемся выгрузить в оебс tpt=%s', tpt_id)
            exp_kwargs = make_kwargs(tpt_data)
            steps.ExportSteps.export_oebs(**exp_kwargs)
            exported = True
            logger.info(u'exported to oebs: %s', join_kwargs(exp_kwargs))
        except (TestsError, XmlRpc.XmlRpcError) as e:
            if isinstance(e, TestsError):
                error = str(e).decode('utf-8')
            else:
                error = e.response
            logger.error(u'ThirdPartyTransaction %s, export of %s error: %s', tpt_id, exp_kwargs, error)
        finally:
            for exp_key, id_ in exp_kwargs.items():
                export_called[exp_key][id_] = exported
        # key = (client_id, person_id, contract_id)
        # cl_per_con_export[key] = u''
        # cl_per_con_tpts_export[key][tpt_id] = u''

    # for key, tpt_dict in cl_per_con_tpts_export.items():
    #     client, person, contract = key
    #
    #     try:
    #         steps.ExportSteps.export_oebs(
    #             client_id=client,
    #             person_id=person,
    #             contract_id=contract
    #         )
    #     except TestsError as e:
    #         error = unicode(e)
    #         cl_per_con_export[key] = error
    #         error = u'doesnt exported because of {}'.format(error)
    #         for tpt_id in tpt_dict:
    #             tpt_dict[tpt_id] = error
    #         continue
    #
    #     for tpt_id in tpt_dict:
    #         try:
    #             steps.ExportSteps.export_oebs(transaction_id=tpt_id)
    #         except XmlRpc.XmlRpcError as e:
    #             tpt_dict[tpt_id] = u'Error {}'.format(e.response)
    #
    # for (client, person, contract), tpt_dict in cl_per_con_tpts_export.items():
    #     error = cl_per_con_export[(client, person, contract)] or u'OK'
    #
    #     print 'contract: `{}`, client: `{}`, person: `{}`: {}'.format(
    #         contract, client, person, error)
    #     for tpt_id, error in tpt_dict.items():
    #         print u'tpt row: `{}` {}'.format(tpt_id, error)

    # for sp_id, error in side_payment_errors.items():
    #     print 'SIDE PAYMENT {} THIRDPARTY_TRANS ERROR: {}'.format(sp_id, error)


def create_client_person_contract(context, client_id=None, person_id=None, export=False, return_dict=False,
                                  partner_integration_params=None, dt=START_LAST_MONTH, **additional_params):
    flds = ('client_id', 'person_id', 'contract_id')
    additional_params['start_dt'] = dt
    output = steps.ContractSteps.create_partner_contract(
        context,
        client_id=client_id,
        person_id=person_id,
        additional_params=additional_params,
        partner_integration_params=partner_integration_params
    )[:3]
    kwargs = {k: v for k, v in zip(flds, output)}
    logger.debug(u'created: %s', kwargs)
    if export:
        steps.ExportSteps.export_oebs(**kwargs)
        logger.info(u'exported to oebs: %s', kwargs)
        # print u'exported to oebs: {}'.format(kwargs)
    if return_dict:
        return kwargs
    return output


class Tpt(dict):
    @staticmethod
    def price_gen_factory():
        for i in itertools.count():
            yield D('1.01') * 2 ** i

    price_gen = price_gen_factory.__get__(1, int)()

    @classmethod
    def clear_price_gen(cls):
        cls.price_gen = cls.price_gen_factory()

    def __init__(self, payment_type, transaction_type=TransactionType.PAYMENT, internal=None, amount=None):
        super(Tpt, self).__init__()
        self['payment_type'] = payment_type
        self['transaction_type'] = transaction_type
        self['internal'] = internal
        self['paysys_type_cc'] = 'yamarketplus' if 'withdraw' in payment_type else 'yamarket'
        self['amount'] = amount or next(self.price_gen)


def old_generation_609_emulation():
    context = BLUE_MARKET_SUBSIDY
    nds = NdsNew.DEFAULT
    # month1, _, month2, _ = utils.Date.previous_two_months_dates()
    month1, _ = utils.Date.previous_month_first_and_last_days()
    month2, _ = utils.Date.current_month_first_and_last_days()
    client_id, person_id, contract_id, contract_eid = steps.ContractSteps.create_partner_contract(
        context,
        additional_params=dict(start_dt=month1, nds=nds.nds_id)
    )

    pages = {
        'delivery_subsidy': dict(page_id=60903, description=u'Синий маркет. Субсидии доставки'),
        'subsidy': dict(page_id=10901, description=u'Синий маркет. Субсидии'),
        'yandex_account_withdraw': dict(page_id=60902, description=u'Синий маркет. Плюс 2.0'),
        'acc_subsidy': dict(page_id=60904, description=u'Синий маркет. Субсидии (УВ)'),
        'acc_delivery_subsidy': dict(page_id=60906, description=u'Синий маркет. Субсидии доставки (УВ)'),
        'acc_ya_withdraw': dict(page_id=60905, description=u'Синий маркет. Плюс 2.0 (УВ)'),
    }

    def get_reward(sum, nds, dt):
        return utils.dround(utils.dround2(sum) / nds.koef_on_dt(dt), 5)

    payments = {
        month1: [
            Tpt('delivery_subsidy'), Tpt('subsidy'), Tpt('yandex_account_withdraw'),
            # Tpt('acc_subsidy'), Tpt('acc_delivery_subsidy'), Tpt('acc_ya_withdraw'),
        ],
        month2: [
            Tpt('delivery_subsidy'), Tpt('subsidy'), Tpt('yandex_account_withdraw'),

            Tpt('acc_subsidy', transaction_type=TransactionType.REFUND),
            Tpt('acc_delivery_subsidy', transaction_type=TransactionType.REFUND),
            Tpt('acc_ya_withdraw', transaction_type=TransactionType.REFUND),

            Tpt('acc_subsidy', internal=1), Tpt('acc_delivery_subsidy', internal=1), Tpt('acc_ya_withdraw', internal=1),

            Tpt('pay_subsidy'), Tpt('pay_ya_withdraw'), Tpt('pay_delivery_subsidy'),
        ]
    }

    sums = defaultdict(lambda: defaultdict(D))
    for dt, fake_data in payments.items():
        steps.SimpleApi.create_fake_tpt_data(context, client_id, person_id, contract_id, dt, fake_data)
        for data in fake_data:
            if data['payment_type'] in pages:
                sums[dt][data['payment_type']] += data['amount'] * data['transaction_type'].sign
        steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, dt)

    act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)
    print act_data

    expected_data = list(
        steps.CommonData.create_expected_pad(
            context, client_id, contract_id, dt, partner_reward=get_reward(sum, nds, dt), nds=nds, **pages[pay_type]
        )
        for dt, pay_type_sums in sums.items()
        for pay_type, sum in pay_type_sums.items()
    )

    utils.check_that(act_data, contains_dicts_with_entries(expected_data),
                     step=u'Сравниваем данные в partner_act_data с ожидаемыми')

    export_client_and_linked(client_id)
    tpts = db.balance().execute('''
        select id from t_thirdparty_transactions where contract_id=:contract_id
        ''',
                                {'contract_id': contract_id})
    for tpt in tpts:
        steps.ExportSteps.export_oebs(transaction_id=tpt['id'])

    # for date, products in payments.items():
    #     for product in products:
    #         create_and_process_tlog_row(product, date)


def migration_609_emulation():
    types = {
        'acc_subsidy': 'subsidy',
        'acc_ya_withdraw': 'yandex_account_withdraw',
        'acc_delivery_subsidy': 'delivery_subsidy',
    }

    def tick(gen_iterable):
        for gen in gen_iterable:
            try:
                yield next(gen)
            except StopIteration:
                yield None

    generators = [move_609_old_tpts_between_pages(old, new) for old, new in types.items()]
    input(u'Иди выполняй sql, уточни контракт по in ({}). Готово? Жми 1 и enter'.format(
        ', '.join(str(contract) for contract in tick(generators))))
    print(list(tick(generators)))


def move_609_old_tpts_between_pages(from_payment_type='acc_subsidy', to_payment_type='subsidy'):
    context = BLUE_MARKET_SUBSIDY
    nds = NdsNew.DEFAULT
    # month1, _, month2, _, month3, _ = utils.Date.previous_three_months_start_end_dates()
    # month1, _, month2, _ = utils.Date.previous_two_months_dates()
    month4, _ = utils.Date.current_month_first_and_last_days()
    month1, month2, month3 = (month4 + relativedelta(months=i) for i in range(-3, 0))
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params=dict(start_dt=month1, nds=nds.nds_id)
    )
    Tpt.clear_price_gen()

    def create_and_process_trust_row(payment_type, date):
        product_id = steps.SimpleNewApi.create_product(context.service.id, client_id)
        _, _, _, payment_id = steps.SimpleApi.create_trust_payment(
            context.service, product_id, paymethod=Subsidy(), price=next(Tpt.price_gen))

        steps.CommonPartnerSteps.export_payment(payment_id)

        payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
            payment_id, TransactionType.PAYMENT)[0]
        return payment_id, payment_data['id']

    def create_fake_row(payment_type, date, **kwargs):
        steps.SimpleApi.create_fake_tpt_data(
            context, client_id, person_id, contract_id, date, [Tpt(payment_type, **kwargs)])

    def create_and_process_tlog_row(payment_type, date, internal=None):
        side_payment_id, transaction_id_payment = steps.PartnerSteps.create_sidepayment_transaction(
            client_id, date, next(Tpt.price_gen), payment_type,
            context.service.id, payload='{}', paysys_type_cc='yamarket', extra_str_0='wrong-609',
            extra_str_1='bank', extra_num_0=internal)
        steps.ExportSteps.create_export_record_and_export(
            side_payment_id, Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT,
            service_id=context.service.id)
        data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
            side_payment_id,
            transaction_type=None,
            source='sidepayment'
        )
        return side_payment_id, data[0]['id']

    def change_pps_payment_type(pps_id, new_payment_type, export=False):
        query = """
        update bo.t_partner_payment_stat pps
        set pps.payment_type=:new_payment_type
        where pps.id=:pps_id
        """
        db.balance().execute(query, {'pps_id': pps_id, 'new_payment_type': new_payment_type})

        if export:
            steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, pps_id)

    def change_tpt_payment_type(tpt_id, new_payment_type, correction=False):
        table_name = 'T_THIRDPARTY_CORRECTIONS' if correction else 'T_THIRDPARTY_TRANSACTIONS'
        query = """
        update bo.{}
        set PAYMENT_TYPE=:new_payment_type
        where id=:tpt_id""".format(table_name)
        db.balance().execute(query, {'tpt_id': tpt_id, 'new_payment_type': new_payment_type})

    def get_corrections(**filters):
        query = u"""
        select * from t_thirdparty_corrections where {}""".format(
            u' and '.join(u'{0}=:{0}'.format(k) for k in filters.keys())
        )
        return db.balance().execute(query, filters)

    subsidy = to_payment_type
    acc_subsidy = from_payment_type

    create_fake_row(subsidy, month1)  # 1.01
    pps_m1, tpt_m1 = create_and_process_tlog_row(subsidy, month1)  # 2.02
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, month1)  # 3.03 old page
    # old migration
    change_pps_payment_type(pps_m1, acc_subsidy)
    change_tpt_payment_type(tpt_m1, acc_subsidy)
    steps.SimpleApi.create_fake_thirdparty_payment(
        simpleapi_defaults.ThirdPartyData.BLUE_MARKET_SUBSIDY, contract_id, person_id, client_id, D('2.02'),
        payment_type=subsidy, is_correction=True, dt=month2)
    steps.SimpleApi.create_fake_thirdparty_payment(
        simpleapi_defaults.ThirdPartyData.BLUE_MARKET_SUBSIDY, contract_id, person_id, client_id, D('2.02'),
        payment_type=acc_subsidy, is_correction=True, dt=month2, transaction_type=TransactionType.REFUND)
    # second month payments
    create_fake_row(subsidy, month2, transaction_type=TransactionType.REFUND)  # -4.04
    pps_m2, tpt_m2 = create_and_process_tlog_row(acc_subsidy, month2)  # 8.08
    change_tpt_payment_type(tpt_m2, acc_subsidy)
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, month2)  # 8.08 new page
    # current month
    pps_m3, tpt_m3 = create_and_process_tlog_row(acc_subsidy, month3)  # 16.16
    change_tpt_payment_type(tpt_m3, acc_subsidy)
    # release was here
    pps_m31, tpt_m31 = create_and_process_tlog_row(acc_subsidy, month3)  # 32.32

    # input('Иди руками в бд запускай всякое для клиента {}, contract {}. Готово? Жми 1 и enter'.format(
    #     client_id, contract_id))
    # yield contract_id
    # second migration
    change_tpt_payment_type(
        int(get_corrections(contract_id=contract_id, payment_type=acc_subsidy)[0]['id']),
        subsidy, correction=True
    )
    change_tpt_payment_type(tpt_m1, subsidy)
    change_tpt_payment_type(tpt_m2, subsidy)
    change_tpt_payment_type(tpt_m3, subsidy)

    steps.SimpleApi.create_fake_thirdparty_payment(
        simpleapi_defaults.ThirdPartyData.BLUE_MARKET_SUBSIDY, contract_id, person_id, client_id, D('8.08'),
        payment_type=acc_subsidy, is_correction=True, dt=month3)
    steps.SimpleApi.create_fake_thirdparty_payment(
        simpleapi_defaults.ThirdPartyData.BLUE_MARKET_SUBSIDY, contract_id, person_id, client_id, D('8.08'),
        payment_type=subsidy, is_correction=True, dt=month3, transaction_type=TransactionType.REFUND)

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, month3)
    # print u'проверяй, что у контракта {} корректировки выставились в 8.08, акт за последний месяц на 37.0333 без ндс (с ндс 44.44)'.format(contract_id)

    pps_m4, tpt_m4 = create_and_process_tlog_row(acc_subsidy, month4)  # 64.64
    yield contract_id
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, month4)
    print u'ожидаем у контракта {} акт на 53.8666 и отсутствие как актов, так и корректировок в acc_*'

    def hide_november_code():
        # this test has different amounts in accs and pays to control sums
        # trust and tlog payment in first month
        create_fake_row('subsidy', month1)  # 1.11
        pps_tlog_old, _ = create_and_process_tlog_row('subsidy', month1)  # 2.22
        steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, month1)  # 3.33

        # trust and tlog payments in second month BEFORE adding new types
        create_fake_row('subsidy', month2)  # 4.44
        pps_tlog_new, _ = create_and_process_tlog_row('subsidy', month2)  # 8.88 (no)
        # new types added
        create_and_process_tlog_row('acc_subsidy', month2, internal=1)  # 17.76
        create_and_process_tlog_row('pay_subsidy', month2)  # 35.52 (no)

        # MIGRATION
        # market billing part
        # new_acc_amount = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
        #     pps_tlog_new, TransactionType.PAYMENT, source='sidepayment')[0]['amount']
        # create_fake_row('acc_subsidy', month2, amount=new_acc_amount)
        create_and_process_tlog_row('acc_subsidy', month2, internal=1)  # 71.04
        # balance part
        # старые нельзя просто перетаскивать, надо создать корректировки на перенос

        input('Иди руками в бд создавай корректировки и апдейть payment_type у клиента {}. Готово? Жми enter'.format(
            client_id))

        change_pps_payment_type(pps_tlog_old, 'acc_subsidy')
        change_pps_payment_type(pps_tlog_new, 'pay_subsidy')

        steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, month2)  # 4.44 and 88.8


@TLI()
def israel_full_test(dt=START_LAST_MONTH):
    context610pay = Context.get(610, currency='ILS')
    context609pay = Context.get(609, currency='ILS')
    context610acc = Context.get(610, currency='ILS', tlog_type='acc')
    context609acc = Context.get(609, currency='ILS', tlog_type='acc')
    contexts = (context610pay, context609pay, context610acc, context609acc)

    client, person610, contract610 = create_client_person_contract(context610acc, dt=dt)
    _, person609, contract609 = create_client_person_contract(context609acc, client_id=client, dt=dt)
    # tr_id = itertools.count(5)

    # 612 открутки + акт
    make_fake_612_completions_all_types(client, dt=dt, currency=Currencies.ILS)
    # for type_ in (BlueMarketOrderType.global_delivery, BlueMarketOrderType.global_fee,
    #               BlueMarketOrderType.global_agency_commission):
    #     steps.PartnerSteps.create_fake_partner_stat_aggr_tlog_completion(
    #         START_LAST_MONTH,
    #         type_=type_,
    #         service_id=Services.BLUE_MARKET.id,
    #         client_id=client, amount=D('12.34'),
    #         last_transaction_id=next(tr_id),
    #         currency='ILS')
    logger.debug(u'try to generate 612 act - client=%s, contract=%s, dt=%s', client, contract610, dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client, contract610, dt)

    export_client_and_linked(client)
    # взаимозачёт
    generate_blue_netting_oebs(contract610, dt=dt)

    # 609/610 транзакции
    # returns = []
    for context in contexts:
        # pps_kwargs = {
        #     'payload': '{}',
        #     'paysys_type_cc': context.tpt_paysys_type_cc,
        #     'extra_str_0': '123-item-987',
        #     'extra_str_1': 'bank',
        #     'currency': context.currency,
        # }
        # side_payment_id, transaction_id_payment = steps.PartnerSteps.create_sidepayment_transaction(
        #     client, utils.Date.moscow_offset_dt(), simpleapi_defaults.DEFAULT_PRICE, context.tpt_payment_type, context.service.id,
        #     **pps_kwargs
        # )
        # steps.ExportSteps.create_export_record_and_export(
        #     side_payment_id, Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT,
        #     service_id=context.service.id)
        # вместо этого всего, можно это дёрнуть, но оно будет сразу в оебс прогружать по ходу создания (а не постфактум)
        logger.debug(u'try to create %s payment_type="%s", paysys_type_cc="%s"',
                     context.service, context.tpt_payment_type, context.tpt_paysys_type_cc)
        # print mes
        # returns.append(mes)
        # returns.append(
        generate_context_side_payment(client, context, dt=dt)  # try except?
        # )

    # print '\n'.join(returns)
    # def get_tpt_ids(contract):
    #     query = """select * from t_thirdparty_transactions where contract_id=:contract_id"""
    #     return (tpt['id'] for tpt in db.balance().execute(query, {'contract_id': contract}))

    # tpt_ids = list(get_tpt_ids(contract610))
    # tpt_ids.extend(get_tpt_ids(contract609))
    # пока что не выгружаю в оебс транзакции из списка

    # 609 заголовки
    logger.debug(u'try to create 609 partner acts, contract=%s, dt=%s', contract609, dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract609, dt)
    logger.info(u'609 acts on meta: `select * from partner_billing.T_PARTNER_ACT_DATA where PARTNER_CONTRACT_ID=%s;`',
                contract609)
    # отрефрешить матвьюху на метабазе?

    # for tpt in tpt_ids:
    #     steps.ExportSteps.export_oebs(transaction_id=tpt)


def export_payment_to_oebs(payment_id):
    tpts = db.balance().execute(
        'select * from BO.T_THIRDPARTY_TRANSACTIONS where payment_id=:payment_id',
        {'payment_id': payment_id}
    )
    for cl in {tpt['partner_id'] for tpt in tpts}:
        export_client_and_linked(cl)

    tpt_ids = [tpt['id'] for tpt in tpts]
    for tpt_id in tpt_ids:
        steps.ExportSteps.export_oebs(transaction_id=tpt_id)

    return tpt_ids


def create_black_market_client_person_contract():
    return create_client_person_contract(
        BLACK_MARKET_1173_CONTEXT,
        partner_integration_params={
            'link_integration_to_client': 1,
            'link_integration_to_client_args': {
                'integration_cc': 'market_business',
                'configuration_cc': 'market_business_default',
            },
            'set_integration_to_contract': 1,
            'set_integration_to_contract_params': {
                'integration_cc': 'market_business'
            }
        },
        export=True
    )


def get_simpleapi_defaults_thirdpartydata(context, payment_type=None, paysys_type_cc=None, commission_pct=D(0)):
    class Test(Enum):
        DYNAMIC = simpleapi_defaults._ThirdPartyData(
            service=context.service,
            firm=context.firm,
            currency=context.currency,
            contract_currency=context.currency,
            commission_pct=commission_pct,
            payment_type=payment_type or context.tpt_payment_type,
            paysys_type=paysys_type_cc or context.tpt_paysys_type_cc
        )

        def __init__(self, service, firm, currency, contract_currency, commission_pct, payment_type, paysys_type):
            self.service = service
            self.firm = firm
            self.currency = currency
            self.contract_currency = contract_currency
            self.commission_pct = commission_pct
            self.payment_type = payment_type
            self.paysys_type = paysys_type

    return Test.DYNAMIC


@TLI()
def compensations_in_tlog(amount=D(100)):
    context_610 = Context.get(610).new(
        tpt_payment_type='compensation',
        tpt_paysys_type_cc='yandex',
    )
    client, person, contract610 = create_client_person_contract(context_610)
    invoice_id, invoice_eid, _ = steps.InvoiceSteps.get_invoice_by_service_or_service_code(
        contract610, service_code=ServiceCode.AGENT_REWARD
    )
    export_client_and_linked(client)

    # старая корректировка от саппортов
    steps.SimpleApi.create_fake_thirdparty_payment(
        get_simpleapi_defaults_thirdpartydata(context_610), contract610, person, client, amount,
        is_correction=True,
        # invoice_eid=invoice_eid,
        payout_ready_dt=datetime.today()
    )
    tpt_id = db.balance().execute(
        'select id from t_thirdparty_corrections where contract_id=:contract', dict(contract=contract610)
    )[0]['id']
    # корректирую некоторые поля в соответствии с тем как это делалось скриптом: не все поля можно было задать через create_fake_thirdparty_payment
    db.balance().execute(
        'update t_thirdparty_corrections set register_id=null, total_sum=:amount where id=:id',
        dict(amount=amount, id=tpt_id)
    )
    steps.ExportSteps.export_oebs(correction_id=tpt_id)
    logger.info(u'exported row `%s` - old "like support"', tpt_id)

    # платёж в thirdparty_transaction из тлога
    try:
        generate_context_side_payment(client, context_610)
    except Exception:
        logger.error(u'НЕ ПРОГРУЗИЛСЯ')
        # не прогрузился. Кажется, для транзакций с paysys_type_cc='yandex' необходимо обязательное заполнение invoice_eid, которое у нас никак не заполняется в цепочке
        # плюс вопрос ещё с payout_ready_dt

    logger.info(u'select * from v_thirdparty_transactions where contract_id=%s', contract610)


def bad_contract_cancelling():
    c = Context.get(610, currency='ILS')
    client, person, contract_offer = create_client_person_contract(c, dt=datetime(2021, 12, 1))
    _, _, contract_not_offer = create_client_person_contract(c.new(is_offer=0), client, person,
                                                             dt=datetime(2021, 12, 1))
    input('иди в админку и закрывай оферту {}'.format(contract_offer))
    make_fake_612_completions_all_types(client, currency=Currencies.ILS)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client, contract_offer, TODAY, manual_export=False)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client, contract_not_offer, TODAY)


def fetch_current_oebs_clone():
    print requests.get('https://oebsapi-billing.testing.oebs.yandex-team.ru/oebsapi/admin/rest/ping', verify=False).text
    # r = requests.get('https://shd.testing.oebs.yandex-team.ru/api/statusBillingApi', verify=False)
    clones = (
        clone
        for line in requests.get('https://shd.testing.oebs.yandex-team.ru/api/statusBillingApi', verify=False).json()
        for clone, status in line.items()
        if status
    )
    for clone in clones:
        print u'old: {}'.format(clone)
    else:
        print u'NO OLD EXPORT!'


# 613 провожу
# steps.CommonSteps.export('THIRDPARTY_TRANS', 'Payment', 6619053838)
# steps.CommonSteps.export('CASH_REGISTER', 'Payment', 6619053838)
# from notifier import data_objects as notifier_objects
# notifier_objects.BaseInfo.get_notification_info(session, 80, 6619053838)
def process_613_payment(payment_id):
    from btestlib.local_config import TEST_TVM_MEDIUM_SECRET
    steps.CommonSteps.export('THIRDPARTY_TRANS', 'Payment', payment_id)
    steps.CommonSteps.export('CASH_REGISTER', 'Payment', payment_id)

    tvm_ticket = os.popen(
        "echo '{}' | ya tool tvmknife get_service_ticket client_credentials -s 2000601 -d 2000447".format(
            TEST_TVM_MEDIUM_SECRET)
    ).read().strip()
    print tvm_ticket
    # todo: call checkouter notification, get json from darkspirit, get check printform (need proxy)


if __name__ == '__main__':  # надо бы разгрести всё закоменченное по функциям
    fetch_current_oebs_clone()
    # side_payments_from_tlog_to_oebs(
    #     'https://yt.yandex-team.ru/hahn/navigation?path=//home/market/testing/billing/tlog/payouts/payments/2021-08-11',
    #     services=[610], source=YTSourceName.BLUE_MARKET_PAYMENT
    # )

    # for cl in (1352156814,):
    #     export_client_and_linked(cl)

    # steps.ExportSteps.export_oebs(contract_id=3959510)

    # for tpt_id in (22413748180, 22413748190, 22413848820, 22413848830):
    #     steps.ExportSteps.export_oebs(transaction_id=tpt_id)

    # generate_610_side_payments(1352156814)

    # generate_blue_netting(3959510)

    # for product in (512790, 512792, 512793,512791,512789):
    #     steps.ExportSteps.export_oebs(product_id=product)

    # distribution_act(
    #     # 4211257
    #     # 4250066
    #     4250067
    # )
    # for contract in (4211189,):
    #     cl, per = get_client_person_from_contract(contract)
    #     steps.ExportSteps.export_oebs(contract_id=contract, client_id=cl, person_id=per)

    # create_client_person_contract(context_609, client_id=1352156814)
    # generate_context_side_payment(1352156814, context_609)
    # create_client_person_contract(context_725, client_id=1352156814, person_id=16371786)
    # create_client_person_contract(context_1100, client_id=1352156814, person_id=16371786)
    # steps.CommonIntegrationSteps.link_integration_configuration_to_client(client_id=1352156814, integration_cc='market_logistics_partner',configuration_cc='market_logistics_partner_delivery_services_conf')
    # generate_context_side_payment(1352156814, context_1100, payment_type=PaymentType.CAR_DELIVERY)

    # steps.CommonPartnerSteps.export_payment(6604674412)

    # create_client_person_contract(context_610)
    # create_client_person_contract(context_609, client_id=1352341742)

    # side_payments_from_tlog_to_oebs(
    #     'https://yt.yandex-team.ru/hahn/navigation?path=//home/market/testing/billing/tlog/payouts/payments/2021-11-09',
    #     source=YTSourceName.BLUE_MARKET_PAYMENT
    # )
    # side_payments_from_tlog_to_oebs(
    #     'https://yt.yandex-team.ru/hahn/navigation?path=//home/market/testing/billing/tlog/payouts/expenses/2021-11-09',
    #     source=YTSourceName.BLUE_MARKET_SUBSIDY
    # )
    # generate_blue_netting(4067537)
    # export_client_and_linked(1352414989)
    # steps.ExportSteps.export_oebs(correction_id=92566271609)
    # steps.CommonPartnerSteps.generate_partner_acts_fair(4067538, TODAY)
    # print steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(4067538)

    # d = create_client_person_contract(context_610)
    # generate_blue_netting(d['contract_id'])
    # generate_trust_payment(1351834252, context_610)
    # print steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
    #     6605048704,
    #     TransactionType.PAYMENT
    # )
    # export_client_and_linked(1352341742)
    # export_client_and_linked(1351834252)
    # steps.ExportSteps.export_oebs(transaction_id=92566498519)
    # steps.ExportSteps.export_oebs(transaction_id=92566470219)
    # generate_blue_netting(3792767)
    # steps.ExportSteps.export_oebs(correction_id=92566485659)
    # steps.ExportSteps.create_export_record_and_export(6606572548, 'THIRDPARTY_TRANS', 'Payment', with_export_record=False)
    # ids = []
    # for p_id in (6606918699, 6606920031, 6606920184):
    #     ids.extend(export_payment_to_oebs(p_id))
    # for id in ids:
    #     print(id)
    # export_payment_to_oebs(6607463602)
    # steps.ExportSteps.export_oebs(transaction_id=193606828629)
    # generation_609_emulation()
    # steps.ExportSteps.export_oebs(product_id=513087)
    # test_new_612_product()
    # move_609_old_tpts_between_pages()

    # client_id = steps.client_steps.ClientSteps.create()
    # steps.integration_steps.CommonIntegrationSteps.link_integration_configuration_to_client(
    #     'blue_subsidy', 'blue_subsidy_common', client_id
    # )
    # steps.integration_steps.CommonIntegrationSteps.link_integration_configuration_to_client('magistrali', 'magistrali_expidition', client_id)
    # person_id = steps.person_steps.PersonSteps.create(1354168437, 'ur', {'is-partner': 0},
    #                                                   inn_type=person_defaults.InnType.RANDOM, full=False)
    # export_client_and_linked(1354778332)
    # steps.ExportSteps.export_oebs(transaction_id=23567275430)
    # generate_context_side_payment(1354778332, BLUE_MARKET_612_ISRAEL, 'partner_payment', 'partner_payment')
    # export_client_and_linked(1354778337)
    # steps.ExportSteps.export_oebs(transaction_id=23567275470)
    # generate_context_side_payment(1354778337, BLUE_MARKET_SUBSIDY_ISRAEL, 'partner_payment', 'partner_payment')
    # side_payments_from_tlog_to_oebs('https://yt.yandex-team.ru/hahn/navigation?sort=asc-false,field-name&path=//home/market/testing/billing/tlog/payouts/payments/2021-12-14')

    # cpc = create_client_person_contract(Context.get(610, currency='ILS'), return_dict=True, export=True)
    # cpc2 = create_client_person_contract(Context.get(609, currency='ILS'), client_id=cpc['client_id'], return_dict=True, export=True)
    # print cpc, cpc2

    # {'person_id': 19152769, 'contract_id': 15425716, 'client_id': 1355004425}
    # {'person_id': 19152793, 'contract_id': 15425729, 'client_id': 1355004425}

    # steps.ExportSteps.export_oebs(
    #     # client_id=1355085920,
    #     # person_id=19209526,
    #     contract_id=15473999
    # )

    # cl_arr = [steps.ClientSteps.create() for _ in range(20)]
    # for client_id in cl_arr:
    #     steps.CommonIntegrationSteps.link_integration_configuration_to_client(
    #         'market_business', 'market_business_default', client_id)
    #     steps.PersonSteps.create(client_id, 'ur',
    #                              {'is-partner': '0'},
    #                              inn_type=person_defaults.InnType.RANDOM,
    #                              full=False)
    # print cl_arr

    # steps.ExportSteps.export_oebs(invoice_id=147092644)
    # steps.ExportSteps.export_oebs(transaction_id=23567663470)
    # steps.ExportSteps.export_oebs(product_id=513076)
    # compensations_in_tlog()create_client_person_contract()
