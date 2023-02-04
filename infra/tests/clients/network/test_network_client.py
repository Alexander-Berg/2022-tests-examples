import pytest

from walle.clients.network.network_client import NetworkClient


class TestNetworkClientAbstractMethods:
    def test_get_switch_ports(self):
        with pytest.raises(NotImplementedError):
            NetworkClient.get_switch_ports()

    def test_interconnect_switch_set(self):
        with pytest.raises(NotImplementedError):
            NetworkClient._interconnect_switch_set()


class TestNetworkClientMethods:
    class MockNetworkClient(NetworkClient):
        @staticmethod
        def get_switch_ports():
            return {"dd": ["fa1", "gi1"], "tt": ["portname2", "portname3"]}

        @staticmethod
        def _interconnect_switch_set():
            return {1, 2, 3}

    def test_is_interconnect_switch(self):
        client = self.MockNetworkClient()
        assert client.is_interconnect_switch(1)
        assert not client.is_interconnect_switch(4)

    @pytest.mark.parametrize(
        ["switch", "shorten_switch"], [("dd.yndx.net", "dd"), ("dd.netinfra.cloud.yandex.net", "dd"), ("dd", "dd")]
    )
    @pytest.mark.parametrize(["port", "shorten_port"], [("FastEthernet1", "fa1"), ("GigabitEthernet1", "gi1")])
    def test_shorten_switch_port_name_successfuly(self, switch, shorten_switch, port, shorten_port):
        client = self.MockNetworkClient()

        result = client.shorten_switch_port_name(switch, port)
        assert result == (shorten_switch, shorten_port)
