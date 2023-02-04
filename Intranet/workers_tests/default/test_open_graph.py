import asynctest

from ..base import BaseWorkerTestCase

from intranet.magiclinks.src.links.dto import List, String, Image
from intranet.magiclinks.src.links.workers.default.open_graph import Worker as OpenGraphWorker


class OpenGraphTestCase(BaseWorkerTestCase):
    worker_class = OpenGraphWorker
    worker_class_file = 'open_graph'

    def test_open_graph_parse_url(self):
        urls_data = (
            ('https://vk.com/kinopoisk?w=wall-108468_2001345',
             'vk.com',
             ),
            ('https://www.engadget.com/2017/12/05/apple-iphone-x-sim-free-unlocked/',
             'www.engadget.com',
             ),
            ('https://wilya.ru/some',
             'wilya.ru',
             )
        )
        for url, hostname_match in urls_data:
            self.parse_url(url, hostname_match)

    def test_open_graph_not_parse_url(self):
        urls_data = (
            'https://something.yandex-team.ru/test',
            'https://yandex-team.ru/test',
            'https://myyandex-team.ru/test',
        )
        for url in urls_data:
            self.parse_url(url, should_parse=False)

    def test_open_graph_url_successful(self):
        url = 'https://twitter.com/aya__fay/status/934746999966052352'
        expected_data = {
            url: List(
                ttl=86400,
                value=[
                    List(
                        ttl=86400,
                        value=[
                            Image(
                                src='https://favicon.yandex.net/favicon/twitter.com',
                                text='OpenGraph'

                            )],
                        action={
                            "event": "click",
                            "type": "halfscreenpreview",
                            "url": 'https://twitter.com/aya__fay/status/934746999966052352',
                        },

                    ),
                    String(value="Aya-Fay on Twitter",),
                    String(value='@Twitter'),
                ]
            )
        }
        cassette_name = 'graph_successful.yaml'
        with asynctest.patch.object(OpenGraphWorker, 'get_headers', return_value={'test': 'some'}):
            with asynctest.patch.object(OpenGraphWorker, 'get_request_params', return_value={'proxy': 'some'}):
                self.loop.run_until_complete(self.response_check(
                    url,
                    expected_data=expected_data,
                    cassette_name=cassette_name,
                ))

    def test_open_graph_url_long_title_successful(self):
        url = 'https://www.engadget.com/2017/12/05/apple-iphone-x-sim-free-unlocked/'
        expected_data = {
            url: List(
                ttl=86400,
                value=[
                    List(
                        ttl=86400,
                        value=[
                            Image(
                                src='https://favicon.yandex.net/favicon/www.engadget.com',
                                text='OpenGraph'

                            )],
                        action={
                            "event": "click",
                            "type": "halfscreenpreview",
                            "url": 'https://www.engadget.com/2017/12/05/apple-iphone-x-sim-free-unlocked/',
                        },

                    ),
                    String(value="Apple's iPhone X is availa... ",),
                    String(value='@Engadget'),
                ]
            )
        }
        cassette_name = 'graph_long_title_successful.yaml'
        with asynctest.patch.object(OpenGraphWorker, 'get_headers', return_value={'test': 'some'}):
            with asynctest.patch.object(OpenGraphWorker, 'get_request_params', return_value={'proxy': 'some'}):
                self.loop.run_until_complete(self.response_check(
                    url,
                    expected_data=expected_data,
                    cassette_name=cassette_name,
                ))

    def test_open_graph_url_failed(self):
        url = 'https://news.rbk.ru/yandsearch?cl4url=www.rbc.ru%2Fpolitics%2F15%2F11%2F2017%2F5a0c12fd9a7947e3a0b5f3af&lr=213&rpt=story'
        expected_data = {
            url: List(
                ttl=1200,
                value=[
                    List(
                        ttl=1200,
                        value=[
                            Image(
                                src='https://favicon.yandex.net/favicon/news.rbk.ru',
                                text='OpenGraph'

                            )],
                        action={
                            "event": "click",
                            "type": "halfscreenpreview",
                            "url": 'https://news.rbk.ru/yandsearch?cl4url=www.rbc.ru%2Fpolitics%2F15%2F11%2F2017%2F5a0c12fd9a7947e3a0b5f3af&lr=213&rpt=story',
                        },

                    ),
                    String(value="https://news.rbk.ru/yandsearch?cl4url=www.rbc.ru%2Fpolitics%2F15%2F11%2F2017%2F5a0c12fd9a7947e3a0b5f3af&lr=213&rpt=story",),
                ]
            )
        }
        cassette_name = 'graph_failed.yaml'
        with asynctest.patch.object(OpenGraphWorker, 'get_headers', return_value={'test': 'some'}):
            with asynctest.patch.object(OpenGraphWorker, 'get_request_params', return_value={'proxy': 'some'}):
                self.loop.run_until_complete(self.response_check(
                    url,
                    expected_data=expected_data,
                    cassette_name=cassette_name,
                ))

    def test_open_graph_wrong_content_type(self):
        url = 'https://www.corkart.pt/Corkart_Russia_2019-2020_.pdf'
        expected_data = {
            url: List(
                ttl=86400,
                value=[
                    List(
                        ttl=86400,
                        value=[
                            Image(
                                src='https://favicon.yandex.net/favicon/www.corkart.pt',
                                text='OpenGraph'

                            )],
                        action={
                            "event": "click",
                            "type": "halfscreenpreview",
                            "url": 'https://www.corkart.pt/Corkart_Russia_2019-2020_.pdf',
                        },

                    ),
                    String(value="https://www.corkart.pt/Corkart_Russia_2019-2020_.pdf",),
                ]
            )
        }
        cassette_name = 'graph_wrong_content_type.yaml'
        with asynctest.patch.object(OpenGraphWorker, 'get_headers', return_value={'test': 'some'}):
            with asynctest.patch.object(OpenGraphWorker, 'get_request_params', return_value={'proxy': 'some'}):
                self.loop.run_until_complete(self.response_check(
                    url,
                    expected_data=expected_data,
                    cassette_name=cassette_name
                ))
