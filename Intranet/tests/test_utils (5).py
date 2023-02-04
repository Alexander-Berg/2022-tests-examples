from django.test import TestCase

from ..utils import normalize_tag_name


class NormalizeTagNameTestCase(TestCase):
    def test_normalize_tag_names(self):
        tags = {
            'Tag': 'tag',
            'Тег': 'тег',
            'teSt tag с КириллиЦей': 'test-tag-с-кириллицей',
            'tag with spaces': 'tag-with-spaces',
            'tag #^   * -- with symbols ()+=&%§ *7': 'tag-with-symbols-7',
        }

        for tag, expected in tags.items():
            assert normalize_tag_name(tag) == expected
