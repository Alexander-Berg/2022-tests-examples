from maps.garden.sdk.module_autostart.module_autostart import Build, BuildId, BuildStatus, BuildManager
from maps.garden.sdk.module_traits import module_traits as mt


MODULE_TRAITS = mt.ModuleTraits(
    name="test_module",
    type=mt.ModuleType.MAP,
)


def test_build_manager():
    builds = [
        Build(
            full_id=BuildId(module_name="ymapsdf_src", id=1),
            source_ids=[],
            properties={"shipping_date": "202101013", "region": "cis1"},
            status=BuildStatus.COMPLETED,
        ),
        Build(
            full_id=BuildId(module_name="ymapsdf_src", id=2),
            source_ids=[],
            properties={"shipping_date": "202101013", "region": "cis2"},
            status=BuildStatus.COMPLETED,
        ),
        Build(
            full_id=BuildId(module_name="ymapsdf_src", id=3),
            source_ids=[],
            properties={"shipping_date": "202101014", "region": "cis1"},
            status=BuildStatus.IN_PROGRESS,
        ),
        Build(
            full_id=BuildId(module_name="ymapsdf_src", id=4),
            source_ids=[],
            properties={"shipping_date": "202101015", "region": "cis1"},
            status=BuildStatus.COMPLETED,
        ),
        Build(
            full_id=BuildId(module_name="ymapsdf_src", id=5),
            source_ids=[],
            properties={"shipping_date": "202101015", "region": "cis2"},
            status=BuildStatus.FAILED,
        ),
    ]

    build_manager = BuildManager(builds, MODULE_TRAITS)

    assert build_manager.target_module_name == "test_module"

    assert build_manager.get_build(BuildId(module_name="ymapsdf_src", id=1))
    assert not build_manager.get_build(BuildId(module_name="ymapsdf_src", id=123))

    result_builds = build_manager.get_builds("ymapsdf_src")
    assert len(result_builds) == len(builds)

    result_builds = build_manager.get_completed_builds("ymapsdf_src")
    assert len(result_builds) == 3  # builds with status BuildStatus.COMPLETED

    assert build_manager.get_builds("another_module") == []
    assert build_manager.get_completed_builds("another_module") == []

    result_build = build_manager.get_last("ymapsdf_src")
    assert result_build.full_id == BuildId(module_name="ymapsdf_src", id=5)

    result_build = build_manager.get_last("ymapsdf_src", status=BuildStatus.COMPLETED)
    assert result_build.full_id == BuildId(module_name="ymapsdf_src", id=4)

    result_build = build_manager.get_last("ymapsdf_src", region="cis2")
    assert result_build.full_id == BuildId(module_name="ymapsdf_src", id=5)

    result_build = build_manager.get_last_completed("ymapsdf_src")
    assert result_build.full_id == BuildId(module_name="ymapsdf_src", id=4)

    result_build = build_manager.get_last_completed("ymapsdf_src", region="cis2")
    assert result_build.full_id == BuildId(module_name="ymapsdf_src", id=2)

    assert not build_manager.get_last_completed("ymapsdf_src", region="another_region")

    result_build = build_manager.get_last_running_build("ymapsdf_src")
    assert result_build.full_id == BuildId(module_name="ymapsdf_src", id=3)
