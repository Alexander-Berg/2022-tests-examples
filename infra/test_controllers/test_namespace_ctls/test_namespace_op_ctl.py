import logging

import inject
import pytest
from mock import mock

from awacs.lib import l3mgrclient
from awacs.model import objects
from awacs.model.components import ComponentConfig
from awacs.model.namespace.operations.ctl import NamespaceOperationCtl
from awtest import wait_until, check_log, wait_until_passes
from infra.awacs.proto import model_pb2
from .operations_util import prepare_namespace, L3_ID, L7_ID, NS_ID, activate_l7, activate_l3


OP_ID = u'test-ns-op'


@pytest.fixture(autouse=True)
def deps(binder, caplog, l3_mgr_client):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        b.bind(l3mgrclient.IL3MgrClient, l3_mgr_client)
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


@pytest.fixture
def ctl(cache, zk_storage, namespace_op_pb):
    ctl = NamespaceOperationCtl(full_uid=(namespace_op_pb.meta.namespace_id, namespace_op_pb.meta.id), cache=cache)
    for runner in ctl.runners.values():
        runner.processing_interval = 0
    return ctl


@pytest.fixture
def namespace_op_pb(cache, zk_storage):
    ns_op_pb = model_pb2.NamespaceOperation()
    ns_op_pb.meta.id = OP_ID
    ns_op_pb.meta.namespace_id = NS_ID
    ns_op_pb.order.content.import_virtual_servers_from_l3mgr.l3_balancer_id = L3_ID
    ns_op_pb.spec.incomplete = True
    objects.NamespaceOperation.zk.create(ns_op_pb)
    wait_until_passes(lambda: objects.NamespaceOperation.cache.must_get(NS_ID, OP_ID))
    return ns_op_pb


def update_ns_op(ns_op_pb, check):
    for pb in objects.NamespaceOperation.zk.update(ns_op_pb.meta.namespace_id, ns_op_pb.meta.id):
        pb.CopyFrom(ns_op_pb)
    return wait_ns_op(check)


def wait_ns_op(check):
    assert wait_until(lambda: check(objects.NamespaceOperation.cache.must_get(NS_ID, OP_ID)))
    return objects.NamespaceOperation.cache.must_get(NS_ID, OP_ID)


def test_transitions(ctx, cache, zk_storage, caplog, l3_mgr_client, ctl, namespace_op_pb):
    l3_mgr_client.awtest_set_default_config()
    prepare_namespace(cache, zk_storage, l7_balancer_ids=[L7_ID], preactivate_l7=True)

    with check_log(caplog) as log:
        ctl.process(ctx)
        assert 'Assigned initial state "LOCKING_L3MGR_SERVICE"' in log.records_text()
    wait_ns_op(lambda pb: pb.order.progress.state.id == 'LOCKING_L3MGR_SERVICE')

    with check_log(caplog) as log:
        ctl.process(ctx)
        assert 'Current state: LOCKING_L3MGR_SERVICE' in log.records_text()
        assert 'Processed, next state: UPDATING_L7_CONTAINER_SPEC' in log.records_text()
    wait_ns_op(lambda pb: pb.order.progress.state.id == 'UPDATING_L7_CONTAINER_SPEC')

    with mock.patch.object(ComponentConfig, 'get_latest_published_version', lambda *_, **__: u'over9000'):
        with check_log(caplog) as log:
            ctl.process(ctx)
            assert 'Current state: UPDATING_L7_CONTAINER_SPEC' in log.records_text()
            assert 'Processed, next state: UPDATING_L7_CONTAINER_SPEC' in log.records_text()
    wait_ns_op(lambda pb: pb.order.progress.state.id == 'UPDATING_L7_CONTAINER_SPEC')

    activate_l7(zk_storage, cache, L7_ID)

    with mock.patch.object(ComponentConfig, 'get_latest_published_version', lambda *_, **__: u'over9000'):
        with check_log(caplog) as log:
            ctl.process(ctx)
            assert 'Current state: UPDATING_L7_CONTAINER_SPEC' in log.records_text()
            assert 'Processed, next state: UPDATING_L3_SPEC' in log.records_text()
    wait_ns_op(lambda pb: pb.order.progress.state.id == 'UPDATING_L3_SPEC')

    with check_log(caplog) as log:
        ctl.process(ctx)
        assert 'Current state: UPDATING_L3_SPEC' in log.records_text()
        assert 'Processed, next state: WAITING_FOR_L3_ACTIVATION' in log.records_text()
    wait_ns_op(lambda pb: pb.order.progress.state.id == 'WAITING_FOR_L3_ACTIVATION')

    activate_l3(zk_storage, cache, L3_ID)

    with check_log(caplog) as log:
        ctl.process(ctx)
        assert 'Current state: WAITING_FOR_L3_ACTIVATION' in log.records_text()
        assert 'Processed, next state: FINISHED' in log.records_text()
    ns_pb = wait_ns_op(lambda pb: pb.order.progress.state.id == 'FINISHED')
    assert not ns_pb.spec.incomplete

    with check_log(caplog) as log:
        ctl.process(ctx)
        assert 'started self deletion' in log.records_text()


def test_cancelling(caplog, dao, cache, zk_storage, ctx, ctl, namespace_op_pb, checker):
    prepare_namespace(cache, zk_storage, l7_balancer_ids=[L7_ID])

    with check_log(caplog) as log:
        ctl.process(ctx)
        assert 'Assigned initial state "LOCKING_L3MGR_SERVICE"' in log.records_text()

    objects.NamespaceOperation.cancel_order(namespace_op_pb, 'test-author', 'test-comment')
    assert wait_until(lambda: objects.NamespaceOperation.cache.must_get(NS_ID, OP_ID).order.cancelled.value)

    with check_log(caplog) as log:
        ctl.process(ctx)
        assert 'Current state: LOCKING_L3MGR_SERVICE' in log.records_text()
        assert 'Processed, next state: CANCELLED' in log.records_text()
    wait_ns_op(lambda pb: pb.order.progress.state.id == 'CANCELLED')
