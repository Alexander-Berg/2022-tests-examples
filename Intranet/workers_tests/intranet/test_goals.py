from asynctest import patch
from freezegun import freeze_time

from ..base import BaseWorkerTestCase
from intranet.magiclinks.src.links.dto import List, String, Image, User
from intranet.magiclinks.src.links.workers.intranet.goals import Worker as GoalsWorker


class GoalsTestCase(BaseWorkerTestCase):
    worker_class = GoalsWorker
    worker_class_file = 'goals'
    maxDiff = None

    def test_goals_parse_url(self):
        group_name = 'id'
        hostname_match = 'goals.yandex-team.ru'
        urls_data = (('https://goals.yandex-team.ru/filter?user=5726&goal=2974',
                      {0: '/filter'}, '2974'),
                     ('https://goals.yandex-team.ru/filter?user=5726&goal=2974&test=3',
                      {0: '/filter'}, '2974'),
                     ('https://goals.yandex-team.ru/okr?user=5726&goal=2974&test=3',
                      {0: '/okr'}, '2974'),
                     ('https://goals.yandex-team.ru/compilations/company?login=ueueueueue&goal=2974',
                      {0: '/compilations/company'}, '2974'),
                     ('https://goals.yandex-team.ru/filter?user=5726&goal=2974/',
                      {0: '/filter'}, '2974'),
                     ('https://goals.yandex-team.ru/filter?goal=2974',
                      {0: '/filter'}, '2974'),
                     ('https://goals.yandex-team.ru/filter?goal=2974/',
                      {0: '/filter'}, '2974'),
                     ('https://goals.yandex-team.ru/filter?goal=2974&user=5726',
                      {0: '/filter'}, '2974'),
                     ('https://goals.yandex-team.ru/filter?goal=2974&user=5726/',
                      {0: '/filter'}, '2974'),
                     )
        for url, path_data, query_data in urls_data:
            self.parse_url(url, hostname_match, path_data, query_data={group_name: query_data})

    def test_goals_without_deadline_success(self):
        url = 'https://goals.yandex-team.ru/filter?user=5726&importance=1,2,0,3&goal=2974'

        expected_data = {
            url: List(
                ttl=2592000,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Goals',
                    ),
                    String(value='Этушка: новый интерфейс'),
                    String(value=' ', hint='Casual'),
                    String(value='Отменена', color='#888C91'),
                    String(value='(Без срока)', color='#888C91'),
                    User(login='avrudakova'),
                ]
            )
        }
        cassette_name = 'goals_without_deadline_success.yaml'
        with patch.object(GoalsWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(
                self.response_check(url, expected_data=expected_data, cassette_name=cassette_name)
            )

    def test_goals_with_deadline_behind_success(self):
        url = 'https://goals.yandex-team.ru/filter?goal=1466&importance=1,2,0,3'

        expected_data = {
            url: List(
                ttl=300,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Goals',
                    ),
                    String(value='Сопровождение проекта Красной розы'),
                    String(value='👑', hint='Crown'),
                    String(value='По плану', color='#07a300'),
                    String(value='(2016 Q4)', color='#ff0000'),
                    User(login='shulgin'),
                ]
            )
        }
        cassette_name = 'goals_without_deadline_behind_success.yaml'
        with patch.object(GoalsWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(
                self.response_check(url, expected_data=expected_data, cassette_name=cassette_name)
            )

    @freeze_time('2010-06-22')
    def test_goals_with_deadline_ahead_success(self):
        url = 'https://goals.yandex-team.ru/filter?group=49788&importance=2,1,0&goal=14537'

        expected_data = {
            url: List(
                ttl=300,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Goals',
                    ),
                    String(value='Универсальный саджест для интранет-сервисов'),
                    String(value=' ', hint='Casual'),
                    String(value='Есть риски', color='#f5ab19'),
                    String(value='(Q4)', color='#888C91'),
                    User(login='avrudakova'),
                ]
            )
        }
        cassette_name = 'goals_without_deadline_ahead_success.yaml'
        with patch.object(GoalsWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(
                self.response_check(url, expected_data=expected_data, cassette_name=cassette_name)
            )

    def test_goals_without_goal_failed(self):
        url = 'https://goals.yandex-team.ru/filter?group=49788&importance=2,1,0'
        expected_data = {}
        cassette_name = 'goals_without_goal_failed.yaml'
        with patch.object(GoalsWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(
                self.response_check(url, expected_data=expected_data, cassette_name=cassette_name)
            )

    def test_goals_with_wrong_goal_failed(self):
        url = 'https://goals.yandex-team.ru/filter?group=49788&importance=2,1,0&goal=14532342342347'
        cassette_name = 'goals_with_wrong_goal_failed.yaml'
        with patch.object(GoalsWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(
                self.fail_response_check(url, cassette_name=cassette_name)
            )
