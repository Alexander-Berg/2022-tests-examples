import pytest
import rstr
import random

import lib.server as server
from lib.server import IDM_ROLE, IDM_GROUP_SLUG


def check_idm_succeeded(response):
    data = response >> 200
    assert data["code"] == 0
    assert data["status"] == "ok"


def test_get_roles_info():
    data = server.get_idm_info() >> 200
    assert data["code"] == 0
    assert data["roles"]["slug"] == IDM_GROUP_SLUG
    assert data["roles"]["values"].get(IDM_ROLE) is not None


def test_add_and_remove_flow():
    logins = [rstr.letters(5, 20) for i in range(10)]

    def check_users_list():
        data = server.get_all_idm_roles() >> 200
        assert data["code"] == 0
        assert len(data["users"]) == len(logins)
        actual_users = sorted(data["users"], key=lambda x: x["login"])

        for i, user in enumerate(actual_users):
            assert user["login"] == logins[i]
            assert len(user["roles"]) == 1
            assert user["roles"][0].get(IDM_GROUP_SLUG) == IDM_ROLE

    for login in logins:
        check_idm_succeeded(server.add_idm_role(login))

    logins = sorted(logins)
    check_users_list()

    while logins:
        i = random.randint(0, len(logins) - 1)
        check_idm_succeeded(server.delete_idm_role(logins[i]))
        del logins[i]
        check_users_list()


def test_add_twice():
    login = rstr.letters(5, 20)
    check_idm_succeeded(server.add_idm_role(login))
    data = server.add_idm_role(login) >> 200
    assert data["code"] == 3
    assert data.get("warning") is not None


def test_delete_unknown():
    data = server.delete_idm_role(rstr.letters(5, 20)) >> 200
    assert data["code"] == 2
    assert data.get("error") is not None
