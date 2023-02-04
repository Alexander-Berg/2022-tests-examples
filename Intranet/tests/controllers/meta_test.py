import pytest

from staff.gap.controllers.meta import find_person


@pytest.mark.django_db
def test_find_dismissed_person(company):
    person_model = company.persons['dep1-person']
    person_model.is_dismissed = True
    person_model.save()

    person = find_person(person_model.login, company.persons['dep2-person'])
    assert not person['first_name']
    assert not person['last_name']


@pytest.mark.django_db
def test_find_person_names_correct(company):
    person_model = company.persons['dep1-person']
    person = find_person(person_model.login, company.persons['dep2-person'])
    assert person['first_name'] == person_model.first_name
    assert person['last_name'] == person_model.last_name
