# coding: utf-8

from datetime import datetime, timedelta

import mock

from pycron.sqlalchemy_mappers import JobStateHistory, JobResponsible
from pycron.main import find_and_kill_hung_subprocesses, \
    create_responsible_messages_data, format_kills_data, \
    RUN_AT_DT_RESTART_STATUS, TIMEOUT_EXIT_MESSAGE, \
    TERMINATE_EXIT_MESSAGE, RUN_AT_DT_RESTART_EXIT_MESSAGE

from tests.base import TestBase
from tests.object_builder import JobDescrBuilder, JobScheduleBuilder


@mock.patch('pycron.main.kill_processes_by_pids')
class KillHungProcessesTestCase(TestBase):

    def test_killed_by_terminate_flag(self, kill_processes_by_pids_mock):
        job_description = JobDescrBuilder(
            terminate=1, name='test_killed_by_terminate_flag'
        ).build(self.session).obj
        JobScheduleBuilder(name=job_description.name).build(self.session)

        job_description.update_lock(self.session, lock=True)

        self.session.refresh(job_description)
        assert len(job_description.locks) == 1, len(job_description.locks)

        state = job_description.locks[0].state
        state.initialize()
        state.set_started(pid=1, ppid=2)
        self.assertIsNone(state.exit_message)
        self.session.flush()

        find_and_kill_hung_subprocesses(self.session,
                                        create_responsible_messages_data())

        kill_processes_by_pids_mock.assert_called_once_with([state.pid])
        self.assertEqual(state.exit_message, TERMINATE_EXIT_MESSAGE)

    def test_killed_by_timeout(self, kill_processes_by_pids_mock):
        job_description = JobDescrBuilder(
            terminate=0, name='test_killed_by_timeout', timeout=10
        ).build(self.session).obj
        JobScheduleBuilder(name=job_description.name).build(self.session)

        job_description.update_lock(self.session, lock=True)

        self.session.refresh(job_description)
        assert len(job_description.locks) == 1, len(job_description.locks)

        state = job_description.locks[0].state
        state.initialize()
        state.set_started(pid=1, ppid=2)
        self.assertIsNone(state.exit_message)
        state.update_dt = datetime.now() - timedelta(
            seconds=int(job_description.timeout) * 2
        )
        self.session.flush()

        find_and_kill_hung_subprocesses(self.session,
                                        create_responsible_messages_data())

        kill_processes_by_pids_mock.assert_called_once_with([state.pid])
        self.assertEqual(state.exit_message, TIMEOUT_EXIT_MESSAGE)

    def test_killed_by_run_at_dt(self, kill_processes_by_pids_mock):
        job_description = JobDescrBuilder(
            terminate=0, name='test_kill_by_run_at_dt'
        ).build(self.session).obj

        responsible = JobResponsible(email='fake',
                                     task_name=job_description.name)
        self.session.add(responsible)

        schedule = JobScheduleBuilder(
            name=job_description.name
        ).build(self.session).obj

        job_description.update_lock(self.session, lock=True)

        self.session.refresh(job_description)
        assert len(job_description.responsibles) == 1
        assert len(job_description.locks) == 1, len(job_description.locks)

        state = job_description.locks[0].state
        state.initialize()
        state.set_started(pid=1, ppid=2)
        self.assertIsNone(state.exit_message)
        schedule.run_at_dt = state.started + timedelta(seconds=1)
        self.session.flush()

        responsible_messages_data = create_responsible_messages_data()
        find_and_kill_hung_subprocesses(self.session, responsible_messages_data)
        self.session.flush()

        self.assertEqual(state.exit_message, RUN_AT_DT_RESTART_EXIT_MESSAGE)

        kill_processes_by_pids_mock.assert_called_once_with([state.pid])

        state_history = self.session.query(JobStateHistory).filter(
            # for index
            JobStateHistory.dt > datetime.now() - timedelta(days=1),
            JobStateHistory.name == job_description.name
        ).all()
        self.assertEqual(len(state_history), 1)
        state_history = state_history[0]
        self.assertEqual(state_history.status, RUN_AT_DT_RESTART_STATUS)
        self.assertEqual(state_history.message, RUN_AT_DT_RESTART_EXIT_MESSAGE)

        kills_messages = responsible_messages_data[responsible.email]['kills']
        self.assertEqual(len(kills_messages), 1)
        # force_restart must be True
        self.assertTrue(kills_messages[0][3])

    def test_run_at_dt_in_past(self, kill_processes_by_pids_mock):
        job_description = JobDescrBuilder(
            terminate=0, name='test_not_kill_by_run_at_dt'
        ).build(self.session).obj

        schedule = JobScheduleBuilder(
            enabled=1, name=job_description.name
        ).build(self.session).obj

        job_description.update_lock(self.session, lock=True)

        self.session.refresh(job_description)
        assert len(job_description.locks) == 1, len(job_description.locks)

        state = job_description.locks[0].state
        state.initialize()
        state.set_started(pid=1, ppid=2)
        schedule.run_at_dt = state.started - timedelta(seconds=1)
        self.session.flush()

        find_and_kill_hung_subprocesses(self.session,
                                        create_responsible_messages_data())

        kill_processes_by_pids_mock.assert_not_called()


class TestFormatKillsData(object):
    def test_empty(self):
        body_data = []
        subject_data = []
        kills_data = []

        format_kills_data(kills_data, body_data, subject_data)

        assert body_data == []
        assert subject_data == []

    def test_not_empty(self):
        body_data = []
        subject_data = []
        kills_data = [
            # name, host, terminate, force_restart
            ('name1', 'host1', 1, 0),
            ('name2', 'host2', 0, 1),
            ('name3', 'host3', 0, 0),
        ]

        format_kills_data(kills_data, body_data, subject_data)

        assert subject_data == ['name1', 'name2', 'name3']
        assert body_data == [
            u'Принудительно завершённые задачи:',
            u'"name1" принудительно остановлена на host1 по флагу terminate',
            (u'"name2" принудительно остановлена на host2 '
             u'из-за обновления поля run_at_dt'),
            u'"name3" принудительно остановлена на host3 по таймауту',
            u''
        ]
