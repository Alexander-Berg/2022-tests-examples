# -*- coding: utf-8 -*-
from django.test import TestCase, override_settings
from rest_framework.routers import DefaultRouter
from events.data_sources.filters import (
    CharListFilter,
    ListFilterSet,
    ListOrCommaSeparatedStringListFilter,
)
from events.data_sources.sources.base import (
    BaseDataSource,
    BaseDataSourceCategory,
    SimpleDataSourceSerializer,
)


class TestDataSourceCategory(BaseDataSourceCategory):
    name = 'test'
    title = 'Test'


class TestDataSourceSerializer(SimpleDataSourceSerializer):
    id_attr = 'id'
    text_attr = 'text'

    class Meta:
        fields = ('id', 'text')


class TestDataSourceFilter(ListFilterSet):
    id = ListOrCommaSeparatedStringListFilter(field_name=TestDataSourceSerializer.id_attr)
    text = CharListFilter(field_name=TestDataSourceSerializer.text_attr)
    text__startswith = CharListFilter(field_name=TestDataSourceSerializer.text_attr, lookup_expr='startswith')


class TestDataSource(BaseDataSource):
    name = 'test'
    title = 'Test'
    desc = 'Test data source'
    category = TestDataSourceCategory()
    allow_external_usage = True
    is_with_pagination = True
    filter_class = TestDataSourceFilter
    serializer_class = TestDataSourceSerializer

    def get_queryset(self):
        return [
            {
                'id': '1',
                'text': 'one',
            },
            {
                'id': '2',
                'text': 'two',
            },
            {
                'id': '3',
                'text': 'three',
            },
            {
                'id': '4',
                'text': 'four',
            },
            {
                'id': '5',
                'text': 'five',
            },
        ]


class TestDataSourceViewSet(TestDataSource.get_view_class()):
    data_source_class = TestDataSource


router = DefaultRouter()
router.root_view_name = 'main_api_path'
router.register(r'data-source/test', TestDataSourceViewSet, basename='data-source-test')
urlpatterns = router.urls


@override_settings(ROOT_URLCONF=__name__)
class TestListFilterSetDataSource(TestCase):
    def setUp(self):
        self.base_url = '/data-source/test/'

    def test_without_filters_should_return_all_items(self):
        response = self.client.get(self.base_url)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['results']), 0)

    def test_with_filter_in_should_return_two_items(self):
        response = self.client.get(f'{self.base_url}?id=2,5')
        self.assertEqual(response.status_code, 200)
        data = {
            it['id']: it['text']
            for it in response.data['results']
        }
        expected = {
            '2': 'two',
            '5': 'five',
        }
        self.assertEqual(len(data), len(expected))
        self.assertDictEqual(data, expected)

    def test_with_filter_in_should_return_one_item(self):
        response = self.client.get(f'{self.base_url}?id=2,2,6')
        self.assertEqual(response.status_code, 200)
        data = {
            it['id']: it['text']
            for it in response.data['results']
        }
        expected = {
            '2': 'two',
        }
        self.assertEqual(len(data), len(expected))
        self.assertDictEqual(data, expected)

    def test_with_filter_in_shouldnt_return_any_item(self):
        response = self.client.get(f'{self.base_url}?id=6')
        self.assertEqual(response.status_code, 200)
        data = {
            it['id']: it['text']
            for it in response.data['results']
        }
        expected = {
        }
        self.assertEqual(len(data), len(expected))
        self.assertDictEqual(data, expected)

    def test_with_filter_id_without_param_should_return_all_items(self):
        response = self.client.get(f'{self.base_url}?id=')
        self.assertEqual(response.status_code, 200)
        data = {
            it['id']: it['text']
            for it in response.data['results']
        }
        expected = {
            '1': 'one',
            '2': 'two',
            '3': 'three',
            '4': 'four',
            '5': 'five',
        }
        self.assertEqual(len(data), len(expected))
        self.assertDictEqual(data, expected)

    def test_with_filter_exact_should_return_one_item(self):
        response = self.client.get(f'{self.base_url}?text=two')
        self.assertEqual(response.status_code, 200)
        data = {
            it['id']: it['text']
            for it in response.data['results']
        }
        expected = {
            '2': 'two',
        }
        self.assertEqual(len(data), len(expected))
        self.assertDictEqual(data, expected)

    def test_with_filter_exact_shouldnt_return_any_item(self):
        response = self.client.get(f'{self.base_url}?text=six')
        self.assertEqual(response.status_code, 200)
        data = {
            it['id']: it['text']
            for it in response.data['results']
        }
        expected = {
        }
        self.assertEqual(len(data), len(expected))
        self.assertDictEqual(data, expected)

    def test_with_filter_text_without_param_should_return_all_items(self):
        response = self.client.get(f'{self.base_url}?text=')
        self.assertEqual(response.status_code, 200)
        data = {
            it['id']: it['text']
            for it in response.data['results']
        }
        expected = {
            '1': 'one',
            '2': 'two',
            '3': 'three',
            '4': 'four',
            '5': 'five',
        }
        self.assertEqual(len(data), len(expected))
        self.assertDictEqual(data, expected)

    def test_with_filter_startswith_should_return_one_item(self):
        response = self.client.get(f'{self.base_url}?text__startswith=th')
        self.assertEqual(response.status_code, 200)
        data = {
            it['id']: it['text']
            for it in response.data['results']
        }
        expected = {
            '3': 'three',
        }
        self.assertEqual(len(data), len(expected))
        self.assertDictEqual(data, expected)

    def test_with_filter_startswith_should_return_two_items(self):
        response = self.client.get(f'{self.base_url}?text__startswith=f')
        self.assertEqual(response.status_code, 200)
        data = {
            it['id']: it['text']
            for it in response.data['results']
        }
        expected = {
            '4': 'four',
            '5': 'five',
        }
        self.assertEqual(len(data), len(expected))
        self.assertDictEqual(data, expected)

    def test_with_filter_startswith_shouldnt_return_any_item(self):
        response = self.client.get(f'{self.base_url}?text__startswith=s')
        self.assertEqual(response.status_code, 200)
        data = {
            it['id']: it['text']
            for it in response.data['results']
        }
        expected = {
        }
        self.assertEqual(len(data), len(expected))
        self.assertDictEqual(data, expected)

    def test_with_filter_startswith_without_param_should_return_all_items(self):
        response = self.client.get(f'{self.base_url}?text__startswith=')
        self.assertEqual(response.status_code, 200)
        data = {
            it['id']: it['text']
            for it in response.data['results']
        }
        expected = {
            '1': 'one',
            '2': 'two',
            '3': 'three',
            '4': 'four',
            '5': 'five',
        }
        self.assertEqual(len(data), len(expected))
        self.assertDictEqual(data, expected)
