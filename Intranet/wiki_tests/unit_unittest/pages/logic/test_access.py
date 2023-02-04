
from wiki.pages.logic.access import get_access
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase


class PageAccessTest(BaseTestCase):
    def setUp(self):
        super(PageAccessTest, self).setUp()
        self.root = self.create_page(supertag='root')
        self.child = self.create_page(supertag='root/child')
        self.restrictions = {
            'users': self.root.get_authors(),
            'groups': [self.create_group()],
        }

    def _set_access(self, page, type_, restrictions=None):
        page.access_set.all().delete()
        if type_ == 'common':
            if page.parent:
                page.access_set.create(is_common=True)
        if type_ == 'anonymous':
            page.access_set.create(is_anonymous=True)
        if type_ == 'owner':
            page.access_set.create(is_owner=True)
        if type_ == 'inherited':
            # no access object in db
            pass
        if type_ == 'restricted':
            for user in restrictions.get('users', []):
                page.access_set.create(staff=user.staff)
            for group in restrictions.get('groups', []):
                page.access_set.create(group=group)

    def assert_access_type(self, page, type_, effective_type):
        access = get_access(page)
        self.assertEqual(access.type, type_)
        self.assertEqual(access.effective_type, effective_type)

        types = (
            'inherited',
            'anonymous',
            'owner',
            'common',
            'restricted',
        )
        for available_type in types:
            value = getattr(access, 'is_' + available_type)
            if type_ == available_type:
                msg = 'is_%s should be True'
                self.assertTrue(value, msg)
            else:
                msg = 'is_%s should be False'
                self.assertFalse(value, msg)

    def assert_access_restrictions(self, page, restrictions):
        access = get_access(page)
        restricted_users = access.restrictions['users']
        restricted_groups = access.restrictions['groups']
        self.assertSetEqual(set(restricted_users), set(restrictions.get('users', [])))
        self.assertSetEqual(set(restricted_groups), set(restrictions.get('groups', [])))

    def test_page_access_root_has_parent(self):
        access = get_access(self.root)
        self.assertFalse(access.has_parent)

    def test_page_access_child_has_parent(self):
        access = get_access(self.child)
        self.assertTrue(access.has_parent)

    def test_page_access_simple_access_types(self):
        types = ('common', 'anonymous', 'owner')
        for type_ in types:
            self._set_access(self.root, type_)
            self._set_access(self.child, type_)
            self.assert_access_type(self.root, type_, type_)
            self.assert_access_type(self.child, type_, type_)

    def test_restricted_access_type(self):
        self._set_access(self.root, 'restricted', self.restrictions)
        self._set_access(self.child, 'restricted', self.restrictions)

        self.assert_access_type(self.root, 'restricted', 'restricted')
        self.assert_access_type(self.child, 'restricted', 'restricted')
        self.assert_access_restrictions(self.root, self.restrictions)
        self.assert_access_restrictions(self.child, self.restrictions)

    def test_inherited_access_type_root(self):
        # this will do nothing
        self._set_access(self.root, 'inherited')
        self.assert_access_type(self.root, 'common', 'common')

    def test_inherited_access_type_child(self):
        self._set_access(self.child, 'inherited')
        for type_ in ('common', 'anonymous', 'owner'):
            self._set_access(self.root, type_)
            self.assert_access_type(self.child, 'inherited', type_)

        self._set_access(self.root, 'restricted', self.restrictions)
        self.assert_access_type(self.child, 'inherited', 'restricted')
        self.assert_access_restrictions(self.child, self.restrictions)
