# -*- coding: utf-8 -*-
from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
    contains,
)
from unittest.mock import patch

from testutils import (
    TestCase,
    create_outer_uid
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    UserModel,
    UserMetaModel,
    ActionModel,
)
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.core.resource.tasks import (
    CreateExistingAccountTask,
)
from intranet.yandex_directory.src.yandex_directory.core.tasks.resource import (
    CreateResourceTask,
)
from intranet.yandex_directory.src.yandex_directory.core.task_queue.base import (
    TASK_STATES,
)
from intranet.yandex_directory.src.yandex_directory.connect_services.idm import get_service
from intranet.yandex_directory.src.yandex_directory.connect_services.idm.exceptions import ClientIDBoundError


class TestCreateExistingAccountTask(TestCase):

    def setUp(self):
        super(TestCreateExistingAccountTask, self).setUp()
        self.uid = create_outer_uid()
        self.mocked_blackbox.userinfo.return_value = {
            'fields': {
                'country': 'ru',
                'login': 'art',
                'firstname': 'Александр',
                'lastname': 'Артеменко',
                'sex': '1',
                'birth_date': '1980-10-05',
            },
            'uid': self.uid,
            'default_email': 'default@ya.ru',
        }
        self.mocked_blackbox.reset_mock()

    def test_create_portal_account(self):
        # Проверяем, что такс выполнился успешно, что создались пользователь и событие
        org_id = self.organization['id']
        CreateExistingAccountTask(self.main_connection).delay(
            uid=self.uid,
            org_id=org_id,
        )
        self.process_tasks()

        self.assert_no_failed_tasks(
            allowed_states=[
                'success',
                # CreateExistingAccountTask заводит эти таски в процессе своей работы
                ('UpdateMembersCountTask', 'free'),
                ('UpdateOrganizationMembersCountTask', 'free'),
                ('SyncExternalIDS', 'free'),
            ],
        )

        meta_user = UserMetaModel(self.meta_connection).get(user_id=self.uid, org_id=org_id)
        assert meta_user is not None
        main_user = UserModel(self.main_connection).get(self.uid, org_id)
        assert main_user is not None

        actions = ActionModel(self.main_connection)\
            .filter(org_id=org_id)\
            .fields('name')\
            .all()
        assert_that(
            actions,
            contains(
                has_entries(name='user_add'),
            )
        )

    def test_create_account_error(self):
        # Проверяем, что если в create_portal_user произошла ошибка,
        # таск завершится с ошибкой, пользователь и событие не создадуться
        org_id = self.organization['id']
        with patch('intranet.yandex_directory.src.yandex_directory.core.resource.tasks.create_portal_user', side_effect=Exception):
            task = CreateExistingAccountTask(self.main_connection).delay(
                uid=10,
                org_id=self.organization['id'],
            )
            self.process_tasks()
            assert_that(
                task.state,
                equal_to(TASK_STATES.failed),
            )
            meta_user = UserMetaModel(self.meta_connection).get(user_id=self.uid, org_id=org_id)
            assert meta_user is None
            main_user = UserModel(self.main_connection).get(self.uid, org_id)
            assert main_user is None

            actions_count = ActionModel(self.main_connection)\
                .filter(org_id=org_id)\
                .fields('name')\
                .count()
            assert actions_count == 0


class TestCreateResourceTask(TestCase):
    def test_duplicate_relations_should_be_ignored(self):
        # Проверяем, что таск выполнится успешно, даже если ему указано две одинаковых связи.
        # Это нужно, чтобы виджет связывания не пятисотил, когда случайно указаны
        # два одинаковых uid:
        # https://st.yandex-team.ru/DIR-6827
        org_id = self.organization['id']
        uid = self.user['id']

        CreateResourceTask(self.main_connection).delay(
            org_id=org_id,
            service_slug=self.service['slug'],
            external_id='42',
            relations=[
                {'object_type': 'user', 'object_id': uid, 'name': 'admin'},
                {'object_type': 'user', 'object_id': uid, 'name': 'admin'},
            ],
        )
        self.process_tasks()
        self.assert_no_failed_tasks()


class TestRequestDirectRole(TestCase):
    def setUp(self):
        super(TestRequestDirectRole, self).setUp()
        self.service = get_service('direct')

    def test_should_fail_if_association_exists(self):
        with patch.object(app.billing_client, 'get_passport_by_uid') as mocked_get_passport:
            mocked_get_passport.return_value = {'ClientId': 'some_id'}
            with self.assertRaises(ClientIDBoundError):
                self.service.request_roles(
                    self.organization['id'],
                    self.user['id'],
                    *[{
                        'path': 'smth',
                        'resource_id': 'resource_id_1',
                        'uid': self.user['id'],
                    }]
                )
