# -*- coding: utf-8 -*-

import functools
from decimal import Decimal as D

import datetime
import mock
import pytest
from dateutil.relativedelta import relativedelta

from balance import constants as const
from balance import mapper
from balance import muzzle_util as ut
from balance.processors.month_proc import create_act_accounter
from cluster_tools import generate_partner_acts as gpa
from tests.balance_tests.rev_partners.common import (
    TAXI_COMMISSION_SERVICE_CODE,
    TAXI_MIN_ACT_PARAMS,
    TAXI_RF_111_MIN_ACT_PRODUCT,
    TAXI_RF_MIN_ACT_AMOUNT,
    TAXI_TLOG_MIGRATION_PARAMS,
    gen_contract,
    get_pa,
    listget,
    real_completions_aggr,
    real_completions_aggr_tlog,
    taxi_prepay_only_commission_contract,
)
from tests.balance_tests.rev_partners.test_taxi import get_order_act


class TestTaxiPytest(object):
    def get_order_act(self, acts):
        return get_order_act(acts)

    def prepare_contract(self, session, postpay=False, con_func=None):
        """Продукты в открутках. предоплата"""
        min_cost = D("0")
        payment_sum = D("750")
        if not con_func:
            con_func = lambda c: taxi_prepay_only_commission_contract(c, min_cost)
        contract = gen_contract(
            session, postpay=postpay, personal_account=True, con_func=con_func
        )

        pa = get_pa(session, contract, service_code=TAXI_COMMISSION_SERVICE_CODE)

        pa.receipt_sum = payment_sum
        assert pa.consume_sum == D("0")

        return contract

    def prepare_completions_aggr(self, dt):
        return []

    def prepare_completions_aggr_tlog(self, act_month):
        return []

    def prepare_func_pathes_completions_mapping(self, act_month):
        return {
            "balance.reverse_partners.get_taxi_completions_aggr": self.prepare_completions_aggr(
                act_month.begin_dt
            ),
            "balance.reverse_partners.get_taxi_completions_aggr_tlog": self.prepare_completions_aggr_tlog(
                act_month
            ),
        }

    def prepare_completions_funcs(self, func_pathes_completions_mapping):
        return {
            "balance.reverse_partners.get_taxi_completions_aggr": functools.partial(
                real_completions_aggr,
                completions=func_pathes_completions_mapping[
                    "balance.reverse_partners.get_taxi_completions_aggr"
                ],
            ),
            "balance.reverse_partners.get_taxi_completions_aggr_tlog": functools.partial(
                real_completions_aggr_tlog,
                completions=func_pathes_completions_mapping[
                    "balance.reverse_partners.get_taxi_completions_aggr_tlog"
                ],
            ),
        }


def sign(compl_type):
    return 1


class TestTaxiNewPromoBase(TestTaxiPytest):
    def get_tlog_timeline_notch(self, obj):
        session = obj.session
        notch = (
            session.query(mapper.TLogTimeline)
            .filter(mapper.TLogTimeline.object_id == obj.id)
            .filter(mapper.TLogTimeline.classname == obj.__class__.__name__)
            .one()
        )
        return notch

    def do_test(self, session, postpay=False):
        # session.config.__dict__['TAXI_MIN_ACT_PARAMS'] = TAXI_MIN_ACT_PARAMS
        session.config.__dict__[
            "TAXI_TLOG_MIGRATION_PARAMS"
        ] = TAXI_TLOG_MIGRATION_PARAMS

        first_month = datetime.datetime(2017, 11, 1)
        act_month = mapper.ActMonth(for_month=first_month)
        contract = self.prepare_contract(session, postpay=postpay)
        func_pathes_completions_mapping = self.prepare_func_pathes_completions_mapping(
            act_month=act_month
        )
        compl_funcs = self.prepare_completions_funcs(
            func_pathes_completions_mapping=func_pathes_completions_mapping
        )
        mockers = []
        try:
            for compl_func_path, compl_func in compl_funcs.iteritems():
                m = mock.patch(compl_func_path, compl_func)
                m.start()
                mockers.append(m)

            gpa.RevPartnerGenerator(contract).generate(act_month)
            session.flush()
        finally:
            [m.stop() for m in mockers]

        export_object = (
            session.query(mapper.Export)
            .filter(
                (mapper.Export.type == "MONTH_PROC")
                & (mapper.Export.classname == "Client")
                & (mapper.Export.state == 0)
                & (mapper.Export.object_id == contract.client.id)
            )
            .one()
        )
        # без рефреша не подтягивает реальные данные в объект
        session.refresh(export_object)

        split_act_creation = session.config.get("ACT_SPLIT_ACT_CREATION", False)
        s_input = ut.Struct(export_object.input)
        act_accounter = create_act_accounter(
            contract.client, s_input, session, split_act_creation=split_act_creation
        )
        acts = act_accounter.do(skip_cut_agava=False)
        session.flush()

        order_act = self.get_order_act(acts)
        pa = get_pa(session, contract, service_code=TAXI_COMMISSION_SERVICE_CODE)
        completions_aggr = func_pathes_completions_mapping[
            "balance.reverse_partners.get_taxi_completions_aggr"
        ]
        completions_aggr_tlog = func_pathes_completions_mapping[
            "balance.reverse_partners.get_taxi_completions_aggr_tlog"
        ]
        expected_qty = sum(
            x[1] - x[4] - x[5] for x in list(completions_aggr) if x[3] == first_month
        ) + sum(
            sign(x[2]) * x[1]
            for x in list(completions_aggr_tlog)
            if x[3] < act_month.end_dt
        )

        assert expected_qty == sum(q.current_qty for q in pa.consumes)
        assert expected_qty == sum(r.act_qty for r in order_act.rows)

        types = {(x[0], x[2]) for x in completions_aggr if x[3] == first_month}
        types.update(
            {
                (x[0], x[2])
                for x in completions_aggr_tlog
                if (x[3] < act_month.end_dt and x[2] != "subvention")
            }
        )
        assert len(types) == len(order_act.rows)
        last_transaction_id = max(
            [
                listget(x, 5, 0)
                for x in list(completions_aggr_tlog)
                if x[3] < act_month.end_dt
            ]
        )
        notch = self.get_tlog_timeline_notch(pa)
        assert last_transaction_id == notch.last_transaction_id


class TestTaxiPrepayCommonNewPromo(TestTaxiNewPromoBase):
    def prepare_completions_aggr(self, dt):
        completions = [
            (const.ServiceId.TAXI_CASH, D("500"), "order", dt, D("200"), D("100")),
            (
                const.ServiceId.TAXI_CASH,
                D("100"),
                "order",
                dt + relativedelta(months=1),
                D("0"),
                D("0"),
            ),
            (const.ServiceId.TAXI_CASH, D("100"), "childchair", dt, D("0"), D("0")),
            (const.ServiceId.TAXI_CARD, D("600"), "order", dt, D("0"), D("100")),
            (const.ServiceId.TAXI_CARD, D("200"), "childchair", dt, D("100"), D("0")),
        ]
        return completions

    def prepare_completions_aggr_tlog(self, act_month):
        dt = act_month.begin_dt

        completions = [
            (const.ServiceId.TAXI_CASH, D("500"), "order", dt, dt, 1),
            (const.ServiceId.TAXI_CASH, D("-300"), "subvention", dt, dt, 2),
            (
                const.ServiceId.TAXI_CASH,
                D("600"),
                "order",
                dt,
                dt + relativedelta(months=1),
                3,
            ),
            (
                const.ServiceId.TAXI_CASH,
                D("600"),
                "order",
                dt + relativedelta(months=1),
                dt,
                4,
            ),
            (const.ServiceId.TAXI_CASH, D("200"), "childchair", dt, dt, 7),
            (const.ServiceId.TAXI_CARD, D("500"), "order", dt, dt, 8),
            (const.ServiceId.TAXI_CARD, D("200"), "childchair", dt, dt, 9),
        ]
        return completions

    def test(self, session):
        self.do_test(session, postpay=False)


class TestTaxiPrepayCashCardNoMainOrderCompletionsNewPromo(TestTaxiNewPromoBase):
    def prepare_completions_aggr(self, dt):
        completions = [
            (const.ServiceId.TAXI_CARD, D("500"), "order", dt, D("200"), D("100")),
            (const.ServiceId.TAXI_CARD, D("100"), "childchair", dt, D("0"), D("0")),
        ]
        return completions

    def prepare_completions_aggr_tlog(self, act_month):
        dt = act_month.begin_dt

        completions = [
            (const.ServiceId.TAXI_CARD, D("500"), "order", dt, dt),
            (const.ServiceId.TAXI_CARD, D("200"), "childchair", dt, dt),
        ]
        return completions

    def test(self, session):
        self.do_test(session, postpay=False)


class TestTaxiPostpayCashNewPromo(TestTaxiNewPromoBase):
    def prepare_completions_aggr(self, dt):
        completions = [
            (const.ServiceId.TAXI_CASH, D("123.66"), "childchair", dt, D("0"), D("12")),
            (const.ServiceId.TAXI_CASH, D("100"), "order", dt, D("20"), D("50")),
        ]
        return completions

    def prepare_completions_aggr_tlog(self, act_month):
        dt = act_month.begin_dt

        completions = [
            (const.ServiceId.TAXI_CASH, D("500"), "order", dt, dt),
            (const.ServiceId.TAXI_CASH, D("200"), "childchair", dt, dt),
            (const.ServiceId.TAXI_CASH, D("-100"), "subvention", dt, dt),
        ]
        return completions

    def test(self, session):
        self.do_test(session, postpay=True)


class TestTaxiPostpayCashNoMainOrderCompletionsNewPromo(TestTaxiNewPromoBase):
    def prepare_completions_aggr(self, dt):
        completions = [
            (
                const.ServiceId.TAXI_CASH,
                D("123.66"),
                "childchair",
                dt,
                D("12"),
                D("14"),
            ),
        ]
        return completions

    def prepare_completions_aggr_tlog(self, act_month):
        dt = act_month.begin_dt

        completions = [
            (const.ServiceId.TAXI_CASH, D("200"), "childchair", dt, dt),
        ]
        return completions

    def test(self, session):
        self.do_test(session, postpay=True)


class TestTaxiPostpayCashCardNewPromo(TestTaxiNewPromoBase):
    def prepare_completions_aggr(self, dt):
        completions = [
            (const.ServiceId.TAXI_CASH, D("500"), "order", dt, D("200"), D("100")),
            (
                const.ServiceId.TAXI_CASH,
                D("100"),
                "order",
                dt + relativedelta(months=1),
                D("0"),
                D("0"),
            ),
            (const.ServiceId.TAXI_CASH, D("100"), "childchair", dt, D("0"), D("0")),
            (const.ServiceId.TAXI_CARD, D("600"), "order", dt, D("0"), D("100")),
            (const.ServiceId.TAXI_CARD, D("200"), "childchair", dt, D("100"), D("0")),
        ]
        return completions

    def prepare_completions_aggr_tlog(self, act_month):
        dt = act_month.begin_dt

        completions = [
            (const.ServiceId.TAXI_CASH, D("500"), "order", dt, dt),
            (const.ServiceId.TAXI_CASH, D("-300"), "subvention", dt, dt),
            (
                const.ServiceId.TAXI_CASH,
                D("600"),
                "order",
                dt,
                dt + relativedelta(months=1),
            ),
            (
                const.ServiceId.TAXI_CASH,
                D("600"),
                "order",
                dt + relativedelta(months=1),
                dt,
            ),
            (
                const.ServiceId.TAXI_CASH,
                D("600"),
                "order",
                dt,
                dt + relativedelta(months=1, days=2),
            ),
            (
                const.ServiceId.TAXI_CASH,
                D("-400"),
                "subvention",
                dt,
                dt + relativedelta(months=1, days=2),
            ),
            (const.ServiceId.TAXI_CASH, D("200"), "childchair", dt, dt),
            (const.ServiceId.TAXI_CARD, D("500"), "order", dt, dt),
            (const.ServiceId.TAXI_CARD, D("200"), "childchair", dt, dt),
        ]
        return completions

    def test(self, session):
        self.do_test(session, postpay=True)


class TestTaxiSplitCompletionsNewPromo(TestTaxiPytest):
    def prepare_completions_aggr(self, dt):
        completions = [
            (const.ServiceId.TAXI_CASH, D("800"), "order", dt, D("50"), D("150")),
            (
                const.ServiceId.TAXI_CASH,
                D("100"),
                "order",
                dt + relativedelta(months=1),
                D("0"),
                D("0"),
            ),
            (const.ServiceId.TAXI_CASH, D("300"), "childchair", dt, D("150"), D("50")),
            (const.ServiceId.TAXI_CARD, D("600"), "order", dt, D("200"), D("300")),
            (const.ServiceId.TAXI_CARD, D("200"), "childchair", dt, D("300"), D("100")),
        ]
        return completions

    def prepare_completions_aggr_tlog(self, act_month):
        dt = act_month.begin_dt

        completions = [
            (const.ServiceId.TAXI_CASH, D("300"), "order", dt, dt),
            (
                const.ServiceId.TAXI_CASH,
                D("600"),
                "order",
                dt + relativedelta(months=1),
                dt,
            ),
            (
                const.ServiceId.TAXI_CASH,
                D("600"),
                "order",
                dt,
                dt + relativedelta(months=1, days=2),
            ),
            (
                const.ServiceId.TAXI_CASH,
                D("-400"),
                "subvention",
                dt,
                dt + relativedelta(months=1, days=2),
            ),
        ]
        return completions

    def test(self, session):
        session.config.__dict__[
            "TAXI_TLOG_MIGRATION_PARAMS"
        ] = TAXI_TLOG_MIGRATION_PARAMS

        first_month = datetime.datetime(2017, 11, 1)
        act_month = mapper.ActMonth(for_month=first_month)
        contract = self.prepare_contract(session)

        func_pathes_completions_mapping = self.prepare_func_pathes_completions_mapping(
            act_month=act_month
        )
        compl_funcs = self.prepare_completions_funcs(
            func_pathes_completions_mapping=func_pathes_completions_mapping
        )
        mockers = []
        try:
            for compl_func_path, compl_func in compl_funcs.iteritems():
                m = mock.patch(compl_func_path, compl_func)
                m.start()
                mockers.append(m)
            gpa.RevPartnerGenerator(contract).generate(act_month)
            session.flush()
        finally:
            [m.stop() for m in mockers]

        export_object = (
            session.query(mapper.Export)
            .filter(
                (mapper.Export.type == "MONTH_PROC")
                & (mapper.Export.classname == "Client")
                & (mapper.Export.state == 0)
                & (mapper.Export.object_id == contract.client.id)
            )
            .one()
        )
        # без рефреша не подтягивает реальные данные в объект
        session.refresh(export_object)

        split_act_creation = session.config.get("ACT_SPLIT_ACT_CREATION", False)
        s_input = ut.Struct(export_object.input)
        act_accounter = create_act_accounter(
            contract.client, s_input, session, split_act_creation=split_act_creation
        )
        acts = act_accounter.do(skip_cut_agava=False)
        session.flush()

        order_act = self.get_order_act(acts)
        pa = get_pa(session, contract, service_code=TAXI_COMMISSION_SERVICE_CODE)

        completions_aggr = func_pathes_completions_mapping[
            "balance.reverse_partners.get_taxi_completions_aggr"
        ]
        completions_aggr_tlog = func_pathes_completions_mapping[
            "balance.reverse_partners.get_taxi_completions_aggr_tlog"
        ]
        expected_qty = sum(
            x[1] - x[4] - x[5] for x in list(completions_aggr) if x[3] == first_month
        ) + sum(
            sign(x[2]) * x[1]
            for x in list(completions_aggr_tlog)
            if x[3] < act_month.end_dt
        )

        assert expected_qty == sum(q.current_qty for q in pa.consumes)
        assert expected_qty == sum(r.act_qty for r in order_act.rows)
        expected = {
            (503352, D("900")),
            (508625, D("100")),
            (505142, D("100")),
        }
        got = {(at.consume.order.service_code, at.act_qty) for at in order_act.rows}
        assert expected == got


class TestTaxiNewPromoMinimalAct(TestTaxiPytest):
    def prepare_completions_aggr(self, dt):
        completions = [
            (const.ServiceId.TAXI_CASH, D("500"), "order", dt, D("300"), D("200")),
            (
                const.ServiceId.TAXI_CASH,
                D("100"),
                "order",
                dt + relativedelta(months=1),
                D("0"),
                D("0"),
            ),
            (const.ServiceId.TAXI_CASH, D("100"), "childchair", dt, D("50"), D("50")),
            (const.ServiceId.TAXI_CARD, D("600"), "order", dt, D("200"), D("400")),
            (const.ServiceId.TAXI_CARD, D("200"), "childchair", dt, D("100"), D("100")),
        ]
        return completions

    @pytest.mark.skip(
        reason="Функционал на проде отключен. Допилить тест в соответсвии с транзакционным логом"
    )
    def test(self, session):
        session.config.__dict__["NEW_TAXI_PROMO_LOGIC"] = 1
        session.config.__dict__["TAXI_MIN_ACT_PARAMS"] = TAXI_MIN_ACT_PARAMS
        session.config.__dict__[
            "TAXI_TLOG_MIGRATION_PARAMS"
        ] = TAXI_TLOG_MIGRATION_PARAMS

        first_month = datetime.datetime(2017, 11, 1)
        act_month = mapper.ActMonth(for_month=first_month)
        contract = self.prepare_contract(session)
        completions = self.prepare_completions(dt=first_month)
        completions_func = self.prepare_completions_func(completions=completions)

        with mock.patch(
            "balance.reverse_partners.get_taxi_completions", completions_func
        ):
            with mock.patch.object(
                session, "now", lambda: first_month + relativedelta(months=1, days=1)
            ):
                gpa.RevPartnerGenerator(contract).generate(act_month)
                session.flush()

        export_object = (
            session.query(mapper.Export)
            .filter(
                (mapper.Export.type == "MONTH_PROC")
                & (mapper.Export.classname == "Client")
                & (mapper.Export.state == 0)
                & (mapper.Export.object_id == contract.client.id)
            )
            .one()
        )
        # без рефреша не подтягивает реальные данные в объект
        session.refresh(export_object)

        split_act_creation = session.config.get("ACT_SPLIT_ACT_CREATION", False)
        s_input = ut.Struct(export_object.input)
        act_accounter = create_act_accounter(
            contract.client, s_input, session, split_act_creation=split_act_creation
        )
        acts = act_accounter.do(skip_cut_agava=False)
        session.flush()

        order_act = self.get_order_act(acts)
        pa = get_pa(session, contract)
        assert sum(
            x[1] - x[4] - x[5] for x in list(completions)
        ) + TAXI_RF_MIN_ACT_AMOUNT == sum(q.current_qty for q in pa.consumes)
        assert len(order_act.rows) == 1
        assert order_act.rows[0].act_qty == D("1")
        assert (
            order_act.rows[0].consume.order.service_code == TAXI_RF_111_MIN_ACT_PRODUCT
        )
