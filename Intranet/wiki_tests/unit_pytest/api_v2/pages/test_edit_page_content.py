from datetime import timedelta
from django.test import override_settings

import pytest
from freezegun import freeze_time

from wiki.api_v2.public.pages.exceptions import PageBodySizeExceeded
from wiki.notifications.models import PageEvent
from wiki.pages.models import PageLink, AbsentPage
from wiki.utils import timezone
from intranet.wiki.tests.wiki_tests.common.utils import celery_eager

pytestmark = [pytest.mark.django_db]


@celery_eager
def test_page_edit__content(client, wiki_users, page_cluster, organizations, groups):
    page = page_cluster['root/a/ad']

    # ВАЖНО - контент снизу уже имеет записанные ответы для мока форматтера, где тот "парсит" в AST

    body = """((/root root)) ((https://ya.ru/ ya.ru)) ((/missing_one))"""
    assert page.revision_set.all().count() == 1

    dt = timezone.now() + timedelta(days=10)

    with freeze_time(dt):
        client.login(wiki_users.asm)
        response = client.post(f'/api/v2/public/pages/{page.id}?fields=content', {'content': body})

        assert response.status_code == 200, response.json()
        page.refresh_from_db()

        assert page.body == body
        assert page.last_author == wiki_users.asm
        assert page.revision_set.all().count() == 2
        last = page.revision_set.order_by('-created_at')[0]
        assert last.author == wiki_users.asm
        assert page.modified_at == dt
        assert page.body_size == len(body)

    # Создание и редактирование страницы должны создавать PageEvent
    pe = PageEvent.objects.filter(page=page).order_by('-created_at').first()
    assert pe.event_type == PageEvent.EVENT_TYPES.edit
    assert pe.author == wiki_users.asm
    assert pe.notify

    links = list(PageLink.objects.filter(from_page=page).all())
    absent_links = list(AbsentPage.objects.filter(from_page=page).all())

    #  Создание и редактирование страниц должно вызывать задачу и ссылки должны трекаться
    assert len(links) == 1
    assert links[0].to_page == page_cluster['root']
    assert len(absent_links) == 1
    assert absent_links[0].to_supertag == 'missingone'


def test_page_edit__content__same_body(client, wiki_users, page_cluster, organizations, groups):
    # пересылка того же контента не порождает ревизию

    page = page_cluster['root/a/ad']
    old_author = page.last_author

    assert page.revision_set.all().count() == 1

    client.login(wiki_users.asm)
    response = client.post(f'/api/v2/public/pages/{page.id}?fields=content', {'content': page.body})

    assert response.status_code == 200, response.json()
    page.refresh_from_db()

    assert page.last_author == old_author
    assert page.revision_set.all().count() == 1


@override_settings(LIMIT__WIKI_TEXT_FOR_PAGE__BYTES=5)
def test_page_edit__body_size_limit(client, wiki_users, test_page, organizations, groups):
    body = 'page test'
    client.login(wiki_users.asm)
    response = client.post(f'/api/v2/public/pages/{test_page.id}?fields=content', {'content': body})
    assert response.status_code == 400
    assert response.json()['error_code'] == PageBodySizeExceeded.error_code
