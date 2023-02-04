import operator

import pytest

from walle.scenario.host_groups_builders import by_bot_project_id
from walle.scenario.host_groups_builders.base import HostGroup, BotProjectIdHostGroupSource
from walle.scenario.host_groups_builders.hosts_properties_getter import ProjectProperties


def _get_project_properties(bot_project_id):
    return ProjectProperties.from_dict(
        dict(tags=["does-not-matter"], bot_project_id=bot_project_id, maintenance_plot_id="does-not-matter")
    )


class TestBuildGroupsByBotProjectId:

    _HOSTS_PROJECT_PROPERTIES_MAP = {
        1: _get_project_properties(100),
        2: _get_project_properties(100),
        3: _get_project_properties(200),
        4: _get_project_properties(None),
        5: _get_project_properties(None),
        6: _get_project_properties(300),
    }

    @pytest.mark.usefixtures("mock_get_abc_project_slug_from_bot_project_id")
    def test_build_groups(self):
        expected_groups = [
            HostGroup(
                group_source=BotProjectIdHostGroupSource(bot_project_id=100, abc_service_slug="100"), hosts_invs=[1, 2]
            ),
            HostGroup(
                group_source=BotProjectIdHostGroupSource(bot_project_id=200, abc_service_slug="200"), hosts_invs=[3]
            ),
            HostGroup(
                group_source=BotProjectIdHostGroupSource(bot_project_id=300, abc_service_slug="300"), hosts_invs=[6]
            ),
        ]
        expected_hosts_without_bot_broject_id = [4, 5]

        actual_groups, actual_hosts_without_bot_broject_id = by_bot_project_id.build_groups_by_bot_project_id(
            self._HOSTS_PROJECT_PROPERTIES_MAP
        )

        assert sorted(expected_groups, key=operator.attrgetter("group_source.bot_project_id")) == sorted(
            actual_groups, key=operator.attrgetter("group_source.bot_project_id")
        )
        assert sorted(expected_hosts_without_bot_broject_id) == sorted(actual_hosts_without_bot_broject_id)
