import pytest
import sform
from mock import patch

from django.conf import settings
from django.core.exceptions import ValidationError

from staff.departments.models import DepartmentRoles

from staff.lib.testing import StaffFactory, DepartmentFactory, DepartmentStaffFactory
from staff.preprofile.action_context import ActionContext
from staff.preprofile.forms.fields import contacts_fields, official_fields
from staff.preprofile.forms import ExternalConsultantForm
from staff.preprofile.forms.validation import validate_phone, validate_login_with_candidate_type
from staff.preprofile.login_validation import LOGIN_VALIDATION_ERROR
from staff.preprofile.models import CANDIDATE_TYPE, PREPROFILE_STATUS
from staff.preprofile.tests.utils import PreprofileFactory


@pytest.mark.parametrize(
    'phone',
    [
        '+79001234567',
        '+79060970790',
        '89060970790',
        '+79165331224',
        '+74951234567',
        '+7 495 123 456 7',
        '+7 495 123 45 67',
        '9165331224',
        '+ 7 (922) 555-1234',
        '+998 33 077 45 29',
        '+995511222351',
    ],
)
def test_valid_phones(phone):
    validate_phone(phone)


@pytest.mark.parametrize(
    'phone',
    [
        '+7900123456',
        '+790609707907',
        '343802',
    ],
)
def test_invalid_phones(phone):
    with pytest.raises(ValidationError, message='invalid_phone_number'):
        validate_phone(phone)


@pytest.mark.django_db()
def test_validate_login_with_candidate_type_former():
    StaffFactory(login='qweqweqwe', is_dismissed=True)
    validate_login_with_candidate_type('qweqweqwe', CANDIDATE_TYPE.FORMER_EMPLOYEE)


@pytest.mark.django_db()
def test_validate_login_with_candidate_type_former_doesnt_exist():
    with pytest.raises(ValidationError, message='login_doesnt_exist'):
        validate_login_with_candidate_type('qweqweqwe', CANDIDATE_TYPE.FORMER_EMPLOYEE)


@pytest.mark.django_db()
def test_validate_login_with_candidate_type_searches_in_active_preprofiles():
    test_login = 'qweqweqwe'
    PreprofileFactory(login=test_login, department=DepartmentFactory(), status=PREPROFILE_STATUS.NEW)
    with pytest.raises(ValidationError, message=LOGIN_VALIDATION_ERROR.LOGIN_NOT_UNIQUE):
        validate_login_with_candidate_type(test_login, CANDIDATE_TYPE.FORMER_EMPLOYEE)


@pytest.mark.django_db()
@patch('staff.preprofile.login_validation.validate_in_ldap')
@patch('staff.preprofile.login_validation.validate_for_dns')
def test_validate_login_with_candidate_type(ldap_mock, dns_mock):
    validate_login_with_candidate_type('qweqweqwe', CANDIDATE_TYPE.NEW_EMPLOYEE)


@pytest.mark.django_db()
def test_validate_login_with_candidate_type_external_doesnt_exist():
    DepartmentFactory(id=settings.EXT_DEPARTMENT_ID)
    with pytest.raises(ValidationError, message='login_doesnt_exist'):
        validate_login_with_candidate_type('qweqweqwe', CANDIDATE_TYPE.EXTERNAL_EMPLOYEE)


@pytest.mark.django_db
def test_validate_login_with_candidate_type_external_wrong_department():
    ext_dep = DepartmentFactory(id=settings.EXT_DEPARTMENT_ID)
    StaffFactory(login='qweqweqwe', is_dismissed=False, department=ext_dep)
    validate_login_with_candidate_type('qweqweqwe', CANDIDATE_TYPE.EXTERNAL_EMPLOYEE)


def test_email_validation_in_works():
    class Form(sform.SForm):
        email = contacts_fields.email()

    form = Form(data={'email': '123'})
    assert not form.is_valid()
    assert 'email' in form.errors_as_dict()['errors']


@pytest.mark.django_db()
@pytest.mark.parametrize('text', ['', ' ', '   \t '])
def test_position_staff_text_validation_in_form_handles_invalid_input(text):
    class Form(sform.SForm):
        position_staff_text = official_fields.position_staff_text()

    form = Form(data={'position_staff_text': text})
    assert not form.is_valid()
    assert 'position_staff_text' in form.errors_as_dict()['errors']


@pytest.mark.django_db()
@pytest.mark.parametrize('text', ['123', 'sdfsdf sdf', 'Йцукен'])
def test_position_staff_text_validation_in_form_handles_valid_input(text):
    class Form(sform.SForm):
        position_staff_text = official_fields.position_staff_text()

    form = Form(data={'position_staff_text': text})
    form.is_valid()
    assert 'position_staff_text' not in form.errors_as_dict()['errors']


@pytest.mark.django_db()
def test_ext_department_descendants_accepted_in_ext_employee_form():
    parent = DepartmentFactory(id=settings.EXT_DEPARTMENT_ID, url='ext')
    DepartmentFactory(url='ext_1', parent=parent)
    chief = StaffFactory()
    DepartmentStaffFactory(department=parent, staff=chief, role_id=DepartmentRoles.CHIEF.value)

    form = ExternalConsultantForm(
        data={'department': 'ext_1'},
        base_initial={'action_context': ActionContext(None, chief)},
    )
    assert not form.is_valid()
    assert 'department' not in form.errors_as_dict()['errors']


@pytest.mark.django_db()
def test_that_other_than_ext_department_descendants_not_accepted_in_ext_employee_form():
    parent = DepartmentFactory(id=settings.EXT_DEPARTMENT_ID, url='ext')
    DepartmentFactory(id=settings.YAMONEY_DEPARTMENT_ID, url='yam')
    chief = StaffFactory()
    DepartmentStaffFactory(department=parent, staff=chief, role_id=DepartmentRoles.CHIEF.value)

    form = ExternalConsultantForm(
        data={'department': 'yam'},
        base_initial={'action_context': ActionContext(None, chief)},
    )
    assert not form.is_valid()
    assert 'department' in form.errors_as_dict()['errors']


@pytest.mark.django_db()
def test_non_chief_fill_not_pass_validation_for_department_in_ext_employee_form():
    parent = DepartmentFactory(id=settings.EXT_DEPARTMENT_ID, url='ext')
    DepartmentFactory(url='ext_1', parent=parent)
    chief = StaffFactory()

    form = ExternalConsultantForm(
        data={'department': 'ext_1'},
        base_initial={'action_context': ActionContext(None, chief)},
    )
    assert not form.is_valid()
    assert 'department' in form.errors_as_dict()['errors']


@pytest.mark.django_db()
@pytest.mark.parametrize("role", [DepartmentRoles.CHIEF.value, DepartmentRoles.DEPUTY.value])
def test_crown_accepted_in_ext_employee_form(role):
    parent = DepartmentFactory(id=settings.EXT_DEPARTMENT_ID, url='ext')
    dep = DepartmentFactory(url='ext_1', parent=parent)
    chief = StaffFactory()
    DepartmentStaffFactory(department=dep, staff=chief, role_id=role)

    form = ExternalConsultantForm(
        data={'department': 'ext_1'},
        base_initial={'action_context': ActionContext(None, chief)},
    )
    assert not form.is_valid()
    assert 'department' not in form.errors_as_dict()['errors']
