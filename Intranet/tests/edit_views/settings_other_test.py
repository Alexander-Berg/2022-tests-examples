import pytest
import json

from django.conf import settings
from django.core.urlresolvers import reverse

from staff.person_profile.edit_views.edit_settings_other_view import FORM_NAME
from staff.person.models import Staff
from staff.user_settings.objects import PersonSettings
from staff.lib.testing import (
    StaffFactory,
)

VIEW_NAME = 'edit-settings-other'
COLLECTION_NAME = PersonSettings.MONGO_COLLECTION


@pytest.mark.django_db
def test_that_check_that_data_in_form_save_in_model(client, mocked_mongo):
    test_person = StaffFactory(
        login=settings.AUTH_TEST_USER,
    )
    test_person.auto_translate = False
    test_person.shell = '/bin/bash'
    test_person.lang_ui = 'ru'
    test_person.show_all_middle_name = False
    test_person.show_beta_interface = False
    test_person.save()

    data_form = {
        'auto_translate': 'true',
        'domain': 'Y',
        'is_calendar_vertical': 'true',
        'lang_ui': 'en',
        'shell': '/usr/local/bin/bash',
        'show_all_middle_name': 'true',
        'show_beta_interface': 'true',
        'tz': 'Europe/Moscow'
    }

    url = reverse('profile:{}'.format(VIEW_NAME), kwargs={'login': test_person.login})
    response = client.post(
        url,
        json.dumps({FORM_NAME: [data_form]}),
        content_type='application/json',
    )

    data_form.update(
        auto_translate=True,
        show_all_middle_name=True,
        is_calendar_vertical=True,
        show_beta_interface=True,
    )

    person_with_new_data = Staff.objects.get(id=test_person.id)

    assert response.status_code == 200

    assert data_form.pop('is_calendar_vertical') == PersonSettings(test_person.id)['is_calendar_vertical']
    for field, value in data_form.items():
        new_val = getattr(person_with_new_data, field)
        assert new_val == value, f'{field} expect: {value} now: {new_val}'
