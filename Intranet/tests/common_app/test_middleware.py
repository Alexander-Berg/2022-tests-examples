# -*- coding: utf-8 -*-
from django.conf.urls import url
from django.db import models
from django.test import TestCase, override_settings

from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework import serializers, routers, viewsets


class SomeView(APIView):
    def get(self, request, *args, **kwargs):
        return Response()


class SomeViewWithAdditionalLoggingData(APIView):
    def get(self, request, *args, **kwargs):
        request._request.additional_data_to_log = {
            'ping': 'pong',
        }
        return Response()


class TestFrontendAuthView(APIView):
    def get(self, request, *args, **kwargs):
        return Response({
            'frontend': getattr(request, 'frontend', 'noooooo')
        })


class TestIpsView(APIView):
    def get(self, request, *args, **kwargs):
        return Response({
            'REMOTE_ADDR': request.META.get('REMOTE_ADDR'),
            'FRONTEND_REMOTE_ADDR': request.META.get('FRONTEND_REMOTE_ADDR'),
        })


urlpatterns = [
    url(r'test/$', SomeView.as_view(), name='form'),
    url(r'test-with-additional-data/$', SomeViewWithAdditionalLoggingData.as_view(), name='test-with-additional-data'),
    url(r'test-frontend-auth/$', TestFrontendAuthView.as_view(), name='frontend-auth'),
    url(r'test-ips/$', TestIpsView.as_view(), name='ips'),
]


class TestMessageModel(models.Model):
    name = models.CharField(max_length=100)

    class Meta:
        app_label = 'history'


class TestMessageModelSerializer(serializers.ModelSerializer):
    class Meta:
        model = TestMessageModel
        fields = (
            'name',
            'id',
        )


class TestMessageViewSet(viewsets.ModelViewSet):
    serializer_class = TestMessageModelSerializer
    queryset = TestMessageModel.objects.all()


router = routers.DefaultRouter()
router.root_view_name = 'main_api_path'
router.register('testmodels', TestMessageViewSet)
urlpatterns += router.urls


@override_settings(ROOT_URLCONF=__name__)
class TestFrontendAuthMiddleware(TestCase):
    @override_settings(FRONTENDS_BY_AUTH_KEY={
        '123': {'auth_key': '123', 'name': 'events'}
    })
    def test_should_authorize_frontend_if_it_has_valid_auth_key(self):
        headers = {
            'HTTP_X_FRONTEND_AUTHORIZATION': '123'
        }
        response = self.client.get('/test-frontend-auth/', **headers)
        expected = {
            'auth_key': '123',
            'name': 'events'
        }
        self.assertEqual(response.data['frontend'], expected)

    def test_should_not_authorize_frontend_if_it_has_not_valid_auth_key(self):
        headers = {
            'HTTP_X_FRONTEND_AUTHORIZATION': 'some-not-valid-key'
        }
        response = self.client.get('/test-frontend-auth/', **headers)
        self.assertEqual(response.data['frontend'], None)

    def test_should_not_authorize_frontend_if_it_has_no_auth_key_in_request(self):
        self.assertEqual(self.client.get('/test-frontend-auth/').data['frontend'], None)


@override_settings(ROOT_URLCONF=__name__)
class TestSetRemoterAddresses(TestCase):
    def test_should_use_default_remote_address_if_frontend_is_not_authorized(self):
        headers = {
            'REMOTE_ADDR': '1.2.3.4'
        }
        response = self.client.get('/test-ips/', **headers)
        self.assertEqual(response.data['REMOTE_ADDR'], '1.2.3.4')
        self.assertEqual(response.data['FRONTEND_REMOTE_ADDR'], '1.2.3.4')

    def test_should_use_value_from_real_ip_if_its_in_request(self):
        headers = {
            'REMOTE_ADDR': '1.2.3.4',
            'HTTP_X_REAL_IP': '2.3.4.5',
        }
        response = self.client.get('/test-ips/', **headers)
        self.assertEqual(response.data['REMOTE_ADDR'], '2.3.4.5')
        self.assertEqual(response.data['FRONTEND_REMOTE_ADDR'], '2.3.4.5')

    def test_should_use_default_remote_address_if_frontend_with_not_valid_auth_key(self):
        headers = {
            'REMOTE_ADDR': '1.2.3.4',
            'HTTP_X_FRONTEND_AUTHORIZATION': 'some-not-valid-key',
            'HTTP_X_USER_IP': '4.3.2.1'
        }
        response = self.client.get('/test-ips/', **headers)
        self.assertEqual(response.data['REMOTE_ADDR'], '1.2.3.4')
        self.assertEqual(response.data['FRONTEND_REMOTE_ADDR'], '1.2.3.4')

    @override_settings(FRONTENDS_BY_AUTH_KEY={
        '123': {'auth_key': '123', 'name': 'events'}
    })
    def test_should_use_user_ip_as_remote_addr_and_frontend_ip_is_frontend_is_authorized(self):
        headers = {
            'REMOTE_ADDR': '1.2.3.4',
            'HTTP_X_FRONTEND_AUTHORIZATION': '123',
            'HTTP_X_USER_IP': '4.3.2.1'
        }
        response = self.client.get('/test-ips/', **headers)
        self.assertEqual(response.data['REMOTE_ADDR'], '4.3.2.1')
        self.assertEqual(response.data['FRONTEND_REMOTE_ADDR'], '1.2.3.4')
