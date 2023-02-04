# -*- coding: utf-8 -*-

from unittest.mock import (
    patch,
)
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.common.models.base import ALL_FIELDS
from intranet.yandex_directory.src.yandex_directory.core.models import (
    UserModel,
    UserMetaModel,
    ServiceModel,
    OrganizationModel,
    RobotServiceModel,
    OrganizationServiceModel,
)
from hamcrest import (
    assert_that,
    equal_to,
    contains_inanyorder,
    contains,
    has_entries,
    empty,
)
from testutils import (
    TestCase,
    create_organization,
    create_organization_without_domain,
    assert_called_once,
    assert_not_called,
    fake_userinfo,
)

from intranet.yandex_directory.src.yandex_directory.core.utils import prepare_user
from intranet.yandex_directory.src.yandex_directory.core.utils.robots import (
    create_robot_for_service_and_org_id,
    singleton_robot_uids,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    enable_service,
    disable_service,
)
from intranet.yandex_directory.src.yandex_directory.core.models import ActionModel


class TestCreateRobotsForService(TestCase):
    def setUp(self):
        super(TestCreateRobotsForService, self).setUp()
        self.service = ServiceModel(self.meta_connection).create(
            client_id='kjsdjfhgJfhGJdmdbEThkL',
            slug='wiki',
            name='Wiki services',
            robot_required=True
        )
        self.org1 = create_organization(
            self.meta_connection,
            self.main_connection,
            label='org1',
        )['organization']
        self.org2 = create_organization(
            self.meta_connection,
            self.main_connection,
            label='org2',
        )['organization']
        self.org3 = create_organization(
            self.meta_connection,
            self.main_connection,
            label='org3',
        )['organization']

        self.org4 = create_organization(
            self.meta_connection,
            self.main_connection,
            label='yandex-team',
        )

        OrganizationServiceModel(self.main_connection).create(
            org_id=self.org1['id'],
            service_id=self.service['id']
        )
        OrganizationServiceModel(self.main_connection).create(
            org_id=self.org2['id'],
            service_id=self.service['id']
        )

        self.service_with_custom_robot_name = ServiceModel(self.meta_connection).create(
            client_id='kjsdjfhgJfhGJdmdbEThkT',
            slug='test-service',
            name='Custom robot name test service',
            robot_name={
                'ru': 'Кастомное имя робота',
                'en': 'Custom robot name',
            },
            robot_required=True
        )

        self.en_org = create_organization(
            self.meta_connection,
            self.main_connection,
            label='en_org',
            language='en',
        )['organization']

    @patch('intranet.yandex_directory.src.yandex_directory.core.actions.user.event_user_added')
    def test_check_create_robots(self, mock_user_event):
        # Проверяем, создание роботов для org1 & org2

        for org_id in [self.org1['id'], self.org2['id']]:
            create_robot_for_service_and_org_id(self.meta_connection,
                                                self.main_connection,
                                                self.service['slug'],
                                                org_id=org_id)
            users = UserModel(self.main_connection).find(
                filter_data=dict(org_id=org_id),
                fields=ALL_FIELDS
            )
            org_users = [u
                         for u in users
                         if prepare_user(
                                 self.main_connection,
                                 u,
                                 api_version=1,
                         ).get('is_robot')]
            assert_that(len(org_users), 1)

            # Проверка, что департамента у робота - нет
            robot = prepare_user(
                self.main_connection,
                org_users[0],
                api_version=1,
            )
            assert_that(robot['department_id'], equal_to(None))
            # И что отдаем robot_id & slug
            assert_that(robot['is_robot'], True)
            assert_that(robot['service_slug'], equal_to(self.service['slug']))

            # Проверяем, что в нужные записи про связь робот-сервис и сервис-организация есть в таблице
            ro = OrganizationServiceModel(self.main_connection).find(
                filter_data=dict(org_id=org_id, service_id=self.service['id']))
            assert_that(len(ro), 1)
            rs = RobotServiceModel(self.main_connection) \
                 .filter(
                     uid=robot['id'],
                     org_id=org_id,
                     service_id=self.service['id'],
                )
            assert_that(len(rs), 1)

            # Проверяем, что есть роботная группа
            robot_group = OrganizationModel(self.main_connection).get_robots(org_id)
            assert_that(len(robot_group), 1)

        # Проверяем что вызывались нужные event-ы:
        assert_that(mock_user_event.call_count, 2)
        call_kwargs = [kwargs for args, kwargs in mock_user_event.call_args_list]

        # В обоих организациях должно быть заведено по роботу
        assert_that(
            call_kwargs,
            contains_inanyorder(
                has_entries(
                    object_value=has_entries(
                        is_robot=True,
                        service_slug='wiki',
                        nickname='robot-wiki',
                        org_id=self.org1['id']
                    )
                ),
                has_entries(
                    object_value=has_entries(
                        is_robot=True,
                        service_slug='wiki',
                        nickname='robot-wiki',
                        org_id=self.org2['id'],
                    )
                )
            )
        )

        # проверяем, что создались события добавления роботов
        create_robot_actions = ActionModel(self.main_connection).filter(name='service_robot_add').all()
        assert_that(
            create_robot_actions,
            contains_inanyorder(
                has_entries(
                    org_id=self.org1['id'],
                    object=self.service,
                ),
                has_entries(
                    org_id=self.org2['id'],
                    object=self.service,
                )
            )
        )

    def test_create_robots_for_organizations_with_different_languages(self):
        # Проверяем, создание роботов для org1 & org2 с языками ru и en соответственно
        en_org = create_organization(
            self.meta_connection,
            self.main_connection,
            label='en_organization',
            language='en'
        )['organization']
        ru_org = create_organization(
            self.meta_connection,
            self.main_connection,
            label='ru_organization',
            language='ru'
        )['organization']

        OrganizationServiceModel(self.main_connection).create(
            org_id=en_org['id'],
            service_id=self.service['id']
        )
        OrganizationServiceModel(self.main_connection).create(
            org_id=ru_org['id'],
            service_id=self.service['id']
        )

        for organization in [en_org, ru_org]:
            create_robot_for_service_and_org_id(
                self.meta_connection,
                self.main_connection,
                self.service['slug'],
                org_id=organization['id'],
            )
            users = UserModel(self.main_connection).find(
                filter_data=dict(org_id=organization['id']),
                fields=ALL_FIELDS
            )

            org_users = [
                u for u in users
                if prepare_user(
                        self.main_connection,
                        u,
                        api_version=1,
                ).get('is_robot')
            ]
            assert_that(len(org_users), 1)
            robot = org_users[0]

            if organization['language'] == 'ru':
                name = 'Робот сервиса %s' % self.service['slug'].capitalize()
            else:
                name = 'Robot for %s' % self.service['slug'].capitalize()

            assert_that(
                robot['name']['first'],
                equal_to({
                    'ru': name,
                    'en': '%s Robot' % self.service['slug'].capitalize(),
                }),
            )

    @patch('intranet.yandex_directory.src.yandex_directory.core.actions.user.event_user_added')
    def test_create_robots_for_yandex_team_org(self, mock_user_event):
        # Для DIR-3186 мы запрещаем заводить роботного пользователя для
        # тимной организации.
        create_robot_for_service_and_org_id(self.meta_connection,
                                            self.main_connection,
                                            self.service['slug'],
                                            org_id=self.org4['organization']['id'])

        # Проверяем, что в организации нет робота.
        org_id = self.org4['organization']['id']
        users = UserModel(self.main_connection).find(
            filter_data=dict(org_id=org_id),
            fields=['id', 'is_robot']
        )
        robots = [u for u in users if u['is_robot']]

        assert_that(robots, empty())

        # Проверяем, что не вызывались нужные event-ы:
        assert_that(mock_user_event.call_count, equal_to(0))

    def test_create_robots_with_custom_names(self):
        # Проверяем, что можем создавать роботов со стандартным и кастомным именем.
        # Кастомное имя на русском
        create_robot_for_service_and_org_id(self.meta_connection,
                                            self.main_connection,
                                            self.service_with_custom_robot_name['slug'],
                                            org_id=self.org1['id'])
        # Кастомное имя на английском
        create_robot_for_service_and_org_id(self.meta_connection,
                                            self.main_connection,
                                            self.service_with_custom_robot_name['slug'],
                                            org_id=self.en_org['id'])

        # Стандартное имя
        create_robot_for_service_and_org_id(self.meta_connection,
                                            self.main_connection,
                                            self.service['slug'],
                                            org_id=self.org1['id'])

        users = UserModel(self.main_connection).find(
            filter_data=dict(org_id=self.org1['id']),
            fields=['is_robot', 'name']
        ) + UserModel(self.main_connection).find(
            filter_data=dict(org_id=self.en_org['id']),
            fields=['is_robot', 'name']
        )

        robots = [u['name'] for u in users if u['is_robot']]

        ru_custom_robot_name = {
            'first': {
                'ru': self.service_with_custom_robot_name['robot_name']['ru'],
                'en': '%s Robot' % self.service_with_custom_robot_name['slug'].capitalize(),
            },
            'last': {'en': '', 'ru': ''}
        }
        en_custom_robot_name = {
            'first': {
                'ru': self.service_with_custom_robot_name['robot_name']['en'],
                'en': '%s Robot' % self.service_with_custom_robot_name['slug'].capitalize(),
            },
            'last': {'en': '', 'ru': ''}
        }
        standard_robot_name = {
            'first': {
                'en': '%s Robot' % self.service['slug'].capitalize(),
                'ru': 'Робот сервиса %s' % self.service['slug'].capitalize(),
            },
            'last': {'en': '', 'ru': ''}
        }
        assert_that(
            robots,
            contains_inanyorder(
                ru_custom_robot_name,
                en_custom_robot_name,
                standard_robot_name,
            )
        )

    def test_create_robot_with_uid(self):
        # Если у сервиса прописан robot_uid, то мы должны добавить в организацию именно его,
        # взяв данные из паспорта
        # и это должно работать даже для организации без домена
        org = create_organization_without_domain(
            self.meta_connection,
            self.main_connection,
        )
        org_id = org['organization']['id']

        robot_uid = 100500
        service = ServiceModel(self.meta_connection).create(
            slug='service-with-robot-uid',
            name='Service with custom robot',
            robot_required=True,
            robot_uid=robot_uid,
        )
        robot_nickname = 'yndx-robot-electronic'

        app.blackbox_instance.userinfo.return_value = fake_userinfo(
            login=robot_nickname,
        )

        create_robot_for_service_and_org_id(self.meta_connection,
                                            self.main_connection,
                                            service['slug'],
                                            org_id=org_id)

        robots = UserModel(self.main_connection) \
                .filter(org_id=org_id, is_robot=True) \
                .fields('nickname') \
                .all()

        assert_that(
            robots,
            contains(
                has_entries(
                    nickname=robot_nickname,
                    id=robot_uid,
                )
            )
        )

        # Убедимся, что робот не появился и в другой
        # организации, куда мы его не добавляли
        other_org_id = self.org1['id']
        robot_users = UserModel(self.main_connection) \
                      .fields('org_id', 'service_slug') \
                      .filter(org_id=other_org_id, id=robot_uid) \
                      .all()
        assert_that(
            robot_users,
            empty()
        )
        # убедиться, чтоб робот считается внутренним пользователем
        robots = UserMetaModel(self.meta_connection).\
            filter(org_id=org_id, id=robot_uid).\
            fields('is_outer', 'user_type').\
            all()
        assert_that(
            robots,
            contains(
                has_entries(
                    is_outer=False,
                    user_type='inner_user',
                )
            )
        )

    def test_create_two_robots_with_same_uid(self):
        # Проверим, что можно создать в двух организациях робота с одним и тем же uid
        robot_uid = 100500
        service = ServiceModel(self.meta_connection).create(
            slug='service-with-robot-uid',
            name='Service with custom robot',
            robot_required=True,
            robot_uid=robot_uid,
        )
        robot_nickname = 'yndx-robot-electronic'

        app.blackbox_instance.userinfo.return_value = fake_userinfo(
            login=robot_nickname,
        )

        org_ids = [self.org1['id'], self.organization['id']]

        for org_id in org_ids:
            create_robot_for_service_and_org_id(self.meta_connection,
                                                self.main_connection,
                                                service['slug'],
                                                org_id=org_id)

        resulting_org_ids = UserModel(self.main_connection) \
                            .filter(id=robot_uid) \
                            .scalar('org_id')

        assert_that(
            resulting_org_ids,
            contains_inanyorder(
                *org_ids
            )
        )

    def test_create_service_with_incorrect_robot_name(self):
        # Проверяем, что нельзя задать имя роботу не в виде {'ru': u'имя', 'en': 'name'}
        try:
            self.service_with_custom_robot_name = ServiceModel(self.meta_connection).create(
                client_id='kjsdjfhgJfhGJdmdbEThkS',
                slug='incorrect-robot-name-test-service',
                name='Incorrect robot name test service',
                robot_name={
                        'ru': 'Кастомное имя робота без en части.',
                },
                robot_required=True
            )
        except Exception as e:
            assert_that(str(e), equal_to('Custom robot name has a wrong scheme'))


class TestChangeIsEnabledRobotStatus(TestCase):
    def setUp(self):
        super(TestChangeIsEnabledRobotStatus, self).setUp()
        self.service = ServiceModel(self.meta_connection).create(
            client_id='1243',
            slug='wiki',
            name='Wiki services',
            robot_required=True
        )
        self.org1 = create_organization(
            self.meta_connection,
            self.main_connection,
            label='org1',
        )['organization']

        enable_service(
            self.meta_connection,
            self.main_connection,
            self.org1['id'],
            self.service['slug']
        )
        self.robot_id = create_robot_for_service_and_org_id(
            self.meta_connection,
            self.main_connection,
            self.service['slug'],
            org_id=self.org1['id'],
        )

    def test_disable_robot(self):
        # проверяем, что при выключении сервиса, его робот блокируется
        robot = RobotServiceModel(self.main_connection) \
                .filter(
                    org_id=self.org1['id'],
                    service_id=self.service['id'],
                ).one()

        assert_that(
            robot,
            has_entries(slug=self.service['slug'])
        )

        disable_service(
            self.meta_connection,
            self.main_connection,
            self.org1['id'],
            self.service['slug'],
            'some-reason',
        )
        self.process_tasks()

        assert_called_once(
            self.mocked_passport.block_user,
            self.robot_id,
        )

    def test_dont_disable_portal_robots(self):
        # проверяем, что при выключении сервиса, его робот блокируется
        slug = 'search'
        service = ServiceModel(self.meta_connection).create(
            client_id='12435',
            slug=slug,
            name='Search',
            robot_required=True,
            # Укажем, что для робота нужен портальный uid
            robot_uid=100500
        )
        org_id = self.organization['id']
        robot_nickname = 'yndx-robot-search'
        app.blackbox_instance.userinfo.return_value = fake_userinfo(
            login=robot_nickname,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            org_id,
            slug,
        )
        self.process_tasks()

        # проверим, что робот завёлся
        robot = RobotServiceModel(self.main_connection) \
            .filter(
                org_id=org_id,
                service_id=service['id'],
            ).one()

        assert_that(
            robot,
            has_entries(slug=slug)
        )

        # А теперь отключим сервис
        disable_service(
            self.meta_connection,
            self.main_connection,
            org_id,
            slug,
            'some-reason',
        )
        self.process_tasks()
        # И убедимся, что не пытались заблокировать робота
        assert_not_called(
            self.mocked_passport.block_user,
        )
        # Так же, не должно быть никаких упавших тасков в очереди
        self.assert_no_failed_tasks()

    def test_enable_robot(self):
        # проверяем, что при повторном включении сервиса его робот становится активным
        disable_service(
            self.meta_connection,
            self.main_connection,
            self.org1['id'],
            self.service['slug'],
            'some-reason',
        )
        self.process_tasks()

        enable_service(
            self.meta_connection,
            self.main_connection,
            self.org1['id'],
            self.service['slug'],
        )
        self.process_tasks()
        assert_called_once(
            self.mocked_passport.unblock_user,
            self.robot_id,
        )

    def test_singleton_uids(self):
        uids = singleton_robot_uids(self.meta_connection)
        self.assertEqual([], uids)

        ServiceModel(self.meta_connection).create(
            client_id='ijghjfhgJfh1JdmavEThkL',
            slug='forms',
            name='Forms services',
            robot_uid=11,
            robot_required=True
        )
        uids = singleton_robot_uids(self.meta_connection)
        self.assertEqual([11], uids)
