import copy

import ipaddress  # noqa
import six
from datetime import datetime, timedelta
from typing import Dict, Any

from awacs.lib.l3mgrclient import L3MgrException, L3MgrClient
from awtest.mocks import AwtestMockMeta, AwtestMockMixin


class L3MgrMockClient(six.with_metaclass(AwtestMockMeta, L3MgrClient, AwtestMockMixin)):
    datetime_str = u'%Y-%m-%dT%H:%M:%S.%fZ'

    def __init__(self, *args, **kwargs):  # noqa
        AwtestMockMixin.__init__(self)
        self.config = None  # type: Dict[six.text_type, Any]  # noqa
        self._configs = {}
        self._config_id = None
        self.vs = []
        self._all_vs = {}
        self.timestamp = None
        self.abc_slug = 'rclb'

        self.ipv4 = ipaddress.IPv4Address(u'127.0.0.0')
        self.ipv6 = ipaddress.IPv6Address(u'::0')

    def _update_config(self):
        self._config_id += 1
        self.config[u'state'] = u'NEW'
        self.timestamp += timedelta(minutes=5)
        self.config[u'timestamp'] = self.timestamp.strftime(self.datetime_str)
        self.config[u'id'] = self._config_id
        self.vs = [self._all_vs.get(vs_id) for vs_id in self.config[u'vs_id']]
        self._configs[self._config_id] = copy.deepcopy(self.config)

    def awtest_set_default_config(self):
        self.config = copy.deepcopy(DEFAULT_CONFIG)
        self._config_id = self.config[u'id']
        self._configs[self._config_id] = copy.deepcopy(self.config)
        self.vs = [copy.deepcopy(DEFAULT_VS)]
        self._all_vs[self.vs[0][u'id']] = self.vs[0]
        self.timestamp = datetime.strptime(self.config[u'timestamp'], self.datetime_str)

    def awtest_activate_config(self, config_id):
        self.config = self._configs[int(config_id)]
        self.config[u'state'] = u'ACTIVE'
        self._config_id = self.config[u'id']
        self.vs = [self._all_vs[vs_id] for vs_id in self.config[u'vs_id']]
        self.timestamp = datetime.strptime(self.config[u'timestamp'], self.datetime_str)

    def awtest_set_config_state(self, config_id, state):
        self._configs[int(config_id)][u'state'] = state
        if self.config[u'id'] == config_id:
            self.config[u'state'] = state

    def awtest_add_vs(self, vs):
        while vs[u'id'] in self._all_vs:
            vs[u'id'] += 1
        self._all_vs[vs[u'id']] = vs
        self.config[u'vs_id'].append(vs[u'id'])
        self.vs.append(vs)

    @staticmethod
    def list_services_by_fqdn(*args, **kwargs):
        return []

    def get_service(self, service_id, request_timeout=None):
        return {
            u'abc': self.abc_slug,
            u'fqdn': u'l3mgr-awacs-test.yandex-team.ru',
            u'config': self.config or {},
            u'vs': self.vs or [],
            u'id': u'999',
        }

    def get_vs(self, _, vs_id, request_timeout=None):
        return self._all_vs.get(vs_id, copy.deepcopy(DEFAULT_VS))

    def create_service(self, fqdn, abc_code, data=None):
        return {u'object': self.get_service(735)}

    def list_roles(self, *args, **kwargs):
        return {u'objects': [{u'abc': self.abc_slug, u'permission': u'l3mgr.editrs_service'}]}

    @staticmethod
    def add_role(*args, **kwargs):
        pass

    def get_new_ip(self, abc_code, v4=False, external=False, fqdn=None):
        if v4:
            self.ipv4 += 1
            ip = self.ipv4
        else:
            self.ipv6 += 1
            ip = self.ipv6
        return {u'object': six.text_type(ip)}

    def create_virtual_server(self, svc_id, ip, port, protocol, config=None, rs=None, groups=None):
        vs = copy.deepcopy(DEFAULT_VS)
        vs[u'ip'] = ip
        vs[u'port'] = port
        vs[u'protocol'] = protocol
        vs[u'config'] = config
        vs[u'id'] = max(self._all_vs.keys() or [0]) + 1
        if groups:
            vs[u'group'] = groups
        self._all_vs[vs[u'id']] = vs
        return {u'object': {u'id': vs[u'id']}}

    def _check_config_id(self, use_etag, latest_cfg_id):
        if use_etag and latest_cfg_id is None:
            raise RuntimeError(u'latest_cfg_id must be set if use_etag=True')
        if latest_cfg_id is not None and latest_cfg_id != sorted(self._configs)[-1]:
            raise L3MgrException(u'412: Precondition failed: {} != {}'.format(latest_cfg_id, self.config[u'id']))

    def create_config_with_vs(self, svc_id, vs_ids, comment, use_etag, latest_cfg_id=None):
        self._check_config_id(use_etag, latest_cfg_id)
        if self.config is None:
            self.awtest_set_default_config()
        self.config[u'vs_id'] = vs_ids
        self._update_config()
        return {
            u'result': u'OK',
            u'object': {u'id': self._config_id},
        }

    def create_config_with_rs(self, svc_id, groups, use_etag, latest_cfg_id=None):
        self._check_config_id(use_etag, latest_cfg_id)
        if self.config is None:
            self.awtest_set_default_config()
        self._update_config()
        for vs in self.vs:
            vs[u'group'] = groups
        return {
            u'result': u'OK',
            u'object': {u'id': self._config_id},
        }

    def process_config(self, svc_id, cfg_id, use_etag, latest_cfg_id=None, force=False):
        self._check_config_id(use_etag, latest_cfg_id)
        self.config[u'state'] = u'TESTING'
        return {u'result': u'OK'}

    def list_grants(self, *args, **kwargs):
        return {u'objects': [self.abc_slug]}

    @staticmethod
    def set_grants(*args, **kwargs):
        pass

    def get_config(self, service_id, config_id):
        return self._configs[int(config_id)]

    def get_latest_config(self, service_id):
        return self.config

    def update_meta(self, svc_id, data):
        return

    def get_abc_service_info(self, abc_service_slug):
        return {u'lb': [{}, {}]}


DEFAULT_CONFIG = {
    'comment': '...',
    'service': {
        'abc': 'SR',
        'url': '/api/v1/service/735',
        'fqdn': 'l3mgr-awacs-test.yandex-team.ru',
        'archive': False,
        'state': 'ACTIVE',
        'id': 735
    },
    'vs_id': [0],
    'url': '/api/v1/service/735/config/xxx',
    'timestamp': u'2018-02-02T06:32:06.846Z',
    'description': '...',
    'state': u'NEW',
    'id': 0,
}

DEFAULT_VS = {
    'status': [
        {
            'timestamp': '2018-04-13T08:24:48.039Z', 'state': 'ACTIVE',
            'lb': {
                'full': False, 'name': 'myt-lb24', 'url': '/api/v1/balancer/242',
                'fqdn': 'myt-lb24.yndx.net', 'state': 'ACTIVE', 'location': ['IVA', 'MYT'],
                'test_env': False, 'id': 242
            },
            'description': 'Updated at 2018-04-13 08:24:44.837974+00:00'
        },
        {
            'timestamp': '2018-04-13T08:24:38.119Z', 'state': 'ACTIVE',
            'lb': {
                'full': False, 'name': 'sas1-lb26', 'url': '/api/v1/balancer/67',
                'fqdn': 'sas1-lb26.yndx.net', 'state': 'ACTIVE', 'location': ['SAS'],
                'test_env': False,
                'id': 67
            },
            'description': 'Updated at 2018-04-13 08:24:34.419381+00:00'
        }
    ],
    'lb': [],
    'ext_id': '3b3038081cbc9b4b54aa4fdee4dd8a75d5932981e2c63b8a976a6a1457bce9a5',
    'url': '/api/v1/service/735/vs/4063',
    'total_count': 2,
    'editable': False,
    'active_count': 0,
    'id': 0,
    'ip': '2a02:6b8:0:3400:0:2da:0:2',
    'protocol': 'TCP',
    'group': ['...'],
    'config': {
        "OPS": False,
        "HOST": None,
        "DIGEST": None,
        "METHOD": "TUN",
        "QUORUM": 1,
        "MH_PORT": False,
        "ANNOUNCE": True,
        "CHECK_URL": "/ping",
        "DC_FILTER": True,
        "SCHEDULER": "wrr",
        "CHECK_TYPE": "HTTP_GET",
        "CONNECT_IP": "2a02:6b8:0:3400:0:2da:0:2",
        "DELAY_LOOP": 10,
        "HYSTERESIS": 0,
        "CHECK_RETRY": 1,
        "MH_FALLBACK": False,
        "STATUS_CODE": 200,
        "TESTING_LBS": None,
        "CONNECT_PORT": 80,
        "CHECK_TIMEOUT": 1,
        "DYNAMICWEIGHT": True,
        "HTTP_PROTOCOL": None,
        "INHIBIT_ON_FAILURE": False,
        "PERSISTENCE_TIMEOUT": None,
        "CHECK_RETRY_TIMEOUT": 1,
        "DYNAMICWEIGHT_RATIO": 30,
        "DYNAMICWEIGHT_IN_HEADER": False,
        "DYNAMICWEIGHT_ALLOW_ZERO": True
    },
    'port': 80
}
