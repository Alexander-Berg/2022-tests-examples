import faker

from django.conf import settings

from rest_framework.test import APITestCase

from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin
from lms.users.tests.factories import UserFactory

from ..utils import normalize_tag_name
from .factories import TagFactory

fake = faker.Faker()


class TagViewSetListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:tag-list'

    def setUp(self):
        self.user = UserFactory()
        self.tags = TagFactory.create_batch(5)
        self.client.force_login(user=self.user)

    def build_expected_list(self, tags: list):
        return [
            {
                'id': tag.id,
                'name': tag.name,
                'normalized_name': tag.normalized_name,
            }
            for tag in sorted(tags, key=lambda x: x.normalized_name)
        ]

    def test_url(self):
        self.assertURLNameEqual('tags/', args=(), base_url=settings.API_BASE_URL)

    def test_list_limit(self):
        expected = self.build_expected_list(self.tags)[:3]
        self.list_request(url=f"{self.get_url()}?limit=3", expected=expected, num_queries=4)

    def test_list_offset(self):
        expected = self.build_expected_list(self.tags)[2:]
        self.list_request(url=f"{self.get_url()}?offset=2", expected=expected, num_queries=4)

    def test_list_filter_name(self):
        tag = TagFactory(name=fake.pystr())
        search_name = normalize_tag_name(tag.name)
        expected = self.build_expected_list([tag])

        self.list_request(url=f"{self.get_url()}?name={search_name}", expected=expected, num_queries=4)
