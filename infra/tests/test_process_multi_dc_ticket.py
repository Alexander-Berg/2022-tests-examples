# -*- coding: utf-8 -*-

import mock
import pytest
from infra.rtc.janitor.test_utils import monkeypatch_function
from infra.rtc.janitor.scenario import Scenario
import infra.rtc.janitor.fsm_stages.process_multi_dc_ticket as process_multi_dc_ticket
from infra.rtc.janitor.fsm_stages.constants import (REMOVE_PROCESSING_TAG, PROCESS_MULTIDC_TICKET,)


class Object(object):
    pass


ctx = Object()
ctx.obj = Object()
ctx.obj.walle_client = mock.Mock()
ctx.obj.st_client = mock.MagicMock()

issue_TEST_600 = mock.Mock()
issue_TEST_600.key = 'TEST-600'
issue_TEST_600.status.key = 'open'
issue_TEST_600.tags = ['janitor_processed:parent']

issue_TEST_710 = mock.Mock()
issue_TEST_710.key = 'TEST-710'
issue_TEST_710.status.key = 'closed'
issue_TEST_710.tags = ['janitor_processed:subticket']

issue_TEST_720 = mock.Mock()
issue_TEST_720.key = 'TEST-720'
issue_TEST_720.status.key = 'closed'
issue_TEST_720.tags = ['janitor_processed:subticket']


def make_mock_scenario():
    checked_scenario = Scenario.action(
        ctx,
        type='multi_dc_parent_processing',
        ref_ticket_key='ITDC-600',
        ticket_created_by='tester@',
        responsible='tester@',
        comment='Check_test',
        ticket_key='TEST-600',
        hosts=[100404032, 100404033, 100404034, 100404035]
        )
    checked_scenario.labels.update({
        'fsm_stages': ','.join([
            REMOVE_PROCESSING_TAG,
            PROCESS_MULTIDC_TICKET,
        ]),
        # 'fsm_prev_stage': RESOLVE_HOSTS_STAGE,
    })
    return checked_scenario


def getitem(index):
    return {
        'TEST_600': issue_TEST_600,
        'TEST-710': issue_TEST_710,
        'TEST_720': issue_TEST_720,
    }[index]


ctx.obj.st_client.__getitem__.side_effect = getitem


def st_links(*args, **kwargs):
    return [
        {
            'relationship': 'outward',
            'ticket': issue_TEST_710
        },
        {
            'relationship': 'outward',
            'ticket': issue_TEST_720
        }
    ]


def test_skip_process_multi_dc_ticket():
    issue_TEST_600.tags = ['comp-host:move']
    checked_scenario = make_mock_scenario()
    with pytest.raises(Exception):
        process_multi_dc_ticket.process_multi_dc_ticket_stage(ctx.obj, checked_scenario)


def test_closed_process_multi_dc_ticket(monkeypatch):
    issue_TEST_600.tags = ['janitor_processed:parent']
    checked_scenario = make_mock_scenario()
    monkeypatch_function(monkeypatch, process_multi_dc_ticket.st_get_linked_multidc_subtickets, module=process_multi_dc_ticket, side_effect=st_links)
    monkeypatch_function(monkeypatch, process_multi_dc_ticket.render_template, module=process_multi_dc_ticket, return_value="Message")
    monkeypatch_function(monkeypatch, process_multi_dc_ticket.st_transistion, module=process_multi_dc_ticket, return_value=True)
    assert process_multi_dc_ticket.process_multi_dc_ticket_stage(ctx.obj, checked_scenario)
    process_multi_dc_ticket.render_template.assert_called()
    assert process_multi_dc_ticket.render_template.call_args[0][0] == 'multi_dc_subticket_message.jinja'
    process_multi_dc_ticket.st_transistion.assert_called()
    assert process_multi_dc_ticket.st_transistion.call_args[1]['state'] == 'closed'


def test_opened_process_multi_dc_ticket(monkeypatch):
    issue_TEST_600.tags = ['janitor_processed:parent']
    issue_TEST_710.status.key = 'open'
    issue_TEST_720.status.key = 'open'
    checked_scenario = make_mock_scenario()
    monkeypatch_function(monkeypatch, process_multi_dc_ticket.st_get_linked_multidc_subtickets, module=process_multi_dc_ticket, side_effect=st_links)
    monkeypatch_function(monkeypatch, process_multi_dc_ticket.render_template, module=process_multi_dc_ticket, return_value="Message")
    monkeypatch_function(monkeypatch, process_multi_dc_ticket.st_transistion, module=process_multi_dc_ticket, return_value=True)
    assert process_multi_dc_ticket.process_multi_dc_ticket_stage(ctx.obj, checked_scenario)
    process_multi_dc_ticket.render_template.assert_not_called()
    process_multi_dc_ticket.st_transistion.assert_not_called()
