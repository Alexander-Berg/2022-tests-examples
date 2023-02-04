from ..base import BaseWorkerTestCase
from intranet.magiclinks.src.links.dto import List, User
from intranet.magiclinks.src.links.workers.intranet.staff_user import Worker as StaffUserWorker


class StaffUserWorkerTestCase(BaseWorkerTestCase):
    worker_class = StaffUserWorker
    worker_class_file = 'staff_user'

    def test_staff_user_parse_url(self):
        hostname_match = 'staff.yandex-team.ru'
        group_name = 'login'
        urls_data = (
            ('https://staff.yandex-team.ru/smosker',
             'smosker'),
            ('https://staff.yandex-team.ru/testsome/',
             'testsome'),
            ('https://staff.yandex-team.ru/test-some/',
             'test-some'),
            ('https://staff.yandex-team.ru/test_some/',
             'test_some'),
        )
        for url, path_match in urls_data:
            self.parse_url(url, hostname_match, {group_name: path_match})

    def test_staff_user_not_parse_url(self):
        urls_data = (
            'https://staff.yandex-team.ru/department/smosker',
            'https://staff.yandex-team.ru/smth/testsome/',
        )
        for url in urls_data:
            self.parse_url(url, should_parse=False)

    def test_staff_user_successful(self):
        url = 'https://staff.yandex-team.ru/volozh'
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    User(title='Аркадий Волож', login='volozh'),
                ]
            )
        }
        cassette_name = 'staff_user_successful.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_staff_user_eng_successful(self):
        url = 'https://staff.yandex-team.ru/volozh'
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    User(title='Arkady Volozh', login='volozh'),
                ]
            )
        }
        cassette_name = 'staff_user_eng_successful.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_staff_user_wrong_login_failed(self):
        url = 'https://staff.yandex-team.ru/testmeplease/'
        expected_data = {}
        cassette_name = 'staff_user_failed.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_staff_user_bad_response_failed(self):
        url = 'https://staff.yandex-team.ru/smosker/'
        expected_data = {}
        cassette_name = 'staff_user_bad_response_failed.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))
