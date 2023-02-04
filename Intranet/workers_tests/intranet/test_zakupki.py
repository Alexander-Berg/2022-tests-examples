from asynctest import patch

from ..base import BaseWorkerTestCase
from intranet.magiclinks.src.links.dto import List, String, Image, User
from intranet.magiclinks.src.links.workers.intranet.zakupki import Worker as ZakupkiWorker


class ZakupkiWorkerTestCase(BaseWorkerTestCase):
    worker_class = ZakupkiWorker
    worker_class_file = 'zakupki'

    def test_zakupki_parse_url(self):
        hostname_match = 'procu.test.yandex-team.ru'
        group_name = 'key'
        urls_data = (
            ('https://procu.test.yandex-team.ru/YP957',
             'YP957'),
            ('https://procu.test.yandex-team.ru/YP966/comments',
             'YP966'),
            ('https://procu.test.yandex-team.ru/YP966/comments/',
             'YP966'),
            ('https://procu.test.yandex-team.ru/YP966/',
             'YP966'),
            ('https://procu.test.yandex-team.ru/YP966/procurement',
             'YP966'),
            ('https://procu.test.yandex-team.ru/YP966/procurement/',
             'YP966'),
        )

        for url, path_match in urls_data:
            self.parse_url(url, hostname_match, {group_name: path_match})

    def test_zakupki_success(self):
        url = 'https://procu.test.yandex-team.ru/YP642'
        expected_data = {
            url: List(
                ttl=300,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Zakupki',
                    ),
                    String(value='YP642', strike=False, ),
                    String(value=u'Сбор предложений', color='#eeaa00', ),
                    String(value=u'Тест комментариев к предложению'),
                ]
            )
        }
        cassette_name = 'zakupki_success.yaml'
        with patch.object(ZakupkiWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(self.response_check(url,
                                                             expected_data=expected_data,
                                                             cassette_name=cassette_name,
                                                             ))

    def test_zakupki_with_user_success(self):
        url = 'https://procu.test.yandex-team.ru/YP570'
        expected_data = {
            url: List(
                ttl=300,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Zakupki',
                    ),
                    String(value='YP570', strike=False, ),
                    String(value=u'Выбор поставщика', color='#00aaff', ),
                    String(value=u'Ведро гвоздей'),
                    User(login='apioro'),
                ]
            )
        }
        cassette_name = 'zakupki_with_user_success.yaml'
        with patch.object(ZakupkiWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(
                self.response_check(
                    url,
                    expected_data=expected_data,
                    cassette_name=cassette_name,
                )
            )

    def test_zakupki_with_strike_success(self):
        url = 'https://procu.test.yandex-team.ru/YP605'
        expected_data = {
            url: List(
                ttl=3600,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Zakupki',
                    ),
                    String(value='YP605', strike=True, ),
                    String(value=u'Закрыт', color='#ee0000', ),
                    String(value=u'Фантазия'),
                    User(login='apioro'),
                ]
            )
        }
        cassette_name = 'zakupki_with_strike_success.yaml'
        with patch.object(ZakupkiWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(self.response_check(url,
                                                             expected_data=expected_data,
                                                             cassette_name=cassette_name,
                                                             ))

    def test_zakupki_fail_error_response(self):
        url = 'https://procu.test.yandex-team.ru/YP642234234'
        expected_data = {
            url: List(
                ttl=60,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Zakupki',
                    ),
                    String(value='YP642234234'),
                ],
                completed=False,
            )
        }
        cassette_name = 'zakupki_fail_error_response.yaml'
        with patch.object(ZakupkiWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(self.response_check(url,
                                                             expected_data=expected_data,
                                                             cassette_name=cassette_name,
                                                             ))
