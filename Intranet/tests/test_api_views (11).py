import faker

from django.conf import settings

from rest_framework import status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin
from lms.users.tests.factories import UserFactory

from ..models import UserTag
from ..utils import normalize_tag_name
from .factories import TagFactory, UserTagFactory

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

    def test_list(self):
        expected = self.build_expected_list(self.tags)
        self.list_request(url=self.get_url(), expected=expected, num_queries=4)


class MyTagsViewSetTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:my-tags'

    def setUp(self):
        self.user = UserFactory()

    def test_url(self):
        self.assertURLNameEqual('my/tags/', args=(), base_url=settings.API_BASE_URL)

    def test_no_auth(self):
        self.update_request(url=self.get_url(), data={}, status_code=status.HTTP_403_FORBIDDEN, num_queries=0)

    def test_update(self):
        tags = TagFactory.create_batch(10)
        for tag in tags[:5]:
            UserTagFactory(user=self.user, tag=tag)
        tag_names = [tag.name for tag in tags[2:7]] + [f"{fake.word()}-new" for _ in range(3)]

        data = {
            "tags": tag_names
        }

        self.client.force_login(user=self.user)
        self.update_request(
            url=self.get_url(),
            data=data,
            status_code=status.HTTP_204_NO_CONTENT,
            num_queries=15
        )
        user_tags = set(UserTag.objects.filter(user_id=self.user.id).values_list('tag__normalized_name', flat=True))
        expected_tags = {normalize_tag_name(tag) for tag in tag_names}
        self.assertEqual(user_tags, expected_tags)

    def test_list(self):
        user_tags = UserTagFactory.create_batch(5, user=self.user)
        UserTagFactory.create_batch(10)

        expected = [
            {
                "id": user_tag.tag.id,
                "name": user_tag.tag.name,
                "normalized_name": user_tag.tag.normalized_name,
            }
            for user_tag in sorted(user_tags, key=lambda x: x.tag.normalized_name)
        ]

        self.client.force_login(user=self.user)
        self.list_request(url=self.get_url(), expected=expected, num_queries=4, check_ids=False)
