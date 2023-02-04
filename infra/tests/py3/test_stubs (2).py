from __future__ import unicode_literals

import nanny_rpc_client
from infra.nanny2.stubs.python import hq_stub


def test_stubs():
    transport = nanny_rpc_client.RetryingRpcClient(rpc_url='https://federated.yandex-team.ru/rpc/federated/')
    hq_stub.InstanceServiceStub(transport)
