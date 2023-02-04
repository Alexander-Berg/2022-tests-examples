import datetime
import pytest
from copy import deepcopy
from mock import patch
from intranet.trip.src.enums import (
    TripStatus,
    PTStatus,
    Gender,
    PurposeKind,
    DocumentType,
    Citizenship,
    TripType,
    ConferenceParticiationType,
)
from intranet.trip.src.lib.staff.sync import StaffTripPush
from intranet.trip.src.lib.staff.converters import TripToStaffConverter

from ...mocks import StaffApiGatewayMock
from ...factories import date_from, date_to, date_from_str, date_middle_str, date_to_str


pytestmark = pytest.mark.asyncio

person_id = 123
trip_id = 234
company_id = 1
document_id = 345
purpose_id = 456
city_id = 777
assignment_value = '30100'


purposes_fields = [
    {
        'name': 'Конференция',
        'name_en': 'Conference',
        'kind': PurposeKind.any.name,
        'aeroclub_grade': 16,
    },
    {
        'name': 'Поездка',
        'name_en': 'Journey',
        'kind': PurposeKind.any.name,
        'aeroclub_grade': 16,
    },
]

person_fields = {
    'gender': Gender.female.name,
    'first_name': 'Ваня',
    'last_name': 'Усович',
    'middle_name': 'Смешнович',
    'first_name_en': 'Vanya',
    'last_name_en': 'Usovitch',
    'middle_name_en': '',
    'uid': '123',
    'external_uid': '5550f26d-2728-49c4-b4c8-0b2b30b42555',
    'date_of_birth': datetime.date(year=1988, month=6, day=17),
    'phone_number': '+79250475555',
}

document_fields = {
    'document_type': DocumentType.passport.name,
    'number': '32254',
    'series': 'HB',
    'citizenship': Citizenship.BY.name,
    'first_name': 'Ivan',
    'last_name': 'Usovitch',
    'middle_name': 'Petrovitch',
    'issued_on': datetime.date.today() - datetime.timedelta(days=365),
    'expires_on': datetime.date.today() + datetime.timedelta(days=365 * 4),
}

person_trip_fields = {
    'status': PTStatus.new,
    'gap_date_from': date_from,
    'gap_date_to': date_to,
    'is_hidden': False,
    'with_days_off': False,
    'city_from': 'Одесса',
    'need_visa': True,
}

trip_fields = {
    'status': TripStatus.new,
    'date_from': date_from,
    'date_to': date_to,
    'city_from': 'Воркута',
    'city_to': 'Ванкувер',
    'country_from': 'Россия',
    'country_to': 'Канада',
    'comment': 'trip_comment',
    'issue_travel': 'TRAVEL-123456',
}

travel_details_fields = {
    'tracker_issue': 'ISSUE-55555',
    'is_created_on_provider': False,
    'need_visa_assistance': True,
    'taxi_date_from': date_from,
    'taxi_date_to': date_to,
    'is_taxi_activated': True,
    'is_drive_activated': True,
    'taxi_access_phone': '+79256788765',
    'comment': 'travel_comment',
}

person_conf_details_fields = {
    'role': ConferenceParticiationType.listener.name,
    'tracker_issue': 'INTERCONF-3333',
    'comment': 'person_conf_comment',
}

route_info = [{
    'city': 'Воркута',
    'country': 'Россия',
    'provider_city_id': '1',
    'date': date_from_str,
    'need_hotel': True,
    'need_transfer': True,
}, {
    'city': 'Веллингтон',
    'country': 'Новая Зеландия',
    'provider_city_id': '2',
    'date': date_middle_str,
    'need_hotel': True,
    'need_transfer': True,
}, {
    'city': 'Ванкувер',
    'country': 'Канада',
    'provider_city_id': '3',
    'date': date_to_str,
    'need_hotel': True,
    'need_transfer': True,
}]


async def mocked_tvm_service_ticket(service_name: str) -> str:
    return 'tvm2serviceticket'


async def mocked_tvm_headers_getter(service_name: str) -> dict:
    return {
        'X-Ya-Service-Ticket': 'tvm2serviceticket'
    }


async def mocked_assignments(self, logins: list[str]) -> dict[str, dict]:
    return {
        login: {
            'choices': [
                {
                    'name': 'some',
                    'value': assignment_value
                },
            ],
        }
        for login in logins
    }


async def _create_trip(f, is_complex=False):
    await f.create_company(company_id=company_id)
    await f.create_purpose(purpose_id=purpose_id, **purposes_fields[0])
    await f.create_person(person_id=person_id, company_id=company_id, **person_fields)
    await f.create_city(city_id=city_id)
    await f.create_person_document(document_id=document_id, person_id=person_id, **document_fields)
    route = route_info if is_complex else None
    await f.create_trip(
        trip_id=trip_id,
        author_id=person_id,
        purpose_ids=[purpose_id],
        route=route,
        **trip_fields,
    )
    await f.create_person_trip(
        trip_id=trip_id,
        person_id=person_id,
        document_id=document_id,
        route=route,
        **person_trip_fields,
    )
    await f.create_travel_details(
        trip_id=trip_id, person_id=person_id, city_id=city_id, **travel_details_fields,
    )


def get_expected_route():
    expected_route = deepcopy(route_info)
    for item in expected_route:
        item['city'] = {'name': {'ru': item['city'], 'en': None}}
        item['country'] = {'name': {'ru': item['country'], 'en': None}}
    return expected_route


async def _create_conf_trip(f):
    await _create_trip(f)
    await f.create_conf_details(trip_id=trip_id)
    await f.create_person_conf_details(
        trip_id=trip_id, person_id=person_id, **person_conf_details_fields,
    )


async def get_full_trip(f, uow, is_complex):
    await _create_trip(f, is_complex)

    staff_sync = await StaffTripPush.init(uow)
    trip = await uow.trips.get_trip_for_staff_push(trip_id=trip_id)
    return await staff_sync.enrich_trip(trip)


@pytest.fixture
async def trip_full(f, uow, client, patch_gateways):
    return await get_full_trip(f, uow, is_complex=False)


@pytest.fixture
async def trip_full_complex(f, uow, client, patch_gateways):
    return await get_full_trip(f, uow, is_complex=True)


@pytest.fixture
async def conf_trip_full(f, uow, client, patch_gateways):
    await _create_conf_trip(f)

    staff_sync = await StaffTripPush.init(uow)
    trip = await uow.trips.get_trip_for_staff_push(trip_id=trip_id)
    return await staff_sync.enrich_trip(trip)


@pytest.fixture
async def patch_gateways():
    with patch(
        'intranet.trip.src.api.auth.get_tvm_service_ticket',
        mocked_tvm_service_ticket,
    ), patch(
        'intranet.trip.src.lib.staff.sync.get_tvm_service_ticket',
        mocked_tvm_service_ticket,
    ), patch(
        'intranet.trip.src.lib.messenger.sync.get_tvm_service_ticket',
        mocked_tvm_service_ticket,
    ), patch(
        'intranet.trip.src.lib.staff.sync.StaffApiGateway',
        StaffApiGatewayMock,
    ), patch(
        'intranet.trip.src.lib.staff.gateway.StaffGateway.get_assignments',
        mocked_assignments,
    ):
        yield


async def test_trip_full(trip_full):
    assert trip_full.trip_id == trip_id
    assert trip_full.date_from == trip_fields['date_from']
    assert trip_full.date_to == trip_fields['date_to']
    assert trip_full.status == trip_fields['status']
    assert trip_full.city_from == trip_fields['city_from']
    assert trip_full.city_to == trip_fields['city_to']
    assert trip_full.issue_travel == trip_fields['issue_travel']
    assert trip_full.comment == trip_fields['comment']


async def test_trip_author(trip_full):
    author = trip_full.author

    assert author.person_id == person_id
    assert author.company_id == company_id
    assert author.uid == person_fields['uid']
    assert author.external_uid == person_fields['external_uid']
    assert author.first_name == person_fields['first_name']
    assert author.last_name == person_fields['last_name']
    assert author.middle_name == person_fields['middle_name']
    assert author.middle_name_en == person_fields['middle_name_en']
    assert author.first_name_en == person_fields['first_name_en']
    assert author.last_name_en == person_fields['last_name_en']
    assert author.gender == person_fields['gender']
    assert author.date_of_birth == person_fields['date_of_birth']
    assert author.phone_number == person_fields['phone_number']


async def test_person_trip(trip_full):
    person_trip = trip_full.person_trips[0]

    assert len(person_trip.documents) == 1
    assert person_trip.purposes == []
    assert person_trip.travel_details is not None
    assert person_trip.conf_details is None
    assert person_trip.avia_services == []
    assert person_trip.railroad_services == []
    assert person_trip.hotel_services == []
    assert person_trip.gap_date_from == person_trip_fields['gap_date_from']
    assert person_trip.gap_date_to == person_trip_fields['gap_date_to']
    assert person_trip.is_hidden == person_trip_fields['is_hidden']
    assert person_trip.with_days_off == person_trip_fields['with_days_off']
    assert person_trip.compensation_type is None
    assert person_trip.employee_assignment == assignment_value


async def test_travel_details(trip_full):
    travel_details = trip_full.person_trips[0].travel_details

    assert travel_details.city is None
    assert travel_details.tracker_issue == travel_details_fields['tracker_issue']
    assert travel_details.is_created_on_provider == travel_details_fields['is_created_on_provider']
    assert travel_details.need_visa_assistance == travel_details_fields['need_visa_assistance']
    assert travel_details.taxi_date_from == travel_details_fields['taxi_date_from']
    assert travel_details.taxi_date_to == travel_details_fields['taxi_date_to']
    assert travel_details.is_taxi_activated == travel_details_fields['is_taxi_activated']
    assert travel_details.is_drive_activated == travel_details_fields['is_drive_activated']
    assert travel_details.taxi_access_phone == travel_details_fields['taxi_access_phone']
    assert travel_details.comment == travel_details_fields['comment']


async def test_trip_purposes(trip_full):
    trip_purposes = trip_full.purposes

    assert len(trip_purposes) == 1
    assert trip_purposes[0].purpose_id == purpose_id
    assert trip_purposes[0].name == purposes_fields[0]['name']
    assert trip_purposes[0].name_en == purposes_fields[0]['name_en']
    assert trip_purposes[0].kind.name == purposes_fields[0]['kind']
    assert trip_purposes[0].aeroclub_grade == purposes_fields[0]['aeroclub_grade']


async def test_trip_document(trip_full):
    documents = trip_full.person_trips[0].documents

    assert len(documents) == 1
    document = documents[0]

    assert document.document_id == document_id
    assert document.document_type.name == document_fields['document_type']
    assert document.series == document_fields['series']
    assert document.number == document_fields['number']
    assert document.issued_on == document_fields['issued_on']
    assert document.expires_on == document_fields['expires_on']
    assert document.citizenship.name == document_fields['citizenship']
    assert document.first_name == document_fields['first_name']
    assert document.last_name == document_fields['last_name']
    assert document.middle_name == document_fields['middle_name']


async def test_trip_persons(trip_full):
    person_trips = trip_full.person_trips
    assert len(person_trips) == 1

    person = person_trips[0].person

    assert person.person_id == person_id
    assert person.company_id == company_id
    assert person.uid == person_fields['uid']
    assert person.external_uid == person_fields['external_uid']
    assert person.first_name == person_fields['first_name']
    assert person.last_name == person_fields['last_name']
    assert person.middle_name == person_fields['middle_name']
    assert person.middle_name_en == person_fields['middle_name_en']
    assert person.first_name_en == person_fields['first_name_en']
    assert person.last_name_en == person_fields['last_name_en']
    assert person.gender == person_fields['gender']
    assert person.date_of_birth == person_fields['date_of_birth']
    assert person.phone_number == person_fields['phone_number']


async def test_trip_route(trip_full_complex):
    route = trip_full_complex.route

    for actual, expected in zip(route, get_expected_route()):
        actual_dict = actual.dict()
        actual_dict['date'] = actual_dict['date'].strftime('%Y-%m-%d')
        expected['aeroclub_city_id'] = int(expected['provider_city_id'])
        assert expected == actual_dict


async def test_convert_trip_to_staff(trip_full_complex):
    assert trip_full_complex.event_type == TripType.trip

    staff_data = TripToStaffConverter(trip_full_complex).as_create_json()
    assert set(staff_data) == {
        'city_list',
        'employee_list',
        'receiver_side',
        'objective',
        'purpose',
    }
    assert len(staff_data['employee_list']) == 1

    assert staff_data['receiver_side'] == 'yandex'
    assert staff_data['objective'] == trip_full_complex.comment
    assert staff_data['purpose'] == [purpose_id]

    city_list = staff_data['city_list']
    assert len(city_list) == 4
    assert city_list[0]['city'] == city_list[-1]['city']
    assert city_list[0]['country'] == city_list[-1]['country']
    for index, point in enumerate(route_info):
        assert city_list[index]['city'] == point['city']
        assert city_list[index]['country'] == point['country']
        assert city_list[index]['departure_date'] == route_info[max(index - 1, 0)]['date']
    assert city_list[-1]['is_return_route'] is True
    assert route_info[-1]['date'] == city_list[-1]['departure_date']

    for city_data in city_list:
        # пока захардкоженные значения
        assert city_data['transport'] == 'aircraft'
        assert city_data['city_arrive_date_type'] == 'departure'  # всегда departue
        assert city_data['time_proposal'] == ''
        assert city_data['baggage'] == 'hand'  # дефолт ручная кладь, если есть сервис, то оттуда
        assert city_data['fare'] == 'most_economical'  # дефолт. Потом посмотрим.
        assert city_data['has_tickets'] is False
        assert city_data['tickets_cost'] == ''
        assert city_data['tickets_cost_currency'] == 'RUB'  # service_avia.currency
        assert city_data['car_rent'] is False  # Буду арендовать автомобиль
        assert city_data['need_hotel'] is True  # Нужна гостиница
        assert city_data['hotel'] == ''  # todo: из service_hotel
        assert city_data['ready_to_upgrade'] is False
        assert city_data['upgrade_comment'] == ''
        assert city_data['comment'] == ''

    person_data = staff_data['employee_list'][0]
    assert person_data == {
        'employee': person_id,
        'passport_number': f'{document_fields["series"]} {document_fields["number"]}',
        'passport_name': f'{document_fields["first_name"]} {document_fields["last_name"]}',
        'employee_assignment': assignment_value,
        'mobile_packages': False,
        'mobile_date_to': '',
        'mobile_date_from': '',
        'corporate_mobile_no': '',
        'need_mobile_additional_packages': '',
        'need_taxi': True,
        'mobile_number_for_taxi': travel_details_fields['taxi_access_phone'],
        'need_copy_of_insurance': False,
        'need_visa': True,
        'has_holidays': False,
        'holidays_comment': '',
        'compensation': None,
        'custom_dates': True,
        'departure_date': person_trip_fields['gap_date_from'].strftime('%Y-%m-%d'),
        'return_date': person_trip_fields['gap_date_to'].strftime('%Y-%m-%d'),
        'is_private': False,
        'trip_info': '',
        'interested_user_list': [],
        'comment': 'travel_comment',
    }


async def test_convert_trip_conf_to_staff(conf_trip_full):
    assert conf_trip_full.event_type == TripType.trip_conf

    staff_data = TripToStaffConverter(conf_trip_full).as_create_json()
    assert set(staff_data) == {
        'city_list',
        'employee_list',
        'event_name',
        'event_cost',
        'event_date_from',
        'event_date_to',
        'purpose',
        'objective',
        'receiver_side',
    }
    assert len(staff_data['employee_list']) == 1
    assert staff_data['receiver_side'] == 'yandex'
    assert staff_data['objective'] == conf_trip_full.conf_details.participation_terms
    assert staff_data['purpose'] == [purpose_id]

    assert staff_data['event_name'] == (
        'Конференция по новым компьютерным технологиям и защите компьютерных программ'
    )
    assert staff_data['event_cost'] == (
        "\nстоимость: 0\nпромокод: qwerty\n**Тип нужного билета**: test\n"
        "**Сайт**: https://yandex.ru\n**Программа мероприятия**: https://yandex.ru/program"
    )
    assert staff_data['event_date_from'] == date_from_str
    assert staff_data['event_date_to'] == date_to_str

    person_data = staff_data['employee_list'][0]

    assert person_data == {
        'employee': person_id,
        'event_role': ConferenceParticiationType.listener.name,
        'passport_number': f'{document_fields["series"]} {document_fields["number"]}',
        'passport_name': f'{document_fields["first_name"]} {document_fields["last_name"]}',
        'employee_assignment': assignment_value,
        'mobile_packages': False,
        'mobile_date_to': '',
        'mobile_date_from': '',
        'corporate_mobile_no': '',
        'need_mobile_additional_packages': '',
        'need_taxi': True,
        'mobile_number_for_taxi': travel_details_fields['taxi_access_phone'],
        'need_copy_of_insurance': False,
        'need_visa': True,
        'has_holidays': False,
        'holidays_comment': '',
        'compensation': None,
        'custom_dates': True,
        'departure_date': person_trip_fields['gap_date_from'].strftime('%Y-%m-%d'),
        'return_date': person_trip_fields['gap_date_to'].strftime('%Y-%m-%d'),
        'is_private': False,
        'trip_info': '',
        'interested_user_list': [],
        'comment': (
            'trip_comment\ntravel_comment\nperson_conf_comment\n'
            'Должность для бейджа: badge position\nИмя для бейджа: badge name'
        ),
    }


async def test_add_person_trip(f, trip_full, uow, patch_gateways):
    new_person_id = 122
    await f.create_person(person_id=new_person_id, company_id=company_id, **person_fields)
    await f.create_person_trip(
        trip_id=trip_id, person_id=new_person_id, document_id=document_id, **person_trip_fields,
    )
    await f.create_travel_details(
        trip_id=trip_id, person_id=new_person_id, city_id=city_id, **travel_details_fields,
    )
    staff_sync = await StaffTripPush.init(uow)
    trip = await uow.trips.get_trip_for_staff_push(trip_id=trip_id)
    trip = await staff_sync.enrich_trip(trip)
    assert [
        person_trip.person.person_id
        for person_trip in trip.person_trips
    ] == [person_id, new_person_id]
