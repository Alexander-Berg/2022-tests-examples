# -*- coding: utf-8 -*-

from irt.bannerland.hosts import get_full_hosts_info, get_host_datacenter


def test_hosts_dc():
    for host in get_full_hosts_info():
        dc = get_host_datacenter(host)
        assert isinstance(dc, str), "Invalid datacenter type for host '{}'.".format(host)
        assert dc, "Invalid datacenter '{}' for host '{}'.".format(dc, host)
