from unittest.mock import patch

from testutils import (
    fake_userinfo,
    TestCase,
)

from intranet.yandex_directory.src.yandex_directory.core.cloud.utils import sync_cloud_org
from intranet.yandex_directory.src.yandex_directory.core.models import OrganizationMetaModel, ServiceModel
from intranet.yandex_directory.src.yandex_directory.core.cloud.tasks import (
    SyncCloudOrgsTask,
    SyncCloudOrgTask,
    SendSyncCloudOrgTasksForBatch,
)
from intranet.yandex_directory.src.yandex_directory.core.models import UserModel, UserMetaModel, TaskModel
from intranet.yandex_directory.src.yandex_directory.common.utils import Ignore
from intranet.yandex_directory.src.yandex_directory.core.task_queue.base import TASK_STATES
from intranet.yandex_directory.src.yandex_directory.core.models.service import enable_service


class TestSyncCloudOrg(TestCase):
    def test_dismiss_user(self):
        org = self.create_organization(organization_type='cloud', preset='cloud')
        OrganizationMetaModel(self.meta_connection).update(
            update_data={'cloud_org_id': 'cloud_1'},
            filter_data={'id': org['id']},
        )
        user1 = self.create_user(org_id=org['id'], uid=9000000000000001)
        user2 = self.create_user(org_id=org['id'], uid=9000000000000002)
        user3 = self.create_user(org_id=org['id'], uid=9000000000000003)
        user4 = self.create_user(org_id=org['id'], uid=3434343)
        user5 = self.create_user(org_id=org['id'], uid=1130000000000001)

        with patch('intranet.yandex_directory.src.yandex_directory.connect_services.cloud.grpc'
                   '.client.GrpcCloudClient.list_users_by_organization_id') as list_users, \
            patch('intranet.yandex_directory.src.yandex_directory.app.cloud_blackbox_instance') \
                as mock_cloud_blackbox_instance, \
            patch('intranet.yandex_directory.src.yandex_directory.app.blackbox_instance') \
                as mock_blackbox_instance:
            list_users_response = self.get_dict_object({
                'next_page_token': None,
                'users': [
                    {
                        'subject_claims': {
                            'yandex_claims': {
                                'passport_uid': None,
                            },
                            'federation': {
                                'id': 'id-1',
                            },
                            'sub': 'sub-1',
                            'preferred_username': 'username1',
                            'email': 'email1@mail.ru',
                            'given_name': 'given_name1',
                            'family_name': 'family_name1',
                        }
                    },
                    {
                        'subject_claims': {
                            'yandex_claims': {
                                'passport_uid': None,
                            },
                            'federation': {
                                'id': 'id-1',
                            },
                            'sub': 'sub-2',
                            'preferred_username': 'username2',
                            'email': 'email2@domain.com',
                            'given_name': 'given_name2',
                            'family_name': 'family_name2',
                        }
                    },
                ]
            })
            list_users.return_value = list_users_response

            mock_cloud_blackbox_instance.userinfo.return_value = [
                {'uid': user1['id'], 'attributes': {'193': 'sub-1'}},
                {'uid': user2['id'], 'attributes': {'193': 'sub-2'}},
            ]
            mock_blackbox_instance.userinfo.return_value = fake_userinfo(uid=user3['id'])

            sync_cloud_org(org_id=org['id'])

            # облачный федеративный пользователь будет уволен
            user3_meta = UserMetaModel(self.meta_connection).get(user_id=user3['id'], org_id=org['id'], is_outer=Ignore)
            self.assertEqual(user3_meta['is_dismissed'], True)
            user_3_main = UserModel(self.main_connection).get(
                user_id=user3['id'], org_id=org['id'], is_dismissed=Ignore,
            )
            self.assertTrue(user_3_main['is_dismissed'])

            # портальный паспортный пользователь будет уволен
            user4_meta = UserMetaModel(self.meta_connection).get(user_id=user4['id'], org_id=org['id'], is_outer=Ignore)
            self.assertTrue(user4_meta['is_dismissed'])

            user_4_main = UserModel(self.main_connection).get(
                user_id=user3['id'], org_id=org['id'], is_dismissed=Ignore,
            )
            self.assertTrue(user_4_main['is_dismissed'])

            # доменный пользователь не будет уволен
            user5_meta = UserMetaModel(self.meta_connection).get(user_id=user5['id'], org_id=org['id'], is_outer=Ignore)
            self.assertFalse(user5_meta['is_dismissed'])

            user_5_main = UserModel(self.main_connection).get(
                user_id=user5['id'], org_id=org['id'], is_dismissed=Ignore,
            )
            self.assertFalse(user_5_main['is_dismissed'])

    def test_change_user_data(self):
        org = self.create_organization(organization_type='cloud', preset='cloud')
        OrganizationMetaModel(self.meta_connection).update(
            update_data={'cloud_org_id': 'cloud_1'},
            filter_data={'id': org['id']},
        )
        user = self.create_user(
            org_id=org['id'],
            uid=9000000000000001,
            nickname='nickname',
        )
        new_nickname = 'change_nickname'
        with patch('intranet.yandex_directory.src.yandex_directory.connect_services.cloud.grpc'
                   '.client.GrpcCloudClient.list_users_by_organization_id') as list_users, \
            patch('intranet.yandex_directory.src.yandex_directory.app.blackbox_instance') \
                as mock_blackbox_instance:
            list_users_response = self.get_dict_object({
                'next_page_token': None,
                'users': [
                    {
                        'subject_claims': {
                            'yandex_claims': {
                                'passport_uid': user['id'],
                            },
                            'federation': {
                                'id': 'id-1',
                            },
                            'sub': 'sub-1',
                            'preferred_username': new_nickname,
                            'email': 'email1@domain.com',
                            'given_name': 'given_name1',
                            'family_name': 'family_name1',
                        }
                    },
                ]
            })
            list_users.return_value = list_users_response
            mock_blackbox_instance.userinfo.return_value = fake_userinfo(
                uid=user['id'],
                login=new_nickname,
                cloud_uid='sub-1',
            )
            sync_cloud_org(org_id=org['id'])

        user = UserModel(self.main_connection).get(user_id=user['id'], org_id=org['id'])
        self.assertEqual(user['nickname'], new_nickname)

    def test_add_user(self):
        org = self.create_organization(organization_type='cloud', preset='cloud')
        OrganizationMetaModel(self.meta_connection).update(
            update_data={'cloud_org_id': 'cloud_1'},
            filter_data={'id': org['id']},
        )
        uid_to_add = 9000000000000001

        with patch('intranet.yandex_directory.src.yandex_directory.connect_services.cloud.grpc'
                   '.client.GrpcCloudClient.list_users_by_organization_id') as list_users_by_organization_id, \
            patch('intranet.yandex_directory.src.yandex_directory.app.cloud_blackbox_instance') \
                as mock_cloud_blackbox_instance, \
            patch('intranet.yandex_directory.src.yandex_directory.app.blackbox_instance') \
                as mock_blackbox_instance:
            list_users_response = self.get_dict_object({
                'next_page_token': None,
                'users': [
                    {
                        'subject_claims': {
                            'yandex_claims': {
                                'passport_uid': None,
                            },
                            'federation': {
                                'id': 'id-1',
                            },
                            'sub': 'sub-1',
                            'preferred_username': 'username1',
                            'email': 'email1@mail.ru',
                            'given_name': 'given_name1',
                            'family_name': 'family_name1',
                        }
                    },
                ]
            })
            list_users_by_organization_id.return_value = list_users_response

            mock_cloud_blackbox_instance.userinfo.return_value = [
                {'uid': uid_to_add, 'attributes': {'193': 'sub-1'}},
            ]
            mock_blackbox_instance.userinfo.return_value = fake_userinfo(uid=uid_to_add, cloud_uid='sub-1')

            sync_cloud_org(org_id=org['id'])

            user_meta = UserMetaModel(self.meta_connection).get(user_id=uid_to_add, org_id=org['id'])
            self.assertEqual(user_meta['is_dismissed'], False)

            user = UserModel(self.main_connection).get(user_id=uid_to_add, org_id=org['id'], is_dismissed=False)
            self.assertIsNotNone(user)
            self.assertEqual(user['cloud_uid'], 'sub-1')

    def test_complex(self):
        org = self.create_organization(organization_type='cloud', preset='cloud')
        OrganizationMetaModel(self.meta_connection).update(
            update_data={'cloud_org_id': 'cloud_1'},
            filter_data={'id': org['id']},
        )
        user1 = self.create_user(org_id=org['id'], uid=9000000000000001)  # уволить
        user2 = self.create_user(org_id=org['id'], uid=9000000000000002)  # оставить
        user3_uid = 9000000000000003  # добавить

        with patch('intranet.yandex_directory.src.yandex_directory.connect_services.cloud.grpc'
                   '.client.GrpcCloudClient.list_users_by_organization_id') as list_users, \
            patch('intranet.yandex_directory.src.yandex_directory.app.cloud_blackbox_instance') \
                as mock_cloud_blackbox_instance, \
            patch('intranet.yandex_directory.src.yandex_directory.app.blackbox_instance') \
                as mock_blackbox_instance:

            list_users_response = self.get_dict_object({
                'next_page_token': None,
                'users': [
                    {
                        'subject_claims': {
                            'yandex_claims': {
                                'passport_uid': None,
                            },
                            'federation': {
                                'id': 'id-1',
                            },
                            'sub': 'sub-2',
                            'preferred_username': 'username2',
                            'email': 'email2@domain.com',
                            'given_name': 'given_name2',
                            'family_name': 'family_name2',
                        }
                    },
                    {
                        'subject_claims': {
                            'yandex_claims': {
                                'passport_uid': None,
                            },
                            'federation': {
                                'id': 'id-1',
                            },
                            'sub': 'sub-3',
                            'preferred_username': 'username3',
                            'email': 'email3@domain.com',
                            'given_name': 'given_name3',
                            'family_name': 'family_name3',
                        }
                    },
                ]
            })
            list_users.return_value = list_users_response

            mock_cloud_blackbox_instance.userinfo.return_value = [
                {'uid': user2['id'], 'attributes': {'193': 'sub-2'}},
                {'uid': user3_uid, 'attributes': {'193': 'sub-3'}},
            ]
            mock_blackbox_instance.userinfo.return_value = fake_userinfo(uid=user3_uid)

            sync_cloud_org(org_id=org['id'])

            user1_meta = UserMetaModel(self.meta_connection).get(user_id=user1['id'], org_id=org['id'])
            self.assertEqual(user1_meta['is_dismissed'], True)
            user1_model = UserModel(self.main_connection).get(user_id=user1['id'], org_id=org['id'], is_dismissed=True)
            self.assertIsNotNone(user1_model)

            user2_meta = UserMetaModel(self.meta_connection).get(user_id=user2['id'], org_id=org['id'])
            self.assertEqual(user2_meta['is_dismissed'], False)
            self.assertEqual(user2_meta['cloud_uid'], 'sub-2')
            user2_model = UserModel(self.main_connection).get(user_id=user2['id'], org_id=org['id'], is_dismissed=False)
            self.assertIsNotNone(user2_model)

            user3_meta = UserMetaModel(self.meta_connection).get(user_id=user3_uid, org_id=org['id'])
            self.assertEqual(user3_meta['is_dismissed'], False)
            user3_model = UserModel(self.main_connection).get(user_id=user3_uid, org_id=org['id'], is_dismissed=False)
            self.assertIsNotNone(user3_model)

    def test_save_display_name_if_first_name_not_exists(self):
        org = self.create_organization(organization_type='cloud', preset='cloud')
        OrganizationMetaModel(self.meta_connection).update(
            update_data={'cloud_org_id': 'cloud_1'},
            filter_data={'id': org['id']},
        )
        uid_to_add = 9000000000000001

        with patch('intranet.yandex_directory.src.yandex_directory.connect_services.cloud.grpc'
                   '.client.GrpcCloudClient.list_users_by_organization_id') as list_users, \
            patch('intranet.yandex_directory.src.yandex_directory.app.cloud_blackbox_instance') \
                as mock_cloud_blackbox_instance, \
            patch('intranet.yandex_directory.src.yandex_directory.app.blackbox_instance') \
                as mock_blackbox_instance:
            list_users_response = self.get_dict_object({
                'next_page_token': None,
                'users': [
                    {
                        'subject_claims': {
                            'yandex_claims': {
                                'passport_uid': None,
                            },
                            'federation': {
                                'id': 'id-1',
                            },
                            'sub': 'abad1dea',
                            'preferred_username': 'display_name_1',
                            'email': 'email1@domain.com',
                            'given_name': '',
                            'family_name': '',
                            'name': 'okay',
                        }
                    },
                ]
            })
            list_users.return_value = list_users_response

            mock_cloud_blackbox_instance.userinfo.return_value = [
                {'uid': uid_to_add, 'attributes': {'193': 'abad1dea'}},
            ]
            mock_blackbox_instance.userinfo.return_value = fake_userinfo(
                uid=uid_to_add,
                cloud_uid='abad1dea',
                first_name=None,
                last_name=None,
                cloud_display_name='display_name_1',
            )

            sync_cloud_org(org_id=org['id'])

            user_meta = UserMetaModel(self.meta_connection).get(user_id=uid_to_add, org_id=org['id'])
            self.assertEqual(user_meta['is_dismissed'], False)

            user = UserModel(self.main_connection).get(user_id=uid_to_add, org_id=org['id'], is_dismissed=False)
            self.assertIsNotNone(user)
            self.assertEqual(user['cloud_uid'], 'abad1dea')
            self.assertEqual(user['name']['first'], 'okay')
            self.assertEqual(user['name']['last'], '')


class TestSyncCloudOrgs(TestCase):
    def test_simple(self):
        task_model = TaskModel(self.main_connection)

        org_id = self.create_organization(organization_type='cloud', preset='cloud')['id']
        ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='wiki',
            name='wiki',
            paid_by_license=True,
            ready_default=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            org_id,
            'wiki',
        )
        task = SyncCloudOrgsTask(self.main_connection).delay()
        self.process_task(task.task_id)

        task = task_model.get(task.task_id)
        assert task['state'] == TASK_STATES.success

        batch_tasks = task_model.find({'task_name': SendSyncCloudOrgTasksForBatch.get_task_name(), 'state': TASK_STATES.free})
        assert len(batch_tasks) == 1
        batch_task_id = batch_tasks[0]['id']
        self.process_task(batch_task_id)

        batch_task = task_model.get(batch_task_id)
        assert batch_task['state'] == TASK_STATES.success

        sync_tasks = task_model.find({'task_name': SyncCloudOrgTask.get_task_name(), 'state': TASK_STATES.free})
        assert len(sync_tasks) == 1
        assert sync_tasks[0]['params']['org_id'] == org_id

    def test_wo_service(self):
        task_model = TaskModel(self.main_connection)
        ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='wiki',
            name='wiki',
            paid_by_license=True,
            ready_default=True,
        )
        self.create_organization(organization_type='cloud', preset='cloud')

        task = SyncCloudOrgsTask(self.main_connection).delay()
        self.process_task(task.task_id)

        task = task_model.get(task.task_id)
        assert task['state'] == TASK_STATES.success

        sync_tasks = task_model.find({'task_name': SendSyncCloudOrgTasksForBatch.get_task_name(), 'state': TASK_STATES.free})
        assert len(sync_tasks) == 0


    def test_many_orgs(self):
        task_model = TaskModel(self.main_connection)
        ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            paid_by_license=True,
            ready_default=True,
        )
        org_ids = [self.create_organization(organization_type='cloud', preset='cloud')['id'] for _ in range(25)]
        for org_id in org_ids:
            enable_service(
                self.meta_connection,
                self.main_connection,
                org_id,
                'tracker',
            )
        # создадим еще одну организацию - без включенного сервиса
        self.create_organization(organization_type='cloud', preset='cloud')

        task_id = SyncCloudOrgsTask(self.main_connection).delay().task_id

        self.process_task(task_id)
        task = task_model.get(task_id)
        assert task['state'] == TASK_STATES.success

        batch_tasks = task_model.find({'task_name': SendSyncCloudOrgTasksForBatch.get_task_name(), 'state': TASK_STATES.free})
        assert len(batch_tasks) == 3  # по умолчанию 10 размер батча
        for task in batch_tasks:
            self.process_task(task['id'])
            batch_task = task_model.get(task['id'])
            assert batch_task['state'] == TASK_STATES.success

        sync_tasks = task_model.find({'task_name': SyncCloudOrgTask.get_task_name(), 'state': TASK_STATES.free})
        assert len(sync_tasks) == 25
        assert {x['params']['org_id'] for x in sync_tasks} == set(org_ids)
