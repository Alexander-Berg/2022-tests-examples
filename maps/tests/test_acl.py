import datetime as dt
import flask
import mongomock
import pytest
import pytz

from maps.garden.libs.auth.auth_server import UserInfo, GardenClients, AuthMethod, GARDEN_CLIENT_TO_USER
from maps.garden.libs_server.application.authorization import check_acl, require_username, check_auth
from maps.garden.libs_server.common.contour_manager import Contour
from maps.garden.libs_server.common.errors import AuthException, ForbiddenException
from maps.garden.libs_server.acl_manager.acl_manager import AclManager, UserRole, ADMIN_ROLE, MODULE_USER


MODULE_USERNAME = "someuser"
MODULE_NAME = "somemodule"

ADMIN_USER = "adminuser"

USER_CONTOUR = Contour(
    _id="user_contour",
    owner=MODULE_USERNAME,
    creation_time=dt.datetime.now(pytz.utc),
    is_system=False,
)

SYSTEM_CONTOUR = Contour(
    _id="stable",
    owner=None,
    creation_time=dt.datetime.now(pytz.utc),
    is_system=True,
)


def _require_username(user: UserInfo = None):
    @require_username
    def test_func(*, auth: UserInfo):
        return auth

    app = flask.Flask(__name__)
    app.testing = True

    with app.app_context():
        flask.g.user = user
        return test_func()


def _check_auth(user: UserInfo = None):
    @check_auth
    def test_func(*, auth: UserInfo):
        return auth

    app = flask.Flask(__name__)
    app.testing = True

    with app.app_context():
        flask.g.user = user
        return test_func()


@pytest.fixture(scope="function")
def setup_test(mocker):
    acl_manager = AclManager(mongomock.MongoClient(tz_aware=True).db)
    admin_role = UserRole(
        username=ADMIN_USER,
        role=ADMIN_ROLE
    )
    user_role = UserRole(
        username=MODULE_USERNAME,
        role=MODULE_USER,
        module_name=MODULE_NAME
    )
    acl_manager.add_role(admin_role)
    acl_manager.add_role(user_role)

    mocker.patch("maps.garden.libs_server.application.state.acl_manager", return_value=acl_manager)


def _get_user(username=MODULE_USER, servicename=GardenClients.GARDEN_UI_STABLE):
    return UserInfo(username=username, servicename=servicename, method=AuthMethod.TVM)


def test_acl(setup_test):
    user = _get_user(ADMIN_USER)
    check_acl(MODULE_NAME, SYSTEM_CONTOUR, user)
    check_acl(MODULE_NAME, USER_CONTOUR, user)

    user = _get_user(MODULE_USERNAME)
    check_acl(MODULE_NAME, SYSTEM_CONTOUR, user)
    check_acl(MODULE_NAME, USER_CONTOUR, user)


def test_no_role(setup_test):
    user = _get_user(MODULE_USERNAME)

    with pytest.raises(ForbiddenException):
        check_acl("other_module", SYSTEM_CONTOUR, user)


def test_other_contour(setup_test):
    user = _get_user("other_user")

    with pytest.raises(ForbiddenException):
        check_acl(MODULE_NAME, USER_CONTOUR, user)


def test_custom_service(setup_test):
    user = _get_user(MODULE_USERNAME, GardenClients.STYLEREPO_STABLE)
    check_acl(MODULE_NAME, SYSTEM_CONTOUR, user)


def test_custom_service_not_permitted(setup_test):
    user =_get_user(MODULE_USERNAME, GardenClients.STYLEREPO_TESTING)

    with pytest.raises(ForbiddenException):
        check_acl(MODULE_NAME, SYSTEM_CONTOUR, user)


def test_check_auth():
    user = _get_user()
    assert user == _check_auth(user)


def test_check_auth_wo_user():
    user = None

    with pytest.raises(AuthException):
        _check_auth(user)


def test_require_username():
    user = _get_user()
    assert user == _require_username(user)


def test_require_username_wo_user():
    user = None

    with pytest.raises(AuthException):
        _require_username(user)


def test_require_username_wo_username():
    user = _get_user(username=None)

    with pytest.raises(ForbiddenException):
        _require_username(user)


def test_require_username_garden_client():
    user = _get_user(username=None, servicename=GardenClients.GARDEN_CLIENT_STABLE)
    assert _require_username(user).username == GARDEN_CLIENT_TO_USER[GardenClients.GARDEN_CLIENT_STABLE]
