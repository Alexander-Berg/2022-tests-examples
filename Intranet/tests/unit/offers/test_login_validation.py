import pytest

from django.conf import settings
from unittest.mock import patch, Mock

from intranet.femida.src.offers.choices import EMPLOYEE_TYPES, OFFER_STATUSES
from intranet.femida.src.offers.login.validators import LoginValidator, LoginValidationError

from intranet.femida.tests import factories as f
from intranet.femida.tests.mock.offers import FakeNewhireAPI
from intranet.femida.tests.utils import assert_not_raises


pytestmark = pytest.mark.django_db


def check_login_validation(success, error_code=None):
    if success:
        return assert_not_raises(LoginValidationError)
    else:
        return pytest.raises(LoginValidationError, match=error_code)


def test_empty_login():
    """
    Проверяет, что пустой логин всегда невалиден
    """
    validator = LoginValidator()
    with check_login_validation(success=False, error_code='login_error_unsuitable'):
        validator.validate('')


@patch('intranet.femida.src.offers.login.validators.NewhireAPI', FakeNewhireAPI)
@pytest.mark.parametrize('length,success', (
    (settings.MIN_LOGIN_LENGTH, True),
    (settings.MAX_LOGIN_LENGTH, True),
    (settings.MIN_LOGIN_LENGTH - 1, False),
    (settings.MAX_LOGIN_LENGTH + 1, False),
))
def test_login_length(length, success):
    """
    Проверяет, что корректно работает валидация длины логина
    """
    login = 'l' * length
    validator = LoginValidator()

    with check_login_validation(success, 'login_error_bad_length'):
        validator.validate(login)


@patch('intranet.femida.src.offers.login.validators.NewhireAPI', FakeNewhireAPI)
@pytest.mark.parametrize('status,success', (
    (OFFER_STATUSES.draft, False),
    (OFFER_STATUSES.on_approval, False),
    (OFFER_STATUSES.closed, False),
    (OFFER_STATUSES.deleted, True),
    (OFFER_STATUSES.rejected, True),
))
def test_login_uniqueness_in_offers(status, success):
    """
    Проверяет, что при валидации учитывается наличие в БД
    неудалённых/неотменённых офферов с конфликтующим логином,
    а удалённые/отменённые как раз игнорируются
    """
    login = 'login'
    f.OfferFactory(status=status, username=login)
    validator = LoginValidator()

    with check_login_validation(success, 'login_error_not_unique'):
        validator.validate(login)


@patch('intranet.femida.src.offers.login.validators.NewhireAPI', FakeNewhireAPI)
@pytest.mark.parametrize('status', (
    OFFER_STATUSES.draft,
    OFFER_STATUSES.on_approval,
    OFFER_STATUSES.closed,
))
def test_login_uniqueness_in_offers_with_exclude(status):
    """
    Проверяет, что корректно работает исключение офферов при валидации логина.
    Актуально для ситуаций, когда пользователь в оффере выставляет
    тот же логин, что уже установлен сейчас
    """
    login = 'login'
    offer = f.OfferFactory(status=status, username=login)
    validator = LoginValidator(exclude_offer_ids=[offer.id])

    with check_login_validation(success=True):
        validator.validate(login)


@pytest.mark.parametrize('status_code,error_code', (
    (400, 'login_error_unsuitable'),
    (500, 'login_error_newhire_check_failed'),
))
def test_login_in_newhire_failure(status_code, error_code):
    """
    Проверяет, что корректно работает валидация логина в Наниматоре
    """
    validator = LoginValidator()
    check_login = Mock()
    check_login.return_value = {}, status_code

    with patch('intranet.femida.src.offers.login.validators.NewhireAPI.check_login', check_login):
        with check_login_validation(success=False, error_code=error_code):
            validator.validate('login')


@pytest.mark.parametrize('is_dismissed,success', (
    (True, True),
    (False, False),
    (None, False),
))
def test_former_employee_login(is_dismissed, success):
    """
    Проверяет, что корректно валидируется логин бывшего сотрудника
    """
    login = 'login'
    validator = LoginValidator(EMPLOYEE_TYPES.former)
    if is_dismissed is not None:
        f.UserFactory(username=login, is_dismissed=is_dismissed)

    with check_login_validation(success, 'login_error_not_former_employee'):
        validator.validate(login)


@pytest.mark.parametrize('is_dismissed,success', (
    (True, False),
    (False, True),
    (None, False),
))
def test_current_employee_login(is_dismissed, success):
    """
    Проверяет, что корректно валидируется логин действующего сотрудника
    """
    login = 'login'
    validator = LoginValidator(EMPLOYEE_TYPES.current)
    if is_dismissed is not None:
        f.UserFactory(
            username=login,
            is_dismissed=is_dismissed,
            department__id=settings.EXTERNAL_DEPARTMENT_ID,
        )

    with check_login_validation(success, 'login_error_not_current_employee'):
        validator.validate(login)


@pytest.mark.parametrize('root_department_id,employee_type,success', (
    # ДВК - действующий внешний консультант
    # Внешний консультант выводится как ДВК - ок
    (settings.EXTERNAL_DEPARTMENT_ID, EMPLOYEE_TYPES.current, True),
    # Сотрудник Яндекса выводится как ДВК - не ок
    (settings.YANDEX_DEPARTMENT_ID, EMPLOYEE_TYPES.current, False),
    # Сотрудник Яндекса ротируется - ок
    (settings.YANDEX_DEPARTMENT_ID, EMPLOYEE_TYPES.rotation, True),
    # Сотрудник Outstaff ротирутеся - ок
    (settings.OUTSTAFF_DEPARTMENT_ID, EMPLOYEE_TYPES.rotation, True),
    # Внешний консультант ротируется - не ок
    (settings.EXTERNAL_DEPARTMENT_ID, EMPLOYEE_TYPES.rotation, False),
    # Сотрудник Яндекса выводится переводом стажёра в штат - ок
    (settings.YANDEX_DEPARTMENT_ID, EMPLOYEE_TYPES.intern, True),
    # Сотрудник Outstaff выводится переводом стажёра в штат - ок
    (settings.OUTSTAFF_DEPARTMENT_ID, EMPLOYEE_TYPES.intern, True),
    # Внешний консультант выводится переводом стажёра в штат - не ок
    (settings.EXTERNAL_DEPARTMENT_ID, EMPLOYEE_TYPES.intern, False),
))
def test_current_employee_login_by_department(root_department_id, employee_type, success):
    """
    Проверяет, что корректно валидируется логин действующего сотрудника
    в зависимости от подразделения и типа вывода
    """
    login = 'login'
    validator = LoginValidator(employee_type)
    department = f.DepartmentFactory(ancestors=[root_department_id])
    f.UserFactory(username=login, department=department)

    with check_login_validation(success, 'login_error_not_current_employee'):
        validator.validate(login)
