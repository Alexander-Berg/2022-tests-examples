# -*- coding: utf-8 -*-

import unittest

import pytest

from app.tests import views as testcase
from core import models
from core.tests import factory


@pytest.mark.usefixtures('patch_tanker_adapter')
class EditablePageUpdateTest(testcase.YauthAdminTestCase):

    def setUp(self):
        super(EditablePageUpdateTest, self).setUp()
        self.page = factory.EditablePageFactory()
        self.newtext = 'newtext'
        self.draft = factory.PageTranslationFactory(
            page=self.page, status=models.PageTranslation.STATUS_DRAFT)
        self.uploaded = factory.PageTranslationFactory(
            page=self.page, status=models.PageTranslation.STATUS_UPLOADED)
        self.downloaded = factory.PageTranslationFactory(
            page=self.page, status=models.PageTranslation.STATUS_DOWNLOADED)
        self.published = factory.PageTranslationFactory(
            page=self.page, status=models.PageTranslation.STATUS_PUBLISHED)

    def _assert_updated_attrs(self, status, **kwargs):
        updated = models.PageTranslation.objects.get(page=self.page, status=status)
        for attr, value in kwargs.items():
            assert getattr(updated, attr) == value


class EditablePageDraftUpdateTest(EditablePageUpdateTest):
    url_name = 'page:update-draft'

    def setUp(self):
        super(EditablePageDraftUpdateTest, self).setUp()
        self.form_data = {
            'slug': self.page.slug,
            'order': self.page.order,
            'title_ru': self.draft.title_ru,
            'text_ru': self.newtext,
        }

    def test_next_status_draft(self):
        next_status = models.PageTranslation.STATUS_DRAFT
        self.form_data['status'] = next_status
        response = self._post_request(slug=self.page.slug)
        assert response.status_code == 302
        self._assert_updated_attrs(next_status, text_ru=self.newtext)

    def test_next_status_uploaded(self):
        next_status = models.PageTranslation.STATUS_UPLOADED
        self.form_data['status'] = next_status
        response = self._post_request(slug=self.page.slug)
        assert response.status_code == 302
        self._assert_updated_attrs(
            next_status, title_ru=self.draft.title_ru, text_ru=self.newtext)



class EditablePageUploadedUpdateTest(EditablePageUpdateTest):
    url_name = 'page:update-uploaded'

    def setUp(self):
        super(EditablePageUploadedUpdateTest, self).setUp()
        self.form_data = {
            'slug': self.page.slug,
            'order': self.page.order,
            'title_ru': self.uploaded.title_ru,
            'text_ru': self.newtext,
        }

    def test_next_status_uploaded(self):
        next_status = models.PageTranslation.STATUS_UPLOADED
        self.form_data['status'] = next_status
        response = self._post_request(slug=self.page.slug)
        assert response.status_code == 302
        self._assert_updated_attrs(next_status, text_ru=self.newtext)

    def test_next_status_draft(self):
        next_status = models.PageTranslation.STATUS_DRAFT
        self.form_data['status'] = next_status
        response = self._post_request(slug=self.page.slug)
        assert response.status_code == 302
        self._assert_updated_attrs(
            next_status, title_ru=self.uploaded.title_ru, text_ru=self.newtext)


class EditablePageDownloadedTest(EditablePageUpdateTest):
    url_name = 'page:update-downloaded'

    def setUp(self):
        super(EditablePageDownloadedTest, self).setUp()
        self.form_data = {
            'slug': self.page.slug,
            'order': self.page.order,
            'title_ru': self.downloaded.title_ru,
            'text_ru': self.newtext,
        }

    def test_next_status_published(self):
        next_status = models.PageTranslation.STATUS_PUBLISHED
        self.form_data['status'] = next_status
        response = self._post_request(slug=self.page.slug)
        assert response.status_code == 302
        self._assert_updated_attrs(next_status, text_ru=self.downloaded.text_ru)

    def test_next_status_uploaded(self):
        next_status = models.PageTranslation.STATUS_UPLOADED
        self.form_data['status'] = next_status
        response = self._post_request(slug=self.page.slug)
        assert response.status_code == 302
        self._assert_updated_attrs(next_status, text_ru=self.newtext)

    def test_next_status_draft(self):
        next_status = models.PageTranslation.STATUS_DRAFT
        self.form_data['status'] = next_status
        response = self._post_request(slug=self.page.slug)
        assert response.status_code == 302
        self._assert_updated_attrs(next_status, text_ru=self.newtext)


class EditablePagePublishedTest(EditablePageUpdateTest):
    url_name = 'page:update-published'

    def setUp(self):
        super(EditablePagePublishedTest, self).setUp()
        self.form_data = {
            'slug': self.page.slug,
            'order': self.page.order,
            'title_ru': self.published.title_ru,
            'text_ru': self.newtext,
        }

    def test_next_status_draft(self):
        next_status = models.PageTranslation.STATUS_DRAFT
        self.form_data['status'] = next_status
        response = self._post_request(slug=self.page.slug)
        assert response.status_code == 302
        self._assert_updated_attrs(next_status, text_ru=self.newtext)

    def test_next_status_draft(self):
        next_status = models.PageTranslation.STATUS_UPLOADED
        self.form_data['status'] = next_status
        response = self._post_request(slug=self.page.slug)
        assert response.status_code == 302
        self._assert_updated_attrs(next_status, text_ru=self.newtext)
