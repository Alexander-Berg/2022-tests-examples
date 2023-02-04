"""Tests admin requests."""

import json
from unittest.mock import call

import pytest
from six import text_type as str

import walle.admin_requests.request as admin_requests
from infra.walle.server.tests.lib.util import MonkeyPatch, TestCase, any_task_status
from sepelib.core import config
from walle import audit_log
from walle import authorization
from walle.admin_requests.constants import RequestTypes, EineCode, _RequestPattern
from walle.admin_requests.severity import admin_request_severity_tag_factory, RequestSource, BotTag, EineTag
from walle.clients import bot
from walle.hosts import Host
from walle.models import timestamp, monkeypatch_timestamp
from walle.projects import RepairRequestSeverity
from walle.scenario.constants import ScriptName
from walle.util.misc import drop_none

BOT_ID = 99
TICKET = "ITDC"


@pytest.fixture()
def test(request, monkeypatch_timestamp, mp):
    return TestCase.create(request)


@pytest.fixture
def mock_cancel_request(mp):
    return mp.function(admin_requests._cancel_request)


@pytest.fixture(scope="module")
def fetch_patterns(request):
    mp = MonkeyPatch()
    request.addfinalizer(mp.undo)

    mp.setitem(config.get_value("bot"), "host", "bot.yandex-team.ru")

    patterns = bot.raw_request("/adm/js/request_patterns.php")
    # undo it right now so decreasing chances that any test hit real BOT accidentally
    mp.undo()
    patterns_parsed = json.loads(patterns)

    # inject request types that we know are here.
    patterns_parsed[RequestTypes.GPU_OVERHEATED.operation] = {
        "data": [
            {
                "Id": str(RequestTypes.GPU_MISSING.problem),
                "RequestType": RequestTypes.GPU_MISSING.operation.upper(),
                "Options": {
                    "fields": {
                        "slot": {"check": "desire"},
                        "serial": {"check": "desire"},
                    }
                },
            },
            {
                "Id": str(RequestTypes.GPU_OVERHEATED.problem),
                "RequestType": RequestTypes.GPU_OVERHEATED.operation.upper(),
                "Options": {
                    "fields": {
                        "slot": {"check": "desire"},
                        "serial": {"check": "desire"},
                    }
                },
            },
            {
                "Id": str(RequestTypes.GPU_BANDWIDTH_TOO_LOW.problem),
                "RequestType": RequestTypes.GPU_BANDWIDTH_TOO_LOW.operation.upper(),
                "Options": {
                    "fields": {
                        "slot": {"check": "desire"},
                        "serial": {"check": "desire"},
                    }
                },
            },
        ]
    }
    patterns_parsed[RequestTypes.SECOND_TIME_NODE.operation]["data"] += [
        {
            "Id": str(RequestTypes.SECOND_TIME_NODE.problem),
            "RequestType": RequestTypes.SECOND_TIME_NODE.operation.upper(),
            "Options": {"fields": {}},
        },
    ]

    for req_type in patterns_parsed:
        for req in patterns_parsed[req_type]["data"]:
            if not req["Options"]["fields"]:
                req["Options"]["fields"] = {"eine_code": {"check": "optional"}}
            else:
                req["Options"]["fields"].setdefault("eine_code", {"check": "optional"})

    return patterns_parsed


def find_pattern(patterns, request_type):
    for pattern in patterns[request_type.operation]["data"]:
        if pattern["RequestType"].upper() == request_type.operation.upper() and pattern["Id"] == str(
            request_type.problem
        ):
            return pattern


@pytest.fixture()
def mock_request_create(mp, fetch_patterns):
    def _create_in_bot(issuer, request_type, project_id, host_inv, params, *args, **kwargs):
        pattern = find_pattern(fetch_patterns, request_type)
        fields = pattern["Options"]["fields"]
        required = filter(lambda n: fields[n]["check"] == "require", fields)

        # check all required fields present
        for field in required:
            if field != "comment":  # we create comment from error messages inside this mocked method
                assert field in params

        # check all present fields allowed
        for field in params:
            assert field in fields

        return admin_requests.Result(bot_id=BOT_ID, ticket=TICKET)

    return mp.function(admin_requests._create_in_bot, side_effect=_create_in_bot)


@pytest.fixture
def mock_create_in_bot(mp):
    return mp.function(admin_requests._create_in_bot, return_value=admin_requests.Result(bot_id=BOT_ID, ticket=TICKET))


@pytest.mark.online
@pytest.mark.parametrize(
    "request_type",
    [requset_type for requset_type in RequestTypes.ALL_TYPES if requset_type not in RequestTypes.ALL_NOC],
)
def test_request_type_exists(fetch_patterns, request_type):
    pattern = find_pattern(fetch_patterns, request_type)
    assert pattern is not None


_NON_TRIVIAL_REQUESTS = {
    RequestTypes.IPMI_UNREACHABLE.type,
    RequestTypes.IPMI_HOST_MISSING.type,
    RequestTypes.MALFUNCTIONING_LINK_RX_CRC_ERRORS.type,
    RequestTypes.CORRUPTED_MEMORY.type,
    RequestTypes.CORRUPTED_DISK_BY_SLOT.type,
    RequestTypes.BAD_DISK_CABLE.type,
    RequestTypes.INVALID_MEMORY_SIZE.type,
    RequestTypes.CPU_FAILED.type,
    RequestTypes.GPU_MISSING.type,
    RequestTypes.GPU_OVERHEATED.type,
    RequestTypes.INFINIBAND_MISMATCH.type,
    RequestTypes.PCIE_DEVICE_BANDWIDTH_TOO_LOW.type,
    RequestTypes.INFINIBAND_INVALID_PHYS_STATE.type,
    RequestTypes.INFINIBAND_ERR.type,
    RequestTypes.INFINIBAND_INVALID_STATE.type,
    RequestTypes.INFINIBAND_LOW_SPEED.type,
}

_TRIVIAL_REQUESTS = [
    request_type for request_type in RequestTypes.ALL_TYPES if request_type.type not in _NON_TRIVIAL_REQUESTS
]


@pytest.mark.online
@pytest.mark.parametrize("request_type", _TRIVIAL_REQUESTS)
@pytest.mark.parametrize(
    ["decision_params", "params"],
    [
        ({}, {}),
        ({"eine_code": ["EINE_ERROR_CODE"]}, {"eine_code": "EINE_ERROR_CODE"}),
        ({"eine_code": ["EINE_ERROR_CODE", "CODE"]}, {"eine_code": "EINE_ERROR_CODE,CODE"}),
    ],
)
def test_create_admin_request(test, mock_request_create, request_type, decision_params, params, mp):
    project = test.mock_project(dict(id="prj-id"))
    host = Host(inv=999, name="host-name", project=project.id)

    test.admin_requests.mock(
        {
            "id": admin_requests._request_id(request_type, 999),
            "time": timestamp(),
            "type": request_type.type,
            "bot_id": BOT_ID,
            "host_inv": host.inv,
            "host_name": host.name,
            "host_uuid": host.uuid,
        },
        save=False,
    )

    admin_requests.create_admin_request(host, request_type, "Reason mock", **decision_params)

    test.admin_requests.assert_equal()
    mock_request_create.assert_called_once_with(
        authorization.ISSUER_WALLE,
        request_type,
        "prj-id",
        host_inv=host.inv,
        host_name=host.name,
        reason="Reason mock",
        params=params,
        severity_tag=BotTag.MEDIUM,
    )


@pytest.mark.online
@pytest.mark.parametrize(
    ["request_type", "params"],
    [
        (RequestTypes.IPMI_UNREACHABLE, {"eine_code": EineCode.IPMI_PROTO_ERROR}),
        (RequestTypes.IPMI_HOST_MISSING, {"eine_code": EineCode.IPMI_DNS_RESOLUTION_FAILED}),
    ],
)
def test_on_ipmi_errors(test, mock_request_create, request_type, params):
    project = test.mock_project(dict(id="prj-id"))
    host = Host(inv=999, name="host-name", project=project.id)

    test.admin_requests.mock(
        {
            "id": admin_requests._request_id(request_type, 999),
            "time": timestamp(),
            "type": request_type.type,
            "bot_id": BOT_ID,
            "host_inv": host.inv,
            "host_name": host.name,
            "host_uuid": host.uuid,
        },
        save=False,
    )

    admin_requests.create_admin_request(host, request_type, "Reason mock")

    test.admin_requests.assert_equal()
    mock_request_create.assert_called_once_with(
        authorization.ISSUER_WALLE,
        request_type,
        "prj-id",
        host_inv=host.inv,
        host_name=host.name,
        reason="Reason mock",
        params=params,
        severity_tag=BotTag.MEDIUM,
    )


@pytest.mark.online
@pytest.mark.parametrize(
    ["decision_params", "params"],
    [
        ({}, {}),
        ({"eine_code": ["RX_CRC_ERROR"]}, {"eine_code": "RX_CRC_ERROR"}),
        ({"eine_code": ["RX_CRC_ERROR", "CODE"]}, {"eine_code": "RX_CRC_ERROR,CODE"}),
    ],
)
def test_on_link_crc_errors(test, mock_request_create, decision_params, params):
    request_type = RequestTypes.MALFUNCTIONING_LINK_RX_CRC_ERRORS
    project = test.mock_project(dict(id="prj-id"))
    host = Host(inv=999, name="host-name", project=project.id)

    test.admin_requests.mock(
        {
            "id": admin_requests._request_id(request_type, 999),
            "time": timestamp(),
            "type": request_type.type,
            "bot_id": BOT_ID,
            "host_inv": host.inv,
            "host_name": host.name,
            "host_uuid": host.uuid,
        },
        save=False,
    )

    admin_requests.create_admin_request(host, request_type, "Reason mock", **decision_params)

    expected_reason = (
        "Reason mock\nYASM graph: "
        "https://yasm.yandex-team.ru/chart/itype=common;hosts=host-name;signals=netstat-ierrs_summ/\n"
    )

    test.admin_requests.assert_equal()
    mock_request_create.assert_called_once_with(
        authorization.ISSUER_WALLE,
        request_type,
        "prj-id",
        host_inv=host.inv,
        host_name=host.name,
        reason=expected_reason,
        params=params,
        severity_tag=BotTag.MEDIUM,
    )


@pytest.mark.online
@pytest.mark.parametrize(
    ["decision_params", "params"],
    [
        ({}, {}),
        ({"eine_code": ["MEM_ECC_ERROR"]}, {"eine_code": "MEM_ECC_ERROR"}),
        ({"eine_code": ["MEM_ECC_ERROR", "CODE"]}, {"eine_code": "MEM_ECC_ERROR,CODE"}),
    ],
)
def test_on_corrupted_memory(test, mock_request_create, decision_params, params):
    project = test.mock_project(dict(id="prj-id"))
    host = Host(inv=999, name="host-name", project=project.id)
    request_type = RequestTypes.CORRUPTED_MEMORY
    slot = 3

    test.admin_requests.mock(
        {
            "id": admin_requests._request_id(request_type, 999, slot),
            "time": timestamp(),
            "type": request_type.type,
            "bot_id": BOT_ID,
            "host_inv": host.inv,
            "host_name": host.name,
            "host_uuid": host.uuid,
        },
        save=False,
    )

    admin_requests.create_admin_request(
        host, request_type, reason="Reason mock", slot=slot, errors=["error1", "error2"], **decision_params
    )

    expected_reason = "Reason mock\n\nECC errors:\n* error1\n* error2"
    test.admin_requests.assert_equal()
    mock_request_create.assert_called_once_with(
        authorization.ISSUER_WALLE,
        request_type,
        "prj-id",
        host_inv=host.inv,
        host_name=host.name,
        reason=expected_reason,
        params=dict(params, slot=slot),
        severity_tag=BotTag.MEDIUM,
    )


@pytest.mark.online
@pytest.mark.parametrize(
    ["decision_params", "params"],
    [
        ({}, {}),
        ({"eine_code": ["MEM_SIZE_ERROR"]}, {"eine_code": "MEM_SIZE_ERROR"}),
        ({"eine_code": ["MEM_SIZE_ERROR", "CODE"]}, {"eine_code": "MEM_SIZE_ERROR,CODE"}),
    ],
)
def test_on_invalid_memory_size(test, mock_request_create, decision_params, params):
    project = test.mock_project(dict(id="prj-id"))
    host = Host(inv=999, name="host-name", project=project.id)
    request_type = RequestTypes.INVALID_MEMORY_SIZE
    test.admin_requests.mock(
        {
            "id": admin_requests._request_id(request_type, host.inv),
            "time": timestamp(),
            "type": request_type.type,
            "bot_id": BOT_ID,
            "host_inv": host.inv,
            "host_name": host.name,
            "host_uuid": host.uuid,
        },
        save=False,
    )

    admin_requests.create_admin_request(host, request_type, "Reason mock", expected=64, real=56, **decision_params)

    test.admin_requests.assert_equal()
    mock_request_create.assert_called_once_with(
        authorization.ISSUER_WALLE,
        request_type,
        "prj-id",
        host_inv=host.inv,
        host_name=host.name,
        reason="Reason mock",
        params=dict(params, realmem=56),
        severity_tag=BotTag.MEDIUM,
    )


@pytest.mark.online
def test_on_cpu_failed_size(test, mock_request_create):
    project = test.mock_project(dict(id="prj-id"))
    host = Host(inv=999, name="host-name", project=project.id)
    slot = 1
    request_type = RequestTypes.CPU_FAILED
    test.admin_requests.mock(
        {
            "id": admin_requests._request_id(request_type, host.inv, slot),
            "time": timestamp(),
            "type": request_type.type,
            "bot_id": BOT_ID,
            "host_inv": host.inv,
            "host_name": host.name,
            "host_uuid": host.uuid,
        },
        save=False,
    )

    admin_requests.create_admin_request(host, request_type, "Reason mock", slot=slot)

    test.admin_requests.assert_equal()
    mock_request_create.assert_called_once_with(
        authorization.ISSUER_WALLE,
        request_type,
        "prj-id",
        host_inv=host.inv,
        host_name=host.name,
        reason="Reason mock",
        params={"slot": slot},
        severity_tag=BotTag.MEDIUM,
    )


@pytest.mark.online
@pytest.mark.parametrize(
    ["decision_eine_code", "params_eine_code"],
    [
        (None, None),
        ("GPU_ERROR", "GPU_ERROR"),
        (["GPU_ERROR"], "GPU_ERROR"),
        (["GPU_ERROR", "ERROR_CODE"], "GPU_ERROR,ERROR_CODE"),
    ],
)
@pytest.mark.parametrize(
    ["errors", "decision_params", "request_params", "id_parts"],
    [
        # should get params from error messages
        (
            ["availability: GPU Tesla V100-PCIE-32GB PCIe 0000:87:00.0 not found"],
            {},
            {"slot": "0000:87:00.0"},
            ["0000:87:00.0"],
        ),
        (
            ["availability: less local GPUs 6 than in bot 8: 0321418015041, 0321418015869"],
            {},
            {"serial": "0321418015041"},
            ["0321418015041"],
        ),
        (["availability: less local GPUs 6 than in bot 8"], {}, {}, []),
        # should get params from decision params. HW-wtcher does not currently provide these, this is for future.
        (
            ["availability: less local GPUs 6 than in bot 8: 0321418015041, 0321418015869"],
            {"serial": "0321418015869"},
            {"serial": "0321418015869"},
            ["0321418015869"],
        ),
        (
            [
                "availability: GPU Tesla V100-PCIE-32GB PCIe 0000:87:00.0 not found",
                "availability: GPU Tesla V100-PCIE-32GB PCIe 0000:88:00.0 not found",
            ],
            {"slot": "0000:88:00.0", "serial": "0321418015869"},
            {"slot": "0000:88:00.0", "serial": "0321418015869"},
            ["0000:88:00.0", "0321418015869"],
        ),
    ],
)
def test_on_missing_gpu(
    test, mock_request_create, errors, decision_params, request_params, id_parts, decision_eine_code, params_eine_code
):
    project = test.mock_project(dict(id="prj-id"))
    host = Host(inv=999, name="host-name", project=project.id)

    request_type = RequestTypes.GPU_MISSING
    test.admin_requests.mock(
        {
            "id": admin_requests._request_id(request_type, host.inv, *id_parts),
            "time": timestamp(),
            "type": request_type.type,
            "bot_id": BOT_ID,
            "host_inv": host.inv,
            "host_name": host.name,
            "host_uuid": host.uuid,
        },
        save=False,
    )

    if decision_eine_code:
        decision_params["eine_code"] = decision_eine_code
        request_params["eine_code"] = params_eine_code

    admin_requests.create_admin_request(host, request_type, "Reason mock", errors=errors, **decision_params)

    expected_reason = "Reason mock\n\nGPU errors:\n* {}".format("\n* ".join(errors))
    test.admin_requests.assert_equal()
    mock_request_create.assert_called_once_with(
        authorization.ISSUER_WALLE,
        request_type,
        "prj-id",
        host_inv=host.inv,
        host_name=host.name,
        reason=expected_reason,
        params=request_params,
        severity_tag=BotTag.MEDIUM,
    )


@pytest.mark.online
@pytest.mark.parametrize(
    ["decision_eine_code", "params_eine_code"],
    [
        (None, None),
        ("GPU_ERROR", "GPU_ERROR"),
        (["GPU_ERROR"], "GPU_ERROR"),
        (["GPU_ERROR", "ERROR_CODE"], "GPU_ERROR,ERROR_CODE"),
    ],
)
@pytest.mark.parametrize(
    ["errors", "decision_params", "request_params", "id_parts"],
    [
        # should get params from error messages
        (
            ["temperature: GPU GeForce GTX 1080 Ti PCIe 0000:05:00.0 temp is 90 C (threshold 90 C)"],
            {},
            {"slot": "0000:05:00.0"},
            ["0000:05:00.0"],
        ),
        (
            ["temperature: acitve capping (sw thermal slowdown) on GPU GeForce GTX 1080 Ti PCIe 0000:85:00.0"],
            {},
            {"slot": "0000:85:00.0"},
            ["0000:85:00.0"],
        ),
        # should get params from decision params. HW-wtcher does not currently provide these, this is for future.
        (
            ["temperature: acitve capping (sw thermal slowdown) on GPU GeForce GTX 1080 Ti PCIe 0000:85:00.0"],
            {"slot": "0000:88:00.0"},
            {"slot": "0000:88:00.0"},
            ["0000:88:00.0"],
        ),
        (
            [
                "temperature: acitve capping (sw thermal slowdown) on GPU GeForce GTX 1080 Ti PCIe 0000:85:00.0",
                "temperature: GPU GeForce GTX 1080 Ti PCIe 0000:05:00.0 temp is 90 C (threshold 90 C)",
            ],
            {"slot": "0000:88:00.0", "serial": "0321418015869"},
            {"slot": "0000:88:00.0", "serial": "0321418015869"},
            ["0000:88:00.0"],
        ),
    ],
)
def test_on_gpu_overheat(
    test, mock_request_create, errors, decision_params, request_params, id_parts, decision_eine_code, params_eine_code
):
    project = test.mock_project(dict(id="prj-id"))
    host = Host(inv=999, name="host-name", project=project.id)

    request_type = RequestTypes.GPU_OVERHEATED
    test.admin_requests.mock(
        {
            "id": admin_requests._request_id(request_type, host.inv, *id_parts),
            "time": timestamp(),
            "type": request_type.type,
            "bot_id": BOT_ID,
            "host_inv": host.inv,
            "host_name": host.name,
            "host_uuid": host.uuid,
        },
        save=False,
    )

    if decision_eine_code:
        decision_params["eine_code"] = decision_eine_code
        request_params["eine_code"] = params_eine_code

    admin_requests.create_admin_request(host, request_type, "Reason mock", errors=errors, **decision_params)

    expected_reason = "Reason mock\n\nGPU errors:\n* {}".format("\n* ".join(errors))
    test.admin_requests.assert_equal()
    mock_request_create.assert_called_once_with(
        authorization.ISSUER_WALLE,
        request_type,
        "prj-id",
        host_inv=host.inv,
        host_name=host.name,
        reason=expected_reason,
        params=request_params,
        severity_tag=BotTag.MEDIUM,
    )


@pytest.mark.online
@pytest.mark.parametrize(
    ["decision_params", "params"],
    [
        ({}, {}),
        ({"eine_code": ["DISK_ERROR"]}, {"eine_code": "DISK_ERROR"}),
        ({"eine_code": ["DISK_ERROR", "CODE"]}, {"eine_code": "DISK_ERROR,CODE"}),
    ],
)
@pytest.mark.parametrize(
    ["slot", "model", "serial"],
    [
        (2, "hirosima", "nagasaki"),
        (2, "hirosima", None),
        (2, None, "nagasaki"),
        (2, None, None),
    ],
)
def test_on_corrupted_disk_slot(test, mock_request_create, slot, model, serial, decision_params, params):
    project = test.mock_project(dict(id="prj-id"))
    host = Host(inv=999, name="mock-host-name", project=project.id)
    request_type = RequestTypes.CORRUPTED_DISK_BY_SLOT
    test.admin_requests.mock(
        {
            "id": admin_requests._request_id(request_type, 999, slot, *filter(None, [serial])),
            "time": timestamp(),
            "type": request_type.type,
            "bot_id": BOT_ID,
            "host_inv": host.inv,
            "host_name": host.name,
            "host_uuid": host.uuid,
        },
        save=False,
    )

    admin_requests.create_admin_request(
        host,
        request_type,
        "Reason mock",
        slot=slot,
        model=model,
        serial=serial,
        errors=["error1", "error2"],
        **decision_params
    )
    test.admin_requests.assert_equal()
    mock_request_create.assert_called_once_with(
        authorization.ISSUER_WALLE,
        request_type,
        host.project,
        host_inv=host.inv,
        host_name=host.name,
        reason="Reason mock\n\nDisk errors:\n* error1\n* error2",
        params=drop_none(dict(params, slot=slot, serial=serial)),
        severity_tag=BotTag.MEDIUM,
    )


@pytest.mark.online
@pytest.mark.parametrize(
    ["decision_params", "params"],
    [
        ({}, {}),
        ({"eine_code": ["DISK_ERROR"]}, {"eine_code": "DISK_ERROR"}),
        ({"eine_code": ["DISK_ERROR", "CODE"]}, {"eine_code": "DISK_ERROR,CODE"}),
    ],
)
@pytest.mark.parametrize(
    ["slot", "model", "serial"],
    [
        (None, "hirosima", "nagasaki"),
        (None, None, "nagasaki"),
    ],
)
def test_on_corrupted_disk_serial(test, mock_request_create, slot, model, serial, decision_params, params):
    project = test.mock_project(dict(id="prj-id"))
    host = Host(inv=999, name='mock-host-name', project=project.id)
    request_type = RequestTypes.CORRUPTED_DISK_BY_SERIAL

    test.admin_requests.mock(
        {
            "id": admin_requests._request_id(request_type, 999, serial),
            "time": timestamp(),
            "type": request_type.type,
            "bot_id": BOT_ID,
            "host_inv": host.inv,
            "host_name": host.name,
            "host_uuid": host.uuid,
        },
        save=False,
    )

    admin_requests.create_admin_request(
        host,
        request_type,
        "Reason mock",
        errors=["error1", "error2"],
        slot=slot,
        model=model,
        serial=serial,
        **decision_params
    )

    test.admin_requests.assert_equal()
    mock_request_create.assert_called_once_with(
        authorization.ISSUER_WALLE,
        request_type,
        host.project,
        host_inv=host.inv,
        host_name=host.name,
        reason="Reason mock\n\nDisk errors:\n* error1\n* error2",
        params=dict(params, serial=serial),
        severity_tag=BotTag.MEDIUM,
    )


@pytest.mark.online
@pytest.mark.parametrize(
    ["decision_params", "params"],
    [
        ({}, {}),
        ({"eine_code": ["DISK_ERROR"]}, {"eine_code": "DISK_ERROR"}),
        ({"eine_code": ["DISK_ERROR", "CODE"]}, {"eine_code": "DISK_ERROR,CODE"}),
        ({"eine_code": ["ATA_LINK_DEGRADED"]}, {"eine_code": "ATA_LINK_DEGRADED"}),
    ],
)
@pytest.mark.parametrize(
    ["slot", "serial", "shelf_inv", "request_type"],
    [
        (1, "nagasaki", "10009", RequestTypes.CORRUPTED_DISK_BY_SLOT),
        (1, None, "10009", RequestTypes.CORRUPTED_DISK_BY_SLOT),
        (None, "nagasaki", "10009", RequestTypes.CORRUPTED_DISK_BY_SERIAL),
        (1, "nagasaki", "10009", RequestTypes.BAD_DISK_CABLE),
    ],
)
def test_on_corrupted_disk_shelf(
    test, mock_request_create, slot, serial, shelf_inv, request_type, decision_params, params
):
    project = test.mock_project(dict(id="prj-id"))
    host = Host(inv=999, name="hostname-mock", project=project.id)
    test.admin_requests.mock(
        {
            "id": admin_requests._request_id(request_type, 999, *filter(None, [slot, serial, shelf_inv])),
            "time": timestamp(),
            "type": request_type.type,
            "bot_id": BOT_ID,
            "host_inv": host.inv,
            "host_name": host.name,
            "host_uuid": host.uuid,
        },
        save=False,
    )

    admin_requests.create_admin_request(
        host,
        request_type,
        reason="Reason mock",
        errors=["error1", "error2"],
        slot=slot,
        serial=serial,
        shelf_inv=shelf_inv,
        **decision_params
    )
    mock_request_create.assert_called_once_with(
        authorization.ISSUER_WALLE,
        request_type,
        host.project,
        host_inv=shelf_inv,
        host_name=None,
        reason="Reason mock\n\nDisk errors:\n* error1\n* error2",
        params=drop_none(dict(params, serial=serial, slot=slot)),
        severity_tag=BotTag.MEDIUM,
    )

    test.admin_requests.assert_equal()


@pytest.mark.parametrize("request_type", RequestTypes.ALL_TYPES)
def test_create(test, mp, request_type, mock_create_in_bot):
    monkeypatch_timestamp(mp)

    project = test.mock_project(dict(id="prj-id"))
    host = test.mock_host(dict(project=project.id))

    test.admin_requests.mock(
        {
            "id": admin_requests._request_id(request_type, host.inv),
            "time": timestamp(),
            "type": request_type.type,
            "bot_id": BOT_ID,
            "host_inv": host.inv,
            "host_name": host.name,
            "host_uuid": host.uuid,
        },
        save=False,
    )
    admin_requests._create(authorization.ISSUER_WALLE, request_type, host, {}, "Reason mock")

    mock_create_in_bot.assert_called_once_with(
        authorization.ISSUER_WALLE,
        request_type,
        host.project,
        host_inv=host.inv,
        host_name=host.name,
        params={},
        reason="Reason mock",
        severity_tag=BotTag.MEDIUM,
    )

    test.admin_requests.assert_equal()


def test_create_for_shelf(test, mp, mock_create_in_bot):
    monkeypatch_timestamp(mp)
    request_type = RequestTypes.CORRUPTED_DISK_BY_SERIAL

    project = test.mock_project(dict(id="prj-id"))
    host = test.mock_host(dict(project=project.id))
    shelf_inv = "shelf-inv-mock"

    test.admin_requests.mock(
        {
            "id": admin_requests._request_id(request_type, host.inv),
            "time": timestamp(),
            "type": request_type.type,
            "bot_id": BOT_ID,
            "host_inv": host.inv,
            "host_name": host.name,
            "host_uuid": host.uuid,
        },
        save=False,
    )
    admin_requests._create(authorization.ISSUER_WALLE, request_type, host, {}, "Reason mock", shelf_inv=shelf_inv)

    mock_create_in_bot.assert_called_once_with(
        authorization.ISSUER_WALLE,
        request_type,
        host.project,
        host_inv=shelf_inv,
        host_name=None,
        params={},
        reason="Reason mock",
        severity_tag=BotTag.MEDIUM,
    )
    test.admin_requests.assert_equal()


@pytest.mark.parametrize("request_type", RequestTypes.ALL_TYPES)
def test_create_duplicated(test, mp, request_type, mock_create_in_bot):
    monkeypatch_timestamp(mp)

    host = Host(inv=999)
    request = test.admin_requests.mock(
        {
            "id": admin_requests._request_id(request_type, 999),
            "time": 0,
            "type": request_type.type,
            "bot_id": 0,
            "host_inv": host.inv,
            "host_uuid": host.uuid,
        }
    )
    admin_requests._create(authorization.ISSUER_WALLE, request_type, host, {}, "Reason mock")
    request.bot_id = BOT_ID
    request.time = timestamp()
    test.admin_requests.assert_equal()


def test_get_last_request_status_no_requests(test, mp):
    get_status = mp.function(admin_requests.get_request_status)
    assert admin_requests.get_last_request_status(RequestTypes.ALL_TYPES[0], 0) is None
    assert get_status.mock_calls == []


def test_get_last_request_status_outdated(test, mp):
    request = test.admin_requests.mock()

    get_status = mp.function(
        admin_requests.get_request_status,
        return_value={
            "status": admin_requests.STATUS_PROCESSED,
            "close_time": timestamp() - admin_requests._REQUEST_RESOLUTION_TIMEOUT,
        },
    )

    assert admin_requests.get_last_request_status(request, request.host_inv) is None
    get_status.assert_called_once_with(request.bot_id)


def test_get_last_request_status(test, mp):
    request = test.admin_requests.mock()

    status = {"status": "status-mock"}
    get_status = mp.function(admin_requests.get_request_status, return_value=status)

    assert admin_requests.get_last_request_status(request, request.host_inv) is status
    get_status.assert_called_once_with(request.bot_id)


def test_cancel(mock_cancel_request, test):
    request_type = RequestTypes.IPMI_HOST_MISSING
    # Match by inventory number
    test.admin_requests.mock(
        {
            "id": admin_requests._request_id(request_type, 0),
            "type": request_type.type,
            "bot_id": 100,
            "host_inv": 0,
        },
        add=False,
    )

    # Host doesn't match by inv
    test.admin_requests.mock(
        {
            "id": admin_requests._request_id(request_type, 1),
            "type": request_type.type,
            "bot_id": 101,
            "host_inv": 1,
        }
    )

    # Match by host name
    test.admin_requests.mock(
        {
            "id": admin_requests._request_id(request_type, 2),
            "type": request_type.type,
            "bot_id": 102,
            "host_inv": 2,
            "host_name": "two",
        },
        add=False,
    )

    # Host doesn't match by name
    test.admin_requests.mock(
        {
            "id": admin_requests._request_id(request_type, 4),
            "type": request_type.type,
            "bot_id": 103,
            "host_inv": 3,
            "host_name": "three",
        }
    )

    admin_requests.cancel_all_by_host(inv=0, name="two")

    assert sorted(mock_cancel_request.mock_calls) == sorted([call(100), call(102)])
    test.admin_requests.assert_equal()


def test_gc(test, mock_cancel_request, monkeypatch_locks):
    # Host which is processing some task
    host_with_task = test.mock_host(dict(inv=0, status=any_task_status(), tier=2))
    request_type = RequestTypes.IPMI_HOST_MISSING

    test.admin_requests.mock(
        {
            "id": admin_requests._request_id(request_type, host_with_task.inv),
            "bot_id": 100,
            "host_inv": host_with_task.inv,
            "host_uuid": host_with_task.uuid,
        }
    )

    # Host which isn't processing any task
    steady_host = test.mock_host(dict(inv=1))
    test.admin_requests.mock(
        {
            "id": admin_requests._request_id(request_type, steady_host.inv),
            "bot_id": 101,
            "host_inv": steady_host.inv,
            "host_uuid": steady_host.uuid,
        },
        add=False,
    )

    # Request for host that isn't in our database already
    test.admin_requests.mock(
        {
            "id": admin_requests._request_id(request_type, 666),
            "bot_id": 102,
            "host_inv": 666,
            "host_uuid": "00000000000000000000000000000666",
        },
        add=False,
    )

    # Request that can't be cancelled
    test.admin_requests.mock(
        {
            "id": admin_requests._request_id(request_type, 999),
            "bot_id": 103,
            "host_inv": 999,
            "host_uuid": "00000000000000000000000000000999",
        },
        add=False,
    )  # foreign_request

    admin_requests._gc_admin_requests()

    assert sorted(mock_cancel_request.mock_calls) == sorted([call(101), call(102), call(103)])

    test.admin_requests.assert_equal()
    test.hosts.assert_equal()


@pytest.mark.online
@pytest.mark.parametrize("request_type", _TRIVIAL_REQUESTS[::-1])
def test_admin_requests_audit_log(test, mock_request_create, request_type):
    project = test.mock_project(dict(id="prj-id"))
    host = Host(inv=999, project=project.id)
    test.admin_requests.mock(
        {
            "id": admin_requests._request_id(request_type, 999),
            "time": timestamp(),
            "type": request_type.type,
            "bot_id": BOT_ID,
            "host_inv": host.inv,
            "host_name": host.name,
            "host_uuid": host.uuid,
        },
        save=False,
    )

    admin_requests.create_admin_request(host, request_type, "Reason mock", **{})
    audit_entry = audit_log.LogEntry.objects(payload={"ticket": TICKET}).get()

    assert TICKET == audit_entry.payload['ticket']


def test_all_request_patterns_in_all_types(test):
    for request_pattern in vars(RequestTypes):
        if isinstance(request_pattern, _RequestPattern):
            assert request_pattern in RequestTypes.ALL_TYPES


@pytest.mark.parametrize(
    ["project_severity", "request_source", "result_tag"],
    [
        (RepairRequestSeverity.HIGH, RequestSource.EINE, EineTag.HIGH),
        (RepairRequestSeverity.MEDIUM, RequestSource.EINE, EineTag.MEDIUM),
        (RepairRequestSeverity.LOW, RequestSource.EINE, EineTag.LOW),
        (None, RequestSource.EINE, EineTag.MEDIUM),
        (RepairRequestSeverity.HIGH, RequestSource.BOT, BotTag.HIGH),
        (RepairRequestSeverity.MEDIUM, RequestSource.BOT, BotTag.MEDIUM),
        (RepairRequestSeverity.LOW, RequestSource.BOT, BotTag.LOW),
        (None, RequestSource.BOT, BotTag.MEDIUM),
    ],
)
def test_get_severity_tag_for_bot_and_eine_from_project(walle_test, project_severity, request_source, result_tag):
    project = walle_test.mock_project({"repair_request_severity": project_severity, "id": "mock-prj"})
    host = walle_test.mock_host(dict(project=project.id))

    tag = admin_request_severity_tag_factory(host, request_source)
    assert tag == result_tag


@pytest.mark.parametrize(
    ["request_source", "result_tag"], [(RequestSource.EINE, EineTag.MEDIUM), (RequestSource.BOT, BotTag.MEDIUM)]
)
def test_get_severity_tag_for_bot_and_eine_by_default(walle_test, request_source, result_tag):
    project = walle_test.mock_project({"id": "mock-prj"})
    host = walle_test.mock_host(dict(project=project.id))

    tag = admin_request_severity_tag_factory(host, request_source)
    assert tag == result_tag


@pytest.mark.parametrize(
    ["request_source", "scenario_type", "result_tag"],
    [
        (RequestSource.EINE, ScriptName.ITDC_MAINTENANCE, EineTag.MEDIUM),
        (RequestSource.EINE, ScriptName.HOSTS_TRANSFER, EineTag.LOW),
    ],
)
def test_get_severity_tag_for_bot_and_eine_if_host_in_scenario(walle_test, request_source, result_tag, scenario_type):
    scenario = walle_test.mock_scenario(dict(scenario_id=1, scenario_type=scenario_type))
    project = walle_test.mock_project({"id": "mock-prj", "repair_request_severity": RepairRequestSeverity.MEDIUM})
    host = walle_test.mock_host(dict(project=project.id, scenario_id=scenario.scenario_id))

    tag = admin_request_severity_tag_factory(host, request_source)
    assert tag == result_tag
