import pytest
from staff.trip_questionary.controller.operations import SingleStOperation, PersonalStOperation

from staff.person.models import Passport, Visa, DiscountCard

from staff.lib.testing import (
    PassportFactory,
    VisaFactory,
    DiscountCardFactory,
    PASSPORT_TYPE,
)


@pytest.fixture
def person_and_documents(company):
    person = company['persons']['dep12-person']
    passports = [
        PassportFactory(
            person=person,
            description='test internal passport',
            doc_type=PASSPORT_TYPE.INTERNAL,
            issue_country='Россия',
            country_code='ru',
            number='ABC 3231111',
            first_name='Первое',
            last_name='Второе',
            middle_name='Среднее',
            is_active=True,
        ),
        PassportFactory(
            person=person,
            description='test foreign inactive passport',
            doc_type=PASSPORT_TYPE.FOREIGN,
            issue_country='Ukraine',
            country_code='ru',
            number='DEF 1111674',
            first_name='First',
            last_name='Last',
            middle_name='Middle',
            is_active=False,
        ),
    ]
    visas = [
        VisaFactory(
            person=person,
            description='test active visa',
            country='Thailand',
            number='0987654321234567890',
            is_multiple=False,
            is_active=True,
        ),
        VisaFactory(
            person=person,
            description='test disabled visa',
            country='Urkaina',
            number='380384832254',
            is_multiple=False,
            is_active=False,
        ),
    ]
    discounts = [
        DiscountCardFactory(
            person=person,
            description='test enabled discount',
            number='2387943879423897',
            is_active=True,
        ),
        DiscountCardFactory(
            person=person,
            description='test disabled discount',
            number='90380324892389747682368723sfd',
            is_active=False,
        ),
    ]
    return person, {'passports': passports, 'visas': visas, 'discounts': discounts}


@pytest.mark.parametrize('operation_class', [SingleStOperation, PersonalStOperation])
@pytest.mark.django_db
def test_personal_data_in_context(person_and_documents, operation_class):
    employee, docs = person_and_documents

    person_documents = operation_class.get_personal_data(employee.login)

    assert len(person_documents['passports']) == 1
    assert len(person_documents['visas']) == 1
    assert len(person_documents['passports']) == 1

    passport = person_documents['passports'][0]
    assert passport == Passport.objects.filter(id=passport['id']).values().get()

    visa = person_documents['visas'][0]
    assert visa == Visa.objects.filter(id=visa['id']).values().get()

    discountcard = person_documents['discountcards'][0]
    assert discountcard == DiscountCard.objects.filter(id=discountcard['id']).values().get()
