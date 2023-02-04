import pytest
import mock
from yabs.conf.utils import AttrDict
from datetime import datetime
import ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.resource
from ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.task import Task, GraphState, TasksFlow
from ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.pipeline.action import \
    send_leader_workflow_switch_info
from ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.resource import LeaderWorkflowIDTracker


class GraphiteMock(object):
    _instance = None

    def __new__(cls, *args):
        if GraphiteMock._instance is None:
            GraphiteMock._instance = object.__new__(cls)
        return GraphiteMock._instance

    def __init__(self, *args):
        self.last_log_date = None
        self.ml_task_id = None
        self.metrics_dict = None

    def send(self, last_log_date, ml_task_id, metrics_dict):
        self.last_log_date = last_log_date
        self.ml_task_id = ml_task_id
        self.metrics_dict = metrics_dict

    def set_prefix(self, prefix):
        self.graphite_prefix = prefix


@pytest.yield_fixture()
def graphite_mock():
    yield GraphiteMock()


@pytest.yield_fixture()
def resource_factory(monkeypatch):
    monkeypatch.setattr(ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.resource.resource,
                        "GraphiteHandler", GraphiteMock)
    yield ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.resource.ResourceFactory()


@pytest.yield_fixture()
def running_leader():
    task = Task()
    task.load_from_dict({
        "ml_task_id": "task",
        "last_log_date": datetime(2016, 9, 1, 14),
        "nirvana_flow_id": "task_flowid4",
        "predecessor_task_id": "task,2016090112",
        "graph_state": GraphState.running,
        "is_leader": True
    })
    task.set_leadership_start_time(datetime.now())
    yield task


@pytest.yield_fixture()
def graph_failed():
    task = Task()
    task.load_from_dict({
        "ml_task_id": "task",
        "last_log_date": datetime(2016, 9, 1, 14),
        "nirvana_flow_id": "task_flowid3",
        "predecessor_task_id": "task,2016090112",
        "graph_state": GraphState.finished_failed,
        "is_leader": False
    })
    task.set_leadership_start_time(datetime(1970, 1, 1))
    yield task


@pytest.fixture()
def tasks_flow(running_leader, graph_failed):
    return TasksFlow([graph_failed, running_leader])


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
                "full_state_models": "ahaha_full_state_models1",
                "ml_task_id": "task"
            },
            "model_matcher": {
                "prev_model_table": "prev_models_table",
                "ml_task_id": "task"
            },
            "graphite": {

            }
        }
    })


@pytest.yield_fixture()
def mbus():
    yield


def test__send_correct_switch_info_metrics(graph_failed, running_leader, tasks_flow, mbus,
                                           resource_factory, flowkeeper_conf, graphite_mock):
    tracker = LeaderWorkflowIDTracker(flowkeeper_conf.online_flowkeeper.recovery_policy)
    tracker.first_leader = graph_failed
    tracker.second_leader = running_leader
    send_leader_workflow_switch_info(graph_failed, tasks_flow, mbus, resource_factory,
                                     flowkeeper_conf.online_flowkeeper)
    assert graphite_mock.ml_task_id == "task"
    assert graphite_mock.metrics_dict == {
        "max_leader_timeout_violated": 5,
        "workflow_switch_logdate": graph_failed.last_log_date
    }
    assert isinstance(graph_failed.last_log_date, datetime)
    assert graphite_mock.graphite_prefix == "one_min.online_learning.leader_wf_id_switch"


def test__send_correct_switch_info_metrics_wf_not_changed(running_leader, tasks_flow, mbus,
                                                          resource_factory, flowkeeper_conf, graphite_mock):
    tracker = LeaderWorkflowIDTracker(flowkeeper_conf.online_flowkeeper.recovery_policy)
    tracker.first_leader = running_leader
    tracker.second_leader = running_leader
    with mock.patch.object(
            ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.resource.resource.GraphiteHandler, "send"
    ):
        send_leader_workflow_switch_info(running_leader, tasks_flow, mbus, resource_factory,
                                         flowkeeper_conf.online_flowkeeper)
        assert not graphite_mock.send.called
