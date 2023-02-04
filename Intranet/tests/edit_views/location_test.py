import json
import mock

import pytest

from django.conf import settings
from django.core.urlresolvers import reverse

from staff.lib.testing import StaffFactory, OfficeFactory
from staff.person.models import Staff

from staff.person_profile.edit_views.edit_location_view import FORM_NAME


VIEW_NAME = 'profile:edit-location'

LOCATION = {
      'location_descr': 'trali-vali',
      'location_descr_en': 'tili-tili',
}
WRONG_LOCATION = {
    'duties': '',
    'duties_en': '',
    'candidate_info': '',
}


@pytest.mark.django_db()
def test_edit_location(client):
    ayd = mock.Mock(return_value=None)

    with mock.patch('staff.person.effects.base.actualize_yandex_disk', new=ayd):
        test_person = StaffFactory(login=settings.AUTH_TEST_USER)
        OfficeFactory(name='Морозов')

        url = reverse(VIEW_NAME, kwargs={'login': test_person.login})

        # Заполняем
        response = client.post(
            url,
            json.dumps({FORM_NAME: [LOCATION]}),
            content_type='application/json',
        )

        assert response.status_code == 200, response.content
        answer = json.loads(response.content)
        assert answer == {
            'target': {}
        }
        expected_location = LOCATION

        assert (
            Staff.objects.values('location_descr', 'location_descr_en')
            .get(pk=test_person.pk) == expected_location
        )
