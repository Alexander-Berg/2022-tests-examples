# coding: utf-8

import logging
from contextlib import contextmanager
from datetime import datetime, timedelta

import mock
import sqlalchemy as sa

from billing.dcs.dcs.utils.sql.schemes import cmp as cmp_scheme
from billing.dcs.dcs.utils.sql.connection import get_result_engine
from billing.dcs.dcs import settings
from billing.dcs.dcs.utils.mnclose import MNCloseTaskStatuses, MNCloseService, \
    MNCloseTaskActions
from billing.dcs.dcs.monitorings.check_runs import need_notify, RunSystems, notify, \
    MAIL_HEADER_TEMPLATE, get_check_run_id, mnclose_task_ready, \
    process_mnclose_month, ProcessPeriodReturnCodes, process_pycron_period, \
    process_schedule, main, parse_cron_string, \
    calculate_previous_expected_start_time, PrettyRunSystemNames, construct_body
from billing.dcs.dcs.utils.common import relative_date, merge_dicts, Struct, \
    LAST_DAY_OF_MONTH
from billing.dcs.dcs.constants import RunStates, PY_DATE_FORMAT, \
    PY_DATETIME_FORMAT

from billing.dcs.tests.utils import BaseTestCase, change_logging_level, create_patcher, \
    create_application, MNCloseServer

CHECK_RUNS_PATCH_PATH = 'billing.dcs.dcs.monitorings.check_runs'

log = logging.getLogger(__name__)
patch = create_patcher(CHECK_RUNS_PATCH_PATH)


def patch_is_test_run(entity):
    """
    Патч функции is_test_run, который повторяет поведение до появления
    этой функции (тогда признак тестового запуска не проверялся).
    """
    return mock.patch(
        '%s.is_test_run' % CHECK_RUNS_PATCH_PATH,
        mock.Mock(return_value=False)
    )(entity)


def clear_db():
    create_application()
    with change_logging_level('sqlalchemy.engine', logging.WARNING),\
            get_result_engine().connect() as connection, connection.begin():
        connection.execute(cmp_scheme.runs.update().values(run_code=None))
        connection.execute(cmp_scheme.runs.delete().where(
            cmp_scheme.runs.c.run_code.like('functest%')
        ))
        connection.execute(cmp_scheme.mon_mnclose_history.delete())
        connection.execute(cmp_scheme.mon_notifications.delete())
        connection.execute(cmp_scheme.mon_schedules.delete())
        connection.execute(cmp_scheme.mon_runs.delete())


# noinspection PyPep8Naming
def setUpModule():
    """
    Эту функцию unittest выполняет один раз для текущего файла.
    Чистим базу перед тестами.
    Если какой-то тест свалится с ошибкой, его tearDown метод может быть
    пропущен, и база останется неконсистентной перед следующим запуском тестов.
    """
    clear_db()


class BaseDBTestCase(BaseTestCase):
    def setUp(self):
        super(BaseDBTestCase, self).setUp()
        self.engine = get_result_engine()

    def tearDown(self):
        clear_db()

    def now(self):
        """
        Аналог datetime.now(), только microseconds заментяет на 0,
        чтобы можно было сравнивать значение после его round-trip до DB,
        при котором эти microseconds теряются.
        """
        return datetime.now().replace(microsecond=0)

    def create_schedule(self, **params):
        params = merge_dicts({
            'run_code': 'functest',
            'check_code_name': 'test_check',
            'run_system': RunSystems.pycron,
            'run_system_task_name': 'dcs_test_check',
            'notify_interval': 30,
            'max_run_time': 30,
            'start_dt': relative_date(day=1),
            'is_active': True
        }, params)

        # see clear_db
        assert params['run_code'].startswith('functest'), \
            'Bad run_code, runs with that run_code will not be deleted'

        self.engine.execute(cmp_scheme.mon_schedules.insert(), params)

        return Struct(self.engine.execute(
            sa.select([cmp_scheme.mon_schedules]).
            where(cmp_scheme.mon_schedules.c.run_code == params['run_code'])
        ).fetchone())

    def create_mon_run(self, **params):
        params = merge_dicts({
            'host': settings.HOST_NAME,
            'start_dt': self.now()
        }, params)

        mon_run_id = self.engine.execute(
            cmp_scheme.mon_runs.insert().returning(cmp_scheme.mon_runs.c.id),
            params
        ).scalar()

        return self.engine.execute(
            sa.select([cmp_scheme.mon_runs]).
            where(cmp_scheme.mon_runs.c.id == mon_run_id)
        ).fetchone()

    def create_run(self, schedule, start_dt, state=RunStates.finished):
        params = {
            'check_code_name': schedule.check_code_name,
            'run_code': schedule.run_code,
            'host': settings.HOST_NAME,
            'start_dt': start_dt,
            'state': state
        }

        run_id = self.engine.execute(
            cmp_scheme.runs.insert().returning(cmp_scheme.runs.c.id),
            params
        ).scalar()

        return self.engine.execute(
            sa.select([cmp_scheme.runs]).
            where(cmp_scheme.runs.c.id == run_id)
        ).fetchone()


class MNCloseTaskReadyTestCase(BaseTestCase):
    def perform_test(self, task_params, expected_result):
        instantiation_date = datetime(2016, 1, 1)
        task_name = 'dcs_test_check'

        task_params = merge_dicts({
            'itask_id': 1,
            'name_id': task_name,
            'available_actions': []
        }, task_params)

        mnclose_months_map = {
            instantiation_date: {
                'current': [
                    task_params
                ]
            }
        }

        with MNCloseServer(mnclose_months_map):
            task = MNCloseService(
                instantiation_date=instantiation_date
            ).get_task(task_name)
            self.assertEqual(mnclose_task_ready(task), expected_result)

    def test_opened_task(self):
        self.perform_test(
            {'status_name_id': MNCloseTaskStatuses.open},
            True
        )

    def test_ready_task(self):
        self.perform_test(
            {'status_name_id': MNCloseTaskStatuses.new,
             'available_actions': [MNCloseTaskActions.open]},
            True
        )

    def test_closed_task(self):
        self.perform_test(
            {'status_name_id': MNCloseTaskStatuses.resolved},
            False
        )

    def test_not_ready_task(self):
        self.perform_test(
            {'status_name_id': MNCloseTaskStatuses.new},
            False
        )


class NeedNotifyTestCase(BaseDBTestCase):
    def test_no_previous_notifications(self):
        self.assertTrue(need_notify(
            mon_start_time=self.now(),
            schedule=self.create_schedule(),
            period_begin_dt=relative_date(day=1)
        ))

    def test_old_previous_notification(self):
        period_begin_dt = relative_date(day=1)
        mon_start_time = self.now()
        schedule = self.create_schedule()
        mon_run = self.create_mon_run(start_dt=mon_start_time)
        notify_interval = timedelta(minutes=schedule.notify_interval)
        self.engine.execute(
            cmp_scheme.mon_notifications.insert(),
            {'mon_run_id': mon_run.id,
             'run_code': schedule.run_code,
             'period_begin_dt': period_begin_dt,
             'sent_dt': mon_start_time - notify_interval * 2}
        )
        self.assertTrue(need_notify(mon_start_time, schedule, period_begin_dt))

    def test_have_recent_notification(self):
        period_begin_dt = relative_date(day=1)
        mon_start_time = self.now()
        schedule = self.create_schedule()
        mon_run = self.create_mon_run(start_dt=mon_start_time)
        self.engine.execute(
            cmp_scheme.mon_notifications.insert(),
            {'mon_run_id': mon_run.id,
             'run_code': schedule.run_code,
             'period_begin_dt': period_begin_dt,
             'sent_dt': mon_start_time}
        )
        self.assertFalse(need_notify(mon_start_time, schedule, period_begin_dt))


@patch('send_mail')
@patch('need_notify')
class NotifyTestCase(BaseDBTestCase):
    def test_not_notify(self, need_notify_mock, send_mail_mock):
        schedule = self.create_schedule()
        mon_run = self.create_mon_run()
        need_notify_mock.return_value = False
        notify(mon_start_time=self.now(), mon_run_id=mon_run.id,
               schedule=schedule, period_begin_dt=relative_date(day=1))
        self.assertFalse(send_mail_mock.called)

    # noinspection PyUnusedLocal
    @patch('construct_body')
    @patch('datetime')
    def test_mon_notification_inserted(self, datetime_mock, construct_body_mock,
                                       need_notify_mock, send_mail_mock):
        period_begin_dt = relative_date(day=1)
        fake_now = mon_start_time = self.now()
        datetime_mock.now.return_value = fake_now

        schedule = self.create_schedule()
        mon_run = self.create_mon_run()

        need_notify_mock.return_value = True
        notify(mon_start_time, mon_run.id, schedule, period_begin_dt)

        mon_notifications = self.engine.execute(
            sa.select([cmp_scheme.mon_notifications])
        ).fetchall()
        self.assertEqual(len(mon_notifications), 1)

        mon_notification = mon_notifications[0]
        self.assertEqual(mon_notification.mon_run_id, mon_run.id)
        self.assertEqual(mon_notification.run_code, schedule.run_code)
        self.assertEqual(mon_notification.period_begin_dt, period_begin_dt)
        self.assertEqual(mon_notification.sent_dt, fake_now)

    @patch('construct_body')
    def test_subject_construction(self, construct_body_mock, need_notify_mock,
                                  send_mail_mock):
        period_begin_dt = relative_date(day=1)
        expected_start_time = relative_date(day=2)
        mon_start_time = self.now()

        schedule = self.create_schedule(run_system=RunSystems.pycron)
        mon_run = self.create_mon_run()

        expected_body = u'test'
        construct_body_mock.return_value = expected_body
        need_notify_mock.return_value = True
        notify(mon_start_time, mon_run.id, schedule, period_begin_dt,
               expected_start_time=expected_start_time)

        expected_header = MAIL_HEADER_TEMPLATE.format(
            check_code_name=schedule.check_code_name,
            run_system_pretty_name=PrettyRunSystemNames[schedule.run_system],
            run_system_task_name=schedule.run_system_task_name,
            period_begin_dt=period_begin_dt.strftime(PY_DATE_FORMAT)
        )
        expected_subject = expected_header + u' Сверка не отработала'

        self.check_mock_calls(
            construct_body_mock,
            [mock.call(schedule, expected_header, False, expected_start_time)]
        )

        self.check_mock_calls(
            send_mail_mock,
            [mock.call(expected_subject, expected_body)]
        )


@patch('get_check_description')
@patch('get_running_check_info')
class ConstructBodyTestCase(BaseTestCase):
    def test_deadline_overdue(self, get_running_check_info_mock,
                              get_check_description_mock):
        check_description = 'test_description'
        get_check_description_mock.return_value = check_description
        running_check_info = {
            'is_running': True,
            'logon_time': relative_date(minutes=-2),
            'machine': 'test machine'
        }
        get_running_check_info_mock.return_value = running_check_info
        schedule = Struct(check_code_name='test_check',
                          run_system=RunSystems.pycron,
                          max_run_time=30)
        expected_start_time = relative_date(day=1, hour=2)
        header = u'[aob][PyCron: dcs_aob][2018.05.01]'
        expected_body = (
            u'{header}\n\n'
            u'Сверка "{check_description}" не отработала согласно расписанию.\n'
            u'Предполагаемая дата запуска: {expected_start_time}\n'
            u'Максимально допустимое время работы: {max_run_time} минут\n'
            u'Сверка выполняется с {logon_time} на {machine}'
        ).format(
            header=header,
            check_description=check_description,
            expected_start_time=expected_start_time.strftime(PY_DATETIME_FORMAT),
            max_run_time=schedule.max_run_time,
            logon_time=running_check_info['logon_time'].strftime(PY_DATETIME_FORMAT),
            machine=running_check_info['machine']
        )
        self.assertEqual(construct_body(
            schedule=schedule,
            header=header,
            mnclose_task_is_closed=False,
            expected_start_time=expected_start_time
        ), expected_body)

    def test_mnclose_task_is_closed(self, get_running_check_info_mock,
                                    get_check_description_mock):
        check_description = 'test_description'
        get_check_description_mock.return_value = check_description
        running_check_info = {'is_running': False}
        get_running_check_info_mock.return_value = running_check_info
        schedule = Struct(check_code_name='test_check',
                          run_system=RunSystems.mnclose)
        header = u'[aob][MNClose: dcs_aob][2018.05.01]'
        expected_body = (
            u'{header}\n\n'
            u'Сверка "{check_description}" не отработала согласно расписанию.\n'
            u'Задача в {mnclose_system_pretty_name} уже закрыта, но запуск не найден.'
        ).format(
            header=header,
            check_description=check_description,
            mnclose_system_pretty_name=PrettyRunSystemNames[schedule.run_system]
        )
        self.assertEqual(construct_body(
            schedule=schedule,
            header=header,
            mnclose_task_is_closed=True
        ), expected_body)


@patch_is_test_run
class GetCheckRunIDTestCase(BaseDBTestCase):
    def test_no_run(self):
        self.assertIsNone(
            get_check_run_id(schedule=self.create_schedule(),
                             period_begin_dt=relative_date(day=1))
        )

    def test_run_too_old(self):
        period_begin_dt = relative_date(day=1)
        schedule = self.create_schedule()
        self.create_run(schedule, period_begin_dt - timedelta(days=1))
        self.assertIsNone(get_check_run_id(schedule, period_begin_dt))

    def test_run_with_bad_state(self):
        period_begin_dt = relative_date(day=1)
        schedule = self.create_schedule()
        self.create_run(schedule, start_dt=period_begin_dt,
                        state=RunStates.failed)
        self.assertIsNone(get_check_run_id(schedule, period_begin_dt))

    def test_run_found(self):
        period_begin_dt = relative_date(day=1)
        schedule = self.create_schedule()
        run = self.create_run(schedule,
                              start_dt=period_begin_dt + timedelta(days=1))
        self.assertEqual(get_check_run_id(schedule, period_begin_dt), run.id)


@patch_is_test_run
class ProcessMNCloseMonthTestCase(BaseDBTestCase):
    @contextmanager
    def mnclose_context(self, month, task_name, task_status,
                        available_actions=None):
        """ Localize mnclose boilerplate """
        mnclose_month = MNCloseService(instantiation_date=month)
        with MNCloseServer({month: {'current': [{
                'itask_id': 1,
                'name_id': task_name,
                'status_name_id': task_status,
                'available_actions': available_actions or []
        }]}}):
            yield mnclose_month

    def test_schedule_start_dt_not_reached(self):
        month = relative_date(day=1)
        mon_start_time = self.now()

        schedule = self.create_schedule(
            run_system=RunSystems.mnclose,
            start_dt=relative_date(month, months=+1)
        )
        mon_run = self.create_mon_run()

        mnclose_month = MNCloseService(instantiation_date=month)

        self.assertEqual(
            process_mnclose_month(
                mon_run.id, mon_start_time, schedule, month, mnclose_month
            ),
            ProcessPeriodReturnCodes.schedule_start_dt_not_reached
        )

    def test_task_not_ready(self):
        month = relative_date(day=1)
        mon_start_time = self.now()

        schedule = self.create_schedule(run_system=RunSystems.mnclose,
                                        start_dt=month)
        mon_run = self.create_mon_run()

        with self.mnclose_context(
            month, schedule.run_system_task_name, MNCloseTaskStatuses.new
        ) as mnclose_month:
            self.assertEqual(
                process_mnclose_month(mon_run.id, mon_start_time, schedule,
                                      month, mnclose_month),
                ProcessPeriodReturnCodes.check_previous_period
            )

    def test_task_became_ready(self):
        month = relative_date(day=1)
        mon_start_time = self.now()

        schedule = self.create_schedule(run_system=RunSystems.mnclose,
                                        start_dt=month)
        mon_run = self.create_mon_run()

        with self.mnclose_context(
            month, schedule.run_system_task_name,
            MNCloseTaskStatuses.new, [MNCloseTaskActions.open]
        ) as mnclose_month:
            self.assertEqual(
                process_mnclose_month(mon_run.id, mon_start_time, schedule,
                                      month, mnclose_month),
                ProcessPeriodReturnCodes.check_previous_period
            )

        query = sa.select([cmp_scheme.mon_mnclose_history])
        self.assertEqual(self.engine.execute(query.count()).scalar(), 1)
        mnclose_history_record = self.engine.execute(query).fetchone()
        self.assertEqual(mnclose_history_record.ready_dt, mon_start_time)
        self.assertEqual(mnclose_history_record.month, month)
        self.assertEqual(mnclose_history_record.task_name,
                         schedule.run_system_task_name)

    def test_deadline_not_reached(self):
        month = relative_date(day=1)
        mon_start_time = self.now()

        schedule = self.create_schedule(run_system=RunSystems.mnclose,
                                        start_dt=month)
        mon_run = self.create_mon_run()

        get_history_records_count = lambda: self.engine.execute(
            sa.select([cmp_scheme.mon_mnclose_history]).count()
        ).scalar()

        self.engine.execute(
            cmp_scheme.mon_mnclose_history.insert(),
            {'task_name': schedule.run_system_task_name,
             'month': month,
             'ready_dt': mon_start_time}
        )

        initial_history_records_count = get_history_records_count()

        with self.mnclose_context(
            month, schedule.run_system_task_name,
            MNCloseTaskStatuses.new, [MNCloseTaskActions.open]
        ) as mnclose_month:
            self.assertEqual(
                process_mnclose_month(mon_run.id, mon_start_time, schedule,
                                      month, mnclose_month),
                ProcessPeriodReturnCodes.check_previous_period
            )

        self.assertEqual(get_history_records_count(),
                         initial_history_records_count)

    @patch('notify')
    def test_deadline_overdue(self, notify_mock):
        month = relative_date(day=1)
        mon_start_time = self.now()

        schedule = self.create_schedule(run_system=RunSystems.mnclose,
                                        start_dt=month)
        mon_run = self.create_mon_run()

        max_run_time = timedelta(minutes=schedule.max_run_time)
        ready_dt = mon_start_time - max_run_time * 2
        self.engine.execute(
            cmp_scheme.mon_mnclose_history.insert(),
            {'task_name': schedule.run_system_task_name,
             'month': month,
             'ready_dt': ready_dt}
        )

        with self.mnclose_context(
            month, schedule.run_system_task_name,
            MNCloseTaskStatuses.new, [MNCloseTaskActions.open]
        ) as mnclose_month:
            self.assertEqual(
                process_mnclose_month(mon_run.id, mon_start_time, schedule,
                                      month, mnclose_month),
                ProcessPeriodReturnCodes.notify_called
            )

        self.check_mock_calls(notify_mock, [
            mock.call(mon_start_time, mon_run.id, schedule, month,
                      expected_start_time=ready_dt)
        ])

    @patch('notify')
    def test_task_resolved(self, notify_mock):
        month = relative_date(day=1)
        mon_start_time = self.now()

        schedule = self.create_schedule(run_system=RunSystems.mnclose,
                                        start_dt=month)
        mon_run = self.create_mon_run()

        with self.mnclose_context(
            month, schedule.run_system_task_name,
            MNCloseTaskStatuses.resolved, [MNCloseTaskActions.reopen]
        ) as mnclose_month:
            self.assertEqual(
                process_mnclose_month(mon_run.id, mon_start_time, schedule,
                                      month, mnclose_month),
                ProcessPeriodReturnCodes.notify_called
            )

        self.check_mock_calls(notify_mock, [
            mock.call(mon_start_time, mon_run.id, schedule, month,
                      mnclose_task_is_closed=True)
        ])

    def test_run_found(self):
        month = relative_date(day=1)
        mon_start_time = self.now()

        schedule = self.create_schedule(run_system=RunSystems.mnclose,
                                        start_dt=month)
        mon_run = self.create_mon_run()
        self.create_run(schedule, start_dt=month)

        mnclose_month = MNCloseService(instantiation_date=month)

        self.assertEqual(
            process_mnclose_month(
                mon_run.id, mon_start_time, schedule, month, mnclose_month
            ),
            ProcessPeriodReturnCodes.run_found
        )


@patch_is_test_run
class ProcessPycronPeriodTestCase(BaseDBTestCase):
    def test_schedule_start_dt_not_reached(self):
        schedule = self.create_schedule(run_system=RunSystems.pycron)
        mon_run = self.create_mon_run()

        self.assertEqual(
            process_pycron_period(
                schedule, mon_start_time=self.now(), mon_run_id=mon_run.id,
                expected_start_time=schedule.start_dt - timedelta(days=1)
            ),
            ProcessPeriodReturnCodes.schedule_start_dt_not_reached
        )

    @patch('notify')
    def test_notify_called(self, notify_mock):
        schedule = self.create_schedule(run_system=RunSystems.pycron)
        mon_run = self.create_mon_run()
        expected_start_time = schedule.start_dt
        max_run_time = timedelta(minutes=schedule.max_run_time)
        mon_start_time = expected_start_time + max_run_time * 2

        self.assertEqual(
            process_pycron_period(schedule, mon_start_time,
                                  mon_run.id, expected_start_time),
            ProcessPeriodReturnCodes.notify_called
        )

        self.check_mock_calls(notify_mock, [mock.call(
            mon_start_time, mon_run.id, schedule, expected_start_time
        )])

    def test_run_found(self):
        schedule = self.create_schedule(run_system=RunSystems.pycron)
        mon_run = self.create_mon_run()
        self.create_run(schedule, start_dt=schedule.start_dt)

        self.assertEqual(
            process_pycron_period(
                schedule, mon_start_time=self.now(),
                mon_run_id=mon_run.id,
                expected_start_time=schedule.start_dt
            ),
            ProcessPeriodReturnCodes.run_found
        )

    def test_deadline_not_reached(self):
        schedule = self.create_schedule(run_system=RunSystems.pycron)
        mon_run = self.create_mon_run()
        mon_start_time = self.now()
        max_run_time = timedelta(minutes=schedule.max_run_time)

        self.assertEqual(
            process_pycron_period(
                schedule, mon_start_time, mon_run.id,
                expected_start_time=mon_start_time - max_run_time / 2
            ),
            ProcessPeriodReturnCodes.check_previous_period
        )


@patch('process_mnclose_schedule')
@patch('process_pycron_schedule')
class ProcessScheduleTestCase(BaseDBTestCase):
    def test_pycron_run_system(self, process_pycron_schedule_mock,
                               process_mnclose_schedule_mock):
        # Fill params with random unique values for later assertions
        (mon_start_time, mon_run_id,
         current_month, current_month_mnclose,
         previous_month, previous_month_mnclose) = range(6)
        schedule = self.create_schedule(run_system=RunSystems.pycron)

        process_schedule(schedule, mon_start_time, mon_run_id,
                         current_month, current_month_mnclose,
                         previous_month, previous_month_mnclose)

        self.assertEqual(process_mnclose_schedule_mock.call_count, 0)
        self.check_mock_calls(process_pycron_schedule_mock, [
            mock.call(schedule, mon_start_time, mon_run_id)
        ])

    def test_mnclose_run_system(self, process_pycron_schedule_mock,
                                process_mnclose_schedule_mock):
        # Fill params with random unique values for later assertions
        (mon_start_time, mon_run_id,
         current_month, current_month_mnclose,
         previous_month, previous_month_mnclose) = range(6)
        schedule = self.create_schedule(run_system=RunSystems.mnclose)

        process_schedule(schedule, mon_start_time, mon_run_id,
                         current_month, current_month_mnclose,
                         previous_month, previous_month_mnclose)

        self.assertEqual(process_pycron_schedule_mock.call_count, 0)
        self.check_mock_calls(process_mnclose_schedule_mock, [mock.call(
            schedule, mon_start_time, mon_run_id, current_month,
            current_month_mnclose, previous_month, previous_month_mnclose
        )])

    def test_unknown_run_system(self, process_pycron_schedule_mock,
                                process_mnclose_schedule_mock):
        # Fill params with random unique values for later assertions
        (mon_start_time, mon_run_id,
         current_month, current_month_mnclose,
         previous_month, previous_month_mnclose) = range(6)
        schedule = self.create_schedule(run_system='fake')

        with self.assertRaises(NotImplementedError):
            process_schedule(schedule, mon_start_time, mon_run_id,
                             current_month, current_month_mnclose,
                             previous_month, previous_month_mnclose)

        self.assertEqual(process_pycron_schedule_mock.call_count, 0)
        self.assertEqual(process_mnclose_schedule_mock.call_count, 0)


# noinspection PyPep8Naming,PyUnusedLocal
@patch('send_mail')
@patch('process_schedule')
@patch('MNCloseService')
class MainTestCase(BaseDBTestCase):
    @patch('datetime')
    def test_schedule_skipped_by_start_dt_filter(self, datetime_mock,
                                                 MNCloseService_mock,
                                                 process_schedule_mock,
                                                 send_mail_mock):
        mon_start_time = self.now()
        datetime_mock.now.return_value = mon_start_time
        self.create_schedule(start_dt=mon_start_time + timedelta(days=1))
        main()
        self.assertEqual(process_schedule_mock.call_count, 0)

    def test_not_active_schedule_is_skipped(self, MNCloseService_mock,
                                            process_schedule_mock,
                                            send_mail_mock):
        self.create_schedule(is_active=False)
        main()
        self.assertEqual(process_schedule_mock.call_count, 0)

    @patch('datetime')
    @patch('settings')
    def test_mon_run_is_inserted(self, settings_mock, datetime_mock,
                                 MNCloseService_mock, process_schedule_mock,
                                 send_mail_mock):
        nows = [self.now(),
                self.now() + timedelta(hours=1)]
        datetime_mock.now.side_effect = nows
        fake_host_name = 'fake host'
        settings_mock.HOST_NAME = fake_host_name

        main()

        mon_runs = self.engine.execute(sa.select([cmp_scheme.mon_runs])).fetchall()
        self.assertEqual(len(mon_runs), 1)
        mon_run = mon_runs[0]
        self.assertEqual(mon_run.host, fake_host_name)
        self.assertEqual(mon_run.start_dt, nows[0])

    @patch('datetime')
    def test_mon_run_finish_dt_is_set(self, datetime_mock, MNCloseService_mock,
                                      process_schedule_mock, send_mail_mock):
        nows = [self.now(),
                self.now() + timedelta(hours=1)]
        datetime_mock.now.side_effect = nows

        main()

        mon_runs = self.engine.execute(sa.select([cmp_scheme.mon_runs])).fetchall()
        self.assertEqual(len(mon_runs), 1)
        mon_run = mon_runs[0]
        self.assertEqual(mon_run.finish_dt, nows[1])

    @patch('datetime')
    def test_process_schedule_call(self, datetime_mock, MNCloseService_mock,
                                   process_schedule_mock, send_mail_mock):
        mon_start_time = self.now()
        datetime_mock.now.return_value = mon_start_time
        schedule = self.create_schedule()

        current_month = relative_date(mon_start_time, day=1)
        previous_month = relative_date(current_month, months=-1)

        current_month_mnclose = mock.Mock()
        previous_month_mnclose = mock.Mock()

        mnclose_service_return_values = {
            current_month: current_month_mnclose,
            previous_month: previous_month_mnclose
        }

        def fake_mnclose_service(mnclose_back_end_url=None,
                                 mnclose_user=None,
                                 instantiation_date=None,
                                 month_graph_state_dt=None):
            return mnclose_service_return_values[instantiation_date]

        MNCloseService_mock.side_effect = fake_mnclose_service

        main()

        self.check_mock_calls(MNCloseService_mock, [
            mock.call(instantiation_date=current_month),
            mock.call(instantiation_date=previous_month)
        ])

        mon_run = self.engine.execute(sa.select([cmp_scheme.mon_runs])).fetchone()
        self.check_mock_calls(process_schedule_mock, [mock.call(
            schedule, mon_start_time, mon_run.id,
            current_month, current_month_mnclose,
            previous_month, previous_month_mnclose
        )])

    def test_with_exception(self, MNCloseService_mock, process_schedule_mock,
                            send_mail_mock):
        # Insert two schedules to check that schedules processing do not
        # stops after first exception
        self.create_schedule()
        self.create_schedule(run_code='functest2')

        class ScheduleException(Exception):
            pass

        process_schedule_mock.side_effect = ScheduleException()
        with self.assertRaises(ScheduleException), \
                change_logging_level('yandex-dcs', logging.CRITICAL):
            main()
        self.assertEqual(process_schedule_mock.call_count, 2)


class CalculatePreviousExpectedStartTimeTestCase(BaseTestCase):
    def test_CHECK_2350(self):
        self.assertEqual(
            calculate_previous_expected_start_time(
                cron=parse_cron_string('10 11 20-31 * *'),
                current_expected_start_time=datetime(2017, 3, 30, 11, 10),
                run_on_relative_kwargs={'day': LAST_DAY_OF_MONTH,
                                        'workdays': -2}
            ),
            datetime(2017, 2, 27, 11, 10)
        )
