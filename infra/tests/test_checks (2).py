# -*- coding: utf-8 -*-

import mock
from infra.rtc.janitor.scenario import Scenario
from infra.rtc.janitor.checks import select_scenarios_overtimed, human_readable_time, send_iteration_status
from test_testdata_scenario_desc import create_scenario_dsc


class Object(object):
    pass


ctx = Object()
ctx.obj = Object()
ctx.obj.counters = {}
ctx.obj.counters['start_time'] = 1000000
ctx.obj.walle_client = mock.Mock()
ctx.obj.juggler_client = mock.Mock()
ctx.obj.juggler_client.send_events_to_juggler = mock.Mock()


def test_select_scenarios_overtimed():
    ctx.obj.scenarios_list = [
        Scenario(**create_scenario_dsc(id=1001, type='janitor_add_hosts', status='finished', ts=1604801)),
        Scenario(**create_scenario_dsc(id=1002, type='janitor_add_hosts', status='cancelled', ts=1604801)),
        Scenario(**create_scenario_dsc(id=1003, type='janitor_add_hosts', status='created', ts=1604801)),
        Scenario(**create_scenario_dsc(id=1004, type='janitor_add_hosts', status='started', ts=1604801)),

        Scenario(**create_scenario_dsc(id=1001, type='janitor_add_hosts', status='finished', ts=1604001)),
        Scenario(**create_scenario_dsc(id=1002, type='janitor_add_hosts', status='cancelled', ts=1604001)),
        Scenario(**create_scenario_dsc(id=1003, type='janitor_add_hosts', status='created', ts=1604001)),
        Scenario(**create_scenario_dsc(id=1004, type='janitor_add_hosts', status='started', ts=1604001)),

        Scenario(**create_scenario_dsc(id=2001, type='janitor_power_off', status='finished', ts=1086400)),
        Scenario(**create_scenario_dsc(id=2002, type='janitor_power_off', status='cancelled', ts=1086400)),
        Scenario(**create_scenario_dsc(id=2003, type='janitor_power_off', status='created', ts=1086400)),
        Scenario(**create_scenario_dsc(id=2004, type='janitor_power_off', status='started', ts=1086400)),

        Scenario(**create_scenario_dsc(id=2001, type='janitor_power_off', status='finished', ts=1086000)),
        Scenario(**create_scenario_dsc(id=2002, type='janitor_power_off', status='cancelled', ts=1086000)),
        Scenario(**create_scenario_dsc(id=2003, type='janitor_power_off', status='created', ts=1086000)),
        Scenario(**create_scenario_dsc(id=2004, type='janitor_power_off', status='started', ts=1086000)),
        ]

    res = list(select_scenarios_overtimed(ctx.obj))
    assert len(res) == 2
    assert [s['scenario'].scenario_id for s in res] == [1004, 2004]
    assert [s['time_limit'] for s in res if s['scenario'].scenario_id == 1004] == [604800]
    assert [s['time_limit'] for s in res if s['scenario'].scenario_id == 2004] == [86400]


def test_human_readable_time():
    assert human_readable_time(604800) == '7d'
    assert human_readable_time(604810) == '7d'
    assert human_readable_time(606880) == '7d 34m'
    assert human_readable_time(608460) == '7d 1h 1m'


def test_send_iteration_status():
    ctx.obj.counters['scenarios_notstarted'] = []
    ctx.obj.counters['scenarios_poweroff_queued'] = []
    ctx.obj.counters['tickets_errors'] = []
    ctx.obj.counters['scenarios_errors'] = []
    ctx.obj.counters['scenarios_overtime'] = [
        {
            'scenario': Scenario(**create_scenario_dsc(id=1001, type='janitor_add_hosts')),
            'time_spent': 606880,
            'time_limit': 604800
        },
        {
            'scenario': Scenario(**create_scenario_dsc(id=1002, type='janitor_power_off')),
            'time_spent': 87400,
            'time_limit': 86400
        },
    ]
    send_iteration_status(ctx.obj)
