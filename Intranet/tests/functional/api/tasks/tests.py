# coding: utf-8
import uuid

from hamcrest import (
    assert_that,
    has_entries,
    equal_to,
)

from testutils import (
    TestCase,
    get_auth_headers,
    PaginationTestsMixin,
)
from intranet.yandex_directory.src.yandex_directory.core.models import TaskModel

from intranet.yandex_directory.src.yandex_directory.core.views.tasks import ALLOWED_FIELDS


class TestTaskDetail(TestCase):
    autoprocess_tasks = False

    def setUp(self):
        super(TestTaskDetail, self).setUp()
        self.second_user = self.create_user()

    def test_404(self):
        # запрашиваем не существующую задачу
        unknown_task_id = uuid.uuid4()
        self.get_json('/tasks/%s/' % unknown_task_id, expected_code=404)

    def test_403(self):
        # запрашиваем чужую задачу

        create_params = {
            'task_name': 'task_name',
            'params': {},
            'queue': 'default',
            'ttl': 60 * 60,
            'author_id': self.second_user['id']
        }
        task = TaskModel(self.main_connection).create(**create_params)

        self.get_json('/tasks/%s/' % task['id'], expected_code=403)

    def test_simple_get(self):
        # успешно получем статус задачи
        create_params = {
            'task_name': 'task_name',
            'params': {},
            'queue': 'default',
            'ttl': 60 * 60,
            'author_id': self.user['id']
        }
        task = TaskModel(self.main_connection).create(**create_params)

        result = self.get_json('/tasks/%s/?fields=id,queue,ttl,task_name,state' % task['id'])

        assert_that(
            result,
            has_entries(
                id=str(task['id']),
                state=task['state'],
                task_name=task['task_name'],
            )
        )

    def test_only_allowed_field(self):
        # получаем только разрешенные поля
        create_params = {
            'task_name': 'task_name',
            'params': {},
            'queue': 'default',
            'ttl': 60 * 60,
            'author_id': self.user['id']
        }
        task = TaskModel(self.main_connection).create(**create_params)

        deny_fields = set(TaskModel.all_fields) - set(ALLOWED_FIELDS)

        for field in deny_fields:
            result = self.get_json('/tasks/%s/?fields=%s' % (task['id'], field))

            assert_that(
                result,
                equal_to(
                    {'id': str(task['id'])}
                )
            )


class TestTaskList__get(PaginationTestsMixin, TestCase):
    entity_list_url = '/tasks/'
    entity_model = TaskModel
    autoprocess_tasks = False

    def get_entity_model_filters(self):
        filters = dict(self.entity_model_filters)
        filters['author_id'] = self.user['id']
        return filters

    def create_entity(self, author_id=None):
        if author_id is None:
            author_id = self.user['id']

        self.entity_counter += 1

        return TaskModel(self.main_connection).create(
            params={'default': 'task'},
            queue='default',
            ttl=60 * 60,
            task_name='task_%s' % self.entity_counter,
            author_id=author_id,
        )

    def test_empty_tasks(self):
        # Проверяем ответ, если задач вообще нет
        data = self.get_json('/tasks/', expected_code=200)
        result = data['result']
        assert_that(
            result,
            equal_to([]),
        )

    def test_tasks_with_unknown_user(self):
        self.create_entity(author_id=928392838)
        data = self.get_json('/tasks/', expected_code=200)
        result = data['result']
        assert_that(
            result,
            equal_to([]),
        )

    def test_tasks_from_different_users(self):
        # Создана задача от имени 1го пользователя
        task1 = self.create_entity(self.user['id'])
        # Создана задача от имени 2го пользователя
        second_user = self.create_user()
        task2 = self.create_entity(second_user['id'])
        data = self.get_json('/tasks/', expected_code=200)
        result = data['result']
        assert_that(len(result), equal_to(1))
        assert_that(
            result[0],
            has_entries(
                id=str(task1['id']),
            )
        )
        data = self.get_json(
            '/tasks/?fields=id,queue,ttl,task_name,state',
            expected_code=200,
            headers=get_auth_headers(as_uid=second_user['id'])
        )
        result = data['result']
        assert_that(len(result), equal_to(1))
        assert_that(
            result[0],
            has_entries(
                id=str(task2['id']),
                state=task2['state'],
            )
        )
