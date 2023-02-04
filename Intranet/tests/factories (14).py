import factory
from factory.django import DjangoModelFactory

from lms.courses.tests.factories import CourseFactory

from ..models import LinkResource, TextResource, VideoResource


class LinkResourceFactory(DjangoModelFactory):
    course = factory.SubFactory(CourseFactory)
    name = factory.Faker('word')
    description = factory.Faker('text')
    url = factory.Faker('uri')

    class Meta:
        model = LinkResource


class TextResourceFactory(DjangoModelFactory):
    course = factory.SubFactory(CourseFactory)
    name = factory.Faker('word')
    description = factory.Faker('text')
    content = factory.Faker('text')

    class Meta:
        model = TextResource


class VideoResourceFactory(DjangoModelFactory):
    course = factory.SubFactory(CourseFactory)
    name = factory.Faker('word')
    description = factory.Faker('text')
    url = factory.Faker('uri')

    class Meta:
        model = VideoResource
