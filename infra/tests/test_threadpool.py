#!/usr/bin/env python

import unittest
import Queue
import time
import mock

from hbfagent.agent import ThreadPoolExecutorLimitedQueue


class TestThreadPoolExecutorLimitedQueue(unittest.TestCase):
    def setUp(self):
        self.max_workers = 1
        self.queue_limit = 2
        self.thread_pool_executor = ThreadPoolExecutorLimitedQueue(
            max_workers=self.max_workers,
            queue_limit=self.queue_limit,
        )
        self.thread_fn = self.thread_sleep_fn
        self.timeout = 3

    def thread_sleep_fn(self, timeout):
        time.sleep(timeout)

    def test_fail_on_put(self):

        with mock.patch('hbfagent.agent.ThreadPoolExecutorLimitedQueue._adjust_thread_count') as m:
            for i in range(self.queue_limit):
                self.thread_pool_executor.submit(self.thread_fn, self.timeout)
        assert m.call_count == self.queue_limit

        with mock.patch('hbfagent.agent.ThreadPoolExecutorLimitedQueue._adjust_thread_count'):
            with self.assertRaises(Queue.Full):
                self.thread_pool_executor.submit(self.thread_fn, self.timeout)
