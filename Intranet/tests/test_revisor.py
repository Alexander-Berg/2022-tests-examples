from builtins import object

import pytest
import requests
from mock import patch

from kelvin.common.revisor import create_client

# from kelvin.common.tvm import tvm2_client

FAKE_SERVICE_TICKET = "QWERTYUIOP{LKJHGFDSAZXCVBNM"


@pytest.mark.skip()
class TestRevisorUserCRUD(object):
    # tvm2_client.get_service_tickets = lambda x: {x: FAKE_SERVICE_TICKET}

    def setup_method(self, method):
        self.requests_post_orig = requests.post
        self.requests_put_orig = requests.put
        self.requests_patch_orig = requests.patch
        self.requests_delete_orig = requests.delete
        self.requests_get_orig = requests.get

    def teardown_method(self, method):
        requests.post = self.requests_post_orig
        requests.put = self.requests_put_orig
        requests.patch = self.requests_patch_orig
        requests.delete = self.requests_delete_orig
        requests.get = self.requests_get_orig

    @staticmethod
    def __request_ok_mock__(url, headers, json, params, verify):
        assert headers["x-tvm2-ticket"] == FAKE_SERVICE_TICKET
        resp = requests.Response()
        resp.status_code = 200
        resp.headers = {'content-type': 'application/json'}
        resp.json = lambda: {"id": 31071982}
        return resp

    def test_revisor_create_user(self, mocker):
        requests.post = TestRevisorUserCRUD.__request_ok_mock__
        c = create_client()
        response = c.create_user(
            data={"p1": 1, "p2": 2},
            yauid=1235567890,
            username="pinochet",
        )
        assert response["id"] == 31071982

    def test_revisor_update_user(self):
        requests.put = TestRevisorUserCRUD.__request_ok_mock__
        c = create_client()
        response = c.update_user(
            revisor_user_id=31071982,
            data={"p1": 1, "p2": 2},
            yauid=1235567890,
            username="pinochet",
        )
        assert response["id"] == 31071982

    def test_revisor_delete_user(self):
        requests.delete = TestRevisorUserCRUD.__request_ok_mock__
        c = create_client()
        response = c.delete_user(
            revisor_user_id=31071982,
        )
        assert response["id"] == 31071982

    def test_revisor_get_user(self):
        requests.get = TestRevisorUserCRUD.__request_ok_mock__
        c = create_client()
        response = c.get_user(
            revisor_user_id=31071982,
        )
        assert response["id"] == 31071982


@pytest.mark.skip()
class TestRevisorGroupCRUD(object):
    # tvm2_client.get_service_tickets = lambda x: {x: FAKE_SERVICE_TICKET}

    def setup_method(self, method):
        self.requests_post_orig = requests.post
        self.requests_put_orig = requests.put
        self.requests_patch_orig = requests.patch
        self.requests_delete_orig = requests.delete
        self.requests_get_orig = requests.get

    def teardown_method(self, method):
        requests.post = self.requests_post_orig
        requests.put = self.requests_put_orig
        requests.patch = self.requests_patch_orig
        requests.delete = self.requests_delete_orig
        requests.get = self.requests_get_orig

    @staticmethod
    def __request_ok_mock__(url, headers, json, params, verify):
        assert headers["x-tvm2-ticket"] == FAKE_SERVICE_TICKET
        resp = requests.Response()
        resp.status_code = 200
        resp.headers = {'content-type': 'application/json'}
        resp.json = lambda: {"id": 31071982}
        return resp

    def test_revisor_create_group(self):
        requests.post = TestRevisorGroupCRUD.__request_ok_mock__
        c = create_client()
        response = c.create_group(
            data={"p1": 1, "p2": 2},
            yauid=1235567890,
            username="pinochet",
        )
        assert response["id"] == 31071982

    def test_revisor_update_group(self):
        requests.put = TestRevisorGroupCRUD.__request_ok_mock__
        c = create_client()
        response = c.update_group(
            revisor_group_id=31071982,
            data={"p1": 1, "p2": 2},
            yauid=1235567890,
            username="pinochet",
        )
        assert response["id"] == 31071982

    def test_revisor_delete_group(self):
        requests.delete = TestRevisorGroupCRUD.__request_ok_mock__
        c = create_client()
        response = c.delete_group(
            revisor_group_id=31071982,
        )
        assert response["id"] == 31071982

    def test_revisor_get_group(self):
        requests.get = TestRevisorGroupCRUD.__request_ok_mock__
        c = create_client()
        response = c.get_group(
            revisor_group_id=31071982,
        )
        assert response["id"] == 31071982


@pytest.mark.skip()
class TestRevisorGroupMembershipCRUD(object):
    pass
