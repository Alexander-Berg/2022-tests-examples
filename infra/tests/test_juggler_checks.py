"""Test cron job that creates juggler checks."""
from collections import defaultdict

import mock
import pytest
from juggler_sdk import Check, NotificationOptions, Child, FlapOptions

from sepelib.core import config
from walle import juggler
from walle.expert.decisionmakers import AbstractDecisionMaker, ModernDecisionMaker
from walle.expert.types import CheckType
from walle.hosts import HostState, HostStatus
from walle.juggler import Group
from walle.projects import Project
from walle.util import mongo


def mock_project_and_locations_data(mp, data):
    mp.function(juggler._fetch_project_locations_from_mongodb, return_value=data)


def mock_project_enabled_checks(mp, check_map, enabled_checks=None):
    # every project have two checks configured, three checks totally

    class VeryCustomDecisionMaker(AbstractDecisionMaker):
        # intentionally do not implement abstract methods: we shall not need to use any of them here.
        def __init__(self, project, checks):
            self.checks = checks
            super().__init__(project, enabled_checks or set())

    dm_map = {
        project_id: VeryCustomDecisionMaker(Project(id=project_id), frozenset(checks))
        for project_id, checks in check_map.items()
    }
    mp.function(juggler.get_decision_maker, module=juggler, side_effect=lambda p: dm_map[p.id])


@pytest.fixture()
def mock_fetch_projects(mp):
    mp.function(juggler._fetch_projects, side_effect=lambda ps: (Project(id=p) for p in ps))


def _setup_juggler_config(mp, **params):
    # set some safe defaults to keep tests smaller
    params.setdefault("source", "wall-e.unittest")
    params.setdefault("group_filters", {})

    for key, value in params.items():
        # make compatible with monkeypatch_config
        mp.config("juggler.{}".format(key), value)


def _get_test_shards():
    return [
        mongo.MongoPartitionerShard(str(id_), mock.MagicMock()) for id_ in range(config.get_value("juggler.shards_num"))
    ]


@pytest.mark.usefixtures("mock_fetch_projects")
def test_extra_group_filters(mp):
    _setup_juggler_config(mp, group_filters={"important_filter": True})
    mock_project_and_locations_data(mp, [Group(project="p1", queue="q1", rack="r1")])
    mock_project_enabled_checks(mp, {"p1": {CheckType.UNREACHABLE}})

    expected_aggregates = {
        "wall-e.unittest-q1-r1": {
            CheckType.UNREACHABLE: ["DEV@queue=q1&rack=r1&prj={}&important_filter=True".format("p1")],
        },
    }

    shards = _get_test_shards()
    marks = defaultdict(str)

    created_aggregates = defaultdict(lambda: defaultdict(list))
    for check in juggler._configured_project_checks(shards, marks):
        created_aggregates[check.host][check.service].extend(c.host for c in check.children)

    assert {k: dict(v) for k, v in created_aggregates.items()} == expected_aggregates


@pytest.mark.usefixtures("mock_fetch_projects")
def test_splitting_checks_by_projects_and_locations(mp):
    _setup_juggler_config(mp)

    # there are three projects in three locations every project have hosts in two of three locations.
    mock_project_and_locations_data(
        mp,
        [
            Group(project="p1", queue="q1", rack="r1"),
            Group(project="p1", queue="q2", rack="r2"),
            Group(project="p2", queue="q2", rack="r2"),
            Group(project="p2", queue="q3", rack="r3"),
            Group(project="p3", queue="q3", rack="r3"),
            Group(project="p3", queue="q1", rack="r1"),
        ],
    )

    # every project have two checks configured, three checks totally
    mock_project_enabled_checks(
        mp,
        {
            "p1": {CheckType.UNREACHABLE, CheckType.SSH},
            "p2": {CheckType.SSH, CheckType.FS_CHECK},
            "p3": {CheckType.FS_CHECK, CheckType.UNREACHABLE},
        },
    )

    single_project_group = "DEV@queue={}&rack={}&prj={}"
    double_project_group = "DEV@queue={}&rack={}&prj={}&prj={}"

    expected_aggregates = {
        "wall-e.unittest-q1-r1": {
            CheckType.UNREACHABLE: [double_project_group.format("q1", "r1", "p1", "p3")],
            CheckType.SSH: [single_project_group.format("q1", "r1", "p1")],
            CheckType.FS_CHECK: [single_project_group.format("q1", "r1", "p3")],
        },
        "wall-e.unittest-q2-r2": {
            CheckType.UNREACHABLE: [single_project_group.format("q2", "r2", "p1")],
            CheckType.SSH: [double_project_group.format("q2", "r2", "p1", "p2")],
            CheckType.FS_CHECK: [single_project_group.format("q2", "r2", "p2")],
        },
        "wall-e.unittest-q3-r3": {
            CheckType.UNREACHABLE: [single_project_group.format("q3", "r3", "p3")],
            CheckType.SSH: [single_project_group.format("q3", "r3", "p2")],
            CheckType.FS_CHECK: [double_project_group.format("q3", "r3", "p2", "p3")],
        },
    }

    shards = _get_test_shards()
    marks = defaultdict(str)

    created_aggregates = defaultdict(lambda: defaultdict(list))
    for check in juggler._configured_project_checks(shards, marks):
        created_aggregates[check.host][check.service].extend(c.host for c in check.children)

    assert {k: dict(v) for k, v in created_aggregates.items()} == expected_aggregates


@pytest.mark.usefixtures("mock_fetch_projects")
def test_adding_predefined_projects_to_filters(mp):
    _setup_juggler_config(mp, group_force_projects=["extra-p1", "extra-p2"])

    mock_project_and_locations_data(
        mp, [Group(project="p1", queue="q1", rack="r1"), Group(project="p2", queue="q2", rack="r2")]
    )

    checks = {CheckType.UNREACHABLE, CheckType.SSH}
    mock_project_enabled_checks(mp, {"p1": checks, "p2": checks})

    expected_aggregates = {
        "wall-e.unittest-q1-r1": {
            CheckType.UNREACHABLE: ["DEV@queue=q1&rack=r1&prj={}&prj={}&prj={}".format("extra-p1", "extra-p2", "p1")],
            CheckType.SSH: ["DEV@queue=q1&rack=r1&prj={}&prj={}&prj={}".format("extra-p1", "extra-p2", "p1")],
        },
        "wall-e.unittest-q2-r2": {
            CheckType.UNREACHABLE: ["DEV@queue=q2&rack=r2&prj={}&prj={}&prj={}".format("extra-p1", "extra-p2", "p2")],
            CheckType.SSH: ["DEV@queue=q2&rack=r2&prj={}&prj={}&prj={}".format("extra-p1", "extra-p2", "p2")],
        },
    }

    shards = _get_test_shards()
    marks = defaultdict(str)

    created_aggregates = defaultdict(lambda: defaultdict(list))
    for check in juggler._configured_project_checks(shards, marks):
        created_aggregates[check.host][check.service].extend(c.host for c in check.children)

    assert {k: dict(v) for k, v in created_aggregates.items()} == expected_aggregates


@pytest.mark.usefixtures("mock_fetch_projects")
def test_adding_predefined_checks(mp):
    _setup_juggler_config(
        mp,
        extra_checks={
            "default": ["extra-c1", "extra-c2"],
            "p1": ["extra-p1-c1", "extra-p1-c2"],
            "p2": ["extra-p2-c1", "extra-p2-c2"],
        },
    )

    mock_project_and_locations_data(
        mp, [Group(project="p1", queue="q1", rack="r1"), Group(project="p2", queue="q2", rack="r2")]
    )

    mp.function(
        juggler.get_decision_maker,
        module=juggler,
        side_effect=lambda p: ModernDecisionMaker(None, {CheckType.SSH}, None, p),
    )

    expected_aggregates = {
        "wall-e.unittest-q1-r1": {
            CheckType.SSH: ["DEV@queue=q1&rack=r1&prj={}".format("p1")],
            "extra-c1": ["DEV@queue=q1&rack=r1&prj={}".format("p1")],
            "extra-c2": ["DEV@queue=q1&rack=r1&prj={}".format("p1")],
            "extra-p1-c1": ["DEV@queue=q1&rack=r1&prj={}".format("p1")],
            "extra-p1-c2": ["DEV@queue=q1&rack=r1&prj={}".format("p1")],
        },
        "wall-e.unittest-q2-r2": {
            CheckType.SSH: ["DEV@queue=q2&rack=r2&prj={}".format("p2")],
            "extra-c1": ["DEV@queue=q2&rack=r2&prj={}".format("p2")],
            "extra-c2": ["DEV@queue=q2&rack=r2&prj={}".format("p2")],
            "extra-p2-c1": ["DEV@queue=q2&rack=r2&prj={}".format("p2")],
            "extra-p2-c2": ["DEV@queue=q2&rack=r2&prj={}".format("p2")],
        },
    }

    shards = _get_test_shards()
    marks = defaultdict(str)

    created_aggregates = defaultdict(lambda: defaultdict(list))
    for check in juggler._configured_project_checks(shards, marks):
        created_aggregates[check.host][check.service].extend(c.host for c in check.children)

    assert {k: dict(v) for k, v in created_aggregates.items()} == expected_aggregates


def test_prjs_with_only_free_and_invalid_hosts_are_excluded(walle_test):
    for inv, project, state, status in [
        # will be excluded, because there are no normal hosts in project
        (1, "p1", HostState.FREE, HostStatus.READY),
        (2, "p1", HostState.MAINTENANCE, HostStatus.INVALID),
        (3, "p1", HostState.ASSIGNED, HostStatus.INVALID),
        # will be included, because there are normal hosts among free and invalid ones
        (4, "p2", HostState.ASSIGNED, HostStatus.READY),
        (5, "p2", HostState.FREE, HostStatus.READY),
        (6, "p2", HostState.MAINTENANCE, HostStatus.INVALID),
        (7, "p2", HostState.ASSIGNED, HostStatus.INVALID),
    ]:
        walle_test.mock_host(
            {
                "inv": inv,
                "project": project,
                "state": state,
                "status": status,
                "location": {"short_queue_name": "q", "rack": "r"},
            }
        )

    assert list(item.project for item in juggler._fetch_project_locations_from_mongodb()) == ["p2"]


@pytest.mark.usefixtures("mock_fetch_projects")
def test_create_unknown_checks_too(mp):
    _setup_juggler_config(mp)

    # there are three projects in three locations every project have hosts in two of three locations.
    mock_project_and_locations_data(mp, [Group(project="p1", queue="q1", rack="r1")])

    custom_check = "a_very_custom_check"
    mock_project_enabled_checks(mp, {"p1": {CheckType.SSH, custom_check}})

    expected_aggregates = {
        "wall-e.unittest-q1-r1": {
            CheckType.SSH: ["DEV@queue=q1&rack=r1&prj={}".format("p1")],
            custom_check: ["DEV@queue=q1&rack=r1&prj={}".format("p1")],
        },
    }

    shards = _get_test_shards()
    marks = defaultdict(str)

    created_aggregates = defaultdict(lambda: defaultdict(list))
    for check in juggler._configured_project_checks(shards, marks):
        created_aggregates[check.host][check.service].extend(c.host for c in check.children)

    assert {k: dict(v) for k, v in created_aggregates.items()} == expected_aggregates


def test_ib_link_aggregator(mp, walle_test):
    _setup_juggler_config(mp)

    project_id = "p1"
    queue_id = "q1"
    rack_id = "r1"
    host = "some-host"
    ib_ports = ["ib_port_1", "ib_port_2"]

    walle_test.mock_project({"id": project_id, "tags": [config.get_value("infiniband.involvement_tag")]})
    walle_test.mock_host(
        {
            "name": host,
            "project": project_id,
            "location": {
                "short_queue_name": queue_id,
                "rack": rack_id,
            },
            "infiniband_info": {"ports": ib_ports},
        }
    )

    # there are three projects in three locations every project have hosts in two of three locations.
    mock_project_and_locations_data(mp, [Group(project_id, queue_id, rack_id)])

    custom_check = "a_very_custom_check"
    mock_project_enabled_checks(mp, {project_id: set(CheckType.ALL) | {custom_check}})

    shards = _get_test_shards()
    marks = defaultdict(str)

    created_aggregates = defaultdict(lambda: defaultdict(list))
    for check in juggler._configured_project_checks(shards, marks):
        created_aggregates[check.host][check.service].append(check)

    aggregator_name = f"wall-e.unittest-{queue_id}-{rack_id}"
    checks = created_aggregates[aggregator_name][CheckType.IB_LINK]
    assert len(checks) == 1
    check = checks[0]

    assert check.host == aggregator_name
    assert check.service == CheckType.IB_LINK
    assert {c.instance for c in check.children} == set(ib_ports)
    assert {c.host for c in check.children} == {host}
    assert {c.service for c in check.children} == {CheckType.IB_LINK}


@pytest.mark.usefixtures("mock_fetch_projects")
def test_does_not_create_checks_for_non_juggler_checks(mp, database):
    _setup_juggler_config(mp)

    project_id = "p1"
    queue_id = "q1"
    rack_id = "r1"

    # there are three projects in three locations every project have hosts in two of three locations.
    mock_project_and_locations_data(mp, [Group(project_id, queue_id, rack_id)])

    custom_check = "a_very_custom_check"
    mock_project_enabled_checks(mp, {project_id: set(CheckType.ALL) | {custom_check}})

    # NOTE(rocco66): 'ib_link' has specific aggregator type
    expected_checks = (set(CheckType.ALL_JUGGLER) | {custom_check}) - {CheckType.IB_LINK}
    expected_aggregates = {
        f"wall-e.unittest-{queue_id}-{rack_id}": {
            check: [f"DEV@queue={queue_id}&rack={rack_id}&prj={project_id}"] for check in expected_checks
        }
    }

    shards = _get_test_shards()
    marks = defaultdict(str)

    created_aggregates = defaultdict(lambda: defaultdict(list))
    for check in juggler._configured_project_checks(shards, marks):
        created_aggregates[check.host][check.service].extend(c.host for c in check.children)

    assert {k: dict(v) for k, v in created_aggregates.items()} == expected_aggregates


@pytest.mark.parametrize(
    "rack, is_valid",
    [
        ("07.ComCenterBN 2-7", True),
        (" 07.ComCenterBN 2-7", False),
        ("Floor3/Raw2/Rack5", False),
        ("HelpDesk#3\u041237", False),
        ("KARANTIN 1/4", False),
        ("MR-Shamora#1012", False),
        ("MoulinRouge#2154,2155,2161,2162", False),
        ("Raw2/Rack5", False),
        ("Raw4 Rack3", True),
        ("Raw4 Rack3 ", False),
        ("07.ComCenterBN-2-7", True),
        ("Floor3-Raw2-Rack5", True),
        ("HelpDesk_3", True),
    ],
)
def test_location_validation(rack, is_valid):
    location = juggler._Location("vla-01", rack, "1")
    validation_res = juggler._validate_location(location)
    if is_valid:
        assert validation_res is None
    else:
        assert isinstance(validation_res, str)


class TestJugglerCheckGenerator:
    @staticmethod
    def _get_check_kwargs(check_name):
        check_kwargs = config.get_value("juggler.checks.{}".format(check_name)).copy()
        for mixin in check_kwargs.pop("mixins"):
            mixin_params = config.get_value("juggler.mixins.{}".format(mixin))
            check_kwargs = juggler._merge(check_kwargs, mixin_params)

        return check_kwargs

    @staticmethod
    def _create_expected_check(check_name, aggregate_name, stand_uid, group_filter, mark, **check_kwargs):
        if "flap" in check_kwargs:
            check_kwargs["flaps_config"] = FlapOptions(**check_kwargs.pop("flap"))

        check_kwargs["tags"] = [stand_uid] + check_kwargs["tags"]

        return Check(
            host=aggregate_name,
            service=check_name,
            mark=mark,
            notifications=[NotificationOptions(**config.get_value("juggler.notifications.push"))],
            children=[
                Child(group_type="WALLE", service=check_name, host="DEV@{}".format(group_filter.to_juggler_request())),
            ],
            **check_kwargs,
        )

    @pytest.mark.parametrize("check_name", ["UNREACHABLE", "ssh"])
    def test_pre_configured_checks(self, check_name):
        aggregate_name = "aggregate-name-mock"
        mark = "mark-mock"
        group_filter = juggler.GroupFilter("queue", "rack", ["prj"])
        stand_uid = "stand-uid-mock"

        check_kwargs = self._get_check_kwargs(check_name)

        expected_check = self._create_expected_check(
            check_name, aggregate_name, stand_uid, group_filter, mark, **check_kwargs
        )

        check_generator = juggler._get_juggler_check_generator(stand_uid, check_name)
        res = check_generator(aggregate_name, group_filter, mark)

        assert res == expected_check

    def test_generic_check(self):
        aggregate_name = "aggregate-name-mock"
        check_name = "check_mock"
        mark = "mark-mock"
        group_filter = juggler.GroupFilter("queue", "rack", ["prj"])
        stand_uid = "stand-uid-mock"

        check_kwargs = self._get_check_kwargs("_generic_passive")
        expected_check = self._create_expected_check(
            check_name, aggregate_name, stand_uid, group_filter, mark, **check_kwargs
        )

        check_generator = juggler._get_juggler_check_generator(stand_uid, check_name)
        res = check_generator(aggregate_name, group_filter, mark)

        assert res.to_dict() == expected_check.to_dict()
