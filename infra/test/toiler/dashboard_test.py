import unittest

import mock

from lacmus2.toiler.dashboard import dashboard_vtype_processor

from . import exhaust


class DashboardTestCase(unittest.TestCase):
    def test(self):
        record = {
            'key': 'dashboard#1',
            'source': {
                'charts': [
                    {'key': 'chart1'},
                    {'key': 'chart2'},
                    {'key': 'chart3'},
                ]
            },
            'meta': {'old': 'meta'},
        }
        database = object()
        proc = dashboard_vtype_processor(database, record, False)
        with mock.patch('genisys.toiler.base.update_volatiles_atime') as ava:
            value, log = exhaust(self, proc, 3)

        c1, c2 = map(tuple, ava.call_args_list)
        self.assertEquals(c1, ((database, 'dashboard', ['dashboard#1']), {}))
        self.assertEquals(c2, ((database, 'chart',
                                ['chart1', 'chart2', 'chart3']), {}))
