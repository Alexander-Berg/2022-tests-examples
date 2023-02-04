# coding: utf-8
import urllib.parse

import responses
from intranet.yandex_directory.src import settings

from hamcrest import (
    assert_that,
    equal_to,
    calling,
    raises,
)
from testutils import TestCase
from intranet.yandex_directory.src.yandex_directory.auth import tvm
from intranet.yandex_directory.src.yandex_directory.connect_services.direct.client.client import DirectClient, DirectRoleDto
from intranet.yandex_directory.src.yandex_directory.connect_services.direct.client.exceptions import DirectInteractionException


class TestDirectClient(TestCase):
    def setUp(self):
        super(TestDirectClient, self).setUp()
        tvm.tickets[settings.DIRECT_SERVICE_SLUG] = 'direct_tvm_ticket'
        self.client = DirectClient()
        self.get_roles_url = urllib.parse.urljoin(settings.DIRECT_HOST, '/connect/idm/get-roles')

    @responses.activate
    def test_with_correct_response(self):
        resource_id = str(54478303)

        responses.add(
            responses.GET,
            self.get_roles_url,
            json={
                "roles": [
                    {
                        "subject_type": "user",
                        "id": "811860566",
                        "org_id": None,
                        "fields": {
                            "resource_id": resource_id
                        },
                        "path": "/direct/user/role/chief/"
                    },
                    {
                        "subject_type": "user",
                        "id": "811860566",
                        "org_id": None,
                        "fields": {
                            "resource_id": resource_id
                        },
                        "path": "/direct/user/role/employee/"
                    }
                ],
                "code": 0,
            },
        )

        expected_roles = [
            DirectRoleDto(None, resource_id, "/direct/user/role/chief/", 811860566),
            DirectRoleDto(None, resource_id, "/direct/user/role/employee/", 811860566),
        ]

        roles = self.client.get_roles(resource_id)

        assert_that(
            roles,
            equal_to(expected_roles)
        )

    @responses.activate
    def test_with_response_with_incorrect_structure(self):
        resource_id = str(55555555)

        responses.add(
            responses.GET,
            self.get_roles_url,
            json={},
            status=200
        )

        assert_that(
            calling(self.client.get_roles).with_args(resource_id),
            raises(DirectInteractionException)
        )


    @responses.activate
    def test_with_response_error(self):
        resource_id = str(55555555)

        responses.add(
            responses.GET,
            self.get_roles_url,
            json={
                "code": 1,
                "error": "resource_id='{resource_id}' not found".format(resource_id=resource_id)
            }
        )

        assert_that(
            calling(self.client.get_roles).with_args(resource_id),
            raises(DirectInteractionException)
        )

    @responses.activate
    def test_with_response_code_different_from_200(self):
        resource_id = str(55555555)

        responses.add(
            responses.GET,
            self.get_roles_url,
            json={},
            status=500
        )

        assert_that(
            calling(self.client.get_roles).with_args(resource_id),
            raises(DirectInteractionException)
        )
