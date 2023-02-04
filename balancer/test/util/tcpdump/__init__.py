# -*- coding: utf-8 -*-
import os
import sys
import pytest
import dpkt
import fcntl
import termios
import array
from datetime import datetime
from balancer.test.util.stdlib.multirun import Multirun
from balancer.test.util.process import Process, BaseProcessManager, ProcessException


def tcpdump_prefix(settings):
    if settings.sandbox:
        return ['sudo', 'tcpdump']
    elif settings.yatest:
        return ['/usr/sbin/tcpdump']
    else:
        return ['tcpdump']


class TCPParseException(Exception):
    pass


class TCPSession(object):
    ENABLING = 0
    ENABLED = 1
    FINISHING = 2
    FINISHED = 3
    CLOSED = 4

    def __init__(self, client, server, start_time):
        super(TCPSession, self).__init__()
        self.client = client
        self.server = server
        self.status = self.ENABLING
        self.finished_by = None
        self.start_time = start_time
        self.fin_time = datetime.max
        self.client_packets = list()
        self.server_packets = list()
        self.other_client_packets = list()
        self.other_server_packets = list()
        self.reset = False

    def is_enabled(self):
        return self.status >= self.ENABLED

    def is_closed(self):
        return self.status == self.CLOSED

    def finished_by_server(self):
        return self.is_closed() and self.finished_by == self.server

    def finished_by_client(self):
        return self.is_closed() and self.finished_by == self.client

    def get_duration(self):
        return self.fin_time - self.start_time

    def get_seconds_duration(self):
        return self.get_duration().total_seconds()

    def add_packet(self, src, timestamp):
        if src == self.server:
            self.server_packets.append(timestamp)
        elif src == self.client:
            self.client_packets.append(timestamp)
        else:
            raise TCPParseException('unknown source: %d' % src)

    def add_other_packet(self, src, timestamp):
        if src == self.server:
            self.other_server_packets.append(timestamp)
        elif src == self.client:
            self.other_client_packets.append(timestamp)
        else:
            raise TCPParseException('unknown source: %d' % src)

    def set_closed(self, finished_by, fin_time):
        self.status = self.CLOSED
        self.finished_by = finished_by
        self.fin_time = fin_time


class Tcpdump(Process):
    def __init__(self, options, dump_file):
        super(Tcpdump, self).__init__(options)
        self.__dump_file = dump_file
        self.__servers = set()
        self.__sessions = list()
        self.__opened_sessions = dict()  # map from clients to current opened sessions
        self.__file_obj = open(self.__dump_file, 'r', buffering=False)
        self.__buf = array.array('i', [0])
        self.check_process()
        for run in Multirun(max_retries=8):
            with run:
                assert self.__avail_bytes
        self.__reader = dpkt.pcap.Reader(self.__file_obj)

    def _finish(self):
        try:
            super(Tcpdump, self)._finish()
        finally:
            self.__file_obj.close()

    @property
    def __avail_bytes(self):
        fcntl.ioctl(self.__file_obj, termios.FIONREAD, self.__buf)
        return self.__buf[0]

    def read_all(self):
        while self.__avail_bytes:
            p = self.next_packet()
            # for debugging problems with tcpdump tests
            if p is not None:
                self._options.logger.error('tcp packet: {} {} {} {}'.format(p.sport, p.dport, p.flags, len(p)))
            else:
                self._options.logger.error('tcp packet: not an ip packet')

    def next_packet(self):
        ts, buf = next(self.__reader)

        decoder = None
        dl = self.__reader.datalink()
        if dl == dpkt.pcap.DLT_LINUX_SLL:
            decoder = dpkt.sll.SLL
        elif dl == dpkt.pcap.DLT_NULL or dl == dpkt.pcap.DLT_LOOP:
            decoder = dpkt.loopback.Loopback
        else:
            decoder = dpkt.ethernet.Ethernet

        eth = decoder(buf)
        if not isinstance(eth.data, (dpkt.ip.IP, dpkt.ip6.IP6)):
            return None
        ip = eth.data
        if not isinstance(eth.data.data, dpkt.tcp.TCP):
            return None
        tcp = ip.data
        src_port = tcp.sport
        dst_port = tcp.dport
        flags = tcp.flags
        ts = self.get_time(ts)
        src_port = int(src_port)
        dst_port = int(dst_port)

        if flags & dpkt.tcp.TH_SYN:
            if src_port not in self.__servers:
                self.__servers.add(dst_port)
                new_session = TCPSession(src_port, dst_port, ts)
                self.__opened_sessions[src_port] = new_session
                self.__sessions.append(new_session)
            else:
                session = self.__opened_sessions[dst_port]
                if session.status == TCPSession.ENABLING:
                    session.status = TCPSession.ENABLED
                else:
                    raise TCPParseException('Error while enabling connection')
        else:
            session = self.get_opened_session(src_port, dst_port)
            if session is not None:
                if flags & dpkt.tcp.TH_FIN:
                    if session.status == TCPSession.FINISHING:
                        session.status = TCPSession.FINISHED
                    elif session.status == TCPSession.ENABLED:
                        session.status = TCPSession.FINISHING
                    else:
                        raise TCPParseException('Error while finishing connection')

                elif flags & dpkt.tcp.TH_RST:
                    session.set_closed(src_port, ts)
                    session.reset = True
                    self.__opened_sessions.pop(session.client)

                elif flags & dpkt.tcp.TH_PUSH:
                    session.add_packet(src_port, ts)

                elif flags == dpkt.tcp.TH_ACK:
                    if session.status == TCPSession.FINISHED:
                        session.set_closed(src_port, ts)
                        self.__opened_sessions.pop(session.client)
                    else:
                        session.add_other_packet(src_port, ts)
        return tcp

    def get_opened_session(self, src, dst):
        client = src if src not in self.__servers else dst
        if client in self.__opened_sessions:
            return self.__opened_sessions[client]
        else:
            return None

    def get_sessions(self):
        return self.__sessions

    def get_closed_sessions(self):
        opened = self.__opened_sessions.values()
        return [s for s in self.__sessions if s not in opened]

    @staticmethod
    def get_time(ts):
        return datetime.fromtimestamp(ts)


class TcpdumpManager(BaseProcessManager):
    def __init__(self, resource_manager, logger, fs_manager, settings, skip=False):
        super(TcpdumpManager, self).__init__(resource_manager, logger, fs_manager)
        self.__fs_manager = fs_manager
        self.__settings = settings
        self.__skip = skip

    def start(self, port, interface='lo'):
        if sys.platform == "darwin" and interface == "lo":
            interface = "lo0"
        try:
            if self.__skip:
                pytest.skip('unable to run tcpdump')
            dump_file = self.__fs_manager.create_file('{}.pcap'.format(port))
            relative_dump_file = os.path.relpath(dump_file, self.__fs_manager.root_dir)
            cmd = tcpdump_prefix(self.__settings) + \
                ['-U', '-n', '-i', interface, '-s', '1500', '-w', relative_dump_file, 'port {}'.format(port)]
            options = self._popen(
                cmd=cmd,
                name='tcpdump',
                cwd=self.__fs_manager.root_dir,
            )
            tcpdump = Tcpdump(options, dump_file)
            self._resource_manager.register(tcpdump)
            return tcpdump
        except ProcessException:
            pytest.skip("TODO(velavokr,alexeylaptev): BALANCER-2936 - tcpdump tests are disabled on this platform")
