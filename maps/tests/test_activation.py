from unittest import mock

from maps.garden.sdk.core import Version
from maps.garden.sdk.ecstatic import TESTING_BRANCH
from maps.garden.sdk import test_utils

from maps.garden.modules.carparks.lib import fill_graph as carparks

from maps.garden.modules.carparks_activation.lib import activation


TEST_REGIONS = [('cis1', 'yandex'), ('tr', 'yandex')]
DATA_VERSION = '2016.01.16t12.13.14'

ECSTATIC_PACKAGE = 'maps.garden.sdk.ecstatic'


def test_fill_graph():
    test_utils.graph.validate_fill_graph_routine(activation.fill_graph)


@mock.patch(
    ECSTATIC_PACKAGE + '.resources.DatasetResource.physically_exists',
    return_value=True)
@mock.patch(
    ECSTATIC_PACKAGE + '.tasks.MoveTask.__call__')
@mock.patch(
    ECSTATIC_PACKAGE + '.tasks.WaitReadyTask.__call__')
@mock.patch(
    ECSTATIC_PACKAGE + '.tasks.ActivateTask.__call__')
def test_module(ecstatic_activate_mock,
                ecstatic_wait_ready_mock,
                ecstatic_move_mock,
                ecstatic_physically_exists_mock,
                environment_settings):
    cook = test_utils.GraphCook(environment_settings)
    carparks.fill_graph(cook.input_builder(), TEST_REGIONS)
    activation.fill_graph(cook.target_builder(), TEST_REGIONS)

    cook.create_input_resource(
        activation.ecstatic.active_resource_name(
            activation.HANDLER_DATASET_RESOURCE,
            TESTING_BRANCH),
        Version(properties={
            activation.DATA_VERSION_PROPERTY: DATA_VERSION}))
    cook.create_input_resource(
        activation.HANDLER_DATASET_RESOURCE,
        Version(properties={
            activation.DATA_VERSION_PROPERTY: DATA_VERSION}))

    cook.create_input_resource(
        activation.ecstatic.active_resource_name(
            activation.RENDERER_DATASET_RESOURCE,
            TESTING_BRANCH),
        Version(properties={
            activation.DATA_VERSION_PROPERTY: DATA_VERSION}))
    cook.create_input_resource(
        activation.RENDERER_DATASET_RESOURCE,
        Version(properties={
            activation.DATA_VERSION_PROPERTY: DATA_VERSION}))

    # Disable subprocesses to track mock calls.
    test_utils.execute(cook)

    dataset = lambda x: x.kwargs['ecstatic_dataset']
    for ecstatic_mock in (ecstatic_move_mock,
                          ecstatic_wait_ready_mock,
                          ecstatic_activate_mock):
        calls = ecstatic_mock.mock_calls
        assert len(calls) == 2
        expected_dataset_names = set(['yandex-maps-carparks-layer:2',
                                      'yandex-maps-carparks-handler-data'])
        call_dataset_names = set(dataset(call).dataset_name for call in calls)
        assert call_dataset_names == expected_dataset_names

        assert dataset(calls[0]).dataset_version == DATA_VERSION
        assert dataset(calls[1]).dataset_version == DATA_VERSION
