# -*- coding: utf-8 -*-
import time
import xml.etree.ElementTree as et
import xml.dom.minidom as minidom

from balancer.test.util import asserts
from balancer_ctx import BalancerCtx, WeightsManager
from balancer.test.mock.mock_backend import MockBackend, MockConfGetter, MockStaticResponse, MockBackendsServer


def verbose_xmltree_find(xmltree_obj, path):
    obj = xmltree_obj.find(path)
    if obj is None:
        parsed = minidom.parseString(et.tostring(xmltree_obj, 'utf-8'))
        raise Exception("Could not find object {0}: {1}".format(path, parsed.toprettyxml(indent="\t")))
    return obj


class SubheavyContext(BalancerCtx):
    """ Operates on local balancer with local mocked backends """

    def __init__(self, balancer):
        super(SubheavyContext, self).__init__()

        self.balancer = balancer
        self._balancer_backends_data = balancer._mocked_data

        unistat_addr = self._balancer_backends_data['instance_unistat_addrs'][0]
        admin_addr = self._balancer_backends_data['instance_admin_addrs'][0]
        addr = self._balancer_backends_data['instance_addrs'][0]

        self._unistat_addr = (unistat_addr['ip'], unistat_addr['port'])
        self._admin_addr = (admin_addr['ip'], admin_addr['port'])
        self._addr = (addr['ip'], addr['port'])
        self._loadConfigGetter()
        self._start_server()
        self.weights_manager = WeightsManager(self, self.balancer.weights_dir)
        self.weights_manager.update_balancer_files_state()

    def _start_server(self, timeout=10**4):
        self._running = True
        self._server = MockBackendsServer("mocked_server")
        for backend in self._balancer_backends_data:
            if backend.startswith("backends_"):
                self._start_failing_backend(backend, timeout)
        self.balancer.start()
        self._server.start()

    def set_native_location(self, location):
        self._native_location = location

    def _loadConfigGetter(self):
        """ Loads (host, port, static_response) for all backends in balancer/test/mock/mock_server/static_response """
        self.configGetter = MockConfGetter(self._balancer_backends_data)

    def perform_prepared_request(self, req={}):
        """ Adds additional headers if not already defined"""

        if "headers" not in req:
            req["headers"] = {}
        if "path" not in req:
            req["path"] = "/"
        if "method" not in req:
            req["method"] = "get"

        realip = '8.8.8.8'
        ts = str(int(time.time() * 1000000))
        reqid = '{}-18226184799727450426'.format(ts)
        extra_headers = [
            ('host', 'yandex.ru'),
            ('X-Forwarded-For', realip),
            ('X-forwarded-For-Y', realip),
            ('x-req-id', reqid),
            ('X-Yandex-RandomUID', '6785791811526908838')
        ]

        for key, val in extra_headers:
            req["headers"][key] = val
        return self.perform_unprepared_request(req)

    def start_mocked_backend(self, backends_name, response=None, **kwargs):
        """
        Basically reloads server with updated static responses
        :param str backend_name: Backends name
        :param dict response: e.g. {"status": 200, "content": "I am service", "headers": [["Content-Type", "text/plain"]]}
        :rtype MockBackend
        """
        for config in self.configGetter.get(backends_name, no_response=(response is not None), **kwargs):
            if response is not None:
                config.mock_static_response = MockStaticResponse(response)
            host = config.get_host()
            self._server.backends[host].update_response(config.mock_static_response.data)
        assert self._server.reload()
        return self._server.backends[host]

    def start_mocked_backends(self, backends, response=None, **kwargs):
        mock_backends = []
        for b in backends:
            backend = self.start_mocked_backend(b, response, **kwargs)
            mock_backends.append(backend)
        return mock_backends

    def _start_failing_backend(self, backends_name, timeout, **kwargs):
        for config in self.configGetter.get(backends_name, no_response=True, **kwargs):
            config.mock_static_response = MockStaticResponse({"status": 500, "headers": [], "sleep_duration": "{}s".format(timeout)})
            backend = MockBackend(config)
            self._server.backends[backend.host] = backend

    # Fix below

    def start_laas_backends(self, response=None, backends_name=None):
        # FIXME Did not set version='HTTP/1.1'
        if response is None:
            mocked_location = '59.939095, 30.315868, 15000, 1524579307'
            mocked_region = '2'
            response = {"status": 200, "headers": [
                ['X-Region-Id', 'mocked_region'],
                ['X-Region-Location', mocked_location],
                ['X-Region-Suspected-Location', mocked_location],
                ['X-Region-City-Id', mocked_region],
                ['X-Region-Suspected-City', mocked_region],
                ['X-Region-By-IP', mocked_region],
                ['X-Region-Is-User-Choice', '0'],
                ['X-Region-Suspected', mocked_region],
                ['X-Region-Should-Update-Cookie', '0'],
                ['X-Region-Precision', mocked_region],
                ['X-Region-Suspected-Precision', mocked_region],
                ['X-IP-Properties', 'EOEB'],
            ], "content": ''}

        backends = self.start_mocked_backend('backends_man#man-prod-laas-yp', response)
        return response, backends

    def start_uaas_backends(self, exp_id=None, response=None, backends_name=None):
        if response is None:
            headers = [
                ['x-yandex-randomuid', '8360477071524594820'],
                ['X-Yandex-ExpSplitParams', 'eyJyIjoyLCJzIjoid2ViIiwiZCI6IiIsIm0iOiIiLCJiIjoiIiwiaSI6ZmFsc2V9'],
                ['X-Yandex-LogstatUID', '5d5da4f31438a77643bcc97caea31cfa'],
                ['X-Yandex-ExpConfigVersion', '9684'],
                ['Content-Length', '9'],
            ]
            if exp_id is not None:
                headers.append(['X-Yandex-ExpBoxes', str(exp_id)],)
            response = {"status": 200, "headers": headers, "content": 'USERSPLIT'}

        if backends_name is None:
            backends_name = 'backends_MAN_UAAS_hbf_mtn_1_'

        backends = self.start_mocked_backend(backends_name, response=response)
        return response, backends

    def start_rps_limiter_backends(self):
        location = 'man'
        if hasattr(self, '_native_location'):
            location = self._native_location

        self.start_mocked_backend(
            'backends_man#rpslimiter-web-{}'.format(location.lower()),
            response={'status': 200}
        )

    def start_remote_log_backends(self, response=None):
        if response is None:
            response = {'status': 200}

        backends = self.start_mocked_backend('backends_man#production-explogdaemon-man', response=response)
        return response, backends

    def wait_remote_log_flush(self):
        while True:
            unistat = self.get_unistat()
            if unistat['remote_log-in_queue_ammv'] == 0:
                break
            time.sleep(0.1)

    def start_service_backends(self, response=None, backends=[]):
        if response is None:
            response = {"content": "i am service", "status": 200}
        mock_backends = self.start_mocked_backends(backends, response)
        return response, mock_backends

    def _build_header_request_checker(self, msg, header_name):
        values = []
        for h, value in msg["headers"]:
            if header_name == h:
                values += [value]
        if len(values) > 0:
            return lambda request: asserts.header_values(request, header_name, values, case_sensitive=False)
        return lambda x: None

    def _build_header_prefix_request_checkers(self, msg, header_prefix):
        checkers = []
        for h, _ in msg["headers"]:
            if h.lower().startswith('x-region'):
                checkers.append(self._build_header_request_checker(msg, h))
        return checkers

    def _run_checkers(self, checkers, requests):
        for request in requests:
            for checker in checkers:
                try:
                    checker(request)
                except AssertionError as e:
                    raise AssertionError(str(e) + ', full request: {}'.format(request))

    def _check_antirobot_headers(self, antirobot_response, requests):
        headers = ['X-Yandex-Internal-Request', 'X-Yandex-Suspected-Robot']
        checkers = [self._build_header_request_checker(antirobot_response, h) for h in headers]
        self._run_checkers(checkers, requests)

    def _check_laas_headers(self, laas_response, requests):
        checkers = self._build_header_prefix_request_checkers(laas_response, 'x-region')
        self._run_checkers(checkers, requests)

    def _check_uaas_headers(self, uaas_response, requests):
        checkers = self._build_header_prefix_request_checkers(uaas_response, 'x-yandex-exp')
        self._run_checkers(checkers, requests)

    def base_service_test(
        self, request, backends_response, backends, has_antirobot=False,
        has_laas=True, has_uaas=True, service_stat_name=None,
        service_total_stat_name='service_total', exp_id=None,
    ):
        if has_antirobot:
            antirobot_response, antirobot_backends = self.start_antirobot_backends()
        if has_laas:
            laas_response, laas_backends = self.start_laas_backends()
        if has_uaas:
            uaas_response, uaas_backends = self.start_uaas_backends(exp_id=exp_id)
            remote_log_response, remote_log_backends = self.start_remote_log_backends()

        response, service_backends = self.start_service_backends(response=backends_response, backends=backends)
        response = self.perform_prepared_request(request)
        assert response.status_code == 200, 'status code is not OK'
        assert response.text == backends_response['content'], 'content is different'

        # suggest genBalancer sends ping-request with every user request, we shouldn't check it
        filter_ping_requests = lambda x: x.request_line.path != '/suggest-ping'
        filter_rps_limiter_requests = lambda x: x.request_line.path != '/quota.acquire'

        filter_requests = lambda x: filter_ping_requests(x) and filter_rps_limiter_requests(x)

        service_requests = None
        for backend in service_backends:
            if len(backend.state.get_requests(filter_requests)) > 0:
                service_requests = backend.state.get_requests(filter_requests)
                break
        assert service_requests is not None, 'failed to find a request to service backends'
        assert len(service_requests) == 1, 'too many requests: {}'.format(service_requests)

        requests = service_requests

        if has_uaas:
            self.wait_remote_log_flush()
            assert len(remote_log_backends.state.get_requests()) > 0, 'failed to find a request to uaas remote log'

            uaas_requests = uaas_backends.state.get_requests()
            assert len(uaas_requests) > 0, 'failed to find a request to uaas'
            self._check_uaas_headers(uaas_response, requests)
            requests += uaas_requests

        if has_laas:
            laas_requests = laas_backends.state.get_requests()
            assert len(laas_requests) > 0, 'failed to find a request to laas'
            self._check_laas_headers(laas_response, requests)
            requests += laas_requests

        if has_antirobot:
            assert sum(map(lambda x: len(x.state.get_requests()), antirobot_backends)) > 0, \
                'failed to find a request to antirobot'
            self._check_antirobot_headers(antirobot_response, requests)

        self.check_stats(service_stat_name, service_total_stat_name, has_antirobot, has_laas, has_uaas)

        return service_requests

    def run_base_service_test(self, path, exp_id=None, **kwargs):
        request = {"path": path, "headers": {}}
        if exp_id is not None:
            request["headers"]["Y-Balancer-Experiments"] = str(exp_id)
        backends_response = {"content": 'i am {}'.format(path), "status": 200}
        return self.base_service_test(request, backends_response, exp_id=exp_id, **kwargs)

    def run_location_weights_test(self, path, control_file, weights, backends, exception_message=None, **kwargs):
        weights_file = self.weights_manager.get_file(control_file)
        weights_file.set(weights)
        try:
            return self.run_base_service_test(path, backends=backends, **kwargs)
        except Exception as ex:
            if exception_message is None:
                raise
            assert exception_message in ex.message

    def run_header_test(self, path, backends, xffy_header=None):
        '''
        MINOTAUR-1039
        Проверяем, что сервисный балансер берет ip юзера из заголовка X-Forwarded-For-Y и не подставляет ip heavy
        '''
        request = {'path': path, 'headers': {'Host': 'yandex.ru'}, 'method': 'get'}
        if xffy_header is not None:
            request['headers']['X-forwarded-For-Y'] = xffy_header
        backends_response = {'content': 'i am {}'.format(path), 'status': 200}

        response, service_backends = self.start_service_backends(response=backends_response, backends=backends)
        response = self.perform_unprepared_request(request)
        assert response.status_code == 200
        assert response.text == backends_response['content']

        xffy_header_check = [xffy_header or '127.0.0.1']

        req_sum = 0
        for backend in service_backends:
            for request in backend.state.get_requests():
                if request.request_line.path == '/suggest-ping':
                    continue

                req_sum += 1
                assert request.headers['x-forwarded-for-y'] == xffy_header_check
        assert req_sum > 0
