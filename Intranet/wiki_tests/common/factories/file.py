import factory

from wiki.files.models import File


class FileFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = File

    url = factory.LazyAttribute(lambda f: File.get_unique_url(f.page, f.name))
