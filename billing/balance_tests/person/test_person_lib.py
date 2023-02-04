# coding=utf-8

"""
Тесты небольших функций из balance/person.py
"""

import pytest

from balance import exc
from balance.mapper import Person
from balance.person import is_new_person, set_detail, normalize_request
from balance.simulation import SimulationReport
from tests.balance_tests.person.person_common import create_person, create_client


def test_is_new_person(session, client):
    person = Person(client, "ph")
    assert is_new_person(person)
    session.add(person)
    assert is_new_person(person)
    session.flush()
    assert is_new_person(person) is False


def test_set_detail_translate(session):
    person = create_person(session)
    set_detail(person, 'fname', (u'xxx' + u'\u00a0' + u'666'))
    assert person.fname == "xxx 666"


@pytest.mark.parametrize('req, normalized_req', [
    ({'city_name': '   \t\nwhitespace\r\f\v  '}, {'city-name': 'whitespace'}),
    ({'city_name': '  double  spaced  '}, {'city-name': 'double spaced'}),
    ({'city_name': 'name'}, {'city-name': 'name'}),
    ({'city-name': 'name'}, {'city-name': 'name'}),
    ({'s-city': 'name'}, {'s-city': 'name', 'city': 'name'}),
    ({'s-_city': 'name'}, {'s--city': 'name', '-city': 'name'}),
    ({'inn': None}, {'inn': None})
])
def test_normalize_person_params(req, normalized_req):
    req = normalize_request(req, SimulationReport())
    assert req == normalized_req


def test_normalize_person_params_failed():
    class A:
        pass

    with pytest.raises(exc.INVALID_PARAM) as exc_info:
        normalize_request({'inn': A()}, SimulationReport())
    assert exc_info.value.msg == "Invalid parameter for function: Can't normalize value."
