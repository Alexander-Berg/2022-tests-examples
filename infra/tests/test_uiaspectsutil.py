# condition: utf-8
from datetime import datetime, timedelta

from awacs.model.uiaspectsutil import get_namespace_its_tabs, dt_to_str
from infra.awacs.proto import model_pb2


def test_get_namespace_its_tabs():
    its_tab_pbs, condition = get_namespace_its_tabs([])
    assert its_tab_pbs == []
    assert condition == model_pb2.Condition(status='True')

    balancer_aspects_set_1_pb = model_pb2.BalancerAspectsSet()
    balancer_aspects_set_1_pb.meta.namespace_id = 'namespace-id'
    balancer_aspects_set_1_pb.meta.balancer_id = 'balancer-id-1'

    attempt_pb = balancer_aspects_set_1_pb.content.its.status.last_attempt
    attempt_pb.started_at.FromDatetime(datetime.utcnow() - timedelta(seconds=30))
    attempt_pb.finished_at.FromDatetime(datetime.utcnow())
    attempt_pb.succeeded.status = 'True'
    balancer_aspects_set_1_pb.content.its.status.last_successful_attempt.CopyFrom(attempt_pb)

    its_content_pb = balancer_aspects_set_1_pb.content.its.content
    its_content_pb.location_paths.extend(['a-1', 'b-1', 'c', 'd'])

    its_tab_pbs, condition = get_namespace_its_tabs([balancer_aspects_set_1_pb])
    assert its_tab_pbs == [
        model_pb2.NamespaceUiAspectsContent.ItsTab(
            id='balancer-id-1',
            location_paths=['a-1', 'b-1', 'c', 'd'],
            condition=model_pb2.Condition(status='True')
        )
    ]
    assert condition == model_pb2.Condition(status='True')

    # add second balancer aspects set
    balancer_aspects_set_2_pb = model_pb2.BalancerAspectsSet()
    balancer_aspects_set_2_pb.meta.namespace_id = 'namespace-id'
    balancer_aspects_set_2_pb.meta.balancer_id = 'balancer-id-2'

    attempt_pb = balancer_aspects_set_2_pb.content.its.status.last_attempt
    attempt_pb.started_at.FromDatetime(datetime.utcnow() - timedelta(seconds=30))
    attempt_pb.finished_at.FromDatetime(datetime.utcnow())
    attempt_pb.succeeded.status = 'True'
    balancer_aspects_set_2_pb.content.its.status.last_successful_attempt.CopyFrom(attempt_pb)

    its_content_pb = balancer_aspects_set_2_pb.content.its.content
    its_content_pb.location_paths.extend(['a-2', 'b-2', 'c', 'd'])

    its_tab_pbs, condition = get_namespace_its_tabs([balancer_aspects_set_1_pb, balancer_aspects_set_2_pb])
    assert its_tab_pbs == [
        model_pb2.NamespaceUiAspectsContent.ItsTab(
            id='balancer-id-1',
            location_paths=['a-1', 'b-1'],
            condition=model_pb2.Condition(status='True')
        ),
        model_pb2.NamespaceUiAspectsContent.ItsTab(
            id='balancer-id-2',
            location_paths=['a-2', 'b-2'],
            condition=model_pb2.Condition(status='True')
        ),
        model_pb2.NamespaceUiAspectsContent.ItsTab(
            id='common',
            location_paths=['c', 'd'],
            condition=model_pb2.Condition(status='True')
        ),
    ]
    assert condition == model_pb2.Condition(status='True')

    # make second balancer aspects set look like the last attempt to resolve "its" aspects has failed
    attempt_pb = balancer_aspects_set_2_pb.content.its.status.last_attempt
    attempt_pb.started_at.FromDatetime(datetime.utcnow() + timedelta(seconds=5))
    attempt_pb.finished_at.FromDatetime(datetime.utcnow() + timedelta(seconds=10))
    attempt_pb.succeeded.status = 'False'
    attempt_pb.succeeded.message = 'Some important reason'
    its_content_pb = balancer_aspects_set_2_pb.content.its.content
    its_content_pb.location_paths.append('x')

    its_tab_pbs, condition = get_namespace_its_tabs([balancer_aspects_set_1_pb, balancer_aspects_set_2_pb])

    expected_message = ("Based on the data retrieved by the last successful "
                        "attempt at {} UTC. The last attempt to resolve \"its\" "
                        "aspects finished at {} UTC and has not been successful.").format(
        dt_to_str(balancer_aspects_set_2_pb.content.its.status.last_successful_attempt.finished_at.ToDatetime()),
        dt_to_str(balancer_aspects_set_2_pb.content.its.status.last_attempt.finished_at.ToDatetime())
    )
    assert its_tab_pbs == [
        model_pb2.NamespaceUiAspectsContent.ItsTab(
            id='balancer-id-1',
            location_paths=['a-1', 'b-1'],
            condition=model_pb2.Condition(status='True')
        ),
        model_pb2.NamespaceUiAspectsContent.ItsTab(
            id='balancer-id-2',
            location_paths=['a-2', 'b-2', 'x'],
            condition=model_pb2.Condition(
                status='Unknown',
                message=expected_message
            )
        ),
        model_pb2.NamespaceUiAspectsContent.ItsTab(
            id='common',
            location_paths=['c', 'd'],
            condition=model_pb2.Condition(
                status='Unknown',
                message='balancer-id-2: {}'.format(expected_message)
            )
        ),
    ]
    assert condition == model_pb2.Condition(status='True')
