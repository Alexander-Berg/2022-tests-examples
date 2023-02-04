from clusterpb import federated_stub
from clusterpb import hq_stub


def test_creating_stubs():
    federated_stub.FederatedClusterServiceStub(None)
    hq_stub.InstanceServiceStub(None)
