import pytest

from asynctest import patch, Mock, PropertyMock

from payplatform.spirit.match_receipts.lib.base import PLATO, HAHN, Context, Cluster
from payplatform.spirit.match_receipts.lib.tasks import TaskManager, TaskId, Task
from payplatform.spirit.match_receipts.lib.operations import DoNothingOperation, YqlOperation, TransferManagerOperation

from .test_data_fields import test_data_hahn, test_data


@pytest.fixture(autouse=True, scope='module')
def patching():
    with patch.object(Cluster, 'exists', Mock(return_value=False)):
        yield


def test_task_manager_explain_simple():
    task_manager = TaskManager()
    real_explanation = {
        task_id: {
            'dependencies': info['dependencies'],
            'operation': repr(info['operation'])
        }
        for task_id, info in task_manager.explain(PLATO, [test_data_hahn, test_data]).items()
    }
    expected_explanation = {
        TaskId(PLATO, 'test_data'): {
            'dependencies': [TaskId(PLATO, 'test_payments'), TaskId(PLATO, 'test_clients')],
            'operation': 'YqlOperation(cluster=Cluster(plato), table=test_data)'
        },
        TaskId(PLATO, 'test_data_hahn'): {
            'dependencies': [TaskId(HAHN, 'test_data_hahn')],
            'operation': 'TransferManagerOperation(cluster=Cluster(plato), table=test_data_hahn)'
        },
        TaskId(HAHN, 'test_clients'): {
            'dependencies': [TaskId(PLATO, 'test_clients')],
            'operation': 'TransferManagerOperation(cluster=Cluster(hahn), table=test_clients)'
        },
        TaskId(PLATO, 'test_payments'): {
            'dependencies': [],
            'operation': 'DoNothingOperation(cluster=Cluster(plato), table=test_payments)'
        },
        TaskId(HAHN, 'test_payments'): {
            'dependencies': [TaskId(PLATO, 'test_payments')],
            'operation': 'TransferManagerOperation(cluster=Cluster(hahn), table=test_payments)'
        },
        TaskId(PLATO, 'test_clients'): {
            'dependencies': [],
            'operation': 'DoNothingOperation(cluster=Cluster(plato), table=test_clients)'
        },
        TaskId(HAHN, 'test_data_hahn'): {
            'dependencies': [TaskId(HAHN, 'test_payments'), TaskId(HAHN, 'test_clients')],
            'operation': 'YqlOperation(cluster=Cluster(hahn), table=test_data_hahn)'
        }
    }
    assert real_explanation == expected_explanation


@pytest.mark.asyncio
@patch.object(Task, '_collect')
async def test_table_double_call_collect(inner_collect_mock):
    task = Task(DoNothingOperation(test_data), [])
    first_future = await task.collect()
    second_future = await task.collect()
    inner_collect_mock.assert_awaited_once()
    assert first_future == second_future


@patch.object(YqlOperation, 'run')
@patch.object(YqlOperation, 'status', PropertyMock(return_value='COMPLETED'))
@patch.object(YqlOperation, 'is_started', PropertyMock(return_value=True))
def test_task_run_succeeded(yql_run_mock: Mock):
    task_manager = TaskManager()
    task_manager.run(PLATO, [test_data])

    yql_run_mock.assert_called_once()


@patch.object(YqlOperation, 'status', YqlOperation._good_status())
@patch.object(TransferManagerOperation, 'status', TransferManagerOperation._good_status())
def test_simple_pipeline():
    Context.tm_client.add_task.reset_mock()
    Context.yql_client.query.reset_mock()

    TaskManager().run(PLATO, [test_data_hahn, test_data])

    assert Context.tm_client.add_task.call_count == 3
    assert Context.yql_client.query.call_count == 2
