import json
from unittest import mock

import pytest

from infra.walle.server.tests.lib.idm_util import make_role_dict
from walle.clients import idm
from walle.clients.idm import IDMRole
from walle.idm import traversal, project_push

GROUP_ID = 123654


@pytest.fixture()
def mock_config(mp):
    mp.config("idm.system_name", "test")
    mp.config("cauth.key_path", "/tmp/key.key")


@pytest.fixture()
def br():
    return idm.BatchRequest()


def mock_node(slug_, idm_props):
    class Node:
        slug = slug_

        def get_idm_properties(self):
            return idm_props

    return Node()


def mock_iter_subtree_nodes(mp, nodes):
    def iter_nodes_mock(path):
        cur_path = path
        for node in nodes:
            cur_path.append(node.slug)
            yield cur_path, node

    iter_nodes_mock = mp.function(traversal.iter_subtree_nodes, side_effect=iter_nodes_mock)
    return iter_nodes_mock


class TestAddProjectNodesToIDM:
    @pytest.fixture()
    def mocked_nodes(self, mp):
        nodes = [mock_node("first", {}), mock_node("second", {})]
        mock_iter_subtree_nodes(mp, nodes)
        return nodes

    def test_push_project_nodes_to_idm(self, mp, br, walle_test, mocked_nodes):
        add_role_node_mock = mp.method(idm.BatchRequest.add_role_node, obj=idm.BatchRequest)
        project = walle_test.mock_project({"id": "project-id"})

        project_push.add_project_role_tree_nodes(br, project)

        assert add_role_node_mock.call_count == len(mocked_nodes)
        base_path = ["scopes", "project", "project", "project-id"]
        assert add_role_node_mock.call_args_list[0] == mock.call(br, base_path, {"slug": "first"})
        assert add_role_node_mock.call_args_list[1] == mock.call(br, base_path + ["first"], {"slug": "second"})


class TestDeleteProjectNodesFromIDM:
    def test_delete_project_node_from_idm(self, mp, br, walle_test):
        remove_role_node_mock = mp.method(idm.BatchRequest.remove_role_node, obj=idm.BatchRequest)

        project = walle_test.mock_project({"id": "project-id"})

        project_push.delete_project_role_tree_nodes(br, project)

        assert remove_role_node_mock.called
        project_path = ["scopes", "project", "project", "project-id"]
        assert remove_role_node_mock.call_args_list[0] == mock.call(br, project_path)


class TestRevokeProjectRoles:
    @pytest.fixture()
    def roles(self):
        return [
            make_role_dict(1111, "granted", "blabus"),
            make_role_dict(2222, "granted", "@svc_group"),
            make_role_dict(3333, "requested", "blubur"),
        ]

    @pytest.fixture()
    def iter_roles_mock(self, mp, roles):
        def iter_roles(path_prefix=None, system=None, type=None):
            for role_dict in roles:
                if IDMRole(role_dict).type == type:
                    yield role_dict

        iter_roles_mock = mp.function(idm.iter_role_dicts, side_effect=iter_roles)
        return iter_roles_mock

    def test_revoke_project_roles(self, mp, br, walle_test, roles, iter_roles_mock):
        revoke_role_mock = mp.method(idm.BatchRequest.revoke_role, obj=idm.BatchRequest)
        project = walle_test.mock_project({"id": "project-id"})

        project_push.revoke_project_roles(br, project)

        assert iter_roles_mock.called
        assert revoke_role_mock.call_count == len(roles)
        expected_role_ids = {role["id"] for role in roles}
        revoked_role_ids = {call[0][1] for call in revoke_role_mock.call_args_list}
        assert revoked_role_ids == expected_role_ids


def check_role_request(subreq, project_id, requester=None, user=None, group=None):
    assert subreq["path"] == "/rolerequests/"
    body = json.loads(subreq["data"])

    assert body["path"] == "/project/{}/owner/".format(project_id)

    if user is not None:
        assert body["user"] == user
    elif group is not None:
        assert body["group"] == group

    if requester is not None:
        assert body["_requester"] == requester
