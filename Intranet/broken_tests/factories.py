import factory

from tasha.models import TgGroupInfo, TgMembership, TelegramAccount, User, TgWhitelistEntry


class UserFactory(factory.django.DjangoModelFactory):
    is_active = True

    class Meta:
        model = User


class TgGroupInfoFactory(factory.django.DjangoModelFactory):
    deactivated = False
    telegram_id = factory.Sequence(lambda n: -n)
    title = factory.Sequence(lambda n: 'title_%s' % n)

    class Meta:
        model = TgGroupInfo


class TelegramAccountFactory(factory.django.DjangoModelFactory):
    user = factory.LazyAttribute(lambda a: UserFactory())
    username = factory.Sequence(lambda n: 'username_%d' % n)
    telegram_id = factory.Sequence(lambda n: n)

    class Meta:
        model = TelegramAccount


class TgMembershipFactory(factory.django.DjangoModelFactory):
    account = factory.LazyAttribute(lambda a: TelegramAccountFactory())
    group = factory.LazyAttribute(lambda a: TgGroupInfoFactory())
    is_active = True
    is_admin = False

    class Meta:
        abstract = False
        model = TgMembership


class TgWhitelistEntryFactory(factory.django.DjangoModelFactory):
    user_account = factory.LazyAttribute(lambda a: TelegramAccountFactory())
    author = factory.LazyAttribute(lambda a: UserFactory())
    group = factory.LazyAttribute(lambda a: TgGroupInfoFactory())
    is_active = True

    class Meta:
        abstract = False
        model = TgWhitelistEntry
