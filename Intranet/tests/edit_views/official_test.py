import pytest
import json

from django.conf import settings
from django.core.urlresolvers import reverse

from staff.person.models import Staff, EMPLOYMENT

from staff.lib.testing import (
    StaffFactory,
    OrganizationFactory,
    UserFactory,
)

FORM = 'official'


def post(client, data, test_person):
    response = client.post(
        reverse('profile:edit-official', kwargs={'login': test_person.login}),
        json.dumps(data),
        content_type='application/json'
    )
    assert response.status_code == 200, response.content
    return json.loads(response.content)


@pytest.mark.django_db()
def test_edit_official(client):
    test_person = StaffFactory(
        user=UserFactory(is_superuser=True),
        login=settings.AUTH_TEST_USER,
        employment=EMPLOYMENT.FULL,
    )

    active_org = OrganizationFactory()
    active_emp = EMPLOYMENT.FULL
    next_org = OrganizationFactory()
    next_emp = EMPLOYMENT.PARTIAL

    def check_test_person(org, emp):
        person = Staff.objects.get(login=settings.AUTH_TEST_USER)
        assert person.organization_id == org.id
        assert person.employment == emp

    data = {FORM: [
        {
            'employment': active_emp,
            'organization_id': str(active_org.id),
        },
    ]}

    answer = post(client, data, test_person)
    assert answer == {'target': {}}

    check_test_person(active_org, active_emp)

    data = {FORM: [
        {
            'employment': 'WRONG',
            'organization_id': str(-1),
        },
    ]}

    answer = post(client, data, test_person)
    assert answer == {'errors': {FORM: {
        '0': {
            'employment': [
                {
                    'error_key': 'choice-field-invalid_choice',
                    'params': {'value': 'WRONG'},
                }
            ],
            'organization_id': [
                {'error_key': 'modelchoice-field-invalid_choice'}
            ],
        },
    }}}

    check_test_person(active_org, active_emp)

    data = {FORM: [
        {
            'employment': next_emp,
            'organization_id': str(next_org.id),
        },
    ]}

    answer = post(client, data, test_person)
    assert answer == {'target': {}}

    check_test_person(next_org, next_emp)
