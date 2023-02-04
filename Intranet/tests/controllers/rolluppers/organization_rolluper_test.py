import pytest

from staff.lib.testing import OrganizationFactory
from staff.person.models import Organization

from staff.oebs.controllers.rolluppers.organization_rollupper import OrganizationRollupper
from staff.oebs.tests.factories import OrganizationFactory as OebsOrganizationFactory


@pytest.mark.django_db
def test_organization_update_on_rollup():
    org_amount = 4

    organizations = [
        OrganizationFactory()
        for _ in range(org_amount)
    ]
    oebs_organizations = [
        OebsOrganizationFactory(
            name_ru=f'Организация {i}',
            name_en=f'Organization {i}',
            country_code='RU',
            dis_organization=organizations[i],
        )
        for i in range(org_amount)
    ]

    OrganizationRollupper().run_rollup()

    organizations = Organization.objects.order_by('id')

    for i in range(org_amount):
        assert organizations[i].name == oebs_organizations[i].name_ru
        assert organizations[i].name_en == oebs_organizations[i].name_en
        assert organizations[i].country_code == oebs_organizations[i].country_code
