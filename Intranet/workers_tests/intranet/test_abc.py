from ..base import BaseWorkerTestCase
from intranet.magiclinks.src.links.dto import List, String, Image, User
from intranet.magiclinks.src.links.workers.intranet.abc import Worker as ABCWorker


class ABCWorkerTestCase(BaseWorkerTestCase):
    worker_class = ABCWorker
    worker_class_file = 'abc'

    def test_abc_parse_url(self):
        group_name = 'service'
        urls_data = (
            ('https://abc.yandex-team.ru/services/magiclinks/', 'magiclinks', 'abc.yandex-team.ru'),
            ('https://abc.yandex-team.ru/services/tools', 'tools', 'abc.yandex-team.ru'),
        )
        for url, path_match, hostname_match in urls_data:
            self.parse_url(url, hostname_match, {group_name: path_match})

    def test_abc_development_success(self):
        url = 'https://abc.yandex-team.ru/services/tools/'
        expected_data = {
            url: List(
                ttl=86400,
                value=[
                    Image(
                        src=self.worker_class.PLAY,
                        text='ABC',
                    ),
                    String(value='Внутренние и b2b сервисы'),
                    User(login='puroman'),
                    String(value='(59 участников)', color='#999999'),
                ]
            )
        }
        cassette_name = 'abc_development_success.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_abc_supported_success(self):
        url = 'https://abc.yandex-team.ru/services/viewer/'
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.PAUSE,
                        text='ABC',
                    ),
                    String(value='Вьюверы Поиска'),
                    User(login='alexkoshelev'),
                    String(value='(4 участника)', color='#999999'),
                ]
            )
        }
        cassette_name = 'abc_supported_success.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_abc_closed_success(self):
        url = 'https://abc.yandex-team.ru/services/smm-tool'
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.STOP,
                        text='ABC',
                    ),
                    String(value='SMM (deleted)'),
                    User(login='gnoma'),
                    String(value='(6 участников)', color='#999999'),
                ]
            )
        }
        cassette_name = 'abc_closed_success.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_abc_wrong_slug_failed(self):
        url = 'https://abc.yandex-team.ru/services/magiclinkGH/'
        cassette_name = 'abc_wrong_slug_failed.yaml'
        self.loop.run_until_complete(self.fail_response_check(url,
                                                              cassette_name=cassette_name,
                                                              ))

    def test_abc_wrong_path_failed(self):
        url = 'https://abc.yandex-team.ru/services/magiclinks/test'
        expected_data = {}
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         ))

    def test_abc_without_owner_success(self):
        url = 'https://abc.yandex-team.ru/services/meta_other/'
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.PAUSE,
                        text='ABC',
                    ),
                    String(value='Другие сервисы'),
                    String(value='(Нет участников)', color='#999999'),
                ]
            )
        }
        cassette_name = 'abc_without_owner_success.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))

    def test_abc_without_internal_access(self):
        url = 'https://abc.yandex-team.ru/services/meta_other/'
        expected_data = {
            url: List(
                ttl=60,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text=self.worker_class.FAVICON_TEXT,
                    ),
                    String(value='https://abc.yandex-team.ru/services/meta_other/'),
                ],
                completed=False,
            )
        }
        cassette_name = 'abc_without_internal_access.yaml'
        self.loop.run_until_complete(self.response_check(url,
                                                         expected_data=expected_data,
                                                         cassette_name=cassette_name,
                                                         ))
