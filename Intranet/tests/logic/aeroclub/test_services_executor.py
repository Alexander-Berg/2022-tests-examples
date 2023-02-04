from typing import Optional
import pytest
from mock import patch
from intranet.trip.src.lib.aeroclub.exceptions import AeroclubWorkflowError, AeroclubBadDocument
from intranet.trip.src.lib.aeroclub.enums import ServiceState, ACServiceType

from intranet.trip.src.logic.aeroclub.services import AeroclubServicesExecutor
from intranet.trip.src.enums import ServiceStatus, ServiceType, PTStatus
from intranet.trip.src.lib.aeroclub.enums import AuthorizationStatus


pytestmark = pytest.mark.asyncio


PERSON_ID = 1
TRIP_ID = 1

AC_TRIP_DATA = {
    'authorization_token': 'abcdef',
    'authorization_status': 'Authorized',
    'services': [{
        'order_number': 1,
        'number': 1,
        'travellers': [],
        'available_actions': 'Reservation',
        'service_state': ServiceState.unknown,
    }],
}

AC_SERVICE_DATA = {
    'order_number': 1,
    'type': 'Avia',
    'number': 1,
    'documents': [
        {
            'attachments': [
                {
                    'url': 'https://fs.aeroclub.ru/v1/blob/123',
                    'name': 'билет.pdf',
                    'content_type': 'application/pdf',
                    'length': 584922,
                },
            ],
        },
    ],
}


AC_AUTHORIZATION_STATE_DATA = {
    'data': {
        'authorization_assertion': 'NotRequired',
        'authorization_status': 'Authorized',
        'authorizers': [],
    },
    'message': None,
    'errors': None,
    'is_success': True,
}


class FakeAeroclubClient:

    def __init__(
            self,
            broken_add_person_to_service: bool = False,
            broken_authorization_request: bool = False,
            authorization_token: Optional[str] = 'abcdef',
            authorization_status: AuthorizationStatus = AuthorizationStatus.authorized,
            aeroclub_trip_data: dict = None,
            aeroclub_service_data: dict = None,
            free_seats: list = None,
            broken_free_seats: bool = False,
    ):
        self.aeroclub_trip_data = aeroclub_trip_data or AC_TRIP_DATA
        self.aeroclub_service_data = aeroclub_service_data or AC_SERVICE_DATA
        self.broken_add_person_to_service = broken_add_person_to_service
        self.broken_authorization_request = broken_authorization_request
        self.authorization_status = authorization_status
        self.authorization_token = authorization_token
        self.free_seats = free_seats
        self.broken_free_seats = broken_free_seats

    async def get_trip(self, *args, **kwargs):
        data = self.aeroclub_trip_data
        data['authorization_token'] = self.authorization_token
        return data

    async def get_service(self, *args, **kwargs):
        return self.aeroclub_service_data

    async def add_person_to_service(self, *args, **kwargs):
        if self.broken_add_person_to_service:
            raise AeroclubBadDocument
        else:
            return {}

    async def reserve_service(self, *args, **kwargs):
        return {}

    async def get_services_authorization_state(self, *args, **kwargs):
        data = AC_AUTHORIZATION_STATE_DATA
        data['data']['authorization_status'] = self.authorization_status
        return data

    async def send_services_authorization_request(self, *args, **kwargs):
        if self.broken_authorization_request:
            raise AeroclubWorkflowError
        else:
            return {}

    async def authorize_trip(self, *args, **kwargs):
        return {}

    async def execute_services(self, *args, **kwargs):
        return {}

    async def get_service_custom_properties(self, *args, **kwargs):
        return {}

    async def add_service_custom_properties(self, *args, **kwargs):
        return {}

    async def get_profile(self, *args, **kwargs):
        return {}

    async def get_free_seats(self, *args, **kwargs):
        if self.broken_free_seats:
            return {
                'data': {
                    'carriage_details': {},
                },
            }
        data = {
            'data': {
                'carriage_details': [{
                    'places': []
                }],
            },
        }
        if not self.free_seats:
            return data
        places = [{'number': place} for place in self.free_seats]
        data['data']['carriage_details'][0]['places'] = places
        return data


async def _create_person_trip(f, **fields):
    await f.create_person(person_id=PERSON_ID)
    await f.create_trip(trip_id=TRIP_ID)
    fields = {
        'trip_id': TRIP_ID,
        'person_id': PERSON_ID,
        'status': PTStatus.executing,
        **fields,
    }
    await f.create_person_trip(**fields)


async def _create_service(f, service_id, **fields):
    fields = {
        'service_id': service_id,
        'person_id': PERSON_ID,
        'trip_id': TRIP_ID,
        'provider_order_id': service_id,
        'provider_service_id': service_id,
        **fields
    }
    await f.create_service(**fields)


async def get_executor(uow):
    return await AeroclubServicesExecutor.init(
        uow=uow,
        trip_id=TRIP_ID,
        person_id=PERSON_ID,
    )


async def test_check_person_trip_status(f, uow):
    await _create_person_trip(f, status=PTStatus.new)
    await _create_service(f, service_id=1)
    executor = await get_executor(uow)

    with pytest.raises(AeroclubWorkflowError):
        executor.check_person_trip()


async def test_check_missing_document_id(f, uow):
    await _create_person_trip(f)
    await _create_service(f, service_id=1, provider_document_id=None)
    executor = await get_executor(uow)
    executor.check_person_trip()


async def test_check_person_trip_ok(f, uow):
    await _create_person_trip(f)
    await _create_service(f, service_id=1)
    executor = await get_executor(uow)
    executor.check_person_trip()


async def test_executor_bad_document(f, uow, mock_redis_lock):
    with patch('intranet.trip.src.logic.aeroclub.services.RedisLock', return_value=mock_redis_lock):
        await _create_person_trip(f)
        await _create_service(f, service_id=1, status=ServiceStatus.in_progress)
        executor = await get_executor(uow)

        fake_aeroclub_client = FakeAeroclubClient(broken_add_person_to_service=True)
        with patch('intranet.trip.src.logic.aeroclub.services.aeroclub', fake_aeroclub_client):
            is_ok = await executor.run()

        assert is_ok is False
        service = await uow.services.get_service(service_id=1)
        assert service.status == ServiceStatus.in_progress
        assert service.provider_document_id is None


async def test_authorize_services_not_approved(f, uow):
    await _create_person_trip(f, is_approved=False)
    await _create_service(f, service_id=1, status=ServiceStatus.in_progress)

    executor = await get_executor(uow)
    with patch('intranet.trip.src.logic.aeroclub.services.aeroclub', FakeAeroclubClient()):
        with pytest.raises(AeroclubWorkflowError):
            await executor.load_aeroclub_trip()
            await executor.authorize_services()

    service = await uow.services.get_service(service_id=1)
    assert service.is_authorized is False


async def test_authorize_services_already_authorized(f, uow):
    await _create_person_trip(f)
    await _create_service(f, service_id=1, status=ServiceStatus.in_progress)

    executor = await get_executor(uow)
    fake_aeroclub_client = FakeAeroclubClient(authorization_status=AuthorizationStatus.authorized)
    with patch('intranet.trip.src.logic.aeroclub.services.aeroclub', fake_aeroclub_client):
        await executor.load_aeroclub_trip()
        await executor.authorize_services()


async def test_authorize_services_pending_then_error(f, uow):
    await _create_person_trip(f)
    await _create_service(f, service_id=1, status=ServiceStatus.in_progress)

    executor = await get_executor(uow)
    fake_aeroclub_client = FakeAeroclubClient(
        authorization_status=AuthorizationStatus.pending,
        broken_authorization_request=True,
    )
    with patch('intranet.trip.src.logic.aeroclub.services.aeroclub', fake_aeroclub_client):
        with pytest.raises(AeroclubWorkflowError):
            await executor.load_aeroclub_trip()
            await executor.authorize_services()

    service = await uow.services.get_service(service_id=1)
    assert service.is_authorized is False


async def test_authorize_services_missed_token(f, uow):
    await _create_person_trip(f)
    await _create_service(f, service_id=1, status=ServiceStatus.in_progress)

    executor = await get_executor(uow)
    fake_aeroclub_client = FakeAeroclubClient(
        authorization_status=AuthorizationStatus.pending,
        authorization_token=None,
    )
    with patch('intranet.trip.src.logic.aeroclub.services.aeroclub', fake_aeroclub_client):
        with pytest.raises(AeroclubWorkflowError):
            await executor.load_aeroclub_trip()
            await executor.authorize_services()

    service = await uow.services.get_service(service_id=1)
    assert service.is_authorized is False


async def test_authorize_services_pending_and_authorized(f, uow):
    await _create_person_trip(f)
    await _create_service(f, service_id=1, status=ServiceStatus.in_progress)

    executor = await get_executor(uow)

    fake_aeroclub_client = FakeAeroclubClient(
        aeroclub_trip_data={
            'authorization_token': None,
            'authorization_status': 'NotSupported',
            'services': [{
                'order_number': 1,
                'number': 1,
                'travellers': [{'id': 1}],
                'available_actions': 'Reservation',
                'service_state': ServiceState.unknown,
            }],
        },
        authorization_status=AuthorizationStatus.pending,
    )
    with patch('intranet.trip.src.logic.aeroclub.services.aeroclub', fake_aeroclub_client):
        await executor.load_aeroclub_trip()
        await executor.authorize_services()
    service = await uow.services.get_service(service_id=1)
    assert service.is_authorized is True


@pytest.mark.parametrize('service_state, status', (
    (ServiceState.execution, ServiceStatus.executed),
    (ServiceState.refunding, ServiceStatus.cancelled),
    (ServiceState.unknown, ServiceStatus.in_progress),
    (ServiceState.reservation, ServiceStatus.reserved),
))
async def test_update_in_db_one_service(f, uow, service_state, status):
    await _create_person_trip(f)
    await _create_service(f, service_id=1, status=ServiceStatus.in_progress)
    aeroclub_trip_data = {
        'authorization_token': 'abcdef',
        'authorization_status': 'Authorized',
        'services': [{
            'order_number': 1,
            'number': 1,
            'service_state': service_state,
        }],
    }
    fake_aeroclub_client = FakeAeroclubClient(aeroclub_trip_data=aeroclub_trip_data)
    executor = await get_executor(uow)
    with patch('intranet.trip.src.logic.aeroclub.services.aeroclub', fake_aeroclub_client):
        state = await executor.update_in_db()

    person_trip = await uow.person_trips.get_detailed_person_trip(TRIP_ID, PERSON_ID)
    assert len(person_trip.services) == 1
    service = person_trip.services[0]
    assert service.status == status
    assert state == {1: service_state}


async def test_update_in_db_delete_service(f, uow):
    await _create_person_trip(f)
    await _create_service(f, service_id=1, status=ServiceStatus.in_progress)
    aeroclub_trip_data = {
        'authorization_token': 'abcdef',
        'authorization_status': 'Authorized',
        'services': [],
    }
    fake_aeroclub_client = FakeAeroclubClient(aeroclub_trip_data=aeroclub_trip_data)
    executor = await get_executor(uow)
    with patch('intranet.trip.src.logic.aeroclub.services.aeroclub', fake_aeroclub_client):
        state = await executor.update_in_db()

    person_trip = await uow.person_trips.get_detailed_person_trip(TRIP_ID, PERSON_ID)
    assert len(person_trip.services) == 1
    assert person_trip.services[0].status == ServiceStatus.deleted
    assert state == {1: None}


async def test_update_in_db_already_delete_service(f, uow):
    await _create_person_trip(f)
    await _create_service(f, service_id=1, status=ServiceStatus.deleted)
    aeroclub_trip_data = {
        'authorization_token': 'abcdef',
        'authorization_status': 'Authorized',
        'services': [{
            'order_number': 1,
            'number': 1,
            'service_state': ServiceState.unknown,
        }],
    }
    fake_aeroclub_client = FakeAeroclubClient(aeroclub_trip_data=aeroclub_trip_data)
    executor = await get_executor(uow)
    with patch('intranet.trip.src.logic.aeroclub.services.aeroclub', fake_aeroclub_client):
        state = await executor.update_in_db()
    person_trip = await uow.person_trips.get_detailed_person_trip(TRIP_ID, PERSON_ID)
    assert len(person_trip.services) == 1
    assert person_trip.services[0].status == ServiceStatus.deleted
    assert state == {1: ServiceState.unknown}


async def test_update_in_db_create_service(f, uow):
    await _create_person_trip(f)
    aeroclub_trip_data = {
        'authorization_token': 'abcdef',
        'authorization_status': 'Authorized',
        'services': [{
            'order_number': 1,
            'number': 1,
            'service_state': ServiceState.execution,
            'type': ACServiceType.avia,
        }]
    }
    fake_aeroclub_client = FakeAeroclubClient(aeroclub_trip_data=aeroclub_trip_data)
    executor = await get_executor(uow)
    with patch('intranet.trip.src.logic.aeroclub.services.aeroclub', fake_aeroclub_client):
        state = await executor.update_in_db()

    person_trip = await uow.person_trips.get_detailed_person_trip(TRIP_ID, PERSON_ID)
    assert len(person_trip.services) == 1
    service = person_trip.services[0]
    assert service.status == ServiceStatus.executed
    assert service.is_broken is False
    assert service.provider_order_id == 1
    assert service.provider_service_id == 1
    assert service.is_from_provider is True
    assert service.type == ServiceType.avia
    assert state == {service.service_id: ServiceState.execution}


@pytest.mark.parametrize('ac_service_state, expected_db_status', (
    (ServiceState.no_places, ServiceStatus.cancelled),
    (ServiceState.expired, ServiceStatus.cancelled),
    (ServiceState.exchanged, ServiceStatus.cancelled),
    (ServiceState.rejected, ServiceStatus.in_progress),
))
async def test_update_in_db_no_places(f, uow, ac_service_state, expected_db_status):
    await _create_person_trip(f)
    await _create_service(f, service_id=1, status=ServiceStatus.in_progress)
    aeroclub_trip_data = {
        'authorization_token': 'abcdef',
        'authorization_status': 'Authorized',
        'services': [{
            'order_number': 1,
            'number': 1,
            'service_state': ac_service_state,
            'type': ACServiceType.avia,
        }]
    }
    fake_aeroclub_client = FakeAeroclubClient(aeroclub_trip_data=aeroclub_trip_data)
    executor = await get_executor(uow)
    with patch('intranet.trip.src.logic.aeroclub.services.aeroclub', fake_aeroclub_client):
        state = await executor.update_in_db()

    person_trip = await uow.person_trips.get_detailed_person_trip(TRIP_ID, PERSON_ID)
    assert len(person_trip.services) == 1
    service = person_trip.services[0]
    assert service.status == expected_db_status
    assert service.is_broken is True
    assert service.provider_order_id == 1
    assert service.provider_service_id == 1
    assert state == {service.service_id: ac_service_state}


@pytest.mark.parametrize(
    'free_seats, is_broken, expected_status, expected_seat_number', (
        ([11, 12, 13, 15], False, ServiceStatus.reserved, 13),
        ([11, 15], False, ServiceStatus.in_progress, None),
        (None, True, ServiceStatus.in_progress, None),
    ))
async def test_reserve_rail_service(
        f,
        uow,
        free_seats,
        is_broken,
        expected_status,
        expected_seat_number,
):
    seat_number = 13

    await _create_person_trip(f)
    await _create_service(
        f,
        service_id=1,
        status=ServiceStatus.in_progress,
        type=ServiceType.rail,
        seat_number=seat_number,
        is_authorized=True,
    )

    executor = await get_executor(uow)

    fake_aeroclub_client = FakeAeroclubClient(
        aeroclub_trip_data={
            'authorization_token': None,
            'authorization_status': 'NotSupported',
            'services': [{
                'order_number': 1,
                'number': 1,
                'travellers': [{'id': 1}],
                'available_actions': 'Reservation',
                'service_state': ServiceState.unknown,
            }],
        },
        authorization_status=AuthorizationStatus.pending,
        free_seats=free_seats,
        broken_free_seats=is_broken,
    )
    with patch('intranet.trip.src.logic.aeroclub.services.aeroclub', fake_aeroclub_client):
        await executor.reserve_rail_services()
    service = await uow.services.get_service(service_id=1)
    assert service.status == expected_status
    assert service.seat_number == expected_seat_number
    assert service.is_broken == is_broken
