# coding: utf-8
import inject
import mock
import pytest
from sepelib.core import config as appconfig
from infra.swatlib import metrics
from infra.orly.proto import orly_pb2

from infra.swatlib.orly_client import IOrlyClient, OrlyClient, OrlyBrake, OperationOutcome, OrlyBrakeApplied


@pytest.fixture
def lapapam_enabled_orly_brake():
    appconfig.set_value('run.enabled_orly_brake_rules', ['lapapam'])
    yield
    appconfig.set_value('run.enabled_orly_brake_rules', [])


@pytest.fixture
def production_true():
    prev = appconfig.get_value('run.production')
    appconfig.set_value('run.production', True)
    yield
    appconfig.set_value('run.production', prev)


@pytest.fixture
def production_false():
    prev = appconfig.get_value('run.production')
    appconfig.set_value('run.production', False)
    yield
    appconfig.set_value('run.production', prev)


@pytest.fixture
def orly_client():
    yield OrlyClient(url='qwerty')


@pytest.fixture(scope='function', autouse=True)
def deps(orly_client):
    def configure(b):
        b.bind(IOrlyClient, orly_client)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


def test_orly_client(orly_client):
    with mock.patch.object(orly_client._client, 'start_operation',
                           return_value=(orly_pb2.OperationResponse(), None)) as start_operation_stub:
        orly_client.start_operation(rule='push', id_='qwerty', labels=[('x', '1'), ('y', '2')])
    start_operation_stub.assert_called_once()

    for call in start_operation_stub.mock_calls:
        _, args, kwargs = call
        expected_op_req_pb = orly_pb2.OperationRequest(rule='push', id='qwerty')
        expected_op_req_pb.labels['x'] = '1'
        expected_op_req_pb.labels['y'] = '2'
        expected_args = (expected_op_req_pb,)
        assert args == expected_args
        assert not kwargs


def test_orly_brake_disabled_if_production_false(production_false, lapapam_enabled_orly_brake):
    b = OrlyBrake(rule='lapapam', metrics_registry=metrics.ROOT_REGISTRY)
    assert not b.is_enabled()


def test_orly_brake(orly_client, production_true, lapapam_enabled_orly_brake):
    b = OrlyBrake(rule='pumpurum', metrics_registry=metrics.ROOT_REGISTRY)
    assert not b.is_enabled()

    b = OrlyBrake(rule='lapapam', metrics_registry=metrics.ROOT_REGISTRY)
    assert b.is_enabled()

    b.maybe_apply(op_id='qwerty')

    op_labels = [('test-label', 'test-value')]

    with mock.patch.object(orly_client, 'start_operation',
                           return_value=(OperationOutcome.FORBIDDEN, 'just testing')):
        with pytest.raises(OrlyBrakeApplied, match='just testing') as e:
            b.maybe_apply(op_id='qwerty', op_labels=op_labels)

    with mock.patch.object(orly_client, 'start_operation',
                           return_value=(OperationOutcome.ALLOWED, 'just testing')):
        b.maybe_apply(op_id='qwerty', op_labels=op_labels)

    with mock.patch.object(orly_client, 'start_operation',
                           return_value=(OperationOutcome.UNAVAILABLE, 'just testing')):
        b.maybe_apply(op_id='qwerty', op_labels=op_labels)
