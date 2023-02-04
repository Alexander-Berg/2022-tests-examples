import mongomock

from maps.garden.libs_server.acl_manager.acl_manager import AclManager, UserRole, ADMIN_ROLE, MODULE_USER


def test_acl_manager():
    database = mongomock.MongoClient(tz_aware=True).db
    acl_manager = AclManager(database)

    assert not acl_manager.get_all_roles()

    admin_role = UserRole(
        username="admin_user",
        role=ADMIN_ROLE,
    )

    assert admin_role.is_admin()

    acl_manager.add_role(admin_role)

    assert acl_manager.has_user_module_role("admin_user", "some_module")
    assert acl_manager.is_admin("admin_user")
    assert acl_manager.has_role(admin_role)

    assert len(acl_manager.get_all_roles()) == 1
    assert acl_manager.get_all_roles()[0] == admin_role

    acl_manager.remove_role(admin_role)

    assert not acl_manager.has_user_module_role("admin_user", "some_module")
    assert not acl_manager.is_admin("admin_user")
    assert not acl_manager.has_role(admin_role)

    assert not acl_manager.get_all_roles()

    user_role = UserRole(
        username="module_user",
        role=MODULE_USER,
        module_name="some_module",
    )

    acl_manager.add_role(user_role)

    assert acl_manager.has_user_module_role("module_user", "some_module")
    assert not acl_manager.is_admin("admin_user")
    assert acl_manager.has_role(user_role)

    assert len(acl_manager.get_all_roles()) == 1
    assert acl_manager.get_all_roles()[0] == user_role

    acl_manager.add_role(admin_role)
    assert len(acl_manager.get_all_roles()) == 2
    assert admin_role in acl_manager.get_all_roles()
    assert user_role in acl_manager.get_all_roles()


def test_remove_module_roles():
    database = mongomock.MongoClient(tz_aware=True).db
    acl_manager = AclManager(database)

    acl_manager.add_role(UserRole(username="user1", role=MODULE_USER, module_name="ymapsdf"))
    acl_manager.add_role(UserRole(username="user2", role=MODULE_USER, module_name="ymapsdf"))
    acl_manager.add_role(UserRole(username="user1", role=MODULE_USER, module_name="geocoder"))

    assert acl_manager.has_user_module_role("user1", "ymapsdf")
    assert acl_manager.has_user_module_role("user2", "ymapsdf")
    assert acl_manager.has_user_module_role("user1", "geocoder")

    acl_manager.remove_module_roles("ymapsdf")

    assert not acl_manager.has_user_module_role("user1", "ymapsdf")
    assert not acl_manager.has_user_module_role("user2", "ymapsdf")
    assert acl_manager.has_user_module_role("user1", "geocoder")
