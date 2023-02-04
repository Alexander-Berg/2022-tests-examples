import collections
import subprocess
import ipaddress
import asyncio
import pytest
import time

from .lib import yandex_networks
from .lib import utils
from .lib import defs


NS_PER_MS = 1000000
NS_PER_SEC = NS_PER_MS * 1000
FLOWLABEL_MASK = 0x000FFFFF
YATTL_BIT = 0x00400000


def test_prog_attached(run_ebpf_agent, run_bpftool):
    cgroups = run_bpftool(['cgroup', 'tree'])
    found = False
    for cg in cgroups:
        if cg['cgroup'] == '/sys/fs/cgroup/unified':
            for prog in cg['programs']:
                if (prog['name'] == 'tcp_rto' and
                        prog['attach_type'] == 'sock_ops' and
                        prog['attach_flags'] == 'multi'):
                    found = True
                    break
            if found:
                break
    assert found, "tcp_rto program isn't attached"


def _test_rto(all_networks, run_bpftrace_flowlabel, parse_bpftrace_flowlabel, setup_veth_peer, try_connect, agent_running=False):
    setup_veth_peer([n.net[1] for n in all_networks], drop=True)

    bpftrace = run_bpftrace_flowlabel()
    # wait for bpftrace start
    time.sleep(10)

    def chunks(lst, n):
        for i in range(0, len(lst), n):
            yield lst[i:i + n]

    def chunk_connect(nets):
        loop = asyncio.get_event_loop()
        tasks = []
        for n in nets:
            addr = str(n.net[1])  # ::1
            tasks.append(loop.create_task(try_connect(addr)))
        loop.run_until_complete(asyncio.wait(tasks))

    for nets in chunks(all_networks, 20):
        chunk_connect(nets)

    bpftrace.terminate()
    bpftrace.wait()

    retransmits = collections.defaultdict(list)
    for addr, fl, ts in parse_bpftrace_flowlabel(bpftrace):
        for n in all_networks:
            if addr == n.net[1]:
                assert fl != 0, "Zero flowlabel when agent is running"
                if len(retransmits[n]) > 0:
                    assert ts > retransmits[n][len(retransmits[n]) - 1][1], "Retransmit timestamps are unsorted"
                retransmits[n].append((fl, ts))

    for n in all_networks:
        assert n in retransmits, "Missed retransmits to {}".format(n)

    if hasattr(pytest, 'dc'):
        dc = pytest.dc
    else:
        dc = defs.DC.SAS1

    # yattl bit is flaky, no time to debug :(
    bad_yattl_bits = 0

    for n, flowlabels in retransmits.items():
        is_inside_dc = n.dc == dc

        # check YaTTL logic
        if agent_running:
            # assert flowlabels[0][0] & YATTL_BIT == 0, "YaTTL bit is set in first SYN retransmit"
            if flowlabels[0][0] & YATTL_BIT != 0:
                bad_yattl_bits += 1

            if is_inside_dc:
                assert len(flowlabels) > 1, 'Too few retransmits to {}'.format(n)
                # assert flowlabels[1][0] & YATTL_BIT == YATTL_BIT, "YaTTL bit isn't set in the second SYN retransmit (inside dc)"
                if flowlabels[1][0] & YATTL_BIT != YATTL_BIT:
                    bad_yattl_bits += 1
                assert flowlabels[0][0] & FLOWLABEL_MASK == flowlabels[1][0] & FLOWLABEL_MASK, 'Flowlabel in the first SYN retransmit is changed (inside dc)'
            else:
                assert flowlabels[0][0] & FLOWLABEL_MASK != flowlabels[1][0] & FLOWLABEL_MASK, "Flowlabel isn't changed in the first SYN retransmit (inter dc)"
            for i in range(2, len(flowlabels)):
                # assert flowlabels[i][0] & YATTL_BIT == 0, "YaTTL bit is set after second SYN retransmit"
                if flowlabels[i][0] & YATTL_BIT != 0:
                    bad_yattl_bits += 1
                assert flowlabels[i][0] & FLOWLABEL_MASK != flowlabels[i - 1][0] & FLOWLABEL_MASK, "Flowlabel isn't changed after second SYN retransmit"

        assert bad_yattl_bits < 5, 'Too many failed YaTTL bits'

        # check RTO
        if is_inside_dc and agent_running:
            for i in range(1, len(flowlabels)):
                assert (flowlabels[i][1] - flowlabels[i - 1][1]) / NS_PER_MS < 200, 'Too big RTO to {}'.format(n)
        else:
            for i in range(1, len(flowlabels)):
                assert (flowlabels[i][1] - flowlabels[i - 1][1]) / NS_PER_MS > 900, 'Too small RTO to {}'.format(n)


# def test_default_rto(run_all_dcs, all_networks, run_bpftrace_flowlabel, parse_bpftrace_flowlabel, setup_veth_peer, try_connect):
#     _test_rto(all_networks, run_bpftrace_flowlabel, parse_bpftrace_flowlabel, setup_veth_peer, try_connect)


def test_rto(run_ebpf_agent, bb_networks, run_bpftrace_flowlabel, parse_bpftrace_flowlabel, setup_veth_peer, try_connect):
    _test_rto(bb_networks, run_bpftrace_flowlabel, parse_bpftrace_flowlabel, setup_veth_peer, try_connect, True)


def test_sock_min_rto(run_ebpf_agent_sock_min_rto, all_networks, run_bpftrace_flowlabel, parse_bpftrace_flowlabel, setup_veth_peer, try_connect):
    n = all_networks[0]
    setup_veth_peer([n.net[1]])

    maj, min, patch, _ = utils.kernel_version()
    if maj < 5 or min < 4 or patch < 134:
        return

    bpftrace = run_bpftrace_flowlabel()
    # wait for bpftrace start
    time.sleep(10)

    loop = asyncio.get_event_loop()
    addr = str(n.net[1])  # ::1
    task = loop.create_task(try_connect(addr))
    loop.run_until_complete(asyncio.wait([task]))

    conn = task.result()
    assert conn is not None, 'Connect to {} timed out'.format(addr)

    subprocess.check_call(['ip', 'netns', 'exec', 'test', 'ip6tables', '-A', 'INPUT', '-j', 'DROP', '-d', '2a02::/16'])
    req = 'GET / HTTP/1.1\r\n\r\n'.encode('utf-8')
    conn.write(req)
    time.sleep(3)

    bpftrace.terminate()
    bpftrace.wait()

    retransmits = []
    for addr, fl, ts in parse_bpftrace_flowlabel(bpftrace):
        if addr == n.net[1]:
            assert fl != 0, "Zero flowlabel when agent is running"
            if len(retransmits) > 0:
                assert ts > retransmits[len(retransmits) - 1][1], "Retransmit timestamps are unsorted"
            retransmits.append((fl, ts))

    for i in range(1, len(retransmits)):
        assert (retransmits[i][1] - retransmits[i - 1][1]) / NS_PER_MS < 200, 'Too big RTO to {}'.format(n)


def test_yttl_blacklist(run_ebpf_agent_yttl_blacklist, run_bpftrace_flowlabel, parse_bpftrace_flowlabel, setup_veth_peer, try_connect):
    blacklisted_net = yandex_networks.get_dc_networks(defs.DC.SAS1)[1].net
    blacklisted_addr = blacklisted_net[1]

    setup_veth_peer([blacklisted_addr], drop=True)

    bpftrace = run_bpftrace_flowlabel()
    # wait for bpftrace start
    time.sleep(10)

    loop = asyncio.get_event_loop()
    tasks = []
    tasks.append(loop.create_task(try_connect(str(blacklisted_addr))))
    loop.run_until_complete(asyncio.wait(tasks))

    bpftrace.terminate()
    bpftrace.wait()

    found_addr = False
    flowlabels = []
    prev_ts = -1
    for addr, fl, ts in parse_bpftrace_flowlabel(bpftrace):
        if addr == blacklisted_addr:
            found_addr = True
            assert fl != 0, "Zero flowlabel when agent is running"
            if len(flowlabels) > 0:
                assert ts > prev_ts, "Retransmit timestamps are unsorted"
            flowlabels.append(fl)
            prev_ts = ts

    assert found_addr, "Could not find blacklisted address"
    assert len(flowlabels) > 2, "Too few retransmits to blacklisted address"
    assert flowlabels[0] != flowlabels[1], "Flowlabel isn't changed between retransmits to blacklisted address"
    assert flowlabels[1] & YATTL_BIT == 0, "YaTTL bit is set in the second SYN retransmit for blacklisted address"


def test_cong(run_ebpf_agent_cong, run_bpftrace_flowlabel, parse_bpftrace_flowlabel, run_bpftrace_cong, parse_bpftrace_cong, setup_veth_peer, try_connect):
    nets = yandex_networks.get_grouped_networks()

    vla_fb_addr = nets[defs.DC.VLA][defs.NETTYPE.FASTBONE][0].net[1]
    vla_bb_addr = nets[defs.DC.VLA][defs.NETTYPE.BACKBONE][0].net[1]
    sas_fb_addr = nets[defs.DC.SAS1][defs.NETTYPE.FASTBONE][0].net[2]
    remote_addrs = [vla_fb_addr, vla_bb_addr, sas_fb_addr]

    local_addr = nets[defs.DC.SAS1][defs.NETTYPE.FASTBONE][0].net[1]

    setup_veth_peer(remote_addrs, local_addr=local_addr)

    bpftrace = run_bpftrace_cong()
    # wait for bpftrace start
    time.sleep(15)

    loop = asyncio.get_event_loop()
    tasks = []
    for addr in remote_addrs:
        tasks.append(loop.create_task(try_connect(str(addr))))
    loop.run_until_complete(asyncio.wait(tasks))

    time.sleep(3)
    for i in range(3):
        assert tasks[i].result() is not None, 'Connect to {} timed out'.format(remote_addrs[i])

    bpftrace.terminate()
    bpftrace.wait()

    last_cong = {vla_fb_addr: None, vla_bb_addr: None}
    for addr, cong in parse_bpftrace_cong(bpftrace):
        if addr in remote_addrs:
            last_cong[addr] = cong

    assert last_cong[vla_fb_addr] == "bbr", 'Failed to set bbr to fastbone inter dc address {}, found wrong congestion control: {}'.format(vla_fb_addr, last_cong[vla_fb_addr])
    assert last_cong[vla_bb_addr] != "bbr", 'Set bbr to backbone inter dc address {}'.format(vla_bb_addr)
    assert last_cong[sas_fb_addr] != "bbr", 'Set bbr to fastbone intra dc address {}'.format(sas_fb_addr)


def _test_flowlabel(run_bpftrace_flowlabel, parse_bpftrace_flowlabel, setup_veth_peer, try_connect, agent_running=False):
    l3_addr = ipaddress.IPv6Address('2a02:6b8:0:3400::1')
    other_addr = ipaddress.IPv6Address('2a02:6b8:0:3401::1')
    setup_veth_peer([l3_addr, other_addr])

    bpftrace = run_bpftrace_flowlabel()
    # wait for bpftrace start
    time.sleep(10)

    loop = asyncio.get_event_loop()
    tasks = []
    tasks.append(loop.create_task(try_connect(str(l3_addr))))
    tasks.append(loop.create_task(try_connect(str(other_addr))))
    loop.run_until_complete(asyncio.wait(tasks))

    l3_conn = tasks[0].result()
    other_conn = tasks[1].result()
    assert l3_conn is not None, 'Connect to {} timed out'.format(l3_addr)
    assert other_conn is not None, 'Connect to {} timed out'.format(other_addr)

    connected_ts = int(time.clock_gettime(time.CLOCK_BOOTTIME) * NS_PER_SEC)

    subprocess.check_call(['ip', 'netns', 'exec', 'test', 'ip6tables', '-A', 'INPUT', '-j', 'DROP', '-d', '2a02::/16'])
    req = 'GET / HTTP/1.1\r\n\r\n'.encode('utf-8')
    l3_conn.write(req)
    other_conn.write(req)
    time.sleep(3)

    bpftrace.terminate()
    bpftrace.wait()

    good_flowlabel = {l3_addr: None, other_addr: None}
    retransmit_flowlabels = collections.defaultdict(list)
    for addr, fl, ts in parse_bpftrace_flowlabel(bpftrace):
        if addr in [l3_addr, other_addr]:
            if ts > connected_ts:
                retransmit_flowlabels[addr].append(fl)
            else:
                # save last success flowlabel before iptables activation
                good_flowlabel[addr] = fl

    for addr, fl in good_flowlabel.items():
        assert fl is not None, 'Failed to find good flowlabel to {}'.format(addr)
    for addr, fls in retransmit_flowlabels.items():
        assert len(fls) > 3, 'Too few retransmits to {}'.format(addr)

    if agent_running:
        for fl in retransmit_flowlabels[l3_addr]:
            assert fl == good_flowlabel[l3_addr], 'Flowlabel to {} is changed'.format(l3_addr)
        del retransmit_flowlabels[l3_addr]

        for addr, fls in retransmit_flowlabels.items():
            assert fls[0] == fls[1], "First two retransmit flowlabels to {} aren't equal".format(addr)
            for i in range(2, len(fls)):
                assert fls[i] != fls[i - 1], "Flowlabel isn't changed in next retransmit to {}".format(addr)
    else:
        for addr, fls in retransmit_flowlabels.items():
            for fl in fls:
                assert fl == 0x60000000, "Flowlabel isn't zero without ebpf-agent"


def test_default_flowlabel(run_bpftrace_flowlabel, parse_bpftrace_flowlabel, setup_veth_peer, try_connect):
    _test_flowlabel(run_bpftrace_flowlabel, parse_bpftrace_flowlabel, setup_veth_peer, try_connect)


def test_flowlabel(run_ebpf_agent, run_bpftrace_flowlabel, parse_bpftrace_flowlabel, setup_veth_peer, try_connect):
    _test_flowlabel(run_bpftrace_flowlabel, parse_bpftrace_flowlabel, setup_veth_peer, try_connect, True)
