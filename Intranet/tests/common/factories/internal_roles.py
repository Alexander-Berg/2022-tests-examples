# encoding: utf-8


import factory

from plan.internal_roles import models
from common.factories.staff import StaffFactory


class InternalRoleFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.InternalRole

    staff = factory.SubFactory(StaffFactory)
