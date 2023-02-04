import typing as tp

import mock

from infra.walle.server.tests.lib.util import mock_host_health_status
from sepelib.core.constants import HOUR_SECONDS, MINUTE_SECONDS
from walle.expert import triage, juggler
from walle.expert.types import CheckStatus
from walle.hosts import HostState, HostStatus, HealthStatus, Decision as DecisionDocument
from walle.models import timestamp
from walle.util import mongo


def monkeypatch_health_data(mp, hosts, mock_health_data, status=CheckStatus.FAILED):
    result_value = [
        juggler.HostHealthStatus(
            host.name,
            [
                {
                    "type": check["type"],
                    "metadata": check["metadata"],
                    "status": status,
                    "status_mtime": timestamp() - HOUR_SECONDS,
                    "timestamp": timestamp() - MINUTE_SECONDS,
                    "effective_timestamp": timestamp() - 2 * MINUTE_SECONDS,
                }
                for check in mock_health_data
            ],
        )
        for host in hosts
    ]
    mp.function(juggler._fetch_health_data, return_value=result_value)
    return result_value


# noinspection PyProtectedMember
def run_triage(processor: tp.Optional[triage.TriageShardProcessor] = None):
    if processor is None:
        processor = triage.TriageShardProcessor(1)
    shard = mongo.MongoPartitionerShard("0", mock.MagicMock())
    return processor._triage(shard)


def mock_host(walle_test, inv, decision, decision_status, **overrides):
    health_status = mock_host_health_status(status=HealthStatus.STATUS_FAILURE)
    health_status.decision = DecisionDocument(**decision.to_dict())

    host_params = {
        "inv": inv,
        "state": HostState.ASSIGNED,
        "status": HostStatus.READY,
        "health": health_status,
        "decision_status_timestamp": timestamp(),
        "decision_status": decision_status,
    }
    host_params.update(overrides)

    return walle_test.mock_host(host_params)
