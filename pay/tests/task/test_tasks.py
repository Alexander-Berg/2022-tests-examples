import json

from hamcrest import assert_that, is_, has_entry
from mock import patch, Mock

from yb_darkspirit.task import Task
from yb_darkspirit.task.task_manager import TASKS


@patch.dict(TASKS, {})
def test_unknown_task_run(test_client):
    resp = test_client.post('/v1/task/unknown/run')
    assert_that(resp.status_code, is_(404), resp.data)
    assert_that(json.loads(resp.data), has_entry('task_name', 'unknown'))


class GoodTask(Task):
    @classmethod
    def name(cls):
        return 'good'

    def run(self, session, data):
        return {'result': 'ok'}


@patch.dict(TASKS, {GoodTask.name(): GoodTask()})
def test_task_run(test_client):
    resp = test_client.post('/v1/task/good/run')
    assert_that(resp.status_code, is_(200), resp.data)
    assert_that(json.loads(resp.data), has_entry('result', 'ok'))


@patch.dict(TASKS, {GoodTask.name(): GoodTask()})
@patch.object(GoodTask, 'run', Mock())
def test_task_request_calls_run_with_data(session, test_client):
    data = {'data': {'test_field': 'test_value'}}
    resp = test_client.post('/v1/task/good/run', json=data)

    GoodTask.run.assert_called_with(session, data['data'])


@patch.dict(TASKS, {GoodTask.name(): (GoodTask(), None)})
@patch.object(GoodTask, 'run', Mock())
def test_task_request_fails_on_0_limit(session, test_client):
    data = {'data': {'limit': 0}}
    resp = test_client.post('/v1/task/good/run', json=data)

    assert_that(resp.status_code, is_(422))


class BadTask(Task):
    _exception_message = 'Failure'

    @classmethod
    def name(cls):
        return 'bad'

    def run(self, session, data):
        raise Exception(self._exception_message)


@patch.dict(TASKS, {BadTask.name(): BadTask()})
def test_task_run_with_failure(test_client):
    resp = test_client.post('/v1/task/bad/run')
    assert_that(resp.status_code, is_(500), resp.data)
    assert_that(json.loads(resp.data), has_entry('message', 'Error: {}'.format(BadTask._exception_message)))
