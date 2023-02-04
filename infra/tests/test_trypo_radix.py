from ipaddress import ip_address

import pytest

from walle.trypo_radix import TRYPOCompatibleRadix, extract_project_id, parse_network, ParsedNetwork


class TestParseNetwork:
    @pytest.mark.parametrize(
        "network",
        [
            "2a02:6b8:0::/40",
            "2a02:6b8:c00::1",
            "192.168.0.1/8",
            "192.168.0.1",
        ],
    )
    def test_simple_network_parsing(self, network):
        parsed = parse_network(network)
        assert parsed == ParsedNetwork(network=network, project_id=None, range_prefixlen=None)

    @pytest.mark.parametrize(
        "network, expected",
        [
            ("123@2a02:6b8:0::/40", ParsedNetwork(project_id="123", network="2a02:6b8:0::/40", range_prefixlen=None)),
            ("1@2a02:6b8:0::/80", ParsedNetwork(project_id="1", network="2a02:6b8:0::/80", range_prefixlen=None)),
            (
                "abcdef01@2a02:6b8:0::/24",
                ParsedNetwork(project_id="abcdef01", network="2a02:6b8:0::/24", range_prefixlen=None),
            ),
        ],
    )
    def test_trypo_network_parsing(self, network, expected):
        parsed = parse_network(network)
        assert parsed == expected

    @pytest.mark.parametrize(
        "network, expected",
        [
            (
                "123/12@2a02:6b8:0::/40",
                ParsedNetwork(project_id="123", network="2a02:6b8:0::/40", range_prefixlen="12"),
            ),
            ("1/3@2a02:6b8:0::/80", ParsedNetwork(project_id="1", network="2a02:6b8:0::/80", range_prefixlen="3")),
            (
                "abcdef01/32@2a02:6b8:0::/24",
                ParsedNetwork(project_id="abcdef01", network="2a02:6b8:0::/24", range_prefixlen="32"),
            ),
        ],
    )
    def test_trypo_range_network_parsing(self, network, expected):
        parsed = parse_network(network)
        assert parsed == expected


@pytest.mark.parametrize(
    "ip, expected_project_id",
    [
        ("::1", "00000000"),
        ("2a02:6b8:c0c:f80:0:1329::1", "00001329"),
        ("2a02:6b8:c0c:f80:dead:beef::1", "deadbeef"),
    ],
)
def test_extract_project_id(ip, expected_project_id):
    assert extract_project_id(ip_address(ip)) == expected_project_id


class TestTRYPOCompatibleRadix:
    @pytest.fixture
    def radix(self):
        return TRYPOCompatibleRadix()

    def test_single_ips_v4(self, radix):
        assert radix.search_best("127.0.0.1") is None
        radix.add("127.0.0.1")
        assert radix.search_best("127.0.0.1").prefix == "127.0.0.1/32"

    def test_single_ips_v6(self, radix):
        assert radix.search_best("2a02:6b8::1") is None
        radix.add("2a02:6b8::1")
        assert radix.search_best("2a02:6b8::1").prefix == "2a02:6b8::1/128"

    def test_classic_networks_v4(self, radix):
        assert radix.search_best("127.0.0.1") is None
        radix.add("127.0.0.0/31")
        assert radix.search_best("127.0.0.1").prefix == "127.0.0.0/31"

    def test_classic_networks_v6(self, radix):
        assert radix.search_best("2a02:6b8::1") is None
        radix.add("2a02:6b8::/127")
        match = radix.search_best("2a02:6b8::1")
        assert match.prefix == "2a02:6b8::/127"
        assert radix.node_repr(match) == "2a02:6b8::/127"

    def test_trypo_networks(self, radix):
        assert radix.search_best("2a02:6b8:c0c:f80:0:1329::1") is None
        radix.add("1329@2a02:6b8:c00::/40")

        match = radix.search_best("2a02:6b8:c0c:f80:0:1329::1")
        assert match.prefix == "2a02:6b8:c00::/40"
        assert radix.node_repr(match) == "00001329@2a02:6b8:c00::/40"

        assert radix.search_best("2a02:6b8:c0c:f80:0:1328::1") is None

    def test_trypo_range_network(self, radix):
        assert radix.search_best("2a02:6b8:c0c:f80:dead:1329::1") is None
        radix.add("dead0000/16@2a02:6b8:c00::/40")
        radix.add("dead0000/16@2aaa:6b8:c00::/40")

        match = radix.search_best("2a02:6b8:c0c:f80:dead:1329::1")
        assert match.prefix == "2a02:6b8:c00::/40"
        assert radix.node_repr(match) == "dead0000/16@2a02:6b8:c00::/40"

        assert radix.search_best("2a02:6b8:c0c:f80:dead:beef::1").prefix == "2a02:6b8:c00::/40"
        assert radix.search_best("2aaa:6b8:c0c:f80:dead:beef::1").prefix == "2aaa:6b8:c00::/40"
        assert radix.search_best("2a02:6b8:c0c:f80:deaf:1329::1") is None

    def test_prefers_trypo_matches_if_prefixlen_is_longer(self, radix):
        classic = "2a02:6b8:c00::/40"
        trypo = "1329@2a02:6b8:c00::/40"
        radix.add(classic)
        radix.add(trypo)
        ip = "2a02:6b8:c00:0:0:1329::ffff"  # matches both classic and trypo

        match = radix.search_best(ip)
        assert match.prefix == classic
        assert match.data.get("project_id") == "00001329"

    def test_prefers_classic_matches_if_prefixlen_is_longer(self, radix):
        classic = "2a02:6b8:c00::1329:0:0/96"
        trypo = "1329@2a02:6b8:c00::/40"
        radix.add(classic)
        radix.add(trypo)
        ip = "2a02:6b8:c00::1329:0:ffff"  # matches both classic and trypo

        match = radix.search_best(ip)
        assert match.prefix == classic
        assert match.data == {}
