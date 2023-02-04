# coding: utf-8

from testutils import (
    TestCase,
    tvm2_auth_success,
)
from hamcrest import (
    assert_that,
    contains,
)
from intranet.yandex_directory.src.yandex_directory.auth.scopes import scope
from intranet.yandex_directory.src.yandex_directory.core.task_queue.base import load_tasks


class TestTaskNames(TestCase):
    enable_admin_api = True

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_task_names_list(self):
        # Проверим, что ручка вернет список тасков
        # в алфавитном порядке и без названия модулей

        # Для того, чтобы список был полный, как в проде,
        # надо подгрузить все модули с тасками.
        load_tasks()

        result = self.get_json('/admin/tasks/names/')
        assert 'AccessRestoreTask' in result
        # Имя родительского класса Task отсутствует в этом списке
        assert 'Task' not in result
