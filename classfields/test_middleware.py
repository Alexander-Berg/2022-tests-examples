import json
import requests_mock
from furl import furl
from django.conf import settings
from django.contrib.auth.models import Group
from django.test import TestCase, override_settings


@override_settings(
    MIDDLEWARE=[
        "django.contrib.sessions.middleware.SessionMiddleware",
        "django.contrib.auth.middleware.AuthenticationMiddleware",
        "catalogue.middleware.UserIPMiddleware",
        "catalogue.middleware.IdmApiAuthMiddleware",
    ],
    STATICFILES_STORAGE='django.contrib.staticfiles.storage.StaticFilesStorage'
)
class IdmAuthMiddlewareTest(TestCase):
    SUPERUSER_ROLES = {
        "login": "test",
        "roles": [
            "/internal/USERGROUPS/feature/DEVELOPER/role/FULL/",
            "/internal/AUTOPARTSADMIN/feature/ROLES/role/SUPERUSER/",
        ]
    }
    USER_ROLES = {
        "login": "test",
        "roles": [
            "/internal/AUTOPARTSADMIN/feature/ROLES/role/GROUP1/",
            "/internal/AUTOPARTSADMIN/feature/ROLES/role/GROUP2/",
            "/internal/AUTOPARTSADMIN/feature/ROLES/role/GROUPP3/",
        ]
    }

    def setUp(self):
        Group.objects.bulk_create([Group(id=i, name=f'test_{i}') for i in range(1, 10)])

    def test_no_session_cookie(self):
        """
        Check, that without cookie user is redirected to login page
        :return:
        """
        path = "/admin/"
        resp = self.client.get(path)
        self.assertEqual(resp.status_code, 302)
        self.assertTrue(resp.url.startswith("https://passport.yandex-team.ru/passport?mode=auth&retpath="))
        url_parsed = furl(resp.url)
        self.assertEqual(furl(url_parsed.args["retpath"]).pathstr, path)

    def test_bad_session_cookie(self):
        """
        Check, that without valid session cookie user is redirected to login page
        :return:
        """
        cookies = self.client.cookies
        cookies.load({settings.Y_SESSION_COOKIE: "somesessioncookie"})
        with requests_mock.Mocker() as m:
            m.get(f"{settings.IDM_API_HOST}/get-user", status_code=403)
            resp = self.client.get("/")
            self.assertEqual(resp.status_code, 302)
            self.assertTrue(resp.url.startswith("https://passport.yandex-team.ru/passport?mode=auth&retpath="))

    def login_superuser(self):
        cookies = self.client.cookies
        cookies.load({settings.Y_SESSION_COOKIE: "somesessioncookie"})
        with requests_mock.Mocker() as m:
            m.get(f"{settings.IDM_API_HOST}/get-user", text=json.dumps(self.SUPERUSER_ROLES))
            resp = self.client.get("/admin/")
        return resp

    def test_good_session_cookie(self):
        """
        Check, that with valid session cookie user has access to the site
        :return:
        """
        resp = self.login_superuser()
        self.assertTrue(resp.status_code, 200)
        user = resp.wsgi_request.user
        self.assertEqual(user.is_superuser, True)
        self.assertEqual(user.groups.count(), 0)
        # check that session persists
        resp = self.client.get("/admin/")
        self.assertEqual(resp.wsgi_request.user.is_superuser, True)

    def test_change_user_roles(self):
        """
        Check, that changes in user roles are are supported
        :return:
        """
        resp = self.login_superuser()
        first_user = resp.wsgi_request.user
        with requests_mock.Mocker() as m:
            m.get(f"{settings.IDM_API_HOST}/get-user", text=json.dumps(self.USER_ROLES))
            self.client.cookies.pop("sessionid")
            resp = self.client.get("/admin/")
            second_user = resp.wsgi_request.user
            self.assertEqual(first_user.id, second_user.id)
            self.assertEqual(second_user.is_superuser, False)
            self.assertEqual(set(second_user.groups.all().values_list("id", flat=True)), {1, 2})

    def test_session_persistence(self):
        """
        Check, that session persists, and we don't make additional idm requests
        :return:
        """
        self.login_superuser()
        resp = self.client.get("/admin/")
        self.assertTrue(resp.status_code, 200)
        user = resp.wsgi_request.user
        self.assertEqual(user.is_superuser, True)
        self.assertEqual(user.groups.count(), 0)

    def test_unknown_idm_response(self):
        """
        Check, that on idm fail user does not have access to the site
        :return:
        """
        cookies = self.client.cookies
        cookies.load({settings.Y_SESSION_COOKIE: "somesessioncookie"})
        with requests_mock.Mocker() as m:
            m.get(f"{settings.IDM_API_HOST}/get-user", status_code=400)
            resp = self.client.get("/admin/")
            self.assertTrue(resp.status_code, 403)
