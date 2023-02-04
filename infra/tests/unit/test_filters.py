from __future__ import unicode_literals
import pytest

from yp_proto.yp.client.hq.proto import types_pb2
from instancectl.hq.volumes import template_filters


def test_last_revision_filter():
    instance_1 = types_pb2.Instance()
    instance_1.status.revision.add().id = '001'
    instance_2 = types_pb2.Instance()
    instance_2.status.revision.add().id = '002'
    filtered = template_filters.last_revision([instance_1, instance_2])
    assert filtered == [instance_2]


def test_alive_filter():
    instance_1 = types_pb2.Instance()
    instance_1.status.ready.status = 'True'
    instance_2 = types_pb2.Instance()
    filtered = template_filters.alive([instance_1, instance_2])
    assert filtered == [instance_1]


def test_aliver_or_last_revision_filter():
    instance_1 = types_pb2.Instance()
    instance_1.status.revision.add().id = '001'
    instance_2 = types_pb2.Instance()
    instance_2.status.revision.add().id = '002'
    instance_1.status.ready.status = 'True'
    filtered = template_filters.alive_or_last_revision([instance_1, instance_2])
    assert filtered == [instance_1]


def test_instances_services():
    instance_1 = types_pb2.Instance()
    instance_1.meta.service_id = 'service_1'
    instance_2 = types_pb2.Instance()
    instance_2.meta.service_id = 'service_2'
    with pytest.raises(TypeError):
        template_filters.last_revision([instance_1, instance_2])
