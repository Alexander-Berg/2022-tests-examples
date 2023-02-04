import pytest

from walle.clients import staff
from walle.errors import UserNotInOwningABCService

USER = "user_login"
BOT_PROJECT_ID = 100009


class TestAuthenticateUserByBotProjectId:
    def mock_user_groups(self, mp, groups):
        mp.function(staff.get_user_groups, return_value=set(groups))

    @pytest.fixture
    def mock_service(self, mp, mock_get_planner_id_by_bot_project_id, mock_abc_get_service_slug):
        mock_get_planner_id_by_bot_project_id()
        mock_abc_get_service_slug()

    def test_user_belongs_to_service(self, walle_test, mp, mock_service):
        from walle.views.api.project_api.common import authenticate_user_by_bot_project_id

        self.mock_user_groups(mp, ["@svc_some_service", "@irrelevant_group"])
        authenticate_user_by_bot_project_id(USER, BOT_PROJECT_ID)

    def test_raises_if_user_doesnt_belong_to_service(self, walle_test, mp, mock_service):
        from walle.views.api.project_api.common import authenticate_user_by_bot_project_id

        self.mock_user_groups(mp, ["@svc_other_service"])
        with pytest.raises(UserNotInOwningABCService):
            authenticate_user_by_bot_project_id(USER, BOT_PROJECT_ID)
