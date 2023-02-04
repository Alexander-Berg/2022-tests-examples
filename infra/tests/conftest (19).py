# coding: utf-8
from __future__ import print_function

import socket
import time
import tempfile
import shutil
import json
import contextlib
import threading

import py
import pytest
import tornado.httpserver
import tornado.netutil
import tornado.web
import tornado.ioloop

from yatest.common.network import PortManager
from yatest.common import execute, binary_path

CLICKHOUSE_CONFIG = """\
<?xml version="1.0"?>
<yandex>
    <http_port>{http_port}</http_port>
    <tcp_port>{tcp_port}</tcp_port>
    <listen_host>{host}</listen_host>
    <path>{db}</path>
    <tmp_path>{tmp}</tmp_path>
    <mark_cache_size>10485760</mark_cache_size>
    <profiles>
        <default>
        </default>
    </profiles>
    <quotas>
        <default>
        </default>
    </quotas>
    <users>
        <netmon>
            <password>netmon</password>
            <networks>
                <ip>::/0</ip>
            </networks>
            <profile>default</profile>
            <quota>default</quota>
        </netmon>
    </users>
    <remote_servers>
        <netmon>
            <shard>
                <weight>1</weight>
                <internal_replication>false</internal_replication>
                <replica>
                    <host>{host}</host>
                    <port>{tcp_port}</port>
                    <user>netmon</user>
                    <password>netmon</password>
                </replica>
            </shard>
        </netmon>
    </remote_servers>
</yandex>
"""

ZOOKEEPER_CONFIG = """\
tickTime=2000
dataDir={db}
clientPort={port}
"""

LOGGER_CONFIG = """\
log4j.rootLogger=${zookeeper.root.logger}

log4j.appender.CONSOLE=org.apache.log4j.ConsoleAppender
log4j.appender.CONSOLE.Threshold=INFO
log4j.appender.CONSOLE.layout=org.apache.log4j.PatternLayout
log4j.appender.CONSOLE.layout.ConversionPattern=%d{ISO8601} - %-5p [%t:%C{1}@%L] - %m%n
"""


@pytest.yield_fixture(scope="session")
def session_tempdir():
    path = py.path.local(tempfile.mkdtemp())
    try:
        yield path
    finally:
        shutil.rmtree(str(path))


def wait_for_socket(host, port):
    for _ in xrange(300):
        try:
            socket.create_connection((host, port))
        except socket.error:
            time.sleep(0.1)
        else:
            break


@pytest.yield_fixture(scope="session")
def clickhouse(session_tempdir):
    with PortManager() as pm:
        host = "localhost"
        http_port = pm.get_port()
        tcp_port = pm.get_port()
        with tempfile.NamedTemporaryFile() as config:
            config.write(CLICKHOUSE_CONFIG.format(
                http_port=http_port,
                tcp_port=tcp_port,
                host=host,
                db=session_tempdir.mkdir("clickhouse-db"),
                tmp=session_tempdir.mkdir("clickhouse-tmp")
            ))
            config.flush()
            proc = execute([
                "dist/clickhouse-server",
                "--config-file={}".format(config.name)
            ], wait=False)
            wait_for_socket(host, tcp_port)
            try:
                yield (host, tcp_port)
            finally:
                proc.kill()


@pytest.yield_fixture(scope="session")
def zookeeper_logger_config(session_tempdir):
    with tempfile.NamedTemporaryFile() as config:
        config.write(LOGGER_CONFIG)
        config.flush()
        yield config.name


@pytest.yield_fixture(scope="session")
def zookeeper(session_tempdir, zookeeper_logger_config):
    java_path = session_tempdir.mkdir("java")
    zookeeper_path = "svn/target/zookeeper-3.4.6-jar-with-dependencies.jar"
    execute(["tar", "xf", "java.tgz", "--strip", "1", "-C", str(java_path)])
    with PortManager() as pm:
        host = "localhost"
        port = pm.get_port()
        with tempfile.NamedTemporaryFile() as config:
            config.write(ZOOKEEPER_CONFIG.format(
                port=port,
                db=session_tempdir.mkdir("zookeeper-db")
            ))
            config.flush()
            proc = execute([
                str(java_path.join("bin", "java")),
                "-cp", zookeeper_path,
                "-Djava.net.preferIPv6Addresses=true",
                "-Dzookeeper.root.logger=INFO,ROLLINGFILE",
                "-Dlog4j.configuration=file://{}".format(zookeeper_logger_config),
                "org.apache.zookeeper.server.ZooKeeperServerMain",
                config.name
            ], wait=False)
            wait_for_socket(host, port)
            try:
                yield (host, port)
            finally:
                proc.kill()


class WalleBlockedHostsHandler(tornado.web.RequestHandler):

    def get(self):
        self.write({"result": [
            "jmon-test.search.yandex.net"
        ]})


class WalleHostsHandler(tornado.web.RequestHandler):

    def get(self):
        self.write({"result": [
            {
                "inv": 1,
                "name": "jmon-unstable.search.yandex.net",
                "status": "dead",
                "state": "free"
            }
        ]})


class JugglerGroupsHandler(tornado.web.RequestHandler):

    def get(self):
        self.write({
            "success": True,
            "group": {
                "instances_list": [
                    ["sas1-{}.search.yandex.net".format(1000 + i), None, {}, None]
                    for i in xrange(1000)
                ] + [
                    ["man1-{}.search.yandex.net".format(1000 + i), None, {}, None]
                    for i in xrange(1000)
                ]
            }
        })


@pytest.yield_fixture(scope="session")
def http_server():
    http_app = tornado.web.Application([
        ("/v1/dns/blocked-host-names", WalleBlockedHostsHandler),
        ("/v1/hosts", WalleHostsHandler),
        ("/api/groups/request_group", JugglerGroupsHandler),
    ])

    loop = tornado.ioloop.IOLoop()
    server = tornado.httpserver.HTTPServer(http_app, io_loop=loop)

    def target():
        loop.make_current()
        loop.start()

    def setup(host, port):
        server.listen(port, host)

    def teardown():
        server.stop()
        loop.stop()

    thread = threading.Thread(target=target)
    thread.start()
    with PortManager() as pm:
        host = "localhost"
        port = pm.get_port()
        loop.add_callback(setup, host, port)
        wait_for_socket(host, port)
        try:
            yield (host, port)
        finally:
            loop.add_callback(teardown)
            thread.join()
            loop.close(all_fds=True)


@contextlib.contextmanager
def netmon_binary(path, settings, ports):
    host, http_port, tcp_port = ports
    with tempfile.NamedTemporaryFile() as config:
        json.dump(settings, config)
        config.flush()
        proc = execute([
            binary_path(path),
            "--config", config.name,
            "--http-port", str(http_port),
            "--interconnect-port", str(tcp_port),
            "--verbose"
        ], wait=False)
        wait_for_socket(host, http_port)
        wait_for_socket(host, tcp_port)
        try:
            yield (host, http_port, tcp_port)
        finally:
            proc.kill()


def generate_netmon_config(zookeeper, clickhouse, http_server, netmon_slicer_ports):
    netmon_host, _, netmon_tcp_port = netmon_slicer_ports
    fixture_url = "http://{}:{}".format(*http_server)
    return {
        "clickhouse": {
            "shards": [
                {
                    "host": clickhouse[0],
                    "port": clickhouse[1]
                }
            ],
            "probes_replicated": False,
            "username": "netmon",
            "password": "netmon"
        },
        "walle_url": fixture_url,
        "juggler_url": fixture_url,
        "blackbox_url": fixture_url,
        "sandbox_url": fixture_url,
        "staff_url": fixture_url,
        "slicer": {
            "shards": [
                {
                    "host": netmon_host,
                    "port": netmon_tcp_port
                }
            ]
        },
        "zookeeper": {
            "hosts": "{}:{}".format(*zookeeper),
            "prefix": "/"
        }
    }


@contextlib.contextmanager
def allocate_netmon_ports():
    with PortManager() as pm:
        host = "localhost"
        http_port = pm.get_port()
        tcp_port = pm.get_port()
        yield (host, http_port, tcp_port)


@pytest.yield_fixture(scope="session")
def netmon_slicer_ports():
    with allocate_netmon_ports() as ports:
        yield ports


@pytest.yield_fixture(scope="session")
def netmon_slicer(zookeeper, clickhouse, http_server, netmon_slicer_ports):
    settings = generate_netmon_config(zookeeper, clickhouse, http_server, netmon_slicer_ports)
    with netmon_binary("infra/netmon/slicer/netmon-slicer", settings, netmon_slicer_ports) as address:
        yield (address[0], address[2])


@pytest.yield_fixture(scope="session")
def netmon_aggregator_ports():
    with allocate_netmon_ports() as ports:
        yield ports


@pytest.yield_fixture(scope="session")
def netmon_aggregator(zookeeper, clickhouse, http_server, netmon_slicer, netmon_slicer_ports, netmon_aggregator_ports):
    settings = generate_netmon_config(zookeeper, clickhouse, http_server, netmon_slicer_ports)
    with netmon_binary("infra/netmon/aggregator/netmon-aggregator", settings, netmon_aggregator_ports) as address:
        yield (address[0], address[1])


@pytest.fixture(scope="session")
def netmon_aggregator_url(netmon_aggregator):
    return "http://{}:{}".format(*netmon_aggregator)
