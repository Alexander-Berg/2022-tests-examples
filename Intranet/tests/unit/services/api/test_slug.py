import pytest
import pretend

from common import factories
from rest_framework.reverse import reverse
from plan.services.api.slug import SCHEDULE_SLUG_TYPE

pytestmark = pytest.mark.django_db


def get_slug(client, name, **params):
    data = {'name': name}
    data.update(**params)
    response = client.json.get(
        reverse('api-frontend:service-make-slug-list'),
        data
    )

    assert response.status_code == 200
    return response.json()['slug']


def validate_slug(client, slug, **params):
    data = {'slug': slug}
    data.update(params)
    response = client.json.get(
        reverse('api-frontend:service-validate-slug-list'),
        data,
    )

    assert response.status_code == 200
    result = response.json()
    return result['valid'], result['alternative']


def validate_invalid_slug(client, slug, message, **params):
    data = {'slug': slug}
    data.update(params)
    response = client.json.get(
        reverse('api-frontend:service-validate-slug-list'),
        data,
    )

    assert response.status_code == 400
    expected = {
        'error': {
            'detail': 'Sent data is wrong.',
            'code': 'bad_request',
            'message': message
        }
    }
    assert response.json() == expected


@pytest.fixture
def cities():
    factories.ServiceFactory(slug='moscow_2')
    factories.ServiceFactory(slug='moscow33')
    factories.ServiceFactory(slug='spb11')
    factories.ServiceFactory(slug=('a' * 50))

    return pretend.stub(
        moscow=factories.ServiceFactory(slug='moscow'),
        moscow1=factories.ServiceFactory(slug='moscow1'),
        spb=factories.ServiceFactory(slug='spb'),
    )


@pytest.fixture
def schedules(cities):
    factories.ScheduleFactory(service=cities.moscow, slug='moscow_duty')
    factories.ScheduleFactory(service=cities.moscow, slug='moscow_duty1')
    factories.ScheduleFactory(service=cities.spb, slug='duty')
    factories.ScheduleFactory(service=cities.spb, slug='duty2')


def test_make_slug(client, cities):
    assert get_slug(client, 'Moscow') == 'moscow2'
    assert get_slug(client, 'Moscow 2') == 'moscow_21'
    assert get_slug(client, 'Moscow 3') == 'moscow_3'
    assert get_slug(client, 'Moscoww') == 'moscoww'
    assert get_slug(client, 'SPB') == 'spb1'
    assert get_slug(client, ('a' * 50)) == ('a' * 49) + '1'
    assert get_slug(client, 'Perm') == 'perm'


def test_make_schedule_slug(client, cities, schedules):
    assert get_slug(
        client, 'Moscow',
        type=SCHEDULE_SLUG_TYPE,
        service=cities.moscow.id
    ) == 'moscow_moscow'

    assert get_slug(
        client, 'duty',
        type=SCHEDULE_SLUG_TYPE,
        service=cities.moscow.id
    ) == 'moscow_duty2'

    assert get_slug(
        client, 'moscow_duty_SMTH',
        type=SCHEDULE_SLUG_TYPE,
        service=cities.moscow.id
    ) == 'moscow_duty_smth'


def test_validate_slug(client, cities):
    assert validate_slug(client, 'moscow') == (False, 'moscow2')
    assert validate_slug(client, 'spb') == (False, 'spb1')
    assert validate_slug(client, 'spb1') == (True, None)
    assert validate_slug(client, ('a' * 25)) == (True, None)
    assert validate_slug(client, 'ufa') == (True, None)

    factories.RoleScopeFactory(slug='testing')
    assert validate_slug(client, 'moscow_testing') == (False, 'moscow_testing1')


def test_validate_duty_slug(client, schedules, cities):
    assert validate_slug(
        client, 'moscow_duty',
        type=SCHEDULE_SLUG_TYPE,
        service=cities.moscow.id
    ) == (False, 'moscow_duty2')

    assert validate_slug(
        client, 'moscow_duty',
        type=SCHEDULE_SLUG_TYPE,
        service=cities.spb.id
    ) == (False, 'spb_moscow_duty')

    assert validate_slug(
        client, 'duty',
        type=SCHEDULE_SLUG_TYPE,
        service=cities.moscow.id
    ) == (False, 'moscow_duty2')

    assert validate_slug(
        client, 'moscow_duty3',
        type=SCHEDULE_SLUG_TYPE,
        service=cities.moscow.id
    ) == (True, None)

    assert validate_slug(
        client, 'spb_duty',
        type=SCHEDULE_SLUG_TYPE,
        service=cities.spb.id
    ) == (True, None)

    assert validate_slug(
        client, 'spb_duty4',
        type=SCHEDULE_SLUG_TYPE,
        service=cities.spb.id
    ) == (True, None)

    assert validate_slug(
        client, 'duty',
        type=SCHEDULE_SLUG_TYPE,
        service=cities.spb.id
    ) == (False, 'spb_duty')


def test_make_slug_frontend_api(client):
    response = client.json.get(
        reverse('services:service_make_slug'),
        {'name': 'test_me'}
    )

    assert response.status_code == 200
    assert response.json()['content']['slug'] == 'test_me'


def test_validate_invalid_slug(client):
    match_message = {
        'ru': 'Неверный слаг. '
              'Слаг должен состоять из латинских букв в нижнем регистре, цифр, знаков подчеркивания или дефиса.',
        'en': 'Slug is invalid. '
              'Slug should contain only lower alphanumeric, underscore and hyphen characters.',
    }
    length_message = {
        'ru': 'Неверный слаг. '
              'Длина слага должна быть не более 50 символов.',
        'en': 'Slug is invalid. '
              'Slug length should not exceed 50 characters.',
    }
    digit_message = {
        'ru': 'Неверный слаг. '
              'Слаг не может состоять из одних цифр.',
        'en': 'Slug is invalid. '
              'Slug can\'t contain only digits.',
    }

    validate_invalid_slug(client, 'Moscow City', match_message)
    validate_invalid_slug(client, 'moskva*1', match_message)
    validate_invalid_slug(client, 'a' * 60, length_message)
    validate_invalid_slug(client, 'Москва', match_message)
    validate_invalid_slug(client, '111', digit_message)
