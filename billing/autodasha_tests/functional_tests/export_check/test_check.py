# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import datetime as dt

import pytest
from autodasha.tools.check_export.checker import ExportChecker
from tests.autodasha_tests.functional_tests.export_check.db_check_utils import (
    get_client, get_act, get_invoice, export_object
)


@pytest.fixture
def checker():
    return ExportChecker()


def extract_state_rate(queue_obj):
    return {(k, v[:2]) for k, v in queue_obj.parameters.iteritems()}


def extract_res_struct(res):
    return {(p.struct, e) for p, e in res}


def test_check_good(session, queue_object, checker):
    act = get_act(session)

    _, a_p = export_object(queue_object, act, 1)
    _, i_p = export_object(queue_object, act.invoice, 1)

    today = dt.datetime.combine(dt.date.today(), dt.time())
    a_p.last_update_dt = today
    i_p.last_update_dt = today

    session.flush()
    session.expire_all()

    req_parameters = {
        ('Act', 'OEBS', act.id): (1, 0, today),
        ('Invoice', 'OEBS', act.invoice_id): (1, 0, today),
    }

    res = checker.check(queue_object)

    assert res == []
    assert queue_object.parameters == req_parameters


def test_check_waiting(session, queue_object, checker):
    invoice = get_invoice(session)

    _, i_p = export_object(queue_object, invoice, 0)
    last_update_dt = dt.datetime.now() - dt.timedelta(minutes=1)
    i_p.last_update_dt = last_update_dt

    req_parameters = {
        ('Invoice', 'OEBS', invoice.id): (0, 0, last_update_dt),
    }

    session.flush()
    session.expire_all()

    res = checker.check(queue_object)

    assert res is None
    assert queue_object.parameters == req_parameters


def test_check_waiting_partial(session, queue_object, checker):
    act = get_act(session)

    _, a_p = export_object(queue_object, act, 1)
    _, i_p = export_object(queue_object, act.invoice, 0)
    last_update_dt = dt.datetime.now() - dt.timedelta(minutes=1)
    i_p.last_update_dt = last_update_dt
    a_p.last_update_dt = last_update_dt

    req_parameters = {
        ('Invoice', 'OEBS', act.invoice_id): (0, 0, last_update_dt),
        ('Act', 'OEBS', act.id): (1, 0, last_update_dt),
    }

    session.flush()
    session.expire_all()

    res = checker.check(queue_object)

    assert res is None
    assert queue_object.parameters == req_parameters


def test_check_waiting_retry(session, queue_object, checker):
    invoice = get_invoice(session)

    _, i_p = export_object(queue_object, invoice, 0, 666)
    prev_dt = dt.datetime.now() - dt.timedelta(minutes=1)
    i_p.last_update_dt = prev_dt
    i_p.last_rate = 0

    req_parameters = {
        (('Invoice', 'OEBS', invoice.id), (0, 666)),
    }

    session.flush()
    session.expire_all()

    start_dt = dt.datetime.now()
    res = checker.check(queue_object)

    assert res is None
    assert extract_state_rate(queue_object) == req_parameters
    assert i_p.last_update_dt >= start_dt


def test_check_waiting_long(session, queue_object, checker):
    invoice = get_invoice(session)

    i_e, i_p = export_object(queue_object, invoice, 0)
    prev_dt = dt.datetime.now() - dt.timedelta(minutes=666)
    i_p.last_update_dt = prev_dt
    i_e.error = 'Nope.'

    req_parameters = {
        ('Invoice', 'OEBS', invoice.id): (1, 0, prev_dt),
    }

    session.flush()
    session.expire_all()

    res = checker.check(queue_object)

    assert res == [(i_p, 'Nope.')]
    assert queue_object.parameters == req_parameters


def test_check_waiting_long_retry(session, queue_object, checker):
    invoice = get_invoice(session)

    i_e, i_p = export_object(queue_object, invoice, 0, 666)
    prev_dt = dt.datetime.now() - dt.timedelta(minutes=666)
    i_p.last_update_dt = prev_dt
    i_p.last_rate = 0
    i_e.error = 'Nope.'

    req_parameters = {
        (('Invoice', 'OEBS', invoice.id), (0, 666)),
    }

    session.flush()
    session.expire_all()

    start_dt = dt.datetime.now()
    res = checker.check(queue_object)

    assert res is None
    assert extract_state_rate(queue_object) == req_parameters
    assert i_p.last_update_dt >= start_dt


def test_check_bad(session, queue_object, checker):
    act = get_act(session)

    last_update_dt = dt.datetime.now() - dt.timedelta(minutes=1)

    a_e, a_p = export_object(queue_object, act, 2)
    a_e.error = 'NOOOOOO!!!!'
    a_p.last_update_dt = last_update_dt
    i_e, i_p = export_object(queue_object, act.invoice, 2)
    i_e.error = 'NOT THAT, PLEASE!!!!!'
    i_p.last_update_dt = last_update_dt

    req_parameters = {
        ('Invoice', 'OEBS', act.invoice_id): (1, 0, last_update_dt),
        ('Act', 'OEBS', act.id): (1, 0, last_update_dt),
    }

    session.flush()
    session.expire_all()

    res = checker.check(queue_object)

    assert extract_res_struct(res) == {(a_p.struct, 'NOOOOOO!!!!'), (i_p.struct, 'NOT THAT, PLEASE!!!!!')}
    assert queue_object.parameters == req_parameters


def test_check_bad_waiting(session, queue_object, checker):
    act = get_act(session)

    last_update_dt = dt.datetime.now() - dt.timedelta(minutes=1)

    a_e, a_p = export_object(queue_object, act, 2)
    a_e.error = 'NOOOOOO!!!!'
    a_p.last_update_dt = last_update_dt
    _, i_p = export_object(queue_object, act.invoice, 0)
    i_p.last_update_dt = last_update_dt

    req_parameters = {
        ('Invoice', 'OEBS', act.invoice_id): (0, 0, last_update_dt),
        ('Act', 'OEBS', act.id): (0, 0, last_update_dt),
    }

    session.flush()
    session.expire_all()

    res = checker.check(queue_object)

    assert res is None
    assert queue_object.parameters == req_parameters


def test_check_good_bad(session, queue_object, checker):
    act = get_act(session)

    last_update_dt = dt.datetime.now() - dt.timedelta(minutes=1)

    a_e, a_p = export_object(queue_object, act, 1)
    a_p.last_update_dt = last_update_dt
    i_e, i_p = export_object(queue_object, act.invoice, 2)
    i_e.error = 'NOT THAT, PLEASE!!!!!'
    i_p.last_update_dt = last_update_dt

    req_parameters = {
        ('Invoice', 'OEBS', act.invoice_id): (1, 0, last_update_dt),
        ('Act', 'OEBS', act.id): (1, 0, last_update_dt),
    }

    session.flush()
    session.expire_all()

    res = checker.check(queue_object)

    assert res == [(i_p, 'NOT THAT, PLEASE!!!!!')]
    assert queue_object.parameters == req_parameters


def test_check_bad_waiting_long(session, queue_object, checker):
    act = get_act(session)

    last_update_dt = dt.datetime.now() - dt.timedelta(minutes=1)
    old_dt = dt.datetime.now() - dt.timedelta(minutes=666)

    a_e, a_p = export_object(queue_object, act, 2)
    a_e.error = 'NOOOOOO!!!!'
    a_p.last_update_dt = last_update_dt
    i_e, i_p = export_object(queue_object, act.invoice, 0)
    i_p.last_update_dt = old_dt
    i_e.error = 'Nope.'

    req_parameters = {
        ('Invoice', 'OEBS', act.invoice_id): (1, 0, old_dt),
        ('Act', 'OEBS', act.id): (1, 0, last_update_dt),
    }

    session.flush()
    session.expire_all()

    res = checker.check(queue_object)

    assert extract_res_struct(res) == {(a_p.struct, 'NOOOOOO!!!!'), (i_p.struct, 'Nope.')}
    assert queue_object.parameters == req_parameters


def test_check_unknown_status(session, queue_object, checker):
    invoice = get_invoice(session)

    last_update_dt = dt.datetime.now() - dt.timedelta(minutes=1)
    i_e, i_p = export_object(queue_object, invoice, 3)
    i_p.last_update_dt = last_update_dt

    req_parameters = {
        ('Invoice', 'OEBS', invoice.id): (1, 0, last_update_dt),
    }

    session.flush()
    session.expire_all()

    res = checker.check(queue_object)

    assert res == []
    assert queue_object.parameters == req_parameters


def test_check_timeout_waiting(session, queue_object, checker):
    client = get_client(session)

    e, c = export_object(queue_object, client, 0, type_='CLIENT_BATCH')
    prev_dt = dt.datetime.now() - dt.timedelta(minutes=666)
    c.last_update_dt = prev_dt
    e.error = 'Nope.'

    req_parameters = {
        ('Client', 'CLIENT_BATCH', client.id): (0, 0, prev_dt),
    }

    session.flush()
    session.expire_all()

    res = checker.check(queue_object)

    assert res is None
    assert queue_object.parameters == req_parameters


def test_check_timeout_waiting_too_long(session, queue_object, checker):
    client = get_client(session)

    e, c = export_object(queue_object, client, 0, type_='CLIENT_BATCH')
    prev_dt = dt.datetime.now() - dt.timedelta(minutes=66666)
    c.last_update_dt = prev_dt
    e.error = 'Nope.'

    req_parameters = {
        ('Client', 'CLIENT_BATCH', client.id): (1, 0, prev_dt),
    }

    session.flush()
    session.expire_all()

    res = checker.check(queue_object)

    assert res == [(c, 'Nope.')]
    assert queue_object.parameters == req_parameters


def test_check_max_retry(session, queue_object, checker):
    client = get_client(session)

    e, c = export_object(queue_object, client, 0, 1, type_='CLIENT_BATCH')
    prev_dt = dt.datetime.now() - dt.timedelta(minutes=1)
    e.error = 'Nope.'
    c.last_update_dt = prev_dt

    req_parameters = {
        ('Client', 'CLIENT_BATCH', client.id): (1, 0, prev_dt),
    }

    session.flush()
    session.expire_all()

    res = checker.check(queue_object)

    assert res == [(c, 'Nope.')]
    assert queue_object.parameters == req_parameters
