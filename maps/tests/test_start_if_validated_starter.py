from maps.garden.sdk.module_autostart import module_autostart as autostart
from maps.garden.sdk.module_autostart import validator_starters
from maps.garden.sdk.module_traits import module_traits as mt

MODULE_TRAITS = mt.ModuleTraits(
    name="geocoder_export",
    type=mt.ModuleType.MAP,
    sources=["geocoder_indexer"],
)


def test_success():
    source_build = autostart.Build(
        full_id=autostart.BuildId(module_name="geocoder_indexer", id=1),
        source_ids=[],
        properties={"release_name": "202101013"},
        status=autostart.BuildStatus.COMPLETED,
    )

    validator_build = autostart.Build(
        full_id=autostart.BuildId(module_name="geocoder_tester", id=1),
        source_ids=[autostart.BuildId(module_name="geocoder_indexer", id=1)],
        properties={"release_name": "202101013"},
        status=autostart.BuildStatus.COMPLETED,
    )

    build_manager = autostart.BuildManager([source_build, validator_build], MODULE_TRAITS)

    validator_starters.start_if_validated(validator_build, build_manager)

    assert build_manager.build_to_create.source_ids == [source_build.full_id]
    assert build_manager.build_to_create.properties is None
