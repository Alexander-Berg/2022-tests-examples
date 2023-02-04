import pytest

from .basic_test import BasicTest

from data_types.head_unit import HeadUnit
from data_types.user import User

from maps.automotive.remote_tasks.proto.remote_tasks_pb2 import Task, TaskSpecification

import lib.scheduler as scheduler
import lib.manager as manager
import lib.fakeenv as fakeenv


class TestUserHandles(BasicTest):
    def test_cli_add_works(self, test_user):
        head_unit = HeadUnit()
        spec = TaskSpecification()
        spec.bug_report_data.CopyFrom(TaskSpecification.BugReportData())

        scheduler.add_task(head_unit.head_id, spec.SerializeToString(), test_user) >> 200

        task_proto_binary = manager.get_available_task(head_unit=head_unit) >> 200
        task = Task()
        assert task.ParseFromString(task_proto_binary)
        spec = TaskSpecification()
        assert spec.ParseFromString(task.spec)
        assert spec.WhichOneof('data') == 'bug_report_data'

    def test_cli_cancel_works(self, test_user):
        head_unit = HeadUnit()
        spec = TaskSpecification()
        spec.bug_report_data.CopyFrom(TaskSpecification.BugReportData())
        task_id = int(scheduler.add_task(head_unit.head_id, spec.SerializeToString(), test_user) >> 200)

        scheduler.cancel_task(task_id, test_user) >> 200

        manager.get_available_task(head_unit=head_unit) >> 204

    def test_unregistered_users_are_restricted(self):
        test_user = User()

        head_unit = HeadUnit()
        spec = TaskSpecification()
        spec.bug_report_data.CopyFrom(TaskSpecification.BugReportData())

        scheduler.add_task(head_unit.head_id, spec.SerializeToString(), test_user) >> 401
        scheduler.cancel_task(task_id=42, user=test_user) >> 401
        scheduler.get_result(task_id=42, user=test_user) >> 401
        scheduler.get_file(file_id="abcd", user=test_user) >> 401

    def test_users_without_idm_role_are_restricted(self):
        test_user = User()
        fakeenv.add_user(test_user)

        head_unit = HeadUnit()
        spec = TaskSpecification()
        spec.bug_report_data.CopyFrom(TaskSpecification.BugReportData())

        scheduler.add_task(head_unit.head_id, spec.SerializeToString(), test_user) >> 401
        scheduler.cancel_task(task_id=42, user=test_user) >> 401
        scheduler.get_result(task_id=42, user=test_user) >> 401
        scheduler.get_file(file_id="abcd", user=test_user) >> 401
