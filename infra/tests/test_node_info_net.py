import socket
import mock
import infra.rtc.nodeinfo.lib.modules.net as net


def test_get_bandwidth():
    mock_open = mock.mock_open(read_data='25000\n')
    bw = net.get_bandwidth('mock', mock_open)
    assert bw == 25000
    mock_open.assert_called_with('/sys/class/net/mock/speed', 'r')


def test_get_network_info(monkeypatch):
    gethostname_mock = mock.Mock(return_value='host.mock')
    monkeypatch.setattr(socket, 'gethostname', gethostname_mock)

    def _get_ya_netconfig_state():
        return {
            "bb_iface": "eth1",
            "fb_net": "2a02:6b8:fc10:807::/64",
            "bb_net": "2a02:6b8:c00:807::/64",
            "bb_ipv6_addr": "2a02:6b8:c03:317:0:604:5c35:2d",
            "fb_ipv6_addr": "2a02:6b8:fc03:16:0:604:5c35:2d",
        }
    monkeypatch.setattr(net, 'get_ya_netconfig_state', _get_ya_netconfig_state)

    get_bw_mock = mock.Mock(return_value=25000)
    monkeypatch.setattr(net, 'get_bandwidth', get_bw_mock)

    info = net.get_network_info()

    get_bw_mock.assert_called_with('eth1')
    assert info.interface_name == 'eth1'
    assert info.bb_prefix == '2a02:6b8:c00:807::/64'
    assert info.fb_prefix == '2a02:6b8:fc10:807::/64'
    assert info.bb_fqdn == 'host.mock'
    assert info.fb_fqdn == 'fb-host.mock'
    assert info.bb_ipv6_addr == '2a02:6b8:c03:317:0:604:5c35:2d'
    assert info.fb_ipv6_addr == '2a02:6b8:fc03:16:0:604:5c35:2d'
    assert info.bandwidth == 25000
