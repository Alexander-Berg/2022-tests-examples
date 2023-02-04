import pytest

from .basic_test import BasicTest

from data_types.head_unit import HeadUnit
from data_types.task_result import TaskResult

from maps.automotive.remote_tasks.proto.remote_tasks_pb2 import TaskSpecification, ErrorMessage

import lib.scheduler as scheduler
import lib.manager as manager
import google.protobuf.text_format as text_format


class TestResultUploading(BasicTest):
    @staticmethod
    def create_task(user):
        head_unit = HeadUnit()
        spec = TaskSpecification()
        spec.bug_report_data.CopyFrom(TaskSpecification.BugReportData())
        task_id = int(scheduler.add_task(head_unit.head_id, spec.SerializeToString(), user) >> 200)
        return task_id

    @pytest.mark.parametrize("is_error", [True, False])
    def test_can_upload_result_and_get_it(self, is_error, test_user):
        task_id = TestResultUploading.create_task(test_user)

        task_result = TaskResult(
            task_id=task_id,
            error=is_error,
        )
        manager.upload_result(task_result) >> 200
        if not is_error:
            received_task_result = scheduler.get_result(task_id=task_id, user=test_user) >> 200
            assert received_task_result == task_result.result

        original_result = task_result.result
        task_result.result = TaskResult.result()

        manager.upload_result(task_result) >> 422
        task_result.error = not is_error
        manager.upload_result(task_result) >> 422

        if not is_error:
            received_task_result = scheduler.get_result(task_id=task_id, user=test_user) >> 200
            assert received_task_result == original_result

    @pytest.mark.parametrize("is_error", [True, False])
    def test_cant_upload_result_for_missing_task(self, is_error):
        error_textual_response = manager.upload_result(TaskResult(
            task_id=TaskResult.task_id(),
            error=is_error,
        )) >> 422

        error_message = ErrorMessage()
        error_message.ParseFromString(error_textual_response)
        assert error_message.text == "Task with this ID does not exist"

    @pytest.mark.parametrize("is_error", [True, False])
    def test_can_upload_and_get_canceled_task_result(self, is_error, test_user):
        task_id = TestResultUploading.create_task(test_user)
        scheduler.cancel_task(task_id, user=test_user) >> 200

        error_textual_response = scheduler.get_result(task_id=task_id, user=test_user) >> 422

        error_message = text_format.Parse(error_textual_response, ErrorMessage())
        assert error_message.text == "There is no result for this task"

        task_result = TaskResult(
            task_id=task_id,
            error=is_error,
        )
        manager.upload_result(task_result) >> 200
        if not is_error:
            received_task_result = scheduler.get_result(task_id=task_id, user=test_user) >> 200
            assert received_task_result == task_result.result

    def test_get_missing_task_result_fails(self, test_user):
        error_textual_response = scheduler.get_result(task_id=TaskResult.task_id(), user=test_user) >> 422

        error_message = text_format.Parse(error_textual_response, ErrorMessage())
        assert error_message.text == "There is no result for this task"

    @pytest.mark.parametrize("is_error", [True, False])
    def test_can_upload_big_result(self, is_error, test_user):
        BIG_RESULT_LEGNTH = 3 * 1024 * 1024
        task_id = TestResultUploading.create_task(test_user)
        manager.upload_result(TaskResult(
            task_id=task_id,
            result=TaskResult.result(length=BIG_RESULT_LEGNTH),
            error=is_error,
        )) >> 200
