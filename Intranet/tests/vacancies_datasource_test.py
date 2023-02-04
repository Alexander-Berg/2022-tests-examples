import pytest
from mock import patch

from staff.femida.constants import VACANCY_STATUS
from staff.femida.vacancies_datasource import VacanciesDatasource
from staff.lib.testing import StaffFactory, OccupationFactory

STUB_DATETIME = '2017-03-10T13:30:43.880971Z'
STUB_STATUS = VACANCY_STATUS.CLOSED


def make_row(row_id, name):
    person = StaffFactory()
    occupation = OccupationFactory()
    return {
        'id': row_id,
        'name': name,
        'created': STUB_DATETIME,
        'modified': STUB_DATETIME,
        'status': STUB_STATUS,
        'is_published': True,
        'is_hidden': True,
        'profession_staff_id': occupation.name,
        'department_url': 'yandex',
        'access': [person.login],
    }


@patch('staff.femida.vacancies_datasource.VacanciesDatasource._request')
@pytest.mark.django_db
def test_vacancies_datasource_single_page(request_mock):
    single_page_result = {'next': None, 'results': [make_row(1, '1'), make_row(2, '2')]}

    datasource = VacanciesDatasource()
    request_mock.return_value = single_page_result

    results = list(datasource)
    assert 2 == len(results)


@patch('staff.femida.vacancies_datasource.VacanciesDatasource._request')
@pytest.mark.django_db
def test_vacancies_datasource_three_pages(request_mock):
    return_results = [
        {'next': 'second_page', 'results': [make_row(1, '1'), make_row(2, '2')]},
        {'next': 'third_page', 'results': [make_row(3, '1'), make_row(4, '2')]},
        {'next': None, 'results': [make_row(5, '5')]},
    ]

    datasource = VacanciesDatasource()
    request_mock.side_effect = return_results

    results = list(datasource)
    assert 5 == len(results)
