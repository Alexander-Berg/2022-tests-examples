from __future__ import absolute_import

import gevent

import mock

from cStringIO import StringIO
from collections import defaultdict

from skycore.kernel_util.unittest import TestCase, main
from skycore.kernel_util.sys.gettime import monoTime
from skycore.kernel_util.sys import TempDir
from skycore.kernel_util.uuid import genuuid
from skycore.kernel_util import logging

from skycore.framework.event import Event
from skycore.framework.greendeblock import Deblock
from skycore.components.service import Component, ServiceConfig, Service, State
from skycore import initialize_skycore


class RegistryMock(object):
    def subscribe(self, callback, sections, config_only=False):
        pass

    def unsubscribe(self, callback):
        pass


class ProcMock(object):
    def __init__(self, delay, code, *args, **kwargs):
        self.delay = delay
        self.code = code
        self.finish_event = Event()
        self.args = args
        self.kwargs = kwargs
        self.raw_args = ' '.join(kwargs['args'])
        self.exitstatus = None
        self.rss = 0
        self.max_rss = 0
        self.cpu_usage = 0
        self.uuid = genuuid()
        gevent.spawn(self._execute)

    def _execute(self):
        gevent.sleep(self.delay)
        self.exitstatus = {
            'exited': 1,
            'signaled': 0,
            'exitstatus': self.code,
        }
        self.finish_event.set()

    @property
    def restorable(self):
        return True

    @property
    def succeeded(self):
        return self.exitstatus is not None and self.exitstatus['exitstatus'] == 0

    @property
    def failed(self):
        return self.exitstatus is not None and self.exitstatus['exitstatus'] != 0

    @property
    def ready(self):
        return self.exitstatus is not None

    def kill(self):
        self.send_signal(9)

    def link(self, cb):
        if callable(cb):
            self.finish_event.rawlink(lambda e: cb(self))
        else:
            self.finish_event.link_event(cb)

    def wait(self, timeout=None):
        return self.finish_event.wait(timeout)

    def send_signal(self, signal):
        if not self.finish_event.is_set():
            self.exitstatus = {
                'exited': 1,
                'signaled': 1,
                'exitstatus': -signal
            }
            self.finish_event.set()


class StarterMock(object):
    def __init__(self, log, code):
        self.log = log
        self.code = code
        self.check_code = code
        self.delay = 30.0
        self.check_delay = 0.5
        self.executed = []

    def run(self, *args, **kwargs):
        self.executed.append((args, kwargs))
        self.log.info("starting %s, %s", args, kwargs)
        if 'check' in kwargs['args']:
            return ProcMock(self.check_delay, self.check_code, *args, **kwargs)
        else:
            delay = self.delay
            if 'delay' in kwargs['args']:
                delay = int(kwargs['args'][-1])
            return ProcMock(delay, self.code, *args, **kwargs)

    def get_valid_cgroups(self, name):
        return []


class NamespaceMock(object):
    base = '/'
    skynetdir = ''
    supervisordir = ''
    datadir = '/'
    rundir = '/'
    name = 'test'
    hashes = defaultdict(str)
    log = logging.getLogger('nmspc')
    childs = []

    def add_child(self, child, *args, **kwargs):
        self.childs.append(child)

    def get_service_metainfo(self, name):
        unit = mock.Mock()
        unit.release = {'md5': self.hashes[name]}
        return unit

    def remove_service(self, name):
        for child in list(self.childs):
            if child.name == name:
                self.childs.remove(child)


class TestService(TestCase):
    def setUp(self):
        initialize_skycore()
        super(TestService, self).setUp()

        self.log = logging.getLogger('tstsrvc')
        self.log.propagate = False
        self.io = StringIO()
        handler = logging.StreamHandler(self.io)
        self.log.setLevel(logging.DEBUG)
        self.log.addHandler(handler)
        self.tempdir = TempDir()
        self.tempdir.open()
        self.namespace = NamespaceMock()
        self.namespace.rundir = self.tempdir.dir()
        self.starter = StarterMock(self.log, 0)
        self.deblock = Deblock(name='test_service')

    def tearDown(self):
        self.tempdir.close()
        super(TestService, self).tearDown()

    def test_simple_service(self):
        cfg = ServiceConfig.from_context(
            name='test',
            base='/',
            basepath='/',
            porto='no',
            requirements=[],
            check='check',
            stop='SIGINT',
            conf_sections=[],
            conf_action=None,
            conf_format='yaml',
            cgroup='',
            executables=['start'],
            user='root',
        )
        srvc = Service(
            namespace=self.namespace,
            deblock=self.deblock,
            output_logger_factory=mock.Mock(),
            context_changed_event=mock.Mock(),
            starter=self.starter,
            registry=RegistryMock(),
            cfg=cfg,
        )
        srvc.log.logger.setLevel(logging.DEBUG)
        srvc.start()
        self.assertEqual(srvc.state, State.STOPPED)
        self.assertFalse(list(srvc.proc_uuids()))
        srvc.log.info("STARTING")
        self.assertTrue(srvc.start_service(timeout=2.0))
        self.assertEqual(srvc.state, State.PRERUNNING)
        self.assertEqual(srvc.dependencies, set())
        self.assertEqual(srvc.name, 'test')
        uuids = list(srvc.proc_uuids())
        self.assertEqual(len(uuids), 1)

        srvc.log.info("STOPPING")
        self.starter.check_code = 1
        srvc.kill_procs()
        self.assertTrue(srvc.stop_service(timeout=2.0))
        self.assertEqual(srvc.state, State.STOPPED)
        self.assertFalse(list(srvc.proc_uuids()))

        srvc.log.info("STARTING")
        self.starter.check_code = 0
        self.assertTrue(srvc.start_service(timeout=2.0))
        self.assertEqual(srvc.state, State.PRERUNNING)
        self.assertEqual(srvc.dependencies, set())
        self.assertEqual(srvc.name, 'test')

        new_uuids = list(srvc.proc_uuids())
        self.assertEqual(len(new_uuids), 1)
        self.assertNotEqual(new_uuids, uuids)
        uuids = new_uuids

        srvc.log.info("RESTARTING")
        with mock.patch.object(Service, 'STATE_CHANGE_TIMEOUT', 0.1):
            srvc.restart_service(timeout=2.0)
        self.assertEqual(srvc.state, State.PRERUNNING)
        self.assertEqual(srvc.dependencies, set())
        self.assertEqual(srvc.name, 'test')

        new_uuids = list(srvc.proc_uuids())
        self.assertEqual(len(new_uuids), 1)
        self.assertNotEqual(new_uuids, uuids)

        srvc.log.info("STOPPING")
        self.starter.check_code = 1
        srvc.kill_procs()
        self.assertTrue(srvc.stop_service(timeout=2.0))
        self.assertEqual(srvc.state, State.STOPPED)
        self.assertFalse(list(srvc.proc_uuids()))

    @mock.patch.object(Service, 'STATE_CHANGE_TIMEOUT', 1.0)
    def test_service_restart_interval(self):
        cfg = ServiceConfig.from_context(
            name='test',
            base='/',
            basepath='/',
            porto='no',
            cgroup='',
            requirements=[],
            check='check',
            stop='stop',
            conf_sections=[],
            conf_action=None,
            conf_format='yaml',
            executables=['start'],
            user='root',
        )
        srvc = Service(
            namespace=self.namespace,
            deblock=self.deblock,
            output_logger_factory=mock.Mock(),
            context_changed_event=mock.Mock(),
            starter=self.starter,
            registry=RegistryMock(),
            cfg=cfg,
        )
        srvc.log.logger.setLevel(logging.DEBUG)
        srvc.start()
        try:
            self.assertEqual(srvc.state, State.STOPPED)
            self.starter.check_code = 1
            interval = srvc._state_controller.start_controller.interval
            self.assertFalse(srvc.start_service(timeout=5.0))
            self.assertGreater(srvc._state_controller.start_controller.interval, interval)
            self.assertNotEqual(srvc.state, State.RUNNING)

            self.assertTrue(srvc.stop_service(timeout=2.0))
            self.assertEqual(srvc.state, State.STOPPED)
            self.assertEqual(srvc._state_controller.start_controller.interval, interval)
        finally:
            srvc.stop()

    def test_process_immediate_restart(self):
        cfg = ServiceConfig.from_context(
            name='test',
            base='/',
            basepath='/',
            porto='no',
            cgroup='',
            requirements=[],
            check='check',
            stop='stop',
            conf_sections=[],
            conf_action=None,
            conf_format='yaml',
            executables=['start1', 'delay 1'],
            user='root',
        )
        srvc = Service(
            namespace=self.namespace,
            deblock=self.deblock,
            output_logger_factory=mock.Mock(),
            context_changed_event=mock.Mock(),
            starter=self.starter,
            registry=RegistryMock(),
            cfg=cfg,
        )
        srvc.log.logger.setLevel(logging.DEBUG)
        srvc.start()

        try:
            self.assertEqual(srvc.state, State.STOPPED)
            self.assertTrue(srvc.start_service(timeout=2.0))
            self.assertEqual(srvc.state, State.PRERUNNING)

            sleep_time = (srvc._state_controller.state_started + 1.0) - monoTime()  # give proc time to die
            with mock.patch.object(srvc, '_start_proc', autospec=True) as start_mock:
                def make_proc(path, *kargs, **kwargs):
                    res = mock.Mock()
                    res.raw_args = path
                    res.exitstatus = {'exitstatus': 0, 'exited': 1, 'signaled': 0}
                    return res

                start_mock.side_effect = make_proc
                if sleep_time > 0:
                    gevent.sleep(sleep_time)
                self.assertGreater(start_mock.call_count, 0)
                start_mock.assert_has_calls([
                    ((), {'path': 'delay 1', 'fast': False, 'raw_args': 'delay 1', 'kind': 'service'}),
                ])

        finally:
            srvc.stop()

    def test_get_api(self):
        cfg = ServiceConfig.from_context(
            name='test',
            base='/',
            basepath='/',
            porto='no',
            cgroup='',
            requirements=[],
            check='',
            stop='',
            conf_sections=[],
            conf_format='yaml',
            conf_action=None,
            executables=[],
            user='root',
            api={'python': {
                'import_path': ['${CURDIR}/api', '/${RUNNING_PROCESSES}/'],
                'module': 'ya.skynet.${NAMESPACE}.skynet',
                'intopt': 42,
                'noneopt': None,
            }},
        )
        srvc = Service(
            namespace=self.namespace,
            deblock=self.deblock,
            output_logger_factory=mock.Mock(),
            context_changed_event=mock.Mock(),
            starter=self.starter,
            registry=RegistryMock(),
            cfg=cfg,
        )
        self.assertIsNone(srvc.get_api('shell'))

        api = srvc.get_api('python')
        self.assertIsNone(api['noneopt'])
        self.assertEqual(api['intopt'], 42)
        self.assertEqual(api['module'], 'ya.skynet.test.skynet')
        self.assertEqual(api['import_path'], ['//api', '/0/'])

    def test_save_restore(self):
        cfg = ServiceConfig.from_context(
            name='test',
            base='/',
            basepath='/',
            porto='no',
            cgroup='',
            requirements=[],
            check='',
            stop='',
            conf_sections=[],
            conf_format='yaml',
            conf_action=None,
            executables=[],
            user='root',
            api={'python': {
                'import_path': ['${CURDIR}/api', '/${RUNNING_PROCESSES}/'],
                'module': 'ya.skynet.${NAMESPACE}.skynet',
                'intopt': 42,
                'noneopt': None,
            }},
        )
        srvc = Service(
            namespace=self.namespace,
            deblock=self.deblock,
            output_logger_factory=mock.Mock(),
            context_changed_event=mock.Mock(),
            starter=self.starter,
            registry=RegistryMock(),
            cfg=cfg,
        )

        ctx = srvc.context
        new_srvc = Service(
            namespace=self.namespace,
            deblock=self.deblock,
            output_logger_factory=mock.Mock(),
            context_changed_event=mock.Mock(),
            starter=self.starter,
            registry=RegistryMock(),
            cfg=cfg,
            context=ctx,
        )
        self.assertEqual(new_srvc.last_check_time, -1)
        self.assertEqual(new_srvc.last_start_time, -1)

        update_time = monoTime()
        srvc.last_check_time = update_time
        srvc.last_start_time = update_time
        ctx = srvc.context

        new_srvc = Service(
            namespace=self.namespace,
            deblock=self.deblock,
            output_logger_factory=mock.Mock(),
            context_changed_event=mock.Mock(),
            starter=self.starter,
            registry=RegistryMock(),
            cfg=cfg,
            context=ctx,
        )

        self.assertTrue(abs(new_srvc.last_check_time - update_time) < 0.1)
        self.assertTrue(abs(new_srvc.last_start_time - update_time) < 0.1)

    def test_service_with_deps(self):
        pass  # FIXME write the test

    def test_namespace_save_restore(self):
        pass  # FIXME write the test

    @mock.patch.object(Component, 'start')
    def test_service_start_failure(self, start):
        def side_effect(*args, **kwargs):
            raise OSError(2, "Oops")
        start.side_effect = side_effect
        cfg = ServiceConfig.from_context(
            name='test',
            base='/',
            basepath='/',
            porto='no',
            cgroup='',
            requirements=[],
            check='',
            stop='',
            conf_sections=[],
            conf_format='yaml',
            conf_action=None,
            executables=[],
            user='root',
            api={'python': {
                'import_path': ['${CURDIR}/api', '/${RUNNING_PROCESSES}/'],
                'module': 'ya.skynet.${NAMESPACE}.skynet',
                'intopt': 42,
                'noneopt': None,
            }},
        )
        srvc = Service(
            namespace=self.namespace,
            deblock=self.deblock,
            starter=self.starter,
            output_logger_factory=mock.Mock(),
            context_changed_event=mock.Mock(),
            registry=RegistryMock(),
            cfg=cfg,
        )

        self.assertIn(srvc, self.namespace.childs)

        srvc.start()

        self.assertNotIn(srvc, self.namespace.childs)


if __name__ == '__main__':
    main()
