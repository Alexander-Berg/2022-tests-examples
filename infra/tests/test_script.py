from infra.rtc.rebootctl.lib import script
from hosts_data_source import hostlist


def test_hosts_portion_g_nothing():
    """
    check, what function `hosts_portion_g` returns full list without grouping options
    """
    plist = list(script.hosts_portion_g(hostlist))
    assert len(plist) == 1
    assert all(i in hostlist for i in plist[0][0])
    assert plist[0][1] is None


def test_hosts_portion_g_group_byack():
    """
    check, what function `hosts_portion_g` returns list grouped by rack
    """
    res = script.hosts_portion_g(hostlist, boundary=("rack",))
    for list, attr in res:
        racks = set()
        for host in list:
            racks.add(host['location']['rack'])
        assert len(racks) == 1


def test_hosts_portion_g_group_dbl():
    """
    check, what function `hosts_portion_g` returns list grouped by rack and short_datacenter_name
    """
    res = script.hosts_portion_g(hostlist, boundary=("rack", "short_datacenter_name"))
    for list, attr in res:
        racks = set()
        sdcs = set()
        for host in list:
            racks.add(host['location']['rack'])
            sdcs.add(host['location']['short_datacenter_name'])
        assert len(racks) == 1
        assert len(sdcs) == 1


def test_hosts_portion_g_group_byack_p():
    """
    check, what function `hosts_portion_g` returns list grouped by rack and no more then `portion`
    """
    portion = 10
    res = script.hosts_portion_g(hostlist, boundary=("rack",), portion=portion)
    for list, attr in res:
        racks = set()
        cnt = 0
        for host in list:
            cnt += 1
            racks.add(host['location']['rack'])
        assert len(racks) == 1
        assert cnt <= portion
        assert cnt != 0


def test_uniq_err():
    assert script.uniq_err(['1', '2']) is None
    assert script.uniq_err([]) is None
    assert script.uniq_err(['1', '2', '1']) is not None
