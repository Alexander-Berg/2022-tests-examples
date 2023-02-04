import pytest

from django.conf import settings

from staff.departments.models import DepartmentStaff, Department
from staff.preprofile.models import CANDIDATE_TYPE, PREPROFILE_STATUS
from staff.preprofile.tests.utils import PreprofileFactory

from staff.person.controllers import PersonCtl
from staff.person.models import Staff
from staff.person import effects


@pytest.fixture()
def adoption_data(company):
    int_dep = company.yandex
    ext_dep = company.ext
    ext_person = company.persons['ext-person']

    preprofile = PreprofileFactory(
        login=ext_person.login,
        uid=ext_person.uid,
        guid=ext_person.guid,
        department=int_dep,
        candidate_type=CANDIDATE_TYPE.EXTERNAL_EMPLOYEE,
        status=PREPROFILE_STATUS.READY,
    )

    adoption_data = {
        'login': preprofile.login,
        'uid': preprofile.uid,
        'guid': preprofile.guid,
        'first_name': preprofile.first_name,
        'last_name': preprofile.last_name,
        'first_name_en': preprofile.first_name,
        'last_name_en': preprofile.last_name,
        'birthday': preprofile.birthday,
        'gender': preprofile.gender,
        'address': preprofile.address or '',
        'department': preprofile.department,
        'office': preprofile.office,
        'organization': preprofile.organization,
        'position': preprofile.position_staff_text,
        'employment': preprofile.employment_type,
        'preprofile_id': preprofile.id,
    }
    return {
        'data': adoption_data,
        'person': ext_person,
        'preprofile': preprofile,
        'dep_from': ext_dep,
        'dep_to': int_dep,
    }


@pytest.fixture()
def adoption(adoption_data):
    ctl = PersonCtl(adoption_data['person'])
    adoption_data["person_ctl"] = ctl
    return {
        'data': adoption_data['data'],
        'person': adoption_data['person'],
        'person_ctl': ctl,
        'preprofile': adoption_data['preprofile'],
        'dep_from': adoption_data['dep_from'],
        'dep_to': adoption_data['dep_to']
    }


@pytest.mark.django_db
def test_adoption(adoption, achievements):
    person_ctl = adoption['person_ctl']
    assert Staff.objects.get(login=adoption['person'].login).department == adoption['dep_from']

    person_ctl.adopt_external_to_yandex_or_outstaff(adoption['data'])
    person_ctl.save()

    person = Staff.objects.get(login=adoption['person'].login)
    assert person.department == adoption['dep_to']
    for field, value in adoption['data'].items():
        if field == 'preprofile_id':
            continue
        assert getattr(person, field) == value

    assert DepartmentStaff.objects.filter(staff=person).count() == 0


def test_check_wrong_adoption_data(adoption):
    wrong_adoption_data = {
        'login': 'another-login',
        'uid': 'randomuid',
        'guid': 'randomguid',
        'department': adoption['dep_from'],
        'nonexistent_field': 'nonexistent_value',
    }

    for field, wrong_value in wrong_adoption_data.items():
        adoption_data = adoption['data'].copy()
        adoption_data[field] = wrong_value
        del adoption_data['preprofile_id']
        with pytest.raises(RuntimeError):
            adoption['person_ctl']._check_adoption_data(adoption_data)


def test_check_adoption_to_outstaff_is_valid(adoption):
    adoption_data = adoption['data'].copy()
    adoption_data['department'] = Department.objects.get(id=settings.OUTSTAFF_DEPARTMENT_ID)
    del adoption_data['preprofile_id']

    adoption['person_ctl']._check_adoption_data(adoption_data)


@pytest.mark.django_db
def test_personctl_restore_pushes_to_ad(adoption, achievements):
    person_ctl = adoption['person_ctl']

    person_ctl.restore()

    assert effects.push_person_data_to_ad in person_ctl._delayed_effects


@pytest.mark.django_db
def test_personctl_create_pushes_to_ad(adoption_data, achievements):
    data = adoption_data['data']
    data['login'] = 'testcreatepushedtoad'

    person_ctl = PersonCtl.create(data)

    assert effects.push_person_data_to_ad in person_ctl._delayed_effects


@pytest.mark.django_db
def test_personctl_rotate_pushes_to_ad(adoption, achievements):
    person_ctl = adoption['person_ctl']
    data = adoption['data']

    person_ctl.rotate(data)

    assert effects.push_person_data_to_ad in person_ctl._delayed_effects
