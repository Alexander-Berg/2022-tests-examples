from urllib.parse import urljoin

from django.conf import settings
from django.urls import reverse

from rest_framework import serializers, status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin

from .factories import CourseFactory


class CourseIndexListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'index_api:course-index-list'

    def test_url(self):
        self.assertURLNameEqual('courses/', base_url=settings.INDEX_API_BASE_URL)

    def test_list(self):
        CourseFactory(is_archive=True, is_active=True, show_in_catalog=True)
        CourseFactory(is_archive=False, is_active=False, show_in_catalog=True)
        CourseFactory(is_archive=False, is_active=True, show_in_catalog=False)

        course = CourseFactory(is_archive=False, is_active=True, show_in_catalog=True)

        detail_url = reverse('index_api:course-index-detail', args=(course.pk,))

        expected = [{
            'id': course.pk,
            'created': serializers.DateTimeField().to_representation(course.created),
            'updated': serializers.DateTimeField().to_representation(course.modified),
            'name': {
                'ru': course.name,
                'language': 'ru',
            },
            'content': {
                'ru': course.description,
                'language': 'ru',
            },
            'url': course.frontend_url,
            'api_url': urljoin('http://testserver/', detail_url),
        }]

        self.list_request(url=self.get_url(), expected=expected, only_ids=False, num_queries=2)


class CourseIndexDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'index_api:course-index-detail'

    def test_url(self):
        self.assertURLNameEqual('courses/{}/', args=(1,), base_url=settings.INDEX_API_BASE_URL)

    def test_detail(self):
        course = CourseFactory(is_archive=False, is_active=True, show_in_catalog=True)

        url = self.get_url(course.pk)

        expected = {
            'id': course.pk,
            'created': serializers.DateTimeField().to_representation(course.created),
            'updated': serializers.DateTimeField().to_representation(course.modified),
            'name': {
                'ru': course.name,
                'language': 'ru',
            },
            'content': {
                'ru': course.description,
                'language': 'ru',
            },
            'url': course.frontend_url,
            'api_url': urljoin('http://testserver/', url),
        }

        self.detail_request(url, expected=expected, num_queries=1)

    def test_not_found(self):
        course = CourseFactory(is_archive=True, is_active=True, show_in_catalog=True)

        self.detail_request(
            self.get_url(course.pk),
            check_errors=True,
            expected={'detail': 'not_found'},
            status_code=status.HTTP_404_NOT_FOUND,
            num_queries=1
        )

        course = CourseFactory(is_archive=False, is_active=False, show_in_catalog=True)

        self.detail_request(
            self.get_url(course.pk),
            check_errors=True,
            expected={'detail': 'not_found'},
            status_code=status.HTTP_404_NOT_FOUND,
            num_queries=1
        )

        course = CourseFactory(is_archive=False, is_active=True, show_in_catalog=False)

        self.detail_request(
            self.get_url(course.pk),
            check_errors=True,
            expected={'detail': 'not_found'},
            status_code=status.HTTP_404_NOT_FOUND,
            num_queries=1
        )
