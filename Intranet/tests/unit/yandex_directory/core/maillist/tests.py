# -*- coding: utf-8 -*-
from hamcrest import (
    assert_that,
    has_entries,
)

from testutils import (
    TestCase,
)
from intranet.yandex_directory.src.yandex_directory.core.maillist.tasks import (
    CreateMaillistTask,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    DepartmentModel,
)


class TestCreateMaillistTask(TestCase):
    def setUp(self):
        super(TestCreateMaillistTask, self).setUp()
        self.org_id = self.organization['id']
        self.dep_id = self.create_department(org_id=self.org_id)['id']
        self.label = 'my_own_department'

    def test_create_maillist_task_success(self):
        task = CreateMaillistTask(self.main_connection).delay(
            org_id=self.org_id,
            department_id=self.dep_id,
            label=self.label,
            ignore_login_not_available=True,
        )
        self.process_tasks()

        self.assert_no_failed_tasks()
        dep = DepartmentModel(self.main_connection).get(self.dep_id, self.org_id)
        assert_that(
            dep,
            has_entries(
                label=self.label,
                uid=task.get_result(),
            )
        )
