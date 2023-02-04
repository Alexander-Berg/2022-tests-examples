import faker

from django.conf import settings

from rest_framework import serializers
from rest_framework.test import APITestCase

from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin
from lms.users.tests.factories import LabUserFactory

from ..models import Tag
from ..utils import normalize_tag_name
from .factories import TagFactory

fake = faker.Faker()


class TagLabViewSetCreateTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:tag-create'

    def setUp(self):
        self.user = LabUserFactory(is_staff=True)
        self.tags = TagFactory.create_batch(5)
        self.client.force_login(user=self.user)

    def build_payload(self, tag: Tag):
        return {
            "name": tag.name,
        }

    def build_expected_list(self, tags: list):
        return [
            {
                'id': tag.id,
                'name': tag.name,
                'normalized_name': tag.normalized_name,
                'created': serializers.DateTimeField().to_representation(tag.created),
                'modified': serializers.DateTimeField().to_representation(tag.modified),
            }
            for tag in sorted(tags, key=lambda x: x.normalized_name)
        ]

    def test_url(self):
        self.assertURLNameEqual('tags/', args=(), base_url=settings.LABAPI_BASE_URL)

    def test_create(self):
        tag = TagFactory.build()

        def expected(response):
            return {
                'id': response.data['id'],
                'name': tag.name,
                'normalized_name': normalize_tag_name(tag.name),
                'created': serializers.DateTimeField().to_representation(response.data['created']),
                'modified': serializers.DateTimeField().to_representation(response.data['modified']),
            }

        self.create_request(self.get_url(), data=self.build_payload(tag), expected=expected, num_queries=5)

    def test_list(self):
        expected = self.build_expected_list(self.tags)
        self.list_request(url=self.get_url(), expected=expected, num_queries=6)


class TagLabViewSetDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:tag-detail'

    def setUp(self):
        self.user = LabUserFactory(is_staff=True)
        self.tag = TagFactory()
        self.client.force_login(user=self.user)

    def build_payload(self, tag: Tag):
        return {
            "name": tag.name,
        }

    def test_url(self):
        self.assertURLNameEqual(
            'tags/{}/',
            args=(self.tag.id,),
            base_url=settings.LABAPI_BASE_URL
        )

    def test_detail(self):
        expected = {
            'id': self.tag.id,
            'name': self.tag.name,
            'normalized_name': self.tag.normalized_name,
            'created': serializers.DateTimeField().to_representation(self.tag.created),
            'modified': serializers.DateTimeField().to_representation(self.tag.modified),
        }

        self.detail_request(
            self.get_url(self.tag.id),
            expected=expected,
            num_queries=5
        )

    def test_update(self):
        tag = TagFactory()
        new_tag = TagFactory.build()
        request_payload = self.build_payload(new_tag)

        def expected(response):
            return {
                'id': tag.id,
                'name': new_tag.name,
                'normalized_name': normalize_tag_name(new_tag.name),
                'created': serializers.DateTimeField().to_representation(tag.created),
                'modified': serializers.DateTimeField().to_representation(response.data['modified']),
            }

        self.update_request(
            self.get_url(tag.id),
            data=request_payload,
            expected=expected,
            num_queries=6
        )

    def test_delete(self):
        tag = TagFactory()
        self.delete_request(
            self.get_url(tag.id),
            num_queries=8
        )
