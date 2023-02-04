import factory
from django.conf import settings
from django.utils import timezone

from intranet.search.core import models


SEARCHES = list(settings.ISEARCH['searches']['base'])
SAAS_SERVICES = list(set(settings.ISEARCH['api']['saas'].keys()) - {'base'})


class User(factory.DjangoModelFactory):
    username = factory.Sequence(lambda n: f'user{n}')

    class Meta:
        model = 'users.User'


class Group(factory.DjangoModelFactory):
    class Meta:
        model = 'auth.Group'


class Permission(factory.DjangoModelFactory):
    class Meta:
        model = 'auth.Permission'
        django_get_or_create = ('codename',)


class BaseSearchRefFactory(factory.DjangoModelFactory):

    search = factory.Sequence(lambda n: f'search{n}')


class ExternalWizardRule(BaseSearchRefFactory):

    class Meta:
        model = 'core.ExternalWizardRule'


class Facet(BaseSearchRefFactory):

    class Meta:
        model = 'core.Facet'

    facet = factory.Sequence(lambda n: f'facet{n}')
    value = factory.Sequence(lambda n: f'facet_value{n}')


class Feature(factory.DjangoModelFactory):

    class Meta:
        model = 'core.Feature'


class Formula(BaseSearchRefFactory):

    class Meta:
        model = 'core.Formula'


class GroupAttr(BaseSearchRefFactory):
    class Meta:
        model = 'core.GroupAttr'


class Organization(factory.DjangoModelFactory):

    class Meta:
        model = 'core.Organization'

    directory_id = factory.Sequence(lambda n: n + 1)


class PushRecord(BaseSearchRefFactory):

    class Meta:
        model = 'core.PushRecord'


class PushInstance(factory.DjangoModelFactory):

    class Meta:
        model = 'core.PushInstance'


class Service(factory.DjangoModelFactory):
    class Meta:
        model = 'core.Service'

    organization = factory.SubFactory(Organization)


class Revision(factory.DjangoModelFactory):

    class Meta:
        model = 'core.Revision'

    organization = factory.SubFactory(Organization)
    search = 'wiki'
    service = SAAS_SERVICES[0]
    status = 'active'


class Indexation(BaseSearchRefFactory):

    class Meta:
        model = 'core.Indexation'

    revision = factory.SubFactory(Revision)
    start_time = factory.LazyFunction(timezone.now)


class FerrymanTable(factory.DjangoModelFactory):

    class Meta:
        model = 'core.FerrymanTable'

    revision = factory.SubFactory(Revision)


class MovedPage(factory.DjangoModelFactory):

    class Meta:
        model = 'core.MovedPage'

    old_url = factory.Sequence(lambda a: f'https://wiki.yandex-team.ru/old-url/{a}')
    new_url = factory.Sequence(lambda a: f'https://wiki.yandex-team.ru/new-url/{a}')


def reload_obj(obj):
    return obj.__class__.objects.get(pk=obj.pk)


def create_indexation(revision=None, **kwargs):
    kwargs.setdefault('status', models.Indexation.STATUS_DONE)
    if not revision:
        revision = create_revision()
    return Indexation(
        revision=revision,
        search=revision.search,
        index=revision.index,
        **kwargs
    )


def create_revision(**kwargs):
    return Revision(**kwargs)


def get_revision_data(**kwargs):
    stub = Revision.build(**kwargs)
    revision = {
        'id': 1,
        'service': stub.service,
        'search': stub.search,
        'index': stub.index,
        'backend': stub.backend,
    }
    return revision


def create_push(status=models.PushRecord.STATUS_DONE, **kwargs):
    data = {
        'search': 'st',
        'index': '',
        'meta': {}
    }
    data.update(kwargs)
    return PushRecord(status=status, **data)


def create_push_instance(push=None, status=models.PushInstance.STATUS_DONE, **kwargs):
    return PushInstance(push=push, status=status, **kwargs)


def create_facet(revision, **kwargs):
    data = dict({
        'search': revision.search,
        'index': revision.index,
        'backend': revision.backend,
        'revision': revision,
    }, **kwargs)
    return Facet(**data)


def create_group_attr(revision, **kwargs):
    data = dict({
        'search': revision.search,
        'index': revision.index,
        'backend': revision.backend,
        'revision': revision,
    }, **kwargs)
    return GroupAttr(**data)
