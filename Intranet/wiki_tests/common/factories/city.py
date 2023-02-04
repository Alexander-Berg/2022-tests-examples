import factory
from django.utils import timezone
from django.conf import settings

if settings.IS_INTRANET:
    from wiki.intranet.models import City

    class CityFactory(factory.django.DjangoModelFactory):
        class Meta:
            model = City

        name: str

        created_at = factory.LazyAttribute(lambda a: timezone.now())
        modified_at = factory.LazyAttribute(lambda a: timezone.now())
