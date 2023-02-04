import mock

from django.test.client import Client


class CauthPublicClient(Client):
    def __init__(self, *args, **kwargs):
        super(CauthPublicClient, self).__init__(*args, **kwargs)
        self.server = None

    def request(self, **request):
        request.update({
            'HTTP_USER_AGENT': 'CAUTH/yandex-cauth-1.3.4',
        })

        if self.server is None:
            return super(CauthPublicClient, self).request(**request)

        with mock.patch(
            target='infra.cauth.server.public.api.views.base.BaseView.remote_server',
            new_callable=mock.PropertyMock,
        ) as remote_server:
            remote_server.return_value = self.server
            return super(CauthPublicClient, self).request(**request)
