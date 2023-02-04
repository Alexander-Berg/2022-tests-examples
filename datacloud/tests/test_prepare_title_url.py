# -*- coding: utf-8 -*-
import unittest
from datacloud.features.dssm import prepare_title_url
from datacloud.dev_utils.testing.testing_utils import FakeContext, RecordsGenerator


class TestTitleUrlReducer(unittest.TestCase):
    def test_prod_reducer(self):
        ext_id_records = [
            {'yuid': '111', 'cid': '1'},
        ]
        watch_log_records = [
            {
                'yuid': '111',
                'timestamp': 1524031431,
                'title': 'Яндекс',
                'url': 'https://www.yandex.ru/'
            },
            {
                'yuid': '111',
                'timestamp': 1524031600,
                'title': 'Google',
                'url': 'https://www.google.com/'
            }
        ]
        context = FakeContext()
        reducer = prepare_title_url.TitleUrlReducer()
        generator = RecordsGenerator([ext_id_records, watch_log_records], context)
        result_records = list(reducer({'yuid': '111'}, generator, context))
        expected_records = [
            {'url': u'https://www.yandex.ru/', 'hash': 'd43e6eab50de0d1528e39529a49ca850', 'key': '1', 'timestamp': 1524031431, 'title': u'\u042f\u043d\u0434\u0435\u043a\u0441'},
            {'url': u'https://www.google.com/', 'hash': '222897291ef72b3d2537212e74474c84', 'key': '1', 'timestamp': 1524031600, 'title': u'Google'}
        ]
        self.assertEqual(expected_records, result_records)

    def test_retro_date_filter(self):
        ext_id_records = [
            {'yuid': '111', 'external_id': '1', 'timestamp': 152403005},
        ]
        watch_log_records = [
            {
                'yuid': '111',
                'timestamp': 152403001,
                'title': 'Яндекс',
                'url': 'https://www.yandex.ru/'
            },
            {
                'yuid': '111',
                'timestamp': 152403002,
                'title': 'Google',
                'url': 'https://www.google.com/'
            },
            {  # Должен отфильтроваться по timestamp
                'yuid': '111',
                'timestamp': 1524035009,
                'title': 'Rambler',
                'url': 'https://www.ramblre.ru/'
            }
        ]
        context = FakeContext()
        reducer = prepare_title_url.TitleUrlReducer()
        generator = RecordsGenerator([ext_id_records, watch_log_records], context)
        result_records = list(reducer({'yuid': '111'}, generator, context))
        expected_records = [
            {'url': u'https://www.yandex.ru/', 'hash': 'd43e6eab50de0d1528e39529a49ca850', 'key': '1', 'timestamp': 152403001, 'title': u'\u042f\u043d\u0434\u0435\u043a\u0441'},
            {'url': u'https://www.google.com/', 'hash': '222897291ef72b3d2537212e74474c84', 'key': '1', 'timestamp': 152403002, 'title': u'Google'}
        ]
        self.assertEqual(expected_records, result_records)


if __name__ == '__main__':
    unittest.main()
