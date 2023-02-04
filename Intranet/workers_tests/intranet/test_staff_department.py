from ..base import BaseWorkerTestCase
from intranet.magiclinks.src.links.dto import List, String, Image
from intranet.magiclinks.src.links.workers.intranet.staff_department import Worker as StaffDepartmentWorker


class StaffDepartmentWorkerTestCase(BaseWorkerTestCase):
    worker_class = StaffDepartmentWorker
    worker_class_file = 'staff_department'

    def test_staff_department_parse_url(self):
        hostname_match = 'staff.yandex-team.ru'
        group_name = 'url'
        urls_data = (
            ('https://staff.yandex-team.ru/departments/yandex_infra_tech_tools/',
             'yandex_infra_tech_tools'),
        )
        for url, path_match in urls_data:
            self.parse_url(url, hostname_match, {group_name: path_match})

    def test_staff_department_url_successful(self):
        url = 'https://staff.yandex-team.ru/departments/yandex_infra_tech_tools/'
        expected_data = {
            url: List(
                ttl=86400,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Staff'
                    ),
                    String(value='Отдел внутренних сервисов'),
                    String(value=81, color='gray'),
                ]
            )
        }
        cassette_name = 'staff_department_successful.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_staff_department_url_no_match_failed(self):
        url = 'https://staff.yandex-team.ru/departments/yandex_some_tech_tools_new/'
        expected_data = {
            url: List(
                ttl=86400,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Staff'
                    ),
                    String(value='https://staff.yandex-team.ru/departments/yandex_some_tech_tools_new/'),
                ]
            )
        }
        cassette_name = 'staff_department_no_match_failed.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_staff_department_url_failed(self):
        url = 'https://staff.yandex-team.ru/departments/yandex_some_tech_tools/'
        expected_data = {
            url: List(
                ttl=60,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Staff'
                    ),
                    String(value='yandex_some_tech_tools'),
                ],
                completed=False,
            )
        }
        cassette_name = 'staff_department_failed.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))
