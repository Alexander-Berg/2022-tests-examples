# -*- coding: utf-8 -*-
from datetime import timedelta
from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
    has_length,
    none,
    instance_of,
    contains_inanyorder,
    calling,
    raises,
)
from unittest.mock import patch

from intranet.yandex_directory.src.yandex_directory.common.utils import SENSITIVE_DATA_PLACEHOLDER
from intranet.yandex_directory.src.yandex_directory.core.utils import ROOT_DEPARTMENT_ID

from intranet.yandex_directory.src.yandex_directory.core.mail_migration.exception import ExistingPassportAccountError
from intranet.yandex_directory.src.yandex_directory.core.mail_migration.account.tasks import (
    CreateAccountTask,
    SetAccountConsistencyTask,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    TaskModel,
    UserModel,
    UserMetaModel,
    ActionModel)
from testutils import (
    TestCase,
    create_user,
    override_settings,
)
from intranet.yandex_directory.src.yandex_directory.core.task_queue.base import (
    TASK_STATES,
    get_default_queue,
)
from intranet.yandex_directory.src.yandex_directory.core.task_queue.worker import TaskProcessor
from intranet.yandex_directory.src.yandex_directory.core.task_queue.exceptions import DuplicatedTask


class TestSetAccountConsistencyTask(TestCase):

    def setUp(self):
        super(TestSetAccountConsistencyTask, self).setUp()
        self.nickname = 'tester'

    def test_set_consistency_task_new_account(self):
        # Аккаунта не было ни в паспорте, ни в директории
        # Проверяем, что таск завершился успешно
        with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.account.tasks.get_user_id_from_passport', return_value=None):
            task = SetAccountConsistencyTask(self.main_connection).delay(
                nickname=self.nickname,
                org_id=self.organization['id'],
            )
            self.process_tasks()

        assert_that(
            task.state,
            equal_to(TASK_STATES.success),
        )

    def test_set_consistency_task_existing_account(self):
        # Аккаунт существует и в паспорте и в директории, но id разные
        # Проверяем, что таск завершился со статусом failed
        with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.account.tasks.get_user_id_from_passport', return_value='2'):
            with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.account.tasks.get_user_id_from_directory', return_value=1):
                task = SetAccountConsistencyTask(self.main_connection).delay(
                    nickname=self.nickname,
                    org_id=self.organization['id'],
                )
                self.process_tasks()

        assert_that(
            task.state,
            equal_to(TASK_STATES.failed),
        )

    def test_set_consistency_task_account_exists_in_passport(self):
        # Аккаунт существует только в паспорте
        # Проверяем, что таск завершился со статусом failed
        # Проверям, что бросился ExistingPassportAccountError
        with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.account.tasks.get_user_id_from_passport', return_value=2):
            task = SetAccountConsistencyTask(self.main_connection).delay(
                nickname=self.nickname,
                org_id=self.organization['id'],
            )
            self.process_tasks()

        assert_that(
            task.state,
            equal_to(TASK_STATES.failed),
        )
        assert_that(
            task.exception,
            instance_of(ExistingPassportAccountError),
        )

    def test_set_consistency_task_account_exists_in_directory(self):
        # Аккаунт существует только в директории
        # Проверяем, что таск уволил имеющегося пользователя
        # и завршился успешно
        user = create_user(
            self.meta_connection,
            self.main_connection,
            user_id=1,
            nickname=self.nickname,
            name={'first': self.nickname, 'last': self.nickname},
            email="{}@not_yandex_test.ws.autotest.yandex.ru".format(self.nickname),
            org_id=self.organization['id'],
        )

        assert_that(
            UserModel(self.main_connection).filter(
                id=user['id'],
                org_id=self.organization['id'],
                is_dismissed=True,
            ),
            has_length(0),
        )

        with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.account.tasks.get_user_id_from_passport', return_value=None):
            task = SetAccountConsistencyTask(self.main_connection).delay(
                nickname=self.nickname,
                org_id=self.organization['id'],
            )
            self.process_tasks()

        assert_that(
            task.state,
            equal_to(TASK_STATES.success),
        )
        assert_that(
            UserModel(self.main_connection).filter(
                id=user['id'],
                org_id=self.organization['id'],
                is_dismissed=True,
            ),
            has_length(1),
        )


class TestCreateAccountTask(TestCase):

    def setUp(self):
        super(TestCreateAccountTask, self).setUp()
        self.nickname = 'tester'

    def test_create_account_task__success(self):
        # Создаем таск на создание аккаунта
        # Проверяем, что аккаунт создан
        # Проверяем, что метаданные созданы
        # Проверяем, что таск успешно завершился, и в поле result таска записался id созданного аккаунта
        # Проверяем, что в параметры таска записались email и old_password
        task = CreateAccountTask(self.main_connection).delay(
            nickname=self.nickname,
            password='test-password',
            org_id=self.organization['id'],
            email='test@test',
            old_password='old-password',
            first_name=None,
            last_name=None,
        )
        self.process_tasks()

        user = UserModel(self.main_connection).filter(
            nickname=self.nickname,
            org_id=self.organization['id'],
        ).one()
        assert_that(
            user,
            has_entries(
                nickname=self.nickname,
                first_name=self.nickname.capitalize(),
                last_name=self.nickname.capitalize(),
                org_id=self.organization['id'],
                department_id=ROOT_DEPARTMENT_ID,
            )
        )
        user_meta_data = UserMetaModel(self.meta_connection).filter(
            org_id=self.organization['id'],
            id=user['id'],
        )
        assert_that(
            user_meta_data,
            has_length(1),
        )
        assert_that(
            task.state,
            equal_to(TASK_STATES.success),
        )
        assert_that(
            task.get_result(),
            equal_to(user['id']),
        )
        task_from_db = TaskModel(self.main_connection).get(id=task.task_id)
        assert_that(
            task_from_db['params'],
            has_entries(
                email='test@test',
                old_password=SENSITIVE_DATA_PLACEHOLDER,
            ),
        )
        actions = ActionModel(self.main_connection) \
            .filter(org_id=self.organization['id'], name='user_add') \
            .all()
        assert_that(
            actions,
            contains_inanyorder(
                has_entries(
                    object=has_entries(
                        id=user['id'],
                        nickname=user['nickname'],
                    )
                )
            )
        )

    def test_create_account_task__rollback(self):
        # Создаем в базе пользователя
        # Запускаем таск так, чтобы отработал откат
        # Проверяем, что откат удалил пользователя из базы
        # Проверям, что таск завершился со статусом rollback
        user = create_user(
            self.meta_connection,
            self.main_connection,
            user_id=1,
            nickname=self.nickname,
            name={'first': self.nickname, 'last': self.nickname},
            email="{}@not_yandex_test.ws.autotest.yandex.ru".format(self.nickname),
            org_id=self.organization['id'],
        )

        with patch.object(CreateAccountTask, 'do', side_effect=Exception):
            task = CreateAccountTask(self.main_connection).delay(
                nickname=self.nickname,
                password='test-password',
                org_id=self.organization['id'],
                email="{}@not_yandex_test.ws.autotest.yandex.ru".format(self.nickname),
                old_password='old-password',
                first_name=None,
                last_name=None,
            )
            self.process_tasks()

        assert_that(
            UserModel(self.main_connection).filter(id=user['id']).one(),
            none(),
        )
        assert_that(
            task.state,
            equal_to(TASK_STATES.rollback),
        )
        actions = ActionModel(self.main_connection) \
            .filter(org_id=self.organization['id'], name='user_dismiss') \
            .all()
        assert_that(
            actions,
            contains_inanyorder(
                has_entries(
                    object=has_entries(
                        id=user['id'],
                        nickname=user['nickname'],
                    )
                )
            )
        )

    def test_full_account_creation_process__existing_account(self):
        # Проверяем, что если аккаунт существует и в паспорте и в директории
        # таск SetAccountConsistencyTask завершается успешно и записывает
        # в метаданные таска CreateAccountTask id существующего аккаунта.
        # Таск CreateAccountTask при этом переходит в статус free
        # А когда его берет воркер, он успешно завершается
        # в result записывается id существующего аккаунта из метаданных
        set_consistency_task = SetAccountConsistencyTask(self.main_connection).delay(
            start_in=timedelta(seconds=-5),
            nickname=self.nickname,
            org_id=self.organization['id'],
        )
        create_account_task = CreateAccountTask(
            self.main_connection,
            depends_on=[set_consistency_task.task_id]
        ).delay(
            start_in=timedelta(seconds=-5),
            nickname=self.nickname,
            org_id=self.organization['id'],
            password='test-password',
            email='test@test',
            old_password='old-password',
            first_name=None,
            last_name=None,
        )
        locked_data = TaskModel(self.main_connection).lock_for_worker('worker', get_default_queue())
        existing_account_id = 2
        if locked_data:
            with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.account.tasks.get_user_id_from_passport', return_value=str(existing_account_id)):
                with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.account.tasks.get_user_id_from_directory', return_value=existing_account_id):
                    TaskProcessor(SetAccountConsistencyTask, locked_data).process(self.main_connection)

        assert_that(
            set_consistency_task.state,
            equal_to(TASK_STATES.success),
        )
        assert_that(
            create_account_task.get_metadata(),
            has_entries(
                account_id=existing_account_id,
            )
        )
        assert_that(
            create_account_task.state,
            equal_to(TASK_STATES.free),
        )

        locked_data = TaskModel(self.main_connection).lock_for_worker('worker', get_default_queue())
        if locked_data:
            TaskProcessor(CreateAccountTask, locked_data).process(self.main_connection)

        assert_that(
            create_account_task.state,
            equal_to(TASK_STATES.success),
        )
        assert_that(
            create_account_task.get_result(),
            equal_to(existing_account_id),
        )

    def test_full_account_creation_process__new_account(self):
        # Проверяем, что если аккаунт не существует ни в паспорте ни в директории
        # таск SetAccountConsistencyTask завершается успешно
        # Таск CreateAccountTask создает новый аккаунт
        set_consistency_task = SetAccountConsistencyTask(self.main_connection).delay(
            start_in=timedelta(seconds=-5),
            nickname=self.nickname,
            org_id=self.organization['id'],
        )
        create_account_task = CreateAccountTask(
            self.main_connection,
            depends_on=[set_consistency_task.task_id]
        ).delay(
            start_in=timedelta(seconds=-5),
            nickname=self.nickname,
            org_id=self.organization['id'],
            password='test-password',
            email='test@test',
            old_password='old-password',
            first_name=None,
            last_name=None,
        )
        locked_data = TaskModel(self.main_connection).lock_for_worker('worker', get_default_queue())

        if locked_data:
            with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.account.tasks.get_user_id_from_passport', return_value=None):
                TaskProcessor(SetAccountConsistencyTask, locked_data).process(self.main_connection)

        assert_that(
            set_consistency_task.state,
            equal_to(TASK_STATES.success),
        )
        assert_that(
            create_account_task.state,
            equal_to(TASK_STATES.free),
        )

        locked_data = TaskModel(self.main_connection).lock_for_worker('worker', get_default_queue())

        if locked_data:
            TaskProcessor(CreateAccountTask, locked_data).process(self.main_connection)

        assert_that(
            create_account_task.state,
            equal_to(TASK_STATES.success),
        )

        user = UserModel(self.main_connection).filter(
            nickname=self.nickname,
            org_id=self.organization['id'],
        ).one()
        assert_that(
            user,
            has_entries(
                id=create_account_task.get_result(),
                nickname=self.nickname,
                first_name=self.nickname.capitalize(),
                last_name=self.nickname.capitalize(),
                org_id=self.organization['id'],
            )
        )

    def test_create_account_on_dependency_fail(self):
        # Проверяем, что если в таске SetAccountConsistencyTask произошла ошибка,
        # таск CreateAccountTask переходит в статус failed
        # И что воркер не возьмет задачу CreateAccountTask, несмотря на то,
        # что у неё ещё осталитсь попытки и есть rollback
        set_consistency_task = SetAccountConsistencyTask(self.main_connection).delay(
            start_in=timedelta(seconds=-5),
            nickname=self.nickname,
            org_id=self.organization['id'],
        )
        create_account_task = CreateAccountTask(
            self.main_connection,
            depends_on=[set_consistency_task.task_id]
        ).delay(
            start_in=timedelta(seconds=-5),
            nickname=self.nickname,
            org_id=self.organization['id'],
            password='test-password',
            email='test@test',
            old_password='old-password',
            first_name=None,
            last_name=None,
        )
        locked_data = TaskModel(self.main_connection).lock_for_worker('worker', get_default_queue())
        locked_data['tries']=SetAccountConsistencyTask.tries-1

        if locked_data:
            with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.account.tasks.get_user_id_from_passport', return_value=2):
                TaskProcessor(SetAccountConsistencyTask, locked_data).process(self.main_connection)

        assert_that(
            set_consistency_task.state,
            equal_to(TASK_STATES.failed),
        )
        assert_that(
            create_account_task.state,
            equal_to(TASK_STATES.canceled),
        )
        locked_data = TaskModel(self.main_connection).lock_for_worker('worker', get_default_queue())
        assert_that(
            locked_data,
            none(),
        )

    def test_cacnel_cleans_private_data(self):
        task = CreateAccountTask(self.main_connection).delay(
            nickname=self.nickname,
            password='test-password',
            org_id=self.organization['id'],
            email='test@test',
            old_password='old-password',
            first_name=None,
            last_name=None,
        )
        CreateAccountTask(self.main_connection, task_id=task.task_id).cancel()
        state = TaskModel(self.main_connection).get_state(task.task_id)
        params = TaskModel(self.main_connection).get_params(task.task_id)

        assert_that(
            state,
            equal_to(TASK_STATES.canceled),
        )

        assert_that(
            params,
            has_entries(
                password=SENSITIVE_DATA_PLACEHOLDER,
                old_password=SENSITIVE_DATA_PLACEHOLDER,
            )
        )

    def test_duplicated_task(self):
        # проверяем, что при поиске duplicate тасков учитываются только org_id и nickname
        CreateAccountTask(self.main_connection).delay(
            start_in=timedelta(minutes=1),
            nickname=self.nickname,
            password='test-password',
            org_id=self.organization['id'],
            email='test@test',
            old_password='old-password',
            first_name='Test',
            last_name='Test',
        )

        assert_that(
            calling(
                CreateAccountTask(self.main_connection).delay
            ).with_args(
                nickname=self.nickname,
                password='234%!',
                org_id=self.organization['id'],
                email='test234@test',
                old_password='12345',
                first_name='Test123',
                last_name='Tes123t',
            ),
            raises(DuplicatedTask)
        )
