from unittest import mock

from infra.walle.server.tests.lib.util import get_issuer
from walle.clients import qloud
from walle.hosts import HostState
from walle.models import timestamp
from walle.scenario.constants import SchedulerName
from walle.scenario.marker import Marker, MarkerStatus
from walle.scenario.mixins import BaseStage, HostStage
from walle.scenario.scenario import Scenario
from walle.scenario.stage_info import StageInfo, StageStatus, StageRegistry
from walle.util.misc import drop_none


class MockedQloudClient:
    def __init__(self):
        self.hosts = dict()

    def find_host(self, hostname):
        return self.hosts.get(hostname, None)

    def get_segment_project(self, segment):
        return "qloud"

    def set_qloud_host_state(self, host, state, comment=None):
        host.state = state

    def add_host(self, hostname, segment, data=None):
        installation, segment = qloud.split_segment(segment)
        host_data = {"fqdn": hostname, "segment": segment, "slots": [], "state": "INITIAL"}
        if data:
            host_data.update(data)
        self.hosts[hostname] = qloud.QloudHost(installation, host_data)
        self.hosts[hostname].meta_called = False

    def update_qloud_host_metadata(self, host):
        host.meta_called = True
        return None

    def update_host_metadata(self, hostname):
        host = self.find_host(hostname)
        if host:
            return self.update_qloud_host_metadata(host)

    def remove_qloud_host(self, host):
        self.hosts.pop(host.name)
        return None

    def find_host_installation_segment(self, hostname):
        host = self.hosts.get(hostname, None)
        if host:
            return host.installation, host.segment
        return None, None

    def get_host_state(self, hostname):
        host = self.find_host(hostname)
        if not host:
            raise qloud.QloudHostNotFound("Qloud host {} not found".format(hostname))
        return host.state

    def set_host_state(self, hostname, state, comment=None):
        host = self.find_host(hostname)
        if not host:
            raise qloud.QloudHostNotFound("Qloud host {} not found".format(hostname))
        return self.set_qloud_host_state(host, state, comment)


def get_test_scenario_document_with_stage_info(size=1):
    stages = []
    test_values = {
        "scenario_id": 1,
        "name": "test",
        "issuer": "tester",
        "scenario_type": "noop",
        "next_check_time": 0,
        "creation_time": 0,
    }
    for i in range(size):
        stage = StageInfo(seq_num=i, name=str(i), status=StageStatus.QUEUE, status_time=timestamp())
        stages.append(stage)

    root_stage_info = StageInfo(seq_num=stages[0].seq_num, stages=stages)
    document = Scenario(stage_info=root_stage_info, **test_values)
    return document


def get_scenario_params(creation_time=0, scenario_type="wait", script_args=None):
    if not script_args:
        script_args = {}
    script_args["schedule_type"] = SchedulerName.ALL
    return {
        "scenario_id": 1,
        "name": "test",
        "issuer": get_issuer(manual=True),
        "scenario_type": scenario_type,
        "next_check_time": 0,
        "creation_time": creation_time,
        "script_args": script_args,
        "uses_uuid_keys": True,
    }


def mock_qloud_project(walle_test, id):
    return walle_test.mock_project({"id": id, "tags": ["rtc.scheduler-qloud"]})
    pass


def mock_host_inside_qloud(walle_test, qloud_client, project_id=None, qloud_state=None):
    if project_id is None:
        project_id = mock_qloud_project(walle_test, "mock-qloud").id
    host = walle_test.mock_host({"state": HostState.ASSIGNED, "project": project_id})
    if qloud_state is not None:
        qloud_client.add_host(host.name, "ext.mock")
        qloud_client.set_host_state(host.name, qloud_state)
    return host


def mock_scenario(**overrides):
    params = dict(get_scenario_params(), **overrides)
    return Scenario(**params)


def make_mock_stage(run_return_value=None, cleanup_return_value=None):
    class MockStage(BaseStage):
        run = mock.Mock(return_value=run_return_value or Marker.success())
        cleanup = mock.Mock(return_value=cleanup_return_value or Marker.success())

    StageRegistry.ITEMS["MockStage"] = MockStage

    return MockStage()


def make_mock_host_stage(marker_status=MarkerStatus.SUCCESS, data=None):
    def stage_run(stage_info, scenario, host, scenario_stage_info):
        assert stage_info.name == "MockHostStage"
        params = drop_none(dict(status=marker_status, data=data))
        return Marker(**params)

    class MockHostStage(HostStage):
        run = mock.Mock(side_effect=stage_run)

    StageRegistry.ITEMS["MockHostStage"] = MockHostStage

    return MockHostStage()


def get_mock_host_stage():
    return StageRegistry.ITEMS["MockHostStage"]
