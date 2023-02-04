import pytest

from io import BytesIO

from django.urls.base import reverse

from intranet.femida.tests import factories as f
from intranet.femida.tests.clients import APIClient
from intranet.femida.tests.utils import patch_service_permissions


pytestmark = pytest.mark.django_db


def test_file_upload(su_client):
    url = reverse('api:attachments:upload')
    img = BytesIO(b'imagedata')
    img.name = 'image.jpg'
    data = {
        'file': img,
    }
    response = su_client.put(url, data, format='multipart')
    assert response.status_code == 200


def test_attachment_detail_by_cookie_superuser(su_client):
    attachment = f.AttachmentFactory.create()
    url = reverse('api:attachments:detail', kwargs={'pk': attachment.id})
    response = su_client.get(url)
    assert response.status_code == 200


def test_attachment_detail_by_cookie_without_permissions(client):
    attachment = f.AttachmentFactory.create()
    url = reverse('api:attachments:detail', kwargs={'pk': attachment.id})
    response = client.get(url)
    assert response.status_code == 403


def test_attachment_detail_by_cookie_and_uid_with_permissions(su_client):
    attachment = f.AttachmentFactory.create()
    uid = '1'
    f.create_recruiter(uid=uid)
    f.CandidateAttachmentFactory.create(attachment=attachment)

    url = reverse('api:attachments:detail', kwargs={'pk': attachment.id})
    response = su_client.get(url, data={'uid': uid})
    assert response.status_code == 200


def test_attachment_detail_by_cookie_and_uid_without_permissions(su_client):
    attachment = f.AttachmentFactory.create()
    uid = '1'
    f.UserFactory.create(uid=uid)

    url = reverse('api:attachments:detail', kwargs={'pk': attachment.id})
    response = su_client.get(url, data={'uid': uid})
    assert response.status_code == 403


@patch_service_permissions({200: ['permissions.can_see_all_attachments'], 403: []})
@pytest.mark.parametrize('tvm_id_aka_status_code', (200, 403))
def test_attachment_detail_by_tvm_superuser(tvm_id_aka_status_code):
    user = f.create_superuser()
    client = APIClient()
    client.login(
        login=user.username,
        mechanism_name='tvm',
        tvm_client_id=tvm_id_aka_status_code,
    )
    attachment = f.AttachmentFactory.create()
    url = reverse('api:attachments:detail', kwargs={'pk': attachment.id})
    response = client.get(url)
    assert response.status_code == tvm_id_aka_status_code
