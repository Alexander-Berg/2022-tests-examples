# encoding: utf-8


import factory

from plan.hr import models
from common.factories.staff import StaffFactory
from common.factories.services import ServiceMemberFactory

__all__ = ['PartRateHistoryFactory']


class PartRateHistoryFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.PartRateHistory

    service_member = factory.SubFactory(ServiceMemberFactory)
    member_staff = factory.LazyAttribute(lambda a: a.service_member.staff)
    staff = factory.SubFactory(StaffFactory)
