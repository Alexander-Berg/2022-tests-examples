import json

import pytest

from django.conf import settings
from django.core.urlresolvers import reverse

from staff.lib.testing import StaffFactory


@pytest.mark.django_db
@pytest.mark.parametrize('test_value', [True, False])
def test_view(client, mocked_mongo, test_value):
    tester = StaffFactory(login=settings.AUTH_TEST_USER)
    mocked_mongo.db['person_settings'].insert_one({'person_id': tester.id, 'is_calendar_vertical': test_value})
    client.login(user=tester.user)

    response = client.get(reverse('profile:is_calendar_vertical', kwargs={'login': settings.AUTH_TEST_USER}))

    assert response.status_code == 200
    answer = json.loads(response.content)
    assert answer.get('is_calendar_vertical') is test_value, answer
