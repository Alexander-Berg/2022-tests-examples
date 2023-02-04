import pytest
import socket
import errno
import kern

IPTOS_CLASS_CS0 = 0x00
IPTOS_CLASS_CS1 = 0x20
IPTOS_CLASS_CS2 = 0x40
IPTOS_CLASS_CS3 = 0x60

pytestmark = pytest.mark.unprivileged()


def prio_tos(prio, tos):
    return (prio & ~7) | (tos >> 5)


def check_priority_v4(prio, tos):
    sk = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    assert sk.getsockopt(socket.IPPROTO_IP, socket.IP_TOS) == tos
    assert sk.getsockopt(socket.SOL_SOCKET, socket.SO_PRIORITY) == prio

    sk.setsockopt(socket.IPPROTO_IP, socket.IP_TOS, IPTOS_CLASS_CS1)

    assert sk.getsockopt(socket.IPPROTO_IP, socket.IP_TOS) == IPTOS_CLASS_CS1
    assert sk.getsockopt(socket.SOL_SOCKET, socket.SO_PRIORITY) == prio_tos(prio, IPTOS_CLASS_CS1)

    sk.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sk.bind(('', 0))
    _, port = sk.getsockname()
    sk.listen()

    cl = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    cl.setblocking(0)
    assert cl.connect_ex(('', port)) == errno.EINPROGRESS

    cs, _ = sk.accept()

    assert cl.getsockopt(socket.IPPROTO_IP, socket.IP_TOS) == tos
    assert cl.getsockopt(socket.SOL_SOCKET, socket.SO_PRIORITY) == prio

    assert cs.getsockopt(socket.IPPROTO_IP, socket.IP_TOS) == IPTOS_CLASS_CS1
    assert cs.getsockopt(socket.SOL_SOCKET, socket.SO_PRIORITY) == prio_tos(prio, IPTOS_CLASS_CS1)


def check_priority_v6(prio, tos):
    sk = socket.socket(socket.AF_INET6, socket.SOCK_STREAM)

    assert sk.getsockopt(socket.IPPROTO_IP, socket.IP_TOS) == tos
    assert sk.getsockopt(socket.IPPROTO_IPV6, socket.IPV6_TCLASS) == tos
    assert sk.getsockopt(socket.SOL_SOCKET, socket.SO_PRIORITY) == prio_tos(prio, tos)

    sk.setsockopt(socket.IPPROTO_IP, socket.IP_TOS, IPTOS_CLASS_CS1)

    assert sk.getsockopt(socket.IPPROTO_IP, socket.IP_TOS) == IPTOS_CLASS_CS1
    assert sk.getsockopt(socket.IPPROTO_IPV6, socket.IPV6_TCLASS) == tos
    assert sk.getsockopt(socket.SOL_SOCKET, socket.SO_PRIORITY) == prio_tos(prio, IPTOS_CLASS_CS1)

    sk.setsockopt(socket.IPPROTO_IPV6, socket.IPV6_TCLASS, IPTOS_CLASS_CS2)

    assert sk.getsockopt(socket.IPPROTO_IP, socket.IP_TOS) == IPTOS_CLASS_CS1
    assert sk.getsockopt(socket.IPPROTO_IPV6, socket.IPV6_TCLASS) == IPTOS_CLASS_CS2
    assert sk.getsockopt(socket.SOL_SOCKET, socket.SO_PRIORITY) == prio_tos(prio, IPTOS_CLASS_CS2)

    sk.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sk.bind(('', 0))
    _, port, _, _ = sk.getsockname()
    sk.listen()

    cl = socket.socket(socket.AF_INET6, socket.SOCK_STREAM)
    cl.setblocking(0)
    assert cl.connect_ex(('', port)) == errno.EINPROGRESS

    cs, _ = sk.accept()

    assert cl.getsockopt(socket.IPPROTO_IP, socket.IP_TOS) == tos
    assert cl.getsockopt(socket.IPPROTO_IPV6, socket.IPV6_TCLASS) == tos
    assert cl.getsockopt(socket.SOL_SOCKET, socket.SO_PRIORITY) == prio

    assert cs.getsockopt(socket.IPPROTO_IP, socket.IP_TOS) == IPTOS_CLASS_CS1
    assert cs.getsockopt(socket.IPPROTO_IPV6, socket.IPV6_TCLASS) == IPTOS_CLASS_CS2
    assert cs.getsockopt(socket.SOL_SOCKET, socket.SO_PRIORITY) == prio_tos(prio, IPTOS_CLASS_CS2)


def test_net_cls_priority(make_cgroup, make_task):
    cg = kern.task_cgroup('self', 'net_cls')
    for attr in ['priority', 'ya.priority']:
        if attr in cg:
            priority_attr = attr
            break
    else:
        pytest.xfail("net_cls priority not supported")

    prio = cg[priority_attr]
    if prio:
        check_priority_v4(prio, IPTOS_CLASS_CS0)
        check_priority_v6(prio, IPTOS_CLASS_CS0)

    cg = make_cgroup('net_cls')
    task = make_task(cgroups=[cg])

    prio = 0x20000
    cg[priority_attr] = prio
    task.call_func(check_priority_v4, prio, IPTOS_CLASS_CS0)
    task.call_func(check_priority_v6, prio, IPTOS_CLASS_CS0)

    prio = prio_tos(prio, IPTOS_CLASS_CS3)
    cg[priority_attr] = prio
    task.call_func(check_priority_v4, prio, IPTOS_CLASS_CS3)
    task.call_func(check_priority_v6, prio, IPTOS_CLASS_CS3)
