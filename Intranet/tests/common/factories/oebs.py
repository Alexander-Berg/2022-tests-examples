import factory
from django.utils import timezone

from plan.oebs import models
from .services import ServiceFactory
from .staff import StaffFactory


class OEBSAgreementFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.OEBSAgreement

    service = factory.SubFactory(ServiceFactory)
    requester = factory.SubFactory(StaffFactory)
    start_date = timezone.now().date()
