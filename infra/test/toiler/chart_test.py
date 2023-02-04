import unittest

import mock

from lacmus2.toiler import config
from lacmus2.toiler.chart import ChartVtypeProcessor

from ..redis_test import BaseRedisTestCase
from . import exhaust


class ChartCallTestCase(BaseRedisTestCase):
    def test(self):
        self.storage.process_hostreport('h1', 10, key='k1', value='v1.1')
        self.storage.process_hostreport('h2', 10, key='k1', value='v1.1')
        self.storage.process_hostreport('h3', 10, key='k1', value='v1.2')
        self.storage.process_hostreport('h4', 10, key='k1', value='v1.2')
        self.storage.process_hostreport('h1', 10, key='k2', value='v2.1')
        self.storage.process_hostreport('h2', 10, key='k2', value='v2.1')
        self.storage.process_hostreport('h3', 10, key='k2', value='v2.1')
        self.storage.process_hostreport('h4', 10, key='k2', value='v2.2')
        self.storage.process_hostreport('h5', 10, key='k1', value='v1.1')

        self.storage.save_selector_hosts('selector', '00112233',
                                         hosts=['h1', 'h2', 'h3', 'h4'])

        proc = ChartVtypeProcessor(self.storage, None)
        record = {
            'source': {
                'selector_vtype': 'selector',
                'selector_key': '00112233',
                'signal': 'k1',
                'filters': [('k2', 'v2.1')]
            },
            'meta': {'old': 'meta'}
        }
        database = object()
        with mock.patch('genisys.toiler.base.update_volatiles_atime') as ava:
            value, log = exhaust(self, proc(database, record, forced=False), 3)

        ava.assert_called_once_with(database, vtype='selector',
                                    keys=['00112233'])

        self.assertEquals(value, (
            {'': 0, 'v1.1': 2, 'v1.2': 1},
            {'old': 'meta'}
        ))

    def test_no_selector(self):
        proc = ChartVtypeProcessor(self.storage, None)
        record = {
            'source': {
                'selector_vtype': None,
                'selector_key': None,
                'signal': 'k1',
                'filters': [('k2', 'v2.1')]
            },
            'meta': {'old': 'meta'}
        }
        database = object()
        with mock.patch('genisys.toiler.base.update_volatiles_atime') as ava:
            value, log = exhaust(self, proc(database, record, forced=False), 2)
        ava.assert_not_called()


class ChartGetResultTtlTestCase(BaseRedisTestCase):
    def test(self):
        proc = ChartVtypeProcessor(self.storage, None)
        self.storage.mark_chart_as_viewed('k2')
        self.assertEquals(
            proc.get_result_ttl({'key': 'k1'}),
            config.CHART_RESULT_TTL
        )
        self.assertEquals(
            proc.get_result_ttl({'key': 'k2'}),
            config.CHART_VIEWED_RESULT_TTL
        )


class ChartPostprocessValueTestCase(unittest.TestCase):
    def test(self):
        historical = mock.MagicMock()
        proc = ChartVtypeProcessor(None, historical)
        proc.postprocess_value({'key': 'k1'}, [])
        historical.write_point.assert_not_called()
        proc.postprocess_value({'key': 'k1'}, None)
        historical.write_point.assert_not_called()
        proc.postprocess_value({'key': 'k1'}, {'v1': 10, 'v2': 20})
        historical.write_point.assert_called_once_with(
            'k1', {'v1': 10, 'v2': 20}
        )
