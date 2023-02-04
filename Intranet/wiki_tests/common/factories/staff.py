import factory

from django.utils import timezone

from wiki.intranet.models import Staff
from intranet.wiki.tests.wiki_tests.common.factories.user import UserFactory


class StaffFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = Staff

    login = factory.Sequence(lambda n: 'staff%s' % n)
    uid = factory.Sequence(lambda n: str(n))
    login_ld = factory.LazyAttribute(lambda a: a.login)
    created_at = factory.LazyAttribute(lambda a: timezone.now())
    modified_at = factory.LazyAttribute(lambda a: timezone.now())
    native_lang = factory.LazyAttribute(lambda a: 'ru')
    gender = factory.LazyAttribute(lambda a: 'M')
    user = factory.LazyAttribute(lambda a: UserFactory(username=a.login))
