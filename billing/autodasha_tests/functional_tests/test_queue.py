# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import collections
import datetime as dt
import uuid
import time

import mock
import pytest

from autodasha.core import queue
from autodasha.db import mapper as a_mapper


class QueueObjectTest(a_mapper.QueueObject):
    __mapper_args__ = {'polymorphic_identity': 'TEST'}


class ProcessorTest(queue.AbstractProcessor):
    NAME = 'TEST'
    DELAY = 666
    NUM_RETRIES = 666

    def _do_process(self, queue_object):
        queue_object.parameters += 666

    def process(self, queue_object):
        with queue_object.session.begin_nested():
            self._do_process(queue_object)


@pytest.fixture
def queue_objects(request, session):
    res = []
    for fixture_param in request.param:
        i = a_mapper.Issue(id=uuid.uuid4().hex, key=uuid.uuid4().hex)
        session.add(i)

        if isinstance(fixture_param, collections.Iterable) and not isinstance(fixture_param, basestring):
            q_param, next_dt = fixture_param
        else:
            q_param, next_dt = fixture_param, None

        res.append(i.enqueue('TEST', q_param, next_dt))

    return res


@pytest.mark.parametrize(['queue_objects', 'input_params'], [[(-1, 0, 1, 6)] * 2], indirect=['queue_objects'], ids=[''])
def test_instant_simple(session, queue_objects, input_params):
    processor = ProcessorTest()
    loop = queue.QueueProcessingLoop(processor)

    start_dt = dt.datetime.now().replace(microsecond=0)
    loop.run(session)
    finish_dt = dt.datetime.now()

    for queue_object, inp_param in zip(queue_objects, input_params):
        session.refresh(queue_object)
        assert queue_object.parameters == inp_param + 666
        assert queue_object.state == 1
        assert queue_object.rate == 0
        assert queue_object.next_dt <= start_dt
        assert start_dt <= queue_object.processed_dt <= finish_dt


@pytest.mark.parametrize(['queue_objects'], [[(-1, 0, 1)]], indirect=True, ids=[''])
def test_waiting(session, queue_objects):
    processor = ProcessorTest()
    loop = queue.QueueProcessingLoop(processor, run_min=2, sleep_sec=10)

    with session.begin():
        qo1, qo2, qo3 = queue_objects
        qo2.next_dt = dt.datetime.now() + dt.timedelta(seconds=30)
        qo3.next_dt = dt.datetime.now() + dt.timedelta(seconds=60)

    start_dt = dt.datetime.now().replace(microsecond=0)
    loop.run(session)
    finish_dt = dt.datetime.now()

    assert finish_dt - start_dt <= dt.timedelta(minutes=2, seconds=10)

    assert all(q.state == 1 for q in queue_objects)
    assert all(q.rate == 0 for q in queue_objects)
    assert all(start_dt <= q.processed_dt <= finish_dt for q in queue_objects)
    assert all(q.next_dt <= finish_dt for q in queue_objects)

    assert [(q.parameters == (i - 1) + 666) for i, q in enumerate(queue_objects)]

    proc_dt_list = [q.processed_dt for q in queue_objects]
    assert proc_dt_list == sorted(proc_dt_list)


def _do_process_delay_fail(self, queue_object):
    if queue_object.parameters == 'delay':
        raise queue.DelayException
    elif queue_object.parameters == 'fail':
        assert 'War' is 'Peace'
    else:
        queue_object.parameters = 'done'


@mock.patch.object(ProcessorTest, '_do_process', _do_process_delay_fail)
@pytest.mark.parametrize(['queue_objects'], [[('delay', 'fail', 'process', 'fail')]], indirect=True, ids=[''])
def test_instant_delay_fail(session, queue_objects):
    q_delay, q_fail, q_process, q_last_fail = queue_objects

    with session.begin():
        q_last_fail.rate = 665

    processor = ProcessorTest()
    loop = queue.QueueProcessingLoop(processor)

    start_dt = dt.datetime.now().replace(microsecond=0)
    loop.run(session)
    finish_dt = dt.datetime.now()

    map(session.refresh, queue_objects)

    assert q_delay.parameters == 'delay'
    assert q_delay.state == 0
    assert q_delay.rate == 0
    assert q_delay.processed_dt is None
    assert q_delay.next_dt >= start_dt + dt.timedelta(minutes=666)

    assert q_fail.parameters == 'fail'
    assert q_fail.state == 0
    assert q_fail.rate == 1
    assert q_fail.processed_dt is None
    assert q_fail.next_dt >= start_dt + dt.timedelta(minutes=666)

    assert q_process.parameters == 'done'
    assert q_process.state == 1
    assert q_process.rate == 0
    assert start_dt <= q_process.processed_dt <= finish_dt
    assert q_process.next_dt <= start_dt

    assert q_last_fail.parameters == 'fail'
    assert q_last_fail.state == 2
    assert q_last_fail.rate == 666
    assert q_last_fail.processed_dt is None
    assert q_last_fail.next_dt <= start_dt


def _do_process_sleep(self, queue_object):
    queue_object.parameters += 666
    time.sleep(40)


@mock.patch.object(ProcessorTest, '_do_process', _do_process_sleep)
@pytest.mark.parametrize(['queue_objects'], [[(-1, 0, 1)]], indirect=True, ids=[''])
def test_instant_bound(session, queue_objects):
    processor = ProcessorTest()
    loop = queue.QueueProcessingLoop(processor, stop_min=1)

    with session.begin():
        qo1, qo2, qo3 = queue_objects
        qo2.next_dt = dt.datetime.now() + dt.timedelta(seconds=10)
        qo3.next_dt = dt.datetime.now() + dt.timedelta(seconds=20)

    start_dt = dt.datetime.now().replace(microsecond=0)
    loop.run(session)
    finish_dt = dt.datetime.now()

    assert finish_dt - start_dt <= dt.timedelta(minutes=1, seconds=25)

    assert [q.state for q in queue_objects] == [1, 1, 0]
    assert all(q.rate == 0 for q in queue_objects)
    assert [bool(q.processed_dt) for q in queue_objects] == [True, True, False]
    assert all(q.next_dt <= finish_dt for q in queue_objects)

    assert [(q.parameters == (i - 1) + 666) for i, q in enumerate(queue_objects)]


@pytest.mark.parametrize(['queue_objects'], [[(-1, 0, 1)]], indirect=True, ids=[''])
def test_waiting_bound(session, queue_objects):
    processor = ProcessorTest()
    loop = queue.QueueProcessingLoop(processor, stop_min=1, run_min=2, sleep_sec=10)

    with session.begin():
        qo1, qo2, qo3 = queue_objects
        qo2.next_dt = dt.datetime.now() + dt.timedelta(seconds=40)
        qo3.next_dt = dt.datetime.now() + dt.timedelta(seconds=70)

    start_dt = dt.datetime.now().replace(microsecond=0)
    loop.run(session)
    finish_dt = dt.datetime.now()

    assert finish_dt - start_dt <= dt.timedelta(minutes=1, seconds=5)

    assert [q.state for q in queue_objects] == [1, 1, 0]
    assert all(q.rate == 0 for q in queue_objects)
    assert [bool(q.processed_dt) for q in queue_objects] == [True, True, False]
    assert [q.next_dt <= finish_dt for q in queue_objects] == [True, True, False]

    assert [(q.parameters == (i - 1) + 666) for i, q in enumerate(queue_objects)]
