import factory

from django.utils import timezone

from wiki.pages.models import Page, Revision
from wiki.utils.supertag import translit


class PageFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = Page
        strategy = factory.enums.BUILD_STRATEGY

    title = factory.Sequence(lambda n: f'Page{n}')
    supertag = factory.LazyAttribute(lambda a: translit(a.tag))
    modified_at = factory.LazyAttribute(lambda a: timezone.now())
    modified_at_for_index = factory.LazyAttribute(lambda a: timezone.now())
    formatter_version = '1.1'

    @factory.post_generation
    def create_revision(obj, create, extracted, **kwargs):
        obj._should_skip_reserved_protection = True
        obj.save()
        Revision.objects.create_from_page(obj)
