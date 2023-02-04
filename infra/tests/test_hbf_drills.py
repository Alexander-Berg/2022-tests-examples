from unittest.mock import Mock

import pytest

from infra.walle.server.tests.lib.util import mock_hbf_drill
from walle import hbf_drills
from walle.expert.constants import HW_WATCHER_CHECK_MAX_POSSIBLE_DELAY
from walle.hosts import Host, HostLocation
from walle.models import monkeypatch_timestamp


class TestMakeDrillObj:
    raw = {
        "project": "_LBKNETS_",
        "project_ips": [
            "1517@2a02:6b8:c00::/40",
            "444a@2a02:6b8:c00::/40",
        ],
        "location": "_DC_SAS_NETS_",
        "location_ips": [
            "5.45.214.0/29",
            "2a02:6b8:0:1a05::/64",
        ],
        "begin": 1580983357,
        "duration": 3600,
        "exclude": [],
        "exclude_ips": [
            "2a02:6b8:fc0a::/48",
        ],
    }

    def test_convert(self):
        res = hbf_drills._make_drill_obj(self.raw)
        expected_dict = {
            "id": "sas|_LBKNETS_|1580983357|1580986957",
            "project_macro": "_LBKNETS_",
            "location": "sas",
            "start_ts": 1580983357,
            "end_ts": 1580986957,
            "project_ips": ["1517@2a02:6b8:c00::/40", "444a@2a02:6b8:c00::/40"],
            "exclude_ips": ["2a02:6b8:fc0a::/48"],
        }
        assert res.to_api_obj() == expected_dict


class TestProcessHbfDrills:
    def test_adds_new(self, hbf_drills_mocker, mp):
        drills = [
            mock_hbf_drill(hbf_drills_mocker, {"id": "sas|_LBKNETS_|1580983357|1580986957"}, save=False),
            mock_hbf_drill(hbf_drills_mocker, {"id": "man|_BLANETS_|1580983300|1580986900"}, save=False),
        ]
        self._mock_incoming(mp, drills)

        hbf_drills._process_hbf_drills()

        hbf_drills_mocker.assert_equal()

    def test_updates_changed(self, hbf_drills_mocker, walle_test, mp):
        existing = mock_hbf_drill(hbf_drills_mocker, {"id": "vla|_EXISTING_|1580983357|1580986957"})

        new_ips = ["2a02:6b8:b010:4100::/56"]
        self._mock_incoming(
            mp,
            [
                mock_hbf_drill(
                    hbf_drills_mocker,
                    # ips changed
                    {"id": "vla|_EXISTING_|1580983357|1580986957", "project_ips": new_ips},
                    save=False,
                    add=False,
                )
            ],
        )

        hbf_drills._process_hbf_drills()

        existing.project_ips = new_ips
        existing.set_hash()
        hbf_drills_mocker.assert_equal()

    def test_deletes_not_started(self, hbf_drills_mocker, mp):
        existing = mock_hbf_drill(hbf_drills_mocker, {"id": "vla|_EXISTING_|100|1000"})
        self._mock_incoming(mp, [])
        monkeypatch_timestamp(mp, cur_time=99)

        hbf_drills._process_hbf_drills()

        hbf_drills_mocker.remove(existing)
        hbf_drills_mocker.assert_equal()

    def test_deletes_completely_finished(self, hbf_drills_mocker, mp):
        existing = mock_hbf_drill(hbf_drills_mocker, {"id": "vla|_EXISTING_|100|1000"})
        self._mock_incoming(mp, [])
        monkeypatch_timestamp(mp, cur_time=existing.obsoletion_ts + 1)

        hbf_drills._process_hbf_drills()

        hbf_drills_mocker.remove(existing)
        hbf_drills_mocker.assert_equal()

    def test_defers_inprocess_drill_deletion_if_feasible(self, hbf_drills_mocker, mp):
        existing = mock_hbf_drill(hbf_drills_mocker, {"id": "vla|_EXISTING_|100|1000"})  # obsoletion_ts is 3200
        self._mock_incoming(mp, [])
        cur_ts = 200
        time_mocker = monkeypatch_timestamp(mp, cur_time=cur_ts)

        hbf_drills._process_hbf_drills()

        existing.end_ts = cur_ts  # now obsoletion_ts is 2420, which will happen sooner
        existing.set_hash()
        hbf_drills_mocker.assert_equal()

        cron_job_period = 15 * 60
        for _ in range(2):
            time_mocker.bump_time(cron_job_period)  # 1100, 2000
            hbf_drills._process_hbf_drills()
            hbf_drills_mocker.assert_equal()

        time_mocker.bump_time(cron_job_period)  # 2900 > 2420
        hbf_drills._process_hbf_drills()
        hbf_drills_mocker.remove(existing)
        hbf_drills_mocker.assert_equal()

    def test_doesnt_change_drill_if_not_feasible(self, hbf_drills_mocker, mp):
        existing = mock_hbf_drill(hbf_drills_mocker, {"id": "vla|_EXISTING_|100|200"})  # obsoletion_ts is 2420
        self._mock_incoming(mp, [])
        cur_ts = 300  # between end_ts and obsoletion_ts
        time_mocker = monkeypatch_timestamp(mp, cur_time=cur_ts)

        cron_job_period = 15 * 60
        for _ in range(2):
            time_mocker.bump_time(cron_job_period)  # 1200, 2100
            hbf_drills._process_hbf_drills()
            hbf_drills_mocker.assert_equal()

        time_mocker.bump_time(cron_job_period)  # 3000 > 2420
        hbf_drills._process_hbf_drills()
        hbf_drills_mocker.remove(existing)
        hbf_drills_mocker.assert_equal()

    def _mock_incoming(self, mp, incoming):
        mp.function(hbf_drills._get_incoming_drills, return_value={d.id: d for d in incoming})


def get_matching_hosts(matching_ips):
    return [
        Host(**{"name": "mock-host", "location": HostLocation(short_datacenter_name="vla"), "ips": [ip]})
        for ip in matching_ips
    ]


class TestDrillHostMatcher:
    start_ts = 1580983357
    end_ts = start_ts + 3600
    cur_ts = start_ts + 500

    # ips matching project_ips
    matching_ips = [
        "2a02:6b8:b010:4100::ffff",  # usual ipv6
        "2a02:6b8:c00:0:0:45b7:0:ffff",  # trypo
        "2a02:6b8:c00:0:0123:aaaa:0:ffff",  # trypo range
        "2a02:6b8:c00:0:0123:eeee:0:ffff",  # trypo range
    ]
    # ips matching exclude_ips
    excluded_ips = [
        "2a02:6b8:b010:4100::1234",  # explicitly excluded ip
        "2a02:6b8:b010:4100:1234::ffff",  # excluded network
        "2a02:06b8:0c00:0aaa:0:45b7:0:ffff",  # excluded trypo network
        "2a02:06b8:0c00:0aaa:0123::ffff",  # excluded trypo network range
        "2a02:06b8:0c00:0aaa:0123:aaaa::ffff",
        "2a02:06b8:0c00:0aaa:0123:ffff::ffff",
    ]

    @pytest.fixture
    def matcher(self):
        drill_props = {
            "location": "vla",
            "start_ts": self.start_ts,
            "end_ts": self.end_ts,
            "project_ips": [
                "2a02:6b8:b010:4100::/56",  # usual ipv6
                "45b7@2a02:6b8:c00:0::/40",  # trypo
                "45b7@3a02:6b8:c00:0::/40",
                "1230000/16@2a02:6b8:c00::/40",  # trypo range
            ],
            "exclude_ips": [
                "2a02:6b8:b010:4100::1234",  # exclude ip (from first project network)
                "2a02:6b8:b010:4100:1234::/72",  # exclude network (subnet of first project network)
                "45b7@2a02:06b8:c00:0aaa::/64",  # exclude trypo network (subnet of second project network)
                "1230000/16@2a02:06b8:0c00:0aaa::/64",  # exclude trypo network range (subnet of fourth project network)
            ],
            "project_macro": "_LBKNETS_",
        }
        drill = hbf_drills.HbfDrill(**drill_props)
        drill.mk_id()
        drill.set_hash()
        return hbf_drills.DrillHostMatcher(drill)

    @pytest.fixture
    def matching_ts(self, mp):
        monkeypatch_timestamp(mp, cur_time=self.cur_ts)

    @pytest.mark.parametrize("matching_host", get_matching_hosts(matching_ips))
    def test_matches(self, matcher, matching_host, matching_ts):
        assert matcher.match_host(matching_host)

    @pytest.mark.parametrize("time_inc", [-1, 3600 + HW_WATCHER_CHECK_MAX_POSSIBLE_DELAY + 1])
    @pytest.mark.parametrize("matching_host", get_matching_hosts(matching_ips))
    def test_filters_by_time(self, matcher, matching_host, time_inc, mp):
        monkeypatch_timestamp(mp, cur_time=self.start_ts + time_inc)
        assert not matcher.match_host(matching_host)

    @pytest.mark.parametrize("ip", excluded_ips)
    @pytest.mark.parametrize("matching_host", get_matching_hosts(matching_ips))
    def test_skips_excluded_hosts(self, matcher, matching_host, matching_ts, ip):
        matching_host.ips = [ip]
        assert not matcher.match_host(matching_host)


def test_get_hbf_drills(hbf_drills_mocker):
    expected_drills = [
        mock_hbf_drill(hbf_drills_mocker, {"id": "man|_PROJ1_|1580983357|1580986957"}),
        mock_hbf_drill(hbf_drills_mocker, {"id": "sas|_PROJ2_|1580983357|1580986957"}),
        mock_hbf_drill(hbf_drills_mocker, {"id": "val|_PROJ3_|1580983357|1580986957"}),
    ]
    assert hbf_drills.get_hbf_drills() == expected_drills


class TestHBFDrillsCollection:
    def test_filters_hosts_without_ips(self, walle_test):
        host = Host(ips=[], location=HostLocation(short_datacenter_name="vla"))
        drill_col = hbf_drills.HbfDrillsCollection([])
        assert not drill_col.get_host_inclusion_reason(host)

    def test_filters_hosts_without_matching_dc(self, walle_test):
        host = Host(ips=["2a02:06b8:0c00:0aaa:0123:ffff::ffff"], location=HostLocation(short_datacenter_name="vla"))
        drill_col = hbf_drills.HbfDrillsCollection([])
        man_drill_matcher = Mock()
        drill_col.dc_to_drills["man"] = [man_drill_matcher]

        assert not drill_col.get_host_inclusion_reason(host)
        assert not man_drill_matcher.match_host.called

    def test_searches_for_match_in_matching_locations(self, walle_test, hbf_drills_mocker):
        host = Host(ips=["2a02:06b8:0c00:0aaa:0123:ffff::ffff"], location=HostLocation(short_datacenter_name="vla"))
        drill_col = hbf_drills.HbfDrillsCollection([])

        vla_drill_matchers = [Mock(), Mock()]
        vla_drill_matchers[0].match_host.return_value = None

        reason = "reason-mock"
        vla_drill_matchers[1].match_host.return_value = reason

        drill_col.dc_to_drills["vla"] = vla_drill_matchers

        assert drill_col.get_host_inclusion_reason(host) == reason
        assert all(matcher.match_host.called for matcher in vla_drill_matchers)
