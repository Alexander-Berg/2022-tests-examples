import pytest

from awacs.model.l7heavy_config.order import processors as p
from awacs.model import objects
from awacs.lib.l7heavy_client import IL7HeavyClient
from awtest import wait_until_passes, wait_until
from infra.awacs.proto import model_pb2
from .conftest import NS_ID

from awacs.model.util import WEIGHTS_KNOB_ID


@pytest.fixture
def prepare_namespace(zk_storage, cache):
    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = NS_ID
    ns_pb.spec.its.knobs.common_knobs.add(its_ruchka_id=WEIGHTS_KNOB_ID)
    ns_pb.spec.balancer_constraints.instance_tags.prj = NS_ID
    zk_storage.create_namespace(NS_ID, ns_pb)
    wait_until_passes(lambda: cache.must_get_namespace(NS_ID))


def test_start(ctx, prepare_namespace, l7heavy_config_order):
    assert p.Started(l7heavy_config_order).process(ctx).name == u'CREATING_ITS_KNOB'


def test_creating_l7heavy_dashboard(ctx, prepare_namespace, l7heavy_config_order):
    assert p.CreatingL7HeavyDashboard(l7heavy_config_order).process(ctx).name == u'PUSH_EMPTY_CONFIG_TO_ITS'
    assert l7heavy_config_order.context['version']
    assert IL7HeavyClient.instance().configs[NS_ID]['group_id'] == 'other'


def test_push_empty_config_to_its(ctx, prepare_namespace, l7heavy_config_order):
    c = IL7HeavyClient.instance()
    l7heavy_config_order.context['version'] = c.versions[NS_ID]

    assert c.its_values[NS_ID] is None
    assert p.PushEmptyConfigToIts(l7heavy_config_order).process(ctx).name == u'CREATING_WEIGHT_SECTIONS'
    assert c.its_values[NS_ID] == c.versions[NS_ID]


def test_creating_weight_sections(ctx, prepare_namespace, l7heavy_config_order):
    assert p.CreatingWeightSections(l7heavy_config_order).process(ctx).name == u'SAVING_SPEC'


def test_saving_spec(ctx, prepare_namespace, l7heavy_config_order):
    assert objects.L7HeavyConfig.zk.must_get(NS_ID, NS_ID).spec.incomplete

    assert p.SavingSpec(l7heavy_config_order).process(ctx).name == u'FINISHED'
    wait_until(lambda: not objects.L7HeavyConfig.zk.must_get(NS_ID, NS_ID).spec.incomplete)


def test_cancelling(ctx, prepare_namespace, l7heavy_config_order):
    assert p.Cancelling(l7heavy_config_order).process(ctx).name == u'CANCELLED'
    wait_until(lambda: not objects.L7HeavyConfig.cache.must_get(NS_ID, NS_ID).spec.incomplete)
