from asynctest import patch, MagicMock

from intranet.magiclinks.src.links.workers.biz.inflector import Worker as InflectorBizWorker
from intranet.magiclinks.src.links.workers.intranet.inflector import Gender, Case

from ..base import BaseWorkerTestCase
from ..intranet.test_inflector import make_expected


PREFIX, PREFIX_ENCODED = 'кого', '%D0%BA%D0%BE%D0%B3%D0%BE'
MOCK_INFLECT = {'first_name': 'Никиту', 'last_name': 'Глебова'}
MOCK_DIRECTORY = {
    'gender': 'male',
    'is_dismissed': False,
    'name': {'first': 'Никита', 'last': 'Глебов'}
}


class InflectorBizWorkerTestCase(BaseWorkerTestCase):
    worker_class = InflectorBizWorker
    worker_class_file = 'inflector'

    @patch.object(InflectorBizWorker, 'make_request_to_directory', return_value=MOCK_DIRECTORY)
    @patch.object(InflectorBizWorker, 'request_inflect', return_value=MOCK_INFLECT)
    def test_inflector_biz__success(self, inflect: MagicMock, directory: MagicMock):
        url = f'data://inflector/glebov-n?lang=ru&prefix={PREFIX_ENCODED}'
        expected_data = make_expected(url, first_name='Никиту', last_name='Глебова')
        check = self.response_check(url, expected_data=expected_data)

        self.loop.run_until_complete(check)
        directory.assert_called_once_with(login='glebov-n')
        inflect.assert_called_once_with('Никита', 'Глебов', gender=Gender.MALE, case=Case.ACCUSATIVE)

    @patch.object(InflectorBizWorker, 'make_request_to_directory', side_effect=Exception)
    @patch.object(InflectorBizWorker, 'request_inflect')
    def test_inflector__invalid_login(self, inflect: MagicMock, directory: MagicMock):
        url = 'data://inflector/invalid-login'

        expected_data = make_expected(url, first_name='invalid-login', last_name='', completed=False)
        check = self.response_check(url, expected_data=expected_data)
        self.loop.run_until_complete(check)
        inflect.assert_not_called()

    @patch.object(InflectorBizWorker, 'make_request_to_directory')
    @patch.object(InflectorBizWorker, 'request_inflect', return_value=MOCK_INFLECT)
    def test_inflector__dismissed(self, inflect: MagicMock, directory: MagicMock):
        url = f'data://inflector/glebov-n?lang=ru&prefix={PREFIX_ENCODED}'

        for dismissed in [True, False]:
            directory.return_value = MOCK_DIRECTORY | {'is_dismissed': dismissed}
            expected_data = make_expected(url, first_name='Никиту', last_name='Глебова', is_dismissed=dismissed)
            check = self.response_check(url, expected_data=expected_data)
            self.loop.run_until_complete(check)
