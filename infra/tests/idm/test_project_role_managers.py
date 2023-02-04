import pytest

from walle.constants import ROBOT_WALLE_OWNER
from walle.errors import BadRequestError
from walle.idm import project_role_managers, role_storage

USER = "user1"
GROUP = "group2"
ROLE_PATH = ["scopes", "projects", "project", "mocked-default-project", "role", "some-role"]


class TestCommonStorageStrategy:
    @pytest.fixture()
    def strategy(self, walle_test):
        return project_role_managers.CommonStorage(walle_test.default_project, ROLE_PATH)

    def test_list_members(self, strategy, mp):
        role_storage.add_role_member(ROLE_PATH, USER)
        role_storage.add_role_member(ROLE_PATH, GROUP)
        mock = mp.function(role_storage.get_role_members, wrap_original=True)

        members = strategy.list_members()
        assert mock.called
        assert set(members) == {USER, GROUP}

    def test_add_member(self, strategy, mp):
        mock = mp.function(role_storage.add_role_member, wrap_original=True)
        strategy.add_member(GROUP)
        assert mock.called

    def test_remove_member(self, strategy, mp):
        role_storage.add_role_member(ROLE_PATH, USER)
        mock = mp.function(role_storage.remove_role_member, wrap_original=True)
        strategy.remove_member(USER)
        assert mock.called


class TestRealUserProcessorStrategy:
    @pytest.fixture()
    def strategy(self, walle_test):
        return project_role_managers.RealUserProcessor()

    def test_doesnt_allow_none_members(self, strategy):
        with pytest.raises(BadRequestError):
            strategy.process_member(None)

    @pytest.mark.parametrize("member", [USER, GROUP])
    def test_doesnt_change_members(self, strategy, member):
        assert strategy.process_member(member) == member

    def test_doesnt_change_requester(self, strategy):
        assert strategy.process_requester("some-user") == "some-user"


class TestFixedUserProcessorStrategy:
    fixed_member = "@svc_someservice"

    @pytest.fixture()
    def strategy(self, walle_test):
        strategy_cls = project_role_managers.gen_fixed_member_processor_strategy(self.fixed_member)
        return strategy_cls()

    @pytest.mark.parametrize("member", [USER, GROUP])
    def test_process_member_doesnt_allow_other_members(self, strategy, member):
        with pytest.raises(BadRequestError):
            strategy.process_member(member)

    @pytest.mark.parametrize("member", [None, fixed_member])
    def test_process_member_allows_only_fixed_member(self, strategy, member):
        assert strategy.process_member(member) == self.fixed_member

    def test_process_requester_sets_fixed_requester(self, strategy):
        assert strategy.process_requester(USER) == ROBOT_WALLE_OWNER


def test_owner_manager(walle_test):
    owner_manager = project_role_managers.ProjectRole.get_role_manager("owner", walle_test.default_project)
    assert owner_manager.role_name == "owner"
    assert owner_manager.storage_strategy is project_role_managers.CommonStorage
    assert owner_manager.member_processing_strategy is project_role_managers.RealUserProcessor
    assert owner_manager.audit_log_strategy is project_role_managers.OwnerRoleChange


def test_user_manager(walle_test):
    user_manager = project_role_managers.ProjectRole.get_role_manager("user", walle_test.default_project)
    assert user_manager.role_name == "user"
    assert user_manager.storage_strategy is project_role_managers.CommonStorage
    assert user_manager.member_processing_strategy is project_role_managers.RealUserProcessor
    assert user_manager.audit_log_strategy is project_role_managers.CommonProjectRoleChange


def test_superuser_manager(walle_test):
    user_manager = project_role_managers.ProjectRole.get_role_manager("superuser", walle_test.default_project)
    assert user_manager.role_name == "superuser"
    assert user_manager.storage_strategy is project_role_managers.CommonStorage
    assert user_manager.member_processing_strategy is project_role_managers.RealUserProcessor
    assert user_manager.audit_log_strategy is project_role_managers.CommonProjectRoleChange


def test_noc_access_manager(walle_test):
    noc_access_manager = project_role_managers.ProjectRole.get_role_manager("noc_access", walle_test.default_project)
    assert noc_access_manager.role_name == "noc_access"
    assert noc_access_manager.storage_strategy is project_role_managers.CommonStorage
    assert noc_access_manager.member_processing_strategy is project_role_managers.RealUserProcessor
    assert noc_access_manager.audit_log_strategy is project_role_managers.CommonProjectRoleChange


def test_ssh_rebooter_manager(walle_test):
    rebooter_manager = project_role_managers.ProjectRole.get_role_manager("ssh_rebooter", walle_test.default_project)
    assert rebooter_manager.role_name == "ssh_rebooter"
    assert rebooter_manager.storage_strategy is project_role_managers.CommonStorage

    assert issubclass(rebooter_manager.member_processing_strategy, project_role_managers.FixedMemberProcessor)
    assert rebooter_manager.member_processing_strategy._fixed_member == ROBOT_WALLE_OWNER

    assert rebooter_manager.audit_log_strategy is project_role_managers.SshRebooterRoleChange
