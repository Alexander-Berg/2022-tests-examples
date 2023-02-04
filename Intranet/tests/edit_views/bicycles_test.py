import pytest
import json

from django.conf import settings
from django.core.urlresolvers import reverse

from staff.person.models import Bicycle
from staff.person_profile.edit_views.edit_bicycles_view import FORM_NAME

from staff.lib.testing import StaffFactory


VIEW_NAME = 'profile:edit-bicycles'

NEW_BICYCLES = [
    {
        'plate': 979798,
        'description': 'My red bike',
    },
    {
        'plate': 970001,
        'description': 'Конь педальный',
    },
]

NEW_INVALID_BICYCLES = [
    {
        'plate': 7652444439,
        'description': 'Неверный номер плашки',
    },
    {
        'plate': 80384832254,
        'description': 'Opel 2',
    },
    {
        'plate': 3452765344,
        'description': 'На описание велосипеда выделено 50 символов. Текст '
                       'больше 50 символов не должен пройти валидацию.',
    },
]


def str_plate(bicycle_data):
    return {
        'plate': str(bicycle_data['plate']),
        'description': bicycle_data['description']
    }


def get_db_objects(login):
    return Bicycle.objects.filter(owner__login=login).order_by('position')


@pytest.mark.django_db()
def test_edit_bicycles(client):

    test_person = StaffFactory(
        login=settings.AUTH_TEST_USER,
    )
    url = reverse(VIEW_NAME, kwargs={'login': test_person.login})

    # Добавляем велосипеды
    response = client.post(
        url,
        json.dumps({FORM_NAME: [str_plate(bicycle) for bicycle in NEW_BICYCLES]}),
        content_type='application/json',
    )
    assert response.status_code == 200, response.content
    answer = json.loads(response.content)
    assert answer == {
        'target': {
            'bicycles': NEW_BICYCLES
        }
    }

    db_data = list(get_db_objects(test_person.login).values('plate', 'description'))
    assert db_data == NEW_BICYCLES

    # Пытаемся добавить в конец невалидные данные двух автомобилей
    response = client.post(
        url,
        json.dumps({FORM_NAME: [str_plate(bicycle) for bicycle in NEW_INVALID_BICYCLES]}),
        content_type='application/json'
    )
    assert response.status_code == 200, response.content
    answer = json.loads(response.content)
    assert answer == {
        'errors': {
            'bicycles': {
                '0': {'plate': [{'error_key': 'invalid'}]},
                '1': {'plate': [{'error_key': 'invalid'}]},
                '2': {
                    'description': [
                        {
                            'error_key': 'default-field-max_length',
                            'params': {'limit_value': '50', 'show_value': '97'}
                        }
                    ],
                    'plate': [{'error_key': 'invalid'}]}
            }
        }
    }

    # Удаляем первый велосипед, оставляем только второй
    objects_data = NEW_BICYCLES[1:2]
    response = client.post(
        url,
        json.dumps({FORM_NAME: [str_plate(bicycle) for bicycle in objects_data]}),
        content_type='application/json'
    )
    assert response.status_code == 200, response.content
    answer = json.loads(response.content)
    assert answer == {
        'target': {
            'bicycles': objects_data
        }
    }
    db_data = list(
        get_db_objects(test_person.login).values('plate', 'description', 'position')
    )
    assert len(db_data) == 1
    assert db_data == [
        {
            'plate': objects_data[0]['plate'],
            'description': objects_data[0]['description'],
            'position': 0
        }
    ]
