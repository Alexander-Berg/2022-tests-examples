import collections
import logging

import inject
import pytest

from awacs.lib.l7heavy_client import IL7HeavyClient
from awacs.model.l7heavy_config.order.processors import L7HeavyConfigOrder
from awacs.model import objects, util
from awtest import wait_until
from infra.awacs.proto import model_pb2


NS_ID = u'namespace-id'
LOGIN = u'robot'


class MockL7HeavyClient(IL7HeavyClient):
    def __init__(self):
        super(MockL7HeavyClient, self).__init__()
        self.configs = {}
        self.versions = collections.defaultdict(str)
        self.sections = collections.defaultdict(list)
        self.its_values = collections.defaultdict(lambda: None)

    def create_config(self, config_id, group_id, its_value_path, logins=None, groups=None, labels=None, link=None):
        assert config_id not in self.configs
        self.configs[config_id] = {
            'id': config_id,
            'group_id': group_id,
            'its_value_path': its_value_path,
            'managers': {
                'logins': logins or [],
                'groups': groups or [],
            },
            'labels': labels or []
        }
        if link is not None:
            self.configs[config_id]['metadata'] = {'owner': util.AWACS_L7HEAVY_OWNER_NAME, 'link': link}
        self.versions[config_id] = 'a'
        cfg = self.configs[config_id]
        cfg['version'] = self.versions[config_id]
        return cfg

    def get_config(self, config_id):
        assert config_id
        return self.versions['version'], self.configs[config_id]

    def update_config(self, config_id, version, config):
        assert version == self.versions[config_id]
        self.configs[config_id] = config
        self.configs[config_id]['id'] = config_id
        self.versions[config_id] += 'a'
        return self.versions[config_id], self.configs[config_id]

    def get_config_sections(self, config_id):
        return self.versions[config_id], self.sections[config_id]

    def save_sections(self, config_id, version, sections):
        assert self.versions[config_id] == version
        self.versions[config_id] += 'a'
        self.sections[config_id] = sections
        return self.versions[config_id], sections

    def get_its_version(self, config_id):
        return self.its_values[config_id]

    def push_weights_to_its(self, config_id, target_version, current_version=''):
        if current_version:
            assert self.its_values[config_id] == current_version
        self.its_values[config_id] = target_version


@pytest.fixture(autouse=True)
def deps(binder, caplog, l3_mgr_client):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        b.bind(IL7HeavyClient, MockL7HeavyClient())
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


def wait_l7heavy_config(check):
    assert wait_until(lambda: check(objects.L7HeavyConfig.cache.get(NS_ID, NS_ID)))


def create_l7heavy_order_pb(group='other'):
    meta = model_pb2.L7HeavyConfigMeta(id=NS_ID, namespace_id=NS_ID)
    meta.mtime.GetCurrentTime()
    meta.author = LOGIN
    l7heavy_config_pb = model_pb2.L7HeavyConfig(meta=meta)
    l7heavy_config_pb.spec.incomplete = True
    l7heavy_config_pb.order.content.mode = l7heavy_config_pb.order.content.CREATE
    l7heavy_config_pb.order.content.group_id = group

    objects.L7HeavyConfig.zk.create(l7heavy_config_pb)

    wait_l7heavy_config(lambda pb: pb)
    return l7heavy_config_pb


@pytest.fixture
def l7heavy_config_order():
    return L7HeavyConfigOrder(create_l7heavy_order_pb())


def update_l7heavy_config(l7heavy_config_pb, check):
    for pb in objects.L7HeavyConfig.zk.update(NS_ID, NS_ID):
        pb.CopyFrom(l7heavy_config_pb)
    wait_l7heavy_config(check)


def create_active_l7heavy_state():
    state_pb = model_pb2.L7HeavyConfigState(l7heavy_config_id=NS_ID, namespace_id=NS_ID)
    rev_pb = state_pb.l7heavy_config.statuses.add(revision_id=u'xxx')
    rev_pb.active.status = u'True'
    objects.L7HeavyConfig.state.zk.create(state_pb)

    wait_until(lambda: objects.L7HeavyConfig.state.zk.must_get(NS_ID, NS_ID).l7heavy_config.statuses[0].active.status)
