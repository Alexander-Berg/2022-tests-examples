#!/usr/bin/env python

import unittest
import json
import urllib2
from copy import copy, deepcopy
from os.path import join
import gevent

from system import FakeLocker
import infra.dist.dmover.lib.internal.locker
import infra.dist.dmover.lib.internal.repo
infra.dist.dmover.lib.internal.locker.Locker = FakeLocker
from infra.dist.dmover.lib.driver import Driver
from infra.dist.dmover.lib.server import Server
from infra.dist.dmover.lib.internal.package import Package
from infra.dist.dmover.lib.internal.dmove_request import DmoveRequest
from infra.dist.dmover.lib.context import Context
from infra.dist.dmover.lib.internal.exc import DmoveError

from repo_index import DebianRepoIndex, RedhatRepoIndex
from system import System


def fake_grep(self, branch, architecture, packs):
    if self.name == 'turbo':
        raise NotImplementedError
    for location in self.grep_local(branch, architecture, packs):
        yield location


def fake_locate(self, *args, **kwargs):
    self.cacus = False
    res = self.orig_locate(*args, **kwargs)
    self.cacus = True
    return res


def _fake_repull_atom(self, branch, arch):
    self.log.info(
        "Imitation of repo update for {}/{}/{}".format(
            self.name, branch, arch)
    )

infra.dist.dmover.lib.internal.repo.Repo.orig_locate = infra.dist.dmover.lib.internal.repo.Repo.locate_packages
infra.dist.dmover.lib.internal.repo.Repo.locate_packages = fake_locate
infra.dist.dmover.lib.internal.repo.DebianRepo._repull_atom = _fake_repull_atom


def init(system_root='/root/'):
    system = System(system_root)

    context = Context()
#   context.boot(['--console-log'])
    context.boot([])
    context.cfg['repos']['dmoveTimeout'] = 3
    context.cfg['repos']['path'] = system.root

    old_meths = {
        'isdir': infra.dist.dmover.lib.internal.repo.os.path.isdir,
    }

    infra.dist.dmover.lib.internal.repo.open = system.open
    infra.dist.dmover.lib.internal.repo.os.path.isdir = system.isdir
    infra.dist.dmover.lib.internal.repo.DebianRepo.invoke_dmove = system.debian_dmove
    infra.dist.dmover.lib.internal.repo.RedhatRepo.invoke_dmove = system.redhat_dmove

    # Unfortunately it's diffucult to simulate dpkg -I for source package detection
    infra.dist.dmover.lib.internal.repo.DebianRepo.detect_source_package = infra.dist.dmover.lib.internal.repo.Repo.detect_source_package

    return system, context, old_meths


def revert(old_meths):
    infra.dist.dmover.lib.internal.repo.os.path.isdir = old_meths.get('isdir')
    infra.dist.dmover.lib.internal.repo.open = old_meths.get('open')

TEST_REPO_NAME = 'debian'
TEST_DATA = {
    'repo_info': {
        'os_type': 'debian',
        'dmove_key': '-d',
    },
    'fake_repo_kwargs': {
        'branch': 'unstable',
        'arch': 'all',
    },
    'fake_repo_class': DebianRepoIndex,
    'packages': {
        'package': {
            'version': '2.2.3',
            'arch': 'all',
        },
    },
}


def fill_repo(system, context, repo_name=None, data=None):
    if repo_name is None:
        repo_name = TEST_REPO_NAME
    if data is None:
        data = TEST_DATA
    repo_info = data['repo_info']
    repo = infra.dist.dmover.lib.internal.repo.make_repo(repo_name, repo_info, context)
    for branch in ['unstable', 'testing', 'prestable', 'stable']:
        for arch in ['all', 'amd64', 'i386']:
            system.get_repo(name=repo_name,
                            dmove_key=repo_info['dmove_key'],
                            repo_class=data['fake_repo_class'],
                            branch=branch,
                            arch=arch)
    fake_repo = system.get_repo(
        name=repo_name,
        dmove_key=repo_info['dmove_key'],
        repo_class=data['fake_repo_class'],
        **data['fake_repo_kwargs']
    )

    packs = []
    for pack_name, pack_data in data['packages'].iteritems():
        package = Package(pack_name, pack_data['version'], None)
        fake_repo.add_package(package, pack_data['arch'])
        packs.append(package)

    system.build_indexes()

    return repo, packs


class TestSelf(unittest.TestCase):

    def setUp(self):
        self.system = System('/var/')

    def test_open(self):
        open = self.system.open

        file_content = 'blah minor'
        file_path = '/var/tmp/blah'
        self.system.add_file(file_path, file_content)

        self.assertRaises(IOError, open, '/nonexistent/file')
        self.assertEqual(open(file_path).read(), file_content)

    def test_isdir(self):
        isdir = self.system.isdir
        root = self.system.root
        self.assertFalse(isdir('/root/'))
        self.assertTrue(isdir(root))

        name = 'super-repo'
        branch = 'stable'
        arch = 'all'

        self.assertFalse(isdir(join(root, name)))
        self.assertFalse(isdir(join(root, name, branch)))
        self.assertFalse(isdir(join(root, name, branch, arch)))

        self.system.get_debian_repo(name=name, branch=branch, arch=arch)

        self.assertTrue(isdir(join(root, name)))
        self.assertTrue(isdir(join(root, name, branch)))
        self.assertTrue(isdir(join(root, name, branch, arch)))


class TestRepo(unittest.TestCase):

    def setUp(self):
        self.system, self.context, self.old_meths = init()

    def tearDown(self):
        revert(self.old_meths)

    def test_make_repo(self):
        repo_info = {
            'os_type': 'debian',
            'dmove_key': '-d',
        }
        debian_repo = infra.dist.dmover.lib.internal.repo.make_repo('debian', repo_info, self.context)
        self.assertIsInstance(debian_repo, infra.dist.dmover.lib.internal.repo.DebianRepo)

        repo_info['os_type'] = 'redhat'
        redhat_repo = infra.dist.dmover.lib.internal.repo.make_repo('redhat', repo_info, self.context)
        self.assertIsInstance(redhat_repo, infra.dist.dmover.lib.internal.repo.RedhatRepo)

    def test_dmove(self):
        test_data = {
            'redhat': {
                'repo_info': {
                    'os_type': 'redhat',
                    'dmove_key': '-rh',
                },
                'fake_repo_class': RedhatRepoIndex,
                'fake_repo_kwargs': {
                    'branch': 'unstable',
                    'arch': 'i386',
                },
                'packages': {
                    'package': {
                        'version': '1.0-29',
                        'arch': 'noarch',
                    },
                },
                'move_to': 'testing',
            },

            'redhat-nobranch': {
                'repo_info': {
                    'os_type': 'redhat6-unbranched',
                    'dmove_key': '-r',
                },
                'fake_repo_kwargs': {
                    'branch': None,
                    'arch': 'x86_64',
                    'redhat_version': 6,
                },
                'fake_repo_class': RedhatRepoIndex,
                'packages': {
                    'package': {
                        'version': '2.1-338',
                        'arch': 'noarch',
                    },
                },
            },

            'debian': {
                'repo_info': {
                    'os_type': 'debian',
                    'dmove_key': '-d',
                },
                'fake_repo_kwargs': {
                    'branch': 'testing',
                    'arch': 'all',
                },
                'fake_repo_class': DebianRepoIndex,
                'packages': {
                    'package': {
                        'version': '2.2.3',
                        'arch': 'all',
                    },
                },
                'move_to': 'stable',
            },
        }

        for repo_name, data in test_data.iteritems():
            repo, packs = fill_repo(self.system, self.context, repo_name, data)

            locations = repo.locate_packages(packs)

            self.assertItemsEqual(packs, [l.package for l in locations])

            if 'move_to' in data:
                to_branch = data['move_to']
                repo.dmove(locations, to_branch)
                new_repo_kwargs = copy(data['fake_repo_kwargs'])
                new_repo_kwargs['branch'] = to_branch
                new_repo = self.system.get_repo(name=repo_name, **new_repo_kwargs)
                for pack in packs:
                    self.assertTrue(new_repo.has_package(pack.name, pack.version))

    def test_missing_location(self):
        repo, packs = fill_repo(self.system, self.context)

        missing_pack = Package('nonexistant', '1.0-2', None)
        locations = repo.locate_packages([missing_pack] + packs)
        missing_location = None
        for loc in locations:
            if loc.package is missing_pack:
                missing_location = loc
        self.assertIsNotNone(missing_location)
        self.assertTrue(missing_location.is_missing())

    def test_dmove_missing(self):
        repo, packs = fill_repo(self.system, self.context)

        missing_pack = Package('yandex-nothing', '3.15-9~20', None)
        locations = repo.locate_packages([missing_pack])
        self.assertRaises(DmoveError, repo.dmove, locations, 'stable')
        for loc in locations:
            loc.branch = 'unstable'
        self.assertRaises(DmoveError, repo.dmove, locations, 'stable')

    def test_dmove_error_and_skip(self):
        repo, packs = fill_repo(self.system, self.context)
        locations = repo.locate_packages(packs)

        def buggy_dmove(repo, dmove_key, to_branch, location, skip_reindex=False):
            return 1, 'Error'

        infra.dist.dmover.lib.internal.repo.DebianRepo.invoke_dmove = buggy_dmove
        self.assertRaises(DmoveError, repo.dmove, locations, 'stable')
        # Buggy dmove should not affect packages already in place
        self.assertTrue(repo.dmove(locations, TEST_DATA['fake_repo_kwargs']['branch']))

    def test_unbranched_repo(self):
        data = {
            'repo_info': {
                'os_type': 'redhat6-unbranched',
                'dmove_key': None,
            },
            'fake_repo_kwargs': {
                'arch': 'x86_64',
            },
            'fake_repo_class': RedhatRepoIndex,
            'packages': {
                'package': {
                    'version': '2.2.3',
                    'arch': 'noarch',
                },
            }
        }
        repo, packs = fill_repo(self.system, self.context,  'unbranched', data)
        locations = repo.locate_packages(packs)
        self.assertTrue(repo.dmove(locations, 'stable'))


class TestDriver(unittest.TestCase):

    def setUp(self):
        self.system, self.context, self.old_meths = init()
        self.driver = Driver(self.context)

    def tearDown(self):
        revert(self.old_meths)

    def test_request(self):
        repo, packs = fill_repo(self.system, self.context)

        move_to = 'stable'
        request_data = {
            'repos': {
                TEST_REPO_NAME: {
                    'info': TEST_DATA['repo_info'],
                    'packages': [[pack.name, pack.version] for pack in packs],
                },
            },
            'branch': move_to
        }

        request = DmoveRequest(self.context, request_data)
        self.driver.dmove(request)

        new_repo_kwargs = copy(TEST_DATA['fake_repo_kwargs'])
        new_repo_kwargs['branch'] = move_to
        new_repo = self.system.get_repo(name=TEST_REPO_NAME, **new_repo_kwargs)
        for pack in packs:
            self.assertTrue(new_repo.has_package(pack.name, pack.version))

    def test_force(self):
        repo, packs = fill_repo(self.system, self.context)

        new_repo_kwargs = copy(TEST_DATA['fake_repo_kwargs'])
        new_repo_kwargs['branch'] = 'testing'
        new_repo = self.system.get_repo(name=TEST_REPO_NAME, **new_repo_kwargs)
        new_repo.add_package(Package('static', '1.0-5', None), arch='all')
        self.system.build_indexes()
        move_to = 'unstable'

        request_data = {
            'repos': {
                TEST_REPO_NAME: {
                    'info': TEST_DATA['repo_info'],
                    'packages': [['static', '1.0-5']],
                },
            },
            'branch': move_to
        }
        request = DmoveRequest(self.context, request_data)
        self.driver.dmove(request)
        self.assertTrue(new_repo.has_package('static', '1.0-5'))

        request_data = {
            'repos': {
                TEST_REPO_NAME: {
                    'info': TEST_DATA['repo_info'],
                    'packages': [['static', '1.0-5']],
                },
            },
            'branch': move_to,
            'force': True
        }
        request = DmoveRequest(self.context, request_data)
        self.driver.dmove(request)
        new_repo_kwargs = copy(TEST_DATA['fake_repo_kwargs'])
        new_repo_kwargs['branch'] = move_to
        new_repo = self.system.get_repo(name=TEST_REPO_NAME, **new_repo_kwargs)
        self.assertTrue(new_repo.has_package('static', '1.0-5'))

    def test_unstable(self):
        repo, packs = fill_repo(self.system, self.context)

        new_repo_kwargs = copy(TEST_DATA['fake_repo_kwargs'])
        new_repo = self.system.get_repo(name=TEST_REPO_NAME, **new_repo_kwargs)
        new_repo.add_package(Package('static', '1.0-5', None), arch='all')

        move_to = 'stable'
        request_data = {
            'repos': {
                TEST_REPO_NAME: {
                    'info': TEST_DATA['repo_info'],
                    'packages': [['static', '1.0-5']],
                    },
                },
            'branch': move_to
        }

        request = DmoveRequest(self.context, request_data)
        self.driver.dmove(request)

        new_repo_kwargs = copy(TEST_DATA['fake_repo_kwargs'])
        new_repo_kwargs['branch'] = move_to
        new_repo = self.system.get_repo(name=TEST_REPO_NAME, **new_repo_kwargs)
        self.assertTrue(new_repo.has_package('static', '1.0-5'))

    def test_double_request(self):
        repo, packs = fill_repo(self.system, self.context)

        request_data = {
            'repos': {
                TEST_REPO_NAME: {
                    'info': TEST_DATA['repo_info'],
                    'packages': [[pack.name, pack.version] for pack in packs],
                    },
                },
            'branch': 'testing'
        }

        request = DmoveRequest(self.context, request_data)
        self.driver.dmove(request)

        request_data['branch'] = 'stable'
        request = DmoveRequest(self.context, request_data)
        self.driver.dmove(request)

        new_repo_kwargs = copy(TEST_DATA['fake_repo_kwargs'])
        new_repo_kwargs['branch'] = 'stable'
        new_repo = self.system.get_repo(name=TEST_REPO_NAME, **new_repo_kwargs)
        for pack in packs:
            self.assertTrue(new_repo.has_package(pack.name, pack.version))


class TestServer(unittest.TestCase):

    def setUp(self):
        self.system, self.context, self.old_meths = init()
        self.port = 19185
        self.context.cfg['web']['mode'] = 'tcp'
        self.context.cfg['web']['tcpPort'] = 19185
        self.driver = Driver(self.context)
        self.server = Server(self.context, self.driver)
        self.server.start()

    def tearDown(self):
        self.server.stop()
        revert(self.old_meths)

    def call_dmove(self, request):
        url = 'http://localhost:%d/dmove' % self.port
        return urllib2.urlopen(url, request)

    def test_dmove(self):
        repo, packs = fill_repo(self.system, self.context)

        move_to = 'stable'
        request = json.dumps({
            'repos': {
                TEST_REPO_NAME: {
                    'info': TEST_DATA['repo_info'],
                    'packages': [[pack.name, pack.version] for pack in packs],
                }
            },
            'branch': move_to,
        })

        self.call_dmove(request)

        new_repo_kwargs = copy(TEST_DATA['fake_repo_kwargs'])
        new_repo_kwargs['branch'] = move_to
        new_repo = self.system.get_repo(name=TEST_REPO_NAME, **new_repo_kwargs)
        for pack in packs:
            self.assertTrue(new_repo.has_package(pack.name, pack.version))

    def test_missing(self):
        repo, packs = fill_repo(self.system, self.context)

        request = json.dumps({
            'repos': {
                TEST_REPO_NAME: {
                    'info': TEST_DATA['repo_info'],
                    'packages': [['yandex-missing-package', '1.0-1337']],
                }
            },
            'branch': 'stable',
        })

        http_error = None
        try:
            self.call_dmove(request)
        except urllib2.HTTPError as http_error:
            pass

        self.assertIsNotNone(http_error)
        self.assertEqual(http_error.code, 500)

    def test_bad_repo_type(self):
        repo_info = copy(TEST_DATA['repo_info'])
        repo_info['os_type'] = 'ms_windows_nt4'

        request = json.dumps({
            'repos': {
                TEST_REPO_NAME: {
                    'info': repo_info,
                    'packages': [['yandex-missing-package', '1.0-1337']],
                }
            },
            'branch': 'stable',
        })

        http_error = None
        try:
            self.call_dmove(request)
        except urllib2.HTTPError as http_error:
            pass

        self.assertIsNotNone(http_error)
        self.assertEqual(http_error.code, 500)

    def test_turbo_not_implemented(self):

        # Repo must exists for arch detector to work
        fake_repo = self.system.get_repo(
            name='turbo',
            dmove_key='-t',
            repo_class=DebianRepoIndex,
            branch='unstable',
            arch='all'
        )
        self.system.build_indexes()

        repo_info = copy(TEST_DATA['repo_info'])
        repo_info['os_type'] = 'debian'

        request = json.dumps({
            'repos': {
                'turbo': {
                    'info': repo_info,
                    'packages': [['yandex-missing-package', '1.0-1337']],
                }
            },
            'branch': 'stable',
        })

        http_error = None
        try:
            self.call_dmove(request)
        except urllib2.HTTPError as http_error:
            pass

        self.assertIsNotNone(http_error)
        self.assertEqual(http_error.code, 501)

    def test_ping(self):
        ping_result = urllib2.urlopen('http://localhost:%d/ping' % self.port).read()
        self.assertEqual(ping_result, 'Ok')


class TestServerConcurrently(unittest.TestCase):
    def setUp(self):
        self.system, self.context, self.old_meths = init()
        self.port_1 = 19185
        # self.context.log.addHandler(StreamHandler(sys.stdout))
        self.context.cfg['web']['mode'] = 'tcp'
        self.context.cfg['web']['tcpPort'] = self.port_1
        self.driver_1 = Driver(self.context)
        self.server_1 = Server(self.context, self.driver_1)
        self.server_1.start()
        self.port_2 = 19186
        self.context.cfg['web']['tcpPort'] = self.port_2
        self.driver_2 = Driver(self.context)
        self.server_2 = Server(self.context, self.driver_2)
        self.server_2.slow_down = True
        self.server_2.start()
        self.concurrent_test_data = deepcopy(TEST_DATA)
        self.concurrent_test_data['packages']['package2'] = {
            'version': '1.3.9',
            'arch': 'all',
        }

    def tearDown(self):
        self.server_1.stop()
        self.server_2.stop()
        revert(self.old_meths)

    def call_dmove(self, request, port):
        url = 'http://localhost:%d/dmove' % port
        return urllib2.urlopen(url, request)

    def test_parallel_dmove(self):
        self.server_1.slow_down = True
        repo, packs = fill_repo(
            self.system, self.context, data=self.concurrent_test_data)

        move_to = 'stable'
        request_1 = {
            'repos': {
                TEST_REPO_NAME: {
                    'info': TEST_DATA['repo_info'],
                    'packages': [[packs[0].name, packs[0].version]],
                }
            },
            'branch': move_to,
        }
        request_2 = deepcopy(request_1)
        request_2['repos'][TEST_REPO_NAME]['packages'] = [
            [packs[1].name, packs[1].version]
        ]
        dmove_greenlets = [
            gevent.spawn(self.call_dmove, json.dumps(request_1),  self.port_1),
            gevent.spawn(self.call_dmove, json.dumps(request_2),  self.port_2)
        ]
        gevent.joinall(dmove_greenlets)

        new_repo_kwargs = copy(TEST_DATA['fake_repo_kwargs'])
        new_repo_kwargs['branch'] = move_to
        new_repo = self.system.get_repo(name=TEST_REPO_NAME, **new_repo_kwargs)
        for pack in packs:
            self.assertTrue(new_repo.has_package(pack.name, pack.version))

    def test_parallel_dmove_same_package(self):
        self.server_1.slow_down = True
        repo, packs = fill_repo(self.system, self.context)

        move_to = 'stable'
        move_from = TEST_DATA['fake_repo_kwargs']['branch']
        request_1 = {
            'repos': {
                TEST_REPO_NAME: {
                    'info': TEST_DATA['repo_info'],
                    'packages': [[pack.name, pack.version] for pack in packs],
                }
            },
            'branch': move_to,
        }
        dmove_greenlets = [
            gevent.spawn(self.call_dmove, json.dumps(request_1),  self.port_1),
            gevent.spawn(self.call_dmove, json.dumps(request_1),  self.port_2)
        ]
        gevent.joinall(dmove_greenlets, raise_error=True)

        new_repo_kwargs = copy(TEST_DATA['fake_repo_kwargs'])
        new_repo_kwargs['branch'] = move_to
        new_repo = self.system.get_repo(name=TEST_REPO_NAME, **new_repo_kwargs)

        old_repo_kwargs = copy(TEST_DATA['fake_repo_kwargs'])
        old_repo_kwargs['branch'] = move_from
        old_repo = self.system.get_repo(name=TEST_REPO_NAME, **old_repo_kwargs)

        for pack in packs:
            self.assertTrue(new_repo.has_package(pack.name, pack.version))
            self.assertFalse(old_repo.has_package(pack.name, pack.version))

    def test_dmove_packages_from_one_source(self):
        self.server_1.slow_down = False
        test_data = deepcopy(TEST_DATA)
        test_pkg_name = TEST_DATA['packages'].iterkeys().next()
        pfms_name = 'package_from_same_source'
        pfms_ver = TEST_DATA['packages'][test_pkg_name]['version']
        test_data['packages'][pfms_name] = {
            'version': pfms_ver,
            'arch': TEST_DATA['packages'][test_pkg_name]['arch'],
        }
        repo, packs = fill_repo(self.system, self.context, data=test_data)

        move_to = 'stable'
        move_from = TEST_DATA['fake_repo_kwargs']['branch']
        request_1 = {
            'repos': {
                TEST_REPO_NAME: {
                    'info': TEST_DATA['repo_info'],
                    'packages': [
                        [name, pack['version']]
                        for name, pack in TEST_DATA['packages'].iteritems()],
                }
            },
            'branch': move_to,
        }
        self.call_dmove(json.dumps(request_1), self.port_1)

        request_2 = deepcopy(request_1)
        request_2['repos'][TEST_REPO_NAME]['packages'] = [
            [pfms_name, pfms_ver]
        ]
        request_2_inst = DmoveRequest(self.context, request_2)
        self.driver_1.dmove(request_2_inst)

        self.call_dmove(json.dumps(request_2), self.port_2)

        new_repo_kwargs = copy(TEST_DATA['fake_repo_kwargs'])
        new_repo_kwargs['branch'] = move_to
        new_repo = self.system.get_repo(name=TEST_REPO_NAME, **new_repo_kwargs)

        old_repo_kwargs = copy(TEST_DATA['fake_repo_kwargs'])
        old_repo_kwargs['branch'] = move_from
        old_repo = self.system.get_repo(name=TEST_REPO_NAME, **old_repo_kwargs)

        for pack in packs:
            self.assertTrue(new_repo.has_package(pack.name, pack.version))
            self.assertFalse(old_repo.has_package(pack.name, pack.version))

    # bacause of mocking architecture, we cannot test package movement,
    # when we move one package, and another package from the same source
    # is moving automatically to the same environment

    def test_parallel_dmove_same_package_diff_branches(self):
        self.server_1.slow_down = True
        repo, packs = fill_repo(self.system, self.context)
        move_to = 'stable'
        request_1 = {
            'repos': {
                TEST_REPO_NAME: {
                    'info': TEST_DATA['repo_info'],
                    'packages': [[pack.name, pack.version] for pack in packs],
                }
            },
            'branch': move_to,
        }
        request_2 = copy(request_1)
        request_2['branch'] = 'unstable'
        dmove_greenlets = [
            gevent.spawn(self.call_dmove, json.dumps(request_1),  self.port_1),
            gevent.spawn(self.call_dmove, json.dumps(request_2),  self.port_2)
        ]
        gevent.joinall(dmove_greenlets)

        new_repo_kwargs = copy(TEST_DATA['fake_repo_kwargs'])
        new_repo_kwargs['branch'] = move_to
        new_repo = self.system.get_repo(name=TEST_REPO_NAME, **new_repo_kwargs)
        for pack in packs:
            self.assertTrue(new_repo.has_package(pack.name, pack.version))


if __name__ == '__main__':
    unittest.main()
