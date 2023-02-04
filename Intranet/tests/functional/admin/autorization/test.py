# -*- coding: utf-8 -*-

from testutils import (
    TestCase,
    get_oauth_headers,
    get_auth_headers,
)


class TestAdminAutorization(TestCase):
    enable_admin_api = True

    def setUp(self, *args, **kwargs):
        super(TestAdminAutorization, self).setUp(*args, **kwargs)

    def test_not_allowed_roles_with_oauth(self):
        self.get_json(
            '/admin/organizations/',
            headers=get_oauth_headers(),
            expected_code=401,
        )

    def test_not_allowed_roles_with_auth(self):
        self.get_json(
            '/admin/organizations/',
            headers=get_auth_headers(),
            expected_code=401,
        )
