from datacloud.features.cluster import extract_url
from hamcrest import assert_that, equal_to


# # TODO: Test on retro and on prod behaviour
# TODO: Test reduce counter
# TODO: Test date-filter


class TestHostNameUrlExtractor(object):

    def test_single_record(self):
        input_records = [
            {
                'yuid': '12345',
                'timestamp': 1497869132,
                'title': 'Яндекс',
                'url': 'https://m.avito.ru/catalog/moskva/uslugi'
            }
        ]
        result_records = list(extract_url.HostNameUrlExtractor(is_retro=False)(input_records))
        expected_records = [
            {'yuid': '12345', 'host': 'm.avito.ru', 'counter': 1, 'timestamp': 1497869132}
        ]

        assert_that(expected_records, equal_to(result_records))

    def test_counter(self):
        input_records = [
            {
                'yuid': '12345',
                'timestamp': 1497869132,
                'title': 'Яндекс',
                'url': 'https://m.avito.ru/catalog/moskva/uslugi'
            },
            {
                'yuid': '12345',
                'timestamp': 1497869200,
                'title': 'Яндекс',
                'url': 'https://m.avito.ru/catalog/spb/uslugi'
            }
        ]
        result_records = list(extract_url.HostNameUrlExtractor(is_retro=False)(input_records))
        expected_records = [
            {'yuid': '12345', 'host': 'm.avito.ru', 'counter': 2, 'timestamp': 1497869200}
        ]
        assert_that(expected_records, equal_to(result_records))

    def test_single_remove_www(self):
        input_records = [
            {
                'yuid': '12345',
                'timestamp': 1497869132,
                'title': 'Яндекс',
                'url': 'https://www.m.avito.ru/catalog/moskva/uslugi'
            }
        ]
        result_records = list(extract_url.HostNameUrlExtractor(is_retro=False)(input_records))
        expected_records = [
            {'yuid': '12345', 'host': 'm.avito.ru', 'counter': 1, 'timestamp': 1497869132}
        ]

        assert_that(expected_records, equal_to(result_records))
