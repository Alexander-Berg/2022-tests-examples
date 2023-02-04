import mock
import os
import yatest
import pytest

import yp.client
from yp_proto.yp.client.api.proto import pod_agent_pb2
from infra.mc_rsc.src import model
from infra.mc_rsc.src.lib.yp_client import YpClient
from infra.mc_rsc.src.lib.loaders import YsonLoader, PbLoader

from sepelib.core import config as sconfig


MC_RS_ID = 'mc-rs-id'
RS_ID = 'rs-id'
XDC = 'xdc'
MAX_UNAVAILABLE = 2


def source_path(path):
    try:
        return yatest.common.source_path(path)
    except AttributeError:
        # only for local pycharm tests
        return os.path.join(os.environ["PWD"], path)


def load_config():
    config_path = source_path("infra/mc_rsc/tests/unit/cfg_test.yaml")
    sconfig.load(config_path)


@pytest.fixture(scope='session', autouse=True)
def config():
    load_config()
    return sconfig


class DummyThreadPool(object):

    def apply(self, func, args=None, kwargs=None):
        args = args or []
        kwargs = kwargs or {}
        return func(*args, **kwargs)

    def spawn(self, func, *args, **kwargs):
        m = mock.Mock()
        m.get.side_effect = lambda: func(*args, **kwargs)
        return m


@pytest.fixture
def yp_client(config):
    xdc = config.get_value('yp.xdc')
    address = None
    for c in config.get_value('yp.clusters'):
        if c['cluster'] == xdc:
            address = c['address']
    if address is None:
        return
    base_yp_client = yp.client.YpClient(address=address)
    stub = mock.create_autospec(base_yp_client.create_grpc_object_stub())
    rv = YpClient(stub=stub, loader=YsonLoader)
    rv.tp = DummyThreadPool()
    return rv


@pytest.fixture
def yp_proto_client(config):
    xdc = config.get_value('yp.xdc')
    address = None
    for c in config.get_value('yp.clusters'):
        if c['cluster'] == xdc:
            address = c['address']
    if address is None:
        return
    base_yp_client = yp.client.YpClient(address=address)
    stub = mock.create_autospec(base_yp_client.create_grpc_object_stub())
    rv = YpClient(stub=stub, loader=PbLoader)
    rv.tp = DummyThreadPool()
    return rv


@pytest.fixture
def target():
    return 2


@pytest.fixture
def first_cluster():
    return 'sas'


@pytest.fixture
def second_cluster():
    return 'man'


@pytest.fixture
def replica_count():
    return 3


@pytest.fixture
def mc_rs(first_cluster, second_cluster, replica_count, target):
    obj = yp.data_model.TMultiClusterReplicaSet()
    obj.meta.id = MC_RS_ID
    obj.spec.revision = target
    obj.spec.deployment_strategy.max_unavailable = MAX_UNAVAILABLE
    for cluster in (first_cluster, second_cluster):
        c = obj.spec.clusters.add()
        c.cluster = cluster
        c.spec.replica_count = replica_count
        ac = c.spec.constraints.antiaffinity_constraints.add()
        ac.key = 'node'
        ac.max_pods = 1
    return model.MultiClusterReplicaSet(obj=obj, cluster=XDC)


@pytest.fixture
def rs():
    obj = yp.data_model.TReplicaSet()
    obj.meta.id = RS_ID
    return model.ReplicaSet(obj=obj, cluster=XDC)


@pytest.fixture
def ready_pod(target):
    p = yp.data_model.TPod()
    p.meta.id = 'ready-pod'
    p.meta.pod_set_id = 'pod-set'
    p.spec.pod_agent_payload.spec.revision = target
    p.status.agent.pod_agent_payload.status.revision = target
    p.status.agent.state = yp.data_model.PCS_STARTED
    p.status.agent.pod_agent_payload.status.ready.status = pod_agent_pb2.EConditionStatus_TRUE
    p.status.scheduling.node_id = 'some-node'
    return p


@pytest.fixture
def failed_pod(target):
    p = yp.data_model.TPod()
    p.meta.id = 'failed-pod'
    p.meta.pod_set_id = 'pod-set'
    p.spec.pod_agent_payload.spec.revision = target
    p.status.agent.pod_agent_payload.status.revision = target
    p.status.agent.state = yp.data_model.PCS_STARTED
    p.status.agent.pod_agent_payload.status.failed.status = pod_agent_pb2.EConditionStatus_TRUE
    p.status.scheduling.node_id = 'some-node'
    return p


@pytest.fixture
def in_progress_pod(target):
    p = yp.data_model.TPod()
    p.meta.id = 'in-progress-pod'
    p.meta.pod_set_id = 'pod-set'
    p.spec.pod_agent_payload.spec.revision = target
    p.status.agent.state = yp.data_model.PCS_UNKNOWN
    p.status.scheduling.node_id = 'some-node'
    return p


@pytest.fixture
def eviction_requested_pod(target):
    p = yp.data_model.TPod()
    p.meta.id = 'eviction-requested-pod'
    p.meta.pod_set_id = 'pod-set'
    p.spec.pod_agent_payload.spec.revision = target
    p.status.eviction.state = yp.data_model.ES_REQUESTED
    p.status.scheduling.node_id = 'some-node'
    return p


@pytest.fixture
def removed_ready_pod(target):
    p = yp.data_model.TPod()
    p.meta.id = 'removed-ready-pod'
    p.meta.pod_set_id = 'pod-set'
    p.spec.pod_agent_payload.spec.revision = target
    p.spec.pod_agent_payload.spec.target_state = yp.data_model.EPodAgentTargetState_REMOVED
    p.status.agent.pod_agent_payload.status.revision = target
    p.status.agent.state = yp.data_model.PCS_STARTED
    p.status.agent.pod_agent_payload.status.ready.status = pod_agent_pb2.EConditionStatus_TRUE
    p.status.scheduling.node_id = 'some-node'
    return p


@pytest.fixture
def removed_in_progress_pod(target):
    p = yp.data_model.TPod()
    p.meta.id = 'removed-in-progress-pod'
    p.meta.pod_set_id = 'pod-set'
    p.spec.pod_agent_payload.spec.revision = target
    p.spec.pod_agent_payload.spec.target_state = yp.data_model.EPodAgentTargetState_REMOVED
    p.status.agent.pod_agent_payload.status.revision = target
    p.status.agent.state = yp.data_model.PCS_STARTED
    p.status.agent.pod_agent_payload.status.ready.status = pod_agent_pb2.EConditionStatus_FALSE
    p.status.scheduling.node_id = 'some-node'
    return p


@pytest.fixture
def eviction_requested_pod_w_target_state_removed(target):
    p = yp.data_model.TPod()
    p.meta.id = 'eviction-requested-pod-removed-in-progress-pod'
    p.meta.pod_set_id = 'pod-set'
    p.spec.pod_agent_payload.spec.revision = target
    p.status.eviction.state = yp.data_model.ES_REQUESTED
    p.status.scheduling.node_id = 'some-node'
    p.spec.pod_agent_payload.spec.target_state = yp.data_model.EPodAgentTargetState_REMOVED
    return p
