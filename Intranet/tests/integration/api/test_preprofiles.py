import pytest

from unittest.mock import patch

from django.conf import settings
from django.core.files.base import ContentFile
from django.urls.base import reverse

from intranet.femida.src.offers import choices

from intranet.femida.tests import factories as f
from intranet.femida.tests.mock.offers import FakeNewhireAPI

pytestmark = pytest.mark.django_db


@patch('intranet.femida.src.offers.login.validators.NewhireAPI', FakeNewhireAPI)
@patch('intranet.femida.src.offers.controllers.NewhireAPI', FakeNewhireAPI)
def test_preprofile_accept(client):
    preprofile = f.PreprofileFactory()
    photo = f.AttachmentFactory()
    document = f.AttachmentFactory()
    f.PreprofileAttachmentFactory(
        preprofile=preprofile,
        attachment=photo,
        type=choices.OFFER_ATTACHMENT_TYPES.photo,
    )
    f.PreprofileAttachmentFactory(
        preprofile=preprofile,
        attachment=document,
        type=choices.OFFER_ATTACHMENT_TYPES.document,
    )
    link = f.OfferLinkFactory(preprofile=preprofile)
    uid = link.uid.hex
    url = reverse('external-api:preprofiles-accept', kwargs={'uid': uid})
    data = {
        'username': 'username',
        'last_name': 'Last',
        'first_name': 'First',
        'last_name_en': 'Last',
        'first_name_en': 'First',
        'gender': choices.GENDER.M,
        'birthday': '01.01.1990',
        'citizenship': choices.CITIZENSHIP.RU,
        'residence_address': 'something',
        'phone': '+79203334455',
        'home_email': 'email@example.com',
        'join_at': '01.01.2019',
        'is_agree': True,
        'nda_accepted': True,
        'photo': [photo.id],
        'documents': [document.id],

        # Банковские реквизиты
        'bic': '123456789',
        'bank_name': 'Yandex Money',
        'bank_account': '01234567890123456789',
    }
    response = client.post(url, data)
    assert response.status_code == 200, response.content


@patch('intranet.femida.src.offers.controllers.NewhireAPI', FakeNewhireAPI)
def test_preprofile_accept_form(client):
    preprofile = f.PreprofileFactory()
    link = f.OfferLinkFactory(preprofile=preprofile)
    uid = link.uid.hex
    url = reverse('external-api:preprofiles-accept-form', kwargs={'uid': uid})
    response = client.get(url)
    assert response.status_code == 200


@patch('intranet.femida.src.offers.login.validators.NewhireAPI.check_login')
@patch('intranet.femida.src.offers.controllers.NewhireAPI', FakeNewhireAPI)
@pytest.mark.parametrize('status_code', [200, 400])
def test_preprofile_check_login(mocked_check_login, client, status_code):
    mocked_check_login.return_value = {}, status_code

    preprofile = f.PreprofileFactory()
    link = f.OfferLinkFactory(preprofile=preprofile)
    uid = link.uid.hex
    url = reverse('external-api:preprofiles-check-login', kwargs={'uid': uid})
    data = {
        'username': 'username',
    }
    response = client.get(url, data)
    assert response.status_code == status_code, response.content


def test_preprofile_attachment_upload(client):
    preprofile = f.PreprofileFactory()
    link = f.OfferLinkFactory(preprofile=preprofile)
    uid = link.uid.hex
    url = reverse('external-api:preprofiles-attachment-upload', kwargs={'uid': uid})
    data = {
        'file': ContentFile('file', name='file.txt'),
    }
    response = client.post(url, data, format='multipart')
    assert response.status_code == 200


def test_preprofile_attachment_upload_too_large_file(client):
    preprofile = f.PreprofileFactory()
    link = f.OfferLinkFactory(preprofile=preprofile)
    uid = link.uid.hex
    url = reverse('external-api:preprofiles-attachment-upload', kwargs={'uid': uid})
    max_size = settings.OFFER_MAX_ATTACHMENT_SIZE * 1024 ** 2
    data = {
        'file': ContentFile('*' * (max_size + 1), name='file.txt'),
    }
    response = client.post(url, data, format='multipart')
    assert response.status_code == 400


@patch('intranet.femida.src.api.offers.base_views.NewhireAPI.attach_phone')
def test_preprofile_attach_eds_phone(mocked_attach_phone, client):
    mocked_attach_phone.return_value = ({}, 200)
    preprofile = f.PreprofileFactory()
    link = f.OfferLinkFactory(preprofile=preprofile)
    uid = link.uid.hex
    phone = '+77777777777'

    url = reverse('external-api:preprofiles-attach-eds-phone', kwargs={'uid': uid})
    response = client.post(url, data={'eds_phone': phone})
    preprofile.refresh_from_db()
    data = {
        'object_type': 'preprofile',
        'object_id': preprofile.id,
        'phone_number': phone,
    }

    assert response.status_code == 200
    assert preprofile.eds_phone == phone
    mocked_attach_phone.assert_called_once_with(data)


@patch('intranet.femida.src.api.offers.base_views.NewhireAPI.verify_code')
def test_preprofile_verify_eds_phone(mocked_verify_code, client):
    mocked_verify_code.return_value = ({}, 200)
    preprofile = f.PreprofileFactory()
    link = f.OfferLinkFactory(preprofile=preprofile)
    uid = link.uid.hex
    code = '123'

    url = reverse('external-api:preprofiles-verify-eds-phone', kwargs={'uid': uid})
    response = client.post(url, data={'code': code})
    preprofile.refresh_from_db()
    data = {
        'object_type': 'preprofile',
        'object_id': preprofile.id,
        'code': code,
    }

    assert response.status_code == 200
    mocked_verify_code.assert_called_once_with(data)
    assert preprofile.is_eds_phone_verified is True
