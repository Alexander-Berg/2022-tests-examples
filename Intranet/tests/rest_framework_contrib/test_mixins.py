# -*- coding: utf-8 -*-
from django.db import models
from django.test import TestCase, override_settings
from rest_framework import serializers, routers, viewsets

from events.rest_framework_contrib.mixins import ManyLookupFieldsMixin


class SomeModelWithSlug(models.Model):
    slug = models.SlugField(blank=True, null=True, unique=True)

    class Meta:
        app_label = 'rest_framework_contrib'


class SomeModelWithSlugSerializer(serializers.ModelSerializer):
    class Meta:
        model = SomeModelWithSlug
        fields = (
            'id',
            'slug',
        )


class SomeModelWithSlugViewSet(ManyLookupFieldsMixin, viewsets.ModelViewSet):
    serializer_class = SomeModelWithSlugSerializer
    queryset = SomeModelWithSlug.objects.all()
    lookup_fields = ('slug', 'pk')


viewset_router = routers.DefaultRouter()
viewset_router.register('some-models', SomeModelWithSlugViewSet)
urlpatterns = viewset_router.urls


@override_settings(ROOT_URLCONF=__name__)
class TestManyLookupFieldsMixin(TestCase):

    def setUp(self):
        self.obj = SomeModelWithSlug.objects.create(slug='some_slug')

    def assert_data_exists(self):
        response = self.client.get('/some-models/')
        exp_data = {'count': 1, 'next': None, 'previous': None, 'results': [{'id': self.obj.pk, 'slug': self.obj.slug}]}
        msg = 'Не создан объект тестовой модели'
        self.assertEqual(response.status_code, 200, msg=msg)
        self.assertEqual(response.data, exp_data, msg=msg)

    def test_get_by_non_exists_pk_should_return_404(self):
        self.assert_data_exists()
        for _id in [12345678, 'some-non-existing-slug']:
            response = self.client.get('/some-models/%s/' % 12345678)
            msg = 'Если не существует модели с таким slug или pk - нужно вернуть 404 (%s)' % _id
            self.assertEqual(response.status_code, 404, msg=msg)

    def test_get_by_slug_should_return_object(self):
        self.assert_data_exists()
        response = self.client.get('/some-models/%s/' % self.obj.slug)
        msg = 'Должен был вернуться объект со слагом %s' % self.obj.slug
        self.assertEqual(response.status_code, 200, msg=msg)
        exp_data = {'id': self.obj.pk, 'slug': self.obj.slug}
        self.assertEqual(response.data, exp_data, msg=msg)

    def test_get_by_pk_should_return_object(self):
        self.assert_data_exists()
        response = self.client.get('/some-models/%s/' % self.obj.pk)
        msg = 'Должен был вернуться объект с id %s' % self.obj.pk
        self.assertEqual(response.status_code, 200, msg=msg)
        exp_data = {'id': self.obj.pk, 'slug': self.obj.slug}
        self.assertEqual(response.data, exp_data, msg=msg)
