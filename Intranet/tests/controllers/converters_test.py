import pytest

from staff.person.models import Organization

from staff.oebs.controllers.converters import organization_id


@pytest.mark.django_db
def test_organization_id_is_not_none_on_oebs_none(company):
    assert organization_id(None) is Organization.get_outstaff_org().id
