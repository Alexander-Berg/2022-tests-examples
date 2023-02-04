# -*- coding: utf-8 -*-

import mock
from infra.rtc.janitor.test_utils import monkeypatch_function
from infra.rtc.janitor.scenario import Scenario
import infra.rtc.janitor.fsm_stages.add_preorder as add_preorder
import infra.rtc.janitor.fsm_stages.common as fsm_cmmn
from infra.rtc.janitor.fsm_stages.constants import (
    ADD_PREORDER_STAGE,
    VALIDATE_DESTINATION_PROJECT_STAGE
)


class Object(object):
    pass


def save(self, client):
    return self.as_dict()


ctx = Object()
ctx.obj = Object()
ctx.obj.bot_client = mock.Mock()
ctx.obj.bot_client.get_preorder_info = mock.Mock()
ctx.obj.walle_client = mock.Mock()
ctx.obj.walle_client.add_preorder = mock.Mock()
ctx.obj.st_client = mock.Mock()
ctx.obj.dry_run = False


def create_mock_scenario(data):
    res = Scenario.action(
        ctx,
        **parsed_ticket
    )
    res.labels.update({
        'fsm_stages': ADD_PREORDER_STAGE,
        'fsm_prev_stage': VALIDATE_DESTINATION_PROJECT_STAGE
    })
    res.save = save
    return res


parsed_ticket = {
    'preorder_id': '8803',
    'ticket_created_by': 'sereglond',
    'ticket_creation_date': '2020-05-13',
    'ticket_key': 'RUNTIMECLOUD-16334',
    'ticket_summary': 'Ввод хостов из предзаказа в rtc-mtn от 13.05.2020',
    'type': 'preorder_add_hosts',
    'target_project_id': 'rtc-mtn',
    'whole_preorder': True
}


def test_add_preorder_notwhole(monkeypatch):
    parsed_ticket['whole_preorder'] = False
    preorder_info_test = {
        "status_id": 2,
        "status_code": "commited",
        "status_display": "Ждет подтверждения".decode('utf-8'),
        "ticket_id": "DISPENSERREQ-3358",
        }
    ctx.obj.bot_client.get_preorder_info = mock.Mock(return_value=preorder_info_test)
    monkeypatch_function(monkeypatch, add_preorder.finish_stage, module=add_preorder, side_effect=fsm_cmmn.finish_stage)
    monkeypatch_function(monkeypatch, add_preorder.render_template, module=add_preorder, return_value=True)
    monkeypatch_function(monkeypatch, add_preorder.st_transistion, module=add_preorder, return_value=True)
    add_preorder.add_preorder_stage(ctx.obj, create_mock_scenario(parsed_ticket))
    add_preorder.finish_stage.assert_called()
    add_preorder.render_template.assert_not_called()
    assert add_preorder.st_transistion.call_count == 2
    st_transistion_call_args_list = add_preorder.st_transistion.call_args_list
    assert [call for call in st_transistion_call_args_list if call.kwargs['state'] == 'readyForDev']
    assert [call for call in st_transistion_call_args_list if call.kwargs['state'] == 'inProgress']


def test_add_preorder_notready(monkeypatch):
    parsed_ticket['whole_preorder'] = True
    preorder_info_test = {
        "status_id": 2,
        "status_code": "commited",
        "status_display": "Ждет подтверждения".decode('utf-8'),
        "ticket_id": "DISPENSERREQ-3358",
        }
    ctx.obj.bot_client.get_preorder_info = mock.Mock(return_value=preorder_info_test)
    monkeypatch_function(monkeypatch, add_preorder.finish_stage, module=add_preorder, side_effect=fsm_cmmn.finish_stage)
    monkeypatch_function(monkeypatch, add_preorder.render_template, module=add_preorder, return_value=True)
    monkeypatch_function(monkeypatch, add_preorder.st_transistion, module=add_preorder, return_value=True)
    add_preorder.add_preorder_stage(ctx.obj, create_mock_scenario(parsed_ticket))
    add_preorder.finish_stage.assert_called()
    add_preorder.render_template.assert_called()
    assert add_preorder.render_template.call_args.args[0] == 'add_preorder_message.jinja'
    assert add_preorder.render_template.call_args.kwargs['text_case'] == 'PREORDER_NOT_READY'
    add_preorder.st_transistion.assert_called()
    assert add_preorder.st_transistion.call_args.kwargs['resolution'] == 'invalid'
    assert add_preorder.st_transistion.call_args.kwargs['state'] == 'closed'


def test_add_preorder_added(monkeypatch):
    parsed_ticket['whole_preorder'] = True
    preorder_info_test = {
        "status_id": 3,
        "status_code": "approved",
        "status_display": "В закупке".decode('utf-8'),
        "ticket_id": "DISPENSERREQ-3358",
        }
    ctx.obj.bot_client.get_preorder_info = mock.Mock(return_value=preorder_info_test)
    monkeypatch_function(monkeypatch, add_preorder.finish_stage, module=add_preorder, side_effect=fsm_cmmn.finish_stage)
    monkeypatch_function(monkeypatch, add_preorder.render_template, module=add_preorder, return_value=True)
    monkeypatch_function(monkeypatch, add_preorder.st_transistion, module=add_preorder, return_value=True)
    add_preorder.add_preorder_stage(ctx.obj, create_mock_scenario(parsed_ticket))
    add_preorder.finish_stage.assert_called()
    add_preorder.render_template.assert_called()
    assert add_preorder.render_template.call_args.args[0] == 'add_preorder_message.jinja'
    assert add_preorder.render_template.call_args.kwargs['text_case'] == 'PREORDER_ADDED'
    assert add_preorder.st_transistion.call_count == 2
    st_transistion_call_args_list = add_preorder.st_transistion.call_args_list
    assert [call for call in st_transistion_call_args_list if call.kwargs['state'] == 'readyForDev']
    assert [call for call in st_transistion_call_args_list if call.kwargs['state'] == 'inProgress']
