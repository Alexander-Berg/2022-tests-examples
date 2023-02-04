from __future__ import absolute_import

import os
import fcntl
import textwrap
from cStringIO import StringIO

import mock
import yaml

from skycore.kernel_util.unittest import TestCase, main
from skycore.kernel_util import logging
from skycore.kernel_util.sys import TempDir

from skycore.components.serviceupdater import ServiceUpdater
from skycore.service_config import ServiceConfig
from skycore.components.service import Service, State, StateController
from skycore.components.namespace import Namespace, InstallationUnit
from skycore.framework.greendeblock import Deblock
from skycore import initialize_skycore


def service_mock(ns, name):
    cfg = ServiceConfig.from_context(
        name=name,
        basepath='/',
        porto='no',
        requirements=[],
        check='check',
        stop='stop',
        conf_sections=[],
        conf_action=None,
        conf_format='yaml',
        executables=[],
        user='root',
    )
    srvc = Service(
        namespace=ns,
        starter=mock.Mock(),
        output_logger_factory=mock.Mock(),
        cfg=cfg,
    )
    return srvc


def mock_timecast(ts):
    return int(ts)


def mock_config_srvc_1():
    return {
        'subsections': {'skynet': {
            'subsections': {
                'versions': {
                    'config_hash': '2134',
                    'config': {
                        'md5': '12345',
                        'svn_url': '12345',
                        'urls': {},
                        'version': 'bcd',
                        'filename': 'skycore',
                    },
                },
                'skycore': {'subsections': {
                    'namespaces': {
                        'subsections': {
                            'skynet': {
                                'subsections': {
                                    'service1': {
                                        'config_hash': '2134',
                                        'config': {
                                            'md5': '12345',
                                            'urls': {},
                                            'version': 'bcd',
                                            'size': 1,
                                            'filename': 'service1',
                                        }
                                    },
                                },
                                'config_hash': '7777',
                            }
                        },
                        'config_hash': '7777',
                    }
                }}
            },
            'config_hash': '7777',
        }}}


def mock_config_srvc_1_2():
    return {
        'subsections': {'skynet': {
            'subsections': {
                'versions': {
                    'config_hash': '2134',
                    'config': {
                        'md5': '12345',
                        'svn_url': '12345',
                        'urls': {},
                        'version': 'bcd',
                        'size': 1,
                        'filename': 'skycore',
                    }
                },
                'skycore': {'subsections': {
                    'namespaces': {
                        'subsections': {
                            'skynet': {
                                'subsections': {
                                    'service1': {
                                        'config_hash': '2134',
                                        'config': {
                                            'md5': '12345',
                                            'urls': {},
                                            'version': 'bcd',
                                            'size': 1,
                                            'filename': 'service1',
                                        }
                                    },
                                    'service2': {
                                        'config_hash': '2134',
                                        'config': {
                                            'md5': '12345',
                                            'urls': {},
                                            'version': 'bcd',
                                            'size': 1,
                                            'filename': 'service2',
                                        }
                                    }
                                },
                                'config_hash': '7777',
                            }
                        },
                        'config_hash': '7777',
                    }
                }}
            },
            'config_hash': '7777',
        }}}


def mock_config_srvc_1_21():
    return {
        'subsections': {'skynet': {
            'subsections': {
                'versions': {
                    'config_hash': '2134',
                    'config': {
                        'md5': '12345',
                        'svn_url': '12345',
                        'urls': {},
                        'version': 'bcd',
                        'size': 1,
                        'filename': 'skycore',
                    }
                },
                'skycore': {
                    'subsections': {
                        'namespaces': {
                            'subsections': {
                                'skynet': {
                                    'subsections': {
                                        'service1': {
                                            'config_hash': '2134',
                                            'config': {
                                                'md5': '12345',
                                                'urls': {},
                                                'version': 'bcd',
                                                'size': 1,
                                                'filename': 'service1',
                                            }
                                        },
                                        'service2': {
                                            'config_hash': '2134',
                                            'config': {
                                                'md5': '56789',
                                                'urls': {},
                                                'version': 'bcd',
                                                'size': 1,
                                                'filename': 'service2',
                                            }
                                        }
                                    },
                                    'config_hash': '7777',
                                }
                            },
                            'config_hash': '7777',
                        }
                    },
                    'config_hash': '7777',
                }
            },
            'config_hash': '7777',
        }}}


def mock_config_srvc_3_22_new_skycore():
    return {
        'subsections': {'skynet': {
            'subsections': {
                'versions': {
                    'config_hash': '2134',
                    'config': {
                        'md5': '23456',
                        'svn_url': '23456',
                        'urls': {},
                        'version': 'abc',
                        'size': 1,
                        'filename': 'skycore',
                    }
                },
                'skycore': {
                    'subsections': {
                        'namespaces': {
                            'subsections': {
                                'skynet': {
                                    'subsections': {
                                        'service3': {
                                            'config_hash': '2134',
                                            'config': {
                                                'md5': '23456',
                                                'urls': {},
                                                'version': 'bcd',
                                                'size': 1,
                                                'filename': 'service3',
                                            }
                                        },
                                        'service2': {
                                            'config_hash': '2134',
                                            'config': {
                                                'md5': '23457',
                                                'urls': {},
                                                'version': 'bcd',
                                                'size': 1,
                                                'filename': 'service2',
                                            }
                                        }
                                    },
                                    'config_hash': '7777',
                                }
                            },
                            'config_hash': '7777',
                        }
                    },
                    'config_hash': '7777',
                }
            },
            'config_hash': '7777',
        }}}


def mock_config_srvc_3_22():
    return {
        'subsections': {'skynet': {
            'subsections': {
                'versions': {
                    'config_hash': '2134',
                    'config': {
                        'md5': '12345',
                        'svn_url': '12345',
                        'urls': {},
                        'version': 'bcd',
                        'size': 1,
                        'filename': 'skycore',
                    }
                },
                'skycore': {
                    'subsections': {
                        'namespaces': {
                            'subsections': {
                                'skynet': {
                                    'subsections': {
                                        'service3': {
                                            'config_hash': '2134',
                                            'config': {
                                                'md5': '23456',
                                                'urls': {},
                                                'version': 'bcd',
                                                'size': 1,
                                                'filename': 'service3',
                                            }
                                        },
                                        'service2': {
                                            'config_hash': '2134',
                                            'config': {
                                                'md5': '23457',
                                                'urls': {},
                                                'version': 'bcd',
                                                'size': 1,
                                                'filename': 'service2',
                                            }
                                        }
                                    },
                                    'config_hash': '7777',
                                }
                            },
                            'config_hash': '7777',
                        }
                    },
                    'config_hash': '7777',
                }
            },
            'config_hash': '7777',
        }}}


def mock_config_srvc_31_22():
    return {
        'subsections': {'skynet': {
            'subsections': {
                'versions': {
                    'config_hash': '2134',
                    'config': {
                        'md5': '12345',
                        'svn_url': '12345',
                        'urls': {},
                        'version': 'bcd',
                        'size': 1,
                        'filename': 'skycore',
                    }
                },
                'skycore': {
                    'subsections': {
                        'namespaces': {
                            'subsections': {
                                'skynet': {
                                    'subsections': {
                                        'service3': {
                                            'config_hash': '2134',
                                            'config': {
                                                'md5': '23458',
                                                'urls': {},
                                                'version': 'bcd',
                                                'size': 1,
                                                'filename': 'service3',
                                            }
                                        },
                                        'service2': {
                                            'config_hash': '2134',
                                            'config': {
                                                'md5': '23457',
                                                'urls': {},
                                                'version': 'bcd',
                                                'size': 1,
                                                'filename': 'service2',
                                            }
                                        }
                                    },
                                    'config_hash': '7777',
                                }
                            },
                            'config_hash': '7777',
                        }
                    },
                    'config_hash': '7777',
                }
            },
            'config_hash': '7777',
        }}}


def mock_config_srvc_32_22():
    return {
        'subsections': {'skynet': {
            'subsections': {
                'versions': {
                    'config_hash': '2134',
                    'config': {
                        'md5': '12345',
                        'svn_url': '12345',
                        'urls': {},
                        'version': 'bcd',
                        'size': 1,
                        'filename': 'skycore',
                    }
                },
                'skycore': {
                    'subsections': {
                        'namespaces': {
                            'subsections': {
                                'skynet': {
                                    'subsections': {
                                        'service3': {
                                            'config_hash': '2134',
                                            'config': {
                                                'md5': '23459',
                                                'urls': {},
                                                'version': 'bcd',
                                                'size': 1,
                                                'filename': 'service3',
                                            }
                                        },
                                        'service2': {
                                            'config_hash': '2134',
                                            'config': {
                                                'md5': '23457',
                                                'urls': {},
                                                'version': 'bcd',
                                                'size': 1,
                                                'filename': 'service2',
                                            }
                                        }
                                    },
                                    'config_hash': '7777',
                                }
                            },
                            'config_hash': '7777',
                        }
                    },
                    'config_hash': '7777',
                }
            },
            'config_hash': '7777',
        }}}


def mock_config_srvc_33_2broken():
    return {
        'subsections': {'skynet': {
            'subsections': {
                'versions': {
                    'config_hash': '2134',
                    'config': {
                        'md5': '12345',
                        'svn_url': '12345',
                        'urls': {},
                        'version': 'bcd',
                        'size': 1,
                        'filename': 'skycore',
                    }
                },
                'skycore': {
                    'subsections': {
                        'namespaces': {
                            'subsections': {
                                'skynet': {
                                    'subsections': {
                                        'service3': {
                                            'config_hash': '2134',
                                            'config': {
                                                'md5': '34567',
                                                'urls': {},
                                                'version': 'bcd',
                                                'size': 1,
                                                'filename': 'service3',
                                            }
                                        },
                                        'service2': {
                                            'config_hash': '2134',
                                            'config': {
                                                'md5': '34568',
                                                'urls': {},
                                                'version': 'bcd',
                                                'size': 1,
                                                'filename': 'service2',
                                            }
                                        },
                                        'service4': {
                                            'config_hash': '2134',
                                            'config': {
                                                'md5': '34569',
                                                'urls': {},
                                                'version': 'bcd',
                                                'size': 1,
                                                'filename': 'service2',
                                            }
                                        }
                                    },
                                    'config_hash': '7777',
                                }
                            },
                            'config_hash': '7777',
                        }
                    },
                    'config_hash': '7777',
                }
            },
            'config_hash': '7777',
        }}}


def mock_parse_advertised_release(data):
    return data


def mock_extract(archive, dest, log=None, fsyncqueue=None):
    dst = os.path.join(dest, os.path.basename(archive)) + '.scsd'
    with open(dst, 'wb') as f:
        f.write(textwrap.dedent(
            """
            conf: []
            conf_format: yaml
            conf_action: None
            cgroup: ''
            user: root
            exec: []
            require: []
            check: check
            stop: stop
            porto: no
            """))
        return None, None


def mock_extract_no_restart(archive, dest, log=None, fsyncqueue=None):
    dst = os.path.join(dest, os.path.basename(archive)) + '.scsd'
    with open(dst, 'wb') as f:
        f.write(textwrap.dedent(
            """
            conf: []
            conf_format: yaml
            conf_action: None
            cgroup: ''
            user: root
            exec: []
            require: []
            check: check
            stop: stop
            porto: no
            restart_on_upgrade: false
            """))
        return None, None


def mock_start_service(self, *args, **kwargs):
    self.log.warning("starting")
    self.set_required_state(State.RUNNING)
    self._state_controller.state = State.RUNNING
    return True


def mock_stop_service(self, *args, **kwargs):
    self.log.warning("stopping")
    self.set_required_state(State.STOPPED)
    self._state_controller.state = State.STOPPED
    return True


class RegistryMock(object):
    _data = {}

    def subscribe(self, callback, sections, config_only=False):
        pass

    def unsubscribe(self, callback):
        pass

    def query(self, query, deepcopy=True):
        cfg = self._data
        for el in query:
            cfg = cfg['subsections'][el]
        return cfg

    def send_report(self, success, description, hostname=None):
        pass


class TestServiceUpdater(TestCase):
    def setUp(self):
        initialize_skycore()
        super(TestServiceUpdater, self).setUp()

        self.log = logging.getLogger('tstsrvcupd')
        # self.log.propagate = False
        self.log.setLevel(logging.DEBUG)
        self.io = StringIO()
        handler = logging.StreamHandler(self.io)
        self.log.addHandler(handler)
        self.deblock = Deblock(name='test_serviceupdater')

    def tearDown(self):
        self.deblock.stop()
        super(TestServiceUpdater, self).tearDown()

    @mock.patch.object(
        StateController,
        '_perform_check',
        lambda self, *args, **kwargs: (
            (False, 0)
            if self._required_state == State.STOPPED
            else (True, 0)
        )
    )
    @mock.patch.object(Service, 'start_service', autospec=True)
    @mock.patch.object(Service, 'stop_service', autospec=True)
    @mock.patch.object(Service, 'on_install', autospec=True)
    @mock.patch.object(Service, 'on_uninstall', autospec=True)
    @mock.patch.object(Service, '_service_send_registry', autospec=True)
    @mock.patch('skycore.components.namespace.download')
    @mock.patch('skycore.components.namespace.extract')
    @mock.patch('skycore.components.serviceupdater.parse_advertised_release', mock_parse_advertised_release)
    @mock.patch('skycore.components.namespace.parse_advertised_release', mock_parse_advertised_release)
    def test_service_replacement(self, extract, download,
                                 _service_send_registry,
                                 on_uninstall,
                                 on_install, stop_service,
                                 start_service):
        extract.side_effect = mock_extract
        start_service.side_effect = mock_start_service
        stop_service.side_effect = mock_stop_service

        with TempDir() as base:
            workdir = os.path.join(base, 'workdir')
            os.mkdir(workdir)
            rundir = os.path.join(base, 'rundir')
            os.mkdir(rundir)

            registry = RegistryMock()
            registry._data = mock_config_srvc_1()
            core_revision = mock_parse_advertised_release(registry.query(['skynet', 'versions']))['config']

            updater = ServiceUpdater(
                base=base,
                deblock=self.deblock,
                workdir=workdir,
                rundir=rundir,
                linkdir=workdir,
                downloaddir=workdir,
                apidir=workdir,
                statefile=os.path.join(workdir, 'skycore.state'),
                config=registry,
                namespaces={},
                starter=mock.Mock(),
                output_logger_factory=mock.Mock(),
                core_revision=core_revision['md5'],
            )

            # install service1
            self.log.info("stage 1: install 'service1'")

            updater.check_all_namespaces(self.log, 'test updater')
            self.assertEqual(set(updater.namespaces.keys()), {'skynet'})
            ns = updater.namespaces['skynet']
            self.assertEqual(set(ns.rhashes.keys()), {'12345'})
            self.assertEqual(set(ns.services.keys()), {'service1'})
            srvc1 = ns.services['service1']
            self.assertEqual(srvc1.state, State.RUNNING)

            on_install.assert_called_once_with(srvc1, upgrade=False)
            on_install.reset_mock()
            self.assertEqual(on_uninstall.call_count, 0)
            start_service.assert_called_once_with(srvc1)
            start_service.reset_mock()
            self.assertEqual(stop_service.call_count, 0)

            # service2 with same hash, shouldn't be installed
            self.log.info("stage 2: add 'service2' with existing hash, no changes should occur")
            registry._data = mock_config_srvc_1_2()

            updater.check_all_namespaces(self.log, 'test updater')
            self.assertEqual(set(updater.namespaces.keys()), {'skynet'})
            self.assertIs(updater.namespaces['skynet'], ns)
            self.assertEqual(set(ns.rhashes.keys()), {'12345'})
            self.assertEqual(set(ns.services.keys()), {'service1'})
            self.assertIs(ns.services['service1'], srvc1)
            self.assertEqual(srvc1.state, State.RUNNING)

            self.assertEqual(on_install.call_count, 0)
            self.assertEqual(on_uninstall.call_count, 0)
            self.assertEqual(start_service.call_count, 0)
            self.assertEqual(stop_service.call_count, 0)

            # install service2
            self.log.info("stage 3: install 'service2'")
            registry._data = mock_config_srvc_1_21()

            updater.check_all_namespaces(self.log, 'test updater')
            self.assertEqual(set(updater.namespaces.keys()), {'skynet'})
            self.assertEqual(set(updater.namespaces['skynet'].rhashes.keys()), {'12345', '56789'})

            self.assertIs(updater.namespaces['skynet'], ns)
            self.assertEqual(set(ns.services.keys()), {'service1', 'service2'})
            self.assertIs(ns.services['service1'], srvc1)
            self.assertEqual(srvc1.state, State.RUNNING)

            srvc2 = ns.services['service2']
            self.assertEqual(srvc2.state, State.RUNNING)

            on_install.assert_called_once_with(srvc2, upgrade=False)
            on_install.reset_mock()
            self.assertEqual(on_uninstall.call_count, 0)
            start_service.assert_called_once_with(srvc2)
            start_service.reset_mock()
            self.assertEqual(stop_service.call_count, 0)

            # skycore hash change, require no actions
            self.log.info("stage 4: skycore hash changed together with services, no changes should occur")
            registry._data = mock_config_srvc_3_22_new_skycore()

            updater.check_all_namespaces(self.log, 'test updater')
            self.assertEqual(set(updater.namespaces.keys()), {'skynet'})
            self.assertEqual(set(updater.namespaces['skynet'].rhashes.keys()), {'12345', '56789'})

            self.assertIs(updater.namespaces['skynet'], ns)
            self.assertEqual(set(ns.services.keys()), {'service1', 'service2'})

            self.assertIs(ns.services['service1'], srvc1)
            self.assertEqual(srvc1.state, State.RUNNING)
            self.assertIs(ns.services['service2'], srvc2)
            self.assertEqual(srvc2.state, State.RUNNING)

            self.assertEqual(on_install.call_count, 0)
            self.assertEqual(on_uninstall.call_count, 0)
            self.assertEqual(start_service.call_count, 0)
            self.assertEqual(stop_service.call_count, 0)

            # upgrade service2, install service3, remove service1
            self.log.info(
                "stage 5: skycore hash returns back, upgrade 'service2', install 'service3', remove 'service1'"
            )
            registry._data = mock_config_srvc_3_22()

            updater.check_all_namespaces(self.log, 'test updater')
            self.assertEqual(set(updater.namespaces.keys()), {'skynet'})
            self.assertEqual(set(updater.namespaces['skynet'].rhashes.keys()), {'23456', '23457'})

            self.assertIs(updater.namespaces['skynet'], ns)
            self.assertEqual(set(ns.services.keys()), {'service2', 'service3'})

            srvc22 = ns.services['service2']
            self.assertIsNot(srvc22, srvc2)
            self.assertEqual(srvc22.state, State.RUNNING)
            self.assertEqual(srvc2.state, State.STOPPED)

            srvc3 = ns.services['service3']
            self.assertEqual(srvc3.state, State.RUNNING)

            self.assertEqual(on_install.call_count, 2)
            on_install.assert_has_calls([
                ((srvc22,), {'upgrade': True}),
                ((srvc3,), {'upgrade': False}),
            ], any_order=True)
            on_install.reset_mock()

            self.assertEqual(on_uninstall.call_count, 2)
            on_uninstall.assert_has_calls([
                ((srvc2,), {'upgrade': True}),
                ((srvc1,), {'upgrade': False}),
            ], any_order=True)
            on_uninstall.reset_mock()

            self.assertEqual(start_service.call_count, 2)
            start_service.assert_has_calls([
                ((srvc22,), {}),
                ((srvc3,), {}),
            ], any_order=True)
            start_service.reset_mock()

            self.assertEqual(stop_service.call_count, 2)
            stop_service.assert_has_calls([
                ((srvc2,), {'timeout': Service.STATE_CHANGE_TIMEOUT * 3.5}),
                ((srvc1,), {'timeout': Service.STATE_CHANGE_TIMEOUT * 3.5}),
            ], any_order=True)
            stop_service.reset_mock()

            # upgrade service3 with restart
            self.log.info("stage 6: upgrade `service3` with restart (old version requires)")
            registry._data = mock_config_srvc_31_22()
            extract.side_effect = mock_extract_no_restart

            updater.check_all_namespaces(self.log, 'test updater')
            self.assertEqual(set(updater.namespaces.keys()), {'skynet'})
            self.assertEqual(set(updater.namespaces['skynet'].rhashes.keys()), {'23458', '23457'})

            self.assertIs(updater.namespaces['skynet'], ns)
            self.assertEqual(set(ns.services.keys()), {'service2', 'service3'})

            self.assertIs(ns.services['service2'], srvc22)
            self.assertEqual(srvc22.state, State.RUNNING)

            srvc32 = ns.services['service3']
            self.assertIsNot(srvc32, srvc3)
            self.assertEqual(srvc32.state, State.RUNNING)
            self.assertEqual(srvc3.state, State.STOPPED)

            on_install.assert_called_once_with(srvc32, upgrade=True)
            on_install.reset_mock()

            on_uninstall.assert_called_once_with(srvc3, upgrade=True)
            on_uninstall.reset_mock()

            start_service.assert_called_once_with(srvc32)
            start_service.reset_mock()
            stop_service.assert_called_once_with(srvc3, timeout=Service.STATE_CHANGE_TIMEOUT * 3.5)
            stop_service.reset_mock()

            # upgrade service3 without restart
            self.log.info("stage 7: upgrade `service3` without restart (both versions support)")
            registry._data = mock_config_srvc_32_22()

            updater.check_all_namespaces(self.log, 'test updater')
            self.assertEqual(set(updater.namespaces.keys()), {'skynet'})
            self.assertEqual(set(updater.namespaces['skynet'].rhashes.keys()), {'23459', '23457'})

            self.assertIs(updater.namespaces['skynet'], ns)
            self.assertEqual(set(ns.services.keys()), {'service2', 'service3'})

            self.assertIs(ns.services['service2'], srvc22)
            self.assertEqual(srvc22.state, State.RUNNING)

            srvc33 = ns.services['service3']
            self.assertIsNot(srvc33, srvc32)
            self.assertEqual(srvc33.state, State.RUNNING)
            self.assertEqual(srvc32.state, State.RUNNING)

            on_install.assert_called_once_with(srvc33, upgrade=True)
            on_install.reset_mock()

            on_uninstall.assert_called_once_with(srvc32, upgrade=True)
            on_uninstall.reset_mock()

            self.assertEqual(start_service.call_count, 0)
            self.assertEqual(stop_service.call_count, 0)

            # upgrade service3 without updating broken service2
            self.log.info("stage 8: upgrade `service3` without upgrading broken service2")
            registry._data = mock_config_srvc_33_2broken()

            updater.check_all_namespaces(self.log, 'test updater')
            self.assertEqual(set(updater.namespaces.keys()), {'skynet'})
            self.assertEqual(set(updater.namespaces['skynet'].rhashes.keys()), {'34567', '23457'})

            self.assertIs(updater.namespaces['skynet'], ns)
            self.assertEqual(set(ns.services.keys()), {'service2', 'service3'})

            self.assertIs(ns.services['service2'], srvc22)
            self.assertEqual(srvc22.state, State.RUNNING)

            srvc34 = ns.services['service3']
            self.assertIsNot(srvc34, srvc33)
            self.assertEqual(srvc34.state, State.RUNNING)

            on_install.assert_called_once_with(srvc34, upgrade=True)
            on_install.reset_mock()

            on_uninstall.assert_called_once_with(srvc33, upgrade=True)
            on_uninstall.reset_mock()

            self.assertEqual(start_service.call_count, 0)
            self.assertEqual(stop_service.call_count, 0)

    def test_start_with_broken_service(self):
        with TempDir() as base:
            workdir = os.path.join(base, 'workdir')
            os.mkdir(workdir)
            rundir = os.path.join(base, 'rundir')
            os.mkdir(rundir)

            registry = RegistryMock()
            registry._data = mock_config_srvc_1()
            core_revision = mock_parse_advertised_release(registry.query(['skynet', 'versions']))['config']

            updater = ServiceUpdater(
                base=base,
                deblock=self.deblock,
                workdir=workdir,
                rundir=workdir,
                linkdir=workdir,
                downloaddir=workdir,
                apidir=workdir,
                statefile=os.path.join(workdir, 'skycore.state'),
                config=registry,
                namespaces={},
                starter=None,
                output_logger_factory=mock.Mock(),
                core_revision=core_revision['svn_url'],
            )
            ns1 = updater.namespaces['skynet'] = Namespace(
                starter=None,
                deblock=self.deblock,
                output_logger_factory=mock.Mock(),
                context_changed_event=updater.context_changed_event,
                name='skynet',
                workdir=workdir,
                rundir=rundir,
                parent=updater,
            )
            ns1._add_service(
                Service(namespace=ns1,
                        deblock=self.deblock,
                        starter=None,
                        output_logger_factory=mock.Mock(),
                        context_changed_event=ns1.context_changed_event,
                        cfg=ServiceConfig.from_context(name='srv1',
                                                       base=base,
                                                       basepath='',
                                                       porto='yes',
                                                       requirements=['srv2'],
                                                       cgroup='/nonexistent',
                                                       check='check1',
                                                       stop='stop1',
                                                       conf_sections=['sctn1'],
                                                       conf_action=None,
                                                       conf_format='yaml',
                                                       executables=['exec1'],
                                                       user='nobody')),
                InstallationUnit('<test>', {'md5': 'a' * 32}, self.deblock),
            )
            ns1._add_service(
                Service(namespace=ns1,
                        deblock=self.deblock,
                        starter=None,
                        output_logger_factory=mock.Mock(),
                        context_changed_event=ns1.context_changed_event,
                        cfg=ServiceConfig.from_context(name='srv2',
                                                       base=base,
                                                       basepath='',
                                                       porto='yes',
                                                       requirements=[],
                                                       check='check2',
                                                       stop='stop2',
                                                       conf_sections=['sctn2'],
                                                       conf_action=None,
                                                       conf_format='yaml',
                                                       executables=['exec2'],
                                                       user='nobody')),
                InstallationUnit('<test>', {'md5': 'b' * 32}, self.deblock),
            )

            saved_state = yaml.safe_dump(updater.context)
            restored_state = yaml.load(StringIO(saved_state))

            self.log.info("Now we'll restore with broken one service")
            new_updater = ServiceUpdater(
                base=base,
                deblock=self.deblock,
                workdir=workdir,
                rundir=workdir,
                linkdir=workdir,
                downloaddir=workdir,
                apidir=workdir,
                statefile=os.path.join(workdir, 'skycore.state'),
                config=registry,
                namespaces={},
                starter=None,
                output_logger_factory=mock.Mock(),
                core_revision=core_revision['svn_url'],
            )
            self.log.info("restoring context")
            new_updater.restore_context(restored_state)
            self.log.info("starting service updater")
            new_updater.start()

            try:
                assert 'skynet' in new_updater.namespaces
                assert 'srv2' in new_updater.namespaces['skynet'].services
                assert 'srv1' not in new_updater.namespaces['skynet'].services

                srv2 = new_updater.namespaces['skynet'].services['srv2']
                for loopfn, loop in srv2.loops.iteritems():
                    assert loop is not None, loopfn
                    assert not loop.ready(), loopfn
            finally:
                self.log.info("Stopping service updater")
                new_updater.stop()

    def test_repair_corrupted(self):
        with TempDir() as base:
            workdir = os.path.join(base, 'workdir')
            os.mkdir(workdir)
            rundir = os.path.join(base, 'rundir')
            os.mkdir(rundir)

            registry = RegistryMock()
            registry._data = mock_config_srvc_1()
            core_revision = mock_parse_advertised_release(registry.query(['skynet', 'versions']))['config']

            updater = ServiceUpdater(
                base=base,
                deblock=self.deblock,
                workdir=workdir,
                rundir=workdir,
                linkdir=workdir,
                downloaddir=workdir,
                apidir=workdir,
                statefile=os.path.join(workdir, 'skycore.state'),
                config=registry,
                namespaces={},
                starter=None,
                output_logger_factory=mock.Mock(),
                core_revision=core_revision['svn_url'],
            )

            ns = updater.namespaces['skynet'] = Namespace(
                starter=None,
                deblock=self.deblock,
                output_logger_factory=mock.Mock(),
                context_changed_event=updater.context_changed_event,
                name='skynet',
                workdir=workdir,
                rundir=rundir,
                parent=updater,
            )
            svc = Service(namespace=ns,
                          deblock=self.deblock,
                          starter=None,
                          output_logger_factory=mock.Mock(),
                          context_changed_event=ns.context_changed_event,
                          cfg=ServiceConfig.from_context(name='srv1',
                                                         base=base,
                                                         basepath='',
                                                         porto='yes',
                                                         requirements=[],
                                                         check='check1',
                                                         stop='stop1',
                                                         conf_sections=['sctn1'],
                                                         conf_action=None,
                                                         conf_format='yaml',
                                                         executables=['exec1'],
                                                         user='nobody'))
            unit = InstallationUnit('<test>', {'md5': 'a' * 32}, self.deblock)
            unit.conntent_md5 = 'd41d8cd98f00b204e9800998ecf8427e'  # empty md5
            unit.content_paths = ['nonexistent']
            ns._add_service(svc, unit)

            self.assertFalse(unit.dirty)
            svc.set_required_state(State.RUNNING)
            svc._state_controller._set_state(State.CLEANUP, svc)
            svc._state_controller.start_attempts = 500
            svc._state_controller.tick(svc)
            self.assertFalse(unit.dirty)

            svc._state_controller._set_state(State.CLEANUP, svc)
            unit.content_md5 = 'test'
            svc._state_controller.tick(svc)
            self.assertTrue(unit.dirty)

            with mock.patch('skycore.components.namespace.extract') as extract:
                extract.side_effect = lambda *args, **kwargs: ('a' * 32, 'd41d8cd98f00b204e9800998ecf8427e')
                unit.extract(workdir, self.log)
                list(ns.install_services(unit))

            self.assertFalse(unit.dirty)

    def test_state_recover(self):
        with TempDir() as base:
            workdir = os.path.join(base, 'workdir')
            os.mkdir(workdir)
            rundir = os.path.join(base, 'rundir')
            os.mkdir(rundir)

            registry = RegistryMock()
            registry._data = mock_config_srvc_1()
            core_revision = mock_parse_advertised_release(registry.query(['skynet', 'versions']))['config']

            updater = ServiceUpdater(
                base=base,
                deblock=self.deblock,
                workdir=workdir,
                rundir=workdir,
                linkdir=workdir,
                downloaddir=workdir,
                apidir=workdir,
                statefile=os.path.join(workdir, 'skycore.state'),
                config=registry,
                namespaces={},
                starter=None,
                output_logger_factory=mock.Mock(),
                core_revision=core_revision['svn_url'],
            )

            updater.write_context()
            with self.assertRaises(Exception):
                with open(updater.statefile, 'r') as f:
                    fcntl.flock(f.fileno(), fcntl.LOCK_NB | fcntl.LOCK_EX)

            updater.locked_state.close()
            with open(updater.statefile, 'r') as f:
                fcntl.flock(f.fileno(), fcntl.LOCK_NB | fcntl.LOCK_EX)

            updater.write_context()
            with self.assertRaises(Exception):
                with open(updater.statefile, 'r') as f:
                    fcntl.flock(f.fileno(), fcntl.LOCK_NB | fcntl.LOCK_EX)

    def test_restart(self):
        with TempDir() as base:
            workdir = os.path.join(base, 'workdir')
            os.mkdir(workdir)
            rundir = os.path.join(base, 'rundir')
            os.mkdir(rundir)

            registry = RegistryMock()
            registry._data = mock_config_srvc_1()
            core_revision = mock_parse_advertised_release(registry.query(['skynet', 'versions']))['config']

            updater = ServiceUpdater(
                base=base,
                deblock=self.deblock,
                workdir=workdir,
                rundir=workdir,
                linkdir=workdir,
                downloaddir=workdir,
                apidir=workdir,
                statefile=os.path.join(workdir, 'skycore.state'),
                config=registry,
                namespaces={},
                starter=None,
                output_logger_factory=mock.Mock(),
                core_revision=core_revision['svn_url'],
            )

            ns1 = updater.namespaces['skynet'] = Namespace(
                starter=None,
                deblock=self.deblock,
                output_logger_factory=mock.Mock(),
                context_changed_event=updater.context_changed_event,
                name='skynet',
                workdir=workdir,
                rundir=rundir,
                parent=updater,
            )
            ns1._add_service(
                Service(namespace=ns1,
                        deblock=self.deblock,
                        starter=None,
                        output_logger_factory=mock.Mock(),
                        context_changed_event=ns1.context_changed_event,
                        cfg=ServiceConfig.from_context(name='srv1',
                                                       base=base,
                                                       basepath='',
                                                       porto='yes',
                                                       requirements=['srv2'],
                                                       check='check1',
                                                       stop='stop1',
                                                       conf_sections=['sctn1'],
                                                       conf_action=None,
                                                       conf_format='yaml',
                                                       executables=['exec1'],
                                                       user='nobody')),
                InstallationUnit('<test>', {'md5': 'a' * 32}, self.deblock),
            )
            ns1._add_service(
                Service(namespace=ns1,
                        deblock=self.deblock,
                        starter=None,
                        output_logger_factory=mock.Mock(),
                        context_changed_event=ns1.context_changed_event,
                        cfg=ServiceConfig.from_context(name='srv2',
                                                       base=base,
                                                       basepath='',
                                                       porto='yes',
                                                       requirements=[],
                                                       check='check2',
                                                       stop='stop2',
                                                       conf_sections=['sctn2'],
                                                       conf_action=None,
                                                       conf_format='yaml',
                                                       executables=['exec2'],
                                                       user='nobody')),
                InstallationUnit('<test>', {'md5': 'b' * 32}, self.deblock),
            )
            ns2 = updater.namespaces['ns2'] = Namespace(
                starter=None,
                deblock=self.deblock,
                output_logger_factory=mock.Mock(),
                context_changed_event=updater.context_changed_event,
                name='ns2',
                workdir=workdir,
                rundir=rundir,
                parent=updater,
            )
            ns2._add_service(
                Service(namespace=ns2,
                        deblock=self.deblock,
                        starter=None,
                        output_logger_factory=mock.Mock(),
                        context_changed_event=ns2.context_changed_event,
                        cfg=ServiceConfig.from_context(name='srv1',
                                                       base=base,
                                                       basepath='',
                                                       porto='no',
                                                       requirements=[],
                                                       check='check3',
                                                       stop='stop3',
                                                       conf_sections=['sctn3'],
                                                       conf_action=None,
                                                       conf_format='yaml',
                                                       executables=['exec3'],
                                                       user='root')),
                InstallationUnit('<test>', {'md5': 'a' * 32}, self.deblock),
            )

            saved_state = yaml.safe_dump(updater.context)
            restored_state = yaml.load(StringIO(saved_state))

            new_updater = ServiceUpdater(
                base=base,
                deblock=self.deblock,
                workdir=workdir,
                rundir=workdir,
                linkdir=workdir,
                downloaddir=workdir,
                apidir=workdir,
                statefile=os.path.join(workdir, 'skycore.state'),
                config=registry,
                namespaces={},
                starter=None,
                output_logger_factory=mock.Mock(),
                core_revision=core_revision['svn_url'],
            )
            new_updater.restore_context(restored_state)

            with mock.patch('skycore.framework.utils.to_monotime') as to_monotime:
                with mock.patch('skycore.framework.utils.from_monotime') as from_monotime:
                    to_monotime.side_effect = mock_timecast
                    from_monotime.side_effect = mock_timecast

                    self.assertEqual(updater.namespaces, new_updater.namespaces)
                    self.assertEqual(updater.context, new_updater.context)


if __name__ == '__main__':
    main()
