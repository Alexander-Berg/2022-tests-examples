from asynctest import patch

from intranet.magiclinks.src.links.dto import List, String, Image
from intranet.magiclinks.src.links.workers.intranet import femida_candidate

from ..base import BaseWorkerTestCase


class FemidaCandidateWorkerTestCase(BaseWorkerTestCase):

    worker_class = femida_candidate.Worker
    class_instance = worker_class('test', None, None)
    worker_class_file = 'femida_candidate'

    def test_femida_parse_url(self):
        hostname_match = 'femida.yandex-team.ru'
        url = 'https://femida.yandex-team.ru/candidates/87931278/'
        path_data = {
            'id': '87931278',
            'resource': 'candidates',
        }
        self.parse_url(url, hostname_match, path_data)

    def test_femida_candidate_success(self):
        url = 'https://femida.yandex-team.ru/candidates/87809072'
        expected_data = {
            url: List(
                ttl=604800,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text='Femida',
                    ),
                    String(value='Александр Пушкин'),
                ],
            ),
        }
        cassette_name = 'femida_candidate_success.yaml'
        with patch.object(femida_candidate.Worker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(self.response_check(
                url,
                expected_data=expected_data,
                cassette_name=cassette_name,
            ))

    def test_femida_candidate_fail(self):
        url = 'https://femida.yandex-team.ru/candidates/87809072234234'
        cassette_name = 'femida_candidate_fail.yaml'
        with patch.object(femida_candidate.Worker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(self.fail_response_check(
                url,
                cassette_name=cassette_name,
            ))

    def test_femida_fail_with_wrong_id(self):
        url = 'https://femida.yandex-team.ru/candidates/8780907fd'
        expected_data = {}
        with patch.object(femida_candidate.Worker, 'get_headers', return_value={'test': 'some'}):
            self.loop.run_until_complete(self.response_check(
                url,
                expected_data=expected_data,
            ))
