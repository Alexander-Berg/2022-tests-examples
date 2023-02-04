"""Tests for functions that wrap and utilize the StaffClient."""

import json
from unittest import mock

import pytest

from infra.walle.server.tests.lib.util import TestCase
from sepelib.yandex.staff import StaffClient
from walle.clients.staff import (
    get_user_groups,
    _get_staff_client,
    check_owners,
    check_logins,
    check_login,
    InvalidGroupError,
    InvalidLoginError,
    resolve_owners,
    batch_get_groups_members,
    groups_to_ids,
    id_to_group,
)

walle_groups = """{"groups": [
    {"group": {"url": "virtual_robots"}},
    {"group": {"url": "robots"}},
    {"group": {"url": "affiliation-external"}},
    {"group": {"url": "wiki_external"}}],
    "department_group": {"url": "virtual_robots", "ancestors": [{"url": "virtual"}]}}"""

walle_eve_users = """{"links": {}, "page": 1, "limit": 10, "result":
    [{"login": "robot-walle"}, {"login": "robot-eve"}], "total": 2, "pages": 1}"""
robot_crew_groups = """{"links": {}, "page": 1, "limit": 10, "result": [
    {"url": "robots", "id": 1}, {"url": "crew", "id": 2}], "total": 2, "pages": 1}"""


class SetVsStringMatcher:
    def __init__(self, *logins):
        self.logins = set(logins)

    def __eq__(self, other):
        other = set(other.split(","))
        return self.logins == other


@pytest.fixture(autouse=True)
def test(request):
    return TestCase.create(request)


def patch_staff_client(mp, *stubs):
    mock_staff_client = mock.Mock(StaffClient)

    for method_name, return_value in stubs:
        method = getattr(mock_staff_client, method_name)
        method.return_value = return_value

    mp.function(_get_staff_client, return_value=mock_staff_client)
    return mock_staff_client


def test_get_user_groups(mp):
    client = patch_staff_client(mp, ("list_persons", json.loads(walle_groups)))

    expected_groups = [
        "@virtual_robots",
        "@robots",
        "@affiliation-external",
        "@wiki_external",
        "@virtual_robots",
        "@virtual",
    ]
    groups = get_user_groups("robot-walle")

    assert groups == expected_groups
    client.list_persons.assert_called_once_with(
        {"login": "robot-walle"},
        ("groups.group.url", "department_group.ancestors.url", "department_group.url"),
        one=True,
    )


def test_check_login(mp):
    mock_check_logins = mp.function(check_logins, side_effect=lambda logins, allow_dismissed: logins)

    assert check_login("test") == "test"
    mock_check_logins.assert_called_once_with(["test"], False)
    mock_check_logins.reset_mock()

    assert check_login("test", False) == "test"
    mock_check_logins.assert_called_once_with(["test"], False)
    mock_check_logins.reset_mock()

    assert check_login("test", True) == "test"
    mock_check_logins.assert_called_once_with(["test"], True)
    mock_check_logins.reset_mock()


def test_check_owners(mp):
    client = patch_staff_client(
        mp, ("list_persons", json.loads(walle_eve_users)), ("list_groups", json.loads(robot_crew_groups))
    )

    given_owners = ["robot-walle", "robot-eve", "@robots", "@crew"]
    owners = check_owners(given_owners)

    assert sorted(owners) == sorted(given_owners)
    client.list_persons.assert_called_once_with(
        {
            "login": SetVsStringMatcher("robot-walle", "robot-eve"),
            "_query": 'official.affiliation!="external" or official.is_robot==true',
            "official.is_dismissed": "false",
            "is_deleted": "false",
            "_limit": 2,
        },
        fields=["login"],
    )

    client.list_groups.assert_called_once_with(
        {"url": SetVsStringMatcher("robots", "crew"), "is_deleted": "false", "_limit": 2}, fields=("url",)
    )


def test_check_owners__invalid_user(mp):
    client = patch_staff_client(
        mp, ("list_persons", json.loads(walle_eve_users)), ("list_groups", json.loads(robot_crew_groups))
    )

    given_owners = ["robot-walle", "robot-eve", "robot-auto", "@robots", "@crew"]
    with pytest.raises(InvalidLoginError) as invalid_login:
        check_owners(given_owners)

    assert "robot-auto" in str(invalid_login)

    client.list_persons.assert_called_once_with(
        {
            "login": SetVsStringMatcher("robot-walle", "robot-eve", "robot-auto"),
            "_query": 'official.affiliation!="external" or official.is_robot==true',
            "official.is_dismissed": "false",
            "is_deleted": "false",
            "_limit": 3,
        },
        fields=["login"],
    )


def test_check_owners__invalid_groups(mp):
    client = patch_staff_client(
        mp, ("list_persons", json.loads(walle_eve_users)), ("list_groups", json.loads(robot_crew_groups))
    )

    given_owners = ["robot-walle", "robot-eve", "@robots", "@crew", "@earth"]
    with pytest.raises(InvalidGroupError) as invalid_group:
        check_owners(given_owners)

    assert "@earth" in str(invalid_group)

    client.list_groups.assert_called_once_with(
        {"url": SetVsStringMatcher("robots", "crew", "earth"), "is_deleted": "false", "_limit": 3}, fields=("url",)
    )


def test_resolve_owners_ignores_missing_logins(mp):
    existing_groups = ["ussr"]
    ussr_group_members = ["lenin", "stalin"]
    existing_project_owners = ["lenin", "trotsky"]

    mock_staff_client = mock.Mock(StaffClient)
    mock_staff_client.list_groups.return_value = {"result": [{"url": slug} for slug in existing_groups]}
    mock_staff_client.list_persons.side_effect = [
        {"result": [{"login": login} for login in existing_project_owners], "pages": 1},  # call from check_logins
        {"result": [{"login": login} for login in ussr_group_members], "pages": 1},  # call from get_group_members
    ]

    mp.function(_get_staff_client, return_value=mock_staff_client)

    owners = ["@ussr", "@usa", "@de", "lenin", "trotsky", "obama", "merkel"]
    expected_resolved_owners = sorted(set(existing_project_owners + ussr_group_members))

    assert resolve_owners(tuple(owners)) == expected_resolved_owners


def test_batch_get_groups_members(mp, disable_caches, load_test_json):
    # don't forget to wash yourself after this test (testing batch tree requests is a dirty way of earning salary)
    client = patch_staff_client(mp, ("list_persons", load_test_json("mocks/staff_groups_batch.json")))

    groups_to_resolve = tuple(
        sorted(
            [
                "relevant_dg1",
                "relevant_dg2",
                "relevant_dga1",
                "relevant_dga2",
                "relevant_g1",
                "relevant_g2",
                "empty_group",
            ]
        )
    )

    group_to_member = batch_get_groups_members(tuple("@{}".format(g) for g in groups_to_resolve))

    joined_groups = ",".join('"{}"'.format(group) for group in groups_to_resolve)
    expected_query = (
        '(groups.group.url in [{joined_groups}] or '
        'department_group.ancestors.url in [{joined_groups}] or '
        'department_group.url in [{joined_groups}]) and '
        '(official.is_robot==true or official.affiliation!="external")'.format(joined_groups=joined_groups)
    )
    expected_filters = {'official.is_dismissed': False}
    client.list_persons.assert_called_once_with(
        fields=('login', 'groups.group.url', 'department_group.ancestors.url', 'department_group.url'),
        spec=dict(_page=1, _query=expected_query, **expected_filters),
    )

    expected_group_to_member = {
        "@relevant_dg1": ["user1"],
        "@relevant_dg2": ["user2"],
        "@relevant_dga1": ["user1"],
        "@relevant_dga2": ["user2"],
        "@relevant_g1": ["user1", "user2"],
        "@relevant_g2": ["user2"],
        "@empty_group": [],
    }
    assert group_to_member == expected_group_to_member


def test_groups_to_ids(mp):
    client = patch_staff_client(mp, ("list_groups", json.loads(robot_crew_groups)))
    group_ids = ["@robots", "@crew", "@non_existen"]
    groups = groups_to_ids(group_ids)

    assert groups == {"@robots": 1, "@crew": 2}
    assert client.list_groups.mock_calls == [
        mock.call(
            {"url": ",".join([group[1:] for group in set(group_ids)]), "_limit": 3, "is_deleted": "false"},
            fields=('url', 'id'),
        ),
        mock.call({"url": "non_existen", "_limit": 1, "is_deleted": "true"}, fields=('url', 'id')),
    ]


@pytest.mark.parametrize("is_deleted", (True, False))
def test_id_to_group(mp, is_deleted):
    client = patch_staff_client(
        mp,
        (
            "list_groups",
            json.loads(
                """{"links": {}, "page": 1, "limit": 10, "result": [
        {"url": "robots", "id": 1}], "total": 1, "pages": 1}"""
            ),
        ),
    )
    group = id_to_group(1, is_deleted=is_deleted)

    assert group == "@robots"
    client.list_groups.assert_called_once_with(
        {"id": 1, "is_deleted": "true" if is_deleted else "false"}, fields=("url", "id")
    )
