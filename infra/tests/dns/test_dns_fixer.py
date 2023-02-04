"""Tests hosts' DNS records management."""

from functools import partial
from itertools import count

import mock
import pytest

import walle.clients.dns.slayer_dns_api
import walle.dns.dns_fixer
import walle.dns.dns_lib
from infra.walle.server.tests.lib import util
from infra.walle.server.tests.lib.util import TestCase
from walle import restrictions, network
from walle.application import app
from walle.constants import (
    NETWORK_SOURCE_LLDP,
    MAC_SOURCE_AGENT,
    VLAN_SCHEME_STATIC,
    VLAN_SCHEME_SEARCH,
    VLAN_SCHEME_MTN,
    VLAN_SCHEME_MTN_HOSTID,
)
from walle.dns import dns_fixer
from walle.errors import InvalidHostConfiguration
from walle.hosts import HostState, HostLocation, DnsConfiguration, HostStatus, HostMessage
from walle.models import timestamp
from walle.network import DnsRecord
from walle.util import mongo
from walle.util.misc import drop_none

MAC1 = "01:02:03:04:05:06"
MAC2 = "11:12:13:14:15:16"

SWITCH1 = "switch-mock1"
SWITCH2 = "switch-mock2"

PROJECT2 = "other-project"

VLANS1 = [666]
VLANS2 = [1035, 1036]

IPS1 = ["192.168.1.3", "2a02:6b8:c03:72a:0:604:a627:d4db"]
IPS2 = ["192.168.0.4", "2a02:6b8:c03:72a:0:604:a627:beef"]


@pytest.fixture
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


@pytest.yield_fixture(autouse=True)
def tier_2():
    yield from util.tier_2()


def _mock_host_network(test, inv, project=None, state=HostState.ASSIGNED, dns_conf=None, dns_conf_kwargs={}, **kwargs):
    host_params = drop_none(dict(inv=inv, project=project, state=state))
    host_params.update(_hw_kwargs())
    host_params.update(kwargs)

    if dns_conf is not None:
        dns_conf = dict(dns_conf)
        dns_conf.update(dns_conf_kwargs)
        host_params["dns"] = DnsConfiguration(**dns_conf)

    host = test.mock_host(host_params)
    return host, test.mock_host_network(_nw_kwargs(), host=host)


@pytest.mark.parametrize("with_global_automation", (True, False))
@pytest.mark.parametrize("with_project_automation", (True, False))
def test_fix_records(test, mp, monkeypatch_locks, with_global_automation, with_project_automation):
    fixed_hosts = []

    def fix_host_dns_records(host, *args):
        fixed_hosts.append(host.inv)

    vlan_config = network._VlanConfig(VLANS1, VLANS1[0], 1388)
    mp.function(walle.dns.dns_fixer._fix_host_dns_records, side_effect=fix_host_dns_records)
    mp.function(walle.network.get_host_expected_vlans, return_value=vlan_config)

    settings = app.settings()
    settings.disable_healing_automation = True
    settings.disable_dns_automation = not with_global_automation
    settings.save()

    unmanaged_project_by_vlan_scheme = test.mock_project(
        dict(
            id="unmanaged-vlan",
            healing_automation={"enabled": False},
            vlan_scheme=None,
            dns_automation={"enabled": with_project_automation},
            dns_domain="fake.yandex.net",
        )
    )
    unmanaged_project_by_dns_domain = test.mock_project(
        dict(
            id="unmanaged-dns",
            healing_automation={"enabled": False},
            vlan_scheme=None,
            dns_automation={"enabled": with_project_automation},
            dns_domain="fake.yandex.net",
        )
    )
    project_search = test.mock_project(
        dict(
            id="managed-search",
            healing_automation={"enabled": False},
            dns_automation={"enabled": with_project_automation},
            dns_domain="fake.yandex.net",
            vlan_scheme=VLAN_SCHEME_SEARCH,
        )
    )
    project_mtn = test.mock_project(
        dict(
            id="managed-mtn",
            healing_automation={"enabled": False},
            dns_automation={"enabled": with_project_automation},
            dns_domain="fake.yandex.net",
            vlan_scheme=VLAN_SCHEME_MTN,
        )
    )
    project_mtn_hostid = test.mock_project(
        dict(
            id="managed-mtn-hostid",
            healing_automation={"enabled": False},
            dns_automation={"enabled": with_project_automation},
            dns_domain="fake.yandex.net",
            vlan_scheme=VLAN_SCHEME_MTN_HOSTID,
        )
    )
    project_static = test.mock_project(
        dict(
            id="managed-static",
            healing_automation={"enabled": False},
            dns_automation={"enabled": with_project_automation},
            dns_domain="fake.yandex.net",
            vlan_scheme=VLAN_SCHEME_STATIC,
        )
    )

    up2date_dns_conf = dict(
        mac=MAC1,
        switch=SWITCH1,
        project=project_search.id,
        vlans=VLANS1,
        vlan_scheme=VLAN_SCHEME_SEARCH,
        check_time=timestamp() - dns_fixer._CHECK_PERIOD,
    )

    outdated_dns_conf = dict(
        mac=MAC2,
        switch=SWITCH2,
        project=PROJECT2,
        vlans=VLANS2,
        vlan_scheme=VLAN_SCHEME_SEARCH,
        check_time=timestamp() - dns_fixer._CHECK_PERIOD,
    )

    next_inv = partial(next, count())

    def _mock_host(project=project_search.id, state=HostState.ASSIGNED, dns_conf=None, dns_conf_kwargs={}, **kwargs):
        return _mock_host_network(
            test,
            inv=next_inv(),
            project=project,
            state=state,
            dns_conf=dns_conf,
            dns_conf_kwargs=dns_conf_kwargs,
            **kwargs
        )[0]

    test.mock_host(dict(inv=next_inv(), project=project_search.id, state=HostState.FREE, **_hw_kwargs()))

    no_dns_host = _mock_host()
    _mock_host(project=unmanaged_project_by_vlan_scheme.id)
    _mock_host(project=unmanaged_project_by_dns_domain.id)
    # restricted_host
    _mock_host(restrictions=[restrictions.AUTOMATION])
    # partially_freed_host
    _mock_host(name=network.get_free_host_name_template().fill(1))
    # up_to_date_host
    _mock_host(dns_conf=up2date_dns_conf)

    up_to_date_recheck_search_host = _mock_host(
        dns_conf=up2date_dns_conf, dns_conf_kwargs=dict(check_time=timestamp() - dns_fixer._FULL_CHECK_PERIOD)
    )
    up_to_date_recheck_static_host = _mock_host(
        project=project_static.id,
        dns_conf=up2date_dns_conf,
        dns_conf_kwargs=dict(check_time=timestamp() - dns_fixer._FULL_CHECK_PERIOD),
    )
    up_to_date_recheck_mtn_host = _mock_host(
        project=project_mtn.id,
        dns_conf=up2date_dns_conf,
        dns_conf_kwargs=dict(check_time=timestamp() - dns_fixer._FULL_CHECK_PERIOD),
    )
    up_to_date_recheck_mtn_hostid_host = _mock_host(
        project=project_mtn_hostid.id,
        dns_conf=up2date_dns_conf,
        dns_conf_kwargs=dict(check_time=timestamp() - dns_fixer._FULL_CHECK_PERIOD),
    )

    outdated_mac_host = _mock_host(dns_conf=up2date_dns_conf, dns_conf_kwargs=dict(mac=MAC2))
    outdated_switch_host = _mock_host(dns_conf=up2date_dns_conf, dns_conf_kwargs=dict(switch=SWITCH2))
    outdated_project_host = _mock_host(dns_conf=up2date_dns_conf, dns_conf_kwargs=dict(project=PROJECT2))
    outdated_vlan_scheme_host = _mock_host(
        dns_conf=up2date_dns_conf, dns_conf_kwargs=dict(vlan_scheme=VLAN_SCHEME_STATIC)
    )
    outdated_vlans_host = _mock_host(dns_conf=up2date_dns_conf, dns_conf_kwargs=dict(vlans=VLANS2))

    # outdated_with_check_time_host
    _mock_host(dns_conf=outdated_dns_conf, dns_conf_kwargs=dict(check_time=timestamp()))

    # outdated_with_error_time_host
    _mock_host(
        dns_conf=outdated_dns_conf,
        dns_conf_kwargs=dict(check_time=0, error_time=timestamp() - dns_fixer._ERROR_CHECK_PERIOD + 1),
    )

    on_maintenance_host = _mock_host(
        state=HostState.MAINTENANCE, status=HostStatus.default(HostState.MAINTENANCE), dns_conf=outdated_dns_conf
    )

    # outdated_with_null_active_mac_address_host
    _mock_host(dns_conf=outdated_dns_conf, active_mac=None)

    outdated_ips_differ_host = _mock_host(dns_conf=up2date_dns_conf, dns_conf_kwargs=dict(ips=IPS2), ips=IPS1)

    partitioner = mongo.MongoPartitionerService("some")
    partitioner.start()
    dns_fixer._DNS_FIXER.run(partitioner, only_l3_search_hosts=False)

    if with_global_automation and with_project_automation:
        assert sorted(fixed_hosts) == sorted(
            [
                no_dns_host.inv,
                up_to_date_recheck_search_host.inv,
                up_to_date_recheck_mtn_host.inv,
                up_to_date_recheck_mtn_hostid_host.inv,
                up_to_date_recheck_static_host.inv,
                outdated_mac_host.inv,
                outdated_switch_host.inv,
                outdated_project_host.inv,
                outdated_vlan_scheme_host.inv,
                outdated_vlans_host.inv,
                on_maintenance_host.inv,
                outdated_ips_differ_host.inv,
            ]
        )
    else:
        assert not fixed_hosts

    test.hosts.assert_equal()


def test_fix_ok_records(mp, test):
    mp.function(
        walle.network.get_host_dns_records,
        return_value=[
            DnsRecord("AAAA", "fqdn1", ["2a02:06b8:b011:0070:0225:90ff:fe88:b334"]),
            DnsRecord("A", "fqdn1", ["192.168.1.10"]),
            DnsRecord("AAAA", "fqdn2", ["2a02:06b8:b010:0070:0225:90ff:fe88:b334"]),
        ],
    )
    mp.function(dns_fixer.get_operations_for_dns_records, module=dns_fixer, return_value=[])

    host, host_network = _mock_host_network(test, inv=0, state=HostState.ASSIGNED)

    dns_conf = _mock_dns_conf(host)
    dns_fixer._fix_host_dns_records(host.copy(), host_network, test.default_project, dns_conf, test.statbox_logger)
    host.dns = DnsConfiguration(
        switch=SWITCH1,
        mac=MAC1,
        project=host.project,
        vlan_scheme=VLAN_SCHEME_SEARCH,
        vlans=VLANS1,
        ips=IPS1,
        check_time=timestamp(),
    )

    test.hosts.assert_equal()


def test_clear_error_if_no_change(mp, test, monkeypatch_locks):
    # mp.function(dns_fixer.get_operations_for_dns_records, module=dns_fixer, return_value=[])

    vlan_config = network._VlanConfig(VLANS1, VLANS1[0], 1388)
    mp.function(walle.network.get_host_expected_vlans, return_value=vlan_config)

    project_search = test.mock_project(
        dict(
            id="managed-search",
            healing_automation={"enabled": False},
            dns_automation={"enabled": True},
            dns_domain="fake.yandex.net",
            vlan_scheme=VLAN_SCHEME_SEARCH,
        )
    )

    host, host_network = _mock_host_network(
        test,
        inv=0,
        state=HostState.ASSIGNED,
        project=project_search.id,
        messages={"dns_fixer": [HostMessage.error("mock")]},
        dns=DnsConfiguration(
            switch=SWITCH1,
            mac=MAC1,
            project=project_search.id,
            vlan_scheme=VLAN_SCHEME_SEARCH,
            vlans=VLANS1,
            check_time=timestamp() - dns_fixer._CHECK_PERIOD - 1,
        ),
    )

    shard = mongo.MongoPartitionerShard("0", mock.Mock())
    dns_fixer._DNS_FIXER._fix_dns_records(shard, {project_search.id: project_search}, False)

    host.messages = {}
    test.hosts.assert_equal()


@pytest.mark.parametrize("with_dns", (True, False))
def test_fix_with_invalid_host_configuration(mp, test, with_dns):
    mp.function(walle.network.get_host_dns_records, side_effect=InvalidHostConfiguration("Mock error"))

    host, host_network = _mock_host_network(test, inv=0, state=HostState.ASSIGNED)
    if with_dns:
        host.dns = DnsConfiguration(switch=SWITCH2, mac=MAC2, project=host.project)
        host.save()

    dns_conf = _mock_dns_conf(host)
    dns_fixer._fix_host_dns_records(host.copy(), host_network, test.default_project, dns_conf, test.statbox_logger)
    host.dns = DnsConfiguration(check_time=timestamp())
    host.messages["dns_fixer"] = [HostMessage.error("Failed to check DNS records: Mock error.")]

    test.hosts.assert_equal()


def test_fix_invalid_records(mp, test, mp_juggler_source):
    mp.function(
        walle.network.get_host_dns_records,
        return_value=[
            DnsRecord("AAAA", "fqdn1", ["2a02:06b8:b011:0070:0225:90ff:fe88:b334"]),
            DnsRecord("A", "fqdn1", ["192.168.1.10"]),
            DnsRecord("AAAA", "fqdn2", ["2a02:06b8:b010:0070:0225:90ff:fe88:b334"]),
        ],
    )
    mp.function(
        dns_fixer.get_operations_for_dns_records,
        module=dns_fixer,
        return_value=[
            walle.clients.dns.DnsApiOperation.delete("A", "fqdn2", "127.0.0.1"),
        ],
    )
    mock_dns_client = mp.function(walle.clients.dns.slayer_dns_api.DnsClient)

    host, host_network = _mock_host_network(test, inv=0)

    dns_config = _mock_dns_conf(host)
    dns_fixer._fix_host_dns_records(host.copy(), host_network, test.default_project, dns_config, test.statbox_logger)
    host.dns = DnsConfiguration(
        switch=SWITCH1,
        mac=MAC1,
        project=host.project,
        vlans=VLANS1,
        vlan_scheme=VLAN_SCHEME_SEARCH,
        ips=IPS1,
        check_time=timestamp(),
        update_time=timestamp(),
    )

    test.hosts.assert_equal()
    mock_dns_client.return_value.apply_operations.assert_called_once_with(
        [
            walle.clients.dns.DnsApiOperation.delete("A", "fqdn2", "127.0.0.1"),
        ]
    )


def test_mismatching_ips_interrupt_and_save_message(mp, test, mp_juggler_source):
    mp.function(
        walle.network.get_host_dns_records,
        return_value=[
            DnsRecord("AAAA", "fqdn1", ["2a02:06b8:b011:0070:0225:90ff:fe88:b334"]),
            DnsRecord("A", "fqdn1", ["192.168.1.10"]),
            DnsRecord("AAAA", "fqdn2", ["2a02:06b8:b010:0070:0225:90ff:fe88:b334"]),
        ],
    )
    mp.function(
        dns_fixer.get_operations_for_dns_records,
        module=dns_fixer,
        return_value=[
            walle.clients.dns.DnsApiOperation.delete("A", "fqdn2", "127.0.0.1"),
        ],
    )
    mock_dns_client = mp.function(walle.clients.dns.slayer_dns_api.DnsClient)

    # ips don't match ips from dns
    host_ips = ["2a02:06b8:b011:0070:0225:90ff:fe88:b334", "192.168.1.99"]
    host = test.mock_host(dict(inv=0, state=HostState.ASSIGNED, ips=host_ips, **_hw_kwargs()))

    with pytest.raises(dns_fixer.IPsMismatch) as exc_info:
        dns_fixer._fix_host_dns_records(
            host.copy(), None, test.default_project, _mock_dns_conf(host), test.statbox_logger
        )
    message = "Cannot fix DNS records of {}: {}".format(host.human_name(), exc_info.value)
    host.messages["dns_fixer"] = [HostMessage.error(message)]

    test.hosts.assert_equal()
    assert not mock_dns_client.return_value.apply_operations.called


def _mock_dns_conf(host, **extra):
    dns_conf = dict(
        mac=MAC1,
        switch=SWITCH1,
        project_id=host.project,
        vlans=VLANS1,
        vlan_scheme=VLAN_SCHEME_SEARCH,
        mac_source="eine",
        switch_source="eine",
        ips=IPS1,
    )
    dns_conf.update(extra)
    return dns_fixer._HostDnsConfig(**dns_conf)


def _hw_kwargs(**kwargs):
    default = {
        'active_mac': MAC1,
        'active_mac_source': MAC_SOURCE_AGENT,
        'location': HostLocation(switch=SWITCH1, network_source=NETWORK_SOURCE_LLDP),
    }
    default.update(kwargs)
    return default


def _nw_kwargs(**kwargs):
    default = {
        'active_mac_time': timestamp(),
        'network_switch': SWITCH1,
        'network_source': NETWORK_SOURCE_LLDP,
        'network_timestamp': timestamp(),
    }
    default.update(kwargs)
    return default
