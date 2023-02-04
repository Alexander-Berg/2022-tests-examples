import unittest

import mock

from lacmus2.redis import Lacmus2RedisStorage


class BaseRedisTestCase(unittest.TestCase):
    REDIS_CFG = dict(
        USE_SENTINEL=False,
        HOST='127.0.0.1',
        PORT=6379,
        SOCKET_TIMEOUT=10,
        RETRY_ON_TIMEOUT=True,
        DBNUM=7,
    )

    def setUp(self):
        super(BaseRedisTestCase, self).setUp()
        self.storage = Lacmus2RedisStorage(self.REDIS_CFG)
        self.redis = self.storage.redis
        self.redis.flushdb()


class HostReportTestCase(BaseRedisTestCase):
    def test(self):
        self.storage.process_hostreport(
            'h1.a', 101, key='k1', value='v1.1'
        )

        self.assertEquals(self.redis.get('hk2v\0h1.a\0k1'), 'v1.1')
        self.assertEquals(self.redis.hgetall('k2vc\0k1'), {'v1.1': '1'})
        self.assertEquals(list(self.redis.zscan_iter('expires')),
                          [('h1.a\x00k1', 101.0)])
        self.assertEquals(list(self.redis.sscan_iter('kv2hh\0k1\0v1.1')),
                          ['h1.a'])
        self.assertEquals(list(self.redis.sscan_iter('all_keys')), ['k1'])

        self.storage.process_hostreport('h1.a', 102, key='k2', value='v2.1')
        self.storage.process_hostreport('h2.a', 103, key='k1', value='v1.1')
        self.storage.process_hostreport('h3.a', 99, key='k1', value='v1.2')
        self.storage.process_hostreport('h4.a', 98, key='k2', value='v2.1')
        self.storage.process_hostreport('h4.a', 97, key='k2', value='v2.2')
        self.storage.process_hostreport('h4.a', 105, key='k1', value='v1.1')
        self.storage.process_hostreport('h4.a', 105, key='k1', value='v1.2')
        self.storage.process_hostreport('h4.a', 105, key='k1', value='v1.1')

        self.assertEquals(self.redis.hgetall('k2vc\0k1'),
                          {'v1.1': '3', 'v1.2': '1'})
        self.assertEquals(self.redis.hgetall('k2vc\0k2'),
                          {'v2.1': '2'})
        self.assertEquals(list(self.redis.zscan_iter('expires')), [
            ('h4.a\x00k2', 98.0),
            ('h3.a\x00k1', 99.0),
            ('h1.a\x00k1', 101.0),
            ('h1.a\x00k2', 102.0),
            ('h2.a\x00k1', 103.0),
            ('h4.a\x00k1', 105.0)
        ])
        self.assertEquals(sorted(self.redis.sscan_iter('kv2hh\0k1\0v1.1')),
                          ['h1.a', 'h2.a', 'h4.a'])
        self.assertEquals(sorted(self.redis.sscan_iter('kv2hh\0k2\0v2.1')),
                          ['h1.a', 'h4.a'])
        self.assertEquals(sorted(self.redis.sscan_iter('kv2hh\0k1\0v1.2')),
                          ['h3.a'])
        self.assertEquals(sorted(self.redis.sscan_iter('all_keys')),
                          ['k1', 'k2'])

        self.storage.process_hostreport('h3.a', 99, key='k1', value='v1.1')

        self.assertEquals(self.redis.hgetall('k2vc\0k1'),
                          {'v1.1': '4', 'v1.2': '0'})
        self.assertEquals(list(self.redis.sscan_iter('kv2hh\0k1\0v1.2')), [])


class CleanupTestCase(BaseRedisTestCase):
    def test(self):
        self.storage.process_hostreport('h1.a', 100, key='k', value='v')
        self.storage.process_hostreport('h2.a', 105, key='k', value='v')
        self.storage.process_hostreport('h3.a', 110, key='k', value='v')

        with mock.patch('time.time') as mock_time:
            mock_time.return_value = 10
            self.assertEquals(0, self.storage.cleanup(max_age=10, limit=10))
            self.assertEquals(self.redis.hgetall('k2vc\0k'), {'v': '3'})

            mock_time.return_value = 100
            self.assertEquals(0, self.storage.cleanup(max_age=10, limit=10))
            self.assertEquals(self.redis.hgetall('k2vc\0k'), {'v': '3'})

            mock_time.return_value = 112
            self.assertEquals(1, self.storage.cleanup(max_age=10, limit=10))
            self.assertEquals(self.redis.hgetall('k2vc\0k'), {'v': '2'})
            self.assertEquals(sorted(self.redis.zscan_iter('expires')),
                              [('h2.a\0k', 105.0), ('h3.a\0k', 110.0)])
            self.assertEquals(sorted(self.redis.sscan_iter('kv2hh\0k\0v')),
                              ['h2.a', 'h3.a'])

            mock_time.return_value = 225
            self.assertEquals(1, self.storage.cleanup(max_age=110, limit=1))
            self.assertEquals(self.redis.hgetall('k2vc\0k'), {'v': '1'})
            self.assertEquals(list(self.redis.zscan_iter('expires')),
                              [('h3.a\0k', 110.0)])
            self.assertEquals(list(self.redis.sscan_iter('kv2hh\0k\0v')),
                              ['h3.a'])

            self.assertEquals(1, self.storage.cleanup(max_age=110, limit=1))
            self.assertEquals(self.redis.hgetall('k2vc\0k'), {'v': '0'})
            self.assertEquals(list(self.redis.zscan_iter('expires')), [])
            self.assertEquals(list(self.redis.sscan_iter('kv2hh\0k\0v')), [])

            self.assertEquals(0, self.storage.cleanup(max_age=110, limit=1))


class GetSignalsTestCase(BaseRedisTestCase):
    def test(self):
        self.storage.process_hostreport('h1.a', 100, key='k1', value='v1')
        self.storage.process_hostreport('h1.a', 100, key='k2', value='v2')
        self.storage.process_hostreport('h2.a', 100, key='k5', value='v3')
        self.storage.process_hostreport('h4.a', 100, key='k5', value='v2')
        self.storage.process_hostreport('h3.a', 100, key='k5', value='v2')
        self.storage.process_hostreport('h5.a', 100, key='k5', value='v4')
        self.storage.process_hostreport('h5.a', 100, key='k5', value='v0')
        self.storage.process_hostreport('h9.a', 10, key='k9', value='v9')

        self.assertEquals(self.storage.get_signals(), {
            'k1': {'v1': 1},
            'k2': {'v2': 1},
            'k5': {'v0': 1, 'v2': 2, 'v3': 1},
            'k9': {'v9': 1}
        })

        self.storage.cleanup(max_age=20, limit=1)

        self.assertEquals(self.storage.get_signals(), {
            'k1': {'v1': 1},
            'k2': {'v2': 1},
            'k5': {'v0': 1, 'v2': 2, 'v3': 1}
        })


class _BaseListHostsGetChartTestCase(BaseRedisTestCase):
    FIXTURE_REPORTS = [
        ('h1.a', 'k0', 'v0.1'),
        ('h1.a', 'k1', 'v1.1'),
        ('h1.a', 'k2', 'v2.1'),
        ('h1.a', 'k3', 'v3.1'),
        ('h2.a', 'k0', 'v0.1'),
        ('h2.a', 'k1', 'v1.1'),
        ('h2.a', 'k2', 'v2.2'),
        ('h2.a', 'k3', 'v3.1'),
        ('h3.a', 'k0', 'v0.1'),
        ('h3.a', 'k1', 'v1.2'),
        ('h3.a', 'k2', 'v2.1'),
        ('h3.a', 'k3', 'v3.1'),
        ('h4.a', 'k0', 'v0.1'),
        ('h4.a', 'k1', 'v1.1'),
        ('h4.a', 'k2', 'v2.1'),
    ]
    FIXTURE_SELECTORS = [
        ('vtype1', 'skey1', ['h1.a', 'h2.a', 'h3.a', 'h4.a']),
        ('vtype1', 'skey2', ['h1.a', 'h2.a', 'h4.a']),
        ('vtype2', 'skey1', ['h2.a', 'h4.a']),
        ('vtype2', 'skey2', ['h1.a', 'h3.a']),
    ]

    def setUp(self):
        super(_BaseListHostsGetChartTestCase, self).setUp()
        for host, key, value in self.FIXTURE_REPORTS:
            self.storage.process_hostreport(host, 10, key, value)
        for svtype, skey, hosts in self.FIXTURE_SELECTORS:
            self.redis.sadd(self.storage._key('s2hh', svtype, skey), *hosts)

    def tearDown(self):
        self.assertFalse([key for key in self.redis.keys()
                          if key.startswith('tmp')])
        super(_BaseListHostsGetChartTestCase, self).tearDown()


class ListHostsTestCase(_BaseListHostsGetChartTestCase):
    def test_no_selector_no_filters(self):
        hosts, page, numpages = self.storage.list_hosts(
            None, None, 'k1', 'v1.10', [], page=0, hosts_on_page=10
        )
        self.assertEquals(hosts, [])
        self.assertEquals((page, numpages), (0, 0))

        hosts, page, numpages = self.storage.list_hosts(
            None, None, 'k1', 'v1.1', [], page=0, hosts_on_page=10
        )
        self.assertEquals(hosts, ['h1.a', 'h2.a', 'h4.a'])
        self.assertEquals((page, numpages), (0, 1))

        hosts, page, numpages = self.storage.list_hosts(
            None, None, 'k1', 'v1.1', [], page=2, hosts_on_page=10
        )
        self.assertEquals(hosts, ['h1.a', 'h2.a', 'h4.a'])
        self.assertEquals((page, numpages), (0, 1))

        hosts, page, numpages = self.storage.list_hosts(
            None, None, 'k1', 'v1.1', [], page=0, hosts_on_page=1
        )
        self.assertEquals(hosts, ['h1.a'])
        self.assertEquals((page, numpages), (0, 3))
        hosts, page, numpages = self.storage.list_hosts(
            None, None, 'k1', 'v1.1', [], page=1, hosts_on_page=1
        )
        self.assertEquals(hosts, ['h2.a'])
        self.assertEquals((page, numpages), (1, 3))

        hosts, page, numpages = self.storage.list_hosts(
            None, None, 'k1', 'v1.1', [], page=1, hosts_on_page=2
        )
        self.assertEquals(hosts, ['h4.a'])
        self.assertEquals((page, numpages), (1, 2))

        hosts, page, numpages = self.storage.list_hosts(
            None, None, 'k1', None, [], page=1, hosts_on_page=2
        )
        self.assertEquals(hosts, [])
        self.assertEquals((page, numpages), (0, 0))

    def test_no_selector_with_filters(self):
        hosts, page, numpages = self.storage.list_hosts(
            None, None, 'k2', 'v2.1', [('k1', 'v1.2')],
            page=0, hosts_on_page=10
        )
        self.assertEquals(hosts, ['h3.a'])
        self.assertEquals((page, numpages), (0, 1))

        hosts, page, numpages = self.storage.list_hosts(
            None, None, 'k2', 'v2.1', [('k1', 'v1.1')],
            page=0, hosts_on_page=10
        )
        self.assertEquals(hosts, ['h1.a', 'h4.a'])
        self.assertEquals((page, numpages), (0, 1))

        hosts, page, numpages = self.storage.list_hosts(
            None, None, 'k2', 'v2.1', [('k3', 'v3.1')],
            page=0, hosts_on_page=10
        )
        self.assertEquals(hosts, ['h1.a', 'h3.a'])
        self.assertEquals((page, numpages), (0, 1))

        hosts, page, numpages = self.storage.list_hosts(
            None, None, 'k3', None, [('k2', 'v2.1')],
            page=0, hosts_on_page=10
        )
        self.assertEquals(hosts, ['h4.a'])
        self.assertEquals((page, numpages), (0, 1))

    def test_with_selector_no_filters(self):
        hosts, page, numpages = self.storage.list_hosts(
            'vtype1', 'skey1', 'k0', 'v0.1', [],
            page=0, hosts_on_page=10
        )
        self.assertEquals(hosts, ['h1.a', 'h2.a', 'h3.a', 'h4.a'])
        self.assertEquals((page, numpages), (0, 1))

        hosts, page, numpages = self.storage.list_hosts(
            'vtype1', 'skey1', 'k1', 'v1.1', [], page=0, hosts_on_page=10
        )
        self.assertEquals(hosts, ['h1.a', 'h2.a', 'h4.a'])
        self.assertEquals((page, numpages), (0, 1))

        hosts, page, numpages = self.storage.list_hosts(
            'vtype2', 'skey1', 'k1', 'v1.1', [], page=0, hosts_on_page=10
        )
        self.assertEquals(hosts, ['h2.a', 'h4.a'])
        self.assertEquals((page, numpages), (0, 1))

        hosts, page, numpages = self.storage.list_hosts(
            'vtype2', 'skey1', 'k3', None, [], page=0, hosts_on_page=10
        )
        self.assertEquals(hosts, ['h4.a'])
        self.assertEquals((page, numpages), (0, 1))

    def test_with_selector_with_filters(self):
        hosts, page, numpages = self.storage.list_hosts(
            'vtype1', 'skey1', 'k0', 'v0.1', [], page=0, hosts_on_page=10
        )
        self.assertEquals(hosts, ['h1.a', 'h2.a', 'h3.a', 'h4.a'])
        self.assertEquals((page, numpages), (0, 1))

        hosts, page, numpages = self.storage.list_hosts(
            'vtype1', 'skey1', 'k1', 'v1.1', [], page=0, hosts_on_page=10
        )
        self.assertEquals(hosts, ['h1.a', 'h2.a', 'h4.a'])
        self.assertEquals((page, numpages), (0, 1))

        hosts, page, numpages = self.storage.list_hosts(
            'vtype2', 'skey2', 'k1', 'v1.1', [], page=0, hosts_on_page=10
        )
        self.assertEquals(hosts, ['h1.a'])
        self.assertEquals((page, numpages), (0, 1))

        hosts, page, numpages = self.storage.list_hosts(
            'vtype2', 'skey2', 'k1', None, [], page=0, hosts_on_page=10
        )
        self.assertEquals(hosts, [])
        self.assertEquals((page, numpages), (0, 0))

        hosts, page, numpages = self.storage.list_hosts(
            'vtype2', 'skey1', 'k3', None, [], page=0, hosts_on_page=10
        )
        self.assertEquals(hosts, ['h4.a'])
        self.assertEquals((page, numpages), (0, 1))

    def test_compact(self):
        hosts, page, numpages = self.storage.list_hosts(
            None, None, 'k0', 'v0.1', [],
            page=0, hosts_on_page=10, compact=True
        )
        self.assertEquals(hosts, ['h{1..4}.a'])


class GetChartTestCase(_BaseListHostsGetChartTestCase):
    def test_no_selector_no_filters(self):
        chart = self.storage.get_chart(None, None, 'k1', [])
        self.assertEquals(chart, {'v1.1': 3, 'v1.2': 1, '': 0})

        self.storage.process_hostreport('h3.a', 10, 'k1', 'v1.1')
        chart = self.storage.get_chart(None, None, 'k1', [])
        self.assertEquals(chart, {'v1.1': 4, '': 0})

        chart = self.storage.get_chart(None, None, 'k3', [])
        self.assertEquals(chart, {'v3.1': 3, '': 0})

    def test_no_selector_with_filters(self):
        chart = self.storage.get_chart(None, None, 'k2', [('k1', 'v1.2')])
        self.assertEquals(chart, {'': 0, 'v2.1': 1})

        chart = self.storage.get_chart(None, None, 'k3', [('k1', 'v1.1')])
        self.assertEquals(chart, {'': 1, 'v3.1': 2})

    def test_with_selector_no_filters(self):
        chart = self.storage.get_chart('vtype1', 'skey1', 'k1', [])
        self.assertEquals(chart, {'': 0, 'v1.1': 3, 'v1.2': 1})

        chart = self.storage.get_chart('vtype1', 'skey2', 'k1', [])
        self.assertEquals(chart, {'': 0, 'v1.1': 3})

        chart = self.storage.get_chart('vtype2', 'skey1', 'k1', [])
        self.assertEquals(chart, {'': 0, 'v1.1': 2})

        chart = self.storage.get_chart('vtype2', 'skey2', 'k1', [])
        self.assertEquals(chart, {'': 0, 'v1.1': 1, 'v1.2': 1})

        chart = self.storage.get_chart('vtype1', 'skey1', 'k3', [])
        self.assertEquals(chart, {'': 1, 'v3.1': 3})

    def test_with_selector_with_filters(self):
        chart = self.storage.get_chart(
            'vtype1', 'skey1', 'k1', [('k2', 'v2.1')],
        )
        self.assertEquals(chart, {'': 0, 'v1.1': 2, 'v1.2': 1})

        chart = self.storage.get_chart(
            'vtype1', 'skey1', 'k1', [('k2', 'v2.2')]
        )
        self.assertEquals(chart, {'': 0, 'v1.1': 1})

        chart = self.storage.get_chart(
            'vtype2', 'skey1', 'k1', [('k3', 'v3.1')]
        )
        self.assertEquals(chart, {'': 0, 'v1.1': 1})

        chart = self.storage.get_chart(
            'vtype1', 'skey1', 'k3', [('k1', 'v1.1')]
        )
        self.assertEquals(chart, {'': 1, 'v3.1': 2})

        chart = self.storage.get_chart(
            'vtype1', 'skey1', 'k3', [('k1', 'v1.5')]
        )
        self.assertEquals(chart, {'': 0})


class ViewedChartsTestCase(BaseRedisTestCase):
    def test(self):
        self.assertFalse(self.storage.is_chart_viewed('ckey1'))
        self.storage.mark_chart_as_viewed('ckey1')
        self.assertEquals(self.redis.get('cv\0ckey1'), '1')
        self.assertEquals(self.redis.ttl('cv\0ckey1'), 5)
        self.assertTrue(self.storage.is_chart_viewed('ckey1'))


class SaveSelectorHostsTestCase(BaseRedisTestCase):
    def test(self):
        self.storage.save_selector_hosts(
            'svtype', 'skey', hosts=['h1.a', 'h2.a', 'h4.a']
        )
        self.assertEquals(sorted(self.redis.sscan_iter('s2hh\0svtype\0skey')),
                          ['h1.a', 'h2.a', 'h4.a'])

        self.storage.save_selector_hosts('svtype', 'skey', hosts=[])
        self.assertEquals(sorted(self.redis.sscan_iter('s2hh\0svtype\0skey')),
                          [])
