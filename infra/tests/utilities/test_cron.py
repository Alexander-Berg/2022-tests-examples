"""Tests distributed cron."""

import enum
import random
import time

import gevent
import pytest
from apscheduler.schedulers.gevent import GeventScheduler as Scheduler
from gevent.event import Event
from gevent.lock import RLock
from gevent.queue import JoinableQueue

from infra.walle.server.tests.lib.util import TestCase
from sepelib.core import config
from walle.clients import juggler as juggler_client
from walle.util import cloud_tools
from walle.util.cron import Cron
from walle.util.mongo import lock as mongo_lock


class TestCronType(str, enum.Enum):
    TEST_JOB = "test-job"


@pytest.fixture
def test(request, database, mp):
    scheduler = Scheduler()
    scheduler.start()
    request.addfinalizer(scheduler.shutdown)
    mongo_lock.start_heartbeat(scheduler)
    request.addfinalizer(mongo_lock.stop_heartbeat)
    return TestCase.create(request)


@pytest.mark.skip
@pytest.mark.slow
@pytest.mark.skip_on_cov
@pytest.mark.flaky(reruns=3, reruns_delay=20)
@pytest.mark.usefixtures("send_event_mock", "database")
@pytest.mark.parametrize("concurrency,retry_delay", ((1, 0), (5, 0), (5, 1)))
def test_cron(test, mp, concurrency, retry_delay):
    done = Event()
    stat = {"runs": [], "last_run": time.time()}

    def job():
        runs = len(stat["runs"])
        if runs >= 7:
            return False

        # Emulate concurrency
        gevent.idle()

        runs += 1
        stat["runs"].append(time.time() - stat["last_run"])
        stat["last_run"] = time.time()

        if runs == 3:
            return False
        elif runs == 5:
            raise ValueError
        elif runs == 7:
            done.set()

    start_time = time.time()
    crons = [Cron("test", retry_delay=retry_delay) for i in range(concurrency)]

    try:
        for cron in crons:
            cron.add_job("test-job", job, period=2, retry_period=5)
            cron.start()

        done.wait()
    finally:
        for cron in crons:
            cron.stop()

    run_time = time.time() - start_time
    for cron in crons:
        assert cron._loops <= round(run_time) * 3

    _compare_times(stat["runs"], [0, 2, 2, 5, 2, 5, 2])


@pytest.mark.skip
@pytest.mark.slow
@pytest.mark.skip_on_cov
@pytest.mark.flaky(reruns=3, reruns_delay=20)
@pytest.mark.usefixtures("send_event_mock", "database")
@pytest.mark.parametrize("crons_count,jobs_count", [(1, 1), (5, 3), (3, 15)])
def test_cron_task_spreading(test, mp, crons_count, jobs_count):
    stat = {}
    job_queue = JoinableQueue()

    def add_job(cron, job_id):
        stat[job_id] = {"runs": [], "last_run": time.time()}

        def job():
            job_stat = stat[job_id]
            runs = len(job_stat["runs"])

            if runs >= 7:
                return False

            # Emulate concurrency
            gevent.idle()

            runs += 1
            job_stat["runs"].append(time.time() - job_stat["last_run"])
            job_stat["last_run"] = time.time()

            if runs == 3:
                return False
            elif runs == 5:
                raise ValueError
            elif runs == 7:
                job_queue.get()
                job_queue.task_done()

        cron.add_job(job_id, job, period=2, retry_period=5)

    crons = [Cron("test") for i in range(crons_count)]

    try:
        for i in range(jobs_count):
            job_queue.put(i)
            for cron in crons:
                add_job(cron, "test-job-{}".format(i))

        for cron in crons:
            cron.start()

        job_queue.join()
    finally:
        for cron in crons:
            cron.stop()

    for job_id, job_stat in stat.items():
        _compare_times(job_stat["runs"], [0, 2, 2, 5, 2, 5, 2], job_id)


@pytest.mark.skip
@pytest.mark.slow
@pytest.mark.skip_on_cov
@pytest.mark.flaky(reruns=3, reruns_delay=20)
@pytest.mark.usefixtures("send_event_mock", "database")
@pytest.mark.parametrize(["crons_count", "jobs_count"], [(5, 3), (3, 15)])
def test_cron_instance_fail_out(test, mp, crons_count, jobs_count):
    stat = {}
    cron_fail_out_lock = RLock
    job_queue = JoinableQueue()

    def add_job(cron, job_id):
        stat[job_id] = {"runs": [], "last_run": time.time()}

        def job():
            job_stat = stat[job_id]
            runs = len(job_stat["runs"])

            if runs >= 7:
                return False

            # Emulate concurrency
            gevent.idle()

            runs += 1
            job_stat["runs"].append(time.time() - job_stat["last_run"])
            job_stat["last_run"] = time.time()

            if runs == 3:
                return False
            elif runs == 4:
                # get one cron and stop it. Other crons must repartition.
                with cron_fail_out_lock:
                    if len(crons) == crons_count:
                        cron = random.choice(crons)
                        cron.stop()

            elif runs == 5:
                raise ValueError
            elif runs == 7:
                job_queue.get()
                job_queue.task_done()

        cron.add_job(job_id, job, period=2, retry_period=5)

    crons = [Cron("test") for i in range(crons_count)]

    try:
        for i in range(jobs_count):
            job_queue.put(i)
            for cron in crons:
                add_job(cron, "test-job-{}".format(i))

        for cron in crons:
            cron.start()

        job_queue.join()
    finally:
        for cron in crons:
            cron.stop()

    for job_id, job_stat in stat.items():
        # delays are bigger because of startup costs on repartition
        _compare_times(job_stat["runs"], [0, 2, 2, 5, 2, 5, 2], job_id, run_cost=3)


def _compare_times(result_times, expected_times, job_id=None, run_cost=0):
    # A workaround for slow TeamCity agents
    accuracy = 1.5
    startup_cost = 2  # partitioner startup

    try:
        assert len(result_times) == len(expected_times)

        for result_time, expected_time in zip(result_times, expected_times):
            assert expected_time <= result_time <= expected_time + accuracy + startup_cost
            startup_cost = run_cost
    except AssertionError:
        print("{} job has missed run times: {} instead of {}".format(job_id, result_times, expected_times))
        raise


def test_cron_monitor_ok(test, database, mp):
    done = Event()
    event_sent = Event()
    call_read = Event()

    def mock_send_event(juggler_service_name, status, message, *args, **kwargs):
        event_sent.set()

    send_event_mock = mp.function(juggler_client.send_event, side_effect=mock_send_event)
    runs = 0

    def job():
        nonlocal runs
        runs += 1
        if runs == 2:
            event_sent.wait()
            done.set()
            call_read.wait()
        return

    cron = Cron("test")
    mp.setattr(cron, 'JOB_MONITORING_INTERVAL', 0.01)
    cron.add_job(TestCronType.TEST_JOB, job, period=1)
    cron.start()
    done.wait()
    send_event_mock.assert_called()
    call_kwargs = send_event_mock.call_args.kwargs
    call_read.set()
    cron.stop()
    assert call_kwargs["juggler_service_name"] == "wall-e.cron.{}.last_success_run".format("test-job")
    assert call_kwargs["status"] == juggler_client.JugglerCheckStatus.OK
    assert call_kwargs["host_name"] == "wall-e.srv.{}".format(config.get_value("environment.name"))
    assert call_kwargs["tags"] == ["wall-e.cron", cloud_tools.get_process_identifier()]


@pytest.mark.parametrize(["period", "work_time"], [(0.2, None), (1, 0.2)])
def test_cron_monitor_timeout(test, database, mp, period, work_time):
    crit_sent = Event()

    def mock_send_event(juggler_service_name, status, message, *args, **kwargs):
        if status == juggler_client.JugglerCheckStatus.CRIT:
            crit_sent.set()

    send_event_mock = mp.function(juggler_client.send_event, side_effect=mock_send_event)
    done = Event()
    call_read = Event()
    runs = 0

    def job():
        crit_sent.clear()
        nonlocal runs
        runs += 1
        crit_sent.wait()
        if runs == 2:
            done.set()
            call_read.wait()
        return

    cron = Cron("test")
    mp.setattr(cron, 'JOB_MONITORING_INTERVAL', 0.01)
    cron.add_job(TestCronType.TEST_JOB, job, period=period, expected_work_time=work_time)
    cron.start()
    done.wait()
    send_event_mock.assert_called()
    call_kwargs = send_event_mock.call_args.kwargs
    call_read.set()
    cron.stop()
    job_state = cron.get_job_state("test-job")
    assert call_kwargs["juggler_service_name"] == "wall-e.cron.{}.last_success_run".format("test-job")
    assert call_kwargs["status"] == juggler_client.JugglerCheckStatus.CRIT
    assert call_kwargs["host_name"] == "wall-e.srv.{}".format(config.get_value("environment.name"))
    assert call_kwargs["tags"] == ["wall-e.cron", job_state.instance]
