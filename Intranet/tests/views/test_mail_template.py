# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import pytest

from django.core.urlresolvers import reverse

from app.tests.views import YauthAdminTestCase
from core import models
from core.tests import factory


pytestmark = pytest.mark.django_db


@pytest.fixture
def patch_mail_template_form_choices(monkeypatch):
    import app.forms
    app.forms.MailTemplateForm.declared_fields['code']._choices = [('badge', 'badge')]
    monkeypatch.setattr('app.views.mail_template.MailTemplateForm', app.forms.MailTemplateForm)


class MailTemplateTest(YauthAdminTestCase):

    def setUp(self):
        super(MailTemplateTest, self).setUp()
        self.template = factory.MailTemplateFactory()


@pytest.mark.usefixtures('patch_tanker', 'patch_mail_template_form_choices')
class CreateMailTemplateTest(MailTemplateTest):

    def test_form_rendered(self):
        url = reverse('mail_template:create')
        response = self.client.get(url)
        assert 'instance' in response.context
        assert 'language' in response.context['form'].fields
        assert 'code' in response.context['form'].fields
        assert '<select name="language">' in response.content.decode('utf-8')

    def test_template_created(self):
        url = reverse('mail_template:create')
        data = dict(subject='subject', template='template',
                    code='badge', language=0)

        response = self.client.post(url, data)
        templates = models.MailTemplate.objects.filter(code='badge')
        assert response.status_code == 302
        assert templates.count() == 2
        assert templates.first().template == 'template'

    def test_form_errors(self):
        url = reverse('mail_template:create')
        data = dict(subject='', template='updated',
                    code='badge', language=0)
        response = self.client.post(url, data)
        assert response.status_code == 200
        assert 'subject' in response.context['form'].errors


@pytest.mark.usefixtures('patch_tanker', 'patch_mail_template_form_choices')
class UpdateMailTemplateTest(MailTemplateTest):

    @property
    def url(self):
        return reverse('mail_template:mail', kwargs={'template_id': self.template.pk})

    def test_form_rendered(self):
        response = self.client.get(self.url)
        assert 'instance' in response.context
        assert 'language' not in response.context['form'].fields
        assert 'code' in response.context['form'].fields
        assert '<select name="language">' not in response.content.decode('utf-8')

    def test_template_updated(self):
        data = dict(subject='subject', template='updated', code='badge')
        response = self.client.post(self.url, data)
        templates = models.MailTemplate.objects.filter(code='badge')
        assert response.status_code == 302
        assert templates.count() == 2
        assert templates.first().template == 'updated'

    def test_form_errors(self):
        data = dict(subject='', template='updated', code='badge')
        response = self.client.post(self.url, data)
        assert response.status_code == 200
        assert 'subject' in response.context['form'].errors
