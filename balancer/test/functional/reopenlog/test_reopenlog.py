# -*- coding: utf-8 -*-
import os
import time
import subprocess

import balancer.test.plugin.context as mod_ctx

from balancer.test.util.predef.handler.server.http import SimpleConfig, DummyConfig
from balancer.test.util.predef import http
from balancer.test.util import asserts
from balancer.test.util.stdlib.multirun import Multirun

from configs import ReopenlogSimple, DynamicReopenlogSimple


class ReopenlogContext(object):
    def start_backends(self):
        self.start_backend(SimpleConfig())
        self.start_backend(DummyConfig(), name='dummy_backend')

    def start_all(self, **kwargs):
        self.start_backends()
        self.start_balancer(ReopenlogSimple(**kwargs))
        time.sleep(1)

    def perform_ok_request(self):
        response = self.perform_request(http.request.get())
        asserts.status(response, 200)
        return response

    def perform_error_request(self):
        return self.perform_request_xfail(http.request.get('/error'))

    def perform_requests(self):
        self.perform_ok_request()
        self.perform_error_request()

    @property
    def access_log(self):
        return self.balancer.config.access_log

    @property
    def error_log(self):
        return self.balancer.config.error_log

    @property
    def sd_log(self):
        return self.balancer.config.sd_log

    @property
    def instance_log(self):
        return self.balancer.config.instance_log

    def log_size(self, log_name):
        return os.stat(log_name).st_size

    def access_log_size(self):
        return self.log_size(self.access_log)

    def assert_access_log_empty(self):
        assert self.access_log_size() == 0

    def assert_access_log_not_empty(self):
        for run in Multirun():
            with run:
                assert self.access_log_size() > 0

    def error_log_size(self):
        return self.log_size(self.error_log)

    def assert_error_log_empty(self):
        assert self.error_log_size() == 0

    def assert_error_log_not_empty(self):
        for run in Multirun():
            with run:
                assert self.error_log_size() > 0

    def instance_log_size(self):
        return self.log_size(self.instance_log)

    def assert_instance_log_empty(self):
        assert self.instance_log_size() == 0

    def assert_instance_log_not_empty(self):
        for run in Multirun():
            with run:
                assert self.instance_log_size() > 0

    def move_log(self, log_src_name, log_dst_name):
        os.rename(log_src_name, log_dst_name)

    def move_accesslog(self):
        self.move_log(self.access_log, self.access_log + '.old')

    def move_errorlog(self):
        self.move_log(self.error_log, self.error_log + '.old')

    def move_instancelog(self):
        self.move_log(self.instance_log, self.instance_log + '.old')

    def move_sdlog(self):
        self.move_log(self.sd_log, self.sd_log + '.old')

    def chmod(self, file_name, permissions):
        with open(file_name, 'a'):
            pass
        os.chmod(file_name, permissions)

    def change_errorlog_permissions(self, permissions=0000):
        self.chmod(self.error_log, permissions)

    def change_accesslog_permissions(self, permissions=0000):
        self.chmod(self.access_log, permissions)

    def change_instancelog_permissions(self, permissions=0000):
        self.chmod(self.instance_log, permissions)

    def rotate_logs(self):
        self.move_accesslog()
        self.move_errorlog()
        self.move_sdlog()

    def call_reopenlog(self, error=False):
        response = self.perform_request(http.request.get('/admin?action=reopenlog'), port=self.balancer.config.admin_port)
        if error:
            asserts.status(response, 500)
        else:
            asserts.status(response, 200)
        return response

    def call_reload_config(self):
        self.balancer.reload_config(self.balancer.config, keep_ports=True, save_globals=True, timeout=1)


reopen_ctx = mod_ctx.create_fixture(ReopenlogContext)


def test_rotate(reopen_ctx):
    """
    Balancer on /admin?action=reopenlog should perform
    close+open on log names from config.
    """
    reopen_ctx.start_all()

    reopen_ctx.perform_requests()

    reopen_ctx.assert_access_log_not_empty()
    reopen_ctx.assert_error_log_not_empty()

    reopen_ctx.rotate_logs()

    response = reopen_ctx.call_reopenlog()
    asserts.content(response, 'Reopen log complete.')

    time.sleep(1.1)
    reopen_ctx.assert_access_log_empty()
    reopen_ctx.assert_error_log_empty()

    reopen_ctx.perform_requests()
    reopen_ctx.assert_access_log_not_empty()
    reopen_ctx.assert_error_log_not_empty()

    # check that the old files is not in use
    assert (subprocess.call(['lsof', reopen_ctx.balancer.config.access_log + '.old']) == 1)
    assert (subprocess.call(['lsof', reopen_ctx.balancer.config.error_log + '.old']) == 1)
    assert (subprocess.call(['lsof', reopen_ctx.balancer.config.sd_log + '.old']) == 1)


def test_rotate_errorlog_bad_permissions(reopen_ctx):
    """
    It is possible that open() may fail while
    /admin?action=reopenlog for errorlog due to
    file permissions change. The logs not touched
    by this fail should rotate. The log with failed
    open is in unspecified behaviour. Balancer still
    works
    """
    reopen_ctx.start_all()

    reopen_ctx.perform_requests()

    reopen_ctx.assert_access_log_not_empty()
    reopen_ctx.assert_error_log_not_empty()

    reopen_ctx.rotate_logs()
    reopen_ctx.change_errorlog_permissions()

    response = reopen_ctx.call_reopenlog(error=True)
    content = response.data.content
    assert reopen_ctx.error_log in content
    assert content.endswith('Reopen log complete.')

    time.sleep(1.1)
    reopen_ctx.assert_access_log_empty()

    reopen_ctx.perform_requests()
    reopen_ctx.assert_access_log_not_empty()

    assert (subprocess.call(['lsof', reopen_ctx.balancer.config.access_log + '.old']) == 1)
    assert (subprocess.call(['lsof', reopen_ctx.balancer.config.error_log + '.old']) == 0)
    assert (subprocess.call(['lsof', reopen_ctx.balancer.config.error_log]) == 1)


def test_rotate_accesslog_bad_permissions(reopen_ctx):
    """
    It is possible that open() may fail while
    /admin?action=reopenlog for accesslog due to
    file permissions change. The logs not touched
    by this fail should rotate. The log with failed
    open is in unspecified behaviour. Balancer still
    works
    """
    reopen_ctx.start_all()

    reopen_ctx.perform_requests()

    reopen_ctx.assert_access_log_not_empty()
    reopen_ctx.assert_error_log_not_empty()

    reopen_ctx.rotate_logs()
    reopen_ctx.change_accesslog_permissions()

    response = reopen_ctx.call_reopenlog(error=True)
    content = response.data.content
    assert reopen_ctx.access_log in content
    assert content.endswith('Reopen log complete.')

    time.sleep(1.1)
    reopen_ctx.assert_error_log_empty()

    reopen_ctx.perform_requests()
    reopen_ctx.assert_error_log_not_empty()

    assert (subprocess.call(['lsof', reopen_ctx.balancer.config.error_log + '.old']) == 1)
    assert (subprocess.call(['lsof', reopen_ctx.balancer.config.access_log + '.old']) == 0)
    assert (subprocess.call(['lsof', reopen_ctx.balancer.config.access_log]) == 1)


def test_instance_rotate_pinger_log(reopen_ctx):
    pinger_log = 'pinger_log'
    reopen_ctx.start_all(workers=1, pinger_log=pinger_log)

    pinger_log = reopen_ctx.balancer.config.pinger_log
    pinger_log_old = pinger_log + '.old'

    # check that the file is in use
    assert (subprocess.call(['lsof', pinger_log]) == 0)

    # check that the file is not empty
    for run in Multirun():
        with run:
            assert os.stat(pinger_log).st_size > 0

    # log rotation
    os.rename(pinger_log, pinger_log_old)
    response = reopen_ctx.call_reopenlog()
    asserts.content(response, 'Reopen log complete.')

    # check that the old file is not in use
    assert (subprocess.call(['lsof', pinger_log_old]) == 1)

    # check that the new file is in use
    assert (subprocess.call(['lsof', pinger_log]) == 0)


def test_rotate_dynamic_log(reopen_ctx):
    blacklist_file = reopen_ctx.manager.fs.create_file('blacklist')
    reopen_ctx.start_backend(SimpleConfig(), name='backend1')
    reopen_ctx.start_backend(SimpleConfig(), name='backend2')
    reopen_ctx.start_balancer(DynamicReopenlogSimple(backends_blacklist=blacklist_file))

    dynamic_log = reopen_ctx.balancer.config.dynamic_balancing_log
    dynamic_log_old = dynamic_log + '.old'

    # check that the file is in use
    assert (subprocess.call(['lsof', dynamic_log]) == 0)

    # check that the file is not empty
    for run in Multirun():
        with run:
            assert os.stat(dynamic_log).st_size > 0

    # log rotation
    os.rename(dynamic_log, dynamic_log_old)
    response = reopen_ctx.call_reopenlog()
    asserts.content(response, 'Reopen log complete.')

    # check that the old file is not in use
    assert (subprocess.call(['lsof', dynamic_log_old]) == 1)

    # check that the new file is in use
    assert (subprocess.call(['lsof', dynamic_log]) == 0)

    # trigger writing to new file
    reopen_ctx.manager.fs.rewrite(blacklist_file, 'first\n')
    time.sleep(5)

    # check that the new file is not empty
    for run in Multirun():
        with run:
            assert os.stat(dynamic_log).st_size > 0


def test_instance_rotate(reopen_ctx):
    """
    Balancer on /admin?action=reopenlog should perform
    close+open on log names from config.
    """
    reopen_ctx.start_all(workers=1)

    reopen_ctx.assert_instance_log_not_empty()

    reopen_ctx.move_instancelog()

    response = reopen_ctx.call_reopenlog()
    assert 'Reopen log complete.' in response.data.content

    assert (subprocess.call(['lsof', reopen_ctx.balancer.config.instance_log + '.old']) == 1)

    reopen_ctx.call_reload_config()
    time.sleep(5)
    reopen_ctx.assert_instance_log_not_empty()
    for run in Multirun():
        with run:
            found_child_message = False
            with open(reopen_ctx.instance_log) as f:
                for line in f:
                    if 'Child started' in line:
                        found_child_message = True
                        break
            assert found_child_message
