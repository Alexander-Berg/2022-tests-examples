# coding: utf-8

from testutils import TestCase, create_organization, auth_as
from unittest.mock import patch

from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
)
from intranet.yandex_directory.src.yandex_directory.core.models import ServiceModel

size_100G = 100 * 1024 * 1024 * 1024
size_1T = 1024 * 1024 * 1024 * 1024


class TestDiskUsage(TestCase):
    api_version = 'v4'
    def setUp(self, *args, **kwargs):
        super(TestDiskUsage, self).setUp(*args, **kwargs)

        # организация в этих тестах должна быть платной
        # чтобы ей выдавалось место
        self.enable_paid_mode()

    def test_get(self):
        # свободное место для организаций до 11 человек (<=10)
        result = self.get_json('/disk-usage/')
        assert_that(
            result,
            has_entries(
                free=size_1T,
                limit=size_1T,
            )
        )

    def test_get_for_free_organizations_should_return_zero_free_space(self):
        # если организация бесплатная, нужно вернуть 0 в качестве значения поля free
        self.disable_paid_mode()
        result = self.get_json('/disk-usage/')
        assert_that(
            result,
            has_entries(
                free=0,
                limit=0,
            )
        )

    def test_post(self):
        # Проверяем, что сервис может запостить сколько в нём используется места
        # и директория запомнит это
        # для организаций до 11 человек (<=10)
        with auth_as(self.service,
                     org_id=self.organization['id'],
                     scopes=['work_with_any_organization']):
            result = self.post_json(
                '/disk-usage/',
                {'usage': 123},
                expected_code=200,
            )

            assert_that(
                result,
                has_entries(
                    free=size_1T - 123,
                    limit=size_1T,
                )
            )

            # Теперь дернем ручку через GET, чтобы получить те же данные
            result = self.get_json('/disk-usage/')

            # и мы ожидаем всё тех же данных
            assert_that(
                result,
                has_entries(
                    free=size_1T - 123,
                    limit=size_1T,
                )
            )

            # Теперь снова сделаем пост и убедимся, что
            # отдастся новый остаток места
            result = self.post_json(
                '/disk-usage/',
                {'usage': 100500},
                expected_code=200,
            )

            assert_that(
                result,
                has_entries(
                    free=size_1T - 100500,
                    limit=size_1T,
                )
            )

    def test_post_0_usage(self):
        # можно передать 0 значение
        with auth_as(self.service,
                     org_id=self.organization['id'],
                     scopes=['work_with_any_organization']):
            self.post_json(
                '/disk-usage/',
                {'usage': 0},
                expected_code=200,
            )



    def test_post_from_multiple_services(self):
        # Проверяем, больше чем один сервис могут репортить
        # использование диска, и результирующий usage будет
        # рассчитываться путём сложения
        # для организаций до 11 человек

        first_service = self.service
        second_service = ServiceModel(self.meta_connection).create(
            slug='second-service',
            name='Second Name',
            client_id='не важно какое тут будет значение',
        )

        first_service_usage = 1000
        second_service_usage = 42
        total_usage = first_service_usage + second_service_usage

        with auth_as(first_service,
                     org_id=self.organization['id'],
                     scopes=['work_with_any_organization']):
            result = self.post_json(
                '/disk-usage/',
                {'usage': first_service_usage},
                expected_code=200,
            )

        with auth_as(second_service,
                     org_id=self.organization['id'],
                     scopes=['work_with_any_organization']):
            result = self.post_json(
                '/disk-usage/',
                {'usage': second_service_usage},
                expected_code=200,
            )

            assert_that(
                result,
                has_entries(
                    # Ручка должна учесть, что место используют оба сервиса
                    free=size_1T - total_usage,
                    limit=size_1T,
                )
            )

    def test_post_to_few_organizations(self):
        # Проверяем, использование месте отслеживается по-отдельности
        # для каждой организации и одна организация не влияет на другую
        # для организаций до 11 человек (<=10)

        # Создадим вторую организацию
        org_info = create_organization(
            self.meta_connection,
            self.main_connection,
            label='second',
            domain_part='second.com',
        )
        another_org_id = org_info['organization']['id']

        # в self.organization уже включен платный режим, нужно включить его только для another_org_id
        self.enable_paid_mode(org_id=another_org_id)

        first_organization_usage = 100500
        second_organization_usage = 4242

        # Сначала запостим
        with auth_as(self.service,
                     org_id=self.organization['id'],
                     scopes=['work_with_any_organization']):
            result = self.post_json(
                '/disk-usage/',
                {'usage': first_organization_usage},
                expected_code=200,
            )

        with auth_as(self.service,
                     org_id=another_org_id,
                     scopes=['work_with_any_organization']):
            result = self.post_json(
                '/disk-usage/',
                {'usage': second_organization_usage},
                expected_code=200,
            )

            assert_that(
                result,
                has_entries(
                    # Убедимся, что в данном случае, учтен usage только
                    # второй организации
                    free=size_1T - second_organization_usage,
                    limit=size_1T,
                )
            )

    def test_more_10_users_org(self):
        # Проверяем, использование месте
        # для организаций от 11 человек (>10)

        # Создадим вторую организацию
        org_info = create_organization(
            self.meta_connection,
            self.main_connection,
            label='second',
            domain_part='second.com',
        )
        another_org_id = org_info['organization']['id']
        self.enable_paid_mode(org_id=another_org_id)

        add_users_count = 10
        for _ in range(add_users_count):
            self.create_user(org_id=another_org_id)

        organization_usage = 4242

        # Сначала запостим
        with auth_as(self.service,
                     org_id=another_org_id,
                     scopes=['work_with_any_organization']):
            result = self.post_json(
                '/disk-usage/',
                {'usage': organization_usage},
                expected_code=200,
            )
            expected_size = 11 * size_100G  # 11 пользователей в организации
            assert_that(
                result,
                has_entries(
                    free=expected_size - organization_usage,
                    limit=expected_size,
                )
            )
