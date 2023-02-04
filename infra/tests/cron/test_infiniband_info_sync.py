import json

from walle.clients import racktables
from walle.cron.infiniband_info_sync import _infiniband_info_sync
from walle.hosts import InfinibandInfo


def _make_resp_string(name, cluster_tag, ports):
    return "\n".join(
        json.dumps(
            {
                "switch": "sas-gpuib2-leaf40",
                "port": "21",
                "neighbor_name": name,
                "neighbor_port": port,
                "cluster_tag": cluster_tag,
            }
        )
        for port in ports
    )


def test_sync_infiniband_new(walle_test, mp):
    host = walle_test.mock_host()

    cluster_tag = "YATI2"
    ports = {"mlx5_0", "mlx5_1"}
    mp.function(racktables.request, return_value=_make_resp_string(host.name, cluster_tag, ports))
    _infiniband_info_sync()

    host.reload()
    assert host.infiniband_info.cluster_tag == cluster_tag
    assert set(host.infiniband_info.ports) == set(ports)


def test_sync_infiniband_outdated(walle_test, mp):
    host = walle_test.mock_host()
    host.infiniband_info = InfinibandInfo(cluster_tag="YATI2")
    host.save()

    mp.function(racktables.request, return_value=_make_resp_string("other_host", "YATI1", ["port1"]))
    _infiniband_info_sync()

    host.reload()
    assert not host.infiniband_info


def test_sync_infiniband_update(walle_test, mp):
    host = walle_test.mock_host()
    host.infiniband_info = InfinibandInfo(cluster_tag="YATI1")
    host.save()

    updated_cluster_tag = "YATI2"
    mp.function(racktables.request, return_value=_make_resp_string(host.name, updated_cluster_tag, ["mlx5_0"]))
    _infiniband_info_sync()

    host.reload()
    assert host.infiniband_info.cluster_tag == updated_cluster_tag
