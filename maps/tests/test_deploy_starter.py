from maps.garden.sdk.module_autostart import module_autostart as autostart
from maps.garden.sdk.module_autostart import deploy_starters
from maps.garden.sdk.module_traits import module_traits as mt

MODULE_TRAITS = mt.ModuleTraits(
    name="bicycle_graph_deployment",
    type=mt.ModuleType.DEPLOYMENT,
    sources=["bicycle_graph"],
)


def test_success():
    source_build = autostart.Build(
        full_id=autostart.BuildId(module_name="bicycle_graph", id=1),
        source_ids=[],
        properties={"release_name": "20201103"},
        status=autostart.BuildStatus.COMPLETED,
    )

    build_manager = autostart.BuildManager([source_build], MODULE_TRAITS)

    deploy_starters.deploy_instantly(source_build, build_manager)

    assert build_manager.build_to_create.source_ids == [source_build.full_id]
    assert build_manager.build_to_create.properties is None


def test_old_release():
    source_build = autostart.Build(
        full_id=autostart.BuildId(module_name="bicycle_graph", id=1),
        source_ids=[],
        properties={"release_name": "20201103"},
        status=autostart.BuildStatus.COMPLETED,
    )

    other_deploy_build = autostart.Build(
        full_id=autostart.BuildId(module_name="bicycle_graph_deployment", id=1),
        source_ids=[],
        properties={"release_name": "20201104"},
        status=autostart.BuildStatus.COMPLETED,
    )

    build_manager = autostart.BuildManager([source_build, other_deploy_build], MODULE_TRAITS)

    deploy_starters.deploy_instantly(source_build, build_manager)

    # source build has older release_name that current deploy build
    assert build_manager.build_to_create is None
