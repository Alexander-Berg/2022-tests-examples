import factory
from django.utils import timezone
from django.conf import settings

if settings.IS_INTRANET:
    from wiki.intranet.models import Country

    class CountryFactory(factory.django.DjangoModelFactory):
        class Meta:
            model = Country

        name: str
        geo_base_id = factory.Sequence(lambda n: n)

        created_at = factory.LazyAttribute(lambda a: timezone.now())
        modified_at = factory.LazyAttribute(lambda a: timezone.now())
