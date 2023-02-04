import os
import os.path
import popen2
import subprocess
import threading
import time
import unittest
import re

try:
    import elementtree.ElementTree as et
except:
    import xml.etree.ElementTree as et

import logging

import yutil
import ycorba

import Yandex

log = logging.getLogger('test_ycorba')

def _start_proc(cmd):
    proc = popen2.Popen3(cmd, True)
    
    def reader(prefix, stream):
        for l in stream.xreadlines(): log.debug('servant %s: %s' % (prefix, l.strip()))

    threads=[threading.Thread(target=reader, args=('stdout', proc.fromchild,)),
             threading.Thread(target=reader, args=('stderr', proc.childerr,))]

    for th in threads:
        th.setDaemon(True)
        th.start()

    time.sleep(1)

    return (proc, threads)

def _stop_proc(tup):
    subprocess.call(['kill', str(tup[0].pid)])

    for th in tup[1]:
        th.join()

def _clean_up():
    def _just_do(f, *args, **kw):
        try:
            f(*args, **kw)
        except:
            pass

    _just_do(os.remove, 'test_ycorba_test_servant.pid')
    _just_do(os.remove, 'test_ycorba_test_servant.log')

def _start_servant():
    _clean_up()

    return _start_proc('YANDEX_XML_CONFIG=test/test_ycorba_test_servant.cfg python test/test_ycorba_test_servant.py')

def _stop_servant(proc):
    _stop_proc(proc)

def _test_servant():
    return ycorba.servant('Yandex/Test.id')

class StartServantTestCase(unittest.TestCase):
    def setUp(self):
        self.proc = _start_servant()

    def tearDown(self):
        _stop_servant(self.proc)

class TestDummy(StartServantTestCase):
    def test_check_it_works(self):
        servant = _test_servant()
        
        self.assertEqual(servant.helloWorld(),
                         'Hello, World!')

class TestAutomagic(StartServantTestCase):
    def test_has_ping(self):
        servant = _test_servant()
        
        self.assertEqual(servant.Ping(), True)

    def test_has_ping_as_string(self):
        servant = _test_servant()

        self.assertEqual(servant.PingAsString(), 'Ok')

    def test_log_call(self):
        servant = _test_servant()

        def logdata():
            return file('test_ycorba_test_servant.log').read()

        hw = re.compile(r'helloWorld')
        ping = re.compile(r'Ping')

        d = logdata()
        hw_count_init = len(hw.findall(d))
        ping_count_init = len(hw.findall(d))

        servant.Ping()

        d = logdata()
        hw_count_ping = len(hw.findall(d))
        ping_count_ping = len(ping.findall(d))

        self.assertEqual(hw_count_init, hw_count_ping)
        self.assert_(ping_count_init < ping_count_ping, '%s < %s' % (ping_count_init, ping_count_ping))

        servant.helloWorld()

        d = logdata()
        hw_count_ping_hw = len(hw.findall(d))
        ping_count_ping_hw = len(ping.findall(d))

        self.assert_(hw_count_ping < hw_count_ping_hw)
        self.assertEqual(ping_count_ping, ping_count_ping_hw)

class TestDecorators(StartServantTestCase):
    def test_ui_method_exc(self):
        servant = _test_servant()

        r = et.fromstring(servant.raiseExc())

        self.assertEqual(r.tag, 'error')
        self.assertEqual(r.text, 'exception')

    def test_ui_method_elementtree(self):
        servant = _test_servant()

        r = et.fromstring(servant.returnElementTree())

        self.assertEqual(r.tag, 'a')
        self.assertEqual(r.text or '', '')

    def test_ui_method_multiple_out(self):
        servant = _test_servant()

        (out_str, res) = servant.multipleOut()

        out = et.fromstring(out_str)

        self.assertEqual(out.tag, 'a')
        self.assertEqual(res, 'aaa')

class TestPidFile(unittest.TestCase):
    def test_pid_file(self):
        proc = _start_servant()

        self.assert_(os.path.exists('test_ycorba_test_servant.pid'))

        _stop_servant(proc)

        self.assert_(not os.path.exists('test_ycorba_test_servant.pid'))

if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    unittest.main()
