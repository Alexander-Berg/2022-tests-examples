import pytest
from datetime import datetime, timedelta

from arq.jobs import JobResult
from intranet.trip.src.enums import ServiceStatus
from intranet.trip.src.logic.monitoring import (
    get_broken_services_count,
    get_trips_for_staff_push_count,
    get_person_trips_for_aeroclub_create_count,
    get_not_authorized_person_trips_count,
    get_person_for_ihub_sync_count,
    get_person_trips_without_chat_id_count,
    build_arq_stats_response,
)

pytestmark = pytest.mark.asyncio


async def _create_person_trip(f, **fields):
    fields['trip_id'] = fields.get('trip_id', 1)
    fields['person_id'] = fields.get('person_id', 1)
    await f.create_person(person_id=fields['person_id'])
    await f.create_trip(trip_id=fields['trip_id'], provider_city_to_id=1)
    await f.create_person_trip(**fields)


async def _create_service(f, service_id, **fields):
    await _create_person_trip(f)
    fields = {
        'service_id': service_id,
        'person_id': 1,
        'trip_id': 1,
        'provider_order_id': service_id,
        'provider_service_id': service_id,
        'status': ServiceStatus.draft,
        **fields
    }
    await f.create_service(**fields)


async def test_broken_services_count(f, uow):
    broken_services_count = await get_broken_services_count(uow)
    assert broken_services_count == [['broken_services_count_axxx', 0]]
    await _create_service(f, service_id=1, is_broken=True)
    broken_services_count = await get_broken_services_count(uow)
    assert broken_services_count == [['broken_services_count_axxx', 1]]


async def test_trips_for_staff_push_count(f, uow):
    trips_for_staff_push_count = await get_trips_for_staff_push_count(uow)
    assert trips_for_staff_push_count == [['trips_for_staff_push_count_axxx', 0]]
    await _create_person_trip(f)
    trips_for_staff_push_count = await get_trips_for_staff_push_count(uow)
    assert trips_for_staff_push_count == [['trips_for_staff_push_count_axxx', 1]]


async def test_person_trips_for_aeroclub_create_count(f, uow):
    person_trips_for_ac_create_count = await get_person_trips_for_aeroclub_create_count(uow)
    assert person_trips_for_ac_create_count == [['person_trips_for_aeroclub_create_count_axxx', 0]]
    await _create_person_trip(f, aeroclub_trip_id=None)
    person_trips_for_ac_create_count = await get_person_trips_for_aeroclub_create_count(uow)
    assert person_trips_for_ac_create_count == [['person_trips_for_aeroclub_create_count_axxx', 1]]


async def test_not_authorized_person_trips_count(f, uow):
    not_authorized_person_trips_count = await get_not_authorized_person_trips_count(uow)
    assert not_authorized_person_trips_count == [['not_authorized_person_trips_count_axxx', 0]]
    await _create_person_trip(f, is_authorized=False, aeroclub_trip_id=1)
    not_authorized_person_trips_count = await get_not_authorized_person_trips_count(uow)
    assert not_authorized_person_trips_count == [['not_authorized_person_trips_count_axxx', 1]]


async def test_person_for_ihub_sync_count(f, uow):
    person_for_ihub_sync_count = await get_person_for_ihub_sync_count(uow)
    assert person_for_ihub_sync_count == [['person_for_ihub_sync_count_axxx', 0]]
    await _create_person_trip(f)
    person_for_ihub_sync_count = await get_person_for_ihub_sync_count(uow)
    assert person_for_ihub_sync_count == [['person_for_ihub_sync_count_axxx', 1]]


async def test_person_trips_without_chat_id_count(f, uow):
    person_trips_without_chat_id_count = await get_person_trips_without_chat_id_count(uow)
    assert person_trips_without_chat_id_count == [['person_trips_without_chat_id_count_axxx', 0]]
    await _create_person_trip(f)
    person_trips_without_chat_id_count = await get_person_trips_without_chat_id_count(uow)
    assert person_trips_without_chat_id_count == [['person_trips_without_chat_id_count_axxx', 1]]


def _build_job_result(**kwargs):
    return JobResult(
        args=tuple(),
        kwargs={},
        score=None,
        result=None,
        queue_name='arq:stage_queue',
        job_id='job_id',
        **kwargs
    )


def _get_dt(seconds):
    return datetime(2022, 1, 1) + timedelta(seconds=seconds)


async def test_arq_stats(f, uow):
    job_results = [
        _build_job_result(
            function='kek',
            success=True,
            job_try=1,
            enqueue_time=_get_dt(0),
            start_time=_get_dt(1),
            finish_time=_get_dt(2),
        ),
        _build_job_result(
            function='kek',
            success=True,
            job_try=2,
            enqueue_time=_get_dt(1),
            start_time=_get_dt(3),
            finish_time=_get_dt(4),
        ),
        _build_job_result(
            function='lol',
            success=True,
            job_try=1,
            enqueue_time=_get_dt(2),
            start_time=_get_dt(5),
            finish_time=_get_dt(7),
        ),
        _build_job_result(
            function='kek',
            success=False,
            job_try=1,
            enqueue_time=_get_dt(3),
            start_time=_get_dt(7),
            finish_time=_get_dt(9),
        ),
    ]
    data = build_arq_stats_response(
        job_results=job_results,
        queued_count=3,
    )
    assert data == {
        'queue_name': 'arq:stage_queue',
        'queued_count': 3,
        'counts': {
            'total': 4,
            'by_success': {
                True: 3,
                False: 1,
            },
            'by_function': {
                'kek': 3,
                'lol': 1,
            },
            'by_tries': {
                1: 3,
                2: 1,
            },
        },
        'timings': {
            'delay': {
                'min': 1.0,
                'max': 4.0,
                'avg': 2.5,
                'median': 3.0,
            },
            'execution': {
                'min': 1.0,
                'max': 2.0,
                'avg': 1.5,
                'median': 2.0,
            },
        },
    }


async def test_empty_arg_stats(f):
    data = build_arq_stats_response(
        job_results=[],
        queued_count=0,
    )
    assert data == {
        'queue_name': 'arq:stage_queue',
        'queued_count': 0,
        'counts': {
            'total': 0,
            'by_success': {},
            'by_function': {},
            'by_tries': {},
        },
        'timings': {
            'delay': {},
            'execution': {},
        },
    }
