# -*- coding: utf-8 -*-

import mock
# from infra.rtc.janitor import common
from infra.rtc.janitor.scenario import Scenario
from infra.rtc.janitor.fsm_stages.check_intersept_started import check_intersept_started_stage
from infra.rtc.janitor.fsm_stages.constants import CHECK_TO_MNT_COMPLETE, START_SCENARIO_STAGE, CHECK_TASK_COUNT_STAGE, CHECK_INTERSEPT_STARTED_STAGE, CREATE_SCENARIO_STAGE
from test_testdata_scenario_desc import create_scenario_dsc
from data_host_resolver_cache import resolved_hosts


class Object(object):
    pass


ctx = Object()
ctx.obj = Object()
ctx.obj.walle_client = mock.Mock()
ctx.obj.st_client = mock.Mock()
ctx.obj.hosts_info = resolved_hosts
ctx.obj.scenarios_list = [
    Scenario(**create_scenario_dsc(id=3651, type='hosts-add', hosts=[1585580302], status='finished')),
    Scenario(**create_scenario_dsc(id=1003, type='janitor_add_hosts', hosts=[100404031, 100404032, 100404033], status='started')),
    Scenario(**create_scenario_dsc(id=2003, type='janitor_power_off', hosts=[100404032], status='created')),
    Scenario(**create_scenario_dsc(id=2004, type='janitor_power_off', hosts=[100404032, 100404033, 100404034], status='started')),
    ]


def mock_common_get_hosts_info(c, list):
    for item in list:
        if isinstance(item, int):
            item = str(item)
        if item in resolved_hosts:
            yield resolved_hosts[item]
        else:
            continue


def test_resolve_hosts_stage():
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
    checked_scenario.labels.update({
        'fsm_stages': ','.join([
            CHECK_TO_MNT_COMPLETE,
            START_SCENARIO_STAGE,
            CHECK_TASK_COUNT_STAGE,
            CHECK_INTERSEPT_STARTED_STAGE
        ]),
        'fsm_prev_stage': CREATE_SCENARIO_STAGE,
    })
    with mock.patch('infra.rtc.janitor.fsm_stages.check_intersept_started.finish_stage', result=True) as mock_finish_stage, \
         mock.patch('infra.rtc.janitor.fsm_stages.check_intersept_started.retry_stage', result=True) as mock_retry_stage, \
         mock.patch('infra.rtc.janitor.fsm_stages.check_intersept_started.get_hosts_info', side_effect=mock_common_get_hosts_info), \
         mock.patch('infra.rtc.janitor.fsm_stages.check_intersept_started.render_template', result=True) as mock_render_template, \
         mock.patch('infra.rtc.janitor.fsm_stages.check_intersept_started.st_transistion', result=True):  # noqa
        check_intersept_started_stage(ctx.obj, checked_scenario)
        mock_finish_stage.assert_not_called()
        mock_retry_stage.assert_called()
        mock_render_template.assert_called()
        called_scenarios = mock_render_template.call_args
        assert called_scenarios[0] == ('power_off_message.jinja',)
        called_scenario_ids = [s['scenario'].scenario_id for s in called_scenarios[1]['intersect_scenarios']]
        assert called_scenario_ids == [1003, 2004]
