# -*- coding: utf-8 -*-

import mock
from infra.rtc.janitor.scenario import Scenario
from infra.rtc.janitor.fsm_stages.check_task_count import check_task_count_stage
from infra.rtc.janitor.fsm_stages.constants import CHECK_TO_MNT_COMPLETE, START_SCENARIO_STAGE, CHECK_TASK_COUNT_STAGE, CHECK_INTERSEPT_STARTED_STAGE
import infra.rtc.janitor.fsm_stages.common as fsm_cmmn
from test_testdata_scenario_desc import create_scenario_dsc


class Object(object):
    pass


ctx = Object()
ctx.obj = Object()
ctx.obj.walle_client = mock.Mock()
ctx.obj.st_client = mock.Mock()
ctx.obj.scenarios_list = []
ctx.obj.hosts_info = {}

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


def none(*args, **kwargs):
    return


checked_scenario.save = none


def test_stage_overcount_firsttime():
    checked_scenario.labels.update({
        'fsm_stages': ','.join([
            CHECK_TO_MNT_COMPLETE,
            START_SCENARIO_STAGE,
            CHECK_TASK_COUNT_STAGE
        ]),
        'fsm_prev_stage': CHECK_INTERSEPT_STARTED_STAGE
    })
    ctx.obj.scenarios_list = [Scenario(**create_scenario_dsc(id=id, type='janitor_power_off', status='started')) for id in xrange(10)]
    with mock.patch('infra.rtc.janitor.fsm_stages.check_task_count.finish_stage', side_effect=fsm_cmmn.finish_stage) as mock_finish_stage, \
         mock.patch('infra.rtc.janitor.fsm_stages.check_task_count.retry_stage', side_effect=fsm_cmmn.retry_stage) as mock_retry_stage, \
         mock.patch('infra.rtc.janitor.fsm_stages.check_task_count.render_template', result=True) as mock_render_template, \
         mock.patch('infra.rtc.janitor.fsm_stages.check_task_count.st_transistion', result=True):  # noqa
        check_task_count_stage(ctx.obj, checked_scenario)
        mock_finish_stage.assert_not_called()
        mock_retry_stage.assert_called()
        mock_render_template.assert_called()


def test_stage_overcount_retry():
    checked_scenario.labels.update({
        'fsm_stages': ','.join([
            CHECK_TO_MNT_COMPLETE,
            START_SCENARIO_STAGE,
            CHECK_TASK_COUNT_STAGE
        ]),
        'fsm_prev_stage': CHECK_TASK_COUNT_STAGE
    })
    ctx.obj.scenarios_list = [Scenario(**create_scenario_dsc(id=id, type='janitor_power_off', status='started')) for id in xrange(10)]
    with mock.patch('infra.rtc.janitor.fsm_stages.check_task_count.finish_stage', side_effect=fsm_cmmn.finish_stage) as mock_finish_stage, \
         mock.patch('infra.rtc.janitor.fsm_stages.check_task_count.retry_stage', side_effect=fsm_cmmn.retry_stage) as mock_retry_stage, \
         mock.patch('infra.rtc.janitor.fsm_stages.check_task_count.render_template', result=True) as mock_render_template, \
         mock.patch('infra.rtc.janitor.fsm_stages.check_task_count.st_transistion', result=True):  # noqa
        check_task_count_stage(ctx.obj, checked_scenario)
        mock_finish_stage.assert_not_called()
        mock_retry_stage.assert_called()
        mock_render_template.assert_not_called()


def test_stage_ok_retry():
    checked_scenario.labels.update({
        'fsm_stages': ','.join([
            CHECK_TO_MNT_COMPLETE,
            START_SCENARIO_STAGE,
            CHECK_TASK_COUNT_STAGE
        ]),
        'fsm_prev_stage': CHECK_TASK_COUNT_STAGE
    })
    ctx.obj.scenarios_list = [Scenario(**create_scenario_dsc(id=id, type='janitor_power_off', status='created')) for id in xrange(10)]
    with mock.patch('infra.rtc.janitor.fsm_stages.check_task_count.finish_stage', side_effect=fsm_cmmn.finish_stage) as mock_finish_stage, \
         mock.patch('infra.rtc.janitor.fsm_stages.check_task_count.retry_stage', side_effect=fsm_cmmn.retry_stage) as mock_retry_stage, \
         mock.patch('infra.rtc.janitor.fsm_stages.check_task_count.render_template', result=True) as mock_render_template, \
         mock.patch.object(checked_scenario, 'save', side_effect=lambda x: x) as mock_save, \
         mock.patch('infra.rtc.janitor.fsm_stages.check_task_count.st_transistion', result=True):  # noqa
        check_task_count_stage(ctx.obj, checked_scenario)
        mock_save.assert_called()
        mock_finish_stage.assert_called()
        mock_retry_stage.assert_not_called()
        mock_render_template.assert_not_called()
        assert checked_scenario.fsm_stages == [
            CHECK_TO_MNT_COMPLETE,
            START_SCENARIO_STAGE,
            CHECK_TASK_COUNT_STAGE,
            CHECK_INTERSEPT_STARTED_STAGE
        ]
        assert checked_scenario.fsm_prev_stage == CHECK_TASK_COUNT_STAGE


def test_stage_ok_firsttime():
    checked_scenario.labels.update({
        'fsm_stages': ','.join([
            CHECK_TO_MNT_COMPLETE,
            START_SCENARIO_STAGE,
            CHECK_TASK_COUNT_STAGE
        ]),
        'fsm_prev_stage': CHECK_INTERSEPT_STARTED_STAGE
    })
    ctx.obj.scenarios_list = [Scenario(**create_scenario_dsc(id=id, type='janitor_power_off', status='created')) for id in xrange(10)]
    with mock.patch('infra.rtc.janitor.fsm_stages.check_task_count.finish_stage', side_effect=fsm_cmmn.finish_stage) as mock_finish_stage, \
         mock.patch('infra.rtc.janitor.fsm_stages.check_task_count.retry_stage', side_effect=fsm_cmmn.retry_stage) as mock_retry_stage, \
         mock.patch('infra.rtc.janitor.fsm_stages.check_task_count.render_template', result=True) as mock_render_template, \
         mock.patch('infra.rtc.janitor.fsm_stages.check_task_count.st_transistion', result=True):  # noqa
        check_task_count_stage(ctx.obj, checked_scenario)
        mock_finish_stage.assert_called()
        mock_retry_stage.assert_not_called()
        mock_render_template.assert_called()
        called_scenarios = mock_render_template.call_args
        assert called_scenarios[0] == ('power_off_message.jinja',)
        assert checked_scenario.fsm_stages == [
            CHECK_TO_MNT_COMPLETE,
            START_SCENARIO_STAGE
        ]
        assert checked_scenario.fsm_prev_stage == CHECK_TASK_COUNT_STAGE
