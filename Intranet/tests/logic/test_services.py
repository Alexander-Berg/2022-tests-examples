import mock
import pytest

from intranet.trip.src.logic.services import DeleteAction
from intranet.trip.src.enums import ServiceStatus


pytestmark = pytest.mark.asyncio


async def _create_person_trip(f, trip_id, person_id, is_approved=False):
    await f.create_person(person_id=person_id)
    await f.create_trip(trip_id=trip_id)
    await f.create_person_trip(
        trip_id=trip_id,
        person_id=person_id,
        aeroclub_journey_id=1,
        aeroclub_trip_id=1,
        is_authorized=True,
        is_approved=is_approved,
    )


@pytest.mark.parametrize('service_status, is_person_trip_approved, should_be_available', (
    (ServiceStatus.draft, True, True),
    (ServiceStatus.draft, False, True),
    (ServiceStatus.verification, True, True),
    (ServiceStatus.verification, False, True),
    (ServiceStatus.in_progress, True, True),
    (ServiceStatus.in_progress, False, True),
    (ServiceStatus.deleted, True, False),
    (ServiceStatus.deleted, False, False),
    (ServiceStatus.executed, True, True),
    (ServiceStatus.executed, False, True),
    (ServiceStatus.reserved, True, True),
    (ServiceStatus.reserved, False, True),
    (ServiceStatus.cancelled, True, False),
    (ServiceStatus.cancelled, False, False),
))
async def test_service_delete_action(
        f,
        uow,
        mock_redis_lock,
        service_status,
        is_person_trip_approved,
        should_be_available,
):
    with mock.patch(
        'intranet.trip.src.logic.aeroclub.services.RedisLock',
        return_value=mock_redis_lock,
    ):
        trip_id = person_id = service_id = 1
        await _create_person_trip(
            f=f,
            trip_id=trip_id,
            person_id=person_id,
            is_approved=is_person_trip_approved,
        )
        await f.create_service(
            service_id=service_id,
            trip_id=trip_id,
            person_id=person_id,
            status=service_status,
        )

        user = await uow.persons.get_user(person_id=1)
        action = await DeleteAction.init(
            uow=uow,
            user=user,
            service_id=1,
        )
        is_available = await action.is_available()
        assert is_available == should_be_available
