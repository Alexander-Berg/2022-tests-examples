import socket
import array
import os
from . import syscall


class PidSock(object):
    def __init__(self):
        self.sk = socket.socketpair(socket.AF_UNIX)
        self.sk[0].setsockopt(socket.SOL_SOCKET, socket.SO_PASSCRED, 1)
        self.sk[1].setsockopt(socket.SOL_SOCKET, socket.SO_PASSCRED, 1)

    def send_pid(self, pid):
        cred = array.array("i", [pid, 0, 0])
        self.sk[0].sendmsg([cred], [(socket.SOL_SOCKET, socket.SCM_CREDENTIALS, cred)])

    def recv_pid(self):
        cred = array.array("i")
        msg, ancdata, flags, addr = self.sk[1].recvmsg(12, socket.CMSG_SPACE(12))
        for cmsg_level, cmsg_type, cmsg_data in ancdata:
            if (cmsg_level == socket.SOL_SOCKET and cmsg_type == socket.SCM_CREDENTIALS):
                cred.fromstring(cmsg_data[:len(cmsg_data) - (len(cmsg_data) % cred.itemsize)])
        return cred[0]


class Namespace(object):

    flags = {
        'cgroup': syscall.CLONE_NEWCGROUP,
        'ipc': syscall.CLONE_NEWIPC,
        'mnt': syscall.CLONE_NEWNS,
        'net': syscall.CLONE_NEWNET,
        'pid': syscall.CLONE_NEWPID,
        'user': syscall.CLONE_NEWUSER,
        'uts': syscall.CLONE_NEWUTS,
    }

    def __init__(self, name, pid='self', unshare=False):
        self.name = name
        self.ns = open('/proc/{}/ns/{}'.format(pid, name))
        self.unshare = unshare

    def ino(self):
        return os.fstat(self.ns.fileno()).st_ino

    def __repr__(self):
        return '<Namespace {}:{}>'.format(self.name, self.ino())

    def __eq__(self, other):
        return self.ino() == other.ino()

    def __ne__(self, other):
        return self.ino() != other.ino()

    def enter(self):
        syscall.setns(self.ns.fileno(), self.flags[self.name])
        if self.unshare:
            self.unshare = False
            syscall.unshare(Namespace.flags[self.name])
