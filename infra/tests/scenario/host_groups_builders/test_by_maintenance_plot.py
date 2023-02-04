import operator

from walle.scenario.host_groups_builders.base import HostGroup, MaintenancePlotHostGroupSource
from walle.scenario.host_groups_builders.by_maintenance_plot import build_groups_by_maintenance_plot
from walle.scenario.host_groups_builders.hosts_properties_getter import ProjectProperties


def _get_project_properties(maintenance_plot_id):
    return ProjectProperties.from_dict(
        dict(tags=["does-not-matter"], bot_project_id=42, maintenance_plot_id=maintenance_plot_id)
    )


class TestBuildGroupsByMaintenancePlot:

    _HOSTS_PROJECT_PROPERTIES_MAP = {
        1: _get_project_properties("plot-foo"),
        2: _get_project_properties("plot-foo"),
        3: _get_project_properties("plot-bar"),
        4: _get_project_properties(None),
        5: _get_project_properties(None),
        6: _get_project_properties("plot-baz"),
    }

    def test_build_groups(self):
        expected_groups = [
            HostGroup(group_source=MaintenancePlotHostGroupSource(maintenance_plot_id="plot-foo"), hosts_invs=[1, 2]),
            HostGroup(group_source=MaintenancePlotHostGroupSource(maintenance_plot_id="plot-bar"), hosts_invs=[3]),
            HostGroup(group_source=MaintenancePlotHostGroupSource(maintenance_plot_id="plot-baz"), hosts_invs=[6]),
        ]
        expected_hosts_without_maintenance_plot = [4, 5]

        actual_groups, actual_hosts_without_maintenance_plot = build_groups_by_maintenance_plot(
            self._HOSTS_PROJECT_PROPERTIES_MAP
        )

        assert sorted(expected_groups, key=operator.attrgetter("group_source.maintenance_plot_id")) == sorted(
            actual_groups, key=operator.attrgetter("group_source.maintenance_plot_id")
        )
        assert sorted(expected_hosts_without_maintenance_plot) == sorted(actual_hosts_without_maintenance_plot)
