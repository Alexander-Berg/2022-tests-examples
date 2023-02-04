import pytest

from infra.walle.server.tests.lib.util import monkeypatch_method
from walle.scenario.host_groups_builders.base import (
    HostGroup,
    SpecificProjectTagHostGroupSource,
    MaintenancePlotHostGroupSource,
    BotProjectIdHostGroupSource,
)
from walle.scenario.host_groups_builders.hosts_list_splitters import DefaultHostsListSplitter
from walle.scenario.host_groups_builders.hosts_properties_getter import ProjectProperties


class TestItdcMaintenanceHostsListSplitter:
    @pytest.mark.usefixtures("mock_get_abc_project_slug_from_bot_project_id")
    @pytest.mark.parametrize(
        "hosts_project_properties_map, expected_result",
        [
            (
                {
                    1: ProjectProperties(tags=["foo"], bot_project_id=1, maintenance_plot_id="plot-foo"),
                    2: ProjectProperties(tags=["bar"], bot_project_id=1, maintenance_plot_id="plot-bar"),
                    3: ProjectProperties(tags=["baz"], bot_project_id=2, maintenance_plot_id="plot-foo"),
                    4: ProjectProperties(tags=["baz"], bot_project_id=3, maintenance_plot_id="plot-foo"),
                },
                [
                    HostGroup(
                        group_source=MaintenancePlotHostGroupSource(maintenance_plot_id="plot-foo"),
                        hosts_invs=[1, 3, 4],
                    ),
                    HostGroup(
                        group_source=MaintenancePlotHostGroupSource(maintenance_plot_id="plot-bar"), hosts_invs=[2]
                    ),
                ],
            ),
            (
                {
                    1: ProjectProperties(tags=["yt"], bot_project_id=1, maintenance_plot_id=None),
                    2: ProjectProperties(tags=["yp"], bot_project_id=1, maintenance_plot_id=None),
                    3: ProjectProperties(tags=["yabs"], bot_project_id=2, maintenance_plot_id=None),
                    4: ProjectProperties(
                        tags=["rtc.reboot_segment-yt_masters"], bot_project_id=3, maintenance_plot_id=None
                    ),
                },
                [
                    HostGroup(
                        group_source=SpecificProjectTagHostGroupSource(specific_project_tag="yt"), hosts_invs=[1]
                    ),
                    HostGroup(
                        group_source=SpecificProjectTagHostGroupSource(specific_project_tag="yp"), hosts_invs=[2]
                    ),
                    HostGroup(
                        group_source=SpecificProjectTagHostGroupSource(specific_project_tag="yabs"), hosts_invs=[3]
                    ),
                    HostGroup(
                        group_source=SpecificProjectTagHostGroupSource(
                            specific_project_tag="rtc.reboot_segment-yt_masters"
                        ),
                        hosts_invs=[4],
                    ),
                ],
            ),
            (
                {
                    1: ProjectProperties(tags=["foo"], bot_project_id=1, maintenance_plot_id=None),
                    2: ProjectProperties(tags=["bar"], bot_project_id=1, maintenance_plot_id=None),
                    3: ProjectProperties(tags=["baz"], bot_project_id=2, maintenance_plot_id=None),
                    4: ProjectProperties(tags=["baz"], bot_project_id=3, maintenance_plot_id=None),
                },
                [
                    HostGroup(
                        group_source=BotProjectIdHostGroupSource(bot_project_id=1, abc_service_slug="1"),
                        hosts_invs=[1, 2],
                    ),
                    HostGroup(
                        group_source=BotProjectIdHostGroupSource(bot_project_id=2, abc_service_slug="2"), hosts_invs=[3]
                    ),
                    HostGroup(
                        group_source=BotProjectIdHostGroupSource(bot_project_id=3, abc_service_slug="3"), hosts_invs=[4]
                    ),
                ],
            ),
            (
                {
                    1: ProjectProperties(tags=["yt"], bot_project_id=1, maintenance_plot_id="yt-maintenance-plot"),
                    2: ProjectProperties(tags=["yt"], bot_project_id=2, maintenance_plot_id=None),
                    3: ProjectProperties(tags=["foo"], bot_project_id=3, maintenance_plot_id=None),
                    4: ProjectProperties(tags=["bar"], bot_project_id=3, maintenance_plot_id=None),
                    5: ProjectProperties(
                        tags=["rtc.reboot_segment-yt_masters"], bot_project_id=3, maintenance_plot_id=None
                    ),
                    6: ProjectProperties(tags=["yabs"], bot_project_id=4, maintenance_plot_id="some-maintenance-plot"),
                    7: ProjectProperties(tags=["yabs"], bot_project_id=5, maintenance_plot_id=None),
                    8: ProjectProperties(tags=["yt", "yp"], bot_project_id=6, maintenance_plot_id=None),
                },
                [
                    HostGroup(
                        group_source=MaintenancePlotHostGroupSource(maintenance_plot_id="yt-maintenance-plot"),
                        hosts_invs=[1],
                    ),
                    HostGroup(
                        group_source=MaintenancePlotHostGroupSource(maintenance_plot_id="some-maintenance-plot"),
                        hosts_invs=[6],
                    ),
                    HostGroup(
                        group_source=SpecificProjectTagHostGroupSource(specific_project_tag="yt"), hosts_invs=[2, 8]
                    ),
                    HostGroup(
                        group_source=SpecificProjectTagHostGroupSource(
                            specific_project_tag="rtc.reboot_segment-yt_masters"
                        ),
                        hosts_invs=[5],
                    ),
                    HostGroup(
                        group_source=SpecificProjectTagHostGroupSource(specific_project_tag="yabs"), hosts_invs=[7]
                    ),
                    HostGroup(
                        group_source=BotProjectIdHostGroupSource(bot_project_id=3, abc_service_slug="3"),
                        hosts_invs=[3, 4],
                    ),
                ],
            ),
        ],
    )
    def test_split(self, mp, hosts_project_properties_map, expected_result):
        monkeypatch_method(
            mp,
            method=DefaultHostsListSplitter._get_hosts_project_properties_map,
            obj=DefaultHostsListSplitter,
            return_value=hosts_project_properties_map,
        )

        hosts_invs = [inv for inv in hosts_project_properties_map.keys()]
        assert expected_result == DefaultHostsListSplitter.split(hosts_invs)
