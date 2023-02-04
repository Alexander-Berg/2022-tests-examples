# encoding: utf-8


import factory

from plan.roles import models


class RoleScopeFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.RoleScope

    slug = factory.Sequence(lambda n: 'slug%s' % n)
    name = factory.Sequence(lambda n: 'RoleScope %s' % n)
    name_en = factory.Sequence(lambda n: 'RoleScope_en %s' % n)
    can_issue_at_duty_time = True


class RoleFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = models.Role

    name = factory.Sequence(lambda n: 'Role %s' % n)
    name_en = factory.Sequence(lambda n: 'Role_en %s' % n)
    scope = factory.SubFactory(RoleScopeFactory)
    code = factory.Sequence(lambda n: 'code%s' % n)
    position = 0
