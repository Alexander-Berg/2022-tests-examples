import pytest
import mock

from datetime import datetime, timedelta

from intranet.trip.src.logic.triggers.drive import DriveTrigger

from ..mocks import async_return

pytestmark = pytest.mark.asyncio


async def create_person_trip(factory, date_from, date_to, is_active):
    await factory.create_city(city_id=1)
    await factory.create_person(person_id=1)
    await factory.create_trip(trip_id=1, author_id=1)
    await factory.create_person_trip(trip_id=1, person_id=1)
    await factory.create_travel_details(
        trip_id=1,
        person_id=1,
        city_id=1,
        taxi_date_from=date_from,
        taxi_date_to=date_to,
        is_drive_activated=is_active,
    )


async def test_inactive_state_not_changed(f, uow):
    today = datetime.now().date()
    date_from = today - timedelta(days=10)
    date_to = today - timedelta(days=7)

    await create_person_trip(
        factory=f,
        date_from=date_from,
        date_to=date_to,
        is_active=False,
    )

    pt = await uow.person_trips.get_detailed_person_trip(trip_id=1, person_id=1)
    trigger = DriveTrigger(uow, pt)
    with mock.patch(
        'intranet.trip.src.logic.triggers.drive.DriveGateway.activate',
    ) as activate_mock, mock.patch(
        'intranet.trip.src.logic.triggers.drive.DriveGateway.deactivate',
    ) as deactivate_mock:
        await trigger.execute()
        activate_mock.assert_not_called()
        deactivate_mock.assert_not_called()


async def test_active_state_not_changed(f, uow):
    today = datetime.now().date()
    date_from = today - timedelta(days=1)
    date_to = today + timedelta(days=1)

    await create_person_trip(
        factory=f,
        date_from=date_from,
        date_to=date_to,
        is_active=True,
    )

    pt = await uow.person_trips.get_detailed_person_trip(trip_id=1, person_id=1)
    trigger = DriveTrigger(uow, pt)
    with mock.patch(
        'intranet.trip.src.logic.triggers.drive.DriveGateway.activate',
    ) as activate_mock, mock.patch(
        'intranet.trip.src.logic.triggers.drive.DriveGateway.deactivate',
    ) as deactivate_mock:
        await trigger.execute()
        activate_mock.assert_not_called()
        deactivate_mock.assert_not_called()


async def test_activated(f, uow):
    today = datetime.now().date()
    date_from = today
    date_to = today + timedelta(days=7)

    await create_person_trip(
        factory=f,
        date_from=date_from,
        date_to=date_to,
        is_active=False,
    )

    pt = await uow.person_trips.get_detailed_person_trip(trip_id=1, person_id=1)
    trigger = DriveTrigger(uow, pt)
    with mock.patch.object(
        trigger,
        'activate',
        return_value=async_return(None),
    ) as activate_mock, mock.patch.object(
        trigger,
        'deactivate',
        return_value=async_return(None),
    ) as deactivate_mock:
        await trigger.execute()
        activate_mock.assert_called()
        deactivate_mock.assert_not_called()


async def test_deactivated(f, uow):
    today = datetime.now().date()
    date_from = today - timedelta(days=7)
    date_to = today - timedelta(days=1)

    await create_person_trip(
        factory=f,
        date_from=date_from,
        date_to=date_to,
        is_active=True,
    )

    pt = await uow.person_trips.get_detailed_person_trip(trip_id=1, person_id=1)
    trigger = DriveTrigger(uow, pt)
    with mock.patch.object(
        trigger,
        'activate',
        return_value=async_return(None),
    ) as activate_mock, mock.patch.object(
        trigger,
        'deactivate',
        return_value=async_return(None),
    ) as deactivate_mock:
        await trigger.execute()
        activate_mock.assert_not_called()
        deactivate_mock.assert_called()
