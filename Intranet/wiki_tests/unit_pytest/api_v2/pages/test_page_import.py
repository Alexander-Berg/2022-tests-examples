import io
import mock
import pytest

from wiki.api_v2.public.pages.exceptions import SlugOccupied, WrongFileExtension
from wiki.api_v2.public.upload_sessions.exceptions import InvalidUploadSession
from wiki.pages.models import Page
from wiki.uploads.consts import UploadSessionStatusType

from intranet.wiki.tests.wiki_tests.common.data_helper import read_test_asset_as_stream

pytestmark = [pytest.mark.django_db]


def test_import_txt(client, wiki_users, upload_sessions):
    session_id = 'a2986e79-c999-4d9f-ab23-c60bb677e5ab'
    upload_sessions[session_id].status = UploadSessionStatusType.FINISHED
    upload_sessions[session_id].save()

    file_data = '*page content*\nсодержимое страницы\n'
    page_slug = 'pagefromtxt'

    with pytest.raises(Page.DoesNotExist):
        Page.objects.get(supertag=page_slug)

    client.login(wiki_users.thasonic)
    with mock.patch(
        'wiki.uploads.s3_client.BaseS3Client.get_object_body_stream',
        lambda *args: io.BytesIO(file_data.encode())
    ):
        response = client.post(f'/api/v2/public/pages/import', {
            'upload_session': session_id,
            'slug': page_slug,
            'title': page_slug,
        })

    assert response.status_code == 200
    assert response.json()['slug'] == page_slug
    page = Page.objects.get(supertag=page_slug)
    assert page.body == file_data

    upload_sessions[session_id].refresh_from_db()
    assert upload_sessions[session_id].status == UploadSessionStatusType.USED

    # Повторный вызов должен привезти к ошибке, т.к. страница уже существует
    response = client.post(f'/api/v2/public/pages/import', {
        'upload_session': session_id,
        'slug': page_slug,
        'title': page_slug,
    })
    assert response.status_code == 400
    assert response.json()['error_code'] == SlugOccupied.error_code


def test_import_doc(client, wiki_users, upload_sessions):
    session_id = '279af70e-9952-4989-9fde-180d54c5fdfc'
    upload_sessions[session_id].status = UploadSessionStatusType.FINISHED
    upload_sessions[session_id].save()

    page_slug = 'pagefromdoc'
    with pytest.raises(Page.DoesNotExist):
        Page.objects.get(supertag=page_slug)

    client.login(wiki_users.thasonic)
    with mock.patch(
        'wiki.uploads.s3_client.BaseS3Client.get_object_body_stream',
        lambda *args: read_test_asset_as_stream('doc_with_picture.docx')
    ):
        response = client.post(f'/api/v2/public/pages/import', {
            'upload_session': session_id,
            'slug': page_slug,
            'title': page_slug,
        })

    assert response.status_code == 200
    assert response.json()['slug'] == page_slug
    page = Page.objects.get(supertag=page_slug)
    assert page.body == '\nМистер кот\n\n== <<Imported images>>\nfile:image1.jpg\n'

    upload_sessions[session_id].refresh_from_db()
    assert upload_sessions[session_id].status == UploadSessionStatusType.USED


def test_wrong_target(client, wiki_users, upload_sessions):
    session_ids = [
        '9b18deaa-b969-4caa-a4f0-b13e455b610b',  # attachment
        'a7901b58-d316-4b86-ac85-002a3bf68d88',  # import grid
    ]
    client.login(wiki_users.thasonic)
    for session_id in session_ids:
        upload_sessions[session_id].status = UploadSessionStatusType.FINISHED
        upload_sessions[session_id].save()

        response = client.post(f'/api/v2/public/pages/import', {
            'upload_session': session_id,
            'slug': 'badsession',
            'title': 'badsession',
        })
        assert response.status_code == 400
        assert response.json()['error_code'] == InvalidUploadSession.error_code

        # Проверяем, что статус сессии при этом не изменился
        upload_sessions[session_id].refresh_from_db()
        assert upload_sessions[session_id].status == UploadSessionStatusType.FINISHED


def test_wrong_extension(client, wiki_users, upload_sessions):
    session_ids = [
        '3ee4bb73-57f4-4048-ac66-ce1777341cb0',  # empty ext
        '37a000ae-0478-4f2c-b93f-60a6e596ac0e',  # pdf
    ]
    client.login(wiki_users.thasonic)
    for session_id in session_ids:
        upload_sessions[session_id].status = UploadSessionStatusType.FINISHED
        upload_sessions[session_id].save()

        response = client.post(f'/api/v2/public/pages/import', {
            'upload_session': session_id,
            'slug': 'badext',
            'title': 'badext',
        })
        assert response.status_code == 400
        assert response.json()['error_code'] == WrongFileExtension.error_code

        upload_sessions[session_id].refresh_from_db()
        assert upload_sessions[session_id].status == UploadSessionStatusType.FINISHED
