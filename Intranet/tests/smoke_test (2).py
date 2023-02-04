import pytest

from datetime import date, datetime, timedelta

import json
from django.core.urlresolvers import reverse

from django.test import TestCase, RequestFactory
import mock

from staff.trip_questionary.controller.operations.registry import OperationsRegistry
from staff.trip_questionary.controller.operations.taxi import CreateTaxiAccount, RemoveTaxiAccount
from staff.trip_questionary.controller.operations.gap import CreateGap

from staff.trip_questionary.forms import TripHeadForm
from staff.trip_questionary.models import TripQuestionaryCollection

from staff.gap.controllers.gap import GapCtl
from staff.gap.workflows.choices import GAP_STATES as GS

from staff.lib.testing import (
    StaffFactory,
    UserFactory,
    OfficeFactory,
    CityFactory,
    CountryFactory,
)
from staff.trip_questionary.views import _get_initial, _get_trip

from staff.trip_questionary.tests.fixture import create_department_infrastructure


class TestPrefillFromGap(TestCase):
    factory = RequestFactory()

    def test_prefill_from_gap_en_date(self):
        data = {
            'employee': 'den-lesnov',
            'left_edge': '04.10.2013',
            'right_edge': '04.10.2013',
            'trip_info': 'Укажите как с вами лучше связаться '
                         'и к кому обратиться во время вашего отсутствия',
        }
        request = self.factory.post('/trip/trip', data=data, follow=True)
        request.user = UserFactory()
        office = OfficeFactory(
            city=CityFactory(
                name='city_name',
                country=CountryFactory(name='country_name')
            )
        )
        request.user.profile = StaffFactory(user=request.user,
                                            office=office,
                                            login='den-lesnov')
        result = _get_initial(request)

        self.assertDictContainsSubset(
            {'trip_date_from': date(2013, 10, 4)}, result
        )
        self.assertDictContainsSubset(
            {'trip_date_to': date(2013, 10, 4)}, result
        )
        self.assertIsNotNone(result.get('city_list'))
        self.assertEqual(
            result['city_list'][0]['city'],
            request.user.profile.office.city.name
        )
        self.assertEqual(
            result['city_list'][0]['country'],
            request.user.profile.office.city.country.name
        )
        self.assertDictContainsSubset(
            {'employee_list': [{'employee': request.user.profile}]}, result)


@pytest.mark.skip
@pytest.mark.django_db()
def test_trip_form_factory():
    """Тесты форм командировок"""
    from staff.trip_questionary.forms import TripConfHeadForm, TripHeadForm, ConfHeadForm

    staff_1 = StaffFactory(login='staff_1')
    staff_2 = StaffFactory(login='staff_2')

    data = {
        'purpose': [1, 3],
        'objective': 'Гульнуть',
        'trip_date_from': '2013-05-21',
        'trip_date_to': '2013-06-12',
        'event_date_from': '2013-05-22',
        'event_date_to': '2013-06-05',
        'comment': 'Все будет хорошо!',
        'event_cost': 'Стоимость',
        'event_name': 'Мероприятие',
        'city_list': [
            {
                'city': 'Питер',
                'country': 'Россия',
                'transport': 'aircraft',
                'departure_date': '2013-05-21',
                'need_hotel': '',
                'hotel': 'Роза',
                'comment': 'Городок',
            },
            {
                'city': 'Киев',
                'country': 'Украина',
                'transport': 'train',
                'departure_date': '2013-05-25',
                'need_hotel': 'True',
                'hotel': 'Фиалка',
                'comment': 'Городок 2',
            },
        ],
        'employee_list': [
            {
                'employee': staff_2.id,
                'passport_number': '3467 345432',
                'passport_name': 'bar bar bar',
                'mobile_packages': False,
                'need_mobile_additional_packages': '',
                'need_copy_of_insurance': '',
                'need_visa': 'False',
                'need_taxi': 'False',
                'departure_date': '',
                'return_date': '',
                'trip_info': 'Гульнуть красиво',
                'comment': '',
                'interested_user_list': [
                    {
                        'employee': 'dmirain@yandex-team.ru',
                    },
                    {
                        'employee': 'sergey-syrkin@yandex.ru',
                    },
                ],
            },
            {
                'employee': staff_1.id,
                'passport_number': '3467 345464',
                'passport_name': 'foo foo foo',
                'mobile_packages': False,
                'need_mobile_additional_packages': '',
                'need_copy_of_insurance': '',
                'need_visa': 'False',
                'need_taxi': 'False',
                'departure_date': '',
                'return_date': '',
                'trip_info': 'Гульнуть заметно',
                'comment': '',
                'interested_user_list': [
                    {
                        'employee': 'mixael@yandex-team.ru',
                    },
                    {
                        'employee': 'sibirev@yandex-team.ru',
                    },
                    {
                        'employee': 'tools-dev@yandex-team.ru',
                    },
                ],

            },
        ],

    }

    trip_form = TripConfHeadForm(data=data)
    assert trip_form.errors == {}  # Падает пока есть бага с обязательным mobile_number_for_taxi

    trip_form = TripHeadForm(data=data)
    assert trip_form.errors == {}

    trip_form = ConfHeadForm(data=data)
    assert trip_form.errors == {}


@pytest.mark.skip
@pytest.mark.django_db()
def test_trip_save():
    from staff.trip_questionary.views import _get_trip
    from staff.trip_questionary.controller.operations.registry import delay_diff_operations

    staff_1 = StaffFactory(login='staff_1')
    staff_2 = StaffFactory(login='staff_2')

    trip, event_type = _get_trip(None, 'trip_conf', author=None)

    data = {
        'author': staff_1,
        'trip_date_to': date(2013, 5, 25),
        'event_name': 'Мероприятие',
        'trip_date_from': date(2013, 5, 25),
        'event_date_from': date(2013, 5, 22),
        'event_date_to': date(2013, 6, 5),
        'event_cost': 'Стоимость',
        'city_list': [{
            'comment': 'Городок',
            'city': 'Питер',
            'departure_date': date(2013, 5, 21),
            'hotel': 'Роза',
            'is_return_route': False,
            'need_hotel': False,
            'transport': 'aircraft',
            'country': 'Россия',
            'city_arrive_date_type': 'departure'
        }, {
            'comment': 'Городок 2',
            'city': 'Киев',
            'departure_date': date(2013, 5, 25),
            'hotel': 'Фиалка',
            'is_return_route': False,
            'need_hotel': True,
            'transport': 'train',
            'country': 'Украина',
            'city_arrive_date_type': 'departure'
        }],
        'employee_list': [{
            'comment': '',
            'transfer': '',
            'has_holidays': False,
            'passport_number': '3467 345432',
            'mobile_date_to': None,
            'passport_name': 'bar bar bar',
            'custom_dates': False,
            'departure_date': None,
            'need_visa': False,
            'need_mobile_additional_packages': '',
            'corporate_mobile_no': '',
            'holidays_comment': '',
            'event_role': 'listener',
            'is_private': False,
            'mobile_packages': False,
            'employee': staff_1,
            'trip_info': 'Гульнуть красиво',
            'need_copy_of_insurance': False,
            'mobile_date_from': None,
            'return_date': None,
            'interested_user_list': [
                {'employee': 'dmirain@yandex-team.r'},
                {'employee': 'sergey-syrkin@yandex.r'}
            ]
        }, {
            'comment': '',
            'transfer': '',
            'has_holidays': False,
            'passport_number': '3467 345464',
            'mobile_date_to': None,
            'passport_name': 'foo foo foo',
            'custom_dates': False,
            'departure_date': None,
            'need_visa': False,
            'need_mobile_additional_packages': '',
            'corporate_mobile_no': '',
            'holidays_comment': '',
            'event_role': 'listener',
            'is_private': False,
            'mobile_packages': False,
            'employee': staff_2,
            'trip_info': 'Гульнуть заметно',
            'need_copy_of_insurance': False,
            'mobile_date_from': None,
            'return_date': None,
            'interested_user_list': [
                {'employee': 'mixael@yandex-team.r'},
                {'employee': 'sibirev@yandex-team.r'},
                {'employee': 'tools-dev@yandex-team.r'}
            ]
        }]
    }

    trip.update(data)
    delay_diff_operations(trip=trip)  # Падает при попытке получить токен для staff_1


class TestGetRecipients(TestCase):
    def setUp(self):
        create_department_infrastructure(self)

    def send(self, organizer, foreign=False):
        from staff.person.models import Staff
        from staff.person.dis_staff_services import get_chiefs_role_bulk
        from staff.trip_questionary.models import get_recipients

        staff_list = Staff.objects.select_related('department')
        chiefs_role = get_chiefs_role_bulk(staff_list, level_gte=1)
        with self.settings(
                DIS_DIRECTION_KIND_ID=self.dep_kind_lvl_2.id,
                DIS_DIVISION_KIND_ID=self.dep_kind_lvl_3.id,
        ):
            result = get_recipients(organizer, staff_list, chiefs_role)
        return result

    def check(self, result, correct_result, organizer):
        self.assertTrue(len(result) == len(correct_result))
        for k, v in result.items():
            self.assertSetEqual(set(v), set([r for r in correct_result[k] if r != organizer]))

    def get_not_foreign_result(self):
        return {
            self.chief_yandex: [],
            self.deputy_yandex: [],
            self.person_yandex: [],

            self.chief_subyandex: [],
            self.deputy_subyandex: [self.chief_subyandex],
            self.person_subyandex: [self.chief_subyandex],

            self.chief_direction_1: [self.chief_subyandex],
            self.deputy_direction_1: [self.chief_direction_1],
            self.person_direction_1: [self.chief_direction_1],

            self.chief_division_1: [self.chief_direction_1],
            self.deputy_division_1: [self.chief_division_1],
            self.person_division_1: [self.chief_division_1],

            # т.к. нет chief_regular_1, то непосредственный начальник и
            # начальник 2-го уровня (отдела) - одно лицо
            self.person_regular_1: [self.chief_division_1, ],

            self.chief_direction_2: [self.chief_subyandex],
            self.deputy_direction_2: [self.chief_direction_2],
            self.person_direction_2: [self.chief_direction_2],

            self.chief_division_2: [self.chief_direction_2],
            self.deputy_division_2: [self.chief_division_2],
            self.person_division_2: [self.chief_division_2],

            self.chief_regular_2: [self.chief_division_2],
            self.deputy_regular_2: [self.chief_division_2, self.chief_regular_2],
            self.person_regular_2: [self.chief_division_2, self.chief_regular_2],

            self.chief_division_3: [self.chief_direction_2],
            self.deputy_division_3: [self.chief_division_3],
            self.person_division_3: [self.chief_division_3],
        }

    def get_foreign_result(self):
        return {
            self.chief_yandex: [],
            self.deputy_yandex: [],
            self.person_yandex: [],

            self.chief_subyandex: [],
            self.deputy_subyandex: [self.chief_subyandex],
            self.person_subyandex: [self.chief_subyandex],

            self.chief_direction_1: [self.chief_subyandex],
            self.deputy_direction_1: [self.chief_direction_1],
            self.person_direction_1: [self.chief_direction_1],

            self.chief_division_1: [self.chief_direction_1],
            self.deputy_division_1: [self.chief_direction_1, self.chief_division_1],
            self.person_division_1: [self.chief_direction_1, self.chief_division_1],

            self.person_regular_1: [self.chief_direction_1, self.chief_division_1],

            self.chief_direction_2: [self.chief_subyandex],
            self.deputy_direction_2: [self.chief_direction_2],
            self.person_direction_2: [self.chief_direction_2],

            self.chief_division_2: [self.chief_direction_2],
            self.deputy_division_2: [self.chief_direction_2, self.chief_division_2],
            self.person_division_2: [self.chief_direction_2, self.chief_division_2],

            self.chief_regular_2: [self.chief_direction_2, self.chief_division_2],
            self.deputy_regular_2: [self.chief_direction_2, self.chief_regular_2],
            self.person_regular_2: [self.chief_direction_2, self.chief_regular_2],

            self.chief_division_3: [self.chief_direction_2],
            self.deputy_division_3: [self.chief_division_3, self.chief_direction_2],
            self.person_division_3: [self.chief_division_3, self.chief_direction_2],
        }

    @pytest.mark.skip
    def test_recipients_local_trip(self):
        from staff.person.models import Staff
        for organizer in Staff.objects.all():
            result = self.send(organizer)
            correct_result = self.get_not_foreign_result()
            self.check(result, correct_result, organizer=organizer)

    @pytest.mark.skip
    def test_recipients_foreign_trip(self):
        from staff.person.models import Staff
        for organizer in Staff.objects.all():
            result = self.send(organizer, foreign=True)
            correct_result = self.get_foreign_result()
            self.check(result, correct_result, organizer=organizer)


@pytest.mark.skip
@mock.patch('staff.trip_questionary.controller.operations.registry.OperationsCache')
def test_using_taxi(MockOperationCache, db, mocked_mongo, settings):

    d = mock.Mock()
    create_department_infrastructure(d)

    settings.DIS_DIRECTION_KIND_ID = d.dep_kind_lvl_2.id
    settings.DIS_DIVISION_KIND_ID = d.dep_kind_lvl_3.id
    settings.ST_TRAVEL_FIELD_ROUTE_TYPE = {'there': 350, 'complex': 352, 'there_and_backward': 351}
    settings.ST_TRAVEL_FIELD_TRANSPORT = {'bus': 355, 'aircraft': 353, 'train': 354, 'car': 356}

    # чтобы CreateTaxiAccount.match_preconditions вернул True
    trip_datetime_from = datetime.today() - timedelta(days=3)
    trip_datetime_to = datetime.today() + timedelta(days=3)

    admin = StaffFactory(login='admin', user=UserFactory(username='admin'))
    admin.user.is_superuser = True
    admin.user.save()

    trip = TripQuestionaryCollection().new(author=d.person_yandex)
    trip.data['event_type'] = 'trip'
    fake_trip_issue = {
        'startDate': '2017-11-27T00:00:00.000Z',
        'endDate': '2017-12-04T00:00:00.000Z',
        'taxiStartDate': trip_datetime_from,
        'taxiEndDate': trip_datetime_to,
        'assignmentID': 6976,
        'accounting': {
            'self': 'https://st-api.test.yandex-team.ru/v2/users/1120000000043689',
            'id': 'mirzalieva',
            'display': 'Ольга Мирзалиева'
        },
        'id': '5a155c19fc48b20020d6174b',
        'transport': {
            'self': 'https://st-api.test.yandex-team.ru/v2/translations/353',
            'id': '353',
            'display': 'Самолёт'
        },
        'votes': 0,
        'ticketOrHotelUpgrade': 'Нет',
        'self': 'https://st-api.test.yandex-team.ru/v2/issues/TRAVEL-37824',
        'createdAt': '2017-11-22T14:14:33.042Z',
        'department': 'Поисковый портал',
        'countryTo': 'Россия',
        'status': {
            'self': 'https://st-api.test.yandex-team.ru/v2/statuses/1',
            'id': '1',
            'key': 'checkIn',
            'display': 'Чек ин'
        },
        'lastCommentUpdatedAt': '2017-11-22T11:15:09.813+0000',
        'employee': {
            'self': 'https://st-api.test.yandex-team.ru/v2/users/1120000000000699',
            'id': 'vechernov',
            'display': 'Василий Чернов'
        },
        'hotelNeeded': 'Да',
        'purpose': 'kjhsdakjhlasdfkjlhsad',
        'key': 'TRAVEL-37824',
        'cityTo': 'Dar es Salaam',
        'routeType': {
            'self': 'https://st-api.test.yandex-team.ru/v2/translations/351',
            'id': '351',
            'display': 'Туда — обратно'
        },
        'countryFrom': 'Россия',
        'humanResources': {
            'self': 'https://st-api.test.yandex-team.ru/v2/users/1120000000024549',
            'id': 'kadykeeva',
            'display': 'Кристина Кадыкеева'
        },
        'favorite': False,
        'itinerary': 'Москва – Dar es Salaam – Москва',
        'cityFrom': 'Москва',
        'transportFrom': {
            'self': 'https://st-api.test.yandex-team.ru/v2/translations/353',
            'id': '353',
            'display': 'Самолёт'
        }
    }
    data = {'city_list': [{'baggage': 'hand',
                           'car_rent': False,
                           'city': 'Moscow',
                           'city_arrive_date_type': 'departure',
                           'comment': '',
                           'country': 'Russian Federation',
                           'departure_date': trip_datetime_to.date(),
                           'fare': 'most_economical',
                           'has_tickets': False,
                           'hotel': '',
                           'is_return_route': False,
                           'need_hotel': True,
                           'ready_to_upgrade': False,
                           'tickets_cost': None,
                           'tickets_cost_currency': 'RUB',
                           'time_proposal': '',
                           'transport': 'aircraft',
                           'upgrade_comment': ''},
                          {'baggage': 'hand',
                           'car_rent': False,
                           'city': 'Ekaterinovka',
                           'city_arrive_date_type': 'departure',
                           'comment': '',
                           'country': 'Russian Federation',
                           'departure_date': trip_datetime_from.date(),
                           'fare': 'most_economical',
                           'has_tickets': False,
                           'hotel': '',
                           'is_return_route': False,
                           'need_hotel': True,
                           'ready_to_upgrade': False,
                           'tickets_cost': None,
                           'tickets_cost_currency': 'RUB',
                           'time_proposal': '',
                           'transport': 'aircraft',
                           'upgrade_comment': ''},
                          {'baggage': 'hand',
                           'car_rent': False,
                           'city': 'Moscow',
                           'city_arrive_date_type': 'departure',
                           'comment': '',
                           'country': 'Russian Federation',
                           'departure_date': trip_datetime_to.date(),
                           'fare': 'most_economical',
                           'has_tickets': False,
                           'hotel': '',
                           'is_return_route': True,
                           'need_hotel': False,
                           'ready_to_upgrade': False,
                           'tickets_cost': None,
                           'tickets_cost_currency': 'RUB',
                           'time_proposal': '',
                           'transport': 'aircraft',
                           'upgrade_comment': '',
                           }],
            'comment': '',
            'employee_list': [{'comment': '',
                               'compensation': 'money',
                               'corporate_mobile_no': '',
                               'custom_dates': False,
                               'departure_date': None,
                               'employee': d.chief_yandex.id,
                               'employee_assignment': '451633',
                               'has_holidays': False,
                               'holidays_comment': '',
                               'interested_user_list': [],
                               'is_private': False,
                               'mobile_date_from': None,
                               'mobile_date_to': None,
                               'mobile_packages': False,
                               'need_copy_of_insurance': False,
                               'need_mobile_additional_packages': '',
                               'need_taxi': True,
                               'mobile_number_for_taxi': '+79250475556',
                               'need_visa': False,
                               'passport_name': 'dfsdfs',
                               'passport_number': '32423423',
                               'return_date': None,
                               'transfer': '',
                               'trip_info': '',
                               },
                              ],
            'event_date_from': None,
            'event_date_to': None,
            'event_type': 'trip',
            'is_locked': False,
            'is_new': False,
            'objective': 'goal',
            'receiver_side': '',
            'trip_date_from': trip_datetime_from.date(),
            'trip_date_to': trip_datetime_to.date(),
            }

    form = TripHeadForm(data=data, initial=trip.data)
    assert form.is_valid()

    trip.update(form.cleaned_data)
    trip.data['trip_issue'] = fake_trip_issue
    trip.data['employee_list'][0]['trip_issue'] = fake_trip_issue
    trip.save()

    op = OperationsRegistry(trip)

    activate_taxi_operation = CreateTaxiAccount(**op._get_emp_params(0))
    assert activate_taxi_operation.match_preconditions()

    # чтобы RemoveTaxiAccount.match_preconditions вернул True закроем тикет wont'fix-ом:
    trip.data['employee_list'][0]['is_taxi_account_active'] = True
    trip.data['employee_list'][0]['trip_issue']['status'] = {
        'self': 'https://st-api.test.yandex-team.ru/v2/statuses/3',
        'id': '3',
        'key': 'closed',
        'display': 'Закрыт'
    }
    trip.data['employee_list'][0]['trip_issue']['resolution'] = {
        'self': 'https://st-api.test.yandex-team.ru/v2/resolutions/2',
        'id': '2',
        'key': 'won\'tFix',
        'display': 'Не будет исправлено',
    }

    op = OperationsRegistry(trip)
    deactivate_taxi_operation = RemoveTaxiAccount(**op._get_emp_params(0))
    assert deactivate_taxi_operation.match_preconditions()


@pytest.mark.django_db
@mock.patch('staff.gap.controllers.counter.CounterCtl.get_counter')
@mock.patch('staff.trip_questionary.controller.operations.registry.OperationsCache')
def test_new_gap_from_trip_questionary(MockOperationCache, MockCounterCtl, mocked_mongo, client):
    staff_test = StaffFactory(login='staff_test')
    date_from = datetime.today()
    date_to = datetime.today() + timedelta(days=1)

    data = {
        'event_name': 'some',
        'purpose': ['1'],
        'objective': '',
        'event_date_from': date_from,
        'event_date_to': date_to,
        'event_cost': 'some',
        'employee_list': [{'employee': staff_test, 'comment': '', 'conf_issue': {'key': 'TKEY'}}],
        'conf_issue': {'key': 'TKEY'}
    }

    trip, event_type = _get_trip(None, 'conf', author=staff_test)
    trip.update(data)
    trip.save()

    op = OperationsRegistry(trip)

    create_gap = CreateGap(**op._get_emp_params(0))
    create_gap.run()

    created_gap = GapCtl().find_gap_by_id(1)

    assert created_gap['state'] == GS.NEW
    assert created_gap['gap_type'] == 'conference'
    assert created_gap['created_by_id'] == staff_test.id
    assert created_gap['modified_by_id'] == staff_test.id
    assert created_gap['master_issue'] == 'TKEY'
    assert created_gap['full_day'] is True

    response = client.get('{url}?l={login}&date_from={date_from}&date_to={date_to}'.format(
        url=reverse('gap:gaps-calendar'),
        login=staff_test.login,
        date_from=date_from,
        date_to=date_to,
    ))

    calendar_gaps = json.loads(response.content)['persons'][staff_test.login]['gaps']

    assert calendar_gaps
    assert calendar_gaps[0]['id'] == created_gap['id']
