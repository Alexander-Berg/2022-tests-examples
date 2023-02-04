from __future__ import absolute_import

import mock

from skycore.kernel_util.unittest import TestCase, main
from skycore.kernel_util.sys import TempDir
from skycore.kernel_util import logging

from skycore.components.namespace import Namespace, InstallationUnit
from skycore.components.service import ServiceConfig, Service, State, StateController
from skycore.framework.greendeblock import Deblock
from skycore import initialize_skycore


class TestNamespace(TestCase):
    def setUp(self):
        initialize_skycore()
        super(TestNamespace, self).setUp()

        self.log = logging.getLogger('')
        self.log.setLevel(logging.DEBUG)
        self.tempdir1 = TempDir()
        self.tempdir1.open()
        self.tempdir2 = TempDir()
        self.tempdir2.open()
        self.starter = mock.Mock()
        self.deblock = Deblock(name='test_namespace')

    def tearDown(self):
        self.tempdir2.close()
        self.tempdir1.close()
        self.deblock.stop()
        super(TestNamespace, self).tearDown()

    @mock.patch.object(
        StateController,
        '_perform_check',
        lambda self, *args, **kwargs: (
            (False, 0)
            if self._required_state == State.STOPPED
            else (True, 0)
        )
    )
    def test_failed_upgrade(self):
        parent = mock.Mock()
        parent.base = '/'
        parent.log = self.log

        ns = Namespace(
            starter=self.starter,
            deblock=self.deblock,
            output_logger_factory=mock.Mock(),
            context_changed_event=mock.Mock(),
            name='testns',
            workdir=self.tempdir1.dir(),
            rundir=self.tempdir2.dir(),
            registry=mock.Mock(),
            parent=parent,
        )
        ns.log.logger.setLevel(logging.DEBUG)
        ns.start()

        cfg = ServiceConfig.from_context(
            name='test',
            base='/',
            basepath='/',
            porto='no',
            requirements=[],
            check='',
            stop='',
            conf_sections=['a'],
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
        srvc_old = Service(
            namespace=ns,
            deblock=self.deblock,
            starter=self.starter,
            output_logger_factory=mock.Mock(),
            context_changed_event=mock.Mock(),
            registry=ns.registry,
            cfg=cfg,
        )

        meta_old = {'md5': 'aaaa', 'urls': None}
        unit_old = InstallationUnit('<test 1>', meta_old, self.deblock)

        try:
            ns.inject_one(srvc_old, unit_old, ns.log)

            for loop in srvc_old.loops.itervalues():
                self.assertTrue(loop.started and not loop.ready())

            srvc_new = Service(
                namespace=ns,
                starter=self.starter,
                deblock=self.deblock,
                registry=ns.registry,
                output_logger_factory=mock.Mock(),
                context_changed_event=mock.Mock(),
                cfg=cfg,
            )
            meta_new = {'md5': 'bbbb', 'urls': None}
            unit_new = InstallationUnit('<test 1>', meta_new, self.deblock)

            def raise_error(*args, **kwargs):
                raise Exception("Cannot subscribe")

            ns.registry.subscribe.side_effect = raise_error
            with self.assertRaises(Exception):
                ns.inject_one(srvc_new, unit_new, ns.log)

            self.assertNotIn(srvc_new, ns.childs)
            self.assertIn(srvc_old, ns.childs)
            self.assertIn('test', ns.services)
            self.assertIn('test', ns.services_meta)
            self.assertIn('test', ns.hashes)
            self.assertIn('aaaa', ns.rhashes)
            self.assertNotIn('bbbb', ns.rhashes)

            for loop in srvc_old.loops.itervalues():
                self.assertTrue(loop.started and not loop.ready())
            for loop in srvc_new.loops.itervalues():
                self.assertFalse(loop.started and not loop.ready())

        finally:
            ns.stop()

    def test_double_ops(self):
        parent = mock.Mock()
        parent.base = '/'
        parent.log = self.log

        ns = Namespace(
            starter=self.starter,
            deblock=self.deblock,
            output_logger_factory=mock.Mock(),
            context_changed_event=mock.Mock(),
            name='testns',
            workdir=self.tempdir1.dir(),
            rundir=self.tempdir2.dir(),
            registry=mock.Mock(),
            parent=parent,
        )
        ns.log.logger.setLevel(logging.DEBUG)
        ns.start()

        cfg = ServiceConfig.from_context(
            name='test',
            base='/',
            basepath='/',
            porto='no',
            requirements=[],
            check='',
            stop='',
            conf_sections=['a'],
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
            namespace=ns,
            deblock=self.deblock,
            starter=self.starter,
            output_logger_factory=mock.Mock(),
            context_changed_event=mock.Mock(),
            registry=ns.registry,
            cfg=cfg,
        )
        srvc2 = Service(
            namespace=ns,
            deblock=self.deblock,
            starter=self.starter,
            output_logger_factory=mock.Mock(),
            context_changed_event=mock.Mock(),
            registry=ns.registry,
            cfg=cfg,
        )
        meta = {'md5': 'aaaa', 'urls': None}
        unit = InstallationUnit('<test 2>', meta, self.deblock)
        meta2 = {'md5': 'aaaa', 'urls': None}
        unit2 = InstallationUnit('<test 2>', meta2, self.deblock)

        try:
            ns.inject_one(srvc, unit, ns.log, start_on_install=False)
            ns.inject_one(srvc2, unit2, ns.log, start_on_install=False)

            ns.remove_service(cfg.name)
            ns.remove_service(cfg.name)
        finally:
            ns.stop()


if __name__ == '__main__':
    main()
