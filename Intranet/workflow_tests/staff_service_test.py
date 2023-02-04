import pytest

from staff.lib.testing import OfficeFactory, OrganizationFactory, PlacementFactory
from staff.budget_position.workflow_service import gateways, entities


@pytest.mark.django_db
def test_placement_for():
    office = OfficeFactory()
    organization = OrganizationFactory()

    placement = PlacementFactory(office_id=office.id, organization_id=organization.id, active_status=True)
    PlacementFactory(office_id=office.id, organization_id=organization.id, active_status=False)

    target = gateways.StaffService()
    assert target.placement_for(office.id, organization.id) == entities.Placement(
        id=placement.id,
        office_id=placement.office_id,
        organization_id=placement.organization_id,
    )


@pytest.mark.django_db
def test_placement_for_no_placements():
    office = OfficeFactory()
    organization = OrganizationFactory()

    PlacementFactory(office_id=office.id, organization_id=organization.id, active_status=False)

    target = gateways.StaffService()
    assert target.placement_for(office.id, organization.id) is None


@pytest.mark.django_db
def test_placement_for_several_placements():
    office = OfficeFactory()
    organization = OrganizationFactory()

    PlacementFactory(office_id=office.id, organization_id=organization.id, active_status=True)
    PlacementFactory(office_id=office.id, organization_id=organization.id, active_status=True)

    target = gateways.StaffService()
    assert target.placement_for(office.id, organization.id) is None
