# -*- coding: utf-8 -*-
import time
from shlex import split
from subprocess import Popen, PIPE
import balancer.test.util.asserts as asserts
from balancer.test.util.stdlib.multirun import Multirun
from configs import WithPingerConfig, WithoutPingerConfig
from balancer.test.util import process
from balancer.test.util.predef import http
import requests


def count_of_threaded_children(master_pid):
    p1 = Popen(split("ps -eT"), stdout=PIPE)
    p2 = Popen(split("grep " + str(master_pid)), stdin=p1.stdout, stdout=PIPE)
    p3 = Popen(split(r'grep "b-w\|b-p\|b-u"'), stdin=p2.stdout, stdout=PIPE)
    p4 = Popen(split("wc -l"), stdin=p3.stdout, stdout=PIPE)
    out, _ = p4.communicate()
    return int(out)


def check_request(ctx, request, expected_content):
    response = ctx.perform_request(request, port=ctx.balancer.config.port)
    asserts.content(response, expected_content)


def test_pinger_required_starts_pinger(ctx):
    ctx.start_balancer(WithPingerConfig(workers=1, response='aaa'), debug=True)
    time.sleep(2)
    master_pid = process.get_children(ctx.balancer.pid, ctx.logger, recursive=False)[0]

    assert count_of_threaded_children(master_pid) == 2


def test_pinger_doesnt_affect_regualar(ctx):
    ctx.start_balancer(WithPingerConfig(workers=1, response='aaa'), debug=True)
    time.sleep(2)

    for run in Multirun():
        check_request(ctx, http.request.get(), 'aaa')


def test_pinger_doesnt_affect_admin(ctx):
    ctx.start_balancer(WithPingerConfig(workers=1, response='aaa'), debug=True)
    time.sleep(2)
    admin_port = ctx.balancer.config.admin_port

    for run in Multirun():
        requests.get("http://localhost:{0}/admin/events/call/dbg_master_pid".format(admin_port)).text


def test_pinger_notrequired_doesnt_start_pinger(ctx):
    ctx.start_balancer(WithoutPingerConfig(workers=1, response='aaa'), debug=True)
    time.sleep(2)
    master_pid = process.get_children(ctx.balancer.pid, ctx.logger, recursive=False)[0]

    assert count_of_threaded_children(master_pid) == 1
