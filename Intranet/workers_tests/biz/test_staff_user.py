from asynctest import patch

from django.test.utils import override_settings

from ..base import BaseWorkerTestCase
from intranet.magiclinks.src.links.dto import List, User
from intranet.magiclinks.src.links.workers.biz.staff_user import Worker as StaffUserWorker


BIZ_STAFF_SETTINGS = {
    'DIR_HOST': 'https://api-internal-test.directory.ws.yandex.net',
    'DIR_TVM2_CLIENT_ID': '2000204',
}


class StaffUserWorkerTestCase(BaseWorkerTestCase):
    worker_class = StaffUserWorker
    worker_class_file = 'staff_user'

    def test_staff_user_parse_url(self):
        hostname_match = 'team.test.yandex.ru'
        group_name = 'login'
        urls_data = (
            ('https://team.test.yandex.ru/smosker',
             'smosker'),
            ('https://team.test.yandex.ru/testsome/',
             'testsome'),
            ('https://team.test.yandex.ru/test-some/',
             'test-some'),
            ('https://team.test.yandex.ru/test_some/',
             'test_some'),
            ('https://team.test.yandex.ru/test.some/',
             'test.some'),
        )
        for url, path_match in urls_data:
            self.parse_url(url, hostname_match, {group_name: path_match})

    @override_settings(**BIZ_STAFF_SETTINGS)
    def test_staff_user_successful(self):
        url = 'https://team.test.yandex.ru/egorova'
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    User(title='Екатерина Егорова', login='egorova'),
                ]
            )
        }
        cassette_name = 'staff_user_biz_successful.yaml'
        with patch.object(StaffUserWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(self.response_check(url,
                                                             expected_data=expected_data,
                                                             cassette_name=cassette_name,
                                                             ))

    @override_settings(**BIZ_STAFF_SETTINGS)
    def test_staff_user_wrong_login_fail(self):
        url = 'https://team.test.yandex.ru/some_strange_thing'
        expected_data = {}
        cassette_name = 'staff_user_biz_wrong_login_fail.yaml'
        with patch.object(StaffUserWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(self.response_check(url,
                                                             expected_data=expected_data,
                                                             cassette_name=cassette_name,
                                                             ))

    @override_settings(**BIZ_STAFF_SETTINGS)
    def test_staff_user_api_error_fail(self):
        url = 'https://team.test.yandex.ru/smosker'
        expected_data = {}
        cassette_name = 'staff_user_biz_api_error_fail.yaml'
        with patch.object(StaffUserWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(self.response_check(url,
                                                             expected_data=expected_data,
                                                             cassette_name=cassette_name,
                                                             ))
