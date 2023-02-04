import copy
import unittest
import json
import random

import mock
import pymongo

from genisys.toiler import section, base, email


class OverlayConfigsTestCase(unittest.TestCase):
    def test(self):
        cfg = {
            "foo": {
                "bar": 1,
                "baz": 2,
                "fuuuu": {"la": "lala"},
            },
            "quux": 3,
            "bzz": {"some": {7: 8}},
        }
        overlay = {
            "new": 4,
            "foo": {
                "newnew": 5,
                "baz": False,
            },
            "quux": {"blah": [1]},
            "bzz": {"some": 9},
        }
        cfg_original = copy.deepcopy(cfg)
        overlay_original = copy.deepcopy(overlay)
        result = section._overlay_config(cfg, overlay)
        self.assertEquals(cfg, cfg_original)
        self.assertEquals(overlay, overlay_original)

        self.assertEquals(result, {
            "foo": {
                "bar": 1,
                "baz": False,
                "fuuuu": {"la": "lala"},
                "newnew": 5,
            },
            "new": 4,
            "quux": {"blah": [1]},
            "bzz": {"some": 9},
        })


def exhaust(test_case, gen, max_iterations):
    iterations = 0
    log = []
    try:
        while True:
            log.append(next(gen))
            iterations += 1
            test_case.assertLess(iterations, max_iterations)
    except StopIteration as exc:
        return exc.value, log


class _MergeTestCase(unittest.TestCase):
    def _res_cfg(self, *nums):
        res = {}
        for i in sorted(nums, reverse=True):
            for j in range(i + 1):
                res['i{}'.format(j)] = i
        return res

    def _gen_cfg(self, length):
        return [{'i{}'.format(j): i for j in range(i + 1)}
                for i in range(length)]


class MergeHostConfigsTestCase(_MergeTestCase):
    def test(self):
        hosts = [
            {"h1", "h2", "h3", "h4", "h5", "h6",     }, # 0
            {"h1",       "h3",       "h5",       "h7"}, # 1
            {"h1", "h2",       "h4", "h5",       "h7"}, # 2
            {"h1",       "h3",       "h5",       "h7"}, # 3
            {      "h2",       "h4",       "h6"      }, # 4
            {                  "h4",       "h6"      }, # 5
            {            "h3",                       }, # 6
            {            "h3",       "h5",           }, # 7
            {            "h3",       "h5",           }, # 8
        ]
        configs = self._gen_cfg(len(hosts))
        rule_names = ['r0', 'r1', 'r2', 'r3', 'r4', 'r5', 'r6', 'r7', 'r8']
        gen = section._merge_hosts_configs(hosts, configs, rule_names)
        (res_hosts, res_configs), _ = exhaust(self, gen, max_iterations=120)

        expected = [
            ('h1', (0, 1, 2, 3)),
            ('h2', (0, 2, 4)),
            ('h3', (0, 1, 3, 6, 7, 8)),
            ('h4', (0, 2, 4, 5)),
            ('h5', (0, 1, 2, 3, 7, 8)),
            ('h6', (0, 4, 5)),
            ('h7', (1, 2, 3)),
        ]
        for hostname, keys in expected:
            self.assertEquals(res_configs[res_hosts[hostname]],
                              {'config': self._res_cfg(*keys),
                               'matched_rules': [rule_names[idx] for idx in keys]})

    def test_all_hosts(self):
        hosts = [
            {            "h3"      }, # 0
            None,                     # 1
            {      "h2",           }, # 2
            None,                     # 3
            {      "h2", "h3"      }, # 4
            {"h1",                 }, # 5
            None,                     # 6
            None,                     # 7
            {"h1",       "h3"      }, # 8
            {"h1", "h2", "h3"      }, # 9
            None,                     # 10
            {                  "h4"}  # 11
        ]
        configs = self._gen_cfg(len(hosts))
        rule_names = ['r%d' % (i, ) for i in range(len(hosts))]
        gen = section._merge_hosts_configs(hosts, configs, rule_names)
        (res_hosts, res_configs), _ = exhaust(self, gen, max_iterations=800)

        self.assertEquals(res_configs[res_hosts['h1']], {
            'config': self._res_cfg(10, 9, 8, 7, 6, 5, 3, 1),
            'matched_rules': 'r1 r3 r5 r6 r7 r8 r9 r10'.split(),
        })
        self.assertEquals(res_configs[res_hosts['h2']], {
            'config': self._res_cfg(10, 9, 7, 6, 4, 3, 2, 1),
            'matched_rules': 'r1 r2 r3 r4 r6 r7 r9 r10'.split(),
        })
        self.assertEquals(res_configs[res_hosts['h3']], {
            'config': self._res_cfg(10, 9, 8, 7, 6, 4, 3, 1, 0),
            'matched_rules': 'r0 r1 r3 r4 r6 r7 r8 r9 r10'.split(),
        })
        self.assertEquals(res_configs[res_hosts['h4']], {
            'config': self._res_cfg(11, 10, 7, 6, 3, 1),
            'matched_rules': 'r1 r3 r6 r7 r10 r11'.split(),
        })
        self.assertEquals(res_configs[-1], {
            'config': self._res_cfg(10, 7, 6, 3, 1),
            'matched_rules': 'r1 r3 r6 r7 r10'.split(),
        })

    def test_over_64_rules(self):
        hosts = [{'h%2d' % i} for i in range(65)]
        rule_names = ['r%2d' % i for i in range(len(hosts))]
        configs = self._gen_cfg(len(hosts))
        gen = section._merge_hosts_configs(hosts, configs, rule_names)
        (res_hosts, res_configs), _ = exhaust(self, gen, max_iterations=800)
        self.assertEquals(res_hosts,
                          base._deserialize(base._serialize(res_hosts)))
        self.assertEquals(res_configs,
                          base._deserialize(base._serialize(res_configs)))


class MergeHostResourcesTestCase(_MergeTestCase):
    def test(self):
        hosts = [
            {      "h2",           }, # 0
            {"h1",       "h3"      }, # 1
            None,                     # 2
            {                  "h4"}, # 3
            None,                     # 4
            {"h1",                 }  # 5
        ]
        configs = self._gen_cfg(len(hosts))
        rule_names = 'r0 r1 r2 r3 r4 r5'.split()
        gen = section._merge_hosts_resources(hosts, configs, rule_names)
        (res_hosts, res_configs), _ = exhaust(self, gen, max_iterations=4)

        self.assertEquals(res_hosts, {
            'h1': 1,
            'h2': 0,
            'h3': 1,
            'h4': -1,
        })
        self.assertEquals(res_configs, {
            -1: {'config': {'i0': 2, 'i1': 2, 'i2': 2}, 'matched_rules': ['r2']},
            0: {'config': {'i0': 0}, 'matched_rules': ['r0']},
            1: {'config': {'i0': 1, 'i1': 1}, 'matched_rules': ['r1']}
        })


class SectionVTypeProcessorTestCase(unittest.TestCase):
    testdb_host = 'localhost'
    testdb_name = 'genisys_unittest'

    def clear_db(self):
        for collection in self.database.collection_names(False):
            self.database[collection].drop()

    def setUp(self):
        super(SectionVTypeProcessorTestCase, self).setUp()
        client = pymongo.MongoClient(self.testdb_host)
        self.database = client[self.testdb_name]
        self.clear_db()
        self.mock_email = mock.patch.object(email, 'email').start()
        def fake_config_hash(config):
            return 'config_hash({})'.format(json.dumps(config, sort_keys=1))
        self.mock_config_hash = \
                mock.patch('genisys.toiler.section._get_config_hash').start()
        self.mock_config_hash.side_effect = fake_config_hash

    def tearDown(self):
        super(SectionVTypeProcessorTestCase, self).tearDown()
        self.clear_db()
        self.mock_email.stop()
        self.mock_config_hash.stop()

    def test_missing_selectors(self):
        self.database.volatile.insert_many([
            {'value': base._serialize(None),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'selector1',
             'raw_key': 'selector1',
             'mtime': 100000,
             'utime': 120000,
             'last_status': 'same'},
        ])

        secrec = {
            'source': {
                'rules': [{'selector_keys': ['selector1'], 'rule_name': 'r1'},
                          {'selector_keys': ['selector2'], 'rule_name': 'r2'}],
                'stype': 'yaml',
            },
            'key': 'secrec',
            'raw_key': 'sec1.sec11',
            'ctime': 90000,
            'mtime': 90000,
            'utime': 90000,
            'meta': {'old': 'meta', 'owners': ['o1', 'o2']},
        }
        proc = section.section_vtype_processor(self.database, secrec, False)
        res, log = exhaust(self, proc, 10)
        self.assertEquals(res, (None, {
            'old': 'meta', 'notified_on_broken_selectors': ['selector2'],
            'owners': ['o1', 'o2'],
        }))
        self.assertEquals(self.mock_email.call_count, 1)
        self.maxDiff = None
        self.assertEquals(self.mock_email.call_args[1], dict(
            context={
                'broken_selectors': [{'key': 'selector2',
                                      'status': 'missing',
                                      'rule_names': {'r2'},
                                      'proclog': [],
                                      'selector': 'n/a'}],
                'secrec': secrec},
            send_to=['o1', 'o2'],
            subject_template='broken_selectors_subj.txt',
            body_template='broken_selectors.txt'
        ))

    def test_broken_selectors(self):
        self.database.volatile.insert_many([
            {'value': base._serialize(None),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'h(s1)',
             'raw_key': 's1',
             'mtime': 100000,
             'utime': 120000,
             'ttime': 119999,
             'last_status': 'same'},
            {'value': base._serialize(None),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'h(s2)',
             'raw_key': 'sr2',
             'mtime': 100000,
             'utime': 120000,
             'ttime': 134567,
             'pcount': 12,
             'ecount': 13,
             'last_status': 'error',
             'proclog': [(123, 's2 proclog')]},
            {'value': base._serialize(None),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'h(s4)',
             'raw_key': 'sr4',
             'mtime': 100000,
             'utime': 120000,
             'ttime': 119999,
             'last_status': 'error',
             'proclog': [(123, 's4 proclog')]},

            {'vtype': 'section',
             'key': base.volatile_key_hash(''),
             'meta': {'owners': ['o1']},
             'value': None,
             'source': base._serialize(None)},
            {'vtype': 'section',
             'key': base.volatile_key_hash('sec1'),
             'meta': {'owners': ['o5']},
             'value': None,
             'source': base._serialize(None)},

            {'vtype': 'section',
             'key': 'sec1.sec11'},
        ])

        secrec = {
            'source': {
                'rules': [{'selector_keys': ['h(s1)'], 'rule_name': 'r1'},
                          {'selector_keys': ['h(s2)'], 'rule_name': 'r3'},
                          {'selector_keys': ['h(s2)'], 'rule_name': 'r2'},
                          {'selector_keys': ['h(s3)'], 'rule_name': 'r4'},
                          {'selector_keys': ['h(s4)'], 'rule_name': 'r5'},
                          {'selector_keys': [], 'rule_name': 'default'}],
                'stype': 'yaml',
            },
            'key': 'sec1.sec11',
            'raw_key': 'sec1.sec11',
            'ctime': 90000,
            'mtime': 90000,
            'utime': 90000,
            'meta': {'owners': ['o1', 'o2', 'o5'], 'revision': 124,
                     'notified_on_broken_selectors': ['h(s3)', 'h(s4)']},
        }
        proc = section.section_vtype_processor(self.database, secrec, False)
        res, log = exhaust(self, proc, 20)
        self.assertEquals(res, (None, {
            'notified_on_broken_selectors': ['h(s2)', 'h(s3)', 'h(s4)'],
            'owners': ['o1', 'o2', 'o5'],
            'revision': 124
        }))
        self.assertEquals(self.mock_email.call_count, 1)
        self.maxDiff = None
        self.assertEquals(self.mock_email.call_args[1], dict(
            context={
                'broken_selectors': [{'key': 'h(s2)',
                                      'selector': 'sr2',
                                      'rule_names': {'r2', 'r3'},
                                      'proclog': [[123, 's2 proclog']],
                                      'status': 'failed',
                                      'pcount': 12,
                                      'fail_duration': 14567}],
                'secrec': secrec},
            send_to=['o1', 'o2', 'o5'],
            subject_template='broken_selectors_subj.txt',
            body_template='broken_selectors.txt'
        ))

        secrec['meta']['notified_on_broken_selectors'] = ['h(s2)', 'h(s3)',
                                                          'h(s4)']
        self.mock_email.reset_mock()
        self.database.volatile.update_one(
            {'vtype': 'selector', 'key': 'h(s2)'},
            {'$set': {'utime': 80000, 'last_status': 'same'}}
        )
        proc = section.section_vtype_processor(self.database, secrec, False)
        res, log = exhaust(self, proc, 10)
        self.mock_email.assert_not_called()
        self.assertEquals(secrec['meta']['notified_on_broken_selectors'],
                          ['h(s3)', 'h(s4)'])

    def test_broken_selectors_has_never_been_calculated(self):
        self.database.volatile.insert_many([
            {'value': base._serialize(None),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'h(s1)',
             'raw_key': 's1',
             'mtime': 100000,
             'utime': 120000,
             'ttime': 119999,
             'last_status': 'same'},
            {'value': base._serialize(None),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'h(s2)',
             'raw_key': 'sr2',
             'mtime': None,
             'utime': None,
             'ctime': 4567,
             'ttime': 134567,
             'pcount': 12,
             'ecount': 13,
             'last_status': 'error',
             'proclog': [(123, 's2 proclog')]},
            {'value': base._serialize(None),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'h(s4)',
             'raw_key': 'sr4',
             'mtime': None,
             'utime': None,
             'ctime': 119998,
             'ttime': 119999,
             'last_status': 'error',
             'proclog': [(123, 's4 proclog')]},

            {'vtype': 'section',
             'key': base.volatile_key_hash(''),
             'meta': {'owners': ['o1']},
             'value': None,
             'source': base._serialize(None)},
            {'vtype': 'section',
             'key': base.volatile_key_hash('sec1'),
             'meta': {'owners': ['o5']},
             'value': None,
             'source': base._serialize(None)},

            {'vtype': 'section',
             'key': 'sec1.sec11'},
        ])

        secrec = {
            'source': {
                'rules': [{'selector_keys': ['h(s1)'], 'rule_name': 'r1'},
                          {'selector_keys': ['h(s2)'], 'rule_name': 'r3'},
                          {'selector_keys': ['h(s2)'], 'rule_name': 'r2'},
                          {'selector_keys': ['h(s3)'], 'rule_name': 'r4'},
                          {'selector_keys': ['h(s4)'], 'rule_name': 'r5'},
                          {'selector_keys': [], 'rule_name': 'default'}],
                'stype': 'yaml',
            },
            'key': 'sec1.sec11',
            'raw_key': 'sec1.sec11',
            'ctime': 90000,
            'mtime': 90000,
            'utime': 90000,
            'meta': {'owners': ['o1', 'o2', 'o5'], 'revision': 124,
                     'notified_on_broken_selectors': ['h(s3)', 'h(s4)']},
        }
        proc = section.section_vtype_processor(self.database, secrec, False)
        res, log = exhaust(self, proc, 20)
        self.assertEquals(res, (None, {
            'notified_on_broken_selectors': ['h(s2)', 'h(s3)', 'h(s4)'],
            'owners': ['o1', 'o2', 'o5'],
            'revision': 124
        }))
        self.assertEquals(self.mock_email.call_count, 1)
        self.maxDiff = None
        self.assertEquals(self.mock_email.call_args[1], dict(
            context={
                'broken_selectors': [{'key': 'h(s2)',
                                      'selector': 'sr2',
                                      'rule_names': {'r2', 'r3'},
                                      'proclog': [[123, 's2 proclog']],
                                      'status': 'failed',
                                      'pcount': 12,
                                      'fail_duration': 130000}],
                'secrec': secrec},
            send_to=['o1', 'o2', 'o5'],
            subject_template='broken_selectors_subj.txt',
            body_template='broken_selectors.txt'
        ))

        secrec['meta']['notified_on_broken_selectors'] = ['h(s2)', 'h(s3)',
                                                          'h(s4)']
        self.mock_email.reset_mock()
        self.database.volatile.update_one(
            {'vtype': 'selector', 'key': 'h(s2)'},
            {'$set': {'utime': 80000, 'last_status': 'same'}}
        )
        proc = section.section_vtype_processor(self.database, secrec, False)
        res, log = exhaust(self, proc, 10)
        self.mock_email.assert_not_called()
        self.assertEquals(secrec['meta']['notified_on_broken_selectors'],
                          ['h(s3)', 'h(s4)'])

    def test_broken_selectors_not_yet_alarming(self):
        self.database.volatile.insert_many([
            {'value': base._serialize(None),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'h(s1)',
             'raw_key': 's1',
             'mtime': 100000,
             'utime': 120000,
             'ttime': 119999,
             'last_status': 'same'},
            {'value': base._serialize(None),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'h(s2)',
             'raw_key': 'sr2',
             'mtime': 100000,
             'utime': 120000,
             'ttime': 120567,
             'pcount': 100,
             'last_status': 'error',
             'proclog': [(123, 's2 proclog')]},
            {'value': base._serialize(None),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'h(s4)',
             'raw_key': 'sr4',
             'mtime': 100000,
             'utime': 120000,
             'ttime': 119999,
             'last_status': 'error',
             'proclog': [(123, 's4 proclog')]},

            {'vtype': 'section',
             'key': base.volatile_key_hash(''),
             'meta': {'owners': ['o1']},
             'value': None,
             'source': base._serialize(None)},
            {'vtype': 'section',
             'key': base.volatile_key_hash('sec1'),
             'meta': {'owners': ['o5']},
             'value': None,
             'source': base._serialize(None)},

            {'vtype': 'section',
             'key': 'sec1.sec11'},
        ])

        secrec = {
            'source': {
                'rules': [{'selector_keys': ['h(s1)'], 'rule_name': 'r1'},
                          {'selector_keys': ['h(s2)'], 'rule_name': 'r3'},
                          {'selector_keys': ['h(s2)'], 'rule_name': 'r2'},
                          {'selector_keys': ['h(s3)'], 'rule_name': 'r4'},
                          {'selector_keys': ['h(s4)'], 'rule_name': 'r5'},
                          {'selector_keys': [], 'rule_name': 'default'}],
                'stype': 'yaml',
            },
            'key': 'sec1.sec11',
            'raw_key': 'sec1.sec11',
            'ctime': 90000,
            'mtime': 90000,
            'utime': 90000,
            'meta': {'owners': ['o1', 'o2', 'o5'], 'revision': 124,
                     'notified_on_broken_selectors': ['h(s3)', 'h(s4)']},
        }
        proc = section.section_vtype_processor(self.database, secrec, False)
        res, log = exhaust(self, proc, 20)
        self.assertEquals(res, (None, {
            'notified_on_broken_selectors': ['h(s3)', 'h(s4)'],
            'owners': ['o1', 'o2', 'o5'],
            'revision': 124
        }))
        self.mock_email.assert_not_called()

        self.database.volatile.update_one(
            {'vtype': 'selector', 'key': 'h(s2)'},
            {'$set': {'utime': 80000, 'last_status': 'same'}}
        )
        proc = section.section_vtype_processor(self.database, secrec, False)
        res, log = exhaust(self, proc, 10)
        self.mock_email.assert_not_called()
        self.assertEquals(secrec['meta']['notified_on_broken_selectors'],
                          ['h(s3)', 'h(s4)'])

    def test_broken_selectors_not_yet_alarming2(self):
        self.database.volatile.insert_many([
            {'value': base._serialize(None),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'h(s1)',
             'raw_key': 's1',
             'mtime': 100000,
             'utime': 120000,
             'ttime': 119999,
             'last_status': 'same'},
            {'value': base._serialize(None),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'h(s2)',
             'raw_key': 'sr2',
             'mtime': 100000,
             'utime': 120000,
             'ttime': 122567,
             'pcount': 5,
             'last_status': 'error',
             'proclog': [(123, 's2 proclog')]},
            {'value': base._serialize(None),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'h(s4)',
             'raw_key': 'sr4',
             'mtime': 100000,
             'utime': 120000,
             'ttime': 119999,
             'last_status': 'error',
             'proclog': [(123, 's4 proclog')]},

            {'vtype': 'section',
             'key': base.volatile_key_hash(''),
             'meta': {'owners': ['o1']},
             'value': None,
             'source': base._serialize(None)},
            {'vtype': 'section',
             'key': base.volatile_key_hash('sec1'),
             'meta': {'owners': ['o5']},
             'value': None,
             'source': base._serialize(None)},

            {'vtype': 'section',
             'key': 'sec1.sec11'},
        ])

        secrec = {
            'source': {
                'rules': [{'selector_keys': ['h(s1)'], 'rule_name': 'r1'},
                          {'selector_keys': ['h(s2)'], 'rule_name': 'r3'},
                          {'selector_keys': ['h(s2)'], 'rule_name': 'r2'},
                          {'selector_keys': ['h(s3)'], 'rule_name': 'r4'},
                          {'selector_keys': ['h(s4)'], 'rule_name': 'r5'},
                          {'selector_keys': [], 'rule_name': 'default'}],
                'stype': 'yaml',
            },
            'key': 'sec1.sec11',
            'raw_key': 'sec1.sec11',
            'ctime': 90000,
            'mtime': 90000,
            'utime': 90000,
            'meta': {'owners': ['o1', 'o2', 'o5'], 'revision': 124,
                     'notified_on_broken_selectors': ['h(s3)', 'h(s4)']},
        }
        proc = section.section_vtype_processor(self.database, secrec, False)
        res, log = exhaust(self, proc, 20)
        self.assertEquals(res, (None, {
            'notified_on_broken_selectors': ['h(s3)', 'h(s4)'],
            'owners': ['o1', 'o2', 'o5'],
            'revision': 124
        }))
        self.mock_email.assert_not_called()

        self.database.volatile.update_one(
            {'vtype': 'selector', 'key': 'h(s2)'},
            {'$set': {'utime': 80000, 'last_status': 'same'}}
        )
        proc = section.section_vtype_processor(self.database, secrec, False)
        res, log = exhaust(self, proc, 10)
        self.mock_email.assert_not_called()
        self.assertEquals(secrec['meta']['notified_on_broken_selectors'],
                          ['h(s3)', 'h(s4)'])

    def test_outdated_selector(self):
        self.database.volatile.insert_many([
            {'value': base._serialize(None),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'selector1',
             'mtime': 100000,
             'utime': 120000,
             'last_status': 'same'},
        ])

        secrec = {
            'source': {'rules': [{'selector_keys': ['selector1']}],
                       'stype': 'yaml'},
            'ctime': 130000,
            'mtime': 130000,
            'utime': 130000,
            'meta': {'old': 'meta', 'reresolve_selectors': True},
            'key': 'secrec',
        }
        proc = section.section_vtype_processor(self.database, secrec, False)
        res, log = exhaust(self, proc, 10)
        self.assertEquals(res, (None, {'old': 'meta',
                                       'reresolve_selectors': True}))

    def test_outdated_selector_not_reresolve(self):
        self.database.volatile.insert_many([
            {'value': base._serialize(['h1', 'h2']),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'selector1',
             'mtime': 100000,
             'utime': 120000,
             'last_status': 'same'},
        ])

        secrec = {
            'source': {'rules': [{'selector_keys': ['selector1'],
                                  'rule_name': 'r1',
                                  'config': {'cfg': 100500}}],
                       'stype': 'yaml'},
            'ctime': 130000,
            'mtime': 130000,
            'utime': 130000,
            'meta': {'old': 'meta', 'reresolve_selectors': False},
            'key': 'secrec',
        }
        proc = section.section_vtype_processor(self.database, secrec, False)
        res, log = exhaust(self, proc, 10)
        self.assertEquals(res, (
            {'configs': {1: {'config': {'cfg': 100500},
                             'config_hash': 'config_hash({"cfg": 100500})',
                             'matched_rules': ['r1']}},
             'hosts': {'h1': 1, 'h2': 1}},
            {'old': 'meta', 'reresolve_selectors': False}
        ))

    def test_empty_rules_list(self):
        secrec = {'source': {'rules': []},
                  'meta': {'old': 'meta'},
                  'key': 'k',
                  'vtype': 'section'}
        self.database.volatile.insert_many([secrec])
        proc = section.section_vtype_processor(self.database, secrec, False)
        with mock.patch('genisys.toiler.base._get_ts') as ts:
            ts.return_value = 1444000
            res, log = exhaust(self, proc, 10)
        self.assertEquals(res, ({'hosts': {}, 'configs': {}}, {'old': 'meta'}))
        self.assertEquals(self.database.volatile.find_one()['atime'], 1444000)

    def test_not_all_selectors_have_data(self):
        self.database.volatile.insert_many([
            {'value': base._serialize(None),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'selector1',
             'mtime': 144000001,
             'utime': 144000001,
             'last_status': 'same'},
            {'value': base._serialize(None),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'selector2',
             'mtime': None,
             'utime': None,
             'last_status': 'new'},
        ])

        secrec = {
            'source': {
                'rules': [
                    {'selector_keys': ['selector1']},
                    {'selector_keys': ['selector2']},
                ],
                'stype': 'yaml',
            },
            'ctime': 144000000,
            'mtime': 144000000,
            'utime': 144000000,
            'meta': {'old': 'meta'},
            'key': 'secrec',
        }
        proc = section.section_vtype_processor(self.database, secrec, False)
        res, log = exhaust(self, proc, 10)
        self.assertEquals(res, (None, {'old': 'meta'}))

    def test_not_all_resources_have_data(self):
        self.database.volatile.insert_many([
            {'value': base._serialize(None),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'selector1',
             'mtime': 144000001,
             'utime': 144000001,
             'last_status': 'same'},
            {'value': base._serialize(None),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'selector2',
             'mtime': 144000002,
             'utime': 144000002,
             'last_status': 'same'},

            {'value': base._serialize(None),
             'source': base._serialize(None),
             'vtype': 'sandbox_resource',
             'key': 'resource1',
             'mtime': 144000002,
             'utime': 144000002},
            {'value': None,
             'source': base._serialize(None),
             'vtype': 'sandbox_resource',
             'key': 'resource2',
             'mtime': None,
             'utime': 144000003},

            {'value': None,
             'source': base._serialize(None),
             'vtype': 'sandbox_releases',
             'key': 'SKYBIN_releases',
             'mtime': 144000004,
             'utime': 144000004},
        ])

        secrec = {
            'source': {
                'rules': [
                    {'selector_keys': ['selector1'],
                     'resource_key': 'resource1'},
                    {'selector_keys': ['selector2'],
                     'resource_key': 'resource2'},
                ],
                'stype': 'sandbox_resource',
                'stype_options': {'resource_type': 'SKYBIN'},
                'sandbox_releases_key': 'SKYBIN_releases',
            },
            'ctime': 144000000,
            'mtime': 144000000,
            'utime': 144000000,
            'meta': {'old': 'meta'},
            'key': 'secrec',
        }
        proc = section.section_vtype_processor(self.database, secrec, False)
        res, log = exhaust(self, proc, 10)
        self.assertEquals(res, (None, {'old': 'meta'}))

    def test_some_resources_are_missing(self):
        self.database.volatile.insert_many([
            {'value': base._serialize(None),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'selector1',
             'mtime': 144000001,
             'utime': 144000001,
             'last_status': 'same'},
        ])

        secrec = {
            'source': {
                'rules': [
                    {'selector_keys': ['selector1'],
                     'resource_key': 'resource1'},
                ],
                'stype': 'sandbox_resource',
                'stype_options': {'resource_type': 'SKYBIN'},
                'sandbox_releases_key': 'SKYBIN_releases',
            },
            'ctime': 144000000,
            'mtime': 144000000,
            'utime': 144000000,
            'meta': {'old': 'meta'},
            'key': 'secrec',
        }
        proc = section.section_vtype_processor(self.database, secrec, False)
        res, log = exhaust(self, proc, 10)
        self.assertEquals(res, (None, {'old': 'meta'}))

    def test_no_selectors_updated(self):
        self.database.volatile.insert_many([
            {'value': base._serialize(None),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'selector1',
             'mtime': 144000003,
             'utime': 144000003,
             'last_status': 'same'},
            {'value': base._serialize(None),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'selector2',
             'mtime': 144000004,
             'utime': 144000004,
             'last_status': 'same'},
        ])
        secrec = {
            'source': {
                'rules': [
                    {'selector_keys': ['selector1']},
                ],
                'stype': 'yaml',
            },
            'ctime': 144000001,
            'mtime': 144000000,
            'utime': 144000006,
            'value': 'oldvalue',
            'meta': {'old': 'meta'},
            'key': 'secrec',
        }
        proc = section.section_vtype_processor(self.database, secrec, False)
        res, log = exhaust(self, proc, 10)
        self.assertEquals(res, ('oldvalue', {'old': 'meta'}))

    def test_no_selectors_updated_forced_update(self):
        self.database.volatile.insert_many([
            {'value': base._serialize(['h1', 'h2']),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'selector1',
             'mtime': 144000003,
             'utime': 144000003,
             'last_status': 'same'},
        ])
        secrec = {
            'source': {
                'rules': [
                    {'selector_keys': ['selector1'], 'rule_name': 'r1',
                     'config': {'c': 'fg'}},
                ],
                'stype': 'yaml',
            },
            'ctime': 144000001,
            'mtime': 144000002,
            'utime': 144000006,
            'value': 'oldvalue',
            'meta': {'old': 'meta'},
            'key': 'secrec',
        }
        proc = section.section_vtype_processor(self.database, secrec, True)
        res, log = exhaust(self, proc, 10)
        self.assertEquals(res, (
            {'configs': {1: {'config': {'c': 'fg'},
                             'config_hash': 'config_hash({"c": "fg"})',
                             'matched_rules': ['r1']}},
             'hosts': {'h1': 1, 'h2': 1}},
            {'old': 'meta'}
        ))

    def test_no_resources_updated(self):
        self.database.volatile.insert_many([
            {'value': base._serialize(None),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'selector1',
             'mtime': 144000003,
             'utime': 144000003,
             'last_status': 'same'},
            {'value': base._serialize(None),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'selector2',
             'mtime': 144000003,
             'utime': 144000003,
             'last_status': 'same'},

            {'value': base._serialize(None),
             'source': base._serialize(None),
             'vtype': 'sandbox_resource',
             'key': 'resource1',
             'mtime': 144000002,
             'utime': 144000002},
            {'value': None,
             'source': base._serialize(None),
             'vtype': 'sandbox_resource',
             'key': 'resource2',
             'mtime': 144000005,
             'utime': 144000005},

            {'value': None,
             'source': base._serialize([]),
             'vtype': 'sandbox_releases',
             'key': 'SKYBIN_releases',
             'mtime': 144000004,
             'utime': 144000004},
        ])
        secrec = {
            'source': {
                'rules': [
                    {'selector_keys': ['selector1'],
                     'resource_key': 'resource1'},
                    {'selector_keys': ['selector2'],
                     'resource_key': 'resource2'},
                ],
                'stype': 'sandbox_resource',
                'stype_options': {'resource_type': 'SKYBIN'},
                'sandbox_releases_key': 'SKYBIN_releases',
            },
            'key': 'secrec',
            'ctime': 144000001,
            'mtime': 144000002,
            'utime': 144000009,
            'value': 'oldvalue',
            'meta': {'old': 'meta'},
        }
        proc = section.section_vtype_processor(self.database, secrec, False)
        res, log = exhaust(self, proc, 10)
        self.assertEquals(res, ('oldvalue', {'old': 'meta'}))

    def test_compile_sandbox_resource(self):
        self.database.volatile.insert_many([
            {'value': base._serialize(['h1', 'h2']),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'selector1',
             'mtime': 144000010,
             'utime': 144000020,
             'last_status': 'same'},
            {'value': base._serialize(['h2', 'h3']),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'selector2',
             'mtime': 144000003,
             'utime': 144000010,
             'last_status': 'same'},

            {'value': base._serialize({'resourze': 1}),
             'source': base._serialize(None),
             'vtype': 'sandbox_resource',
             'key': 'resource1',
             'mtime': 144000002,
             'utime': 144000002},
            {'value': base._serialize({'resourze': 2}),
             'source': base._serialize(None),
             'vtype': 'sandbox_resource',
             'key': 'resource2',
             'mtime': 144000010,
             'utime': 144000012},

            {'vtype': 'section',
             'key': 'secrec'},
        ])

        secrec = {
            'source': {
                'rules': [
                    {'selector_keys': ['selector1'],
                     'rule_name': 'r1',
                     'resource_key': 'resource1'},
                    {'selector_keys': ['selector2'],
                     'rule_name': 'r2',
                     'resource_key': 'resource2'},
                ],
                'stype': 'sandbox_resource',
                'stype_options': {'resource_type': 'SKYBIN'},
                'sandbox_releases_key': 'SKYBIN_releases',
            },
            'ctime': 144000007,
            'mtime': 144000009,
            'utime': 144000009,
            'meta': {'old': 'meta'},
            'key': 'secrec'
        }
        proc = section.section_vtype_processor(self.database, secrec, False)
        res, log = exhaust(self, proc, 10)
        self.assertEquals(res, (
            {'configs': {0: {'config': {'resourze': 1},
                             'config_hash': 'config_hash({"resourze": 1})',
                             'matched_rules': ['r1']},
                         1: {'config': {'resourze': 2},
                             'config_hash': 'config_hash({"resourze": 2})',
                             'matched_rules': ['r2']}},
             'hosts': {'h1': 0, 'h2': 0, 'h3': 1}},
            {'old': 'meta'}
        ))

    def test_compile_yaml(self):
        self.database.volatile.insert_many([
            {'value': base._serialize(['h1', 'h2']),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'selector1',
             'mtime': 144000010,
             'utime': 144000020,
             'last_status': 'same'},
            {'value': base._serialize(['h2', 'h3']),
             'source': base._serialize(None),
             'vtype': 'selector',
             'key': 'selector2',
             'mtime': 144000003,
             'utime': 144000010,
             'last_status': 'same'},

            {'vtype': 'section',
             'key': 'secrec'},
        ])

        secrec = {
            'source': {
                'rules': [
                    {'selector_keys': ['selector1'],
                     'rule_name': 'r1',
                     'config': {'v1': 1}},
                    {'selector_keys': ['selector2'],
                     'rule_name': 'r2',
                     'config': {'v2': 1}},
                ],
                'stype': 'yaml',
            },
            'ctime': 144000007,
            'mtime': None,
            'utime': None,
            'meta': {'old': 'meta'},
            'key': 'secrec',
        }
        proc = section.section_vtype_processor(self.database, secrec, False)
        (configs, meta), log = exhaust(self, proc, 10)
        self.assertEquals(meta, {'old': 'meta'})
        self.maxDiff = None
        self.assertEquals(configs, {
            'configs': {1: {'config': {'v1': 1},
                            'config_hash': 'config_hash({"v1": 1})',
                            'matched_rules': ['r1']},
                        2: {'config': {'v2': 1},
                            'config_hash': 'config_hash({"v2": 1})',
                            'matched_rules': ['r2']},
                        3: {'config': {'v1': 1, 'v2': 1},
                            'config_hash': 'config_hash({"v1": 1, "v2": 1})',
                            'matched_rules': ['r1', 'r2']}},
            'hosts': {'h1': 1, 'h2': 3, 'h3': 2}
        })


class ConfigHashTestCase(unittest.TestCase):
    def test_stability(self):
        nums = list(set(''.join(random.sample('012345', 4))
                        for _ in range(1000)))
        dct0 = dict(zip(nums, nums))
        hash0 = section._get_config_hash(dct0)
        for _ in range(100):
            random.shuffle(nums)
            dct1 = dict(zip(nums, nums))
            self.assertEquals(dct0, dct1)
            hash1 = section._get_config_hash(dct1)
            self.assertEquals(hash0, hash1)


class NotifyOnBrokenSelectorsTestCase(unittest.TestCase):
    testdb_host = 'localhost'
    testdb_name = 'genisys_unittest'
    CONFIG = dict(
        SMTP_SERVER='smtpsrv',
        SMTP_PORT=12325,
        EMAIL_LINKS_BASE='http://127.0.0.1:5001',
        EMAIL_ADDRESS_FROM_ALIAS='genisys-tests',
        EMAIL_SUBJECT_PREFIX='[genisys-tests] ',
        EMAIL_ADDRESS_FROM_ADDRESS='unittests@genisys',
        EMAIL_MAX_RECIPIENTS=10,
    )

    def setUp(self):
        super(NotifyOnBrokenSelectorsTestCase, self).setUp()
        self.cfgmock = mock.patch('genisys.toiler.email.config')
        m = self.cfgmock.start()
        for key, value in self.CONFIG.items():
            setattr(m, key, value)
        client = pymongo.MongoClient(self.testdb_host)
        self.database = client[self.testdb_name]
        self.clear_db()
        self.database.volatile.insert_many([
            {'vtype': 'section',
             'key': base.volatile_key_hash(''),
             'source': base._serialize(None),
             'meta': {'owners': ['user1']}},
            {'vtype': 'section',
             'key': base.volatile_key_hash('sec1'),
             'source': base._serialize(None),
             'meta': {'owners': ['user3']}},
            {'vtype': 'section',
             'key': base.volatile_key_hash('sec1.sec11'),
             'source': base._serialize(None),
             'meta': {'owners': []}},
        ])
        self.broken_selectors = [
            {'rule_names': set(['rule1', 'rule20']),
             'key': 'selkey1',
             'status': 'failed',
             'selector': 'selkeyraw1',
             'proclog': [(123, 'proclog1'), (123.45, 'proclog2')],
             'fail_duration': 35 * 60,
             'pcount': 21},
            {'rule_names': set(['rule3']),
             'key': 'selkey2',
             'status': 'missing',
             'selector': 'n/a',
             'proclog': [],
             },
        ]

    def tearDown(self):
        self.cfgmock.stop()
        super(NotifyOnBrokenSelectorsTestCase, self).tearDown()
        self.clear_db()

    def clear_db(self):
        for collection in self.database.collection_names(False):
            self.database[collection].drop()

    def test(self):
        secrec = {
            'meta': {'owners': ['user9'], 'revision': 12},
            'raw_key': 'sec1.sec11.sec123',
        }
        notify = section._notify_on_broken_selectors(self.database, secrec,
                                                     self.broken_selectors)
        with mock.patch('smtplib.SMTP') as mock_smtp:
            res, log = exhaust(self, notify, 10)
        self.assertIs(res, None)
        send_calls = mock_smtp('smtpsrv', 12325).__enter__().send_message
        self.assertEquals(send_calls.call_count, 1)
        ((msg1, ), _), = send_calls.call_args_list
        self.assertEquals(msg1._headers[:4], [
            ('From', 'genisys-tests <"unittests@genisys">'),
            ('To', 'user1@yandex-team.ru, user3@yandex-team.ru, user9@yandex-team.ru'),
            ('Subject', '[genisys-tests] Some selectors are broken in section sec1.sec11.sec123'),
            ('Content-Type', 'text/plain; charset="utf-8"')
        ])
        self.maxDiff = None
        self.assertEquals(msg1.get_content(), """\
Broken selectors appeared in section "sec1.sec11.sec123"
  http://127.0.0.1:5001/sections/sec1.sec11.sec123?revision=12

Rule: "rule1"
  http://127.0.0.1:5001/rules/sec1.sec11.sec123/rule1?revision=12
Rule: "rule20"
  http://127.0.0.1:5001/rules/sec1.sec11.sec123/rule20?revision=12

Selector:
  selkeyraw1

We've tried to resolve it 21 times in 35 minutes, but got no luck.
Processing log of the last attempt goes below:
  proclog1
  proclog2


Rule: "rule3"
  http://127.0.0.1:5001/rules/sec1.sec11.sec123/rule3?revision=12

Selector:
  n/a

Selector is missing from "volatile" collection.


You have received this email because you are the owner of the section.

--
Brought to you by genisys
This is an automatic notification, do not reply.
""")

    def test_failed_group_resolving(self):
        class Response500(object):
            def raise_for_status(self):
                raise Exception(500)

        secrec = {
            'meta': {'owners': ['group:g1', 'group:g2'], 'revision': 12},
            'raw_key': 'sec1.sec11.sec123',
        }
        notify = section._notify_on_broken_selectors(self.database, secrec,
                                                     self.broken_selectors)
        with mock.patch('smtplib.SMTP'):
            with mock.patch('requests.get') as mock_get:
                mock_get.return_value = Response500()
                res, log = exhaust(self, notify, 10)
