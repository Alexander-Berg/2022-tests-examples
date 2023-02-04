from datetime import datetime
from decimal import Decimal
from typing import Type, Optional

import factory

from staff.departments.models import Department
from staff.lib.testing import TimeStampedFactory, ValueStreamFactory

from staff.umbrellas.models import Umbrella, UmbrellaAssignment


class UmbrellaFactory(TimeStampedFactory):
    goal_id: int = factory.Sequence(lambda n: n + 1)
    issue_key: str = factory.Sequence(lambda n: 'u_key_%d' % n)
    name: str = factory.Sequence(lambda n: 'u_name_%d' % n)
    value_stream: Department = factory.SubFactory(ValueStreamFactory)

    class Meta:
        model: Type = Umbrella


class UmbrellaAssignmentFactory(TimeStampedFactory):
    umbrella: Umbrella = factory.SubFactory(UmbrellaFactory)
    engagement: Decimal = factory.Sequence(lambda n: Decimal(n + 1).quantize(Decimal('1.000')))
    engaged_from: datetime = datetime.now()
    engaged_to: Optional[datetime] = None

    class Meta:
        model: Type = UmbrellaAssignment
