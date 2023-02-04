# -*- coding: utf-8 -*-
import datetime
import multiprocessing
import pytest
import re
import time

import balancer.test.plugin.context as mod_ctx
import balancer.test.util.asserts as asserts
from balancer.test.util.stdlib.multirun import Multirun

from configs import AdminShutdownConfig, NoHTTPConfig

from balancer.test.util.predef.handler.server.http import SimpleConfig, SimpleDelayedConfig
from balancer.test.util.predef import http

from balancer.test.util.process import BalancerStartError, call


class AdminContext(object):
    def __init__(self):
        super(AdminContext, self).__init__()
        self.__workers = self.request.param

    def start_admin_balancer(self, config):
        config.workers = self.__workers
        return self.start_balancer(config, debug=True)


admin_ctx = mod_ctx.create_fixture(AdminContext, params=[2], ids=['workers_2'])


def test_admin_shutdown(admin_ctx):
    """
    SEPE-4134
    При запросе /admin?action=shutdown балансер пишет в лог много ошибок вида
    5856	2013-05-13T16:26:53.781104Z	on error: (Invalid argument) util/server/listen.cpp:109: can not accept
    """
    admin_ctx.start_backend(SimpleConfig())
    admin_ctx.start_admin_balancer(AdminShutdownConfig())

    admin_ctx.perform_request(http.request.get('/admin?action=shutdown'), port=admin_ctx.balancer.config.admin_port)

    with open(admin_ctx.balancer.config.log, 'r') as log:
        for line in log:
            assert 'can not accept' not in line

    for run in Multirun():
        with run:
            assert not admin_ctx.balancer.is_alive()
    admin_ctx.balancer.set_finished()


@pytest.mark.skip(reason='BALANCER-643: write dmesg helper')
def test_admin_correct_shutdown(admin_ctx):
    """
    BALANCER-643
    Балансер должен завершиться без ошибок при запросе /admin?action=shutdown
    """
    admin_ctx.start_backend(SimpleDelayedConfig(response_delay=100))
    admin_ctx.start_admin_balancer(AdminShutdownConfig())

    pids = [admin_ctx.balancer.pid] + admin_ctx.balancer.get_children()

    def run_request():
        try:
            admin_ctx.perform_request(http.request.get('/'), port=admin_ctx.balancer.config.port)
        except:
            pass

    threads = [multiprocessing.Process(target=run_request) for i in xrange(10)]

    try:
        for thread in threads:
            thread.start()

        admin_ctx.perform_request(http.request.get('/admin?action=shutdown'), port=admin_ctx.balancer.config.admin_port)

        shutdown_start = datetime.datetime.now()
        delta = datetime.timedelta(seconds=12)
        while datetime.datetime.now() - shutdown_start < delta:
            if admin_ctx.balancer.is_alive():
                time.sleep(0.1)
            else:
                break

        assert not admin_ctx.balancer.is_alive()
        assert admin_ctx.balancer.return_code == 0, "Return code should be 0"
        admin_ctx.balancer.set_finished()

        dmesg = call(['dmesg', '-t'], admin_ctx.logger).stdout  # TODO: BALANCER-643
        for line in dmesg.splitlines():
            for pid in pids:
                assert '[' + str(pid) + ']' not in line

    finally:
        for thread in threads:
            thread.join(10)
            if thread.is_alive():
                thread.terminate()


def test_admin_version_sandbox_task_id(admin_ctx):
    """
    BALANCER-655
    При запросе /admin?action=version в ответе должна быть строка
    Sandbox task: <task number>
    """
    admin_ctx.start_backend(SimpleConfig())
    admin_ctx.start_admin_balancer(AdminShutdownConfig())

    response = admin_ctx.perform_request(http.request.get('/admin?action=version'), port=admin_ctx.balancer.config.admin_port)
    match = re.search(r'^\s*Sandbox task: \d+\s*$', response.data.content, flags=re.MULTILINE)
    assert match is not None


def test_invalid_config(admin_ctx):
    """
    SEPE-5581
    Если в конфиге для какого-нибудь модуля не хватает модуля над ним,
    то балансер должен завершиться с кодом возврата 1
    """
    # TODO: check return code
    with pytest.raises(BalancerStartError):
        admin_ctx.start_admin_balancer(NoHTTPConfig())


def test_invalid_admin_request(admin_ctx):
    """BALANCER-3061"""
    admin_ctx.start_backend(SimpleConfig())
    admin_ctx.start_admin_balancer(AdminShutdownConfig())

    response = admin_ctx.perform_request(http.request.get('/admin?action=xxx'), port=admin_ctx.balancer.config.admin_port)
    asserts.status(response, 400)

    response = admin_ctx.perform_request(http.request.get('/admin?action=reload_config'), port=admin_ctx.balancer.config.admin_port)
    asserts.status(response, 400)

    response = admin_ctx.perform_request(http.request.get('/admin?action=graceful_shutdown&timeout=xxx'), port=admin_ctx.balancer.config.admin_port)
    asserts.status(response, 400)


def test_localhost_80(admin_ctx):
    """BALANCER-3092"""
    admin_ctx.start_backend(SimpleConfig())
    admin_ctx.start_admin_balancer(AdminShutdownConfig())

    response = admin_ctx.perform_request(http.request.get('/'), port=admin_ctx.balancer.config.admin_port)
    asserts.status(response, 200)
