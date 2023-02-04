import pytest

from staff.lib.testing import GeographyFactory, GeographyDepartmentFactory

from staff.budget_position.workflow_service.gateways.geography_resolver import GeographyResolver


def test_resolve_geography_no_geography_id():
    assert GeographyResolver().resolve_geography(None) == (None, None)


@pytest.mark.django_db
def test_resolve_geography_disabled_geography():
    geo = GeographyFactory(intranet_status=0)

    result = GeographyResolver().resolve_geography(geo.department_instance.url)

    assert result[0] is None
    assert result[1] is not None


@pytest.mark.django_db
def test_resolve_geography_disabled_department_instance():
    department_instance = GeographyDepartmentFactory(intranet_status=0)
    geo = GeographyFactory(intranet_status=1, department_instance=department_instance)

    result = GeographyResolver().resolve_geography(geo.department_instance.url)

    assert result[0] is None
    assert result[1] is not None


@pytest.mark.django_db
def test_resolve_geography():
    geo = GeographyFactory(intranet_status=1)

    result = GeographyResolver().resolve_geography(geo.department_instance.url)

    assert result[0] == geo.department_instance.id
    assert result[1] is None
