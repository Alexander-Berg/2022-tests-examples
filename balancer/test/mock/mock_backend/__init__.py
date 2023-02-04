import os
import json
import time
import requests
import subprocess
import yatest.common
from yatest.common import test_output_path

from balancer.test.util.proto.http.message import HTTPRequestLine, HTTPRequest


class MockStaticResponse:
    def __init__(self, data=None):
        if data is None:
            data = {}
        assert isinstance(data, dict)
        self.data = data

    def update(self, key, value):
        self.data[key] = value

    def dumps(self):
        return json.dumps(self.data, indent=2)


class MockServerProtoConfig:
    def __init__(self):
        self.conf = dict()

    def set(self, key, value):
        self.conf[key] = value
        return self

    def dumps(self):
        config = []
        for key, val in self.conf.iteritems():
            if isinstance(val, str):
                val = '"%s"' % val
            config.append(": ".join([str(key), str(val)]))
        return "\n".join(config)


class MockBackendConfig:
    """ Merged config """
    def __init__(self, host, port, backends_name, mock_static_response):
        self.__host = host
        self.__port = port
        self.__backends_name = backends_name
        self.mock_static_response = mock_static_response

    def get_host(self):
        return self.__host

    def get_port(self):
        return self.__port

    def get_backends_name(self):
        return self.__backends_name


class MockServerConfig:
    """ server config and static responses for all addresses """
    def __init__(self, mock_server_proto_conf, mock_static_response):
        self.conf = mock_server_proto_conf
        self.response = mock_static_response

    def dump(self, config_path, response_path):
        with open(config_path, 'w') as f:
            f.write(self.conf.dumps())
        with open(response_path, 'w') as f:
            f.write(self.response.dumps())


class MockConfGetter:
    def __init__(self, mocked_data, no_response=False):
        # responses.json contain {backends_name: static_response}
        if no_response:
            self._response_data = {}
        else:
            self._response_data_path = yatest.common.source_path('balancer/test/mock/mock_server/static_responses/responses.json')
            self._response_data = self.__load(self._response_data_path)
        self._mocked_data = mocked_data

    def __load(self, source_path):
        return json.load(open(source_path))

    def get(self, backends_name, no_response=False, **kwargs):
        """ Returns one MockBackendConfig """
        if backends_name not in self._mocked_data:
            raise Exception("MockConfGetter: no mocked data for backend '{}'".format(backends_name))

        if backends_name not in self._response_data and not no_response:
            raise Exception("MockConfGetter: no response defined for backend '{}'".format(backends_name))

        for mocked_data in self._mocked_data[backends_name]:
            response = MockStaticResponse()
            if not no_response:
                response = MockStaticResponse(self._response_data[backends_name])
            yield MockBackendConfig(mocked_data["host"], mocked_data["port"], backends_name, response)


class MockBackendState:
    def __init__(self, data):
        self._requests = data
        self._requsts_by_uid = dict()
        self.requests = []
        for i in xrange(len(self._requests)):
            request = self._requests[i]
            reqline = HTTPRequestLine(*request["request_line"].split())
            httpreq = HTTPRequest(reqline, request["headers"], request["data"])
            self.requests.append(httpreq)
            key = None
            value = None
            for key in request["headers"]:
                value = request["headers"][key]
                key = key.lower()
            if "x-req-id" == key:
                reqid = value
                self._requsts_by_uid[reqid] = httpreq

    def get_request(self, reqid):
        return self._requsts_by_uid.setdefault(reqid, None)

    def get_requests(self, filter_func=lambda x: True):
        return list(filter(filter_func, self.requests))


class MockBackend(object):
    def __init__(self, mock_backend_config):
        self.__conf = mock_backend_config
        self.host, self.port = self.__conf.get_host(), self.__conf.get_port()
        self._running = False

    def ping(self, path):
        resp = self.simple_request(path, no_check=True)
        return resp.status_code == 200

    def simple_request(self, path="/dumb?true=verytrue", no_check=False):
        assert self._running or no_check
        return requests.get("http://{}:{}{}".format(self.host, self.port, path))

    @property
    def mock_static_response(self):
        return self.__conf.mock_static_response

    @property
    def state(self):
        return MockBackendState(self.simple_request("/admin?action=get_all_requests").json())

    def update_response(self, response):
        self.__conf.mock_static_response = MockStaticResponse(response)

    def get_response(self, key, value):
        return self.__conf.mockStaticResponse

    def get_backends_name(self):
        return self.__conf.get_backends_name()


class MockBackendsServer(object):
    def __init__(self, name=None, bin_path=None, pytest_mod=True):
        if name is None:
            name = 'server_at_%d' % time.time()
        self.name = name
        self.backends = {}
        self.__bin_path = yatest.common.binary_path("balancer/test/mock/mock_server/mock_server") if (bin_path is None and pytest_mod) else bin_path
        test_output_path_func = test_output_path if pytest_mod else os.path.abspath

        self.__conf_path = test_output_path_func("serverconf_{}.pb.txt".format(self.name))
        self.__response_path = test_output_path_func("static_response_{}.json".format(self.name))
        self.__log_file = test_output_path_func("mocked_server_logs_{}.logs".format(self.name))
        self.__backend_ips_file = test_output_path_func("backend_ips_{}.json".format(self.name))
        self.__mstats_file = test_output_path_func("mstat_{}.json".format(self.name))

        self._running = False

    def get_response_path(self):
        return self.__response_path

    def configure(self, **kwargs):
        assert len(self.backends) > 0, "no backends to configure"
        self.__conf = MockServerConfig(self._generateServerConf(**kwargs), self._generateStaticResponses(**kwargs))
        self._generate_backend_ips()
        self.__conf.conf.set("LogFile", self.__log_file)

    def _generateServerConf(self, **kwargs):
        port = self.backends[self.backends.keys()[0]].port
        addresses = []
        for _, backend in self.backends.iteritems():
            assert backend.port == port, "all backends should have same port"
            addresses += [backend.host]
        # addresses = "[{}]".format(", ".join(addresses))
        proto_config = MockServerProtoConfig().set("Threads", 2).set("Port", port).set("BindAddress", addresses)
        if "Threads" in kwargs:
            proto_config.set("Threads", kwargs["Threads"])
        if "KeepAliveEnabled" in kwargs:
            proto_config.set("KeepAliveEnabled", kwargs["KeepAliveEnabled"])
        if "RequestTTL" in kwargs:
            proto_config.set("RequestTTL", kwargs["RequestTTL"])

        return proto_config

    def _generateStaticResponses(self, **kwargs):
        responses = {}
        if 'static_responses' in kwargs:
            return MockStaticResponse(kwargs['static_responses'])
        for _, backend in self.backends.iteritems():
            responses[backend.host] = backend.mock_static_response.data
        return MockStaticResponse(responses)

    def _generate_backend_ips(self):
        self.backend_ips = {}
        for _, backend in self.backends.iteritems():
            self.backend_ips[backend.host] = backend.get_backends_name()

    def start(self, **kwargs):
        if self._running:
            raise Exception("Mock backend already running")
        self.configure(**kwargs)
        self.__conf.dump(self.__conf_path, self.__response_path)
        with open(self.__backend_ips_file, 'w') as fd:
            json.dump(self.backend_ips, fd)
        self._running = True
        cmd = [self.__bin_path, "-c", self.__conf_path, "-r", self.__response_path, '-b', self.__backend_ips_file, "-s"]
        if 'do_not_save_requests' in kwargs and kwargs['do_not_save_requests']:
            cmd = cmd[:-1]
        self.__proc = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

        if 'pytest_mod' in kwargs and not kwargs['pytest_mod']:
            cmd_path = os.path.abspath("mock_server_start.sh")
        else:
            cmd_path = test_output_path("mock_server_start.sh")

            with (open(cmd_path, 'w')) as f:
                f.write(" ".join(cmd))

        tout = 0.10
        tries = 100
        backend = self.backends[self.backends.keys()[0]]

        for i in xrange(tries):

            if self.__proc.returncode is not None:
                stdout, stderr = self.__proc.communicate()
                raise Exception('Mock server exited with code {}.\nStdout: {}\nStderr: {}'.format(self.__proc.returncode, stdout, stderr))

            if i + 1 >= tries:
                self.__proc.kill()
                stdout, stderr = self.__proc.communicate()
                raise Exception("Mock server didn't start after {} tries.\nStdout: {}\nStderr: {}".format(tries, stdout, stderr))

            try:
                time.sleep(tout)
                if backend.ping("/admin?action=get_all_requests"):
                    break
            except requests.exceptions.ConnectionError:
                continue
            except requests.exceptions.Timeout:
                continue

        for _, backend in self.backends.iteritems():
            backend._running = True

        return self.reload(reconfigure=False)

    def reload(self, reconfigure=True, do_not_dump=False, **kwargs):
        backend = self.backends[self.backends.keys()[0]]
        if reconfigure:
            self.configure(**kwargs)
        if not do_not_dump:
            self.__conf.dump(self.__conf_path, self.__response_path)
        req = backend.simple_request("/admin?action=reload_static_responses")
        return req.status_code == 200

    def stop(self):
        if self._running and len(self.backends) > 0:
            backend = self.backends[self.backends.keys()[0]]
            mstat_req = backend.simple_request("/remote_admin?action=mstat")
            with open(self.__mstats_file, 'w') as f:
                f.write(json.dumps({'code': mstat_req.status_code, 'content': mstat_req.content}))
            shutdown_status_code = backend.simple_request("/admin?action=shutdown").status_code
            assert shutdown_status_code == 200
            stdout, stderr = self.__proc.communicate()
            if len(stdout) or len(stderr):
                raise Exception('stdout: {},\nstderr: {}'.format(stdout, stderr))
            self._running = False
            for _, b in self.backends.iteritems():
                b._running = False

    def __del__(self):
        if self._running:
            self.stop()
