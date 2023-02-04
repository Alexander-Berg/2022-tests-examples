# coding: utf-8

from yatest import common

import logging
import os
import random
import time
import requests
import hashlib
import json
import six


class LocalSaas:
    def __init__(self, service_configs_dir=None, cluster_config=None, cwd='.', config_patch=None):
        self._process = None
        self._search_port = 0
        self._indexer_port = 0
        self._service_configs_dir = service_configs_dir
        self._cluster_config = cluster_config if cluster_config else 'cluster_1be_internal.cfg'
        self._cwd = cwd
        self._config_patch = config_patch

    def _read_ports(self, logfile):
        try:
            with open(logfile, 'r') as f:
                cont = f.read()
            if ';search_port=' not in cont or ';indexer_port=' not in cont:
                return False
            parts = cont.split(';')
            spp = [p.split('=')[1] for p in parts if p.startswith('search_port=')][0]
            ipp = [p.split('=')[1] for p in parts if p.startswith('indexer_port=')][0]
            self._search_port = int(spp)
            self._indexer_port = int(ipp)
        except Exception as e:
            logging.error('problem with opening file %s, error %s', logfile, e)

    def link_binary(self, src, dst):
        if os.path.exists(dst):
            os.remove(dst)
        logging.info('try symlink %s -> %s' % (src, dst))
        os.symlink(src, dst)

    def start(self, timeout=180):
        if self._process:
            logging.warn('local saas already started')
            return

        t_log_path = os.path.join(self._cwd, 'local_saas')

        rtyserver_test_src = common.build_path(os.path.join('saas', 'rtyserver_test', 'local_saas', 'sandbox-data', 'rtyserver_test'))
        rtyserver_test_dst = os.path.join(self._cwd, 'rtyserver_test')
        self.link_binary(rtyserver_test_src, rtyserver_test_dst)

        logging.info('rtyserver_test is ready: %s' % rtyserver_test_dst)
        cluster_cfg_path = common.source_path(os.path.join('saas', 'rtyserver_test', 'func', 'configs', 'cluster', self._cluster_config))
        cmd = [rtyserver_test_dst, '-t', 'SuspendIdle', '-g', cluster_cfg_path, '-k', 'notset']
        if self._config_patch is not None:
            cmd += ['-P', json.dumps(self._config_patch)]
        if self._service_configs_dir:
            cmd += ['-d', self._service_configs_dir]

        env = dict(os.environ)
        port_st = 3000 + int(5000 * random.random())
        env['RTY_TESTS_START_PORT'] = str(port_st)
        env['RTY_TESTS_USE_LOCALHOST'] = 'true'
        logging.info('start_port=%s' % (port_st))

        self._process = common.process.execute(cmd, env=env, cwd=self._cwd,
                                               wait=False, check_exit_code=False,
                                               stdout=open(t_log_path + '.out', 'w'),
                                               stderr=open(t_log_path + '.err', 'w'))
        for i in range(timeout):
            self._read_ports(t_log_path + '.out')
            if self._search_port != 0:
                break
            time.sleep(1)
            if not self._process.running:
                logging.error('cluster failed, exit_code=%s, check_num=%s' % (self._process.exit_code, i))
            assert self._process.running

    def stop(self):
        if self._process and self._process.running:
            self._process.kill()
        self._process = None

    @property
    def search_port(self):
        return self._search_port

    @property
    def indexer_port(self):
        return self._indexer_port

    @property
    def service_name(self):
        return 'tests'

    def get_service_hash(self, format):
        r = requests.get('http://localhost:{port}/?info_server=yes'.format(port=self.indexer_port))
        r.raise_for_status()
        data = r.json()
        for adapter in data["config"]["Proxy"][0]["Adapter"]:
            if adapter["Name"] == format:
                return hashlib.md5(six.ensure_binary(self.service_name + ' ' + adapter["SecretCode"])).hexdigest()
        raise Exception('unknown indexing format: %s' % format)
