# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import pytest

from app.tasks import tanker
from core import models
from core.tanker import api
    

def test_update_page_translations(page, patch_tanker):
    tanker.update_page_translations()
    updated_page = models.StaticPage.objects.get(pk=page.pk)
    assert updated_page.status == models.StaticPage.STATUS_PUBLISHED
    translated_page = models.StaticPage.objects.get(
        page_type=page.page_type, language=models.StaticPage.LANGUAGE_ENGLISH)
    assert translated_page.text == 'text'


@pytest.fixture
def page(db):
    return models.StaticPage.objects.create(
        status=models.StaticPage.STATUS_IN_TANKER,
        text='text',
    )


@pytest.fixture
def patch_tanker(monkeypatch):
    class TankerMock(api.Tanker):
        def download_keyset(self, keyset_id):
            return {
                'ru': {'index': {'text': 'текст'}},
                'en': {'index': {'text': 'text'}},
            }
    monkeypatch.setattr(
        tanker, 'Tanker', TankerMock)
