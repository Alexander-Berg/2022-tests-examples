import collections
import dataclasses
import datetime as dt
import typing as tp

import networkx
import pytest
import pytz

from maps.garden.sdk.module_traits.module_traits import ModuleType, ModuleTraits, SortOption

from maps.garden.libs_server.build import (
    build_defs,
    build_statistics
)

from maps.garden.libs_server.common.module_event_types import CommonEventType
from maps.garden.libs_server.log_storage.module_event_storage import ModuleEventStorage
from maps.garden.libs_server.module import module_settings_storage

from maps.garden.tools.releases_monitoring.lib import path_inspection

CONTOUR_NAME = "contour_name"
SOURCE_NAME = "ymapsdf_src"
NOW = dt.datetime(2020, 10, 9, 16, 00, 00, tzinfo=pytz.utc)
LONG_AGO = NOW - path_inspection.MAX_BUILD_AGE * 2
RECENTLY = dt.datetime(2020, 10, 8, 13, 00, 00, tzinfo=pytz.utc)

MODULES_TRAITS_LIST = [
    ModuleTraits(name=SOURCE_NAME, type=ModuleType.SOURCE, sort_options=[SortOption(key_pattern="test_pattern")]),
    ModuleTraits(name="ymapsdf", type=ModuleType.MAP),
    ModuleTraits(name="road_graph_build", type=ModuleType.REDUCE),
    ModuleTraits(name="yet_another_graph_build", type=ModuleType.REDUCE),
    ModuleTraits(name="road_graph_deployment", type=ModuleType.DEPLOYMENT),
]


@dataclasses.dataclass
class ContourHelper:
    build_statistics_storage: build_statistics.BuildStatisticsStorage
    flow_graphs: tp.List[networkx.DiGraph]
    fresh_sources_builds: dict[dict[int, build_defs.Build]]
    module_settings: module_settings_storage.ModuleSettingsStorage
    module_event_storage: ModuleEventStorage
    contour_name: str
    ui_hostname: str = "https://test.yandex/"
    _build_counter: tp.Mapping[str, int] = dataclasses.field(
        default_factory=collections.Counter,
    )

    def save(
        self,
        name: str,
        creation_time: dt.datetime,
        sources: tp.Optional[tp.List[build_defs.Build]] = None,
        status_string: build_defs.BuildStatusString = build_defs.BuildStatusString.COMPLETED,
        more_extras: dict = {}
    ) -> build_defs.Build:

        self._build_counter[name] += 1
        tracked_ancestors = []
        if name == SOURCE_NAME:
            extras = {"shipping_date": f"{creation_time.strftime('%Y%m%d')}_1232321"}

            tracked_ancestors = [build_defs.TrackedAncestorBuild(
                name=SOURCE_NAME, build_id=self._build_counter[name]
            )]
        else:
            extras = {"shipping_date": max([source.extras.get("shipping_date", "") for source in sources])}
            tracked_ancestors_set = set()
            for src in sources or []:
                for build in src.tracked_ancestor_builds:
                    tracked_ancestors_set.add(build)

            tracked_ancestors = list(tracked_ancestors_set)

        if more_extras:
            extras.update(more_extras)

        ticket = "MAPSGARDENBUILD-123" if status_string == build_defs.BuildStatusString.FAILED else None

        build = build_defs.Build(
            id=self._build_counter[name],
            name=name,
            contour_name=CONTOUR_NAME,
            status=build_defs.BuildStatus(string=status_string),
            extras=extras,
            created_at=creation_time,
            sources=[build_defs.Source.generate_from(src_build) for src_build in sources or []],
            startrek_issue_key=ticket,
            tracked_ancestor_builds=tracked_ancestors,
            request_id=1
        )

        build_statistics = self.build_statistics_storage.create(build)

        if name == SOURCE_NAME and creation_time >= RECENTLY:
            self.fresh_sources_builds[SOURCE_NAME][self._build_counter[name]] = build_statistics

        return build

    def get_module_inspector(self, module_name: str):
        return path_inspection.ModuleInspector(
            source=SOURCE_NAME,
            target=module_name,
            contour_name=self.contour_name,
            ui_hostname=self.ui_hostname,
            fresh_source_builds=self.fresh_sources_builds[SOURCE_NAME],
            flow_graphs=self.flow_graphs,
            module_settings=self.module_settings,
            module_event_storage=self.module_event_storage,
            build_statistics_storage=self.build_statistics_storage
        )


@pytest.fixture
def contour_helper(db):
    build_statistics_storage = build_statistics.BuildStatisticsStorage(db)
    module_settings = module_settings_storage.ModuleSettingsStorage(db)

    flow_graph = networkx.DiGraph(name="Graph name")
    flow_graph.add_edge(SOURCE_NAME, "ymapsdf")
    flow_graph.add_edge("ymapsdf", "road_graph_build")
    flow_graph.add_edge("ymapsdf", "yet_another_graph_build")
    flow_graph.add_edge("road_graph_build", "road_graph_deployment")
    flow_graph.add_edge("yet_another_graph_build", "road_graph_deployment")

    return ContourHelper(
        build_statistics_storage=build_statistics_storage,
        flow_graphs=[flow_graph],
        module_settings=module_settings,
        module_event_storage=ModuleEventStorage(db),
        contour_name=CONTOUR_NAME,
        fresh_sources_builds=collections.defaultdict(dict)
    )


@pytest.mark.freeze_time(NOW)
def test_no_fresh_sources(contour_helper: ContourHelper):
    contour_helper.save(SOURCE_NAME, LONG_AGO)

    module_inspector = contour_helper.get_module_inspector("ymapsdf")
    assert not module_inspector.check()


@pytest.mark.freeze_time(NOW)
def test_no_fresh_target(contour_helper: ContourHelper):
    old_source = contour_helper.save(SOURCE_NAME, LONG_AGO)
    contour_helper.save(SOURCE_NAME, RECENTLY)
    contour_helper.save("ymapsdf", LONG_AGO+dt.timedelta(seconds=1), sources=[old_source])
    module_inspector = contour_helper.get_module_inspector("ymapsdf")
    return module_inspector.check()


@pytest.mark.freeze_time(NOW)
def test_old_target_with_multiple_regions_sources(contour_helper: ContourHelper):
    sources = [
        contour_helper.save(SOURCE_NAME, LONG_AGO, more_extras={"region": "cis1"}),
        contour_helper.save(SOURCE_NAME, LONG_AGO - dt.timedelta(days=1), more_extras={"region": "cis2"}),
        contour_helper.save(SOURCE_NAME, LONG_AGO - dt.timedelta(days=2), more_extras={"region": "eu"}),
        contour_helper.save(SOURCE_NAME, LONG_AGO - dt.timedelta(days=2), more_extras={"region": "tr"})
    ]
    contour_helper.save(SOURCE_NAME, RECENTLY)
    contour_helper.save(
        "ymapsdf", RECENTLY,
        sources=sources
    )
    module_inspector = contour_helper.get_module_inspector("ymapsdf")
    return module_inspector.check()


@pytest.mark.freeze_time(NOW)
def test_autostart_disabled(contour_helper: ContourHelper):
    old_source_build = contour_helper.save(SOURCE_NAME, LONG_AGO)
    contour_helper.save(SOURCE_NAME, RECENTLY)
    contour_helper.save("ymapsdf", LONG_AGO, sources=[old_source_build])

    contour_helper.module_event_storage.add_common_event(
        contour_name=contour_helper.contour_name,
        module_name="ymapsdf",
        username="@test_user",
        event_type=CommonEventType.AUTOSTART_DISABLED
    )

    contour_helper.module_event_storage.add_common_event(
        contour_name=contour_helper.contour_name,
        module_name="ymapsdf",
        username="@test_user",
        event_type=CommonEventType.AUTOSTART_ENABLED
    )

    contour_helper.module_event_storage.add_common_event(
        contour_name=contour_helper.contour_name,
        module_name="ymapsdf",
        username="@test_user",
        event_type=CommonEventType.AUTOSTART_DISABLED
    )

    contour_helper.module_settings.disable_autostart(
        module_name="ymapsdf",
        contour_name=contour_helper.contour_name,
        requested_by_user="@test_user"
    )
    contour_helper.module_settings.disable_autostart(
        module_name=SOURCE_NAME,
        contour_name=contour_helper.contour_name,
        requested_by_user="@another_user"
    )

    module_inspector = contour_helper.get_module_inspector("ymapsdf")

    return module_inspector.check()


@pytest.mark.freeze_time(NOW)
def test_some_fresh_targets_and_sources(contour_helper: ContourHelper):
    old_source_build = contour_helper.save(SOURCE_NAME, LONG_AGO)
    new_source_build = contour_helper.save(SOURCE_NAME, RECENTLY)

    # First build is a bit older but it has some fresh sources
    contour_helper.save(
        name="ymapsdf",
        creation_time=RECENTLY - dt.timedelta(seconds=1),
        sources=[old_source_build, new_source_build],
    )
    contour_helper.save(
        name="ymapsdf",
        creation_time=RECENTLY,
        sources=[old_source_build],
    )

    module_inspector = contour_helper.get_module_inspector("ymapsdf")

    assert not module_inspector.check()


@pytest.mark.freeze_time(NOW)
def test_sources_shipping_date_later_than_need(contour_helper: ContourHelper):
    old_source_build = contour_helper.save(SOURCE_NAME, LONG_AGO)
    new_source_build = contour_helper.save(SOURCE_NAME, RECENTLY+dt.timedelta(days=1))

    # First build is a bit older but it has some fresh sources
    contour_helper.save(
        name="ymapsdf",
        creation_time=RECENTLY - dt.timedelta(seconds=1),
        sources=[old_source_build, new_source_build],
    )
    contour_helper.save(
        name="ymapsdf",
        creation_time=RECENTLY,
        sources=[old_source_build],
    )

    module_inspector = contour_helper.get_module_inspector("ymapsdf")

    assert not module_inspector.check()


@pytest.mark.freeze_time(NOW)
def test_multiple_different_sources(contour_helper: ContourHelper):
    contour_helper.save(
        SOURCE_NAME, RECENTLY, status_string=build_defs.BuildStatusString.CANCELLED
    )
    old_source_build = contour_helper.save(SOURCE_NAME, LONG_AGO)
    new_source_build = contour_helper.save(SOURCE_NAME, RECENTLY)

    # TODO: We would like to see information about this module in the table
    #  at the bottom of the tickets message, but we have not yet figured out how to do this
    contour_helper.save(
        name="ymapsdf",
        creation_time=RECENTLY + dt.timedelta(seconds=1),
        sources=[old_source_build],
        status_string=build_defs.BuildStatusString.FAILED
    )

    ymapsdfs = [
        contour_helper.save(
            name="ymapsdf",
            creation_time=RECENTLY - dt.timedelta(seconds=1),
            sources=[old_source_build],
        ),
        contour_helper.save(
            name="ymapsdf",
            creation_time=RECENTLY,
            sources=[new_source_build],
        )
    ]
    contour_helper.save(
        name="road_graph_build",
        creation_time=RECENTLY,
        sources=ymapsdfs,
        status_string=build_defs.BuildStatusString.FAILED,
    )

    module_inspector = contour_helper.get_module_inspector("road_graph_build")
    return module_inspector.check()


@pytest.mark.freeze_time(NOW)
def test_some_fresh_targets_but_no_fresh_sources(contour_helper: ContourHelper):
    old_source_build = contour_helper.save(SOURCE_NAME, LONG_AGO)
    contour_helper.save(SOURCE_NAME, RECENTLY)

    contour_helper.save("ymapsdf", RECENTLY, [old_source_build])

    module_inspector = contour_helper.get_module_inspector("ymapsdf")

    return module_inspector.check()


@pytest.mark.freeze_time(NOW)
def test_do_not_print_new_sources_in_report(contour_helper: ContourHelper):
    old_source_build = contour_helper.save(SOURCE_NAME, LONG_AGO)
    contour_helper.save(SOURCE_NAME, RECENTLY)
    contour_helper.save(
        SOURCE_NAME, RECENTLY, status_string=build_defs.BuildStatusString.CANCELLED
    )
    old_source_build = contour_helper.save(SOURCE_NAME, LONG_AGO)
    new_source_build = contour_helper.save(SOURCE_NAME, RECENTLY)
    too_new_source = contour_helper.save(SOURCE_NAME, NOW)

    # We do not want to see it in report
    # because it does not give information about why there are no fresh builds
    # since this is too new a build and it would not have had time to be processed
    contour_helper.save(
        name="ymapsdf",
        creation_time=NOW,
        sources=[too_new_source]
    )

    ymapsdfs = [
        contour_helper.save(
            name="ymapsdf",
            creation_time=RECENTLY - dt.timedelta(seconds=1),
            sources=[old_source_build],
        ),
        contour_helper.save(
            name="ymapsdf",
            creation_time=RECENTLY,
            sources=[new_source_build],
        )
    ]
    contour_helper.save(
        name="road_graph_build",
        creation_time=RECENTLY,
        sources=ymapsdfs,
        status_string=build_defs.BuildStatusString.FAILED,
    )

    module_inspector = contour_helper.get_module_inspector("road_graph_build")
    return module_inspector.check()


@pytest.mark.freeze_time(NOW)
def test_long_path_with_failed_module_in_the_middle(contour_helper: ContourHelper):
    old_ymapsdf_src = contour_helper.save(SOURCE_NAME, LONG_AGO)
    new_ymapsdf_src = contour_helper.save(SOURCE_NAME, RECENTLY)

    # New ymapsdf has failed so road graph sources the old build
    old_ymapsdf = contour_helper.save(
        name="ymapsdf",
        creation_time=LONG_AGO,
        status_string=build_defs.BuildStatusString.COMPLETED,
        sources=[old_ymapsdf_src],
    )
    contour_helper.save(
        name="ymapsdf",
        creation_time=RECENTLY,
        status_string=build_defs.BuildStatusString.FAILED,
        sources=[new_ymapsdf_src],
    )

    road_graph_build = contour_helper.save(
        name="road_graph_build",
        creation_time=LONG_AGO,
        sources=[old_ymapsdf],
    )
    contour_helper.save(
        name="road_graph_deployment",
        creation_time=LONG_AGO,
        sources=[road_graph_build],
    )

    module_inspector = contour_helper.get_module_inspector("road_graph_deployment")

    return module_inspector.check()


@pytest.mark.freeze_time(NOW)
def test_long_path_with_canceled_module_in_the_middle(contour_helper: ContourHelper):
    old_ymapsdf_src = contour_helper.save(SOURCE_NAME, LONG_AGO)
    new_ymapsdf_src = contour_helper.save(SOURCE_NAME, RECENTLY)

    # New ymapsdf has been canceled so road graph sources the old build
    old_ymapsdf = contour_helper.save(
        name="ymapsdf",
        creation_time=LONG_AGO,
        status_string=build_defs.BuildStatusString.COMPLETED,
        sources=[old_ymapsdf_src],
    )
    contour_helper.save(
        name="ymapsdf",
        creation_time=RECENTLY,
        status_string=build_defs.BuildStatusString.CANCELLED,
        sources=[new_ymapsdf_src],
    )

    road_graph_build = contour_helper.save(
        name="road_graph_build",
        creation_time=LONG_AGO,
        sources=[old_ymapsdf],
    )
    contour_helper.save(
        name="road_graph_deployment",
        creation_time=LONG_AGO,
        sources=[road_graph_build],
    )

    module_inspector = contour_helper.get_module_inspector("road_graph_deployment")

    return module_inspector.check()


@pytest.mark.freeze_time(NOW)
def test_long_path_with_failed_module_in_the_end(contour_helper: ContourHelper):
    old_ymapsdf_src = contour_helper.save(SOURCE_NAME, LONG_AGO)
    new_ymapsdf_src = contour_helper.save(SOURCE_NAME, RECENTLY)

    old_ymapsdf = contour_helper.save("ymapsdf", LONG_AGO, [old_ymapsdf_src])
    new_ymapsdf = contour_helper.save("ymapsdf", RECENTLY, [new_ymapsdf_src])

    old_road_graph_build = contour_helper.save("road_graph_build", LONG_AGO, [old_ymapsdf])
    new_road_graph_build = contour_helper.save("road_graph_build", RECENTLY, [new_ymapsdf])

    contour_helper.save(
        name="road_graph_deployment",
        creation_time=LONG_AGO,
        sources=[old_road_graph_build],
        status_string=build_defs.BuildStatusString.COMPLETED,
    )
    contour_helper.save(
        name="road_graph_deployment",
        creation_time=RECENTLY,
        sources=[new_road_graph_build],
        status_string=build_defs.BuildStatusString.FAILED,
        more_extras={
            "region": "cis1",
            "release_name": "55-55-55",
        }
    )

    module_inspector = contour_helper.get_module_inspector("road_graph_deployment")

    return module_inspector.check()


@pytest.mark.freeze_time(NOW)
def test_broken_path(contour_helper: ContourHelper):
    old_source_build = contour_helper.save(SOURCE_NAME, LONG_AGO)
    contour_helper.save(SOURCE_NAME, RECENTLY)
    contour_helper.save("ymapsdf", LONG_AGO, sources=[old_source_build])

    # Emulate bad module traits: e.g. someone removed autostarter section by mistake
    contour_helper.flow_graphs[0].remove_edge(SOURCE_NAME, "ymapsdf")

    module_inspector = contour_helper.get_module_inspector("ymapsdf")

    return module_inspector.check()


@pytest.mark.freeze_time(NOW)
def test_none_tracked_ancestors_path(contour_helper: ContourHelper):
    old_source_build = contour_helper.save(SOURCE_NAME, LONG_AGO)
    contour_helper.save(SOURCE_NAME, RECENTLY)
    build = contour_helper.save("ymapsdf", LONG_AGO, sources=[old_source_build])

    # Some module that has not tracked ancestors
    build.tracked_ancestor_builds = None

    return contour_helper.get_module_inspector("ymapsdf").check()
