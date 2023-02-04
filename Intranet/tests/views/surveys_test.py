from datetime import datetime, timedelta
import json
import pytest

from django.conf import settings
from django.core.urlresolvers import reverse

from staff.lib.testing import StaffFactory, SurveyFactory, DepartmentFactory, OfficeFactory


@pytest.mark.django_db
def test_surveys_by_stranger_observer(client, mocked_mongo):
    owner = StaffFactory(login=settings.AUTH_TEST_USER)

    now = datetime.now()
    long_ago = now - timedelta(weeks=42)
    far_future = now + timedelta(weeks=42)

    SurveyFactory(
        department=owner.department,
        start_at=long_ago,
        end_at=far_future,
    )

    SurveyFactory(
        department=owner.department,
        start_at=long_ago - timedelta(days=1),
        end_at=long_ago,
    )

    SurveyFactory(
        department=owner.department,
        start_at=far_future,
        end_at=far_future + timedelta(days=1),
    )

    observer = StaffFactory(login='stranger')

    client.login(user=observer.user)

    url = reverse('profile:survey', kwargs={'login': observer.login})

    response = client.get(url)

    assert response.status_code == 403
    assert json.loads(response.content) == {}


@pytest.mark.django_db
def test_not_surveys(client, mocked_mongo):
    person = StaffFactory(login=settings.AUTH_TEST_USER)
    client.login(user=person.user)

    url = reverse('profile:survey', kwargs={'login': person.login})

    response = client.get(url)

    assert response.status_code == 200
    assert json.loads(response.content) == {}


@pytest.mark.django_db
def test_not_actual_surveys(client, mocked_mongo):
    person = StaffFactory(login=settings.AUTH_TEST_USER)

    now = datetime.now()
    long_ago = now - timedelta(weeks=42)
    far_future = now + timedelta(weeks=42)

    SurveyFactory(
        department=person.department,
        start_at=long_ago - timedelta(days=1),
        end_at=long_ago,
    )

    SurveyFactory(
        department=person.department,
        start_at=far_future,
        end_at=far_future + timedelta(days=1),
    )

    client.login(user=person.user)

    url = reverse('profile:survey', kwargs={'login': person.login})

    response = client.get(url)

    assert response.status_code == 200
    assert json.loads(response.content) == {}


@pytest.mark.django_db
def test_actual_surveys(client, mocked_mongo):
    person = StaffFactory(login=settings.AUTH_TEST_USER)

    now = datetime.now()
    long_ago = now - timedelta(weeks=42)
    far_future = now + timedelta(weeks=42)

    d = {
        'id': 1,
        'block_text': 'Block text',
        'button_text': 'Button text',
        'button_link': 'Button link',
    }

    SurveyFactory(
        department=person.department,
        start_at=long_ago,
        end_at=far_future,
        **d
    )

    client.login(user=person.user)

    url = reverse('profile:survey', kwargs={'login': person.login})

    response = client.get(url)

    assert response.status_code == 200
    assert json.loads(response.content) == {'target': {'surveys': [d]}}


@pytest.mark.django_db
def test_surveys_filter_by_department_and_office(client):
    person = StaffFactory(login=settings.AUTH_TEST_USER)
    department = DepartmentFactory()
    office = OfficeFactory()

    start = datetime.now() - timedelta(days=1)
    end = datetime.now() + timedelta(days=1)

    SurveyFactory(
        department=person.department,
        office=office,
        start_at=start,
        end_at=end,
        block_text='bad office',
    )

    SurveyFactory(
        department=department,
        office=person.office,
        start_at=start,
        end_at=end,
        block_text='bad department',
    )

    SurveyFactory(
        department=person.department,
        office=None,
        start_at=start,
        end_at=end,
        block_text='good department',
    )

    SurveyFactory(
        department=person.department,
        office=person.office,
        start_at=start,
        end_at=end,
        block_text='good department and office',
    )

    SurveyFactory(
        department=None,
        office=person.office,
        start_at=start,
        end_at=end,
        block_text='good office',
    )

    SurveyFactory(
        department=None,
        office=None,
        start_at=start,
        end_at=end,
        block_text='good survey for all',
    )

    client.login(user=person.user)
    url = reverse('profile:survey', kwargs={'login': person.login})
    response = client.get(url)

    surveys = json.loads(response.content)['target']['surveys']

    assert response.status_code == 200
    assert len(surveys) == 4

    for survey in surveys:
        assert 'good' in survey['block_text']
