import http.client
import json

from maps.garden.libs.auth.auth_server import UserInfo, GardenClients, AuthMethod
from maps.garden.sdk.module_traits.module_traits import ModuleTraits, ModuleType

from maps.garden.libs_server.acl_manager.acl_manager import AclManager, ADMIN_ROLE, MODULE_USER, UserRole

from maps.garden.server.lib.formats import idm_objects as io


def test_info(garden_client, module_helper):
    module_helper.add_module_to_system_contour(
        ModuleTraits(
            name="ymapsdf",
            type=ModuleType.MAP,
        )
    )

    module_helper.add_module_to_user_contour(
        ModuleTraits(
            name="test_module",
            type=ModuleType.MAP,
        ),
        user_contour="user_contour"
    )

    # New module was built in Sandbox and registered in Garden
    # but not released yet
    module_helper.register_module(
        ModuleTraits(
            name="new_module",
            type=ModuleType.MAP,
        )
    )

    return garden_client.get("idm/info/").get_json()


def test_all_roles(garden_client, db):
    db.acl.drop()
    acl_manager = AclManager(db)

    admin_role = UserRole(
        username="admin_user",
        role=ADMIN_ROLE,
    )

    some_user_role = UserRole(
        username="module_user",
        role=MODULE_USER,
        module_name="some_module",
    )

    another_user_role = UserRole(
        username="module_user",
        role=MODULE_USER,
        module_name="another_module",
    )

    acl_manager.add_role(admin_role)
    acl_manager.add_role(some_user_role)
    acl_manager.add_role(another_user_role)

    return garden_client.get("idm/get-all-roles/").get_json()


def test_role(garden_client, db, module_helper, mocker):
    db.acl.drop()
    acl_manager = AclManager(db)

    traits = ModuleTraits(
        name="some_module",
        type=ModuleType.MAP,
    )
    module_helper.add_module_to_system_contour(traits)

    mocker.patch(
        "maps.garden.libs_server.application.flask_utils.user_from_request",
        return_value=UserInfo(username=None, servicename=GardenClients.IDM, method=AuthMethod.TVM)
    )

    # add module user role twice
    for i in [0, 1]:
        response = garden_client.post("idm/add-role/", data={
            "login": "user",
            "path": "/role/module/some_module/module_role/module_user",
            "role": json.dumps({
                "role": "module",
                "module": "some_module",
                "module_role": MODULE_USER,
            }),
        }, content_type="application/x-www-form-urlencoded")

        assert response.status_code == http.client.OK
        assert response.get_json() == io.IdmOk().dict()
        assert acl_manager.get_all_roles()
        assert acl_manager.get_all_roles()[0] == UserRole(
            username="user",
            role=MODULE_USER,
            module_name="some_module"
        )

    # add admin role twice
    for i in [0, 1]:
        response = garden_client.post("idm/add-role/", data={
            "login": "admin",
            "path": "/role/garden/admin",
            "role": json.dumps({
                "role": "garden",
                "garden": ADMIN_ROLE,
            }),
        }, content_type="application/x-www-form-urlencoded")

        assert response.status_code == http.client.OK
        assert response.get_json() == io.IdmOk().dict()
        assert len(acl_manager.get_all_roles()) == 2

    # add role for unexisting module
    unexisting_role_data = {
        "login": "user",
        "path": "/role/module/unexisting_module/module_role/module_user",
        "role": json.dumps({
            "role": "module",
            "module": "unexisting_module",
            "module_role": MODULE_USER,
        }),
    }
    response = garden_client.post("idm/add-role/", data=unexisting_role_data, content_type="application/x-www-form-urlencoded")

    assert response.status_code == http.client.OK
    assert "warning" in response.get_json()
    assert len(acl_manager.get_all_roles()) == 2

    # remove unexisting admin role
    response = garden_client.post("idm/remove-role/", data={
        "login": "fake_admin",
        "path": "/role/garden/admin",
        "role": json.dumps({
            "role": "garden",
            "garden": ADMIN_ROLE,
        }),
    }, content_type="application/x-www-form-urlencoded")

    assert response.status_code == http.client.OK
    assert response.get_json() == io.IdmWarning(code=0, warning="User fake_admin has no role admin:None").dict()
    assert len(acl_manager.get_all_roles()) == 2

    # remove unexisting module_user role
    response = garden_client.post("idm/remove-role/", data={
        "login": "user",
        "path": "/role/module/fake_module/module_role/module_user",
        "role": json.dumps({
            "role": "module",
            "module": "fake_module",
            "module_role": MODULE_USER,
        }),
    }, content_type="application/x-www-form-urlencoded")

    assert response.status_code == http.client.OK
    assert response.get_json() == io.IdmWarning(code=0, warning="User user has no role module_user:fake_module").dict()
    assert len(acl_manager.get_all_roles()) == 2

    # remove existing admin role
    response = garden_client.post("idm/remove-role/", data={
        "login": "admin",
        "path": "/role/garden/admin",
        "role": json.dumps({
            "role": "garden",
            "garden": ADMIN_ROLE,
        }),
    }, content_type="application/x-www-form-urlencoded")

    assert response.status_code == http.client.OK
    assert response.get_json() == io.IdmOk().dict()
    assert len(acl_manager.get_all_roles()) == 1

    # remove existing module_role
    response = garden_client.post("idm/remove-role/", data={
        "login": "user",
        "path": "/role/module/some_module/module_role/module_user",
        "role": json.dumps({
            "role": "module",
            "module": "some_module",
            "module_role": MODULE_USER,
        }),
    }, content_type="application/x-www-form-urlencoded")

    assert response.status_code == http.client.OK
    assert response.get_json() == io.IdmOk().dict()
    assert not acl_manager.get_all_roles()


def test_invalid_request(garden_client, db, mocker):
    db.acl.drop()
    acl_manager = AclManager(db)

    mocker.patch(
        "maps.garden.libs_server.application.flask_utils.user_from_request",
        return_value=UserInfo(username=None, servicename=GardenClients.IDM, method=AuthMethod.TVM)
    )

    # invalid add role request
    response = garden_client.post("idm/add-role/", data={
        "login": "admin",
        "path": "/role/garden/admin",
        "role": "invalid_role",
    }, content_type="application/x-www-form-urlencoded")

    assert response.status_code == http.client.OK
    assert response.get_json()["code"] == 1
    assert response.get_json()["error"]
    assert not acl_manager.get_all_roles()

    admin_role = UserRole(
        username="admin_user",
        role=ADMIN_ROLE,
    )

    some_user_role = UserRole(
        username="module_user",
        role=MODULE_USER,
        module_name="some_module",
    )

    another_user_role = UserRole(
        username="module_user",
        role=MODULE_USER,
        module_name="another_module",
    )

    acl_manager.add_role(admin_role)
    acl_manager.add_role(some_user_role)
    acl_manager.add_role(another_user_role)

    # invalid remove role request
    response = garden_client.post("idm/remove-role/", data={
        "login": "admin",
        "path": "/role/garden/admin",
        "role": json.dumps({"invalid_role": "role_values"}),
    }, content_type="application/x-www-form-urlencoded")

    assert response.status_code == http.client.OK
    assert response.get_json()["code"] == 1
    assert response.get_json()["error"]
    assert len(acl_manager.get_all_roles()) == 3

    assert acl_manager.has_role(admin_role)
    assert acl_manager.has_role(some_user_role)
    assert acl_manager.has_role(another_user_role)


def test_forbidden(garden_client, mocker):
    mocker.patch(
        "maps.garden.libs_server.application.flask_utils.user_from_request",
        return_value=UserInfo(username=None, servicename=GardenClients.GARDEN_UI_STABLE, method=AuthMethod.TVM)
    )

    response = garden_client.get("idm/info/")
    assert response.status_code == http.client.OK

    response = garden_client.post("idm/add-role/")
    assert response.status_code == http.client.FORBIDDEN

    response = garden_client.post("idm/remove-role/")
    assert response.status_code == http.client.FORBIDDEN

    response = garden_client.get("idm/get-all-roles/")
    assert response.status_code == http.client.OK
