from typing import List, Dict, Union

from django.test import override_settings
import pytest
from unittest.mock import patch, Mock

from intranet.femida.src.core.models import Location, WorkMode, City
from intranet.femida.src.startrek.utils import ResolutionEnum
from intranet.femida.src.vacancies.choices import VACANCY_STATUSES, VACANCY_RESOLUTIONS
from intranet.femida.src.vacancies.controllers import vacancy_close_by_issue, fill_cities
from intranet.femida.tests import factories as f


@pytest.mark.parametrize('issue_resolution, expected_resolution', (
    (ResolutionEnum.wont_fix, VACANCY_RESOLUTIONS.cancelled),
    (ResolutionEnum.transfer, VACANCY_RESOLUTIONS.move),
))
@patch('intranet.femida.src.vacancies.controllers.close_vacancy_applications')
@patch('intranet.femida.src.vacancies.controllers.archive_vacancy_publications')
def test_vacancy_close_by_issue(mocked_archive_publications, mocked_close_applications,
                                issue_resolution, expected_resolution):
    vacancy = f.create_vacancy()
    issue = Mock()
    issue.resolution.key = issue_resolution

    vacancy_close_by_issue(vacancy, issue)

    assert vacancy.status == VACANCY_STATUSES.closed
    assert vacancy.resolution == expected_resolution
    mocked_archive_publications.assert_called_once_with(vacancy)
    mocked_close_applications.assert_called_once_with(vacancy)


@pytest.mark.parametrize('issue_resolution', (
    ResolutionEnum.declined,
    ResolutionEnum.fixed,
))
@patch('intranet.femida.src.vacancies.controllers.close_vacancy_applications')
@patch('intranet.femida.src.vacancies.controllers.archive_vacancy_publications')
def test_vacancy_close_by_issue_wrong_resolution(mocked_archive_publications,
                                                 mocked_close_applications,
                                                 issue_resolution):
    vacancy = f.create_vacancy()
    issue = Mock()
    issue.resolution.key = issue_resolution

    vacancy_close_by_issue(vacancy, issue)

    assert vacancy.status != VACANCY_STATUSES.closed
    assert vacancy.resolution == ''
    assert not mocked_archive_publications.called
    assert not mocked_close_applications.called


CITY_HOMEWORKER_ID = 100500


@override_settings(CITY_HOMEWORKER_ID=CITY_HOMEWORKER_ID)
@pytest.mark.parametrize(
    'locations_name_en, work_mode_slug, cities, expected',
    (
        (
            ['Moscow', 'Saint-Petersburg'],
            ['remote'],

            [
                {'name_en': 'Moscow', 'id': 1},
                {'name_en': 'Saint-Petersburg', 'id': 2},
            ],
            [1, 2, CITY_HOMEWORKER_ID],
        ),
        (
            [],
            ['remote'],
            [
                {'name_en': 'Moscow', 'id': 1},
                {'name_en': 'Saint-Petersburg', 'id': 2},
            ],
            [CITY_HOMEWORKER_ID],
        ),
        (
            [],
            ['office'],
            [
                {'name_en': 'Moscow', 'id': 1},
                {'name_en': 'Saint-Petersburg', 'id': 2},
            ],
            [],
        ),
        (
            ['USA, New York City'],
            ['office'],
            [
                {'name_en': 'Moscow', 'id': 1},
                {'name_en': 'Saint-Petersburg', 'id': 2},
            ],
            [],
        ),
        (
            ['Germany, Berlin'],
            ['mixed'],
            [
                {'name_en': 'Moscow', 'id': 1},
                {'name_en': 'Saint-Petersburg', 'id': 2},
                {'name_en': 'Berlin', 'id': 3},
            ],
            [3],
        ),
        (
            ['Germany, Berlin', 'Russia, Moscow'],
            ['mixed'],
            [
                {'name_en': 'Moscow', 'id': 1},
                {'name_en': 'Saint-Petersburg', 'id': 2},
                {'name_en': 'Berlin', 'id': 3},
            ],
            [1, 3],
        ),
    )
)
def test_fill_cities(
    locations_name_en: List[str],
    work_mode_slug: List[str],
    cities: List[Dict[str, Union[str, int]]],
    expected: List[int],
):
    f.WorkModeFactory(slug=work_mode_slug)

    for name_en in locations_name_en:
        f.LocationFactory.create(name_en=name_en)

    for c in cities:
        f.CityFactory(**c)

    cities_obj = fill_cities(Location.objects.all(), WorkMode.objects.all())
    assert cities_obj == list(City.objects.filter(id__in=expected))


def test_fill_cities_none():
    assert fill_cities(None, None) == []
