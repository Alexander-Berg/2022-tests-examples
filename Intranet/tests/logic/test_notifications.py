import pytest

from intranet.trip.src.api.schemas import EventIn, EventServiceIn
from intranet.trip.src.enums import PTStatus
from intranet.trip.src.logic.notifications import process_notification


pytestmark = pytest.mark.asyncio


@pytest.mark.parametrize('event_in, pt_status, need_to_run_executor', (
    (
        EventIn(
            code='EventWithoutServiceInDraftTrip',
            aeroclub_journey_id=1,
            aeroclub_trip_id=1,
            services=None,
        ),
        PTStatus.draft,
        False,
    ),
    (
        EventIn(
            code='EventWithoutServiceInExecutingTrip',
            aeroclub_journey_id=2,
            aeroclub_trip_id=2,
            services=None,
        ),
        PTStatus.executing,
        False,
    ),
    (
        EventIn(
            code='EventWithServiceInDraftTrip',
            aeroclub_journey_id=3,
            aeroclub_trip_id=3,
            services=[
                EventServiceIn(
                    aeroclub_order_id=1,
                    aeroclub_service_id=1,
                ),
            ],
        ),
        PTStatus.draft,
        False,
    ),
    (
        EventIn(
            code='ExecutionStatusSucceeded',
            aeroclub_journey_id=4,
            aeroclub_trip_id=4,
            services=[
                EventServiceIn(
                    aeroclub_order_id=2,
                    aeroclub_service_id=2,
                ),
            ],
        ),
        PTStatus.executing,
        True,
    ),
    (
        EventIn(
            code='EventWithoutServiceInDraftTrip',
            provider_journey_id=1,
            provider_trip_id=1,
            services=None,
        ),
        PTStatus.draft,
        False,
    ),
    (
        EventIn(
            code='EventWithoutServiceInExecutingTrip',
            provider_journey_id=2,
            provider_trip_id=2,
            services=None,
        ),
        PTStatus.executing,
        False,
    ),
    (
        EventIn(
            code='EventWithServiceInDraftTrip',
            provider_journey_id=3,
            provider_trip_id=3,
            services=[
                EventServiceIn(
                    aeroclub_order_id=1,
                    aeroclub_service_id=1,
                ),
            ],
        ),
        PTStatus.draft,
        False,
    ),
    (
        EventIn(
            code='ExecutionStatusSucceeded',
            provider_journey_id=4,
            provider_trip_id=4,
            services=[
                EventServiceIn(
                    aeroclub_order_id=2,
                    aeroclub_service_id=2,
                ),
            ],
        ),
        PTStatus.executing,
        True,
    ),
))
async def test_process_notification(f, uow, event_in, pt_status, need_to_run_executor):
    await f.create_person()
    await f.create_trip()
    await f.create_person_trip(
        trip_id=1,
        person_id=1,
        aeroclub_journey_id=event_in.aeroclub_journey_id,
        aeroclub_trip_id=event_in.aeroclub_trip_id,
        is_authorized=True,
        status=pt_status,
    )
    if event_in.services:
        await f.create_service(
            trip_id=1,
            person_id=1,
            provider_order_id=event_in.services[0].aeroclub_order_id,
            provider_service_id=event_in.services[0].aeroclub_service_id,
        )

    await process_notification(uow, event_in)

    if need_to_run_executor:
        assert uow._jobs[0][0] == 'process_aeroclub_event_task'
    else:
        assert uow._jobs == []
