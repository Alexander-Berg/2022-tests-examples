import pytest

from django.conf import settings
from django.utils import timezone
from mock import patch, MagicMock
from waffle.testutils import override_switch

from wiki.api_core.waffle_switches import S3_DEFAULT_STORAGE
from wiki.api_v2.public.upload_sessions.exceptions import StorageUploadError
from wiki.pages.models import Page, Revision
from wiki.pages.models.consts import PageType
from wiki.uploads.s3_client import S3_CLIENT
from wiki.utils.s3.storage import S3Storage
from wiki.utils.s3.exceptions import S3StorageError

pytestmark = [pytest.mark.django_db]


@override_switch(S3_DEFAULT_STORAGE, True)
def test_storage(wiki_users):
    Page.s3_key.field.storage = S3Storage()
    now = timezone.now()
    page = Page.objects.create(
        title='Test Page',
        supertag='testpage',
        modified_at=now,
        modified_at_for_index=now,
        owner=wiki_users.thasonic,
        last_author=wiki_users.thasonic,
    )
    page.body = 'pagebody'
    page.save()

    s3_key = str(page.s3_key)
    assert s3_key.startswith(f'{settings.WIKI_CODE}/page/{page.id}') is True
    assert bool(page.mds_storage_id) is False

    revision = Revision.objects.create_from_page(page)
    assert revision.s3_key.name == page.s3_key.name
    assert bool(revision.mds_storage_id) is False


@override_switch(S3_DEFAULT_STORAGE, True)
@patch.object(S3_CLIENT, 'put_object')
def test_storage_error(put_object: MagicMock, client, wiki_users):
    Page.s3_key.field.storage = S3Storage()
    client.login(wiki_users.thasonic)

    # Первый API запрос создаст домашнюю страницу пользователя,
    # только после этого мокаем s3 client
    client.get('/api/v2/public/me')

    put_object.side_effect = StorageUploadError()
    response = client.post(
        '/api/v2/public/pages?fields=content,access',
        data={
            'page_type': PageType.REGULAR_PAGE.value,
            'title': 'Test',
            'slug': 'testpage',
            'content': 'FooBar',
        },
    )

    assert response.status_code == 502
    assert response.json()['error_code'] == S3StorageError.error_code
