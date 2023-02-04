import subprocess
import ipaddress
import asyncio
import signal
import pytest
import json
import time
import os

import yatest.common

from .lib import yandex_networks
from .lib import defs


def mount_cgroup2():
    if not os.path.isdir('/sys/fs/cgroup/unified'):
        os.makedirs('/sys/fs/cgroup/unified')
        subprocess.check_call(['mount', '-t', 'cgroup2', 'cgroup2', '/sys/fs/cgroup/unified'])


def mount_bpffs():
    if not os.path.isdir('/sys/fs/bpf'):
        os.makedirs('/sys/fs/bpf')
    subprocess.check_call(['mount', '-t', 'bpf', 'bpf', '/sys/fs/bpf'])


def create_config(config={'TcpRto': {'CrossDcRto': False, 'YttlEnabled': True}}):
    with open('/usr/share/ebpf-agent/config.json', 'w') as f:
        json.dump(config, f)


def create_yttl_blacklist(blacklisted_net):
    data={"device_networks": [{"networks": [{"network": blacklisted_net}]}]}
    with open('/usr/share/ebpf-agent/yttl_blacklist_nets.txt', 'w') as f:
        json.dump(data, f)


def set_cong_sysctl(cong):
    subprocess.check_call(['sysctl', 'net.ipv4.tcp_congestion_control=' + cong])


@pytest.fixture(scope='session', autouse=True)
def prepare():
    mount_cgroup2()
    mount_bpffs()
    subprocess.check_call(['mkdir', '-p', '/usr/share/ebpf-agent'])
    create_config()


def _enter_cgroup(path):
    if not os.path.isdir(path):
        os.makedirs(path)
    with open(path + '/cgroup.procs', 'w') as f:
        f.write(str(os.getpid()))


def _setup_eth0(dc):
    nets = yandex_networks.get_dc_networks(dc)
    ip = str(nets[0].net[1234])

    subprocess.check_call(["ip", "addr", "add", ip, "dev", "eth0"])

    return ip


def _teardown_eth0(ip):
    subprocess.check_call(["ip", "addr", "del", ip, "dev", "eth0"])


@pytest.fixture(params=list(defs.DC))
def run_all_dcs(request):
    pytest.dc = request.param


def _run_ebpf_agent(dc):
    pytest.dc = dc
    ebpf_agent_bin = yatest.common.build_path('infra/ebpf-agent') + '/ebpf-agent'
    ebpf_agent = subprocess.Popen([ebpf_agent_bin, '--log', '/var/log/ebpf-agent.log'])
    # wait for programs attach
    time.sleep(1)
    return ebpf_agent


@pytest.fixture
def run_ebpf_agent():
    dc = defs.DC.SAS1
    ip = _setup_eth0(dc)
    ebpf_agent = _run_ebpf_agent(dc)
    yield
    ebpf_agent.terminate()
    ebpf_agent.wait()
    _teardown_eth0(ip)


@pytest.fixture(params=list(defs.DC))
def run_ebpf_agent_all_dcs(request):
    dc = request.param
    ip = _setup_eth0(dc)
    ebpf_agent = _run_ebpf_agent(dc)
    yield
    ebpf_agent.terminate()
    ebpf_agent.wait()
    _teardown_eth0(ip)


@pytest.fixture
def run_ebpf_agent_sock_min_rto():
    create_config({'TcpRto': {'SockMinRto': True}})
    dc = defs.DC.SAS1
    ip = _setup_eth0(dc)
    ebpf_agent = _run_ebpf_agent(dc)
    yield
    ebpf_agent.terminate()
    ebpf_agent.wait()
    _teardown_eth0(ip)
    create_config()


@pytest.fixture
def run_ebpf_agent_cong():
    # Enable and disable bbr to load tcp_bbr module
    set_cong_sysctl("bbr")
    set_cong_sysctl("htcp")

    create_config({'TcpRto': {'CongControl': True}})
    dc = defs.DC.SAS1
    ip = _setup_eth0(dc)
    ebpf_agent = _run_ebpf_agent(dc)
    yield
    ebpf_agent.terminate()
    ebpf_agent.wait()
    _teardown_eth0(ip)
    create_config()


@pytest.fixture
def run_ebpf_agent_tos():
    create_config({'Programs': {'Enabled': ['tcp_tos', 'tcp_rto']}})
    dc = defs.DC.SAS1
    ip = _setup_eth0(dc)
    ebpf_agent = _run_ebpf_agent(dc)
    yield
    ebpf_agent.terminate()
    ebpf_agent.wait()
    _teardown_eth0(ip)
    create_config()


@pytest.fixture
def run_ebpf_agent_yttl_blacklist():
    dc = defs.DC.SAS1
    nets = yandex_networks.get_dc_networks(dc)
    blacklisted_net = str(nets[1].net)
    create_yttl_blacklist(blacklisted_net)
    ip = _setup_eth0(dc)
    ebpf_agent = _run_ebpf_agent(dc)
    yield
    ebpf_agent.terminate()
    ebpf_agent.wait()
    _teardown_eth0(ip)


@pytest.fixture
def run_ebpf_agent_tclass_lock():
    create_config({'Programs': {'Enabled': ['tclass_lock']}})
    dc = defs.DC.SAS1
    ip = _setup_eth0(dc)
    ebpf_agent = _run_ebpf_agent(dc)
    yield
    ebpf_agent.terminate()
    ebpf_agent.wait()
    _teardown_eth0(ip)
    create_config()


def _run_net_stat(daemon=False):
    net_stat_bin = yatest.common.build_path('infra/ebpf-agent/utils/net-stat') + '/net-stat'
    net_stat = subprocess.Popen([net_stat_bin, '--per-dc', '/sys/fs/cgroup/unified/test'], stdout=subprocess.PIPE)
    if daemon:
        # wait for programs attach
        time.sleep(1)
    return net_stat


@pytest.fixture
def run_net_stat():
    _enter_cgroup('/sys/fs/cgroup/unified/test')
    net_stat = _run_net_stat(daemon=True)
    yield
    _enter_cgroup('/sys/fs/cgroup/unified')
    net_stat.terminate()
    net_stat.wait()


@pytest.fixture
def get_net_stats():
    def _get_net_stats():
        net_stat = _run_net_stat()
        net_stat.wait(2)
        return json.load(net_stat.stdout)
    return _get_net_stats


@pytest.fixture
def run_bpftool():
    def _run_bpftool(args=[]):
        bpftool_bin = yatest.common.build_path('infra/kernel/tools/bpftool/release/bpftool/bpftool')
        return json.loads(subprocess.check_output([bpftool_bin, '-j'] + list(args)))
    return _run_bpftool


def _run_bpftrace(args=[]):
    bpftrace_bin = yatest.common.build_path('infra/kernel/tools/bpftrace/release/bpftrace/bpftrace')
    return subprocess.Popen([bpftrace_bin] + list(args), stdout=subprocess.PIPE)


@pytest.fixture
def run_bpftrace():
    return _run_bpftrace


@pytest.fixture
def run_bpftrace_cong():
    def _run_bpftrace_cong():
        prog = """
k:tcp_init_congestion_control
{
    @sk[tid] = arg0;
}

kr:tcp_init_congestion_control
/@sk[tid]/
{
    $sk = (struct sock *)@sk[tid];
    $daddr = ntop($sk->__sk_common.skc_v6_daddr.in6_u.u6_addr8);
    $icsk = (struct inet_connection_sock *)$sk;
    $name = $icsk->icsk_ca_ops->name;
    printf("%s %s\\n", $daddr, $name);
    delete(@sk[tid]);
}
"""
        return _run_bpftrace(['--include', 'net/inet_connection_sock.h', '--include', 'net/tcp.h', '-e', prog.replace('\n', '')])
    return _run_bpftrace_cong


@pytest.fixture
def run_bpftrace_flowlabel():
    def _run_bpftrace_flowlabel():
        prog = """
kprobe:ip6_output
{
    $sk = (struct sock *)arg1;
    $daddr = ntop($sk->__sk_common.skc_v6_daddr.in6_u.u6_addr8);
    $skb = (struct sk_buff *)arg2;
    $hdr = $skb->head + $skb->network_header;
    $fl = *(uint32 *)$hdr;
    $fl = (($fl & 0xFF000000) >> 24) | (($fl & 0x00FF0000) >> 8) | (($fl & 0x0000FF00) << 8) | (($fl & 0x000000FF) << 24);
    printf("%s %x %llu\\n", $daddr, $fl, nsecs);
}
"""
        return _run_bpftrace(['--include', 'linux/socket.h', '--include', 'net/sock.h', '-e', prog.replace('\n', '')])
    return _run_bpftrace_flowlabel


@pytest.fixture
def parse_bpftrace_flowlabel():
    def _parse_bpftrace_flowlabel(bpftrace):
        res = []
        for line in bpftrace.stdout.read().splitlines()[1:]:
            if line:
                addr, fl, ts = line.decode('utf-8').split(' ')
                res.append((ipaddress.IPv6Address(addr), int(fl, base=16), int(ts)))
        return res
    return _parse_bpftrace_flowlabel


@pytest.fixture
def parse_bpftrace_cong():
    def _parse_bpftrace_cong(bpftrace):
        res = []
        for line in bpftrace.stdout.read().splitlines()[1:]:
            if line:
                addr, cong = line.decode('utf-8').split(' ')
                res.append((ipaddress.IPv6Address(addr), cong))
        return res
    return _parse_bpftrace_cong


@pytest.fixture(scope='session')
def all_networks():
    return yandex_networks.get_networks()


@pytest.fixture
def bb_networks():
    return yandex_networks.get_nettype_networks(defs.NETTYPE.BACKBONE)


@pytest.fixture
def fb_networks():
    return yandex_networks.get_nettype_networks(defs.NETTYPE.FASTBONE)


@pytest.fixture
def sas1_networks():
    return yandex_networks.get_dc_networks(defs.DC.SAS1)


@pytest.fixture
def sas2_networks():
    return yandex_networks.get_dc_networks(defs.DC.SAS2)


@pytest.fixture
def vla_networks():
    return yandex_networks.get_dc_networks(defs.DC.VLA)


@pytest.fixture
def vlx_networks():
    return yandex_networks.get_dc_networks(defs.DC.VLX)


@pytest.fixture
def man_networks():
    return yandex_networks.get_dc_networks(defs.DC.MAN)


@pytest.fixture
def setup_veth_peer():
    def _setup_veth_peer(addrs, local_addr="fdc::8000", drop=False):
        local_addr = str(local_addr)
        subprocess.check_call(["ip", "link", "add", "veth0", "address", "52:54:00:00:72:55", "type", "veth", "peer", "name", "veth1", "address", "52:54:00:00:55:27"])
        subprocess.check_call(["ip", "netns", "add", "test"])
        subprocess.check_call(["ip", "link", "set", "veth1", "netns", "test"])
        subprocess.check_call(["ip", "link", "set", "veth0", "up"])
        subprocess.check_call(["ip", "netns", "exec", "test", "ip", "link", "set", "veth1", "up"])
        subprocess.check_call(["ip", "addr", "add", local_addr, "dev", "veth0"])
        subprocess.check_call(["ip", "netns", "exec", "test", "ip", "-6", "neigh", "add", local_addr, "dev", "veth1", "lladdr", "52:54:00:00:72:55", "nud", "permanent"])
        subprocess.check_call(["ip", "netns", "exec", "test", "ip", "-6", "route", "add", local_addr, "dev", "veth1"])
        for addr in list(addrs):
            addr = str(addr)
            subprocess.check_call(["ip", "netns", "exec", "test", "ip", "addr", "add", addr, "dev", "veth1"])
            subprocess.check_call(["ip", "-6", "neigh", "add", addr, "dev", "veth0", "lladdr", "52:54:00:00:55:27", "nud", "permanent"])
            subprocess.check_call(["ip", "-6", "route", "add", addr, "dev", "veth0"])
        if drop:
            subprocess.check_call(["ip", "netns", "exec", "test", "ip6tables", "-A", "INPUT", "-j", "DROP", "-i", "veth1"])
        else:
            # subprocess.Popen() hangs :(
            os.popen('ip netns exec test nc -6dkl 80')
            # wait for nc start
            time.sleep(1)
    yield _setup_veth_peer

    try:
        for pid in subprocess.check_output(['ip', 'netns', 'pids', 'test']):
            try:
                os.kill(pid, signal.SIGTERM)
            except:
                pass

        subprocess.check_call(["ip", "netns", "del", "test"])
        subprocess.check_call(["ip", "link", "del", "dev", "veth0"])
    except:
        pass


@pytest.fixture
def try_connect():
    async def _try_connect(addr, port=80, timeout=5):
        future = asyncio.open_connection(addr, port)
        try:
            _, writer = await asyncio.wait_for(future, timeout)
            return writer
        except asyncio.TimeoutError:
            return None
    yield _try_connect
