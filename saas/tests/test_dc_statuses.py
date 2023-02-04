import uuid
from typing import Tuple

import pytest
from google.protobuf import text_format

from saas.library.python.common_functions import DC_LOCATIONS
from saas.tools.devops.lb_dc_checker.dc_checker.checkers.service import ServiceChecker
from saas.tools.devops.lb_dc_checker.proto.result_pb2 import TDcCheckerResult


@pytest.fixture
def datacenters(fake) -> Tuple[str, ...]:
    return tuple(map(lambda x: x.lower(), DC_LOCATIONS))


async def _get_dc_checker_result(service_checker, dc_to_availability_info, call_num=1) -> TDcCheckerResult:
    await service_checker._save_dc_availability_info(dc_to_availability_info)

    assert service_checker._write_zk_data.call_count == call_num
    call = service_checker._write_zk_data.await_args_list[call_num - 1]
    proto_text = call.args[0]
    return text_format.Parse(proto_text, TDcCheckerResult())


@pytest.fixture
def service_checker(mocker, datacenters) -> ServiceChecker:
    config = mocker.MagicMock()
    service = mocker.MagicMock()
    cluster_checker = mocker.MagicMock()
    zk_client = mocker.MagicMock()

    service_checker = ServiceChecker(config, service, cluster_checker, zk_client)
    mocker.patch.object(service_checker, '_SAAS_DATACENTERS', datacenters)
    mocker.patch.object(service_checker, '_write_zk_data', mocker.AsyncMock())
    return service_checker


@pytest.mark.asyncio
async def test_offset_check_all_passed(service_checker, datacenters) -> None:
    dc_to_availability_info = {}
    for dc in datacenters:
        dc_to_availability_info[dc] = {
            'offset_check': True,
            'cluster_check': True
        }

    data = await _get_dc_checker_result(service_checker, dc_to_availability_info)
    assert len(data.AvailableDcs) == len(datacenters)
    assert len(data.UnavailableDcs) == 0


@pytest.mark.asyncio
async def test_offset_check_offset_single_fail(fake, service_checker, datacenters) -> None:
    dc_to_availability_info = {}
    fail_idx = fake.random_int(min=0, max=len(datacenters) - 1)
    for idx, dc in enumerate(datacenters):
        dc_to_availability_info[dc] = {
            'offset_check': True if idx != fail_idx else False,
            'cluster_check': True
        }

    data = await _get_dc_checker_result(service_checker, dc_to_availability_info)
    assert len(data.AvailableDcs) == len(datacenters) - 1
    assert len(data.UnavailableDcs) == 1
    assert data.UnavailableDcs[0] == datacenters[fail_idx]


@pytest.mark.asyncio
async def test_offset_check_cluster_single_fail(fake, service_checker, datacenters) -> None:
    dc_to_availability_info = {}
    fail_idx = fake.random_int(min=0, max=len(datacenters) - 1)
    for idx, dc in enumerate(datacenters):
        dc_to_availability_info[dc] = {
            'offset_check': True,
            'cluster_check': True if idx != fail_idx else False
        }

    data = await _get_dc_checker_result(service_checker, dc_to_availability_info)
    assert len(data.AvailableDcs) == len(datacenters) - 1
    assert len(data.UnavailableDcs) == 1
    assert data.UnavailableDcs[0] == datacenters[fail_idx]


@pytest.mark.asyncio
async def test_offset_check_single_fail_same(fake, service_checker, datacenters) -> None:
    dc_to_availability_info = {}
    fail_idx = fake.random_int(min=0, max=len(datacenters) - 1)
    for idx, dc in enumerate(datacenters):
        dc_to_availability_info[dc] = {
            'offset_check': True if idx != fail_idx else False,
            'cluster_check': True if idx != fail_idx else False
        }

    data = await _get_dc_checker_result(service_checker, dc_to_availability_info)
    assert len(data.AvailableDcs) == len(datacenters) - 1
    assert len(data.UnavailableDcs) == 1
    assert data.UnavailableDcs[0] == datacenters[fail_idx]


@pytest.mark.asyncio
async def test_offset_check_single_fail_different(fake, service_checker, datacenters) -> None:
    dc_to_availability_info = {}

    offset_check_fail_idx = fake.random_int(min=0, max=len(datacenters) - 1)
    cluster_check_fail_idx = (offset_check_fail_idx - 1) if offset_check_fail_idx != 0 else len(datacenters) - 1

    for idx, dc in enumerate(datacenters):
        dc_to_availability_info[dc] = {
            'offset_check': True if idx != offset_check_fail_idx else False,
            'cluster_check': True if idx != cluster_check_fail_idx else False
        }

    data = await _get_dc_checker_result(service_checker, dc_to_availability_info)
    assert len(data.AvailableDcs) == len(datacenters) - 1
    assert len(data.UnavailableDcs) == 1
    assert data.UnavailableDcs[0] == datacenters[cluster_check_fail_idx]


@pytest.mark.asyncio
async def test_offset_check_single_fails(service_checker, datacenters) -> None:
    dc_to_availability_info = {}

    for call_num, offset_hex, cluster_hex in [(i + 1, uuid.uuid4().hex, uuid.uuid4().hex) for i in range(10)]:

        offset_check_fail_idx = int(offset_hex, 16) % len(datacenters)
        cluster_check_fail_idx = int(cluster_hex, 16) % len(datacenters)

        for idx, dc in enumerate(datacenters):
            dc_to_availability_info[dc] = {
                'offset_check': True if idx != offset_check_fail_idx else False,
                'cluster_check': True if idx != cluster_check_fail_idx else False
            }

        data = await _get_dc_checker_result(service_checker, dc_to_availability_info, call_num=call_num)
        assert len(data.AvailableDcs) == len(datacenters) - 1
        assert len(data.UnavailableDcs) == 1
        assert data.UnavailableDcs[0] == datacenters[cluster_check_fail_idx]


@pytest.mark.asyncio
async def test_offset_all_failed(service_checker, datacenters):
    dc_to_availability_info = {}
    for idx, dc in enumerate(datacenters):
        dc_to_availability_info[dc] = {
            'offset_check': False,
            'cluster_check': True
        }

    data = await _get_dc_checker_result(service_checker, dc_to_availability_info)
    assert len(data.AvailableDcs) == len(datacenters)
    assert len(data.UnavailableDcs) == 0


@pytest.mark.asyncio
async def test_cluster_all_failed(service_checker, datacenters):
    dc_to_availability_info = {}
    for idx, dc in enumerate(datacenters):
        dc_to_availability_info[dc] = {
            'offset_check': True,
            'cluster_check': False
        }

    data = await _get_dc_checker_result(service_checker, dc_to_availability_info)
    assert len(data.AvailableDcs) == len(datacenters)
    assert len(data.UnavailableDcs) == 0


@pytest.mark.asyncio
async def test_all_failed(service_checker, datacenters):
    dc_to_availability_info = {}
    for idx, dc in enumerate(datacenters):
        dc_to_availability_info[dc] = {
            'offset_check': False,
            'cluster_check': False
        }

    data = await _get_dc_checker_result(service_checker, dc_to_availability_info)
    assert len(data.AvailableDcs) == len(datacenters)
    assert len(data.UnavailableDcs) == 0
