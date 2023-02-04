import json

from mock import patch, Mock
import pytest

from django.core.urlresolvers import reverse
from django.test.client import RequestFactory

from staff.achievery.views.create_achievement import validate_create_achievement, create_achievement
from staff.groups.models import Group
from staff.lib.auth import auth_mechanisms as auth


def _create_valid_form_data(title='some achievement name', icon_url=None):
    data = {
        'questions': [
            {
                'slug': 'title',
                'value': title,
            },
            {
                'slug': 'title_en',
                'value': title,
            },
            {
                'slug': 'category',
                'value': 'work',
            },
            {
                'slug': 'service_name',
                'value': 'service name',
            },
        ],
    }

    if icon_url is not None:
        data['questions'].append({'slug': 'icon_url', 'value': icon_url})

    return data


@pytest.mark.django_db
def test_validate_create_achievement():
    data = _create_valid_form_data()

    rf = RequestFactory()
    request = rf.post(
        reverse('achievery:validate_create_achievement'),
        json.dumps(data),
        content_type='application/json',
    )

    response = validate_create_achievement(request)

    assert response.status_code == 200
    assert json.loads(response.content) == {'status': 'OK'}
    assert Group.objects


@pytest.mark.django_db
def test_validate_create_achievement_bad_data():
    data = _create_valid_form_data()
    missing_field = data['questions'].pop(-1)

    rf = RequestFactory()
    request = rf.post(
        reverse('achievery:validate_create_achievement'),
        json.dumps(data),
        content_type='application/json',
    )

    response = validate_create_achievement(request)
    response_data = json.loads(response.content)

    assert response.status_code == 200
    assert response_data['status'] == 'ERROR'
    assert missing_field['slug'] in response_data['errors']


@pytest.mark.django_db
def test_validate_create_achievement_bad_json():
    rf = RequestFactory()
    request = rf.post(
        reverse('achievery:validate_create_achievement'),
        'not json',
        content_type='application/json',
    )

    response = validate_create_achievement(request)

    assert response.status_code == 400


@pytest.mark.django_db
def test_create_achievement():
    title = 'cool title'
    data = _create_valid_form_data(title=title)

    rf = RequestFactory()
    request = rf.post(
        reverse('achievery:create_achievement'),
        json.dumps(data),
        content_type='application/json',
    )
    request.auth_mechanism = auth.TVM
    request.yauser = None
    request.user = Mock()

    patch_task = patch('staff.achievery.views.create_achievement.create_achievement_task.delay')

    with patch_task as mocked_created_achievement_task:
        response = create_achievement(request)

        mocked_created_achievement_task.assert_called_once()
        assert response.status_code == 200


@pytest.mark.django_db
def test_create_achievement_bad_json():
    rf = RequestFactory()
    request = rf.post(
        reverse('achievery:create_achievement'),
        'not json',
        content_type='application/json',
    )
    request.auth_mechanism = auth.TVM
    request.yauser = None
    request.user = Mock()

    response = create_achievement(request)

    assert response.status_code == 400
