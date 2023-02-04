import socket
import resource
import time
import threading


def spawn_sockets(nr):
    for i in range(nr):
        sk = socket.socket(socket.AF_INET)
        sk.detach()


def spawn_threads(nr):
    class DummyThread(threading.Thread):
        def run(self):
            time.sleep(3600)

    for i in range(nr):
        th = DummyThread()
        th.daemon = True
        th.start()


def attach_current(cg):
    cg.attach()


def get_socket_priority():
    sk = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    return sk.getsockopt(socket.SOL_SOCKET, socket.SO_PRIORITY)


def test_net_cls_attach(make_cgroup, make_task):
    cg = make_cgroup('net_cls')

    socket_priority = None
    if 'ya.priority' in cg:
        socket_priority = 0x12345678
        cg['ya.priority'] = socket_priority

    resource.setrlimit(resource.RLIMIT_NOFILE, (1001000, 1001000))

    hog = make_task(cgroups=[cg])
    assert cg.has_task(hog.pid)

    hog.call_func(spawn_sockets, 100000)
    hog.call_func(spawn_threads, 1000)

    probe = make_task()
    assert not cg.has_task(probe.pid)

    attach_time = time.time()
    probe.call_func(attach_current, cg)
    attach_time = time.time() - attach_time

    # https://st.yandex-team.ru/KERNEL-253
    assert attach_time < 1
    assert cg.has_task(probe.pid)

    if socket_priority is not None:
        assert probe.call_func(get_socket_priority) == socket_priority
