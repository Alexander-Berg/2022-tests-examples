from asynctest import patch

from ..base import BaseWorkerTestCase
from intranet.magiclinks.src.links.dto import List, String, Image, User
from intranet.magiclinks.src.links.workers.intranet.startrek_issue import Worker as StartrekIssueWorker


class StartrekIssueWorkerTestCase(BaseWorkerTestCase):
    worker_class = StartrekIssueWorker
    worker_class_file = 'startrek_issue'

    def test_startrek_issue_parse_url(self):
        hostname_match = 'st.yandex-team.ru'
        group_name = 'key'
        urls_data = (
            ('https://st.yandex-team.ru/WIKI-9960',
             'WIKI-9960'),
        )
        for url, path_match in urls_data:
            self.parse_url(url, hostname_match, {group_name: path_match})

    def test_startrek_issue_success(self):
        url = 'https://st.yandex-team.ru/WIKI-9960'
        expected_data = {
            url: List(
                ttl=60,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Startrek',
                    ),
                    String(value='WIKI-9960',
                           strike=False,
                           action={
                               "event": "click",
                               "type": "halfscreenpreview",
                               "url": 'https://st.yandex-team.ru/issueCard/WIKI-9960'
                           }),
                    String(value='Открыт', color='#999'),
                    String(value=u'Поменять формат ответа для тикетов st'),
                    User(login='smosker'),
                ]
            )
        }
        cassette_name = 'startrek_issue_success.yaml'
        with patch.object(StartrekIssueWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(self.response_check(url,
                                                             expected_data=expected_data,
                                                             cassette_name=cassette_name,
                                                             ))

    def test_startrek_issue_strike_english_success(self):
        url = 'https://st.yandex-team.ru/WIKI-9960'
        expected_data = {
            url: List(
                ttl=60,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Startrek',
                    ),
                    String(value='WIKI-9960',
                           color='#999',
                           strike=True,
                           action={
                               "event": "click",
                               "type": "halfscreenpreview",
                               "url": 'https://st.yandex-team.ru/issueCard/WIKI-9960'
                           }),
                    String(value='Resolved', color='#999'),
                    String(value=u'Поменять формат ответа для тикетов st'),
                    User(login='smosker'),
                ]
            )
        }
        cassette_name = 'startrek_issue_english_strike_success.yaml'
        with patch.object(StartrekIssueWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(self.response_check(url,
                                                             expected_data=expected_data,
                                                             cassette_name=cassette_name,
                                                             ))

    def test_startrek_issue_english_success(self):
        url = 'https://st.yandex-team.ru/WIKI-9960'
        expected_data = {
            url: List(
                ttl=60,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Startrek',
                    ),
                    String(value='WIKI-9960',
                           strike=False,
                           action={
                               "event": "click",
                               "type": "halfscreenpreview",
                               "url": 'https://st.yandex-team.ru/issueCard/WIKI-9960'
                           }),
                    String(value='Open', color='#999'),
                    String(value=u'Поменять формат ответа для тикетов st'),
                    User(login='smosker'),
                ]
            )
        }
        cassette_name = 'startrek_issue_english_success.yaml'
        with patch.object(StartrekIssueWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(self.response_check(url,
                                                             expected_data=expected_data,
                                                             cassette_name=cassette_name,
                                                             ))

    def test_startrek_issue_fail_error_response(self):
        url = 'https://st.yandex-team.ru/WIKI-9960123123'
        expected_data = {
            url: List(
                ttl=60,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Startrek',
                    ),
                    String(value='WIKI-9960123123'),
                ],
                completed=False,
            )
        }
        cassette_name = 'startrek_issue_fail_error_response.yaml'
        with patch.object(StartrekIssueWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(self.response_check(url,
                                                             expected_data=expected_data,
                                                             cassette_name=cassette_name,
                                                             ))

    def test_startrek_issue_fail_blank_response(self):
        url = 'https://st.yandex-team.ru/WIKI-12345'
        expected_data = {
            url: List(
                ttl=60,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Startrek',

                    ),
                    String(value='WIKI-12345'),
                ],
                completed=False,
            )
        }
        cassette_name = 'startrek_issue_blank_response.yaml'
        with patch.object(StartrekIssueWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(self.response_check(url,
                                                             expected_data=expected_data,
                                                             cassette_name=cassette_name,
                                                             ))

    def test_startrek_issue_redirect_success(self):
        url = 'https://st.yandex-team.ru/QLOUD-1494'
        expected_data = {
            url: List(
                ttl=60,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Startrek',
                    ),
                    String(value='QLOUDDEV-905',
                           color='#999',
                           strike=True,
                           action={
                               "event": "click",
                               "type": "halfscreenpreview",
                               "url": 'https://st.yandex-team.ru/issueCard/QLOUDDEV-905'
                           }),
                    String(value='Закрыт', color='#999'),
                    String(value=u'Добавить proxy_redirect в секцию компонента HTTP_BALANCER в конфиг nginx балансера'),
                    User(login='kozhapenko'),
                ]
            )
        }
        cassette_name = 'startrek_issue_redirect_success.yaml'
        with patch.object(StartrekIssueWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(self.response_check(url,
                                                             expected_data=expected_data,
                                                             cassette_name=cassette_name,
                                                             ))
