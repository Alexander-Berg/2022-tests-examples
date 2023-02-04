from __future__ import absolute_import

import os
from cStringIO import StringIO

import mock
import errno
import copy

from skycore.kernel_util.unittest import TestCase, main
from skycore.kernel_util import logging
from skycore.kernel_util.sys import TempDir

from skycore.components.cgroupscontroller import CgroupsController
from skycore import initialize_skycore


def mock_config():
    return {
        'subsections': {
            'sys': {
                'subsections': {
                    'cgroups': {
                        'config': {
                            'cgroup:infra': {
                                'cgroup:oops': {
                                    'memory': {
                                        'memory.limit_in_bytes': '384M'
                                    }
                                }
                            },
                            'cgroup:skynet': {
                                'cgroup:services': {
                                    'cgroup:cqudp': {
                                        'memory': {
                                            'memory.limit_in_bytes': '750M'
                                        }
                                    },
                                    'cgroup:cqueue': {
                                        'memory': {
                                            'memory.limit_in_bytes': '750M'
                                        }
                                    },
                                    'cgroup:skybone': {
                                        'memory': {
                                            'memory.limit_in_bytes': '2G'
                                        }
                                    },
                                    'memory': {
                                        'memory.limit_in_bytes': '3G'
                                    }
                                },
                                'blkio': {
                                    'blkio.weight': 1111
                                },
                                'cpu': {},
                                'devices': {},
                                'freezer': {},
                                'memory': {
                                    'memory.limit_in_bytes': '8G',
                                    'memory.use_hierarchy': 1
                                }
                            },
                            'cgroup:skynet_tasks': {
                                'blkio': {},
                                'cpu': {},
                                'devices': {},
                                'freezer': {},
                                'memory': {
                                    'memory.use_hierarchy': 1
                                }
                            },
                            'options': {
                                'config_version': 1,
                                'path': '/sys/fs/cgroup'
                            }
                        },
                        'config_hash': 'dfbd3332747eaa377efc143ac2ae8db44dbbf2b2'
                    }
                }
            }
        }
    }


class RegistryMock(object):
    _data = {}

    def subscribe(self, callback, sections, config_only=False):
        for path in sections:
            try:
                value = self.query(path)
            except LookupError:
                # missing section, nothing to return yet
                continue

            # extract config section only, no metadata required
            config = value['config']
            if config:
                config['__config_hash'] = value['config_hash']
            callback(path, config)

    def unsubscribe(self, callback):
        pass

    def query(self, query, deepcopy=True):
        cfg = self._data
        for el in query:
            cfg = cfg['subsections'][el]

        return copy.deepcopy(cfg) if deepcopy else cfg

    def send_report(self, success, description, hostname=None):
        pass


class ProcessLockMock(object):
    def __enter__(self):
        pass

    def __exit__(self, exc_type, exc_val, exc_tb):
        pass


class TestCgroupController(TestCase):
    def setUp(self):
        initialize_skycore()
        super(TestCgroupController, self).setUp()

        self.log = logging.getLogger('tstcgrpctrl')
        self.log.setLevel(logging.DEBUG)
        self.io = StringIO()
        handler = logging.StreamHandler(self.io)
        self.log.addHandler(handler)

    def _create_cgroup(self, path):
        for c, rw in self.controllers:
            if path.startswith(c) and not rw:
                # we cannot create cgroup in read-only controller
                self.log.debug('MOCK: Read-only: %s (%s)', path, c)
                ex = OSError()
                ex.errno = errno.EROFS
                raise ex

        os.mkdir(path)
        self.log.debug('MOCK: Create cgroup: %s', path)
        tasks_path = os.path.join(path, 'tasks')
        # create dummy task file
        with open(tasks_path, 'w'):
            pass

    def _create_controllers(self, path, controllers):
        self.controllers = []
        for controller, rw in controllers:
            cont_path = os.path.join(path, controller)
            os.mkdir(cont_path)
            self.controllers.append((cont_path, rw))

    def _prepare_test(self, mock, base, controllers):
        mock.side_effect = self._create_cgroup
        workdir = os.path.join(base, 'workdir')
        os.mkdir(workdir)
        rundir = os.path.join(base, 'rundir')
        os.mkdir(rundir)
        registry = RegistryMock()
        registry._data = mock_config()

        # override cgroup path in current config
        registry._data['subsections']['sys']['subsections']['cgroups']['config']['options']['path'] = rundir

        skycore_cgroups_content = '''
__cgroup_path: %s
blkio:
  skycore:
    core: {}
    services: {}
cpu:
  skycore:
    core: {}
    services: {}
devices:
  skycore:
    core: {}
    services: {}
freezer:
  skycore:
    core: {}
    services: {}
memory:
  skycore:
    core:
      memory.limit_in_bytes: 1G
    memory.use_hierarchy: 1
    services:
      memory.limit_in_bytes: 1G
''' % rundir
        with open(os.path.join(workdir, 'skycore.cgroups'), 'w') as fd:
            fd.write(skycore_cgroups_content)

        self._create_controllers(rundir, controllers)
        self.controller = CgroupsController(workdir=workdir, registry=registry, log=self.log, lock=ProcessLockMock())
        self.controller._update_skycore_cgroups(log=self.log)

        self.log.debug('####################################################################')
        for cgroup in self.controller.valid_cgroups:
            self.log.debug('VALID: %s', cgroup)

        for cgroup in self.controller.known_cgroups:
            self.log.debug('KNOWN: %s', cgroup)
        self.log.debug('####################################################################')

    @mock.patch.object(CgroupsController, '_create_cgroup')
    def test_standard_controllers(self, mock):
        with TempDir() as base:
            controllers = [
                ('blkio', True), ('devices', True), ('cpu', True), ('freezer', True), ('memory', True)
            ]
            self._prepare_test(mock, base, controllers)

            for group, expected_result in [
                ('skynet', [os.path.join(base, c[0], 'skynet') for c in self.controllers if c[1]]),
                ('skynet/services', [os.path.join(base, c[0], 'skynet/services') for c in self.controllers if c[1]]),
                ('infra', [os.path.join(base, c[0], 'infra') for c in self.controllers if c[1]]),
                ('infra/oops', [os.path.join(base, c[0], 'infra/oops') for c in self.controllers if c[1]]),
                ('dummy', ['EXCEPTION'])
            ]:
                try:
                    result = sorted(self.controller.get_valid_cgroups(group))
                except Exception:
                    result = ['EXCEPTION']
                expected_result = sorted(expected_result)
                self.assertEqual(expected_result, result)

    @mock.patch.object(CgroupsController, '_create_cgroup')
    def test_with_systemd_controllers(self, mock):
        with TempDir() as base:
            controllers = [
                ('blkio', True), ('devices', True), ('cpu', True), ('freezer', True), ('memory', True),
                ('systemd', False)
            ]
            self._prepare_test(mock, base, controllers)

            for group, expected_result in [
                ('skynet', [os.path.join(base, c[0], 'skynet') for c in self.controllers if c[1]]),
                ('skynet/services', [os.path.join(base, c[0], 'skynet/services') for c in self.controllers if c[1]]),
                ('infra', [os.path.join(base, c[0], 'infra') for c in self.controllers if c[1]]),
                ('infra/oops', [os.path.join(base, c[0], 'infra/oops') for c in self.controllers if c[1]]),
                ('dummy', ['EXCEPTION'])
            ]:
                try:
                    result = sorted(self.controller.get_valid_cgroups(group))
                except Exception:
                    result = ['EXCEPTION']
                expected_result = sorted(expected_result)
                self.assertEqual(expected_result, result)

    @mock.patch.object(CgroupsController, '_create_cgroup')
    def test_only_systemd_controllers(self, mock):
        with TempDir() as base:
            controllers = [
                ('systemd', False)
            ]
            self._prepare_test(mock, base, controllers)

            for group, expected_result in [
                ('skynet', []), ('skynet/services', []), ('infra', []), ('infra/oops', []), ('dummy', ['EXCEPTION'])
            ]:
                try:
                    result = sorted(self.controller.get_valid_cgroups(group))
                except Exception as e:
                    self.log.debug(e)
                    result = ['EXCEPTION']
                expected_result = sorted(expected_result)
                self.assertEqual(expected_result, result)

    @mock.patch.object(CgroupsController, '_create_cgroup')
    def test_all_readonly_controllers(self, mock):
        with TempDir() as base:
            controllers = [
                ('blkio', False), ('devices', False), ('cpu', False), ('freezer', False), ('memory', False)
            ]
            self._prepare_test(mock, base, controllers)

            for group, expected_result in [
                ('skynet', []), ('skynet/services', []), ('infra', []), ('infra/oops', []), ('dummy', ['EXCEPTION'])
            ]:
                try:
                    result = sorted(self.controller.get_valid_cgroups(group))
                except Exception:
                    result = ['EXCEPTION']
                expected_result = sorted(expected_result)
                self.assertEqual(expected_result, result)


if __name__ == '__main__':
    main()
