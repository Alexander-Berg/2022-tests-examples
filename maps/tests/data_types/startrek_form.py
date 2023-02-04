import rstr
import random
import google.protobuf.text_format as text_format

from data_types.head_unit import HeadUnit
from maps.automotive.remote_tasks.proto.remote_tasks_pb2 import TaskSpecification


class StartrekForm:
    HEX_DIGITS_UPPER = '0123456789ABCDEF'
    HEX_DIGITS_LOWER = HEX_DIGITS_UPPER.lower()

    def __init__(self, head_id=None, user_id=None, issue_id=None, issue_key=None, task_spec=None):
        self.head_id = head_id or HeadUnit.head_id()
        self.user_id = user_id or StartrekForm.user_id()
        self.issue_id = issue_id or StartrekForm.issue_id()
        self.issue_key = issue_key or StartrekForm.ticket_id()
        self.task_spec = task_spec
        if self.task_spec is None:
            self.task_spec = TaskSpecification()
            self.task_spec.bug_report_data.CopyFrom(TaskSpecification.BugReportData())

    @property
    def spec_text(self):
        return text_format.MessageToString(self.task_spec)

    @staticmethod
    def ticket_id():
        return 'AUTOREPORT-' + str(random.randint(1, 200))

    @staticmethod
    def user_id():
        return rstr.rstr(StartrekForm.HEX_DIGITS_LOWER, 32)

    @staticmethod
    def issue_id():
        return rstr.rstr(StartrekForm.HEX_DIGITS_LOWER, 32)
