import factory

from wiki.featured_pages.models import LinkGroup, LinkGroupToPage


class FeaturedPageGroupFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = LinkGroup

    title = factory.Sequence(lambda n: f'FeaturedPageGroup{n}')
    rank = factory.Sequence(lambda n: str(n))

    @factory.post_generation
    def pages(self, create, extracted, **kwargs):
        if not create:
            return
        if extracted:
            for page, rank in extracted:
                LinkGroupToPage.objects.create(link_group=self, page=page, rank=rank)
