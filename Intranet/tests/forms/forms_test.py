from datetime import datetime, date

import pytest

from django.conf import settings
from django.contrib.auth.models import Permission

from staff.lib.testing import DepartmentFactory
from staff.person.models import GENDER, LANG

from staff.preprofile.models import EMAIL_DOMAIN, CITIZENSHIP, PREPROFILE_STATUS, CANDIDATE_TYPE, FORM_TYPE
from staff.preprofile.repository import Repository
from staff.preprofile.tests.utils import PreprofileFactory


readonly_by_status = {
    PREPROFILE_STATUS.NEW: False,
    PREPROFILE_STATUS.PREPARED: False,
    PREPROFILE_STATUS.APPROVED: True,
    PREPROFILE_STATUS.READY: True,
    PREPROFILE_STATUS.CLOSED: True,
    PREPROFILE_STATUS.CANCELLED: True,
}

always_readonly_fields = {
    'uid',
    'guid',
    'created_at',
    'modified_at',
    'status',
    'chief',
    'ext_form_link',
    'approved_by',
    'adopted_by',
}


@pytest.mark.django_db()
@pytest.mark.parametrize('status, is_readonly', list(readonly_by_status.items()))
def test_outstaff_form_readonly_fields(tester, status, is_readonly):
    person = tester.get_profile()
    preprofile = PreprofileFactory(
        address='Третья улица строителей д.25, кв.12.',
        approved_by=person,
        birthday=date.today(),
        candidate_type=CANDIDATE_TYPE.NEW_EMPLOYEE,
        citizenship=CITIZENSHIP.RUSSIAN,
        created_at=datetime.now(),
        department=DepartmentFactory(id=settings.OUTSTAFF_DEPARTMENT_ID, name='Outstaff', url='outstaff'),
        email='em@i.l',
        email_domain=EMAIL_DOMAIN.YANDEX_TEAM_RU,
        employment_type=None,
        first_name='Имя',
        first_name_en='Name',
        form_type=FORM_TYPE.OUTSTAFF,
        gender=GENDER.MALE,
        guid=None,
        join_at=date.today(),
        lang_ui=LANG.EN,
        last_name='Фамилия',
        last_name_en='Lastname',
        login='sweety09',
        middle_name='Отчество',
        need_vpn=False,
        position_staff_text='Мочалок командир',
        status=status,
    )

    tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))

    controller = Repository(person).existing(preprofile.id)

    front_data = controller.front_data()

    assert set(front_data.keys()) == {'form_type', 'actions', 'data', 'structure'}
    for field_name, field_dict in front_data['data'].items():
        if field_name in always_readonly_fields or is_readonly:
            assert field_dict['readonly'] is True
        else:
            assert 'readonly' not in field_dict
