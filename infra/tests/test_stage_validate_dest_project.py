# -*- coding: utf-8 -*-

import mock
from infra.rtc.janitor.fsm_stages.validate_destination_project import validate_destination_project_stage
from infra.rtc.janitor.scenario import Scenario


patchbase = 'infra.rtc.janitor.fsm_stages.validate_destination_project'


def test_validate_destination_project_qloud_int():

    context = mock.Mock()
    context.qloud_client.get_segments.return_value = [
        "browser",
        "common",
        "zen"
        ]

    scenario = Scenario.action(
        context,
        type='add_hosts',
        ticket_created_by='testbot@',
        comment="T-T-T-Test Him!!!",
        ticket_key='IDKFA-1',
        ticket_summary='Ticket summary',
        target_project_id="qloud.zen",
        responsible='testuser@',
        hosts=[101027234, 101027407, 101027415]
    )

    with mock.patch('{}.{}'.format(patchbase, 'finish_stage', return_value=True)) as mock_finish_stage, \
         mock.patch('{}.{}'.format(patchbase, 'render_template', return_value='Templated')), \
         mock.patch('{}.{}'.format(patchbase, 'st_transistion')):  # noqa
        scenario.hosts = [{u'inv': 'sas5-4422.search.yandex.net'}, {u'inv': 'sas5-0961.search.yandex.net'}, {u'inv': 'sas5-4419.search.yandex.net'}, {u'inv': '101027415'}]
        validate_destination_project_stage(context, scenario)
        mock_finish_stage.assert_called()
        assert scenario.scenario_type == 'hosts-transfer'
        assert scenario.script_args['target_hardware_segment'] == 'pre.zen'
        assert 'target_project_id' not in scenario.script_args


def test_validate_destination_project_qloud_ext():

    context = mock.Mock()
    context.qloud_client.get_segments.return_value = [
        "browser",
        "common",
        "zen"
        ]

    scenario = Scenario.action(
        context,
        type='add_hosts',
        ticket_created_by='testbot@',
        comment="T-T-T-Test Him!!!",
        ticket_key='IDKFA-1',
        ticket_summary='Ticket summary',
        target_project_id="qloud-ext.zen",
        responsible='testuser@',
        hosts=[101027234, 101027407, 101027415]
    )

    with mock.patch('{}.{}'.format(patchbase, 'finish_stage', return_value=True)) as mock_finish_stage, \
         mock.patch('{}.{}'.format(patchbase, 'render_template', return_value='Templated')), \
         mock.patch('{}.{}'.format(patchbase, 'st_transistion')):  # noqa
        scenario.hosts = [{u'inv': 'sas5-4422.search.yandex.net'}, {u'inv': 'sas5-0961.search.yandex.net'}, {u'inv': 'sas5-4419.search.yandex.net'}, {u'inv': '101027415'}]
        validate_destination_project_stage(context, scenario)
        mock_finish_stage.assert_called()
        assert scenario.scenario_type == 'hosts-transfer'
        assert scenario.script_args['target_hardware_segment'] == 'ext.zen'
        assert 'target_project_id' not in scenario.script_args


def test_validate_destination_project_qloud_noinst():

    context = mock.Mock()
    context.qloud_client.get_segments.return_value = [
        "browser",
        "common",
        "zen"
        ]

    scenario = Scenario.action(
        context,
        type='add_hosts',
        ticket_created_by='testbot@',
        comment="T-T-T-Test Him!!!",
        ticket_key='IDKFA-1',
        ticket_summary='Ticket summary',
        target_project_id="qloud-pre.zen",
        responsible='testuser@',
        hosts=[101027234, 101027407, 101027415]
    )

    with mock.patch('{}.{}'.format(patchbase, 'terminate_fsm'), return_value=True) as mock_terminate_fsm, \
         mock.patch('{}.{}'.format(patchbase, 'render_template'), return_value='Templated') as mock_render, \
         mock.patch('{}.{}'.format(patchbase, 'remove_st_janitor_processing')) as mock_remove_st_janitor_processing, \
         mock.patch('{}.{}'.format(patchbase, 'st_transistion')):  # noqa
        scenario.hosts = [{u'inv': 'sas5-4422.search.yandex.net'}, {u'inv': 'sas5-0961.search.yandex.net'}, {u'inv': 'sas5-4419.search.yandex.net'}, {u'inv': '101027415'}]
        validate_destination_project_stage(context, scenario)
        mock_terminate_fsm.assert_called()
        mock_remove_st_janitor_processing.assert_called()
        mock_render.assert_called_with(
            'qloud_installation_not_exist.jinja',
            qloud_installation='qloud-pre',
            qloud_installations_aval=['qloud', 'qloud-ext']
            )
        assert scenario.scenario_type == 'hosts-transfer'


def test_validate_destination_project_qloud_nosrv():

    context = mock.Mock()
    context.qloud_client.get_segments.return_value = [
        "browser",
        "common",
        "zen"
        ]

    scenario = Scenario.action(
        context,
        type='add_hosts',
        ticket_created_by='testbot@',
        comment="T-T-T-Test Him!!!",
        ticket_key='IDKFA-1',
        ticket_summary='Ticket summary',
        target_project_id="qloud.nothing",
        responsible='testuser@',
        hosts=[101027234, 101027407, 101027415]
    )

    with mock.patch('{}.{}'.format(patchbase, 'terminate_fsm', return_value=True)) as mock_terminate_fsm, \
         mock.patch('{}.{}'.format(patchbase, 'render_template', return_value='Templated')) as mock_render, \
         mock.patch('{}.{}'.format(patchbase, 'st_transistion')):  # noqa
        scenario.hosts = [{u'inv': 'sas5-4422.search.yandex.net'}, {u'inv': 'sas5-0961.search.yandex.net'}, {u'inv': 'sas5-4419.search.yandex.net'}, {u'inv': '101027415'}]
        validate_destination_project_stage(context, scenario)
        mock_terminate_fsm.assert_called()
        mock_render.assert_called_with(
            'qloud_segment_not_exist.jinja',
            qloud_installation='qloud',
            qloud_segment='nothing'
            )
        assert scenario.scenario_type == 'hosts-transfer'
