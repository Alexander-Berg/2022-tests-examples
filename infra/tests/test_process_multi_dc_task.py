# -*- coding: utf-8 -*-

import mock
from infra.rtc.janitor.test_utils import monkeypatch_function
from infra.rtc.janitor.scenario import Scenario
import infra.rtc.janitor.fsm_stages.process_multi_dc_task as process_multi_dc_task
from infra.rtc.janitor.fsm_stages.constants import (
    START_SCENARIO_STAGE,
    SET_DUTY_AS_ASSIGNEE,
    ADD_TAGS_TO_ST_STAGE,
    CREATE_SCENARIO_STAGE,
    VALIDATE_DESTINATION_PROJECT_STAGE,
    PROCESS_MULTI_DC_TASK_STAGE,
    RESOLVE_HOSTS_STAGE,
)
import infra.rtc.janitor.fsm_stages.common as fsm_cmmn
from data_host_resolver_cache import resolved_hosts


class Object(object):
    pass


def save(self, client):
    return self.as_dict()


issue_TEST_600 = mock.MagicMock()
issue_TEST_600.key = 'TEST-600'
issue_TEST_600.queue.key = 'TESTQUEUE'
issue_TEST_600.queue.id = '1734'
issue_TEST_600.type.key = 'serviceRequest'
issue_TEST_600.type.id = '42'
issue_TEST_600.tags = ['comp-host:move']
issue_TEST_600._link_list = []
issue_TEST_600.links.get_all.side_effect = lambda: issue_TEST_600._link_list
issue_TEST_600.summary = 'TEST-600'
issue_TEST_600.description = 'Test 600 ticket'

issues = {'TEST-600': issue_TEST_600}
issues_id = 100


def mock_issue_creator(*args, **kwargs):
    global issues_id, issues
    issue = mock.MagicMock()
    issue.queue = kwargs['queue']
    issue.type = kwargs['type']
    issue.tags = kwargs['tags']
    issue.summary = kwargs['summary']
    issue.description = kwargs['description']
    issue._link_list = kwargs.get('links', [])
    issue.links.get_all.side_effect = lambda: issue._link_list
    issue.key = 'SUBTICKET-{}'.format(issues_id)
    issues_id += 1
    issues[issue.key] = issue
    return issue


ctx = Object()
ctx.obj = Object()
ctx.obj.walle_client = mock.MagicMock()
ctx.obj.st_client = mock.MagicMock()
ctx.obj.st_client.__getitem__.side_effect = lambda x: issues[x]
ctx.obj.st_client.issues.create.side_effect = mock_issue_creator
ctx.obj.scenarios_list = []
ctx.obj.hosts_info = resolved_hosts
ctx.obj.st_client.issues = issues


checked_scenario = Scenario.action(
    ctx,
    type='add_hosts',
    ticket_created_by='tester@',
    responsible='tester@',
    comment='multi DC test',
    ticket_key='TEST-600',
    target_project_id='rtc-mtn',
    hosts=[100404032, 100404033, 100404034, 101283388, 101287675, 101283408, 101283432, 101283448]
    )
checked_scenario.save = save


def test_process_multi_dc_task_stage_pass(monkeypatch):
    checked_scenario.labels.update({
        'fsm_stages': ','.join([
            START_SCENARIO_STAGE,
            SET_DUTY_AS_ASSIGNEE,
            ADD_TAGS_TO_ST_STAGE,
            CREATE_SCENARIO_STAGE,
            VALIDATE_DESTINATION_PROJECT_STAGE,
            PROCESS_MULTI_DC_TASK_STAGE
        ]),
        'fsm_prev_stage': RESOLVE_HOSTS_STAGE
    })
    monkeypatch_function(monkeypatch, process_multi_dc_task.finish_stage, module=process_multi_dc_task, side_effect=fsm_cmmn.finish_stage)
    monkeypatch_function(monkeypatch, process_multi_dc_task.render_template, module=process_multi_dc_task, return_value=True)
    process_multi_dc_task.process_multi_dc_task_stage(ctx.obj, checked_scenario)
    process_multi_dc_task.finish_stage.assert_called()
    process_multi_dc_task.render_template.assert_not_called()
    assert checked_scenario.fsm_curr_stage == VALIDATE_DESTINATION_PROJECT_STAGE
    assert checked_scenario.fsm_prev_stage == PROCESS_MULTI_DC_TASK_STAGE


def test_process_multi_dc_task_stage_split(monkeypatch):
    checked_scenario.labels.update({
        'fsm_stages': ','.join([
            START_SCENARIO_STAGE,
            SET_DUTY_AS_ASSIGNEE,
            ADD_TAGS_TO_ST_STAGE,
            CREATE_SCENARIO_STAGE,
            VALIDATE_DESTINATION_PROJECT_STAGE,
            PROCESS_MULTI_DC_TASK_STAGE
        ]),
        'fsm_prev_stage': RESOLVE_HOSTS_STAGE
    })
    returned_issue = mock.Mock()
    returned_issue.key = 'SUBTICKET-001'

    checked_scenario.script_args['target_project_id'] = 'rtc-[dc]-test'
    monkeypatch_function(monkeypatch, process_multi_dc_task.terminate_fsm, module=process_multi_dc_task, side_effect=fsm_cmmn.terminate_fsm)
    monkeypatch_function(monkeypatch, process_multi_dc_task.render_template, module=process_multi_dc_task, return_value=True)
    monkeypatch_function(monkeypatch, process_multi_dc_task.st_create_ticket, module=process_multi_dc_task, side_effect=mock_issue_creator)
    monkeypatch_function(monkeypatch, process_multi_dc_task.st_transistion, module=process_multi_dc_task, return_value=True)
    process_multi_dc_task.process_multi_dc_task_stage(ctx.obj, checked_scenario)
    process_multi_dc_task.terminate_fsm.assert_called()
    process_multi_dc_task.render_template.assert_called()
    process_multi_dc_task.st_create_ticket.assert_called()
    assert checked_scenario.fsm_curr_stage == ''
    assert checked_scenario.fsm_prev_stage == PROCESS_MULTI_DC_TASK_STAGE
    assert process_multi_dc_task.st_create_ticket.call_count == 2
    tags = [sorted(c[1]['tags']) for c in process_multi_dc_task.st_create_ticket.call_args_list]
    assert sorted(tags) == [
        ['comp-host:move', 'janitor_for_processing', 'janitor_processed:subticket', 'janitor_processed_dc:sas'],
        ['comp-host:move', 'janitor_for_processing', 'janitor_processed:subticket', 'janitor_processed_dc:vla']
    ]
    assert process_multi_dc_task.st_transistion.call_count == 2
    data = {}
    for c in process_multi_dc_task.render_template.call_args_list:
        if 'hosts' in c.kwargs:
            data[c.kwargs['DC']] = sorted(c.kwargs['hosts'])
    assert data == {'sas': ['100404032', '100404033', '100404034'], 'vla': ['101283388', '101283408', '101283432', '101283448', '101287675']}


def test_process_multi_dc_task_stage_split_partial(monkeypatch):
    checked_scenario.labels.update({
        'fsm_stages': ','.join([
            START_SCENARIO_STAGE,
            SET_DUTY_AS_ASSIGNEE,
            ADD_TAGS_TO_ST_STAGE,
            CREATE_SCENARIO_STAGE,
            VALIDATE_DESTINATION_PROJECT_STAGE,
            PROCESS_MULTI_DC_TASK_STAGE
        ]),
        'fsm_prev_stage': RESOLVE_HOSTS_STAGE
    })

    checked_scenario.script_args['target_project_id'] = 'rtc-[dc]-test'
    link = mock.Mock()
    link.direction = 'depended'
    link.object = mock_issue_creator(
        description=True,
        links=[{'issue': 'TEST-600', 'relationship': 'is dependent by'}],
        queue='TESTQUEUE',
        summary=True,
        tags=['comp-host:move', 'janitor_for_processing', 'janitor_processed:subticket', 'janitor_processed_dc:sas'],
        type=42
    )
    issue_TEST_600._link_list = [link]
    monkeypatch_function(monkeypatch, process_multi_dc_task.terminate_fsm, module=process_multi_dc_task, side_effect=fsm_cmmn.terminate_fsm)
    monkeypatch_function(monkeypatch, process_multi_dc_task.render_template, module=process_multi_dc_task, return_value=True)
    monkeypatch_function(monkeypatch, process_multi_dc_task.st_create_ticket, module=process_multi_dc_task, side_effect=mock_issue_creator)
    monkeypatch_function(monkeypatch, process_multi_dc_task.st_transistion, module=process_multi_dc_task, return_value=True)
    process_multi_dc_task.process_multi_dc_task_stage(ctx.obj, checked_scenario)
    process_multi_dc_task.terminate_fsm.assert_called()
    process_multi_dc_task.render_template.assert_called()
    process_multi_dc_task.st_create_ticket.assert_called()
    assert checked_scenario.fsm_curr_stage == ''
    assert checked_scenario.fsm_prev_stage == PROCESS_MULTI_DC_TASK_STAGE
    assert process_multi_dc_task.st_create_ticket.call_count == 1
    tags = [sorted(c[1]['tags']) for c in process_multi_dc_task.st_create_ticket.call_args_list]
    assert sorted(tags) == [
        ['comp-host:move', 'janitor_for_processing', 'janitor_processed:subticket', 'janitor_processed_dc:vla']
    ]
    data = {}
    for c in process_multi_dc_task.render_template.call_args_list:
        if 'hosts' in c.kwargs:
            data[c.kwargs['DC']] = sorted(c.kwargs['hosts'])
    assert data == {'vla': ['101283388', '101283408', '101283432', '101283448', '101287675']}


def test_get_processed_dc():
    link = mock.Mock()
    link.direction = 'depended'
    link.object = mock_issue_creator(
        description=True,
        links=[{'issue': 'TEST-600', 'relationship': 'is dependent by'}],
        queue='TESTQUEUE',
        summary=True,
        tags=['comp-host:move', 'janitor_for_processing', 'janitor_processed:subticket', 'janitor_processed_dc:sas'],
        type=42
    )
    issue_TEST_600._link_list = [link]
    assert process_multi_dc_task.get_processed_dc(ctx.obj, 'TEST-600') == ['sas']
