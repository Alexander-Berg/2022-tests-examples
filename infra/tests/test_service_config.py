from __future__ import absolute_import

from skycore.kernel_util.unittest import TestCase, main
from skycore.service_config import ServiceConfig
import os

import mock


class TestServiceConfig(TestCase):
    def _cfg_handler(self, var_type, var_name):
        if var_name.endswith('yes'):
            return True
        elif var_name.endswith('no'):
            return False
        else:
            return var_name.rsplit('.', 1)[1]

    def test_v1(self):
        cfg = ServiceConfig.from_context(
            name='test',
            base='/',
            basepath='nonexistent',
            porto='${cfg:skynet.service:enable.porto.no}',
            requirements=[
                'service2',
                '${cfg:skynet.service:some.service3}'
            ],
            check='${cfg:skynet.service:some.check}',
            stop='${cfg:skynet.service:some.stop}',
            conf_sections=[
                'section1',
                'section.${cfg:skynet.service:some.conf}'
            ],
            conf_format='${cfg:skynet.service:some.xml}',
            conf_action='SIG${cfg:skynet.service:some.INT}',
            executables=[
                '${cfg:skynet.service:some.exe}',
                '${cfg:skynet.service:other.exe}',
            ],
            user='${cfg:skynet.service:some.nobody}',
            cgroup='${cfg:skynet.service:some.cgroup}',
            limits={
                '${cfg:skynet.service:some.NOFILE}': '${cfg:skynet.service.some.unlimited}',
            },
            env={
                '${cfg:skynet.service:some.SERVICE}': '${CFGFILE}',
            },
            api={'python': {
                'import_path': ['${CURDIR}/api', '/${RUNNING_PROCESSES}/'],
                'module': 'ya.skynet.${NAMESPACE}.skynet',
                'intopt': 42,
                'noneopt': None,
            }},
            install_script='${cfg:skynet.service:some.install}',
            uninstall_script='${cfg:skynet.service:some.uninstall}',
            restart_on_upgrade='${cfg:skynet.service:some.restart.no}',
            install_as_privileged='${cfg:skynet.service:some.install.yes}',
            porto_options={
                '${cfg:skynet.service:some.bind}': '${cfg:skynet.service:some.bind.khlebnikov}',
            },
            porto_container='${cfg:skynet.service:some.container}',
        )
        cfg.set_type_handler('cfg', self._cfg_handler)

        data = cfg.as_dict(base='/')
        self.assertEqual(data['version'], 1)

        ref_cfg = ServiceConfig.from_context(base='/', **data)

        self.assertEqual(cfg, ref_cfg)

        data['version'] = 2
        non_ref_cfg = ServiceConfig.from_context(base='/', **data)
        self.assertNotEqual(cfg, non_ref_cfg)

        self.assertEqual(cfg.basepath, '/nonexistent')
        self.assertEqual(cfg.take('name'), 'test')
        self.assertEqual(cfg.take('porto'), 'False')
        self.assertEqual(cfg.dependencies, frozenset(['service2', 'service3']))
        self.assertEqual(cfg.take('check'), 'check')
        self.assertEqual(cfg.take('stop'), 'stop')
        self.assertEqual(cfg.configs, frozenset(['section1', 'section.conf']))
        self.assertEqual(cfg.take('conf_format'), 'xml')
        self.assertEqual(cfg.take('conf_action'), 'SIGINT')
        self.assertEqual(cfg.registry_filename, 'configuration.xml')
        self.assertEqual(cfg.take('executables'), ['exe', 'exe'])
        self.assertEqual(cfg.take('user'), 'nobody')
        self.assertEqual(cfg.take('cgroup'), 'cgroup')
        self.assertEqual(cfg.take('limits'), {'NOFILE': 'unlimited'})
        self.assertEqual(cfg.take('env'), {'SERVICE': 'configuration.xml'})

        self.assertEqual(
            cfg.take('api'),
            {
                'python': {
                    'import_path': ['/nonexistent/api', '//'],
                    'module': 'ya.skynet..skynet', 'intopt': 42, 'noneopt': None
                }
            }
        )
        cfg.set_var_handler('RUNNING_PROCESSES', lambda: 42)
        self.assertEqual(
            cfg.take('api'),
            {
                'python': {
                    'import_path': ['/nonexistent/api', '/42/'],
                    'module': 'ya.skynet..skynet', 'intopt': 42, 'noneopt': None
                }
            }
        )
        cfg.set_var_handler('NAMESPACE', 'unknown')
        self.assertEqual(
            cfg.take('api'),
            {
                'python': {
                    'import_path': ['/nonexistent/api', '/42/'],
                    'module': 'ya.skynet.unknown.skynet', 'intopt': 42, 'noneopt': None
                }
            }
        )

        self.assertEqual(cfg.take('install_script'), 'install')
        self.assertEqual(cfg.take('uninstall_script'), 'uninstall')
        self.assertEqual(cfg.take('restart_on_upgrade'), 'False')
        self.assertEqual(cfg.take('install_as_privileged'), 'True')
        self.assertEqual(cfg.take('porto_options'), {'bind': 'khlebnikov'})
        self.assertEqual(cfg.take('porto_container'), 'container')

    def test_v2(self):
        cfg = ServiceConfig.from_context(
            name='test',
            base='/',
            basepath='nonexistent',
            porto='${cfg:skynet.service:enable.porto.no}',
            requirements=[
                'service2',
                '${cfg:skynet.service:some.service3}'
            ],
            check='${alias:COMMON} ${cfg:skynet.service:some.check}',
            stop='${alias:COMMON2} ${cfg:skynet.service:some.stop}',
            conf_sections=[
                'section1',
                'section.${cfg:skynet.service:some.conf}'
            ],
            conf_format='${cfg:skynet.service:some.xml}',
            conf_action='SIG${cfg:skynet.service:some.INT}',
            executables=[
                '${alias:FAILURE} ${cfg:skynet.service:some.exe}',
                '${cfg:skynet.service:other.exe}',
            ],
            user='${cfg:skynet.service:some.nobody}',
            cgroup='${cfg:skynet.service:some.cgroup}',
            limits={
                '${cfg:skynet.service:some.NOFILE}': '${cfg:skynet.service.some.unlimited}',
            },
            env={
                '${cfg:skynet.service:some.SERVICE}': '${CFGFILE}',
            },
            api={'python': {
                'import_path': ['${CURDIR}/api', '/${RUNNING_PROCESSES}/'],
                'module': 'ya.skynet.${NAMESPACE}.skynet',
                'intopt': 42,
                'noneopt': None,
            }},
            install_script='${alias:LOOP1}',
            uninstall_script='${alias:LOOP2}',
            restart_on_upgrade='${cfg:skynet.service:some.restart.no}',
            install_as_privileged='${cfg:skynet.service:some.install.yes}',
            porto_options={
                '${cfg:skynet.service:some.bind}': '${cfg:skynet.service:some.bind.khlebnikov}',
            },
            porto_container='${cfg:skynet.service:some.container}',
            version=2,
            aliases={
                'COMMON': 'test ${cfg:skynet.service:some.test2}',
                'COMMON2': '${alias:COMMON} ${cfg:skynet.service.some.test3}',
                'FAILURE': 'xx ${alias:FAILURE}',
                'LOOP1': 'xx1 ${alias:LOOP2}',
                'LOOP2': 'xx2 ${alias:LOOP1}',
            },
        )
        cfg.set_type_handler('cfg', self._cfg_handler)
        cfg.set_var_handler('RUNDIR', '/nonexistent')

        data = cfg.as_dict(base='/')
        self.assertEqual(data['version'], 2)

        ref_cfg = ServiceConfig.from_context(base='/', **data)

        self.assertEqual(cfg, ref_cfg)

        data['version'] = 1
        non_ref_cfg = ServiceConfig.from_context(base='/', **data)
        self.assertNotEqual(cfg, non_ref_cfg)

        self.assertEqual(cfg.basepath, '/nonexistent')
        self.assertEqual(cfg.take('name'), 'test')
        self.assertEqual(cfg.take('porto'), 'False')
        self.assertEqual(cfg.dependencies, frozenset(['service2', 'service3']))
        self.assertEqual(cfg.take('check'), 'test test2 check')
        self.assertEqual(cfg.take('stop'), 'test test2 test3 stop')
        self.assertEqual(cfg.configs, frozenset(['section1', 'section.conf']))
        self.assertEqual(cfg.take('conf_format'), 'xml')
        self.assertEqual(cfg.take('conf_action'), 'SIGINT')
        self.assertEqual(cfg.registry_filename, '/nonexistent/conf/configuration.xml')
        with self.assertRaises(RuntimeError):
            cfg.take('executables')
        self.assertEqual(cfg.take('user'), 'nobody')
        self.assertEqual(cfg.take('cgroup'), 'cgroup')
        self.assertEqual(cfg.take('limits'), {'NOFILE': 'unlimited'})
        self.assertEqual(cfg.take('env'), {'SERVICE': '/nonexistent/conf/configuration.xml'})

        self.assertEqual(
            cfg.take('api'),
            {
                'python': {
                    'import_path': ['/nonexistent/api', '//'],
                    'module': 'ya.skynet..skynet', 'intopt': 42, 'noneopt': None
                }
            }
        )
        cfg.set_var_handler('RUNNING_PROCESSES', lambda: 42)
        self.assertEqual(
            cfg.take('api'),
            {
                'python': {
                    'import_path': ['/nonexistent/api', '/42/'],
                    'module': 'ya.skynet..skynet', 'intopt': 42, 'noneopt': None
                }
            }
        )
        cfg.set_var_handler('NAMESPACE', 'unknown')
        self.assertEqual(
            cfg.take('api'),
            {
                'python': {
                    'import_path': ['/nonexistent/api', '/42/'],
                    'module': 'ya.skynet.unknown.skynet', 'intopt': 42, 'noneopt': None
                }
            }
        )

        with self.assertRaises(RuntimeError):
            cfg.take('install_script')
        with self.assertRaises(RuntimeError):
            cfg.take('uninstall_script')
        self.assertEqual(cfg.take('restart_on_upgrade'), 'False')
        self.assertEqual(cfg.take('install_as_privileged'), 'True')
        self.assertEqual(cfg.take('porto_options'), {'bind': 'khlebnikov'})
        self.assertEqual(cfg.take('porto_container'), 'container')

    def test_v3(self):
        cfg = ServiceConfig.from_context(
            name='test',
            base='/',
            basepath='nonexistent',
            porto='no',
            requirements=[],
            check='',
            stop='',
            conf_sections=[],
            conf_format='',
            conf_action='',
            executables=[
                '${env:SERVICE} ololo',
                '${env:nonexistent12345r}'
            ],
            user='${cfg:skynet.service:some.nobody}',
            env={
                '${cfg:skynet.service:some.SERVICE}': 'test ${srvenv:nonexistent12345q}',
                'nonexistent12345r': '${srvenv:nonexistent12345r}:1',
            },
            version=3,
        )
        cfg.set_type_handler('cfg', self._cfg_handler)

        data = cfg.as_dict()
        self.assertEqual(data['version'], 3)

        ref_cfg = ServiceConfig.from_context(base='/', **data)

        self.assertEqual(cfg, ref_cfg)

        for e in ('nonexistent12345q', 'nonexistent12345r'):
            os.environ.pop(e, None)
        try:
            self.assertEqual(cfg.take('executables'), ['test  ololo', ':1'])
            os.environ['nonexistent12345q'] = 'keke'
            self.assertEqual(cfg.take('executables'), ['test keke ololo', ':1'])
            os.environ['nonexistent12345r'] = 'koko'
            self.assertEqual(cfg.take('executables'), ['test keke ololo', 'koko:1'])
        finally:
            for e in ('nonexistent12345q', 'nonexistent12345r'):
                os.environ.pop(e, None)

    def test_v4(self):
        cfg = ServiceConfig.from_context(
            name='test',
            base='/',
            basepath='nonexistent',
            porto='no',
            requirements=[],
            check='',
            stop='',
            conf_sections=[],
            conf_format='yaml',
            conf_action='',
            executables=[
                'ololo ${cfgfile:section.a.b.c}',
            ],
            user='nobody',
            env={},
            version=4,
        )
        cfg.set_var_handler('RUNDIR', '/nonexistent')

        data = cfg.as_dict()
        self.assertEqual(data['version'], 4)

        ref_cfg = ServiceConfig.from_context(base='/', **data)
        self.assertEqual(cfg, ref_cfg)
        self.assertEqual(cfg.take('executables'), ['ololo /nonexistent/conf/section.a.b.c.yaml'])

    def test_v6_existing(self):
        cfg = ServiceConfig.from_context(
            name='test',
            base='/',
            basepath='nonexistent',
            porto='no',
            requirements=[],
            check='',
            stop='',
            conf_sections=[],
            conf_format='yaml',
            conf_action='',
            executables=[
                'ololo ${cfgfile:section.a.b.c}',
            ],
            user='nobody',
            env={},
            version=6,
            restart_on_skydeps_upgrade=True
        )
        cfg.set_var_handler('RUNDIR', '/nonexistent')

        data = cfg.as_dict()
        self.assertEqual(data['version'], 6)

        ref_cfg = ServiceConfig.from_context(base='/', **data)
        self.assertEqual(cfg, ref_cfg)
        self.assertEqual(cfg.take('restart_on_skydeps_upgrade'), True)

    def test_v6_missing(self):
        cfg = ServiceConfig.from_context(
            name='test',
            base='/',
            basepath='nonexistent',
            porto='no',
            requirements=[],
            check='',
            stop='',
            conf_sections=[],
            conf_format='yaml',
            conf_action='',
            executables=[
                'ololo ${cfgfile:section.a.b.c}',
            ],
            user='nobody',
            env={},
            version=6
        )
        cfg.set_var_handler('RUNDIR', '/nonexistent')

        data = cfg.as_dict()
        self.assertEqual(data['version'], 6)

        ref_cfg = ServiceConfig.from_context(base='/', **data)
        self.assertEqual(cfg, ref_cfg)
        self.assertEqual(cfg.take('restart_on_skydeps_upgrade'), False)

    def test_v7(self):
        base_data = {
            'porto': 'yes',
            'conf': '',
            'exec': ['1'],
            'user': 'nobody',
            'scsd_version': 7,
        }
        log = mock.Mock()

        ServiceConfig.from_data('test', '/test', base_data.copy(), log)

        cfg = base_data.copy()
        cfg['porto_options'] = None
        ServiceConfig.from_data('test', '/test', cfg, log)

        cfg = base_data.copy()
        cfg['porto_options'] = {}
        ServiceConfig.from_data('test', '/test', cfg, log)

        with self.assertRaises(TypeError):
            cfg = base_data.copy()
            cfg['porto_options'] = 'kekeke'
            ServiceConfig.from_data('test', '/test', cfg, log)

        with self.assertRaises(TypeError):
            cfg = base_data.copy()
            cfg['porto_options'] = {
                'meta': 'kekeke',
            }
            ServiceConfig.from_data('test', '/test', cfg, log)

        with self.assertRaises(TypeError):
            cfg = base_data.copy()
            cfg['porto_options'] = {
                'something': {},
            }
            ServiceConfig.from_data('test', '/test', cfg, log)

        cfg = base_data.copy()
        cfg['porto_options'] = {
            'meta': {
                'a': 'b',
            },
            'check': {},
            'exec': None,
            'stop': {},
            'install': {},
            'uninstall': {},
        }
        ServiceConfig.from_data('test', '/test', cfg, log)


if __name__ == '__main__':
    main()
