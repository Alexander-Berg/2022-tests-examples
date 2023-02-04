import operator

from walle.scenario.constants import HostGroupsBuildersSpecificProjectTags
from walle.scenario.host_groups_builders.base import HostGroup, SpecificProjectTagHostGroupSource
from walle.scenario.host_groups_builders.by_specific_project_tag import build_groups_by_specific_project_tag
from walle.scenario.host_groups_builders.hosts_properties_getter import ProjectProperties


def _get_project_properties(tags):
    return ProjectProperties.from_dict(dict(tags=tags, bot_project_id=42, maintenance_plot_id="does-not-matter"))


class TestBuildGroupsBySpecificProjectTag:

    _HOSTS_PROJECT_PROPERTIES_MAP = {
        1: _get_project_properties(["yt", "some-yt-tag"]),
        2: _get_project_properties(["yt", "another-yt-tag"]),
        3: _get_project_properties(["yt", "another-yt-tag"]),
        4: _get_project_properties(["yp", "some-yp-tag"]),
        5: _get_project_properties(["yabs", "yabs-foo", "yabs-bar"]),
        6: _get_project_properties(["yabs", "yabs-foo", "yabs-bar"]),
        7: _get_project_properties(["other-foo"]),
        8: _get_project_properties(["other-foo"]),
        9: _get_project_properties(["other-foo"]),
        10: _get_project_properties(None),
        11: _get_project_properties(None),
        12: _get_project_properties(None),
        13: _get_project_properties(["yp", "yt", "some-other-tags"]),
        14: _get_project_properties(["yp", "yt", "some-other-tag", "rtc.reboot_segment-yt_masters"]),
    }

    def test_build_groups(self):
        expected_groups = [
            HostGroup(
                group_source=SpecificProjectTagHostGroupSource(
                    specific_project_tag=HostGroupsBuildersSpecificProjectTags.YT_MASTERS_PROJECT_TAG,
                ),
                hosts_invs=[14],
            ),
            HostGroup(
                group_source=SpecificProjectTagHostGroupSource(
                    specific_project_tag=HostGroupsBuildersSpecificProjectTags.YT_PROJECT_TAG,
                ),
                hosts_invs=[1, 2, 3, 13],
            ),
            HostGroup(
                group_source=SpecificProjectTagHostGroupSource(
                    specific_project_tag=HostGroupsBuildersSpecificProjectTags.YP_PROJECT_TAG,
                ),
                hosts_invs=[4],
            ),
            HostGroup(
                group_source=SpecificProjectTagHostGroupSource(
                    specific_project_tag=HostGroupsBuildersSpecificProjectTags.YABS_PROJECT_TAG,
                ),
                hosts_invs=[5, 6],
            ),
        ]
        expected_hosts_without_specific_project_tags = [7, 8, 9, 10, 11, 12]

        actual_groups, actual_hosts_without_specific_project_tags = build_groups_by_specific_project_tag(
            self._HOSTS_PROJECT_PROPERTIES_MAP
        )

        assert sorted(expected_groups, key=operator.attrgetter("group_source.specific_project_tag")) == sorted(
            actual_groups, key=operator.attrgetter("group_source.specific_project_tag")
        )
        assert sorted(expected_hosts_without_specific_project_tags) == sorted(
            actual_hosts_without_specific_project_tags
        )
