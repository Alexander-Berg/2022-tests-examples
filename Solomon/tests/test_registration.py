from __future__ import print_function

import sys  # noqa
from time import sleep
from urlparse import parse_qs

import pytest
import yatest

from google.protobuf.text_format import Parse
from yatest.common.network import PortManager
from http_server import HttpServer, HttpHandler

from solomon.agent.protos.modules_config_pb2 import TAgentModuleConfig
from solomon.agent.protos.push_config_pb2 import TPushConfig
from solomon.agent.protos.registration_config_pb2 import TProviderRegistrationConfig
from solomon.protos.agent.agent_pb2 import TRegisterAgentRequest, TRegisterAgentResponse

from misc import send_metrics


CONF_PATH = yatest.common.test_source_path('data/registration.conf')

# TODO: replace all IPv4 addresses with IPv6
PUSH_TO_AGENT_CONF = """
    HttpPush {
        BindAddress: "127.0.0.1"
        BindPort: %(port)s
        Name: "httpPush"

        Handlers {
            Project: "solomon"
            Service: "test_registration"

            Endpoint: "/"
        }
    }
"""

PUSH_FROM_AGENT_CONF = """
    Hosts: [
        {
            Host: "127.0.0.1"
            Port: %(port)s
        }
    ],
    AllShards: true
    Cluster: "solomon_registration_test"
    PushInterval: "12s"
    RetryInterval: "5s"
    RetryTimes: 3
"""

PROVIDER = 'solomon'

REGISTRATION_CONF = """
    Provider: "%(provider)s"

    Endpoints {
        Url: "http://127.0.0.1:%(port)s/"
    }

    PullInterval: "15s"
"""

GOT_PUSH_REQUEST = False
GOT_REGISTRATION_REQUESTS = False
PROVIDER_IN_REQUEST = False
CORRECT_REGISTRATION_REQUEST = False


class PushHandler(HttpHandler):
    def do_POST(self):
        global GOT_PUSH_REQUEST
        GOT_PUSH_REQUEST = True

        self.send_response(200)
        self.send_header('content-type', 'application/text')
        self.end_headers()
        self.wfile.write('OK')


def create_registration_handler(fail_registration_req=False):
    class RegistrationHandler(HttpHandler, object):
        def __init__(self, *args, **kwargs):
            super(RegistrationHandler, self).__init__(*args, **kwargs)

        def do_POST(self):
            global GOT_REGISTRATION_REQUESTS
            global CORRECT_REGISTRATION_REQUEST, PROVIDER_IN_REQUEST

            GOT_REGISTRATION_REQUESTS = True

            length = int(self.headers["Content-Length"])
            request = self.rfile.read(length)

            try:
                registration_request = TRegisterAgentRequest.FromString(request)
                assert self.path.startswith('/?')
                qs = parse_qs(self.path[2:])

                PROVIDER_IN_REQUEST = ('provider' in qs) and len(qs['provider']) == 1 and qs['provider'][0] == PROVIDER

                CORRECT_REGISTRATION_REQUEST = registration_request.PullIntervalSeconds == 15
            except BaseException:
                CORRECT_REGISTRATION_REQUEST = False

            if self._fail_registration_req:
                self.send_response(500)
                self.send_header('content-type', 'application/text')
                self.end_headers()
                self.wfile.write('internal error')

                self._fail_registration_req = False
            else:
                response = TRegisterAgentResponse()
                response.RegisterDelaySeconds = 2

                self.send_response(200)
                self.send_header('content-type', 'application/x-protobuf')
                self.end_headers()
                self.wfile.write(response.SerializeToString())

    RegistrationHandler._fail_registration_req = fail_registration_req

    return RegistrationHandler


def send_data_to_agent(url):
    send_metrics(url, {
        'sensors': [
            {
                'labels': {
                    'label_name': 'label_value'
                },
                'timeseries': [
                    {'ts': i, 'value': i} for i in range(1, 100)
                ]
            }
        ]
    })


class CommonSetup(object):
    def _initialize(self, net, fail_registration_req=False):
        registration_server_port = net.get_port()
        push_server_port = net.get_port()
        port_for_pushing_to_agent = net.get_port()

        # TODO: specify jitter value to spend less time waiting
        self._url_for_pushing_to_agent = 'http://127.0.0.1:' + str(port_for_pushing_to_agent)

        self.push_server = HttpServer(push_server_port, PushHandler)
        registration_handler = create_registration_handler(fail_registration_req)
        self.registration_server = HttpServer(registration_server_port, registration_handler)

        self.push_config = Parse(PUSH_FROM_AGENT_CONF % {'port': push_server_port}, TPushConfig())
        registration_config_str = REGISTRATION_CONF % {
            'port': registration_server_port,
            'provider': PROVIDER,
        }
        self.registration_config = Parse(registration_config_str, TProviderRegistrationConfig())

        self.modules_conf = Parse(PUSH_TO_AGENT_CONF % {'port': port_for_pushing_to_agent}, TAgentModuleConfig())

    def _start_servers(self):
        global GOT_PUSH_REQUEST, GOT_REGISTRATION_REQUESTS
        global CORRECT_REGISTRATION_REQUEST, PROVIDER_IN_REQUEST

        GOT_PUSH_REQUEST = False
        GOT_REGISTRATION_REQUESTS = False
        CORRECT_REGISTRATION_REQUEST = False
        PROVIDER_IN_REQUEST = False

        try:
            self.push_server.start()
            self.registration_server.start()
            yield  # pytest: everything after a yield is a teardown procedure
        finally:
            self.push_server.stop()
            self.registration_server.stop()


class TestNoRegistrationConfig(CommonSetup):
    CONF_OVERRIDES = {}

    @pytest.fixture(autouse=True)
    def agent_conf(self, request):
        with PortManager() as net:
            self._initialize(net)

            TestNoRegistrationConfig.CONF_OVERRIDES = {
                "Modules": [self.modules_conf],
                "Push": self.push_config,
                # No Registration
            }

            for elem in self._start_servers():
                yield elem

    def test(self, agent):
        send_data_to_agent(self._url_for_pushing_to_agent)

        sleep(30)

        global GOT_PUSH_REQUEST, GOT_REGISTRATION_REQUESTS

        assert GOT_PUSH_REQUEST is True
        assert GOT_REGISTRATION_REQUESTS is False


class TestSuccessfullRegistration(CommonSetup):
    CONF_OVERRIDES = {}

    @pytest.fixture(autouse=True)
    def agent_conf(self, request):
        with PortManager() as net:
            self._initialize(net)

            TestSuccessfullRegistration.CONF_OVERRIDES = {
                "Modules": [self.modules_conf],
                "Push": self.push_config,
                "Registration": self.registration_config,
            }

            for elem in self._start_servers():
                yield elem

    def test(self, agent):
        send_data_to_agent(self._url_for_pushing_to_agent)

        sleep(40)

        global GOT_PUSH_REQUEST, GOT_REGISTRATION_REQUESTS, PROVIDER_IN_REQUEST

        assert GOT_PUSH_REQUEST is False
        assert GOT_REGISTRATION_REQUESTS is True
        assert CORRECT_REGISTRATION_REQUEST is True
        assert PROVIDER_IN_REQUEST is True


class TestFailedToRegisterAtFirst(CommonSetup):
    CONF_OVERRIDES = {}

    @pytest.fixture(autouse=True)
    def agent_conf(self, request):
        with PortManager() as net:
            self._initialize(net, fail_registration_req=True)

            self.push_config.PushInterval = "15s"

            TestFailedToRegisterAtFirst.CONF_OVERRIDES = {
                "Modules": [self.modules_conf],
                "Push": self.push_config,
                "Registration": self.registration_config,
            }

            for elem in self._start_servers():
                yield elem

    def test(self, agent):
        send_data_to_agent(self._url_for_pushing_to_agent)

        sleep(32)  # first registration request

        global GOT_PUSH_REQUEST, GOT_REGISTRATION_REQUESTS, PROVIDER_IN_REQUEST

        assert GOT_PUSH_REQUEST is False
        assert GOT_REGISTRATION_REQUESTS is True
        assert CORRECT_REGISTRATION_REQUEST is True
        assert PROVIDER_IN_REQUEST is True

        sleep(20)  # retries to register and fails

        assert GOT_PUSH_REQUEST is True  # fallback to push mode

        GOT_PUSH_REQUEST = False
        sleep(30)  # should successfuly register now
        sleep(25)  # waits for another push request

        assert GOT_PUSH_REQUEST is False  # but there are none, since we've registered
