# coding: utf-8

from hamcrest import (
    assert_that,
    has_entries,
    none,
    not_none,
)
from testutils import TestCase
from unittest.mock import patch
from intranet.yandex_directory.src.yandex_directory.core.dependencies import Service, Setting


class TestDependency(TestCase):
    api_version = 'v6'

    def test_service_can_be_found_in_the_dict(self):
        # Удостоверимся, что несмотря на то, что в словаре с зависимостями
        # в качестве ключей используются инстансы, на самом деле сравнение
        # идёт по неймспейсу и слагу

        dependencies = {
            Service('wiki'): [Service('yamb'), Setting('shared-contacts', True)],
            Service('tracker'): [Setting('shared-contacts', True)],
            Setting('shared-contacts', True): [Service('staff')],
        }

        # То есть, вот так мы должны найти результат
        assert_that(
            dependencies[Service('wiki')],
            not_none(),
        )

        # А так – нет
        assert_that(
            dependencies.get(Service('shmiki')),
            none(),
        )

        # И так тоже нет, хотя слаг совпадает со слагом сервиса,
        # но имеется в виду настройка
        assert_that(
            dependencies.get(Setting('wiki', True)),
            none(),
        )

    def test_handle_should_return_static_dependencies(self):
        # Проверим, что ручка отдаёт зависимости в том виде, в котором они
        # указаны в объекте dependencies.

        dependencies = {
            Service('wiki'): [Service('yamb'), Setting('shared-contacts', True)],
            Service('tracker'): [Setting('shared-contacts', True)],
            Setting('shared-contacts', True): [Service('staff')],
        }

        with patch('intranet.yandex_directory.src.yandex_directory.core.dependencies.dependencies', new=dependencies):
            response = self.get_json('/dependencies/')

            assert_that(
                response,
                has_entries({
                    'service.wiki': ['service.yamb', 'setting.shared-contacts'],
                    'service.tracker': ['setting.shared-contacts'],
                    'setting.shared-contacts': ['service.staff'],
                })
            )
