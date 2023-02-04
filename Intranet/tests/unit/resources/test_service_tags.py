import pretend
import pytest

from plan.resources.models import ServiceResource
from plan.resources.tasks import actualize_service_tags
from common import factories


@pytest.fixture
def data():
    resource_type_with_tag = factories.ResourceTypeFactory()
    resource_type_without_tag = factories.ResourceTypeFactory()
    service_tag = factories.ServiceTagFactory()
    resource_type_with_tag.service_tags.add(service_tag)
    service = factories.ServiceFactory()
    return pretend.stub(
        resource_with_tag=factories.ResourceFactory(type=resource_type_with_tag),
        resource_without_tag=factories.ResourceFactory(type=resource_type_without_tag),
        tag=service_tag,
        service=service,
    )


def test_add_on_grant(data):
    resource = data.resource_with_tag
    service_resource = factories.ServiceResourceFactory(
        service=data.service,
        resource=resource,
        state=ServiceResource.GRANTING,
    )
    service_resource._grant()
    service_resource.save()
    tags = list(data.service.tags.values_list('id', flat=True))
    assert tags == [data.tag.id]


@pytest.mark.parametrize('remove_all', [True, False])
def test_remove_on_deprive(data, remove_all):
    resource_a = data.resource_with_tag
    resource_b = factories.ResourceFactory(type=resource_a.type)
    service_resources = [
        factories.ServiceResourceFactory(
            state=ServiceResource.GRANTING,
            service=data.service,
            resource=resource,
        )
        for resource in [resource_a, resource_b]
    ]
    for service_resource in service_resources:
        service_resource._grant()
        service_resource.save()

    service_resources[0].forced_deprive(check_tags=True)
    service_resources[0].save()
    if remove_all:
        service_resources[1].forced_deprive(check_tags=True)
        service_resources[1].save()
    tags = list(data.service.tags.values_list('id', flat=True))
    if remove_all:
        assert tags == []
    else:
        assert tags == [data.tag.id]


def test_task(data):
    service_to_remove = factories.ServiceFactory()
    service_to_add = factories.ServiceFactory()
    service_to_remove.tags.add(data.tag)
    service_with_deprived = factories.ServiceFactory()
    service_with_deprived.tags.add(data.tag)

    factories.ServiceResourceFactory(
        service=service_to_add,
        resource=data.resource_with_tag,
        state=ServiceResource.GRANTED,
    )
    factories.ServiceResourceFactory(
        service=service_with_deprived,
        resource=data.resource_with_tag,
        state=ServiceResource.DEPRIVED,
    )
    for tag in service_to_add.tags.all():
        service_to_add.tags.remove(tag)

    actualize_service_tags()

    assert list(service_to_add.tags.values_list('id', flat=True)) == [data.tag.id]
    assert list(service_with_deprived.tags.values_list('id', flat=True)) == []
    assert list(service_to_remove.tags.values_list('id', flat=True)) == []
