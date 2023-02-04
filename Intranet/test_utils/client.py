from typing import Optional
from django.contrib.auth import get_user_model
from django.test import Client
from django.test.client import MULTIPART_CONTENT

User = get_user_model()


class BetterRestClient(Client):
    MIMETYPE_JSON = 'application/json'
    FORM_URLENCODED = 'application/x-www-form-urlencoded'
    FORM_MULTIPART = MULTIPART_CONTENT
    user: Optional[User] = None
    tvm_client_id: str = '12345'
    mechanism_name: str = 'tvm'
    unset_organization = 'unset'
    client_ip: str = '192.168.0.1'

    def login(self, user):
        self.user = user
        return self

    def set_client_ip(self, client_ip):
        self.client_ip = client_ip
        return self

    def logout(self):
        self.user = None

    def use_tvm2(self, client_id=tvm_client_id):
        self.mechanism_name = 'tvm'
        self.tvm_client_id = client_id
        return self

    def use_cookie_auth(self):
        self.mechanism_name = 'cookie'
        return self

    def use_oauth(self):
        self.mechanism_name = 'oauth'
        return self

    def post(self, path, data=None, content_type=MIMETYPE_JSON, follow=False, **extra):
        if data is None:
            data = {}
        return super().post(path, data=data, content_type=content_type, follow=follow, **extra)

    def put(self, path, data=None, content_type=MIMETYPE_JSON, follow=False, **extra):
        if data is None:
            data = {}
        return super().put(path, data, content_type, follow, **extra)

    def delete(self, path, data=None, content_type=MIMETYPE_JSON, **extra):
        if data is None:
            data = {}
        return super().delete(path, data, content_type=content_type, **extra)

    def request(self, **request):

        if self.user:
            self.user.refresh_from_db()
            request['UNITTEST_USER'] = self.user

        request['UNITTEST_TVM_CLIENT'] = self.tvm_client_id
        request['UNITTEST_MECHANISM_NAME'] = self.mechanism_name

        request['UNITTEST_MECHANISM_NAME'] = self.mechanism_name
        request['HTTP_X_FORWARDED_FOR_Y'] = self.client_ip
        request['CONTENT_TYPE'] = request.get('CONTENT_TYPE', self.MIMETYPE_JSON)

        return super().request(**request)
