import json
from datetime import date, timedelta
from unittest.mock import patch

import pytest
from django.conf import settings
from django.core.urlresolvers import reverse

from staff.departments.tests.factories import StaffOfficeLogFactory
from staff.departments.tree.export_attendance import ExportAttendance, ExportAttendanceAccessException
from staff.departments.tree.views import export_attendance
from staff.lib.testing import OfficeFactory, StaffFactory, DepartmentFactory, CityFactory
from staff.person.models import Staff


def _create_fake_work_days(date_from, length, offset=0):
    return {settings.RUSSIA_GEO_ID: [{'date': date_from + timedelta(days=day)} for day in range(length)][offset:]}


@pytest.mark.django_db
def test_export_attendance_works(rf, mocked_mongo):
    city = CityFactory(geo_id=settings.RUSSIA_GEO_ID)
    office = OfficeFactory(city=city)
    department = DepartmentFactory()
    for i in range(settings.EXPORT_ATTENDANCE_MIN_FILTER_PERSONS):
        StaffFactory(office=office, department=department)

    date_to = date.today()
    date_from = date_to - timedelta(days=settings.EXPORT_ATTENDANCE_MIN_PERIOD_DAYS)

    fake_work_days = _create_fake_work_days(date_from, settings.EXPORT_ATTENDANCE_MIN_PERIOD_DAYS)
    url = reverse('departments-api:export-attendance')
    request = rf.get(url, {'date_from': date_from, 'date_to': date_to})
    request.user = StaffFactory().user
    with patch('staff.departments.tree.export_attendance.get_holidays', return_value=fake_work_days):
        response = export_attendance(request)

    assert response.status_code == 200


@pytest.mark.django_db
def test_export_attendance_works_with_department(rf, mocked_mongo):
    city = CityFactory(geo_id=settings.RUSSIA_GEO_ID)
    office = OfficeFactory(city=city)
    department = DepartmentFactory()
    for i in range(settings.EXPORT_ATTENDANCE_MIN_FILTER_PERSONS):
        StaffFactory(office=office, department=department)

    other_department = DepartmentFactory()
    other_persons = []
    for i in range(settings.EXPORT_ATTENDANCE_MIN_FILTER_PERSONS):
        other_persons.append(StaffFactory(office=office, department=other_department))

    date_to = date.today()
    date_from = date_to - timedelta(days=settings.EXPORT_ATTENDANCE_MIN_PERIOD_DAYS)

    for day in range(settings.EXPORT_ATTENDANCE_MIN_PERIOD_DAYS):
        target_date = date_from + timedelta(days=day)
        for other_person in other_persons:
            StaffOfficeLogFactory(staff=other_person, office=office, date=target_date)

    fake_work_days = _create_fake_work_days(date_from, settings.EXPORT_ATTENDANCE_MIN_PERIOD_DAYS)
    url = reverse('departments-api:export-attendance')
    request = rf.get(url, {'department': department.id, 'date_from': date_from, 'date_to': date_to})
    request.user = StaffFactory().user
    with patch('staff.departments.tree.export_attendance.get_holidays', return_value=fake_work_days):
        response = export_attendance(request)

    assert response.status_code == 200
    response_decoded = json.loads(response.content)
    assert response_decoded['median'] == response_decoded['average'] == 0.0


@pytest.mark.django_db
def test_export_attendance_validates_form(rf):
    url = reverse('departments-api:export-attendance')
    request = rf.get(url)
    request.user = StaffFactory().user
    response = export_attendance(request)
    assert response.status_code == 400

    response_decoded = json.loads(response.content)
    assert response_decoded['errors']['date_from'][0]['code'] == 'required'
    assert response_decoded['errors']['date_to'][0]['code'] == 'required'


@pytest.mark.django_db
def test_export_attendance_raises_too_few_persons():
    city = CityFactory(geo_id=settings.RUSSIA_GEO_ID)
    office = OfficeFactory(city=city)
    department = DepartmentFactory()

    for i in range(settings.EXPORT_ATTENDANCE_MIN_FILTER_PERSONS - 1):
        StaffFactory(office=office, department=department)

    date_to = date.today()
    date_from = date_to - timedelta(days=settings.EXPORT_ATTENDANCE_MIN_PERIOD_DAYS)

    queryset = Staff.objects.filter(department=department)
    exporter = ExportAttendance(queryset, date_from, date_to)

    fake_work_days = _create_fake_work_days(date_from, settings.EXPORT_ATTENDANCE_MIN_PERIOD_DAYS)
    with patch('staff.departments.tree.export_attendance.get_holidays', return_value=fake_work_days):
        with pytest.raises(ExportAttendanceAccessException) as exc_info:
            exporter.export()

    assert exc_info.value.field == 'filter'
    assert exc_info.value.message == 'too_few_persons'


@pytest.mark.django_db
def test_export_attendance_excludes_holidays():
    city = CityFactory(geo_id=settings.RUSSIA_GEO_ID)
    office = OfficeFactory(city=city)
    department = DepartmentFactory()

    for i in range(settings.EXPORT_ATTENDANCE_MIN_FILTER_PERSONS):
        StaffFactory(office=office, department=department)

    period_length = settings.EXPORT_ATTENDANCE_MIN_PERIOD_DAYS * 2
    date_to = date.today()
    date_from = date_to - timedelta(days=period_length)

    person = StaffFactory(office=office, department=department)
    person2 = StaffFactory(office=office, department=department)
    for day in range(settings.EXPORT_ATTENDANCE_MIN_PERIOD_DAYS * 2):
        target_date = date_from + timedelta(days=day)
        StaffOfficeLogFactory(staff=person, office=office, date=target_date)
        if day < settings.EXPORT_ATTENDANCE_MIN_PERIOD_DAYS:
            StaffOfficeLogFactory(staff=person2, office=office, date=target_date)

    queryset = Staff.objects.filter(department=department)
    exporter = ExportAttendance(queryset, date_from, date_to)

    fake_work_days = _create_fake_work_days(date_from, period_length, offset=settings.EXPORT_ATTENDANCE_MIN_PERIOD_DAYS)
    with patch('staff.departments.tree.export_attendance.get_holidays', return_value=fake_work_days):
        result = exporter.export()

    assert result['average'] == 0.1


@pytest.mark.django_db
def test_export_attendance_excludes_foreign_office():
    city = CityFactory(geo_id=settings.RUSSIA_GEO_ID)
    office, foreign_office = OfficeFactory(city=city), OfficeFactory(city=city)
    department = DepartmentFactory()

    for i in range(settings.EXPORT_ATTENDANCE_MIN_FILTER_PERSONS):
        StaffFactory(office=office, department=department)

    date_to = date.today()
    date_from = date.today() - timedelta(days=settings.EXPORT_ATTENDANCE_MIN_PERIOD_DAYS)

    person = StaffFactory(office=office, department=department)
    for day in range(settings.EXPORT_ATTENDANCE_MIN_PERIOD_DAYS):
        target_date = date_from + timedelta(days=day)
        StaffOfficeLogFactory(staff=person, office=foreign_office, date=target_date)

    queryset = Staff.objects.filter(department=department)
    exporter = ExportAttendance(queryset, date_from, date_to)
    fake_work_days = _create_fake_work_days(date_from, settings.EXPORT_ATTENDANCE_MIN_PERIOD_DAYS)
    with patch('staff.departments.tree.export_attendance.get_holidays', return_value=fake_work_days):
        result = exporter.export()

    assert result['median'] == result['average'] == 0
