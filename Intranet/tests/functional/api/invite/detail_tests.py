# coding: utf-8

from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
    none,
)

from testutils import (
    TestCase,
    get_auth_headers,
    create_department
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    InviteModel,
)
from intranet.yandex_directory.src.yandex_directory.core.views.invite import (
    InviteDetailView,
)


class TestInviteDetailView(TestCase):
    def setUp(self):
        super(TestInviteDetailView, self).setUp()
        self.dep_id = 5
        create_department(
            self.main_connection,
            org_id=self.organization['id'],
            dep_id=self.dep_id,
        )
        self.headers = get_auth_headers(as_org=self.organization['id'])

    def test_get__ok(self):
        invite_code = InviteModel(self.meta_connection).create(
            department_id=None,
            org_id=self.organization['id'],
            author_id=1,
        )
        response = self.get_json('/invites/{}/'.format(invite_code), headers=self.headers)

        assert_that(
            response,
            has_entries(
                code=invite_code,
                org_name=self.organization['name'],
                last_use=none(),
            )
        )

    def test_get__but_no_org(self):
        self.get_json('/invites/{}/'.format(10), headers=self.headers, expected_code=404)

    def test_internal(self):
        # проверяем что ручка internals
        assert_that(
            InviteDetailView.get.__dict__.get('internal', False),
            equal_to(True)
        )
