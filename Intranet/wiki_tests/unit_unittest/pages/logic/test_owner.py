
from mock import Mock, patch

from wiki.pages.logic import owner as owner_logic
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase


@patch('wiki.pages.logic.owner.signals.access_changed', Mock())
class OwnerLogicTest(BaseTestCase):
    def setUp(self):
        self.volozh = self.get_or_create_user('volozh')
        self.thasonic = self.get_or_create_user('thasonic')

        self.root = self.create_page(supertag='root', tag='root', owner=self.volozh)
        self.one = self.create_page(supertag='root/one', tag='root/one', owner=self.thasonic)
        self.two = self.create_page(supertag='root/two', tag='root/two', owner=self.volozh)

    def test_change_owner_for_page(self):
        owner_logic.change_owner_for_page(
            page=self.one,
            user=self.thasonic,
            new_owner=self.volozh,
        )

        page = self.refresh_objects(self.one)
        self.assertEqual(page.owner, self.volozh)

    def test_change_owner_for_cluster_by_root_owner(self):
        owner_logic.change_owner_for_cluster(
            root=self.root,
            user=self.volozh,
            new_owner=self.thasonic,
        )

        root, one, two = self.refresh_objects(self.root, self.one, self.two)
        self.assertEqual(root.owner, self.thasonic)
        self.assertEqual(one.owner, self.thasonic)
        self.assertEqual(two.owner, self.thasonic)

    def test_change_owner_for_cluster_by_somebody(self):
        owner_logic.change_owner_for_cluster(
            root=self.root,
            user=self.thasonic,
            new_owner=self.volozh,
        )

        root, one, two = self.refresh_objects(self.root, self.one, self.two)
        self.assertEqual(root.owner, self.volozh)
        self.assertEqual(one.owner, self.thasonic)
        self.assertEqual(two.owner, self.volozh)

    def test_change_owner_for_cluster_with_old_owner(self):
        owner_logic.change_owner_for_cluster(
            root=self.root,
            user=self.thasonic,
            new_owner=self.volozh,
            old_owner=self.thasonic,
        )

        root, one, two = self.refresh_objects(self.root, self.one, self.two)
        self.assertEqual(root.owner, self.volozh)
        self.assertEqual(one.owner, self.volozh)
        self.assertEqual(two.owner, self.volozh)

    def test_change_owner_for_cluster_with_old_owner_by_subpage_owner(self):
        owner_logic.change_owner_for_cluster(
            root=self.root,
            user=self.volozh,
            new_owner=self.thasonic,
            old_owner=self.volozh,
        )

        root, one, two = self.refresh_objects(self.root, self.one, self.two)
        self.assertEqual(root.owner, self.thasonic)
        self.assertEqual(one.owner, self.thasonic)
        self.assertEqual(two.owner, self.thasonic)
