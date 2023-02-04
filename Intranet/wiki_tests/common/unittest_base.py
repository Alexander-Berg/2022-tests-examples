from ujson import loads

from wiki.utils import timezone
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase


class BaseApiTestCase(BaseTestCase):
    api_url = '/_api/frontend'
    handler = None
    default_page = None

    def check_invalid_data(self, response, error_key=None, error_value=None):
        return self.check_invalid_form(response, error_key, error_value)

    def check_invalid_form(self, response, error_key=None, error_value=None, error_code='CLIENT_SENT_INVALID_DATA'):
        self.assertEqual(response.status_code, 409)

        json = loads(response.content)
        error = json['error']

        self.assertEqual(error['error_code'], error_code)

        if error_key is None:
            actual_error_value = error['message']
        else:
            actual_error_value = error['errors'][error_key]

        if error_value:
            if not isinstance(error_value, list):
                error_value = [error_value]
            if not isinstance(error_value, list):
                error_value = [error_value]
            self.assertEqual(actual_error_value, error_value)
        elif error_key:
            self.assertTrue(len(error['errors'][error_key]) > 0)

        return actual_error_value

    def check_error(self, response, status_code, error_code):
        self.assertEqual(response.status_code, status_code, msg='[%s] %s' % (response.status_code, response.content))
        json = loads(response.content)
        error = json['error']
        self.assertEqual(error['error_code'], error_code)

    def get_url(self, page=None, placeholders=None, query_params=None):
        """
        Если в урле должен быть тег, то нужно передать page.
        """
        query_params = query_params or {}

        parts = [self.api_url]
        page = page or self.default_page
        if page:
            parts.append(page.supertag)
        if self.handler:
            if placeholders:
                if isinstance(placeholders, dict):
                    handler_formatted = self.handler.format(**placeholders)
                else:
                    handler_formatted = self.handler.format(*placeholders)
            else:
                handler_formatted = self.handler

            parts.append('.' + handler_formatted)

        url = '/'.join(parts)
        if query_params:
            url += '?' + '&'.join(['%s=%s' % (k, v) for (k, v) in list(query_params.items())])
        return url

    def request(self, method, **kwargs):
        status = kwargs.pop('status', None)
        data = kwargs.pop('data', {})
        url = self.get_url(**kwargs)

        client_method = getattr(self.client, method)
        response = client_method(url, data=data)
        if status is not None:
            self.assertEqual(response.status_code, status)
        response_data = loads(response.content)
        return response_data.get('data') or response_data.get('error')

    def get(self, **kwargs):
        return self.request('get', **kwargs)

    def post(self, **kwargs):
        return self.request('post', **kwargs)

    def put(self, **kwargs):
        return self.request('put', **kwargs)

    def delete(self, **kwargs):
        return self.request('delete', **kwargs)


def now_for_tests():
    return timezone.now()
