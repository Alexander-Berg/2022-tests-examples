# -*- coding: utf-8 -*-
import os
import time
import requests
import json


class WeightsManagerFile(object):
    def __init__(self, weights_manager, path, check_from_stats):
        super(WeightsManagerFile, self).__init__()

        self.weights_manager = weights_manager
        self.path = path
        self.check_from_stats = check_from_stats
        self.weights = {}

    def close(self):
        try:
            os.remove(self.path)
            # is it good, or is it bad?
        except Exception:
            pass

    def set(self, source_weights):
        """
        Creates weihts file with weights from source_weights
        :param dict source_weights: weights, e.g. {"search_man" : 1, "search_sas": 1}
        """
        self.weights = source_weights
        to_save = ('{}, {}'.format(s, w) for s, w in self.weights.iteritems())
        with open(self.path, 'w') as f:
            f.write('\n'.join(to_save) + '\n')

        if self.check_from_stats:
            self.weights_manager._wait_file_sync_for(self.path)

    def __enter__(self):
        return self

    def __exit__(self, exception_type, exception_value, traceback):
        if exception_type is not None:
            raise
        self.close()


class WeightsManager(object):
    """ Manages weights for balancer  """
    def __init__(self, balancer_ctx, dir_path):
        super(WeightsManager, self).__init__()

        self.balancer_ctx = balancer_ctx
        self.dir_path = dir_path

    def get_file(self, filename, check_from_stats=True):
        return WeightsManagerFile(self, os.path.join(self.dir_path, filename), check_from_stats)

    def _parse_balancer_files_state(self, result):
        state = {}
        for worker_result in result:
            for fname, curr_id in worker_result[0]['file-readers'].iteritems():
                if fname not in state:
                    state[fname] = curr_id
                else:
                    state[fname] = min(state[fname], curr_id)
        return state

    def _wait_file_sync_for(self, path):
        previous_state = self._balancer_files_state[path]
        self.update_balancer_files_state()

        while self._balancer_files_state[path] <= previous_state:
            self.update_balancer_files_state()
            time.sleep(0.1)

    def update_balancer_files_state(self):
        resp = self.balancer_ctx.perform_admin_request({
            'method': 'get',
            'path': '/admin/events/json/dump_shared_files',
            'headers': {}
        })
        self._balancer_files_state = self._parse_balancer_files_state(json.loads(resp.text))


class BalancerCtx(object):
    @property
    def native_location(self):
        return self._native_location

    def perform_request(self, addr, req):
        methods_map = {'get': requests.get, 'post': requests.post, 'head': requests.head}
        return methods_map[req['method']]('http://{}:{}{}'.format(addr[0], addr[1], req['path']), headers=req.get('headers'), allow_redirects=False)

    def perform_admin_request(self, req):
        response = self.perform_request(self._admin_addr, req)
        assert response.status_code == 200
        return response

    def perform_unprepared_request(self, req):
        return self.perform_request(self._addr, req)

    def get_unistat(self):
        req = {'path': '/unistat', 'method': 'get'}
        response = self.perform_request(self._unistat_addr, req)
        assert response.status_code == 200
        return dict(json.loads(response.text))

    def stopall(self):
        if self._running:
            self._server.stop()
            self._running = False

    def check_stats(self, service_name, service_total_stat_name, has_antirobot, has_laas, has_uaas):
        unistat = self.get_unistat()

        sum_unistat_hgram = lambda hgram: sum(map(lambda x: x[1], unistat[hgram]))

        def check_stat_report(name, should_be, report_size=False):
            counter = 1 if should_be else 0
            assert unistat.get('report-{}-succ_summ'.format(name), 0) == counter, "succ summ to {} != {}".format(name, counter)
            assert unistat.get('report-{}-fail_summ'.format(name), 0) == 0, "fail summ to {} != {}".format(name, counter)

            if report_size:
                assert sum_unistat_hgram('report-{}-input_size_hgram'.format(name)) == counter, "input size mismatch for {}".format(name)
                assert sum_unistat_hgram('report-{}-output_size_hgram'.format(name)) == counter, "input size mismatch for {}".format(name)

                if should_be:
                    assert len(unistat['report-{}-input_size_hgram'.format(name)]) > 0, "failed to find requests {}".format(name)
                    assert len(unistat['report-{}-output_size_hgram'.format(name)]) > 0, "failed to find requests {}".format(name)
                else:
                    assert len(unistat['report-{}-input_size_hgram'.format(name)]) == 0, "failed to find requests {}".format(name)
                    assert len(unistat['report-{}-output_size_hgram'.format(name)]) == 0, "failed to find requests {}".format(name)

        check_stat_report('antirobot', has_antirobot)
        check_stat_report('l7heavygeobasemodule', has_laas)
        check_stat_report('expgetter', has_uaas)
        check_stat_report('remotelog', has_uaas)

        if service_total_stat_name is not None:
            check_stat_report(service_total_stat_name, True, True)

        if service_name is not None:
            check_stat_report(service_name, True, True)

    def start_antirobot_backends(self, response=None):
        headers = [['X-Yandex-Internal-Request', '0'], ['X-Yandex-Suspected-Robot', '0']]
        if response is None:
            response = {'status': 200, 'headers': headers}
        return response, [
            self.start_mocked_backend(
                'backends_{0}#prod-antirobot-yp-prestable-{0}'.format(self.native_location.lower()), response),
            self.start_mocked_backend(
                'backends_{0}#prod-antirobot-yp-{0}'.format(self.native_location.lower()), response),
        ]
