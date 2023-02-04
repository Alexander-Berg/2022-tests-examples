# coding=utf-8
from datetime import datetime, timedelta
import pytz
from unittest import mock
from billing.apikeys.apikeys import mapper
import mongoengine as me


class FakeTask(mapper.Task):

    @property
    def entity(self):
        return None

    @classmethod
    def _extract_task_unique_name(cls, entity):
        return "FAKE"

    @classmethod
    def _enqueue(cls, *args, **kwargs):
        pass

    def _do_task(self):
        return self.dt + timedelta(minutes=5)


class FakeFailingTask(FakeTask):

    @classmethod
    def _extract_task_unique_name(cls, entity):
        return "FAKE_FAILING"

    def _do_task(self):
        raise Exception


class TaskFailureReport(mapper.ApiKeysDocument):
    dt = me.DateTimeField(required=True)
    task = me.ReferenceField("Task", required=True)
    host_name = me.StringField()
    task_cls = me.StringField()
    status = me.StringField()
    error = me.StringField()


def test_task_runs_properly(mongomock):
    FakeTask(dt=datetime.now(pytz.utc)).save()
    task = mapper.Task.checkout('FakeTask')
    task_dt_after_execution = task.dt + timedelta(minutes=5)
    task.do_task()

    assert task.host_name is not None
    assert task.last_status == 'OK'
    assert task.last_error is None
    assert task.dt == task_dt_after_execution
    assert task.dt == task_dt_after_execution
    assert task.last_success_dt is not None


def test_task_fails_properly(mongomock):
    with mock.patch('billing.apikeys.apikeys.mapper.task.TaskFailureReport', new=TaskFailureReport):
        FakeFailingTask(dt=datetime.now(pytz.utc)).save()
        task = mapper.Task.checkout('FakeFailingTask')
        task.do_task()
        report = TaskFailureReport.objects.first()
        assert task.host_name is not None
        assert task.last_status == 'Exception()'
        assert 'raise Exception' in task.last_error
        assert report.host_name is not None
        assert report.status == task.last_status
        assert report.error == task.last_error
