import json
from json import JSONDecodeError

import pytest
from django.conf import settings
from django.test import Client


class ApiClient(Client):

    def __init__(self, *, monkeypatch=None, enforce_csrf_checks=False, auth=None,  **defaults):
        super().__init__(enforce_csrf_checks, **defaults)
        self.service_alias = None
        self.monkeypatch = monkeypatch
        self.auth = auth

    def mock_service(self, service_id):
        self.monkeypatch.setattr('bcl.api.views.base.view.Service.tvm_ids', {settings.BCL_TVM_ID_TEST: service_id})

    def post(self, path, data=None, content_type='application/json', follow=False, secure=False, **extra):
        return super().post(path, data, content_type, follow, secure, **extra)

    def generic(self, method, path, data='', content_type='', **kwargs):
        service_alias = self.service_alias

        if service_alias:
            kwargs['HTTP_X_BCL_SERVICE_ALIAS'] = service_alias

        response = super().generic(method, path, data, content_type, **kwargs)
        contents = response.content.decode()

        try:
            response.json = json.loads(contents)

        except JSONDecodeError:
            raise ValueError(f'Non JSON:\n{contents}')

        response.ok = response.status_code == 200

        return response


@pytest.fixture
def api_client_bare(patch_tvm_auth):
    return ApiClient(auth=patch_tvm_auth)


@pytest.fixture
def api_client(api_client_bare, monkeypatch):
    api_client_bare.monkeypatch = monkeypatch
    api_client_bare.auth(settings.BCL_TVM_ID_TEST)
    return api_client_bare
