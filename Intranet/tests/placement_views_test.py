import json
from mock import patch

from django.core.urlresolvers import reverse

from staff.lib.testing import PlacementFactory, OrganizationFactory, OfficeFactory

from staff.budget_position.views import check_placement_view


@patch('staff.lib.decorators._check_service_id', lambda *a, **b: True)
def test_check_placement_view_returns_true_on_valid_combination(femida_user, rf):
    # given
    organization = OrganizationFactory(intranet_status=1)
    office = OfficeFactory(intranet_status=1)
    PlacementFactory(organization=organization, office=office)

    request = rf.get(
        reverse('budget-position-api:check-placement'),
        {'office': office.id, 'organization': organization.id},
    )
    request.user = femida_user
    request.yauser = None

    # when
    response = check_placement_view(request)

    # then
    assert json.loads(response.content) == {'exists': True}


@patch('staff.lib.decorators._check_service_id', lambda *a, **b: True)
def test_check_placement_view_returns_false_on_invalid_combination(femida_user, rf):
    # given
    organization = OrganizationFactory(intranet_status=1)
    office = OfficeFactory(intranet_status=1)
    PlacementFactory(organization=organization, office=office, active_status=False)

    request = rf.get(
        reverse('budget-position-api:check-placement'),
        {'office': office.id, 'organization': organization.id},
    )
    request.user = femida_user
    request.yauser = None

    # when
    response = check_placement_view(request)

    # then
    assert json.loads(response.content) == {'exists': False}
