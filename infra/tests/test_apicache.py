# coding: utf-8
from awacs.lib.strutils import flatten_full_id as f
from awacs.model.apicache import copy_per_balancer_statuses
from infra.awacs.proto import model_pb2


def test_copy_per_balancer_statuses():
    namespace_1_id = 'namespace-1'
    namespace_2_id = 'namespace-2'
    upstream_1_id = (namespace_1_id, 'upstream-1')
    backend_1_id = (namespace_1_id, 'backend-1')
    backend_2_id = (namespace_1_id, 'backend-2')

    balancer_1_state_pb = model_pb2.BalancerState(
        namespace_id=namespace_1_id,
        balancer_id='balancer-1',
        generation=123
    )

    rev_status_pb = balancer_1_state_pb.upstreams[f(namespace_1_id, upstream_1_id)].statuses.add(revision_id='a')
    rev_status_pb.ctime.FromSeconds(1000)
    rev_status_pb.validated.status = 'True'
    rev_status_pb.in_progress.status = 'False'
    rev_status_pb.active.status = 'True'

    rev_status_pb = balancer_1_state_pb.upstreams[f(namespace_1_id, upstream_1_id)].statuses.add(revision_id='b')
    rev_status_pb.ctime.FromSeconds(2000)
    rev_status_pb.validated.status = 'False'
    rev_status_pb.validated.message = 'Vsjo ploho'
    rev_status_pb.in_progress.status = 'False'
    rev_status_pb.active.status = 'False'

    rev_status_pb = balancer_1_state_pb.backends[f(namespace_1_id, backend_1_id)].statuses.add(revision_id='a')
    rev_status_pb.ctime.FromSeconds(1000)
    rev_status_pb.validated.status = 'True'
    rev_status_pb.in_progress.status = 'False'
    rev_status_pb.active.status = 'True'

    rev_status_pb = balancer_1_state_pb.upstreams[f(namespace_1_id, backend_2_id)].statuses.add(revision_id='a')
    rev_status_pb.ctime.FromSeconds(1000)
    rev_status_pb.validated.status = 'True'
    rev_status_pb.in_progress.status = 'False'
    rev_status_pb.active.status = 'False'

    balancer_2_state_pb = model_pb2.BalancerState(
        namespace_id='namespace-1',
        balancer_id='balancer-2',
        generation=123
    )

    rev_status_pb = balancer_2_state_pb.upstreams[f(namespace_1_id, upstream_1_id)].statuses.add(revision_id='b')
    rev_status_pb.ctime.FromSeconds(2000)
    rev_status_pb.validated.status = 'True'
    rev_status_pb.in_progress.status = 'False'
    rev_status_pb.active.status = 'True'

    rev_status_pb = balancer_2_state_pb.backends[f(namespace_1_id, backend_1_id)].statuses.add(revision_id='a')
    rev_status_pb.ctime.FromSeconds(1000)
    rev_status_pb.validated.status = 'True'
    rev_status_pb.in_progress.status = 'False'
    rev_status_pb.active.status = 'True'

    rev_status_pb = balancer_2_state_pb.backends[f(namespace_1_id, backend_1_id)].statuses.add(revision_id='b')
    rev_status_pb.ctime.FromSeconds(2000)
    rev_status_pb.validated.status = 'False'
    rev_status_pb.validated.message = 'Vsjo ploho'
    rev_status_pb.in_progress.status = 'False'
    rev_status_pb.active.status = 'False'

    balancer_3_state_pb = model_pb2.BalancerState(
        namespace_id=namespace_2_id,
        balancer_id='balancer-3',
        generation=123
    )

    rev_status_pb = balancer_3_state_pb.upstreams[f(namespace_2_id, upstream_1_id)].statuses.add(revision_id='b')
    rev_status_pb.ctime.FromSeconds(2000)
    rev_status_pb.validated.status = 'True'
    rev_status_pb.in_progress.status = 'False'
    rev_status_pb.active.status = 'True'

    rev_status_pb = balancer_3_state_pb.backends[f(namespace_2_id, backend_1_id)].statuses.add(revision_id='a')
    rev_status_pb.ctime.FromSeconds(1000)
    rev_status_pb.validated.status = 'True'
    rev_status_pb.in_progress.status = 'False'
    rev_status_pb.active.status = 'True'

    rev_status_pb = balancer_3_state_pb.backends[f(namespace_2_id, backend_2_id)].statuses.add(revision_id='a')
    rev_status_pb.ctime.FromSeconds(100)
    rev_status_pb.validated.status = 'False'
    rev_status_pb.in_progress.status = 'False'
    rev_status_pb.active.status = 'False'

    upstream_pb = model_pb2.Upstream()
    upstream_pb.meta.namespace_id, upstream_pb.meta.id = upstream_1_id
    upstream_pb.meta.version = 'b'
    upstream_pb.meta.ctime.FromSeconds(1000)
    upstream_pb.meta.mtime.FromSeconds(2000)

    new_upstream_pb = copy_per_balancer_statuses(
        entity_pb=upstream_pb,
        balancer_state_pbs=[balancer_1_state_pb, balancer_2_state_pb, balancer_3_state_pb])
    assert new_upstream_pb.meta == upstream_pb.meta
    status_pb = new_upstream_pb.statuses[0]
    assert status_pb.id == 'a'
    assert status_pb.ctime.seconds == 1000
    assert status_pb.validated == {
        'namespace-1:balancer-1': model_pb2.Condition(status='True'),
    }
    assert status_pb.in_progress == {
        'namespace-1:balancer-1': model_pb2.InProgressCondition(status='False'),
    }
    assert status_pb.active == {
        'namespace-1:balancer-1': model_pb2.Condition(status='True'),
    }

    status_pb = new_upstream_pb.statuses[1]

    assert status_pb.id == 'b'
    assert status_pb.ctime.seconds == 2000
    assert status_pb.validated == {
        'namespace-1:balancer-1': model_pb2.Condition(status='False', message='Vsjo ploho'),
        'namespace-1:balancer-2': model_pb2.Condition(status='True'),
    }
    assert status_pb.in_progress == {
        'namespace-1:balancer-1': model_pb2.InProgressCondition(status='False'),
        'namespace-1:balancer-2': model_pb2.InProgressCondition(status='False'),
    }
    assert status_pb.active == {
        'namespace-1:balancer-1': model_pb2.Condition(status='False'),
        'namespace-1:balancer-2': model_pb2.Condition(status='True'),
    }

    backend_pb = model_pb2.Backend()
    backend_pb.meta.namespace_id, backend_pb.meta.id = backend_1_id
    backend_pb.meta.version = 'b'
    backend_pb.meta.ctime.FromSeconds(1000)
    backend_pb.meta.mtime.FromSeconds(2000)

    new_backend_pb = copy_per_balancer_statuses(
        entity_pb=backend_pb,
        balancer_state_pbs=[balancer_1_state_pb, balancer_2_state_pb, balancer_3_state_pb])
    assert new_backend_pb.meta == backend_pb.meta
    status_pb = new_backend_pb.statuses[0]
    assert status_pb.id == 'a'
    assert status_pb.ctime.seconds == 1000

    assert status_pb.validated == status_pb.active == {
        'namespace-1:balancer-1': model_pb2.Condition(status='True'),
        'namespace-1:balancer-2': model_pb2.Condition(status='True'),
        'namespace-2:balancer-3': model_pb2.Condition(status='True'),
    }
    assert status_pb.in_progress == {
        'namespace-1:balancer-1': model_pb2.InProgressCondition(status='False'),
        'namespace-1:balancer-2': model_pb2.InProgressCondition(status='False'),
        'namespace-2:balancer-3': model_pb2.InProgressCondition(status='False'),
    }

    status_pb = new_backend_pb.statuses[1]
    assert status_pb.id == 'b'
    assert status_pb.ctime.seconds == 2000

    assert status_pb.validated == {
        'namespace-1:balancer-2': model_pb2.Condition(status='False', message='Vsjo ploho'),
    }
    assert status_pb.in_progress == {
        'namespace-1:balancer-2': model_pb2.InProgressCondition(status='False'),
    }
    assert status_pb.active == {
        'namespace-1:balancer-2': model_pb2.Condition(status='False'),
    }
