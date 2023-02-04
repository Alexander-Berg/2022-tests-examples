from copy import deepcopy

from testutils import TestCase, override_settings, assert_not_called, fake_userinfo, create_organization_without_domain

from intranet.yandex_directory.src.yandex_directory.core.models.organization import OrganizationSsoSettingsModel
from intranet.yandex_directory.src.yandex_directory.core.models.domain import DomainModel
from intranet.yandex_directory.src.yandex_directory.core.models.task import TaskModel
from intranet.yandex_directory.src.yandex_directory.core.task_queue.base import TASK_STATES
from intranet.yandex_directory.src.yandex_directory.common.utils import Ignore
from intranet.yandex_directory.src.yandex_directory.core.utils import is_domain_uid


class TestSyncSsoOrgsTask(TestCase):
    def test_simple(self):
        from intranet.yandex_directory.src.yandex_directory.sso.tasks import SyncSsoOrgsTask, SyncSsoOrgTask

        self.create_organization()
        orgs = []
        for i in range(1, 10):
            org_id = self.create_organization()['id']
            OrganizationSsoSettingsModel(self.main_connection).insert_or_update(org_id, True, True)
            orgs.append(org_id)

        with override_settings(SYNC_SSO_ORGS_TASK_CHUNK_SIZE=3):
            task = SyncSsoOrgsTask(self.main_connection).delay()
            task.tries_delay = 0

            self.process_task(task.task_id)
            assert TaskModel(self.main_connection).get(task.task_id)['state'] == TASK_STATES.success

        tasks = TaskModel(self.main_connection).filter(task_name=SyncSsoOrgTask.get_task_name())

        task_orgs = [x['params']['org_id'] for x in tasks]
        assert sorted(task_orgs) == sorted(orgs)


class TestSyncSsoOrgTask(TestCase):
    create_organization = False

    def setUp(self):
        super(TestSyncSsoOrgTask, self).setUp()
        self.organization = create_organization_without_domain(self.meta_connection, self.main_connection)['organization']
        OrganizationSsoSettingsModel(self.main_connection).insert_or_update(self.organization['id'], True, True)
        DomainModel(self.main_connection).create('master-domain.ru', self.organization['id'], owned=True, master=True)

    def test_not_sso(self):
        users = {
            1130000000000001: {},
            1130000000000002: {},
            1130000000000003: {},
        }
        OrganizationSsoSettingsModel(self.main_connection).update(
            filter_data={'org_id': self.organization['id']},
            update_data={'enabled': False},
        )
        self._prepare_uids(users, users)
        self._run_task()
        assert_not_called(self.mocked_blackbox.account_uids)

        self._check_uid(1130000000000001)
        self._check_uid(1130000000000002)
        self._check_uid(1130000000000003)

    def test_no_master_domain(self):
        from intranet.yandex_directory.src.yandex_directory.core.models.domain import DomainModel

        users = {
            1130000000000001: {},
            1130000000000002: {},
            1130000000000003: {},
        }
        DomainModel(self.main_connection).delete(filter_data={'org_id': self.organization['id']}, force_remove_all=True)

        self._prepare_uids(users, users)
        self._run_task(expected_failed=True)
        assert_not_called(self.mocked_blackbox.account_uids)

        self._check_uid(1130000000000001)
        self._check_uid(1130000000000002)
        self._check_uid(1130000000000003)

    def test_with_slave_domains(self):
        from intranet.yandex_directory.src.yandex_directory.core.models.domain import DomainModel

        users = {
            1130000000000001: {},
            1130000000000002: {},
            1130000000000003: {},
        }
        DomainModel(self.main_connection).create('slave-domain-1.ru', self.organization['id'], owned=True, master=False)
        DomainModel(self.main_connection).create('slave-domain-2.ru', self.organization['id'], owned=True, master=False)

        self._prepare_uids(users, users)
        self._run_task()

        self._check_uid(1130000000000001)
        self._check_uid(1130000000000002)
        self._check_uid(1130000000000003)

    def test_all_correct(self):
        users = {
            1130000000000001: {},
            1130000000000002: {},
            1130000000000003: {},
        }
        self._prepare_uids(users, users)
        self._run_task()

        self._check_uid(1130000000000001)
        self._check_uid(1130000000000002)
        self._check_uid(1130000000000003)

    def test_no_dismiss_portal_user(self):
        users = {
            1130000000000001: {},
            1130000000000002: {},
            1130000000000003: {},
            5555555555: {},
        }
        self._prepare_uids(users, users)
        self._run_task()

        self._check_uid(1130000000000001)
        self._check_uid(1130000000000002)
        self._check_uid(1130000000000003)
        self._check_uid(5555555555, first_name='_user', last_name='_user')

    def test_dismiss_domain_user_if_not_exists_in_blackbox(self):
        self._prepare_uids(
            {
                1130000000000001: {},
                1130000000000002: {},
                1130000000000003: {},
            },
            {
                1130000000000001: {},
                1130000000000002: {},
            }
        )
        self._run_task()

        self._check_uid(1130000000000001)
        self._check_uid(1130000000000002)
        self._check_uid(1130000000000003, is_dismissed=True)

    def test_create_new_domain_user(self):
        self._prepare_uids(
            {
                1130000000000001: {},
                1130000000000002: {},
            },
            {
                1130000000000001: {},
                1130000000000002: {},
                1130000000000003: {},
            }
        )
        self._run_task()

        self._check_uid(1130000000000001)
        self._check_uid(1130000000000002)
        self._check_uid(1130000000000003)

    def test_not_create_new_domain_user_if_maillist(self):
        self._prepare_uids(
            {
                1130000000000001: {},
                1130000000000002: {},
            },
            {
                1130000000000001: {},
                1130000000000002: {},
                1130000000000003: {'is_maillist': True},
            }
        )
        self._run_task()

        self._check_uid(1130000000000001)
        self._check_uid(1130000000000002)
        self._check_uid(1130000000000003, not_exist=True)

    def test_many_dismiss_one_user(self):
        self._prepare_uids({}, {1130000000000001: {}})
        self._run_task()
        self._check_uid(1130000000000001)

        self._prepare_uids({}, {})
        self._run_task()
        self._check_uid(1130000000000001, is_dismissed=True)

        self._prepare_uids({}, {1130000000000001: {}})
        self._run_task()
        self._check_uid(1130000000000001)

        self._prepare_uids({}, {})
        self._run_task()
        self._check_uid(1130000000000001, is_dismissed=True)

    def test_check_sync_firstname(self):
        users = {
            1130000000000001: {},
            1130000000000002: {},
            1130000000000003: {},
        }
        blackbox_users = deepcopy(users)
        blackbox_users[1130000000000001]['first_name'] = 'new firstname'
        self._prepare_uids(users, blackbox_users)
        self._run_task()

        self._check_uid(1130000000000001, first_name='new firstname')
        self._check_uid(1130000000000002)
        self._check_uid(1130000000000003)

    def _check_uid(self, uid, **kwargs):
        from intranet.yandex_directory.src.yandex_directory.core.models.user import UserModel, UserMetaModel

        user = UserModel(self.main_connection).filter(id=uid, is_dismissed=Ignore).one()
        meta_user = UserMetaModel(self.meta_connection).filter(id=uid).one()

        if kwargs.get('not_exist', False):
            assert user is None
            assert meta_user is None
            return

        assert user['id'] == uid
        assert user['org_id'] == self.organization['id']
        assert user['is_dismissed'] == kwargs.get('is_dismissed', False)
        assert user['is_sso'] == True
        assert user['nickname'] == str(uid)[10:] + '_user'
        assert user['email'] == str(uid)[10:] + '_user@master-domain.ru'
        assert user['first_name'] == kwargs.get('first_name', 'user_first_name')
        assert user['last_name'] == kwargs.get('last_name', 'user_last_name')

        assert meta_user['id'] == uid
        assert meta_user['org_id'] == self.organization['id']
        assert meta_user['is_dismissed'] == kwargs.get('is_dismissed', False)

    def _prepare_uids(self, connect_users, blackbox_users):
        for uid, user in connect_users.items():
            self.create_user(
                uid=uid,
                is_sso=True,
                nickname=str(uid)[10:] + '_user',
                email=str(uid)[10:] + '_user@master-domain.ru',
                name={
                    'first': 'user_first_name',
                    'last': 'user_last_name',
                },
                is_dismissed=user.get('is_dismissed', False)
            )

        self.mocked_blackbox.account_uids.return_value = blackbox_users.keys()

        userinfos = [
            fake_userinfo(
                uid=uid,
                default_email=f'{str(uid)[10:]}_user@master-domain.ru',
                login=f'{str(uid)[10:]}_user@master-domain.ru',
                first_name=user.get('first_name', 'user_first_name'),
                last_name='user_last_name',
                sex='1',
                birth_date=None,
                country='ru',
                is_available=not user.get('is_dismissed', False),
                is_maillist=user.get('is_maillist', False),
            ) for uid, user in blackbox_users.items() if is_domain_uid(uid)
        ]

        def _find_userinfo(uid, **kwargs):
            return next(u for u in userinfos if u['uid'] == str(uid))
        def _get_userinfos(*args, **kwargs):
            return userinfos

        self.mocked_blackbox.batch_userinfo.side_effect = _get_userinfos

        self.mocked_blackbox.userinfo.side_effect = _find_userinfo

    def _run_task(self, expected_failed=False):
        from intranet.yandex_directory.src.yandex_directory.sso.tasks import SyncSsoOrgTask

        task_id = SyncSsoOrgTask(self.main_connection).delay(org_id=self.organization['id']).task_id
        self.process_task(task_id)

        if expected_failed:
            assert TaskModel(self.main_connection).get(task_id)['state'] == TASK_STATES.failed
        else:
            assert TaskModel(self.main_connection).get(task_id)['state'] == TASK_STATES.success
            if self.mocked_blackbox.call_count > 0:
                assert self.mocked_blackbox.account_uids.call_args[1] == {'domain': 'master-domain.ru'}
