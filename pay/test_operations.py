from payplatform.spirit.match_receipts.lib.base import PLATO, Context
from payplatform.spirit.match_receipts.lib.operations import YqlOperation, TransferManagerOperation, Operation
from .test_data_fields import test_data, test_data_hahn, TestData

import pytest
from unittest.mock import Mock, patch, PropertyMock

from typing import Type


def assert_operation_state(operation: Operation,
                           is_started=False, is_in_progress=False, is_success=False, is_failed=False):
    assert operation.is_started == is_started
    assert operation.is_in_progress == is_in_progress
    assert operation.is_success == is_success
    assert operation.is_failed == is_failed


@pytest.mark.parametrize("operation_type,arguments", [
    (YqlOperation, (test_data,)),
    (TransferManagerOperation, (test_data_hahn, PLATO)),
])
def test_lifecycle(operation_type: Type[Operation], arguments):
    with patch.object(operation_type, 'status', new_callable=PropertyMock) as mock_status:
        operation = operation_type(*arguments)

        mock_status.return_value = 'my_in_progress_status'
        assert_operation_state(operation)
        operation.run()
        assert_operation_state(operation, is_started=True, is_in_progress=True)

        mock_status.return_value = operation._good_status()
        assert_operation_state(operation, is_started=True, is_success=True)

        mock_status.return_value = next(iter(operation._bad_statuses()))
        assert_operation_state(operation, is_started=True, is_failed=True)


def test_transfer_manager_operation_run():
    mocked_client = Context.tm_client  # type: Mock
    mocked_client.add_task.reset_mock()

    operation = TransferManagerOperation(test_data_hahn, PLATO)
    assert operation.tm_client is None
    assert operation.task_id is None
    operation.run()
    assert operation.tm_client == mocked_client
    assert operation.task_id is not None
    mocked_client.add_task.assert_called_once_with(
        source_cluster='hahn', source_table='//home_hahn/execution_history/time_string/test_data_hahn',
        destination_cluster='plato', destination_table='//home_ofd/execution_history/time_string/test_data_hahn'
    )


@pytest.mark.parametrize('table', [test_data, test_data_hahn])
def test_yql_client_operation_run_success(table: TestData):
    mocked_client = Context.yql_client  # type: Mock
    mocked_client.query.reset_mock()

    operation = YqlOperation(table)
    assert operation.request is None
    operation.run()
    assert operation.request is not None

    cluster = table.__cluster__
    execution_dir = Context.get_execution_dir(cluster)
    mocked_client.query.assert_called_once_with(
        f'{cluster.yql_header} insert into {execution_dir}/{table.short_name} select * from {execution_dir}/test_clients;'
    )
    operation.request.run.assert_called_once()
