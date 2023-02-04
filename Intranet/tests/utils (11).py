from datetime import date
import json

import factory

from django.contrib.auth.models import Permission
from django.core.urlresolvers import reverse

from staff.lib.testing import OfficeFactory, StaffFactory

from staff.preprofile.models import (
    CANDIDATE_TYPE,
    EMPLOYMENT,
    FORM_TYPE,
    HardwareProfile,
    PREPROFILE_STATUS,
    Preprofile,
    ProfileForDepartment,
)


class PreprofileFactory(factory.DjangoModelFactory):
    class Meta:
        model = Preprofile

    form_type = FORM_TYPE.EMPLOYEE
    join_at = date.today()
    approved_by = factory.SubFactory(StaffFactory)
    office = factory.SubFactory(OfficeFactory)
    login = factory.Sequence(lambda x: 'login{}'.format(x))
    address = factory.Sequence(lambda x: 'Street {}'.format(x))
    phone = factory.Sequence(lambda x: '+790609{:05d}'.format(x))
    candidate_type = CANDIDATE_TYPE.NEW_EMPLOYEE
    employment_type = EMPLOYMENT.FULL
    status = PREPROFILE_STATUS.NEW
    recruiter = factory.SubFactory(StaffFactory)


def post_new_form(rf, form_type, form_data):
    return rf.post(
        reverse('preprofile:new_form', kwargs={'form_type': form_type}),
        json.dumps(form_data),
        content_type='application/json',
    )


def post_new_form_api(rf, form_type, form_data):
    return rf.post(
        reverse('preprofile:new_form_api', kwargs={'form_type': form_type}),
        json.dumps(form_data),
        content_type='application/json',
    )


class HardwareFactory(factory.DjangoModelFactory):
    class Meta:
        model = HardwareProfile


class ProfileForDepartmentFactory(factory.DjangoModelFactory):
    class Meta:
        model = ProfileForDepartment


def add_hardware_profile(department):
    profile = HardwareFactory(
        profile_id='default',
        name='default',
        url='yandex',
    )

    ProfileForDepartmentFactory(
        profile=profile,
        department=department,
    )

    return profile


def make_adopter(user):
    user.user_permissions.add(Permission.objects.get(codename='add_personadoptapplication'))
