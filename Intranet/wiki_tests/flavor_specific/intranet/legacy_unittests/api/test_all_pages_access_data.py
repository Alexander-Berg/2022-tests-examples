from mock import Mock, PropertyMock
from pretend import stub

from wiki.pages.access.external import _interpret_access_for_all_pages
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class SetCacheForAllPagesTest(BaseApiTestCase):
    def test_interpret_access_for_anonymous(self):
        access = {
            'is_owner': False,
            'is_restricted': False,
            'is_anonymous': True,
        }

        self.assertRaises(ValueError, lambda: _interpret_access_for_all_pages(stub(), access))

    def test_interpret_access_for_owner(self):
        access = {
            'is_owner': True,
        }

        page_stub = stub(get_authors=lambda: [])

        self.assertEqual(_interpret_access_for_all_pages(page_stub, access), (page_stub, [], []))

        owner_profile = stub()
        page_stub = stub(get_authors=lambda: [stub(staff=owner_profile)])

        self.assertEqual(_interpret_access_for_all_pages(page_stub, access), (page_stub, [owner_profile], []))

        from wiki.intranet.models import Staff

        staff_stub = Mock(staff=Mock())
        type(staff_stub).staff = PropertyMock(side_effect=Staff.DoesNotExist)
        page_stub = stub(get_authors=lambda: [staff_stub])

        self.assertEqual(_interpret_access_for_all_pages(page_stub, access), (page_stub, [], []))

    def test_interpret_restricted(self):
        users = [stub(), stub()]
        groups = [stub()]
        page_stub = stub()

        access = {
            'is_owner': False,
            'is_restricted': True,
            'groups': groups,
            'users': users,
        }

        self.assertEqual(_interpret_access_for_all_pages(page_stub, access), (page_stub, users, groups))

    def test_interpret_common(self):
        page_stub = stub()

        access = {
            'is_owner': False,
            'is_restricted': False,
            'is_anonymous': False,
            'is_common': True,
        }

        self.assertEqual(_interpret_access_for_all_pages(page_stub, access), (page_stub, None, None))

    def test_interpret_common_wiki(self):
        page_stub = stub()

        access = {
            'is_owner': False,
            'is_restricted': False,
            'is_anonymous': False,
            'is_common': False,
            'is_common_wiki': True,
        }

        self.assertEqual(_interpret_access_for_all_pages(page_stub, access), (page_stub, None, None))

    def setUp(self):
        pass

    def tearDown(self):
        pass
