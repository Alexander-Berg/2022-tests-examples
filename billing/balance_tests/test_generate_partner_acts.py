# -*- coding: utf-8 -*-
from datetime import datetime, timedelta
from decimal import Decimal as D

import sqlalchemy as sa
from butils import exc
from billing.contract_iface import contract_meta

from balance import constants as const, contractpage, mapper, muzzle_util as ut, scheme
from balance.constants import BankType, OebsPayReceiveType
from cluster_tools import generate_partner_acts as gpa
from tests import object_builder as ob
from tests.base import BalanceTest


ADDAPPTER_2_SCALE = 'addappter_common_scale'


def gen_addapter_spendabe_contract(session,  con_func=None):
    contract = mapper.Contract(ctype=contract_meta.ContractTypes(type='SPENDABLE'))
    session.add(contract)
    contract.client = ob.ClientBuilder().build(session).obj
    contract.person = ob.PersonBuilder(client=contract.client,
                                       type='ph',
                                       is_partner=True,
                                       bank_type=BankType.YANDEX_MONEY,
                                       yamoney_wallet='4000000000000'
                                       ).build(session).obj
    contract.col0.dt = datetime(2018, 9, 1)

    contract.col0.firm = 1
    contract.col0.is_offer = 1
    contract.col0.manager_code = 1122
    contract.col0.currency = 643
    contract.col0.pay_to = OebsPayReceiveType.YANDEX_MONEY
    contract.col0.is_signed = datetime(2018, 9, 1)
    contract.col0.services = {630: 1}
    contract.col0.nds = 0

    contract.external_id = contract.create_new_eid()

    if con_func:
        con_func(contract)

    cp = contractpage.ContractPage(session, contract.id)
    session.flush()

    return contract


class TestAddappter2Pytest():

    def prepare_completions(self):
        raise NotImplementedError

    def prepare_contract(self, session, con_func=None):
        return gen_addapter_spendabe_contract(session, con_func=con_func)

    def make_completions(self, session, client, compl_struct):
        # compl_struct = ut.Struct(dt, qty)
        for compl in compl_struct:
            session.execute(scheme.partner_product_completion.insert(
                {
                    'dt': compl.dt,
                    'client_id': client.id,
                    'service_id': const.ServiceId.ADDAPPTER_2,
                    'qty': compl.qty
                }
            ))

    def create_scale_maybe(self, session, scale_name=None):
        scale_name = scale_name or ADDAPPTER_2_SCALE
        try:
            scale = session.query(mapper.Scale).getone(code=scale_name, namespace='addappter_2')
        except exc.NOT_FOUND:
            scale = mapper.Scale(code=scale_name, namespace='addappter_2', type='staircase_cost_max_cut')
            session.add(scale)
            session.flush()

    def hide_scale_points(self, session, scale_name=None):
        scale_name = scale_name or ADDAPPTER_2_SCALE
        scale = session.query(mapper.Scale).getone(code=scale_name, namespace='addappter_2')
        session.query(mapper.ScalePoint).filter(mapper.ScalePoint.scale == scale,
                                                mapper.ScalePoint.namespace == 'addappter_2'
                                                ).update({mapper.ScalePoint.hidden: 1})
        session.flush()

    def insert_scale_points(self, session, start_dt, end_dt=None, scale_name=None, points=None, currency='RUB'):
        # points=[(x, y, max_sum), ]
        if not points:
            points = [(0,  0,  None),
                      (11, 35, None),
                      (21, 40, None),
                      (31, 50, 12500)]
        scale_name = scale_name or ADDAPPTER_2_SCALE
        for p in points:
            point = mapper.ScalePoint(scale_code=scale_name, namespace='addappter_2', start_dt=start_dt, end_dt=end_dt,
                                      x=p[0], y=p[1], max_sum=p[2], currency=currency)
            session.add(point)
        session.flush()

    def prepare_data(self, session, month):
        act_month = mapper.ActMonth(for_month=month)
        contract = self.prepare_contract(session)
        self.create_scale_maybe(session)
        self.hide_scale_points(session)
        self.insert_scale_points(session, start_dt=month)
        compls = self.prepare_completions()
        self.make_completions(session, contract.client, compls)

        gen = gpa.get_generator(contract, act_month=act_month)
        gen.generate(act_month)
        session.flush()

        return contract


class TestAddapterStandartSuccess(TestAddappter2Pytest):

    def prepare_completions(self):
        return [ut.Struct(dt=datetime(2018, 9, 1), qty=10),
                ut.Struct(dt=datetime(2018, 9, 30), qty=1)]

    def test_do(self, session):
        month = datetime(2018, 9, 1)
        contract = self.prepare_data(session, month)
        self.check_result(session, contract, month)

    def check_result(self, session, contract, month):
        pad = session.query(mapper.PartnerActData)\
            .filter(mapper.PartnerActData.contract == contract)\
            .filter(mapper.PartnerActData.dt == month)\
            .one()

        assert pad.dt == month
        assert pad.partner_reward_wo_nds == 385
        assert pad.currency == 'RUR'
        assert pad.iso_currency == 'RUB'
        assert pad.partner_contract_id == contract.id
        assert pad.update_dt is not None


class TestAddapterMaxSumExceed(TestAddappter2Pytest):

    def prepare_completions(self):
        return [ut.Struct(dt=datetime(2018, 9, 1), qty=240),
                ut.Struct(dt=datetime(2018, 9, 30), qty=11)]

    def test_do(self, session):
        month = datetime(2018, 9, 1)
        contract = self.prepare_data(session, month)
        self.check_result(session, contract, month)

    def check_result(self, session, contract, month):
        pad = session.query(mapper.PartnerActData)\
            .filter(mapper.PartnerActData.contract == contract)\
            .filter(mapper.PartnerActData.dt == month)\
            .one()

        assert pad.dt == month
        assert pad.partner_reward_wo_nds == 12500
        assert pad.currency == 'RUR'
        assert pad.iso_currency == 'RUB'
        assert pad.partner_contract_id == contract.id
        assert pad.update_dt is not None


class TestAddapterNewScale(TestAddappter2Pytest):

    def insert_scale_points(self, session, start_dt, end_dt=None, scale_name=None, points=None, currency='RUB'):
        scale_name = scale_name or ADDAPPTER_2_SCALE
        points = [(0,  0,  None),
                  (11, 35, None),
                  (21, 40, None),
                  (31, 50, 12500)]
        for p in points:
            point = mapper.ScalePoint(scale_code=scale_name, namespace='addappter_2',
                                      start_dt=datetime(2018, 8, 1), end_dt=datetime(2018, 9, 1),
                                      x=p[0], y=p[1], max_sum=p[2], currency=currency)
            session.add(point)
        session.flush()

        points = [(0,  0,  None),
                  (11, D('35.123'), None)]
        for p in points:
            point = mapper.ScalePoint(scale_code=scale_name, namespace='addappter_2',
                                      start_dt=datetime(2018, 9, 1), end_dt=datetime(2018, 9, 30),
                                      x=p[0], y=p[1], max_sum=p[2], currency=currency)
            session.add(point)
        session.flush()

    def prepare_completions(self):
        return [ut.Struct(dt=datetime(2018, 9, 1), qty=240),
                ut.Struct(dt=datetime(2018, 9, 30), qty=11)]

    def test_do(self, session):
        month = datetime(2018, 9, 1)
        contract = self.prepare_data(session, month)
        self.check_result(session, contract, month)

    def check_result(self, session, contract, month):
        pad = session.query(mapper.PartnerActData)\
            .filter(mapper.PartnerActData.contract == contract)\
            .filter(mapper.PartnerActData.dt == month)\
            .one()

        assert pad.dt == month
        assert pad.partner_reward_wo_nds == D('8815.87')
        assert pad.currency == 'RUR'
        assert pad.iso_currency == 'RUB'
        assert pad.partner_contract_id == contract.id
        assert pad.update_dt is not None


def prepare_contract_with_tpts(session, begin_dt, end_dt, tpts=()):
    contract = mapper.Contract(ctype=contract_meta.ContractTypes(type='SPENDABLE'))
    session.add(contract)

    col0 = contract.col0
    contract.client = ob.ClientBuilder().build(session).obj
    contract.person = ob.PersonBuilder(client=contract.client, type='ur').build(session).obj

    # договор действует только в октябре; при этом, он не активен ни на начало, ни на конец месяца
    col0.dt = begin_dt
    col0.is_signed = begin_dt
    col0.end_dt = end_dt

    # прочее
    col0.currency = 643
    col0.firm = const.FirmId.TAXI
    col0.nds = 0
    col0.payment_type = 3
    col0.services = {const.ServiceId.TAXI_CORP: 1, const.ServiceId.TAXI_CORP_PARTNERS: 1}
    session.flush()

    # открутки
    with session.begin():
        for item in tpts:
            next_id = session.execute(
                'select bo.s_request_order_id.nextval from dual'
            ).scalar()
            session.add(mapper.ThirdPartyTransaction(
                id=next_id,
                contract_id=contract.id,
                amount=item.get('amount', 100),
                dt=item.get('dt', begin_dt + timedelta(days=1)),
                transaction_type=item.get('transaction_type', 'payment'),
                service_id=const.ServiceId.TAXI_CORP))

    return contract


class TestCorpTaxiGenerator(BalanceTest):
    _dt_act = datetime(2018, 10, 31)
    _dt_begin = datetime(2018, 10, 1)
    _dt_end = datetime(2019, 1, 1)

    def test_corp_taxi_part_month(self):
        """ Генерация актов, когда договор действует неполный месяц. """
        dt_begin = self._dt_begin + timedelta(1)
        dt_end = self._dt_act - timedelta(5)
        contract = prepare_contract_with_tpts(self.session, dt_begin, dt_end,
                                              [{'dt': dt_begin + timedelta(1)}])
        # актим
        with self.session.begin():
            month = mapper.ActMonth(for_month=self._dt_act)
            gpa.get_generator(contract, act_month=month).generate(month)

        # должен получиться акт
        act_exists = self.session\
            .query(mapper.PartnerActData)\
            .filter(mapper.PartnerActData.partner_contract_id == contract.id)\
            .exists()
        self.assertTrue(act_exists)

    def test_corp_taxi_empty_tpts(self):
        """ Генерация актов, когда у договора в месяце нет откруток. """
        contract = prepare_contract_with_tpts(self.session, self._dt_begin, self._dt_end,
                                              [{'dt': self._dt_act + timedelta(1)}])
        # актим
        with self.session.begin():
            month = mapper.ActMonth(for_month=self._dt_act)
            gpa.get_generator(contract, act_month=month).generate(month)

        # т.к нет откруток до даты акта - нет акта
        act_exists = self.session\
            .query(mapper.PartnerActData)\
            .filter(mapper.PartnerActData.partner_contract_id == contract.id)\
            .exists()
        self.assertFalse(act_exists)

    def corp_taxi_tpts_base(self, begin_dt, end_dt, act_dt):
        # создаем транзакции для контракта, так что бы они были как внутри, так и вне закрываемого отрезка дат
        contract = prepare_contract_with_tpts(self.session, begin_dt, end_dt,
                                              [
                                                  {'dt': begin_dt - timedelta(1), 'amount': 1},
                                                  {'dt': begin_dt + timedelta(1), 'amount': 2},
                                                  {'dt': act_dt - timedelta(1), 'amount': 4},
                                                  {'dt': act_dt + timedelta(1), 'amount': 8},
                                                  {'dt': end_dt, 'amount': 16},
                                              ])
        # актим
        with self.session.begin():
            month = mapper.ActMonth(for_month=self._dt_act)
            gpa.get_generator(contract, act_month=month).generate(month)

        # в акте должны быть учтены только попадающие в текущий месяц, причем до даты закрытия договора, открутки
        return self.session\
            .query(mapper.PartnerActData)\
            .filter(mapper.PartnerActData.partner_contract_id == contract.id)\
            .first()

    def test_corp_taxi_tpts(self):
        """ Генерация актов, когда у договора есть открутки как до даты акта, так и после. """
        act = self.corp_taxi_tpts_base(self._dt_begin, self._dt_end, self._dt_act)
        self.assertIsNotNone(act)
        self.assertEqual(act.partner_reward_wo_nds, 2 + 4)

    def test_corp_taxi_tpts_ended_contract(self):
        """ Генерация актов, когда у договора есть открутки как до даты акта, так и после,
         для договора, завершенного до даты акта """
        dt_end = self._dt_act - timedelta(5)
        act = self.corp_taxi_tpts_base(self._dt_begin, dt_end, self._dt_act)
        self.assertIsNotNone(act)
        self.assertEqual(act.partner_reward_wo_nds, 2 + 16)
