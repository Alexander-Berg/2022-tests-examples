"""
Test prof module
"""
import unittest
import time

from sepelib.util.prof.timer import SimpleTimer


class TestSimpleTimer(unittest.TestCase):
    def test_double_stop_start(self):
        t = SimpleTimer()
        t.start()
        self.assertRaises(RuntimeError, t.start)
        t.stop()
        self.assertRaises(RuntimeError, t.stop)

    def test_context_manager(self):
        t = SimpleTimer()
        with t:
            time.sleep(0.1)
        # check that we're stopped
        self.assertRaises(RuntimeError, t.stop)
        self.assertTrue(t.elapsed > 0)
