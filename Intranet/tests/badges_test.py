import pytest
from datetime import date

from staff.lib.testing import StaffFactory, DepartmentFactory

from staff.preprofile.tests.utils import PreprofileFactory

from staff.rfid.controllers import Badges
from staff.rfid.models import Badge
from staff.rfid.constants import OWNER


@pytest.mark.django_db
def test_candidate_badge_slicing():
    count = 10
    preprofiles = [
        PreprofileFactory(
            first_name='Koluychka',
            last_name='Vonyuchka',
            recruiter=StaffFactory(login='recruiter'),
            department=DepartmentFactory(),
        )
        for _ in range(count)
    ]
    [
        Badge.objects.create(
            first_name='first_name',
            first_name_en='last_name',
            last_name='first_name',
            last_name_en='last_name',

            owner=OWNER.CANDIDATE,
            preprofile=preprofile,
            login='login-{}'.format(num),
            photo='photo_url',
            position='',
            join_at=date.today(),
        )
        for num, preprofile in enumerate(preprofiles, start=1)
    ]

    slice1 = Badges()[3:7]
    slice2 = Badges()[:5]
    slice3 = Badges()[8:]

    assert len(list(slice1)) == 4
    assert len(list(slice2)) == 5
    assert len(list(slice3)) == 2


@pytest.mark.django_db
def test_candidate_badges_fiter():
    preprofile = PreprofileFactory(
        first_name='Koluychka',
        last_name='Vonyuchka',
        recruiter=StaffFactory(login='recruiter'),
        department=DepartmentFactory(),
    )
    Badge.objects.create(
        first_name='first_name',
        first_name_en='last_name',
        last_name='first_name',
        last_name_en='last_name',

        owner=OWNER.CANDIDATE,
        preprofile=preprofile,
        login='login',
        photo='photo_url',
        position='',
        join_at=date.today(),
    )

    badge = Badges().filter(preprofile_id=preprofile.id).first()
    assert badge.as_dict('preprofile_id')['preprofile_id'] == preprofile.id
