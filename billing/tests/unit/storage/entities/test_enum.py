import pytest

from hamcrest import assert_that, equal_to, greater_than, greater_than_or_equal_to, less_than, less_than_or_equal_to

from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import RoleType


class TestRoleType:
    def test_get_indices(self):
        assert_that(
            RoleType.OWNER.get_indices(),
            equal_to(
                {
                    RoleType.OWNER: 0,
                    RoleType.VIEWER: 1,
                }
            ),
        )

    def test_comparison(self):
        all_items = list(RoleType)

        for idx, left in enumerate(all_items[:-1], start=1):
            for right in all_items[idx:]:
                assert_that(left, greater_than(right))
                assert_that(left, greater_than_or_equal_to(right))
                assert_that(right, less_than(left))
                assert_that(right, less_than_or_equal_to(left))

    @pytest.mark.parametrize('role', list(RoleType))
    def test_equality(self, role):
        assert_that(role, equal_to(role))
        assert_that(role, greater_than_or_equal_to(role))
        assert_that(role, less_than_or_equal_to(role))
