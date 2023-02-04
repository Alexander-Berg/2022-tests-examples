# encoding: utf-8


import factory
from waffle.models import Switch


class SwitchFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = Switch

    name = factory.Sequence(lambda n: 'name_%s' % n)
