import pytest

from infra.walle.server.tests.lib.api_util import delete_default_project
from walle.idm import role_storage
from walle.idm.common import idm_list_path_to_string

PATH1 = ["scopes", "test", "branch", "variant"]
PATH2 = ["scopes", "prod", "branch", "another"]
USER = "user"
GROUP = "@group"


def _add_role_membership(path, member):
    role_storage.IDMRoleMembership(path=idm_list_path_to_string(path), path_components=path, member=member).save()


@pytest.mark.parametrize("path_to_str", [True, False])
def test_get_role_members(walle_test, path_to_str):
    _add_role_membership(PATH1, USER)
    _add_role_membership(PATH2, GROUP)

    path = idm_list_path_to_string(PATH1) if path_to_str else PATH1
    assert role_storage.get_role_members(path) == [USER]


@pytest.mark.parametrize("path_to_str", [True, False])
def test_is_role_member(walle_test, path_to_str):
    _add_role_membership(PATH1, USER)
    _add_role_membership(PATH2, GROUP)

    path = idm_list_path_to_string(PATH1) if path_to_str else PATH1
    assert role_storage.is_role_member(path, USER)
    assert not role_storage.is_role_member(path, GROUP)


@pytest.mark.parametrize("path_to_str", [True, False])
def test_add_role_member(walle_test, path_to_str):
    delete_default_project(walle_test)
    path = idm_list_path_to_string(PATH1) if path_to_str else PATH1
    role_storage.add_role_member(path, USER)

    assert role_storage.IDMRoleMembership.objects().count() == 1
    expected = role_storage.IDMRoleMembership(path=idm_list_path_to_string(PATH1), path_components=PATH1, member=USER)
    got = role_storage.IDMRoleMembership.objects().get()
    for field in ("path", "path_components", "member"):
        assert got[field] == expected[field]


@pytest.mark.parametrize("path_to_str", [True, False])
def test_remove_role_member(walle_test, path_to_str):
    delete_default_project(walle_test)
    _add_role_membership(PATH1, USER)
    path = idm_list_path_to_string(PATH1) if path_to_str else PATH1
    role_storage.remove_role_member(path, USER)
    assert role_storage.IDMRoleMembership.objects().count() == 0
