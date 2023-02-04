from asynctest import patch, MagicMock

from ids.exceptions import BackendError

from intranet.magiclinks.src.links.workers.intranet.inflector import (
    Worker as InflectorWorker,
    Gender,
    Case,
    List,
    Inflect,
)
from ..base import BaseWorkerTestCase


PREFIX, PREFIX_ENCODED = 'кого', '%D0%BA%D0%BE%D0%B3%D0%BE'
MOCK_INFLECT = {'first_name': 'Никиту', 'last_name': 'Глебова'}
MOCK_STAFF = {
            'personal': {'gender': 'male'},
            'official': {'is_dismissed': False},
            'name': {
                'first': {'ru': 'Никита', 'en': 'Nikita'},
                'last': {'ru': 'Глебов', 'en': 'Glebov'},
            }
        }


def make_expected(url: str, first_name: str, last_name: str, is_dismissed=False, completed=True) -> dict[str, List]:
    return {
        url: List(
            completed=completed,
            ttl=InflectorWorker.TTL_MAP['default'] if completed else InflectorWorker.TTL_MAP['fail'],
            value=[
                Inflect(name=first_name, last_name=last_name, is_dismissed=is_dismissed)
            ],
        )
    }


class InflectorWorkerTestCase(BaseWorkerTestCase):
    worker_class = InflectorWorker
    worker_class_file = 'inflector'

    def test_inflector__parse_url(self):
        urls_data = [
            'data://inflector/user?lang=ru&prefix=кто',
            'data://inflector/user?lang=ru',
            f'data://inflector/user?prefix={PREFIX_ENCODED}',
            'data://inflector/user?prefix=кто&lang=ru',
            'data://inflector/user',
            'http://inflector/user',
            'data://inflector/login',
            'data://inflector/user?lang=ru&any=param&no-have=sense',
        ]

        for url in urls_data:
            self.parse_url(url, hostname_match='inflector')

    def test_inflector__parse_url__invalid(self):
        urls_data = [
            'data://inflector/?lang=ru',  # no login
            'data://other/user',  # other host
            'data://inflector22/user',  # other host
            'data://inflector/inv@lid-login',  # invalid login
            'data://inflector/',  # empty
        ]

        for url in urls_data:
            self.assertRaises(AttributeError, self.parse_url, url, hostname_match='inflector')

    @patch.object(InflectorWorker, 'request_staff', return_value=MOCK_STAFF)
    @patch.object(InflectorWorker, 'request_inflect', return_value=MOCK_INFLECT)
    def test_inflector__success(self, inflect: MagicMock, staff: MagicMock):
        url = f'data://inflector/glebov-n?lang=ru&prefix={PREFIX_ENCODED}'
        expected_data = make_expected(url, first_name='Никиту', last_name='Глебова')
        check = self.response_check(url, expected_data=expected_data)

        self.loop.run_until_complete(check)
        inflect.assert_called_once_with('Никита', 'Глебов', gender=Gender.MALE, case=Case.ACCUSATIVE)
        staff.assert_called_once_with(login='glebov-n')

    @patch.object(InflectorWorker, 'request_staff', return_value=MOCK_STAFF)
    @patch.object(InflectorWorker, 'request_inflect', return_value=MOCK_INFLECT)
    def test_inflector__invalid_query(self, inflect: MagicMock, staff: MagicMock):
        # only login
        url = 'data://inflector/glebov-n'
        expected_data = make_expected(url, first_name='Никита', last_name='Глебов')
        check = self.response_check(url, expected_data=expected_data)
        self.loop.run_until_complete(check)

        # invalid lang
        url = 'data://inflector/glebov-n?lang=XX'
        expected_data = make_expected(url, first_name='Никита', last_name='Глебов')
        check = self.response_check(url, expected_data=expected_data)
        self.loop.run_until_complete(check)

        # lang = en
        url = 'data://inflector/glebov-n?lang=en'
        expected_data = make_expected(url, first_name='Nikita', last_name='Glebov')
        check = self.response_check(url, expected_data=expected_data)
        self.loop.run_until_complete(check)

        # invalid case
        url = 'data://inflector/glebov-n?case=именительный'
        expected_data = make_expected(url, first_name='Никита', last_name='Глебов')
        check = self.response_check(url, expected_data=expected_data)
        self.loop.run_until_complete(check)

        # lang = en and any case
        url = f'data://inflector/glebov-n?lang=en&case={PREFIX_ENCODED}'
        expected_data = make_expected(url, first_name='Nikita', last_name='Glebov')
        check = self.response_check(url, expected_data=expected_data)
        self.loop.run_until_complete(check)

        staff.assert_called()
        inflect.assert_not_called()

    @patch.object(InflectorWorker, 'request_staff', return_value=MOCK_STAFF)
    @patch.object(InflectorWorker, 'request_inflect', return_value=MOCK_INFLECT)
    def test_inflector__inflect_call(self, inflect: MagicMock, staff: MagicMock):
        # именительный
        url = 'data://inflector/glebov-n?lang=ru&prefix=кто'
        expected_data = make_expected(url, first_name='Никита', last_name='Глебов')
        check = self.response_check(url, expected_data=expected_data)
        self.loop.run_until_complete(check)
        inflect.assert_not_called()

        # lang=en
        url = 'data://inflector/glebov-n?lang=en&prefix=кого'
        expected_data = make_expected(url, first_name='Nikita', last_name='Glebov')
        check = self.response_check(url, expected_data=expected_data)
        self.loop.run_until_complete(check)
        inflect.assert_not_called()

        # invalid lang
        url = 'data://inflector/glebov-n?lang=XX&prefix=оком'
        expected_data = make_expected(url, first_name='Никита', last_name='Глебов')
        check = self.response_check(url, expected_data=expected_data)
        self.loop.run_until_complete(check)
        inflect.assert_not_called()

        # valid
        url = f'data://inflector/glebov-n?lang=ru&prefix={PREFIX}'
        expected_data = make_expected(url, first_name='Никиту', last_name='Глебова')
        check = self.response_check(url, expected_data=expected_data)
        self.loop.run_until_complete(check)
        inflect.assert_called()

    @patch.object(InflectorWorker, 'request_staff')
    @patch.object(InflectorWorker, 'request_inflect', return_value=MOCK_INFLECT)
    def test_inflector__dismissed(self, inflect: MagicMock, staff: MagicMock):
        url = 'data://inflector/glebov-n'

        # dismissed
        staff.return_value = MOCK_STAFF | {'official': {'is_dismissed': True}}
        expected_data = make_expected(url, first_name='Никита', last_name='Глебов', is_dismissed=True)
        check = self.response_check(url, expected_data=expected_data)
        self.loop.run_until_complete(check)

        # memorial
        staff.return_value = MOCK_STAFF | {'memorial': {'id': ...}, 'official': {'is_dismissed': True}}
        expected_data = make_expected(url, first_name='Никита', last_name='Глебов', is_dismissed=False)
        check = self.response_check(url, expected_data=expected_data)
        self.loop.run_until_complete(check)

    @patch.object(InflectorWorker, 'request_staff', side_effect=BackendError)
    @patch.object(InflectorWorker, 'request_inflect')
    def test_inflector__invalid_login(self, inflect: MagicMock, staff: MagicMock):
        url = 'data://inflector/invalid-login'

        expected_data = make_expected(url, first_name='invalid-login', last_name='', completed=False)
        check = self.response_check(url, expected_data=expected_data)
        self.loop.run_until_complete(check)
        inflect.assert_not_called()
