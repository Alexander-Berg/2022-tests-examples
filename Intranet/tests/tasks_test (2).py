from datetime import date, datetime, time, timedelta
import pytest
from django.db.transaction import get_connection
from mock import call, Mock, patch

from django.conf import settings

from staff.lib.testing import (
    StaffFactory,
    OrganizationFactory,
    CityFactory,
    OfficeFactory,
)
from staff.oebs.tests.factories import OrganizationFactory as OebsOrganizationFactory

from staff.gap.exceptions import MandatoryVacationYearChangedError
from staff.gap.tasks import (
    CreateMandatoryVacations,
    CreateMandatoryVacationsForIds,
    generate_mandatory_vacation_dates,
    MoveOrApproveMandatoryVacationsForGeoIds,
    remind_about_upcoming_mandatory_vacations,
    remind_about_upcoming_vacations,
    update_vacation_after_reminder_sent,
    get_vacations_to_remind,
    send_reminder_mail_about_upcoming_vacation,
)


class FakeVacationWorkflow:
    mocked_edit_gap = Mock()
    mocked_confirm_gap = Mock()

    @classmethod
    def init_to_modify(cls, *args, **kwargs):
        return cls()

    def edit_gap(self, *args, **kwargs):
        self.mocked_edit_gap(*args, **kwargs)
        assert get_connection().in_atomic_block

    def confirm_gap(self, *args, **kwargs):
        self.mocked_confirm_gap(*args, **kwargs)
        assert get_connection().in_atomic_block


class FakeCursor:
    def __init__(self, collection):
        self.collection = collection

    def count(self):
        return len(self.collection)

    def __iter__(self):
        return (doc for doc in self.collection)


class FakeTemplatesCtl:

    TEMPLATES = [
        {
            '_id': '1',
            'type': 'email',
            'tag': 'remind_about_mandatory_vacation_30_days_before',
            'template': 'Reminder 30 days before mandatory vacation',
        },
        {
            '_id': '2',
            'type': 'email',
            'tag': 'remind_about_mandatory_vacation_30_days_before_chief',
            'template': 'Reminder 30 days before mandatory vacation',
        },
        {
            '_id': '3',
            'type': 'email',
            'tag': 'remind_about_mandatory_vacation_21_days_before',
            'template': 'Reminder 21 days before mandatory vacation',
        },
        {
            '_id': '4',
            'type': 'email',
            'tag': 'remind_about_mandatory_vacation_21_days_before_chief',
            'template': 'Reminder 21 days before mandatory vacation',
        },
        {
            '_id': '5',
            'type': 'email',
            'tag': 'remind_about_vacation_3_days_before',
            'template': 'Reminder 3 days before vacation',
        },
        {
            '_id': '6',
            'type': 'email',
            'tag': 'remind_about_vacation_3_days_before_chief',
            'template': 'Reminder 3 days before vacation',
        },
    ]

    def __init__(self, *args, **kwargs):
        pass

    def find_one_not_strict(self, type, tag, **kwargs):
        for template in self.TEMPLATES:
            if template['tag'] == tag and template['type'] == type:
                return template


def get_fake_datetime_class(now):
    datetime_mock = Mock()
    datetime_mock.now = Mock(return_value=now)
    return datetime_mock


def get_fake_date_class(today):
    date_mock = Mock()
    date_mock.today = Mock(return_value=today)
    return date_mock


@pytest.mark.django_db
def test_create_mandatory_vacations(company):
    yandex_usa = OrganizationFactory(name='Яндекс.США', name_en='Yandex.USA')
    OebsOrganizationFactory(country_code='US', dis_organization_id=yandex_usa.id)

    moscow = CityFactory(geo_id=100)
    redrose = OfficeFactory(city=moscow)

    staff = [
        StaffFactory(
            login='uhura',
            organization=company.organizations['yandex'],
            vacation=15,
            affiliation='yandex',
            office=redrose,
        ),
        StaffFactory(
            login='hubble',
            organization=company.organizations['yandex'],
            vacation=15,
            affiliation='yandex',
            office=redrose,
        ),
        StaffFactory(
            login='spock',
            organization=yandex_usa,
            vacation=20,
            affiliation='yandex',
            office=redrose,
        ),
        StaffFactory(
            login='kirk',
            organization=company.organizations['yandex'],
            vacation=16,
            affiliation='yandex',
            office=redrose,
        ),
        StaffFactory(
            login='ivan',
            organization=company.organizations['yandex'],
            vacation=36,
            affiliation='external',
            office=redrose,
        ),
        StaffFactory(
            login='petr',
            organization=company.organizations['yandex'],
            vacation=9,
            affiliation='yandex',
            office=redrose,
        ),
    ]

    with patch('staff.gap.tasks.get_logins_with_mandatory_vacation', Mock(return_value={'hubble'})):
        with patch('staff.gap.tasks.CreateMandatoryVacationsForIds.delay') as mocked_task:
            CreateMandatoryVacations.locked_run()
            call_args = mocked_task.call_args[0][0]
            expected_call_args = [(staff[0].id, moscow.geo_id), (staff[3].id, moscow.geo_id)]
            assert call_args == expected_call_args or list(reversed(call_args)) == expected_call_args


@pytest.mark.django_db
def test_create_mandatory_vacations_for_ids():
    staff_amount = 5
    geo_id = 225
    staff_ids = [(StaffFactory().id, geo_id) for _ in range(staff_amount)]

    mock_new_gap = Mock()
    mock_workflow = Mock()
    mock_workflow.new_gap = mock_new_gap
    mock_workflow_class = Mock(return_value=mock_workflow)

    start = date(2021, 2, 1)
    finish = date(2021, 2, 16)
    deadline = date(2021, 1, 20)
    mock_generate_dates = Mock(return_value=(start, finish))

    next_year = date.today().year + 1
    min_start_date = date(year=next_year, **settings.MANDATORY_VACATION_MIN_START_DATE)
    max_start_date = date(year=next_year, **settings.MANDATORY_VACATION_MAX_START_DATE)

    with patch('staff.gap.tasks.VacationWorkflow', mock_workflow_class):
        with patch('staff.gap.tasks.get_date_n_business_days_before', Mock(return_value=deadline)):
            with patch('staff.gap.tasks.generate_mandatory_vacation_dates', mock_generate_dates):
                CreateMandatoryVacationsForIds.run(staff_ids)

                mock_workflow_class.assert_has_calls(
                    [
                        call(modifier_id=settings.ROBOT_STAFF_ID, person_id=staff_ids[i][0])
                        for i in range(staff_amount)
                    ],
                )
                mock_generate_dates.assert_has_calls(
                    [
                        call(min_start_date, max_start_date, geo_id)
                        for _ in range(staff_amount)
                    ],
                )
                assert mock_new_gap.call_count == staff_amount


def test_generate_mandatory_vacation_dates():
    min_start_date = date(2021, 1, 1)
    max_start_date = date(2021, 1, 31)
    geo_id = 225

    mock_randint = Mock(return_value=2)
    mock_min_finish_date = Mock()
    with patch('staff.gap.tasks.randint', mock_randint):
        with patch('staff.gap.tasks.get_min_mandatory_vacation_date_to', mock_min_finish_date):
            generate_mandatory_vacation_dates(min_start_date, max_start_date, geo_id)

    mock_randint.assert_called_once_with(0, max_start_date.day - min_start_date.day)
    mock_min_finish_date.assert_called_once_with(min_start_date + timedelta(mock_randint()), geo_id)


@pytest.mark.django_db
@patch('staff.gap.tasks.VacationWorkflow', FakeVacationWorkflow)
@patch('staff.gap.tasks.datetime', get_fake_datetime_class(now=datetime(2021, 8, 1)))
def test_move_mandatory_vacations_for_geo_ids():
    geo_id = 225
    today = date(2021, 8, 1)
    old_date_from = today + timedelta(settings.MANDATORY_VACATION_DEADLINE_DAYS)
    old_date_to = today + timedelta(settings.MANDATORY_VACATION_DURATION + settings.MANDATORY_VACATION_DEADLINE_DAYS)

    new_deadline = datetime.combine(
        date(2021, 8, 20),
        time.min,
    )
    new_date_from = datetime.combine(
        old_date_from + timedelta(settings.MANDATORY_VACATION_OFFSET),
        time.min,
    )
    new_date_to = datetime.combine(
        old_date_to + timedelta(settings.MANDATORY_VACATION_OFFSET),
        time.min,
    )

    mocked_get_data_to_move_gap = Mock(
        return_value=(
            new_date_from,
            new_date_to,
            new_deadline,
        ),
    )
    fake_vacations = FakeCursor(
        [
            {
                'date_from': old_date_from,
                'date_to': old_date_to,
                'deadline': today,
                'mandatory': True,
            },
        ],
    )
    with patch('staff.gap.tasks.GapCtl.find_gaps', Mock(return_value=fake_vacations)):
        with patch('staff.gap.tasks.get_data_to_move_gap', mocked_get_data_to_move_gap):
            MoveOrApproveMandatoryVacationsForGeoIds.run(geo_id)
            mocked_get_data_to_move_gap.assert_called_once_with(
                deadline=today,
                geo_id=geo_id,
            )
            FakeVacationWorkflow.mocked_edit_gap.assert_called_once_with(
                {
                    'date_from': new_date_from,
                    'date_to': new_date_to,
                    'deadline': new_deadline,
                },
            )


@pytest.mark.django_db
@patch('staff.gap.tasks.VacationWorkflow', FakeVacationWorkflow)
@patch('staff.gap.tasks.datetime', get_fake_datetime_class(now=datetime(2021, 8, 1)))
def test_approve_mandatory_vacations_for_geo_ids():
    geo_id = 225
    today = date(2021, 8, 1)

    mocked_get_data_to_move_gap = Mock(
        return_value=(None, None, None),
    )
    fake_vacations = FakeCursor(
        [
            {
                'deadline': today,
                'mandatory': True,
            },
        ],
    )
    with patch('staff.gap.tasks.GapCtl.find_gaps', Mock(return_value=fake_vacations)):
        with patch('staff.gap.tasks.get_data_to_move_gap', mocked_get_data_to_move_gap):
            FakeVacationWorkflow.mocked_edit_gap = Mock(side_effect=MandatoryVacationYearChangedError)
            MoveOrApproveMandatoryVacationsForGeoIds.run(geo_id)
            mocked_get_data_to_move_gap.assert_called_once_with(
                deadline=today,
                geo_id=geo_id,
            )
            FakeVacationWorkflow.mocked_confirm_gap.assert_called_once()


@pytest.mark.django_db
def test_remind_about_upcoming_mandatory_vacations(company):
    person = company.persons['dep12-person']
    person_dict = {
        'id': person.id,
        'login': person.login,
        'first_name': person.first_name,
        'last_name': person.last_name,
        'first_name_en': person.first_name_en,
        'last_name_en': person.last_name_en,
        'department_id': person.department_id,
        'work_email': person.work_email,
    }
    chief_dict = {
        'id': person.department.chief.id,
        'login': person.department.chief.login,
        'work_email': person.department.chief.work_email,
    }

    vacation = {
        'id': '1001',
        'state': 'new',
        'workflow': 'vacation',
        'date_from': datetime(2021, 1, 1),
        'date_to': datetime(2021, 1, 31),
        'full_day': True,
        'person_login': person.login,
        'person_id': person.id,
        'log': [],
    }

    with patch('staff.gap.tasks.get_vacations_to_remind', Mock(return_value=[vacation])):
        with patch('staff.gap.tasks.send_reminder_mail_about_upcoming_vacation') as mocked_send:
            with patch('staff.gap.tasks.update_vacation_after_reminder_sent') as mocked_update:
                remind_about_upcoming_mandatory_vacations()

                for reminder in settings.MANDATORY_VACATION_REMINDERS:
                    assert call(
                        vacation=vacation,
                        person_dict=person_dict,
                        chief_dict=chief_dict,
                        tag=f'remind_about_mandatory_vacation_{reminder}_days_before',
                    ) in mocked_send.call_args_list

                    assert call(vacation, reminder) in mocked_update.call_args_list


@pytest.mark.django_db
def test_remind_about_upcoming_vacations(company):
    person = company.persons['dep12-person']
    person_dict = {
        'id': person.id,
        'login': person.login,
        'first_name': person.first_name,
        'last_name': person.last_name,
        'first_name_en': person.first_name_en,
        'last_name_en': person.last_name_en,
        'department_id': person.department_id,
        'work_email': person.work_email,
    }
    chief_dict = {
        'id': person.department.chief.id,
        'login': person.department.chief.login,
        'work_email': person.department.chief.work_email,
    }
    reminder = settings.VACATION_REMINDER

    vacation = {
        'id': '1001',
        'state': 'new',
        'workflow': 'vacation',
        'date_from': datetime(2021, 1, 1),
        'date_to': datetime(2021, 1, 31),
        'full_day': True,
        'person_login': person.login,
        'person_id': person.id,
        'log': [],
    }

    with patch('staff.gap.tasks.get_vacations_to_remind', Mock(return_value=[vacation])):
        with patch('staff.gap.tasks.send_reminder_mail_about_upcoming_vacation') as mocked_send:
            with patch('staff.gap.tasks.update_vacation_after_reminder_sent') as mocked_update:
                remind_about_upcoming_vacations()

                mocked_send.assert_called_with(
                    vacation=vacation,
                    person_dict=person_dict,
                    chief_dict=chief_dict,
                    tag=f'remind_about_vacation_{reminder}_days_before',
                )
                mocked_update.assert_called_with(vacation, reminder)


@pytest.mark.django_db
@patch('staff.gap.controllers.email_ctl.TemplatesCtl', FakeTemplatesCtl)
@patch('staff.gap.controllers.templates.TemplatesCtl', FakeTemplatesCtl)
def test_update_vacation_after_reminder_sent(robot_staff_user, company):
    person = company.persons['dep12-person']
    settings.ROBOT_STAFF_ID = robot_staff_user.get_profile().id

    reminder = 21
    vacation = {
        'id': '1001',
        'state': 'new',
        'workflow': 'vacation',
        'date_from': datetime(2021, 1, 1),
        'date_to': datetime(2021, 1, 31),
        'full_day': True,
        'person_login': person.login,
        'person_id': person.id,
        'to_notify': [],
        'vacation_updated': False,
    }

    with patch('staff.gap.controllers.gap.GapCtl.update_gap') as mocked_update_gap:
        update_vacation_after_reminder_sent(vacation, reminder)

        expected_updated_vacation = {
            'id': '1001',
            'state': 'new',
            'workflow': 'vacation',
            'date_from': datetime(2021, 1, 1),
            'date_to': datetime(2021, 1, 31),
            'full_day': True,
            'person_login': person.login,
            'person_id': person.id,
            'reminders_sent': [21],
            'to_notify': [],
            'vacation_updated': False,
        }

        mocked_update_gap.assert_called_with(
            settings.ROBOT_STAFF_ID,
            expected_updated_vacation,
        )


@patch('staff.gap.tasks.date', get_fake_date_class(today=date(2021, 8, 1)))
def test_get_vacations_to_remind():
    reminder = 3
    other_reminders = [30, 21]
    all_reminders = [30, 21, 3]
    vacations = FakeCursor(
        [
            {
                'id': '1001',
            },
            {
                'id': '1002',
                'reminders_sent': other_reminders,
            },
            {
                'id': '1003',
                'reminders_sent': all_reminders,
            },
        ],
    )
    mocked_find_gaps = Mock(return_value=vacations)
    with patch('staff.gap.tasks.GapCtl.find_gaps', mocked_find_gaps):
        vacations_to_remind = get_vacations_to_remind(reminder, mandatory_only=True)
        expected_query = {
            'workflow': 'vacation',
            'mandatory': True,
            'date_from': {
                '$gte': datetime(2021, 8, 1) + timedelta(reminder),
                '$lte': datetime(2021, 8, 1) + timedelta(reminder + 1),
            },
        }

        mocked_find_gaps.assert_called_with(expected_query)
        assert len(vacations_to_remind) == 2


@pytest.mark.django_db
@patch('staff.gap.controllers.email_ctl.TemplatesCtl', FakeTemplatesCtl)
@patch('staff.gap.controllers.templates.TemplatesCtl', FakeTemplatesCtl)
def test_send_reminder_mail_about_upcoming_vacation(company):

    person = company.persons['dep12-person']
    person_dict = {
        'id': person.id,
        'login': person.login,
        'first_name': person.first_name,
        'last_name': person.last_name,
        'first_name_en': person.first_name_en,
        'last_name_en': person.last_name_en,
        'work_email': person.work_email,
    }
    chief = company.persons['dep12-chief']
    chief_dict = {
        'id': chief.id,
        'login': chief.login,
        'work_email': chief.work_email,
    }

    vacation = {
        'id': '1001',
        'state': 'new',
        'workflow': 'vacation',
        'date_from': datetime(2021, 1, 1),
        'date_to': datetime(2021, 1, 31),
        'full_day': True,
        'person_login': person.login,
        'person_id': person.id,
    }

    fake_vacations = FakeCursor([vacation])

    with patch('staff.gap.tasks.GapCtl.find_gaps', Mock(return_value=fake_vacations)):
        with patch('staff.gap.controllers.email_ctl.EmailCtl._send') as mocked_send:
            tag = 'remind_about_vacation_3_days_before'
            send_reminder_mail_about_upcoming_vacation(vacation, person_dict, chief_dict, tag)

            expected_to_send = [
                {
                    'template': FakeTemplatesCtl().find_one_not_strict(type='email', tag=(tag + '_chief')),
                    'to_send': [
                        {
                            'email': chief.work_email,
                            'context': {
                                'addressee': {
                                    'login': chief.login,
                                    'lang_ui': chief.lang_ui,
                                    'organization_id': chief.organization_id,
                                    'office__city_id': chief.office.city_id,
                                },
                            },
                            'tag': tag + '_chief',
                        },
                    ],
                },
                {
                    'template': FakeTemplatesCtl().find_one_not_strict(type='email', tag=tag),
                    'to_send': [
                        {
                            'email': person.work_email,
                            'context': {
                                'addressee': {
                                    'login': person.login,
                                    'lang_ui': person.lang_ui,
                                    'organization_id': person.organization_id,
                                    'office__city_id': person.office.city_id,
                                },
                            },
                            'tag': tag,
                        },
                    ],
                },
            ]

            mocked_send.assert_called_with(
                person_dict,
                person_dict,
                expected_to_send,
                None,
            )
