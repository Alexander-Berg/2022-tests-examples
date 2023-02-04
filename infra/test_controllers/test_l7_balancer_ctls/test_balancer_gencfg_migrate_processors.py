import inject
import logging
import pytest
import mock

from awacs.lib.order_processor.model import FeedbackMessage
from awacs.resolver.gencfg.client import IGencfgClient

from awacs.model.balancer.operations.migrate_from_gencfg_to_yp_lite import (
    GencfgMigration,
    Start,
    DeterminingOrthogonalTags,
    FillingRemainingTags,
    DeterminingVirtualServices,
    CreatingBalancer,
    Cancelling)
from infra.awacs.proto import model_pb2
from awtest import wait_until, wait_until_passes
from awtest.api import Api


NS_ID = u'namespace-id'
BALANCER_ID = u'balancer-id_sas'

L7_MACRO = '''
---
l7_macro:
  version: 0.0.1
  http: {}'''

ABC_ID = 1821  # rclb
CTYPE_TAG1 = 'prod'
CTYPE_TAG2 = 'prestable'
PRJ_TAG1 = 'prj1'
PRJ_TAG2 = 'prj2'
SYSTEM_PRJ_TAG = 'cplb'
VS1 = 'sas.yandex.net'
VS2 = 'man.yandex.net'


class MockGencfgClient(IGencfgClient):
    def do_list_group_instances_data(self, name, version):
        if name == 'SAS_BALANCER':
            return [{'dc': 'sas', 'tags': ['a_ctype_{}'.format(CTYPE_TAG1),
                                           'a_prj_{}'.format(PRJ_TAG1)]}]
        if name == 'MAN_BALANCER':
            return [{'dc': 'man', 'tags': ['a_ctype_{}'.format(CTYPE_TAG1),
                                           'a_prj_{}'.format(PRJ_TAG2),
                                           'a_prj_{}'.format(SYSTEM_PRJ_TAG)]}]
        return []

    def get_group_card(self, name, release):
        rv = mock.Mock()
        if name == 'SAS_BALANCER':
            rv.virtual_services = [VS1]
        elif name == 'MAN_BALANCER':
            rv.virtual_services = [VS2]
        else:
            rv.virtual_services = []
        return rv


@pytest.fixture(autouse=True)
def deps(binder_with_nanny_client, caplog):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        binder_with_nanny_client(b)
        b.bind(IGencfgClient, MockGencfgClient())

    inject.clear_and_configure(configure)
    yield
    inject.clear()


def create_gencfg_balancer():
    Api.create_namespace(NS_ID)
    b_pb = model_pb2.BalancerSpec()
    b_pb.config_transport.nanny_static_file.service_id = 'gencfg_groups_SAS_BALANCER'
    b_pb.type = model_pb2.YANDEX_BALANCER
    b_pb.yandex_balancer.yaml = L7_MACRO
    b_pb.yandex_balancer.mode = b_pb.yandex_balancer.EASY_MODE
    return Api.create_balancer(namespace_id=NS_ID, balancer_id=BALANCER_ID, spec_pb=b_pb)


def create_gencfg_migration(cache, zk_storage, starting_state, context=None, location='SAS', flb_ctype='', flb_prj=''):
    b_pb = model_pb2.BalancerOperation()
    b_pb.meta.id = BALANCER_ID
    b_pb.meta.namespace_id = NS_ID
    b_pb.order.content.migrate_from_gencfg_to_yp_lite.SetInParent()
    b_pb.order.content.migrate_from_gencfg_to_yp_lite.abc_service_id = ABC_ID
    b_pb.order.content.migrate_from_gencfg_to_yp_lite.new_balancer_id = 'new_balancer'
    b_pb.order.content.migrate_from_gencfg_to_yp_lite.fallback.ctype = flb_ctype
    b_pb.order.content.migrate_from_gencfg_to_yp_lite.fallback.prj = flb_prj
    allocation_request_pb = b_pb.order.content.migrate_from_gencfg_to_yp_lite.allocation_request
    allocation_request_pb.nanny_service_id_slug = 'not-used-yet'
    allocation_request_pb.network_macro = '_SEARCHSAND_'
    allocation_request_pb.preset.type = allocation_request_pb.preset.MICRO
    allocation_request_pb.preset.instances_count = 3
    allocation_request_pb.location = location.upper()

    b_pb.spec.incomplete = True
    b_pb.order.progress.state.id = starting_state
    zk_storage.create_balancer_operation(namespace_id=NS_ID,
                                         balancer_id=BALANCER_ID,
                                         balancer_operation_pb=b_pb)
    wait_until_passes(lambda: cache.must_get_balancer_operation(NS_ID, BALANCER_ID), timeout=1)
    migration = GencfgMigration(b_pb)
    if context:
        migration.context = context
    return migration


def make_group(name):
    return {'name': name, 'release': 'trunk'}


def update_balancer_op(cache, zk_storage, b_pb, check):
    for pb in zk_storage.update_balancer_operation(NS_ID, BALANCER_ID):
        pb.CopyFrom(b_pb)
    wait_balancer_op(cache, check)


def wait_balancer_op(cache, check):
    assert wait_until(lambda: check(cache.must_get_balancer_operation(NS_ID, BALANCER_ID)), timeout=1)


def test_started(cache, zk_storage, ctx):
    create_gencfg_balancer()
    migration = create_gencfg_migration(cache, zk_storage, 'START')
    assert Start(migration).process(ctx).name == 'DETERMINING_ORTHOGONAL_TAGS'


@pytest.mark.parametrize('gencfg_group_names,old_orthogonal_tags,location,'
                         'expected_ctype,expected_prj,expected_candidates', [
                             (['SAS_BALANCER'], {}, 'SAS', CTYPE_TAG1, PRJ_TAG1, {}),
                             (['MAN_BALANCER'], {}, 'MAN', CTYPE_TAG1, PRJ_TAG2, {}),
                             (['SAS_BALANCER'], {}, 'MAN', CTYPE_TAG1, PRJ_TAG1, {}),
                             (['SAS_BALANCER'], {'ctype': CTYPE_TAG2, 'prj': PRJ_TAG1}, 'SAS', CTYPE_TAG2, PRJ_TAG1, {}),
                             (['SAS_BALANCER', 'MAN_BALANCER'], {}, 'SAS', CTYPE_TAG1, PRJ_TAG1, {}),
                             (['SAS_BALANCER', 'MAN_BALANCER'], {}, 'MAN', CTYPE_TAG1, PRJ_TAG2, {}),
                             (['SAS_BALANCER', 'MAN_BALANCER'], {}, 'VLA', CTYPE_TAG1, None, {'prj': {PRJ_TAG1, PRJ_TAG2}}),
                         ])
def test_determining_orthogonal_tags(cache, zk_storage, ctx, gencfg_group_names, old_orthogonal_tags,
                                     expected_ctype, expected_prj, expected_candidates, location):
    create_gencfg_balancer()
    context = {'gencfg_groups': [make_group(name) for name in gencfg_group_names], 'old_orthogonal_tags': old_orthogonal_tags}
    migration = create_gencfg_migration(cache, zk_storage, 'DETERMINING_ORTHOGONAL_TAGS', context=context, location=location)
    processor = DeterminingOrthogonalTags(migration)
    assert processor.process(ctx).name == ('FILLING_REMAINING_TAGS' if expected_candidates else 'DETERMINING_VIRTUAL_SERVICES')
    assert processor.operation.context.get('ctype') == expected_ctype
    assert processor.operation.context.get('prj') == expected_prj
    descriptions = {}
    for candidate in expected_candidates:
        descriptions[candidate] = ('Multiple tags for {} are found: ("{}"), please choose which one to '
                                   'use'.format(candidate, '", "'.join(expected_candidates[candidate])))
    assert processor.operation.context.get('instance_tags_descriptions', {}) == descriptions


@pytest.mark.parametrize('ctype,prj,flb_ctype,flb_prj,expected_state', [
    (CTYPE_TAG1, PRJ_TAG1, '', '', 'DETERMINING_VIRTUAL_SERVICES'),
    (None, PRJ_TAG1, CTYPE_TAG1, '', 'DETERMINING_VIRTUAL_SERVICES'),
    (None, PRJ_TAG1, CTYPE_TAG1, PRJ_TAG2, 'DETERMINING_VIRTUAL_SERVICES'),
    (None, None, '', '', 'FILLING_REMAINING_TAGS'),
    (None, PRJ_TAG1, '', '', 'FILLING_REMAINING_TAGS'),
])
def test_filling_remaining_tags(cache, zk_storage, ctx, ctype, prj, flb_ctype, flb_prj, expected_state):
    create_gencfg_balancer()
    context = {}
    if ctype:
        context['ctype'] = ctype
    if prj:
        context['prj'] = prj
    migration = create_gencfg_migration(cache, zk_storage, 'FILLING_REMAINING_TAGS', context=context,
                                        flb_ctype=flb_ctype, flb_prj=flb_prj)
    processor = FillingRemainingTags(migration)
    assert processor.process(ctx).name == expected_state
    if expected_state == 'DETERMINING_VIRTUAL_SERVICES':
        assert processor.operation.context.get('ctype', '') == ctype or flb_ctype
        assert processor.operation.context.get('prj', '') == prj or flb_prj


@pytest.mark.parametrize('ctype,prj', [
    (None, None),
    (None, PRJ_TAG1),
    (CTYPE_TAG1, None),
])
def test_filling_remaining_tags_error(cache, zk_storage, ctx, ctype, prj):
    create_gencfg_balancer()
    context = {}
    if ctype:
        context['ctype'] = ctype
    if prj:
        context['prj'] = prj
    migration = create_gencfg_migration(cache, zk_storage, 'FILLING_REMAINING_TAGS', context=context)
    migration.context['instance_tags_candidates'] = {
        'ctype': ['c1', 'c2'],
        'prj': ['p1', 'p2'],
    }
    migration.context['instance_tags_descriptions'] = {
        'ctype': 'Multiple tags for ctype are found: ("c1", "c2"), please choose which one to use',
        'prj': 'Multiple tags for prj are found: ("p1", "p2"), please choose which one to use',
    }
    rv = FillingRemainingTags(migration).process(ctx)
    assert isinstance(rv, FeedbackMessage)
    if not ctype:
        assert rv.content['cannot_infer_tags_error'].ctype_candidates == ['c1', 'c2']
        assert rv.content['cannot_infer_tags_error'].prj_candidates == ['p1', 'p2']
        assert rv.pb_error_type == model_pb2.BalancerOperationOrder.OrderFeedback.CANNOT_INFER_TAGS_ERROR


@pytest.mark.parametrize('gencfg_group_names,expected_virtual_service_ids', [
    (['SAS_BALANCER'], {VS1}),
    (['MAN_BALANCER'], {VS2}),
    (['VLA_BALANCER'], set()),
    (['SAS_BALANCER', 'MAN_BALANCER', 'VLA_BALANCER'], {VS1, VS2}),
])
def test_determining_virtual_services(cache, zk_storage, ctx, gencfg_group_names, expected_virtual_service_ids):
    create_gencfg_balancer()
    context = {'gencfg_groups': [make_group(name) for name in gencfg_group_names]}
    migration = create_gencfg_migration(cache, zk_storage, 'DETERMINING_VIRTUAL_SERVICES', context=context)
    processor = DeterminingVirtualServices(migration)
    assert processor.process(ctx).name == 'CREATING_BALANCER'
    assert processor.operation.context['virtual_service_ids'] == expected_virtual_service_ids


@pytest.mark.parametrize('ctype,prj,virtual_service_ids,location', [
    (CTYPE_TAG1, PRJ_TAG2, {VS1, VS2}, 'SAS'),
    (CTYPE_TAG2, PRJ_TAG1, {}, 'MAN'),
])
def test_creating_balancer(cache, zk_storage, ctx, ctype, prj, virtual_service_ids, location):
    old_balancer_pb = create_gencfg_balancer()
    context = {'ctype': ctype, 'prj': prj, 'virtual_service_ids': virtual_service_ids}
    migration = create_gencfg_migration(cache, zk_storage, 'CREATING_BALANCER', context=context, location=location)
    processor = CreatingBalancer(migration)
    assert processor.process(ctx).name == 'CREATING_BALANCER'

    new_balancer_id = processor.operation.pb.order.content.migrate_from_gencfg_to_yp_lite.new_balancer_id
    new_balancer_pb = zk_storage.must_get_balancer(NS_ID, new_balancer_id)
    meta_pb = new_balancer_pb.meta
    assert meta_pb.location.type == meta_pb.location.YP_CLUSTER
    assert meta_pb.location.yp_cluster == location
    order_content_pb = new_balancer_pb.order.content
    assert order_content_pb.instance_tags.metaprj == 'yp'
    assert order_content_pb.instance_tags.ctype == ctype
    assert order_content_pb.instance_tags.prj == prj
    assert order_content_pb.allocation_request.network_macro == '_SEARCHSAND_'
    assert order_content_pb.allocation_request.preset.instances_count == 3
    assert order_content_pb.allocation_request.virtual_service_ids == sorted(virtual_service_ids)
    assert order_content_pb.abc_service_id == ABC_ID
    assert order_content_pb.activate_balancer
    assert len(meta_pb.indices) == 1
    assert meta_pb.indices[0].ctime == meta_pb.mtime
    assert meta_pb.indices[0].id == meta_pb.version
    assert meta_pb.indices[0].id != old_balancer_pb.meta.indices[0].id
    assert meta_pb.indices[0].id != old_balancer_pb.meta.indices[0].id
    assert meta_pb.indices[0].included_backend_ids == old_balancer_pb.meta.indices[0].included_backend_ids

    new_balancer_pb.spec.incomplete = False
    new_balancer_pb.meta.generation += 1
    processor.operation.dao.cache._set_balancer_pb(NS_ID + '/' + new_balancer_id, new_balancer_pb)
    assert processor.process(ctx).name == 'FINISHED'
    assert not migration.pb.spec.incomplete


def test_cancelling(ctx, cache, zk_storage):
    create_gencfg_balancer()
    migration = create_gencfg_migration(cache, zk_storage, 'CANCELLING')
    assert Cancelling(migration).process(ctx).name == 'CANCELLED'
    assert not migration.pb.spec.incomplete
