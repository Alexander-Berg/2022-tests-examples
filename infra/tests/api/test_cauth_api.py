import six

from walle.clients import cauth, staff
from walle.hosts import HostState
from walle.projects import CauthSettingsDocument


class TestProjectsToHosts:
    def test_does_not_return_free_hosts(self, walle_test):
        self.mock_project_w_hosts(walle_test, proj_id=1, num_hosts=4, num_free_hosts=1)
        resp = walle_test.api_client.get("/v1/cauth/projects-to-hosts")
        projects_to_hosts = self.parse_response(resp.data)
        assert "project-1" in projects_to_hosts
        # project-1-3 is FREE and must be omitted
        assert projects_to_hosts["project-1"] == {"project-1-0", "project-1-1", "project-1-2"}

    def test_returns_empty_projects(self, walle_test):
        self.mock_project_w_hosts(walle_test, proj_id=1, num_hosts=0, num_free_hosts=0)
        resp = walle_test.api_client.get("/v1/cauth/projects-to-hosts")
        projects_to_hosts = self.parse_response(resp.data)
        assert projects_to_hosts["mocked-default-project"] == set()

    def test_multiple_projects(self, walle_test):
        self.mock_project_w_hosts(walle_test, proj_id=1, num_hosts=2, num_free_hosts=1)
        self.mock_project_w_hosts(walle_test, proj_id=2, num_hosts=2, num_free_hosts=0)
        resp = walle_test.api_client.get("/v1/cauth/projects-to-hosts")
        projects_to_hosts = self.parse_response(resp.data)
        assert projects_to_hosts == {
            "mocked-default-project": set(),
            "project-1": {"project-1-0"},
            "project-2": {"project-2-0", "project-2-1"},
        }

    def mock_project_w_hosts(self, walle_test, proj_id, num_hosts, num_free_hosts=0):
        project = "project-{}".format(proj_id)
        walle_test.mock_project({"id": project})

        for host_idx in range(num_hosts):
            host_state = HostState.ASSIGNED if host_idx < num_hosts - num_free_hosts else HostState.FREE
            walle_test.mock_host(
                {
                    "inv": 10 * proj_id + host_idx,
                    "name": "{}-{}".format(project, host_idx),
                    "project": project,
                    "state": host_state,
                }
            )

    def parse_response(self, data):
        lines = six.ensure_str(data).split("\n")
        projects_to_hosts = {}
        for line in lines:
            if line:
                project, hosts = line.split(":")
                projects_to_hosts[project] = set(hosts.split(",")) if hosts else set()
        return projects_to_hosts


class TestProjectsToResponsibles:
    def test_project_to_responsibles(self, walle_test, mp):
        mp.function(
            staff.batch_get_groups_members, return_value={"@group1": ["g1_user1", "g1_user2"], "@group2_empty": []}
        )

        walle_test.mock_project({"id": "proj1", "owners": sorted(["user1", "@group1"])})
        walle_test.mock_project({"id": "proj2", "owners": sorted(["@group1", "@group2_empty"])})

        expected = {
            "mocked-default-project": ["mocked-user"],
            "proj1": sorted(["user1", "g1_user1", "g1_user2"]),
            "proj2": sorted(["g1_user1", "g1_user2"]),
        }
        resp = walle_test.api_client.get("/v1/cauth/projects-to-responsibles")
        assert resp.json == expected


class TestProjectsCauthSettings:
    def test_projects_cauth_settings(self, walle_test):
        cauth_settings = CauthSettingsDocument(
            flow_type=cauth.CauthFlowType.BACKEND_SOURCES,
            trusted_sources=[cauth.CauthSource.WALLE, cauth.CauthSource.IDM],
        )
        walle_test.mock_project({"id": "proj1"})
        walle_test.mock_project({"id": "proj2", "cauth_settings": cauth_settings})

        expected = {
            "mocked-default-project": {},
            "proj1": {},
            "proj2": {
                "flow": "backend_sources",
                "trusted_sources": "walle,idm",
                "key_sources": "staff",
            },
        }
        resp = walle_test.api_client.get("/v1/cauth/projects-cauth-settings")
        assert resp.json == expected
