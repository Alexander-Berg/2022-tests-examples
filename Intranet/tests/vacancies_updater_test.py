from mock import patch

import pytest

from staff.lib.sync_tools.updater import Updater, DataDiffMerger
from staff.lib.testing import StaffFactory, OccupationFactory

from staff.femida.models import FemidaVacancy
from staff.femida.constants import VACANCY_STATUS
from staff.femida.tests.utils import FemidaVacancyFactory
from staff.femida.vacancies_datagenerator import VacanciesDataGenerator
from staff.femida.vacancies_datasource import VacanciesDatasource

STUB_DATETIME = '2017-03-10T13:30:43.880971Z'


def make_row(row_id, name, status, offer, budget_position_id=None):
    person = StaffFactory()
    occupation = OccupationFactory()
    result = {
        'id': row_id,
        'name': name,
        'created': STUB_DATETIME,
        'modified': STUB_DATETIME,
        'is_published': True,
        'is_hidden': True,
        'profession_staff_id': occupation.name,
        'department_url': 'yandex',
        'status': status,
        'offer': offer,
        'access': [person.login],
    }

    if budget_position_id:
        result['budget_position_id'] = budget_position_id

    return result


@pytest.mark.django_db
@patch('staff.femida.vacancies_datasource.VacanciesDatasource._request')
def test_updater(request_mock, company):
    FemidaVacancyFactory(id=100, name='To be changed', department_url='yandex_dep1')
    FemidaVacancyFactory(id=101, name='To be deleted')

    page_result = {
        'next': None,
        'results': [
            make_row(100, 'Changed', VACANCY_STATUS.CLOSED, {'id': 42}),
            make_row(102, 'New', VACANCY_STATUS.IN_PROGRESS, {'id': 44}, 100500),
        ],
    }

    request_mock.return_value = page_result

    data_source = VacanciesDatasource()
    data_generator = VacanciesDataGenerator(data_source)
    data_diff_merger = DataDiffMerger(data_generator)
    updater = Updater(data_diff_merger, None)
    results = updater.run_sync()

    assert results['updated'] == 1
    assert results['created'] == 1
    assert results['deleted'] == 1
    assert results['all'] == 3
    assert results['skipped'] == 0

    new_vacancy = FemidaVacancy.objects.get(id=102)
    assert new_vacancy.name == 'New'
    assert new_vacancy.status == VACANCY_STATUS.IN_PROGRESS
    assert new_vacancy.offer_id == 44
    assert new_vacancy.headcount_position_id == 100500

    changed_vacancy = FemidaVacancy.objects.get(id=100)
    assert changed_vacancy.department_url == 'yandex'
