# -*- coding: utf-8 -*-
import uuid
from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
    contains,
    not_none,
    none,
    contains_inanyorder,
)
from unittest.mock import patch
from werkzeug.datastructures import FileMultiDict

from testutils import (
    TestCase,
    create_inner_uid,
    override_settings,
    source_path,
)
from intranet.yandex_directory.src.yandex_directory.common.utils import force_text
from intranet.yandex_directory.src.yandex_directory.core.views.mail_migration import _validate_migration_file
from intranet.yandex_directory.src.yandex_directory.core.mail_migration import (
    MailMigrationTask,
    CreateMailBoxesTask,
    DeleteCollectorsTask,
    CreateMailCollectorsTask,
    CreateMailCollectorTask,
    WaitingForMigrationsTask,
)
from intranet.yandex_directory.src.yandex_directory.core.mail_migration.utils import MAIL_MIGRATION_STATES
from intranet.yandex_directory.src.yandex_directory.core.utils import create_user
from intranet.yandex_directory.src.yandex_directory.passport import PassportApiClient
from intranet.yandex_directory.src.yandex_directory.passport.exceptions import (
    PasswordWeak,
    LoginProhibitedsymbols,
    LoginNotAvailable,
    LoginNotavailable,
    PasswordLikelogin, YandexMailToOrgWithDomain)
from intranet.yandex_directory.src.yandex_directory.core.models import MailMigrationFileModel, TaskModel
from intranet.yandex_directory.src.yandex_directory.core.task_queue import Task


class TestMailMigrationViewJson__post(TestCase):
    def setUp(self):
        super(TestMailMigrationViewJson__post, self).setUp()
        self.admin_user = self.create_user(is_outer=True)

    def test_upload_migration_data(self):
        # Создаем корректную Дату
        # Проверяем, что таск MailMigrationTask создался с правильными параметрами
        # Проверяем, что создался таск DeleteCollectorsTask
        # (Удаление файла замокано, чтобы можно было проверить контент файла)
        accounts_list = [{
                            'email': 'test1@test.com',
                            'password': 'valid_pwd_1',
                            'first_name': 'Тест',
                            'last_name': 'Тестов',
                            'new_login': 'testtestov1',
                            'new_password': 'new_valid_pwd1'
                          },
                          {
                            'email': 'test2@test.com',
                            'password': 'valid_pwd_2'
                          }
                        ]
        with patch.object(CreateMailBoxesTask, 'do', return_value=None), \
             patch.object(CreateMailCollectorsTask, 'do', return_value=None), \
             patch.object(WaitingForMigrationsTask, 'do', return_value=None), \
             patch.object(PassportApiClient, 'validate_login', return_value=None), \
             patch.object(PassportApiClient, 'validate_password', return_value=None), \
             patch.object(CreateMailBoxesTask, 'rollback', return_value=None):
            response = self.post_json(
                '/mail-migration/',
                data={
                    'accounts_list': accounts_list,
                    'host': 'testhost',
                    'port': 933,
                    'protocol': 'imap',
                    'ssl': False,
                    'no_delete_msgs': False,
                    'sync_abook': False,
                },
                expected_code=200,
            )

        assert_that(
            response,
            has_entries(
                mail_migration_id=not_none()
            ),
        )
        mail_migration_id = response['mail_migration_id']

        mf = MailMigrationFileModel(self.main_connection).filter(org_id=self.organization['id']).one()
        file_content = force_text('email,password,first_name,last_name,new_login,new_password\ntest1@test.com,valid_pwd_1,Тест,Тестов,testtestov1,new_valid_pwd1\ntest2@test.com,valid_pwd_2,,,,\n')

        assert_that(
            mf,
            has_entries(
                file=file_content,
            ),
        )


        task = TaskModel(self.main_connection).get(mail_migration_id)

        assert_that(
            task,
            has_entries(
                task_name=MailMigrationTask.get_task_name(),
                params=has_entries(
                    migration_file_id=str(mf['id']),
                    host='testhost',
                    port=933,
                    org_id=self.organization['id'],
                    imap=True,
                    ssl=False,
                    no_delete_msgs=False,
                    sync_abook=False,
                    mark_archive_read=True,
                )
            )
        )

        delete_collectors_task = TaskModel(self.main_connection).filter(
            task_name=DeleteCollectorsTask.get_task_name(),
        ).one()

        assert_that(
            delete_collectors_task,
            none(),
        )

    def test_upload_migration_data_no_email(self):
        # Создаем корректную Дату
        # Проверяем, что таск MailMigrationTask создался с правильными параметрами
        # Проверяем, что создался таск DeleteCollectorsTask
        # (Удаление файла замокано, чтобы можно было проверить контент файла)
        accounts_list = [{
            'password': 'valid_pwd_1',
            'first_name': 'Тест',
            'last_name': 'Тестов',
            'new_login': 'testtestov1',
            'new_password': 'new_valid_pwd1'
        },
            {
                'email': 'test2@test.com',
                'password': 'valid_pwd_2'
            }
        ]
        with patch.object(CreateMailBoxesTask, 'do', side_effect=None), \
             patch.object(CreateMailCollectorsTask, 'do', side_effect=None), \
             patch.object(WaitingForMigrationsTask, 'do', side_effect=None), \
             patch.object(PassportApiClient, 'validate_login', side_effect=None), \
             patch.object(PassportApiClient, 'validate_password', side_effect=None):
            response = self.post_json(
                '/mail-migration/',
                data={
                    'accounts_list': accounts_list,
                    'host': 'testhost',
                    'port': 933,
                    'protocol': 'imap',
                    'ssl': False,
                    'no_delete_msgs': False,
                    'sync_abook': False,
                },
                expected_code=422,
            )

    def test_upload_incorrect_migration_data(self):
        # Загружаем некорректные данные
        # Проверяем, что вернется ошибка с описанием
        accounts_list = [{
                            'email': 'test1@test.com',
                            'password': 'valid_pwd_1',
                            'first_name': 'Тест',
                            'last_name': 'Тестов',
                            'new_login': 'testtestov1',
                            'new_password': 'new_valid_pwd1'
                          },
                         {
                            'email': 'test4test.com',
                            'password': 'valid_pwd_4',
                            'last_name': 'abcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabc',
                         },
                         {
                             'email': 'test5@test.com',
                             'password': 'test5@test.com',
                         }
                         ]
        with patch.object(PassportApiClient, 'validate_login', side_effect=None), \
             patch.object(PassportApiClient, 'validate_password', side_effect=None):
            response = self.post_json(
                '/mail-migration/',
                data={
                    'accounts_list': accounts_list,
                    'host': 'testhost',
                    'port': 933,
                },
                expected_code=422,
            )
        error_list = [
            {'line': 3, 'message': ['invalid_email_format', 'last_name_is_too_long'], 'email': 'test4test.com'},
            {'line': 4, 'message': ['password.likelogin'], 'email': 'test5@test.com'},
        ]
        assert_that(
            response,
            has_entries(
                code='migration_json_is_invalid',
                params=has_entries(
                    errors=contains(*error_list)
                )
            ),
        )

        mf = MailMigrationFileModel(self.main_connection).filter(org_id=self.organization['id']).one()
        assert_that(
            mf,
            equal_to(None),
        )

    def test_upload_migration_data__fail_passport_validation(self):
        accounts_list = [{
                            'email': 'test1@test.com',
                            'password': 'valid_pwd_1',
                            'first_name': 'Тест',
                            'last_name': 'Тестов',
                            'new_login': 'testtestov1',
                            'new_password': 'new_valid_pwd1'
                          }]
        with patch.object(PassportApiClient, 'validate_login', side_effect=LoginProhibitedsymbols), \
            patch.object(PassportApiClient, 'validate_password', side_effect=PasswordWeak):
            response = self.post_json(
                '/mail-migration/',
                data={
                    'accounts_list': accounts_list,
                    'host': 'testhost',
                    'port': 933,
                },
                expected_code=422,
            )

        error_list = [{'email': 'test1@test.com', 'line': 2, 'message': ['login.prohibitedsymbols', 'password.weak']}]
        assert_that(
            response,
            has_entries(
                code='migration_json_is_invalid',
                params=has_entries(
                    errors=contains(*error_list),
                )
            ),
        )

    def test_bad_json_format(self):
        accounts_list = [{
            'email': 'something@wrong.ru',
            'password': 'Maybe there(no)',
            },
            'wrong_field']
        with patch.object(CreateMailBoxesTask, 'do', side_effect=None), \
             patch.object(CreateMailCollectorsTask, 'do', side_effect=None), \
             patch.object(WaitingForMigrationsTask, 'do', side_effect=None), \
             patch.object(PassportApiClient, 'validate_login', side_effect=None), \
             patch.object(PassportApiClient, 'validate_password', side_effect=None):
            response = self.post_json(
                '/mail-migration/',
                data={
                    'accounts_list': accounts_list,
                    'host': 'testhost',
                    'port': 933,
                    'protocol': 'imap',
                    'ssl': False,
                    'no_delete_msgs': False,
                    'sync_abook': False,
                },
                expected_code=422,
            )

    def test_upload_invalid_additional_params(self):
        # Создаем корректную Дату
        # Проверяем, что таск MailMigrationTask создался с правильными параметрами
        # Проверяем, что создался таск DeleteCollectorsTask
        # (Удаление файла замокано, чтобы можно было проверить контент файла)
        accounts_list = [{
                            'email': 'test1@test.com',
                            'password': 'valid_pwd_1',
                            'first_name': 'Тест',
                            'last_name': 'Тестов',
                            'new_login': 'testtestov1',
                            'new_password': 'new_valid_pwd1'
                          },
                          {
                            'email': 'test2@test.com',
                            'password': 'valid_pwd_2'
                          }
                        ]
        with patch.object(CreateMailBoxesTask, 'do', side_effect=None), \
             patch.object(CreateMailCollectorsTask, 'do', side_effect=None), \
             patch.object(WaitingForMigrationsTask, 'do', side_effect=None), \
             patch.object(PassportApiClient, 'validate_login', side_effect=None), \
             patch.object(PassportApiClient, 'validate_password', side_effect=None):
            response = self.post_json(
                '/mail-migration/',
                data={
                    'accounts_list': accounts_list,
                    'host': 'testhost',
                    'port': 933,
                    'ssl': 'ivalid-value',
                },
                expected_code=422,
            )

    def test_duplicate_login(self):
        accounts_list = [
            {
                'email': 'test1@test.com',
                'password': 'valid_pwd_1',
                'first_name': 'Тест',
                'last_name': 'Тестов',
                'new_login': 'testtestov1',
                'new_password': 'new_valid_pwd1'
            },
            {
                'email': 'testtestov1@test.com',
                'password': 'valid_pwd_2'
            }
        ]
        with patch.object(PassportApiClient, 'validate_login', side_effect=None), \
             patch.object(PassportApiClient, 'validate_password', side_effect=None):
            response = self.post_json(
                '/mail-migration/',
                data={
                    'accounts_list': accounts_list,
                    'host': 'testhost',
                    'port': 933,
                },
                expected_code=422,
            )
        duplicate_logins = ['testtestov1']
        assert_that(
            response,
            has_entries(
                code='duplicate_login',
                params=has_entries(
                    duplicate_logins=contains_inanyorder(*duplicate_logins)
                )
            ),
        )
        mf = MailMigrationFileModel(self.main_connection).filter(org_id=self.organization['id']).one()
        assert_that(
            mf,
            equal_to(None),
        )


    def test_yandex_mail_to_same_domain(self):
        # Пытаемся смигрировать яндексовскую почту в логин, который уже занят, так как нет поля new_login.
        accounts_list = [
            {
                'email': 'test1@not_yandex_test.ws.autotest.yandex.ru',
                'password': 'valid_pwd_1',
            }
        ]
        with patch.object(PassportApiClient, 'validate_login', side_effect=None), \
             patch.object(PassportApiClient, 'validate_password', side_effect=None):
            response = self.post_json(
                '/mail-migration/',
                data={
                    'accounts_list': accounts_list,
                    'host': 'imap.yandex.ru',
                    'port': 933,
                },
                expected_code=422,
            )
        error_list = [{'email': 'test1@not_yandex_test.ws.autotest.yandex.ru', 'line': 2, 'message': [YandexMailToOrgWithDomain.code]}]
        assert_that(
            response,
            has_entries(
                code='migration_json_is_invalid',
                params=has_entries(
                    errors=contains(*error_list),
                )
            ),
        )

    def test_yandex_mail_to_same_domain_newlogin(self):
        # Пытаемся смигрировать яндексовую почту в логин, который уже имеется в организации
        user_data = {
            'id': create_inner_uid(12345),
            'name': {
                'first': {
                    'ru': 'Gena'
                },
                'last': {
                    'ru': 'Chibisov'
                }
            },
            'gender': 'male',
            'nickname': 'testtestov1',
            'email': 'web-chib@ya.ru',
        }

        create_user(
            self.meta_connection,
            self.main_connection,
            org_id=self.organization['id'],
            user_data=user_data,
            nickname=user_data['nickname']
        )


        accounts_list = [
            {
                'email': 'test1@not_yandex_test.ws.autotest.yandex.ru',
                'password': 'valid_pwd_1',
                'new_login': 'testtestov1',
            }
        ]
        with patch.object(PassportApiClient, 'validate_login', side_effect=None), \
             patch.object(PassportApiClient, 'validate_password', side_effect=None):
            response = self.post_json(
                '/mail-migration/',
                data={
                    'accounts_list': accounts_list,
                    'host': 'imap.yandex.ru',
                    'port': 933,
                },
                expected_code=422,
            )
        error_list = [
            {'email': 'test1@not_yandex_test.ws.autotest.yandex.ru', 'line': 2, 'message': [YandexMailToOrgWithDomain.code]}]
        assert_that(
            response,
            has_entries(
                code='migration_json_is_invalid',
                params=has_entries(
                    errors=contains(*error_list),
                )
            ),
        )


class TestMailMigrationView__post(TestCase):
    def setUp(self):
        super(TestMailMigrationView__post, self).setUp()
        self.admin_user = self.create_user(is_outer=True)

    def test_upload_migration_file(self):
        # Загружаем корректный файл
        # Проверяем, что он записался в базу и что контент совпадает с загруженным файлом
        # Проверяем, что таск MailMigrationTask создался с правильными параметрами
        # Проверяем, что создался таск DeleteCollectorsTask
        # (Удаление файла замокано, чтобы можно было проверить контент файла)
        filesdict = FileMultiDict()
        file_path = source_path(
            'intranet/yandex_directory/tests/functional/api/mail_migration/data/valid_migration.csv'
        )
        f = open(file_path, 'rb')
        filesdict.add_file('migration_file', f, filename=file_path)
        migration_file = filesdict.get('migration_file')

        with patch.object(CreateMailBoxesTask, 'do', side_effect=None), \
             patch.object(CreateMailCollectorsTask, 'do', side_effect=None), \
             patch.object(WaitingForMigrationsTask, 'do', side_effect=None), \
             patch.object(PassportApiClient, 'validate_login', side_effect=None), \
             patch.object(PassportApiClient, 'validate_password', side_effect=None), \
             patch.object(CreateMailBoxesTask, 'rollback', side_effect=None):
            response = self.post_form_data(
                '/mail-migration/',
                data={
                    'migration_file': migration_file,
                    'host': 'testhost',
                    'port': 933,
                    'protocol': 'imap',
                    'ssl': False,
                    'no_delete_msgs': False,
                    'sync_abook': False,
                },
                expected_code=200,
            )
        assert_that(
            response,
            has_entries(
                mail_migration_id=not_none()
            ),
        )
        mail_migration_id = response['mail_migration_id']

        mf = MailMigrationFileModel(self.main_connection).filter(org_id=self.organization['id']).one()
        file_path = source_path(
            'intranet/yandex_directory/tests/functional/api/mail_migration/data/valid_migration.csv'
        )
        with open(file_path) as f:
            file_content = f.read()
        assert_that(
            mf,
            has_entries(
                file=file_content,
            ),
        )

        task = TaskModel(self.main_connection).get(mail_migration_id)

        assert_that(
            task,
            has_entries(
                task_name=MailMigrationTask.get_task_name(),
                params=has_entries(
                    migration_file_id=str(mf['id']),
                    host='testhost',
                    port=933,
                    org_id=self.organization['id'],
                    imap=True,
                    ssl=False,
                    no_delete_msgs=False,
                    sync_abook=False,
                    mark_archive_read=True,
                )
            )
        )

        delete_collectors_task = TaskModel(self.main_connection).filter(
            task_name=DeleteCollectorsTask.get_task_name(),
        ).one()

        assert_that(
            delete_collectors_task,
            none(),
        )

    def test_upload_incorrect_migration_file(self):
        # Загружаем некорректный файл
        # Проверяем, что вернется ошибка с описанием
        filesdict = FileMultiDict()
        file_path = source_path(
            'intranet/yandex_directory/tests/functional/api/mail_migration/data/invalid_migration.csv'
        )
        f = open(file_path, 'rb')
        filesdict.add_file('migration_file', f, filename=file_path)
        migration_file = filesdict.get('migration_file')

        with patch.object(PassportApiClient, 'validate_login', side_effect=None), \
            patch.object(PassportApiClient, 'validate_password', side_effect=None):
            response = self.post_form_data(
                '/mail-migration/',
                data={
                    'migration_file': migration_file,
                    'host': 'testhost',
                    'port': 933,
                },
                expected_code=422,
            )
        error_list = [
            {'line': 3, 'errors': ['invalid_record_length'], 'email': 'test2@test.com'},
            {'line': 4, 'errors': ['invalid_email_format', 'last_name_is_too_long'], 'email': 'test4test.com'},
            {'line': 5, 'errors': [PasswordLikelogin.code], 'email': 'test5@test.com'},
        ]
        assert_that(
            response,
            has_entries(
                code='migration_file_is_invalid',
                params=has_entries(
                    errors=contains(*error_list)
                )
            ),
        )

        mf = MailMigrationFileModel(self.main_connection).filter(org_id=self.organization['id']).one()
        assert_that(
            mf,
            equal_to(None),
        )

    def test_error_if_we_are_trying_to_import_from_one_box_into_multiple_accounts(self):
        content = """
email,password,first_name,last_name,new_login,new_password
some@example.com,password,Пользователь,Раз,first,first_password
some@example.com,password,Пользователь,Два,second,second_password
        """
        host = 'testhost'
        with patch.object(PassportApiClient, 'validate_login', side_effect=None), \
             patch.object(PassportApiClient, 'validate_password', side_effect=None):
            errors, errors_to_mail, duplicates = _validate_migration_file(
                self.main_connection,
                self.organization['id'],
                content.encode('utf-8'),
                host,
            )
            bad_line = 3
            assert_that(errors.get(bad_line), contains('email_already_used'))
            assert errors_to_mail.get(bad_line) == 'some@example.com'

    def test_upload_migration_file__fail_passport_validation(self):
        filesdict = FileMultiDict()
        file_path = source_path(
            'intranet/yandex_directory/tests/functional/api/mail_migration/data/failed_passport_validation.csv'
        )
        f = open(file_path, 'rb')
        filesdict.add_file('migration_file', f, filename=file_path)
        migration_file = filesdict.get('migration_file')

        with patch.object(PassportApiClient, 'validate_login', side_effect=LoginProhibitedsymbols), \
            patch.object(PassportApiClient, 'validate_password', side_effect=PasswordWeak):
            response = self.post_form_data(
                '/mail-migration/',
                data={
                    'migration_file': migration_file,
                    'host': 'testhost',
                    'port': 933,
                },
                expected_code=422,
            )

        error_list = [PasswordWeak.code, LoginProhibitedsymbols.code]
        assert_that(
            response,
            has_entries(
                code='migration_file_is_invalid',
                params=has_entries(
                    errors=contains_inanyorder(
                        has_entries(
                            line=2,
                            errors=contains_inanyorder(*error_list),
                        )
                    )
                )
            ),
        )

        mf = MailMigrationFileModel(self.main_connection).filter(org_id=self.organization['id']).one()
        assert_that(
            mf,
            equal_to(None),
        )

    def test_upload_incorrect_migration_file_no_fields(self):
        # Загружаем некорректный файл
        # Проверяем, что вернется ошибка с описанием
        filesdict = FileMultiDict()
        file_path = source_path(
            'intranet/yandex_directory/tests/functional/api/mail_migration/data/invalid_migration_no_fields.csv'
        )
        f = open(file_path, 'rb')
        filesdict.add_file('migration_file', f, filename=file_path)
        migration_file = filesdict.get('migration_file')

        response = self.post_form_data(
            '/mail-migration/',
            data={
                'migration_file': migration_file,
                'host': 'testhost',
                'port': 933,
            },
            expected_code=422,
        )

        assert_that(
            response,
            has_entries(
                code='required_fields_missed',
                message='{fields} are required fields',
            ),
        )

        mf = MailMigrationFileModel(self.main_connection).filter(org_id=self.organization['id']).one()
        assert_that(
            mf,
            equal_to(None),
        )

    def test_no_host_expect_error(self):
        # Не передаем в ручку хост
        # Проверяем, что вернется ошибка с описанием
        filesdict = FileMultiDict()
        file_path = source_path(
            'intranet/yandex_directory/tests/functional/api/mail_migration/data/invalid_migration_no_fields.csv'
        )
        f = open(file_path, 'rb')
        filesdict.add_file('migration_file', f, filename=file_path)
        migration_file = filesdict.get('migration_file')

        response = self.post_form_data(
            '/mail-migration/',
            data={
                'migration_file': migration_file,
                'port': 993,
            },
            expected_code=422,
        )

        assert_that(
            response,
            has_entries(
                code='required_field',
                message='Please, provide field "{field}"',
                params=has_entries(field='host'),
            ),
        )

        mf = MailMigrationFileModel(self.main_connection).filter(org_id=self.organization['id']).one()
        assert_that(
            mf,
            equal_to(None),
        )

    def test_no_port_expect_error(self):
        # Не передаем в ручку порт
        # Проверяем, что вернется ошибка с описанием
        filesdict = FileMultiDict()
        file_path = source_path(
            'intranet/yandex_directory/tests/functional/api/mail_migration/data/invalid_migration_no_fields.csv'
        )
        f = open(file_path, 'rb')
        filesdict.add_file('migration_file', f, filename=file_path)
        migration_file = filesdict.get('migration_file')

        response = self.post_form_data(
            '/mail-migration/',
            data={
                'migration_file': migration_file,
                'host': 'test'
            },
            expected_code=422,
        )

        assert_that(
            response,
            has_entries(
                code='required_field',
                message='Please, provide field "{field}"',
                params=has_entries(field='port'),
            ),
        )

        mf = MailMigrationFileModel(self.main_connection).filter(org_id=self.organization['id']).one()
        assert_that(
            mf,
            equal_to(None),
        )

    def test_invalid_additional_params(self):
        # получаем ошибку если передаем недопустимое значение для
        # дополнительных параметров мигратора ящиков
        filesdict = FileMultiDict()
        file_path = source_path(
            'intranet/yandex_directory/tests/functional/api/mail_migration/data/valid_migration.csv'
        )
        f = open(file_path, 'rb')
        filesdict.add_file('migration_file', f, filename=file_path)
        migration_file = filesdict.get('migration_file')

        with patch.object(CreateMailBoxesTask, 'do', side_effect=None), \
            patch.object(CreateMailCollectorsTask, 'do', side_effect=None):
            response = self.post_form_data(
                '/mail-migration/',
                data={
                    'migration_file': migration_file,
                    'host': 'testhost',
                    'port': 933,
                    'ssl': '123',
                },
                expected_code=422,
            )
        assert_that(
            response,
            has_entries(
                code='invalid_value',
                params={
                    'field': 'ssl'
                }
            ),
        )

    def test_login_notavailable_error(self):
        # Загружаем корректный файл, но делаем вид, что такой аккаунт уже есть в пасспорте
        # Проверяем, что он записался в базу и что контент совпадает с загруженным файлом
        # Проверяем, что таск MailMigrationTask создался с правильными параметрами
        # (Удаление файла замокано, чтобы можно было проверить контент файла)
        filesdict = FileMultiDict()
        file_path = source_path(
            'intranet/yandex_directory/tests/functional/api/mail_migration/data/failed_passport_validation.csv'
        )
        f = open(file_path, 'rb')
        filesdict.add_file('migration_file', f, filename=file_path)
        migration_file = filesdict.get('migration_file')

        with patch.object(CreateMailBoxesTask, 'do', side_effect=None), \
                patch.object(CreateMailCollectorsTask, 'do', side_effect=None), \
                patch.object(WaitingForMigrationsTask, 'do', side_effect=None), \
                patch.object(PassportApiClient, 'validate_login', side_effect=LoginNotavailable), \
                patch.object(PassportApiClient, 'validate_password', side_effect=None), \
                patch.object(CreateMailBoxesTask, 'rollback', side_effect=None):
            response = self.post_form_data(
                '/mail-migration/',
                data={
                    'migration_file': migration_file,
                    'host': 'testhost',
                    'port': 933,
                    'protocol': 'imap',
                    'ssl': False,
                    'no_delete_msgs': False,
                    'sync_abook': False,
                },
                expected_code=200,
            )
        assert_that(
            response,
            has_entries(
                mail_migration_id=not_none()
            ),
        )
        mail_migration_id = response['mail_migration_id']

        mf = MailMigrationFileModel(self.main_connection).filter(org_id=self.organization['id']).one()
        file_path = source_path(
            'intranet/yandex_directory/tests/functional/api/mail_migration/data/failed_passport_validation.csv'
        )
        with open(file_path) as f:
            file_content = f.read()
        assert_that(
            mf,
            has_entries(
                file=file_content,
            ),
        )

        task = TaskModel(self.main_connection).get(mail_migration_id)

        assert_that(
            task,
            has_entries(
                task_name=MailMigrationTask.get_task_name(),
                params=has_entries(
                    migration_file_id=str(mf['id']),
                    host='testhost',
                    port=933,
                    org_id=self.organization['id'],
                    imap=True,
                    ssl=False,
                    no_delete_msgs=False,
                    sync_abook=False,
                    mark_archive_read=True,
                )
            )
        )

    def test_login_not_available_error(self):
        # Загружаем корректный файл, но делаем вид, что такой аккаунт уже есть в паспорте
        # Проверяем, что он записался в базу и что контент совпадает с загруженным файлом
        # Проверяем, что таск MailMigrationTask создался с правильными параметрами
        # (Удаление файла замокано, чтобы можно было проверить контент файла)
        filesdict = FileMultiDict()
        file_path = source_path(
            'intranet/yandex_directory/tests/functional/api/mail_migration/data/failed_passport_validation.csv'
        )
        f = open(file_path, 'rb')
        filesdict.add_file('migration_file', f, filename=file_path)
        migration_file = filesdict.get('migration_file')

        with patch.object(CreateMailBoxesTask, 'do', side_effect=None), \
            patch.object(CreateMailCollectorsTask, 'do', side_effect=None), \
            patch.object(WaitingForMigrationsTask, 'do', side_effect=None), \
            patch.object(PassportApiClient, 'validate_login', side_effect=LoginNotAvailable), \
            patch.object(PassportApiClient, 'validate_password', side_effect=None), \
                patch.object(CreateMailBoxesTask, 'rollback', side_effect=None):
            response = self.post_form_data(
                '/mail-migration/',
                data={
                    'migration_file': migration_file,
                    'host': 'testhost',
                    'port': 933,
                    'protocol': 'pop3',
                    'ssl': False,
                    'no_delete_msgs': False,
                    'sync_abook': False,
                },
                expected_code=200,
            )
        assert_that(
            response,
            has_entries(
                mail_migration_id=not_none()
            ),
        )
        mail_migration_id = response['mail_migration_id']

        mf = MailMigrationFileModel(self.main_connection).filter(org_id=self.organization['id']).one()
        file_path = source_path(
            'intranet/yandex_directory/tests/functional/api/mail_migration/data/failed_passport_validation.csv'
        )
        with open(file_path) as f:
            file_content = f.read()
        assert_that(
            mf,
            has_entries(
                file=file_content,
            ),
        )

        task = TaskModel(self.main_connection).get(mail_migration_id)

        assert_that(
            task,
            has_entries(
                task_name=MailMigrationTask.get_task_name(),
                params=has_entries(
                    migration_file_id=str(mf['id']),
                    host='testhost',
                    port=933,
                    org_id=self.organization['id'],
                    imap=False,
                    ssl=False,
                    no_delete_msgs=False,
                    sync_abook=False,
                    mark_archive_read=True,
                )
            )
        )

    def test_duplicate_login(self):
        filesdict = FileMultiDict()
        file_path = source_path(
            'intranet/yandex_directory/tests/functional/api/mail_migration/data/duplicate_login.csv'
        )
        f = open(file_path, 'rb')
        filesdict.add_file('migration_file', f, filename=file_path)
        migration_file = filesdict.get('migration_file')

        with patch.object(PassportApiClient, 'validate_login', side_effect=None), \
            patch.object(PassportApiClient, 'validate_password', side_effect=None):
            response = self.post_form_data(
                '/mail-migration/',
                data={
                    'migration_file': migration_file,
                    'host': 'testhost',
                    'port': 933,
                },
                expected_code=422,
            )
        duplicate_logins = ['testtestov1', 'testtestov3']
        assert_that(
            response,
            has_entries(
                code='duplicate_login',
                params=has_entries(
                    duplicate_logins=contains_inanyorder(*duplicate_logins)
                )
            ),
        )
        mf = MailMigrationFileModel(self.main_connection).filter(org_id=self.organization['id']).one()
        assert_that(
            mf,
            equal_to(None),
        )


class TestMailMigrationView__get(TestCase):
    autoprocess_tasks = False

    def setUp(self):
        super(TestMailMigrationView__get, self).setUp()
        self.mailboxes_task_id = TaskModel(self.main_connection).create(
            task_name=CreateMailBoxesTask.get_task_name(),
            params={},
            queue='default',
            ttl=None,
        )['id']
        self.collectors_task_id = TaskModel(self.main_connection).create(
            task_name=CreateMailCollectorsTask.get_task_name(),
            params={},
            queue='default',
            ttl=None,
        )['id']
        waiting_for_migration_task_id_1 = TaskModel(self.main_connection).create(
            task_name=CreateMailCollectorTask.get_task_name(),
            params={},
            queue='default',
            ttl=None,
            metadata={
                'folders': [
                    {
                        'messages': '10',
                        'collected': '4',
                        'errors': '1',
                    },
                    {
                        'messages': '10',
                        'collected': '5',
                        'errors': '0',
                    },
                ]
            }
        )['id']
        waiting_for_migration_task_id_2 = TaskModel(self.main_connection).create(
            task_name=CreateMailCollectorTask.get_task_name(),
            params={},
            queue='default',
            ttl=None,
            metadata={
                'folders': [
                    {
                        'messages': '10',
                        'collected': '0',
                        'errors': '5',
                    },
                    {
                        'messages': '10',
                        'collected': '5',
                        'errors': '0',
                    },
                ]
            },
        )['id']
        waiting_for_migrations_task_id = TaskModel(self.main_connection).create(
            task_name=WaitingForMigrationsTask.get_task_name(),
            params={},
            queue='default',
            ttl=None,
            depends_on=[waiting_for_migration_task_id_1, waiting_for_migration_task_id_2]
        )['id']
        self.migrations_task_id = TaskModel(self.main_connection).create(
            task_name=MailMigrationTask.get_task_name(),
            params={'org_id': self.organization['id']},
            queue='default',
            ttl=None,
            depends_on=[
                self.mailboxes_task_id,
                self.collectors_task_id,
                waiting_for_migrations_task_id,
            ]
        )['id']
        self.migration_url = '/mail-migration/?mail_migration_id={}'.format(self.migrations_task_id)

    def test_check(self):
        # миграция в процессе работы
        result = self.get_json(self.migration_url)
        assert_that(
            result,
            equal_to(
                [
                    {
                        'stage': 'accounts-creating',
                        'state': MAIL_MIGRATION_STATES.pending,
                    },
                    {
                        'stage': 'collectors-creating',
                        'state': MAIL_MIGRATION_STATES.pending,
                    },
                    # {
                    #     'stage': 'mail-collecting',
                    #     'state': 'in-progress',
                    #     'progress': 50,
                    # },
                ],
            )
        )

    def test_check_not_found(self):
        # запрашиваем несуществубщий таск
        unknown_task_id = uuid.uuid4()
        self.get_json(
            "/mail-migration/?mail_migration_id={}".format(unknown_task_id),
            expected_code=404,
        )

    def test_check_not_uuid(self):
        # передаём не uid
        self.get_json(
            "/mail-migration/?mail_migration_id={}".format('NOTUID'),
            expected_code=404,
        )

    def test_get_migration_progress_by_org_id(self):
        # Не передаем mail_migration_id
        # Таск на миграцию находится по org_id
        TaskModel(self.main_connection).set_state(self.mailboxes_task_id, 'success')
        TaskModel(self.main_connection).set_state(self.collectors_task_id, 'failed')
        result = self.get_json("/mail-migration/")
        assert_that(
            result,
            equal_to(
                [
                    {
                        'stage': 'accounts-creating',
                        'state': 'success',
                    },
                    {
                        'stage': 'collectors-creating',
                        'state': 'failed',
                    },
                ],
            )
        )
