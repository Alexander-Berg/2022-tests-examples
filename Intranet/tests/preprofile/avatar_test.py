import pytest

from staff.person_avatar.controllers import PreprofileAvatar, AVATAR, MAIN, GRAVATAR

from staff.person_avatar.models import AvatarMetadata
from staff.preprofile.tests.utils import PreprofileFactory
from staff.lib.testing import DepartmentFactory
from staff.preprofile.models import EMAIL_DOMAIN


@pytest.mark.django_db
def test_avatar_id():
    login1 = 'hubba'
    login2 = 'BUBBA'
    dep = DepartmentFactory(name='yandecks')
    preprofile = PreprofileFactory(
        login=login1,
        email_domain=EMAIL_DOMAIN.YANDEX_TEAM_COM_UA,
        department=dep,
    )
    meta = AvatarMetadata(preprofile=preprofile)
    meta.save()
    avatar = PreprofileAvatar(owner=preprofile, avatar_metadata=meta)

    assert avatar.avatar_id() == str(meta.id)
    assert avatar.avatar_id(attr=MAIN) == '{}-main'.format(login1)
    assert avatar.avatar_id(attr=AVATAR) == '{}-avatar'.format(login1)
    assert avatar.avatar_id(attr=GRAVATAR) == 'e545663203d720c46dea1c64af2d16cd'

    assert avatar.avatar_id(old_login=login2) == str(meta.id)
    assert avatar.avatar_id(attr=MAIN, old_login=login2) == '{}-main'.format(login2)
    assert avatar.avatar_id(attr=AVATAR, old_login=login2) == '{}-avatar'.format(login2)
    assert avatar.avatar_id(attr=GRAVATAR, old_login=login2) == '64b55f5a4b2b68fcfa99b263dce6ca80'
