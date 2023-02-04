import factory

from wiki.inline_grids.models import Grid


class InlineGridFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = Grid

    title = factory.Sequence(lambda n: f'InlineGrid {n}')
