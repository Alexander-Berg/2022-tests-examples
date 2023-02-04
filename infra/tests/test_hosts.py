"""Tests various operations on hosts."""


from collections import namedtuple

import pytest

import walle.expert
import walle.host_status
from infra.walle.server.tests.lib.util import (
    TestCase,
    mock_task,
)
from walle.clients import inventory
from walle.clients.eine import ProfileMode, EineProfileTags
from walle.constants import (
    EINE_PROFILES_WITH_DC_SUPPORT,
    NetworkTarget,
    PROVISIONER_LUI,
    PROVISIONER_EINE,
    FLEXY_EINE_PROFILE,
)
from walle.errors import InvalidProfileNameError
from walle.hosts import (
    HostState,
    HostStatus,
    ProfileConfiguration,
    DeployConfiguration,
    Host,
)

MOCK_TICKET_KEY = "MOCK-1234"


@pytest.fixture
def test(mp, request, monkeypatch_timestamp):
    mp.function(
        walle.host_status.get_tickets_by_query, module=walle.host_status, return_value=[{"key": MOCK_TICKET_KEY}]
    )
    return TestCase.create(request)


class TestISSFlag:
    def test_iss_flag_default_value_is_false(self, test):
        host = test.mock_host(dict(state=HostState.ASSIGNED, task=mock_task()))
        assert host.get_iss_ban_flag() is False

    def test_iss_flag_set_to_true(self, test):
        host = test.mock_host(dict(state=HostState.ASSIGNED, task=mock_task()))
        host.set_iss_ban_flag()
        assert host.get_iss_ban_flag() is True


@pytest.mark.parametrize(
    "profile,profile_tags,result_profile,result_profile_tags",
    (
        (TestCase.host_profile, ["a", "b"], TestCase.host_profile, ["a", "b", "c", "d"]),
        (TestCase.host_profile, None, TestCase.host_profile, ["c", "d"]),
        (None, ["a", "b"], TestCase.project_profile(), ["a", "b", "c", "d"]),
        (None, None, TestCase.project_profile(), ["c", "d"]),
    ),
)
def test_deduce_profile_configuration(mp, test, profile, profile_tags, result_profile, result_profile_tags):
    mp.function(inventory.get_eine_profiles, return_value=[TestCase.project_profile(), TestCase.host_profile])

    project = test.mock_project({"id": "some-id", "profile": TestCase.project_profile(), "profile_tags": ["c", "d"]})
    host = test.mock_host(dict(project=project.id))

    assert host.deduce_profile_configuration(profile, profile_tags) == ProfileConfiguration(
        result_profile, result_profile_tags, None
    )


@pytest.mark.parametrize(
    "profile,profile_tags,result_profile,result_profile_tags",
    (
        (TestCase.host_profile, ["a", "b"], TestCase.host_profile, ["a", "b"]),
        (TestCase.host_profile, None, TestCase.host_profile, []),
    ),
)
def test_deduce_profile_configuration_without_defaults(
    mp, test, profile, profile_tags, result_profile, result_profile_tags
):
    mp.function(inventory.get_eine_profiles, return_value=[TestCase.host_profile])

    project = test.mock_project({"id": "some-id", "profile": None, "profile_tags": None})
    host = test.mock_host(dict(project=project.id))

    assert host.deduce_profile_configuration(profile, profile_tags) == ProfileConfiguration(
        result_profile, result_profile_tags, None
    )


@pytest.mark.parametrize("profile_tags", (None, ["a", "b"]))
def test_deduce_profile_configuration_without_defaults_and_custom_profile(mp, test, profile_tags):
    mp.function(inventory.get_eine_profiles, return_value=[TestCase.host_profile])

    project = test.mock_project({"id": "some-id", "profile": None, "profile_tags": None})
    host = test.mock_host(dict(project=project.id))

    expected_configuration = ProfileConfiguration(TestCase.host_profile, [], None)
    assert expected_configuration == host.deduce_profile_configuration(profile=TestCase.host_profile)


def test_deduce_profile_configuration_invalid_profile(mp, test):
    mp.function(inventory.get_eine_profiles, return_value=[TestCase.project_profile()])

    project = test.mock_project({"id": "some-id"})
    host = test.mock_host(dict(project=project.id))

    with pytest.raises(InvalidProfileNameError):
        host.deduce_profile_configuration(profile=TestCase.host_profile)


def monkeypatch_for_deduce_profile_configuration_tests(mp, test):
    mp.function(inventory.get_eine_profiles, return_value=EINE_PROFILES_WITH_DC_SUPPORT + [TestCase.host_profile])

    host = test.mock_host()
    chosen_profile = FLEXY_EINE_PROFILE
    return host, chosen_profile


def test_deduce_profile_configuration_with_profile_mode(mp, test):
    host, chosen_profile = monkeypatch_for_deduce_profile_configuration_tests(mp, test)

    assert host.deduce_profile_configuration(TestCase.host_profile, ["a", "b"]) == ProfileConfiguration(
        TestCase.host_profile, ["a", "b"], None
    )

    assert host.deduce_profile_configuration(chosen_profile, ["a", "b"]) == ProfileConfiguration(
        chosen_profile, ["a", "b"], None
    )

    tags = ["a", EineProfileTags.FULL_PROFILING, EineProfileTags.ADVANCED_LOAD, "b"]
    assert host.deduce_profile_configuration(chosen_profile, tags) == ProfileConfiguration(
        chosen_profile, sorted(tags), [ProfileMode.HIGHLOAD_TEST]
    )

    tags = ["a", EineProfileTags.FULL_PROFILING, EineProfileTags.ADVANCED_LOAD, "b", EineProfileTags.MEMORY_RELAX]
    assert host.deduce_profile_configuration(chosen_profile, tags) == ProfileConfiguration(
        chosen_profile, sorted(tags), None
    )

    tags = ["a", EineProfileTags.FULL_PROFILING, EineProfileTags.ADVANCED_LOAD, EineProfileTags.DANGEROUS_LOAD, "b"]
    assert host.deduce_profile_configuration(chosen_profile, tags) == ProfileConfiguration(
        chosen_profile, sorted(tags), [ProfileMode.DISK_RW_TEST, ProfileMode.HIGHLOAD_TEST]
    )

    tags = ["a", EineProfileTags.DANGEROUS_LOAD, "b"]
    assert host.deduce_profile_configuration(chosen_profile, tags) == ProfileConfiguration(
        chosen_profile, sorted(tags), None
    )

    tags = [
        "a",
        EineProfileTags.FULL_PROFILING,
        EineProfileTags.ADVANCED_LOAD,
        EineProfileTags.DANGEROUS_LOAD,
        EineProfileTags.MEMORY_RELAX,
        "b",
    ]
    assert host.deduce_profile_configuration(chosen_profile, tags) == ProfileConfiguration(
        chosen_profile, sorted(tags), [ProfileMode.DISK_RW_TEST]
    )

    tags = [
        "a",
        EineProfileTags.FULL_PROFILING,
        EineProfileTags.ADVANCED_LOAD,
        EineProfileTags.SMART_RELAX,
        EineProfileTags.DANGEROUS_LOAD,
        EineProfileTags.MEMORY_RELAX,
        "b",
    ]
    assert host.deduce_profile_configuration(chosen_profile, tags) == ProfileConfiguration(
        chosen_profile, sorted(tags), None
    )

    tags = ["a", EineProfileTags.SMART_RELAX, EineProfileTags.MEMORY_RELAX, "b"]
    result_tags = ["a", EineProfileTags.SMART_RELAX, EineProfileTags.FULL_PROFILING, EineProfileTags.ADVANCED_LOAD, "b"]
    assert host.deduce_profile_configuration(chosen_profile, tags, ProfileMode.HIGHLOAD_TEST) == ProfileConfiguration(
        chosen_profile, sorted(result_tags), [ProfileMode.HIGHLOAD_TEST]
    )


def test_deduce_profile_configuration_with_default_mode(mp, test):
    host, chosen_profile = monkeypatch_for_deduce_profile_configuration_tests(mp, test)

    expected_result = ProfileConfiguration(chosen_profile, [EineProfileTags.FULL_PROFILING], [ProfileMode.DEFAULT])
    assert host.deduce_profile_configuration(chosen_profile, [], ProfileMode.DEFAULT) == expected_result

    expected_result = ProfileConfiguration(chosen_profile, ["a", EineProfileTags.FULL_PROFILING], [ProfileMode.DEFAULT])
    assert (
        host.deduce_profile_configuration(
            chosen_profile, [EineProfileTags.SWP_UP, EineProfileTags.FULL_PROFILING, "a"], ProfileMode.DEFAULT
        )
        == expected_result
    )


def test_deduce_profile_configuration_with_firmware_update_mode(mp, test):
    host, chosen_profile = monkeypatch_for_deduce_profile_configuration_tests(mp, test)

    assert host.deduce_profile_configuration(chosen_profile, [], ProfileMode.FIRMWARE_UPDATE) == ProfileConfiguration(
        chosen_profile,
        [EineProfileTags.FIRMWARE_UPDATE, EineProfileTags.MEMORY_RELAX, EineProfileTags.SMART_RELAX],
        [ProfileMode.FIRMWARE_UPDATE],
    )

    assert host.deduce_profile_configuration(
        chosen_profile, [EineProfileTags.FULL_PROFILING], ProfileMode.FIRMWARE_UPDATE
    ) == ProfileConfiguration(
        chosen_profile,
        [EineProfileTags.FIRMWARE_UPDATE, EineProfileTags.MEMORY_RELAX, EineProfileTags.SMART_RELAX],
        [ProfileMode.FIRMWARE_UPDATE],
    )


def test_deduce_profile_configuration_with_highload_test_mode(mp, test):
    host, chosen_profile = monkeypatch_for_deduce_profile_configuration_tests(mp, test)

    assert host.deduce_profile_configuration(chosen_profile, [], ProfileMode.HIGHLOAD_TEST) == ProfileConfiguration(
        chosen_profile, [EineProfileTags.ADVANCED_LOAD, EineProfileTags.FULL_PROFILING], [ProfileMode.HIGHLOAD_TEST]
    )

    assert host.deduce_profile_configuration(
        chosen_profile, [EineProfileTags.SWP_UP, EineProfileTags.FULL_PROFILING, EineProfileTags.ADVANCED_LOAD]
    ) == ProfileConfiguration(
        chosen_profile,
        [EineProfileTags.ADVANCED_LOAD, EineProfileTags.FULL_PROFILING, EineProfileTags.SWP_UP],
        [ProfileMode.HIGHLOAD_TEST],
    )

    assert host.deduce_profile_configuration(
        chosen_profile, [EineProfileTags.SWP_UP], ProfileMode.HIGHLOAD_TEST
    ) == ProfileConfiguration(
        chosen_profile, [EineProfileTags.ADVANCED_LOAD, EineProfileTags.FULL_PROFILING], [ProfileMode.HIGHLOAD_TEST]
    )


def test_deduce_profile_configuration_with_disr_rw_test_mode(mp, test):
    host, chosen_profile = monkeypatch_for_deduce_profile_configuration_tests(mp, test)

    assert host.deduce_profile_configuration(
        chosen_profile, [EineProfileTags.FULL_PROFILING, EineProfileTags.DANGEROUS_LOAD]
    ) == ProfileConfiguration(
        chosen_profile, [EineProfileTags.DANGEROUS_LOAD, EineProfileTags.FULL_PROFILING], [ProfileMode.DISK_RW_TEST]
    )

    expected_modes = (
        [ProfileMode.DISK_RW_TEST, ProfileMode.HIGHLOAD_TEST]
        if chosen_profile != FLEXY_EINE_PROFILE
        else [ProfileMode.DISK_RW_TEST]
    )

    assert host.deduce_profile_configuration(
        chosen_profile, ["a", EineProfileTags.MEMORY_RELAX, EineProfileTags.SMART_RELAX, "b"], ProfileMode.DISK_RW_TEST
    ) == ProfileConfiguration(
        chosen_profile, ["a", "b", EineProfileTags.DANGEROUS_LOAD, EineProfileTags.FULL_PROFILING], expected_modes
    )


def test_deduce_profile_configuration_with_swp_up_mode(mp, test):
    host, chosen_profile = monkeypatch_for_deduce_profile_configuration_tests(mp, test)

    assert host.deduce_profile_configuration(chosen_profile, [], ProfileMode.SWP_UP) == ProfileConfiguration(
        chosen_profile, [EineProfileTags.SWP_UP], [ProfileMode.SWP_UP]
    )

    assert host.deduce_profile_configuration(
        chosen_profile, [EineProfileTags.FULL_PROFILING], ProfileMode.SWP_UP
    ) == ProfileConfiguration(
        chosen_profile, [EineProfileTags.FULL_PROFILING, EineProfileTags.SWP_UP], [ProfileMode.SWP_UP]
    )


DeployParamsFixture = namedtuple("DeployParamsFixture", ["configured", "requested", "applied", "deploy_config"])
DeployParams = namedtuple("DeployParams", ["provisioner", "config", "tags", "network", "deploy_config_policy"])


@pytest.mark.parametrize(
    "deploy_params",
    [
        # use specified provisioner and config, update the host
        DeployParamsFixture(
            configured=DeployParams(PROVISIONER_LUI, "hc", ["ht"], None, None),
            requested=DeployParams(PROVISIONER_EINE, "rc", None, None, None),
            applied=DeployParams(PROVISIONER_EINE, "rc", None, None, None),
            deploy_config=DeployConfiguration(PROVISIONER_EINE, "rc", None, False, NetworkTarget.DEFAULT, True, None),
        ),
        # use specified config and tags(None), use provisioner from host, update the host
        DeployParamsFixture(
            configured=DeployParams(PROVISIONER_EINE, "hc", ["ht"], None, None),
            requested=DeployParams(None, "rc", None, None, None),
            applied=DeployParams(PROVISIONER_EINE, "rc", None, None, None),
            deploy_config=DeployConfiguration(PROVISIONER_EINE, "rc", None, False, NetworkTarget.DEFAULT, True, None),
        ),
        # use specified config, tags and network, use provisioner from host, update the host
        DeployParamsFixture(
            configured=DeployParams(PROVISIONER_EINE, "hc", ["ht"], None, None),
            requested=DeployParams(None, "rc", ["rt"], NetworkTarget.SERVICE, None),
            applied=DeployParams(PROVISIONER_EINE, "rc", ["rt"], NetworkTarget.SERVICE, None),
            deploy_config=DeployConfiguration(PROVISIONER_EINE, "rc", ["rt"], False, NetworkTarget.SERVICE, True, None),
        ),
        # use specified network, use config, tags and provisioner from host, update the host
        DeployParamsFixture(
            configured=DeployParams(PROVISIONER_EINE, "hc", ["ht"], None, None),
            requested=DeployParams(None, None, None, NetworkTarget.SERVICE, None),
            applied=DeployParams(PROVISIONER_EINE, "hc", ["ht"], NetworkTarget.SERVICE, None),
            deploy_config=DeployConfiguration(PROVISIONER_EINE, "hc", ["ht"], False, NetworkTarget.SERVICE, True, None),
        ),
        # use specified provisioner, config and tags, update the host
        DeployParamsFixture(
            configured=DeployParams(None, None, None, None, None),
            requested=DeployParams(PROVISIONER_EINE, "rc", ["rt"], None, None),
            applied=DeployParams(PROVISIONER_EINE, "rc", ["rt"], None, None),
            deploy_config=DeployConfiguration(PROVISIONER_EINE, "rc", ["rt"], False, NetworkTarget.DEFAULT, True, None),
        ),
        # use specified provisioner, config and network, update the host
        DeployParamsFixture(
            configured=DeployParams(None, None, None, None, None),
            requested=DeployParams(PROVISIONER_LUI, "rc", None, NetworkTarget.PROJECT, "passthrough"),
            applied=DeployParams(PROVISIONER_LUI, "rc", None, NetworkTarget.PROJECT, "passthrough"),
            deploy_config=DeployConfiguration(
                PROVISIONER_LUI, "rc", None, False, NetworkTarget.PROJECT, True, "passthrough"
            ),
        ),
        # inherit all fields from project, don't update the host
        DeployParamsFixture(
            configured=DeployParams(None, None, None, None, None),
            requested=DeployParams(None, None, None, None, None),
            applied=DeployParams(None, None, None, None, None),
            deploy_config=DeployConfiguration(
                PROVISIONER_LUI, "pc", ["pt"], False, NetworkTarget.DEFAULT, True, "diskmanager"
            ),
        ),
        # use specified provisioner and config, update the host
        DeployParamsFixture(
            configured=DeployParams(None, None, None, None, None),
            requested=DeployParams(PROVISIONER_LUI, "rc", None, None, "passthrough"),
            applied=DeployParams(PROVISIONER_LUI, "rc", None, None, "passthrough"),
            deploy_config=DeployConfiguration(
                PROVISIONER_LUI, "rc", None, False, NetworkTarget.DEFAULT, True, "passthrough"
            ),
        ),
        # inherit provisioner from project and use specified config, update the host
        DeployParamsFixture(
            configured=DeployParams(None, None, None, None, None),
            requested=DeployParams(None, "rc", None, None, None),
            applied=DeployParams(PROVISIONER_LUI, "rc", None, None, None),
            deploy_config=DeployConfiguration(PROVISIONER_LUI, "rc", None, False, NetworkTarget.DEFAULT, True, None),
        ),
        # inherit provisioner from project and use specified config and network, update the host
        DeployParamsFixture(
            configured=DeployParams(None, None, None, None, None),
            requested=DeployParams(None, "rc", None, NetworkTarget.SERVICE, None),
            applied=DeployParams(PROVISIONER_LUI, "rc", None, NetworkTarget.SERVICE, None),
            deploy_config=DeployConfiguration(PROVISIONER_LUI, "rc", None, False, NetworkTarget.SERVICE, True, None),
        ),
        # inherit provisioner and config from project use network, update the host
        DeployParamsFixture(
            configured=DeployParams(None, None, None, None, None),
            requested=DeployParams(None, None, None, NetworkTarget.SERVICE, None),
            applied=DeployParams(PROVISIONER_LUI, "pc", ["pt"], NetworkTarget.SERVICE, "diskmanager"),
            deploy_config=DeployConfiguration(
                PROVISIONER_LUI, "pc", ["pt"], False, NetworkTarget.SERVICE, True, "diskmanager"
            ),
        ),
        # host has everything, don't update it
        DeployParamsFixture(
            configured=DeployParams(PROVISIONER_LUI, "hc", None, None, "passthrough"),
            requested=DeployParams(None, None, None, None, None),
            applied=DeployParams(None, None, None, None, None),
            deploy_config=DeployConfiguration(
                PROVISIONER_LUI, "hc", None, False, NetworkTarget.DEFAULT, True, "passthrough"
            ),
        ),
        # host has everything, don't update it
        DeployParamsFixture(
            configured=DeployParams(PROVISIONER_EINE, "hc", ["ht"], None, None),
            requested=DeployParams(None, None, None, None, None),
            applied=DeployParams(None, None, None, None, None),
            deploy_config=DeployConfiguration(PROVISIONER_EINE, "hc", ["ht"], False, NetworkTarget.DEFAULT, True, None),
        ),
        # use specified provisioner, config and tags, update the host
        DeployParamsFixture(
            configured=DeployParams(None, None, None, None, None),
            requested=DeployParams(PROVISIONER_EINE, "rc", [], None, None),
            applied=DeployParams(PROVISIONER_EINE, "rc", None, None, None),
            deploy_config=DeployConfiguration(PROVISIONER_EINE, "rc", None, False, NetworkTarget.DEFAULT, True, None),
        ),
    ],
)
def test_deduce_deploy_configuration(mp, test, deploy_params):
    mp.function(inventory.get_eine_profiles, return_value=["profile-mock"])
    mp.function(inventory.check_deploy_configuration)

    test.mock_project(
        {
            "id": "test-project",
            "name": "test-project",
            "provisioner": PROVISIONER_LUI,
            "deploy_config": "pc",
            "deploy_tags": ["pt"],
            "deploy_config_policy": "diskmanager",
        }
    )
    host = test.mock_host(
        {"inv": 0, "state": HostState.ASSIGNED, "status": HostStatus.READY, "project": "test-project"}
    )

    if deploy_params.configured.provisioner is not None:
        host.provisioner = deploy_params.configured.provisioner
    else:
        del host.provisioner

    if deploy_params.configured.config is not None:
        host.config = deploy_params.configured.config
    else:
        del host.config

    if deploy_params.configured.tags is not None:
        host.deploy_tags = deploy_params.configured.tags
    else:
        del host.deploy_tags

    if deploy_params.configured.deploy_config_policy is not None:
        host.deploy_config_policy = deploy_params.configured.deploy_config_policy
    else:
        del host.deploy_config_policy

    hp, hc, ht, hn, hdcp, dc = host.deduce_deploy_configuration(*deploy_params.requested)
    assert [(hp, hc, ht, hn, hdcp), dc] == [deploy_params.applied, deploy_params.deploy_config]


class TestIsMaintenance:
    @pytest.mark.parametrize(
        "status", (HostStatus.default(HostState.MAINTENANCE), HostStatus.DEAD, HostStatus.ALL_TASK[0])
    )
    def test_maintenance_no_issuer_is_maintenance(self, status):
        host = Host(
            state=HostState.MAINTENANCE, status=status, state_author="maintenance-author@", status_author="other-user@"
        )
        assert host.is_maintenance()

    @pytest.mark.parametrize(
        "status", (HostStatus.default(HostState.MAINTENANCE), HostStatus.DEAD, HostStatus.ALL_TASK[0])
    )
    def test_maintenance_with_other_issuer_is_maintenance(self, status):
        host = Host(
            state=HostState.MAINTENANCE, status=status, state_author="maintenance-author@", status_author="other-user@"
        )
        assert host.is_maintenance("request-issuer@")

    @pytest.mark.parametrize(
        "status", (HostStatus.default(HostState.MAINTENANCE), HostStatus.DEAD, HostStatus.ALL_TASK[0])
    )
    def test_maintenance_with_same_state_author_is_not_maintenance(self, status):
        host = Host(
            state=HostState.MAINTENANCE, status=status, state_author="request-issuer@", status_author="other-user@"
        )
        assert not host.is_maintenance("request-issuer@")
