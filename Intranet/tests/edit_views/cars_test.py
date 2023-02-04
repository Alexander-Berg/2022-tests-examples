import pytest
import json

from django.conf import settings
from django.core.urlresolvers import reverse

from staff.person.models import StaffCar
from staff.person_profile.edit_views.edit_cars_view import FORM_NAME

from staff.lib.testing import StaffFactory


VIEW_NAME = 'profile:edit-cars'

NEW_CARS = [
    {
        "plate": "В855КУ1991",
        "model": "222 BMW Civic",
    },
    {
        "plate": "А877ХВ1991",
        "model": "Opel 2",
    },
]

NEW_INVALID_CARS = [
    {
        "plate": "В855КУ1991",
        "model": "222 BMW Civic",
    },
    {
        "plate": "А877ХВ1991",
        "model": "Opel 2",
    },
    {
        "plate": "334423+8",
        "model": "[На модель автомобиля выделено 100 символов. Текст больше 100"
                 " символов не должен пройти валидацию. 99, 100]",
    },
    {
        "plate": "",
        "model": "Тут всё ок",
    },
]


def get_db_objects(login):
    return StaffCar.objects.active().filter(staff__login=login).order_by('position')


@pytest.mark.django_db()
def test_edit_cars(client):
    test_person = StaffFactory(
        login=settings.AUTH_TEST_USER,
    )
    url = reverse(VIEW_NAME, kwargs={'login': test_person.login})

    # Добавляем автомобили
    response = client.post(
        url,
        json.dumps({FORM_NAME: NEW_CARS}),
        content_type='application/json',
    )
    assert response.status_code == 200, response.content
    answer = json.loads(response.content)
    assert answer == {
        'target': {
            'cars': [
                {'model': '222 BMW Civic', 'plate': 'В 855 КУ 1991'},
                {'model': 'Opel 2', 'plate': 'А 877 ХВ 1991'},
            ]
        }
    }

    db_data = list(get_db_objects(test_person.login).values('model', 'plate'))
    assert db_data == [
        {'model': '222 BMW Civic', 'plate': 'В 855 КУ 1991'},
        {'model': 'Opel 2', 'plate': 'А 877 ХВ 1991'},
    ]

    # Пытаемся добавить в конец невалидные данные двух автомобилей
    response = client.post(
        url,
        json.dumps({FORM_NAME: NEW_INVALID_CARS}),
        content_type='application/json'
    )
    assert response.status_code == 200, response.content
    answer = json.loads(response.content)
    assert answer == {
        'errors': {
            'cars': {
                '2': {
                    'model': [{
                        'error_key': 'default-field-max_length',
                        'params': {
                            'limit_value': '100', 'show_value': '107'
                        }
                    }],
                    'plate': [{
                        'error_key': 'default-field-invalid'
                    }]
                },
                '3': {
                    'plate': [{
                        'error_key': 'default-field-required'
                    }]
                }
            }
        }
    }

    # Удаляем первый автомобиль, оставляем только второй
    objects_data = NEW_CARS[1:2]
    response = client.post(
        url,
        json.dumps({FORM_NAME: objects_data}),
        content_type='application/json'
    )
    assert response.status_code == 200, response.content
    answer = json.loads(response.content)
    assert answer == {
        'target': {
            'cars': [
                {'plate': 'А 877 ХВ 1991', 'model': 'Opel 2'}
            ]
        }
    }
    db_data = list(get_db_objects(test_person.login).values(
        'model', 'plate', 'position'))
    assert len(db_data) == 1
    assert db_data == [
        {'plate': 'А 877 ХВ 1991', 'model': 'Opel 2', 'position': 0}
    ]
