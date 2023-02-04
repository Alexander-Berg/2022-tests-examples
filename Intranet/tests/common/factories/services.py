# encoding: utf-8


import factory

from plan.services import models
from common.factories.contacts import ContactTypeFactory
from common.factories.staff import StaffFactory, DepartmentFactory
from common.factories.roles import RoleFactory


class ServiceTagFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.ServiceTag

    name = factory.Sequence(lambda n: 'Тег %s' % n)
    name_en = factory.Sequence(lambda n: 'Tag %s' % n)
    slug = factory.Sequence(lambda n: 'tag_slug%s' % n)
    color = factory.Sequence(lambda n: 'color %s' % n)


class ServiceTypeFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.ServiceType

    name = factory.Sequence(lambda n: 'ТипСервиса %s' % n)
    name_en = factory.Sequence(lambda n: 'ServiceType %s' % n)
    code = factory.Sequence(lambda n: 'code_%s' % n)


class ServiceTypeFunctionFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.ServiceTypeFunction

    name = factory.Sequence(lambda n: 'ТипСервиса %s' % n)
    name_en = factory.Sequence(lambda n: 'ServiceType %s' % n)
    code = factory.Sequence(lambda n: 'code_%s' % n)


class ServiceFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.Service

    state = models.Service.states.IN_DEVELOP
    slug = factory.Sequence(lambda n: 'slug%04d' % n)
    name = factory.Sequence(lambda n: 'Service %04d' % n)
    name_en = factory.Sequence(lambda n: 'Service %04d' % n)
    owner = factory.SubFactory(StaffFactory)
    service_type = factory.SubFactory(ServiceTypeFactory)
    staff_id = factory.Sequence(lambda n: n)
    is_exportable = True
    is_base = False


class ServiceMemberFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.ServiceMember

    staff = factory.SubFactory(StaffFactory)
    service = factory.SubFactory(ServiceFactory)
    role = factory.SubFactory(RoleFactory)
    part_rate = factory.LazyAttribute(lambda a: None)
    state = models.ServiceMember.states.ACTIVE

    @factory.post_generation
    def make_member_active(self, *args, **kwargs):
        if self.state == models.ServiceMember.states.ACTIVE:
            self.activate()


class ServiceMemberDepartmentFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.ServiceMemberDepartment

    service = factory.SubFactory(ServiceFactory)
    department = factory.SubFactory(DepartmentFactory)
    role = factory.SubFactory(RoleFactory)
    state = models.ServiceMemberDepartment.states.ACTIVE


class ServiceContactFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.ServiceContact

    type = factory.SubFactory(ContactTypeFactory)
    service = factory.SubFactory(ServiceFactory)
    title = factory.Sequence(lambda n: 'Contact %s' % n)


class ServiceCreateRequestFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.ServiceCreateRequest

    service = factory.SubFactory(ServiceFactory)
    move_to = factory.SubFactory(ServiceFactory)
    requester = factory.SubFactory(StaffFactory)


class ServiceMoveRequestFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.ServiceMoveRequest

    service = factory.SubFactory(ServiceFactory)
    requester = factory.SubFactory(StaffFactory)
    destination = factory.SubFactory(ServiceFactory)


class ServiceDeleteRequestFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.ServiceDeleteRequest

    service = factory.SubFactory(ServiceFactory)
    requester = factory.SubFactory(StaffFactory)


class ServiceCloseRequestFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.ServiceCloseRequest

    service = factory.SubFactory(ServiceFactory)
    requester = factory.SubFactory(StaffFactory)


class ServiceSuspiciousReasonFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.ServiceSuspiciousReason
