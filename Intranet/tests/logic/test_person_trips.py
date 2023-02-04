import pytest

from datetime import date
from pydantic import ValidationError

from intranet.trip.src.api.schemas import PersonTripUpdate, PersonTripPartialUpdate
from intranet.trip.src.enums import PTStatus, ConferenceParticiationType, Citizenship
from ..factories import date_from, date_to


pytestmark = pytest.mark.asyncio


async def get_action(uow):
    from intranet.trip.src.logic.person_trips import PersonTripCreateUpdateAction

    user = await uow.persons.get_user(person_id=1)
    action = await PersonTripCreateUpdateAction.init(
        uow=uow,
        user=user,
        trip_id=1,
        person_id=1,
    )
    return action


async def test_trip_create_wrong_purpose(uow, f):
    await f.create_person(person_id=1)
    person_trip_create = PersonTripUpdate(
        purposes=[999],
        documents=[1, 2],
        is_hidden=False,
        with_days_off=True,
    )
    action = await get_action(uow)
    with pytest.raises(ValidationError):
        await action.execute(person_trip_update=person_trip_create)


async def _get_person_trip_create(f, is_offline=False, is_belarus=False):
    await f.create_city(city_id=1)
    await f.create_purpose(purpose_id=1)
    aeroclub_company_id = None if is_offline else 1
    country = Citizenship.BY if is_belarus else Citizenship.RU
    await f.create_company(company_id=1, aeroclub_company_id=aeroclub_company_id, country=country)
    await f.create_person(person_id=1, company_id=1)
    await f.create_person_document(person_id=1, document_id=1)
    await f.create_trip(trip_id=1, provider_city_from_id=123, city_from='Moscow')
    return PersonTripUpdate(
        purposes=[1],
        documents=[1],
        is_hidden=False,
        with_days_off=True,
    )


@pytest.mark.parametrize('is_offline', (True, False))
async def test_person_trip_create(f, uow, is_offline):
    person_trip_create = await _get_person_trip_create(f, is_offline=is_offline)

    action = await get_action(uow)
    await action.execute(person_trip_update=person_trip_create)
    person_trip = await uow.person_trips.get_detailed_person_trip(trip_id=1, person_id=1)

    assert person_trip.status == PTStatus.new
    assert len(person_trip.purposes) == 1
    assert len(person_trip.documents) == 1
    assert person_trip.city_from == 'Moscow'
    assert person_trip.provider_city_from_id == '123'
    assert person_trip.aeroclub_city_from_id == 123
    assert person_trip.is_offline is is_offline


async def test_person_trip_update(f, uow):
    await f.create_city(city_id=1)
    await f.create_person(person_id=1)
    await f.create_purpose(purpose_id=1)
    await f.create_purpose(purpose_id=2)
    await f.create_person_document(person_id=1, document_id=1)
    await f.create_trip(
        trip_id=1,
        person_ids=[1], purpose_ids=[1],
        date_from=date_from,
        date_to=date_to,
    )
    await f.create_trip_purpose(trip_id=1, purpose_id=1)
    await f.create_trip_purpose(trip_id=1, purpose_id=2)
    person_trip_update = PersonTripUpdate(
        purposes=[],
        documents=[],
        is_hidden=True,
        with_days_off=False,
    )
    action = await get_action(uow)
    await action.execute(person_trip_update=person_trip_update)

    person_trip = await uow.person_trips.get_detailed_person_trip(trip_id=1, person_id=1)
    assert person_trip.status == PTStatus.new
    assert len(person_trip.purposes) == 2
    purpose_ids = {p.purpose_id for p in person_trip.purposes}
    assert purpose_ids == {1, 2}
    assert person_trip.is_hidden is True
    assert person_trip.with_days_off is False
    assert person_trip.gap_date_from == date_from
    assert person_trip.gap_date_to == date_to


async def test_person_trip_revival(f, uow):
    await f.create_city(city_id=1)
    await f.create_person(person_id=1)
    await f.create_purpose(purpose_id=1)
    await f.create_purpose(purpose_id=2)
    await f.create_person_document(person_id=1, document_id=1)
    await f.create_trip(trip_id=1)
    await f.create_person_trip(
        trip_id=1,
        person_id=1,
        status=PTStatus.cancelled,
        is_hidden=False,
        with_days_off=True,
    )
    await f.create_person_trip_purpose(purpose_id=1, trip_id=1, person_id=1)

    person_trip_update = PersonTripUpdate(
        purposes=[2],
        documents=[1],
        is_hidden=True,
        with_days_off=False
    )
    action = await get_action(uow)
    await action.execute(person_trip_update=person_trip_update)
    person_trip = await uow.person_trips.get_detailed_person_trip(trip_id=1, person_id=1)
    assert person_trip.status == PTStatus.new
    assert len(person_trip.purposes) == 1
    assert person_trip.purposes[0].purpose_id == 2
    assert person_trip.is_hidden is True
    assert person_trip.with_days_off is False


async def create_test_conf(
    f,
    with_person: bool = True,
    **conf_details_fields
):
    """Создаёт в базе person_trip и сотрудника со айдишниками 1"""
    trip_id = 1
    person_id = 1
    purpose_id = 1
    await f.create_city(city_id=1)
    await f.create_person(person_id=person_id)
    await f.create_purpose(purpose_id=purpose_id)
    await f.create_trip(trip_id=trip_id, purpose_ids=[purpose_id])
    await f.create_person_document(person_id=person_id, document_id=1)

    if with_person:
        await f.create_person_trip(trip_id=trip_id, person_id=person_id)
        await f.create_person_trip_purpose(purpose_id, trip_id, person_id)
        await f.create_person_conf_details(
            trip_id=trip_id,
            person_id=person_id,
            role=ConferenceParticiationType.speaker,
            **conf_details_fields,
        )
    return trip_id, person_id, purpose_id


def _get_person_trip_update(**update_conf_data):
    return PersonTripUpdate(
        purposes=[1],
        travel_details=None,
        conf_details={
            'price': '0',
            'promo_code': '-',
            'discount': '-',
            **update_conf_data
        },
        documents=[1],
        is_hidden=False,
        with_days_off=False,
    )


@pytest.mark.parametrize('is_creation, role, need_job', (
    (False, ConferenceParticiationType.listener, True),
    (True, ConferenceParticiationType.listener, True),
    (False, ConferenceParticiationType.speaker, False),
))
async def test_person_trip_update_conf_role(f, uow, is_creation, role, need_job):
    await create_test_conf(f=f, with_person=not is_creation)
    person_trip_update = _get_person_trip_update(role=role)
    action = await get_action(uow)
    await action.execute(person_trip_update=person_trip_update)
    update_issue_job = (
        'update_conf_role_in_tracker_issue_task', {'person_id': 1, 'trip_id': 1},
    )
    job_run = update_issue_job in uow._jobs
    assert job_run == need_job


@pytest.mark.parametrize('is_creation, topic, need_job', (
    (False, 'Другая тема доклада', True),
    (True, 'Другая тема доклада', True),
    (False, 'Тема доклада', False),
))
async def test_person_trip_update_presentation_topic(f, uow, is_creation, topic, need_job):
    await create_test_conf(
        f=f,
        with_person=not is_creation,
        presentation_topic='Тема доклада',
        is_hr_approved=False,
    )
    person_trip_update = _get_person_trip_update(
        role=ConferenceParticiationType.speaker,
        presentation_topic=topic,
    )
    action = await get_action(uow)
    await action.execute(person_trip_update=person_trip_update)
    update_issue_job = (
        'update_speaker_details_in_tracker_issue_task', {'person_id': 1, 'trip_id': 1},
    )
    job_run = update_issue_job in uow._jobs
    assert job_run == need_job


@pytest.mark.parametrize('is_creation, is_hr_approved, need_job', (
    (False, True, True),
    (True, True, True),
    (False, False, False),
))
async def test_person_trip_update_is_hr_approved(f, uow, is_creation, is_hr_approved, need_job):
    await create_test_conf(
        f=f,
        with_person=not is_creation,
        presentation_topic='Тема доклада',
        is_hr_approved=False,
    )
    person_trip_update = _get_person_trip_update(
        presentation_topic='Тема доклада',
        role=ConferenceParticiationType.speaker,
        is_hr_approved=is_hr_approved,
    )
    action = await get_action(uow)
    await action.execute(person_trip_update=person_trip_update)
    update_issue_job = (
        'update_speaker_details_in_tracker_issue_task', {'person_id': 1, 'trip_id': 1},
    )
    job_run = update_issue_job in uow._jobs
    assert job_run == need_job


@pytest.mark.parametrize('is_creation, is_paid_by_host, need_job', (
    (False, False, True),
    (True, False, True),
    (False, True, False),
))
async def test_person_trip_bribe_warning(f, uow, is_creation, is_paid_by_host, need_job):
    await create_test_conf(
        f=f,
        with_person=not is_creation,
        presentation_topic='Тема доклада',
        is_hr_approved=False,
        is_paid_by_host=is_paid_by_host,
    )
    person_trip_update = _get_person_trip_update(
        role=ConferenceParticiationType.speaker,
        is_paid_by_host=True,
    )
    action = await get_action(uow)
    await action.execute(person_trip_update=person_trip_update)
    update_issue_job = (
        'update_bribe_warning_in_tracker_issue_task', {'person_id': 1, 'trip_id': 1},
    )
    job_run = update_issue_job in uow._jobs
    assert job_run == need_job


async def test_person_trip_partial_update(f, uow):
    gap_date_from = date(2022, 1, 1)
    gap_date_to = date(2022, 2, 2)

    await f.create_city(city_id=1)
    await f.create_purpose(purpose_id=1)
    await f.create_company(company_id=1, aeroclub_company_id=1)
    await f.create_person(person_id=1, company_id=1)
    await f.create_person_document(person_id=1, document_id=1)
    await f.create_trip(trip_id=1, provider_city_from_id=123, city_from='Moscow')
    await f.create_conf_details(trip_id=1, is_another_city=True)
    await f.create_person_trip(
        trip_id=1,
        person_id=1,
        gap_date_from=gap_date_from,
        gap_date_to=gap_date_to,
    )
    await f.create_travel_details(
        trip_id=1,
        person_id=1,
    )
    person_trip_update = PersonTripPartialUpdate(
        conf_details={
            'price': '0',
            'promo_code': '-',
            'discount': '-',
            'role': ConferenceParticiationType.listener,
        },
    )

    action = await get_action(uow)
    await action.execute(person_trip_update=person_trip_update, partial_update=True)

    person_trip = await uow.person_trips.get_detailed_person_trip(trip_id=1, person_id=1)

    assert person_trip.conf_details.is_another_city is True
    assert person_trip.conf_details.role == ConferenceParticiationType.listener
    assert person_trip.gap_date_from == gap_date_from
    assert person_trip.gap_date_to == gap_date_to
