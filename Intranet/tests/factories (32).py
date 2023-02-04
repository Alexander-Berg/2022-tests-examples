from datetime import date
from typing import Type

import factory

from staff.syncs.models import StartrekUmbrella, StartrekVs


class StarTrekVsFactory(factory.DjangoModelFactory):
    issue_key: str = factory.Sequence(lambda n: 'st_vs_key-%d' % n)
    name: str = factory.Sequence(lambda n: 'st_vs)name_%d' % n)
    abc_service_id: int = factory.Sequence(lambda n: n + 1)
    deadline: date = date.today()

    class Meta:
        model: Type = StartrekVs


class StarTrekUmbrellaFactory(factory.DjangoModelFactory):
    issue_key: str = factory.Sequence(lambda n: 'st_u_key-%d' % n)
    name: str = factory.Sequence(lambda n: 'st_u_name_%d' % n)
    vs: StartrekVs = factory.SubFactory(StarTrekVsFactory)

    class Meta:
        model: Type = StartrekUmbrella
