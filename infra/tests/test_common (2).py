# -*- coding: utf-8 -*-

import mock
from infra.rtc.janitor import common
from infra.rtc.janitor.scenario import Scenario
from infra.rtc.janitor.constants import PROG_NAME, PROG_VER
from test_testdata_scenario_desc import create_scenario_dsc


class Object(object):
    pass


ctx = Object()
ctx.obj = Object()
ctx.obj.walle_client = mock.Mock()

ctx.obj.scenarios_list = [
    Scenario(**create_scenario_dsc(id=3651, type='hosts-add', hosts=[100404032], status='finished')),
    Scenario(**create_scenario_dsc(id=3652, type='hosts-add', hosts=[100404032, 100404094], status='canceled')),
    Scenario(**create_scenario_dsc(id=3654, type='hosts-add', hosts=[100404032, 100404034, 100404004], status='created', labels={'ticket_created_by': 'labeluser@'})),
    Scenario(**create_scenario_dsc(id=3659, type='hosts-add', hosts=[100404032], status='started')),

    Scenario(**create_scenario_dsc(id=1001, type='janitor_add_hosts', hosts=[100404032], status='finished')),
    Scenario(**create_scenario_dsc(id=1002, type='janitor_add_hosts', hosts=[100404032], status='cancelled')),
    Scenario(**create_scenario_dsc(id=1003, type='janitor_add_hosts', hosts=[100404032], status='created')),
    Scenario(**create_scenario_dsc(id=1004, type='janitor_add_hosts', hosts=[100404032], status='started')),

    Scenario(**create_scenario_dsc(id=2001, type='janitor_power_off', hosts=[100404032], status='finished')),
    Scenario(**create_scenario_dsc(id=2002, type='janitor_power_off', hosts=[100404032], status='cancelled')),
    Scenario(**create_scenario_dsc(id=2003, type='janitor_power_off', hosts=[100404032], status='created', issuer='labeluser@')),
    Scenario(**create_scenario_dsc(id=2004, type='janitor_power_off', hosts=[100404032], status='started')),
    ]
checked_scenario = Scenario.action(
    ctx,
    type='power_off',
    ref_ticket_key='ITDC-600',
    ticket_created_by='tester@',
    responsible='tester@',
    comment='Check_test',
    ticket_key='TEST-600',
    hosts=[100404032, 100404033, 100404034, 100404035]
    )


def test_scenario_default_labels():
    assert checked_scenario.default_scenario_labels == {'source': PROG_NAME, 'source_ver': PROG_VER}


def test_find_intersections_std():
    i = common.find_intersections_by_hosts(ctx.obj, checked_scenario)
    res = {r['scenario'].scenario_id: r['hosts_ids'] for r in i}
    assert res == {
        1003: [100404032],
        1004: [100404032],
        2003: [100404032],
        2004: [100404032],
        3654: [100404032, 100404034],
        3659: [100404032]
    }


def test_find_intersections_labels():
    i = common.find_intersections_by_hosts(ctx.obj, checked_scenario, janitor_owned=True)
    scenario_ids = [r['scenario'].scenario_id for r in i]
    assert sorted(scenario_ids) == [1003, 1004, 2003, 2004]
    i = common.find_intersections_by_hosts(ctx.obj, checked_scenario, labels={'ticket_created_by': 'labeluser@'})
    scenario_ids = [r['scenario'].scenario_id for r in i]
    assert sorted(scenario_ids) == [2003, 3654]
    i = common.find_intersections_by_hosts(ctx.obj, checked_scenario, janitor_owned=True, labels={'ticket_created_by': 'labeluser@'})
    scenario_ids = [r['scenario'].scenario_id for r in i]
    assert sorted(scenario_ids) == [2003]


def test_find_intersections_status():
    i = common.find_intersections_by_hosts(ctx.obj, checked_scenario, status=('started',))
    scenario_ids = [r['scenario'].scenario_id for r in i]
    assert sorted(scenario_ids) == [1004, 2004, 3659]
    i = common.find_intersections_by_hosts(ctx.obj, checked_scenario, status=('started',), janitor_owned=True)
    scenario_ids = [r['scenario'].scenario_id for r in i]
    assert sorted(scenario_ids) == [1004, 2004]
