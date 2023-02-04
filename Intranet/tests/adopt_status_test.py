import json
import pytest

from django.core.urlresolvers import reverse
from staff.lib.testing import StaffFactory
from staff.preprofile.views import adopt_status
from staff.preprofile.models import PersonAdoptApplication, STATUS


@pytest.mark.django_db
def test_adopt_status(rf):

    person_data = {
        'login': 'tester2',
        'status': STATUS.NEW,
        'status_reason': 'Some status',
        'offer_id': 100500,
        'first_name': 'Vasya',
        'last_name': 'Pupkin'
    }

    PersonAdoptApplication.objects.create(**person_data)

    request = rf.get(reverse('preprofile:adopt_status'))

    request.user = StaffFactory(login='tester').user
    response = adopt_status(request)
    result = json.loads(response.content)['result']

    expected_person_data_keys = {
        'login',
        'status',
        'status_reason',
        'first_name',
        'last_name',
        'preprofile_id',
    }

    assert response.status_code == 200
    assert len(result) == 1
    assert set(result[0].keys()) == expected_person_data_keys

    for key in expected_person_data_keys:
        if key in person_data:
            assert person_data[key] == result[0][key]
