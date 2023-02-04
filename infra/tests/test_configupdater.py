from __future__ import absolute_import

import logging
import mock
import sys
import os

import yaml

from skycore.kernel_util.unittest import TestCase, main
from skycore.kernel_util.sys.dirut import TempDir
from skycore.components.configupdater import ConfigUpdater
from skycore.components.serviceupdater import ServiceUpdater
from skycore.framework.utils import Path
from skycore.framework.greendeblock import Deblock
from skycore import initialize_skycore


test_config = {
    'config': {'stable': 7580, 'staging': 7580},
    'config_hash': '1f724b771997c38731dc68f28a417eaf650432f5',
    'snapshot_revision': 12345,
    'name': 'genisys',
    'path': '',
    'subsections': {
        'skynet': {
            'config': None,
            'config_hash': '143ce804f86e2a38cdd72ed93a62fb036dad31d5',
            'name': 'skynet',
            'path': 'skynet',
            'subsections': {
                'base': {
                    'config': {'v1': True},
                    'config_hash': '9f9397a56636a3c7592e237333042ab7d5d14d27',
                    'name': 'base',
                    'path': 'skynet.base',
                    'subsections': {}
                },
                'services': {
                    'config': None,
                    'config_hash': '882b7cc8d9f6b70debf4d95046b37d48e5b637c2',
                    'name': 'services',
                    'path': 'skynet.services',
                    'subsections': {
                        'copier': {
                            'config': {'port': 6881},
                            'config_hash': '718acee296861e7bafeaf3f0bec8d8a1814a000a',
                            'name': 'copier',
                            'path': 'skynet.services.copier',
                            'subsections': {}
                        },
                        'cqudp': {
                            'config': {'port': 10040},
                            'config_hash': 'b0ed9c6f01435ca2cdb42454e5073800a6cb7b67',
                            'name': 'cqudp',
                            'path': 'skynet.services.cqudp',
                            'subsections': {}
                        }
                    }
                }
            }
        },
        'sys': {
            'config': None,
            'config_hash': '660889666c646c0defd0ced2f4bbf8f4b6b3e9c8',
            'name': 'sys',
            'path': 'sys',
            'subsections': {
                'cgroups': {
                    'config': {
                        'enabled': True
                    },
                    'config_hash': '29decfd16259616824b9862eae2d83089417a7b4',
                    'name': 'cgroups',
                    'path': 'sys.cgroups',
                    'subsections': {}
                }
            }
        }
    }
}


test_config_apply = {
    'config': {'stable': 7580, 'staging': 7580},
    'config_hash': '1181c1834012245d785120e3505ed169',
    'snapshot_revision': 12345,
    'name': 'genisys',
    'path': '',
    'subsections': {
        'skynet': {
            'config': None,
            'config_hash': '405ab7b6446d93e2c014022bf5f1ab76',
            'name': 'skynet',
            'path': 'skynet',
            'subsections': {
                'skycore': {
                    'config': None,
                    'config_hash': '9f9397a56636a3c7592e237333042ab7d5d14d27',
                    'name': 'skycore',
                    'path': 'skynet.skycore',
                    'subsections': {
                        'version': {
                            'config': {'md5': '12345', 'urls': None, 'version': 'bcd', 'filename': 'skycore'},
                            'config_hash': '9f9397a56636a3c7592e237333042ab7d5d14d27',
                            'name': 'version',
                            'path': 'skynet.skycore.version',
                            'subsections': {}
                        },
                        'namespaces': {
                            'config': None,
                            'config_hash': '0bee89b07a248e27c83fc3d5951213c1',
                            'name': 'namespaces',
                            'path': 'skynet.skycore.namespaces',
                            'subsections': {
                                'tst1': {
                                    'config': None,
                                    'config_hash': 'f5ac8127b3b6b85cdc13f237c6005d80',
                                    'name': 'tst1',
                                    'path': 'skynet.skycore.namespaces.tst1',
                                    'subsections': {
                                        'srvc1': {
                                            'config': {
                                                'md5': '12346',
                                                'urls': None,
                                                'version': 'def',
                                                'filename': 'tst1'
                                            },
                                            'config_hash': '9b9af6945c95f1aa302a61acf75c9bd6',
                                            'name': 'srvc1',
                                            'path': 'skynet.skycore.namespaces.tst1.srvc1',
                                            'subsections': {},
                                        },
                                    },
                                },
                            },
                        },
                    },
                },
            },
        },
    },
}

expected_callback_configs = [
    {
        'overridable': None,
        'config': {
            'subsections': {
                'skynet': {
                    'subsections': {
                        'base': {
                            'config': {'v1': True},
                            'config_hash': '9f9397a56636a3c7592e237333042ab7d5d14d27',
                            'name': 'base',
                            'path': 'skynet.base',
                            'subsections': {}
                        },
                        'services': {
                            'subsections': {
                                'copier': {
                                    'config': {'port': 6881},
                                    'config_hash': '718acee296861e7bafeaf3f0bec8d8a1814a000a',
                                    'name': 'copier',
                                    'path': 'skynet.services.copier',
                                    'subsections': {}
                                }
                            }
                        }
                    }
                }
            }
        }
    },
    {
        'overridable': None,
        'config': {
            'subsections': {
                'skynet': {
                    'subsections': {
                        'base': {
                            'config': {'v1': True},
                            'config_hash': '9f9397a56636a3c7592e237333042ab7d5d14d27',
                            'name': 'base',
                            'path': 'skynet.base',
                            'subsections': {}
                        },
                        'services': {
                            'subsections': {
                                'cqudp': {
                                    'config': {'port': 10040},
                                    'config_hash': 'b0ed9c6f01435ca2cdb42454e5073800a6cb7b67',
                                    'name': 'cqudp',
                                    'path': 'skynet.services.cqudp',
                                    'subsections': {}
                                },
                            }
                        }
                    }
                }
            }
        }
    },
    {
        'overridable': ['new_service'],
        'config': {
            'subsections': {
                'skynet': {
                    'subsections': {
                        'services': {
                            'config': None,
                            'config_hash': '882b7cc8d9f6b70debf4d95046b37d48e5b637c2',
                            'name': 'services',
                            'path': 'skynet.services',
                            'subsections': {
                                'copier': {
                                    'config': {'port': 6881},
                                    'config_hash': '718acee296861e7bafeaf3f0bec8d8a1814a000a',
                                    'name': 'copier',
                                    'path': 'skynet.services.copier',
                                    'subsections': {}
                                },
                                'cqudp': {
                                    'config': {'port': 10040},
                                    'config_hash': 'b0ed9c6f01435ca2cdb42454e5073800a6cb7b67',
                                    'path': 'skynet.services.cqudp',
                                    'name': 'cqudp',
                                    'subsections': {}
                                }
                            }
                        }
                    }
                },
                'sys': {
                    'subsections': {
                        'cgroups': {
                            'config': {'enabled': True},
                            'config_hash': '29decfd16259616824b9862eae2d83089417a7b4',
                            'name': 'cgroups',
                            'path': 'sys.cgroups',
                            'subsections': {}
                        }
                    }
                }
            }
        }
    },
    {
        'overridable': ['skynet', 'services', 'new_service'],
        'config': {}
    }
]


def download_config(hostname, log, last_modified=None, config_hash=None):
    return None, test_config


def download_config_apply(hostname, log, last_modified=None, config_hash=None):
    return None, test_config_apply


def mock_parse_advertised_release(data):
    return data


def send_report(hostname, log, snapshot_revision, success, description=''):
    # do nothing
    pass


def update_configs(path, config_hash):
    _update_config(path, config_hash, test_config, ['new_service'])
    for cfg in expected_callback_configs:
        _update_config(path, config_hash, cfg['config'], cfg['overridable'])


def _update_config(path, config_hash, config, section_to_create_missing):
    section_config = config
    for section in path:
        subsection_config = section_config.get('subsections', {})
        if section not in subsection_config:
            if section_to_create_missing and section in section_to_create_missing:
                if 'path' in section_config:
                    subsection_config[section] = {'config': {'enabled': True},
                                                  'config_hash': config_hash,
                                                  'name': section,
                                                  'path': section_config['path'] + '.' + section,
                                                  'subsections': {}
                                                  }
                else:
                    if section == path[-1]:
                        subsection_config[section] = {'config': {'enabled': True},
                                                      'config_hash': config_hash,
                                                      'name': section,
                                                      'path': '.'.join(path),
                                                      'subsections': {}
                                                      }
                    else:
                        subsection_config[section] = {'subsections': {}}

                    if not section_config:
                        section_config['subsections'] = subsection_config

            else:
                # there is no this section, stop updating
                break

        section_config = subsection_config[section]
        if 'config_hash' in section_config:
            section_config['config_hash'] = config_hash


class TestConfigUpdater(TestCase):
    def setUp(self):
        initialize_skycore()
        super(TestConfigUpdater, self).setUp()
        logging.basicConfig(stream=sys.stderr)
        self.log = logging.getLogger('test')
        self.log.setLevel(logging.DEBUG)
        self.deblock = Deblock(name='test_configupdater')

    def tearDown(self):
        self.deblock.stop()
        super(TestConfigUpdater, self).tearDown()

    @mock.patch('skycore.components.configupdater.send_report')
    @mock.patch('skycore.components.configupdater.download_config')
    @mock.patch('skycore.components.serviceupdater.parse_advertised_release', mock_parse_advertised_release)
    def test_failed_update(self, dc, sr):
        dc.side_effect = download_config
        with TempDir() as tempdir:
            cfg = ConfigUpdater(config_dir=tempdir, filename='active.yaml', log=self.log, deblock=self.deblock)
            cfg.paused = True

            rev = mock_parse_advertised_release(
                test_config_apply['subsections']['skynet']['subsections']['skycore']['subsections']['version']
            )['config']
            cfg.update_config(self.log, forced=True)
            self.assertEqual(sr.call_count, 1)
            self.assertEqual(sr.call_args[0][3], True)
            sr.reset_mock()

            srvcmngr = ServiceUpdater(
                output_logger_factory=mock.Mock(),
                deblock=self.deblock,
                base=tempdir,
                workdir=tempdir,
                rundir=tempdir,
                linkdir=tempdir,
                apidir=tempdir,
                statefile=os.path.join(tempdir, 'skycore.state'),
                downloaddir=tempdir,
                config=cfg,
                namespaces={},
                starter=None,
                core_revision=rev['md5'],
            )
            try:
                srvcmngr.start()
                self.assertEqual(sr.call_count, 0)
                # self.assertEqual(sr.call_args[0][3], False)
                sr.reset_mock()

                dc.side_effect = download_config_apply
                cfg.update_config(self.log, forced=True)

                self.assertEqual(sr.call_count, 2)
                self.assertEqual(sr.call_args[0][3], False)
                sr.reset_mock()
            finally:
                srvcmngr.stop()

    @mock.patch('skycore.components.configupdater.download_config', download_config)
    @mock.patch('skycore.components.configupdater.send_report', send_report)
    def test_subsections(self):
        with TempDir() as tempdir:
            config = ConfigUpdater(config_dir=tempdir, filename='active.yaml', log=self.log, deblock=self.deblock)
            config.paused = True
            config.update_config(forced=True)
            self.assertIn('config_hash', config.query([]))
            subsection = config.query(['skynet', 'services', 'copier'])
            self.assertIn('config_hash', subsection)
            self.assertEqual(subsection['name'], 'copier')
            self.assertEqual(subsection['path'], 'skynet.services.copier')

    @mock.patch('skycore.components.configupdater.download_config', download_config)
    @mock.patch('skycore.components.configupdater.send_report', send_report)
    def test_overrides(self):
        def _ensure_old(cfg):
            self.assertIn('config_hash', cfg.query([]))
            self.assertNotIn('new_special_field', cfg.query([]))
            subsection = cfg.query(['skynet', 'services', 'copier'])
            self.assertIn('config_hash', subsection)
            self.assertEqual(subsection['config']['port'], 6881)
            self.assertNotIn('new_data', subsection['config'])

        def _ensure_new(cfg):
            self.assertIn('config_hash', cfg.query([]))
            self.assertEqual(cfg.query([])['new_special_field'], 42)
            subsection = cfg.query(['skynet', 'services', 'copier'])
            self.assertIn('config_hash', subsection)
            self.assertEqual(subsection['config']['port'], 6882)
            self.assertEqual(subsection['config']['new_data'], 6883)

        with TempDir() as tempdir:
            config = ConfigUpdater(config_dir=tempdir, filename='active.yaml', log=self.log, deblock=self.deblock)
            config.paused = True
            config.update_config(forced=True)

            # check vanilla
            _ensure_old(config)

            overrides = Path(tempdir).join('overrides.d')
            overrides.ensure(dir=1)

            override1 = {'subsections': {'skynet': {'subsections': {'services': {'subsections': {
                'copier': {'config': {'port': 6882, 'new_data': 6883}}
            }}}}}}
            override2 = {'new_special_field': 42}
            yaml.dump(override1, overrides.join('copier.yaml').open('wb'))
            yaml.dump(override2, overrides.join('special.yaml').open('wb'))

            # update with overrides
            config.update_config(forced=True)
            _ensure_new(config)

            # from disk with overrides
            config2 = ConfigUpdater(config_dir=tempdir, filename='active.yaml', log=self.log, deblock=self.deblock)
            config2.paused = True
            _ensure_new(config2)

            # update from disk with overrides
            config2.update_config(forced=True)
            _ensure_new(config2)

            # remove overrides and check vanilla
            overrides.remove()

            config3 = ConfigUpdater(config_dir=tempdir, filename='active.yaml', log=self.log, deblock=self.deblock)
            config3.paused = True
            _ensure_old(config3)

            # check that not updated ones are still overriden
            _ensure_new(config)
            _ensure_new(config2)

            # update old ones and check for override removal
            config.update_config(forced=True)
            config2.update_config(forced=True)
            _ensure_old(config)
            _ensure_old(config2)

            # and just to be sure
            config3.update_config(forced=True)
            _ensure_old(config3)

    @mock.patch('skycore.components.configupdater.download_config', download_config)
    @mock.patch('skycore.components.configupdater.send_report', send_report)
    def test_subscriptions(self):
        with TempDir() as tempdir:
            config = ConfigUpdater(config_dir=tempdir, filename='active.yaml', log=self.log, deblock=self.deblock)
            config.paused = True
            config.update_config(forced=True)

            # subscribe
            self.log.debug('New mode subscribing [callback #0] ...')
            self.result = {}
            config.subscribe(self._test_callback0,
                             [['skynet', 'base'], ['skynet', 'services', 'copier']], config_only=True)
            expected_result = {0: 2}
            self.assertEqual(self.result, expected_result)
            config.unsubscribe(self._test_callback0)

            self.log.debug('Subscribing [callback #1] ...')
            self.result = {}
            config.subscribe(self._test_callback1,
                             [['skynet', 'base'], ['skynet', 'services', 'copier']])
            expected_result = {1: 1}
            self.assertEqual(self.result, expected_result)

            self.log.debug('Subscribing [callback #2] ...')
            self.result = {}
            config.subscribe(self._test_callback2,
                             [['skynet', 'base'], ['skynet', 'services', 'cqudp']])
            expected_result = {2: 1}
            self.assertEqual(self.result, expected_result)

            self.log.debug('Subscribing [callback #3] ...')
            self.result = {}
            config.subscribe(self._test_callback3,
                             [['skynet', 'services'], ['sys', 'cgroups']])
            expected_result = {3: 1}
            self.assertEqual(self.result, expected_result)

            self.log.debug('Subscribing for missing section [callback #4] ...')
            self.result = {}
            config.subscribe(self._test_callback4,
                             [['skynet', 'services', 'new_service']])
            # no notification for missing section
            expected_result = {}
            self.assertEqual(self.result, expected_result)

            # change config: skynet.base - expect 2 notifications
            self.log.debug('Updating skynet.base ...')
            self.result = {}
            expected_result = {1: 1, 2: 1}
            update_configs(['skynet', 'base'], 'dead0104f86e2a38cdd72ed93a62fb036dad31d5')
            config.update_config(forced=True)
            self.assertEqual(self.result, expected_result)

            # change config: add skynet.services.new_service - expect 2 notification
            self.log.debug('Updating skynet.services.new_service ...')
            self.result = {}
            expected_result = {3: 1, 4: 1}
            update_configs(['skynet', 'services', 'new_service'], 'dead0204f86e2a38cdd72ed93a62fb036dad31d5')
            config.update_config(forced=True)
            self.assertEqual(self.result, expected_result)

            # change config: skynet.services.cqudp - expect 2 notifications
            self.log.debug('Updating skynet.services.cqudp ...')
            self.result = {}
            expected_result = {2: 1, 3: 1}
            update_configs(['skynet', 'services', 'cqudp'], 'dead0304f86e2a38cdd72ed93a62fb036dad31d5')
            config.update_config(forced=True)
            self.assertEqual(self.result, expected_result)

            # change config: sys.cgroups - expect 1 notification
            self.log.debug('Updating sys.cgroups ...')
            self.result = {}
            expected_result = {3: 1}
            update_configs(['sys', 'cgroups'], 'dead0404f86e2a38cdd72ed93a62fb036dad31d5')
            config.update_config(forced=True)
            self.assertEqual(self.result, expected_result)

            # change config: skynet.services.copier and skynet.services.cqudp - expect 3 notification
            self.log.debug('Updating skynet.services.copier and services.cqudp ...')
            self.result = {}
            expected_result = {1: 1, 2: 1, 3: 1}
            update_configs(['skynet', 'services', 'copier'], 'dead0504f86e2a38cdd72ed93a62fb036dad31d5')
            update_configs(['skynet', 'services', 'cqudp'], 'dead0604f86e2a38cdd72ed93a62fb036dad31d5')
            config.update_config(forced=True)
            self.assertEqual(self.result, expected_result)

            # unsubscribe
            self.log.debug('Unsubscribing ...')
            config.unsubscribe(self._test_callback1)
            config.unsubscribe(self._test_callback2)
            config.unsubscribe(self._test_callback3)
            config.unsubscribe(self._test_callback4)

            # change config: skynet - expect 0 notification
            self.log.debug('Updating skynet ...')
            self.result = {}
            expected_result = {}
            update_configs(['skynet'], 'dead0504f86e2a38cdd72ed93a62fb036dad31d5')
            config.update_config(forced=True)
            self.assertEqual(self.result, expected_result)

    def _test_callback0(self, path, config):
        self.assertIn(path, [['skynet', 'base'], ['skynet', 'services', 'copier']])
        config.pop('__config_hash')
        # check config
        if path == ['skynet', 'base']:
            if config == expected_callback_configs[0]['config']['subsections']['skynet']['subsections']['base']['config']:
                val = self.result.get(0, 0)
                self.result[0] = val + 1
        elif path == ['skynet', 'services', 'copier']:
            if config == expected_callback_configs[0]['config']['subsections']['skynet']['subsections']['services']['subsections']['copier']['config']:
                val = self.result.get(0, 0)
                self.result[0] = val + 1

    def _test_callback1(self, path, config):
        self.assertEqual(path, None)
        self._process_config(1, config)

    def _test_callback2(self, path, config):
        self.assertEqual(path, None)
        self._process_config(2, config)

    def _test_callback3(self, path, config):
        self.assertEqual(path, None)
        self._process_config(3, config)

    def _test_callback4(self, path, config):
        self.assertEqual(path, None)
        self._process_config(4, config)

    def _process_config(self, callback_id, config):
        # check config
        if config == expected_callback_configs[callback_id - 1]['config']:
            val = self.result.get(callback_id, 0)
            self.result[callback_id] = val + 1
        else:
            self.log.critical('Callback #%d: config mismatched', callback_id)
            self.log.critical('Received: %s', str(config))
            self.log.critical('Expected: %s', expected_callback_configs[callback_id - 1]['config'])


if __name__ == '__main__':
    main()
