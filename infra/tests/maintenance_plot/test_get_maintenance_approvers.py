import pytest

from infra.walle.server.tests.lib.maintenance_plot_util import (
    TEST_DEFAULT_MAINTENANCE_APPROVERS,
    TEST_ROLE_CODE,
    generate_abc_service_members_response,
    paginate_abc_results,
)
from infra.walle.server.tests.lib.util import monkeypatch_request, mock_response
from walle.maintenance_plot import constants
from walle.maintenance_plot.model import (
    MaintenancePlot,
    MaintenancePlotMetaInfo,
    MaintenancePlotCommonSettings,
    MaintenanceApprovers,
    CommonScenarioSettings,
)


@pytest.fixture
def mock_abc_config(mp):
    mp.config("abc.host", "abc-back.y-t.ru")
    mp.config("abc.access_token", "AQAD-much-serious-token")


class TestGetMaintenanceApprovers:

    default_approver_login = "test-default-approver"
    role_code = "some-role"
    role_scope_slug = "some-role-scope-slug"

    @pytest.mark.usefixtures("mock_abc_config")
    @pytest.mark.parametrize(
        (
            "maintenance_approvers_logins, "
            "maintenance_approvers_abc_roles_codes, "
            "maintenance_approvers_abc_role_scope_slugs, "
            "expected_list_of_logins"
        ),
        [
            # Empty.
            (
                [],
                [],
                [],
                [default_approver_login],
            ),
            # Only fixed logins.
            (
                ["login-1", "login-2"],
                [],
                [],
                ["login-1", "login-2"],
            ),
            # Fixed logins and an ABC role code.
            (
                ["login-1", "login-2"],
                [role_code],
                [],
                ["login-1", "login-2", "mr-everywhere", "some-role-login-1", "some-role-login-2"],
            ),
            # Fixed logins and an ABC role scope slug.
            (
                ["login-1", "login-2"],
                [],
                [role_scope_slug],
                ["login-1", "login-2", "mr-everywhere", "some-role-scope-slug-login"],
            ),
            # Fixed logins and an ABC role scope slug.
            (
                [],
                [role_code],
                [role_scope_slug],
                ["mr-everywhere"],
            ),
        ],
    )
    def test_get_approvers(
        self,
        mp,
        monkeypatch,
        maintenance_approvers_logins,
        maintenance_approvers_abc_roles_codes,
        maintenance_approvers_abc_role_scope_slugs,
        expected_list_of_logins,
    ):
        # Mock ABC response.
        generated_response = generate_abc_service_members_response(
            maintenance_approvers_abc_roles_codes, maintenance_approvers_abc_role_scope_slugs
        )
        monkeypatch_request(monkeypatch, mock_response(generated_response))

        # Mock default approver here, default approvers mechanics is tested in `test_default_approvers`.
        mp.method(
            MaintenancePlot._get_default_approvers_for_service_slug,
            obj=MaintenancePlot,
            return_value=[self.default_approver_login],
        )

        maintenance_plot = MaintenancePlot(
            id="mock-id",
            meta_info=MaintenancePlotMetaInfo(name="Test", abc_service_slug="some-abc-service-slug"),
            common_settings=MaintenancePlotCommonSettings(
                maintenance_approvers=MaintenanceApprovers(
                    logins=maintenance_approvers_logins,
                    abc_roles_codes=maintenance_approvers_abc_roles_codes,
                    abc_role_scope_slugs=maintenance_approvers_abc_role_scope_slugs,
                ),
                common_scenarios_settings=CommonScenarioSettings(),
            ),
        )
        actual_list_of_logins = maintenance_plot.get_approvers()

        assert sorted(expected_list_of_logins) == sorted(actual_list_of_logins)

    @pytest.mark.usefixtures("mock_abc_config")
    def test_default_approvers(self, mp, monkeypatch):
        mp.setattr(constants, "DEFAULT_MAINTENANCE_APPROVERS", TEST_DEFAULT_MAINTENANCE_APPROVERS)

        default_approvers_persons = [
            {
                "person": {"login": self.default_approver_login, "is_robot": False},
                "role": {"scope": {"slug": "whatever"}, "code": TEST_ROLE_CODE},
            }
        ]
        generated_response = paginate_abc_results([default_approvers_persons])
        monkeypatch_request(monkeypatch, mock_response(generated_response))

        maintenance_plot = MaintenancePlot(
            id="mock-id",
            meta_info=MaintenancePlotMetaInfo(name="Test", abc_service_slug="some-abc-service-slug"),
            common_settings=MaintenancePlotCommonSettings(
                maintenance_approvers=MaintenanceApprovers(
                    logins=[], abc_roles_codes=[], abc_role_scope_slugs=[], abc_duty_schedule_slugs=[]
                ),
                common_scenarios_settings=CommonScenarioSettings(),
            ),
        )
        assert [self.default_approver_login] == maintenance_plot.get_approvers()
