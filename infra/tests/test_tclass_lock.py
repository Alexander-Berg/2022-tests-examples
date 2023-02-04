import socket

from .lib import utils


def _test_tclass_lock(all_networks, setup_veth_peer, agent_running=False):
    n = all_networks[0]
    setup_veth_peer([n.net[1]])

    maj, min, _, _ = utils.kernel_version()
    if maj < 5 or min < 4:
        return

    addr = str(n.net[1])  # ::1
    dest = (addr, 80)

    sock = socket.socket(socket.AF_INET6, socket.SOCK_DGRAM)
    sock.connect(dest)

    err = sock.setsockopt(socket.IPPROTO_IPV6, socket.IPV6_TCLASS, 0xFF)
    assert not err, "setsockopt returned error: {}".format(err)

    tclass = sock.getsockopt(socket.IPPROTO_IPV6, socket.IPV6_TCLASS)

    if agent_running:
        assert tclass != 0xFF, "tclass changed with agent on"
    else:
        assert tclass == 0xFF, "tclass didn't change with agent off"

    sndbuf_size = 5555
    err = sock.setsockopt(socket.SOL_SOCKET, socket.SO_SNDBUF, sndbuf_size)
    assert not err, "setsockopt returned error: {}".format(err)

    val = sock.getsockopt(socket.SOL_SOCKET, socket.SO_SNDBUF)
    assert val == sndbuf_size * 2, "tclass_lock breaks other socket options"


def test_default_tclass_lock(all_networks, setup_veth_peer):
    _test_tclass_lock(all_networks, setup_veth_peer)


def test_tclass_lock(run_ebpf_agent_tclass_lock, all_networks, setup_veth_peer):
    _test_tclass_lock(all_networks, setup_veth_peer, True)
