# -*- coding: utf-8 -*-
import datetime as dt
import multiprocessing as m
import pytest

from Queue import Empty, Full

import balancer.test.util.sync as sync


# --------------------- Queue -------------------------


@pytest.mark.parametrize('timeout', [None, 0, 1.0])
def test_empty_queue_blocking(timeout):
    default_timeout = 5.0
    q = sync.Queue(default_timeout)

    assert q.empty()
    assert not q.full()
    assert q.qsize() == 0

    start = dt.datetime.now()

    with pytest.raises(Empty):
        q.get(block=True, timeout=timeout)

    finish = dt.datetime.now()

    target_timeout = timeout
    if timeout is None or timeout <= 0:
        target_timeout = default_timeout

    assert (finish - start) <= dt.timedelta(seconds=1.2 * target_timeout)

    assert q.empty()

    q.finish()

    assert q.empty()
    assert not q.full()
    assert q.qsize() == 0


@pytest.mark.parametrize('timeout', [None, 0, 42])
def test_empty_queue_nonblocking(timeout):
    default_timeout = 5.0
    q = sync.Queue(default_timeout)

    assert q.empty()
    assert not q.full()
    assert q.qsize() == 0

    with pytest.raises(Empty):
        q.get(block=False, timeout=timeout)

    assert q.empty()

    q.finish()

    assert q.empty()
    assert not q.full()
    assert q.qsize() == 0


class StopMarker(object):
    def __eq__(self, rhs):
        return isinstance(rhs, StopMarker)


STOP_MARKER = StopMarker()


class QueuePusher(object):
    def __init__(self, queue, objects_to_push, pre_run=None, post_run=None):
        super(QueuePusher, self).__init__()
        self._objects_to_push = objects_to_push
        self._queue = queue
        self._p = None
        self._result_queue = m.Queue()
        self._pre_run = pre_run
        self._post_run = post_run

    def start(self):
        if self._p:
            raise RuntimeError('QueuePusher has started already')
        self._p = m.Process(target=self.run)
        self._p.start()
        return self

    def join(self):
        if self._p:
            self._p.join()
            self._p = None
        return self

    def run(self):
        if self._pre_run:
            self._pre_run(self._queue, self._result_queue)

        for to_push in self._objects_to_push:
            self._queue.put(to_push)

        if self._post_run:
            self._post_run(self._queue, self._result_queue)


class QueuePopper(object):
    def __init__(self, queue):
        super(QueuePopper, self).__init__()
        self.objects = []
        self._queue = queue
        self._p = None
        self._result_queue = m.Queue()

    def start(self):
        if self._p:
            raise RuntimeError('QueuePopper has started already')
        self._p = m.Process(target=self.run)
        self._p.start()
        return self

    def join(self):
        if self._p:
            self._p.join()
            self._p = None
        return self

    def run(self):
        empty_retries_left = 3
        while True:
            try:
                self.objects.append(self._queue.get())
                if self.objects[-1] == STOP_MARKER:
                    break
            except Empty:
                empty_retries_left = empty_retries_left - 1
                if empty_retries_left <= 0:
                    break
        self._result_queue.put(self.objects)

    def extract_result(self):
        return self._result_queue.get()


@pytest.mark.parametrize('maxsize', [1, 100])
def test_produce_consume(maxsize):
    timeout = 5
    objects = range(1, 10)
    objects.append(STOP_MARKER)
    queue = sync.Queue(timeout, maxsize=maxsize)
    producer = QueuePusher(queue, objects)
    consumer = QueuePopper(queue)
    producer.start()
    consumer.start()
    producer.join()
    consumer.join()
    result_objects = consumer.extract_result()
    assert objects[:-1] == result_objects[:-1]


@pytest.mark.parametrize('block', [False, True], ids=['nonblock', 'block'])
def test_put_full(block):
    timeout = 1.0
    q = sync.Queue(timeout, maxsize=1)

    q.put(0)
    start = dt.datetime.now()
    with pytest.raises(Full):
        q.put(1, block=block)
    finish = dt.datetime.now()

    item = q.get()
    assert item == 0
    assert finish - start < dt.timedelta(seconds=timeout * 1.2)

    q.put(1)
    assert q.get() == 1


@pytest.mark.parametrize('how', ['close', 'finish'])
def test_close_empty(how):
    maxsize = 100
    timeout = 1
    objects = range(1, 10)
    objects.append(STOP_MARKER)
    queue = sync.Queue(timeout, maxsize=maxsize)

    def _pre_run(queue, result_queue):
        if how == 'close':
            queue.close()
        elif how == 'finish':
            queue.finish()

    def _post_run(queue, result_queue):
        result_queue.put('no exceptions raised')

    producer = QueuePusher(queue, objects, pre_run=_pre_run, post_run=_post_run)
    consumer = QueuePopper(queue)
    producer.start()
    consumer.start()
    producer.join()
    consumer.join()
    result_objects = consumer.extract_result()
    assert [] == result_objects


def test_close():
    maxsize = 100
    timeout = 1
    objects = range(1, 10)
    objects.append(STOP_MARKER)
    queue = sync.Queue(timeout, maxsize=maxsize)

    def _pre_run(queue, result_queue):
        queue.put('pre-close')
        queue.close()

    def _post_run(queue, result_queue):
        result_queue.put('no exceptions raised')

    producer = QueuePusher(queue, objects, pre_run=_pre_run, post_run=_post_run)
    consumer = QueuePopper(queue)
    producer.start()
    consumer.start()
    producer.join()
    consumer.join()
    result_objects = consumer.extract_result()
    assert ['pre-close'] == result_objects


# --------------------- Counter -------------------------

@pytest.mark.parametrize('val', [None, 0, 1, 111])
def test_counter(val):
    if val is None:
        c = sync.Counter()
        val = 0
    else:
        c = sync.Counter(default=val)

    assert c.value == val

    for i in xrange(1, 4):
        c.inc()
        assert c.value == val + i

    c.reset()
    assert c.value == val


def test_counter_mt():
    def _incrementor(counter, count):
        for i in xrange(count):
            counter.inc()

    counter = sync.Counter()

    single_count = 50
    processes = 4

    processors = [m.Process(target=_incrementor, args=(counter, single_count)) for i in xrange(processes)]
    for p in processors:
        p.start()

    for p in processors:
        p.join()

    counter_value = counter.value
    assert counter_value == single_count * processes


def test_counter_mt_reset():
    def _incrementor(counter, count):
        counter.reset()
        for i in xrange(count):
            counter.inc()

    counter = sync.Counter()

    single_count = 50
    processes = 4

    processors = [m.Process(target=_incrementor, args=(counter, single_count)) for i in xrange(processes)]
    for p in processors:
        p.start()

    for p in processors:
        p.join()

    counter_value = counter.value
    assert counter_value <= single_count * processes
