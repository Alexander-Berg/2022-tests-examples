# -*- coding: utf-8 -*-
from intranet.yandex_directory.src.yandex_directory.core.models import (
    ServiceModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    OrganizationServiceModel,
    ResponsibleCannotBeChanged,
)
from intranet.yandex_directory.src.yandex_directory.core.utils.organization import get_organizations_services

from hamcrest import (
    assert_that,
    has_length,
    has_properties,
    contains_inanyorder,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    enable_service,
)

from testutils import (
    TestCase,
    create_organization,
)


class Test__get_organizations_services(TestCase):
    def setUp(self, *args, **kwargs):
        super(Test__get_organizations_services, self).setUp(*args, **kwargs)

        # создадим новую организацию
        self.organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='yandex'
        )['organization']

        # добавим сервисы
        self.org_services = []
        for slug in ['first_org_service_1', 'first_org_service_2']:
            service = ServiceModel(self.meta_connection).create(
                slug=slug,
                name='Autotest Service (%s)' % slug,
                client_id='some_test_service_client_id_%s' % slug,
            )
            # подключим сервис для организации
            org_service = OrganizationServiceModel(self.main_connection).create(
                org_id=self.organization['id'],
                service_id=service['id'],
            )
            service['ready'] = org_service['ready']
            self.org_services.append(service)

        # создадим вторую организацию и сервис в ней, чтобы проверить что она не попадет в выборку
        second_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']
        service = ServiceModel(self.meta_connection).create(
            slug='second_service_slug',
            name='Service',
            client_id='some_test_service_client_id_for_second_org',
        )
        # Подключаем сервис для второй организации
        OrganizationServiceModel(self.main_connection).create(
            org_id=second_organization['id'],
            service_id=service['id'],
        )

    def test_should_return_services_slugs_for_organization(self):
        # проверяем, что возвращается правильный список сервисов
        services = get_organizations_services(
            self.meta_connection,
            self.main_connection,
            org_ids=[self.organization['id']],
        )
        assert_that(
            services,
            has_length(1), # Отдались данные только по одной организации
            has_properties(
                {
                    self.organization['id']: contains_inanyorder(*self.org_services)
                }
            )
        )

    def test_does_not_change_responsible_when_set(self):
        # Нельзя сменить ответственного если он уже есть.
        service = ServiceModel(self.meta_connection).create(
            slug='poisk',
            name='Autotest Service',
            client_id='some_test_service_client_id',
        )
        enable_service(
            self.meta_connection, self.main_connection,
            org_id=self.user['org_id'],
            service_slug='poisk',
        )
        OrganizationServiceModel(self.main_connection).change_responsible(
            service_id=service['id'],
            org_id=self.user['org_id'],
            responsible_id=self.user['id'],
        )

        with self.assertRaises(ResponsibleCannotBeChanged):
            OrganizationServiceModel(self.main_connection).change_responsible(
                service_id=service['id'],
                org_id=self.user['org_id'],
                responsible_id=self.user['id'],
            )

        with self.assertRaises(ResponsibleCannotBeChanged):
            OrganizationServiceModel(self.main_connection).change_responsible(
                service_id=service['id'],
                org_id=self.user['org_id'],
                responsible_id=0,
            )

    def test_allows_to_set_responsible(self):
        # Ответственного можно назначить.
        service = ServiceModel(self.meta_connection).create(
            slug='poisk',
            name='Autotest Service',
            client_id='some_test_service_client_id',
        )
        enable_service(
            self.meta_connection, self.main_connection,
            org_id=self.user['org_id'],
            service_slug='poisk',
        )
        result = OrganizationServiceModel(self.main_connection).change_responsible(
            service_id=service['id'],
            org_id=self.user['org_id'],
            responsible_id=self.user['id'],
        )
        self.assertTrue(result)
