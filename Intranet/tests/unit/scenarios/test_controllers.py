import pytest

from unittest.mock import patch, Mock

from startrek_client.exceptions import Conflict

from ok.core.workflow import WorkflowError
from ok.scenarios.controllers import ScenarioController

from tests import factories as f
from tests.utils.mock import AnyOrderList


pytestmark = pytest.mark.django_db


@patch('ok.scenarios.controllers.sync_group_memberships_task.delay')
def test_scenario_create(mocked_sync_group_memberships):
    approvement_data = {
        'text': 'Approvement text',
        'stages': [{'approver': 'robot-ok'}],
    }
    groups = f.GroupFactory.create_batch(2)
    group_urls = [group.url for group in groups]
    data = {
        'slug': 'scenario',
        'name': 'Scenario',
        'responsible_groups': groups,
        'approvement_data': approvement_data,
    }
    initiator = 'initiator'

    scenario = ScenarioController.create(data, initiator)

    assert scenario.author == initiator
    assert scenario.slug == 'scenario'
    assert scenario.name == 'Scenario'
    assert scenario.approvement_data == approvement_data
    assert sorted(scenario.responsible_groups.values_list('url', flat=True)) == sorted(group_urls)
    mocked_sync_group_memberships.assert_called_once_with(AnyOrderList(group_urls))


@patch('ok.scenarios.controllers.get_macro_body', lambda x: 'macro body')
@patch('ok.tracker.macros.macros.create', return_value=Mock(id=100500))
def test_scenario_create_macro(macro_create_mock):
    queue_name = 'QUEUE'
    scenario = f.ScenarioFactory(name='Scenario')
    controller = ScenarioController(scenario)

    macro = controller.create_macro(queue_name)

    assert macro.scenario == scenario
    assert macro.name == 'Scenario'
    assert macro.body == 'macro body'
    assert macro.tracker_queue.name == queue_name
    assert macro.tracker_id == 100500
    macro_create_mock.assert_called_once_with(
        {'queue': queue_name},
        name=macro.name,
        body='macro body',
    )


@patch('ok.tracker.macros.macros.create', side_effect=Conflict(Mock()))
def test_scenario_create_macro_conflict(macro_create_mock):
    queue_name = 'QUEUE'
    scenario = f.ScenarioFactory(name='Scenario')
    controller = ScenarioController(scenario)

    with pytest.raises(WorkflowError, match=r'^macro_already_exists$'):
        controller.create_macro(queue_name)
