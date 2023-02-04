from ..base import BaseWorkerTestCase
from intranet.magiclinks.src.links.dto import List, String, Image
from intranet.magiclinks.src.links.workers.intranet.atushka import Worker as AtushkaWorker


class AtushkaWorkerTestCase(BaseWorkerTestCase):
    worker_class = AtushkaWorker
    worker_class_file = 'atushka'

    def test_atushka_parse_url(self):
        group_name = 'path'
        urls_data = (
            ('https://avrudakova.at.yandex-team.ru/275', '/275', 'avrudakova.at.yandex-team.ru'),
            ('https://clubs.at.yandex-team.ru/tools-fcukups/1110?parent_id=1112', '/tools-fcukups/1110',
             'clubs.at.yandex-team.ru'),
            ('https://avrudakova.at.yandex-team.ru', '', 'avrudakova.at.yandex-team.ru'),
            ('https://avrudakova.at.yandex-team.ru/', '/', 'avrudakova.at.yandex-team.ru'),
        )
        for url, path_match, hostname_match in urls_data:
            self.parse_url(url, hostname_match, {group_name: path_match})

    def test_atushka_success(self):
        url = 'https://avrudakova.at.yandex-team.ru/275'

        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Atushka',
                    ),
                    String(value='avrudakova.at.yandex-team.ru/275'),
                ]
            )
        }
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         ))

    def test_atushka_success_with_long_path(self):
        url = 'https://clubs.at.yandex-team.ru/tools-fcukups/1110?parent_id=1112'
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Atushka',
                    ),
                    String(value='clubs.at.yandex-team.ru/tools-fcukups/1110'),
                ]
            )
        }
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         ))

    def test_atushka_success_without_path(self):
        url = 'https://clubs.at.yandex-team.ru/'
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Atushka',
                    ),
                    String(value='clubs.at.yandex-team.ru'),
                ]
            )
        }

        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         ))

    def test_atushka_success_without_path_and_slash(self):
        url = 'https://clubs.at.yandex-team.ru'
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Atushka',
                    ),
                    String(value='clubs.at.yandex-team.ru'),
                ]
            )
        }

        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         ))

    def test_atushka_success_without_path_with_params(self):
        url = 'https://clubs.at.yandex-team.ru/#tab=test_data&tags=&plot_groups=main'
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Atushka',
                    ),
                    String(value='clubs.at.yandex-team.ru'),
                ]
            )
        }

        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         ))
