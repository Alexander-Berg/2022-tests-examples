from django.test.utils import override_settings

from ..base import BaseWorkerTestCase
from intranet.magiclinks.src.links.dto import List, String, Image
from intranet.magiclinks.src.links.workers.intranet.lunapark import Worker as LunaparkWorker


class LunaparkWorkerTestCase(BaseWorkerTestCase):
    worker_class = LunaparkWorker
    worker_class_file = 'lunapark'

    def test_lunapark_parse_url(self):
        hostname_match = 'lunapark.yandex-team.ru'
        group_name = 'id'
        urls_data = (
            ('https://lunapark.yandex-team.ru/123', '123'),
            ('https://lunapark.yandex-team.ru/что-угодно', 'что-угодно'),
            ('https://lunapark.yandex-team.ru/1084677#tab=test_data&tags=&plot_groups=main', '1084677')
        )
        for url, path_match in urls_data:
            self.parse_url(url, hostname_match, {group_name: path_match})

    def test_lunapark_success(self):
        url = 'https://lunapark.yandex-team.ru/123'
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Lunapark',
                    ),
                    String(value='123'),
                ]
            )
        }
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         ))

    def test_lunapark_fail_without_id(self):
        url = 'https://lunapark.yandex-team.ru/'
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Lunapark',
                    ),
                    String(value='https://lunapark.yandex-team.ru/'),
                ]
            )
        }
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         ))

    def test_lunapark_fail_wrong_path(self):
        url = 'https://lunapark.yandex-team.ru/#tab=test_data&tags=&plot_groups=main'
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Lunapark',
                    ),
                    String(value='https://lunapark.yandex-team.ru/#tab=test_data&tags=&plot_groups=main'),
                ]
            )
        }
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         ))

    @override_settings(CLEAR_CACHE={'lunapark'})
    def test_lunapark_no_cache_success(self):
        url = 'https://lunapark.yandex-team.ru/123'
        expected_data = {
            url: List(
                ttl=-1,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Lunapark',
                    ),
                    String(value='123'),
                ]
            )
        }
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         ))
