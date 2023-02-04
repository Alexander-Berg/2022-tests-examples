import faker

from django.test import TestCase

from lms.users.tests.factories import UserFactory

from ..models import UserTag
from ..services import update_user_tags
from ..utils import normalize_tag_name
from .factories import TagFactory

fake = faker.Faker()


class UpdateUserTagsServiceTestCase(TestCase):
    def setUp(self):
        self.user = UserFactory()

    def test_add_new_tags(self):
        tags = [f"{fake.word()}-{i}" for i in range(3)]
        with self.assertNumQueries(11):
            update_user_tags(user=self.user, tags=tags)
        user_tags = set(UserTag.objects.filter(user_id=self.user.id).values_list('tag__name', flat=True))
        self.assertEqual(set(tags), user_tags)

    def test_add_existing_tags(self):
        tags = TagFactory.create_batch(3)
        tag_names = [tag.name for tag in tags]
        with self.assertNumQueries(8):
            update_user_tags(user=self.user, tags=tag_names)
        user_tags = set(UserTag.objects.filter(user_id=self.user.id).values_list('tag_id', flat=True))
        tag_ids = {tag.id for tag in tags}
        self.assertEqual(tag_ids, user_tags)

    def test_update_tags(self):
        current_tags = TagFactory.create_batch(5)
        for tag in current_tags:
            UserTag.objects.create(user_id=self.user.id, tag_id=tag.id)
        existing_tags = TagFactory.create_batch(5)
        new_tags = [f"{fake.word()}-{i+10}" for i in range(3)]
        updated_tag_names = [tag.name for tag in current_tags[:2]] + [tag.name for tag in existing_tags[:3]] + new_tags
        with self.assertNumQueries(14):
            update_user_tags(user=self.user, tags=updated_tag_names)
        user_tags = set(UserTag.objects.filter(user_id=self.user.id).values_list('tag__normalized_name', flat=True))
        expected_tags = {normalize_tag_name(tag) for tag in updated_tag_names}
        self.assertEqual(user_tags, expected_tags)

    def test_clear_tags(self):
        current_tags = TagFactory.create_batch(5)
        for tag in current_tags:
            UserTag.objects.create(user_id=self.user.id, tag_id=tag.id)
        with self.assertNumQueries(3):
            update_user_tags(user=self.user, tags=[])
        user_tags = UserTag.objects.filter(user_id=self.user.id)
        self.assertEqual(len(user_tags), 0)
