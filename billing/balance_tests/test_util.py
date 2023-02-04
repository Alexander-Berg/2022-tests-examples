# -*- coding: utf-8 -*-

# Some obscure tests, maybe vaclav knows what they are good for

import unittest
import time
import xml.etree.ElementTree as et
import re

from balance import util as ut
from balance import test_cmn as tcmn
from balance import muzzle_util as utm
from balance import xmlizer as xr

from balance.utils import serialize

class TestLogTimeConsumed(unittest.TestCase):
    def testNormal(self):
        log = tcmn.MockLog()
        ut.log_time_consumed(log, lambda: time.sleep(2), 'test1')
        self.assertEqual(['D', 'D'], [x[0] for x in log.lines])
        self.assertEqual('started test1', log.lines[0][1])
        r = re.compile('finished test1, elapsed ([0-9.]+) ms')
        res = r.match(log.lines[1][1])
        self.assert_(res)
        t = float(res.group(1))
        self.assert_(t > 1500 and t < 3000)
    def testRaise(self):
        log = tcmn.MockLog()
        self.assertRaises(Exception, lambda: ut.log_time_consumed(log, tcmn.func_raises, 'test1'))
        self.assertEqual(['D', 'D'], [x[0] for x in log.lines])

class TestRunOnConnection(unittest.TestCase):
    def testRun(self):
        connect = tcmn.MockConnection()
        self.assert_(not connect.closed)
        ut.run_on_connection(connect, lambda(c): False)
        self.assert_(connect.closed)
    def testFail(self):
        connect = tcmn.MockConnection()
        self.assert_(not connect.closed)
        self.assertRaises(Exception, lambda: ut.run_on_connection(connect, lambda(c): tcmn.func_raises()))
        self.assert_(connect.closed)

class TestSqlSelect(unittest.TestCase):
    def test1(self):
        connect = tcmn.MockConnection([('2', 'b')])
        rows = ut.sql_select(connect, 'a', (1,))
        self.assert_(not connect.closed)
        self.assertEqual(1, len(connect.cursors))
        self.assert_(not connect.cursors[0].invalid)
        self.assertEqual(1, connect.cursors[0].execute__called)
        self.assertEqual('a', connect.cursors[0].execute__sql)
        self.assertEqual((1,), connect.cursors[0].execute__args)
        self.assertEqual(1, connect.cursors[0].fetchall__called)
        self.assertEqual([('2', 'b')], rows)
        self.assertEqual(0, connect.cursors[0].callproc__called)

class TestReportException(unittest.TestCase):
    def test1(self):
        try:
            raise tcmn.MockError()
        except Exception, e:
            log = tcmn.MockLog()
            ut.report_exception(log, e)
            self.assertEqual(1, len(log.lines))
            self.assertEqual('E', log.lines[0][0])
            r = re.compile('MockError: \n  File "[./a-zA-Z0-9_-]+.py", line [0-9]+, in test1\n')
            self.assert_(r.match(log.lines[0][1]))

class TestTry1(unittest.TestCase):
    def testNormal(self):
        cnt_i = [0]
        cnt_e = [0]
        self.assertEqual(123, ut.try1(lambda: 123, lambda(e): tcmn.incr(cnt_i), lambda(e): tcmn.incr(cnt_e)))
        self.assertEqual([0], cnt_i)
        self.assertEqual([0], cnt_e)
    def testError(self):
        cnt_i = [0]
        cnt_e = [0]
        self.assertEqual(1, ut.try1(lambda: tcmn.func_raises(tcmn.MockError), lambda(e): tcmn.incr(cnt_i), lambda(e): tcmn.incr(cnt_e)))
        self.assertEqual([0], cnt_i)
        self.assertEqual([1], cnt_e)
    def testError2(self):
        cnt_i = [0]
        cnt_e = [0]
        self.assertEqual(1, ut.try1(lambda: tcmn.func_raises(Exception), lambda(e): tcmn.incr(cnt_i), lambda(e): tcmn.incr(cnt_e)))
        self.assertEqual([0], cnt_i)
        self.assertEqual([1], cnt_e)
    def testSystemExit(self):
        cnt_i = [0]
        cnt_e = [0]
        self.assertRaises(SystemExit, lambda: ut.try1(
            lambda: tcmn.func_raises(SystemExit), lambda(e): tcmn.incr(cnt_i), lambda(e): tcmn.incr(cnt_e)))
        self.assertEqual([0], cnt_i)
        self.assertEqual([0], cnt_e)
    def testSignal(self):
        from butils.application.sysprocess_tools import SignalException
        for ex in [SignalException, KeyboardInterrupt]:
            cnt_i = [0]
            cnt_e = [0]
            self.assertRaises(ex, lambda: ut.try1(
                lambda: tcmn.func_raises(ex), lambda(e): tcmn.incr(cnt_i), lambda(e): tcmn.incr(cnt_e)))
            self.assertEqual([1], cnt_i)
            self.assertEqual([0], cnt_e)

def do_raise(ex):
    raise ex

class TestException(unittest.TestCase):
    def test1(self):
        ex = utm.INVALID_BAD_DEBT_SUM(123, 567)
        self.assertEqual('Invalid bad debt sum 567 for invoice 123', str(ex))
        self.assertEqual(
            '<error><msg>Invalid bad debt sum 567 for invoice 123</msg>'
            '<invoice-id>123</invoice-id><debt-sum>567</debt-sum><wo-rollback>0</wo-rollback><method>xyz</method>'
            '<code>INVALID_BAD_DEBT_SUM</code><parent-codes><code>INVALID_PARAM</code>'
            '<code>EXCEPTION</code></parent-codes><contents>Invalid bad debt sum 567 for invoice 123</contents></error>',
            xr.xml2str(xr.getxmlizer(ex).xmlize('xyz')))

    def test2(self):
        ex = utm.EXCEPTION('abc')
        self.assertEqual('Unknown error occurred in Balance engine', str(ex))
        self.assertEqual(
            '<error>'
            '<msg>Unknown error occurred in Balance engine</msg>'
            '<wo-rollback>abc</wo-rollback>'
            '<method>xyz</method>'
            '<code>EXCEPTION</code>'
            '<parent-codes />'
            '<contents>Unknown error occurred in Balance engine</contents>'
            '</error>',
            xr.xml2str(xr.getxmlizer(ex).xmlize('xyz')))

    def test3(self):
        self.assertRaises(utm.INVALID_PARAM, lambda:
                          do_raise(utm.INVALID_BAD_DEBT_SUM(123, 567)))

class TestSerialize(unittest.TestCase):
    def test_serialize(self):
        class A(object):
            serialize = serialize.Serializable(
                default = ('a1', 'a2', 'a3')
            )
            a1 = 'a1'
            a2 = 'a2'
            a3 = 'a3'

            @property
            def a4(self):
                return A()

        class B(object):
            serialize = serialize.Serializable(
                default = ('b1', 'a')
            )

            b1 = 'qqq'
            @property
            def a(self):
                return A()

            @property
            def l(self):
                return [A(), A()]

        s = B().serialize({'a.a4.a4': 'default', 'l.a4': 'default'})
        assert s['a']['a4']['a4']['a1'] == 'a1'
        assert s['l'][1]['a4']['a3'] == 'a3'


if __name__ == '__main__':
    import nose.core
    #nose.core.run(defaultTest='test_util:TestTry1') # add your test class/method
    nose.core.runmodule()
