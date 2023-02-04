from yatest.common.network import PortManager

from datetime import datetime, timedelta
from flask import Flask
from gevent.pywsgi import WSGIServer
from nose.tools import assert_true, assert_false, assert_raises
from requests.exceptions import ReadTimeout
from threading import Thread, Condition
import json

import maps.analyzer.services.jams_analyzer.tools.jams_uploader.lib.common as common


class TestServer(Thread):
    def __init__(self, port, data_section):
        Thread.__init__(self)
        self._port = port
        self._host = "localhost:" + str(self._port)
        self._data_section = data_section
        self._info = {data_section: {"size": 0, "last_signal_time": 0}}
        self._delay = 0
        self._cv = Condition()
        self._is_started = False
        with self._cv:
            self.start()
            while not self._is_started:
                self._cv.wait()

    def _shutdown(self):
        self.http_server.stop()

    def run(self):
        app = Flask(__name__)

        @app.route('/info')
        def info():
            i = 0
            while i < self._delay and not self.http_server._stop_event.wait(1):
                i += 1
            return json.dumps(self._info)

        self.http_server = WSGIServer(('', self._port), app)
        self.http_server.start()
        with self._cv:
            self._is_started = True
            self._cv.notify_all()
        while not self.http_server._stop_event.wait(1):
            pass

    def set_data(self, size, last_signal_time):
        self._info[self._data_section] = {"size": size, "last_signal_time": int((last_signal_time - datetime(1970, 1, 1)).total_seconds())}

    def set_delay(self, delay):
        self._delay = delay

    def hostname(self):
        return self._host


class TestServerManager(object):
    def __init__(self, data_section):
        self._data_section = data_section
        self._pm = PortManager()
        self._servers = []

    def __enter__(self):
        self._pm.__enter__()
        return self

    def __exit__(self, type, value, traceback):
        for server in self._servers:
            server._shutdown()
        for server in self._servers:
            server.join()
        self._pm.__exit__(type, value, traceback)

    def create(self):
        server = TestServer(self._pm.get_port(), self._data_section)
        self._servers.append(server)
        return server

    def data_section(self):
        return self._data_section


def test_http_coordination():
    with TestServerManager("jams_data") as tsm:
        hosts = [tsm.create() for _ in range(3)]
        associates = [x.hostname() for x in hosts]
        hostname, associates = associates[0], associates[1:]

        make_decision = lambda: common.pretty_good(hostname, associates, "analyzer-segmentshandler.maps.yandex.net", tsm.data_section())

        t = datetime.utcnow() - timedelta(seconds=5)
        for host in hosts:
            host.set_data(100000, t)

        assert_true(make_decision())

        hosts[0].set_delay(10)
        assert_raises(ReadTimeout, make_decision)

        hosts[0].set_delay(0)
        hosts[0].set_data(10000, t)
        assert_false(make_decision())

        hosts[1].set_delay(10)
        assert_false(make_decision())

        hosts[2].set_delay(10)
        assert_true(make_decision())

        for host in hosts[1:]:
            host.set_delay(0)
            host.set_data(100000, t - timedelta(days=1))
        assert_true(make_decision())

        for host in hosts[1:]:
            host.set_data(100000, t)
        hosts[0].set_data(100000, t - timedelta(days=1))
        assert_false(make_decision())

        associates[1] = "yandex.porn:12345"
        for host in hosts[:2]:
            host.set_data(100000, t)
        assert_true(make_decision())

        hosts[1].set_delay(10)
        assert_true(make_decision())
