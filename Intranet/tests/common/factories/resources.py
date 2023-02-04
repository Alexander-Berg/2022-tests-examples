import factory

from plan.resources import models
from common.factories.services import ServiceFactory


class ResourceTypeCategoryFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.ResourceTypeCategory

    name = factory.Sequence(lambda n: 'TypeCategory %s' % n)
    name_en = factory.Sequence(lambda n: 'TypeCategory %s (en)' % n)
    slug = factory.Sequence(lambda n: 'slug%s' % n)
    description = factory.Sequence(lambda n: 'Description %s' % n)


class ResourceTagCategoryFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.ResourceTagCategory

    name = factory.Sequence(lambda n: 'TagCategory %s' % n)
    name_en = factory.Sequence(lambda n: 'TagCategory %s (en)' % n)
    slug = factory.Sequence(lambda n: 'slug%s' % n)


class ResourceTagFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.ResourceTag

    name = factory.Sequence(lambda n: 'Tag %s' % n)
    name_en = factory.Sequence(lambda n: 'Tag %s (en)' % n)
    slug = factory.Sequence(lambda n: 'slug%s' % n)
    description = factory.Sequence(lambda n: 'Description %s' % n)

    category = factory.SubFactory(ResourceTagCategoryFactory)


class ResourceTypeFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.ResourceType

    name = factory.Sequence(lambda n: 'Type %s' % n)
    supplier = factory.SubFactory(ServiceFactory)


class ResourceFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.Resource

    type = factory.SubFactory(ResourceTypeFactory)
    name = factory.Sequence(lambda n: 'Resource %s' % n)


class ServiceResourceFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.ServiceResource

    resource = factory.SubFactory(ResourceFactory)
    service = factory.SubFactory(ServiceFactory)
    type = factory.SelfAttribute('resource.type')


class ServiceResourceCounterFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.ServiceResourceCounter

    service = factory.SubFactory(ServiceFactory)
    resource_type = factory.SubFactory(ResourceTypeFactory)
