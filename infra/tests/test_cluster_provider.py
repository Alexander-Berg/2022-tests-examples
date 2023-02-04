import grpc
import json
import mock
import pytest

from infra.yasm.gateway.lib.client.cluster_provider import GatewayHost, SolomonGatewayClusterProvider


def test_host_subscribes_to_channel_states():
    channel = mock.Mock()
    callbacks = []
    channel.subscribe.side_effect = lambda cb, _: callbacks.append(cb)
    channel.unsubscribe.side_effect = lambda cb: callbacks.remove(cb)

    host = GatewayHost("host", "sas", channel)
    assert len(callbacks) == 1
    assert channel.subscribe.call_args[0][1]
    channel.reset_mock()

    callbacks[0](grpc.ChannelConnectivity.READY)
    connected = host.check_connection()
    assert connected
    assert len(callbacks) == 1

    callbacks[0](grpc.ChannelConnectivity.IDLE)
    connected = host.check_connection()
    assert not connected
    assert len(callbacks) == 1
    assert channel.subscribe.call_args[0][1]
    channel.reset_mock()

    callbacks[0](grpc.ChannelConnectivity.CONNECTING)
    connected = host.check_connection()
    assert not connected
    assert len(callbacks) == 1


class MockedChannel(object):
    def __init__(self, target, options, compression):
        self.target = target
        self.options = options
        self.compression = compression
        self.callbacks = []

    def subscribe(self, callback, try_to_connect=None):
        self.callbacks.append(callback)

    def unsubscribe(self, callback):
        self.callbacks.remove(callback)


@pytest.fixture
def mocked_grpc_channel(monkeypatch):
    def mocked_insecure_channel(target, options=None, compression=None):
        return MockedChannel(target, options, compression)
    monkeypatch.setattr(grpc, "insecure_channel", mocked_insecure_channel)


def make_discovery_reponse(hosts, port):
    return json.dumps({
        "hosts": [
            {
                "cluster": host[0:3],
                "fqdn": host
            }
            for host in hosts
        ],
        "ports": {
            "grpc": port
        }
    })


def check_cluster_grouping_and_extract(cluster_hosts, port):
    result = set()
    for cluster, hosts in cluster_hosts:
        for host in hosts:
            assert cluster == host.cluster
            assert host.channel.target == "{}:{}".format(host.fqdn, port)
            result.add(host)
    return result


def test_hosts_are_loaded(mocked_grpc_channel):
    cluster_provider = SolomonGatewayClusterProvider()
    host_names = {"sas.1", "sas.2", "man.1", "man.2"}
    cluster_provider.reload_from_str(make_discovery_reponse(
        hosts=host_names,
        port=5740
    ))
    cluster_hosts = cluster_provider.get_cluster_hosts()
    assert 2 == len(cluster_hosts)
    cluster_hosts = check_cluster_grouping_and_extract(cluster_hosts, 5740)
    assert 4 == len(cluster_hosts)
    assert host_names == {host.fqdn for host in cluster_hosts}


def test_channels_are_reused(mocked_grpc_channel):
    cluster_provider = SolomonGatewayClusterProvider()
    host_names = {"sas.1", "sas.2", "man.1", "man.2"}
    cluster_provider.reload_from_str(make_discovery_reponse(
        hosts=host_names,
        port=5740
    ))
    cluster_hosts = check_cluster_grouping_and_extract(cluster_provider.get_cluster_hosts(), 5740)
    assert 4 == len(cluster_hosts)
    assert host_names == {host.fqdn for host in cluster_hosts}
    channels = {host.channel for host in cluster_hosts}

    host_names = {"sas.1", "sas.3", "man.1", "man.2"}
    cluster_provider.reload_from_str(make_discovery_reponse(
        hosts=host_names,
        port=5740
    ))
    cluster_hosts = check_cluster_grouping_and_extract(cluster_provider.get_cluster_hosts(), 5740)
    assert 4 == len(cluster_hosts)
    assert host_names == {host.fqdn for host in cluster_hosts}
    new_channels = {host.channel for host in cluster_hosts}
    assert 3 == len(new_channels & channels)


def test_port_changes_are_detected(mocked_grpc_channel):
    cluster_provider = SolomonGatewayClusterProvider()
    host_names = {"sas.1", "sas.2", "man.1", "man.2"}
    cluster_provider.reload_from_str(make_discovery_reponse(
        hosts=host_names,
        port=5740
    ))
    cluster_hosts = check_cluster_grouping_and_extract(cluster_provider.get_cluster_hosts(), 5740)
    channels = {host.channel for host in cluster_hosts}
    assert 4 == len(channels)

    cluster_provider.reload_from_str(make_discovery_reponse(
        hosts=host_names,
        port=5741
    ))
    cluster_hosts = check_cluster_grouping_and_extract(cluster_provider.get_cluster_hosts(), 5741)
    new_channels = {host.channel for host in cluster_hosts}
    assert 4 == len(new_channels)
    assert 0 == len(new_channels & channels)
