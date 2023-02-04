import factory
from django.utils import timezone
from django.conf import settings

from wiki.users.models import Group


class GroupFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = Group

    if settings.IS_INTRANET:
        created_at = factory.LazyAttribute(lambda a: timezone.now())
        modified_at = factory.LazyAttribute(lambda a: timezone.now())
