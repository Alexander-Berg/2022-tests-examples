# -*- coding: utf-8 -*-
from hamcrest import (
    assert_that,
    has_entries,
)

from testutils import TestCase
from intranet.yandex_directory.src.yandex_directory.core.models import TaskModel, TaskRelationsModel


class TestTaskRelationsModel(TestCase):

    def test_create(self):
        # Создаем две таски. Создаем связь между ними.
        # Проверяем, что запись появилась в таблице tasks_relations

        create_params = {
            'task_name': 'task_name',
            'params': {},
            'queue': 'default',
            'ttl': 60*60,
            'author_id': self.user['id'],
        }
        task_1 = TaskModel(self.main_connection).create(**create_params)
        task_2 = TaskModel(self.main_connection).create(**create_params)

        TaskRelationsModel(self.main_connection).delete(force_remove_all=True)
        create_relations_params = {
            'task_id': task_1['id'],
            'dependency_task_id': task_2['id'],
        }
        TaskRelationsModel(self.main_connection).create(**create_relations_params)

        assert_that(
            TaskRelationsModel(self.main_connection).find(
                {'task_id': task_1['id']},
                one=True),
            has_entries(**create_relations_params)
        )
        assert_that(
            TaskRelationsModel(self.main_connection).find(
                {'dependency_task_id': task_2['id']},
                one=True),
            has_entries(**create_relations_params)
        )
