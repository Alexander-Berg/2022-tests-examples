from ..base import BaseWorkerTestCase

from intranet.magiclinks.src.links.dto import List, String, Image
from intranet.magiclinks.src.links.workers.intranet.yandex_internal import Worker as YandexInternalWorker


class YandexInternalTestCase(BaseWorkerTestCase):
    worker_class = YandexInternalWorker
    worker_class_file = 'yandex_internal'

    def test_yandex_internal_parse_url(self):
        urls_data = (
            ('https://test.yandex.ru/test',
             'test.yandex.ru',
             ),
            ('https://www.test.ya.ru/test',
             'www.test.ya.ru',
             ),
            ('https://yandex.test.ru/some',
             'yandex.test.ru',
             ),
            ('https://smth.yandex/some',
             'smth.yandex',
             ),
            ('https://auto.ru/some',
             'auto.ru',
             ),
            ('https://avto.ru/some',
             'avto.ru',
             ),
        )
        for url, hostname_match in urls_data:
            self.parse_url(url, hostname_match)

    def test_yandex_internal_not_parse_url(self):
        urls_data = (
            'https://something.yandex-team.ru/test',
            'https://yandex-team.ru/test',
            'https://myyandex-team.ru/test',
            'https://wilya.ru/some',
            'https://testyatest.ru/some',
            'https://autotest.ru/some',
        )
        for url in urls_data:
            self.parse_url(url, should_parse=False)

    def test_yandex_internal_yandex_host_success(self):
        url = 'https://something.yandex.ru/test'
        expected_data = {
            url: List(
                ttl=86400,
                value=[
                    List(
                        ttl=86400,
                        value=[
                            Image(
                                src='https://favicon.yandex.net/favicon/something.yandex.ru',
                                text='YandexInternal'

                            )],
                        action={
                            "event": "click",
                            "type": "halfscreenpreview",
                            "url": 'https://something.yandex.ru/test',
                        },

                    ),
                    String(
                        value="https://something.yandex.ru/test", ),
                ]
            )
        }
        self.loop.run_until_complete(self.response_check(url, expected_data=expected_data, ))

    def test_yandex_internal_kinopoisk_host_success(self):
        url = 'https://kinopoisk.test.ru/test'
        expected_data = {
            url: List(
                ttl=86400,
                value=[
                    List(
                        ttl=86400,
                        value=[
                            Image(
                                src='https://favicon.yandex.net/favicon/kinopoisk.test.ru',
                                text='YandexInternal'

                            )],
                        action={
                            "event": "click",
                            "type": "halfscreenpreview",
                            "url": 'https://kinopoisk.test.ru/test',
                        },

                    ),
                    String(
                        value="https://kinopoisk.test.ru/test", ),
                ]
            )
        }
        self.loop.run_until_complete(self.response_check(url, expected_data=expected_data, ))
