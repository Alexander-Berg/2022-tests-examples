from ..base import BaseWorkerTestCase
from intranet.magiclinks.src.links.dto import List, String, Image, User
from intranet.magiclinks.src.links.workers.intranet.abc_duty import Worker as ABCWorker


class ABCWorkerTestCase(BaseWorkerTestCase):
    worker_class = ABCWorker
    worker_class_file = 'abc_duty'

    def test_abc_parse_url(self):
        group_name = 'role_id'
        urls_data = (
            ('https://abc.yandex-team.ru/services/magiclinks/duty/?role=123', True, '123'),
            ('https://abc.yandex-team.ru/services/tools/', False, None),
            ('https://abc.yandex-team.ru/services/tools', False, None),
            ('https://abc.yandex-team.ru/services/tools/duty/?role=test', False, None),
        )
        for url, should_parse, role_match in urls_data:
            self.parse_url(url, 'abc.yandex-team.ru', query_data={group_name: role_match}, should_parse=should_parse)

    def test_abc_duty_success(self):
        url = 'https://abc.yandex-team.ru/services/abc/duty/?role=1543'
        expected_data = {
            url: List(
                ttl=1800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='ABC Duty',
                    ),
                    String(value='Scrum'),
                    User(login='smosker'),
                    String(value='(до 15-го декабря)', color='#999999'),
                ]
            )
        }
        cassette_name = 'abc_duty_success.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_abc_duty_multiple_success(self):
        url = 'https://abc.yandex-team.ru/services/direct-app-duty/duty?role=2516'
        expected_data = {
            url: List(
                ttl=1800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='ABC Duty',
                    ),
                    String(value='direct-app-duty'),
                    List(
                        ttl=1800,
                        value=[
                            User(login='robot-direct-admin'),
                            User(login='dimitrovsd'),
                        ],
                        separator=', '
                    ),
                    String(value='(до 16-го декабря)', color='#999999'),
                ]
            )
        }
        cassette_name = 'abc_duty_multiple_success.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))


    def test_abc_duty_no_duty_success(self):
        url = 'https://abc.yandex-team.ru/services/abc/duty/?role=1543'
        expected_data = {
            url: List(
                ttl=1800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='ABC Duty',
                    ),
                    String(value='Scrum'),
                    String(value='Не назначен', color='red'),
                    String(value='(до 15-го декабря)', color='#999999'),
                ]
            )
        }
        cassette_name = 'abc_duty_no_duty_success.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_abc_duty_wrong_id_failed(self):
        url = 'https://abc.yandex-team.ru/services/abc/duty/?role=99999999'
        cassette_name = 'abc_duty_wrong_id_failed.yaml'
        self.loop.run_until_complete(self.fail_response_check(url,
                                                              cassette_name=cassette_name,
                                                              completed=True
                                                              ))
