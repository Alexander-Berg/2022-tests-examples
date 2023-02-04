import pytest
import json

from django.conf import settings
from django.core.urlresolvers import reverse

from staff.person.models import (
    Staff,
    GENDER,
    EDU_STATUS,
    EDU_DIRECTION,
    FAMILY_STATUS,
    TSHIRT_SIZE,
)

from staff.lib.testing import (
    StaffFactory,
    UserFactory,
)

FORM = 'other'


def post(client, data, test_person):
    response = client.post(
        reverse('profile:edit-other', kwargs={'login': test_person.login}),
        json.dumps(data),
        content_type='application/json'
    )
    assert response.status_code == 200, response.content
    return json.loads(response.content)


@pytest.mark.django_db()
def test_edit_other(client):
    test_person = StaffFactory(
        user=UserFactory(is_superuser=True),
        login=settings.AUTH_TEST_USER,
    )
    reverse('profile:edit-other', kwargs={'login': test_person.login})

    active_about = 'Активе абоут'
    active_gender = GENDER.MALE
    active_birthday = '1995-04-03'
    active_edu_status = EDU_STATUS.INCOMPLETE
    active_edu_direction = EDU_DIRECTION.LIBERAL
    active_edu_place = 'Активе еду пласе'
    active_edu_place_en = 'Active edu place en'
    active_edu_date = '2003-04-05'
    active_address = 'Активе аддресс'
    active_address_en = 'Active address en'
    active_family_status = FAMILY_STATUS.SINGLE
    active_children = 0
    active_tshirt_size = TSHIRT_SIZE.L
    active_mobile_phone_model = 'Active mobile phone model'

    def check_person():
        test_person = Staff.objects.get(login=settings.AUTH_TEST_USER)
        assert test_person.about == active_about
        assert test_person.gender == active_gender
        assert test_person.birthday.isoformat() == active_birthday
        assert test_person.edu_status == active_edu_status
        assert test_person.edu_direction == active_edu_direction
        assert test_person.edu_place == active_edu_place
        assert test_person.edu_place_en == active_edu_place_en
        assert test_person.edu_date.isoformat() == active_edu_date
        assert test_person.address == active_address
        assert test_person.address_en == active_address_en
        assert test_person.family_status == active_family_status
        assert test_person.children == active_children
        assert test_person.tshirt_size == active_tshirt_size
        assert test_person.mobile_phone_model == active_mobile_phone_model

    data = {'other': [
        {
            'about': active_about,
            'gender': active_gender,
            'birthday': active_birthday,
            'edu_status': active_edu_status,
            'edu_direction': active_edu_direction,
            'edu_place': active_edu_place,
            'edu_place_en': active_edu_place_en,
            'edu_date': active_edu_date,
            'address': active_address,
            'address_en': active_address_en,
            'family_status': active_family_status,
            'children': active_children,
            'tshirt_size': active_tshirt_size,
            'mobile_phone_model': active_mobile_phone_model,
        },
    ]}

    answer = post(client, data, test_person)
    assert answer == {'target': {}}

    check_person()

    data = {FORM: [
        {
            'gender': 'WRONG GENDER',
            'birthday': 'a-b-c',
            'edu_status': 'WRONG EDU STATUS',
            'edu_direction': 'WRONG EDU DIRECTION',
            'edu_date': 'x-y-z',
            'family_status': 'WRONG FAMILY STATUS',
            'tshirt_size': 'WRONG TSHIRT SIZE',
        },
    ]}

    answer = post(client, data, test_person)
    assert answer == {'errors': {FORM: {
        '0': {
            'gender': [{
                'error_key': 'choice-field-invalid_choice',
                'params': {'value': 'WRONG GENDER'},
            }],
            'birthday': [{'error_key': 'date-field-invalid'}],
            'edu_status': [{
                'error_key': 'choice-field-invalid_choice',
                'params': {'value': 'WRONG EDU STATUS'},
            }],
            'edu_direction': [{
                'error_key': 'choice-field-invalid_choice',
                'params': {'value': 'WRONG EDU DIRECTION'},
            }],
            'edu_date': [{'error_key': 'date-field-invalid'}],
            'family_status': [{
                'error_key': 'choice-field-invalid_choice',
                'params': {'value': 'WRONG FAMILY STATUS'},
            }],
            'tshirt_size': [{
                'error_key': 'choice-field-invalid_choice',
                'params': {'value': 'WRONG TSHIRT SIZE'},
            }],
        }
    }}}

    check_person()


@pytest.mark.django_db()
def test_edit_other_optional(client):
    test_person = StaffFactory(
        user=UserFactory(is_superuser=True),
        login=settings.AUTH_TEST_USER,
    )

    active_about = None
    active_gender = GENDER.MALE
    active_birthday = None
    active_edu_status = None
    active_edu_direction = None
    active_edu_place = None
    active_edu_place_en = None
    active_edu_date = None
    active_address = None
    active_address_en = None
    active_family_status = None
    active_children = None
    active_tshirt_size = None
    active_mobile_phone_model = None

    def check_person():
        test_person = Staff.objects.get(login=settings.AUTH_TEST_USER)
        assert test_person.about == ''
        assert test_person.gender == active_gender
        assert test_person.birthday is None
        assert test_person.edu_status == ''
        assert test_person.edu_direction == ''
        assert test_person.edu_place == ''
        assert test_person.edu_place_en == ''
        assert test_person.edu_date is None
        assert test_person.address == ''
        assert test_person.address_en == ''
        assert test_person.family_status == ''
        assert test_person.children is None
        assert test_person.tshirt_size == ''
        assert test_person.mobile_phone_model == ''

    data = {FORM: [
        {
            'about': active_about,
            'gender': active_gender,
            'birthday': active_birthday,
            'edu_status': active_edu_status,
            'edu_direction': active_edu_direction,
            'edu_place': active_edu_place,
            'edu_place_en': active_edu_place_en,
            'edu_date': active_edu_date,
            'address': active_address,
            'address_en': active_address_en,
            'family_status': active_family_status,
            'children': active_children,
            'tshirt_size': active_tshirt_size,
            'mobile_phone_model': active_mobile_phone_model,
        },
    ]}

    answer = post(client, data, test_person)
    assert answer == {'target': {}}

    check_person()
