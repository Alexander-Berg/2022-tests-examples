# encoding: utf-8


import factory

from plan.contacts import models


class ContactTypeFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.ContactType

    code = factory.Sequence(lambda n: 'code %s' % n)
    name = factory.Sequence(lambda n: 'Тип контакта %s' % n)
    name_en = factory.Sequence(lambda n: 'Contact type %s' % n)
