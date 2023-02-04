import json
from hamcrest import assert_that, equal_to, is_, has_entry, contains_exactly, empty, has_length, has_item, not_, has_key
from mock import patch, Mock
from typing import List

from yb_darkspirit import scheme
from yb_darkspirit.task.cron_tasks import CronTask, WsCronTask, UpdateShiftsTask, PullDocumentsTask, GetTasksInfoTask
from yb_darkspirit.task.task_manager import TASKS


@patch.object(CronTask, 'init_task_instances', Mock(return_value={'some_field': 'some_data'}))
def test_cron_task_runs_and_calls_init_task_instances(session):
    task = CronTask()
    result_dict, result_code = task.run(session, {'uuid': 'blabla'})

    assert_that(result_dict, equal_to({'some_field': 'some_data'}))
    assert_that(result_code, equal_to(202))
    CronTask.init_task_instances.assert_called_once_with(session, 'blabla')


@patch.object(CronTask, 'name', Mock(return_value='cron'))
@patch.dict(TASKS, {'cron': CronTask()})
@patch.object(CronTask, 'init_task_instances', Mock(return_value={'some_field': 'some_data'}))
def test_cron_task_run_from_api(session, test_client):
    resp = test_client.post(
        '/v1/task/{cron_task}/run'.format(cron_task=CronTask.name()),
        json={
            'data': {
                'uuid': 'blabla'
            }
        }
    )
    assert_that(resp.status_code, is_(202))
    assert_that(json.loads(resp.data), has_entry('some_field', 'some_data'))
    CronTask.init_task_instances.assert_called_once_with(session, 'blabla')


@patch.object(CronTask, 'name', Mock(return_value='cron'))
def test_get_not_assigned_tasks_doesnt_get_other(session):
    instance = scheme.Task(task_name=CronTask.name(), state='other', init_uuid='blabla')
    with session.begin():
        session.add(instance)

    tasks = CronTask.get_not_assigned_tasks(session)
    assert_that(tasks, is_(empty()))


@patch.object(CronTask, 'name', Mock(return_value='cron'))
def test_get_not_assigned_tasks(session):
    instance = scheme.Task(task_name=CronTask.name(), state=scheme.TaskState.NOT_ASSIGNED.value, init_uuid='blabla')
    with session.begin():
        session.add(instance)

    tasks = CronTask.get_not_assigned_tasks(session)
    assert_that(tasks, contains_exactly(instance))


@patch.object(
    WsCronTask, 'get_ws_list_for_queue', Mock(return_value=[scheme.WhiteSpirit(url='white_master', version='1337')])
)
@patch.object(WsCronTask, 'name', Mock(return_value='ws_cron'))
def test_ws_cron_init_task_instances(session):
    with session.begin():
        result = WsCronTask.init_task_instances(session, 'blabla')

    assert_that({'launched_for_whitespirits_count': 1}, equal_to(result))
    tasks = session.query(scheme.Task).filter(scheme.Task.init_uuid == 'blabla').all()  # type: List[scheme.Task]

    assert_that(tasks, has_length(1))
    assert_that(tasks[0].get_param('whitespirit_url'), equal_to('white_master'))


@patch.object(
    WsCronTask, 'get_ws_urls_from_tasks', Mock(return_value=[])
)
@patch.object(WsCronTask, 'get_not_assigned_tasks', Mock(return_value=[]))
@patch.object(WsCronTask, 'name', Mock(return_value='ws_cron'))
def test_get_ws_list_for_queue(session):
    instance_ws = scheme.WhiteSpirit(url='white_master', version='1337')
    with session.begin():
        session.add(instance_ws)

    wss = WsCronTask.get_ws_list_for_queue(session)
    assert_that(wss, has_item(instance_ws))


@patch.object(WsCronTask, 'get_ws_urls_from_tasks', Mock(return_value=['white_master']))
@patch.object(WsCronTask, 'get_not_assigned_tasks', Mock())
@patch.object(WsCronTask, 'name', Mock(return_value='ws_cron'))
def test_get_ws_list_for_queue_doesnt_return_not_assigned_ws(session):
    instance_ws = scheme.WhiteSpirit(url='white_master', version='1337')
    instance_task = scheme.Task(
        task_name='ws_cron', state=scheme.TaskState.NOT_ASSIGNED.value, init_uuid='blabla',
        params={'whitespirit_url': 'white_master'}
    )
    with session.begin():
        session.add(instance_ws)
        session.add(instance_task)

    tasks = [instance_task]
    WsCronTask.get_not_assigned_tasks.return_value = tasks

    wss = WsCronTask.get_ws_list_for_queue(session)
    assert_that(wss, not_(has_item(instance_ws)))
    WsCronTask.get_not_assigned_tasks.assert_called_once_with(session)
    WsCronTask.get_ws_urls_from_tasks.assert_called_once_with(tasks)


def test_get_ws_urls_from_tasks():
    instance_task = scheme.Task(
        task_name='ws_cron', state=scheme.TaskState.NOT_ASSIGNED.value, init_uuid='blabla',
        params={'whitespirit_url': 'white_master'}
    )
    urls = WsCronTask.get_ws_urls_from_tasks([instance_task])
    assert_that(urls, contains_exactly('white_master'))


def test_get_ws_urls_from_tasks_dont_include_none_ws_url():
    instance_task_with_none = scheme.Task(
        task_name='ws_cron', state=scheme.TaskState.NOT_ASSIGNED.value, init_uuid='blabla'
    )
    urls = WsCronTask.get_ws_urls_from_tasks([instance_task_with_none])
    assert_that(urls, has_length(0))


def test_task_dict_contains_pull_documents_and_update_shifts():
    assert_that(TASKS, has_key(UpdateShiftsTask.name()))
    assert_that(TASKS, has_key(PullDocumentsTask.name()))


@patch.object(GetTasksInfoTask, 'get_tasks_info_from_main_and_ver_tables', Mock())
def test_get_tasks_info_run(session):
    instance_task_info = scheme.TaskInfo(
        task_name='cron_1', worker_id='worker_1',
        state=scheme.TaskState.FINISHED.value, params={'some_field': 'some_value'}
    )
    instance_task = scheme.Task(
        task_name='cron_2', state=scheme.TaskState.IN_PROGRESS.value, init_uuid='blabla', worker_id='worker_2'
    )
    GetTasksInfoTask.get_tasks_info_from_main_and_ver_tables.return_value = ([instance_task], [instance_task_info])

    result = GetTasksInfoTask().run(session, {'uuid': 'blabla'})
    GetTasksInfoTask.get_tasks_info_from_main_and_ver_tables.assert_called_with(session, 'blabla')

    expected_result = {
        'tasks_count': 2,
        'tasks_finished_count': 1,
        'tasks_info': [
            {
                'task_name': instance_task.task_name,
                'worker_id': instance_task.worker_id,
                'state': instance_task.state,
                'params': instance_task.params,
            },
            {
                'task_name': instance_task_info.task_name,
                'worker_id': instance_task_info.worker_id,
                'state': instance_task_info.state,
                'params': instance_task_info.params,
            },
        ],
    }

    assert_that(expected_result, equal_to(result))


@patch.object(scheme.Task, 'get_last_finished_tasks_info_for_uuid', Mock())
def test_get_tasks_info_from_main_and_ver_tables(session):
    instance_task = scheme.Task(
        task_name='cron', state=scheme.TaskState.IN_PROGRESS.value, init_uuid='blabla', worker_id='worker_2'
    )
    with session.begin():
        session.add(instance_task)
    instance_task_info = scheme.TaskInfo(
        task_name='cron', worker_id='worker_1', state=scheme.TaskState.FINISHED.value, params={'some_field': 'some_value'}
    )
    scheme.Task.get_last_finished_tasks_info_for_uuid.return_value = [instance_task_info]

    tasks_in_main_table, finished_tasks_in_ver_table = GetTasksInfoTask.get_tasks_info_from_main_and_ver_tables(session, 'blabla')

    assert_that(tasks_in_main_table, equal_to([instance_task]))
    assert_that(finished_tasks_in_ver_table, equal_to([instance_task_info]))


def test_task_dict_contains_get_tasks_info():
    assert_that(TASKS, has_key(GetTasksInfoTask.name()))
