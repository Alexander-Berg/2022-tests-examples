import factory

from kelvin.tags.models import Tag, TaggedObject


class TagFactory(factory.DjangoModelFactory):
    class Meta:
        model = Tag


class TaggedObjectFactory(factory.DjangoModelFactory):
    tag = factory.SubFactory(TagFactory)

    class Meta:
        model = TaggedObject
