# -*- coding: utf-8 -*-
import uuid
from datetime import timedelta

from hamcrest import (
    assert_that,
    contains_inanyorder,
    has_entries,
    contains_string,
    not_none,
    none,
)

from testutils import (
    TestCase,
    format_date,
    tvm2_auth,
    tvm2_auth_success
)
from intranet.yandex_directory.src.yandex_directory.admin.views.tasks import prepare_task
from intranet.yandex_directory.src.yandex_directory.auth.scopes import scope
from intranet.yandex_directory.src.yandex_directory.auth.service import Service
from intranet.yandex_directory.src.yandex_directory.core.models import (
    TaskModel,
    TaskRelationsModel,
    ServiceModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import INTERNAL_ADMIN_SERVICE_SLUG
from intranet.yandex_directory.src.yandex_directory.core.task_queue.base import (
    Task,
    TASK_STATES,
)


class SimpleTask(Task):
    need_rollback = False
    org_id_is_required = False

    def do(self, **kwargs):
        return 'success'


class FailedTask(SimpleTask):
    org_id_is_required = False
    def do(self,  **kwargs):
        return 1 / 0


class TestAdminTaskDetailView(TestCase):
    enable_admin_api = True

    def setUp(self):
        super(TestAdminTaskDetailView, self).setUp()
        task_1 = TaskModel(self.main_connection).create(
            task_name='intranet.yandex_directory.src.yandex_directory.core.sms.tasks.SendSmsTask',
            params={'org_id': self.organization['id']},
            queue='default',
            ttl=1,
        )
        TaskModel(self.main_connection).set_state(task_1['id'], TASK_STATES.in_progress)
        self.task_1 = TaskModel(self.main_connection).get(task_1['id'])

        task_2 = TaskModel(self.main_connection).create(
            task_name='intranet.yandex_directory.src.yandex_directory.core.utils.tasks.UpdateLicenseCache',
            params={'org_id': self.organization['id']},
            queue='default',
            ttl=1,
        )
        TaskModel(self.main_connection).set_state(task_2['id'], TASK_STATES.success)
        self.task_2 = TaskModel(self.main_connection).get(task_2['id'])

        task_3 = TaskModel(self.main_connection).create(
            task_name='intranet.yandex_directory.src.yandex_directory.core.sms.tasks.SendSmsTask',
            params={},
            queue='default',
            ttl=1,
        )
        TaskModel(self.main_connection).set_state(task_3['id'], TASK_STATES.in_progress)
        TaskRelationsModel(self.main_connection).create(self.task_1['id'], task_3['id'])
        self.task_3 = TaskModel(self.main_connection).get(task_3['id'])

        tvm2_client_id = 42
        self.service = ServiceModel(self.meta_connection).create(
            slug=INTERNAL_ADMIN_SERVICE_SLUG,
            name='Internal Admin Section',
            tvm2_client_ids=[tvm2_client_id],
            scopes=[scope.internal_admin],
            internal=True,
        )
        self.tvm2_service = Service(
            id=self.service['id'],
            name=self.service['name'],
            identity=self.service['slug'],
            is_internal=True,
            ip='127.0.0.1',
        )

        self.headers = {
            'X-Ya-Service-Ticket': 'qqq',
            'X-ORG-ID': self.organization['id'],
        }

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_get_task(self):
        # Проверяем, что находится 1, 2 и 3 таск
        response1 = self.get_json(
            '/admin/tasks/{task_id}/'.format(
                task_id=str(self.task_1['id']),

            ),
            process_tasks=False,
        )
        response2 = self.get_json(
            '/admin/tasks/{task_id}/'.format(
                task_id=str(self.task_2['id']),

            ),
            process_tasks=False,
        )
        response3 = self.get_json(
            '/admin/tasks/{task_id}/'.format(
                task_id=str(self.task_3['id']),

            ),
            process_tasks=False,
        )
        assert_that(
            response1,
            has_entries(
                id=str(self.task_1['id']),
                task_name=self.task_1['task_name'].split('.')[-1],
                state=self.task_1['state'],
                author_id=self.task_1['author_id']
            )
        )
        assert_that(
            response2,
            has_entries(
                id=str(self.task_2['id']),
                task_name=self.task_2['task_name'].split('.')[-1],
                state=self.task_2['state'],
                author_id=self.task_2['author_id']
            )
        )
        assert_that(
            response3,
            has_entries(
                id=str(self.task_3['id']),
                task_name=self.task_3['task_name'].split('.')[-1],
                state=self.task_3['state'],
                author_id=self.task_3['author_id']
            )
        )

    @ tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_get_incorrect_taskid(self):
        # Проверяем, что при некорректном айдишнике он выдаст ошибку
        response = self.get_json(
            '/admin/tasks/{task_id}/'.format(
                task_id=0,
            ),
            expected_code=422
        )

    @ tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_taskid_doesnt_exist(self):
        # Проверяем, что если таска нет, то вернет 404
        response = self.get_json(
            '/admin/tasks/{task_id}/'.format(
                task_id=uuid.uuid4(),
            ),
            expected_code=404
        )


class TestAdminDependentTasksDetailView(TestCase):
    enable_admin_api = True

    def setUp(self):
        super(TestAdminDependentTasksDetailView, self).setUp()
        task_1 = TaskModel(self.main_connection).create(
            task_name='intranet.yandex_directory.src.yandex_directory.core.sms.tasks.SendSmsTask',
            params={'org_id': self.organization['id']},
            queue='default',
            ttl=1,
        )
        TaskModel(self.main_connection).set_state(task_1['id'], TASK_STATES.in_progress)
        self.task_1 = TaskModel(self.main_connection).get(task_1['id'])

        task_2 = TaskModel(self.main_connection).create(
            task_name='intranet.yandex_directory.src.yandex_directory.core.utils.tasks.UpdateLicenseCache',
            params={'org_id': self.organization['id']},
            queue='default',
            ttl=1,
        )
        TaskModel(self.main_connection).set_state(task_2['id'], TASK_STATES.success)
        self.task_2 = TaskModel(self.main_connection).get(task_2['id'])

        task_3 = TaskModel(self.main_connection).create(
            task_name='intranet.yandex_directory.src.yandex_directory.core.sms.tasks.SendSmsTask',
            params={},
            queue='default',
            ttl=1,
        )
        TaskModel(self.main_connection).set_state(task_3['id'], TASK_STATES.in_progress)
        TaskRelationsModel(self.main_connection).create(self.task_1['id'], task_3['id'])
        TaskRelationsModel(self.main_connection).create(self.task_1['id'], task_2['id'])
        TaskRelationsModel(self.main_connection).create(self.task_2['id'], task_3['id'])
        self.task_3 = TaskModel(self.main_connection).get(task_3['id'])

        tvm2_client_id = 42
        self.service = ServiceModel(self.meta_connection).create(
            slug=INTERNAL_ADMIN_SERVICE_SLUG,
            name='Internal Admin Section',
            tvm2_client_ids=[tvm2_client_id],
            scopes=[scope.internal_admin],
            internal=True,
        )
        self.tvm2_service = Service(
            id=self.service['id'],
            name=self.service['name'],
            identity=self.service['slug'],
            is_internal=True,
            ip='127.0.0.1',
        )

        self.headers = {
            'X-Ya-Service-Ticket': 'qqq',
            'X-ORG-ID': self.organization['id'],
        }

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_find_depend(self):
        # Проверяем, что 3 и 2 таски являются подтаском первого
        response = self.get_json(
            '/admin/tasks/dependencies/{dependet_id}/'.format(
                dependet_id=self.task_1['id'],

            ),
            expected_code=200
        )
        assert(len(response['result']) == 2)

        # Проверяем, что 3 таск являтется подтаском второго
        response = self.get_json(
            '/admin/tasks/dependencies/{dependet_id}/'.format(
                dependet_id=self.task_2['id'],

            ),
            expected_code=200
        )
        assert(len(response['result']) == 1)

    @ tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_get_incorrect_nochild_dependentid(self):
        # Проверяем, что при некорректном айдишнике, у которого нет детей ничего не выдаст
        response = self.get_json(
            '/admin/tasks/dependencies/{dependet_id}/'.format(
                dependet_id=self.task_3['id'],
            ),
            expected_code=200
        )
        assert(len(response['result']) == 0)

    @ tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_dependentid_incorrect(self):
        # Проверяем, что при некорреткном айдишнике выдаст ошибку
        response = self.get_json(
            '/admin/tasks/dependencies/{dependet_id}/'.format(
                dependet_id=0,
            ),
            expected_code=422
        )


class TestAdminTasksListView(TestCase):
    enable_admin_api = True

    def clean_actions_events_and_tasks(self):
        # Этот метод пустой для того, чтобы в setUp не подчищались таски
        pass

    def setUp(self):
        super(TestAdminTasksListView, self).setUp()
        task_1 = TaskModel(self.main_connection).create(
            task_name='intranet.yandex_directory.src.yandex_directory.core.sms.tasks.SendSmsTask',
            params={'org_id': self.organization['id']},
            queue='default',
            ttl=1,
        )
        TaskModel(self.main_connection).set_state(task_1['id'], TASK_STATES.in_progress)
        self.task_1 = TaskModel(self.main_connection).get(task_1['id'])

        task_2 = TaskModel(self.main_connection).create(
            task_name='intranet.yandex_directory.src.yandex_directory.core.utils.tasks.UpdateLicenseCache',
            params={'org_id': self.organization['id']},
            queue='default',
            ttl=1,
        )
        TaskModel(self.main_connection).set_state(task_2['id'], TASK_STATES.success)
        self.task_2 = TaskModel(self.main_connection).get(task_2['id'])

        task_3 = TaskModel(self.main_connection).create(
            task_name='intranet.yandex_directory.src.yandex_directory.core.sms.tasks.SendSmsTask',
            params={},
            queue='default',
            ttl=1,
        )
        TaskModel(self.main_connection).set_state(task_3['id'], TASK_STATES.in_progress)
        TaskRelationsModel(self.main_connection).create(self.task_1['id'], task_3['id'])
        self.task_3 = TaskModel(self.main_connection).get(task_3['id'])

        # таск создается автоматически при создании организации
        self.task_4 = TaskModel(self.main_connection).filter(
            task_name='intranet.yandex_directory.src.yandex_directory.core.utils.tasks.UpdateOrganizationMembersCountTask'
        ).one()

        tvm2_client_id = 42
        self.service = ServiceModel(self.meta_connection).create(
            slug=INTERNAL_ADMIN_SERVICE_SLUG,
            name='Internal Admin Section',
            tvm2_client_ids=[tvm2_client_id],
            scopes=[scope.internal_admin],
            internal=True,
        )
        self.tvm2_service = Service(
                    id=self.service['id'],
                    name=self.service['name'],
                    identity=self.service['slug'],
                    is_internal=True,
                    ip='127.0.0.1',
                )

        self.headers = {
            'X-Ya-Service-Ticket': 'qqq',
            'X-ORG-ID': self.organization['id'],
        }

    def test_list_of_tasks_by_org_id(self):
        # Ручка возвращает список тасков в нетерминальных стейтах ('in-progress', 'suspended', 'free'),
        # у которых в поле params есть org_id, равное заданному
        with tvm2_auth(
                100700,
                [scope.internal_admin],
                self.organization['id'],
                self.tvm2_service,
        ):
            response = self.get_json(
                '/admin/organizations/{org_id}/tasks/'.format(org_id=self.organization['id']),
                self.headers,
            )

        assert_that(
            response,
            has_entries(
                result=(
                    contains_inanyorder(_prepare_task(self.task_1))
                )
            )
        )

    def test_list_of_tasks_by_org_id_and_state(self):
        # Ручка возвращает список тасков c заданными стейтами,
        # у которых в поле params есть org_id, равное заданному
        with tvm2_auth(
                100700,
                [scope.internal_admin],
                self.organization['id'],
                self.tvm2_service,
        ):
            response = self.get_json(
                '/admin/organizations/{org_id}/tasks/?state={states}'.format(
                    org_id=self.organization['id'],
                    states=','.join([TASK_STATES.success, TASK_STATES.in_progress]),
                ),
                self.headers,
            )
        expected_response = list(map(_prepare_task, [self.task_1, self.task_2, self.task_4]))
        assert_that(
            response,
            has_entries(
                result=(
                    contains_inanyorder(*expected_response)
                )
            )
        )

    def test_list_of_tasks_by_dependent_id(self):
        # Возвращает все таски в нетерминальных стейтах, от которых зависит заданный
        with tvm2_auth(
                100700,
                [scope.internal_admin],
                self.organization['id'],
                self.tvm2_service,
        ):
            response = self.get_json(
                '/admin/organizations/{org_id}/tasks/?dependent_id={dependent_id}'.format(
                    org_id=self.organization['id'],
                    dependent_id=self.task_1['id']
                ),
                self.headers,
            )
        assert_that(
            response,
            has_entries(
                result=(
                    contains_inanyorder(_prepare_task(self.task_3))
                )
            )
        )

    def test_incorrect_dependent_id(self):
        # Если dependent_id не uuid, возвращаем ошибку
        with tvm2_auth(
                100700,
                [scope.internal_admin],
                self.organization['id'],
                self.tvm2_service,
        ):
            self.get_json(
                '/admin/organizations/{org_id}/tasks/?dependent_id={dependent_id}'.format(
                    org_id=self.organization['id'],
                    dependent_id='abc'
                ),
                self.headers,
                expected_code=422
            )

    def test_list_of_tasks_by_org_id_and_name(self):
        # Возвращает все таски в нетерминальных стейтах с заданным именем таска

        # Тут поменяем таску 2 стейт на нетерминальны и проверим,
        # что ручка его всё равно не вернёт, так как у него другое имя
        TaskModel(self.main_connection).set_state(self.task_2['id'], TASK_STATES.suspended)
        self.task_2 = TaskModel(self.main_connection).get(self.task_2['id'])
        with tvm2_auth(
                100700,
                [scope.internal_admin],
                self.organization['id'],
                self.tvm2_service,
        ):
            response = self.get_json(
                '/admin/organizations/{org_id}/tasks/?task_name={task_name}'.format(
                    org_id=self.organization['id'],
                    task_name='SendSmsTask',
                ),
                self.headers,
                process_tasks=False,
            )
            assert_that(
                response,
                has_entries(
                    result=(
                        contains_inanyorder(_prepare_task(self.task_1))
                    )
                )
            )
            response = self.get_json(
                '/admin/organizations/{org_id}/tasks/?task_name={task_name}'.format(
                    org_id=self.organization['id'],
                    task_name='SendSmsTask,UpdateLicenseCache',
                ),
                self.headers,
                process_tasks=False,
            )
            expected_response = list(map(_prepare_task, [self.task_1, self.task_2]))
            assert_that(
                response,
                has_entries(
                    result=(
                        contains_inanyorder(*expected_response)
                    )
                )
            )

    def test_prepare_task(self):
        # Проверяем, что имя таска отдается без модуля
        # а exception десериализуется
        failed_task = FailedTask(self.main_connection).delay(test_param=1, start_in=timedelta(seconds=-5))
        self.process_tasks()

        task = TaskModel(self.main_connection).get(failed_task.task_id)
        assert_that(
            prepare_task(task),
            has_entries(
                id=not_none(),
                task_name='FailedTask',
                state='failed',
                start_at=not_none(),
                created_at=not_none(),
                finished_at=not_none(),
                author_id=none(),
                result=none(),
                traceback=contains_string(ZeroDivisionError.__name__),
                dependencies_count=0,
                queue='autotests-default',
                params=has_entries(
                    test_param=1,
                ),
            )
        )


def _prepare_task(task):
    task['created_at'] = format_date(task['created_at'])
    task['start_at'] = format_date(task['start_at'])
    task['finished_at'] = format_date(task['finished_at'])
    task['id'] = str(task['id'])
    return prepare_task(task)
