import factory
from django.utils import timezone
from django.conf import settings

if settings.IS_INTRANET:
    from wiki.intranet.models import Department

    class DepartmentFactory(factory.django.DjangoModelFactory):
        class Meta:
            model = Department

        created_at = factory.LazyAttribute(lambda a: timezone.now())
