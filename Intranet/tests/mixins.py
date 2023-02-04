from typing import Union

from django.http import HttpResponse
from django.urls import reverse

from rest_framework import status
from rest_framework.response import Response


class UrlNameMixin:
    """
    Миксин позволяет определять url-name для тестируемой ручки

    Пример:
    ```
    class ExampleTestCase(UrlNameMixin, APITestCase):
        URL_NAME = 'api:example-list'

        def test_list(self):
            response = self.client.get(self.get_url(), format='json')
            ...
    ```

    можно передавать параметры в url:
    ```
    class ExampleTestCase(UrlNameMixin, APITestCase):
        URL_NAME = 'api:example-detail' # url вида: /example/<int:pk>/

        def test_list(self):
            item_id = 1
            response = self.client.get(self.get_url(item_id), format='json')
            ...
    ```

    """
    URL_NAME = ''

    def get_url(self, *args, **kwargs):
        return reverse(self.URL_NAME, args=args, kwargs=kwargs)

    def assertURLNameEqual(self, url: str, base_url=None, args=None, kwargs=None):  # noqa N802
        if base_url:
            url = f'/{base_url}{url}'

        args = args or []
        kwargs = kwargs or {}

        self.assertURLEqual(self.get_url(*args, **kwargs), url.format(*args, **kwargs))


class GenericRequestMixin:
    """
    Миксин для общих API запросов.
    Позволяет быстрее писать простые юнит-тесты по API ручкам
    """

    def list_request(
        self,
        url,
        expected,
        num_queries=1,
        pagination=True,
        count=None,
        check_ids=True,
        only_ids=False,
        check_errors=False,
    ):
        with self.assertNumQueries(num_queries):
            response: Union[Response, HttpResponse] = self.client.get(url, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)

        if check_errors:
            self.assert_errors(response.data, expected)

            return response

        if pagination:
            self.assertEqual(response.data.keys(), {'count', 'next', 'previous', 'results'})
            if count:
                self.assertEqual(response.data.get('count'), count or len(expected))

            results = response.data.get('results')
        else:
            results = response.data

        if check_ids or only_ids:
            result_ids = [r['id'] for r in results]
            expected_ids = [e['id'] for e in expected]
            self.assertListEqual(result_ids, expected_ids, f"\n{result_ids=}\n====\n{expected_ids=}")

        if not only_ids:
            self.assertListEqual(results, expected, f"\n{results=}\n====\n{expected=}")

        return response

    def detail_request(
        self,
        url,
        data='',
        expected=None,
        num_queries=1,
        status_code=None,
        method='get',
        check_errors=False
    ):
        if status_code is None:
            status_code = status.HTTP_200_OK
        request_method = getattr(self.client, method)

        with self.assertNumQueries(num_queries):
            response: Union[Response, HttpResponse] = request_method(url, data=data, format='json')
        self.assertEqual(response.status_code, status_code, response.data)

        if check_errors:
            self.assert_errors(response.data, expected)

            return response

        results = response.data
        if expected:
            if callable(expected):
                expected = expected(response)
            self.assertDictEqual(results, expected, f"\n{results}\n====\n{expected}")

        return response

    def update_request(
        self,
        url,
        data,
        expected=None,
        num_queries=1,
        status_code=None,
        method='put',
        check_errors=False
    ):
        return self.detail_request(url, data, expected, num_queries, status_code, method, check_errors)

    def partial_update_request(
        self,
        url,
        data,
        expected=None,
        num_queries=1,
        status_code=None,
        method='patch',
        check_errors=False,
    ):
        return self.update_request(url, data, expected, num_queries, status_code, method, check_errors)

    def create_request(
        self,
        url,
        data='',
        expected=None,
        num_queries=1,
        status_code=None,
        method='post',
        check_errors=False
    ):
        if status_code is None:
            status_code = status.HTTP_201_CREATED
        return self.detail_request(url, data, expected, num_queries, status_code, method, check_errors)

    def delete_request(self, url, num_queries=1, status_code=None, method='delete', expected=None, check_errors=False):
        if status_code is None:
            status_code = status.HTTP_204_NO_CONTENT
        request_method = getattr(self.client, method)

        with self.assertNumQueries(num_queries):
            response: Union[Response, HttpResponse] = request_method(url)
        self.assertEqual(response.status_code, status_code, response.data)

        if check_errors:
            self.assert_errors(response.data, expected)

        return response

    def assert_errors(self, data, expected_errors):
        if isinstance(data, list):
            for index, error in enumerate(data):
                self.assert_errors(error, expected_errors[index])

        elif isinstance(data, dict):
            for field, errors in data.items():
                self.assert_errors(errors, expected_errors[field])

        else:
            self.assertEqual(data.code, expected_errors)
            # TODO: внедрить проверку ошибок
            # self.assertEqual(data.code, expected_errors.get('code'))
            # expected_str = expected_errors.get('__str__')
            # if expected_str:
            #     self.assertEqual(str(data), expected_str)
