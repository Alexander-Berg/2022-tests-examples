import hashlib

import factory
from django.conf import settings
from wiki.users.models import User


class UserFactoryIntranet(factory.django.DjangoModelFactory):
    class Meta:
        model = User

    username = factory.Sequence(lambda n: f'user{n}')
    email = factory.LazyAttribute(lambda a: f'{a.username}@yandex-team.ru')
    first_name = factory.Sequence(lambda n: f'name{n}')


class UserFactoryCloud(UserFactoryIntranet):
    cloud_uid = factory.LazyAttribute(lambda a: hashlib.md5((a.username + a.email).encode()).hexdigest()[:12].lower())


if settings.IS_BUSINESS:
    UserFactory = UserFactoryCloud
else:
    UserFactory = UserFactoryIntranet
