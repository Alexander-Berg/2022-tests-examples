import pytest
import mock
from yabs.conf.utils import AttrDict
from datetime import datetime
from yabs.tabutils import read_ts_table
import ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.resource
from ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.pipeline import PipelineMessageBus
from ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.task import Task, GraphState, TasksFlow
from ads.nirvana.online_learning.datetime_utils import DATETIME_FORMAT

from ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.resource.model_matcher import DEFAULT_MR_TABLE_FIELDS
from ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.pipeline.action import validate_leader_change, \
    reactivate_failed_nirvana_flow, TaskActionError, \
    StopTaskActionsMessage, change_leader_on_success, update_fail_meta_info, set_leader_if_necessary, \
    consider_leader_change, set_learned_model_reference_in_database

from ads.libs.py_test_mapreduce import write_mr_table  # noqa
from yabs.tabutils import dropTableWithMeta


@pytest.fixture()
def task1():
    task = Task()
    task.load_from_dict({
        "ml_task_id": "task",
        "last_log_date": datetime(2016, 9, 1, 12),
        "nirvana_flow_id": "task_flowid1",
        "predecessor_task_id": "task,2016090111",
        "graph_state": GraphState.finished_succeeded
    })
    return task


@pytest.fixture()
def task2():
    task = Task()
    task.load_from_dict({
        "ml_task_id": "task",
        "last_log_date": datetime(2016, 9, 1, 13),
        "nirvana_flow_id": "task_flowid2",
        "predecessor_task_id": "task,2016090112",
        "graph_state": GraphState.finished_succeeded,
        "is_leader": True
    })
    return task


@pytest.fixture()
def task3():
    task = Task()
    task.load_from_dict({
        "ml_task_id": "task",
        "last_log_date": datetime(2016, 9, 1, 14),
        "nirvana_flow_id": "task_flowid3",
        "predecessor_task_id": "task,2016090113",
        "graph_state": None
    })
    return task


@pytest.fixture()
def task4():
    task = Task()
    task.load_from_dict({
        "ml_task_id": "task",
        "last_log_date": datetime(2016, 9, 2, 1),
        "nirvana_flow_id": "task_flowid3",
        "predecessor_task_id": "task,2016090114",
        "graph_state": GraphState.finished_failed
    })
    return task


@pytest.fixture()
def tasks_flow(task1, task2, task3):
    return TasksFlow([task1, task2, task3])


def singleton(cls):
    instances = {}

    def get_instance(conf):
        if cls not in instances:
            instances[cls] = cls(conf)
        return instances[cls]

    return get_instance


@singleton
class MockDBHandler(object):
    def __init__(self, config):
        pass

    #    def update_state_and_result_in_db(self, *args, **kwargs):
    #        pass
    def update_task(self, task_schema, update_columns=None):
        pass


class MockNirvanaHandler(object):
    _instance = None

    def __new__(cls, config):
        if MockNirvanaHandler._instance is None:
            MockNirvanaHandler._instance = object.__new__(cls)
        MockNirvanaHandler._instance.config = config
        return MockNirvanaHandler._instance

    def reactivate(self, workflow_id):
        return workflow_id + "_reactivated"

    def deactivate(self, workflow_id):
        pass


def assert_cur_datetime_right(mr_rec):
    assert_cur_datetime(mr_rec.StartTime)


def assert_cur_datetime(start_time):
    assert (datetime.strptime(start_time, DATETIME_FORMAT) -
            datetime.now()).total_seconds() < 120


class MockRecoveryPolicy(object):
    _instance = None

    def __new__(cls, config):
        if MockRecoveryPolicy._instance is None:
            MockRecoveryPolicy._instance = object.__new__(cls)
        MockRecoveryPolicy._instance.config = config
        return MockRecoveryPolicy._instance

    def reelect_leader(self, leader):
        change_leader = True
        return change_leader


@pytest.fixture()
def db_handler():
    return MockDBHandler({})


@pytest.fixture()
def nirvana_handler():
    return MockNirvanaHandler({})


@pytest.fixture()
def resource_factory(monkeypatch, db_handler):
    def mock_update_state_and_result_in_db(db_handler, task):
        pass

    monkeypatch.setattr(ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.resource.resource, "NirvanaHandler", MockNirvanaHandler)
    return ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.resource.ResourceFactory()


@pytest.fixture()
def resource_config():
    return AttrDict({"nirvana": {}})


@pytest.fixture()
def mbus():
    return PipelineMessageBus()


def test__validate_leader_change_task3(task3, tasks_flow, mbus, resource_factory, resource_config):
    with pytest.raises(StopTaskActionsMessage):
        validate_leader_change(task3, tasks_flow, mbus, resource_factory, resource_config)


def test__validate_leader_change_task2(task2, tasks_flow, mbus, resource_factory, resource_config):
    try:
        validate_leader_change(task2, tasks_flow, mbus, resource_factory, resource_config)
    except TaskActionError:
        pytest.fail("TaskActionError raised when it should not")


def test__reactivate_failed_nirvana_flow(task1, tasks_flow, mbus, resource_factory, resource_config,
                                         db_handler, nirvana_handler):
    old_nirvana_flow_id = task1.nirvana_flow_id
    # print online_flowkeeper.resource.resource.NirvanaHandler.__dict__
    with mock.patch.object(
            ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.resource.resource.NirvanaHandler, "reactivate"
    ):
        reactivate_failed_nirvana_flow(task1, tasks_flow, mbus, resource_factory, resource_config)
        nirvana_handler.reactivate.assert_called_once_with(old_nirvana_flow_id)


def test__change_leader_on_success(task2, task3, tasks_flow, mbus, resource_factory, resource_config):
    change_leader_on_success(task2, tasks_flow, mbus, resource_factory, resource_config)
    assert tasks_flow.leader == task3
    assert task2.is_finished
    assert task2.graph_state == GraphState.finished_succeeded
    assert task3.is_leader


def test__update_fail_graph_meta(task4, tasks_flow, mbus, resource_factory, resource_config):
    f1 = task4.fail_count
    update_fail_meta_info(task4, tasks_flow, mbus, resource_factory, resource_config)
    assert task4.fail_count == f1 + 1


@pytest.fixture()
def flowkeeper_conf():
    return AttrDict({
        "online_flowkeeper": {
            "db": {
                "database": "online_learning",
                "tasks_table": "Tasks"
            },
            "recovery_policy": {
                "max_fail_count": 10,
                "wait_after_fail_min": 60,
                "max_run_time": 432000,
                "max_leader_run_time": 3600
            },
            "nirvana": {
                "tags": ['online'],
                "oauth_token": "blablablatoken",
                "url": "https://nirvana.yandex-team.ru",
                "request_retries": 30,
                "request_delay": 300,
                "flow": "76f1d00e-a4e3-11e6-98ff-0025909427cc",
                "start_flow": "8fb8ca56-a020-11e6-98ff-0025909427cc"
            },
            "sandbox": {
                "state_file": "run_nirvana_online_learning_state.json",
                "new_graphs_file": "run_nirvana_online_learning_new_graphs_queue.json"
            },
            "flow": {
                "SandboxedUserPipeline"
            },
            "model_storage": {
                "full_state_models": "//home/bs/online_learning",
                "ml_task_id": "task"
            },
            "model_matcher": {
                "prev_model_table": "prev_models_table",
                "ml_task_id": "task"
            }
        }
    })


@pytest.fixture()
def pending_task1():
    task = Task()
    task.load_from_dict({
        "ml_task_id": "task",
        "last_log_date": datetime(2016, 10, 1, 17, 52, 18),
        "nirvana_flow_id": "task_flowid1",
        "graph_state": GraphState.finished_succeeded
    })
    return task


@pytest.fixture()
def pending_task2():
    task = Task()
    task.load_from_dict({
        "ml_task_id": "task",
        "last_log_date": datetime(2016, 10, 1, 18, 52, 18),
        "nirvana_flow_id": "task_flowid1",
        "graph_state": GraphState.finished_succeeded
    })
    return task


@pytest.fixture()
def pending_task3():
    task = Task()
    task.load_from_dict({
        "ml_task_id": "task",
        "last_log_date": datetime(2016, 10, 1, 19, 52, 18),
        "nirvana_flow_id": "task_flowid1",
        "graph_state": GraphState.finished_succeeded
    })
    return task


@pytest.fixture()
def pending_tasks_flow(pending_task1, pending_task2, pending_task3):
    return TasksFlow([pending_task1, pending_task2, pending_task3])


@pytest.yield_fixture()
def delete_used_model_matcher_table():
    yield
    dropTableWithMeta("//home/bs/online_learning/task/model_matcher_table")


def test__INTEGRATION_select_leader_record_with_previous_model(pending_task1, pending_tasks_flow, mbus, resource_factory, flowkeeper_conf, local_yt):  # noqa
    MAPREDUCE_HEADER = "# " + "\t".join(["TaskID:str"] + [x + ":str" for x in DEFAULT_MR_TABLE_FIELDS])
    MAPREDUCE_VAL1 = "{}\t{}\t\t\t\t".format(pending_task1.task_id, "some_prev_model_path")

    header = MAPREDUCE_HEADER
    write_mr_table(
        "\n".join([header, MAPREDUCE_VAL1]),
        "//home/bs/online_learning/task/model_matcher_table")

    flowkeeper_conf.online_flowkeeper.model_matcher.prev_model_table = "//home/bs/online_learning"
    flowkeeper_conf.online_flowkeeper.model_matcher.ml_task_id = "task"
    set_leader_if_necessary(None, pending_tasks_flow, mbus, resource_factory, flowkeeper_conf.online_flowkeeper)

    recs = [r for r in read_ts_table("//home/bs/online_learning/task/model_matcher_table")]
    assert len(recs) == 1
    r = recs[0]
    assert r.PrevModelPath == "some_prev_model_path"
    assert_cur_datetime_right(r)
    assert r.ResultModelPath == ""
    assert r.FinishTime == ""
    assert r.WorkflowID == ""

    dropTableWithMeta("//home/bs/online_learning/task/model_matcher_table")


@pytest.fixture()
def failed_leader_task1():
    task = Task()
    task.load_from_dict({
        "ml_task_id": "task",
        "last_log_date": datetime(2016, 10, 1, 17, 52, 18),
        "nirvana_flow_id": "task_flowid1",
        "graph_state": GraphState.finished_failed,
        "is_leader": True,
        "start_time": datetime(2016, 10, 1, 17, 52, 18)
    })
    return task


@pytest.fixture()
def successor_task():
    task = Task()
    task.load_from_dict({
        "ml_task_id": "task",
        "last_log_date": datetime(2016, 10, 1, 18, 52, 18),
        "nirvana_flow_id": "task_flowid1",
        "graph_state": GraphState.running
    })
    return task


@pytest.fixture()
def failed_leader_workflow(failed_leader_task1, successor_task):
    return TasksFlow([failed_leader_task1, successor_task])


def test__INTEGRATION_consider_leader_change_no_start_time(failed_leader_task1, failed_leader_workflow, mbus, resource_factory, flowkeeper_conf, local_yt, delete_used_model_matcher_table):  # noqa
    leader = failed_leader_task1
    MAPREDUCE_HEADER = "# " + "\t".join(["TaskID:str"] + [x + ":str" for x in DEFAULT_MR_TABLE_FIELDS])
    MAPREDUCE_VAL1 = "{}\t\t\t\t\t".format(leader.task_id)

    header = MAPREDUCE_HEADER
    write_mr_table(
        "\n".join([header, MAPREDUCE_VAL1]),
        "//home/bs/online_learning/task/model_matcher_table")

    flowkeeper_conf.online_flowkeeper.model_matcher.prev_model_table = "//home/bs/online_learning"
    flowkeeper_conf.online_flowkeeper.model_matcher.ml_task_id = "task"

    with pytest.raises(ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.resource.TaskIdInfoError):
        consider_leader_change(leader, failed_leader_workflow, mbus, resource_factory,
                               flowkeeper_conf.online_flowkeeper)


def test__INTEGRATION_consider_leader_change_leader_start_time_timeout(failed_leader_task1, successor_task, failed_leader_workflow, mbus, resource_factory, flowkeeper_conf, local_yt, delete_used_model_matcher_table):  # noqa
    leader = failed_leader_task1
    MAPREDUCE_HEADER = "# " + "\t".join(["TaskID:str"] + [x + ":str" for x in DEFAULT_MR_TABLE_FIELDS])
    MAPREDUCE_VAL1 = "{}\t\t\t{}\t\t".format(leader.task_id, datetime(1970, 1, 1).strftime(DATETIME_FORMAT))

    header = MAPREDUCE_HEADER
    write_mr_table(
        "\n".join([header, MAPREDUCE_VAL1]),
        "//home/bs/online_learning/task/model_matcher_table")

    flowkeeper_conf.online_flowkeeper.model_matcher.prev_model_table = "//home/bs/online_learning"
    flowkeeper_conf.online_flowkeeper.model_matcher.ml_task_id = "task"
    consider_leader_change(leader, failed_leader_workflow, mbus, resource_factory, flowkeeper_conf.online_flowkeeper)

    assert mbus.last_label


def test__INTEGRATION_set_model_reference_in_database(task2, tasks_flow, mbus, resource_factory, flowkeeper_conf):
    succeeded_task = task2
    MAPREDUCE_HEADER = "# " + "\t".join(["TaskID:str"] + [x + ":str" for x in DEFAULT_MR_TABLE_FIELDS])
    MAPREDUCE_VAL1 = "{}\t{}\t\t\t\t".format(succeeded_task.task_id, "some_prev_model_path")

    header = MAPREDUCE_HEADER
    write_mr_table(
        "\n".join([header, MAPREDUCE_VAL1]),
        "//home/bs/online_learning/task/model_matcher_table")

    flowkeeper_conf.online_flowkeeper.model_matcher.prev_model_table = "//home/bs/online_learning"
    flowkeeper_conf.online_flowkeeper.model_matcher.ml_task_id = "task"
    set_learned_model_reference_in_database(task2, tasks_flow, mbus, resource_factory, flowkeeper_conf.online_flowkeeper)

    recs = [r for r in read_ts_table("//home/bs/online_learning/task/model_matcher_table")]
    assert len(recs) == 1
    r = recs[0]
    assert r.PrevModelPath == "some_prev_model_path"
    assert r.ResultModelPath == "//home/bs/online_learning/task/full_state_models/full_state_model_201609011300"
    assert r.FinishTime == ""
    assert r.WorkflowID == ""
