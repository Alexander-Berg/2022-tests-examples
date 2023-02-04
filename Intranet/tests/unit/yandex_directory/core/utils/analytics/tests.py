# -*- coding: utf-8 -*-
from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
    contains,
    contains_inanyorder,
)
from unittest.mock import Mock, patch, ANY
import unittest.mock

from testutils import (
    TestCase,
    override_settings,
    create_organization,
)
from intranet.yandex_directory.src.yandex_directory.common.step.client import STEPApiClient
from intranet.yandex_directory.src.yandex_directory.common.utils import (
    utcnow,
    Ignore,
    format_date,
)
from intranet.yandex_directory.src.yandex_directory.core.commands.save_analytics import Command as SaveAnalyticsCommand
from intranet.yandex_directory.src.yandex_directory.core.commands.update_services_in_shards import Command as UpdateServicesInShards
from intranet.yandex_directory.src.yandex_directory.core.features import get_enabled_organizations_features
from intranet.yandex_directory.src.yandex_directory.core.models import (
    UserModel,
    UserMetaModel,
    OrganizationModel,
    DomainModel,
    OrganizationServiceModel,
    DomainsAnalyticsInfoModel,
    OrganizationServicesAnalyticsInfoModel,
    OrganizationsAnalyticsInfoModel,
    UsersAnalyticsInfoModel,
    TaskModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    ServiceModel,
    enable_service,
    disable_service,
)
from intranet.yandex_directory.src.yandex_directory.core.task_queue import (
    Task,
    TASK_STATES,
)
from intranet.yandex_directory.src.yandex_directory.core.utils.analytics import (
    AnalyticsToYtSaver,
    SendStepEventTask,
)
from intranet.yandex_directory.src.yandex_directory.core.utils.robots import create_robot_for_service_and_org_id
from intranet.yandex_directory.src.yandex_directory.core.utils.tasks import (
    SaveAnalyticsToYTTask,
    SaveAnalyticsLocal,
    SaveAnalyticsMetaTask,
)

YT_TABLE_PREFIX = '//tmp/yandex-connect/tests/tables/'


class Test__get_analytics_table_path(TestCase):
    @override_settings(YT_ANALYTICS_TABLES_PATH=YT_TABLE_PREFIX)
    def test_should_return_table_name_with_path_prefix(self):
        # функция должна вернуть полный путь до таблицы с добавлением сегодняшней даты
        table_name = 'test'
        path = AnalyticsToYtSaver()._get_analytics_table_path(table_name)
        exp_path = '%s%s/%s' % (YT_TABLE_PREFIX, table_name, utcnow().strftime('%Y-%m-%d'))
        assert_that(path, equal_to(exp_path))


class Test_save_analytics_data(TestCase):
    def assert_saved_to_yt(self, table, exp_data):
        saver = AnalyticsToYtSaver()
        save_table = {
            'domains': saver.save_domains_stats_data_to_yt,
            'users':  saver.save_users_stats_data_to_yt,
            'organizations': saver.save_organizations_stats_data_to_yt,
            'org_services': saver.save_org_services_stats_to_yt,
        }
        mocked_yt_utils = Mock()
        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.analytics.yt_utils', mocked_yt_utils):
            with patch('intranet.yandex_directory.src.yandex_directory.core.utils.analytics.yt_utils.is_table_exists', Mock(return_value=False)):
                with patch.object(mocked_yt_utils, 'yt_client'):
                    save_table[table]()

        exp_calls = [
            unittest.mock.call(
                table=saver._get_analytics_table_path(table),
                rows_data=ANY,
            )
        ]

        assert_that(mocked_yt_utils.append_rows_to_table.mock_calls, equal_to(exp_calls))

        exp_schema = [{'name': col_name, 'type': ANY} for col_name in list(exp_data[0].keys())]
        schema = mocked_yt_utils.create_table_if_needed.call_args[0][1]
        assert_that(schema, contains_inanyorder(*exp_schema))

        _, args, kwargs = mocked_yt_utils.append_rows_to_table.mock_calls[0]
        assert_that(
            kwargs['rows_data'],
            contains_inanyorder(*list(map(has_entries, exp_data)))
        )

    def test_should_save_all_domains_data_to_yt(self):
        # проверяем, что save_domains_stats_data_to_yt сохраняет данные всех доменов в YT
        first_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']
        second_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='yandex'
        )['organization']

        for organization in [first_organization, second_organization]:
            for i in range(3):
                DomainModel(self.main_connection).create('domain%s.yandex.ru' % i, organization['id'], owned=True)

        # сохраняем аналитику
        DomainsAnalyticsInfoModel(self.main_connection).save_analytics()

        now_date = utcnow().strftime('%Y-%m-%d')
        exp_data = [
            {'name': d['name'], 'for_date': now_date, 'org_id': d['org_id'], 'master': d['master']}
            for d in DomainModel(self.main_connection).find()
        ]
        self.assert_saved_to_yt('domains', exp_data)

    def test_should_save_all_users_data_to_yt(self):
        # проверяем, что save_users_stats_data_to_yt сохраняет данные пользователей в YT
        first_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']
        second_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='yandex'
        )['organization']
        now_date = utcnow().strftime('%Y-%m-%d')
        for organization in [first_organization, second_organization]:
            for i in range(3):
                self.create_user(org_id=organization['id'])

        self.mocked_passport.account_add.side_effect = lambda *args, **kwargs: self.get_next_uid(outer_admin=False)

        robot_uid = create_robot_for_service_and_org_id(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            service_slug=self.service['slug'],
            org_id=first_organization['id']
        )

        # создадим платный сервис
        for conn in [self.meta_connection, self.main_connection]:
            service = ServiceModel(conn).create(
                slug='tracker',
                name='Yandex.Tracker',
                client_id='test_client_id_tracker',
                paid_by_license=True,
                trial_period_months=2,
                id=99999,
            )
        # подключим сервис для второй оганизации в триале
        OrganizationServiceModel(self.main_connection).create(
            org_id=second_organization['id'],
            service_id=service['id'],
            ready=True,
        )

        # подключим сервис для одного пользователя второй организации по лицензии
        user_with_license = UserModel(self.main_connection).filter(org_id=second_organization['id']).one()
        self.create_licenses_for_service(
            service_id=service['id'],
            user_ids=[user_with_license['id']],
            org_id=second_organization['id'],
        )

        user_data = []
        for organization in OrganizationModel(self.main_connection).find():
            user_data += UserModel(self.main_connection).find(
                fields=['id', 'org_id', 'created', 'email'],
                filter_data={'org_id': organization['id'], 'is_robot': False},
            )

        exp_data = [
            {
                'org_id': u['org_id'],
                'uid': u['id'],
                'email': u['email'],
                'created': format_date(u['created'], only_date=True),
                'for_date': now_date,
                'licensed_services': [service['slug']] if u['id'] == user_with_license['id'] else [],
            }
            for u in user_data
        ]
        # выборка не содержит роботов
        assert robot_uid not in [i['uid'] for i in exp_data]

        # сохраняем аналитику
        UsersAnalyticsInfoModel(self.main_connection).save_analytics()
        self.process_tasks()
        self.assert_saved_to_yt('users', exp_data)

    def test_should_save_all_org_services_data_to_yt(self):
        first_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']
        second_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='yandex'
        )['organization']
        now_date = utcnow().strftime('%Y-%m-%d')

        tracker = ServiceModel(self.meta_connection).create(
            client_id='123',
            slug='tracker',
            name='tracker',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
            ready_default=True,
        )
        wiki = ServiceModel(self.meta_connection).create(
            client_id='345',
            slug='wiki',
            name='wiki',
            robot_required=False,
            paid_by_license=True,
            ready_default=True,
        )
        # синхронизируем таблицу services
        UpdateServicesInShards().try_run()

        enable_service(
            self.meta_connection,
            self.main_connection,
            first_organization['id'],
            tracker['slug']
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            second_organization['id'],
            wiki['slug']
        )
        disable_service(
            self.meta_connection,
            self.main_connection,
            second_organization['id'],
            wiki['slug'],
            'disabled_by_user'
        )

        exp_data = []
        for org_service in OrganizationServiceModel(self.main_connection).find(
            filter_data={
                'ready': True,
                'enabled': Ignore,
            },
            fields=[
                'slug',
                'org_id',
                'trial_expires',
                'ready_at',
                'disable_reason',
                'enabled_at',
                'disabled_at',
                'enabled',
            ]
        ):
            exp_data.append({
                'org_id': org_service['org_id'],
                'slug': org_service['slug'],
                'trial_expires': format_date(org_service['trial_expires'], allow_none=True),
                'ready_at': format_date(org_service['ready_at'], allow_none=True, only_date=True),
                'enabled_at': format_date(org_service['enabled_at'], allow_none=True, only_date=True),
                'disabled_at': format_date(org_service['disabled_at'], allow_none=True, only_date=True),
                'enabled': org_service['enabled'],
                'disable_reason': org_service['disable_reason'],
                'for_date': now_date,
            })
        # выключенные сервисы тоже складываем
        assert_that(
            exp_data,
            contains(
                has_entries(
                    slug=tracker['slug'],
                    enabled=True,
                    disable_reason=None,
                ),
                has_entries(
                    slug=wiki['slug'],
                    enabled=False,
                    disable_reason='disabled_by_user',
                )
            )
        )

        # сохраняем аналитику
        OrganizationServicesAnalyticsInfoModel(self.main_connection).save_analytics()
        self.assert_saved_to_yt('org_services', exp_data)

    def test_should_save_all_organization_data_to_yt(self):
        # проверяем, что save_organizations_stats_data_to_yt сохраняет данные всех организаций в YT
        first_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']
        second_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='yandex'
        )['organization']
        third_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='mailru',
            admin_uid=111, # внешний админ, чтобы проверить, что он будет в выборке
        )['organization']

        # включаем сервис у первой организации, чтобы проверить, что он будет в выборке
        tracker = ServiceModel(self.meta_connection).create(
            client_id='123',
            slug='tracker',
            name='tracker',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
            ready_default=True,
        )
        # синхронизируем таблицу services
        UpdateServicesInShards().try_run()

        enable_service(
            self.meta_connection,
            self.main_connection,
            first_organization['id'],
            tracker['slug']
        )

        self.enable_paid_mode(org_id=first_organization['id'], first_debt_act_date=utcnow(), balance=100)

        # включим платный режим в третьей организации, но не проставим first_debt_act_date
        # чтобы проверить что и в этом случае складывание данных сработает правильно
        self.enable_paid_mode(org_id=third_organization['id'])
        first_organization = OrganizationModel(self.main_connection).get(first_organization['id'])
        assert_that(
            first_organization['subscription_plan'],
            equal_to('paid')
        )
        third_organization = OrganizationModel(self.main_connection).get(third_organization['id'])
        assert_that(
            third_organization['subscription_plan'],
            equal_to('paid')
        )

        # добавим внешнего заместителя админа, чтобы проверить, что он будет в выборке
        self.user_counter += 1
        UserMetaModel(self.meta_connection).create(
            id=111*10**13 + self.user_counter,
            org_id=second_organization['id'],
            user_type='deputy_admin'
        )

        now_date = utcnow().strftime('%Y-%m-%d')
        exp_data = []
        for org in OrganizationModel(self.main_connection).find(fields=['billing_info.*', '*']):
            if org['billing_info']:
                org['balance'] = str(org['billing_info']['balance'])
                org['first_debt_act_date'] = format_date(org['billing_info']['first_debt_act_date'], allow_none=True)
            services = ['tracker'] if org['id'] == first_organization['id'] else []

            # добавим внутренних и внешних админов и замов
            admins = OrganizationModel(self.main_connection).get_admins(org_id=org['id'])
            admins_ids = [admin['id'] for admin in admins]
            if org['admin_uid'] not in admins_ids:
                admins_ids.append(org['admin_uid'])

            deputy_admins = OrganizationModel(self.main_connection).get_deputy_admins(org_id=org['id'])
            outer_deputy_admins = UserMetaModel(self.meta_connection).get_outer_deputy_admins(org_id=org['id'])
            deputy_admins_ids = [admin['id'] for admin in deputy_admins + outer_deputy_admins]
            features = get_enabled_organizations_features(self.meta_connection, self.main_connection.shard, org['id'])
            feature_ids = features[0]['feature_ids'] if features else []


            exp_data.append(
                {
                    'id': org['id'],
                    'registration_date': format_date(org['created'], only_date=True),
                    'source': org['source'],
                    'subscription_plan': org['subscription_plan'],
                    'services': services,
                    'country': org['country'],
                    'language': org['language'],
                    'tld': org['tld'],
                    'balance': org.get('balance'),
                    'first_debt_act_date': org.get('first_debt_act_date'),
                    'organization_type': org.get('organization_type'),
                    'for_date': now_date,
                    'admins': admins_ids,
                    'deputy_admins': deputy_admins_ids,
                    'name': org['name_plain'],
                    'vip': org['vip'],
                    'feature_ids': [],
                }
            )
        # сохраняем аналитику
        OrganizationsAnalyticsInfoModel(self.main_connection).save_analytics()
        self.assert_saved_to_yt('organizations', exp_data)

    def test_save_all_data_send_event_to_step(self):
        with patch.object(AnalyticsToYtSaver, '_save_data_to_yt', Mock()), \
             patch.object(SendStepEventTask, 'delay', Mock()) as send_step:
            AnalyticsToYtSaver().save()
            send_step.assert_has_calls(
                [
                    unittest.mock.call(table='organizations'),
                    unittest.mock.call(table='domains'),
                    unittest.mock.call(table='org_services'),
                    unittest.mock.call(table='users'),
                ],
                any_order=True,
            )

    def test_save_analytics_for_table(self):
        saver = AnalyticsToYtSaver()
        with patch.object(AnalyticsToYtSaver, '_save_data_to_yt', Mock()) as save_org:
            saver.save(local_table='organizations_analytics_info')
            save_org.assert_called_once_with(
                'organizations',
                OrganizationsAnalyticsInfoModel,
                ANY,
                saver.now_date,
                False,
            )


class TestAnalyticsToYtSaver__get_yt_clusters_without_data(TestCase):
    def test_should_check_all_tables(self):
        mocked_get_yt_clusters_without_table = Mock(return_value=['cluster'])
        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.analytics.yt_utils.get_yt_clusters_without_table', mocked_get_yt_clusters_without_table):
            result = AnalyticsToYtSaver().get_yt_folders_without_data()

        exp_calls = []
        for table in ('organizations', 'users', 'domains', 'org_services'):
            exp_calls.append(
                unittest.mock.call(
                    table=AnalyticsToYtSaver()._get_analytics_table_path(table),
                    check_empty_tables=True,
                    yt_clients=ANY,
                )
            )

        mocked_get_yt_clusters_without_table.assert_has_calls(exp_calls)

        assert_that(
            result,
            equal_to(['cluster'])
        )


class TestSaveAnalyticsCommand(TestCase):
    def test_run(self):
        SaveAnalyticsCommand().try_run(copy_to_yt=True)

        assert_that(
            TaskModel(self.main_connection).filter(
                task_name='intranet.yandex_directory.src.yandex_directory.core.utils.tasks.SaveAnalyticsLocal'
            ).count(),
            equal_to(5)
        )
        assert_that(
            TaskModel(self.main_connection).filter(
                task_name='intranet.yandex_directory.src.yandex_directory.core.utils.tasks.SaveAnalyticsToYTTask'
            ).count(),
            equal_to(5)
        )

    def test_run_correct_metadata(self):
        SaveAnalyticsCommand().try_run(table='users_analytics_info', copy_to_yt=True)
        # Обработку тасков надо запустить дважды, чтобы
        # SaveAnalyticsToYTTask на втором заходе смог обновить метаданные.
        self.process_tasks()
        self.process_tasks()

        copy_to_yt_task = TaskModel(self.main_connection).filter(
            task_name='intranet.yandex_directory.src.yandex_directory.core.utils.tasks.SaveAnalyticsToYTTask'
        ).one()
        save_local_task = TaskModel(self.main_connection).filter(
            task_name='intranet.yandex_directory.src.yandex_directory.core.utils.tasks.SaveAnalyticsLocal'
        ).one()

        assert_that(
            save_local_task['state'],
            equal_to(TASK_STATES.success),
        )

        metadata = Task(self.main_connection, task_id=copy_to_yt_task['id']).get_metadata()
        assert_that(
            metadata,
            equal_to({
                self.main_connection.shard:
                    {
                        'id': save_local_task['id'],
                        'state': TASK_STATES.success,
                    }
            })
        )

        assert_that(
            copy_to_yt_task['state'],
            equal_to(TASK_STATES.success),
        )

        assert_that(
            copy_to_yt_task['params'],
            equal_to({
                'table': 'users_analytics_info',
                'recreate': False,
            }),
        )

    def test_defer_copy_to_yt_task(self):
        dependent_tasks = {}
        save_local_task = SaveAnalyticsLocal(self.main_connection).delay()
        TaskModel(self.main_connection).update(
            {'state': TASK_STATES.in_progress},
            {'id': save_local_task.task_id}
        )

        dependent_tasks[1] = {
            'id': save_local_task.task_id,
            'state': save_local_task.state,
        }

        copy_to_yt_task = SaveAnalyticsToYTTask(self.main_connection).delay(
            metadata=dependent_tasks,
        )

        metadata = copy_to_yt_task.get_metadata()
        assert_that(
            metadata,
            equal_to(dependent_tasks)
        )
        assert_that(
            copy_to_yt_task.state,
            equal_to(TASK_STATES.free)
        )

    def test_fail_copy_to_yt_task(self):
        dependent_tasks = {}
        save_local_task = SaveAnalyticsLocal(self.main_connection).delay()
        all_states = set(
            getattr(TASK_STATES, attr) for attr in dir(TASK_STATES)
            if not attr.startswith('__')
        )
        good_states = (TASK_STATES.success, TASK_STATES.free, TASK_STATES.in_progress)

        for bad_state in all_states.difference(good_states):
            TaskModel(self.main_connection).update(
                {'state': bad_state},
                {'id': save_local_task.task_id}
            )

            dependent_tasks[1] = {
                'id': save_local_task.task_id,
                'state': save_local_task.state,
            }

            copy_to_yt_task = SaveAnalyticsToYTTask(self.main_connection).delay(
                metadata=dependent_tasks,
            )
            self.process_tasks()

            assert_that(
                copy_to_yt_task.state,
                equal_to(TASK_STATES.failed)
            )

    def test_tasks_starting(self):
        # Проверим, что мета-таск можно запустить и он не требует org_id
        with patch.object(SaveAnalyticsMetaTask, 'do'):
            result = SaveAnalyticsMetaTask(self.main_connection).delay()
            assert result
