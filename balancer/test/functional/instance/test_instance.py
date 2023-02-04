# -*- coding: utf-8 -*-
import pytest
import os
import time
import re
import resource
import subprocess
from datetime import timedelta
from shlex import split

from configs import InstanceConfig, BadIpAddrConfig, CoroutineStackConfig, LogQueueConfig

from balancer.test.util import asserts
from balancer.test.util import process
from balancer.test.util.stdlib.multirun import Multirun
from balancer.test.util.predef import http

from balancer.test.util.process import BalancerStartError


def check_workers(ctx):
    workers = ctx.balancer.get_workers()

    for run in Multirun():
        with run:
            log_content = ctx.manager.fs.read_file(ctx.balancer.config.instance_log)

            for child in workers:
                spawned = 'Spawned child with pid {pid} and its admin addr'.format(pid=child)
                assert spawned in log_content, 'Spawned child is not in instance_log'


THREAD_WORKER_ID_REGEXP = re.compile(r'Spawned thread child with workerId (\d+) and type worker')


def test_workers_spawn(ctx):
    workers = 15

    ctx.start_balancer(InstanceConfig(workers=15))

    assert workers == ctx.get_worker_count()

    for i in range(30):
        log_content = ctx.manager.fs.read_file(ctx.balancer.config.instance_log)
        matched = THREAD_WORKER_ID_REGEXP.findall(log_content)
        if len(matched) == workers:
            break
        time.sleep(1)
    else:
        raise Exception('Timed out')

    worker_ids = []
    for worker_id in matched:
        worker_ids.append(int(worker_id))

    assert set(worker_ids) == set(map(lambda x: x + 1, xrange(workers)))


def test_worker_pids_in_log(ctx):
    """
    Запускаемся с instance_log, видим список воркеров в логе
    """
    ctx.start_balancer(InstanceConfig(workers=4))

    check_workers(ctx)


def test_enable_reuse_port(ctx):
    """
    BALANCER-536
    Проверяем что балансер запускается с опцией enable_reuse_port
    """
    ctx.start_balancer(InstanceConfig(enable_reuse_port=True))
    response = ctx.perform_request(http.request.post(data='data'))
    asserts.status(response, 200)


def test_dns_timeout_tduration_format(ctx):
    """
    BALANCER-124
    Параметр dns_timeout должен понимать значение в формате TDuration
    """
    ctx.start_balancer(InstanceConfig(dns_timeout='10s'))

    assert ctx.balancer.is_alive()


def test_dns_timeout_milliseconds_format(ctx):
    """
    BALANCER-124
    Параметр dns_timeout должен понимать значение, заданное в миллисекундах
    """
    ctx.start_balancer(InstanceConfig(dns_timeout=10))

    assert ctx.balancer.is_alive()


def test_so_keepalive_enabled(ctx):
    """
    BALANCER-31
    Если в instance включена опция tcp_keepalive,
    то при отутствии клиентских пакетов в течение tcp_keep_idle
    клиенту должен отправиться keepalive probe пакет
    """
    delay = 1
    count = 5
    min_delta = timedelta(seconds=0.9 * delay)
    max_delta = timedelta(seconds=1.1 * delay)

    ctx.start_balancer(InstanceConfig(tcp_keep_idle=delay, tcp_keep_intvl=1, tcp_keep_cnt=1))
    tcpdump = ctx.manager.tcpdump.start(ctx.balancer.config.port)

    with ctx.create_http_connection():
        time.sleep((count + 0.1) * delay)
        for run in Multirun(sum_delay=3):
            with run:
                tcpdump.read_all()

                sessions = tcpdump.get_sessions()
                assert len(sessions) > 0
                sess = sessions[0]
                server_packets = sess.other_server_packets
                deltas = zip(server_packets[:-1], server_packets[1:])
                check_delta = lambda t1_t2: min_delta < t1_t2[1] - t1_t2[0] < max_delta
                assert len(server_packets) >= count
                assert all(map(check_delta, deltas))


def base_test_coroutine_stack_size(ctx, coro_stack_size):
    pytest.skip("TODO(velavokr): BALANCER-2359 - implement a working stack monitoring")


def test_coroutine_stack_size(ctx):
    """
    BALANCER-450
    Размер стека корутин указывается при помощи параметра -C при запуске бинарника балансера
    """
    coro_stack_size = 1000 * 1024
    ctx.start_balancer(CoroutineStackConfig(coro_stack_size=coro_stack_size))
    base_test_coroutine_stack_size(ctx, coro_stack_size)


def test_default_coroutine_stack_size(ctx):
    """
    BALANCER-450
    По умолчанию размер стека корутин равен 100 Кб
    """
    default_coro_stack_size = 100 * 1024
    ctx.start_balancer(CoroutineStackConfig())
    base_test_coroutine_stack_size(ctx, default_coro_stack_size)


@pytest.mark.parametrize('value', [0, -1])
def test_log_queue_invalid_max_size_parse_error(ctx, value):
    """
    BALANCER-573
    Балансер не должен запускаться если указано нулевое или отрицательное значение для опции log_queue_max_size
    """
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(LogQueueConfig(log_queue_max_size=value))


@pytest.mark.parametrize('value', [0, -1])
def test_log_queue_invalid_submit_attempts_parse_error(ctx, value):
    """
    BALANCER-573
    Балансер не должен запускаться если указано нулевое или отрицательное значение для опции log_queue_submit_attempts_count
    """
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(LogQueueConfig(log_queue_submit_attempts_count=value))


def test_log_queue_flush_interval(ctx):
    """
    BALANCER-573
    Логи должны записываються по истечению flush interval
    """
    balancer = ctx.start_balancer(LogQueueConfig(log_queue_max_size=1000, log_queue_submit_attempts_count=1, log_queue_flush_interval='3s'))

    response = ctx.perform_request(http.request.get())
    asserts.status(response, 200)
    asserts.content(response, 'OK')

    log_content = ctx.manager.fs.read_file(balancer.config.instance_log)
    log_size = len(log_content.splitlines())
    time.sleep(3)
    for run in Multirun(plan=[0.1] * 20):
        with run:
            log_content = ctx.manager.fs.read_file(balancer.config.instance_log)
            assert len(log_content.splitlines()) > log_size, "Log flush interval has already passed, there should be records in the log"


@pytest.mark.parametrize('workers', [1, 3, 20])
def test_childs_stats(ctx, workers):
    ctx.start_balancer(InstanceConfig(workers=workers))

    for run in Multirun():
        with run:
            alive = ctx.get_unistat()['childs-alive_ammv']
            assert alive == workers


def test_workers_stats(ctx):
    ctx.start_balancer(InstanceConfig(workers=1))

    unistat = ctx.get_unistat()
    assert 'worker-tcp_conns_ammv' in unistat
    assert 'worker-tcp_max_conns_ammv' in unistat
    assert 'worker-conts_ready_ammv' in unistat
    assert 'worker-conts_waiting_ammv' in unistat
    assert 'worker-cpu_usage_time_desync_summ' in unistat
    assert 'worker-processed_log_items_summ' in unistat
    assert 'worker-lost_log_items_summ' in unistat


def test_maxconn(ctx):
    req = 'GET / HTTP/1.1\r\n\r\n'
    ctx.start_balancer(InstanceConfig(maxconn=2, sosndbuf=1, buffer=1))
    time.sleep(1)

    # conns = [conn1]
    conn1 = ctx.create_http_connection().create_stream()
    time.sleep(0.1)

    # conns = [conn1, conn2]
    conn2 = ctx.create_http_connection().create_stream()
    time.sleep(0.1)

    # conns = [conn2, conn1]
    conn1.write(req[0])
    time.sleep(0.1)

    # conns = [conn1, conn3], conn2 is evicted
    conn3 = ctx.create_http_connection().create_stream()
    time.sleep(0.1)

    # chosen by the LRU connection cancel policy
    with pytest.raises(Exception):
        conn2.write(req[0])

    # conns = [conn3, conn1]
    conn1.write(req[1:])
    time.sleep(0.1)

    # conns = [conn1, conn3]
    conn3.write(req[1:])
    time.sleep(0.1)

    # conns = [conn1, conn3]
    assert conn1.read_response().status == 200
    time.sleep(0.1)

    # conns = [conn4, conn1], conn3 is evicted
    conn4 = ctx.create_http_connection().create_stream()
    time.sleep(0.1)

    # Chosen by the LRU connection cancel policy. Have to perform an additional request to defeat tcp read buffers
    with pytest.raises(Exception):
        conn3.read_response()
        conn3.write(req)
        conn3.read_response()

    conn1.write(req)
    assert conn1.read_response().status == 200
    conn4.write(req)
    assert conn4.read_response().status == 200


def test_open_and_close_connection_counters(ctx):
    ctx.start_balancer(InstanceConfig())
    time.sleep(1)

    with ctx.create_http_connection() as conn:
        response = conn.perform_request(http.request.get())
        asserts.status(response, 200)

        asserts.is_not_closed(conn.sock)

        unistat = ctx.get_unistat()
        assert unistat['worker-tcp_conns_ammv'] == 1
        assert unistat['worker-tcp_conns_open_summ'] == 1
        assert unistat['worker-tcp_conns_close_summ'] == 0

    unistat = ctx.get_unistat()
    assert unistat['worker-tcp_conns_ammv'] == 0
    assert unistat['worker-tcp_conns_open_summ'] == 1
    assert unistat['worker-tcp_conns_close_summ'] == 1


def test_fd_stats(ctx):
    ctx.start_balancer(InstanceConfig(workers=1))

    soft, hard = resource.getrlimit(resource.RLIMIT_NOFILE)

    fdsize = 0
    master_pid = process.get_children(ctx.balancer.pid, ctx.logger, recursive=False)[0]
    p2 = subprocess.Popen(split("cat /proc/" + str(master_pid) + "/status"), stdout=subprocess.PIPE)
    second, _ = p2.communicate()
    lines = second.splitlines()
    for line in lines:
        tmp = line.split('\t')
        if tmp[0] == "FDSize:":
            fdsize = int(tmp[1])
            break

    unistat = ctx.get_unistat()

    assert unistat['no_file_limit_ammv'] == soft
    assert unistat['fd_size_ammv'] == fdsize


def test_brackets_in_ip_addrs_parsing(ctx):
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(BadIpAddrConfig())


def test_fail_while_not_enough_fd(ctx):
    resource.setrlimit(resource.RLIMIT_NOFILE, (4096, 4096))
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(InstanceConfig(workers=10, set_no_file=True))


@pytest.mark.parametrize('workers', [1, 2, 3])
def test_pass_when_enough_fd(ctx, workers):
    _, hard = resource.getrlimit(resource.RLIMIT_NOFILE)
    resource.setrlimit(resource.RLIMIT_NOFILE, (20000 * workers, hard))
    ctx.start_balancer(InstanceConfig(workers=workers, set_no_file=True))


@pytest.mark.parametrize('workers', [1, 2, 3])
def test_set_fd_limit(ctx, workers):
    _, hard = resource.getrlimit(resource.RLIMIT_NOFILE)
    resource.setrlimit(resource.RLIMIT_NOFILE, (10000 * workers, hard))
    ctx.start_balancer(InstanceConfig(workers=workers, set_no_file=True))

    unistat = ctx.get_unistat()
    assert unistat['no_file_limit_ammv'] >= 20000 * workers


def test_bad_admin_port(ctx):
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(InstanceConfig(bad_admin_port=True))

    path = os.getcwd()
    path += "/testing_out_stuff/__tests__.test_instance/test_bad_admin_port/balancer_stderr.txt"
    stderr = ctx.manager.fs.read_file(path)
    assert stderr.find('bind failed for [::1]:1') != -1


def test_bad_stats_port(ctx):
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(InstanceConfig(bad_stats_port=True))

    path = os.getcwd()
    path += "/testing_out_stuff/__tests__.test_instance/test_bad_stats_port/balancer_stderr.txt"
    stderr = ctx.manager.fs.read_file(path)
    assert stderr.find('bind failed for [::1]:1') != -1
