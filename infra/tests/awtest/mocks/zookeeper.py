"""
Copy-paste from sepelib.zookeeper.mock.

The only differences are:
* increased _ZooKeeper.__tick_time (from 2000 to 15000);
* use of ExperimentalSequentialGeventHandler.
"""

from __future__ import unicode_literals

import contextlib
import os
import shutil
import signal
import socket
import subprocess
import sys
import tempfile
import time

from kazoo.client import KazooClient
import sepelib.subprocess.util
from infra.swatlib.zookeeper_client import SequentialGeventHandler


class ZooKeeper(object):
    __host = "localhost"

    __tick_time = 15000

    def __init__(self, path, port_manager=None):
        self.__temp_dir = None
        self.__stdout = None
        self.__stderr = None
        self.__port = None
        self.__process = None
        self.__client = None
        self._port_manager = port_manager

        try:
            self.__start(path)
        except:
            self.close()
            raise

    @property
    def timeout(self):
        return float(self.__tick_time) / 1000 * 2 + 1

    def break_temporarily(self):
        os.kill(self.__process.pid, signal.SIGSTOP)
        time.sleep(self.timeout)

        os.kill(self.__process.pid, signal.SIGCONT)
        time.sleep(self.timeout)

    @property
    def client(self):
        if not self.__client:
            self.__client = KazooClient(hosts=self.__hosts(), handler=SequentialGeventHandler())
        return self.__client

    def connect(self):
        self.client.start()

    def disconnect(self):
        if self.__client is not None:
            self.__client.stop()
            self.__client.close()

    def close(self):
        try:
            self.disconnect()

            if self.__process is not None:
                sepelib.subprocess.util.terminate(self.__process)

            if self.__stdout is not None:
                self.__stdout.close()

            if self.__stderr is not None:
                self.__stderr.close()
        finally:
            if self.__temp_dir is not None:
                shutil.rmtree(self.__temp_dir)

    def __start(self, path):
        IS_ARCADIA = 'ARCADIA_SOURCE_ROOT' in os.environ
        if IS_ARCADIA:
            from yatest import common
            from infra.swatlib.logutil import rndstr
            self.__temp_dir = common.output_path('zk' + rndstr())
            os.mkdir(self.__temp_dir)
        else:
            self.__temp_dir = tempfile.mkdtemp()

        data_path = os.path.join(self.__temp_dir, "data")
        os.mkdir(data_path)

        self.__stdout = open(os.path.join(self.__temp_dir, "stdout"), "w")
        self.__stderr = open(os.path.join(self.__temp_dir, "stderr"), "w")

        if self._port_manager is None:
            self.__port = self.__get_free_port()
        else:
            self.__port = self._port_manager.get_port()

        config = [
            "tickTime={}".format(self.__tick_time),
            "dataDir=" + data_path,
            "clientPort={}".format(self.__port),
        ]

        with open(os.path.join(self.__temp_dir, "zoo.cfg"), "w") as config_file:
            config_file.write("\n".join(config))

        env = dict(os.environ)
        env.update({
            "ZOOCFGDIR": self.__temp_dir,

            # OS X:
            # Prevent java from appearing in menu bar, process dock and from activation of the main workspace on run.
            "JAVA_TOOL_OPTIONS": "-Djava.awt.headless=true",
        })
        if IS_ARCADIA:
            from yatest.common import execute, java_bin
            if 'PATH' in os.environ:
                env['PATH'] = os.environ['PATH'] + os.pathsep + os.path.dirname(java_bin()) + os.pathsep + '/bin' + os.pathsep + '/usr/bin'
            else:
                env['PATH'] = os.path.dirname(java_bin()) + os.pathsep + '/bin' + os.pathsep + '/usr/bin'
            self.__process = execute([os.path.join(path, "bin", "zkServer.sh"), "start-foreground"],
                                     stdout=self.__stdout, stderr=self.__stderr, env=env, wait=False, close_fds=True)
        else:
            stdout = sys.stdout  # self.__stdout
            stderr = sys.stderr  # self.__stderr
            self.__process = subprocess.Popen([os.path.join(path, "bin", "zkServer.sh"), "start-foreground"],
                                              stdout=stdout, stderr=stderr, env=env)

    def __get_free_port(self):
        with contextlib.closing(socket.socket(socket.AF_INET, socket.SOCK_STREAM)) as sock:
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            sock.bind((self.__host, 0))
            return sock.getsockname()[1]

    def __hosts(self):
        return "{}:{}".format(self.__host, self.__port)

    @property
    def hosts(self):
        return self.__hosts()
