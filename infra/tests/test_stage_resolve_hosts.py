# -*- coding: utf-8 -*-

import mock
from infra.rtc.janitor.fsm_stages.resolve_hosts import resolve_hosts_stage
from data_host_resolver_cache import resolved_hosts


def mock_common_get_hosts_info(c, list):
    for item in list:
        if isinstance(item, int):
            item = str(item)
        if item in resolved_hosts:
            yield resolved_hosts[item]
        else:
            continue


def test_resolve_hosts_stage():

    scenario = mock.Mock()
    scenario.scenario_id = 100500
    scenario.hosts_list = [101027234, 101027407, 101027415]
    scenario.issuer = 'testbot@'
    scenario.labels = {
        'task_name': 'power_off',
        'ref_ticket_key': 'IDDQD-1',
        'ticket_summary': 'Ticket summary',
        'ticket_created_by': 'testuser',
        'comment': "T-T-T-Test Him!!!"
    }
    scenario.name = 'TEST poweroff'
    scenario.scenario_type = 'switch-to-maintenance'
    scenario.script_args = {
        'schedule_type': 'all',
        'responsible': 'testuser'
    }
    scenario.status = 'none'
    scenario.ticket_key = 'IDKFA-1'
    scenario.creation_time = 1582723682.10

    with mock.patch('infra.rtc.janitor.fsm_stages.resolve_hosts.finish_stage', result=True) as mock_finish_stage, \
         mock.patch('infra.rtc.janitor.fsm_stages.resolve_hosts.get_hosts_info', side_effect=mock_common_get_hosts_info):  # noqa

        resolve_hosts_stage(None, scenario)
        mock_finish_stage.assert_called()
        assert scenario.hosts.sort() == [101027234, 101027407, 101027415].sort()

    with mock.patch('infra.rtc.janitor.fsm_stages.resolve_hosts.finish_stage', result=True) as mock_finish_stage, \
         mock.patch('infra.rtc.janitor.fsm_stages.resolve_hosts.get_hosts_info', side_effect=mock_common_get_hosts_info):  # noqa
        scenario.hosts = [{u'inv': 'sas5-4422.search.yandex.net'}, {u'inv': 'sas5-0961.search.yandex.net'}, {u'inv': 'sas5-4419.search.yandex.net'}, {u'inv': '101027415'}]
        resolve_hosts_stage(None, scenario)
        mock_finish_stage.assert_called()
        assert scenario.hosts.sort() == [101027234, 101027407, 101027415].sort()
