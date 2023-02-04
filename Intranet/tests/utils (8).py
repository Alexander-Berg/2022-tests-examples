from datetime import datetime
from typing import List

import factory

from staff.femida.models import FemidaVacancy


class FemidaVacancyFactory(factory.DjangoModelFactory):
    class Meta:
        model = FemidaVacancy

    id = factory.Sequence(lambda x: x)
    created = datetime.now()
    modified = datetime.now()
    is_published = False
    members: List[str] = []
