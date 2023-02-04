# -*- coding: utf-8 -*-

import datetime

import hamcrest
import pytest

import btestlib.utils as utils
from balance import balance_api as api
from balance import balance_steps as steps
from btestlib.constants import Services
from btestlib.environments import BalanceHosts as hosts

ADFOX_TOKEN = Services.ADFOX.token


def change_task_status_to_new(task_name):
    steps.CloseMonth.set_state_force(task_name, 'new_unopenable')


@pytest.mark.skipif(datetime.datetime.now().day == 1, reason="Graph is in incorrect state on 1st day of month")
@pytest.mark.no_parallel
@pytest.mark.ignore_hosts(hosts.PT, hosts.PTY, hosts.PTA)
@pytest.mark.parametrize('params', [
    # pytest.mark.smoke(
        {'service_token': ADFOX_TOKEN, 'task_name': 'adfox_alignment', 'expected_exception': False}
    # )
    ,
    # pytest.mark.skip: Пока не будет нормального графа
    # {'service_token': HEALTH_TOKEN, 'task_name': 'health_alignment', 'expected_exception': False},
    # {'service_token': '', 'task_name': 'health_alignment', 'expected_exception': 'Invalid token'},
    # {'service_token': 'high_edison_3b4b6e3fe99987fdf249729dacdd329', 'task_name': 'health_alignment',
    #  'expected_exception': 'Invalid token'},
])
def resolve_mnclose_task(params):
    change_task_status_to_new(params['task_name'])
    state_before = steps.CloseMonth.get_state(params['task_name'])
    utils.check_that(state_before['status'], hamcrest.equal_to('new_unopenable'))
    try:
        steps.CloseMonth.resolve_mnclose_status_for_service(service_token=params['service_token'], task_name=params['task_name'])
        state_after = steps.CloseMonth.get_state(params['task_name'])
        utils.check_that(state_after['status'], hamcrest.equal_to('resolved'))
        utils.check_that(params['expected_exception'], hamcrest.equal_to(False))
    except Exception, exc:
        if params['expected_exception'] == 'Invalid token':
            utils.check_that('Invalid token', hamcrest.equal_to(steps.CommonSteps.get_exception_code(exc, 'contents')))
        else:
            utils.check_that(steps.CommonSteps.get_exception_code(exc, 'contents'), hamcrest.starts_with(params['expected_exception']))
