import json
import pytest

from django.core.urlresolvers import reverse

from staff.preprofile.tests.utils import PreprofileFactory

from staff.lib.testing import StaffFactory, DepartmentFactory, OfficeFactory
from staff.preprofile.views import adopt_persons
from staff.preprofile.models import PersonAdoptApplication, STATUS, PREPROFILE_STATUS
from staff.rfid.controllers import Badges


@pytest.mark.django_db
def test_adopt_candidate(rf):
    data = {
        'persons': [{
            'login': 'tester2',
            'rfid_code': '123456',
        }]
    }

    department = DepartmentFactory(name='yandex', url='yandex', code='yandex')
    office = OfficeFactory(name='office')
    recruiter = StaffFactory(login='recruiter')
    preprofile = PreprofileFactory(
        department_id=department.id,
        first_name='Koluychka',
        last_name='Vonyuchka',
        office=office,
        recruiter=recruiter,
        status=PREPROFILE_STATUS.READY,
    )

    Badges().create(
        code=123456,
        owner='candidate',
        first_name='Вася',
        last_name='Петечкин',
        preprofile_id=preprofile.id,
    )

    request = rf.post(
        reverse('preprofile:adopt_persons'),
        json.dumps(data),
        content_type='application/json'
    )

    test_person = StaffFactory(login='tester')
    test_person.user.is_superuser = True
    test_person.user.save()
    request.user = test_person.user

    response = adopt_persons(request)
    assert response.status_code == 200

    assert PersonAdoptApplication.objects.filter(
        login='tester2',
        rfid_code=123456,
        status=STATUS.NEW,
    ).count() == 1


@pytest.mark.django_db
def test_adopt_two_candidates(rf):
    data = {
        'persons': [{
                'login': 'tester2',
                'rfid_code': '123456',
            },
            {
                'login': 'tester3',
                'rfid_code': '123457000',  # non existing rfid
            },
        ]
    }

    department = DepartmentFactory(name='yandex', url='yandex', code='yandex')
    office = OfficeFactory(name='office')
    recruiter = StaffFactory(login='recruiter')
    preprofile1 = PreprofileFactory(
        department_id=department.id,
        first_name='Koluychka',
        last_name='Vonyuchka',
        office=office,
        recruiter=recruiter,
        status=PREPROFILE_STATUS.READY,
    )
    preprofile2 = PreprofileFactory(
        department_id=department.id,
        first_name='Koluychka',
        last_name='Vonyuchka',
        office=office,
        recruiter=recruiter,
        status=PREPROFILE_STATUS.READY,
    )

    Badges().create(
        code=123456,
        owner='candidate',
        first_name='Вася',
        last_name='Петечкин',
        preprofile_id=preprofile1.id,
    )

    Badges().create(
        code=123457,
        owner='candidate',
        first_name='Вася',
        last_name='Петечкин Джва',
        preprofile_id=preprofile2.id,
    )

    request = rf.post(
        reverse('preprofile:adopt_persons'),
        json.dumps(data),
        content_type='application/json'
    )

    test_person = StaffFactory(login='tester')
    test_person.user.is_superuser = True
    test_person.user.save()
    request.user = test_person.user

    response = adopt_persons(request)
    assert response.status_code == 200

    assert PersonAdoptApplication.objects.filter(
        login='tester2',
        rfid_code=123456,
        status=STATUS.NEW,
    ).count() == 1
