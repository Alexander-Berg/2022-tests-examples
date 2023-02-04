# -*- coding: utf-8 -*-
from __future__ import absolute_import

import mock
import pytest
import tornado.gen
import tornado.ioloop
import tornado.locks
import tornado.testing

from agent import ticker as ticker_
from agent import exceptions


class NoopTicker(ticker_.Ticker):

    def __init__(self, cb, **kwargs):
        self._cb = cb
        self._cancel_cb = kwargs.pop("cancel_cb", lambda: None)

        args = {"interval": 30}
        args.update(kwargs)

        super(NoopTicker, self).__init__(**args)

    def tick(self):
        return self._cb()

    def on_cancel(self):
        return self._cancel_cb()


class TestTicker(tornado.testing.AsyncTestCase):

    def setUp(self):
        super(TestTicker, self).setUp()
        self.cb = mock.Mock()
        self.cb.side_effect = lambda *args: None

    @tornado.testing.gen_test
    def test_spin(self):
        ticker = NoopTicker(self.cb)
        yield ticker.wait()
        for _ in xrange(2):
            # spin may be called many times
            tornado.ioloop.IOLoop.current().add_callback(ticker.spin)
        yield tornado.gen.sleep(0.3)
        yield ticker.cancel()
        assert self.cb.call_count == 2

    @tornado.testing.gen_test
    def test_inner_spin(self):
        loop = tornado.ioloop.IOLoop.current()
        ticker = NoopTicker(lambda: self.cb(ticker.spin()), interval=3600)
        yield ticker.wait()
        # time should be monotonic
        started = loop.time()
        while self.cb.call_count <= 10 and (loop.time() - started) < 5.0:
            yield tornado.gen.sleep(0.01)
        yield ticker.cancel()
        assert self.cb.call_count >= 10

    @tornado.testing.gen_test
    def test_cancel_on_start(self):
        cancel_cb = mock.Mock()
        ticker = NoopTicker(self.cb, cancel_cb=cancel_cb)
        yield ticker.cancel()
        yield tornado.gen.sleep(0.95)
        assert not self.cb.called
        assert cancel_cb.call_count == 1

    @tornado.testing.gen_test
    def test_cancel_after_start(self):
        cancel_cb = mock.Mock()
        ticker = NoopTicker(self.cb, cancel_cb=cancel_cb, interval=0.1)
        yield ticker.wait()
        tornado.ioloop.IOLoop.current().add_callback(ticker.cancel)
        yield tornado.gen.sleep(0.25)
        assert self.cb.call_count >= 1
        assert cancel_cb.call_count == 1

    @tornado.testing.gen_test
    def test_error_handling(self):
        self.cb.side_effect = NotImplementedError
        ticker = NoopTicker(self.cb)
        with pytest.raises(NotImplementedError):
            yield ticker.wait()

        tornado.ioloop.IOLoop.current().add_callback(ticker.spin)
        yield tornado.gen.sleep(0.3)
        assert self.cb.call_count == 2

    @tornado.testing.gen_test
    def test_parallel_spin(self):
        stack = []
        start_event = tornado.locks.Event()
        sync_event = tornado.locks.Event()

        @tornado.gen.coroutine
        def coroutine():
            start_event.set()
            stack.append(None)
            yield sync_event.wait()

        ticker = NoopTicker(coroutine)
        yield start_event.wait()
        ticker.spin()
        yield tornado.gen.sleep(0.11)
        assert len(stack) == 1, "parallel coroutines found"

        sync_event.set()
        yield ticker.wait()

    @tornado.testing.gen_test
    def test_retrying(self):
        self.cb.side_effect = exceptions.RetryError
        ticker = NoopTicker(self.cb)
        yield tornado.gen.moment

        def reset():
            self.cb.side_effect = lambda: None

        tornado.ioloop.IOLoop.current().add_callback(reset)
        yield ticker.wait()
        assert self.cb.call_count == 2

    @tornado.testing.gen_test
    def test_round_by_interval(self):
        ticker = NoopTicker(self.cb, interval=0.01, round_by_interval=True)
        # simply test that code don't crash
        for _ in xrange(2):
            yield ticker.wait()
        assert self.cb.call_count == 2


class TestMonotonicLoopingCall(tornado.testing.AsyncTestCase):

    @tornado.testing.gen_test
    def test_interval_execution(self):
        callback = mock.Mock()
        callback.side_effect = lambda: None
        looping = ticker_.LoopingCall(name="random", function=callback, interval=0.2)
        yield tornado.gen.sleep(0.25)
        yield looping.cancel()
        # we can sleep for more than 0.25 seconds
        assert callback.call_count >= 2

    @tornado.testing.gen_test
    def test_async_function(self):
        callback = mock.Mock()
        callback.side_effect = lambda: tornado.gen.sleep(0.2)
        looping = ticker_.LoopingCall(name="random", function=callback, interval=0.001)
        # looping call _would_ call the function many times during this sleep
        # but since function takes a long time to return it doesn't
        yield tornado.gen.sleep(0.1)
        yield looping.cancel()
        # called only once and sleeping
        assert callback.call_count == 1
