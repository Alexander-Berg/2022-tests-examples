from mock import patch, Mock
import pytest

from staff.lib.testing import StaffFactory

from staff.oebs.controllers.datasources import PersonOccupationDatasource
from staff.oebs.models import PersonOccupation

json_data = '''
{"grades": [
    {
        "login": "denis-p",
        "gradeHist": [
            {
                "currency": null,
                "dateFrom": "2018-03-01",
                "dateTo": "4712-12-31",
                "gradeCity": "\u041c\u043e\u0441\u043a\u0432\u0430",
                "gradeName": "BackendDeveloper.14.2",
                "max": null,
                "mid": null,
                "min": null
            },
            {
                "currency": null,
                "dateFrom": "2017-08-04",
                "dateTo": "2018-02-28",
                "gradeCity": "\u041c\u043e\u0441\u043a\u0432\u0430",
                "gradeName": "BackendDeveloper.13.4",
                "max": null,
                "mid": null,
                "min": null
            }
        ]
    },
    {
        "login": "cracker",
        "gradeHist": [
            {
                "currency": null,
                "dateFrom": "2018-03-01",
                "dateTo": "4712-12-31",
                "gradeCity": "\u041c\u043e\u0441\u043a\u0432\u0430",
                "gradeName": "Developer.14.2",
                "max": null,
                "mid": null,
                "min": null
            },
            {
                "currency": null,
                "dateFrom": "2017-08-04",
                "dateTo": "2018-02-28",
                "gradeCity": "\u041c\u043e\u0441\u043a\u0432\u0430",
                "gradeName": "Developer.13.4",
                "max": null,
                "mid": null,
                "min": null
            }
        ]
    }
]}
'''


@pytest.mark.django_db
@patch('staff.oebs.controllers.datasources.oebs_datasource.OEBSDatasource._save_to_s3')
def test_person_occupation_sync(_save_mock, build_updater):
    StaffFactory(login='denis-p')
    StaffFactory(login='cracker')
    mock = Mock(return_value=Mock(content=json_data, status_code=200))
    session_mock = Mock(post=mock)
    datasource = PersonOccupationDatasource(PersonOccupation.oebs_type, PersonOccupation.method, session_mock)
    updater = build_updater(model=PersonOccupation, datasource=datasource)
    updater.run_sync()
    assert PersonOccupation.objects.count() == 2

    first_person_occupation = PersonOccupation.objects.get(login='denis-p')
    assert first_person_occupation.occupation == 'BackendDeveloper'

    first_person_occupation = PersonOccupation.objects.get(login='cracker')
    assert first_person_occupation.occupation == 'Developer'


update_json_data = '''
{"grades": [
    {
        "login": "denis-p",
        "gradeHist": [
            {
                "currency": null,
                "dateFrom": "2018-03-01",
                "dateTo": "4712-12-31",
                "gradeCity": "\u041c\u043e\u0441\u043a\u0432\u0430",
                "gradeName": "Some.14.2",
                "max": null,
                "mid": null,
                "min": null
            },
            {
                "currency": null,
                "dateFrom": "2017-08-04",
                "dateTo": "2018-02-28",
                "gradeCity": "\u041c\u043e\u0441\u043a\u0432\u0430",
                "gradeName": "BackendDeveloper.13.4",
                "max": null,
                "mid": null,
                "min": null
            }
        ]
    }
]}
'''


@pytest.mark.django_db
@patch('staff.oebs.controllers.datasources.oebs_datasource.OEBSDatasource._save_to_s3')
def test_person_occupation_sync_update(_save_mock, build_updater):
    StaffFactory(login='denis-p')
    mock = Mock(return_value=Mock(content=update_json_data, status_code=200))
    session_mock = Mock(post=mock)
    datasource = PersonOccupationDatasource(PersonOccupation.oebs_type, PersonOccupation.method, session_mock)

    updater = build_updater(model=PersonOccupation, datasource=datasource)
    updater.run_sync()

    assert PersonOccupation.objects.count() == 1

    first_person_occupation = PersonOccupation.objects.get(login='denis-p')
    assert first_person_occupation.occupation == 'Some'
