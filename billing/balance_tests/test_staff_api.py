# -*- coding: utf-8 -*-
import pytest
import mock
import re
import json
import httpretty

from balance import exc
from balance.application import getApplication
from balance.api.staff import StaffApi

GROUP_ID = 666
SERVICE_ID = 777


def make_staff_response(status_code=200, data=None, error_dict=None):
    url = getApplication().get_component_cfg('staff_api')['URL']
    if error_dict:
        res = error_dict
    else:
        res = data or {}
    httpretty.register_uri(
        httpretty.GET,
        re.compile(url + 'v3/group/%s/' % GROUP_ID),
        status=status_code,
        content_type="text/json",
        body=json.dumps(res),
    )


@httpretty.activate
@mock.patch('balance.api.staff.StaffApi._get_tvm_ticket', return_value='666')
def test_invalid_response_by_status_code(_mock_tvm, app, session):
    make_staff_response(status_code=400)
    api = StaffApi(app, session)
    with pytest.raises(exc.STAFF_API_ERROR) as exc_info:
        api.get_abc_service_id(GROUP_ID)
    assert exc_info.value.msg == 'Staff api error: status_code=400, data={}'


@httpretty.activate
@mock.patch('balance.api.staff.StaffApi._get_tvm_ticket', return_value='666')
def test_invalid_response_by_error_in_response(_mock_tvm, app, session):
    make_staff_response(error_dict={'error': 'not found'})
    api = StaffApi(app, session)
    with pytest.raises(exc.STAFF_API_ERROR) as exc_info:
        api.get_abc_service_id(GROUP_ID)
    assert exc_info.value.msg == 'Staff api error: status_code=200, data={"error": "not found"}'


@httpretty.activate
@mock.patch('balance.tvm.get_or_create_tvm_client', side_effect=Exception('Invalid tvm client'))
def test_invalid_tvm_ticket(_mock_tvm, app, session):
    make_staff_response()
    api = StaffApi(app, session)
    with pytest.raises(exc.STAFF_API_ERROR) as exc_info:
        api.get_abc_service_id(GROUP_ID)
    assert exc_info.value.msg == 'Staff api error: status_code=None, data=can\'t get the service ticket for staff'


@httpretty.activate
@mock.patch('balance.api.staff.StaffApi._get_tvm_ticket', return_value='666')
def test_invalid_type(_mock_tvm, app, session):
    make_staff_response(data={'type': 'invalid'})
    api = StaffApi(app, session)
    with pytest.raises(exc.STAFF_API_ERROR) as exc_info:
        api.get_abc_service_id(GROUP_ID)
    assert exc_info.value.msg == 'Staff api error: status_code=200, data={u\'type\': u\'invalid\'}'


@httpretty.activate
@mock.patch('balance.api.staff.StaffApi._get_tvm_ticket', return_value='666')
def test_ok_service(_mock_tvm, app, session):
    make_staff_response(data={'service': {'id': SERVICE_ID}, 'type': 'service'})
    api = StaffApi(app, session)
    assert api.get_abc_service_id(GROUP_ID) == SERVICE_ID


@httpretty.activate
@mock.patch('balance.api.staff.StaffApi._get_tvm_ticket', return_value='666')
def test_invalid_service_id(_mock_tvm, app, session):
    make_staff_response(data={'type': 'service', 'service': {'id': None}})
    api = StaffApi(app, session)
    with pytest.raises(exc.STAFF_API_ERROR):
        api.get_abc_service_id(GROUP_ID)


@pytest.mark.parametrize(
    'second_ancestor',
    [
        {"type": "invalid", "is_deleted": False, "level": 3, 'service': {'id': 123}},
        {"type": "service", "is_deleted": False, "level": 1, 'service': {'id': 123}},
        {"type": "service", "is_deleted": True, "level": 3, 'service': {'id': 123}},
    ],
)
@httpretty.activate
@mock.patch('balance.api.staff.StaffApi._get_tvm_ticket', return_value='666')
def test_ok_servicerole(_mock_tvm, app, session, second_ancestor):
    data = {
        'type': 'servicerole',
        'ancestors': [
            {
                "type": "service",
                "is_deleted": False,
                "service": {
                    "id": SERVICE_ID,
                },
                "level": 2,
            },
        ],
    }
    data['ancestors'].append(second_ancestor)
    make_staff_response(data=data)
    api = StaffApi(app, session)
    assert api.get_abc_service_id(GROUP_ID) == SERVICE_ID


@pytest.mark.parametrize(
    'ancestors',
    [
        [{'type': 'service', 'is_deleted': True, "level": 3, 'service': {'id': 123}}],
        [{'type': 'invalid', 'is_deleted': False, "level": 3, 'service': {'id': 123}}],
        [{'type': 'service', 'is_deleted': False, "level": 3, 'service': {'id': None}}],
        [],
    ],
)
@httpretty.activate
@mock.patch('balance.api.staff.StaffApi._get_tvm_ticket', return_value='666')
def test_invalid_servicerole(_mock_tvm, app, session, ancestors):
    make_staff_response(data={'type': 'servicerole', 'ancestors': ancestors})
    api = StaffApi(app, session)
    with pytest.raises(exc.STAFF_API_ERROR):
        api.get_abc_service_id(GROUP_ID)
