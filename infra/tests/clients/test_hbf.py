from infra.walle.server.tests.lib.util import mock_response
from walle.clients import utils
from walle.clients.hbf import get_hbf_drills, get_macro_networks, get_hosts_of_macro
from walle.hosts import HostLocation


def test_hbf_drills(walle_test, mp, load_test_json):
    expected = load_test_json("mocks/hbf-drills.json")
    mp.request(mock_response(expected))

    resp = get_hbf_drills()
    assert resp == expected


def test_get_macro_networks(walle_test, mp):
    expected = ["41af@2a02:6b8:c00::/40"]
    mp.request(mock_response(expected))
    request_mock = mp.function(utils.request, wrap_original=True)

    resp = get_macro_networks("_MDS_STORAGE_NETS_")
    assert request_mock.call_args[0] == ('hbf', 'GET', 'https://hbf.yandex.net/macros/_MDS_STORAGE_NETS_')
    assert resp == expected


def test_get_hosts_of_macro(walle_test, mp):
    macro_networks = mp.function(get_macro_networks, return_value=["41af@2a02:6b8:c00::/40"])

    walle_test.mock_host({"name": "no-ip", "location": HostLocation(short_datacenter_name="sas"), "ips": [], "inv": 1})
    walle_test.mock_host(
        {
            "name": "non-matching-dc",
            "ips": ["2a02:6b8:c00:0:0:41af::1"],
            "location": HostLocation(short_datacenter_name="man"),
            "inv": 2,
        }
    )
    walle_test.mock_host(
        {
            "name": "no-project-id",
            "ips": ["2a02:6b8:c00::1"],
            "location": HostLocation(short_datacenter_name="sas"),
            "inv": 3,
        }
    )
    walle_test.mock_host(
        {
            "name": "matching",
            "ips": ["2a02:6b8:c00:0:0:41af::2"],
            "location": HostLocation(short_datacenter_name="sas"),
            "inv": 4,
        }
    )

    matching = get_hosts_of_macro("_MDS_STORAGE_NETS_", dc="sas")

    assert macro_networks.called
    assert matching == {'matching': '2a02:6b8:c00:0:0:41af::2 matches 000041af@2a02:6b8:c00::/40'}
