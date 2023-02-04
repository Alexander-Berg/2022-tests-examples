import json

import pytest

from waffle.models import Switch

from django.contrib.auth.models import Permission
from django.core.urlresolvers import reverse

from staff.budget_position.tests.utils import BudgetPositionAssignmentFactory
from staff.headcounts.headcounts_summary.query_builder.query_params import RelatedEntity
from staff.headcounts.views import summary


@pytest.mark.django_db
def test_summary(rf, company):
    # given
    Switch.objects.create(name='enable_summary_handle', active=True)
    BudgetPositionAssignmentFactory(department=company.dep1, value_stream=company.vs_root, geography=company.geo_russia)
    BudgetPositionAssignmentFactory(department=company.dep1, value_stream=company.vs_root, geography=company.geo_russia)
    BudgetPositionAssignmentFactory(department=company.dep2, value_stream=company.vs_root, geography=company.geo_russia)

    form_data = {
        'groupings': [RelatedEntity.department.value, RelatedEntity.value_stream.value],
        'department_filters': [company.yandex.url],
    }
    request = rf.get(reverse('headcounts-api:summary'), data=form_data)
    request.user = company.persons['dep1-hr-analyst'].user

    # when
    response = summary(request)

    # then
    assert response.status_code == 200
    result = json.loads(response.content)
    assert len(result['next_level_grouping']) == 1


@pytest.mark.django_db
def test_summary_from_chief_of_department(rf, company):
    # given
    observer = company.persons['dep1-chief']
    observer.user.user_permissions.add(Permission.objects.get(codename='can_view_headcounts'))
    Switch.objects.create(name='enable_summary_handle', active=True)
    BudgetPositionAssignmentFactory(department=company.dep1, value_stream=company.vs_root, geography=company.geo_russia)
    BudgetPositionAssignmentFactory(department=company.dep1, value_stream=company.vs_root, geography=company.geo_russia)
    BudgetPositionAssignmentFactory(
        department=company.dep11,
        value_stream=company.vs_root,
        geography=company.geo_russia,
    )

    form_data = {
        'groupings': [RelatedEntity.department.value, RelatedEntity.value_stream.value],
        'department_filters': [company.dep11.url],
    }
    request = rf.get(reverse('headcounts-api:summary'), data=form_data)
    request.user = observer.user

    # when
    response = summary(request)

    # then
    assert response.status_code == 200
    result = json.loads(response.content)
    assert len(result['next_level_grouping']) == 1
