import random
from decimal import Decimal

import factory

from staff.kudos.models import Stat, Log
from staff.lib.testing import StaffFactory


class StatFactory(factory.DjangoModelFactory):

    class Meta:
        model = Stat

    person = factory.SubFactory(StaffFactory)


class LogFactory(factory.DjangoModelFactory):

    class Meta:
        model = Log

    issuer = factory.SubFactory(StaffFactory)
    recipient = factory.SubFactory(StaffFactory)
    power = factory.LazyAttribute(lambda _: Decimal('%f' % random.random()))
