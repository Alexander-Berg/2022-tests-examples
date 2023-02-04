from datetime import datetime

import pytest

from staff.lib.testing import StaffFactory
from staff.person.controllers import Person
from staff.person import models as person_models

from staff.oebs.controllers.rolluppers.person_occupation_rollupper import PersonOccupationRollupper
from staff.oebs import models as oebs_models


@pytest.mark.django_db
def test_person_occupation_rolluper_updates_occupation_on_person():
    # given
    occupation1 = person_models.Occupation.objects.create(
        name='occupation1',
        description='',
        description_en='',
        created_at=datetime.now(),
        modified_at=datetime.now(),
    )
    occupation2 = person_models.Occupation.objects.create(
        name='occupation2',
        description='',
        description_en='',
        created_at=datetime.now(),
        modified_at=datetime.now(),
    )

    s = StaffFactory()
    person_ctl = Person(s)
    person_ctl.occupation = occupation1
    person_ctl.save()

    oebs_models.PersonOccupation.objects.create(
        login=s.login,
        occupation='occupation2',
        dis_staff=s,
    )

    # when
    PersonOccupationRollupper.rollup()

    # then
    person_ctl = Person(person_models.Staff.objects.get(login=s.login))
    assert person_ctl.occupation == occupation2
