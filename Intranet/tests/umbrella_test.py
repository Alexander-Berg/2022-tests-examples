from json import loads

import pytest

from django.core.urlresolvers import resolve

from staff.lib.testing import ValueStreamFactory
from staff.umbrellas.tests.factories import UmbrellaFactory

JQUERY_CODE = b'jQuery1234567890123456789_1234567890123'


@pytest.mark.django_db
def test_autocomplete_umbrella(rf, company):
    url = '/center/api/autocomplete/multi/'

    value_stream = ValueStreamFactory()
    other_umbrellas = [UmbrellaFactory() for _ in range(3)]
    umbrellas = [UmbrellaFactory(value_stream=value_stream) for _ in range(3)]

    kwargs = {
        'callback': JQUERY_CODE,
        'types': 'umbrella',
        'q': '',
        'umbrella__value_stream__url': value_stream.url,
    }

    request = rf.get(url, kwargs)
    request.user = company.persons['dep12-chief'].user
    request.LANGUAGE_CODE = 'ru'

    response = resolve(url).func(request)

    assert response.status_code == 200, response.content
    assert response.content.startswith(JQUERY_CODE)
    result = loads(response.content.replace(JQUERY_CODE, b'')[1:-1])
    assert len(result) == len(umbrellas)
    actual_names = {x['name'] for x in result}
    expected_names = {x.name for x in umbrellas}
    assert actual_names == expected_names
    assert all(x.name not in actual_names for x in other_umbrellas)
