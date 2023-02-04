# -*- coding: utf-8 -*-

import datetime as dt
from collections import namedtuple, defaultdict
from decimal import Decimal as D

import mock
import pytest
import sqlalchemy as sa
from billing.contract_iface import contract_meta
from sqlalchemy import orm as orm

from balance import (
    mapper,
    muzzle_util as ut,
    reverse_partners as rp,
    constants as const,
)
from tests import object_builder as ob
from tests.balance_tests.rev_partners.common import get_pa


class OebsCashPaymentFactBuilder(ob.OebsCashPaymentFactBuilder):

    def generate_pk(self, session):
        self.b.xxar_cash_fact_id = session.execute(sa.func.next_value(sa.Sequence('s_request_order_id'))).scalar()
        self.b.source_id = session.execute(sa.func.next_value(sa.Sequence('s_request_order_id'))).scalar()

    def prepare(self, **params):
        bp = self.b
        bp.amount = params['amount']
        bp.operation_type = params['operation_type']
        bp.receipt_number = params['personal_account'].external_id

        bp.created_by = bp.last_updated_by = -1
        bp.creation_date = bp.last_update_date = bp.receipt_date = params['on_dt']

        super(ob.OebsCashPaymentFactBuilder, self).prepare(**params)


class OEBSPaymentsProcessor(object):

    def __init__(self, contract, service_id, service_code=None):
        self.contract = contract
        self.session = contract.session
        self.service_code = service_code
        self.personal_account = get_pa(self.contract, service_id, service_code=service_code)
        self.netting_type = 'correction_commission'
        self.processed_rows = list()

    def get_nettings(self):
        tc = mapper.ThirdPartyCorrection
        sign = sa.func.decode(tc.transaction_type, 'refund', 1, -1)
        netting = self.session \
            .query(tc.id.label('id'),
                   (sign * tc.amount).label('amount')) \
            .filter(tc.contract == self.contract) \
            .filter(tc.payment_type == self.netting_type) \
            .filter(sa.func.nvl(tc.internal, 0) == 0) \
            .filter(tc.id.notin_(self.processed_rows)) \
            .all()
        return netting

    def make_cashpaymentfact_correction(self, on_dt, amount):

        # при неавтокоммитной сессии падает из-за аналогичного триггера.
        with mock.patch('balance.mapper.OebsCashPaymentFact.export_types', []):
            cpf_builder = OebsCashPaymentFactBuilder(amount=amount,
                                                     operation_type='CORRECTION_NETTING',
                                                     personal_account=self.personal_account,
                                                     on_dt=on_dt)
            cpf_builder.generate_pk(self.session)
            return cpf_builder.build(self.session).obj

    def process_nettings(self, on_dt, real_payments_sum):
        netting_for_process = self.get_nettings()

        def reducer(totals, row):
            totals.transaction_ids.append(row.id)
            totals.sum += row.amount
            return totals

        payment_data = reduce(reducer, netting_for_process, ut.Struct(transaction_ids=[], sum=D(0)))
        self.processed_rows.extend(payment_data.transaction_ids)
        return_to_balance_amount = real_payments_sum - payment_data.sum
        if return_to_balance_amount < 0:
            cpf = self.make_cashpaymentfact_correction(on_dt, return_to_balance_amount)
            self.handle_oebs_cash_payment_fact(cpf)

    def handle_oebs_cash_payment_fact(self, obj):

        session = self.session
        transaction, invoice = session.query(mapper.OebsCashPaymentFact, mapper.Invoice) \
            .join(mapper.Invoice, mapper.Invoice.external_id == mapper.OebsCashPaymentFact.receipt_number) \
            .options(orm.joinedload(mapper.Invoice.contract)) \
            .filter(mapper.OebsCashPaymentFact.xxar_cash_fact_id == obj.xxar_cash_fact_id) \
            .first()

        contract = invoice.contract
        cs = contract.current_signed()
        desirable_id = int(transaction.source_id)  # id добавляемой коррекции лежит в этом столбце
        TPC = mapper.ThirdPartyCorrection

        with session.begin():
            if not session.query(TPC).filter(TPC.id == desirable_id).exists():
                amount = -transaction.amount
                correction = TPC.create_correction(cs,
                                                   transaction.creation_date,
                                                   const.ServiceId.TAXI_PAYMENT,
                                                   amount=amount,
                                                   invoice=invoice,
                                                   internal=1,
                                                   payment_type='correction_commission',
                                                   transaction_type='payment')

                correction.id = desirable_id
                correction.completion_qty_left = 0

                correction.apply()
                session.add(correction)


class TestBase(object):

    @staticmethod
    def assertEqual(first, second, msg=None):
        try:
            msg = msg or (
                'Assert failed: %s == %s' % (first, second))
        except:
            pass
        assert first == second, msg


class TestNettingBase(TestBase):
    default_COMPLETION_COEFFICIENT_NETTING_START_DT = dt.datetime(2018, 7, 1)
    default_TAXI_TLOG_MIGRATION_PARAMS = {'taxi_use_tlog_completions': 1}

    service_code = None
    service_id = None
    netting_processor = None

    @staticmethod
    def end_of_the_day(date):
        return date.replace(hour=23, minute=59, second=59)

    @staticmethod
    def netting_time(date):
        return date.replace(hour=23, minute=59, second=58)

    def prepare_contract(self, session):
        raise NotImplementedError

    def add_netting_collateral(self, contract, col_dt, netting_flag, netting_pct, num=None, collateral_type=None):
        if not collateral_type:
            collateral_type = contract_meta.collateral_types['GENERAL'][1046]
        col_attrs = {
            'netting': netting_flag,
            'netting_pct': netting_pct,
            'is_signed': dt.datetime.now(),
            'num': num
        }
        contract.append_collateral(col_dt, collateral_type, **col_attrs)
        contract.session.flush()

    def prepare_func_pathes_completions_mapping(self):
        raise NotImplementedError

    def collateral_params(self):
        return []

    def prepare_completions_funcs(self, func_pathes_completions_mapping):
        raise NotImplementedError

    def query_nettings(self, contract, start_dt, end_dt, filter_internal=True):
        tc = mapper.ThirdPartyCorrection
        query = contract.session.query(tc) \
            .filter(tc.contract == contract) \
            .filter(tc.dt >= start_dt) \
            .filter(tc.dt < end_dt)
        if filter_internal:
            query = query.filter(sa.func.nvl(tc.internal, 0) == 0)
        return query.all()

    def do_test(self, session, step_params, expected_results):
        # netting params:
        #   [ut.Struct(on_dt=dt, process_in_oebs_card_amount=D, change_completions_func=None,
        #    change_new_netting_date_config=None, change_cols_params={num:{params_dict}}, ..]
        # collateral_params:
        #   [ut.Struct(dt=dt, netting_flag=int, netting_pct=D, num=str ..]
        # expected_results:
        #   [ut.Struct(dt=dt, amount=D, completion_qty_left=D, receipt_sum=D, ..]

        contract = self.prepare_contract(session)

        for col_param in self.collateral_params():
            self.add_netting_collateral(contract, col_param.dt, col_param.netting_flag, col_param.netting_pct,
                                        num=col_param.num)
        personal_account = get_pa(contract, self.service_id, service_code=self.service_code)
        oebs_emul = OEBSPaymentsProcessor(contract=contract, service_id=self.service_id, service_code=self.service_code)
        session.config.__dict__[
            'COMPLETION_COEFFICIENT_NETTING_START_DT'] = self.default_COMPLETION_COEFFICIENT_NETTING_START_DT
        session.config.__dict__['TAXI_TLOG_MIGRATION_PARAMS'] = self.default_TAXI_TLOG_MIGRATION_PARAMS

        compl_funcs = self.prepare_completions_funcs(
            func_pathes_completions_mapping=self.prepare_func_pathes_completions_mapping())

        assert len(step_params) == len(expected_results)
        process_taxi_netting_in_oebs = getattr(self, 'PROCESS_TAXI_NETTING_IN_OEBS', False)
        if process_taxi_netting_in_oebs:
            session.config.__dict__['PROCESS_TAXI_NETTING_IN_OEBS_DT'] = dt.datetime(1990, 1, 1)
        process_taxi_netting_in_oebs = process_taxi_netting_in_oebs or contract.is_process_taxi_netting_in_oebs_

        for step_param, expected_result in zip(step_params, expected_results):
            new_completions_funcs = step_param.get('change_completions_funcs')
            if new_completions_funcs:
                compl_funcs.update(new_completions_funcs)
            new_config_date = step_param.get('change_new_netting_date_config')
            if new_config_date:
                session.config.__dict__['COMPLETION_COEFFICIENT_NETTING_START_DT'] = new_config_date
            change_cols_params = step_param.get('change_cols_params')
            if change_cols_params:
                for col_num, change_params in change_cols_params.iteritems():
                    col, = [_col for _col in contract.collaterals if _col.num == col_num]
                    for param_name, param_value in change_params.iteritems():
                        setattr(col, param_name, param_value)
                    session.flush()

            mockers = []
            try:
                for compl_func_path, compl_func in compl_funcs.iteritems():
                    m = mock.patch(compl_func_path, compl_func)
                    m.start()
                    mockers.append(m)

                self.netting_processor(contract, contract.current_signed(step_param.on_dt),
                                       step_param.on_dt).do_process()
                session.flush()
                oebs_card_amount = step_param.get('process_in_oebs_card_amount')
                if oebs_card_amount is not None:
                    oebs_on_dt = ut.trunc_date(step_param.on_dt) + dt.timedelta(hours=3)
                    oebs_emul.process_nettings(oebs_on_dt, oebs_card_amount)
                    session.flush()

                done_nettings = self.query_nettings(contract,
                                                    start_dt=ut.trunc_date(step_param.on_dt - dt.timedelta(days=1)),
                                                    end_dt=step_param.on_dt,
                                                    filter_internal=not process_taxi_netting_in_oebs)
                if done_nettings:
                    done_netting, = done_nettings
                else:
                    done_netting = None

                if expected_result.get('dt'):
                    assert done_netting.dt == expected_result.dt
                    assert done_netting.amount == expected_result.amount
                    if expected_result.get('completion_qty_left') is not None:
                        assert done_netting.completion_qty_left == expected_result.completion_qty_left
                    if expected_result.get('tlog_last_transaction_id') is not None:
                        assert done_netting.tlog_timeline_notch.last_transaction_id == expected_result.tlog_last_transaction_id
                    if process_taxi_netting_in_oebs:
                        assert bool(done_netting.internal)
                else:
                    assert not done_netting

                if expected_result.get('receipt_sum') is not None:
                    assert personal_account.receipt_sum == expected_result.receipt_sum

            finally:
                [m.stop() for m in mockers]


def real_completions_aggr(contract, on_dt, commission_type, from_dt, service_id, completions):
    """
    compl_format:
      [(service_id, commission_sum, order_type, dt, promocode_sum, subsidy_sum)]
    subsidy_sum - опционально, проставим 0, если не передан
    Открутки группируются по order_type, в реальном коде группировка идет по продуктам,
    из-за этого в тестах возможно использовать только уникальные по продуктам order_type"""
    if not from_dt:
        from_dt = dt.datetime(1999, 1, 1)
    completions = filter(lambda x: x[0] == service_id and on_dt > x[3] >= from_dt, completions)
    order_type_key = lambda x: x[2]
    completions = sorted(completions, key=order_type_key)
    gb = ut.groupby(completions, key=order_type_key)
    for order_type, group in gb:
        # докинем 0 в субсидии, если их не передали, чтобы не править все тесты.
        group = [list(gr) if len(gr) == 6 else list(gr) + [D('0')] for gr in group]
        group = list(group)
        product = rp.get_product(service_id, contract, order_type=order_type)
        promo_subt_order, = {pp.promo_subt_order for pp
                             in contract.session.query(mapper.PartnerProduct)
                                 .filter_by(product_mdh_id=product.mdh_id).all()}
        commission_sum = sum(x[1] for x in group)
        promocode_sum = sum(x[4] for x in group)
        subsidy_sum = sum(x[5] for x in group)
        if commission_type == 'promocode_sum':
            qty = promocode_sum
        elif commission_type == 'subsidy_sum':
            qty = subsidy_sum
        else:
            qty = commission_sum
        yield ut.Struct(requested_comm_type_sum=qty, promocode_sum=promocode_sum, subsidy_sum=subsidy_sum,
                        product_id=product.id, promo_subt_order=promo_subt_order)


def real_completions_aggr_tlog(contract, on_dt, on_transaction_dt, from_dt, service_id, completions):
    """
    compl_format:
      [(service_id, amount, completion_type, dt, from_dt)]
    Открутки группируются по completion_type, в реальном коде группировка идет по продуктам,
    из-за этого в тестах возможно использовать только уникальные по продуктам order_type"""
    order_types_mapping = {'subvention': 'order'}
    if not from_dt:
        from_dt = dt.datetime(1999, 1, 1)
    completions = filter(lambda x: x[0] == service_id and (on_dt > x[3] >= from_dt), completions)
    if on_transaction_dt:
        completions = filter(lambda x: on_transaction_dt > x[4], completions)
    order_type_key = lambda x: x[2]
    completions = sorted(completions, key=order_type_key)
    gb = ut.groupby(completions, key=order_type_key)
    for order_type, group in gb:
        group = list(group)
        product = rp.get_product(service_id, contract, order_type=order_types_mapping.get(order_type, order_type))
        promo_subt_order, = {pp.promo_subt_order for pp
                             in contract.session.query(mapper.PartnerProduct)
                                 .filter_by(product_mdh_id=product.mdh_id).all()}
        amount = sum(x[1] for x in group)
        tlog_last_transaction_id = None
        if group and len(group[0]) == 6:
            tlog_last_transaction_id = max(ut.nvl(x[5], 0) for x in group)
        yield ut.Struct(amount=amount, completion_type=order_type, product_id=product.id,
                        promo_subt_order=promo_subt_order, last_transaction_id=tlog_last_transaction_id)


compl_row_aggr = namedtuple('compl_row_aggr',
                            'requested_comm_type_sum subsidy_sum promocode_sum product_id promo_subt_order')
compl_row_aggr_tlog = namedtuple('compl_row_aggr_tlog',
                                 'completion_type amount product_id promo_subt_order last_transaction_id')


@pytest.mark.parametrize(
    'params, raw_completions_aggr, raw_completions_aggr_tlog, expected_compls, expected_last_transaction_id', [
        pytest.param(
            {'filter_services': [const.ServiceId.TAXI_CASH, const.ServiceId.TAXI_CARD],
             'subtract_promo': True,
             'allow_negative_compl': False},
            [[(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('100'), D('70'), D('10')],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('50'), D('20'), D('10')],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'childchair'), D('200'), D('30'), D('20')],
             [(const.ServiceId.TAXI_CARD, 'RUB', 'order'), D('300'), D('40'), D('30')],
             [(const.ServiceId.TAXI_CARD, 'RUB', 'childchair'), D('400'), D('50'), D('40')]],
            [],
            [(503352, D('40')),
             (508625, D('150')),
             (505142, D('230')),
             (508867, D('310'))],
            None,
            id='subtract_enought_cash'),
        pytest.param(
            {'filter_services': [const.ServiceId.TAXI_CASH, const.ServiceId.TAXI_CARD],
             'subtract_promo': True,
             'allow_negative_compl': False},
            [[(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('100'), D('70'), D('40')],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('50'), D('20'), D('30')],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'childchair'), D('200'), D('30'), D('20')],
             [(const.ServiceId.TAXI_CARD, 'RUB', 'order'), D('300'), D('40'), D('30')],
             [(const.ServiceId.TAXI_CARD, 'RUB', 'childchair'), D('400'), D('50'), D('40')]],
            [],
            [(503352, D('0')),
             (508625, D('140')),
             (505142, D('230')),
             (508867, D('310'))],
            None,
            id='subtract_closest_split'),
        pytest.param(
            {'filter_services': [const.ServiceId.TAXI_CASH, const.ServiceId.TAXI_CARD],
             'subtract_promo': True,
             'allow_negative_compl': False},
            [[(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('200'), D('70'), D('40')],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('50'), D('20'), D('30')],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'childchair'), D('200'), D('30'), D('20')],
             [(const.ServiceId.TAXI_CARD, 'RUB', 'order'), D('300'), D('40'), D('30')],
             [(const.ServiceId.TAXI_CARD, 'RUB', 'childchair'), D('400'), D('50'), D('400')]],
            [],
            [(503352, D('40')),
             (508625, D('150')),
             (505142, D('230')),
             (508867, D('0'))],
            None,
            id='subtract_to_top_split'),
        pytest.param(
            {'filter_services': [const.ServiceId.TAXI_CASH, const.ServiceId.TAXI_CARD],
             'subtract_promo': True,
             'allow_negative_compl': False},
            [[(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('200'), D('70'), D('40')],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('50'), D('20'), D('30')],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'childchair'), D('200'), D('30'), D('20')],
             [(const.ServiceId.TAXI_CARD, 'RUB', 'order'), D('300'), D('40'), D('30')],
             [(const.ServiceId.TAXI_CARD, 'RUB', 'childchair'), D('400'), D('50'), D('4000')]],
            [],
            [(503352, D('0')),
             (508625, D('0')),
             (505142, D('0')),
             (508867, D('0'))],
            None,
            id='subtract_zero'),
        pytest.param(
            {'filter_services': [const.ServiceId.TAXI_CASH, const.ServiceId.TAXI_CARD],
             'subtract_promo': True,
             'allow_negative_compl': True},
            [[(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('200'), D('50'), D('200')],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('50'), D('20'), D('30')],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'childchair'), D('200'), D('30'), D('20')],
             [(const.ServiceId.TAXI_CARD, 'RUB', 'order'), D('300'), D('40'), D('30')],
             [(const.ServiceId.TAXI_CARD, 'RUB', 'childchair'), D('400'), D('500'), D('4000')]],
            [],
            [(503352, D('-50')),
             (508625, D('150')),
             (505142, D('230')),
             (508867, D('-4100'))],
            None,
            id='subtract_negative_compls'),

        pytest.param(
            {'filter_services': [const.ServiceId.TAXI_CASH, const.ServiceId.TAXI_CARD],
             'subtract_promo': True,
             'allow_negative_compl': False},
            [[(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('100'), D('70'), D('10')],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('50'), D('20'), D('10')],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'childchair'), D('200'), D('30'), D('20')],
             [(const.ServiceId.TAXI_CARD, 'RUB', 'order'), D('300'), D('40'), D('30')],
             [(const.ServiceId.TAXI_CARD, 'RUB', 'childchair'), D('400'), D('50'), D('40')]],
            [[(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('200'), 9],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'coupon'), D('-100'), 20],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'subvention'), D('-50'), 1],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'childchair'), D('170'), 13],
             [(const.ServiceId.TAXI_CARD, 'RUB', 'order'), D('110'), None],
             [(const.ServiceId.TAXI_CARD, 'RUB', 'childchair'), D('400'), 17]],
            [(503352, D('90')),
             (508625, D('320')),
             (505142, D('340')),
             (508867, D('710'))],
            20,
            id='subtract_enought_cash_tlog'),
        pytest.param(
            {'filter_services': [const.ServiceId.TAXI_CASH, const.ServiceId.TAXI_CARD],
             'subtract_promo': True,
             'allow_negative_compl': False},
            [[(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('100'), D('70'), D('40')],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('50'), D('20'), D('30')],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'childchair'), D('200'), D('30'), D('20')],
             [(const.ServiceId.TAXI_CARD, 'RUB', 'order'), D('300'), D('40'), D('30')],
             [(const.ServiceId.TAXI_CARD, 'RUB', 'childchair'), D('400'), D('50'), D('40')]],
            [[(const.ServiceId.TAXI_CASH, 'RUB', 'coupon'), D('-100'), 300],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('20'), 100],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'childchair'), D('40'), 200]],
            [(503352, D('0')),
             (508625, D('100')),
             (505142, D('230')),
             (508867, D('310'))],
            300,
            id='subtract_closest_split_tlog'),
        pytest.param(
            {'filter_services': [const.ServiceId.TAXI_CASH, const.ServiceId.TAXI_CARD],
             'subtract_promo': True,
             'allow_negative_compl': False},
            [[(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('200'), D('70'), D('40')],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('50'), D('20'), D('30')],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'childchair'), D('200'), D('30'), D('20')],
             [(const.ServiceId.TAXI_CARD, 'RUB', 'order'), D('300'), D('40'), D('30')],
             [(const.ServiceId.TAXI_CARD, 'RUB', 'childchair'), D('400'), D('50'), D('400')]],
            [[(const.ServiceId.TAXI_CARD, 'RUB', 'childchair'), D('20'), 10],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('200'), 66],
             [(const.ServiceId.TAXI_CARD, 'RUB', 'subvention'), D('-40'), 400]
             ],
            [(503352, D('260')),
             (508625, D('150')),
             (505142, D('190')),
             (508867, D('0'))],
            400,
            id='subtract_to_top_split_tlog'),
        pytest.param(
            {'filter_services': [const.ServiceId.TAXI_CASH, const.ServiceId.TAXI_CARD],
             'subtract_promo': True,
             'allow_negative_compl': False},
            [[(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('200'), D('70'), D('40')],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('50'), D('20'), D('30')],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'childchair'), D('200'), D('30'), D('20')],
             [(const.ServiceId.TAXI_CARD, 'RUB', 'order'), D('300'), D('40'), D('30')],
             [(const.ServiceId.TAXI_CARD, 'RUB', 'childchair'), D('400'), D('50'), D('0')]],
            [[(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('20'), 13],
             [(const.ServiceId.TAXI_CARD, 'RUB', 'subvention'), D('-40'), 17],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'coupon'), D('-4000'), 25]],
            [(503352, D('0')),
             (508625, D('0')),
             (505142, D('0')),
             (508867, D('0'))],
            25,
            id='subtract_zero_tlog'),
        pytest.param(
            {'filter_services': [const.ServiceId.TAXI_CASH, const.ServiceId.TAXI_CARD],
             'subtract_promo': True,
             'allow_negative_compl': True},
            [[(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('200'), D('50'), D('200')],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('50'), D('20'), D('30')],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'childchair'), D('200'), D('30'), D('20')],
             [(const.ServiceId.TAXI_CARD, 'RUB', 'order'), D('300'), D('40'), D('30')],
             [(const.ServiceId.TAXI_CARD, 'RUB', 'childchair'), D('400'), D('500'), D('4000')]],
            [[(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('100'), 30],
             [(const.ServiceId.TAXI_CASH, 'RUB', 'coupon'), D('-250'), 15]],
            [(503352, D('-200')),
             (508625, D('150')),
             (505142, D('230')),
             (508867, D('-4100'))],
            30,
            id='subtract_negative_compls_tlog'),
        pytest.param(
            {'filter_services': [const.ServiceId.TAXI_CASH, const.ServiceId.TAXI_CARD],
             'subtract_promo': True,
             'allow_negative_compl': False},
            [[(const.ServiceId.TAXI_CASH, 'RUB', 'order'), D('100'), D('70'), D('10')]],
            [[(const.ServiceId.TAXI_CASH, 'RUB', 'coupon'), D('200'), 666]],
            [(503352, D('20'))],
            666,
            id='skip_subventions_overrefund_tlog'),
    ])
def test_taxi_completions_func(session, params, raw_completions_aggr, raw_completions_aggr_tlog, expected_compls,
                               expected_last_transaction_id):
    contract = mock.MagicMock()

    # Чтобы мок сессии на session.query(mapper.Product).getone(product_id) отдавал мок с id == product_id
    def session_get_func(*args, **kwargs):
        if len(args) == 1:
            return mock.MagicMock(id=args[0])
        else:
            return mock.MagicMock()

    def config_get_func(*args, **kwargs):
        default_mapping = {'TAXI_TLOG_MIGRATION_PARAMS':
                               {'taxi_use_tlog_completions': 1}}
        if len(args) == 1:
            args.append(None)
        return default_mapping.get(args[0], args[1])

    contract.session.query.return_value.get = session_get_func
    contract.session.config.get = config_get_func

    filter_services = params.get('filter_services', None) or [const.ServiceId.TAXI_CASH, const.ServiceId.TAXI_CARD]
    filter_services_func = lambda *args, **kwargs: filter_services

    def make_raw_completions_aggr_func(raw_compls):
        compls = defaultdict(list)
        for compl in raw_compls:
            (service_id, currency_iso_code, order_type), requested_comm_type_sum, promocode_sum, subsidy_sum = compl

            mpp = mapper.PartnerProduct
            pp = session.query(mpp) \
                .filter(mpp.order_type == order_type) \
                .filter(mpp.currency_iso_code == currency_iso_code) \
                .filter(mpp.service_id == service_id) \
                .options(sa.orm.joinedload(mpp.product)) \
                .one()

            compls[service_id].append(compl_row_aggr(requested_comm_type_sum, promocode_sum, subsidy_sum,
                                                     pp.product.id, pp.promo_subt_order))

        def filter_compls_by_service(*args, **kwargs):
            service_id = args[4]
            return compls.get(service_id, [])

        return filter_compls_by_service

    def make_raw_completions_aggr_tlog_func(raw_compls):
        compls = defaultdict(list)
        for compl in raw_compls:
            # докинем last_transaction_id, если не передано
            if len(compl) == 2:
                compl.append(None)
            (service_id, currency_iso_code, order_type), amount, last_transaction_id = compl
            if order_type in ['coupon', 'subvention']:
                order_type = 'order'
                completion_type = 'subvention'
            else:
                completion_type = 'commission'

            mpp = mapper.PartnerProduct
            pp = session.query(mpp) \
                .filter(mpp.order_type == order_type) \
                .filter(mpp.currency_iso_code == currency_iso_code) \
                .filter(mpp.service_id == service_id) \
                .options(sa.orm.joinedload(mpp.product)) \
                .one()

            compls[service_id].append(compl_row_aggr_tlog(completion_type, amount, pp.product.id, pp.promo_subt_order,
                                                          last_transaction_id))

        def filter_compls_by_service(*args, **kwargs):
            service_id = args[4]
            return compls.get(service_id, [])

        return filter_compls_by_service

    get_taxi_completions_aggr_func = make_raw_completions_aggr_func(raw_completions_aggr)
    get_taxi_completions_aggr_tlog_func = make_raw_completions_aggr_tlog_func(raw_completions_aggr_tlog)

    with mock.patch('balance.reverse_partners.filter_services', filter_services_func), \
        mock.patch('balance.reverse_partners.get_taxi_completions_aggr', get_taxi_completions_aggr_func), \
        mock.patch('balance.reverse_partners.get_taxi_completions_aggr_tlog', get_taxi_completions_aggr_tlog_func):
        compls = rp.taxi_completions(contract, None, from_dt=None,
                                     subtract_promo=params['subtract_promo'],
                                     allow_negative_compl=params['allow_negative_compl'])
    last_transaction_id = compls.last_transaction_id
    compls = [(c_[0], c_[1]) for c_ in compls]
    compls.sort()
    expected_compls.sort()
    assert len(compls) == len(expected_compls)
    for compl, expected_compl in zip(compls, expected_compls):
        assert compl == expected_compl
    if expected_last_transaction_id is not None:
        assert expected_last_transaction_id == last_transaction_id


def taxi_default_terms(contract):
    contract.col0.partner_commission_type = 2  # commission_pct mode
    contract.col0.partner_commission_pct = 8
    contract.col0.service_min_cost = 0
    contract.col0.firm = 13
    contract.col0.services = {
        const.ServiceId.TAXI_CASH,
        const.ServiceId.TAXI_PAYMENT,
        const.ServiceId.TAXI_CARD,
    }
    return contract
