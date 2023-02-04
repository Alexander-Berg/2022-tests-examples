
from mock import Mock

from wiki.api_core.errors.permissions import UserHasNoAccess
from wiki.api_core.utils import UIDListPermission
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class UIDListPermissionTest(BaseApiTestCase):
    def setUp(self):
        super(UIDListPermissionTest, self).setUp()
        profile_mock = Mock(uid=13)
        user_mock = Mock(staff=profile_mock)
        self.request_mock = Mock(user=user_mock)
        self.view_mock = Mock()

    def _test(self, _uid_list):
        class Permission(UIDListPermission):
            uid_list = _uid_list

        permission = Permission()

        return permission.has_permission(self.request_mock, self.view_mock)

    def test_none(self):
        self.assertTrue(self._test(None))

    def test_has_access(self):
        self.assertTrue(self._test((5, 13)))

    def test_has_no_aceess(self):
        self.assertRaises(UserHasNoAccess, lambda: self._test((4, 10)))
