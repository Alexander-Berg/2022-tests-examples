
from mock import Mock, patch
from pretend import stub

from wiki.actions.classes.base_action import ParamsWrapper
from wiki.utils.errors import InputValidationError
from intranet.wiki.tests.wiki_tests.common.wiki_client import WikiClient
from intranet.wiki.tests.wiki_tests.unit_unittest.actions.base import HttpOldActionTestCase

WIKI_SERVICE_DATA = {
    'name': {
        'ru': 'Вики',
        'en': 'Wiki',
    },
    'id': 10,
}

PLAN_SERVICE_DATA = {
    'name': {
        'ru': 'Планер',
        'en': 'Planner',
    },
    'id': 383,
}

PLAN_SERVICE_MEMBERS_DATA = (
    [
        {
            'id': 100500,
            'person': {
                'login': 'rodique',
                'first_name': 'Родион',
                'last_name': 'Волчков',
            },
            'role': {
                'name': {
                    'ru': 'Разработчик интерфейсов',
                    'en': 'Frontend developer',
                },
                'id': 11,
            },
        },
        {
            'person': {
                'id': '4565',
                'login': 'solo',
                'uid': '11200000000074754',
                'first_name': 'Алекс',
                'last_name': 'Пушкин',
            },
            'service': {
                'id': 383,
                'slug': 'plan',
                'name': {'ru': 'Планер', 'en': 'Planner'},
                'parent': 872,
            },
            'role': {
                'name': {
                    'ru': 'Разработчик интерфейсов',
                    'en': 'Frontend developer',
                },
                'scope': {
                    'slug': 'development',
                    'name': {
                        'ru': 'Разработка',
                        'en': 'Development',
                    },
                },
                'id': 11,
                'code': 'development',
            },
        },
    ],
)

PLAN_SERVICE_CONTACTS_DATA = [
    {
        'type': {
            'code': 'url_wiki',
            'validator': 'WIKI',
        },
        'title': {'ru': '', 'en': ''},
        'content': 'planner',
        'service': {
            'id': 383,
        },
    },
    {
        'type': {
            'code': 'url_sitelink',
            'validator': 'URL',
        },
        'title': {'ru': 'planner.yandex-team.ru', 'en': ''},
        'content': 'https://planner.yandex-team.ru/',
        'service': {
            'id': 383,
            'name': {'ru': 'Планер', 'en': 'Planner'},
        },
    },
]

WIKI_SERVICE_CONTACTS_DATA = [
    {
        'id': 2323,
        'type': {'id': 18, 'code': 'url_wiki', 'validator': 'WIKI', 'name': {'ru': 'Вики', 'en': 'Wiki'}},
        'title': {'ru': '', 'en': ''},
        'content': 'wiki',
        'service': {'id': 10, 'slug': 'wiki', 'name': {'ru': 'Вики', 'en': 'Wiki'}, 'parent': 654},
    },
]


@patch('wiki.utils.tvm2.get_tvm2_client', Mock())
class ServiceteamTest(HttpOldActionTestCase):
    client_class = WikiClient
    action = 'serviceteam'

    def mock_repo_response(self, getiter_response=None, get_one_response=None, get_response=None):
        from wiki.actions.classes.serviceteam import Serviceteam

        getiter_response = getiter_response or []
        get_response = get_response or stub(__next__=lambda: list())
        get_one_response = get_one_response or None

        first_page = stub(
            __iter__=lambda: iter(getiter_response),
            __len__=lambda: len(getiter_response),
        )
        self.repo_mock = stub(
            getiter=Mock(
                side_effect=[
                    stub(
                        first_page=first_page,
                        total=len(getiter_response),
                    ),
                    stub(
                        first_page=first_page,
                        total=len(getiter_response),
                    ),
                ]
            ),
            get=Mock(side_effect=get_response),
            get_one=Mock(side_effect=[get_one_response]),
        )

        Serviceteam.services_repo = self.repo_mock
        Serviceteam.service_members_repo = self.repo_mock
        Serviceteam.service_contacts_repo = self.repo_mock

    def _assert_correct_service_is_in_response(self, response, expected_name='Планер'):
        self.assertTrue('service_name' in response, '\'service_name\' must be in json')
        self.assertEqual(expected_name, response['service_name'])

    def _assert_repo_called_with(self, method, args=None, kwargs=None):
        args = args or ()
        kwargs = kwargs or {}
        calls = self.repo_mock.get_calls(method)
        self.assertIn((args, kwargs), calls)

    def _get(self, params):
        from wiki.actions.classes.serviceteam import Serviceteam

        self.request.LANGUAGE_CODE = 'ru'
        return Serviceteam(ParamsWrapper(params), self.request).json_for_get(params)

    def test_id_param_service_found(self):
        """
        {{serviceteam id="<SERVICE ID>"}} должен работать
        """
        self.mock_repo_response(get_one_response=PLAN_SERVICE_DATA, get_response=PLAN_SERVICE_MEMBERS_DATA)

        response = self._get(params={'id': 383})

        self.repo_mock.get_one.assert_called_with({'fields': 'id,name', 'id': 383})
        self.repo_mock.get.assert_called_with({'service': 383, 'fields': 'role.name,role.id,person.login,person.name'})
        self._assert_correct_service_is_in_response(response)

    def test_id_param_invalid(self):
        """
        {{serviceteam id="<SERVICE NAME>"}} должен вернуть ошибку
        """
        self.assertRaisesMessage(
            expected_exception=InputValidationError,
            expected_message='actions.Serviceteam:InvalidIdParam',
            callable_obj=lambda: self._get(params={'id': 'Planner'}),
        )

    def test_slug_param_service_found(self):
        """
        {{serviceteam slug="<SERVICE SLUG>"}} должен работать
        """
        self.mock_repo_response(get_one_response=PLAN_SERVICE_DATA, get_response=PLAN_SERVICE_MEMBERS_DATA)

        response = self._get(params={'slug': 'plan'})

        self.repo_mock.get_one.assert_called_with({'fields': 'id,name', 'slug': 'plan'})
        self.repo_mock.get.assert_called_with({'service': 383, 'fields': 'role.name,role.id,person.login,person.name'})
        self._assert_correct_service_is_in_response(response)

    def test_name_param_ru_service_found(self):
        """
        {{serviceteam name="<SERVICE NAME RU>"}} должен работать
        """
        self.mock_repo_response(get_one_response=PLAN_SERVICE_DATA, get_response=PLAN_SERVICE_MEMBERS_DATA)

        response = self._get(params={'name': 'Планер'})

        self.repo_mock.get_one.assert_called_with({'fields': 'id,name', 'name__contains': 'Планер'})
        self.repo_mock.get.assert_called_with({'service': 383, 'fields': 'role.name,role.id,person.login,person.name'})
        self._assert_correct_service_is_in_response(response)

    def test_name_param_en_service_found(self):
        """
        {{serviceteam name="<SERVICE NAME EN>"}} должен работать
        """
        self.mock_repo_response(get_one_response=PLAN_SERVICE_DATA, get_response=PLAN_SERVICE_MEMBERS_DATA)

        response = self._get(params={'name': 'Planner'})

        self.repo_mock.get_one.assert_called_with({'fields': 'id,name', 'name_en__contains': 'Planner'})
        self.repo_mock.get.assert_called_with({'service': 383, 'fields': 'role.name,role.id,person.login,person.name'})
        self._assert_correct_service_is_in_response(response)

    def test_service_param_not_supported(self):
        """
        {{serviceteam service="<ANYTHING>"}} должен вернуть ошибку
        """
        self.assertRaisesMessage(
            expected_exception=InputValidationError,
            expected_message='actions.Serviceteam:ServiceParamNotSupported',
            callable_obj=lambda: self._get(params={'service': 'Планер'}),
        )

    def test_no_param_one_matched_by_supertag(self):
        """
        {{serviceteam}} должен найти сервис по supertag'у
        """
        self.mock_repo_response(
            getiter_response=PLAN_SERVICE_CONTACTS_DATA,
            get_one_response=PLAN_SERVICE_DATA,
            get_response=PLAN_SERVICE_MEMBERS_DATA,
        )

        response = self._get({})

        self.repo_mock.getiter.assert_called_with(
            {
                'fields': 'service.id, service.name',
                'contact_type_code': 'url_wiki',
                'content__contains': self.page.supertag,
            }
        )
        self.repo_mock.get_one.assert_called_with({'fields': 'id,name', 'id': 383})
        self.repo_mock.get.assert_called_with({'service': 383, 'fields': 'role.name,role.id,person.login,person.name'})
        self._assert_correct_service_is_in_response(response)

    def test_no_param_many_matched_by_supertag(self):
        """
        {{serviceteam}} должен в случае неоднозначности вернуть ошибку
        """
        contacts = WIKI_SERVICE_CONTACTS_DATA
        contacts.extend(PLAN_SERVICE_CONTACTS_DATA)
        self.mock_repo_response(getiter_response=contacts)

        self.assertRaisesMessage(
            InputValidationError,
            ', '.join(
                [
                    'actions.Serviceteam:FoundManyServices test',
                    'actions.Serviceteam:UseServiceId Вики 10',
                    'actions.Serviceteam:UseServiceId Планер 383',
                ]
            ),
            lambda: self._get({}),
        )

        self.repo_mock.getiter.assert_called_with(
            {
                'fields': 'service.id, service.name',
                'contact_type_code': 'url_wiki',
                'content__contains': self.page.supertag,
            }
        )

    def test_no_param_no_one_matched_by_supertag(self):
        """
        {{serviceteam}} должен вернуть ошибку в случае невозможности угадать сервис по supertag'у
        """
        self.mock_repo_response()

        self.assertRaisesMessage(
            InputValidationError,
            'actions.Serviceteam:CannotGuessService',
            lambda: self._get({}),
        )

        self.repo_mock.getiter.assert_called_with(
            {'fields': 'service.id, service.name', 'contact_type_code': 'url_wiki', 'content__contains': self.page.tag}
        )

    def test_find_service_with_no_team(self):
        self.mock_repo_response(get_one_response=WIKI_SERVICE_DATA)

        response = self._get(params={'id': 10})

        self.repo_mock.get_one.assert_called_with({'fields': 'id,name', 'id': 10})
        self.repo_mock.get.assert_called_with({'service': 10, 'fields': 'role.name,role.id,person.login,person.name'})

        self._assert_correct_service_is_in_response(response, expected_name='Вики')

    def test_correct_wiki_url_in_contact(self):
        contact = PLAN_SERVICE_CONTACTS_DATA[0]
        self.mock_repo_response(
            get_one_response=PLAN_SERVICE_DATA,
            get_response=[[contact, ], ],
        )

        response = self._get(params={'id': 383, 'contacts': True})
        self.repo_mock.get.assert_called_with({'service': 383, 'fields': 'type.code,type.validator,content,title'})

        link = response['contacts'][0]['link']
        self.assertIn('/' + contact["content"], link)
        self.assertNotIn('//' + contact['content'], link)
