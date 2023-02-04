# -*- coding: utf-8 -*-
import factory

from django.db import models
from django.test import TestCase
from django.contrib.contenttypes.models import ContentType
from guardian.shortcuts import assign_perm

from events.accounts.helpers import YandexClient


class MessageModel(models.Model):
    message = models.TextField()

    class Meta:
        app_label = 'accounts'


class MessageModelFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = MessageModel


class TestUserViewSet__has_permission(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex()
        self.profile.is_staff = True
        self.profile.save()
        self.message = MessageModelFactory(message='msg')

    def test_should_has_permission(self):
        permission = 'accounts.view_messagemodel'
        self.assertFalse(self.profile.has_perm(permission, self.message))
        self.assignPermission('view', [self.message], has=False)
        assign_perm(permission, self.profile, self.message)
        self.assertTrue(self.profile.has_perm(permission, self.message))
        self.assignPermission('view', [self.message], has=True)

    def test_should_has_many_permissions(self):
        permission = 'accounts.view_messagemodel'
        second_model = MessageModelFactory(message='msg')
        self.assertFalse(self.profile.has_perm(permission, self.message))
        self.assertFalse(self.profile.has_perm(permission, second_model))
        self.assignPermission('view', [self.message, second_model], has=False)
        assign_perm(permission, self.profile, self.message)
        assign_perm(permission, self.profile, second_model)
        self.assertTrue(self.profile.has_perm(permission, self.message))
        self.assertTrue(self.profile.has_perm(permission, second_model))
        self.assignPermission('view', [self.message, second_model], has=True)

    def assignPermission(self, permission_name, objects, has=True):
        params = {
            'permissions': []
        }
        for obj in objects:
            content_type_id = ContentType.objects.get_for_model(self.message).id
            params['permissions'].append('%s.%s.%s' % (permission_name, content_type_id, obj.id))
        resp = self.client.get('/admin/api/v2/users/has-permissions/', params)
        exp_permissions = dict([(p, has) for p in params['permissions']])
        self.assertEqual(resp.data, {'permissions': exp_permissions})

    def test_should_has_many_different_permissions(self):
        second_model = MessageModelFactory(message='msg')
        content_type = ContentType.objects.get_for_model(self.message)
        self.assertFalse(self.profile.has_perm('accounts.view_messagemodel', self.message))
        self.assertFalse(self.profile.has_perm('accounts.change_messagemodel', second_model))
        params = ['view.%s.%s' % (content_type.id, self.message.id), 'change.%s.%s' % (content_type.id, second_model.id)]
        exp_permissions = {
            params[0]: False,
            params[1]: False,
        }
        resp = self.client.get('/admin/api/v2/users/has-permissions/', {'permissions': params})
        self.assertEqual(resp.data, {'permissions': exp_permissions})

        assign_perm('accounts.change_messagemodel', self.profile, second_model)
        self.assertFalse(self.profile.has_perm('accounts.view_messagemodel', self.message))
        self.assertTrue(self.profile.has_perm('accounts.change_messagemodel', second_model))
        exp_permissions = {
            params[0]: False,
            params[1]: True,
        }
        resp = self.client.get('/admin/api/v2/users/has-permissions/', {'permissions': params})
        self.assertEqual(resp.data, {'permissions': exp_permissions})
