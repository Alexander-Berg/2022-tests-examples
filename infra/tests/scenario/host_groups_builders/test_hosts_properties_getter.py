from walle.hosts import Host
from walle.scenario.host_groups_builders.hosts_properties_getter import HostsPropertiesGetter, ProjectProperties
from walle.util.mongo import MongoDocument


class TestHostsPropertiesGetter:

    _HOSTS_INVS = [1, 2, 3, 4]
    _HOSTS_MOCK_DATA = [
        {"inv": 1, "project": "project-foo"},
        {"inv": 2, "project": "project-bar"},
        {"inv": 3, "project": "project-baz"},
        {"inv": 4, "project": "project-baz"},
    ]
    _PROJECTS_MOCK_DATA = [
        {"id": "project-foo", "tags": ["foo-1", "foo-2"], "bot_project_id": 1, "maintenance_plot_id": "plot-foo"},
        {"id": "project-bar", "tags": ["bar"], "bot_project_id": 2, "maintenance_plot_id": "plot-bar"},
        {"id": "project-baz", "tags": ["baz"], "bot_project_id": 1, "maintenance_plot_id": "plot-foo"},
    ]

    def test_get_projects_properties(self, walle_test):
        for host_mock_data in self._HOSTS_MOCK_DATA:
            walle_test.mock_host(host_mock_data)

        for project_mock_data in self._PROJECTS_MOCK_DATA:
            walle_test.mock_project(project_mock_data)

        expected_result = {
            1: ProjectProperties(tags=["foo-1", "foo-2"], bot_project_id=1, maintenance_plot_id="plot-foo"),
            2: ProjectProperties(tags=["bar"], bot_project_id=2, maintenance_plot_id="plot-bar"),
            3: ProjectProperties(tags=["baz"], bot_project_id=1, maintenance_plot_id="plot-foo"),
            4: ProjectProperties(tags=["baz"], bot_project_id=1, maintenance_plot_id="plot-foo"),
        }
        assert expected_result == HostsPropertiesGetter.get_projects_properties(self._HOSTS_INVS)

    def test_get_hosts_with_project(self, walle_test):
        for host_mock_data in self._HOSTS_MOCK_DATA:
            walle_test.mock_host(host_mock_data)

        expected_result = [
            MongoDocument.for_model(Host)(data=dict(inv=1, project="project-foo")),
            MongoDocument.for_model(Host)(data=dict(inv=2, project="project-bar")),
            MongoDocument.for_model(Host)(data=dict(inv=3, project="project-baz")),
            MongoDocument.for_model(Host)(data=dict(inv=4, project="project-baz")),
        ]

        actual_result = HostsPropertiesGetter._get_hosts_with_project(self._HOSTS_INVS)

        # Check equality manually.
        for expected_item in expected_result:
            expected_item_found = False
            for actual_item in actual_result:
                if actual_item.inv == expected_item.inv and actual_item.project == expected_item.project:
                    expected_item_found = True
                    actual_result.remove(actual_item)
                    break
            if not expected_item_found:
                raise AssertionError(
                    "Expected item not found in result: inv={} project={}".format(
                        expected_item.inv, expected_item.project
                    )
                )

        # Check that there are no unexpected items in result.
        assert not actual_result

    def test_get_projects_fields_values(self, walle_test):
        for project_mock_data in self._PROJECTS_MOCK_DATA:
            walle_test.mock_project(project_mock_data)

        expected_result = {
            "project-foo": ProjectProperties(tags=["foo-1", "foo-2"], bot_project_id=1, maintenance_plot_id="plot-foo"),
            "project-bar": ProjectProperties(tags=["bar"], bot_project_id=2, maintenance_plot_id="plot-bar"),
            "project-baz": ProjectProperties(tags=["baz"], bot_project_id=1, maintenance_plot_id="plot-foo"),
        }

        projects = {"project-foo", "project-bar", "project-baz"}
        assert expected_result == HostsPropertiesGetter._get_projects_fields_values(projects)
