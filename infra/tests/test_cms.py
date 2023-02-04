"""Tests CMS maintenance."""

import itertools
from collections import defaultdict
from functools import partial
from unittest.mock import call, Mock, PropertyMock

import pytest

from infra.walle.server.tests.lib.util import mock_task, patch_attr, patch
from walle import cms
from walle.clients.cms import CmsApiVersion
from walle.hosts import HostState, HostStatus
from walle.projects import Project


class TestGcCms:
    @pytest.fixture()
    def test_data(self, walle_test, monkeypatch_timestamp):
        self.test = walle_test
        self.cms_tasks = defaultdict(list)

        walle_test.mock_project({"id": "project-1"})
        walle_test.mock_project({"id": "project-2"})
        walle_test.mock_project(
            {
                "id": "project-3",
                "cms": "http://localhost.localdomain/",
                "cms_api_version": CmsApiVersion.V1_0,
                "cms_settings": [{"cms": "http://localhost.localdomain/", "cms_api_version": CmsApiVersion.V1_0}],
            }
        )
        walle_test.mock_project(
            {
                "id": "project-4",
                "cms": "http://localhost.localdomain/",
                "cms_api_version": CmsApiVersion.V1_0,
                "cms_settings": [{"cms": "http://localhost.localdomain/", "cms_api_version": CmsApiVersion.V1_0}],
            }
        )
        walle_test.mock_project(
            {
                "id": "project-5",
                "cms": "http://subdomain.localhost.localdomain/",
                "cms_api_version": CmsApiVersion.V1_0,
                "cms_settings": [
                    {"cms": "http://subdomain.localhost.localdomain/", "cms_api_version": CmsApiVersion.V1_0}
                ],
            }
        )
        walle_test.mock_project(
            {
                "id": "project-6",
                "cms_settings": [
                    {"cms": "http://subdomain.localhost.localdomain/v1/", "cms_api_version": CmsApiVersion.V1_3},
                    {"cms": "http://subdomain.localhost.localdomain/v2/", "cms_api_version": CmsApiVersion.V1_4},
                ],
            }
        )

        # add active tasks
        n_projects = 7
        next_inv = partial(next, itertools.count())
        for status in HostStatus.ALL_TASK * n_projects:
            inv = next_inv()
            project_id = "project-{}".format(inv % n_projects + 1)
            host = walle_test.mock_host(
                {
                    "inv": inv,
                    "state": HostState.ASSIGNED,
                    "status": status,
                    "project": project_id,
                    "task": mock_task(task_id=inv + 100),
                }
            )

            if inv != 1:
                self.cms_tasks[project_id].append(host.task.get_cms_task_id())

        for i in range(n_projects):
            inv = next_inv()
            project_id = "project-{}".format(i % n_projects + 1)
            host = walle_test.mock_host(
                {
                    "inv": inv,
                    "state": HostState.MAINTENANCE,
                    "status": HostStatus.default(HostState.MAINTENANCE),
                    "project": project_id,
                    "cms_task_id": str(inv + 200),
                }
            )
            self.cms_tasks[project_id].append(host.cms_task_id)

        # add staled tasks
        self.cms_tasks["project-1"].append("staled-task-id-p1")
        self.cms_tasks["project-2"].append("staled-task-id-p2")
        self.cms_tasks["project-4"].append("staled-task-id-p4")
        self.cms_tasks["project-5"].append("staled-task-id-p5")
        self.cms_tasks["project-6"].append("staled-task-id-p6")

    @pytest.mark.usefixtures("test_data")
    def test_gc(self, monkeypatch):
        cms_clients = Mock()
        get_cms_orig = Project.get_cms_clients

        def get_cms_mock(project):
            clients = []
            for client in get_cms_orig(project):
                # return mock instead of a real client, but only when real client would be returned
                client_mock = Mock()
                name = PropertyMock(return_value=client.name)
                type(client_mock).name = name
                client_mock.get_tasks.return_value = [{"id": task_id} for task_id in self.cms_tasks.get(project.id, [])]
                cms_clients.attach_mock(client_mock, project.id.replace("-", "_"))
                clients.append(client_mock)
            return clients

        patch_attr(monkeypatch, Project, "get_cms_clients", side_effect=get_cms_mock)

        with patch("walle.cms.CmsNamespace"):
            cms._gc_cms()

        # there are 7 projects, three with default cms (three different calls), two with one common cms (one call)
        # one with it's own cms, and one with YP (should not be called)
        assert sorted(cms_clients.mock_calls) == sorted(
            [
                call.mocked_default_project.get_tasks(),
                call.project_1.get_tasks(),
                call.project_2.get_tasks(),
                call.project_4.get_tasks(),
                call.project_5.get_tasks(),
                call.project_6.get_tasks(),
                call.project_6.get_tasks(),
                call.project_1.delete_task("staled-task-id-p1"),
                call.project_2.delete_task("staled-task-id-p2"),
                call.project_4.delete_task("staled-task-id-p4"),
                call.project_5.delete_task("staled-task-id-p5"),
                call.project_6.delete_task("staled-task-id-p6"),
                call.project_6.delete_task("staled-task-id-p6"),
            ]
        )
        self.test.hosts.assert_equal()

    @pytest.mark.usefixtures("test_data")
    def test_gc_skips_foreign_namespaces(self, monkeypatch):
        cms_clients = Mock()
        get_cms_orig = Project.get_cms_clients

        def get_cms_mock(project):
            clients = []
            for client in get_cms_orig(project):
                # return mock instead of a real client, but only when real client would be returned
                client_mock = Mock()
                name = PropertyMock(return_value=client.name)
                type(client_mock).name = name
                client_mock.get_tasks.return_value = [{"id": task_id} for task_id in self.cms_tasks.get(project.id, [])]
                cms_clients.attach_mock(client_mock, project.id.replace("-", "_"))
                clients.append(client_mock)
            return clients

        patch_attr(monkeypatch, Project, "get_cms_clients", side_effect=get_cms_mock)

        cms._gc_cms()

        # test that GC doesn't delete "staled-task-id" because it has unknown namespace
        # there are 7 projects, three with default cms (three different calls), two with one common cms (one call)
        # one with it's own cms, and one with YP (should not be called)
        assert sorted(cms_clients.mock_calls) == sorted(
            [
                call.mocked_default_project.get_tasks(),
                call.project_1.get_tasks(),
                call.project_2.get_tasks(),
                call.project_4.get_tasks(),
                call.project_5.get_tasks(),
                call.project_6.get_tasks(),
                call.project_6.get_tasks(),
            ]
        )
        self.test.hosts.assert_equal()

    @pytest.mark.usefixtures("test_data")
    def test_gc_with_cms_error(self, monkeypatch):
        get_cms_orig = Project.get_cms_clients

        cms_client = Mock()
        cms_client.get_tasks.side_effect = Exception("Mocked error")

        def get_cms_mock(project):
            if get_cms_orig(project):
                # return mock instead of a real client, but only when real client would be returned
                return [cms_client] * len(get_cms_orig(project))

        patch_attr(monkeypatch, Project, "get_cms_clients", side_effect=get_cms_mock)

        with patch("walle.cms.CmsNamespace"):
            cms._gc_cms()

        # there are 7 projects, three with default cms (three different calls), two with one common cms (one call)
        # one with it's own cms, and one with two cmses (two calls, each one to different cms)
        assert cms_client.mock_calls == [call.get_tasks()]
        self.test.hosts.assert_equal()
