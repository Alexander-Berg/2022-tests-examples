import pytest

from staff.departments.tests.factories import HRProductFactory
from staff.lib.testing import ValueStreamFactory

from staff.budget_position.workflow_service.gateways.hr_product_resolver import HRProductResolver


def test_resolve_hr_product_no_hr_product_id():
    assert HRProductResolver().resolve_hr_product(None) == (None, None)


@pytest.mark.django_db
def test_resolve_hr_product_disabled_hr_product():
    hr_product_id = HRProductFactory(intranet_status=0).id

    result = HRProductResolver().resolve_hr_product(hr_product_id)

    assert result[0] is None
    assert result[1] is not None


@pytest.mark.django_db
def test_resolve_hr_product_no_value_stream():
    hr_product_id = HRProductFactory(intranet_status=1).id

    result = HRProductResolver().resolve_hr_product(hr_product_id)

    assert result[0] is None
    assert result[1] is not None


@pytest.mark.django_db
def test_resolve_hr_product_disabled_value_stream():
    hr_product_id = HRProductFactory(intranet_status=1, value_stream=ValueStreamFactory(intranet_status=0)).id

    result = HRProductResolver().resolve_hr_product(hr_product_id)

    assert result[0] is None
    assert result[1] is not None


@pytest.mark.django_db
def test_resolve_hr_product():
    value_stream = ValueStreamFactory(intranet_status=1)
    hr_product_id = HRProductFactory(intranet_status=1, value_stream=value_stream).id

    result = HRProductResolver().resolve_hr_product(hr_product_id)

    assert result[0] == value_stream.id
    assert result[1] is None
