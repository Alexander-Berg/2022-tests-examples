import factory
from django.utils import timezone
from django.conf import settings

if settings.IS_INTRANET:
    from wiki.intranet.models import Office

    class OfficeFactory(factory.django.DjangoModelFactory):
        class Meta:
            model = Office

        name: str
        from_staff_id = factory.Sequence(lambda n: n)

        created_at = factory.LazyAttribute(lambda a: timezone.now())
        modified_at = factory.LazyAttribute(lambda a: timezone.now())
