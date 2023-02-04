from maps.garden.sdk.module_autostart import module_autostart as autostart
from maps.garden.sdk.module_autostart import deploy_starters
from maps.garden.sdk.module_traits import module_traits as mt

MODULE_TRAITS = mt.ModuleTraits(
    name="bicycle_graph_deployment",
    type=mt.ModuleType.DEPLOYMENT,
    sources=["bicycle_graph"],
    deploy_steps=[
        "testing",
        "stable",
    ],
)

INITIAL_BUILDS = [
    autostart.Build(
        full_id=autostart.BuildId(module_name="bicycle_graph", id=1),
        source_ids=[],
        properties={"release_name": "20201103"},
        status=autostart.BuildStatus.COMPLETED,
    ),
    autostart.Build(
        full_id=autostart.BuildId(module_name="bicycle_graph_deployment", id=1),
        source_ids=[autostart.BuildId(module_name="bicycle_graph", id=1)],
        properties={"release_name": "20201103", "deploy_step": "testing"},
        status=autostart.BuildStatus.COMPLETED,
    ),
    autostart.Build(
        full_id=autostart.BuildId(module_name="bicycle_graph_deployment", id=2),
        source_ids=[autostart.BuildId(module_name="bicycle_graph", id=1)],
        properties={"release_name": "20201103", "deploy_step": "stable"},
        status=autostart.BuildStatus.COMPLETED,
    ),
]


NEW_DATA_BUILD = autostart.Build(
    full_id=autostart.BuildId(module_name="bicycle_graph", id=2),
    source_ids=[],
    properties={"release_name": "20201104"},
    status=autostart.BuildStatus.COMPLETED,
)

TESTING_BUILD = autostart.Build(
    full_id=autostart.BuildId(module_name="bicycle_graph_deployment", id=3),
    source_ids=[autostart.BuildId(module_name="bicycle_graph", id=2)],
    properties={"release_name": "20201104", "deploy_step": "testing"},
    status=autostart.BuildStatus.COMPLETED,
)

STABLE_BUILD = autostart.Build(
    full_id=autostart.BuildId(module_name="bicycle_graph_deployment", id=4),
    source_ids=[autostart.BuildId(module_name="bicycle_graph", id=2)],
    properties={"release_name": "20201104", "deploy_step": "stable"},
    status=autostart.BuildStatus.COMPLETED,
)

NEWER_TESTING_BUILD = autostart.Build(
    full_id=autostart.BuildId(module_name="bicycle_graph_deployment", id=5),
    source_ids=[autostart.BuildId(module_name="bicycle_graph", id=2)],
    properties={"release_name": "20201105", "deploy_step": "testing"},
    status=autostart.BuildStatus.COMPLETED,
)


def test_trigger_is_data_build():
    build_manager = autostart.BuildManager(
        INITIAL_BUILDS + [NEW_DATA_BUILD],
        MODULE_TRAITS,
    )

    deploy_starters.deploy_to_steps_instantly(NEW_DATA_BUILD, build_manager)

    assert build_manager.build_to_create.source_ids == [NEW_DATA_BUILD.full_id]
    assert build_manager.build_to_create.properties == {"deploy_step": "testing"}


def test_trigger_is_testing_step():
    build_manager = autostart.BuildManager(
        INITIAL_BUILDS + [NEW_DATA_BUILD, TESTING_BUILD],
        MODULE_TRAITS,
    )

    deploy_starters.deploy_to_steps_instantly(TESTING_BUILD, build_manager)

    assert build_manager.build_to_create.source_ids == [NEW_DATA_BUILD.full_id]
    assert build_manager.build_to_create.properties == {"deploy_step": "stable"}


def test_trigger_is_stable_step():
    build_manager = autostart.BuildManager(
        INITIAL_BUILDS + [NEW_DATA_BUILD, TESTING_BUILD, STABLE_BUILD],
        MODULE_TRAITS,
    )

    deploy_starters.deploy_to_steps_instantly(STABLE_BUILD, build_manager)

    assert build_manager.build_to_create is None


def test_old_release():
    build_manager = autostart.BuildManager(
        INITIAL_BUILDS + [NEW_DATA_BUILD, NEWER_TESTING_BUILD],
        MODULE_TRAITS,
    )

    deploy_starters.deploy_to_steps_instantly(NEW_DATA_BUILD, build_manager)

    # source build has older release_name that current deploy build
    assert build_manager.build_to_create is None
