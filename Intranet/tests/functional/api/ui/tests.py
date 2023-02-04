# coding: utf-8

import random
import datetime

from unittest.mock import (
    patch,
)
from testutils import (
    create_organization_without_domain,
    get_auth_headers,
    patched_admin_permissions,
    TestCase,
    TestOrganizationWithoutDomainMixin,
)
from hamcrest import (
    assert_that,
    has_entries,
    contains,
    none,
    contains_inanyorder,
    empty,
    has_length,
    all_of,
    has_item,
    not_,
    equal_to,
)
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    ServiceModel,
    ServicesLinksModel,
    enable_service,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    ImageModel,
    DomainModel,
    UserModel,
    OrganizationModel,
)
from intranet.yandex_directory.src.yandex_directory.core.utils.robots import create_robot_for_service_and_org_id
from intranet.yandex_directory.src.yandex_directory.common.utils import (
    format_date,
    url_join,
    utcnow,
)
from intranet.yandex_directory.src.yandex_directory.core.permission.permissions import (
    get_permissions,
    global_permissions,
)
from intranet.yandex_directory.src.yandex_directory.common.exceptions import (
    DomainNotFound,
)
from intranet.yandex_directory.src.yandex_directory.core.utils import build_email


class TestUIView(TestOrganizationWithoutDomainMixin, TestCase):
    def setUp(self):
        super(TestUIView, self).setUp()
        self._client_id = 0

        # Эти данные будут использоваться для всех сервисов,
        # потому что они носят лишь информационных характер
        # и не обязаны быть уникальными.
        self.good_tld_data = {
            'com': {
                'url': '//slug.example.yandex.com/good',
                'icon': 'http://icons.example.yandex.com/slug-com.png',
            },
            'ru': {
                'url': '//slug.example.yandex.ru/good',
                'icon': 'http://icons.example.yandex.com/slug-ru.png',
            }
        }
        self.good_language_data = {
            'ru': {
                'name': 'Название сервиса',
            },
            'en': {
                'name': 'Service name',
            }
        }

        # Это правильный сервис, который нужно показать в шапке
        # и открывать в новой вкладке
        self.create_service('good', self.organization['id'], in_new_tab=True)

        # У этого сервиса специальный признак, говорящий, что
        # его в шапке показывать не надо
        self.create_service('bad', self.organization['id'], show_in_header=False)

        # А в этом сервисе, хоть и есть признак,
        # что его надо показать в шапке, но не хватает
        # данных, поэтому он не должен быть показан.
        self.create_service('ugly', self.organization['id'], good=False)

    def get_next_client_id(self):
        """Возвращает уникальный client_id.
        """
        self._client_id += 1
        return str(self._client_id)

    def create_service(self,
                       slug,
                       org_id,
                       enabled=True,
                       priority=0,
                       show_in_header=True,
                       in_new_tab=False,
                       good=True,
                       available_for_external_admin=False,
                       ):
        """Создаёт тестовый сервис, по умолчанию, подключенный в организации.
        """
        ServicesLinksModel(self.meta_connection).create(
            slug=slug,
            data_by_tld=self.good_tld_data if good else {},
            data_by_language=self.good_language_data if good else {},
            show_in_header=show_in_header,
            in_new_tab=in_new_tab,
            available_for_external_admin=available_for_external_admin,
            priority=priority,
        )
        service = ServiceModel(self.meta_connection).create(
            slug=slug,
            name=slug,
            # Всё равно что тут будет, но аргумент обязательный
            # но надо сдвинуть client_id, чтобы он не пересёкся с теми,
            # что созданы в setUp
            client_id=self.get_next_client_id(),
        )

        if enabled:
            enable_service(
                self.meta_connection,
                self.main_connection,
                org_id,
                slug,
            )
        return service

    def add_logo(self):
        org_id = self.organization['id']
        image = ImageModel(self.main_connection).create(
            org_id,
            meta= {
              'sizes': {
                  'orig': {
                      'height': 960,
                      'path': '/get-connect/5201/some-img/orig',
                      'width': 640
                  }
              }
            }
        )
        OrganizationModel(self.main_connection).update(
            update_data={'logo_id': image['id']},
            filter_data={'id': org_id},
        )

    def test_get_ui_header_data(self):
        # Простейший сценарий, когда все сервисы подключены
        # в организации, но у одного нет нужного для шапки флага,
        # а у другого не хватает некоторых данных

        # укажем лого для органзации
        self.add_logo()

        data = self.get_json('/ui/header/?tld=ru&language=ru')

        assert_that(
            data,
            has_entries(
                # В списке сервисов показываем только
                # тот, который имеет специальный признак
                # и все необходимые для UI данные.
                services=contains(
                    has_entries(
                        slug='good',
                    )
                ),
            ),
        )

    def test_set_host_by_x_host(self):
        # подменяем host через заголовок x-<slug>-host
        new_host = 'my.custom.host.com'
        data = self.get_json('/ui/header/?tld=ru&language=ru', add_headers={'x-good-service-host': new_host})
        assert_that(
            data,
            has_entries(
                services=contains(
                    has_entries(
                        slug='good',
                        url='//my.custom.host.com/good'
                    )
                )
            ),
        )

    def test_get_ui_header_data_in_paid_mode(self):
        # Если организация в платном режиме, то в данных ручки
        # должен быть логотип

        # Включим платный режим
        self.enable_paid_mode()

        # укажем лого для органзации
        self.add_logo()

        data = self.get_json('/ui/header/?tld=ru&language=ru')

        assert_that(
            data,
            has_entries(
                # В списке сервисов показываем только
                # тот, который имеет специальный признак
                # и все необходимые для UI данные.
                services=contains(
                    has_entries(
                        slug='good',
                    )
                ),
                logo={
                    'orig': {
                        'height': 960,
                        'url': app.config['MDS_READ_API'] + '/get-connect/5201/some-img/orig',
                        'width': 640
                    },
                }
            ),
        )

    def test_only_enabled_services_should_be_shown(self):
        # Теперь проверим, что если есть сервис, который
        # потенциально может быть показан в шапке, но он
        # не включён для организации, то ручка его не отдаст

        # Сначала, добавим такой сервис, но включать не будем
        self.create_service('yet-another-good', self.organization['id'], enabled=False)

        data = self.get_json('/ui/header/?tld=ru&language=ru')

        assert_that(
            data,
            has_entries(
                # И опять в списке сервисов должен быть только один,
                # так как yet-another-good – не подключён в
                # организации self.organization.
                services=contains(
                    has_entries(
                        slug='good',
                        in_new_tab=True,
                    )
                ),
                logo=none(),
            ),
        )

    def test_services_ordering(self):
        # В шапке сервисы должны быть упорядочены
        # по увеличению атрибута priority.

        # Сначала создадим десяток сервисов с разным приоритетом
        num_services = 10
        priorities = list(range(1, num_services + 1))
        random.shuffle(priorities)

        # самым первым должен идти сервис, созданный в
        # setUp, так как у него нулевой приоритет.
        matchers = [
            (0, has_entries(slug='good'))
        ]

        for priority in priorities:
            service_slug = 'services-{}'.format(priority)
            matchers.append(
                (priority, has_entries(slug=service_slug))
            )

            self.create_service(service_slug, self.organization['id'], priority=priority)

        data = self.get_json('/ui/header/?tld=ru&language=ru')

        # матчеры надо отсортировать в порядке возрастания приоритета
        matchers.sort(reverse=True)
        matchers = [matcher for priority, matcher in matchers]

        assert_that(
            data,
            has_entries(
                # На этот раз в сервисах должно быть 11
                # штук в указанном порядке
                services=contains(*matchers),
                logo=none(),
            ),
        )

    def test_admin_should_see_portal_in_the_services_list(self):
        # Для внутреннего админа и его заместителя нужно  показывать portal,
        # а для обычных пользователей - не показывать.

        self.create_service(
            'portal',
            self.organization['id'],
            # Порталу не обязательно быть подключенным, мы всё
            # равно показываем его админу
            enabled=False
        )

        # Сделаем запрос от имени админа
        data = self.get_json(
            '/ui/header/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=self.admin_uid),
        )

        assert_that(
            data,
            has_entries(
                # И опять в списке сервисов должен быть portal
                services=contains_inanyorder(
                    has_entries(slug='portal'),
                    has_entries(slug='good'),
                ),
                logo=none(),
            ),
        )

        inner_deputy_admin = self.create_deputy_admin(is_outer=False)
        # Сделаем запрос от имени заместителя админа
        data = self.get_json(
            '/ui/header/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=inner_deputy_admin['id'])
        )

        assert_that(
            data,
            has_entries(
                # И опять в списке сервисов должен быть portal
                services=contains_inanyorder(
                    has_entries(slug='portal'),
                    has_entries(slug='good'),
                ),
                logo=none(),
            ),
        )

        # Но если сделать запрос от имени обычного пользователя
        user = self.create_user()
        data = self.get_json(
            '/ui/header/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=user['id'])
        )

        assert_that(
            data,
            has_entries(
                # То portal в списке сервисов не будет
                services=contains(
                    has_entries(slug='good'),
                ),
                logo=none(),
            ),
        )

    def test_outer_admin_will_see_only_limited_list_of_services(self):
        # Проверим, что внешнему админу и внешнему заместителю мы покажем только избранный список
        # сервисов.

        self.create_service('dashboard', self.organization['id'], available_for_external_admin=True)
        self.create_service('mail', self.organization['id'], available_for_external_admin=True)
        self.create_service('disk', self.organization['id'], available_for_external_admin=True)
        self.create_service('calendar', self.organization['id'], available_for_external_admin=True)
        self.create_service('portal', self.organization['id'], available_for_external_admin=True, enabled=False)
        self.create_service('other-service', self.organization['id'])

        data = self.get_json(
            '/ui/header/?tld=ru&language=ru',
            headers=get_auth_headers(as_outer_admin=self.outer_admin)
        )

        # Админ должен видеть только эти сервисы в шапке,
        # но не 'good' и 'other-service'
        admin_services = ['dashboard', 'mail', 'disk', 'calendar', 'portal']
        slugs = [
            has_entries(slug=slug)
            for slug in admin_services
        ]

        assert_that(
            data,
            has_entries(
                # И опять в списке сервисов должен быть только один,
                # так как yet-another-good – не подключён в
                # организации self.organization.
                services=contains_inanyorder(*slugs),
                logo=none(),
            ),
        )

        outer_deputy_admin = self.create_deputy_admin()
        data = self.get_json(
            '/ui/header/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=outer_deputy_admin['id'])
        )
        assert_that(
            data,
            has_entries(
                # И опять в списке сервисов должен быть только один,
                # так как yet-another-good – не подключён в
                # организации self.organization.
                services=contains_inanyorder(*slugs),
                logo=none(),
            ),
        )

    def test_yaorg_users_will_see_services(self):
        # Проверим, что админу яорг мы покажем те же сервисы, что и обычным внутренним админам

        self.create_service('service-1', self.yandex_organization['id'], available_for_external_admin=True)
        self.create_service('service-2', self.yandex_organization['id'])
        self.create_service('portal', self.yandex_organization['id'], available_for_external_admin=True, enabled=False)
        self.create_service('not-enabled-service', self.yandex_organization['id'], enabled=False)

        data = self.get_json(
            '/ui/header/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=self.yandex_admin['id']),
        )

        admin_services = ['service-1', 'service-2', 'portal']
        slugs = [
            has_entries(slug=slug)
            for slug in admin_services
        ]

        assert_that(
            data,
            has_entries(
                services=contains_inanyorder(*slugs),
                logo=none(),
            ),
        )

        # Проверим, что пользователю яорг мы покажем те же сервисы, что и обычным пользователям
        # все подключеные, portal не показываем.
        user_services = ['service-1', 'service-2']
        slugs = [
            has_entries(slug=slug)
            for slug in user_services
        ]
        data = self.get_json(
            '/ui/header/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=self.yandex_user['id']),
        )
        assert_that(
            data,
            has_entries(
                services=contains_inanyorder(*slugs),
                logo=none(),
            ),
        )


class TestUIOrganizationProfileInfo(TestCase):
    def setUp(self):
        super(TestUIOrganizationProfileInfo, self).setUp()
        self.owner_login = build_email(
            self.main_connection,
            self.user['nickname'],
            self.organization['id'],
        )

    def test_org_profile_init(self):
        org_id = self.organization['id']
        # Проверим данные организации в самом начале
        data = self.get_json(
            '/ui/org-profile/',
            headers=get_auth_headers(as_org=org_id)
        )
        assert_that(
            data,
            has_entries(
                has_natural_domains=False,
                user_count=1,
                department_count=1,
                group_count=0,
                admin_count=2,
                owner_login=self.owner_login,
            )
        )

    def test_org_profile_structure(self):

        org_id = self.organization['id']
        # Добавим новых пользователей
        nickname = 'newuser'
        birthday = datetime.date(day=2, month=3, year=1990)
        data = {
            'name': self.name,
            'nickname': nickname,
            'password': '1234456787',
            'department_id': self.department['id'],
            'birthday': format_date(birthday),
            'gender': 'female',
        }

        response = self.post_json('/users/', data, expected_code=201)

        # Добавим робота
        create_robot_for_service_and_org_id(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            service_slug=self.service['slug'],
            org_id=org_id,
        )

        # Проверим, что число пользователей в организации выводится без роботов
        data = self.get_json(
            '/ui/org-profile/',
            headers=get_auth_headers(as_org=org_id)
        )
        assert_that(
            data,
            has_entries(
                has_natural_domains=False,
                user_count=2,
                department_count=1,
                group_count=0,
                admin_count=2,
                owner_login=self.owner_login,
            )
        )
        # Добавляем админа
        data = {
            'name': self.name,
            'nickname': 'root',
            'password': '1234456787',
            'department_id': self.department['id'],
            'birthday': format_date(birthday),
            'gender': 'male',
        }

        user_data = self.post_json('/users/', data, expected_code=201)

        UserModel(self.main_connection).make_admin_of_organization(
            org_id=org_id,
            user_id=response['id']
        )
        # Проверяем кол-во админов
        data = self.get_json(
            '/ui/org-profile/',
            headers=get_auth_headers(as_org=org_id)
        )
        assert_that(
            data,
            has_entries(
                has_natural_domains=False,
                user_count=3,
                department_count=1,
                group_count=0,
                admin_count=3,
                owner_login=self.owner_login,
            )
        )
        # Добавляем обычную группу
        label = 'group_%d' % random.randint(1024, 65535)
        data = {
            'name': {'ru': label},
            'label': label,
            'description': {'ru': label},
        }
        group_data = self.post_json('/groups/', data)
        # Проверяем группу
        data = self.get_json(
            '/ui/org-profile/',
            headers=get_auth_headers(as_org=org_id)
        )
        assert_that(
            data,
            has_entries(
                has_natural_domains=False,
                user_count=3,
                department_count=1,
                group_count=1,
                admin_count=3,
                owner_login=self.owner_login,
            )
        )
        # Добавляем техническую группу вместе с департаментом
        data = {
            'label': 'dep1',
            'name': {'ru': 'dep1'},
            'parent_id': self.department['id'],
        }
        dep_data = self.post_json('/departments/', data)
        # Проверяем, что число групп не поменялось,
        # а число департаментов - изменилось
        data = self.get_json(
            '/ui/org-profile/',
            headers=get_auth_headers(as_org=org_id)
        )
        assert_that(
            data,
            has_entries(
                has_natural_domains=False,
                user_count=3,
                department_count=2,
                group_count=1,
                admin_count=3,
                owner_login=self.owner_login,
            )
        )

        # Удаляем группу
        self.delete_json('/groups/%s/' % group_data['id'], expected_code=200)

        # Проверяем, что кол-во групп поменялось
        data = self.get_json(
            '/ui/org-profile/',
            headers=get_auth_headers(as_org=org_id)
        )
        assert_that(
            data,
            has_entries(
                has_natural_domains=False,
                user_count=3,
                department_count=2,
                group_count=0,
                admin_count=3,
                owner_login=self.owner_login,
            )
        )

        # Увольняем пользователя
        self.patch_json(
            '/users/%s/' % user_data['id'],
            data={
                'is_dismissed': True,
            }
        )

        # Проверяем, что кол-во пользователей поменялось
        data = self.get_json(
            '/ui/org-profile/',
            headers=get_auth_headers(as_org=org_id)
        )
        assert_that(
            data,
            has_entries(
                has_natural_domains=False,
                user_count=2,
                department_count=2,
                group_count=0,
                owner_login=self.owner_login,
                #TODO: тут нужно проверить, что при удалении сотрудника-админа,
                # он удаляется из админской группы: https://st.yandex-team.ru/DIR-3111
                # admin_count=2
            )
        )
        # Удаляем департамет
        self.delete_json('/departments/%s/' % dep_data['id'])

        # Проверяем, что кол-во департаментов поменялось
        data = self.get_json(
            '/ui/org-profile/',
            headers=get_auth_headers(as_org=org_id)
        )
        assert_that(
            data,
            has_entries(
                has_natural_domains=False,
                user_count=2,
                department_count=1,
                group_count=0,
                owner_login=self.owner_login,
                # TODO: тут нужно проверить, что при удалении сотрудника-админа,
                # он удаляется из админской группы:
                # https://st.yandex-team.ru/DIR-3111
                # admin_count=2
            )
        )

        # Добавляем домен natural.ru
        natural_ru = 'natural.ru'
        data = {
            'name': natural_ru,
        }

        domain_response = self.post_json('/domains/', data)

        # Проверяем, что теперь есть естественные домены
        data = self.get_json(
            '/ui/org-profile/',
            headers=get_auth_headers(as_org=org_id)
        )
        assert_that(
            data,
            has_entries(
                has_natural_domains=True,
                user_count=2,
                department_count=1,
                group_count=0,
                owner_login=self.owner_login,
                # TODO: https://st.yandex-team.ru/DIR-3111
                # тут нужно проверить, что при удалении сотрудника-админа,
                # он удаляется из админской группы:
                # admin_count=2
            )
        )


class TestUIOrganizations(TestCase):
    def setUp(self):
        super(TestUIOrganizations, self).setUp()
        self.second_org = self.create_organization()

    def test_all_organizations_for_outer_admin(self):
        # Так как наш внешний админ пока не состоит в организации,
        # то мы должны показать ему пустой список
        data = self.get_json(
            '/ui/organizations/',
           headers=get_auth_headers(as_uid=self.outer_admin['id'])
        )
        assert_that(data, empty())

        # Однако если он вступит в другую организацию,
        # то мы должны будем отдать про неё информацию:
        self.create_portal_user(
            org_id=self.second_org['id'],
            user_id=self.outer_admin['id'],
        )
        data = self.get_json(
            '/ui/organizations/',
           headers=get_auth_headers(as_uid=self.outer_admin['id'])
        )
        assert_that(
            data,
            contains(
                has_entries(
                    id=self.second_org['id'],
                    name=self.second_org['name']['ru'],
                )
            )
        )

    def test_all_organizations_for_user(self):
        # Для пользователя, ручка должна отдавать только организации, в которых он не уволен
        data = self.get_json(
            '/ui/organizations/',
           headers=get_auth_headers(as_uid=self.user['id'])
        )
        assert_that(
            data,
            contains(
                has_entries(
                    id=self.organization['id'],
                    name=self.organization['name']['ru'],
                )
            )
        )

        # Теперь уволим сотрудника и убедимся, что будет отдаваться пустой список
        self.dismiss(self.organization, self.user['id'])

        data = self.get_json(
            '/ui/organizations/',
           headers=get_auth_headers(as_uid=self.user['id'])
        )
        assert_that(data, empty())


class TestUIService(TestOrganizationWithoutDomainMixin, TestCase):
    def setUp(self):
        super(TestUIService, self).setUp()
        self._client_id = 0

        # Эти данные будут использоваться для всех сервисов,
        # потому что они носят лишь информационных характер
        # и не обязаны быть уникальными.
        self.good_tld_data = {
            'com': {
                'url': '//slug.example.yandex.com/good',
                'icon': 'http://icons.example.yandex.com/slug-com.png',
            },
            'ru': {
                'url': '//slug.example.yandex.ru/good',
                'icon': 'http://icons.example.yandex.com/slug-ru.png',
            }
        }
        self.good_language_data = {
            'ru': {
                'name': 'Название сервиса',
            },
            'en': {
                'name': 'Service name',
            }
        }

        # Это правильный сервис, который нужно показать в шапке
        # и открывать в новой вкладке
        self.create_service('good', self.organization['id'], in_new_tab=True, priority=1)

        # Это сервис не подключенный к организации
        self.create_service('not-enabled-service', self.organization['id'], enabled=False, priority=2)

    def get_next_client_id(self):
        """Возвращает уникальный client_id.
        """
        self._client_id += 1
        return str(self._client_id)

    def create_service(
            self,
            slug,
            org_id,
            enabled=True,
            priority=0,
            show_in_header=True,
            in_new_tab=False,
            good=True,
            available_for_external_admin=False,
            data_by_language={},
            actions=[],
            features=[],
            settings_url='',
            always_enabled=False,
            is_configurable=False,
    ):
        """
        Создаёт тестовый сервис, по умолчанию, подключенный в организации.
        """
        ServicesLinksModel(self.meta_connection).create(
            slug=slug,
            data_by_tld=self.good_tld_data if good else {},
            data_by_language=self.good_language_data if good else data_by_language,
            show_in_header=show_in_header,
            in_new_tab=in_new_tab,
            available_for_external_admin=available_for_external_admin,
            priority=priority,
            actions=actions,
            features=features,
            settings_url=settings_url,
            always_enabled=always_enabled,
            is_configurable=is_configurable,
        )
        service = ServiceModel(self.meta_connection).create(
            slug=slug,
            name=slug,
            # Всё равно что тут будет, но аргумент обязательный
            # но надо сдвинуть client_id, чтобы он не пересёкся с теми,
            # что созданы в setUp
            client_id=self.get_next_client_id(),
        )

        if enabled:
            enable_service(
                self.meta_connection,
                self.main_connection,
                org_id,
                slug,
            )
        return service

    def test_get_ui_service_data(self):
        # один сервис подключен к организации, а один нет
        # но выводим всё равно 2 сервиса
        data = self.get_json('/ui/services/?tld=ru&language=ru')
        assert_that(
            data,
            has_length(2),
        )

        # если сервис не подключен к организации, то свойство available будет False и наоборот
        assert_that(
            data,
            contains_inanyorder(
                has_entries(
                    slug='good',
                    available=True,
                ),
                has_entries(
                    slug='not-enabled-service',
                    available=False,
                ),
            ),
        )

    def test_get_ui_service_data_without_tld_and_language(self):
        # один сервис подключен к организации, а один нет
        # но выводим всё равно 2 сервиса, если мы не передали в запросе tld и language
        data = self.get_json('/ui/services/')
        assert_that(
            data,
            has_length(2),
        )

        # если сервис не подключен к организации, то свойство available будет False и наоборот
        assert_that(
            data,
            contains_inanyorder(
                has_entries(
                    slug='good',
                    available=True,
                ),
                has_entries(
                    slug='not-enabled-service',
                    available=False,
                ),
            ),
        )

    def test_set_host_by_x_host(self):
        # подменяем host через заголовок x-<slug>-host
        new_host = 'my.custom.host.com'
        data = self.get_json('/ui/services/?tld=ru&language=ru', add_headers={'x-good-service-host': new_host})
        assert_that(
            data[-1],
            has_entries(
                slug='good',
                url='//my.custom.host.com/good',
            ),
        )

    def test_services_ordering(self):
        # Сервисы должны быть упорядочены
        # по уменьшению атрибута priority.

        # самыми последними должны идти сервисы, созданный в
        # setUp, так как у них от 1 до 4 приоритет.
        list_services = ServicesLinksModel(self.meta_connection).fields('slug', 'priority').all()
        matchers = [
            (service['priority'], service['slug']) for service in list_services
        ]

        # Создадим десяток сервисов с разным приоритетом
        num_services = 10
        priorities = list(range(3, num_services + 3))
        random.shuffle(priorities)

        for priority in priorities:
            service_slug = 'services-{}'.format(priority)
            matchers.append(
                (priority, service_slug)
            )

            self.create_service(service_slug, self.organization['id'], priority=priority)

        data = self.get_json('/ui/services/?tld=ru&language=ru')

        assert_that(
            data,
            has_length(12),
        )

        # матчеры надо отсортировать в порядке возрастания приоритета
        matchers.sort(reverse=True)
        matchers = [matcher for priority, matcher in matchers]
        for number, matcher in enumerate(matchers):
            assert_that(
                data[number],
                has_entries(
                    slug=matcher,
                ),
            )

    def test_admin_should_see_portal_in_the_services_list(self):
        # Для внутреннего админа и его заместителя нужно  показывать portal, а для обычных пользователей - не
        # показывать, т.е. свойство available будет True или False соответственно

        self.create_service(
            'portal',
            self.organization['id'],
            # Порталу не обязательно быть подключенным, мы всё
            # равно показываем его админу
            enabled=False,
        )

        # Сделаем запрос от имени админа
        data = self.get_json(
            '/ui/services/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=self.admin_uid),
        )

        assert_that(
            data,
            # И опять в списке сервисов должен быть portal
            contains_inanyorder(
                has_entries(
                    slug='portal',
                    available=True,
                ),
                has_entries(
                    slug='good',
                    available=True,
                ),
                has_entries(
                    slug='not-enabled-service',
                    available=False,
                ),
            ),
        )

        inner_deputy_admin = self.create_deputy_admin(is_outer=False)
        # Сделаем запрос от имени заместителя админа
        data = self.get_json(
            '/ui/services/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=inner_deputy_admin['id'])
        )

        assert_that(
            data,
            # И опять в списке сервисов должен быть portal
            contains_inanyorder(
                has_entries(
                    slug='portal',
                    available=True,
                ),
                has_entries(
                    slug='good',
                    available=True,
                ),
                has_entries(
                    slug='not-enabled-service',
                    available=False,
                ),
            ),
        )

        # Но если сделать запрос от имени обычного пользователя
        user = self.create_user()
        data = self.get_json(
            '/ui/services/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=user['id'])
        )
        assert_that(
            data,
            # То portal в списке сервисов не будет
            contains_inanyorder(
                has_entries(
                    slug='portal',
                    available=False,
                ),
                has_entries(
                    slug='good',
                    available=True,
                ),
                has_entries(
                    slug='not-enabled-service',
                    available=False,
                ),
            ),
        )

    def test_not_show_dashboard_in_the_services_list(self):
        # Для всех пользователей не показывать сервис dashboard т.е. свойство available будет False

        self.create_service(
            'dashboard',
            self.organization['id'],
        )

        # Сделаем запрос от имени админа
        data = self.get_json(
            '/ui/services/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=self.admin_uid)
        )

        assert_that(
            data,
            contains_inanyorder(
                has_entries(
                    slug='dashboard',
                    available=False,
                ),
                has_entries(
                    slug='good',
                    available=True,
                ),
                has_entries(
                    slug='not-enabled-service',
                    available=False,
                ),
            ),
        )

    def test_outer_admin_will_see_only_limited_list_of_services(self):
        # Проверим, что внешнему админу и внешнему заместителю мы покажем только избранный список
        # сервисов.

        self.create_service('mail', self.organization['id'], available_for_external_admin=True)
        self.create_service('portal', self.organization['id'], available_for_external_admin=True, enabled=False)

        data = self.get_json(
            '/ui/services/?tld=ru&language=ru',
            headers=get_auth_headers(as_outer_admin=self.outer_admin)
        )

        # Внешний админ должен видеть только сервисы со свойством available_for_external_admin

        servise_contains = [
            has_entries(
                slug='portal',
                available=True,
            ),
            has_entries(
                slug='good',
                available=False,
            ),
            has_entries(
                slug='not-enabled-service',
                available=False,
            ),
            has_entries(
                slug='mail',
                available=True,
            ),
        ]

        assert_that(
            data,
            servise_contains,
        )

        outer_deputy_admin = self.create_deputy_admin()
        data = self.get_json(
            '/ui/services/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=outer_deputy_admin['id'])
        )
        assert_that(
            data,
            servise_contains,
        )

    def test_yaorg_users_will_see_services(self):
        # Проверим, что админу яорг мы покажем те же сервисы, что и обычным внутренним админам
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.yandex_organization['id'],
            'good',
        )
        self.create_service('service-1', self.yandex_organization['id'])
        self.create_service('portal', self.yandex_organization['id'], available_for_external_admin=True, enabled=False)

        data = self.get_json(
            '/ui/services/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=self.yandex_admin['id']),
        )

        assert_that(
            data,
            contains_inanyorder(
                has_entries(
                    slug='good',
                    available=True,
                ),
                has_entries(
                    slug='service-1',
                    available=True,
                ),
                has_entries(
                    slug='portal',
                    available=True,
                ),
                has_entries(
                    slug='not-enabled-service',
                    available=False,
                ),
            ),
        )

        # Проверим, что пользователю яорг мы покажем те же сервисы, что и обычным пользователям
        # все подключеные, portal не показываем.

        data = self.get_json(
            '/ui/services/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=self.yandex_user['id']),
        )
        assert_that(
            data,
            contains_inanyorder(
                has_entries(
                    slug='good',
                    available=True,
                ),
                has_entries(
                    slug='service-1',
                    available=True,
                ),
                has_entries(
                    slug='portal',
                    available=False,
                ),
                has_entries(
                    slug='not-enabled-service',
                    available=False,
                ),
            ),
        )

    def test_get_services_with_two_feature(self):

        self.create_service(
            'with_two_feature',
            self.yandex_organization['id'],
            features='["with_subscriptions", "beta"]',
        )

        # Сделаем запрос от имени админа
        data = self.get_json(
            '/ui/services/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=self.admin_uid),
        )

        assert_that(
            data,
            contains_inanyorder(
                has_entries(
                    slug='with_two_feature',
                    with_subscriptions=True,
                    beta=True,
                ),
                has_entries(
                    slug='good',
                    with_subscriptions=False,
                    beta=False,
                ),
                has_entries(
                    slug='not-enabled-service',
                    with_subscriptions=False,
                    beta=False,
                ),
            ),
        )

    def get_services(self, user, org_id, service_name, update_data, result_actions):
        service = self.create_service(
            service_name,
            org_id,
        )
        ServicesLinksModel(self.meta_connection).update(
            filter_data={'slug': service['slug']},
            update_data=update_data,
        )

        data = self.get_json(
            '/ui/services/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=user),
        )

        assert_that(
            data,
            has_item(
                has_entries(
                    actions=result_actions,
                    slug=service['slug']
                )
            )
        )

    def test_get_services_with_empty_actions(self):
        service = self.create_service(
            'empty',
            self.organization['id'],
        )
        data = self.get_json(
            '/ui/services/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=self.admin_uid),
        )
        assert_that(
            data,
            has_item(
                has_entries(
                    actions=[],
                    slug=service['slug']
                )
            )
        )

    def test_get_services_yamb_with_actions(self):
        self.get_services(
            self.admin_uid,
            self.organization['id'],
            'yamb',
            {'actions': [{'url': '//yandex.{}/chat', 'main': True, 'slug': 'open_chat'}]},
            [{'url': '//yandex.ru/chat', 'main': True, 'slug': 'open_chat'}]
        )

    def test_get_services_webmaster_with_actions_user_admin(self):
        self.get_services(
            self.admin_uid,
            self.organization['id'],
            'webmaster',
            {'actions': [{'url': '//example.{}', 'main': True, 'slug': 'add_anything'}]},
            [{'url': '//example.ru', 'main': True, 'slug': 'add_anything'}]
        )

    def test_get_services_webmaster_with_actions_user_not_admin(self):
        user = self.create_user()
        self.get_services(
            user['id'],
            self.organization['id'],
            'webmaster',
            {'actions': [{'url': '//example.{}', 'main': True, 'slug': 'add_anything'}]},
            []
        )

    def test_get_services_portal_with_actions_has_owned_domains(self):
        self.get_services(
            self.admin_uid,
            self.organization['id'],
            'portal',
            {'actions': [{'url': '//example.{0}/portal/admin#{1}', 'main': True, 'slug': 'add_anything'}]},
            [{'url': '//example.ru/portal/admin#add-user', 'main': True, 'slug': 'add_anything'}]
        )

    def test_get_services_portal_with_actions_not_has_owned_domains(self):
        organization = create_organization_without_domain(
            self.meta_connection,
            self.main_connection,
        )['organization']
        user = self.create_user(org_id=organization['id'])

        self.get_services(
            user['id'],
            organization['id'],
            'portal',
            {'actions': [{'url': '//example.{0}/portal/admin#{1}', 'main': True, 'slug': 'add_anything'}]},
            [{'url': '//example.ru/portal/admin#invite-user', 'main': True, 'slug': 'add_anything'}],
        )

    def test_get_services_calendar_with_actions(self):
        self.get_services(
            self.admin_uid,
            self.organization['id'],
            'calendar',
            {'actions': [{'url': '//calendar.{0}?uid={1}', 'main': True, 'slug': 'add_anything'}]},
            [{'url': '//calendar.ru?uid={}'.format(self.admin_uid), 'main': True, 'slug': 'add_anything'}],
        )

    def test_get_services_staff_with_actions(self):
        nickname = UserModel(self.main_connection).get(self.admin_uid)['nickname']
        self.get_services(
            self.admin_uid,
            self.organization['id'],
            'staff',
            {'actions': [{'url': '//staff.yandex.{0}/${1}/?org_id={2}&uid={3}', 'main': True, 'slug': 'add_anything'}]},
            [
                {
                    'url': '//staff.yandex.ru/${0}/?org_id={1}&uid={2}'.format(
                        nickname,
                        self.organization['id'],
                        self.admin_uid,
                    ),
                    'main': True,
                    'slug': 'add_anything',
                }
            ],
        )

    def test_get_services_direct_with_actions(self):
        self.get_services(
            self.admin_uid,
            self.organization['id'],
            'direct',
            {'actions': [{'url': 'example.{}', 'main': True, 'slug': 'add_anything'}]},
            [{"url": "example.ru", "main": True, "slug": "add_anything"}],
        )

    def test_trial_service(self):
        # проверим, что триал отображается нормально

        service = self.create_service(
            'tracker',
            self.organization['id'],
        )

        # подключим трекер в триале
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            service['slug'],
        )
        # обновляем дату окончания триального периода трекера
        self.update_service_trial_expires_date(
            self.organization['id'],
            service['id'],
            utcnow() - datetime.timedelta(days=50),
        )

        # Сделаем запрос от имени админа
        data = self.get_json(
            '/ui/services/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=self.admin_uid),
        )

        date = utcnow() - datetime.timedelta(days=50)
        assert_that(
            data,
            has_item(
                has_entries(
                    trial={
                        'expiration_date': date.strftime("%Y-%m-%d"),
                        'status': 'expired',
                        'days_till_expiration': 0,
                    },
                    slug=service['slug']
                ),
            )
        )

    def test_get_services_browser(self):
        self.create_service(
            'browser',
            self.organization['id'],
            enabled=False,
            always_enabled=True,

        )
        # обычный пользователь видит всегда браузер с параметрами:
        # enabled = True available = True
        user = self.create_user()
        data = self.get_json(
            '/ui/services/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=user['id'])
        )
        assert_that(
            data,
            has_item(
                has_entries(
                    slug='browser',
                    enabled=True,
                    available=True,
                ),
            ),
        )

        # админ видит всегда браузер с параметрами:
        # enabled = True available = True
        data = self.get_json(
            '/ui/services/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=self.admin_uid)
        )
        assert_that(
            data,
            has_item(
                has_entries(
                    slug='browser',
                    enabled=True,
                    available=True,
                ),
            ),
        )

    def test_get_services_alice_b2b(self):
        data_by_language = {
            "en": {"name": "Alice for business"},
            "ru": {"name": "Алиса для бизнеса"},
        }
        self.create_service(
            'alice_b2b',
            self.organization['id'],
            enabled=False,
            good=False,
            data_by_language=data_by_language
        )
        data = self.get_json(
            '/ui/services/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=self.admin_uid)
        )
        assert_that(
            data,
            has_item(
                has_entries(
                    slug='alice_b2b',
                    name=data_by_language['ru']['name'],
                ),
            ),
        )

    def test_get_settings_url_for_not_disk(self):
        slug = 'example'
        settings_url = '//yandex.{}/portal/services/example'

        self.create_service(
            slug,
            self.organization['id'],
            settings_url=settings_url,
        )

        data = self.get_json(
            '/ui/services/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=self.admin_uid),
        )
        assert_that(
            data,
            has_item(
                has_entries(
                    slug='example',
                    settings_url='//yandex.ru/portal/services/example',
                ),
            ),
        )
    def test_get_settings_url_for_disk(self):
        slug = 'disk'
        settings_url = '//admin-testing.yandex.{0}/disk?org_id=${1}&uid=${2}'

        self.create_service(
            slug,
            self.organization['id'],
            settings_url=settings_url,
        )

        data = self.get_json(
            '/ui/services/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=self.admin_uid),
        )
        assert_that(
            data,
            has_item(
                has_entries(
                    slug='disk',
                    settings_url='//admin-testing.yandex.{0}/disk?org_id=${1}&uid=${2}'.format(
                        'ru',
                        self.organization['id'],
                        self.admin_uid,
                    ),
                ),
            ),
        )

    def test_check_available_false_for_alice_b2b(self):
        # available = False у Алисы и всех тех, кого нет в списке:
        # 'wiki', 'tracker', 'disk', 'forms', 'staff', 'calendar', 'mail',
        # если они не подключены
        self.create_service(
            'alice_b2b',
            self.organization['id'],
            enabled=False,
        )
        data = self.get_json(
            '/ui/services/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=self.admin_uid),
        )
        assert_that(
            data,
            has_item(
                has_entries(
                    slug='alice_b2b',
                    available=False,
                ),
            ),
        )

    def test_check_available_true_for_alice_b2b(self):
        # available = True у Алисы и всех тех, кого нет в списке:
        # 'wiki', 'tracker', 'disk', 'forms', 'staff', 'calendar', 'mail',
        # если они подключены
        self.create_service(
            'alice_b2b',
            self.organization['id'],
            enabled=True,
        )
        data = self.get_json(
            '/ui/services/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=self.admin_uid),
        )
        assert_that(
            data,
            has_item(
                has_entries(
                    slug='alice_b2b',
                    available=True,
                ),
            ),
        )

    def get_permissions_for_user(self, user_id):
        try:
            org_domain = DomainModel(self.main_connection).get_master(user_id)
            master_domain = org_domain['name']
        except DomainNotFound:
            master_domain = None
        return get_permissions(
            self.meta_connection,
            self.main_connection,
            user_id,
            org_id=self.organization['id'],
        )

    def test_filter_by_slug(self):
        self.create_service(
            'disk',
            self.organization['id'],
            enabled=False,
            is_configurable=True,
        )
        user = self.create_user()

        data = self.get_json(
            '/ui/services/?tld=ru&language=ru&slug=disk',
            headers=get_auth_headers(as_uid=user['id']),
        )
        self.assertEqual(len(data), 1)

    def test_check_available_false_has_permissions_manage_services(self):
        # available = False, если нет пермишшена manage services
        self.create_service(
            'disk',
            self.organization['id'],
            enabled=False,
            is_configurable=True,
        )
        user = self.create_user()

        # проверим, что у пользователя нет права
        assert_that(
            self.get_permissions_for_user(user['id']),
            not_(
                has_item(
                    global_permissions.manage_services,
                )
            ),
        )

        data = self.get_json(
            '/ui/services/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=user['id']),
        )
        assert_that(
            data,
            has_item(
                has_entries(
                    slug='disk',
                    available=False,
                ),
            ),
        )

    def test_check_enable_true_has_permissions_department_edit(self):
        # available = True, если есть пермишшен manage services
        self.create_service(
            'disk',
            self.organization['id'],
            enabled=False,
            is_configurable=True,
        )

        # проверим, что у админа есть права
        assert_that(
            self.get_permissions_for_user(self.admin_uid),
            has_item(
                global_permissions.manage_services,
            ),
        )

        data = self.get_json(
            '/ui/services/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=self.admin_uid),
        )
        assert_that(
            data,
            has_item(
                has_entries(
                    slug='disk',
                    available=True,
                ),
            ),
        )

    def test_check_available_false_for_mail_and_calendar(self):
        # available = False, если есть это сервис mail или calendar
        # и не имеют has_owned_domain

        DomainModel(self.main_connection).filter(
            org_id=self.organization['id']).update(owned=False, master=False)
        # проверим, что нет домена
        has_owned_domain = OrganizationModel(self.main_connection).has_owned_domains(
            self.organization['id']
        )
        assert_that(
            has_owned_domain,
            equal_to(False),
        )

        for slug in ['mail', 'calendar']:
            self.create_service(
                slug,
                self.organization['id'],
                enabled=False,
            )

            data = self.get_json(
                '/ui/services/?tld=ru&language=ru',
                headers=get_auth_headers(as_uid=self.admin_uid),
            )
            assert_that(
                data,
                has_item(
                    has_entries(
                        slug=slug,
                        available=False,
                    ),
                ),
            )

    def test_check_available_true_for_portal_of_outer_admin(self):
        # available = True у портала для внешнего админа
        self.create_service(
            'portal',
            self.organization['id'],
            enabled=True,
        )
        data = self.get_json(
            '/ui/services/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=self.outer_admin['id']),
        )
        assert_that(
            data,
            has_item(
                has_entries(
                    slug='portal',
                    available=True,
                ),
            ),
        )

    def test_check_enabled_true_for_webmaster(self):
        # enabled = True у webmaster для внешнего админа
        self.create_service(
            'webmaster',
            self.organization['id'],
            enabled=True,
        )
        data = self.get_json(
            '/ui/services/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=self.outer_admin['id']),
        )
        assert_that(
            data,
            has_item(
                has_entries(
                    slug='webmaster',
                    enabled=True,
                ),
            ),
        )

        # enabled = True у webmaster для админа
        data = self.get_json(
            '/ui/services/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=self.admin_uid),
        )
        assert_that(
            data,
            has_item(
                has_entries(
                    slug='webmaster',
                    enabled=True,
                ),
            ),
        )

        # enabled = False у webmaster для обычного пользователя
        user = self.create_user()

        data = self.get_json(
            '/ui/services/?tld=ru&language=ru',
            headers=get_auth_headers(as_uid=user['id']),
        )
        assert_that(
            data,
            has_item(
                has_entries(
                    slug='webmaster',
                    enabled=False,
                ),
            ),
        )
