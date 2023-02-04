from datetime import date
import json

import pytest

from django.core.urlresolvers import reverse

from staff.lib.testing import StaffFactory

from staff.lenta.views import lenta_data


@pytest.mark.django_db
def test_lenta_data_works(rf, company):
    # given
    request = rf.get(reverse('lenta:lenta-data'))

    # when
    response = lenta_data(request)

    # then
    assert response.status_code == 200


@pytest.mark.django_db
def test_lenta_data_returns_some_results_after_hire(rf, company):
    # given
    request = rf.get(reverse('lenta:lenta-data'))
    person1 = StaffFactory(department=company.yandex)
    person2 = StaffFactory(department=company.yandex)
    today_key = date.today().isoformat()

    # when
    response = lenta_data(request)

    # then
    assert response.status_code == 200
    data = json.loads(response.content)

    person1_hire_action = next(action for action in data[today_key] if action['login'] == person1.login)
    assert person1_hire_action['action'] == 'HIRED'

    person2_hire_action = next(action for action in data[today_key] if action['login'] == person2.login)
    assert person2_hire_action['action'] == 'HIRED'
