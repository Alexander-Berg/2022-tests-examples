import copy
from six.moves import http_client as httplib

import mock

from awacs.lib import nannyclient
from awacs.model.errors import NannyApiError


MOCK_SERVICE = {
    'info_attrs': {
        'content': {'type': 'AWACS_BALANCER'},
        '_id': 'xxx'
    },
    'runtime_attrs': {
        'content': {'engines': {'engine_type': 'ISS_MULTI'}}
    }
}


class NannyMockClient(object):
    def __init__(self, *_, **__):
        self.last_runtime_attrs = {}
        self.remove_service_called = set()
        self.shutdown_service_called = set()
        self.remove_service_snapshot_calls = {}
        self.last_update_dashboard_contents = {}
        self.set_snapshot_state_calls = {}

    def get_service_runtime_attrs(self, service_id, *_, **__):
        if service_id in self.last_runtime_attrs:
            return self.last_runtime_attrs[service_id]
        rv = {
            'content': {
                'instance_spec': {
                    'instancectl': {
                        'version': '99.99'
                    },
                    'containers': [],
                    'layersConfig': {
                        'layer': [],
                    },
                },
                'engines': {
                    'engine_type': 'ISS_MULTI',
                },
                'instances': {},
                'resources': {
                    'url_files': [],
                    'static_files': [],
                    'sandbox_files': []
                }
            },
            '_id': 'aaaa',
        }
        if service_id.startswith('gencfg_groups_'):
            gencfg_group_names = service_id[len('gencfg_groups_'):].upper().split('-')
            rv['content']['instances'] = {
                'extended_gencfg_groups': {
                    'groups': [{'name': name, 'release': 'trunk'} for name in gencfg_group_names],
                }
            }
        return rv

    def update_runtime_attrs_content(self, service_id, snapshot_id, snapshot_priority, runtime_attrs_content,
                                     comment=None):
        _id = snapshot_id + 'a'
        self.last_runtime_attrs[service_id] = {
            'content': runtime_attrs_content,
            '_id': _id
        }
        rv = {
            '_id': _id,
            'change_info': {
                'ctime': 1234567890
            }
        }
        return rv

    def update_info_attrs_content(self, service_id, snapshot_id, info_attrs_content, comment=None):
        pass

    def update_service(self, service_id, runtime_attrs, info_attrs, comment):
        runtime_attrs = self.update_runtime_attrs_content(service_id, runtime_attrs['_id'], None,
                                                          runtime_attrs['content'], comment)
        return {'runtime_attrs': runtime_attrs}

    @staticmethod
    def get_service_auth_attrs(service_id):
        if service_id.startswith('rtc_balancer_not_existing_'):
            response = mock.MagicMock(status_code=httplib.NOT_FOUND)
            raise nannyclient.NannyApiRequestException(response=response)
        return {'content': {}}

    @staticmethod
    def get_service_info_attrs(*_, **__):
        return {'content': {}}

    @staticmethod
    def create_service(*_, **__):
        return None

    def get_service(self, service_id):
        service = copy.deepcopy(MOCK_SERVICE)
        if not service_id.startswith('from_nanny_service'):
            service['runtime_attrs'] = self.get_service_runtime_attrs(service_id)
            return service
        if 'awacs' not in service_id:
            del service['info_attrs']['content']['type']
        if 'yp' in service_id:
            service['runtime_attrs']['content']['engines']['engine_type'] = 'YP_LITE'
        for yp_cluster in ['sas', 'man', 'vla']:
            if yp_cluster in service_id:
                service['info_attrs']['content']['yp_cluster'] = yp_cluster.upper()
        return service

    @staticmethod
    def get_current_runtime_attrs_id_and_ctime(*_, **__):
        return 'z', 1

    @staticmethod
    def get_target_runtime_attrs_id(*_, **__):
        raise NannyApiError

    @staticmethod
    def get_current_runtime_attrs_id(*_, **__):
        return u'z'

    def set_snapshot_state(self, service_id, snapshot_id, state, comment, recipe='default'):
        self.set_snapshot_state_calls[(service_id, snapshot_id)] = state

    @staticmethod
    def get_service_state(service_id):
        if service_id == 'active':
            return {'current_state': {'content': {'active_snapshots': [{'state': 'ACTIVE', 'snapshot_id': 'xxx'}]}}}
        return {'current_state': {'content': {'active_snapshots': []}}}

    @staticmethod
    def list_service_dashboards(service_id):
        return {'value': [{'id': 'dashboard${}'.format(service_id)}]}

    @staticmethod
    def get_dashboard(dashboard_id):
        service_ids = dashboard_id.split('$')[1:]
        return {'content': {
            'groups': [{'services': [{'service_id': s_id} for s_id in service_ids]}]
        }}

    def remove_service(self, service_id, *_, **__):
        self.remove_service_called.add(service_id)

    def shutdown_service(self, service_id, *_, **__):
        self.shutdown_service_called.add(service_id)

    def update_dashboard(self, dashboard_id, content, comment):
        self.last_update_dashboard_contents[dashboard_id] = content

    def remove_service_snapshot(self, service_id, snapshot_id, comment):
        self.remove_service_snapshot_calls[service_id] = snapshot_id
