import json
import pytest

from django.conf import settings
from django.core.urlresolvers import reverse

from staff.lib.testing import StaffFactory
from staff.person.models import StaffExtraFields
from staff.person_avatar.models import AvatarMetadata


@pytest.mark.django_db
def test_photos_view_when_no_photos(client):
    client.login(user=StaffFactory(login=settings.AUTH_TEST_USER).user)
    person = StaffFactory(login='stranger')
    url = reverse('profile:photos', kwargs={'login': person.login})
    response = client.get(url)
    assert response.status_code == 200
    assert json.loads(response.content) == {'target': {'photos': []}}


@pytest.mark.django_db
def test_photos_view_with_photos(client):
    client.login(user=StaffFactory(login=settings.AUTH_TEST_USER).user)
    person = StaffFactory(login='stranger')
    url = reverse('profile:photos', kwargs={'login': person.login})

    photo1 = AvatarMetadata(person=person, is_main=True, is_deleted=False)
    photo2 = AvatarMetadata(person=person, is_avatar=True, is_deleted=False)
    photo3 = AvatarMetadata(person=person, is_deleted=True)

    photo1.save()
    photo2.save()
    photo3.save()

    response = client.get(url)
    assert response.status_code == 200
    photos = json.loads(response.content)['target']['photos']
    assert {'avatar_id': photo1.id, 'is_avatar': photo1.is_avatar, 'is_main': photo1.is_main} in photos
    assert {'avatar_id': photo2.id, 'is_avatar': photo2.is_avatar, 'is_main': photo2.is_main} in photos
    assert {'avatar_id': photo3.id, 'is_avatar': photo3.is_avatar, 'is_main': photo3.is_main} not in photos


@pytest.mark.django_db
def test_photos_view_with_photos_of_dismissed_user_without_extra(client):
    client.login(user=StaffFactory(login=settings.AUTH_TEST_USER).user)
    person = StaffFactory(login='stranger', is_dismissed=True)
    photo = AvatarMetadata(person=person)
    photo.save()

    url = reverse('profile:photos', kwargs={'login': person.login})

    response = client.get(url)
    assert response.status_code == 200
    assert json.loads(response.content) == {'target': {'photos': []}}


@pytest.mark.django_db
def test_photos_view_with_photos_of_agreed_dismissed_user(client):
    client.login(user=StaffFactory(login=settings.AUTH_TEST_USER).user)
    person = StaffFactory(login='stranger', is_dismissed=True)
    StaffExtraFields.objects.create(staff=person, staff_agreement=True)

    photo = AvatarMetadata(person=person, is_deleted=False)
    photo.save()

    url = reverse('profile:photos', kwargs={'login': person.login})

    response = client.get(url)
    assert response.status_code == 200
    photos = json.loads(response.content)['target']['photos']
    assert {'avatar_id': photo.id, 'is_avatar': photo.is_avatar, 'is_main': photo.is_main} in photos
