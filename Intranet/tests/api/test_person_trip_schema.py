import pytest

from intranet.trip.src.config import settings
from intranet.trip.src.models import PersonTrip as PersonTripModel
from intranet.trip.src.api.schemas import PersonTrip as PersonTripSchema
from intranet.trip.src.enums import (
    PTStatus,
    Provider,
    CombinedStatus,
    ServiceStatus,
    ServiceType,
)


def _build_person_trip_model(**fields):
    fields['services'] = [
        {
            'trip_id': 1,
            'person_id': 1,
            'provider_order_id': 1,
            'provider_service_id': 1,
            'type': ServiceType.avia,
            'actions': {
                'add_document': True,
                'change_document': True,
                'remove_document': True,
                'reserve': True,
                'update': True,
                'delete': True,
            },
            **service,
        }
        for service in fields.pop('services', [])
    ]
    fields = {
        'trip_id': 1,
        'person_id': 1,
        'person': None,
        'provider': Provider.aeroclub,
        'status': PTStatus.new,
        'description': 'description',
        'with_days_off': True,
        'is_approved': True,
        'is_hidden': False,
        'is_offline': False,
        'need_visa': False,
        **fields,
    }
    return PersonTripModel(**fields)


def test_aeroclub_url_is_none():
    model = _build_person_trip_model()
    schema = PersonTripSchema(**model.dict())
    assert schema.aeroclub_url is None


def test_aeroclub_url_is_not_none():
    model = _build_person_trip_model(
        aeroclub_journey_id=1,
        aeroclub_trip_id=2,
    )
    schema = PersonTripSchema(**model.dict())
    assert schema.aeroclub_url == f'{settings.trip_base_url}/api/aeroclub/journeys/1/trips/2'


def test_services_is_none():
    model = _build_person_trip_model()
    schema = PersonTripSchema(**model.dict())
    assert schema.services == []


def test_services_without_deleted():
    model = _build_person_trip_model(
        services=[
            {
                'service_id': 1,
                'status': ServiceStatus.draft,
                'is_authorized': False,
            },
            {
                'service_id': 2,
                'status': ServiceStatus.in_progress,
                'is_authorized': True,
            },
            {
                'service_id': 3,
                'status': ServiceStatus.deleted,
                'is_authorized': False,
            },
        ],
    )
    schema = PersonTripSchema(**model.dict())
    service_ids = [service.service_id for service in schema.services]
    assert set(service_ids) == {1, 2}

    provider_service_ids = [service.provider_service_id for service in schema.services]
    assert set(provider_service_ids) == {1}

    provider_order_ids = [service.provider_order_id for service in schema.services]
    assert set(provider_order_ids) == {1}


@pytest.mark.parametrize('status, is_approved, combined_status', (
    (PTStatus.draft, False, CombinedStatus.draft),
    (PTStatus.new, False, CombinedStatus.under_approval),
    (PTStatus.new, True, CombinedStatus.approved),
    (PTStatus.verification, False, CombinedStatus.under_approval),
    (PTStatus.verification, True, CombinedStatus.verification),
    (PTStatus.executing, False, CombinedStatus.under_approval),
    (PTStatus.executing, True, CombinedStatus.executing),
    (PTStatus.executed, True, CombinedStatus.executed),
    (PTStatus.cancelled, True, CombinedStatus.cancelled),
    (PTStatus.closed, True, CombinedStatus.closed),
))
def test_combined_status(status, is_approved, combined_status):
    model = _build_person_trip_model(status=status, is_approved=is_approved)
    schema = PersonTripSchema(**model.dict())
    assert schema.combined_status == combined_status


@pytest.mark.parametrize('status, need_warning_on_cancel', (
    (PTStatus.draft, False),
    (PTStatus.new, False),
    (PTStatus.executing, True),
    (PTStatus.executed, True),
    (PTStatus.cancelled, False),
    (PTStatus.closed, False),
))
def test_need_warning_on_cancel(status, need_warning_on_cancel):
    model = _build_person_trip_model(status=status)
    schema = PersonTripSchema(**model.dict())
    assert schema.need_warning_on_cancel == need_warning_on_cancel
