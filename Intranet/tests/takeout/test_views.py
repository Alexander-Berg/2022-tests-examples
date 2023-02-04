# -*- coding: utf-8 -*-
from django.conf import settings

from events.accounts.factories import UserFactory
from events.takeout.models import TakeoutOperation
from events.yauth_contrib import helpers


class TestTakeoutViewSet__tvm_auth(helpers.TVMAuthTestCase):
    fixtures = ['initial_data.json']

    def test_status(self):
        user = UserFactory()
        self.client.set_user_ticket(user.uid)

        self.client.set_service_ticket(settings.IDM_TVM2_CLIENT)
        response = self.client.get('/1/takeout/status/')
        self.assertEqual(response.status_code, 403)

        self.client.set_service_ticket(settings.TAKEOUT2_TVM2_CLIENT)
        response = self.client.get('/1/takeout/status/')
        self.assertEqual(response.status_code, 200)

        self.client.set_service_ticket(settings.TVM2_CLIENT_ID)
        response = self.client.get('/1/takeout/status/')
        self.assertEqual(response.status_code, 200)

    def test_delete(self):
        user = UserFactory()
        self.client.set_user_ticket(user.uid)

        self.client.set_service_ticket(settings.IDM_TVM2_CLIENT)
        response = self.client.post('/1/takeout/delete/')
        self.assertEqual(response.status_code, 403)

        self.client.set_service_ticket(settings.TAKEOUT2_TVM2_CLIENT)
        response = self.client.post('/1/takeout/delete/')
        self.assertEqual(response.status_code, 200)

        self.client.set_service_ticket(settings.TVM2_CLIENT_ID)
        response = self.client.post('/1/takeout/delete/')
        self.assertEqual(response.status_code, 200)


class TestTakeoutViewSet__cookie_auth(helpers.CookieAuthTestCase):
    fixtures = ['initial_data.json']

    def test_status(self):
        user = UserFactory()
        self.client.set_cookie(user.uid)

        response = self.client.get('/1/takeout/status/')
        self.assertEqual(response.status_code, 403)

        user.is_superuser = True
        user.save()
        response = self.client.get('/1/takeout/status/')
        self.assertEqual(response.status_code, 200)

    def test_delete(self):
        user = UserFactory()
        self.client.set_cookie(user.uid)

        response = self.client.post('/1/takeout/delete/')
        self.assertEqual(response.status_code, 403)

        user.is_superuser = True
        user.save()
        response = self.client.post('/1/takeout/delete/')
        self.assertEqual(response.status_code, 200)


class TestTakeoutViewSet(helpers.CookieAuthTestCase):
    fixtures = ['initial_data.json']

    def test_status(self):
        user = UserFactory(is_superuser=True)
        self.client.set_cookie(user.uid)

        response = self.client.get('/1/takeout/status/?request_id=abcd')
        self.assertEqual(response.status_code, 200)
        expected = {
            'status': 'ok',
            'data': [{'id': '1', 'slug': 'answers', 'state': 'empty'}],
        }
        self.assertEqual(response.data, expected)

    def test_delete(self):
        user = UserFactory(is_superuser=True)
        self.client.set_cookie(user.uid)

        response = self.client.post('/1/takeout/delete/?request_id=abcd')
        self.assertEqual(response.status_code, 200)
        expected = {'status': 'ok'}
        self.assertEqual(response.data, expected)

        op = TakeoutOperation.objects.get(user=user)
        self.assertEqual(op.request_id, 'abcd')
