# -*- coding: utf-8 -*-
import pytest

import os
import signal
import threading
from balancer.test.util.stdlib.multirun import Multirun
import time
from configs import ReloadConfig, ReloadConfigIncorrect, ReloadConfigFailOnInit, ReloadConfigSlowStart

import balancer.test.util.asserts as asserts
from balancer.test.util.balancer._balancer import ReloadConfigFailure
from balancer.test.util.predef import http
from balancer.test.util import process
from balancer.test.util.sanitizers import sanitizers


def check_request(ctx, request, expected_content):
    response = ctx.perform_request(request, port=ctx.balancer.config.port)
    asserts.content(response, expected_content)


def test_reload_config(ctx):
    """
    BALANCER-326
    Балансер должен переключить конфиг при вызове /admin?action=reload_config
    """

    ctx.start_balancer(ReloadConfig(workers=2, response='aaa'), debug=True)
    assert ctx.get_worker_count() == 2
    check_request(ctx, http.request.get(), 'aaa')

    ctx.balancer.reload_config(ReloadConfig(workers=3, response='bbb'))

    time.sleep(5)

    assert 1 <= ctx.get_worker_count() <= 3
    check_request(ctx, http.request.get(), 'bbb')
    for run in Multirun():
        with run:
            assert ctx.get_worker_count() == 3
    check_request(ctx, http.request.get(), 'bbb')


def test_reload_config_double(ctx):
    """
    BALANCER-326
    Балансер должен успешно обрабатывать два переключения подряд
    """
    ctx.start_balancer(ReloadConfig(workers=2, response='aaa'), debug=True)
    assert ctx.get_worker_count() == 2
    check_request(ctx, http.request.get(), 'aaa')

    ctx.balancer.reload_config(ReloadConfig(workers=3, response='bbb'))

    time.sleep(2)

    assert 1 <= ctx.get_worker_count() <= 3
    check_request(ctx, http.request.get(), 'bbb')
    for run in Multirun():
        with run:
            assert ctx.get_worker_count() == 3
    check_request(ctx, http.request.get(), 'bbb')

    ctx.balancer.reload_config(ReloadConfig(workers=2, response='ccc'))

    time.sleep(2)

    assert 1 <= ctx.get_worker_count() <= 2
    check_request(ctx, http.request.get(), 'ccc')
    for run in Multirun():
        with run:
            assert ctx.get_worker_count() == 2
    check_request(ctx, http.request.get(), 'ccc')


def test_reload_config_incorrect(ctx):
    """
    BALANCER-939, BALANCER-1798
    Балансер должен вернуть ошибку при попытке переключения на плохой конфиг
    Релоад хорошего конфига в следующий раз не должен приводить к фейлу
    """
    ctx.start_balancer(ReloadConfig(workers=2, response='aaa'), debug=True)
    assert ctx.get_worker_count() == 2
    check_request(ctx, http.request.get(), 'aaa')

    with pytest.raises(ReloadConfigFailure):
        ctx.balancer.reload_config(ReloadConfigIncorrect())

    time.sleep(2)

    check_request(ctx, http.request.get(), 'aaa')
    assert ctx.get_worker_count() == 2

    ctx.balancer.reload_config(ReloadConfig(workers=2, response='bbb'))


def test_reload_config_fail_on_init(ctx):
    """
    BALANCER-965
    Балансер должен вернуть ошибку при подении первого (тестового) воркера
    """
    ctx.start_balancer(ReloadConfig(workers=2, response='aaa'), debug=True)
    assert ctx.get_worker_count() == 2
    check_request(ctx, http.request.get(), 'aaa')

    with pytest.raises(ReloadConfigFailure):
        ctx.balancer.reload_config(ReloadConfigFailOnInit(workers=3, response='bbb'))

    time.sleep(2)

    check_request(ctx, http.request.get(), 'aaa')
    assert ctx.get_worker_count() == 2


def test_reload_config_kill_new_master_when_new_worker_dies(ctx):
    """
    BALANCER-1776
    Балансер должен убить нового мастера, в случае падения нового воркера
    BALANCER-1767
    Воркер должен вернуть сообщение с кодом ошибки
    """

    ctx.start_balancer(ReloadConfig(workers=2, response='aaa'), debug=True)
    time.sleep(2)
    old_master_pid = process.get_children(ctx.balancer.pid, ctx.logger, recursive=False)[0]

    result = [None]

    def run_request():
        try:
            ctx.balancer.reload_config(ReloadConfigSlowStart(workers=2, response='bbb'))
        except ReloadConfigFailure, exception:
            result[0] = exception.response

    thread = threading.Thread(target=run_request)
    thread.start()
    time.sleep(2)

    masters = process.get_children(ctx.balancer.pid, ctx.logger, recursive=False)
    masters.remove(old_master_pid)
    new_master_pid = masters[0]

    os.kill(new_master_pid, signal.SIGSEGV)

    thread.join()
    time.sleep(2)

    survived_master = process.get_children(ctx.balancer.pid, ctx.logger, recursive=False)

    assert result[0]
    if not sanitizers.msan_enabled() and not sanitizers.asan_enabled() and not sanitizers.ubsan_enabled():
        assert result[0].startswith("Reload config failed: New master exited")
    assert len(survived_master) == 1
    assert survived_master[0] != new_master_pid
    assert survived_master[0] == old_master_pid


def test_reload_config_kill_new_master_when_reload_config_timeouts(ctx):
    ctx.start_balancer(ReloadConfig(workers=2, response='aaa'), debug=True)
    time.sleep(2)
    old_master_pid = process.get_children(ctx.balancer.pid, ctx.logger, recursive=False)[0]

    def run_request():
        try:
            ctx.balancer.reload_config(ReloadConfigSlowStart(workers=2, response='bbb'), timeout=7)
        except ReloadConfigFailure:
            pass

    thread = threading.Thread(target=run_request)
    thread.start()
    time.sleep(2)

    masters = process.get_children(ctx.balancer.pid, ctx.logger, recursive=False)
    masters.remove(old_master_pid)
    new_master_pid = masters[0]

    thread.join()
    time.sleep(26)

    survived_master = process.get_children(ctx.balancer.pid, ctx.logger, recursive=False)

    assert len(survived_master) == 1
    assert survived_master[0] != new_master_pid
    assert survived_master[0] == old_master_pid


def test_reload_config_twice_only_first_works(ctx):
    ctx.start_balancer(ReloadConfig(workers=2, response='aaa'), debug=True)
    time.sleep(2)
    old_master_pid = process.get_children(ctx.balancer.pid, ctx.logger, recursive=False)[0]

    def run_request():
        try:
            ctx.balancer.reload_config(ReloadConfigSlowStart(workers=2, response='bbb'))
        except ReloadConfigFailure:
            pass

    thread = threading.Thread(target=run_request)
    thread.start()
    time.sleep(2)

    masters = process.get_children(ctx.balancer.pid, ctx.logger, recursive=False)
    masters.remove(old_master_pid)
    new_master_pid = masters[0]

    thread2 = threading.Thread(target=run_request)
    thread2.start()

    thread.join()
    thread2.join()
    time.sleep(2)

    survived_master = process.get_children(ctx.balancer.pid, ctx.logger, recursive=False)

    assert len(survived_master) == 1
    assert survived_master[0] == new_master_pid
    assert survived_master[0] != old_master_pid


@pytest.mark.parametrize(
    'save_globals',
    [True, False]
)
def test_reload_config_variables(ctx, save_globals):
    ctx.start_balancer(ReloadConfig(workers=3, response='aaa'), debug=True)
    assert ctx.get_worker_count() == 3
    check_request(ctx, http.request.get(), 'aaa')

    ctx.balancer.reload_config(ReloadConfig(response=None), save_globals=save_globals)
    time.sleep(2)
    check_request(ctx, http.request.get(), 'aaa' if save_globals else '')
    assert ctx.get_worker_count() == (3 if save_globals else 1)
