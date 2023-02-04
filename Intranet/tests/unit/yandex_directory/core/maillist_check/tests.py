# -*- coding: utf-8 -*-

from hamcrest import (
    assert_that,
    equal_to,
    none,
    has_entries,
)

from testutils import (
    TestCase,
    create_department,
    create_group,
    create_user,
)
from intranet.yandex_directory.src.yandex_directory.common.utils import lstring
from intranet.yandex_directory.src.yandex_directory.core.maillist_check.tasks import MaillistsCheckTask
from intranet.yandex_directory.src.yandex_directory.core.models import (
    UserModel,
    MaillistCheckModel,
    DepartmentModel,
    GroupModel,
)
from intranet.yandex_directory.src.yandex_directory.core.task_queue.base import (
    TASK_STATES,
)


class TestMaillistsCheckTask__departments(TestCase):

    def setUp(self):
        super(TestMaillistsCheckTask__departments, self).setUp()
        MaillistCheckModel(self.main_connection).create(
            org_id=self.organization['id'],
            revision=2,
        )
        self.last_revision = 3
        self.dep_id = 5
        self.dep_uid = 111
        self.dep_label = 'new_dep'
        create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=1,
            dep_id=self.dep_id,
            label=self.dep_label,
            uid=self.dep_uid,
        )
        # сделаем пользователя
        self.user_id = 101
        create_user(
            self.meta_connection,
            self.main_connection,
            user_id=self.user_id,
            nickname='petya',
            name={'first': lstring('Test'), 'last': lstring('Testov')},
            email='test@ya.ru',
            groups=[],
            org_id=self.organization['id'],
            department_id=self.dep_id,
        )


    def test_maillist_check_departments_ok(self):
        # Состав рассылки (департамент) в BigML совпадает с директорией.
        # В рассылку отдела должны включаться id всех сотрудников во вложенных
        # отделах + сами вложенные подотделы.

        # Сначала создадим под-отдел и переместим пользователя в подотдел
        org_id = self.organization['id']
        inner_dep_uid = 42
        inner_dep = create_department(
            self.main_connection,
            org_id=org_id,
            parent_id=self.dep_id,
            label='inner',
            uid=inner_dep_uid,
        )

        UserModel(self.main_connection) \
            .filter(org_id=org_id, id=self.user_id) \
            .update(department_id=inner_dep['id'])

        # Теперь у нас должна быть такая структура компании:
        #
        #  Все сотрудники
        #    Департамент (new_dep@ 111)
        #      Департамент (inner@ 42)
        #        user
        #
        # Соответственно, в bigML должно быть две рассылки:
        # В рассылку отдела new_dep включено два uid – отдела inner и пользователя
        # в рассылку отдела inner - только пользователь
        #
        # Замокаем эти данные и посмотрим как сработает сверка:

        def bigml_response(*args, **kwargs):
            list_uid = kwargs['get_params']['list_uid']
            response = []
            if list_uid == inner_dep['uid']:
                response = [{'uid': self.user_id}]
            if list_uid == self.dep_uid:
                response = [
                    {'uid': inner_dep['uid']},
                    {'uid': self.user_id},
                ]
            return {'response': response}

        self.mocked_bigml_get.side_effect = bigml_response
        task = MaillistsCheckTask(self.main_connection).delay(
            org_id=org_id,
        )
        self.process_tasks()

        assert_that(
            task.state,
            equal_to(TASK_STATES.success),
        )
        maillist_check = MaillistCheckModel(self.main_connection).get(self.organization['id'])
        assert_that(
            maillist_check,
            has_entries(
                ml_is_ok=True,
                problems=none(),
                revision=self.last_revision,
            )
        )

    def test_maillist_check_departments_only_in_ml(self):
        # Пользователь есть в BigML, но отсутствует в директории
        bigml_response = {
                'response': [
                    {'uid': self.user_id},
                    {'uid': 102},
                ]
            }

        self.mocked_bigml_get.return_value = bigml_response
        task = MaillistsCheckTask(self.main_connection).delay(
            org_id=self.organization['id'],
        )
        self.process_tasks()

        assert_that(
            task.state,
            equal_to(TASK_STATES.success),
        )
        maillist_check = MaillistCheckModel(self.main_connection).get(self.organization['id'])
        assert_that(
            maillist_check,
            has_entries(
                ml_is_ok=False,
                problems='Department ({}@, {}) has problems\n'
                         'This users should not be subscribed: 102'.format(
                            self.dep_label, self.dep_uid),
                revision=self.last_revision,
            )
        )

    def test_maillist_check_departments_only_in_directory(self):
        # Пользователь есть в директории, но отсутствует в BigML
        bigml_response = {
                'response': [
                ]
            }

        self.mocked_bigml_get.return_value = bigml_response
        task = MaillistsCheckTask(self.main_connection).delay(
            org_id=self.organization['id'],
        )
        self.process_tasks()

        assert_that(
            task.state,
            equal_to(TASK_STATES.success),
        )
        maillist_check = MaillistCheckModel(self.main_connection).get(self.organization['id'])
        assert_that(
            maillist_check,
            has_entries(
                ml_is_ok=False,
                problems='Department ({}@, {}) has problems\n'
                         'This users are not subscribed: {}'.format(
                            self.dep_label, self.dep_uid, self.user_id),
                revision=self.last_revision,
            )
        )

    def test_maillist_check_departments_no_ml_uid(self):
        # В директории нет uid-a рассылки
        DepartmentModel(self.main_connection).update_one(
            self.dep_id,
            self.organization['id'],
            data={'uid': None}
        )

        task = MaillistsCheckTask(self.main_connection).delay(
            org_id=self.organization['id'],
        )
        self.process_tasks()

        assert_that(
            task.state,
            equal_to(TASK_STATES.success),
        )
        maillist_check = MaillistCheckModel(self.main_connection).get(self.organization['id'])
        assert_that(
            maillist_check,
            has_entries(
                ml_is_ok=False,
                problems='Department {}@ has not uid'.format(self.dep_label),
                revision=self.last_revision,
            )
        )


class TestMaillistsCheckTask__groups(TestCase):

    def setUp(self):
        super(TestMaillistsCheckTask__groups, self).setUp()
        MaillistCheckModel(self.main_connection).create(
            org_id=self.organization['id'],
            revision=9,
        )
        self.last_revision = 3

        self.group_uid = 111
        self.group_label = 'new_group'
        group = create_group(
            self.main_connection,
            org_id=self.organization['id'],
            label=self.group_label,
            uid=self.group_uid,
        )
        self.group_id = group['id']

        # сделаем пользователя
        self.user_id = 101
        create_user(
            self.meta_connection,
            self.main_connection,
            user_id=self.user_id,
            nickname='petya',
            name={'first': lstring('Test'), 'last': lstring('Testov')},
            email='test@ya.ru',
            groups=[self.group_id],
            org_id=self.organization['id'],
        )

    def test_maillist_check_groups_ok(self):
        # Состав рассылки (группа) в BigML совпадает с директорией
        bigml_response = {
                'response': [
                    {'uid': self.user_id},
                    {'uid': self.group_uid},
                ]
            }

        self.mocked_bigml_get.return_value = bigml_response
        task = MaillistsCheckTask(self.main_connection).delay(
            org_id=self.organization['id'],
        )
        self.process_tasks()

        assert_that(
            task.state,
            equal_to(TASK_STATES.success),
        )
        maillist_check = MaillistCheckModel(self.main_connection).get(self.organization['id'])
        assert_that(
            maillist_check,
            has_entries(
                ml_is_ok=True,
                problems=none(),
                revision=self.last_revision,
            )
        )

    def test_maillist_check_groups_only_in_ml(self):
        # Пользователь есть в BigML, но отсутствует в директории
        bigml_response = {
                'response': [
                    {'uid': self.user_id},
                    {'uid': 102},
                ]
            }

        self.mocked_bigml_get.return_value = bigml_response
        task = MaillistsCheckTask(self.main_connection).delay(
            org_id=self.organization['id'],
        )
        self.process_tasks()

        assert_that(
            task.state,
            equal_to(TASK_STATES.success),
        )
        maillist_check = MaillistCheckModel(self.main_connection).get(self.organization['id'])
        assert_that(
            maillist_check,
            has_entries(
                ml_is_ok=False,
                problems='Group ({}@, {}) has problems\n'
                         'This users should not be subscribed: 102'.format(
                    self.group_label, self.group_uid),
                revision=self.last_revision,
            )
        )

    def test_maillist_check_groups_only_in_directory(self):
        # Пользователь есть в директории, но отсутствует в BigML
        bigml_response = {
                'response': [
                ]
            }

        self.mocked_bigml_get.return_value = bigml_response
        task = MaillistsCheckTask(self.main_connection).delay(
            org_id=self.organization['id'],
        )
        self.process_tasks()

        assert_that(
            task.state,
            equal_to(TASK_STATES.success),
        )
        maillist_check = MaillistCheckModel(self.main_connection).get(self.organization['id'])
        assert_that(
            maillist_check,
            has_entries(
                ml_is_ok=False,
                problems='Group ({}@, {}) has problems\n'
                         'This users are not subscribed: 101'.format(
                    self.group_label, self.group_uid),
                revision=self.last_revision,
            )
        )

    def test_maillist_check_groups_no_ml_uid(self):
        # В директории нет uid-a рассылки
        GroupModel(self.main_connection).update_one(
            self.organization['id'],
            self.group_id,
            data={'uid': None}
        )

        task = MaillistsCheckTask(self.main_connection).delay(
            org_id=self.organization['id'],
        )
        self.process_tasks()

        assert_that(
            task.state,
            equal_to(TASK_STATES.success),
        )
        maillist_check = MaillistCheckModel(self.main_connection).get(self.organization['id'])
        assert_that(
            maillist_check,
            has_entries(
                ml_is_ok=False,
                problems='Group {}@ has not uid'.format(self.group_label),
                revision=self.last_revision,
            )
        )

    def test_maillist_check_does_not_fail_on_nonexistent_organization(self):
        task = MaillistsCheckTask(self.main_connection).delay(
            org_id=100500,
        )
        self.process_tasks()

        assert_that(
            task.state,
            equal_to(TASK_STATES.success),
        )
