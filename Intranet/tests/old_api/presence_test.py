from datetime import datetime, timedelta
import json

import pytest

from django.core.urlresolvers import reverse

from staff.gap.workflows.utils import find_workflow


@pytest.mark.django_db
def test_available(gap_test, client):
    AbsenceWorkflow = find_workflow('absence')
    base_gap = gap_test.get_base_gap(AbsenceWorkflow)
    AbsenceWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    url = '%s?login_list=%s' % (
        reverse('gap:api-presence'),
        gap_test.test_person.login,
    )
    response = client.get(url)
    assert response.status_code == 200
    result = json.loads(response.content)
    assert result == {
        'result': True,
        'staff': {
            gap_test.test_person.login: {'is_available': True}
        }
    }


@pytest.mark.django_db
def test_not_available(gap_test, client):
    AbsenceWorkflow = find_workflow('absence')
    base_gap = gap_test.get_base_gap(AbsenceWorkflow)
    base_gap['date_from'] = datetime.now() - timedelta(days=1)
    base_gap['date_to'] = datetime.now() + timedelta(days=1)
    AbsenceWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    url = '%s?login_list=%s' % (
        reverse('gap:api-presence'),
        gap_test.test_person.login,
    )
    response = client.get(url)
    assert response.status_code == 200
    result = json.loads(response.content)
    assert result == {
        'result': True,
        'staff': {
            gap_test.test_person.login: {'is_available': False}
        }
    }
