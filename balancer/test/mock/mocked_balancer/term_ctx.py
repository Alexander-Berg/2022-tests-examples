# -*- coding: utf-8 -*-
import time
import requests

from balancer_ctx import BalancerCtx, WeightsManager
from balancer.test.mock.mock_backend import MockBackend, MockConfGetter, MockStaticResponse, MockBackendsServer


def skip_pings(requests):
    return [r for r in requests if r.request_line.path not in ('/awacs-balancer-health-check', '/balancer-health-check', '/slb_ping')]


class TermContext(BalancerCtx):
    def __init__(self, balancer):
        super(TermContext, self).__init__()

        self.balancer = balancer
        self._balancer_backends_data = balancer._mocked_data
        self._running = False
        self.weights_manager = WeightsManager(self, self.balancer.weights_dir)

    def _start_server(self, backends, timeout=10**4, pytest_mod=False, bin_path=None, do_not_save_requests=False):
        self._server = MockBackendsServer('term_ctx_mocked_server', pytest_mod=pytest_mod, bin_path=bin_path)
        for backend in backends:
            if backend.startswith('backends'):
                self._start_failing_backend(backend, timeout)
        self._server.start(pytest_mod=pytest_mod)
        self._running = True

    def initialize(self, location, backends, balancer_backend='slb_ru_man', pytest_mod=True, bin_path=None, do_not_save_requests=False):
        assert location is not None, 'location can not be None'
        self._native_location = location

        unistat_addr = self._balancer_backends_data['instance_unistat_addrs'][0]
        admin_addr = self._balancer_backends_data['instance_admin_addrs'][0]

        self._unistat_addr = (unistat_addr['ip'], unistat_addr['port'])
        self._admin_addr = (admin_addr['ip'], admin_addr['port'])

        self._addr = (
            self._balancer_backends_data['ipdispatch_{}'.format(balancer_backend)][0],
            self._balancer_backends_data['port_{}'.format(balancer_backend)]
        )

        self._load_config_getter(not pytest_mod)
        self.balancer.start(pytest_mod=pytest_mod)

        if backends:
            self._start_server(backends, pytest_mod=pytest_mod, bin_path=bin_path, do_not_save_requests=do_not_save_requests)

        self.weights_manager.update_balancer_files_state()

    def _load_config_getter(self, no_response=False):
        """ Loads (host, port, static_response) for all backends in balancer/test/mock/mock_server/static_response """
        self.config_getter = MockConfGetter(self._balancer_backends_data, no_response)

    def perform_request_xfail(self, req, ssl=None):
        """ no https, fails if balancer doesn't reset connection , doesn't return anything """
        if ssl:
            raise NotImplementedError()
        try:
            self.perform_unprepared_request(req)
        except requests.exceptions.ConnectionError, e:
            if "Connection aborted" not in str(e):
                raise

    def start_mocked_backend(self, backends_name, response=None):
        """
        Basically reloads server with updated static responses
        :param str backend_name: Backends name
        :param dict response: e.g. {"status": 200, "content": "I am service", "headers": [["Content-Type", "text/plain"]]}
        :rtype MockBackend
        """
        for config in self.config_getter.get(backends_name, no_response=(response is not None)):
            if response is not None:
                config.mock_static_response = MockStaticResponse(response)
            host = config.get_host()
            self._server.backends[host].update_response(config.mock_static_response.data)
        assert self._server.reload()
        return self._server.backends[host]

    def _start_failing_backend(self, backends_name, timeout):
        for config in self.config_getter.get(backends_name, no_response=True):
            config.mock_static_response = MockStaticResponse({'status': 500, 'headers': [], 'sleep_duration': '{}s'.format(timeout)})
            backend = MockBackend(config)
            self._server.backends[backend.host] = backend

    def start_responding_backends(self, succesful_backends_response, responding_backends):
        retval = []
        for b in responding_backends:
            retval.append(self.start_mocked_backend(b, succesful_backends_response))
        return retval

    def _enable_location_weigths(self, control_file, weights):
        weights_file = self.weights_manager.get_file(control_file, check_from_stats=False)
        weights_file.set(weights)

    def _build_service_request(self, path):
        return {'method': 'get', 'path': path, 'headers': {'Host': 'yandex.ru'}}

    def base_knoss_test(self, path, responding_backends, has_antirobot=True, service_stat_name=None):
        if has_antirobot:
            antirobot_response, antirobot_backends = self.start_antirobot_backends()

        request = self._build_service_request(path)
        succesful_backends_response = {'status': 200, 'content': 'i am {}'.format(path), 'headers': []}

        backends = self.start_responding_backends(succesful_backends_response, responding_backends)
        response = self.perform_unprepared_request(request)

        assert response.status_code == 200
        assert response.text == succesful_backends_response['content']

        if has_antirobot:
            assert sum(map(lambda x: len(x.state.get_requests()), antirobot_backends)) > 0, \
                'failed to find a request to antirobot'

        req_sum = 0
        for backend in backends:
            requests = skip_pings(backend.state.get_requests())
            req_sum += len(requests)
            if has_antirobot:
                for request in requests:
                    for header in ['x-yandex-internal-request', 'x-yandex-suspected-robot']:
                        assert request.headers[header] == ['0']

        assert req_sum > 0

        self.check_stats(service_stat_name, 'service_total', has_antirobot, False, False)

    def run_knoss_test(
        self, service_name, path, responding_backends, has_antirobot=True, location=None, balancer_backend='slb_ru_man',
        service_stat_name=None,
    ):
        other_backends = [
            'backends_{0}#prod-antirobot-yp-{0}'.format(location.lower()),
            'backends_{0}#prod-antirobot-yp-prestable-{0}'.format(location.lower()),
        ] if has_antirobot else []
        self.initialize(location, responding_backends + other_backends, balancer_backend)

        self.base_knoss_test(path, responding_backends, has_antirobot, service_stat_name)

    def start_failing_backends(self, failing_backends, timeout=10**4):
        retval = []
        for b in failing_backends:
            retval.append(self.start_mocked_backend(b, {'status': 500, 'headers': [], 'sleep_duration': '{}s'.format(timeout)}))
        return retval

    def start_unrequested_backends(self, unrequested_backends):
        retval = []
        for b in unrequested_backends:
            retval.append(self.start_mocked_backend(b, {'status': 200, 'headers': []}))
        return retval

    def base_knoss_single_attempt_test(self, request, failing_backends, unrequested_backends):
        failing = self.start_failing_backends(failing_backends)
        reqid = '{}-18226184799727450426'.format(str(int(time.time() * 1000000)))
        request['headers']['x-req-id'] = reqid
        unrequested = self.start_unrequested_backends(unrequested_backends)
        # check if failed backend got request
        for b in failing:
            b.state.get_request(reqid)
        self.perform_request_xfail(request)
        for b in unrequested:
            assert len(skip_pings(b.state.get_requests())) == 0, 'backend was requested when it should have not been. Probably, attempts != 1 as should be'

    def run_knoss_single_attempt_test(self, service_name, path, failing_backends, unrequested_backends, location=None, balancer_backend='slb_ru_man'):
        assert len(unrequested_backends) > 0, 'test is meaningless if no unrequested_backends checked'

        self.initialize(location, failing_backends + unrequested_backends, balancer_backend)
        request = self._build_service_request(path)
        return self.base_knoss_single_attempt_test(request, failing_backends, unrequested_backends)

    def run_count_attempts_test(self, service_name, path, failing_backends, location=None, balancer_backend='slb_ru_man'):
        self.initialize(location, failing_backends, balancer_backend)

        request = self._build_service_request(path)
        tout = 1
        failing = self.start_failing_backends(failing_backends, timeout=tout)
        self.perform_request_xfail(request)
        failing_sum = 0
        for b in failing:
            failing_sum += len(skip_pings(b.state.get_requests()))

        assert(failing_sum <= 1)

    def run_knoss_location_weights_test(
        self, service_name, path, control_file, weights, backends, has_antirobot=True, location=None, balancer_backend='slb_ru_man',
        service_stat_name=None,
    ):
        self._enable_location_weigths(control_file, weights)

        other_backends = [
            'backends_{0}#prod-antirobot-yp-{0}'.format(location.lower()),
            'backends_{0}#prod-antirobot-yp-prestable-{0}'.format(location.lower()),
        ] if has_antirobot else []
        self.initialize(location, backends + other_backends, balancer_backend)

        self.base_knoss_test(path, backends, has_antirobot, service_stat_name)

    def run_knoss_header_test(self, service_name, path, responding_backends, location=None, balancer_backend='slb_ru_man'):
        """
        MINOTAUR-1039
        Проверяем, что heavy подставляет ip юзера в заголовок X-Forwarded-For-Y
        """
        self.initialize(location, responding_backends, balancer_backend)

        request = self._build_service_request(path)
        succesful_backends_response = {'status': 200, 'content': 'i am {}'.format(path)}
        request['headers']['x-forwarded-for-y'] = '1.1.1.1'
        backends = self.start_responding_backends(succesful_backends_response, responding_backends)
        response = self.perform_unprepared_request(request)
        assert response.status_code == 200
        assert response.text == succesful_backends_response['content']

        req_sum = 0
        for backend in backends:
            requests = skip_pings(backend.state.get_requests())
            req_sum += len(requests)
            for request in requests:
                assert request.headers['x-forwarded-for-y'] == ['127.0.0.1']
        assert req_sum > 0

    def run_knoss_5xx_test(self, service_name, path, responding_backends, location=None, balancer_backend='slb_ru_man'):
        self.initialize(location, responding_backends, balancer_backend)

        request = self._build_service_request(path)
        backends_response = {'status': 500, 'content': 'i am {}'.format(path), 'headers': []}

        backends = self.start_responding_backends(backends_response, responding_backends)
        response = self.perform_unprepared_request(request)

        assert response.status_code == 500
        assert response.text == backends_response['content']

        req_sum = 0
        for backend in backends:
            req_sum += len(skip_pings(backend.state.get_requests()))
        assert req_sum > 0

    def run_knoss_balancer_hint(
        self,
        service_name,
        path,
        responding_backends,
        location=None,
        balancer_backend='slb_ru_man',
        balancer_hint=None,
    ):
        self.initialize(location, responding_backends, balancer_backend)

        request = self._build_service_request(path)
        request['headers']['x-yandex-balancing-hint'] = 'i-m-hacker'

        succesful_backends_response = {'status': 200, 'content': 'i am {}'.format(path)}

        backends = self.start_responding_backends(succesful_backends_response, responding_backends)
        response = self.perform_unprepared_request(request)

        assert response.status_code == 200
        assert response.text == succesful_backends_response['content']

        req_sum = 0
        for backend in backends:
            requests = skip_pings(backend.state.get_requests())
            req_sum += len(requests)
            for request in requests:
                if balancer_hint is None:
                    assert request.headers['x-yandex-balancing-hint'] == []
                else:
                    assert request.headers['x-yandex-balancing-hint'] == [balancer_hint]
        assert req_sum > 0

    def run_knoss_host_banned(self, service_name, path, responding_backends, location=None, balancer_backend='slb_ru_man'):
        self.initialize(location, responding_backends, balancer_backend)

        request = self._build_service_request(path)
        request['headers']['Host'] = 'man.yandex.ru'

        succesful_backends_response = {'status': 200, 'content': 'i am {}'.format(path)}

        backends = self.start_responding_backends(succesful_backends_response, responding_backends)
        response = self.perform_unprepared_request(request)
        assert response.status_code == 406

        for backend in backends:
            assert len(skip_pings(backend.state.get_requests())) == 0


def knoss_backends_with_ids(backends):
    if len(backends) == 2:
        ids = ['local_dc', 'nonlocal_dc']
    else:
        ids = ['backend_{}'.format(i) for i in xrange(len(backends))]
    return backends, ids


def knoss_backends_single_attempt_with_ids(backends):
    if len(backends) <= 1:
        return [], []
    backend_pairs = []
    if len(backends) == 2:
        ids = ['local_dc_fails']
    else:
        ids = ['_'.join(backend_group) for backend_group in backends[:len(backends)-1]]

    for i in xrange(len(backends) - 1):
        failing_backend = backends[i]
        unrequested_backends = [backend for backend_group in backends[i+1:] for backend in backend_group]
        backend_pairs.append((failing_backend, unrequested_backends))
    return backend_pairs, ids
