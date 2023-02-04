import json

import pytest
import six
import http.client

from infra.walle.server.tests.lib.util import mock_response
from walle.clients import idm, cauth, utils, juggler, staff

API_URL = "idm-api-url.yandex-team.ru"
SYSTEM_NAME = "walle-test"
TOKEN = "token-mock"
CERT_PATH = "/some/path.cert"
KEY_PATH = "/some/path.key"

GROUP_ID = 123654
ROLE_ID = 444444


@pytest.fixture()
def mock_config(mp):
    mp.config("idm.api_url", API_URL)
    mp.config("idm.system_name", SYSTEM_NAME)
    mp.config("idm.access_token", TOKEN)
    mp.config("cauth.cert_path", CERT_PATH)
    mp.config("cauth.key_path", KEY_PATH)


@pytest.fixture(autouse=True)
def mock_check_certs_exist(mp):
    return mp.function(utils.check_certs_exist, module=cauth)


def check_request_args(req_mock, method, url, data=None, call_idx=0):
    call_args = req_mock.call_args_list[call_idx]
    url = "https://{}/api/v1/{}".format(API_URL, url)
    assert call_args[0] == (method, url)
    assert call_args[1]["headers"]["Authorization"] == "OAuth {}".format(TOKEN)
    assert call_args[1]["cert"] == (CERT_PATH, KEY_PATH)
    if data is not None:
        assert json.loads(call_args[1]["data"]) == data


@pytest.mark.usefixtures("mock_config")
class TestBatchRequest:
    @pytest.fixture()
    def br(self):
        return idm.BatchRequest()

    def test_add_role_node(self, br):
        node_path = ["scopes", "project", "project", "test2"]
        br.add_role_node(node_path, dict(slug="user", name="user"))

        expected_data = {
            "system": "walle-test",
            "name": "user",
            "parent": "/scopes/project/project/test2/",
            "slug": "user",
        }
        self._check_subrequest(br, "POST", "/rolenodes/", {http.client.CREATED}, data=expected_data)

    def test_remove_role_node(self, br):
        node_path = ["scopes", "project", "project", "test2"]

        br.remove_role_node(node_path)

        self._check_subrequest(
            br, "DELETE", "/rolenodes/{}/scopes/project/project/test2/".format(SYSTEM_NAME), {http.client.NO_CONTENT}
        )

    @pytest.mark.parametrize(
        "member, requested_member",
        [({"user": "blabus"}, {"user": "blabus"}), ({"group": "@svc_plumbus_works"}, {"group": GROUP_ID})],
    )
    def test_request_role(self, mp, br, member, requested_member):
        mp.function(staff.get_group_id, return_value=GROUP_ID)

        node_path = ["scopes", "project", "project", "plumbus"]
        br.request_role(node_path, **member)

        expected_data = dict(requested_member, system=SYSTEM_NAME, path="/project/plumbus/")
        self._check_subrequest(
            br, "POST", "/rolerequests/", {http.client.CREATED, http.client.CONFLICT}, data=expected_data
        )

    def test_revoke_role(self, br):
        br.revoke_role(ROLE_ID)
        self._check_subrequest(br, "DELETE", "/roles/{}/".format(ROLE_ID), {http.client.NO_CONTENT})

    def test_execute(self, mp, br):
        br.revoke_role(ROLE_ID)
        br.request_role(["scopes", "project", "project", "plumbus"], user="blumbus")

        batch_resp = {"responses": [{"body": None, "headers": {}, "status_code": http.client.OK}]}
        request_mock = mp.request(mock_response(batch_resp))

        br.execute()
        check_request_args(request_mock, "POST", "batch/")

        subreqs = json.loads(request_mock.call_args[1]["data"])
        assert len(subreqs) == 2
        for idx, subreq in enumerate(subreqs):
            assert all(field in subreq for field in ["id", "method", "path"])
            assert subreq["id"] == str(idx)

    def test_execute_extracts_errors(self, mp, br):
        br.revoke_role(ROLE_ID)
        br.request_role(["scopes", "project", "project", "plumbus"], user="blumbus")

        normal_msg = "Everything is ok"
        error_msg = "Часть сообщения зачем-то записана in Russian"
        batch_resp = {
            "responses": [
                {"id": "0", "body": {"message": normal_msg}, "headers": {}, "status_code": http.client.NO_CONTENT},
                {"id": "1", "body": {"message": error_msg}, "headers": {}, "status_code": http.client.BAD_REQUEST},
            ]
        }
        mp.request(mock_response(batch_resp, status_code=http.client.BAD_REQUEST))

        with pytest.raises(idm.IDMBatchRequestError) as exc_info:
            br.execute()
        assert six.ensure_str(error_msg, "utf-8") in str(exc_info.value)
        assert six.ensure_str(normal_msg, "utf-8") not in str(exc_info.value)

    def test_execute_handles_top_level_errors(self, mp, br):
        error_msg = "Something bad and very top-level happened"
        batch_resp = {"error_code": "BAD_REQUEST", "message": error_msg}
        mp.request(mock_response(batch_resp, status_code=http.client.BAD_REQUEST))

        with pytest.raises(idm.IDMBatchRequestError) as exc_info:
            br.request_role(["scopes", "project", "project", "plumbus"], user="blumbus")
            br.execute()
        assert error_msg in str(exc_info.value)

    def test_conflicts_are_not_sent_to_juggler(self, mp, br):
        send_mock = mp.function(juggler.send_event)
        br.request_role(["scopes", "project", "project", "plumbus"], user="blumbus")

        conflict_msg = '"Муть на русском" already has the same role (Scopes: project, Project: Project, Role in project: Project owner) in the system "Wall-e"'
        batch_resp = {
            "responses": [
                {
                    'body': {'message': conflict_msg, 'error_code': 'CONFLICT', 'simulated': False},
                    'id': '0',
                    "headers": {},
                    'status_code': http.client.CONFLICT,
                }
            ]
        }
        mp.request(mock_response(batch_resp, status_code=http.client.CONFLICT))

        with br:
            br.execute()
        assert not send_mock.called

    @pytest.mark.parametrize(
        "orig_exc", [ValueError("Something weird happened"), idm.IDMInternalError("Well, this one was expected")]
    )
    def test_context_manager(self, mp, orig_exc):
        mp.method(idm.BatchRequest.execute, side_effect=orig_exc, obj=idm.BatchRequest)

        with pytest.raises(idm.IDMInternalError):
            with idm.BatchRequest() as br:
                br.execute()

    @staticmethod
    def _check_subrequest(br, method, path, status_codes, data=None, subreq_idx=0):
        subreq = br._subrequests[subreq_idx]

        assert subreq["method"] == method
        assert subreq["path"] == path

        if data is not None:
            assert json.loads(subreq["data"]) == data

        assert subreq["id"] == str(subreq_idx)
        assert br._subrequest_id_to_expected_codes[str(subreq_idx)] == status_codes


@pytest.mark.usefixtures("mock_config")
@pytest.mark.parametrize("expected_is_broken", [True, False])
def test_is_system_broken(mp, expected_is_broken):
    system_info_mock = {"slug": SYSTEM_NAME, "is_active": True, "is_broken": expected_is_broken}
    request_mock = mp.request(mock_response(system_info_mock))
    is_broken = idm.is_system_broken()

    check_request_args(request_mock, "GET", "systems/{}/".format(SYSTEM_NAME))
    assert is_broken == expected_is_broken
