# coding: utf-8
from ..base import BaseWorkerTestCase
from intranet.magiclinks.src.links.dto import List, String, Image
from intranet.magiclinks.src.links.workers.intranet.staff_table import Worker as StaffTableWorker


class StaffTableWorkerTestCase(BaseWorkerTestCase):
    worker_class = StaffTableWorker
    worker_class_file = 'staff_table'

    def test_staff_table_parse_url(self):
        hostname_match = 'staff.yandex-team.ru'
        group_name = 'number'
        urls_data = (
            ('https://staff.yandex-team.ru/map/table/245/',
             '245'),
            ('https://staff.yandex-team.ru/map/table/13069',
             '13069'),
        )
        for url, path_match in urls_data:
            self.parse_url(url, hostname_match, {group_name: path_match})

    def test_staff_table_url_successful(self):
        url = 'https://staff.yandex-team.ru/map/table/36073'
        expected_data = {
            url: List(
                ttl=86400,
                value=[
                    String(value='Москва, БЦ Аврора,'),
                    String(value='Третий B-этаж,'),
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Staff'
                    ),
                    String(value=36073),
                ]
            )
        }
        cassette_name = 'staff_table_successful.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_staff_table_url_no_match_failed(self):
        url = 'https://staff.yandex-team.ru/map/table/99999'
        expected_data = {
            url: List(
                ttl=86400,
                value=[
                    String(value='https://staff.yandex-team.ru/map/table/99999'),
                ]
            )
        }
        cassette_name = 'staff_table_no_match_failed.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_staff_table_url_failed(self):
        url = 'https://staff.yandex-team.ru/map/table/99999'
        expected_data = {
            url: List(
                ttl=60,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Staff'
                    ),
                    String(value='99999'),
                ],
                completed=False,
            )
        }
        cassette_name = 'staff_table_failed.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))
