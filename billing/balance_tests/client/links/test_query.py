# -*- coding: utf-8 -*-

import datetime

import pytest

from balance import muzzle_util as ut
from balance.actions import linked_clients
from balance.constants import (
    ClientLinkType,
)

from tests import object_builder as ob

pytestmark = [
    pytest.mark.linked_clients,
]

CLIENT_TYPE_CORE = 'core'
CLIENT_TYPE_BRAND = 'brand'
CLIENT_TYPE_ALIAS = 'alias'

RESULT_TYPE_CLIENT = 'client'
RESULT_TYPE_BRAND_CLIENT = 'brand_client'
RESULT_TYPE_BOTH = 'both'


def _add_result_type(builder, result_type):
    if result_type == RESULT_TYPE_CLIENT:
        return builder.with_client()
    elif result_type == RESULT_TYPE_BRAND_CLIENT:
        return builder.with_brand_client()
    elif result_type == RESULT_TYPE_BOTH:
        return builder.with_both_clients()


def _assert_result_type(res, result_type, checked_client, brand_clients):
    if result_type == RESULT_TYPE_CLIENT:
        assert [r.client_id for r in res] == [checked_client.id]
    elif result_type == RESULT_TYPE_BRAND_CLIENT:
        assert sorted([r.client_id for r in res]) == sorted([cl.id for cl in brand_clients])
    elif result_type == RESULT_TYPE_BOTH:
        req_res = sorted([(checked_client.id, cl.id) for cl in brand_clients])
        assert sorted([(r.client_id, r.brand_client_id) for r in res]) == req_res


class TestForDt(object):
    @pytest.mark.parametrize('with_self', [False, True])
    def test_empty(self, session, with_self):
        client = ob.ClientBuilder.construct(session)

        select = (
            linked_clients.LinkedClientsSelectBuilder(True)
                .with_brand_client()
                .for_dt(datetime.datetime.now())
                .for_clients(client.id)
                .for_group_types(*ClientLinkType.ALL_TYPES)
                .get(with_self)
        )
        res = session.execute(select)
        if with_self:
            assert [r.client_id for r in res] == [client.id]
        else:
            assert list(res) == []

    @pytest.mark.parametrize('with_self', [False, True])
    def test_alias(self, session, with_self):
        client = ob.ClientBuilder.construct(session)
        aliased_client = ob.ClientBuilder.construct(session)
        aliased_client.make_equivalent(client)

        select = (
            linked_clients.LinkedClientsSelectBuilder(True)
                .with_brand_client()
                .for_dt(datetime.datetime.now())
                .for_clients(client.id)
                .for_group_types(*ClientLinkType.ALL_TYPES)
                .get(with_self)
        )
        res = session.execute(select)
        assert sorted([r.client_id for r in res]) == sorted([client.id, aliased_client.id])

    @pytest.mark.parametrize(
        'dt_delta, is_ok, with_self',
        [
            pytest.param(-11, False, True, id='before'),
            pytest.param(-10, True, True, id='past'),
            pytest.param(0, True, True, id='present_with_self'),
            pytest.param(0, True, False, id='present_without_self'),
            pytest.param(9, True, True, id='future'),
            pytest.param(10, False, True, id='after'),
        ]
    )
    def test_brand_base(self, session, dt_delta, is_ok, with_self):
        client = ob.ClientBuilder.construct(session)
        brand_client = ob.ClientBuilder.construct(session)

        start_dt = ut.trunc_date(datetime.datetime.now() - datetime.timedelta(10))
        finish_dt = start_dt + datetime.timedelta(20)

        ob.create_brand(
            session,
            [(start_dt, [client, brand_client])],
            finish_dt
        )

        select = (
            linked_clients.LinkedClientsSelectBuilder(True)
                .with_brand_client()
                .for_dt(datetime.datetime.now() + datetime.timedelta(dt_delta))
                .for_clients(client.id)
                .for_group_types(*ClientLinkType.BRAND_TYPES)
                .get(with_self)
        )
        res = session.execute(select)
        if is_ok:
            assert sorted([r.client_id for r in res]) == sorted([client.id, brand_client.id])
        else:
            assert list(res) == []

    @pytest.mark.parametrize('with_self', [False, True])
    @pytest.mark.parametrize('result_type', [RESULT_TYPE_CLIENT, RESULT_TYPE_BRAND_CLIENT, RESULT_TYPE_BOTH])
    def test_brand_result_type(self, session, result_type, with_self):
        client = ob.ClientBuilder.construct(session)
        brand_client = ob.ClientBuilder.construct(session)

        start_dt = ut.trunc_date(datetime.datetime.now() - datetime.timedelta(10))
        finish_dt = start_dt + datetime.timedelta(20)

        ob.create_brand(
            session,
            [(start_dt, [client, brand_client])],
            finish_dt
        )

        select = (
            linked_clients.LinkedClientsSelectBuilder(result_type != RESULT_TYPE_CLIENT)
                .for_dt(datetime.datetime.now())
                .for_clients(client.id)
                .for_group_types(*ClientLinkType.ALL_TYPES)
        )
        select = _add_result_type(select, result_type)
        res = session.execute(select.get(with_self))

        _assert_result_type(res, result_type, client, [client, brand_client])

    @pytest.mark.parametrize(
        'dt_delta, check_idx, res_idx',
        [
            pytest.param(-1, 0, [0, 1, 2], id='main_before_change'),
            pytest.param(2, 0, [0, 1, 3], id='main_after_change'),
            pytest.param(-1, 3, [], id='added_before_change'),
            pytest.param(2, 3, [0, 1, 3], id='added_after_change'),
            pytest.param(-1, 2, [0, 1, 2], id='removed_before_change'),
            pytest.param(2, 2, [], id='removed_after_change'),
        ]
    )
    def test_brand_change(self, session, dt_delta, check_idx, res_idx):
        start_idx = [0, 1, 2]
        change_idx = [0, 1, 3]

        clients = [ob.ClientBuilder.construct(session) for _ in range(4)]

        change_dt = ut.trunc_date(datetime.datetime.now())
        start_dt = change_dt - datetime.timedelta(10)
        finish_dt = change_dt + datetime.timedelta(10)

        ob.create_brand(
            session,
            [
                (start_dt, [clients[idx] for idx in start_idx]),
                (change_dt, [clients[idx] for idx in change_idx]),
            ],
            finish_dt
        )

        select = (
            linked_clients.LinkedClientsSelectBuilder(True)
                .with_brand_client()
                .for_dt(datetime.datetime.now() + datetime.timedelta(dt_delta))
                .for_clients(clients[check_idx].id)
                .for_group_types(*ClientLinkType.BRAND_TYPES)
                .get()
        )
        res = sorted([r.client_id for r in session.execute(select)])
        req_res = sorted([clients[idx].id for idx in res_idx])
        assert res == req_res

    @pytest.mark.parametrize(
        'dt_delta, is_ok',
        [
            pytest.param(-20, False, id='past'),
            pytest.param(6, True, id='present'),
            pytest.param(20, False, id='future'),
        ]
    )
    @pytest.mark.parametrize('src_client_type', [CLIENT_TYPE_CORE, CLIENT_TYPE_BRAND, CLIENT_TYPE_ALIAS])
    def test_brand_w_alias(self, session, dt_delta, is_ok, src_client_type):
        client = ob.ClientBuilder.construct(session)
        brand_client = ob.ClientBuilder.construct(session)
        alias_client = ob.ClientBuilder.construct(session)
        alias_client.make_equivalent(brand_client)

        start_dt = ut.trunc_date(datetime.datetime.now() - datetime.timedelta(10))
        finish_dt = start_dt + datetime.timedelta(20)

        ob.create_brand(
            session,
            [(start_dt, [client, brand_client])],
            finish_dt
        )

        if src_client_type == CLIENT_TYPE_CORE:
            src_client = client
        elif src_client_type == CLIENT_TYPE_BRAND:
            src_client = brand_client
        elif src_client_type == CLIENT_TYPE_ALIAS:
            src_client = alias_client

        select = (
            linked_clients.LinkedClientsSelectBuilder(True)
                .with_brand_client()
                .for_dt(datetime.datetime.now() + datetime.timedelta(dt_delta))
                .for_clients(src_client.id)
                .for_group_types(*ClientLinkType.BRAND_TYPES)
                .get()
        )
        res = session.execute(select)
        if is_ok:
            assert sorted([r.client_id for r in res]) == sorted([client.id, brand_client.id, alias_client.id])
        else:
            assert list(res) == []

    @pytest.mark.parametrize('src_type', ClientLinkType.BRAND_TYPES)
    @pytest.mark.parametrize('flt_type', ClientLinkType.BRAND_TYPES)
    def test_brand_type_filter(self, session, src_type, flt_type):
        client = ob.ClientBuilder.construct(session)
        brand_client = ob.ClientBuilder.construct(session)

        start_dt = ut.trunc_date(datetime.datetime.now() - datetime.timedelta(10))
        finish_dt = start_dt + datetime.timedelta(20)

        ob.create_brand(
            session,
            [(start_dt, [client, brand_client])],
            finish_dt,
            src_type
        )

        select = (
            linked_clients.LinkedClientsSelectBuilder(True)
                .with_brand_client()
                .for_dt(datetime.datetime.now())
                .for_clients(client.id)
                .for_group_types(flt_type)
                .get()
        )
        res = session.execute(select)
        if src_type == flt_type:
            assert sorted([r.client_id for r in res]) == sorted([client.id, brand_client.id])
        else:
            assert list(res) == []

    def test_brand_condition(self, session):
        client = ob.ClientBuilder.construct(session)
        brand_client1 = ob.ClientBuilder.construct(session)
        brand_client2 = ob.ClientBuilder.construct(session)

        start_dt = ut.trunc_date(datetime.datetime.now() - datetime.timedelta(10))
        finish_dt = start_dt + datetime.timedelta(20)

        ob.create_brand(
            session,
            [(start_dt, [client, brand_client1, brand_client2])],
            finish_dt,
        )

        builder = (
            linked_clients.LinkedClientsSelectBuilder(True)
                .with_brand_client()
                .for_dt(datetime.datetime.now())
                .for_clients(client.id)
                .for_group_types(ClientLinkType.DIRECT)
        )
        select = builder.where(builder.l.c.client_id != builder.r.c.client_id).get()
        res = session.execute(select)

        assert sorted([r.client_id for r in res]) == sorted([brand_client1.id, brand_client2.id])


class TestForPeriod(object):

    @pytest.mark.parametrize(
        'period_from, period_to, check_idx, res_idx',
        [
            pytest.param(-20, -10, 0, [], id='past'),
            pytest.param(10, 20, 0, [], id='future'),
            pytest.param(-11, -9, 0, [0, 1, 2], id='part_past'),
            pytest.param(9, 20, 0, [0, 1, 3], id='part_future'),
            pytest.param(-5, -3, 0, [0, 1, 2], id='main_before_change'),
            pytest.param(3, 5, 0, [0, 1, 3], id='main_after_change'),
            pytest.param(-5, 5, 0, [0, 1, 2, 3], id='main_on_change'),
            pytest.param(-5, 5, 3, [0, 1, 3], id='added_on_change'),
            pytest.param(-5, 5, 2, [0, 1, 2], id='removed_on_change'),
        ]
    )
    def test_brand(self, session, period_from, period_to, check_idx, res_idx):
        start_idx = [0, 1, 2]
        change_idx = [0, 1, 3]

        clients = [ob.ClientBuilder.construct(session) for _ in range(4)]

        change_dt = ut.trunc_date(datetime.datetime.now())
        start_dt = change_dt - datetime.timedelta(10)
        finish_dt = change_dt + datetime.timedelta(10)

        ob.create_brand(
            session,
            [
                (start_dt, [clients[idx] for idx in start_idx]),
                (change_dt, [clients[idx] for idx in change_idx]),
            ],
            finish_dt
        )

        select = (
            linked_clients.LinkedClientsSelectBuilder(True)
                .with_brand_client()
                .for_period(change_dt + datetime.timedelta(period_from), change_dt + datetime.timedelta(period_to))
                .for_clients(clients[check_idx].id)
                .for_group_types(*ClientLinkType.BRAND_TYPES)
                .get()
        )
        res = sorted([r.client_id for r in session.execute(select)])
        req_res = sorted([clients[idx].id for idx in res_idx])
        assert res == req_res

    @pytest.mark.parametrize('with_self', [False, True])
    @pytest.mark.parametrize('result_type', [RESULT_TYPE_CLIENT, RESULT_TYPE_BRAND_CLIENT, RESULT_TYPE_BOTH])
    def test_brand_result_type(self, session, result_type, with_self):
        clients = [ob.ClientBuilder.construct(session) for _ in range(3)]

        change_dt = ut.trunc_date(datetime.datetime.now())
        start_dt = change_dt - datetime.timedelta(10)
        finish_dt = change_dt + datetime.timedelta(10)

        ob.create_brand(
            session,
            [
                (start_dt, clients[:-1]),
                (change_dt, clients[1:]),
            ],
            finish_dt
        )

        select = (
            linked_clients.LinkedClientsSelectBuilder(result_type != RESULT_TYPE_CLIENT)
                .for_period(change_dt - datetime.timedelta(1), change_dt + datetime.timedelta(1))
                .for_clients(clients[1].id)
                .for_group_types(*ClientLinkType.ALL_TYPES)
        )
        select = _add_result_type(select, result_type)
        res = session.execute(select.get(with_self))

        _assert_result_type(res, result_type, clients[1], clients)
