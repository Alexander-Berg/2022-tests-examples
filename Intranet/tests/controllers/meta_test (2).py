import datetime
import json
import pytest

from django.conf import settings
from django.contrib.auth.models import Permission
from django.core.urlresolvers import reverse

from staff.dismissal.tests import factories as dismissal_factories
from staff.lib.testing import (
    CityFactory,
    StaffFactory,
    UserFactory,
)
from staff.person.models import AFFILIATION

from staff.person_profile.permissions.properties import Properties


fields = ['id', 'first_name', 'last_name', 'middle_name', 'login']


def test_hr_is_chief(create_departments_with_hrs):
    properties = Properties(['wlame'], create_departments_with_hrs['dmirain'], True)
    assert properties.get_is_chief('wlame')


def test_deputy_is_not_chief(create_departments_with_hrs):
    properties = Properties(['wlame'], create_departments_with_hrs['denis_p'], True)

    assert not properties.get_is_chief('wlame')


def ensure_equality(hr_partner1, hr_partner2):
    # hr_partner1 - Staff model object
    # hr_partner2 - list with attributes(fetched json)
    for field in fields:
        assert getattr(hr_partner1, field) == hr_partner2[field]


@pytest.mark.django_db
def test_ext_person_has_no_default_acces_to_others(client):
    external_person = StaffFactory(login=settings.AUTH_TEST_USER, affiliation=AFFILIATION.EXTERNAL)
    client.login(user=external_person.user)
    any_person = StaffFactory()

    response = client.get(reverse('profile:meta', kwargs={'login': any_person.login}))
    assert response.status_code == 403


def test_city_in_meta(create_departments_with_hrs, client, mocked_mongo):
    city_name = 'Bobruysk'
    guido = create_departments_with_hrs['guido']
    guido.office.city = CityFactory(name=city_name)
    guido.office.save()
    client.login(user=guido.user)
    response = client.get(reverse('profile:meta', kwargs={'login': 'guido'}))
    assert response.status_code == 200, response.content
    assert json.loads(response.content)['target']['city_name'] == city_name
    assert json.loads(response.content)['target']['city_id'] == guido.office.city_id


def test_dismissal_type_full(create_departments_with_hrs, client, mocked_mongo):
    guido = create_departments_with_hrs['guido']
    # create any dismissal procedure
    cctf = dismissal_factories.ClearanceChitTemplateFactory(department=guido.department, office=None)
    cctf.checkpoints.add(dismissal_factories.CheckPointTemplateFactory())
    cctf.save()
    tester = StaffFactory(login=settings.AUTH_TEST_USER, user=UserFactory(username=settings.AUTH_TEST_USER))
    tester.user.user_permissions.add(Permission.objects.get(codename='can_dismiss_from_anketa'))

    response = client.get(reverse('profile:meta', kwargs={'login': 'guido'}))

    assert response.status_code == 200, response.content
    assert json.loads(response.content)['links']['dismiss_create'].get('type') == 'full'


def test_dismissal_type_fast(create_departments_with_hrs, client, mocked_mongo):
    tester = StaffFactory(login=settings.AUTH_TEST_USER, user=UserFactory(username=settings.AUTH_TEST_USER))
    tester.user.user_permissions.add(Permission.objects.get(codename='can_dismiss_from_anketa'))

    response = client.get(reverse('profile:meta', kwargs={'login': 'guido'}))

    assert response.status_code == 200, response.content
    assert json.loads(response.content)['links']['dismiss_create'].get('type') == 'short'


def test_duty_in_meta(create_departments_with_hrs, client, mocked_mongo):
    guido = create_departments_with_hrs['guido']
    guido.duties = 'asd'
    guido.save()

    client.login(user=guido.user)
    response = client.get(reverse('profile:meta', kwargs={'login': 'guido'}))
    assert response.status_code == 200, response.content
    assert json.loads(response.content)['target']['duties'] == guido.duties


def test_dismissed_meta(create_departments_with_hrs, client, mocked_mongo):
    guido = create_departments_with_hrs['guido']
    guido.is_dismissed = True
    guido.quit_at = datetime.date.today()
    guido.save()

    client.login(user=guido.user)
    response = client.get(reverse('profile:meta', kwargs={'login': 'guido'}))
    resp_data = json.loads(response.content)
    assert response.status_code == 200, response.content
    target = resp_data['target']
    assert not any(target.get(f) for f in ('last_name', 'first_name', 'middle_name'))
    assert target['login'] == guido.login
    assert target['duties'] == guido.duties
    assert target['is_dismissed']
    assert target['quit_info'] == {'month': guido.quit_at.month, 'year': guido.quit_at.year}
    assert target['position'] == guido.position
    loadable = resp_data['loadable_blocks']
    assert set(loadable) == {'services', 'photos', 'departments', 'is_calendar_vertical'}


@pytest.mark.django_db
def test_preferred_name_in_meta(client):
    StaffFactory(
        first_name='Иван',
        last_name='Иванов',
        middle_name='Иванович',
        preferred_name='Ваня',
        login='evan-ivanovich',
    )
    response = client.get(reverse('profile:meta', kwargs={'login': 'evan-ivanovich'}))

    assert response.status_code == 200, response.content
    assert json.loads(response.content)['target'].get('preferred_name') == 'Ваня'
