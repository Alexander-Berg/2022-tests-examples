from typing import Optional

from django.conf import settings
from django.contrib.auth import get_user_model

from intranet.wiki.tests.wiki_tests.common.wiki_client import WikiClient
from wiki.sync.connect.models import Organization
from django.test.client import MULTIPART_CONTENT

MIMETYPE_JSON = 'application/json'
User = get_user_model()


class RestApiClient(WikiClient):
    """
    REST API tests client.
    Provides authentification via __uid query param and "application/json" Content-Type
    """

    user: Optional[User] = None
    tvm_client_id: str = '12345'
    mechanism_name: str = 'tvm'
    unset_organization = 'unset'

    def __init__(self, *args, default_organization: Organization = unset_organization, **kwargs):
        super().__init__(*args, **kwargs)
        self.organization = default_organization

    def login(self, login_or_user, organization: Organization = unset_organization):
        if isinstance(login_or_user, str):  # as login
            self.user = User.objects.get(username=login_or_user)
        elif isinstance(login_or_user, User):  # as user
            self.user = login_or_user
        else:
            raise ValueError(f'User or loginname is expected, got {login_or_user} instead')

        if organization is not self.unset_organization:
            self.organization = organization
        return self

    def post(self, path, data=None, content_type=MIMETYPE_JSON, follow=False, **extra):
        if data is None:
            data = {}
        return super(RestApiClient, self).post(path, data=data, content_type=content_type, follow=follow, **extra)

    def put(self, path, data=None, content_type=MIMETYPE_JSON, follow=False, **extra):
        if data is None:
            data = {}
        return super(RestApiClient, self).put(path, data, content_type, follow, **extra)

    def put_multipart(self, path, data, content_type=MULTIPART_CONTENT, follow=False, **extra):
        data = self._encode_data(data, content_type)

        return super(RestApiClient, self).put(path, data, content_type, follow, **extra)

    def delete(self, path, data=None, content_type=MIMETYPE_JSON, **extra):
        if data is None:
            data = {}
        return super(RestApiClient, self).delete(path, data, content_type=content_type, **extra)

    def request(self, **request):
        """
        Add uid and format params to API query
        """

        # api return format
        # query_params['format'] = 'json'

        if self.user:
            self.user.refresh_from_db()
            request['UNITTEST_USER'] = self.user

        if self.organization is not self.unset_organization:
            organization = self.organization
        elif settings.IS_INTRANET:
            organization = None
        else:
            organization = Organization.objects.filter(dir_id='42').first()

        request['UNITTEST_ORGANIZATION'] = organization
        request['UNITTEST_TVM_CLIENT'] = self.tvm_client_id
        request['UNITTEST_MECHANISM_NAME'] = self.mechanism_name

        # api prefered content-type
        request['CONTENT_TYPE'] = request.get('CONTENT_TYPE', MIMETYPE_JSON)

        return super(RestApiClient, self).request(**request)

    def logout(self):
        self.user = None

    def use_tvm2(self, client_id=tvm_client_id):
        self.mechanism_name = 'tvm'
        self.tvm_client_id = client_id

    def use_cookie_auth(self):
        self.mechanism_name = 'cookie'

    def use_oauth(self):
        self.mechanism_name = 'oauth'
