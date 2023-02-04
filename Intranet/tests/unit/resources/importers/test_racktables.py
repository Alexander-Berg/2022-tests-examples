import pytest

from plan.resources import constants
from common import factories

from plan.resources.models import ServiceResource
from plan.resources.tasks import sync_with_racktables
from utils import vcr_test

pytestmark = pytest.mark.django_db


def test_import_racktables():
    racktables_supplier = factories.ServiceFactory(slug=constants.RACKTABLES_SUPPLIER_SERVICE_SLUG)
    resource_type = factories.ResourceTypeFactory(supplier=racktables_supplier, name='VS')

    service1 = factories.ServiceFactory(id=2411)
    service2 = factories.ServiceFactory(id=2422)
    service3 = factories.ServiceFactory(id=2433)

    resource = factories.ResourceFactory(type=resource_type, external_id='12085')
    old_sr = factories.ServiceResourceFactory(
        resource=resource,
        service=service2,
        state=ServiceResource.GRANTED
    )

    deleted_resource = factories.ResourceFactory(type=resource_type, external_id='322')
    deleted_sr = factories.ServiceResourceFactory(
        resource=deleted_resource,
        service=service3,
        state=ServiceResource.GRANTED
    )

    other_type_resource = factories.ServiceResourceFactory(state=ServiceResource.GRANTED)

    cassette_name = 'resources/racktables_importer.json'
    with vcr_test().use_cassette(cassette_name):
        sync_with_racktables()

    assert ServiceResource.objects.count() == 6

    deleted_sr.refresh_from_db()
    assert deleted_sr.state == ServiceResource.DEPRIVED
    old_sr.refresh_from_db()
    assert old_sr.state == ServiceResource.DEPRIVED
    other_type_resource.refresh_from_db()
    assert other_type_resource.state == ServiceResource.GRANTED

    assert (
        ServiceResource.objects
        .filter(service=service1, resource__external_id='12085', state=ServiceResource.GRANTED)
        .exists()
    )
    assert (
        ServiceResource.objects
        .filter(service=service1, resource__external_id='12096', state=ServiceResource.GRANTED)
        .exists()
    )
    assert (
        ServiceResource.objects
        .filter(service=service2, resource__external_id='12096', state=ServiceResource.GRANTED)
        .exists()
    )
