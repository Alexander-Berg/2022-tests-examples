import collections
import ipaddress
import asyncio
import time

CS1 = 0x02000000
CS2 = 0x04000000
CS3 = 0x06000000
CS4 = 0x08000000
CS_MASK = 0x0f800000
ECN_MASK = 0x00300000


def test_progs_attached(run_ebpf_agent_tos, run_bpftool):
    cgroups = run_bpftool(['cgroup', 'tree'])
    names = []
    for cg in cgroups:
        if cg['cgroup'] == '/sys/fs/cgroup/unified':
            for prog in cg['programs']:
                if prog['attach_type'] == 'sock_ops' and prog['attach_flags'] == 'multi':
                    names.append(prog['name'])
    for name in ['tcp_tos', 'tcp_rto']:
        assert name in names, "{} program isn't attached".format(name)


def check_tos(run_ebpf_agent_tos, run_bpftrace_flowlabel, parse_bpftrace_flowlabel, setup_veth_peer, try_connect, yt=False, fb=False):
    yt_addr_sas = ipaddress.IPv6Address('2a02:6b8:fc00::4496:0:1' if fb else '2a02:6b8:c02::4496:0:1')
    yt_addr_vla = ipaddress.IPv6Address('2a02:6b8:fc09::4496:0:1' if fb else '2a02:6b8:c0d::4496:0:1')
    other_addr = ipaddress.IPv6Address('2a02:6b8:fc00::1' if fb else '2a02:6b8:c02::1')

    if yt:
        local_addr = ipaddress.IPv6Address('2a02:6b8:fc00::10d:5ae7:0:1' if fb else '2a02:6b8:c02::10d:5ae7:0:1')
    else:
        local_addr = ipaddress.IPv6Address('2a02:6b8:fc00::2' if fb else '2a02:6b8:c02::2')

    addrs = [yt_addr_sas, yt_addr_vla, other_addr]
    setup_veth_peer(addrs, local_addr=local_addr)

    bpftrace = run_bpftrace_flowlabel()
    # wait for bpftrace start
    time.sleep(10)

    loop = asyncio.get_event_loop()
    tasks = []
    for addr in addrs:
        tasks.append(loop.create_task(try_connect(str(addr))))
    loop.run_until_complete(asyncio.wait(tasks))

    req = 'GET / HTTP/1.1\r\n\r\n'.encode('utf-8')
    for i, task in enumerate(tasks):
        conn = task.result()
        assert conn is not None, 'Connect to {} timed out'.format(addrs[i])
        conn.write(req)

    time.sleep(3)

    for i, task in enumerate(tasks):
        task.result().close()

    bpftrace.terminate()
    bpftrace.wait()

    flowlabels = collections.defaultdict(list)
    for addr, fl, ts in parse_bpftrace_flowlabel(bpftrace):
        if addr in addrs:
            flowlabels[addr].append(fl)

    for addr in addrs:
        assert flowlabels[addr], 'Failed to find packets to {}'.format(addr)

    if fb:
        for fl in flowlabels[yt_addr_sas]:
            assert fl & CS_MASK == CS2, "Incorrect traffic class to FB YT address. Parsed header: {}".format(hex(fl))

        for fl in flowlabels[other_addr]:
            assert fl & CS_MASK == CS1, "Incorrect traffic class to FB non-YT address. Parsed header: {}".format(hex(fl))
    else:
        if yt:
            ecn_set = False
            for i, fl in enumerate(flowlabels[yt_addr_sas]):
                assert fl & CS_MASK == CS4, "Incorrect traffic class to BB YT address inside dc. Parsed header: {}".format(hex(fl))
                if i == 0:
                    assert fl & ECN_MASK, "ECN was not set in SYN packet for BB YT address inside dc. Parsed header: {}".format(hex(fl))
                else:
                    ecn_set |= fl & ECN_MASK
            assert ecn_set, "ECN was not set in data packets for BB YT address inside dc. Parsed headers: {}".format(flowlabels[yt_addr_sas])

        for fl in flowlabels[yt_addr_vla]:
            assert fl & CS_MASK == CS3, "Incorrect traffic class to BB YT address inter dc. Parsed header: {}".format(hex(fl))

        for fl in flowlabels[other_addr]:
            assert fl & CS_MASK == CS3, "Incorrect traffic class to BB non-YT address. Parsed header: {}".format(hex(fl))


def test_tos_yt(run_ebpf_agent_tos, run_bpftrace_flowlabel, parse_bpftrace_flowlabel, setup_veth_peer, try_connect):
    check_tos(run_ebpf_agent_tos, run_bpftrace_flowlabel, parse_bpftrace_flowlabel, setup_veth_peer, try_connect, yt=True)


def test_tos_yt_fb(run_ebpf_agent_tos, run_bpftrace_flowlabel, parse_bpftrace_flowlabel, setup_veth_peer, try_connect):
    check_tos(run_ebpf_agent_tos, run_bpftrace_flowlabel, parse_bpftrace_flowlabel, setup_veth_peer, try_connect, yt=True, fb=True)


def test_tos(run_ebpf_agent_tos, run_bpftrace_flowlabel, parse_bpftrace_flowlabel, setup_veth_peer, try_connect):
    check_tos(run_ebpf_agent_tos, run_bpftrace_flowlabel, parse_bpftrace_flowlabel, setup_veth_peer, try_connect)


def test_tos_fb(run_ebpf_agent_tos, run_bpftrace_flowlabel, parse_bpftrace_flowlabel, setup_veth_peer, try_connect):
    check_tos(run_ebpf_agent_tos, run_bpftrace_flowlabel, parse_bpftrace_flowlabel, setup_veth_peer, try_connect, fb=True)
