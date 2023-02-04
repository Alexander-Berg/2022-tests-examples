# coding: utf-8

from hamcrest import (
    assert_that,
    has_entries,
    not_none,
    none,
)

from testutils import (
    TestCase,
    get_auth_headers,
    create_department,
    TestOrganizationWithoutDomainMixin,
)
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.common.models.types import (
    ROOT_DEPARTMENT_ID,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    InviteModel,
    ActionModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.invite import invite_type


class TestDepartmentInviteView__get(TestOrganizationWithoutDomainMixin, TestCase):
    def setUp(self):
        super(TestDepartmentInviteView__get, self).setUp()
        self.org_id = self.yandex_organization['id']
        self.dep_id = 5
        create_department(
            self.main_connection,
            org_id=self.org_id,
            dep_id=self.dep_id,
        )
        self.headers = get_auth_headers(as_uid=self.yandex_admin['id'])

        self.personal_invite = InviteModel(self.meta_connection).create(
            self.org_id,
            self.dep_id,
            self.yandex_admin['id'],
        )
        self.department_invite = InviteModel(self.meta_connection).create(
            self.org_id,
            ROOT_DEPARTMENT_ID,
            self.yandex_admin['id'],
            invite_type=invite_type.department,
        )

    def test_get_existing_invite(self):
        # Проверяем, что вернётся существующий код для департамента
        response = self.get_json(
            'departments/{}/invite/'.format(ROOT_DEPARTMENT_ID),
            headers=self.headers,
        )

        assert_that(
            response,
            has_entries(
                code=self.department_invite,
            )
        )

    def test_get_non_existing_invite(self):
        # Проверяем, что вернётся 404, кода нет для отдела нет (при этом есть персональный код)
        self.get_json(
            'departments/{}/invite/'.format(self.dep_id),
            headers=self.headers,
            expected_code=404,
        )

    def test_get_disabled_invite(self):
        # Проверяем, что вернётся 404, если код неактивен
        InviteModel(self.meta_connection).filter(code=self.department_invite).update(enabled=False)
        self.get_json(
            'departments/{}/invite/'.format(self.dep_id),
            headers=self.headers,
            expected_code=404,
        )

    def test_get_invite_for_non_existing_department(self):
        # Проверяем, что вернётся 404, если нет отдела
        non_existing_dep_id = 100
        self.get_json(
            'departments/{}/invite/'.format(non_existing_dep_id),
            headers=self.headers,
            expected_code=404,
        )


class TestDepartmentInviteView__post(TestOrganizationWithoutDomainMixin, TestCase):
    def setUp(self):
        super(TestDepartmentInviteView__post, self).setUp()
        self.org_id = self.yandex_organization['id']
        self.dep_id = 5
        create_department(
            self.main_connection,
            org_id=self.org_id,
            dep_id=self.dep_id,
        )
        self.headers = get_auth_headers(as_uid=self.yandex_admin['id'])

        self.department_invite = InviteModel(self.meta_connection).create(
            self.org_id,
            self.dep_id,
            self.yandex_admin['id'],
            invite_type=invite_type.department,
        )
        self.default_counter = app.config['DEFAULT_INVITE_LIMIT']

    def test_create_new_invite(self):
        # Проверяем, что создаётся новый код
        response = self.post_json(
            'departments/{}/invite/'.format(ROOT_DEPARTMENT_ID),
            headers=self.headers,
            data={},
        )
        assert_that(
            response,
            has_entries(
                code=not_none(),
            )
        )
        assert_that(
            InviteModel(self.meta_connection).get(response['code']),
            has_entries(
                org_id=self.org_id,
                department_id=ROOT_DEPARTMENT_ID,
                created_at=not_none(),
                last_use=none(),
                counter=self.default_counter,
                enabled=True,
                author_id=self.yandex_admin['id'],
                invite_type=invite_type.department,
            ),
        )
        assert_that(
            ActionModel(self.main_connection).filter(
                org_id=self.org_id,
                name='invite_add'
            ).one(),
            has_entries(
                object=has_entries(
                    code=response['code'],
                )
            ),
        )

    def test_create_invite_and_deactivate_existing(self):
        # Проверяем, что имеющийся код деактивируется, и создаётся новый
        response = self.post_json(
            'departments/{}/invite/'.format(self.dep_id),
            headers=self.headers,
            data={},
        )
        assert_that(
            response,
            has_entries(
                code=not_none(),
            )
        )
        assert_that(
            InviteModel(self.meta_connection).get(self.department_invite),
            has_entries(
                org_id=self.org_id,
                department_id=self.dep_id,
                created_at=not_none(),
                last_use=none(),
                counter=self.default_counter,
                enabled=False,
                author_id=self.yandex_admin['id'],
                invite_type=invite_type.department,
            ),
        )
        assert_that(
            InviteModel(self.meta_connection).get(response['code']),
            has_entries(
                org_id=self.org_id,
                department_id=self.dep_id,
                created_at=not_none(),
                last_use=none(),
                counter=self.default_counter,
                enabled=True,
                author_id=self.yandex_admin['id'],
                invite_type=invite_type.department,
            ),
        )
        assert_that(
            ActionModel(self.main_connection).filter(
                org_id=self.org_id,
                name='invite_deactivate'
            ).one(),
            has_entries(
                object=has_entries(
                    code=self.department_invite,
                )
            ),
        )
        assert_that(
            ActionModel(self.main_connection).filter(
                org_id=self.org_id,
                name='invite_add'
            ).one(),
            has_entries(
                object=has_entries(
                    code=response['code'],
                )
            ),
        )

    def test_department_not_exists(self):
        # Проверяем, что если отдела нет - вернется 404
        self.post_json(
            'departments/{}/invite/'.format(100),
            headers=self.headers,
            data={},
            expected_code=404,
        )


class TestDepartmentInviteView__delete(TestOrganizationWithoutDomainMixin, TestCase):
    def setUp(self):
        super(TestDepartmentInviteView__delete, self).setUp()
        self.org_id = self.yandex_organization['id']
        self.headers = get_auth_headers(as_uid=self.yandex_admin['id'])

        self.org_id = self.yandex_organization['id']
        self.department_invite = InviteModel(self.meta_connection).create(
            self.org_id,
            ROOT_DEPARTMENT_ID,
            self.yandex_admin['id'],
            invite_type=invite_type.department,
        )
        self.personal_invite = InviteModel(self.meta_connection).create(
            self.org_id,
            ROOT_DEPARTMENT_ID,
            self.yandex_admin['id'],
        )
        self.dep_id = 5
        create_department(
            self.main_connection,
            org_id=self.org_id,
            dep_id=self.dep_id,
        )
        self.personal_invite_1 = InviteModel(self.meta_connection).create(
            self.org_id,
            self.dep_id,
            self.yandex_admin['id'],
        )

    def test_delete_existing_invite(self):
        # Проверяем, что код для департамента удалится
        self.delete_json(
            'departments/{}/invite/'.format(ROOT_DEPARTMENT_ID),
            headers=self.headers,
        )
        assert_that(
            InviteModel(self.meta_connection).get(self.department_invite),
            has_entries(
                enabled=False,
            ),
        )
        assert_that(
            InviteModel(self.meta_connection).get(self.personal_invite),
            has_entries(
                enabled=True,
            ),
        )
        assert_that(
            ActionModel(self.main_connection).filter(
                org_id=self.org_id,
                name='invite_deactivate'
            ).one(),
            has_entries(
                object=has_entries(
                    code=self.department_invite,
                )
            ),
        )

    def test_non_existing_invite(self):
        # Проверяем, что если кода для департамента нет, возвращается 204 и ничего не происходит
        self.delete_json(
            'departments/{}/invite/'.format(self.dep_id),
            headers=self.headers,
        )
        assert_that(
            InviteModel(self.meta_connection).get(self.personal_invite_1),
            has_entries(
                enabled=True,
            ),
        )
        assert_that(
            ActionModel(self.main_connection).filter(
                org_id=self.org_id,
                name='invite_deactivate'
            ).one(),
            none(),
        )

    def test_non_existing_department(self):
        # Проверяем, что департамента нет, возвращаетс 204
        self.delete_json(
            'departments/{}/invite/'.format(100),
            headers=self.headers,
        )
