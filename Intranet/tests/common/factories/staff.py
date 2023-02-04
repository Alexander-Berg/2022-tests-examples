import factory

from django.contrib.auth.models import User
from django.utils import timezone

from plan.staff import models

__all__ = ['DepartmentFactory', 'DepartmentStaffFactory', 'StaffFactory', 'ServiceScopeFactory', 'UserFactory']

from plan.staff.constants import DEPARTMENT_ROLES


class UserFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = User
    username = factory.Sequence(lambda n: 'user%s' % n)


class DepartmentFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.Department
    url = factory.Sequence(lambda n: 'department_%s' % n)
    name = factory.Sequence(lambda n: 'Department %s' % n)
    name_en = factory.Sequence(lambda n: 'Department %s (en)' % n)
    short_name = factory.Sequence(lambda n: 'Short name %s' % n)
    short_name_en = factory.Sequence(lambda n: 'Short name en %s' % n)
    staff_id = factory.Sequence(lambda n: n)
    created_at = factory.LazyAttribute(lambda a: timezone.now())
    modified_at = factory.LazyAttribute(lambda a: timezone.now())


class StaffFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.Staff

    user = factory.LazyAttribute(lambda a: UserFactory(username=a.login))
    login = factory.Sequence(lambda n: 'staff%s' % n)
    uid = factory.Sequence(lambda n: str(n))
    first_name = factory.Sequence(lambda n: 'name %s' % n)
    first_name_en = factory.Sequence(lambda n: 'name en %s' % n)
    last_name = factory.Sequence(lambda n: 'lastname %s' % n)
    last_name_en = factory.Sequence(lambda n: 'lastname en %s' % n)
    is_robot = False
    gender = factory.LazyAttribute(lambda a: 'M')
    department = factory.SubFactory(DepartmentFactory)
    is_dismissed = False
    work_email = factory.LazyAttribute(lambda a: a.login + '@yandex-team.ru')
    created_at = factory.LazyAttribute(lambda a: timezone.now())
    modified_at = factory.LazyAttribute(lambda a: timezone.now())
    staff_id = factory.Sequence(lambda n: 1000 + n)
    chief = None


class ServiceScopeFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.ServiceScope

    service = factory.SubFactory('common.factories.services.ServiceFactory')
    role_scope = factory.SubFactory('common.factories.roles.RoleScopeFactory')
    staff_id = factory.Sequence(lambda n: n + 10000)
    created_at = factory.LazyAttribute(lambda a: timezone.now())
    modified_at = factory.LazyAttribute(lambda a: timezone.now())


class DepartmentStaffFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.DepartmentStaff

    department = factory.SubFactory(DepartmentFactory)
    staff = factory.SubFactory(StaffFactory)
    role = factory.LazyAttribute(lambda a: DEPARTMENT_ROLES.CHIEF)
