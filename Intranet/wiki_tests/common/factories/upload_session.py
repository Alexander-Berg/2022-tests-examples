import factory

from intranet.wiki.tests.wiki_tests.common.factories.user import UserFactory
from wiki.uploads.models import UploadSession


class UploadSessionFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = UploadSession

    session_id = factory.Sequence(lambda n: f'{n}')
    target = factory.Sequence(lambda n: f'{n}')
    file_name = factory.Sequence(lambda n: f'{n}')
    file_size = factory.Sequence(lambda n: n)

    user = factory.LazyAttribute(lambda a: UserFactory(username=a.login))
