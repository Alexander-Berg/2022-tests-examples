import mock
import pytest
import time

from datetime import timedelta
from io import BytesIO

from wiki.files.consts import ThumbnailStatus
from wiki.files.tasks import CreateThumbnailTask
from wiki.uploads.consts import UploadSessionStatusType
from wiki.api_v2.public.pages.attachments.views import base64_encoded_pixel
from wiki.utils.timezone import now

from intranet.wiki.tests.wiki_tests.common.data_helper import read_test_asset_as_stream
from intranet.wiki.tests.wiki_tests.common.utils import celery_eager


pytestmark = [pytest.mark.django_db]


@celery_eager
def test_attach_image_generates_thumbnail(client, wiki_users, test_page, upload_sessions):
    client.login(wiki_users.thasonic)
    session_id = 'bb143df4-9309-4ae1-97d1-93f8d86d9805'
    upload_sessions[session_id].status = UploadSessionStatusType.FINISHED
    upload_sessions[session_id].save()

    with mock.patch(
        'wiki.uploads.s3_client.BaseS3Client.get_object_body_stream',
        lambda *args: read_test_asset_as_stream('funnycat.png'),
    ):
        response = client.post(f'/api/v2/public/pages/{test_page.id}/attachments', {'upload_sessions': [session_id]})

    assert response.status_code == 200

    test_page.refresh_from_db()
    assert test_page.files == 1

    file_obj = test_page.file_set.first()
    thumbnail_data = file_obj.thumbnail_data
    assert thumbnail_data.status == ThumbnailStatus.CREATED


@celery_eager
def test_preview_file(client, wiki_users, test_page, test_files):
    client.login(wiki_users.thasonic)
    test_file = test_files['file1']
    test_file.name = 'file1.png'
    test_file.s3_storage_key = 'file1'
    test_file.save()

    # Первый запрос создаст celery задачу и сгенерит файл превью
    with mock.patch(
        'wiki.uploads.s3_client.BaseS3Client.get_object_body_stream',
        lambda *args: read_test_asset_as_stream('funnycat.png'),
    ):
        response = client.get(f'/api/v2/public/pages/{test_page.id}/attachments/{test_file.id}/preview')

    assert response.status_code == 200
    assert response.content.decode() == base64_encoded_pixel

    test_file.refresh_from_db()
    thumbnail_data = test_file.thumbnail_data
    assert thumbnail_data.status == ThumbnailStatus.CREATED

    # Повторный запрос вернет файл превью
    response = client.get(f'/api/v2/public/pages/{test_page.id}/attachments/{test_file.id}/preview')
    assert response.status_code == 200
    assert response.get('content-type') == 'image/png'
    assert response.streaming is True

    # Проверяем поле `has_preview`
    response = client.get(f'/api/v2/public/pages/{test_page.id}/attachments')
    assert response.status_code == 200
    for attach in response.json()['results']:
        if attach['id'] == test_file.id:
            assert attach['has_preview'] is True
        else:
            assert attach['has_preview'] is False


def test_preview_not_image_file(client, wiki_users, test_page, test_files):
    client.login(wiki_users.thasonic)
    test_file = test_files['file1']

    # Неверное расширение файла
    response = client.get(f'/api/v2/public/pages/{test_page.id}/attachments/{test_file.id}/preview')
    assert response.status_code == 200
    assert response.content.decode() == base64_encoded_pixel

    # Невалидный контент
    test_file.name = 'file1.png'
    test_file.s3_storage_key = 'file1'
    test_file.save()

    with mock.patch('wiki.uploads.s3_client.BaseS3Client.get_object_body_stream', lambda *args: BytesIO(b'.')):
        CreateThumbnailTask().run(file_id=test_file.id)

    test_file.refresh_from_db()
    thumbnail_data = test_file.thumbnail_data
    assert thumbnail_data.status == ThumbnailStatus.NOT_AVAILABLE

    response = client.get(f'/api/v2/public/pages/{test_page.id}/attachments/{test_file.id}/preview')
    assert response.status_code == 200
    assert response.content.decode() == base64_encoded_pixel


def test_preview_scheduled(client, wiki_users, test_page, test_files):
    client.login(wiki_users.thasonic)
    test_file = test_files['file1']
    test_file.name = 'file1.png'
    test_file.s3_storage_key = 'file1'
    test_file.save()

    response = client.get(f'/api/v2/public/pages/{test_page.id}/attachments/{test_file.id}/preview')
    assert response.status_code == 200

    test_file.refresh_from_db()
    thumbnail_data = test_file.thumbnail_data
    assert thumbnail_data.status == ThumbnailStatus.SCHEDULED

    # Повторный вызов уже зашедуленного файла не шедулит его повторно до истечения таймаута
    time.sleep(1)
    response = client.get(f'/api/v2/public/pages/{test_page.id}/attachments/{test_file.id}/preview')
    assert response.status_code == 200

    test_file.refresh_from_db()
    assert test_file.thumbnail_data.changed_at == thumbnail_data.changed_at

    # Повторный вызов уже зашедуленного файла шедулит его повторно после истечения таймаута
    thumbnail_data.changed_at = now() - timedelta(minutes=10)
    test_file.thumbnail = thumbnail_data.serialize()
    test_file.save()

    response = client.get(f'/api/v2/public/pages/{test_page.id}/attachments/{test_file.id}/preview')
    assert response.status_code == 200

    test_file.refresh_from_db()
    assert (now() - test_file.thumbnail_data.changed_at) < timedelta(seconds=10)
