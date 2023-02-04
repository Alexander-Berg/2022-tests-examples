from asynctest import patch

from ..base import BaseWorkerTestCase
from intranet.magiclinks.src.links.dto import List, String, Image, User
from intranet.magiclinks.src.links.workers.intranet.femida import Worker as FemidaWorker


class FemidaWorkerTestCase(BaseWorkerTestCase):
    worker_class = FemidaWorker
    class_instance = worker_class('test', None, None)
    worker_class_file = 'femida'

    def test_femida_parse_url(self):
        hostname_match = 'femida.yandex-team.ru'

        urls_data = (
            ('https://femida.yandex-team.ru/interviews/27105',
             {'id': '27105',
              'resource': 'interviews'}
             ),
            ('https://femida.yandex-team.ru/problems/4153',
             {'id': '4153',
              'resource': 'problems'}
             ),
            ('https://femida.yandex-team.ru/vacancies/50492/',
             {'id': '50492',
              'resource': 'vacancies'}
             ),
        )
        for url, path_data in urls_data:
            self.parse_url(url, hostname_match, path_data)

    def test_femida_problem_success(self):
        url = 'https://femida.yandex-team.ru/problems/3980'

        expected_data = {
            url: List(
                ttl=86400,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Femida',
                    ),
                    String(value='Свертка списка в диапазоны'),
                    User(login='xornet'),
                    String(value='('),
                    String(value='130', color='#ff0000'),
                    String(value='189', color='#D49300'),
                    String(value='126', color='#049657'),
                    String(value=')'),
                ]
            )
        }
        cassette_name = 'femida_problem_success.yaml'
        with patch.object(FemidaWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(self.response_check(url,
                                                             expected_data=expected_data,
                                                             cassette_name=cassette_name,
                                                             ))

    def test_femida_problem_fail(self):
        url = 'https://femida.yandex-team.ru/problems/398034534534'
        cassette_name = 'femida_problem_fail.yaml'
        with patch.object(FemidaWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(self.fail_response_check(url,
                                                                  cassette_name=cassette_name,
                                                                  ))

    def test_femida_interview_hire_4_success(self):
        url = 'https://femida.yandex-team.ru/interviews/27105'

        expected_data = {
            url: List(
                ttl=172800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Femida',
                    ),
                    String(value='Никита Быков'),
                    String(value=' - секция'),
                    String(value='Управление проектами'),
                    String(value='(Завершена)', color='#425EC3'),
                    Image(
                        src=self.class_instance.interview_grade_icons[4],
                        text='4',
                    )
                ]
            )
        }
        cassette_name = 'femida_interview_hire_4_success.yaml'
        with patch.object(FemidaWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(self.response_check(url,
                                                             expected_data=expected_data,
                                                             cassette_name=cassette_name,
                                                             ))

    def test_femida_interview_no_hire_success(self):
        url = 'https://femida.yandex-team.ru/interviews/27108'

        expected_data = {
            url: List(
                ttl=172800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Femida',
                    ),
                    String(value='Никита Быков'),
                    String(value=' - секция'),
                    String(value='Аналитика'),
                    String(value='(Завершена)', color='#425EC3'),
                    Image(
                        src=self.class_instance.interview_grade_icons[0],
                        text='0',
                    )
                ]
            )
        }
        cassette_name = 'femida_interview_no_hire_success.yaml'
        with patch.object(FemidaWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(self.response_check(url,
                                                             expected_data=expected_data,
                                                             cassette_name=cassette_name,
                                                             ))

    def test_femida_interview_fail(self):
        url = 'https://femida.yandex-team.ru/interviews/27108453453'
        cassette_name = 'femida_interview_fail.yaml'
        with patch.object(FemidaWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(self.fail_response_check(url,
                                                                  cassette_name=cassette_name,
                                                                  ))

    def test_femida_vacancy_success(self):
        url = 'https://femida.yandex-team.ru/vacancies/50492/'

        expected_data = {
            url: List(
                ttl=1800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Femida',
                    ),
                    String(value='Делаем оффер', color='#F7A014'),
                    String(value='2017-05-24 - доработки первой волны'),
                    User(login='kiparis')
                ]
            )
        }
        cassette_name = 'femida_vacancy_success.yaml'
        with patch.object(FemidaWorker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(self.response_check(url,
                                                             expected_data=expected_data,
                                                             cassette_name=cassette_name,
                                                             ))

    def test_femida_fail_with_wrong_id(self):
        urls = ('https://femida.yandex-team.ru/interviews/27adsf108',
                'https://femida.yandex-team.ru/problems/3980!',
                'https://femida.yandex-team.ru/vacancies/5049223423423Е/'
                )
        expected_data = {}
        for url in urls:
            with patch.object(FemidaWorker, 'get_headers', return_value={'test': 'some'}):
                self.loop.run_until_complete(self.response_check(url,
                                                             expected_data=expected_data,
                                                             ))
