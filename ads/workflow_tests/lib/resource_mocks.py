import ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.resource
from ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.resource.leader_workflow_tracker import SingletonMeta,\
    RecoveryPolicy, SwitchReasons
from ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.resource.nirvana import NirvanaResult
import pytest


class MockNirvanaHandler(object):
    _instance = None

    def __new__(cls, config):
        if MockNirvanaHandler._instance is None:
            MockNirvanaHandler._instance = object.__new__(cls)
        MockNirvanaHandler._instance.config = config
        return MockNirvanaHandler._instance

    def reactivate(self, workflow_id):
        new_workflow_id = self.clone_workflow(workflow_id)
        self.start_workflow(new_workflow_id)
        return new_workflow_id

    def deactivate(self, workflow_id):
        return workflow_id

    def getExecutionStateForBlocks(self, workflow_id, blocks):
        pass

    def get_workflow_state(self, workflow_id):
        if '___failed___' in workflow_id:
            return NirvanaResult.fail
        if '___success___' in workflow_id:
            return NirvanaResult.success
        if '___in_progress___' in workflow_id:
            return NirvanaResult.in_progress

    def set_user(self, user):
        pass

    def start_workflow(self,  workflow_id):
        return workflow_id

    def clone_workflow(self, workflow_id, task=None, tags=None):
        return workflow_id

    def get_tags(self, task):
        return ["OL_TEST_TAG"]

    def transform_parameters(self, parameters):
        pass

    def set_workflow_parameters(self, workflow_id, workflow_params):
        return workflow_id


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

    def create_graphite(self):
        pass

    def postprocess(self, last_log_date, metrics_dict):
        return metrics_dict


class LeaderWorkflowIDTrackerMock(object):
    __metaclass__ = SingletonMeta

    def __init__(self, config):
        self.first_leader = None
        self.second_leader = None
        self.recovery_policy = RecoveryPolicy(config)
        pass

    def set_first_leader(self, leader):
        assert leader
        self.first_leader = leader

    def set_second_leader(self, leader):
        assert leader
        self.second_leader = leader

    def get_switch_reason(self):
        # Truncated version: we suppose that graphs always work correctly. For unit-tests processing
        # other cases see ut/test_leader_workflow_tracker.py
        if self.first_leader.nirvana_flow_id == self.second_leader.nirvana_flow_id:
            return SwitchReasons.NOT_SWITCHED
        if self.first_leader.is_succeeded:
            return SwitchReasons.SUCCESS
        if self.recovery_policy.leader_timeout_violated(self.first_leader):
            return SwitchReasons.MAX_LEADER_TIMEOUT_VIOLATED

        return SwitchReasons.UNKNOWN


@pytest.yield_fixture()
def resource_factory(monkeypatch):
    # Mock Nirvana
    monkeypatch.setattr(ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.resource.resource,
                        "NirvanaHandler", MockNirvanaHandler)

    # Mock Graphite
    monkeypatch.setattr(ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.resource.resource,
                        "GraphiteHandler", GraphiteMock)

    # Mock workflow id tracker
    monkeypatch.setattr(ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.resource.resource,
                        "LeaderWorkflowIDTracker", LeaderWorkflowIDTrackerMock)
    yield
