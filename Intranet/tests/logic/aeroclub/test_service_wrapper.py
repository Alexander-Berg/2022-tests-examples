import pytest

from intranet.trip.src.logic.aeroclub.services import ServiceWrapper
from intranet.trip.src.enums import ServiceStatus
from intranet.trip.src.lib.aeroclub.enums import ServiceState


pytestmark = pytest.mark.asyncio


async def _create_service(f, uow, **fields):
    await f.create_person(person_id=1)
    await f.create_trip(trip_id=1)
    await f.create_person_trip(trip_id=1, person_id=1)
    await f.create_service(service_id=1, trip_id=1, person_id=1, **fields)
    service = await uow.services.get_service(service_id=1)
    return service


@pytest.mark.parametrize('service_state, status, in_process_of_cancelling, fields', (
    (
        ServiceState.unknown,
        ServiceStatus.deleted,
        False,
        {},
    ),
    (
        ServiceState.unknown,
        ServiceStatus.deleted,
        True,
        {},
    ),
    (
        ServiceState.unknown,
        ServiceStatus.draft,
        False,
        {},
    ),
    (
        ServiceState.unknown,
        ServiceStatus.draft,
        True,
        {},
    ),
    (
        ServiceState.unknown,
        ServiceStatus.in_progress,
        False,
        {},
    ),
    (
        ServiceState.unknown,
        ServiceStatus.in_progress,
        True,
        {},
    ),
    (
        ServiceState.unknown,
        None,
        False,
        {'is_broken': False, 'status': ServiceStatus.in_progress},
    ),
    (
        ServiceState.unknown,
        None,
        True,
        {'is_broken': False, 'status': ServiceStatus.in_progress},
    ),
    (
        ServiceState.execution,
        ServiceStatus.in_progress,
        False,
        {'status': ServiceStatus.executed},
    ),
    (
        ServiceState.execution,
        ServiceStatus.in_progress,
        True,
        {'status': ServiceStatus.executed},
    ),
    (
        ServiceState.execution,
        ServiceStatus.deleted,
        False,
        {'status': ServiceStatus.executed},
    ),
    (
        ServiceState.execution,
        ServiceStatus.deleted,
        True,
        {'status': ServiceStatus.executed},
    ),
    (
        ServiceState.execution,
        ServiceStatus.draft,
        False,
        {'status': ServiceStatus.executed},
    ),
    (
        ServiceState.execution,
        ServiceStatus.draft,
        True,
        {'status': ServiceStatus.executed},
    ),
    (
        ServiceState.execution,
        ServiceStatus.executed,
        False,
        {},
    ),
    (
        ServiceState.execution,
        ServiceStatus.executed,
        True,
        {},
    ),
    (
        ServiceState.servicing,
        ServiceStatus.in_progress,
        False,
        {'is_broken': True},
    ),
    (
        ServiceState.servicing,
        ServiceStatus.in_progress,
        True,
        {'is_broken': True},
    ),
    (
        ServiceState.servicing,
        ServiceStatus.reserved,
        False,
        {'is_broken': True},
    ),
    (
        ServiceState.servicing,
        ServiceStatus.reserved,
        True,
        {'is_broken': True},
    ),
    (
        ServiceState.servicing,
        ServiceStatus.draft,
        False,
        {'is_broken': True, 'status': ServiceStatus.in_progress},
    ),
    (
        ServiceState.servicing,
        ServiceStatus.draft,
        True,
        {'is_broken': True},
    ),
    (
        ServiceState.refunding,
        ServiceStatus.in_progress,
        False,
        {'status': ServiceStatus.cancelled},
    ),
    (
        ServiceState.refunding,
        ServiceStatus.in_progress,
        True,
        {'status': ServiceStatus.cancelled},
    ),
    (
        ServiceState.refunding,
        ServiceStatus.draft,
        False,
        {'status': ServiceStatus.cancelled},
    ),
    (
        ServiceState.refunding,
        ServiceStatus.draft,
        True,
        {'status': ServiceStatus.cancelled},
    ),
    (
        ServiceState.rejected,
        ServiceStatus.in_progress,
        False,
        {'is_broken': True},
    ),
    (
        ServiceState.rejected,
        ServiceStatus.in_progress,
        True,
        {'is_broken': True},
    ),
    (
        ServiceState.rejected,
        ServiceStatus.draft,
        False,
        {'is_broken': True, 'status': ServiceStatus.in_progress},
    ),
    (
        ServiceState.rejected,
        ServiceStatus.draft,
        True,
        {'is_broken': True, 'status': ServiceStatus.in_progress},
    ),
    (
        ServiceState.expired,
        ServiceStatus.in_progress,
        False,
        {'is_broken': True, 'status': ServiceStatus.cancelled},
    ),
    (
        ServiceState.expired,
        ServiceStatus.in_progress,
        True,
        {'is_broken': True, 'status': ServiceStatus.cancelled},
    ),
    (
        ServiceState.expired,
        ServiceStatus.draft,
        False,
        {'is_broken': True, 'status': ServiceStatus.cancelled},
    ),
    (
        ServiceState.expired,
        ServiceStatus.draft,
        True,
        {'is_broken': True, 'status': ServiceStatus.cancelled},
    ),
    (
        ServiceState.no_places,
        ServiceStatus.in_progress,
        False,
        {'is_broken': True, 'status': ServiceStatus.cancelled},
    ),
    (
        ServiceState.no_places,
        ServiceStatus.in_progress,
        True,
        {'is_broken': True, 'status': ServiceStatus.cancelled},
    ),
    (
        ServiceState.no_places,
        ServiceStatus.draft,
        False,
        {'is_broken': True, 'status': ServiceStatus.cancelled},
    ),
    (
        ServiceState.no_places,
        ServiceStatus.draft,
        True,
        {'is_broken': True, 'status': ServiceStatus.cancelled},
    ),
    (
        ServiceState.exchanged,
        ServiceStatus.in_progress,
        False,
        {'is_broken': True, 'status': ServiceStatus.cancelled},
    ),
    (
        ServiceState.exchanged,
        ServiceStatus.in_progress,
        True,
        {'is_broken': True, 'status': ServiceStatus.cancelled},
    ),
    (
        ServiceState.exchanged,
        ServiceStatus.draft,
        False,
        {'is_broken': True, 'status': ServiceStatus.cancelled},
    ),
    (
        ServiceState.exchanged,
        ServiceStatus.draft,
        True,
        {'is_broken': True, 'status': ServiceStatus.cancelled},
    ),
))
async def test_service_wrapper_update_fields(
        f,
        uow,
        service_state,
        status,
        in_process_of_cancelling,
        fields,
):
    aeroclub_service = {
        'service_state': service_state,
    }
    service = None
    if status is not None:
        service = await _create_service(
            f,
            uow,
            status=status,
            in_process_of_cancelling=in_process_of_cancelling,
        )
    wrapper = ServiceWrapper(service=service, aeroclub_service=aeroclub_service)
    assert fields == wrapper.get_update_fields()


async def test_service_wrapper_not_in_db():
    aeroclub_service = {
        'service_state': ServiceState.execution,
    }
    wrapper = ServiceWrapper(service=None, aeroclub_service=aeroclub_service)
    assert not wrapper.in_db
    assert wrapper.get_update_fields() == {
        'is_broken': False,
        'status': ServiceStatus.executed,
    }


async def test_service_wrapper_missing_aeroclub(f, uow):
    service = await _create_service(f, uow)
    wrapper = ServiceWrapper(service=service, aeroclub_service=None)
    assert wrapper.get_update_fields() == {
        'status': ServiceStatus.deleted,
    }
