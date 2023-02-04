from django.test.utils import override_settings

import blackbox
from asynctest import patch

from ..base import BaseWorkerTestCase
from intranet.magiclinks.src.links.workers.biz.startrek_issue import Worker as StartrekIssueWorker
from intranet.magiclinks.src.links.dto import List, String, Image, User

BIZ_STARTREK_SETTINGS = {
    'STARTREK_DEMO_API_URL': 'https://tracker-demo-api.test.tracker.yandex.net',
    'STARTREK_TVM2_CLIENT_ID': '2000181',
}


class StartrekIssueWorkerTestCase(BaseWorkerTestCase):
    worker_class = StartrekIssueWorker
    worker_class_file = 'startrek_issue'

    def test_startrek_issue_parse_url(self):
        group_name = 'key'
        urls_data = (
            ('https://tracker.test.yandex.ru/WIKI-9960',
             'WIKI-9960', 'tracker.test.yandex.ru'),
            ('https://tracker.test.yandex.com/WIKI-9960',
             'WIKI-9960', 'tracker.test.yandex.com'),
            ('https://demo.tracker.test.yandex.com/WIKI-9960',
             'WIKI-9960', 'demo.tracker.test.yandex.com'),
        )
        for url, path_match, hostname_match in urls_data:
            self.parse_url(url, hostname_match, {group_name: path_match})

    def test_get_map_uids_success(self):
        tickets_map = {'TEST-5': {'some_value': 'test'},
                       'TEST-6': {'assignee': {'id': '12345'}},
                       'TEST-9': {'assignee': {'id': '123'}},
                       'TEST-12': {'assignee': {'id': '12345'}},
                       }

        worker = self.workers[self.worker_class_file]
        self.assertEqual(worker.map_uids(tickets_map), {'12345': {'TEST-6', 'TEST-12'},
                                                        '123': {'TEST-9'}})

    def test_add_logins_to_response_success(self):
        tickets_map = {'TEST-5': {'some_value': 'test'},
                       'TEST-6': {'assignee': {'id': '12345'}},
                       'TEST-9': {'assignee': {'id': '123'}},
                       'TEST-12': {'assignee': {'id': '12345'}},
                       }
        assignee_blackbox_info = {'users': [{'id': '123', 'login': 'test_login'},
                                            {'id': '12345', 'login': 'another_login'},
                                            ]
                                  }

        worker = self.workers[self.worker_class_file]
        assignee_tickets = worker.map_uids(tickets_map)
        result = worker.add_logins_to_response(tickets_map,
                                               assignee_tickets,
                                               assignee_blackbox_info)

        expected_result = {'TEST-5': {'some_value': 'test'},
                           'TEST-6': {'assignee': {'id': 'another_login'}},
                           'TEST-9': {'assignee': {'id': 'test_login'}},
                           'TEST-12': {'assignee': {'id': 'another_login'}},
                           }
        self.assertEqual(result, expected_result)

    @override_settings(**BIZ_STARTREK_SETTINGS)
    def test_startrek_biz_issue_success(self):
        url = 'https://tracker.test.yandex.ru/TEST-1'
        login = 'test_login'
        expected_data = {
            url: List(
                ttl=60,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Startrek',
                    ),
                    String(value='TEST-1',
                           strike=False,
                           action={
                               "event": "click",
                               "type": "halfscreenpreview",
                               "url": 'https://tracker.test.yandex.ru/issueCard/TEST-1'
                           }),
                    String(value='Открыт', color='#999'),
                    String(value=u'Поменять формат ответа для тикетов st'),
                    User(login=login),
                ]
            )
        }
        cassette_name = 'startrek_issue_biz_success.yaml'
        with patch.object(blackbox.JsonBlackbox, 'userinfo') as blackbox_mock:
            blackbox_mock.return_value = {
                'users': [
                    {'id': '54321',
                     'login': login,
                     },
                ]
            }
            with patch('intranet.magiclinks.src.links.workers.biz.base.get_service_ticket', return_value='ticket'):
                with patch.object(StartrekIssueWorker, 'get_headers', return_value={'test': 'some'}):
                    self.loop.run_until_complete(
                        self.response_check(
                            url,
                            expected_data=expected_data,
                            cassette_name=cassette_name,
                        )
                    )
            blackbox_mock.assert_called_once_with(
                uid={'54321': {'TEST-1'}},
                userip='192.168.1.1',
                headers={'X-Ya-Service-Ticket': 'ticket'},
                attributes=1017,
            )

    @override_settings(**BIZ_STARTREK_SETTINGS)
    def test_startrek_biz_demo_issue_success(self):
        url = 'https://demo.tracker.test.yandex.ru/TEST-1'
        login = 'test_login'
        expected_data = {
            url: List(
                ttl=60,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Startrek',
                    ),
                    String(value='TEST-1',
                           strike=False,
                           action={
                               "event": "click",
                               "type": "halfscreenpreview",
                               "url": 'https://demo.tracker.test.yandex.ru/issueCard/TEST-1'
                           }),
                    String(value='Открыт', color='#999'),
                    String(value=u'Поменять формат ответа для тикетов st'),
                    String(value='- test_login'),
                ]
            )
        }
        cassette_name = 'startrek_issue_biz_demo_success.yaml'
        with patch.object(blackbox.JsonBlackbox, 'userinfo') as blackbox_mock:
            blackbox_mock.return_value = {
                'users': [
                    {'id': '54321',
                     'login': login,
                     },
                ]
            }
            with patch('intranet.magiclinks.src.links.workers.biz.base.get_service_ticket', return_value='ticket'):
                with patch.object(StartrekIssueWorker, 'get_headers', return_value={'test': 'some'}):
                    self.loop.run_until_complete(
                        self.response_check(
                            url,
                            expected_data=expected_data,
                            cassette_name=cassette_name,
                        )
                    )
            blackbox_mock.assert_called_once_with(
                uid={'54321': {'TEST-1'}},
                userip='192.168.1.1',
                headers={'X-Ya-Service-Ticket': 'ticket'},
                attributes=1017,
            )
