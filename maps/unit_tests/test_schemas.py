import pytest
from aiohttp import web

from maps.b2bgeo.pipedrive_gate.lib.entities.schemas import Organization, Person, Deal


def test_empty_dadata_results_in_empty_org():
    assert Organization.from_dadata(None) is None
    assert Organization.from_dadata({}) is None


def test_org_name_shall_be_string():
    assert Organization.from_dadata({'value': None}) is None
    assert Organization.from_dadata({'value': {}}) is None
    assert Organization.from_dadata({'value': 1}) is None
    assert Organization.from_dadata({'value': 'test'}) == Organization(name='test')


def test_person_name_shall_be_string():
    with pytest.raises(web.HTTPBadRequest):
        Person.from_survey({'Name': None})

    with pytest.raises(web.HTTPBadRequest):
        Person.from_survey({'Name': {}})

    with pytest.raises(web.HTTPBadRequest):
        Person.from_survey({'Name': 1})

    assert Person.from_survey({'Name': 'test'}) == Person(name='test')


def test_company_name():
    deal = Deal.from_survey({'Company name': 'Yandex'}, 'FormID')
    assert deal.title == 'Yandex FormID'
    assert deal.company_name == 'Yandex'

    with pytest.raises(web.HTTPBadRequest) as error:
        Deal.from_survey({}, 'FormID')
    assert error.value.reason == "Either company name or full name should be provided"


def test_full_name():
    fields = {'Full name': 'Pushkin'}
    deal = Deal.from_survey(fields, 'FormID')
    assert deal.title == 'Pushkin Partner FormID'
    assert deal.company_name == 'Pushkin Partner'
    person = Person.from_survey(fields)
    assert person.name == 'Pushkin'

    fields = {'Company name': 'Yandex', 'Full name': 'Pushkin'}
    deal = Deal.from_survey(fields, 'FormID')
    assert deal.title == 'Yandex FormID'
    assert deal.company_name == 'Yandex'
    person = Person.from_survey(fields)
    assert person.name == 'Pushkin'
