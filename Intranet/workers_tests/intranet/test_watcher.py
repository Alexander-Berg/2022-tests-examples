from asynctest import patch

from ..base import BaseWorkerTestCase
from intranet.magiclinks.src.links.dto import List, String, Image, User
from intranet.magiclinks.src.links.workers.intranet.watcher import Worker as WatcherWorker


class ABCWorkerTestCase(BaseWorkerTestCase):
    worker_class = WatcherWorker
    worker_class_file = 'watcher'

    def test_watcher_parse_url(self):
        urls_data = (
            ('https://abc.yandex-team.ru/services/1851/duty2/', False, None),
            ('https://abc.yandex-team.ru/services/1851/duty2/30224', True, '30224'),
            ('https://abc.yandex-team.ru/services/checkouter/duty2/30224', True, '30224'),
            ('https://abc.yandex-team.ru/services/tools/', False, None),
            ('https://abc.yandex-team.ru/services/tools/duty/?role=123', False, None),
        )
        for url, should_parse, schedule_match in urls_data:
            self.parse_url(
                url, 'abc.yandex-team.ru', path_data={'schedule_id': schedule_match}, should_parse=should_parse
            )

    def test_watcher_success(self):
        url = 'https://abc.yandex-team.ru/services/zanartestservice000/duty2/30213'
        expected_data = {
            url: List(
                ttl=1800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Watcher',
                    ),
                    String(value='Дежурим 24х7, одна смена - 7 дней'),
                    List(
                        ttl=1800,
                        value=[
                            User(login='robot-cat-matroskin'),
                        ],
                        separator=', ',
                    ),
                    String(value='(до 10:00 18-го апреля)', color='#999999'),
                ]
            )
        }
        cassette_name = 'watcher_success.yaml'
        with patch.object(WatcherWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(self.response_check(url,
                                                             expected_data=expected_data,
                                                             cassette_name=cassette_name,
                                                             ))

    def test_watcher_primary_backup_success(self):
        url = 'https://abc.yandex-team.ru/services/zanartestservice000/duty2/30325'
        expected_data = {
            url: List(
                ttl=1800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Watcher',
                    ),
                    String(value='test-new-duty-regress-08-04-logic'),
                    List(
                        ttl=1800,
                        value=[
                            List(
                                ttl=1800,
                                value=[
                                    User(login='robot-cat-bayun'),
                                    String(value='(primary)'),
                                ],
                                separator=' ',
                            ),
                            List(
                                ttl=1800,
                                value=[
                                    User(login='pixel'),
                                    String(value='(backup)'),
                                ],
                                separator=' ',
                            ),
                        ],
                        separator=', ',
                    ),
                    String(value='(до 14-го апреля)', color='#999999'),
                ]
            )
        }
        cassette_name = 'watcher_primary_backup_success.yaml'
        with patch.object(WatcherWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(self.response_check(url,
                                                             expected_data=expected_data,
                                                             cassette_name=cassette_name,
                                                             ))

    def test_watcher_nobody_on_duty(self):
        url = 'https://abc.yandex-team.ru/services/zanartestservice000/duty2/30214'
        expected_data = {
            url: List(
                ttl=1800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Watcher',
                    ),
                    String(value='Дежурят одновременно backup и primary (on-call) по 5 дней'),
                    String(value='Никто не дежурит', color='red'),
                    String(value='(до 18-го апреля)', color='#999999'),
                ]
            )
        }
        cassette_name = 'watcher_nobody_on_duty.yaml'
        with patch.object(WatcherWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(self.response_check(url,
                                                             expected_data=expected_data,
                                                             cassette_name=cassette_name,
                                                             ))

    def test_watcher_wrong_id_failed(self):
        url = 'https://abc.yandex-team.ru/services/zanartestservice000/duty2/999999999'
        cassette_name = 'watcher_wrong_id_failed.yaml'
        with patch.object(WatcherWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(self.fail_response_check(url,
                                                                  cassette_name=cassette_name,
                                                                  completed=False,
                                                                  ))
