# -*- coding: utf-8 -*-
from datetime import datetime, timedelta

from pycron.sqlalchemy_mappers import JobLock, JobState, JobSchedule

from tests.base import TestBase
from tests.object_builder import JobDescrBuilder, JobScheduleBuilder


class TestJobDescr(TestBase):
    def test_update_lock_singleton(self):
        job_descr = JobDescrBuilder().build(self.session).obj
        job_descr.update_lock(self.session)
        self.session.refresh(job_descr)
        self.assertEqual(len(job_descr.locks), 1)

    def test_update_lock_per_host(self):
        job_descr = JobDescrBuilder(count_per_host=2).build(self.session).obj
        job_descr.update_lock(self.session)
        self.session.refresh(job_descr)
        self.assertEqual(len(job_descr.locks), 2)

    def test_switch_modes(self):
        job_descr = JobDescrBuilder(count_per_host=2).build(self.session).obj
        job_descr.update_lock(self.session)
        self.session.refresh(job_descr)
        self.assertEqual(len(job_descr.locks), 2)
        job_descr.count_per_host = None
        self.session.flush()
        job_descr.update_lock(self.session)
        self.session.flush()
        JobLock.delete_unnecessary_locks_and_states(self.session)
        self.session.refresh(job_descr)
        self.assertEqual(len(job_descr.locks), 1)

    def test_mark_unnecessary_states(self):
        job_descr = JobDescrBuilder(count_per_host=2).build(self.session).obj
        job_descr.update_lock(self.session)
        self.session.flush()
        self.session.refresh(job_descr)
        job_descr.mark_unnecessary_states(self.session, 1)
        self.session.flush()
        JobLock.delete_unnecessary_locks_and_states(self.session)
        self.session.flush()
        self.session.refresh(job_descr)
        self.assertEqual(len(job_descr.locks), 1)


class TestJobLock(TestBase):
    def test_remove_unnecessary_locks_and_states(self):
        job_descr = JobDescrBuilder(count_per_host=2).build(self.session).obj
        lock = JobLock("name")
        now = datetime.now()
        started = now+timedelta(days=365)
        lock.state = JobState(started, started)
        self.session.add(lock)
        self.session.flush()
        JobLock.delete_unnecessary_locks_and_states(self.session)
        self.session.flush()
        self.assertEqual(len(job_descr.locks), 0)

    def test_delete_dead_locks_and_states(self):
        job_descr = JobDescrBuilder(count_per_host=2, terminate=0).build(self.session).obj
        JobScheduleBuilder(enabled=1).build(self.session)
        lock = JobLock("name", host='host')
        now = datetime.now()
        started = now - timedelta(days=365)
        lock.state = JobState(started, started)
        self.session.add(lock)
        self.session.flush()
        JobLock.delete_dead_locks_and_states(self.session)
        self.session.flush()
        self.session.expire_all()
        self.assertEqual(len(job_descr.locks), 0)


class TestJobState(object):
    def test_host_worker_id(self, session):
        job_description = JobDescrBuilder(
            terminate=0, name='test_host_worker_id', count_per_host=3
        ).build(session).obj

        job_description.update_lock(session, lock=True)
        session.refresh(job_description)

        state_1 = job_description.locks[0].state
        state_1.initialize(session)
        session.flush()
        state_2 = job_description.locks[1].state
        state_2.initialize(session)
        session.flush()

        session.expire_all()
        assert state_1.host_worker_id == 1
        assert state_2.host_worker_id == 2

        state_2.host_worker_id = 3
        session.flush()
        state_3 = job_description.locks[2].state
        state_3.initialize(session)
        session.flush()

        session.expire_all()
        assert state_1.host_worker_id == 1
        assert state_2.host_worker_id == 3
        assert state_3.host_worker_id == 2


class TestScheduleNeedStart(object):
    def test_need_start_by_run_at_dt(self):
        schedule = JobSchedule('name', '* * * * *')
        schedule.run_at_dt = datetime.now().replace(microsecond=0)
        result = schedule._need_start(
            last_run_dt=schedule.run_at_dt - timedelta(seconds=1)
        )
        assert result == (True, schedule.run_at_dt)

    def test_run_at_dt_in_future(self):
        schedule = JobSchedule('name', '* * * * *')
        schedule.run_at_dt = datetime.now() + timedelta(days=1)
        result = schedule._need_start(
            last_run_dt=datetime.now() - timedelta(days=-1)
        )
        assert result == (False, schedule.run_at_dt)

    def test_run_at_dt_in_past(self):
        schedule = JobSchedule('name', crontab=None)
        schedule.run_at_dt = datetime.now() - timedelta(days=1)
        result = schedule._need_start(
            last_run_dt=datetime.now() - timedelta(days=-1)
        )
        assert result == (False, None)
