import factory

from plan.history import models
from .staff import StaffFactory


class HistoryRawEntryFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.HistoryRawEntry

    staff = factory.SubFactory(StaffFactory)
