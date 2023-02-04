# -*- coding: utf-8 -*-
import datetime
import pytest
from hamcrest import (
    assert_that,
    contains,
    has_entries,
    none,
    is_not,
    empty,
    equal_to,
    contains_inanyorder,
    not_none,
)
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.core.commands.create_swagger_spec import \
    Command as CreateSwaggerSpecCommand

from intranet.yandex_directory.src.yandex_directory.core.commands.service_add import \
    Command as ServiceAddCommand

from intranet.yandex_directory.src.yandex_directory.core.commands.normalize_dismissed import \
    Command as NormalizeDismissedCommand

from intranet.yandex_directory.src.yandex_directory.core.commands.update_services_in_shards import \
    Command as UpdateServicesInShardsCommand

from intranet.yandex_directory.src.yandex_directory.core.commands.create_resources_for_tracker import \
    Command as CreateResourcesCommand

from intranet.yandex_directory.src.yandex_directory.core.commands.update_maillist_uids import \
    Command as UpdateMaillistUidsCommand

from intranet.yandex_directory.src.yandex_directory.core.commands import init_members_count
from intranet.yandex_directory.src.yandex_directory.common.models.types import ROOT_DEPARTMENT_ID
from intranet.yandex_directory.src.yandex_directory.core.models import (
    DepartmentModel,
    UserModel,
    UserDismissedModel,
    ServiceModel,
    OrganizationServiceModel,
    OrganizationModel,
    ResourceModel,
    GroupModel,
)
from testutils import (
    TestCase,
    TestYandexTeamOrgMixin,
    create_department,
    source_path,
)


class TestCreateSwaggerSpecCommand(TestCase):
    @pytest.mark.skip('Пока не настроим сборку документации в аркадии')
    def test_CreateSwaggerSpecCommand(self):
        # При ошибках в схемах будет кидать sys.exit(1), поэтому достаточно
        # вызова без проверок.

        CreateSwaggerSpecCommand()(app)


class TestInitSubMembersCount(TestCase):
    def setUp(self):
        super(TestInitSubMembersCount, self).setUp()

        org_id = self.organization['id']

        self.department1 = create_department(
            self.main_connection,
            org_id
        )
        self.department2 = create_department(
            self.main_connection,
            org_id,
            parent_id=self.department1['id']
        )
        self.department3 = create_department(
            self.main_connection,
            org_id,
            parent_id=self.department2['id']
        )
        self.department4 = create_department(
            self.main_connection,
            org_id
        )

        self.user31 = self.create_user(department_id=self.department3['id'])
        self.user32 = self.create_user(department_id=self.department3['id'])
        self.user33 = self.create_user(department_id=self.department3['id'])

        self.user21 = self.create_user(department_id=self.department2['id'])
        self.user22 = self.create_user(department_id=self.department2['id'])

        self.user11 = self.create_user(department_id=self.department1['id'])

    def test_InitSubMembersCount(self):
        org_id = self.organization['id']

        init_members_count._run_on_organization(
            self.main_connection,
            org_id,
        )
        get_dep = DepartmentModel(self.main_connection).get

        self.assertEqual(get_dep(self.department1['id'], org_id)['members_count'], 6)
        self.assertEqual(get_dep(self.department2['id'], org_id)['members_count'], 5)
        self.assertEqual(get_dep(self.department3['id'], org_id)['members_count'], 3)
        self.assertEqual(get_dep(self.department4['id'], org_id)['members_count'], 0)


class TestServiceAdd(TestCase):
    def test_service_add(self):
        ServiceModel(self.meta_connection).delete(force_remove_all=True)

        params = {
            'client_id': 'client_id',
            'slug': 'slug',
            'name': 'name',
            'robot_required': True
        }

        ServiceAddCommand().try_run(**params)

        assert_that(
            ServiceModel(self.meta_connection).find(),
            contains(
                has_entries(**params)
            )
        )


class TestUpdateServicesInShardsCommand(TestCase):
    def test_update_services(self):
        # 1) Загружаем в пустую таблицу шарда один сервис
        # 2) Обновляем запись на шарде
        # 3) Удаляем запись на шарде
        ServiceModel(self.meta_connection).delete(force_remove_all=True)
        ServiceModel(self.main_connection).delete(force_remove_all=True)

        service = ServiceModel(self.meta_connection).create(
            slug='service-slug',
            name='service-name',
            client_id='service-client_id',
        )
        # 1) Загружаем в пустую базу шарда один сервис
        UpdateServicesInShardsCommand().try_run()
        assert_that(
            ServiceModel(self.main_connection).find(),
            contains(
                has_entries(**service)
            )
        )

        service['slug'] = 'updated_service-slug'
        service = ServiceModel(self.meta_connection).update(
            filter_data={'id': service['id']},
            update_data=service,
        )
        # 2) Обновляем запись на шарде
        UpdateServicesInShardsCommand().try_run()
        assert_that(
            ServiceModel(self.main_connection).find(),
            contains(
                has_entries(**service)
            )
        )

        ServiceModel(self.meta_connection).delete(
            filter_data={'id': service['id']}
        )
        # 3) Удаляем запись на шарде
        UpdateServicesInShardsCommand().try_run()
        assert_that(
            ServiceModel(self.main_connection).find(),
            empty()
        )


class TestCreateResourcesForTrackerCommand(TestCase):
    def test_create_resources_for_tracker(self):
        # создадим трекер с paid_by_license=False, чтобы при добавлении в организацию, ресурсы не создались автоматом
        service = ServiceModel(self.meta_connection).create(
            slug='tracker',
            name='tracker',
            client_id='service-client_id',
            trial_period_months=4,
        )
        assert_that(
            service['trial_period_months'],
            equal_to(4),
        )

        # создадим еще одну организацию
        another_org = OrganizationModel(self.main_connection).create(
            id=123,
            name={'ru': 'НеЯндекс'},
            language='ru',
            label='yandex-money',
            admin_uid=321
        )

        # добавим трекер в организации, но не будем включать его
        org_service1 = OrganizationServiceModel(self.main_connection).create(self.organization['id'], service['id'])
        org_service2 = OrganizationServiceModel(self.main_connection).create(another_org['id'], service['id'])

        # проверим, что ресурсы и дата окончания триального периода не создались
        assert_that(
            org_service1['trial_expires'],
            equal_to(None),
        )
        assert_that(
            org_service1['resource_id'],
            equal_to(None),
        )

        org1 = {
            'org_id': self.organization['id'],
            'service_id': service['id'],
        }
        org2 = {
            'org_id': another_org['id'],
            'service_id': service['id'],
        }
        # обновим даты ready_at
        OrganizationServiceModel(self.main_connection).update(
            filter_data=org1,
            update_data={
                'ready': True,
                'ready_at': datetime.datetime(2017, 2, 1, 12, 00)
            },
        )
        OrganizationServiceModel(self.main_connection).update(
            filter_data=org2,
            update_data={
                'ready': True,
                'ready_at': datetime.datetime(2017, 8, 10, 12, 00)
            },
        )

        CreateResourcesCommand().try_run()
        org_service1 = OrganizationServiceModel(self.main_connection).find(filter_data=org1, one=True)
        org_service2 = OrganizationServiceModel(self.main_connection).find(filter_data=org2, one=True)

        # проверим, что даты окончания триального периода проставились правильно
        assert_that(
            org_service1['trial_expires'],
            equal_to(datetime.date(2017, 11, 30)),
        )
        assert_that(
            org_service2['trial_expires'],
            equal_to(datetime.date(2017, 12, 10)),
        )

        # проверим, что создались ресурсы
        resource1 = ResourceModel(self.main_connection).find(filter_data={
            'id': org_service1['resource_id'],
            'org_id': self.organization['id'],
        })
        resource2 = ResourceModel(self.main_connection).find(filter_data={
            'id': org_service2['resource_id'],
            'org_id': another_org['id'],
        })

        self.assertEqual(len(resource1), 1)
        self.assertEqual(len(resource2), 1)


class TestUpdateMaillistUidsCommand(TestCase):
    def test_update_maillist_uids(self):
        group_with_uid = self.create_group(label='group_with_uid', uid=123)
        group1 = self.create_group(label='group1')
        self.create_group()

        department_with_uid = self.create_department(label='dep_with_uid', uid=456)
        department1 = self.create_department(label='dep')

        def test_uid(*args):
            # используем хеш от email в качестве id рассылки
            return {'uid': hash(args[-1])}

        UpdateMaillistUidsCommand().try_run(org_id=self.organization['id'])

        # проверяем, что uid обновился только у групп и отделов с label и пустым uid
        groups = GroupModel(self.main_connection).find(
            filter_data={'uid__isnull': False},
            fields=['id', 'uid']
        )
        assert_that(
            groups,
            contains_inanyorder(
                has_entries(
                    id=group_with_uid['id'],
                    uid=123,
                ),
                has_entries(
                    id=group1['id'],
                    uid=not_none(),
                ),
            )
        )
        departments = DepartmentModel(self.main_connection).find(
            filter_data={'uid__isnull': False},
            fields=['id', 'uid']
        )
        assert_that(
            departments,
            contains_inanyorder(
                has_entries(
                    id=department_with_uid['id'],
                    uid=456,
                ),
                has_entries(
                    id=department1['id'],
                    uid=not_none(),
                ),
            )
        )
