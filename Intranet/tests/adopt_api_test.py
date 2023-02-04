import json
import random
from datetime import date

import pytest
from mock import patch, Mock

from django.conf import settings

from staff.audit.factory import AUDIT_ACTIONS
from staff.audit.models import Log
from staff.lib.testing import FloorFactory, StaffFactory, DepartmentFactory, TableFactory, OrganizationFactory
from staff.person.models import (
    Staff,
    VerifyCode,
    VALIDATION_TYPES,
    VERIFY_STATE,
    VERIFY_CODE_OBJECT_TYPES,
    GENDER,
    LANG,
)
from staff.person_profile.tests.edit_views.contacts_test import ContactTypeFactory

from staff.preprofile.adopt_api import adopt_preprofile, adoption_data
from staff.preprofile.models import FORM_TYPE, PREPROFILE_STATUS, CANDIDATE_TYPE
from staff.preprofile.tests.utils import PreprofileFactory


def _create_preprofile(**kwargs):
    floor = FloorFactory()
    StaffFactory(
        login='robot-femida',
        user__username='robot-femida',
        department=DepartmentFactory(url='virtual-femida'),
    )
    vc = VerifyCode.objects.create(
        state=VERIFY_STATE.VERIFIED,
        object_type=VERIFY_CODE_OBJECT_TYPES.OFFER,
        object_id=1,
        phone_number='+7 700 123 1230',
    )
    data = dict(
        femida_offer_id=vc.object_id,
        login='imperator',
        form_type=FORM_TYPE.EMPLOYEE,
        status=PREPROFILE_STATUS.READY,
        guid='guid',
        uid='112',
        gender=GENDER.MALE,
        education_direction=None,
        education_status=None,
        email='some@some',
        first_name='Имя',
        last_name='Фамилия',
        middle_name='Отчество',
        first_name_en='First',
        last_name_en='Last',
        table=TableFactory(floor=floor),
        office=floor.office,
        lang_ui='ru',
        crm_alias='crm_alias',
        phone=vc.phone_number,
        is_eds_phone_verified=True,
        approved_by=None,
    )
    data.update(kwargs)
    return PreprofileFactory(**data)


def check_staff(s, preprofile):
    assert s.login == preprofile.login
    assert s.first_name == preprofile.first_name
    assert s.first_name_en == preprofile.first_name_en
    assert s.last_name == preprofile.last_name
    assert s.last_name_en == preprofile.last_name_en
    assert s.middle_name == preprofile.middle_name
    assert s.birthday == preprofile.birthday
    assert s.gender == preprofile.gender
    assert s.department == preprofile.department
    assert s.office == preprofile.office
    assert s.organization == preprofile.organization
    assert s.position == preprofile.position_staff_text
    assert s.employment == preprofile.employment_type
    assert s.home_email == preprofile.email
    assert s.table == preprofile.table
    assert s.lang_ui == preprofile.lang_ui
    assert s.login_crm == preprofile.crm_alias


def check_digital_sign(s, preprofile):
    phones = s.phones.filter(number=preprofile.phone, for_digital_sign=True)
    phone = phones.first()
    assert phone is not None

    codes = list(phone.codes.filter(
        person=s,
        state=VERIFY_STATE.CONFIRMED,
        object_type=VERIFY_CODE_OBJECT_TYPES.OFFER,
        object_id=preprofile.femida_offer_id,
    ))
    assert len(codes) == 1
    assert Log.objects.filter(
        who__username='robot-femida',
        primary_key=codes[0].id,
        action=AUDIT_ACTIONS.CONNECT_PHONE_IN_OEBS_TO_DS,
        data=json.dumps({'status': True}),
    ).exists()


@pytest.mark.django_db()
@patch('staff.person_profile.controllers.digital_sign._connect_phone_in_oebs', lambda l, n: (True, ''))
def test_employee_staff_creation(company, stub_requests, robot_staff_user, achievements):
    ContactTypeFactory(id=1, validation_type=VALIDATION_TYPES.EMAIL)
    preprofile = _create_preprofile(
        department_id=company.yandex.id,
    )

    adopt_preprofile(preprofile, robot_staff_user.staff)

    s = Staff.objects.get(login='imperator')
    assert s
    assert s.uid == preprofile.uid
    assert s.guid == preprofile.guid
    assert s.work_email.startswith(preprofile.login)
    assert s.work_email.endswith(preprofile.email_domain)
    check_staff(s, preprofile)
    check_digital_sign(s, preprofile)


@pytest.mark.django_db()
@patch('staff.person.controllers.effects.unblock_login_in_passport', Mock)
@patch('staff.person_profile.controllers.digital_sign._connect_phone_in_oebs', lambda l, n: (True, ''))
def test_employee_staff_restore(company, stub_requests, robot_staff_user):
    ContactTypeFactory(id=1, validation_type=VALIDATION_TYPES.EMAIL)
    StaffFactory(login='imperator', is_dismissed=True, organization=OrganizationFactory())
    preprofile = _create_preprofile(
        department_id=company.yandex.id,
        candidate_type=CANDIDATE_TYPE.FORMER_EMPLOYEE,
        organization=OrganizationFactory(),
        date_completion_internship=date.today(),
    )

    adopt_preprofile(preprofile, robot_staff_user.staff)

    s = Staff.objects.get(login='imperator')
    assert not s.is_dismissed
    assert s.date_completion_internship == date.today()
    check_staff(s, preprofile)
    check_digital_sign(s, preprofile)


@pytest.mark.django_db()
@patch('staff.person.controllers.effects.unblock_login_in_passport', Mock)
@patch('staff.person_profile.controllers.digital_sign._connect_phone_in_oebs', lambda l, n: (True, ''))
def test_employee_external_to_internal(mock_side_effects, company, robot_staff_user, monkeypatch):
    ContactTypeFactory(id=1, validation_type=VALIDATION_TYPES.EMAIL)
    ext_dep = DepartmentFactory()
    yandex_dep = company.yandex
    monkeypatch.setattr(settings, 'YANDEX_DEPARTMENT_ID', yandex_dep.id)
    monkeypatch.setattr(settings, 'EXT_DEPARTMENT_ID', ext_dep.id)
    StaffFactory(login='imperator', department=ext_dep, organization=OrganizationFactory())
    preprofile = _create_preprofile(
        department_id=company.dep1.id,
        candidate_type=CANDIDATE_TYPE.EXTERNAL_EMPLOYEE,
        organization=OrganizationFactory(),
        date_completion_internship=date.today(),
    )

    adopt_preprofile(preprofile, robot_staff_user.staff)

    s = Staff.objects.get(login='imperator')
    assert s.date_completion_internship == date.today()
    check_staff(s, preprofile)
    check_digital_sign(s, preprofile)


@pytest.mark.django_db()
@pytest.mark.parametrize(
    'language, auto_translate',
    [
        (LANG.EN, True),
        (LANG.RU, False),
        (LANG.TR, True),
    ],
)
def test_adoption_data(company, language, auto_translate):
    preprofile = _create_preprofile(department_id=company.yandex.id, lang_ui=language)
    domain = f'domain{random.random()}'

    with patch('staff.preprofile.adopt_api.to_staff_email_domain', return_value=domain) as to_staff_email_domain_patch:
        result = adoption_data(preprofile)
        to_staff_email_domain_patch.assert_called_once_with(preprofile.email_domain)

    assert len(result) == 20
    assert result['position'] == preprofile.position_staff_text
    assert result['employment'] == preprofile.employment_type
    assert result['preprofile_id'] == preprofile.id
    assert result['home_email'] == preprofile.email or ''
    assert result['domain'] == domain
    assert result['login_crm'] == preprofile.crm_alias or ''
    assert result['auto_translate'] == auto_translate

    for key, value in result.items():
        if key in ('position', 'employment', 'preprofile_id', 'home_email', 'domain', 'login_crm', 'auto_translate'):
            continue

        expected = getattr(preprofile, key)

        if key == 'middle_name':
            expected = expected or ''

        assert value == expected
