import pytest

from maps.garden.sdk.module_autostart import module_autostart as autostart
from maps.garden.sdk.module_autostart import reduce_starters
from maps.garden.sdk.module_traits import module_traits as mt

MODULE_TRAITS = mt.ModuleTraits(
    name="masstransit",
    type=mt.ModuleType.MAP,
    sources=[
        "masstransit_static",
        "pedestrian_graph",
    ],
)

SOURCE_A_1_BUILD = autostart.Build(
    full_id=autostart.BuildId(module_name="masstransit_static", id=1),
    source_ids=[],
    properties={"release_name": "20201103"},
    status=autostart.BuildStatus.COMPLETED,
)

SOURCE_B_1_BUILD = autostart.Build(
    full_id=autostart.BuildId(module_name="pedestrian_graph", id=2),
    source_ids=[],
    properties={"release_name": "20201103"},
    status=autostart.BuildStatus.COMPLETED,
)

SOURCE_A_2_BUILD = autostart.Build(
    full_id=autostart.BuildId(module_name="masstransit_static", id=3),
    source_ids=[],
    properties={"release_name": "20201103"},
    status=autostart.BuildStatus.COMPLETED,
)

SOURCE_B_2_BUILD = autostart.Build(
    full_id=autostart.BuildId(module_name="pedestrian_graph", id=4),
    source_ids=[],
    properties={"release_name": "20201104"},
    status=autostart.BuildStatus.COMPLETED,
)

PREVIOUS_BUILD = autostart.Build(
    full_id=autostart.BuildId(module_name="masstransit", id=1),
    source_ids=[
        SOURCE_A_1_BUILD.full_id,
        SOURCE_B_1_BUILD.full_id,
    ],
    properties={"release_name": "20201103"},
    status=autostart.BuildStatus.COMPLETED,
)

PREVIOUS_BUILD_IN_PROGRESS = autostart.Build(
    full_id=autostart.BuildId(module_name="masstransit", id=2),
    source_ids=[
        SOURCE_A_1_BUILD.full_id,
        SOURCE_B_1_BUILD.full_id,
    ],
    properties={"release_name": "20201104"},
    status=autostart.BuildStatus.IN_PROGRESS,
)


@pytest.mark.parametrize(
    "trigger_build",
    [
        SOURCE_A_1_BUILD,
        SOURCE_B_1_BUILD,
        SOURCE_A_2_BUILD,
        SOURCE_B_2_BUILD,
        PREVIOUS_BUILD,
    ],
)
def test_success(trigger_build):
    """
    Trigger doesn't matter: autostarter always tooks the last sources
    """
    build_manager = autostart.BuildManager(
        [
            SOURCE_A_1_BUILD,
            SOURCE_B_1_BUILD,
            SOURCE_A_2_BUILD,
            SOURCE_B_2_BUILD,
            PREVIOUS_BUILD,
        ],
        MODULE_TRAITS,
    )

    reduce_starters.start_with_last_sources(trigger_build, build_manager)

    assert set(build_manager.build_to_create.source_ids) == {
        SOURCE_A_2_BUILD.full_id,
        SOURCE_B_2_BUILD.full_id,
    }
    assert build_manager.build_to_create.properties is None


def test_previous_build_in_progress():
    """
    Do not create a new build if previous build is in progress
    """
    build_manager = autostart.BuildManager(
        [
            SOURCE_A_1_BUILD,
            SOURCE_B_1_BUILD,
            SOURCE_A_2_BUILD,
            SOURCE_B_2_BUILD,
            PREVIOUS_BUILD,
            PREVIOUS_BUILD_IN_PROGRESS,
        ],
        MODULE_TRAITS,
    )

    reduce_starters.start_with_last_sources(
        SOURCE_A_2_BUILD,
        build_manager,
        module_triggers_itself=True,
    )

    assert build_manager.build_to_create is None
