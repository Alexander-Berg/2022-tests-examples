# -*- coding: utf-8 -*-
import datetime

import pytest
from hamcrest import (
    assert_that,
    equal_to,
    contains,
    contains_inanyorder,
    has_entries,
    has_item,
    not_,
    has_entry,
    has_length,
)

from unittest.mock import patch, ANY
from dateutil.relativedelta import relativedelta
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    ServiceModel,
    ServicesLinksModel,
    OrganizationServiceModel,
    enable_service,
    disable_service,
    disable_licensed_services_by_debt,
    disable_licensed_services_by_trial,
    disable_licensed_services_by_inactive_contracts,
    disable_licensed_services_by_org_blocked,
    UserServiceLicenses,
    reason,
    notify_about_trail_ended,
    on_service_enable,
    MAILLIST_SERVICE_SLUG,
)
from intranet.yandex_directory.src.yandex_directory.core.commands.update_services_in_shards import Command as UpdateServicesInShards
from intranet.yandex_directory.src.yandex_directory.core.models.organization import (
    OrganizationLicenseConsumedInfoModel,
    OrganizationModel, organization_type,
)
from intranet.yandex_directory.src.yandex_directory.common.models.base import ALL_FIELDS
from intranet.yandex_directory.src.yandex_directory.core.actions import action
from intranet.yandex_directory.src.yandex_directory.core.events import event
from intranet.yandex_directory.src.yandex_directory.core.models import (
    ActionModel,
    EventModel,
    TaskModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.resource import ResourceModel
from intranet.yandex_directory.src.yandex_directory.core.utils.robots import get_robot_nickname
from intranet.yandex_directory.src.yandex_directory.common.exceptions import (
    AuthorizationError,
    ServiceNotLicensed,
    ServiceNotFound,
)
from intranet.yandex_directory.src.yandex_directory.core.exceptions import (
    OrganizationHasDebt,
)
from testutils import (
    TestCase,
    override_settings,
    create_organization,
    is_same,
    frozen_time,
    override_mailer,
    fake_userinfo,
)

from intranet.yandex_directory.src.yandex_directory.common.utils import (
    Ignore,
    utcnow,
)
from intranet.yandex_directory.src.yandex_directory.core.utils import only_attrs
from intranet.yandex_directory.src.yandex_directory.core.commands.disable_service_trial_expired import Command as DisableServiceTrialExpiredCommand
from intranet.yandex_directory.src.yandex_directory.connect_services.partner_disk.tasks import DeleteSpacePartnerDiskTask
from intranet.yandex_directory.src.yandex_directory.auth import tvm
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.core.models import UserModel


class TestServiceModel(TestCase):
    def setUp(self):
        super(TestServiceModel, self).setUp()
        ServiceModel(self.meta_connection).delete(force_remove_all=True)
        self.client_id = 'clientid'
        self.slug = 'slug'
        self.name = 'Сервис'
        self.create_params = {
            'client_id': self.client_id,
            'slug': self.slug,
            'name': self.name,
        }

    def test_create(self):
        # создаем новую запись

        service = ServiceModel(self.meta_connection).create(
            **self.create_params
        )

        # в пустой таблице появилась запись
        assert_that(
            ServiceModel(self.meta_connection).count(),
            equal_to(1)
        )
        assert_that(
            service,
            has_entries(
                client_id=self.client_id,
                slug=self.slug,
                name=self.name,
                # Но если включили, то считаем, что он сразу же
                # становится работоспособен.
                ready_default=True,
            )
        )

    def test_get(self):
        actual_service = ServiceModel(self.meta_connection).create(
            **self.create_params
        )

        # получаем данные по id
        assert_that(
            ServiceModel(self.meta_connection).get(actual_service['id']),
            actual_service
        )

    def test_delete(self):
        created_service = ServiceModel(self.meta_connection).create(
            **self.create_params
        )
        # в пустой таблице появилась запись
        assert_that(
            ServiceModel(self.meta_connection).count(),
            equal_to(1)
        )

        # удаление по неизвестному id ничего не удалило
        unknown_service_id = created_service['id'] + 42
        ServiceModel(self.meta_connection).delete_one(unknown_service_id)
        assert_that(
            ServiceModel(self.meta_connection).count(),
            equal_to(1)
        )

        # а по неизвестному id удалило
        ServiceModel(self.meta_connection).delete_one(created_service['id'])
        assert_that(
            ServiceModel(self.meta_connection).count(),
            equal_to(0)
        )

    def test_find(self):
        # поиск по возможным фильтрам
        created_service = ServiceModel(self.meta_connection).create(
            **self.create_params
        )
        filters_data = [
            ('client_id', self.client_id),
            ('id', created_service['id']),
            ('slug', created_service['slug']),
        ]

        for field, value in filters_data:
            finded = ServiceModel(self.meta_connection).find(
                filter_data={field: value}
            )
            assert_that(
                finded,
                contains(created_service)
            )

    def test_find_all_fields(self):
        ServiceModel(self.meta_connection).create(
            **self.create_params
        )
        self.create_params = {
            'slug': self.slug,
            'data_by_tld': {'csbhcdh': 4},
        }

        ServicesLinksModel(self.meta_connection).create(
            **self.create_params
        )

        # получаем данные и проверяем что поля указанные в select_related_fields
        # берутся из ServicesLinksModel
        assert_that(
            ServiceModel(self.meta_connection).find(
                filter_data={'slug': self.slug},
                fields=ALL_FIELDS,
            ),
            contains(
                has_entries(
                    data_by_tld=self.create_params['data_by_tld'])
            )
        )

    def test_filter_by_tvm__client_id(self):
        # Проверяем, что если сервис имеет несколько tvm_client_id,
        # с помощью find можно будет его найти
        service = ServiceModel(self.meta_connection).create(
            name='blah',
            slug='blah',
            tvm_client_ids=[1, 2, 3],
        )
        # Создадим второй сервис, чтобы удостовериться, что find
        # не отдаёт всё подряд.
        ServiceModel(self.meta_connection).create(**self.create_params)

        # Теперь проверим, что сервис найдётся по tvm_client_id=2:
        result = ServiceModel(self.meta_connection).find(
            filter_data={'tvm_client_ids__contains': 2}
        )
        assert_that(
            result,
            contains(is_same(service)),
        )

    def test_get_licensed_service_by_slug(self):
        # передаем несуществующий сервис
        with pytest.raises(ServiceNotFound) as err:
            ServiceModel(self.meta_connection).get_licensed_service_by_slug('unknown_service')
        self.assertEqual(err.value.code, 'service_not_found')

        # передаем сервис без лицензий
        not_licensed_service = ServiceModel(self.meta_connection).create(
            **self.create_params
        )
        with pytest.raises(ServiceNotLicensed) as err:
            ServiceModel(self.meta_connection).get_licensed_service_by_slug(not_licensed_service['slug'])
        self.assertEqual(err.value.code, 'service_not_licensed')

        # сервис с лицензиями ошибок не вызывает
        licensed_service = ServiceModel(self.meta_connection).create(
            name='blah',
            slug='blah',
            client_id='some_id',
            paid_by_license=True,
        )
        service = ServiceModel(self.meta_connection).get_licensed_service_by_slug(licensed_service['slug'])
        assert_that(
            service['id'],
            equal_to(licensed_service['id'])
        )

    def test_get_object_than_does_not_exist_in_services_links(self):
        # проверим, что выводятся поля указанные в select_related, когда такого сервиса нет в ServicesLinks.
        # И выводятся с дефолтными значениями
        ServiceModel(self.meta_connection).create(
            slug='not_in_services_links',
            name='not_in_services_links',
            client_id='some_id',
        )
        service = ServiceModel(self.meta_connection).find(
            filter_data={'slug': 'not_in_services_links'},
            fields=ALL_FIELDS,
        )
        assert_that(
            service,
            contains(
                has_entries(
                    available_for_external_admin=False,
                    data_by_language={},
                    data_by_tld={},
                    in_new_tab=False,
                    priority=1,
                    show_in_header=False,
                )
            )
        )

    def test_filter_and_fields_model(self):
        # проверим чтобы была возможность выводить и фильтровать модель по полям, которых нет в базе
        # и нет в базе, с которой связаны эти поля в select_related_fields
        ServiceModel(self.meta_connection).create(
            slug='not_in_services_links',
            name='not_in_services_links',
            client_id='some_id',
        )
        services = ServiceModel(self.meta_connection).find(
            filter_data={
                'available_for_external_admin': False,
            },
            fields=['slug', 'available_for_external_admin'],
        )
        assert_that(
            services[0],
            has_entries(slug='not_in_services_links'),
            has_entries(available_for_external_admin=False),
        )
        # а теперь добавим в таблицу services_links
        ServicesLinksModel(self.meta_connection).create(
            slug='not_in_services_links',
            available_for_external_admin=True,
        )
        services = ServiceModel(self.meta_connection).find(
            filter_data={
                'available_for_external_admin': True,
            },
            fields=['slug', 'available_for_external_admin'],
        )
        assert_that(
            services[0],
            has_entries(slug='not_in_services_links'),
            has_entries(available_for_external_admin=True),
        )

    def test_filter_model_where_available_for_external_admin_is_number(self):
        # проверим чтобы была возможность фильтровать модель по полям available_for_external_admin
        # с возможными значениями 0 и 1
        ServiceModel(self.meta_connection).create(
            slug='not_in_services_links',
            name='not_in_services_links',
            client_id='some_id',
        )
        services = ServiceModel(self.meta_connection).find(
            filter_data={
                'available_for_external_admin': 0,
            },
            fields=['slug', 'available_for_external_admin'],
        )
        assert_that(
            services[0],
            has_entries(slug='not_in_services_links'),
            has_entries(available_for_external_admin=False),
        )
        # # а теперь добавим в таблицу services_links
        ServicesLinksModel(self.meta_connection).create(
            slug='not_in_services_links',
            available_for_external_admin=True,
        )
        services = ServiceModel(self.meta_connection).find(
            filter_data={
                'available_for_external_admin': 1,
            },
            fields=['slug', 'available_for_external_admin'],
        )
        assert_that(
            services[0],
            has_entries(slug='not_in_services_links'),
            has_entries(available_for_external_admin=True),
        )

    def test_filter_model_where_available_for_external_admin_is_not_valid(self):
        # проверим, что выдает ошибку невалидных query parameters
        ServiceModel(self.meta_connection).create(
            slug='not_in_services_links',
            name='not_in_services_links',
            client_id='some_id',
        )
        from intranet.yandex_directory.src.yandex_directory.core.exceptions import (
            ValidationQueryParametersError,
        )
        with self.assertRaises(ValidationQueryParametersError):
            ServiceModel(self.meta_connection).find(
                filter_data={
                    'available_for_external_admin': '{}',
                },
                fields=['slug', 'available_for_external_admin'],
            )


class TestOrganizationServiceModel__create(TestCase):
    def test_should_save_ready_at_if_service_is_ready_on_creating(self):
        service = ServiceModel(self.meta_connection).create(
            client_id='some-client-id',
            slug='new_service',
            name='Service',
            robot_required=True,
        )
        with frozen_time():
            organization_service = OrganizationServiceModel(self.main_connection).create(
                org_id=self.organization['id'],
                service_id=service['id'],
                ready=True,
            )
            now = utcnow()
            assert_that(
                organization_service['ready_at'],
                equal_to(now),
            )

    def test_should_not_save_ready_at_if_service_is_not_ready_on_creating(self):
        service = ServiceModel(self.meta_connection).create(
            client_id='some-client-id',
            slug='new_service',
            name='Service',
            robot_required=True,
        )
        organization_service = OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=service['id'],
        )
        assert_that(
            organization_service['ready_at'],
            equal_to(None),
        )

    def test_create_organization_service_with_trial(self):
        service = ServiceModel(self.meta_connection).create(
            client_id='some-client-id',
            slug='new_service',
            name='Service',
            robot_required=True,
            trial_period_months=1,
            paid_by_license=True,
        )
        with frozen_time():
            organization_service = OrganizationServiceModel(self.main_connection).create(
                org_id=self.organization['id'],
                service_id=service['id'],
                ready=True,
            )

            assert_that(
                organization_service['ready_at'],
                equal_to(utcnow()),
            )

            assert_that(
                organization_service['trial_expires'],
                equal_to((utcnow() + relativedelta(months=1)).date()),
            )

            resource = ResourceModel(self.main_connection).find(filter_data={'id': organization_service['resource_id']})
            self.assertEqual(len(resource), 1)


class TestOrganizationServiceModel__find(TestCase):
    def test_should_find_organizations_without_robot(self):
        # проверяем, что метод find_organizations_without_robot должен
        # вернет id организаций, в которых не создан робот для указанного сервиса
        service = ServiceModel(self.meta_connection).create(
            client_id='some-client-id',
            slug='new_service',
            name='Service',
            robot_required=True,
        )
        organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='some-org'
        )['organization']
        disabled_service_org = create_organization(
            self.meta_connection,
            self.main_connection,
            label='some-second-org'
        )['organization']
        other_env_org = create_organization(
            self.meta_connection,
            self.main_connection,
            label='another-env-org'
        )['organization']
        OrganizationModel(self.main_connection).update_one(other_env_org['id'], {'environment': 'other'})

        organization_service_model = OrganizationServiceModel(self.main_connection)

        # "добавим" сервис в организации без создания робота
        organization_service = organization_service_model.create(
            org_id=organization['id'],
            service_id=service['id'],
        )
        organization_service_model.create(
            org_id=other_env_org['id'],
            service_id=service['id'],
        )
        # добавим сервис без робота и сразу выключим его
        organization_service_model.create(
            org_id=disabled_service_org['id'],
            service_id=service['id'],
        )
        disable_service(
            self.meta_connection,
            self.main_connection,
            org_id=disabled_service_org['id'],
            service_slug=service['slug'],
            disable_reason='some-reason',
        )

        # проверим, что вернется только организация из текущего окружения, где сервис включен
        organizations_without_robot = organization_service_model.find_organizations_without_robot(
            service_slug=service['slug'],
            service_id=service['id'],
            nickname=get_robot_nickname(service['slug']),
        )
        assert_that(organizations_without_robot, equal_to([organization['id']]))

        # теперь включим сервис нормально
        organization_service_model.delete_one(organization_service['id'])

        enable_service(
            self.meta_connection,
            self.main_connection,
            org_id=organization['id'],
            service_slug=service['slug'],
        )
        self.process_tasks()

        # и список организаций без роботов должен быть пустым
        organizations_without_robot = organization_service_model.find_organizations_without_robot(
            service_slug=service['slug'],
            service_id=service['id'],
            nickname=get_robot_nickname(service['slug']),
        )
        assert_that(organizations_without_robot, equal_to([]))

    def test_filter_disabled_services(self):
        # Проверяем как фильтруется запросы по полю enabled,
        # учитываем значение Ignore.
        service1 = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='new_service1',
            name='Service1',
            robot_required=True,
        )
        service2 = ServiceModel(self.meta_connection).create(
            client_id='some-client-id2',
            slug='new_service2',
            name='Service2',
            robot_required=True,
        )
        organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='some-org'
        )['organization']
        organization_service_model = OrganizationServiceModel(self.main_connection)

        organization_service1 = organization_service_model.create(
            org_id=organization['id'],
            service_id=service1['id'],
        )
        organization_service2 = organization_service_model.create(
            org_id=organization['id'],
            service_id=service2['id'],
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            org_id=organization['id'],
            service_slug=service1['slug'],
        )
        disable_service(
            self.meta_connection,
            self.main_connection,
            org_id=organization['id'],
            service_slug=service2['slug'],
            disable_reason='some-reason',
        )
        filter_data = {'org_id': organization['id']}
        assert_that(
            organization_service_model.find(filter_data=filter_data),
            contains_inanyorder(
                has_entries(
                    id=organization_service1['id'],
                    enabled=True,
                )
            )
        )
        filter_data = {'org_id': organization['id'], 'enabled': Ignore}
        assert_that(
            organization_service_model.find(filter_data=filter_data),
            contains_inanyorder(
                has_entries(
                    id=organization_service1['id'],
                    enabled=True,
                ),
                has_entries(
                    id=organization_service2['id'],
                    enabled=False,
                )
            )
        )
        filter_data = {'org_id': organization['id'], 'enabled': True}
        assert_that(
            organization_service_model.find(filter_data=filter_data),
            contains_inanyorder(
                has_entries(
                    id=organization_service1['id'],
                    enabled=True,
                )
            )
        )
        filter_data = {'org_id': organization['id'], 'enabled': False}
        assert_that(
            organization_service_model.find(filter_data=filter_data),
            contains_inanyorder(
                has_entries(
                    id=organization_service2['id'],
                    enabled=False,
                )
            )
        )

    def test_reenable_service_shouldnt_raise_an_error(self):
        # Проверим, что сервис можно выключить, а потом снова включить
        # и при этом не произойдёт ошибки, описанной  в
        # https://st.yandex-team.ru/TOOLSB-163
        org_id = self.organization['id']
        slug = 'new_service'
        ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug=slug,
            name='Service1',
            robot_required=True,
        )

        # Замокаем паспорт, чтобы при заведении
        # роботного аккаунта не ходить в паспорт
        with patch('intranet.yandex_directory.src.yandex_directory.passport.client.PassportApiClient.account_add') as account_add:
            # в момент запуска автотестов не ходим в паспорт, а хардкодим uid в Паспортном диапазоне
            account_add.return_value = 111*10**13 + 200
            enable_service(
                self.meta_connection,
                self.main_connection,
                org_id=org_id,
                service_slug=slug
            )
            disable_service(
                self.meta_connection,
                self.main_connection,
                org_id=org_id,
                service_slug=slug,
                disable_reason='some-reason',
            )

            # Снова включим сервис
            enable_service(
                self.meta_connection,
                self.main_connection,
                org_id=org_id,
                service_slug=slug
            )

        # И убедимся, что он включился
        organization_service_model = OrganizationServiceModel(self.main_connection)
        assert_that(
            organization_service_model.find(
                filter_data={'org_id': org_id},
                one=True,
            ),
            has_entries(
                enabled=True,
            )
        )

    def test_filter_resource_id_isnull(self):
        service1 = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='new_service1',
            name='Service1',
            paid_by_license=True,
        )
        service2 = ServiceModel(self.meta_connection).create(
            client_id='some-client-id2',
            slug='new_service2',
            name='Service2',
            paid_by_license=True,
        )
        service3 = ServiceModel(self.meta_connection).create(
            client_id='some-client-id3',
            slug='new_service3',
            name='Service3',
            paid_by_license=False,
        )
        for service in [service1, service2, service3]:
            enable_service(
                self.meta_connection,
                self.main_connection,
                org_id=self.organization['id'],
                service_slug=service['slug'],
            )
        org_licensed_services = OrganizationServiceModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
                'resource_id__isnull': False,
            }
        )
        self.assertEqual(len(org_licensed_services), 2)
        assert_that(
            org_licensed_services,
            contains_inanyorder(
                has_entries(
                    service_id=service1['id']
                ),
                has_entries(
                    service_id=service2['id']
                )
            )
        )


class TestOrganizationServiceModel__get_licensed_service_resource_id(TestCase):
    def test_get_licensed_service_resource_id(self):
        service = ServiceModel(self.meta_connection).create(
            client_id='some-client-id',
            slug='new_service',
            name='Service',
            robot_required=True,
            trial_period_months=1,
            paid_by_license=True,
        )
        organization_service = OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=service['id'],
            ready=True,
        )

        assert_that(
            organization_service['resource_id'],
            equal_to(
                OrganizationServiceModel(self.main_connection).get_licensed_service_resource_id(
                    self.organization['id'],
                    service['slug'],
                )
            )
        )

    def test_get_licensed_service_resource_id_invalid_service(self):
        # несуществующий сервис
        with pytest.raises(ServiceNotFound) as err:
            OrganizationServiceModel(self.main_connection).get_licensed_service_resource_id(
                self.organization['id'],
                '123',
            )
        self.assertEqual(err.value.code, 'service_not_found')

        # сервис без лицензий
        service = ServiceModel(self.meta_connection).create(
            client_id='some-client-id',
            slug='new_service',
            name='Service',
        )
        with pytest.raises(ServiceNotLicensed) as err:
            OrganizationServiceModel(self.main_connection).get_licensed_service_resource_id(
                self.organization['id'],
                service['slug'],
            )
        self.assertEqual(err.value.code, 'service_not_licensed')

        # сервис не подключен к организации
        service_licensed = ServiceModel(self.meta_connection).create(
            client_id='some-client-id-123',
            slug='new_service_licensed',
            name='Service licensed',
            paid_by_license=True,
        )
        with pytest.raises(AuthorizationError) as err:
            OrganizationServiceModel(self.main_connection).get_licensed_service_resource_id(
                self.organization['id'],
                service_licensed['slug'],
            )
        self.assertEqual(err.value.code, 'service_is_not_enabled')


class TestOrganizationServiceModel__get_org_services_with_licenses(TestCase):
    def setUp(self):
        super(TestOrganizationServiceModel__get_org_services_with_licenses, self).setUp()
        self.tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            paid_by_license=True,
            ready_default=True,
            trial_period_months=1,
        )
        self.wiki = ServiceModel(self.meta_connection).create(
            client_id='some-client-id2',
            slug='wiki',
            name='wiki',
            paid_by_license=True,
            ready_default=True,
            trial_period_months=0,
        )
        self.services = [self.tracker, self.wiki]
        for service in self.services:
            enable_service(
                self.meta_connection,
                self.main_connection,
                self.organization['id'],
                service['slug'],
            )
        self.resource_ids = {}
        for service in self.services:
            self.resource_ids[service['slug']] = OrganizationServiceModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'service_id': service['id'],
                },
                fields=['resource_id'],
                one=True
            )['resource_id']

    def test_simple(self):
        # проверим, что отдаются все подключенные сервисы с лицензиями
        disabled_service = ServiceModel(self.meta_connection).create(
            client_id='some-client-id3',
            slug='abc',
            name='abc',
            paid_by_license=True,
            ready_default=True,
            trial_period_months=1,
        )
        # включим и выключим сервис, чтобы была запись с enabled=False
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            disabled_service['slug'],
        )
        disable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            disabled_service['slug'],
            'some-reason',
        )
        # обычный сервис
        common_service = ServiceModel(self.meta_connection).create(
            client_id='some-client-id4',
            slug='common_service',
            name='common_service',
            ready_default=True,
        )
        licensed_services = OrganizationServiceModel(self.main_connection).get_org_services_with_licenses(
            self.organization['id']
        )
        exp_result = {}
        for service in self.services:
            exp_result[self.resource_ids[service['slug']]] = service
        assert_that(
            licensed_services,
            equal_to(exp_result)
        )

    def test_only_ids(self):
        # проверим, что возвращаются только id сервисов, если only_id=True
        licensed_services = OrganizationServiceModel(self.main_connection).get_org_services_with_licenses(
            self.organization['id'],
            only_id=True,
        )
        exp_result = {}
        for service in self.services:
            exp_result[self.resource_ids[service['slug']]] = service['id']
        assert_that(
            licensed_services,
            equal_to(exp_result)
        )

    def test_trial_expired(self):
        # проверим, что возвращаются только сервисы, у которых истек триальный период
        licensed_services = OrganizationServiceModel(self.main_connection).get_org_services_with_licenses(
            self.organization['id'],
            trial_expired=True,
            only_id=True,
        )
        # вернется только вики, потому что триального периода у нее нет
        assert_that(
            licensed_services,
            equal_to({self.resource_ids[self.wiki['slug']]: self.wiki['id']})
        )
        # обновляем дату окончания триального периода трекера
        self.update_service_trial_expires_date(
            self.organization['id'],
            self.tracker['id'],
            utcnow() - datetime.timedelta(days=50)
        )

        # теперь вернутся оба сервиса
        licensed_services = OrganizationServiceModel(self.main_connection).get_org_services_with_licenses(
            self.organization['id'],
            trial_expired=True,
            only_id=True,
        )
        exp_result = {}
        for service in self.services:
            exp_result[self.resource_ids[service['slug']]] = service['id']
        assert_that(
            licensed_services,
            equal_to(exp_result)
        )

    def test_service_ids(self):
        # проверим, что возвращаются только указанные сервисы, если они подключены и оплачиваются по лицензиям

        # обычный сервис
        common_service = ServiceModel(self.meta_connection).create(
            client_id='some-client-id4',
            slug='common_service',
            name='common_service',
            ready_default=True,
        )
        licensed_services = OrganizationServiceModel(self.main_connection).get_org_services_with_licenses(
            self.organization['id'],
            service_ids=[self.wiki['id'], self.tracker['id'], common_service['id']],
            only_id=True,
        )
        exp_result = {}
        for service in self.services:
            exp_result[self.resource_ids[service['slug']]] = service['id']
        assert_that(
            licensed_services,
            equal_to(exp_result)
        )

        # запрашиваем обычный сервис
        licensed_services = OrganizationServiceModel(self.main_connection).get_org_services_with_licenses(
            self.organization['id'],
            service_ids=common_service['id'],
            only_id=True,
        )
        assert_that(
            licensed_services,
            equal_to({})
        )


class TestOrganizationServiceModel__notify_about_trial_expiration(TestCase):
    send_email_path = 'intranet.yandex_directory.src.yandex_directory.core.mailer.utils.send_email_to_admins'
    notify_path = 'intranet.yandex_directory.src.yandex_directory.core.mailer.cloud_notify.client.Notifier.notify'

    def test_should_send_email(self):
        email_id = 'some-id'
        mail_config = {
            self.service['slug']: {
                30: email_id,
            }
        }
        with override_settings(MAIL_IDS_BEFORE_TRIAL_END=mail_config):
            with patch(self.send_email_path) as mocked_send_email_to_admins:
                OrganizationServiceModel(self.main_connection)._notify_about_trial_expiration(
                    org_id=self.organization['id'],
                    service_slug=self.service['slug'],
                    rest_days=30,
                )

                mocked_send_email_to_admins.assert_called_once_with(
                    ANY,
                    ANY,
                    self.organization['id'],
                    email_id,
                    lang='ru',
                    tld='ru',
                    organization_name=self.organization['name'],
                )

    @override_mailer()
    def test_should_send_cloud_email(self):
        UserModel(self.main_connection).update(filter_data={'id': self.admin_uid}, update_data={'cloud_uid': 'wow-1'})
        OrganizationModel(self.main_connection).change_organization_type(
            org_id=self.organization['id'],
            new_org_type=organization_type.cloud,
        )
        with patch(self.notify_path) as mocked_notify:
            app.blackbox_instance.userinfo.return_value = fake_userinfo(
                default_email='hellokitty@yandex.ru',
                login='hellokitty',
            )
            OrganizationServiceModel(self.main_connection)._notify_about_trial_expiration(
                org_id=self.organization['id'],
                service_slug='tracker',
                rest_days=1,
            )
            self.process_tasks()
        assert_that(
            len(mocked_notify.call_args_list),
            equal_to(1)
        )
        assert_that(
            mocked_notify.call_args_list[0][1]['template_name'],
            equal_to('tracker.trial.expire-soon-1-day')
        )

    @override_mailer()
    def test_should_not_send_cloud_email(self):
        UserModel(self.main_connection).update(filter_data={'id': self.admin_uid}, update_data={'cloud_uid': 'wow-1'})
        OrganizationModel(self.main_connection).change_organization_type(
            org_id=self.organization['id'],
            new_org_type=organization_type.cloud,
        )
        with patch(self.notify_path) as mocked_notify:
            app.blackbox_instance.userinfo.return_value = fake_userinfo(
                default_email='hellokitty@yandex.ru',
                login='hellokitty',
            )
            OrganizationServiceModel(self.main_connection)._notify_about_trial_expiration(
                org_id=self.organization['id'],
                service_slug='tracker',
                rest_days=3,
            )
            self.process_tasks()
        assert_that(
            len(mocked_notify.call_args_list),
            equal_to(0)
        )


class TestOrganizationServiceModel__send_mail_about_trial_expiration_if_needed(TestCase):
    def test_should_send_email(self):
        # проверим, что при наличи двух подключенных в организации сервисов мы отправим письмо только по тем,
        # у которых действительно скоро заканчивается триальный период
        service_with_trial = ServiceModel(self.meta_connection).create(
            client_id='some-client-id2',
            slug='new_service2',
            name='Service2',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            service_with_trial['slug'],
        )
        org_service_with_trial = OrganizationServiceModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
                'service_id': service_with_trial['id'],
            },
            one=True,
        )

        email_id = 'some-id'
        mail_config = {
            self.service['slug']: {
                30: 'another-email-id',
            },
            service_with_trial['slug']: {
                (org_service_with_trial['trial_expires'] - utcnow().date()).days: email_id,
            }
        }
        with override_settings(MAIL_IDS_BEFORE_TRIAL_END=mail_config):
            with patch('intranet.yandex_directory.src.yandex_directory.core.mailer.utils.send_email_to_admins') as mocked_send_email_to_admins:
                OrganizationServiceModel(self.main_connection).send_mail_about_trial_expiration_if_needed(
                    org_id=self.organization['id'],
                )

                mocked_send_email_to_admins.assert_called_once_with(
                    ANY,
                    ANY,
                    self.organization['id'],
                    email_id,
                    lang='ru',
                    tld='ru',
                    organization_name=self.organization['name'],
                )


class TestOrganizationServiceModel__dependencies(TestCase):
    def test_enable_service_dependencies(self):
        # Создадим 4 сервиса: A, B, C, D
        # и смоделируем ситуацию, когда сервис A зависит от B, а B от C.
        # Включим сервис A и убедимся, что B и C тоже включились.

        service_a = ServiceModel(self.meta_connection).create(
            slug='a',
            name='Service A',
        )
        ServiceModel(self.meta_connection).create(slug='b', name='Service B')
        ServiceModel(self.meta_connection).create(slug='c', name='Service C')
        ServiceModel(self.meta_connection).create(slug='d', name='Service D')

        org_id = self.organization['id']

        # Установим нужные нам зависимости
        from intranet.yandex_directory.src.yandex_directory.core.dependencies import Service, Setting

        header_name = 'header-name'
        deps = {
            Service('a'): [Service('b'), Setting('header', header_name)],
            Service('b'): [Service('c'), Setting('shared_contacts', True)],
        }

        with patch('intranet.yandex_directory.src.yandex_directory.core.dependencies.dependencies', new=deps):
            enable_service(
                self.meta_connection,
                self.main_connection,
                org_id=org_id,
                service_slug=service_a['slug'],
            )

        org = OrganizationModel(self.main_connection).find(
            {
                'id': org_id,
            },
            fields=['services.slug', 'header', 'shared_contacts'],
            one=True,
        )
        services = org['services']

        slugs = only_attrs(services, 'slug')
        assert_that(
            slugs,
            contains_inanyorder('a', 'b', 'c')
        )

        # при активации сервисов выставились соответствующие настройки
        assert_that(
            org,
            has_entries(
                header=header_name,
                shared_contacts=True,
            )
        )


class Test__disable_service(TestCase):
    def setUp(self):
        super(Test__disable_service, self).setUp()
        self.service = ServiceModel(self.meta_connection).create(
            client_id='some-client-id',
            slug='new_service',
            name='Service',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            self.service['slug'],
        )
        self.org_service_model = OrganizationServiceModel(self.main_connection)
        self.filter_data = {
            'org_id': self.organization['id'],
            'service_id': self.service['id'],
        }

    def test_trial_expires_not_update_while_trial(self):
        # проверяем, что дата окончания триального периода не обновлется,
        # если сервис отключили во время триала

        org_service = self.org_service_model.find(
            filter_data=self.filter_data,
            fields=['trial_expires'],
            one=True,
        )
        trial_expired_date = (utcnow() + relativedelta(months=self.service['trial_period_months'])).date()
        assert_that(
            org_service['trial_expires'],
            equal_to(trial_expired_date)
        )
        disable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            self.service['slug'],
            'some-reason',
        )
        self.filter_data['enabled'] = False
        org_service = self.org_service_model.find(
            filter_data=self.filter_data,
            fields=['trial_expires'],
            one=True,
        )
        assert_that(
            org_service['trial_expires'],
            equal_to(trial_expired_date)
        )

    def test_trial_expires_not_update_after_trial(self):
        # проверяем, что дата окончания триального периода не меняется,
        # если сервис выключили после окончания триала

        # обновляем дату окончания триала
        last_week_date = (utcnow() - relativedelta(weeks=1)).date()
        self.org_service_model.update(
            update_data={'trial_expires': last_week_date},
            filter_data=self.filter_data,
        )
        org_service = self.org_service_model.find(
            filter_data=self.filter_data,
            fields=['trial_expires'],
            one=True,
        )
        assert_that(
            org_service['trial_expires'],
            equal_to(last_week_date)
        )

        disable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            self.service['slug'],
            'some-reason',
        )
        self.filter_data['enabled'] = False
        org_service = self.org_service_model.find(
            filter_data=self.filter_data,
            fields=['trial_expires'],
            one=True,
        )

        # дата окончания триала не поменялась
        assert_that(
            org_service['trial_expires'],
            equal_to(last_week_date)
        )

    def test_disable_licensed_services_trial_expired(self):
        # проверим, что лицензионные сервисы отключаются, если триальный период закончился и на него нет лицензий
        service2 = ServiceModel(self.meta_connection).create(
            client_id='some-client-id2',
            slug='new_service2',
            name='Service2',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            service2['slug'],
        )

        # обновляем дату окончания триала
        last_week_date = (utcnow() - relativedelta(weeks=1)).date()
        self.update_service_trial_expires_date(
            self.organization['id'],
            self.service['id'],
            last_week_date
        )
        self.update_service_trial_expires_date(
            self.organization['id'],
            service2['id'],
            last_week_date
        )

        # выдаем лицензии на service2
        new_license = UserServiceLicenses(self.main_connection).create(
            user_id=self.user['id'],
            org_id=self.organization['id'],
            service_id=service2['id'],
        )
        # выключаем сервисы, у которых истек триальный период и проверям, что service2 остался активным
        DisableServiceTrialExpiredCommand().try_run(org_id=None)

        assert_that(
            OrganizationServiceModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'enabled': Ignore
                },
            ),
            contains_inanyorder(
                has_entries(
                    service_id=self.service['id'],
                    enabled=False,
                    disable_reason=reason.trial_expired,
                ),
                has_entries(
                    service_id=service2['id'],
                    enabled=True,
                    disable_reason=None,
                )
            )
        )

    def test_disable_licensed_services_no_payment(self):
        # проверим, что отключаются все лицензионные сервисы (вне зависимости от наличия лицензий),
        # у которых закончился триал и причина отключения no_payment
        service2 = ServiceModel(self.meta_connection).create(
            client_id='some-client-id2',
            slug='new_service2',
            name='Service2',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            service2['slug'],
        )

        # обновляем дату окончания триала
        last_week_date = (utcnow() - relativedelta(weeks=1)).date()
        self.update_service_trial_expires_date(
            self.organization['id'],
            self.service['id'],
            last_week_date
        )
        self.update_service_trial_expires_date(
            self.organization['id'],
            service2['id'],
            last_week_date
        )

        # выдаем лицензии на service2
        new_license = UserServiceLicenses(self.main_connection).create(
            user_id=self.user['id'],
            org_id=self.organization['id'],
            service_id=service2['id'],
        )
        # выключаем сервисы, у которых истек триальный период и проверям, что все сервисы отключены
        disabled_services = disable_licensed_services_by_debt(
            self.meta_connection,
            self.main_connection,
            self.organization['id']
        )
        assert_that(
            disabled_services,
            contains_inanyorder(
                self.service,
                service2
            )
        )
        assert_that(
            OrganizationServiceModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'enabled': True,
                },
            ),
            equal_to([])
        )

    def test_disable_disk_no_payment(self):
        # диск не должен отключаться, но у пользователей должны отобраться все лицензии
        tvm.tickets['partner_disk'] = 'disk-tvm-ticket'

        disk = ServiceModel(self.meta_connection).create(
            client_id='some-client-id2',
            slug='disk',
            name='disk',
            robot_required=False,
            paid_by_license=True,
        )
        UpdateServicesInShards().try_run()
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            disk['slug'],
        )

        # выдаем лицензии на диск
        self.create_licenses_for_service(disk['id'], user_ids=[self.user['id']])
        self.assertEqual(UserServiceLicenses(self.main_connection).count(), 1)

        with patch.object(app, 'partner_disk'):
            disabled_services = disable_licensed_services_by_debt(
                self.meta_connection,
                self.main_connection,
                self.organization['id']
            )
            self.process_tasks()
        assert_that(
            disabled_services,
            contains_inanyorder(
                disk,
            )
        )

        # диск не выключился
        assert_that(
            OrganizationServiceModel(self.main_connection).filter(
                org_id=self.organization['id'],
                enabled=True,
                service_id=disk['id'],
            ).count(),
            equal_to(1)
        )
        self.assertEqual(UserServiceLicenses(self.main_connection).count(), 0)

        disk_tasks = TaskModel(self.main_connection).filter(
            task_name=DeleteSpacePartnerDiskTask.get_task_name(),
        ).all()

        assert_that(
            disk_tasks,
            contains_inanyorder(
                has_entries(
                    params=has_entries(
                        org_id=self.organization['id'],
                        uid=self.user['id'],
                        resource_id=ANY,
                    )
                ),
            )
        )

    def test_disable_licensed_services_trial_expired_two_organizations(self):
        # проверяем, что при окончании триала сервис выключается в нужной организации,
        # чтобы избежать ошибки в DIR-404
        wiki = ServiceModel(self.meta_connection).create(
            client_id='some-client-id2',
            slug='wiki',
            name='wiki',
            ready_default=True,
        )
        # подключим сервис без лицензий, чтобы проверить, что он не выключится
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            wiki['slug'],
        )

        org1 = create_organization(
            self.meta_connection,
            self.main_connection,
            label='some-org'
        )['organization']

        enable_service(
            self.meta_connection,
            self.main_connection,
            org1['id'],
            self.service['slug'],
        )

        org2 = create_organization(
            self.meta_connection,
            self.main_connection,
            label='some-org22'
        )['organization']

        enable_service(
            self.meta_connection,
            self.main_connection,
            org2['id'],
            self.service['slug'],
        )

        # обновляем дату окончания триала у организации self.organization и org1
        last_week_date = (utcnow() - relativedelta(weeks=1)).date()
        self.update_service_trial_expires_date(
            self.organization['id'],
            self.service['id'],
            last_week_date
        )
        self.update_service_trial_expires_date(
            org1['id'],
            self.service['id'],
            last_week_date
        )

        # выдаем лицензии пользователям в org1, чтобы сервис не выключился
        org1_users = [self.create_user(org_id=org1['id']) for _ in range(2)]
        new_license = UserServiceLicenses(self.main_connection).create(
            user_id=org1_users[0]['id'],
            org_id=org1['id'],
            service_id=self.service['id'],
        )
        new_license = UserServiceLicenses(self.main_connection).create(
            user_id=org1_users[1]['id'],
            org_id=org1['id'],
            service_id=self.service['id'],
        )

        # выключаем сервисы, у которых истек триальный период и проверям, что сервис остался активным в org1 и org2
        for org_id in [self.organization['id'], org1['id'], org2['id']]:
            disabled_services = disable_licensed_services_by_trial(
                self.meta_connection,
                self.main_connection,
                org_id,
            )

        assert_that(
            OrganizationServiceModel(self.main_connection).find(
                filter_data={
                    'enabled': False,
                },
            ),
            contains_inanyorder(
                has_entries(
                    org_id=self.organization['id'],
                    service_id=self.service['id'],
                    enabled=False,
                    disable_reason=reason.trial_expired,
                ),
            )
        )

        assert_that(
            OrganizationServiceModel(self.main_connection).find(
                filter_data={
                    'enabled': True,
                },
            ),
            contains_inanyorder(
                has_entries(
                    org_id=org1['id'],
                    service_id=self.service['id'],
                    enabled=True,
                    disable_reason=None,
                ),
                has_entries(
                    org_id=org2['id'],
                    service_id=self.service['id'],
                    enabled=True,
                    disable_reason=None,
                ),
                has_entries(
                    org_id=self.organization['id'],
                    service_id=wiki['id'],
                    enabled=True,
                    disable_reason=None,
                ),
            )
        )

    def test_disable_licensed_services_inactive_contracts(self):
        # проверяем, что сервисы выключатся, если у организации нет активных контрактов
        service2 = ServiceModel(self.meta_connection).create(
            client_id='some-client-id2',
            slug='new_service2',
            name='Service2',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            service2['slug'],
        )

        # обновляем дату окончания триала у service2
        last_week_date = (utcnow() - relativedelta(weeks=1)).date()
        self.update_service_trial_expires_date(
            self.organization['id'],
            service2['id'],
            last_week_date
        )

        # выдаем лицензии на service2
        new_license = UserServiceLicenses(self.main_connection).create(
            user_id=self.user['id'],
            org_id=self.organization['id'],
            service_id=service2['id'],
        )
        # выключаем сервисы, у которых истек триальный период и проверям, что service2 выключился
        disabled_services = disable_licensed_services_by_inactive_contracts(
            self.meta_connection,
            self.main_connection,
            self.organization['id']
        )
        assert_that(
            disabled_services,
            contains(
                service2
            )
        )
        assert_that(
            OrganizationServiceModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'enabled': True,
                },
            ),
            contains(
                has_entries(
                    service_id=self.service['id']
                )
            )
        )

    def test_disable_licensed_services_org_blocked(self):
        # проверим, что отключаются все лицензионные сервисы (вне зависимости от триала и лицензий),
        # если организация блокируется
        service2 = ServiceModel(self.meta_connection).create(
            client_id='some-client-id2',
            slug='new_service2',
            name='Service2',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            service2['slug'],
        )

        # обновляем дату окончания триала у service2
        last_week_date = (utcnow() - relativedelta(weeks=1)).date()
        self.update_service_trial_expires_date(
            self.organization['id'],
            service2['id'],
            last_week_date
        )

        # выдаем лицензии на service2
        new_license = UserServiceLicenses(self.main_connection).create(
            user_id=self.user['id'],
            org_id=self.organization['id'],
            service_id=service2['id'],
        )
        # выключаем платные сервисы
        disabled_services = disable_licensed_services_by_org_blocked(
            self.meta_connection,
            self.main_connection,
            self.organization['id']
        )
        assert_that(
            disabled_services,
            contains_inanyorder(
                self.service,
                service2
            )
        )
        assert_that(
            OrganizationServiceModel(self.main_connection).filter(
                org_id=self.organization['id'],
                enabled=True,
            ).all(),
            equal_to([])
        )

        assert_that(
            OrganizationServiceModel(self.main_connection).filter(
                org_id=self.organization['id'],
                service_id=service2['id'],
                enabled=Ignore,
            ).one()['disable_reason'],
            equal_to(reason.organization_blocked)
        )


class TestUserServiceLicenses(TestCase):
    def setUp(self):
        super(TestUserServiceLicenses, self).setUp()
        self.service = ServiceModel(self.meta_connection).create(
            client_id='some-client-id',
            slug='new_service',
            name='Service',
            paid_by_license=True,
            ready_default=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            self.service['slug'])
        self.org_service = OrganizationServiceModel(self.main_connection).find(filter_data={
            'org_id': self.organization['id'],
            'service_id': self.service['id'],
        }, one=True)

    def test_simple(self):
        new_license = UserServiceLicenses(self.main_connection).create(
            user_id=self.user['id'],
            org_id=self.organization['id'],
            service_id=self.org_service['service_id'],
        )
        self.assertEqual(new_license['service_id'], self.org_service['service_id'])

        license_from_db = UserServiceLicenses(self.main_connection).find(one=True)
        self.assertIsNotNone(license_from_db)
        self.assertEqual(license_from_db['service_id'], new_license['service_id'])

    def test_find_user_licenses(self):
        UserServiceLicenses(self.main_connection).create(
            user_id=self.user['id'],
            org_id=self.organization['id'],
            service_id=self.org_service['service_id'],
        )
        service2 = ServiceModel(self.meta_connection).create(
            client_id='some-client-id2',
            slug='new_service2',
            name='Service2',
            paid_by_license=True,
            ready_default=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            service2['slug'])
        org_service2 = OrganizationServiceModel(self.main_connection).find(filter_data={
            'org_id': self.organization['id'],
            'service_id': service2['id'],
        }, one=True)
        UserServiceLicenses(self.main_connection).create(
            user_id=self.user['id'],
            org_id=self.organization['id'],
            service_id=org_service2['service_id'],
        )
        licenses = UserServiceLicenses(self.main_connection).find(filter_data={
            'user_id': self.user['id'],
            'org_id': self.organization['id'],
        }, fields=['service_id'])
        self.assertEqual(len(licenses), 2)
        assert_that(
            licenses,
            contains_inanyorder(
                has_entries(
                    service_id=self.org_service['service_id']
                ),
                has_entries(
                    service_id=org_service2['service_id']
                )
            )
        )

    def test_update_license_cache(self):
        consumed_licenses = UserServiceLicenses(self.main_connection).find()
        self.assertEqual(len(consumed_licenses), 0)

        users = [self.create_user() for _ in range(4)]
        group1 = self.create_group(members=[{'type': 'user', 'object': users[0]}])

        members = [
            {'type': 'user', 'object': users[1]},
            {'type': 'user', 'object': users[2]},
            {'type': 'user', 'object': users[3]},
            {'type': 'group', 'object': group1}
        ]
        group2 = self.create_group(members=members)

        department1 = self.create_department()
        user5, user6 = self.create_user(department1['id']), self.create_user(department1['id'])

        department2 = self.create_department(
            parent_id=department1['id']
        )
        user7 = self.create_user(department2['id'])

        relations = [
            {
                'name': 'member',
                'group_id': group2['id'],
            },
            {
                'name': 'member',
                'department_id': department1['id'],
            },
            {
                'name': 'member',
                'user_id': self.user['id'],
            }
        ]
        ResourceModel(self.main_connection).update_relations(
            resource_id=self.org_service['resource_id'],
            org_id=self.organization['id'],
            relations=relations,
        )
        UserServiceLicenses(self.main_connection).update_licenses_cache(
            self.organization['id'],
            self.org_service['service_id']
        )
        licenses = UserServiceLicenses(self.main_connection).find()
        self.assertEqual(len(licenses), 8)
        consumed_licenses = OrganizationLicenseConsumedInfoModel(self.main_connection).count()
        self.assertEqual(consumed_licenses, 8)
        all_user_ids = only_attrs(
            users + [user5, user6, user7, self.user],
            'id'
        )
        assert_that(
            only_attrs(
                licenses,
                'user_id'
            ),
            contains_inanyorder(
                *all_user_ids
            )
        )

    def test_update_license_cache_multiple_containers(self):
        # проверяем, что лицензии корректно сохраняются, если они выданы пользователю несколькими способами
        # но в таблице OrganizationLicenseConsumedInfoModel она появляется только один раз
        department = self.create_department()
        user1 = self.create_user(nickname='user1')
        user2 = self.create_user(name={'first': {'ru': 'User2'}})
        user3 = self.create_user(department_id=department['id'], nickname='user_three')
        user4 = self.create_user(name={'first': {'ru': 'cool'}, 'last': {'ru': 'guy'}})

        members = [
            {'type': 'user', 'object': user2},
            {'type': 'user', 'object': user3},
        ]
        group = self.create_group(members=members)
        self.create_licenses_for_service(
            self.service['id'],
            department_ids=[department['id']],
            group_ids=[group['id']],
            user_ids=[user3['id'], user4['id']]
        )

        # пользователю user3 лицензия выдана тремя способами + лицензия у user2 и user4
        licenses = UserServiceLicenses(self.main_connection).find()
        self.assertEqual(len(licenses), 5)

        # но здесь только 3 записи
        consumed_licenses = OrganizationLicenseConsumedInfoModel(self.main_connection).count()
        self.assertEqual(consumed_licenses, 3)


class Test__enable_licensed_service(TestCase):
    def setUp(self):
        super(Test__enable_licensed_service, self).setUp()
        self.service = ServiceModel(self.meta_connection).create(
            client_id='some-client-id',
            slug='new_service',
            name='Service',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
        )
        UpdateServicesInShards().try_run()

        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            self.service['slug'],
        )

    def test_reenable_licensed_service_has_debt(self):
        # проверяем, что повторное включение платного сервиса после окончания триала
        # вызовет ошибку, если есть задолженность

        # включаем платный режим с задолженностью
        first_debt_act_date = datetime.datetime(year=2017, month=0o1, day=0o7).date()
        self.enable_paid_mode(first_debt_act_date=first_debt_act_date, balance=-100)

        # обновляем дату окончания триала
        last_week_date = (utcnow() - relativedelta(weeks=1)).date()
        self.update_service_trial_expires_date(
            self.organization['id'],
            self.service['id'],
            last_week_date
        )

        disable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            self.service['slug'],
            'some-reason'
        )
        # пытаемся включить сервис
        try:
            enable_service(
                self.meta_connection,
                self.main_connection,
                self.organization['id'],
                self.service['slug'],
            )
        except OrganizationHasDebt as e:
            self.assertEqual(e.code, 'organization_has_debt')
        else:
            raise AssertionError(str(OrganizationHasDebt) + ' not raised')

    def test_enable_licensed_service_has_debt(self):
        # проверяем, что можем включить платный сервис в триальном режиме даже если есть долги

        new_service = ServiceModel(self.meta_connection).create(
            client_id='new-client-id',
            slug='some_new_service',
            name='Service_new',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
        )
        # включаем платный режим с задолженностью
        first_debt_act_date = datetime.datetime(year=2017, month=0o1, day=0o7).date()
        self.enable_paid_mode(first_debt_act_date=first_debt_act_date, balance=-100)

        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            new_service['slug'],
        )
        assert_that(
            OrganizationServiceModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'service_id': new_service['id'],
                    'enabled': Ignore
                },
                one=True
            ),
            has_entries(
                service_id=new_service['id'],
                enabled=True
            )
        )

    def test_reenable_licensed_service_ready_default(self):
        # проверяем, что при повторном включении сервиса ready становится равным ready_default

        # включаем платный режим, чтобы была биллинговая информация
        self.enable_paid_mode()
        disable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            self.service['slug'],
            'some-reason'
        )
        assert_that(
            OrganizationServiceModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'service_id': self.service['id'],
                    'enabled': Ignore
                },
                one=True
            ),
            has_entries(
                ready=True,
                disable_reason='some-reason',
            )
        )
        # обновим у сервиса поле ready_default
        ServiceModel(self.meta_connection).update(
            filter_data={'id': self.service['id']},
            update_data={'ready_default': False},
        )

        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            self.service['slug'],
        )

        assert_that(
            OrganizationServiceModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'service_id': self.service['id'],
                },
                one=True
            ),
            has_entries(
                ready=False,
                ready_at=None,
                disable_reason=None,
                disabled_at=None,
            )
        )

    def test_disable_not_ready_service(self):
        # проверяем, что можно выключить сервис до того, как он стал ready и триальный период при этом не начнется
        new_service = ServiceModel(self.meta_connection).create(
            client_id='new-client-id',
            slug='some_new_service',
            name='Service_new',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
            ready_default=False,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            new_service['slug'],
        )
        assert_that(
            OrganizationServiceModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'service_id': new_service['id'],
                },
                one=True
            ),
            has_entries(
                ready=False,
                trial_expires=None,
            )
        )
        disable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            new_service['slug'],
            'some-reason'
        )
        assert_that(
            OrganizationServiceModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'service_id': new_service['id'],
                    'enabled': Ignore,
                },
                one=True
            ),
            has_entries(
                ready=False,
                trial_expires=None,
            )
        )


class TestNotifyAboutTrailEnded(TestCase):
    def setUp(self):
        super(TestNotifyAboutTrailEnded, self).setUp()

        self.service_with_trial = ServiceModel(self.meta_connection).create(
            slug='some-service-slug',
            name='ServiceName',
            client_id='client-id',
            trial_period_months=1,
        )
        UpdateServicesInShards().try_run()

        enable_service(
            self.main_connection,
            self.main_connection,
            org_id=self.organization['id'],
            service_slug=self.service_with_trial['slug'],
        )

    def test_trial_expired_yesterday(self):
        # триал закончился вчера - надо создать про это событие

        yesterday = utcnow().date() - datetime.timedelta(days=1)
        OrganizationServiceModel(self.main_connection).update(
            update_data={
                'trial_expires': yesterday
            },
            filter_data={
                'org_id': self.organization['id'],
                'service_id': self.service_with_trial['id'],
            }
        )

        notify_about_trail_ended(self.main_connection)

        assert_that(
            ActionModel(self.main_connection).filter(org_id=self.organization['id']),
            has_item(
                has_entries(name=action.service_trial_end)
            )
        )

        assert_that(
            EventModel(self.main_connection).filter(org_id=self.organization['id']),
            has_item(
                has_entries(name=event.service_trial_ended)
            )
        )

    def test_trial_expired_10_days_ago(self):
        # триал закончился давно - событие не нужно
        ten_days_ago = utcnow().date() - datetime.timedelta(days=10)
        OrganizationServiceModel(self.main_connection).update(
            update_data={
                'trial_expires': ten_days_ago
            },
            filter_data={
                'org_id': self.organization['id'],
                'service_id': self.service_with_trial['id'],
            }
        )

        notify_about_trail_ended(self.main_connection)

        assert_that(
            ActionModel(self.main_connection).filter(org_id=self.organization['id']),
            not_(
                has_item(
                    has_entries(name=action.service_trial_end)
                )
            )
        )

        assert_that(
            EventModel(self.main_connection).filter(org_id=self.organization['id']),
            not_(
                has_item(
                    has_entries(name=event.service_trial_ended)
                )
            )
        )


class TestServicesLinksModel(TestCase):
    def setUp(self):
        super(TestServicesLinksModel, self).setUp()
        self.slug = 'slug'
        self.create_params = {
            'slug': self.slug,
        }

    def test_create(self):
        # создаем новую запись

        service_link = ServicesLinksModel(self.meta_connection).create(
            **self.create_params
        )

        # в пустой таблице появилась запись
        assert_that(
            ServicesLinksModel(self.meta_connection).count(),
            equal_to(1),
        )

        assert_that(
            service_link,
            has_entries(
                slug=self.slug,
            )
        )

    def test_get(self):
        actual_service_link = ServicesLinksModel(self.meta_connection).create(
            **self.create_params
        )

        # получаем данные по id
        assert_that(
            ServicesLinksModel(self.meta_connection).get(actual_service_link['id']),
            actual_service_link
        )

    def test_delete(self):
        created_service_link = ServicesLinksModel(self.meta_connection).create(
            **self.create_params
        )
        # в пустой таблице появилась запись
        assert_that(
            ServicesLinksModel(self.meta_connection).count(),
            equal_to(1),
        )

        # удаление по неизвестному id ничего не удалило
        unknown_service_id = created_service_link['id'] + 42
        ServicesLinksModel(self.meta_connection).delete_one(unknown_service_id)
        assert_that(
            ServicesLinksModel(self.meta_connection).count(),
            equal_to(1),
        )

        # а по известному id удалило
        ServicesLinksModel(self.meta_connection).delete_one(created_service_link['id'])
        assert_that(
            ServicesLinksModel(self.meta_connection).count(),
            equal_to(0),
        )

    def test_find(self):
        # поиск по возможным фильтрам
        created_service_link = ServicesLinksModel(self.meta_connection).create(
            **self.create_params
        )
        filters_data = [
            ('id', created_service_link['id']),
            ('slug', created_service_link['slug']),
            ('show_in_header', created_service_link['show_in_header']),
            ('can_be_enabled', created_service_link['can_be_enabled']),
            ('can_be_disabled', created_service_link['can_be_disabled']),
        ]

        for field, value in filters_data:
            found = ServicesLinksModel(self.meta_connection).find(
                filter_data={field: value},
            )
            assert_that(
                found,
                contains(created_service_link),
            )
