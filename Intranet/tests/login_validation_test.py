import factory
import pytest

from django.core.exceptions import ValidationError

from staff.lib.testing import StaffFactory
from staff.maillists.models import List

from staff.preprofile.login_validation import (
    validate_login_length,
    validate_login_syntax,
    validate_likeness_in_preprofiles,
    validate_likeness_in_staff,
    validate_for_maillist,
    validate_for_illegal_login,
    validate_by_uniqueness,
    validate_by_uniqueness_in_preprofiles,
    LOGIN_VALIDATION_ERROR,
)
from staff.preprofile.models import IllegalLogin
from staff.preprofile.tests.utils import PreprofileFactory


class IllegalLoginFactory(factory.DjangoModelFactory):
    class Meta:
        model = IllegalLogin


class ListFactory(factory.DjangoModelFactory):
    class Meta:
        model = List


@pytest.mark.parametrize('is_robot', [False, True])
def test_that_short_login_is_invalid(is_robot):
    with pytest.raises(ValidationError, message=LOGIN_VALIDATION_ERROR.BAD_LENGTH):
        validate_login_length('abc', is_robot)


def test_that_long_login_is_invalid_for_human():
    with pytest.raises(ValidationError, message=LOGIN_VALIDATION_ERROR.BAD_LENGTH):
        validate_login_length('deadbeafdeadbeaf', False)


def test_that_long_login_is_invalid_for_robot():
    with pytest.raises(ValidationError, message=LOGIN_VALIDATION_ERROR.BAD_LENGTH):
        validate_login_length('deadbeafdeadbeafdeadbeaf', False)


@pytest.mark.parametrize('char', '`_!@#$%^&*()+=[]{}\\|/?,.<>')
def test_that_special_characters_is_invalid(char):
    with pytest.raises(ValidationError, message=LOGIN_VALIDATION_ERROR.BAD_SYMBOLS):
        validate_login_syntax('qweqwe'+char)


def test_that_login_from_digits_is_invalid_for_human():
    with pytest.raises(ValidationError, message=LOGIN_VALIDATION_ERROR.BAD_SYMBOLS):
        validate_login_syntax('1234567890')


def test_that_login_cannot_contain_consequent_minuses():
    with pytest.raises(ValidationError, message=LOGIN_VALIDATION_ERROR.BAD_SYMBOLS):
        validate_login_syntax('qweqwe--qwe')


def test_that_login_cannot_starts_with_digit():
    with pytest.raises(ValidationError, message=LOGIN_VALIDATION_ERROR.BAD_SYMBOLS):
        validate_login_syntax('0qweqweqwe')


def test_that_login_cannot_starts_with_minus():
    with pytest.raises(ValidationError, message=LOGIN_VALIDATION_ERROR.BAD_SYMBOLS):
        validate_login_syntax('-qweqweqwe')


def test_that_login_cannot_ends_with_minus():
    with pytest.raises(ValidationError, message=LOGIN_VALIDATION_ERROR.BAD_SYMBOLS):
        validate_login_syntax('qweqweqwe-')


def test_that_login_cannot_endswith_minusapi():
    with pytest.raises(ValidationError, message=LOGIN_VALIDATION_ERROR.BAD_SYMBOLS):
        validate_login_syntax('robot-tvm-api')


@pytest.mark.django_db()
def test_that_login_cannot_be_in_blacklist_by_exact_match():
    IllegalLoginFactory(login='qweqweqwe')

    with pytest.raises(ValidationError, message=LOGIN_VALIDATION_ERROR.LOGIN_IN_BLACKLIST):
        validate_for_illegal_login('qweqweqwe')


@pytest.mark.django_db()
def test_that_login_cannot_be_in_blacklist_by_fuzzy_match():
    IllegalLoginFactory(login='www', exact_match=False)

    with pytest.raises(ValidationError, message=LOGIN_VALIDATION_ERROR.LOGIN_IN_BLACKLIST):
        validate_for_illegal_login('wwwHELLO')


@pytest.mark.django_db()
def test_that_login_cannot_be_used_twice():
    StaffFactory(login='qweqweqwe')

    with pytest.raises(ValidationError, message=LOGIN_VALIDATION_ERROR.LOGIN_NOT_UNIQUE):
        validate_by_uniqueness('qweqweqwe')


@pytest.mark.django_db()
def test_that_login_cannot_be_used_twice_in_person_forms(company):
    PreprofileFactory(
        login='qweqweqwe',
        department_id=company.yandex.id,
    )

    with pytest.raises(ValidationError, message=LOGIN_VALIDATION_ERROR.LOGIN_NOT_UNIQUE):
        validate_by_uniqueness_in_preprofiles('qweqweqwe')


@pytest.mark.django_db()
def test_that_login_cannot_be_used_cause_of_likeness_in_forms(company):
    PreprofileFactory(
        login='qweqwe1',
        department_id=company.yandex.id,
    )

    with pytest.raises(ValidationError, message=LOGIN_VALIDATION_ERROR.LOGIN_LIKENESS):
        validate_likeness_in_preprofiles('qweqwe2', None)


@pytest.mark.django_db()
def test_that_login_cannot_be_used_cause_of_likeness_in_staff():
    StaffFactory(login='qweqwe1')

    with pytest.raises(ValidationError, message=LOGIN_VALIDATION_ERROR.LOGIN_LIKENESS):
        validate_likeness_in_staff('qweqwe2')


@pytest.mark.django_db()
def test_that_login_should_differs_from_maillist_name():
    ListFactory(name='q.weqwe-1')

    with pytest.raises(ValidationError, message=LOGIN_VALIDATION_ERROR.LOGIN_MAILLIST_CONFLICT):
        validate_for_maillist('q-weqwe.1')
