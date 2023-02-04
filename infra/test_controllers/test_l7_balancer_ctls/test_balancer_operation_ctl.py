import inject
import logging
import pytest

from awacs.model import events
from awacs.model.balancer.operations.ctl import BalancerOperationCtl as BaseBalancerOperationOrderCtl
from infra.awacs.proto import model_pb2
from awtest import wait_until, check_log, wait_until_passes
from awtest.api import Api


NS_ID = u'namespace-id'
BALANCER_ID = u'balancer-id_sas'

L7_MACRO = '''l7_macro:
  version: 0.0.1
  http: {}'''


class BalancerOperationCtl(BaseBalancerOperationOrderCtl):
    PROCESSING_INTERVAL = 0


@pytest.fixture(autouse=True)
def deps(binder_with_nanny_client, caplog):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        binder_with_nanny_client(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


@pytest.fixture
def ctl(cache, zk_storage, balancer_op_pb):
    ctl = BalancerOperationCtl(NS_ID, BALANCER_ID)
    ctl._pb = balancer_op_pb
    return ctl


@pytest.fixture
def balancer_pb():
    Api.create_namespace(NS_ID)
    b_pb = model_pb2.BalancerSpec()
    b_pb.incomplete = False
    b_pb.config_transport.nanny_static_file.service_id = 'qs_balancer'
    b_pb.type = model_pb2.YANDEX_BALANCER
    b_pb.yandex_balancer.yaml = L7_MACRO
    b_pb.yandex_balancer.mode = b_pb.yandex_balancer.EASY_MODE
    Api.create_balancer(namespace_id=NS_ID, balancer_id=BALANCER_ID, spec_pb=b_pb)
    return b_pb


@pytest.fixture
def balancer_op_pb(cache, zk_storage):
    b_pb = model_pb2.BalancerOperation()
    b_pb.meta.id = BALANCER_ID
    b_pb.meta.namespace_id = NS_ID
    b_pb.order.content.create_system_backend.SetInParent()
    b_pb.spec.incomplete = True
    zk_storage.create_balancer_operation(namespace_id=NS_ID,
                                         balancer_id=BALANCER_ID,
                                         balancer_operation_pb=b_pb)
    wait_until_passes(lambda: cache.must_get_balancer_operation(NS_ID, BALANCER_ID), timeout=1)
    return b_pb


def update_balancer_op(cache, zk_storage, b_pb, check):
    for pb in zk_storage.update_balancer_operation(NS_ID, BALANCER_ID):
        pb.CopyFrom(b_pb)
    wait_balancer_op(cache, check)


def wait_balancer_op(cache, check):
    assert wait_until(lambda: check(cache.must_get_balancer_operation(NS_ID, BALANCER_ID)), timeout=1)
    return cache.must_get_balancer_operation(NS_ID, BALANCER_ID)


def test_old_event_generation(caplog, ctx, ctl):
    event = events.BalancerOperationUpdate(path='', pb=ctl._pb)
    event.pb.meta.generation = -1
    with check_log(caplog) as log:
        ctl._process(ctx, event)
        assert 'Skipped event with stale generation -1' in log.records_text()
        assert 'Assigned initial state "START"' not in log.records_text()


def test_not_started(caplog, ctx, ctl, balancer_op_pb):
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Assigned initial state "STARTED"' in log.records_text()


def test_finished(caplog, cache, zk_storage, ctx, ctl, balancer_op_pb):
    balancer_op_pb.order.status.status = 'FINISHED'
    update_balancer_op(cache, zk_storage, balancer_op_pb, check=lambda pb: pb.order.status.status == 'FINISHED')
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Status is already FINISHED' in log.records_text()
