import pytest
import re
import requests

from .basic_test import BasicTest

from data_types.head_unit import HeadUnit
from data_types.startrek_form import StartrekForm
from data_types.task_result import TaskResult
from data_types.user import User

from maps.automotive.remote_tasks.proto.remote_tasks_pb2 import Task, TaskSpecification, ErrorMessage

import lib.fakeenv as fakeenv
import lib.scheduler as scheduler
import lib.manager as manager
import google.protobuf.text_format as text_format


class TestStartrekHandles(BasicTest):
    def wait_for_comments(self, startrek_form):
        comments = None
        for _ in self.wait_for_startrek():
            comments = fakeenv.startrek_get_comments(startrek_form.issue_key)
            if comments:
                break
        return comments

    def wait_for_comment(self, startrek_form):
        comments = self.wait_for_comments(startrek_form)
        assert len(comments) == 1
        fakeenv.startrek_clear_comments(startrek_form.issue_key)

        return comments[0]

    def wait_for_no_comment(self, startrek_form):
        comments = self.wait_for_comments(startrek_form)
        assert len(comments) == 0

    def test_startrek_workflow_works(self, test_user):
        head_unit = HeadUnit()

        startrek_form = StartrekForm(head_id=head_unit.head_id, user_id=test_user.login)
        scheduler.startrek_add_task(startrek_form) >> 200

        comment = self.wait_for_comment(startrek_form)

        match = re.fullmatch(r"Task \d+ was scheduled for execution", comment)
        assert match, f"Comment doesn't match an expected one: {comment}"

        task_proto_binary = manager.get_available_task(head_unit=head_unit) >> 200

        comment = self.wait_for_comment(startrek_form)
        assert comment == "Task was sent to head unit"

        task = Task()
        assert task.ParseFromString(task_proto_binary)
        spec = TaskSpecification()
        assert spec.ParseFromString(task.spec)
        assert spec.WhichOneof('data') == 'bug_report_data'

        task_result = TaskResult(
            task_id=task.task_id,
        )
        manager.upload_result(task_result) >> 200

        comment = self.wait_for_comment(startrek_form)

        match = re.fullmatch(r"Task completed successfully, get your result here: \n(https?://\S+)", comment)
        assert match, f"Comment doesn't match an expected one: {comment}"

        result_link = match.group(1)
        result = requests.get(
            result_link,
            headers={'Host': scheduler.get_host()},
            cookies={'Session_id': test_user.session_id})
        assert result.status_code == 200
        assert result.content == task_result.result

    def test_head_id_case_insensitive(self, test_user):
        head_unit = HeadUnit()

        head_unit.head_id = head_unit.head_id.upper()
        startrek_form = StartrekForm(head_id=head_unit.head_id, user_id=test_user.login)
        scheduler.startrek_add_task(startrek_form) >> 200

        head_unit.head_id = head_unit.head_id.lower()
        manager.get_available_task(head_unit=head_unit) >> 200

    def test_get_empty_task(self):
        manager.get_available_task(head_unit=HeadUnit()) >> 204

    def test_head_can_get_same_task_twice(self, test_user):
        head_unit = HeadUnit()

        startrek_form = StartrekForm(head_id=head_unit.head_id, user_id=test_user.login)
        scheduler.startrek_add_task(startrek_form) >> 200
        manager.get_available_task(head_unit=head_unit) >> 200

        manager.get_available_task(head_unit=head_unit) >> 200

    def test_user_cant_cancel_task_twice(self, test_user):
        head_unit = HeadUnit()
        startrek_form = StartrekForm(head_id=head_unit.head_id, user_id=test_user.login)
        scheduler.startrek_add_task(startrek_form) >> 200

        scheduler.startrek_cancel_task(startrek_form) >> 200
        error_textual_response = scheduler.startrek_cancel_task(startrek_form) >> 422

        error_message = text_format.Parse(error_textual_response, ErrorMessage())
        assert error_message.text == 'Task is either already canceled or has an uploaded result'

    def test_head_doesnt_get_task_after_cancel(self, test_user):
        head_unit = HeadUnit()
        startrek_form = StartrekForm(head_id=head_unit.head_id, user_id=test_user.login)
        scheduler.startrek_add_task(startrek_form) >> 200

        scheduler.startrek_cancel_task(startrek_form) >> 200

        manager.get_available_task(head_unit=head_unit) >> 204

    def test_user_cant_cancel_missing_task(self, test_user):
        head_unit = HeadUnit()
        startrek_form = StartrekForm(head_id=head_unit.head_id, user_id=test_user.login)

        error_textual_response = scheduler.startrek_cancel_task(startrek_form) >> 422

        error_message = text_format.Parse(error_textual_response, ErrorMessage())
        assert error_message.text == 'Task with this ticket id does not exist'

    def test_wrong_secret_access_denied(self, test_user):
        wrong_secret = 'terribly_wrong_startrek_secret'
        startrek_form = StartrekForm(head_id=HeadUnit().head_id, user_id=test_user.login)

        scheduler.startrek_add_task(startrek_form, st_secret=wrong_secret) >> 401
        scheduler.startrek_cancel_task(startrek_form, st_secret=wrong_secret) >> 401

    def test_wrong_user_access_denied(self):
        startrek_form = StartrekForm(head_id=HeadUnit().head_id, user_id=User().login)

        scheduler.startrek_add_task(startrek_form) >> 401
        scheduler.startrek_cancel_task(startrek_form) >> 401

    def test_synchronizer_disables_on_detach(self, test_user):
        head_unit = HeadUnit()

        scheduler.detach()

        startrek_form = StartrekForm(head_id=head_unit.head_id, user_id=test_user.login)
        scheduler.startrek_add_task(startrek_form) >> 200

        self.wait_for_no_comment(startrek_form)

        scheduler.attach()

        self.wait_for_comment(startrek_form)
