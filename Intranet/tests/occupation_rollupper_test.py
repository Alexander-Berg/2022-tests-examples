from datetime import datetime

import pytest

from staff.person import models as person_models

from staff.oebs.controllers.rolluppers.occupation_rolluper import OccupationRollupper
from staff.oebs import models as oebs_models


@pytest.mark.django_db
def test_occupation_rolluper_updates_dis_instance():
    # given
    occupation = person_models.Occupation.objects.create(
        name='occupation1',
        description='',
        description_en='',
        code='some',
        created_at=datetime.now(),
        modified_at=datetime.now(),
    )

    oebs_models.Occupation.objects.create(
        scale_name='occupation1',
        scale_description='desc',
        scale_description_en='',
        scale_code='some',
        dis_occupation=occupation,
    )

    # when
    OccupationRollupper.rollup()

    # then
    occupation = person_models.Occupation.objects.get(name='occupation1')
    assert occupation.description == 'desc'


@pytest.mark.django_db
def test_occupation_rolluper_creates_dis_instance():
    # given
    oebs_occupation = oebs_models.Occupation.objects.create(
        scale_name='occupation1',
        scale_description='desc',
        scale_description_en='',
        scale_code='some',
    )

    # when
    OccupationRollupper.rollup(create_absent=True)

    # then
    occupation = person_models.Occupation.objects.get(name='occupation1')
    oebs_occupation = oebs_models.Occupation.objects.get(scale_name=oebs_occupation.scale_name)
    assert oebs_occupation.dis_occupation == occupation
