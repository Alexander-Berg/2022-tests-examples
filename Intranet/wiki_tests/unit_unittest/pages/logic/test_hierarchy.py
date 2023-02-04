
from wiki.pages.logic import hierarchy
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase


class HierarchyTest(BaseTestCase):
    def setUp(self):
        super().setUp()
        self.setUsers()

    def test_get_supertags_chain_without_self(self):
        data = (
            ('a', []),
            ('a/b', ['a']),
            ('a/b/c', ['a', 'a/b']),
        )

        for supertag, expected_parents in data:
            parents = hierarchy.get_supertags_chain(supertag)
            self.assertEqual(parents, expected_parents)

    def test_get_supertags_chain_with_self(self):
        data = (
            ('a', ['a']),
            ('a/b', ['a', 'a/b']),
            ('a/b/c', ['a', 'a/b', 'a/b/c']),
        )

        for supertag, expected_parents in data:
            parents = hierarchy.get_supertags_chain(supertag, include_self=True)
            self.assertEqual(parents, expected_parents)

    def test_get_nearest_parent(self):
        # сначала создадим длинный, потом короткий,
        # чтобы проверить, что находится ближайший
        self.create_page(supertag='one/two/three')
        self.create_page(supertag='one')

        data = (
            ('one/two', 'one'),
            ('one/two/three/four/five', 'one/two/three'),
            ('someroot', None),
        )

        for supertag, expected_parent_supertag in data:
            parent = hierarchy.get_nearest_existing_parent(supertag)
            if expected_parent_supertag is None:
                self.assertIsNone(parent)
            else:
                self.assertEqual(parent.supertag, expected_parent_supertag)

    def test_parent_existing(self):
        one = self.create_page(supertag='one')

        parent = hierarchy.get_parent('one/two')
        self.assertEqual(parent, one)

    def test_parent_not_existing(self):
        self.create_page(supertag='one')

        parent = hierarchy.get_parent('one/two/three')
        self.assertIsNone(parent)
