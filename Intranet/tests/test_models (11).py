import faker

from django.db import IntegrityError
from django.test import TestCase

from ..utils import normalize_tag_name
from .factories import TagFactory

fake = faker.Faker()


class TagModelTestCase(TestCase):
    def test_normalized_name(self):
        tag = TagFactory()
        self.assertEqual(normalize_tag_name(tag.name), tag.normalized_name)
        tag.name = fake.pystr()
        tag.save()
        self.assertEqual(normalize_tag_name(tag.name), tag.normalized_name)

    def test_normalized_name_unique(self):
        tag = TagFactory()
        with self.assertRaises(IntegrityError):
            TagFactory(name=tag.normalized_name)
